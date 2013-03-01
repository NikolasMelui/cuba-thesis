/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.gui.components.ValueProvider;
import com.haulmont.cuba.toolkit.gwt.client.utils.VScriptHost;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ClientWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Component for evaluate custom JavaScript from server
 *
 * @author artamonov
 * @version $Id$
 */
@ClientWidget(VScriptHost.class)
public class JavaScriptHost extends AbstractComponent {
    private static final long serialVersionUID = -136425458030091656L;

    private static class ScriptValueProvider implements ValueProvider {

        Map<String, Object> params = new HashMap<>();

        @Override
        public Map<String, Object> getValues() {
            return params;
        }

        @Override
        public Map<String, Object> getParameters() {
            return params;
        }

        public Set<Map.Entry<String, Object>> getEntrySet() {
            return params.entrySet();
        }

        public void putParam(String key, Object value) {
            params.put(key, value);
        }

        public void removeParam(String key) {
            params.remove(key);
        }
    }

    private ScriptValueProvider valueProvider = new ScriptValueProvider();

    private HistoryBackHandler historyBackHandler = null;

    @Override
    public void changeVariables(Object source, Map<String, Object> variables) {
        super.changeVariables(source, variables);

        if (variables.containsKey(VScriptHost.HISTORY_BACK_ACTION) && historyBackHandler != null) {
            historyBackHandler.onHistoryBackPerformed();
        }
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        for (Map.Entry<String, Object> paramEntry : valueProvider.getEntrySet()) {
            Object value = paramEntry.getValue();
            String key = paramEntry.getKey();
            if (value instanceof Map)
                target.addAttribute(key, (Map<?, ?>) value);
            else
                target.addAttribute(key, String.valueOf(value));
        }
        cleanCommand();
    }

    public ValueProvider getValueProvider() {
        return valueProvider;
    }

    public void updateLocale(Map localeMap) {
        cleanCommand();

        valueProvider.putParam(VScriptHost.COMMAND_PARAM_KEY, VScriptHost.LOCALE_COMMAND);
        valueProvider.putParam(VScriptHost.LOCALE_PARAM_KEY, localeMap);

        requestRepaint();
    }

    public void evaluateScript(String script) {
        cleanCommand();

        valueProvider.putParam(VScriptHost.COMMAND_PARAM_KEY, VScriptHost.SCRIPT_COMMAND);
        valueProvider.putParam(VScriptHost.SCRIPT_PARAM_KEY, script);

        requestRepaint();
    }

    public void viewDocument(String documentUrl) {
        cleanCommand();

        valueProvider.putParam(VScriptHost.COMMAND_PARAM_KEY, VScriptHost.VIEW_COMMAND);
        valueProvider.putParam(VScriptHost.URL_PARAM_KEY, documentUrl);

        requestRepaint();
    }

    public void getResource(String resourceUrl) {
        cleanCommand();

        valueProvider.putParam(VScriptHost.COMMAND_PARAM_KEY, VScriptHost.GET_COMMAND);
        valueProvider.putParam(VScriptHost.URL_PARAM_KEY, resourceUrl);

        requestRepaint();
    }

    private void cleanCommand() {
        valueProvider.removeParam(VScriptHost.SCRIPT_PARAM_KEY);
        valueProvider.removeParam(VScriptHost.COMMAND_PARAM_KEY);
    }

    public HistoryBackHandler getHistoryBackHandler() {
        return historyBackHandler;
    }

    public void setHistoryBackHandler(HistoryBackHandler historyBackHandler) {
        this.historyBackHandler = historyBackHandler;
    }

    public interface HistoryBackHandler {
        /**
         * When User performs back step by history
         */
        void onHistoryBackPerformed();
    }
}