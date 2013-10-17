/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.app.ui.serverlogviewer;

import com.haulmont.cuba.core.entity.JmxInstance;
import com.haulmont.cuba.core.sys.logging.LogControlException;
import com.haulmont.cuba.core.sys.logging.LoggingHelper;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.web.app.ui.jmxinstance.edit.JmxInstanceEditor;
import com.haulmont.cuba.web.export.LogDataProvider;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.jmx.JmxControlAPI;
import com.haulmont.cuba.web.jmx.JmxControlException;
import com.haulmont.cuba.web.jmx.JmxRemoteLoggingAPI;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * @author shatokhin
 * @version $Id$
 */
public class ServerLogWindow extends AbstractWindow {

    private static final Log log = LogFactory.getLog(ServerLogWindow.class);

    @Inject
    protected CollectionDatasource<JmxInstance, UUID> jmxInstancesDs;

    @Inject
    protected com.haulmont.cuba.gui.components.Label localJmxField;

    @Inject
    protected LookupPickerField jmxConnectionField;

    @Inject
    protected JmxRemoteLoggingAPI jmxRemoteLoggingAPI;

    @Inject
    protected JmxControlAPI jmxControlAPI;

    @Inject
    protected GroupBoxLayout logFieldBox;

    @Inject
    protected LookupField logFileNameField;

    @Inject
    protected LookupField loggerLevelField;

    @Inject
    protected LookupField loggerNameField;

    @Inject
    protected LookupField appenderNameField;

    @Inject
    protected LookupField appenderLevelField;

    @Inject
    protected CheckBox autoRefreshCheck;

    protected JmxInstance localJmxInstance;

    protected final com.vaadin.ui.Label logTailLabel = new com.vaadin.ui.Label();
    protected final com.vaadin.ui.Panel logContainer = new com.vaadin.ui.Panel();

    @Override
    public void init(Map<String, Object> params) {
        localJmxField.setValue(jmxControlAPI.getLocalNodeName());
        localJmxField.setEditable(false);

        localJmxInstance = jmxControlAPI.getLocalInstance();

        jmxInstancesDs.refresh();
        jmxConnectionField.setValue(localJmxInstance);
        jmxConnectionField.setRequired(true);
        jmxConnectionField.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                JmxInstance jmxInstance = jmxConnectionField.getValue();
                try {
                    refreshLoggers();
                    refreshAppenders();
                    refreshLogFileNames();
                } catch (JmxControlException e) {
                    showNotification(getMessage("exception.unableToConnectToInterface"), NotificationType.WARNING);
                    if (jmxInstance != localJmxInstance) {
                        jmxConnectionField.setValue(localJmxInstance);
                    }
                }
            }
        });

        for (Action action : new LinkedList<>(jmxConnectionField.getActions())) {
            jmxConnectionField.removeAction(action);
        }

        jmxConnectionField.addAction(new PickerField.LookupAction(jmxConnectionField) {
            @Override
            public void afterCloseLookup(String actionId) {
                jmxInstancesDs.refresh();
            }
        });

        jmxConnectionField.addAction(new AbstractAction("actions.Add") {
            @Override
            public void actionPerform(Component component) {
                final JmxInstanceEditor instanceEditor = openEditor("sys$JmxInstance.edit", new JmxInstance(), WindowManager.OpenType.DIALOG);
                instanceEditor.addListener(new CloseListener() {
                    @Override
                    public void windowClosed(String actionId) {
                        if (COMMIT_ACTION_ID.equals(actionId)) {
                            jmxInstancesDs.refresh();
                            jmxConnectionField.setValue(instanceEditor.getItem());
                        }
                    }
                });
            }

            @Override
            public String getIcon() {
                return "icons/plus-btn.png";
            }
        });

        logContainer.setSizeFull();
        logContainer.setScrollable(true);

        ComponentContainer content = logContainer.getContent();
        content.setWidth(Sizeable.SIZE_UNDEFINED, Sizeable.UNITS_PIXELS);

        AbstractComponentContainer groupBox = (AbstractComponentContainer) WebComponentsHelper.unwrap(logFieldBox);
        logContainer.addComponent(logTailLabel);
        groupBox.addComponent(logContainer);

        logTailLabel.setSizeUndefined();
        logTailLabel.setContentMode(Label.CONTENT_XHTML);
        logTailLabel.setStyleName("code-monospace");

        loggerLevelField.setOptionsList(LoggingHelper.getLevels());
        appenderLevelField.setOptionsList(LoggingHelper.getLevels());

        refreshLoggers();
        refreshAppenders();
        refreshLogFileNames();

        loggerNameField.addListener(new ValueListener<Object>() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                List<String> currentLoggers = new ArrayList<>(jmxRemoteLoggingAPI.getLoggers(getSelectedConnection()));

                Collections.sort(currentLoggers);
                currentLoggers.add(0, getMessage("logger.new"));
                if (value != null && value.equals(currentLoggers.get(0))) {
                    openAddLoggerDialog();
                }
            }
        });
    }

    protected void openAddLoggerDialog() {
        final AdditionLoggerWindow additionLogger = openWindow("logcontrol$AddLoggerDialog", WindowManager.OpenType.DIALOG);
        additionLogger.addListener(new CloseListener() {
            @Override
            public void windowClosed(String actionId) {
                if (COMMIT_ACTION_ID.equals(actionId)) {
                    Level level = additionLogger.getSelectedLevel();
                    String loggerName = additionLogger.getSelectedLoggerName();

                    try {
                        jmxRemoteLoggingAPI.setLoggerLevel(getSelectedConnection(), loggerName, level.toString());
                    } catch (LogControlException e) {
                        log.error(e);
                        showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                    }

                    showNotification(String.format(getMessage("logger.setMessage"), loggerName, level.toString()),
                            NotificationType.HUMANIZED);
                    refreshLoggers();
                }
            }
        });
        loggerNameField.setValue(null);
        loggerLevelField.setValue(null);
    }

    // action method
    public void showLogTail() {
        updateLogTail(false);
    }

    public void updateLogTail(boolean isTimedEvent) {
        if (logFileNameField.getValue() != null) {
            String logFileName = logFileNameField.getValue();
            String value;

            try {
                value = jmxRemoteLoggingAPI.getTail(getSelectedConnection(), logFileName);
            } catch (LogControlException e) {
                log.error(e);
                if (!isTimedEvent)
                    showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                return;
            }

            // transform to XHTML
            value = StringEscapeUtils.escapeHtml(value);
            value = StringUtils.replace(value, " ", "&nbsp;");

            // highlight log
            StringBuilder coloredLog = new StringBuilder();
            BufferedReader reader = new BufferedReader(new StringReader(value));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (Level level : LoggingHelper.getLevels())
                        line = highlightLevel(line, level.toString());
                    coloredLog.append(line).append("<br/>");
                }
            } catch (IOException e) {
                log.warn(e);
                return;
            }
            logTailLabel.setValue(coloredLog.toString());
        } else
            showNotification(getMessage("log.notSelected"), NotificationType.HUMANIZED);

        int scrollPos = logContainer.getScrollTop() + 30000;
        logContainer.setScrollTop(scrollPos);
    }

    // action method
    public void getLoggerLevel() {
        if (StringUtils.isNotEmpty(loggerNameField.<String>getValue())) {
            String loggerName = loggerNameField.getValue();
            String level;

            try {
                level = jmxRemoteLoggingAPI.getLoggerLevel(getSelectedConnection(), loggerName);
            } catch (LogControlException e) {
                log.error(e);
                showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                return;
            }

            if (level != null)
                loggerLevelField.setValue(LoggingHelper.getLevelFromString(level));
        } else {
            loggerNameField.setValue(null);
            showNotification(getMessage("logger.notSelected"), NotificationType.HUMANIZED);
        }
    }

    // action method
    public void setLoggerLevel() {
        if (StringUtils.isNotEmpty(loggerNameField.<String>getValue())) {
            if (loggerLevelField.getValue() != null) {
                String loggerName = loggerNameField.getValue();
                Level level = loggerLevelField.getValue();

                try {
                    jmxRemoteLoggingAPI.setLoggerLevel(getSelectedConnection(), loggerName, level.toString());
                } catch (LogControlException e) {
                    log.error(e);
                    showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                    return;
                }

                showNotification(formatMessage("logger.setMessage", loggerName, level.toString()), NotificationType.HUMANIZED);
            } else
                showNotification(getMessage("logger.notSelectedLevel"), NotificationType.HUMANIZED);
        } else {
            loggerNameField.setValue(null);
            showNotification(getMessage("logger.notSelected"), NotificationType.HUMANIZED);
        }
    }

    // action method
    public void getAppenderLevel() {
        if (StringUtils.isNotEmpty(appenderNameField.<String>getValue())) {
            String appenderName = appenderNameField.getValue();
            String threshold;

            try {
                threshold = jmxRemoteLoggingAPI.getAppenderThreshold(getSelectedConnection(), appenderName);
            } catch (LogControlException e) {
                log.error(e);
                showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                return;
            }

            if (threshold != null)
                appenderLevelField.setValue(LoggingHelper.getLevelFromString(threshold));
        } else {
            appenderLevelField.setValue(null);
            showNotification(getMessage("appender.notSelected"), NotificationType.HUMANIZED);
        }
    }

    // action method
    public void setAppenderLevel() {
        if (StringUtils.isNotEmpty(appenderNameField.<String>getValue())) {
            if (appenderLevelField.getValue() != null) {
                String appenderName = appenderNameField.getValue();
                Level threshold = appenderLevelField.getValue();

                try {
                    jmxRemoteLoggingAPI.setAppenderThreshold(getSelectedConnection(), appenderName, threshold.toString());
                } catch (LogControlException e) {
                    log.error(e);
                    showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                    return;
                }

                showNotification(formatMessage("appender.setMessage", appenderName, threshold.toString()), NotificationType.HUMANIZED);
            } else
                showNotification(getMessage("appender.notSelectedThreshold"), NotificationType.HUMANIZED);
        } else {
            appenderNameField.setValue(null);
            showNotification(getMessage("appender.notSelected"), NotificationType.HUMANIZED);
        }
    }

    // action method
    public void downloadLog() {
        String fileName = logFileNameField.getValue();
        if (fileName != null) {
            try {
                LogDataProvider logDataProvider = new LogDataProvider(getSelectedConnection(), fileName);
                AppConfig.createExportDisplay(this).show(logDataProvider, fileName + ".zip");
            } catch (Exception e) {
                showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                log.error(e);
            }
        } else
            showNotification(getMessage("log.notSelected"), NotificationType.HUMANIZED);
    }

    // action method
    public void updateLogTail(@SuppressWarnings("unused") Timer timer) {
        boolean autoRefresh = (Boolean.parseBoolean(autoRefreshCheck.getValue().toString()));
        if (autoRefresh)
            updateLogTail(true);
    }

    // action method
    public void openLoggerControlDialog() {
        Map<String, Object> params = new HashMap<>();
        Map<String, Level> loggersMap = new HashMap<>();
        List<String> currentLoggers = jmxRemoteLoggingAPI.getLoggers(getSelectedConnection());
        for (String loggerName : currentLoggers) {
            String levelString;

            try {
                levelString = jmxRemoteLoggingAPI.getLoggerLevel(getSelectedConnection(), loggerName);
            } catch (LogControlException e) {
                log.error(e);
                showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                return;
            }

            Level logLevel = LoggingHelper.getLevelFromString(levelString);
            loggersMap.put(loggerName, logLevel);
        }
        params.put("loggersMap", loggersMap);

        final ControlLoggerWindow controlLogger = openWindow("logcontrol$LoggerControlDialog", WindowManager.OpenType.DIALOG, params);
        controlLogger.addListener(new CloseListener() {
            @Override
            public void windowClosed(String actionId) {
                if (COMMIT_ACTION_ID.equals(actionId)) {
                    Map<String, Level> levels = controlLogger.getLevels();
                    try {
                        for (String loggerName : levels.keySet()) {
                            String logLevel = jmxRemoteLoggingAPI.getLoggerLevel(getSelectedConnection(), loggerName);
                            Level newLogLevel = levels.get(loggerName);

                            if (!StringUtils.equals(logLevel, newLogLevel.toString())) {
                                jmxRemoteLoggingAPI.setLoggerLevel(getSelectedConnection(), loggerName, newLogLevel.toString());
                            }
                        }
                    } catch (LogControlException e) {
                        log.error(e);
                        showNotification(getMessage("exception.logControl"), NotificationType.ERROR);
                    }
                    showNotification(getMessage("logger.control.apply"), NotificationType.HUMANIZED);
                    refreshLoggers();
                }
            }
        });
    }

    protected String highlightLevel(String line, String level) {
        // use css classes for highlight different log levels
        return line.replaceFirst(level, "<span class='log-level log-level-" + level + "'>" + level + "</span>");
    }

    protected JmxInstance getSelectedConnection() {
        return jmxConnectionField.getValue();
    }

    protected void refreshLoggers() {
        List<String> loggers = new ArrayList<>(jmxRemoteLoggingAPI.getLoggers(getSelectedConnection()));

        Collections.sort(loggers);
        loggers.add(0, getMessage("logger.new"));
        loggerNameField.setOptionsList(loggers);

        logFileNameField.setValue(null);
        loggerLevelField.setValue(null);
    }

    protected void refreshAppenders() {
        List<String> appenders = jmxRemoteLoggingAPI.getAppenders(getSelectedConnection());

        Collections.sort(appenders);
        appenderNameField.setOptionsList(appenders);

        appenderNameField.setValue(null);
        appenderLevelField.setValue(null);
    }

    protected void refreshLogFileNames() {
        List<String> logFiles = jmxRemoteLoggingAPI.getLogFileNames(getSelectedConnection());
        logFileNameField.setOptionsList(logFiles);

        autoRefreshCheck.setValue(false);
        logFileNameField.setValue(null);

        logTailLabel.setValue("");
    }
}