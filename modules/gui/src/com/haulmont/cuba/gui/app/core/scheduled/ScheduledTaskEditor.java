/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.app.core.scheduled;

import com.haulmont.cuba.core.app.SchedulingService;
import com.haulmont.cuba.core.app.scheduled.MethodInfo;
import com.haulmont.cuba.core.app.scheduled.MethodParameterInfo;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.ScheduledTask;
import com.haulmont.cuba.core.entity.ScheduledTaskDefinedBy;
import com.haulmont.cuba.core.entity.SchedulingType;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class ScheduledTaskEditor extends AbstractEditor<ScheduledTask> {

    @Inject
    protected LookupField beanNameField;

    @Inject
    protected LookupField methodNameField;

    @Inject
    protected LookupField userNameField;

    @Inject
    protected OptionsGroup definedByField;

    @Inject
    protected TextField classNameField;

    @Inject
    protected TextField scriptNameField;

    @Inject
    protected Label beanNameLabel;

    @Inject
    protected Label methodNameLabel;

    @Inject
    protected Label classNameLabel;

    @Inject
    protected Label scriptNameLabel;

    @Inject
    protected Container methodParamsBox;

    @Inject
    protected Datasource<ScheduledTask> taskDs;

    @Inject
    protected SchedulingService service;

    @Inject
    protected ComponentsFactory componentsFactory;

    @Inject
    protected OptionsGroup schedulingTypeField;

    //List holds an information about methods of selected bean
    List<MethodInfo> availableMethods = new ArrayList<>();

    @Inject
    protected TextField cronField;

    @Inject
    protected TextField periodField;

    @Inject
    protected DateField startDateField;

    @Inject
    protected Label cronLabel;

    @Inject
    protected Label periodLabel;

    @Inject
    protected Label startDateLabel;

    @Inject
    protected ThemeConstants themeConstants;

    @Inject
    private LinkButton cronHelpButton;

    @Inject
    private Label cronEmptyLabel;

    @Inject
    private Label periodEmptyLabel;

    @Inject
    private Label beanNameEmptyLabel;

    @Inject
    private Label classNameEmptyLabel;

    @Inject
    private Label scriptNameEmptyLabel;

    @Inject
    private Label startDateEmptyLabel;

    @Inject
    private BoxLayout cronHbox;

    private void show(Component... components) {
        for (Component component : components) {
            component.setVisible(true);
        }
    }

    private void hide(Component... components) {
        for (Component component : components) {
            component.setVisible(false);
        }
    }

    private void clear(Field... fields) {
        for (Field component : fields) {
            component.setValue(null);
        }
    }

    @Override
    public void init(Map<String, Object> params) {
        schedulingTypeField.setOptionsList(Arrays.asList(SchedulingType.values()));
        schedulingTypeField.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, @Nullable Object prevValue, @Nullable Object value) {
                setSchedulingTypeField((SchedulingType) value);
            }
        });

        definedByField.setOptionsList(Arrays.asList(ScheduledTaskDefinedBy.values()));
        definedByField.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                if (ScheduledTaskDefinedBy.BEAN == value) {
                    clear(classNameField, scriptNameField);
                    hideAll();
                    show(beanNameField, beanNameLabel, beanNameEmptyLabel, methodNameField, methodNameLabel, methodParamsBox);
                } else if (ScheduledTaskDefinedBy.CLASS == value) {
                    clear(beanNameField, methodNameField, scriptNameField);
                    hideAll();
                    show(classNameField, classNameLabel, classNameEmptyLabel);
                } else if (ScheduledTaskDefinedBy.SCRIPT == value) {
                    clear(beanNameField, methodNameField, classNameField);
                    hideAll();
                    show(scriptNameField, scriptNameLabel, scriptNameEmptyLabel);
                } else {
                    clear(beanNameField, methodNameField, classNameField, scriptNameField);
                    hideAll();
                }
            }

            private void hideAll() {
                hide(classNameField, classNameLabel, scriptNameField, scriptNameLabel, beanNameField, beanNameLabel,
                        methodNameField, methodNameLabel, methodParamsBox, classNameEmptyLabel, beanNameEmptyLabel, scriptNameEmptyLabel);
            }
        });

        final Map<String, List<MethodInfo>> availableBeans = service.getAvailableBeans();
        beanNameField.setOptionsList(new ArrayList<>(availableBeans.keySet()));
        beanNameField.addListener(new ValueListener<LookupField>() {
            @Override
            public void valueChanged(LookupField source, String property, Object prevValue, Object value) {
                methodNameField.setValue(null);
                if (value == null)
                    methodNameField.setOptionsList(Collections.emptyList());
                else {
                    availableMethods = availableBeans.get(value);

                    if (availableMethods != null) {
                        HashMap<String, Object> optionsMap = new HashMap<>();
                        for (MethodInfo availableMethod : availableMethods) {
                            optionsMap.put(availableMethod.getMethodSignature(), availableMethod);
                        }
                        methodNameField.setOptionsMap(optionsMap);
                    }
                }
            }
        });

        methodNameField.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                clearMethodParamsGrid();
                if (value != null) {
                    createMethodParamsGrid((MethodInfo) value);
                }

                String methodName = (value != null) ? ((MethodInfo) value).getName() : null;
                taskDs.getItem().setMethodName(methodName);

                List<MethodParameterInfo> methodParams = (value != null) ? ((MethodInfo) value).getParameters() : Collections.<MethodParameterInfo>emptyList();
                taskDs.getItem().updateMethodParameters(methodParams);
            }
        });

        userNameField.setOptionsList(service.getAvailableUsers());
    }

    private void setSchedulingTypeField(SchedulingType value) {
        if (SchedulingType.CRON == value) {
            hide(periodField, periodLabel, periodEmptyLabel, startDateField, startDateLabel, startDateEmptyLabel);
            clear(periodField, startDateField);
            show(cronField, cronLabel, cronEmptyLabel, cronHelpButton, cronHbox);
        } else {
            hide(cronField, cronLabel, cronEmptyLabel, cronHelpButton, cronHbox);
            clear(cronField);
            show(periodField, periodLabel, periodEmptyLabel, startDateField, startDateLabel, startDateEmptyLabel);
        }
    }


    @Override
    protected void initNewItem(ScheduledTask item) {
        item.setDefinedBy(ScheduledTaskDefinedBy.BEAN);
        item.setSchedulingType(SchedulingType.PERIOD);
    }

    @Override
    public void setItem(Entity item) {
        super.setItem(item);

        if (StringUtils.isNotEmpty(getItem().getMethodName())) {
            setInitialMethodNameValue(getItem());
        }
    }

    /**
     * Method reads values of methodName and parameters from item,
     * finds appropriate MethodInfo object in methodInfoField's optionsList
     * and sets found value to methodInfoField
     */
    private void setInitialMethodNameValue(ScheduledTask task) {
        if (availableMethods == null)
            return;

        List<MethodParameterInfo> methodParamInfos = task.getMethodParameters();
        MethodInfo currentMethodInfo = new MethodInfo(task.getMethodName(), methodParamInfos);
        for (MethodInfo availableMethod : availableMethods) {
            if (currentMethodInfo.definitionEquals(availableMethod)) {
                availableMethod.setParameters(currentMethodInfo.getParameters());
                methodNameField.setValue(availableMethod);
                break;
            }
        }
    }

    private void createMethodParamsGrid(MethodInfo methodInfo) {
        GridLayout methodParamsGrid = componentsFactory.createComponent(GridLayout.NAME);
        methodParamsGrid.setSpacing(true);
        methodParamsGrid.setColumns(2);

        int rowsCount = 0;

        for (final MethodParameterInfo parameterInfo : methodInfo.getParameters()) {
            Label nameLabel = componentsFactory.createComponent(Label.NAME);
            nameLabel.setValue(parameterInfo.getType().getSimpleName() + " " + parameterInfo.getName());

            TextField valueTextField = componentsFactory.createComponent(TextField.NAME);
            valueTextField.setWidth(themeConstants.get("cuba.gui.ScheduledTaskEditor.valueTextField.width"));
            valueTextField.setValue(parameterInfo.getValue());

            valueTextField.addListener(new ValueListener() {
                @Override
                public void valueChanged(Object source, String property, Object prevValue, Object value) {
                    parameterInfo.setValue(value);
                    MethodInfo selectedMethod = methodNameField.getValue();
                    taskDs.getItem().updateMethodParameters(selectedMethod.getParameters());
                }
            });

            methodParamsGrid.setRows(++rowsCount);
            methodParamsGrid.add(nameLabel, 0, rowsCount - 1);
            methodParamsGrid.add(valueTextField, 1, rowsCount - 1);
        }
        methodParamsBox.add(methodParamsGrid);
    }

    private void clearMethodParamsGrid() {
        for (Component component : methodParamsBox.getComponents()) {
            methodParamsBox.remove(component);
        }
    }

    public void getCronHelp() {
        showMessageDialog("Cron", getMessage("cronDescription"), MessageType.CONFIRMATION_HTML);
    }
}