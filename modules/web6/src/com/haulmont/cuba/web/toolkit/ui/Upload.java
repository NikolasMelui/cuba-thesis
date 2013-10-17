/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 22.06.2010 18:31:09
 *
 * $Id$
 */
package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.App;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import org.apache.commons.lang.StringUtils;

@SuppressWarnings("serial")
public class Upload extends com.vaadin.ui.Upload {

    private String action;
    private String buttonWidth;
    
    public Upload(String caption, Receiver uploadReceiver) {
        super(caption, uploadReceiver);
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);
        if (getAction() != null) {
            target.addAttribute("action", getAction());
        }
        if (getButtonWidth() != null)
            target.addAttribute("buttonwidth", buttonWidth);

        // use explicit window name in upload URL
        String currentWindowName = App.getInstance().getCurrentWindowName();
        if (StringUtils.isNotEmpty(currentWindowName))
            target.addAttribute("windowName", currentWindowName);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
        requestRepaint();
    }

    public String getButtonWidth() {
        return buttonWidth;
    }

    public void setButtonWidth(String buttonWidth) {
        this.buttonWidth = buttonWidth;
        requestRepaint();
    }
}