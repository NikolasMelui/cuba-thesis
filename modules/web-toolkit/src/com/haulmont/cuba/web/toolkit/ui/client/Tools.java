/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.haulmont.cuba.web.toolkit.ui.client.sys.ToolsImpl;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ui.VOverlay;

/**
 * @author gorodnov
 * @version $Id$
 */
public class Tools {
    private static ToolsImpl impl;

    static {
        impl = GWT.create(ToolsImpl.class);
    }

    public static void textSelectionEnable(Element el, boolean b) {
        impl.textSelectionEnable(el, b);
    }

    public static String setStyleName(Element el, String style) {
        if (style == null) {
            throw new RuntimeException("Style cannot be null");
        }
        style = style.trim();
        el.setPropertyString("className", style);
        return style;
    }

    public static String addStyleName(Element el, String style) {
        if (style == null) {
            throw new RuntimeException("Style cannot be null");
        }
        style = style.trim();
        el.addClassName(style);
        return style;
    }

    public static void removeStyleName(Element el, String style) {
        if (style == null) {
            throw new RuntimeException("Style cannot be null");
        }
        style = style.trim();
        el.removeClassName(style);
    }

    public static String getStylePrimaryName(Element el) {
        String className = el.getClassName();
        int spaceIdx = className.indexOf(' ');
        if (spaceIdx >= 0) {
            return className.substring(0, spaceIdx);
        }
        return className;
    }

    public static String addStyleDependentName(Element el, String styleSuffix) {
        String s = getStylePrimaryName(el) + '-' + styleSuffix;
        addStyleName(el, s);
        return s;
    }

    public static void removeStyleDependentName(Element el, String styleSuffix) {
        removeStyleName(el, getStylePrimaryName(el) + '-' + styleSuffix);
    }

    public static void replaceClassNames(Element element, String from, String to) {
        String className = element.getClassName();
        String newClassName = "";
        String[] classNames = className.split(" ");
        for (String classNamePart : classNames) {
            if (classNamePart.startsWith(from + "-")) {
                classNamePart = classNamePart.replace(from + "-", to + "-");
            } else if (classNamePart.equals(from)) {
                classNamePart = to;
            }

            newClassName = newClassName + " " + classNamePart;
        }
        element.setClassName(newClassName.trim());
    }

    public static void fixFlashTitleIE() {
        // if url has '#' then title changed in ie8 after flash loaded. This fix changed set normal title
        if (BrowserInfo.get().isIE()) {
            impl.fixFlashTitleIEJS();
        }
    }
    public static void showContextPopup(VOverlay customContextMenuPopup, int left, int top) {
        customContextMenuPopup.setAutoHideEnabled(true);
        customContextMenuPopup.setVisible(false);
        customContextMenuPopup.show();

        // mac FF gets bad width due GWT popups overflow hacks,
        // re-determine width
        int offsetWidth = customContextMenuPopup.getOffsetWidth();
        int offsetHeight = customContextMenuPopup.getOffsetHeight();
        if (offsetWidth + left > Window.getClientWidth()) {
            left = left - offsetWidth;
            if (left < 0) {
                left = 0;
            }
        }
        if (offsetHeight + top > Window.getClientHeight()) {
            top = top - offsetHeight;
            if (top < 0) {
                top = 0;
            }
        }

        customContextMenuPopup.setPopupPosition(left, top);
        customContextMenuPopup.setVisible(true);
    }
}