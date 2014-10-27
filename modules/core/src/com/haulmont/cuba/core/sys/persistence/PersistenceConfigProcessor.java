/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.persistence;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.ConfigurationResourceLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.core.io.Resource;

import javax.persistence.Entity;
import java.io.*;
import java.util.*;

/**
 * Generates a working persistence.xml file combining classes and properties from a set of given persistence.xml files,
 * defined in <code>cuba.persistenceConfig</code> app property.
 *
 * @author krivopustov
 * @version $Id$
 */
public class PersistenceConfigProcessor {

    private String baseDir;
    private List<String> sourceFileNames;
    private String outFileName;

    private Log log = LogFactory.getLog(getClass());

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public void setSourceFiles(List<String> files) {
        sourceFileNames = files;
    }

    public void setOutputFile(String file) {
        outFileName = file;
    }

    public void create() {
        if (sourceFileNames == null || sourceFileNames.isEmpty())
            throw new IllegalStateException("Source file list not set");
        if (StringUtils.isBlank(outFileName))
            throw new IllegalStateException("Output file not set");

        Map<String, String> classes = new LinkedHashMap<String, String>();
        Map<String, String> properties = new HashMap<String, String>();

        properties.putAll(DbmsSpecificFactory.getDbmsFeatures().getJpaParameters());

        for (String fileName : sourceFileNames) {
            Document doc = getDocument(fileName);
            Element puElem = findPersistenceUnitElement(doc.getRootElement());
            if (puElem == null)
                throw new IllegalStateException("No persistence unit named 'cuba' found among multiple units inside " + fileName);
            addClasses(puElem, classes);
            addProperties(puElem, properties);
        }

        File outFile;
        try {
            outFile = new File(outFileName).getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        outFile.getParentFile().mkdirs();

        String disableOrmGenProp = AppContext.getProperty("cuba.disableOrmXmlGeneration");
        if (!Boolean.parseBoolean(disableOrmGenProp)) {
            MappingFileCreator mappingFileCreator = new MappingFileCreator(classes.values(), properties, outFile.getParentFile());
            mappingFileCreator.create();
        }

        String fileName = sourceFileNames.get(sourceFileNames.size() - 1);
        Document doc = getDocument(fileName);
        Element rootElem = doc.getRootElement();

        Element puElem = findPersistenceUnitElement(rootElem);
        if (puElem == null)
            throw new IllegalStateException("No persistence unit named 'cuba' found among multiple units inside " + fileName);

        for (Element element : new ArrayList<Element>(Dom4j.elements(puElem, "class"))) {
            puElem.remove(element);
        }

        puElem.addElement("provider").setText("org.apache.openjpa.persistence.PersistenceProviderImpl");

        for (String className : classes.values()) {
            puElem.addElement("class").setText(className);
        }

        Element propertiesEl = puElem.element("properties");
        if (propertiesEl != null)
            puElem.remove(propertiesEl);

        propertiesEl = puElem.addElement("properties");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Element element = propertiesEl.addElement("property");
            element.addAttribute("name", entry.getKey());
            element.addAttribute("value", entry.getValue());
        }

        log.info("Creating file " + outFile);
        OutputStream os = null;
        try {
            os = new FileOutputStream(outFileName);
            Dom4j.writeDocument(doc, true, os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private void addClasses(Element puElem, Map<String, String> classes) {
        for (Element element : Dom4j.elements(puElem, "class")) {
            String className = element.getText();
            Class<Object> cls = ReflectionHelper.getClass(className);
            Entity annotation = cls.getAnnotation(Entity.class);
            if (annotation != null) {
                classes.put(annotation.name(), className);
            } else {
                classes.put(className, className);
            }
        }
    }

    private void addProperties(Element puElem, Map<String, String> properties) {
        Element propertiesEl = puElem.element("properties");
        if (propertiesEl != null) {
            for (Element element : Dom4j.elements(propertiesEl, "property")) {
                properties.put(element.attributeValue("name"), element.attributeValue("value"));
            }
        }
    }

    private Element findPersistenceUnitElement(Element rootElem) {
        List<Element> puList = Dom4j.elements(rootElem, "persistence-unit");
        if (puList.size() == 1) {
            return puList.get(0);
        } else {
            for (Element element : puList) {
                if ("cuba".equals(element.attributeValue("name"))) {
                    return element;
                }
            }
        }
        return null;
    }

    private Document getDocument(String fileName) {
        Document doc;
        if (baseDir == null) {
            Resource resource = new ConfigurationResourceLoader().getResource(fileName);
            InputStream stream = null;
            try {
                stream = resource.getInputStream();
                doc = Dom4j.readDocument(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        } else {
            if (!fileName.startsWith("/"))
                fileName = "/" + fileName;
            File file = new File(baseDir, fileName);
            if (!file.exists())
                throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());

            doc = Dom4j.readDocument(file);
        }
        return doc;
    }
}
