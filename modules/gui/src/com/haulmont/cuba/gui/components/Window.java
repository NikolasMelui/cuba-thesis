/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 19.12.2008 15:11:57
 * $Id$
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.gui.settings.Settings;

import java.util.Collection;

public interface Window extends IFrame, Component.HasCaption, Component.Actions {
    void addListener(CloseListener listener);
    void removeListener(CloseListener listener);

    void applySettings(Settings settings);

    boolean close(String actionId);

    interface Editor extends Window {
        Object getItem();
        void setItem(Object item);

        boolean isValid();
        void validate() throws ValidationException;

        boolean commit();
        void commitAndClose();
    }

    interface Lookup extends Window {
        Component getLookupComponent();
        void setLookupComponent(Component lookupComponent);

        interface Handler {
            void handleLookup(Collection items);
        }

        Handler getLookupHandler();
        void setLookupHandler(Handler handler);
    }

    interface CloseListener {
        void windowClosed(String actionId);
    }

    interface Wrapper {
        <T extends Window> T getWrappedWindow();
    }
}
