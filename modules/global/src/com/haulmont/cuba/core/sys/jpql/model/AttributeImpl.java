package com.haulmont.cuba.core.sys.jpql.model;

/**
 * User: Alex Chevelev
 * Date: 13.10.2010
 * Time: 23:12:41
 */
public class AttributeImpl implements Attribute {
    private final Class simpleType;
    private final String name;
    private final String referencedEntityName;
    private boolean collection;
    private String userFriendlyName;

    public AttributeImpl(Class simpleType, String name) {
        this.simpleType = simpleType;
        this.name = name;
        referencedEntityName = null;
    }

    public AttributeImpl(String referencedEntityName, String name, boolean isCollection) {
        collection = isCollection;
        this.simpleType = null;
        this.name = name;
        this.referencedEntityName = referencedEntityName;
    }

    public Class getSimpleType() {
        if (simpleType == null)
            throw new IllegalStateException("Not a simpletype attribute");

        return simpleType;
    }

    public String getName() {
        return name;
    }


    public boolean isEntityReferenceAttribute() {
        return referencedEntityName != null;
    }

    public boolean isCollection() {
        return collection;
    }

    public String getReferencedEntityName() {
        if (referencedEntityName == null)
            throw new IllegalStateException("Not a referenced entity attribute");

        return referencedEntityName;
    }

    public String getUserFriendlyName() {
        return userFriendlyName;
    }

    public void setUserFriendlyName(String userFriendlyName) {
        this.userFriendlyName = userFriendlyName;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
