/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys.persistence;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.entity.annotation.Extends;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Generates an orm.xml file containing mapping overrides to support extended entities in associations.
 * Works together with {@link PersistenceConfigProcessor}.
 *
 * @author krivopustov
 * @version $Id$
*/
class MappingFileCreator {

    private static final String XMLNS = "http://java.sun.com/xml/ns/persistence/orm";
    private static final String PERSISTENCE_VER = "1.0";

    private Collection<String> classNames;
    private Map<String, String> properties;
    private File dir;

    private Log log = LogFactory.getLog(getClass());

    MappingFileCreator(Collection<String> classNames, Map<String, String> properties, File dir) {
        this.classNames = classNames;
        this.properties = properties;
        this.dir = dir;
    }

    public void create() {
        Map<Class, Class> extendedClasses = new HashMap<>();
        List<Class> persistentClasses = new ArrayList<>();
        for (String className : classNames) {
            Class<Object> aClass = ReflectionHelper.getClass(className);
            persistentClasses.add(aClass);
            Extends annotation = aClass.getAnnotation(Extends.class);
            if (annotation != null) {
                Class originalClass = annotation.value();
                extendedClasses.put(originalClass, aClass);
            }
        }

        if (extendedClasses.isEmpty())
            return;

        Map<Class<?>, List<Attr>> mappings = new LinkedHashMap<>();
        for (Class aClass : persistentClasses) {
            List<Attr> attrList = processClass(aClass, extendedClasses);
            if (!attrList.isEmpty())
                mappings.put(aClass, attrList);
        }

        if (mappings.isEmpty())
            return;
        log.debug("Found " + mappings.size() + " entities containing extended associations");

        Document doc = createDocument(mappings);
        File file = writeDocument(doc);

        String filePath = file.getAbsolutePath().replace("\\", "/");
        if (!filePath.startsWith("/"))
            filePath = "/" + filePath;

        String prop = properties.get("openjpa.MetaDataFactory");
        if (prop != null)
            log.warn("Please don't set openjpa.MetaDataFactory in your persistence.xml, it is overridden anyway");
        properties.put("openjpa.MetaDataFactory", "jpa(URLs=file://" + filePath + ")");
    }

    private List<Attr> processClass(Class aClass, Map<Class, Class> extendedClasses) {
        List<Attr> list = new ArrayList<>();

        for (Field field : aClass.getDeclaredFields()) {
            Attr.Type type = getAttrType(field);
            if (type != null) {
                Class extClass = extendedClasses.get(field.getType());
                if (extClass != null) {
                    Attr attr = new Attr(type, field, extClass.getName());
                    list.add(attr);
                }
            }
        }

        return list;
    }

    private Attr.Type getAttrType(Field field) {
        if (field.getAnnotation(OneToOne.class) != null)
            return Attr.Type.ONE_TO_ONE;
        else if (field.getAnnotation(OneToMany.class) != null)
            return Attr.Type.ONE_TO_MANY;
        else if (field.getAnnotation(ManyToOne.class) != null)
            return Attr.Type.MANY_TO_ONE;
        else if (field.getAnnotation(ManyToMany.class) != null)
            return Attr.Type.MANY_TO_MANY;
        else
            return null;
    }

    private Document createDocument(Map<Class<?>, List<Attr>> mappings) {
        Document doc = DocumentHelper.createDocument();
        Element rootEl = doc.addElement("entity-mappings", XMLNS);
        rootEl.addAttribute("version", PERSISTENCE_VER);

        for (Map.Entry<Class<?>, List<Attr>> entry : mappings.entrySet()) {
            if (entry.getKey().getAnnotation(MappedSuperclass.class) != null) {
                Element entityEl = rootEl.addElement("mapped-superclass", XMLNS);
                entityEl.addAttribute("class", entry.getKey().getName());
                createAttributes(entry, entityEl);
            }
        }
        for (Map.Entry<Class<?>, List<Attr>> entry : mappings.entrySet()) {
            if (entry.getKey().getAnnotation(Entity.class) != null) {
                Element entityEl = rootEl.addElement("entity", XMLNS);
                entityEl.addAttribute("class", entry.getKey().getName());
                entityEl.addAttribute("name", entry.getKey().getAnnotation(Entity.class).name());
                createAttributes(entry, entityEl);
            }
        }

        return doc;
    }

    private void createAttributes(Map.Entry<Class<?>, List<Attr>> entry, Element entityEl) {
        Element attributesEl = entityEl.addElement("attributes", XMLNS);
        Collections.sort(entry.getValue(), new Comparator<Attr>() {
            @Override
            public int compare(Attr a1, Attr a2) {
                return a1.type.order - a2.type.order;
            }
        });
        for (Attr attr : entry.getValue()) {
            attr.toXml(attributesEl);
        }
    }

    private File writeDocument(Document doc) {
        File file = new File(dir, "orm.xml");
        log.info("Creating file " + file);

        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            Dom4j.writeDocument(doc, true, os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(os);
        }
        return file;
    }

    private class ClassDef {

        private Class<?> entityClass;

        private ClassDef(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        @Nullable
        private Element toXml(Element parentEl) {
            Element el;
            if (entityClass.getAnnotation(Entity.class) != null) {
                el = parentEl.addElement("entity", XMLNS);
            } else if (entityClass.getAnnotation(MappedSuperclass.class) != null) {
                el = parentEl.addElement("mapped-superclass", XMLNS);
            } else {
                log.warn(entityClass + " has neither @Entity nor @MappedSuperclass annotation, ignoring it");
                return null;
            }
            el.addAttribute("class", entityClass.getName());
            return el;
        }
    }

    private static class Attr {

        private enum Type {
            MANY_TO_ONE(1, "many-to-one") {
                @Override
                protected String getFetch(Field field) {
                    return field.getAnnotation(ManyToOne.class).fetch().name();
                }
                @Override
                protected String getMappedBy(Field field) {
                    return null;
                }
            },
            ONE_TO_MANY(2, "one-to-many") {
                @Override
                protected String getFetch(Field field) {
                    return field.getAnnotation(OneToMany.class).fetch().name();
                }
                @Override
                protected String getMappedBy(Field field) {
                    return field.getAnnotation(OneToMany.class).mappedBy();
                }
            },
            ONE_TO_ONE(3, "one-to-one") {
                @Override
                protected String getFetch(Field field) {
                    return field.getAnnotation(OneToOne.class).fetch().name();
                }
                @Override
                protected String getMappedBy(Field field) {
                    return field.getAnnotation(OneToOne.class).mappedBy();
                }
            },
            MANY_TO_MANY(4, "many-to-many") {
                @Override
                protected String getFetch(Field field) {
                    return field.getAnnotation(ManyToMany.class).fetch().name();
                }
                @Override
                protected String getMappedBy(Field field) {
                    return field.getAnnotation(ManyToMany.class).mappedBy();
                }
            };

            private int order;
            private String xml;

            private Type(int order, String xml) {
                this.xml = xml;
            }

            protected abstract String getFetch(Field field);
            protected abstract String getMappedBy(Field field);
        }

        private final Type type;
        private final Field field;
        private final String targetEntity;

        private Attr(Type type, Field field, String targetEntity) {
            this.type = type;
            this.field = field;
            this.targetEntity = targetEntity;
        }

        private Element toXml(Element parentEl) {
            Element el = parentEl.addElement(type.xml, XMLNS);
            el.addAttribute("name", field.getName());
            el.addAttribute("target-entity", targetEntity);
            el.addAttribute("fetch", type.getFetch(field));
            String mappedBy = type.getMappedBy(field);
            if (!StringUtils.isEmpty(mappedBy))
                el.addAttribute("mapped-by", mappedBy);

            // either
            new JoinColumnHandler(field.getAnnotation(JoinColumn.class)).toXml(el);
            // or
            new OrderByHandler(field.getAnnotation(OrderBy.class)).toXml(el);
            new JoinTableHandler(field.getAnnotation(JoinTable.class)).toXml(el);

            return el;
        }
    }

    private static class JoinColumnHandler {

        private JoinColumn annotation;

        private JoinColumnHandler(JoinColumn annotation) {
            this.annotation = annotation;
        }

        protected void toXml(Element parentEl) {
            if (annotation == null)
                return;

            Element el = parentEl.addElement(getElementName());
            el.addAttribute("name", annotation.name());

            if (!StringUtils.isEmpty(annotation.referencedColumnName()))
                el.addAttribute("referenced-column-name", annotation.referencedColumnName());

            if (annotation.unique())
                el.addAttribute("unique", "true");

            if (!annotation.nullable())
                el.addAttribute("nullable", "false");

            if (!annotation.insertable())
                el.addAttribute("insertable", "false");

            if (!annotation.updatable())
                el.addAttribute("updatable", "false");
        }

        protected String getElementName() {
            return "join-column";
        }
    }

    private static class InverseJoinColumnHandler extends JoinColumnHandler {

        private InverseJoinColumnHandler(JoinColumn annotation) {
            super(annotation);
        }

        @Override
        protected String getElementName() {
            return "inverse-join-column";
        }
    }

    private static class JoinTableHandler {

        private JoinTable annotation;

        private JoinTableHandler(JoinTable annotation) {
            this.annotation = annotation;
        }

        private void toXml(Element parentEl) {
            if (annotation == null)
                return;

            Element el = parentEl.addElement("join-table");
            el.addAttribute("name", annotation.name());

            for (JoinColumn joinColumnAnnot : annotation.joinColumns()) {
                new JoinColumnHandler(joinColumnAnnot).toXml(el);
            }
            for (JoinColumn joinColumnAnnot : annotation.joinColumns()) {
                new InverseJoinColumnHandler(joinColumnAnnot).toXml(el);
            }
        }
    }

    private static class OrderByHandler {

        private OrderBy annotation;

        private OrderByHandler(OrderBy annotation) {
            this.annotation = annotation;
        }

        private void toXml(Element parentEl) {
            if (annotation == null)
                return;

            Element el = parentEl.addElement("order-by");
            el.setText(annotation.value());
        }
    }
}
