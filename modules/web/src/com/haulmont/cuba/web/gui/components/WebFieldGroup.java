/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 23.06.2010 11:46:30
 *
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Formatter;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.web.gui.AbstractFieldFactory;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.gui.data.DsManager;
import com.haulmont.cuba.web.gui.data.ItemWrapper;
import com.haulmont.cuba.web.gui.data.PropertyWrapper;
import com.haulmont.cuba.web.toolkit.ui.CheckBox;
import com.haulmont.cuba.web.toolkit.ui.CustomField;
import com.haulmont.cuba.web.toolkit.ui.FieldGroup;
import com.haulmont.cuba.web.toolkit.ui.FieldGroupLayout;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Button;
import com.vaadin.ui.DateField;
import com.vaadin.ui.TextField;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class WebFieldGroup extends WebAbstractComponent<FieldGroup> implements com.haulmont.cuba.gui.components.FieldGroup {

    private static final long serialVersionUID = 768889467060419241L;
    private static final String BORDER_STYLE_NAME = "edit-area";

    private Map<String, Field> fields = new LinkedHashMap<String, Field>();
    private Map<Field, Integer> fieldsColumn = new HashMap<Field, Integer>();
    private Map<Integer, List<Field>> columnFields = new HashMap<Integer, List<Field>>();

    private Set<Field> readOnlyFields = new HashSet<Field>();

    private Map<Field, List<com.haulmont.cuba.gui.components.Field.Validator>> fieldValidators =
            new HashMap<Field, List<com.haulmont.cuba.gui.components.Field.Validator>>();

    private Datasource<Entity> datasource;

    private String caption;
    private String description;

    private int cols = 1;

    private List<ExpandListener> expandListeners = null;
    private List<CollapseListener> collapseListeners = null;

    private final FieldFactory fieldFactory = new FieldFactory();

    private Item itemWrapper;

    private DsManager dsManager;

    private Security security = AppContext.getBean(Security.NAME);

    public WebFieldGroup() {
        component = new FieldGroup(fieldFactory) {
            @Override
            public void addField(Object propertyId, com.vaadin.ui.Field field) {
                Field fieldConf = WebFieldGroup.this.getField(propertyId.toString());
                if (fieldConf != null) {
                    int col = fieldsColumn.get(fieldConf);
                    List<Field> colFields = columnFields.get(col);
                    super.addField(propertyId.toString(), field, col, colFields.indexOf(fieldConf));
                } else {
                    super.addField(propertyId.toString(), field, 0);
                }
            }

            @Override
            public void addCustomField(Object propertyId, CustomFieldGenerator fieldGenerator) {
                Field fieldConf = WebFieldGroup.this.getField(propertyId.toString());
                int col = fieldsColumn.get(fieldConf);
                List<Field> colFields = columnFields.get(col);
                super.addCustomField(propertyId, fieldGenerator, col, colFields.indexOf(fieldConf));
            }
        };
        component.setLayout(new FieldGroupLayout());
        component.addListener(new ExpandCollapseListener());
    }

    @Override
    public void setDebugId(String id) {
        super.setDebugId(id);
        final List<Field> fieldConfs = getFields();
        for (final Field fieldConf : fieldConfs) {
            com.vaadin.ui.Field field = component.getField(fieldConf.getId());
            if (field != null) {
                field.setDebugId(id + ":" + fieldConf.getId());
            }
        }
    }

    public List<Field> getFields() {
        return new ArrayList<Field>(fields.values());
    }

    public Field getField(String id) {
        for (final Map.Entry<String, Field> entry : fields.entrySet()) {
            if (entry.getKey().equals(id)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void addField(Field field) {
        fields.put(field.getId(), field);
        fieldsColumn.put(field, 0);
        fillColumnFields(0, field);
    }

    public void addField(Field field, int col) {
        if (col < 0 || col >= cols) {
            throw new IllegalStateException(String.format("Illegal column number %s, available amount of columns is %s",
                    col, cols));
        }
        fields.put(field.getId(), field);
        fieldsColumn.put(field, col);
        fillColumnFields(col, field);
    }

    private void fillColumnFields(int col, Field field) {
        List<Field> fields = columnFields.get(col);
        if (fields == null) {
            fields = new ArrayList<Field>();

            columnFields.put(col, fields);
        }
        fields.add(field);
    }

    public void removeField(Field field) {
        if (fields.remove(field.getId()) != null) {
            Integer col = fieldsColumn.get(field.getId());

            final List<Field> fields = columnFields.get(col);
            fields.remove(field);
            fieldsColumn.remove(field.getId());
        }
    }

    public float getColumnExpandRatio(int col) {
        return component.getColumnExpandRatio(col);
    }

    public void setColumnExpandRatio(int col, float ratio) {
        component.setColumnExpandRatio(col, ratio);
    }

    public void setCaptionAlignment(FieldCaptionAlignment captionAlignment) {
        FieldGroupLayout layout = component.getLayout();
        layout.setCaptionAlignment(WebComponentsHelper.convertFieldGroupCaptionAlignment(captionAlignment));
    }

    public void addCustomField(String fieldId, CustomFieldGenerator fieldGenerator) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        addCustomField(field, fieldGenerator);
    }

    public void addCustomField(final Field field, final CustomFieldGenerator fieldGenerator) {
        if (!field.isCustom()) {
            throw new IllegalStateException(String.format("Field '%s' must by custom", field.getId()));
        }
        component.addCustomField(field.getId(), new FieldGroup.CustomFieldGenerator() {
            public com.vaadin.ui.Field generateField(Item item, Object propertyId, FieldGroup component) {
                Datasource ds;
                if (field.getDatasource() != null) {
                    ds = field.getDatasource();
                } else {
                    ds = datasource;
                }

                Component c;
                com.vaadin.ui.Field f;

                String id = (String) propertyId;

                MetaPropertyPath propertyPath = ds.getMetaClass().getPropertyPath(id);
                if (propertyPath != null) {
                    c = fieldGenerator.generateField(ds, propertyId);
                    assignTypicalAttributes(c);
                    f = (com.vaadin.ui.Field) WebComponentsHelper.getComposition(c);

                    if (f.getPropertyDataSource() == null) {
                        if (field.getDatasource() != null) {
                            final ItemWrapper dsWrapper = createDatasourceWrapper(ds,
                                    Collections.<MetaPropertyPath>singleton(propertyPath), dsManager);
                            f.setPropertyDataSource(dsWrapper.getItemProperty(propertyPath));
                        } else {
                            f.setPropertyDataSource(itemWrapper.getItemProperty(propertyPath));
                        }
                    }
                } else {
                    c = fieldGenerator.generateField(null, null);
                    assignTypicalAttributes(c);
                    f = (com.vaadin.ui.Field) WebComponentsHelper.getComposition(c);
                }

                if (f.getCaption() == null) {
                    if (field.getCaption() != null) {
                        f.setCaption(field.getCaption());
                    } else if (propertyPath != null) {
                        f.setCaption(MessageUtils.getPropertyCaption(propertyPath.getMetaClass(),
                                id));
                    }
                }

                if (f.getDescription() == null && field.getDescription() != null) {
                    f.setDescription(field.getDescription());
                }

                // some components (e.g. LookupPickerField) have width from the creation, so I commented out this check
                if (/*f.getWidth() == -1f &&*/ field.getWidth() != null) {
                    f.setWidth(field.getWidth());
                }

                applyPermissions(c);

                return f;
            }
        });
    }

    private void assignTypicalAttributes(Component c) {
        if (c instanceof BelongToFrame) {
            BelongToFrame belongToFrame = (BelongToFrame) c;
            if (belongToFrame.getFrame() == null) {
                belongToFrame.setFrame(getFrame());
            }
        }
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
        this.dsManager = new DsManager(datasource, this);

        component.setCols(cols);

        Collection<MetaPropertyPath> fieldsMetaProps = null;
        if (this.fields.isEmpty() && datasource != null) {//collects fields by entity view
            fieldsMetaProps = MetadataHelper.getViewPropertyPaths(datasource.getView(), datasource.getMetaClass());

            final ArrayList<MetaPropertyPath> propertyPaths = new ArrayList<MetaPropertyPath>(fieldsMetaProps);
            for (final MetaPropertyPath propertyPath : propertyPaths) {
                MetaProperty property = propertyPath.getMetaProperty();
                if (property.getRange().getCardinality().isMany() || MetadataHelper.isSystem(property)) {
                    fieldsMetaProps.remove(propertyPath);
                }
            }

            component.setRows(fieldsMetaProps.size());

        } else {
            if (datasource != null) {
                final List<String> fieldIds = new ArrayList<String>(this.fields.keySet());
                fieldsMetaProps = new ArrayList<MetaPropertyPath>();
                for (final String id : fieldIds) {
                    final Field field = getField(id);
                    final MetaPropertyPath propertyPath = datasource.getMetaClass().getPropertyPath(field.getId());
                    final Element descriptor = field.getXmlDescriptor();
                    final String clickAction = (descriptor==null)?(null):(descriptor.attributeValue("clickAction"));
                    if (field.getDatasource() == null && propertyPath != null
                            && StringUtils.isEmpty(clickAction)) {
                        //fieldsMetaProps with attribute "clickAction" will be created manually
                        fieldsMetaProps.add(propertyPath);
                    }
                }
            }

            component.setRows(rowsCount());
        }

        if (datasource != null) {
            itemWrapper = createDatasourceWrapper(datasource, fieldsMetaProps, dsManager);

            if (!this.fields.isEmpty()) {
                //Removes custom fieldsMetaProps from the list. We shouldn't to create components for custom fieldsMetaProps
                for (MetaPropertyPath propertyPath : new ArrayList<MetaPropertyPath>(fieldsMetaProps)) {
                    final Field field = getField(propertyPath.toString());
                    if (field.isCustom()) {
                        fieldsMetaProps.remove(propertyPath);
                    }
                }
            }

            component.setItemDataSource(itemWrapper, fieldsMetaProps);
        } else {
            component.setItemDataSource(null, null);
        }

        createFields(datasource);
    }

    private void createFields(Datasource datasource) {
        for (final String id : this.fields.keySet()) {
            final Field fieldConf = getField(id);
            if (!fieldConf.isCustom()) {
                com.vaadin.ui.Field field;
                Element descriptor = fieldConf.getXmlDescriptor();
                final String clickAction = (descriptor==null)?(null):(descriptor.attributeValue("clickAction"));
                Datasource fieldDs;
                if (datasource != null && fieldConf.getDatasource() == null) {
                    if (!StringUtils.isEmpty(clickAction)) {
                        field = createField(datasource, fieldConf);
                        component.addField(fieldConf.getId(), field);
                    } else {
                        field = component.getField(id);
                    }
                    fieldDs = datasource;
                } else if (fieldConf.getDatasource() != null) {
                    if (!StringUtils.isEmpty(clickAction)) {
                        field = createField(fieldConf.getDatasource(), fieldConf);
                        component.addField(fieldConf.getId(), field);
                    } else {
                        MetaPropertyPath propertyPath = fieldConf.getDatasource().getMetaClass().getPropertyPath(fieldConf.getId());
                        final ItemWrapper dsWrapper = createDatasourceWrapper(fieldConf.getDatasource(),
                                Collections.<MetaPropertyPath>singleton(propertyPath), dsManager);

                        field = fieldFactory.createField(dsWrapper, propertyPath, component);

                        if (field != null && dsWrapper.getItemProperty(propertyPath) != null) {
                            field.setPropertyDataSource(dsWrapper.getItemProperty(propertyPath));
                            component.addField(fieldConf.getId(), field);
                        }
                    }
                    fieldDs = fieldConf.getDatasource();
                } else {
                    throw new IllegalStateException(String.format("Unable to get datasource for field '%s'", id));
                }

                if (field != null) {
                    if (fieldConf.getCaption() != null) {
                        field.setCaption(fieldConf.getCaption());
                    }
                    if (fieldConf.getDescription() != null) {
                        field.setDescription(fieldConf.getDescription());
                    }
                }

                applyPermissions(fieldConf, fieldDs);
            }
        }
    }

    private void applyPermissions(Component c) {
        if (c instanceof DatasourceComponent) {
            DatasourceComponent dsComponent = (DatasourceComponent) c;
            MetaProperty metaProperty = dsComponent.getMetaProperty();

            if (metaProperty != null) {
                dsComponent.setEditable(security.isEntityAttrModificationPermitted(getDatasource().getMetaClass(), metaProperty)
                        && dsComponent.isEditable());
            }
        }
    }

    private void applyPermissions(Field fieldConf, Datasource datasource) {
        MetaPropertyPath propertyPath = datasource.getMetaClass().getPropertyPath(fieldConf.getId());
        if (propertyPath != null) {
            MetaProperty metaProperty = propertyPath.getMetaProperty();

            setEditable(fieldConf, security.isEntityAttrModificationPermitted(getDatasource().getMetaClass(), metaProperty)
                    && isEditable(fieldConf));
        }
    }

    private int rowsCount() {
        int rowsCount = 0;
        for (final List<Field> fields : columnFields.values()) {
            rowsCount = Math.max(rowsCount, fields.size());
        }
        return rowsCount;
    }

    public int getColumns() {
        return cols;
    }

    public void setColumns(int cols) {
        this.cols = cols;
    }

    protected Object convertRawValue(Field field, Object value) throws ValidationException {
        if (value instanceof String) {
            Datatype datatype = null;
            MetaPropertyPath propertyPath = null;

            if (field.getDatasource() != null) {
                propertyPath = datasource.getMetaClass().getPropertyPath(field.getId());
            } else if (datasource != null) {
                propertyPath = datasource.getMetaClass().getPropertyPath(field.getId());
            }

            if (propertyPath != null) {
                if (propertyPath.getMetaProperty().getRange().isDatatype())
                    datatype = propertyPath.getRange().asDatatype();
            }

            if (datatype != null) {
                try {
                    return datatype.parse((String) value, UserSessionProvider.getLocale());
                } catch (ParseException ignored) {
                    String message = MessageProvider.getMessage(WebWindow.class, "invalidValue");
                    String fieldCaption = MessageUtils.getPropertyCaption(propertyPath.getMetaProperty());
                    message = String.format(message, fieldCaption);
                    throw new ValidationException(message);
                }
            }
        }
        return value;
    }

    public void addValidator(final Field field, final com.haulmont.cuba.gui.components.Field.Validator validator) {
        List<com.haulmont.cuba.gui.components.Field.Validator> validators = fieldValidators.get(field);
        if (validators == null) {
            validators = new ArrayList<com.haulmont.cuba.gui.components.Field.Validator>();
            fieldValidators.put(field, validators);
        }
        if (!validators.contains(validator))
            validators.add(validator);
    }

    public void addValidator(String fieldId, com.haulmont.cuba.gui.components.Field.Validator validator) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        addValidator(field, validator);
    }

    public boolean isExpanded() {
        return component.isExpanded();
    }

    public void setExpanded(boolean expanded) {
        component.setExpanded(expanded);
    }

    public boolean isCollapsible() {
        return component.isCollapsable();
    }

    public void setCollapsible(boolean collapsable) {
        component.setCollapsable(collapsable);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
        component.setCaption(caption);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        component.setDescription(description);
    }

    public boolean isEditable() {
        return !component.isReadOnly();
    }

    public void setEditable(boolean editable) {
        component.setReadOnly(!editable);
        // if we have editable field group with some read-only fields then we keep them read-only
        if (editable) {
            for (Field field: readOnlyFields) {
                com.vaadin.ui.Field f = component.getField(field.getId());
                f.setReadOnly(true);
            }
        }
    }

    public boolean isRequired(Field field) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        return f.isRequired();
    }

    public void setRequired(Field field, boolean required, String message) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        f.setRequired(required);
        if (required) {
            f.setRequiredError(message);
        }
    }

    public boolean isRequired(String fieldId) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        return isRequired(field);
    }

    public void setRequired(String fieldId, boolean required, String message) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        setRequired(field, required, message);
    }

    public boolean isEditable(Field field) {
        return !readOnlyFields.contains(field);
    }

    public void setEditable(Field field, boolean editable) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        f.setReadOnly(!editable);
        if (editable) {
            readOnlyFields.remove(field);
        } else {
            readOnlyFields.add(field);
        }
    }

    public void setEditable(String fieldId, boolean editable) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        setEditable(field, editable);
    }

    public boolean isEditable(String fieldId) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        return isEditable(field);
    }

    public boolean isEnabled(Field field) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        return f.isEnabled();
    }

    public void setEnabled(Field field, boolean enabled) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        f.setEnabled(enabled);
    }

    public boolean isEnabled(String fieldId) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        return isEnabled(field);
    }

    public void setEnabled(String fieldId, boolean enabled) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        setEnabled(field, enabled);
    }

    public boolean isVisible(Field field) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        return f.isVisible();
    }

    public void setVisible(Field field, boolean visible) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        f.setVisible(visible);
    }

    public boolean isVisible(String fieldId) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        return isVisible(field);
    }

    public void setVisible(String fieldId, boolean visible) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        setVisible(field, visible);
    }

    @Override
    public boolean isBorderVisible() {
        String styleName = getStyleName();
        if (StringUtils.isNotEmpty(styleName))
            return styleName.contains(BORDER_STYLE_NAME);
        return false;
    }

    @Override
    public void setBorderVisible(boolean borderVisible) {
        String styleName = getStyleName();
        if (borderVisible) {
            if (StringUtils.isNotEmpty(styleName)) {
                if (!styleName.contains(BORDER_STYLE_NAME))
                    styleName = styleName + " " + BORDER_STYLE_NAME;
            } else
                styleName = BORDER_STYLE_NAME;
        } else {
            if (StringUtils.isNotEmpty(styleName))
                styleName = styleName.replace(BORDER_STYLE_NAME, "");
        }
        setStyleName(styleName);
    }

    public Object getFieldValue(Field field) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        return f.getValue();
    }

    public void setFieldValue(Field field, Object value) {
        com.vaadin.ui.Field f = component.getField(field.getId());
        f.setValue(value);
    }

    public Object getFieldValue(String fieldId) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        return getFieldValue(field);
    }

    public void setFieldValue(String fieldId, Object value) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        setFieldValue(field, value);
    }

    @Override
    public void setFieldCaption(String fieldId, String caption) {
        Field field = getField(fieldId);
        if (field == null)
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));

        com.vaadin.ui.Field f = component.getField(field.getId());
        f.setCaption(caption);
    }

    protected ItemWrapper createDatasourceWrapper(Datasource datasource, Collection<MetaPropertyPath> propertyPaths, DsManager dsManager) {
        return new FieldGroupItemWrapper(datasource, propertyPaths, dsManager);
    }

    public void addListener(ExpandListener listener) {
        if (expandListeners == null) {
            expandListeners = new ArrayList<ExpandListener>();
        }
        expandListeners.add(listener);
    }

    public void removeListener(ExpandListener listener) {
        if (expandListeners != null) {
            expandListeners.remove(listener);
            if (expandListeners.isEmpty()) {
                expandListeners = null;
            }
        }
    }

    protected void fireExpandListeners() {
        if (expandListeners != null) {
            for (final ExpandListener listener : expandListeners) {
                listener.onExpand(this);
            }
        }
    }

    public void addListener(CollapseListener listener) {
        if (collapseListeners == null) {
            collapseListeners = new ArrayList<CollapseListener>();
        }
        collapseListeners.add(listener);
    }

    public void removeListener(CollapseListener listener) {
        if (collapseListeners != null) {
            collapseListeners.remove(listener);
            if (collapseListeners.isEmpty()) {
                collapseListeners = null;
            }
        }
    }

    public void postInit() {
    }

    protected void fireCollapseListeners() {
        if (collapseListeners != null) {
            for (final CollapseListener listener : collapseListeners) {
                listener.onCollapse(this);
            }
        }
    }

    public void applySettings(Element element) {
        Element fieldGroupElement = element.element("fieldGroup");
        if (fieldGroupElement != null) {
            String expanded = fieldGroupElement.attributeValue("expanded");
            if (expanded != null) {
                setExpanded(BooleanUtils.toBoolean(expanded));
            }
        }
    }

    public boolean saveSettings(Element element) {
        Element fieldGroupElement = element.element("fieldGroup");
        if (fieldGroupElement != null) {
            element.remove(fieldGroupElement);
        }
        fieldGroupElement = element.addElement("fieldGroup");
        fieldGroupElement.addAttribute("expanded", BooleanUtils.toStringTrueFalse(isExpanded()));
        return true;
    }

    @Override
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    @Override
    public void validate() throws ValidationException {
        final Map<Object, Exception> problems = new HashMap<Object, Exception>();

        for (Field field : getFields()) {
            com.vaadin.ui.Field f = component.getField(field.getId());
            if (f != null && f.isVisible() && f.isEnabled() && !f.isReadOnly()) {
                Object value = convertRawValue(field, getFieldValue(field));
                if (isEmpty(value)) {
                    if (isRequired(field))
                        problems.put(field.getId(), new RequiredValueMissingException(f.getRequiredError(), this));
                } else {
                    List<com.haulmont.cuba.gui.components.Field.Validator> validators = fieldValidators.get(field);
                    if (validators != null) {
                        for (com.haulmont.cuba.gui.components.Field.Validator validator : validators) {
                            try {
                                validator.validate(value);
                            } catch (ValidationException e) {
                                problems.put(field.getId(), e);
                            }
                        }
                    }
                }
            }
        }

        if (!problems.isEmpty()) {
            Map<Field, Exception> problemFields = new HashMap<Field, Exception>();
            for (Map.Entry<Object, Exception> entry : problems.entrySet()) {
                problemFields.put(getField(entry.getKey().toString()), entry.getValue());
            }

            StringBuilder msgBuilder = new StringBuilder();
            for (Iterator<Field> iterator = problemFields.keySet().iterator(); iterator.hasNext(); ) {
                Field field = iterator.next();
                Exception ex = problemFields.get(field);
                msgBuilder.append(ex.getMessage());
                if (iterator.hasNext())
                    msgBuilder.append("<br>");
            }

            FieldsValidationException validationException = new FieldsValidationException(msgBuilder.toString());
            validationException.setProblemFields(problemFields);

            throw validationException;
        }
    }

    private boolean isEmpty(Object value) {
        if (value instanceof String)
            return StringUtils.isBlank((String) value);
        else
            return value == null;
    }

    protected class ExpandCollapseListener implements FieldGroup.ExpandCollapseListener {
        private static final long serialVersionUID = 4917475472402160597L;

        public void onExpand(FieldGroup component) {
            fireExpandListeners();
        }

        public void onCollapse(FieldGroup component) {
            fireCollapseListeners();
        }
    }

    protected class FieldFactory extends AbstractFieldFactory {
        @Override
        protected Datasource getDatasource() {
            return datasource;
        }

        @Override
        protected void initCommon(com.vaadin.ui.Field field, com.haulmont.cuba.gui.components.Field cubaField, MetaPropertyPath propertyPath) {
            final Field fieldConf = getField(propertyPath.toString());
            if ("timeField".equals(fieldType(propertyPath))||(cubaField instanceof WebTimeField)) {
                String s = fieldConf.getXmlDescriptor().attributeValue("showSeconds");
                if (Boolean.valueOf(s)) {
                    ((TimeField) cubaField).setShowSeconds(true);
                }
            } else if (field instanceof TextField) {
                ((TextField) field).setNullRepresentation("");
                if (fieldConf != null) {
                    initTextField((TextField) field, propertyPath.getMetaProperty(), fieldConf.getXmlDescriptor());
                }
            } else if (cubaField instanceof WebDateField) {
                if (getFormatter(propertyPath) != null) {
                    String format = getFormat(propertyPath);
                    if (format != null) {
                        ((WebDateField) cubaField).setDateFormat(format);
                    }
                }
                if (fieldConf != null) {
                    initDateField(field, propertyPath.getMetaProperty(), fieldConf.getXmlDescriptor());
                }
            } else if (field instanceof CheckBox) {
                ((CheckBox) field).setLayoutCaption(true);
            }

            if (fieldConf != null && fieldConf.getWidth() != null) {
                field.setWidth(fieldConf.getWidth());
            }

            if (cubaField != null)
                cubaField.setFrame(getFrame());
        }

        @Override
        protected CollectionDatasource getOptionsDatasource(MetaClass metaClass, MetaPropertyPath propertyPath) {
            final Field field = fields.get(propertyPath.toString());

            Datasource ds = datasource;

            DsContext dsContext;
            if (ds == null) {
                ds = field.getDatasource();
                if (ds == null) {
                    throw new IllegalStateException("FieldGroup datasource is null");
                }
                dsContext = ds.getDsContext();
            } else {
                dsContext = ds.getDsContext();
            }
            Element descriptor = field.getXmlDescriptor();
            String optDsName = descriptor == null ? null : descriptor.attributeValue("optionsDatasource");

            if (!StringUtils.isBlank(optDsName)) {
                CollectionDatasource optDs = dsContext.get(optDsName);
                if (optDs == null) {
                    throw new IllegalStateException("Options datasource not found: " + optDsName);
                }
                return optDs;
            } else {
                return null;
            }
        }

        @Override
        protected void initValidators(com.vaadin.ui.Field field, com.haulmont.cuba.gui.components.Field cubaField, MetaPropertyPath propertyPath, boolean validationVisible) {
            //do nothing
        }

        @Override
        protected Collection<com.haulmont.cuba.gui.components.Field.Validator> getValidators(MetaPropertyPath propertyPath) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean required(MetaPropertyPath propertyPath) {
            return false;
        }

        @Override
        protected String requiredMessage(MetaPropertyPath propertyPath) {
            return null;
        }

        @Override
        protected Formatter getFormatter(MetaPropertyPath propertyPath) {
            Field field = fields.get(propertyPath.toString());
            if (field != null) {
                return field.getFormatter();
            } else {
                return null;
            }
        }

        @Override
        protected String getFormat(MetaPropertyPath propertyPath) {
            Field field = fields.get(propertyPath.toString());
            if (field != null) {
                Element formatterElement = field.getXmlDescriptor().element("formatter");
                return formatterElement.attributeValue("format");
            }
            return null;
        }

        @Override
        protected String fieldType(MetaPropertyPath propertyPath) {
            Field field = fields.get(propertyPath.toString());
            if (field != null) {
                if (field.getXmlDescriptor() != null) {
                    String fieldType = field.getXmlDescriptor().attributeValue("field");
                    if (!StringUtils.isEmpty(fieldType)) {
                        return fieldType;
                    }
                }
            }
            return null;
        }

        @Override
        protected Element getXmlDescriptor(MetaPropertyPath propertyPath) {
            Field field = fields.get(propertyPath.toString());
            return field != null ? field.getXmlDescriptor() : null;
        }

        @Override
        protected void setCaption(com.vaadin.ui.Field field, MetaPropertyPath propertyPath) {
            // if caption not already loaded from attributes then load default caption
            Field fieldConf = WebFieldGroup.this.fields.get(propertyPath.toString());
            if ((fieldConf == null) || (StringUtils.isEmpty(fieldConf.getCaption())))
                super.setCaption(field, propertyPath);
        }
    }

    private com.vaadin.ui.Field createField(final Datasource datasource, final Field fieldConf) {
        MetaPropertyPath propertyPath = datasource.getMetaClass().getPropertyPath(fieldConf.getId());
        final ItemWrapper dsWrapper = createDatasourceWrapper(
                datasource,
                Collections.<MetaPropertyPath>singleton(propertyPath),
                dsManager);

        final LinkField field = new LinkField(datasource, fieldConf);
        field.setCaption(MessageUtils.getPropertyCaption(propertyPath.getMetaProperty()));
        field.setPropertyDataSource(dsWrapper.getItemProperty(propertyPath));

        return field;
    }

    private class LinkField extends CustomField {
        private static final long serialVersionUID = 5555318337278242796L;

        private Button component;

        private LinkField(final Datasource datasource, final Field fieldConf) {
            component = new Button();
            component.setStyleName("link");
            component.addListener(new Button.ClickListener() {
                public void buttonClick(Button.ClickEvent event) {
                    final Instance entity = datasource.getItem();
                    final Entity value = entity.getValueEx(fieldConf.getId());
                    String clickAction = fieldConf.getXmlDescriptor().attributeValue("clickAction");
                    if (!StringUtils.isEmpty(clickAction)) {
                        if (clickAction.startsWith("open:")) {
                            final com.haulmont.cuba.gui.components.IFrame frame = WebFieldGroup.this.getFrame();
                            String screenName = clickAction.substring("open:".length()).trim();
                            final com.haulmont.cuba.gui.components.Window window =
                                    frame.openEditor(
                                            screenName,
                                            value,
                                            WindowManager.OpenType.THIS_TAB);

                            window.addListener(new com.haulmont.cuba.gui.components.Window.CloseListener() {
                                public void windowClosed(String actionId) {
                                    if (com.haulmont.cuba.gui.components.Window.COMMIT_ACTION_ID.equals(actionId)
                                            && window instanceof com.haulmont.cuba.gui.components.Window.Editor) {
                                        Object item = ((com.haulmont.cuba.gui.components.Window.Editor) window).getItem();
                                        if (item instanceof Entity) {
                                            entity.setValueEx(fieldConf.getId(), item);
                                        }
                                    }
                                }
                            });
                        } else if (clickAction.startsWith("invoke:")) {
                            final com.haulmont.cuba.gui.components.IFrame frame = WebFieldGroup.this.getFrame();
                            String methodName = clickAction.substring("invoke:".length()).trim();
                            try {
                                IFrame controllerFrame = WebComponentsHelper.getControllerFrame(frame);
                                Method method = controllerFrame.getClass().getMethod(methodName, Object.class);
                                method.invoke(controllerFrame, value);
                            } catch (NoSuchMethodException e) {
                                throw new RuntimeException("Unable to invoke clickAction", e);
                            } catch (InvocationTargetException e) {
                                throw new RuntimeException("Unable to invoke clickAction", e);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Unable to invoke clickAction", e);
                            }

                        } else {
                            throw new UnsupportedOperationException(String.format("Unsupported clickAction format: %s", clickAction));
                        }
                    }
                }
            });
            setStyleName("linkfield");
            setCompositionRoot(component);
        }

        @Override
        public Class<?> getType() {
            return String.class;
        }

        @Override
        public void valueChange(Property.ValueChangeEvent event) {
            super.valueChange(event);
            component.setCaption(event.getProperty().getValue() == null
                    ? "" : event.getProperty().toString());
        }
    }

    public class FieldGroupItemWrapper extends ItemWrapper {

        private static final long serialVersionUID = -7877886198903628220L;

        public FieldGroupItemWrapper(Datasource datasource, Collection<MetaPropertyPath> propertyPaths, DsManager dsManager) {
            super(datasource, propertyPaths, dsManager);
        }

        public Datasource getDatasource() {
            return (Datasource) item;
        }

        @Override
        protected PropertyWrapper createPropertyWrapper(Object item, MetaPropertyPath propertyPath, DsManager dsManager) {
            return new PropertyWrapper(item, propertyPath, dsManager) {
                @Override
                public boolean isReadOnly() {
                    Field field = fields.get(propertyPath.toString());
                    return !isEditable(field);
                }

                @Override
                public String toString() {
                    Object value = getValue();
                    if (value == null) return null;
                    Field field = fields.get(propertyPath.toString());
                    if (field.getFormatter() != null) {
                        if (value instanceof Instance) {
                            value = ((Instance) value).getInstanceName();
                        }
                        return field.getFormatter().format(value);
                    }
                    return super.toString();
                }
            };
        }
    }
}