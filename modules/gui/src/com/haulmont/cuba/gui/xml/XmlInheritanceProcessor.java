/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.gui.xml.layout.LayoutLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.*;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

/**
 * Provides inheritance of screen XML descriptors.
 *
 * @author krivopustov
 * @version $Id$
 */
public class XmlInheritanceProcessor {

    private static Log log = LogFactory.getLog(XmlInheritanceProcessor.class);

    private Document document;
    private Namespace extNs;
    private Map<String, Object> params;

    private List<ElementTargetLocator> targetLocators = new ArrayList<>();

    protected Resources resources = AppBeans.get(Resources.class);

    public XmlInheritanceProcessor(Document document, Map<String, Object> params) {
        this.document = document;
        this.params = params;

        extNs = document.getRootElement().getNamespaceForPrefix("ext");

        targetLocators.add(new ViewPropertyElementTargetLocator());
        targetLocators.add(new ViewElementTargetLocator());
        targetLocators.add(new ButtonElementTargetLocator());
        targetLocators.add(new CommonElementTargetLocator());
    }

    public Element getResultRoot() {
        Element result;

        Element root = document.getRootElement();
        String ancestorTemplate = root.attributeValue("extends");
        if (!StringUtils.isBlank(ancestorTemplate)) {
            InputStream ancestorStream = resources.getResourceAsStream(ancestorTemplate);
            if (ancestorStream == null) {
                ancestorStream = getClass().getResourceAsStream(ancestorTemplate);
                if (ancestorStream == null) {
                    throw new DevelopmentException("Template is not found", "Ancestor's template path", ancestorTemplate);
                }
            }
            Document ancestorDocument;
            try {
                ancestorDocument = LayoutLoader.parseDescriptor(ancestorStream, params);
            } finally {
                IOUtils.closeQuietly(ancestorStream);
            }
            XmlInheritanceProcessor processor = new XmlInheritanceProcessor(ancestorDocument, params);
            result = processor.getResultRoot();
            process(result, root);

            if (log.isTraceEnabled()) {
                StringWriter writer = new StringWriter();
                Dom4j.writeDocument(result.getDocument(), true, writer);
                log.trace("Resulting template:\n" + writer.toString());
            }
        } else {
            result = root;
        }

        return result;
    }

    private void process(Element resultElem, Element extElem) {
        // set text
        if (!StringUtils.isBlank(extElem.getText()))
            resultElem.setText(extElem.getText());

        // add all attributes from extension
        for (Attribute attribute : Dom4j.attributes(extElem)) {
            if (resultElem == document.getRootElement() && attribute.getName().equals("extends")) {
                // ignore "extends" in root element
                continue;
            }
            resultElem.addAttribute(attribute.getName(), attribute.getValue());
        }

        // add and process elements
        Set<Element> justAdded = new HashSet<Element>();
        for (Element element : Dom4j.elements(extElem)) {
            // look for suitable locator
            ElementTargetLocator locator = null;
            for (ElementTargetLocator l : targetLocators) {
                if (l.suitableFor(element)) {
                    locator = l;
                    break;
                }
            }
            if (locator != null) {
                Element target = locator.locate(resultElem, element);
                // process target or a new element if target not found
                if (target != null) {
                    process(target, element);
                } else {
                    addNewElement(resultElem, element, justAdded);
                }
            } else {
                // if no suitable locator found, look for a single element with the same name
                List<Element> list = Dom4j.elements(resultElem, element.getName());
                if (list.size() == 1 && !justAdded.contains(list.get(0))) {
                    process(list.get(0), element);
                } else {
                    addNewElement(resultElem, element, justAdded);
                }
            }
        }
    }

    private void addNewElement(Element resultElem, Element element, Set<Element> justAdded) {
        String idx = element.attributeValue(new QName("index", extNs));
        Element newElem;
        if (StringUtils.isBlank(idx)) {
            newElem = resultElem.addElement(element.getName());
        } else {
            newElem = DocumentHelper.createElement(element.getName());
            List elements = resultElem.elements();
            elements.add(Integer.valueOf(idx), newElem);
        }
        justAdded.add(newElem);
        process(newElem, element);
    }

    private interface ElementTargetLocator {
        boolean suitableFor(Element extElem);
        Element locate(Element resultParentElem, Element extElem);
    }

    private static class CommonElementTargetLocator implements ElementTargetLocator {

        public boolean suitableFor(Element extElem) {
            return !StringUtils.isBlank(extElem.attributeValue("id"));
        }

        public Element locate(Element resultParentElem, Element extElem) {
            String id = extElem.attributeValue("id");
            for (Element e : Dom4j.elements(resultParentElem)) {
                if (id.equals(e.attributeValue("id"))) {
                    return e;
                }
            }
            return null;
        }
    }

    private static class ViewElementTargetLocator implements ElementTargetLocator {

        public boolean suitableFor(Element extElem) {
            return "view".equals(extElem.getName());
        }

        public Element locate(Element resultParentElem, Element extElem) {
            String entity = extElem.attributeValue("entity");
            String clazz = extElem.attributeValue("class");
            String name = extElem.attributeValue("name");
            for (Element e : Dom4j.elements(resultParentElem)) {
                if (name.equals(e.attributeValue("name"))
                        && ((entity != null && entity.equals(e.attributeValue("entity")))
                            || (clazz != null && clazz.equals(e.attributeValue("class")))))
                {
                    return e;
                }
            }

            return null;
        }
    }

    private static class ViewPropertyElementTargetLocator implements ElementTargetLocator {

        public boolean suitableFor(Element extElem) {
            return "property".equals(extElem.getName());
        }

        public Element locate(Element resultParentElem, Element extElem) {
            String name = extElem.attributeValue("name");
            for (Element e : Dom4j.elements(resultParentElem)) {
                if (name.equals(e.attributeValue("name"))) {
                    return e;
                }
            }
            return null;
        }
    }

    private static class ButtonElementTargetLocator implements ElementTargetLocator {

        public boolean suitableFor(Element extElem) {
            return "button".equals(extElem.getName())
                    && extElem.attributeValue("id") == null
                    && extElem.attributeValue("action") != null;
        }

        public Element locate(Element resultParentElem, Element extElem) {
            String action = extElem.attributeValue("action");
            for (Element e : Dom4j.elements(resultParentElem)) {
                if (action.equals(e.attributeValue("action"))) {
                    return e;
                }
            }
            return null;
        }
    }
}
