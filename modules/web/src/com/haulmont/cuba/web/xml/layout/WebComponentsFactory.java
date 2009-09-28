/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 19.12.2008 16:05:08
 * $Id$
 */
package com.haulmont.cuba.web.xml.layout;

import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.gui.WebTimer;
import com.haulmont.cuba.web.gui.components.*;

import java.util.Map;
import java.util.HashMap;

public class WebComponentsFactory implements ComponentsFactory {

    private static Map<String, Class<? extends Component>> classes = new HashMap<String, Class<?extends Component>>();

    static {
        classes.put("window", WebWindow.class);
        classes.put("window.editor", WebWindow.Editor.class);
        classes.put("window.lookup", WebWindow.Lookup.class);

        classes.put("hbox", WebHBoxLayout.class);
        classes.put("vbox", WebVBoxLayout.class);
        classes.put("grid", WebGridLayout.class);
        classes.put("scrollbox", WebScrollBoxLayout.class);
        classes.put("togglebox", WebToggleBoxLayout.class);
        classes.put("htmlbox", WebHtmlBoxLayout.class);

        classes.put("button", WebButton.class);
        classes.put("label", WebLabel.class);
        classes.put("checkBox", WebCheckBox.class);
        classes.put("groupBox", WebGroupBox.class);
        classes.put("textField", WebTextField.class);
        classes.put("textArea", WebTextArea.class);
        classes.put("iframe", WebFrame.class);
        classes.put("table", WebTable.class);
        classes.put("treeTable", WebTreeTable.class);
        classes.put("dateField", WebDateField.class);
        classes.put("lookupField", WebLookupField.class);
        classes.put("pickerField", WebPickerField.class);
        classes.put("optionsGroup", WebOptionsGroup.class);
        classes.put("upload", WebFileUploadField.class);
        classes.put("split", WebSplitPanel.class);
        classes.put("tree", WebTree.class);
        classes.put("tabsheet", WebTabsheet.class);
        classes.put("embedded", WebEmbedded.class);
    }

    public <T extends Component> T createComponent(String name) throws InstantiationException, IllegalAccessException {
        final Class<Component> componentClass = (Class<Component>) classes.get(name);
        if (componentClass == null) {
            throw new IllegalStateException(String.format("Can't find component class for '%s'", name));
        }
        return (T) componentClass.newInstance();
    }

    public <T extends Timer> T createTimer() throws InstantiationException {
        return (T) new WebTimer();
    }
}
