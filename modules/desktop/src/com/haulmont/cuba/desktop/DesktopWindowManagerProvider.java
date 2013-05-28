/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop;

import com.haulmont.cuba.desktop.gui.components.DesktopComponentsHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManagerProvider;

import javax.annotation.ManagedBean;
import javax.swing.*;
import java.awt.*;

/**
 * @author artamonov
 * @version $Id$
 */
@ManagedBean(WindowManagerProvider.NAME)
public class DesktopWindowManagerProvider implements WindowManagerProvider {

    @Override
    public WindowManager get() {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Could not access to WindowManager outside Event Dispath Thread");

        AWTEvent event = EventQueue.getCurrentEvent();
        if (event == null)
            throw new IllegalStateException("Could not access to WindowManager without Event");

        Object eventSource = event.getSource();
        if (!(eventSource instanceof Component))
            throw new IllegalStateException("Could not access to WindowManager without valid Event source");

        Component sourceComponent = (Component) eventSource;

        TopLevelFrame topFrame = DesktopComponentsHelper.getTopLevelFrame(sourceComponent);

        return topFrame.getWindowManager();
    }
}