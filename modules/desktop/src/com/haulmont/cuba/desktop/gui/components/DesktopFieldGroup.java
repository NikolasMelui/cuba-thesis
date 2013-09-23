/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.desktop.sys.DesktopToolTipManager;
import com.haulmont.cuba.desktop.sys.layout.LayoutAdapter;
import com.haulmont.cuba.desktop.sys.layout.MigLayoutHelper;
import com.haulmont.cuba.desktop.sys.vcl.CollapsiblePanel;
import com.haulmont.cuba.desktop.sys.vcl.ToolTipButton;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import javax.swing.*;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class DesktopFieldGroup extends DesktopAbstractComponent<JPanel> implements FieldGroup, AutoExpanding {

    protected MigLayout layout;
    protected Datasource datasource;
    protected int rows;
    protected int cols = 1;
    protected boolean editable = true;
    protected boolean enabled = true;
    protected boolean borderVisible = false;

    protected Map<String, FieldConfig> fields = new LinkedHashMap<>();
    protected Map<FieldConfig, Integer> fieldsColumn = new HashMap<>();
    protected Map<FieldConfig, Component> fieldComponents = new HashMap<>();
    protected Map<FieldConfig, JLabel> fieldLabels = new HashMap<>();
    protected Map<FieldConfig, ToolTipButton> fieldTooltips = new HashMap<>();
    protected Map<Integer, List<FieldConfig>> columnFields = new HashMap<>();
    protected Map<FieldConfig, CustomFieldGenerator> generators = new HashMap<>();
    protected AbstractFieldFactory fieldFactory = new FieldFactory();

    protected Set<FieldConfig> readOnlyFields = new HashSet<>();
    protected Set<FieldConfig> disabledFields = new HashSet<>();

    protected CollapsiblePanel collapsiblePanel;

    protected Security security = AppBeans.get(Security.NAME);

    public DesktopFieldGroup() {
        LC lc = new LC();
        lc.hideMode(3); // Invisible components will not participate in the layout at all and it will for instance not take up a grid cell.
        lc.insets("0 0 0 0");
        if (LayoutAdapter.isDebug()) {
            lc.debug(1000);
        }

        layout = new MigLayout(lc);
        impl = new JPanel(layout);
        assignClassDebugProperty(impl);
        collapsiblePanel = new CollapsiblePanel(super.getComposition());
        assignClassDebugProperty(collapsiblePanel);
        collapsiblePanel.setBorderVisible(false);

        setWidth(Component.AUTO_SIZE);
    }

    @Override
    public List<FieldConfig> getFields() {
        return new ArrayList<>(fields.values());
    }

    @Override
    public FieldConfig getField(String id) {
        for (final Map.Entry<String, FieldConfig> entry : fields.entrySet()) {
            if (entry.getKey().equals(id)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void fillColumnFields(int col, FieldConfig field) {
        List<FieldConfig> fields = columnFields.get(col);
        if (fields == null) {
            fields = new ArrayList<>();

            columnFields.put(col, fields);
        }
        fields.add(field);
    }

    @Override
    public void addField(FieldConfig field) {
        if (cols == 0) {
            cols = 1;
        }
        addField(field, 0);
    }

    @Override
    public void addField(FieldConfig field, int col) {
        if (col < 0 || col >= cols) {
            throw new IllegalStateException(String.format("Illegal column number %s, available amount of columns is %s",
                    col, cols));
        }
        fields.put(field.getId(), field);
        fieldsColumn.put(field, col);
        fillColumnFields(col, field);
    }

    @Override
    public void removeField(FieldConfig field) {
        if (fields.remove(field.getId()) != null) {
            Integer col = fieldsColumn.get(field.getId());

            final List<FieldConfig> fields = columnFields.get(col);
            fields.remove(field);
            fieldsColumn.remove(field.getId());
        }
    }

    @Override
    public void requestFocus() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!columnFields.isEmpty()) {
                    List<FieldConfig> fields = columnFields.get(0);
                    if (!fields.isEmpty()) {
                        FieldConfig f = fields.get(0);
                        Component c = fieldComponents.get(f);
                        if (c != null) {
                            c.requestFocus();
                        }
                    }
                }
            }
        });
    }

    @Override
    public Datasource getDatasource() {
        return datasource;
    }

    @Override
    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
        if (this.fields.isEmpty() && datasource != null) {
            //collect fields by entity view
            MetadataTools metadataTools = AppBeans.get(MetadataTools.class);
            Collection<MetaPropertyPath> fieldsMetaProps = metadataTools.getViewPropertyPaths(datasource.getView(), datasource.getMetaClass());
            for (MetaPropertyPath mpp : fieldsMetaProps) {
                MetaProperty mp = mpp.getMetaProperty();
                if (!mp.getRange().getCardinality().isMany() && !metadataTools.isSystem(mp)) {
                    FieldConfig field = new FieldConfig(mpp.toString());
                    addField(field);
                }
            }
        }
        rows = rowsCount();

        createFields();
    }

    @Override
    public boolean isRequired(FieldConfig field) {
        Component component = fieldComponents.get(field);
        return component instanceof com.haulmont.cuba.gui.components.Field && ((com.haulmont.cuba.gui.components.Field) component).isRequired();
    }

    @Override
    public void setRequired(FieldConfig field, boolean required, String message) {
        Component component = fieldComponents.get(field);
        if (component instanceof Field) {
            ((Field) component).setRequired(required);
            ((Field) component).setRequiredMessage(message);
        }
    }

    @Override
    public boolean isRequired(String fieldId) {
        FieldConfig field = fields.get(fieldId);
        return field != null && isRequired(field);
    }

    @Override
    public void setRequired(String fieldId, boolean required, String message) {
        FieldConfig field = fields.get(fieldId);
        if (field != null) {
            setRequired(field, required, message);
        }
    }

    @Override
    public void addValidator(FieldConfig field, Field.Validator validator) {
        Component component = fieldComponents.get(field);
        if (component instanceof Field) {
            ((Field) component).addValidator(validator);
        }
    }

    @Override
    public void addValidator(String fieldId, Field.Validator validator) {
        FieldConfig field = fields.get(fieldId);
        if (fieldId != null) {
            addValidator(field, validator);
        }
    }

    @Override
    public boolean isEditable(FieldConfig field) {
        return !readOnlyFields.contains(field);
    }

    @Override
    public void setEditable(FieldConfig field, boolean editable) {
        doSetEditable(field, editable);

        if (editable) {
            readOnlyFields.remove(field);
        } else {
            readOnlyFields.add(field);
        }
    }

    protected void doSetEditable(FieldConfig field, boolean editable) {
        Component component = fieldComponents.get(field);
        if (component instanceof Editable) {
            ((Editable) component).setEditable(editable);
        }
        if (fieldLabels.containsKey(field)) {
            fieldLabels.get(field).setEnabled(editable);
        }
    }

    @Override
    public boolean isEditable(String fieldId) {
        FieldConfig field = fields.get(fieldId);
        return field != null && isEditable(field);
    }

    @Override
    public void setEditable(String fieldId, boolean editable) {
        FieldConfig field = fields.get(fieldId);
        if (field != null) {
            setEditable(field, editable);
        }
    }

    @Override
    public boolean isEnabled(FieldConfig field) {
        Component component = fieldComponents.get(field);
        return component != null && component.isEnabled();
    }

    @Override
    public void setEnabled(FieldConfig field, boolean enabled) {
        doSetEnabled(field, enabled);

        if (enabled) {
            disabledFields.remove(field);
        } else {
            disabledFields.add(field);
        }
    }

    protected void doSetEnabled(FieldConfig field, boolean enabled) {
        Component component = fieldComponents.get(field);
        if (component != null) {
            component.setEnabled(enabled);
        }
        if (fieldLabels.containsKey(field)) {
            fieldLabels.get(field).setEnabled(enabled);
        }
    }

    @Override
    public boolean isEnabled(String fieldId) {
        FieldConfig field = fields.get(fieldId);
        return field != null && isEnabled(field);
    }

    @Override
    public void setEnabled(String fieldId, boolean enabled) {
        FieldConfig field = fields.get(fieldId);
        if (field != null) {
            setEnabled(field, enabled);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        for (FieldConfig field : fields.values()) {
            doSetEnabled(field, enabled && !disabledFields.contains(field));
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isVisible(FieldConfig field) {
        Component component = fieldComponents.get(field);
        return component != null && component.isVisible() && isVisible();
    }

    @Override
    public void setVisible(FieldConfig field, boolean visible) {
        Component component = fieldComponents.get(field);
        if (component != null) {
            component.setVisible(visible);
        }
        if (fieldLabels.containsKey(field)) {
            fieldLabels.get(field).setVisible(visible);
        }
    }

    @Override
    public boolean isVisible(String fieldId) {
        FieldConfig field = fields.get(fieldId);
        return field != null && isVisible(field);
    }

    @Override
    public void setVisible(String fieldId, boolean visible) {
        FieldConfig field = fields.get(fieldId);
        if (field != null) {
            setVisible(field, visible);
        }
    }

    @Override
    public boolean isBorderVisible() {
        return borderVisible;
    }

    @Override
    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
        collapsiblePanel.setBorderVisible(borderVisible);
    }

    @Override
    public Object getFieldValue(FieldConfig field) {
        Component component = fieldComponents.get(field);
        if (component instanceof HasValue) {
            return ((HasValue) component).getValue();
        }
        return null;
    }

    @Override
    public void setFieldValue(FieldConfig field, Object value) {
        Component component = fieldComponents.get(field);
        if (component instanceof HasValue) {
            ((HasValue) component).setValue(value);
        }
    }

    @Override
    public Object getFieldValue(String fieldId) {
        FieldConfig field = getField(fieldId);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        }
        return getFieldValue(field);
    }

    @Override
    public void setFieldValue(String fieldId, Object value) {
        FieldConfig field = getField(fieldId);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        }
        setFieldValue(field, value);
    }

    @Override
    public void requestFocus(String fieldId) {
        FieldConfig field = getField(fieldId);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        }
        final Component fieldComponent = fieldComponents.get(field);
        if (fieldComponent != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fieldComponent.requestFocus();
                }
            });
        }
    }

    @Override
    public void setFieldCaption(String fieldId, String caption) {
        FieldConfig field = getField(fieldId);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        }

        JLabel label = fieldLabels.get(field);
        if (label == null) {
            throw new IllegalStateException(String.format("Label for field '%s' not found", fieldId));
        }
        label.setText(caption);
    }

    @Override
    public void setCaptionAlignment(FieldCaptionAlignment captionAlignment) {
    }

    protected int rowsCount() {
        int rowsCount = 0;
        for (final List<FieldConfig> fields : columnFields.values()) {
            rowsCount = Math.max(rowsCount, fields.size());
        }
        return rowsCount;
    }

    @Override
    public int getColumns() {
        return cols;
    }

    @Override
    public void setColumns(int cols) {
        this.cols = cols;
    }

    @Override
    public float getColumnExpandRatio(int col) {
        return 0;
    }

    @Override
    public void setColumnExpandRatio(int col, float ratio) {
    }

    @Override
    public void addCustomField(String fieldId, CustomFieldGenerator fieldGenerator) {
        FieldConfig field = getField(fieldId);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Field '%s' doesn't exist", fieldId));
        }
        addCustomField(field, fieldGenerator);
    }

    @Override
    public void addCustomField(FieldConfig field, CustomFieldGenerator fieldGenerator) {
        if (!field.isCustom()) {
            throw new IllegalStateException(String.format("Field '%s' must be custom", field.getId()));
        }
        generators.put(field, fieldGenerator);
        // immediately create field, even before postInit()
        createFieldComponent(field);
    }

    @Override
    public void postInit() {
    }

    protected void createFields() {
        impl.removeAll();
        for (FieldConfig field : fields.values()) {
            if (field.isCustom()) {
                continue; // custom field is generated in another method
            }

            createFieldComponent(field);
        }
    }

    protected void createFieldComponent(FieldConfig fieldConf) {
        int col = fieldsColumn.get(fieldConf);
        int row = columnFields.get(col).indexOf(fieldConf);

        Datasource ds;
        if (fieldConf.getDatasource() != null) {
            ds = fieldConf.getDatasource();
        } else {
            ds = datasource;
        }

        String caption = null;
        String description = null;

        CustomFieldGenerator generator = generators.get(fieldConf);
        if (generator == null) {
            generator = createDefaultGenerator(fieldConf);
        }

        Component fieldComponent = generator.generateField(ds, fieldConf.getId());

        if (fieldComponent instanceof Field) { // do not create caption for buttons etc.
            Field cubaField = (Field) fieldComponent;

            caption = fieldConf.getCaption();
            if (caption == null) {
                MetaPropertyPath propertyPath = ds.getMetaClass().getPropertyPath(fieldConf.getId());
                if (propertyPath != null) {
                    caption = AppBeans.get(MessageTools.class).getPropertyCaption(propertyPath.getMetaClass(), fieldConf.getId());
                }
            }

            if (StringUtils.isNotEmpty(cubaField.getCaption())) {
                caption = cubaField.getCaption();     // custom field has manually set caption
            }

            description = fieldConf.getDescription();
            if (StringUtils.isNotEmpty(cubaField.getDescription())) {
                description = cubaField.getDescription();  // custom field has manually set description
            } else if (StringUtils.isNotEmpty(description)) {
                cubaField.setDescription(description);
            }

            if (!cubaField.isRequired()) {
                cubaField.setRequired(fieldConf.isRequired());
            }
            if (fieldConf.getRequiredError() != null) {
                cubaField.setRequiredMessage(fieldConf.getRequiredError());
            }
        }

        if (fieldComponent instanceof HasFormatter) {
            ((HasFormatter) fieldComponent).setFormatter(fieldConf.getFormatter());
        }

        // some components (e.g. LookupPickerField) have width from the creation, so I commented out this check
        if (/*f.getWidth() == -1f &&*/ fieldConf.getWidth() != null) {
            fieldComponent.setWidth(fieldConf.getWidth());
        } else {
            fieldComponent.setWidth(DEFAULT_FIELD_WIDTH);
        }

        applyPermissions(fieldComponent);

        JLabel label = new JLabel(caption);
        label.setVisible(fieldComponent.isVisible());
        CC labelCc = new CC();
        MigLayoutHelper.applyAlignment(labelCc, Alignment.MIDDLE_LEFT);

        impl.add(label, labelCc.cell(col * 3, row, 1, 1));
        fieldLabels.put(fieldConf, label);

        if (description != null && !(fieldComponent instanceof CheckBox)) {
            fieldConf.setDescription(description);
            ToolTipButton tooltipBtn = new ToolTipButton();
            tooltipBtn.setVisible(fieldComponent.isVisible());
            tooltipBtn.setToolTipText(description);
            DesktopToolTipManager.getInstance().registerTooltip(tooltipBtn);
            impl.add(tooltipBtn, new CC().cell(col * 3 + 2, row, 1, 1).alignY("top"));
            fieldTooltips.put(fieldConf, tooltipBtn);
        }
        fieldComponents.put(fieldConf, fieldComponent);
        assignTypicalAttributes(fieldComponent);
        JComponent jComponent = DesktopComponentsHelper.getComposition(fieldComponent);
        CC cell = new CC().cell(col * 3 + 1, row, 1, 1);

        MigLayoutHelper.applyWidth(cell, (int) fieldComponent.getWidth(), fieldComponent.getWidthUnits(), false);
        MigLayoutHelper.applyHeight(cell, (int) fieldComponent.getHeight(), fieldComponent.getHeightUnits(), false);
        MigLayoutHelper.applyAlignment(cell, fieldComponent.getAlignment());

        jComponent.putClientProperty(getSwingPropertyId(), fieldConf.getId());
        impl.add(jComponent, cell);
    }

    protected void applyPermissions(Component c) {
        if (c instanceof DatasourceComponent) {
            DatasourceComponent dsComponent = (DatasourceComponent) c;
            MetaProperty metaProperty = dsComponent.getMetaProperty();

            if (metaProperty != null) {
                dsComponent.setEditable(security.isEntityAttrModificationPermitted(metaProperty)
                        && dsComponent.isEditable());
            }
        }
    }

    protected void assignTypicalAttributes(Component c) {
        if (c instanceof BelongToFrame) {
            BelongToFrame belongToFrame = (BelongToFrame) c;
            if (belongToFrame.getFrame() == null) {
                belongToFrame.setFrame(getFrame());
            }
        }
    }

    protected CustomFieldGenerator createDefaultGenerator(final FieldConfig field) {
        return new CustomFieldGenerator() {
            @Override
            public Component generateField(Datasource datasource, String propertyId) {
                Component component = fieldFactory.createField(datasource, propertyId, field.getXmlDescriptor());
                if (component instanceof HasFormatter) {
                    ((HasFormatter) component).setFormatter(field.getFormatter());
                }
                return component;
            }
        };
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;

        for (FieldConfig field : fields.values()) {
            doSetEditable(field, editable && !readOnlyFields.contains(field));
        }
    }

    @Override
    public String getCaption() {
        return collapsiblePanel.getCaption();
    }

    @Override
    public void setCaption(String caption) {
        collapsiblePanel.setCaption(caption);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setDescription(String description) {
    }

    public Collection<Component> getComponents() {
        return fieldComponents.values();
    }

    @Override
    public JComponent getComposition() {
        return collapsiblePanel;
    }

    @Override
    public boolean expandsWidth() {
        return true;
    }

    @Override
    public boolean expandsHeight() {
        return false;
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
        if (!isVisible() || !isEditable() || !isEnabled()) {
            return;
        }

        Map<FieldConfig, Exception> problems = new HashMap<>();

        for (Map.Entry<FieldConfig, Component> componentEntry : fieldComponents.entrySet()) {
            FieldConfig field = componentEntry.getKey();
            Component component = componentEntry.getValue();

            if (!isEditable(field) || !isEnabled(field) || !isVisible(field)) {
                continue;
            }

            // If has valid state
            if ((component instanceof Validatable) &&
                    (component instanceof Editable)) {
                // If editable
                if (component.isVisible() &&
                        component.isEnabled() &&
                        ((Editable) component).isEditable()) {

                    try {
                        ((Validatable) component).validate();
                    } catch (ValidationException ex) {
                        problems.put(field, ex);
                    }
                }
            }
        }

        if (!problems.isEmpty()) {
            StringBuilder msgBuilder = new StringBuilder();
            for (Iterator<FieldConfig> iterator = problems.keySet().iterator(); iterator.hasNext(); ) {
                FieldConfig field = iterator.next();
                Exception ex = problems.get(field);
                msgBuilder.append(ex.getMessage());
                if (iterator.hasNext()) {
                    msgBuilder.append("<br>");
                }
            }

            FieldsValidationException validationException = new FieldsValidationException(msgBuilder.toString());
            validationException.setProblemFields(problems);
            throw validationException;
        }
    }

    protected class FieldFactory extends AbstractFieldFactory {

        @Override
        protected CollectionDatasource getOptionsDatasource(Datasource datasource, String property) {
            final FieldConfig field = fields.get(property);

            DsContext dsContext;
            if (datasource == null) {
                if (field.getDatasource() == null) {
                    throw new IllegalStateException("FieldGroup datasource is null");
                }
                dsContext = field.getDatasource().getDsContext();
            } else {
                dsContext = datasource.getDsContext();
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
    }
}