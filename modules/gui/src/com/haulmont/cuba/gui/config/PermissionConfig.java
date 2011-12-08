/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 12.03.2009 15:51:46
 *
 * $Id$
 */
package com.haulmont.cuba.gui.config;

import com.haulmont.bali.datastruct.Node;
import com.haulmont.bali.datastruct.Tree;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.*;
import com.haulmont.cuba.core.entity.Updatable;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.ConfigurationResourceLoader;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.security.entity.PermissionTarget;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.core.io.Resource;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * GenericUI class holding information about all permission targets.
 */
@ManagedBean("cuba_PermissionConfig")
public class PermissionConfig {

    public static final String PERMISSION_CONFIG_XML_PROP = "cuba.permissionConfig";

    private class Item {
        private Locale locale;

        private Tree<PermissionTarget> screens;
        private Tree<PermissionTarget> entities;
        private Tree<PermissionTarget> specific;

        private Item(Locale locale) {
            this.locale = locale;

            compileScreens();
            compileEntities();
            compileSpecific();
        }

        private String getMessage(String key) {
            return MessageProvider.getMessage(messagePack, key, locale);
        }

        private void compileScreens() {
            Node<PermissionTarget> root = new Node<PermissionTarget>(
                    new PermissionTarget("menu", getMessage("permissionConfig.screenRoot"), null)
            );
            screens = new Tree<PermissionTarget>(root);

            walkMenu(root);
        }

        private void walkMenu(Node<PermissionTarget> node) {
            for (MenuItem info : menuConfig.getRootItems()) {
                walkMenu(info, node);
            }
        }

        private void walkMenu(MenuItem info, Node<PermissionTarget> node) {
            String id = info.getId();
            String caption = MenuConfig.getMenuItemCaption(id).replaceAll("<.+?>", "").replaceAll("&gt;","");
            caption = StringEscapeUtils.unescapeHtml(caption);

            if (info.getChildren() != null && !info.getChildren().isEmpty()) {
                Node<PermissionTarget> n = new Node<PermissionTarget>(new PermissionTarget("category:" + id, caption, UserSession.getScreenPermissionTarget(clientType, id)));
                node.addChild(n);
                for (MenuItem item : info.getChildren()) {
                    walkMenu(item, n);
                }
            } else {
                if (!"-".equals(info.getId())) {
                    Node<PermissionTarget> n = new Node<PermissionTarget>(
                            new PermissionTarget("item:" + id, caption, UserSession.getScreenPermissionTarget(clientType, id)));
                    node.addChild(n);
                }
            }
        }

        private void compileEntities() {
            Node<PermissionTarget> root = new Node<PermissionTarget>(new PermissionTarget("session", getMessage("permissionConfig.entityRoot"), null));
            entities = new Tree<PermissionTarget>(root);

            Session session = MetadataProvider.getSession();
            List<MetaModel> modelList = new ArrayList<MetaModel>(session.getModels());
            Collections.sort(modelList, new MetadataObjectAlphabetComparator());

            for (MetaModel model : modelList) {
                Node<PermissionTarget> modelNode = new Node<PermissionTarget>(new PermissionTarget("model:" + model.getName(), model.getName(), null));
                root.addChild(modelNode);

                List<MetaClass> classList = new ArrayList<MetaClass>(model.getClasses());
                Collections.sort(classList, new MetadataObjectAlphabetComparator());

                for (MetaClass metaClass : classList) {
                    String name = metaClass.getName();
                    if (name.contains("$")) {
                        String caption = name + " (" + MessageUtils.getEntityCaption(metaClass) + ")";
                        Node<PermissionTarget> node = new Node<PermissionTarget>(new PermissionTarget("entity:" + name, caption, name));
                        modelNode.addChild(node);
                    }
                }
            }
        }

        private void compileSpecific() {
            Node<PermissionTarget> root = new Node<PermissionTarget>(new PermissionTarget("specific", getMessage("permissionConfig.specificRoot"), null));
            specific = new Tree<PermissionTarget>(root);

            final String configName = AppContext.getProperty(PERMISSION_CONFIG_XML_PROP);
            ConfigurationResourceLoader resourceLoader = new ConfigurationResourceLoader();
            StrTokenizer tokenizer = new StrTokenizer(configName);
            for (String location : tokenizer.getTokenArray()) {
                Resource resource = resourceLoader.getResource(location);
                if (resource.exists()) {
                    InputStream stream = null;
                    try {
                        stream = resource.getInputStream();
                        String xml = IOUtils.toString(stream);
                        compileSpecific(xml, root);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                } else {
                    log.warn("Resource " + location + " not found, ignore it");
                }
            }
        }

        private void compileSpecific(String xml, Node<PermissionTarget> root) {
            Document doc = Dom4j.readDocument(xml);
            Element rootElem = doc.getRootElement();

            for (Element element : Dom4j.elements(rootElem, "include")) {
                String fileName = element.attributeValue("file");
                if (!StringUtils.isBlank(fileName)) {
                    String incXml = ScriptingProvider.getResourceAsString(fileName);
                    if (incXml == null)
                        throw new RuntimeException("Config file not found: " + fileName);

                    compileSpecific(incXml, root);
                }
            }

            Element specElem = rootElem.element("specific");
            if (specElem == null)
                return;

            walkSpecific(specElem, root);
        }

        private void walkSpecific(Element element, Node<PermissionTarget> node) {
            //noinspection unchecked
            for (Element elem : (List<Element>) element.elements()) {
                String id = elem.attributeValue("id");
                String caption = getMessage("permission-config." + id);
                if ("category".equals(elem.getName())) {
                    Node<PermissionTarget> n = new Node<PermissionTarget>(new PermissionTarget("category:" + id, caption, null));
                    node.addChild(n);
                    walkSpecific(elem, n);
                } else if ("permission".equals(elem.getName())) {
                    Node<PermissionTarget> n = new Node<PermissionTarget>(new PermissionTarget("permission:" + id, caption, id));
                    node.addChild(n);
                }
            }
        }
    }

    @Inject
    private MenuConfig menuConfig;

    private ClientType clientType;
    private String messagePack;

    private List<Item> items = new ArrayList<Item>();

    private Log log = LogFactory.getLog(PermissionConfig.class);

    public PermissionConfig() {
        this.clientType = AppConfig.getClientType();
        this.messagePack = AppConfig.getMessagesPack();
    }

    private Item getItem(Locale locale) {
        for (Item item : items) {
            if (item.locale.equals(locale))
                return item;
        }
        Item item = new Item(locale);
        items.add(item);
        return item;
    }

    /**
     * All registered screens
     */
    public Tree<PermissionTarget> getScreens(Locale locale) {
        return getItem(locale).screens;
    }

    /**
     * All registered entities
     */
    public Tree<PermissionTarget> getEntities(Locale locale) {
        return getItem(locale).entities;
    }

    /**
     * All specific permissions
     */
    public Tree<PermissionTarget> getSpecific(Locale locale) {
        return getItem(locale).specific;
    }

    /**
     * Entity operations for specified target
     * @return list of {@link com.haulmont.cuba.security.entity.PermissionTarget} objects
     */
    public List<PermissionTarget> getEntityOperations(PermissionTarget entityTarget) {
        if (entityTarget == null) return Collections.emptyList();

        final String id = entityTarget.getId();
        if (!id.startsWith("entity:")) return Collections.emptyList();

        MetaClass metaClass = MetadataProvider.getSession().getClass(id.substring("entity:".length()));
        if (metaClass == null) return Collections.emptyList();

        List<PermissionTarget> result = new ArrayList<PermissionTarget>();
        final String value = entityTarget.getPermissionValue();

        result.add(new PermissionTarget(id + ":read", "read", value + ":read"));

        Class javaClass = metaClass.getJavaClass();

        if (javaClass.isAnnotationPresent(javax.persistence.Entity.class)) {
            result.add(new PermissionTarget(id + ":create", "create", value + ":create"));
            result.add(new PermissionTarget(id + ":delete", "delete", value + ":delete"));
        }

        if (Updatable.class.isAssignableFrom(javaClass)) {
            result.add(new PermissionTarget(id + ":update", "update", value + ":update"));
        }

        return result;
    }

    /**
     * Entity attributes for specified target
     * @return list of {@link com.haulmont.cuba.security.entity.PermissionTarget} objects
     */
    public List<PermissionTarget> getEntityAttributes(PermissionTarget entityTarget) {
        if (entityTarget == null) return Collections.emptyList();

        final String id = entityTarget.getId();
        if (!id.startsWith("entity:")) return Collections.emptyList();

        MetaClass metaClass = MetadataProvider.getSession().getClass(id.substring("entity:".length()));
        if (metaClass == null) return Collections.emptyList();

        final String value = entityTarget.getPermissionValue();

        List<MetaProperty> propertyList = new ArrayList<MetaProperty>(metaClass.getProperties());
        Collections.sort(propertyList, new MetadataObjectAlphabetComparator());

        List<PermissionTarget> result = new ArrayList<PermissionTarget>();
        for (MetaProperty metaProperty : propertyList) {
            String caption = metaProperty.getName() + " (" + MessageUtils.getPropertyCaption(metaProperty) + ")";
            result.add(new PermissionTarget(id + ":" + metaProperty.getName(), caption, value + ":" + metaProperty.getName()));
        }

        return result;
    }

    private class MetadataObjectAlphabetComparator implements Comparator<MetadataObject> {
        public int compare(MetadataObject o1, MetadataObject o2) {
            String n1 = o1 != null ? o1.getName() : null;
            String n2 = o2 != null ? o2.getName() : null;

            return n1 != null ? n1.compareTo(n2) : -1;
        }
    }
}
