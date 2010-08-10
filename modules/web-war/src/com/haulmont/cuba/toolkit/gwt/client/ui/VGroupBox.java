/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 06.08.2010 15:08:26
 *
 * $Id$
 */
package com.haulmont.cuba.toolkit.gwt.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.*;
import com.vaadin.terminal.gwt.client.ui.VPanel;

public class VGroupBox extends VPanel {

    public static final String CLASSNAME = "group-box";

    private int contentNodeBorderPaddingsHor = -1;
    private int contentNodeBorderPaddingsVer = -1;

    protected Element expander;

    protected boolean expanded;
    protected boolean collapsable;

    @Override
    protected void constructDOM() {
        Element fieldSet = DOM.createFieldSet();
        Element legend = DOM.createLegend();
        captionNode = DOM.createDiv();
        expander = DOM.createDiv();
        captionText = DOM.createSpan();
        bottomDecoration = DOM.createDiv();
        contentNode = DOM.createDiv();

        captionNode.setClassName(CLASSNAME + "-caption");
        contentNode.setClassName(CLASSNAME + "-content");
        bottomDecoration.setClassName(CLASSNAME + "-deco");

        captionNode.appendChild(expander);
        captionNode.appendChild(captionText);

        legend.appendChild(captionNode);

        fieldSet.appendChild(legend);
        fieldSet.appendChild(contentNode);
        fieldSet.appendChild(bottomDecoration);
        getElement().appendChild(fieldSet);

        setStyleName(CLASSNAME);

        DOM.sinkEvents(getElement(), Event.ONKEYDOWN);
        DOM.sinkEvents(contentNode, Event.ONSCROLL);

        DOM.sinkEvents(expander, Event.ONCLICK);

        contentNode.getStyle().setProperty("position", "relative");
        getElement().getStyle().setProperty("overflow", "hidden");
    }

    @Override
    protected void renderContent(UIDL uidl) {
        final UIDL layoutUidl = uidl.getChildUIDL(0);
        if (layoutUidl != null) {
            final Paintable newLayout = client.getPaintable(layoutUidl);
            if (newLayout != layout) {
                if (layout != null) {
                    client.unregisterPaintable(layout);
                }
                setWidget((Widget) newLayout);
                layout = newLayout;
            }
            layout.updateFromUIDL(layoutUidl, client);
        }
    }

    @Override
    protected void renderDOM(UIDL uidl) {
        collapsable = uidl.getBooleanAttribute("collapsable");
        if (collapsable) {
            DOM.setStyleAttribute(expander, "display", "");
            removeStyleDependentName("nocollapsable");
        } else {
            addStyleDependentName("nocollapsable");
            DOM.setStyleAttribute(expander, "display", "none");
        }
        if (uidl.getBooleanAttribute("expanded") != expanded) {
            toggleExpand();
        }

        // Handle caption displaying and style names, prior generics.
        // Affects size
        // calculations

        boolean hasCaption = false;
        if (uidl.hasAttribute("caption")
                && !uidl.getStringAttribute("caption").equals("")) {
            setCaption(uidl.getStringAttribute("caption"));
            hasCaption = true;
        } else {
            setCaption("");
        }

        if (!hasCaption) {
            addStyleDependentName("nocaption");
        } else {
            removeStyleDependentName("nocaption");
        }
    }

    protected void toggleExpand() {
        expanded = !expanded;
        if (expanded) {
            captionNode.addClassName("expanded");
        } else {
            captionNode.removeClassName("expanded");
        }
    }

    @Override
    protected int getCaptionPaddingHorizontal() {
        return 0;
    }

    @Override
    protected void detectContainerBorders() {
        String oldWidth = DOM.getStyleAttribute(contentNode, "width");
        String oldHeight = DOM.getStyleAttribute(contentNode, "height");

        DOM.setStyleAttribute(contentNode, "overflow", "hidden");

        DOM.setStyleAttribute(contentNode, "width", "0px");
        DOM.setStyleAttribute(contentNode, "height", "0px");

        borderPaddingHorizontal = contentNodeBorderPaddingsHor = contentNode.getOffsetWidth();
        borderPaddingVertical = contentNodeBorderPaddingsVer = contentNode.getOffsetHeight();

        DOM.setStyleAttribute(contentNode, "width", oldWidth);
        DOM.setStyleAttribute(contentNode, "height", oldHeight);

        DOM.setStyleAttribute(contentNode, "overflow", "auto");

        Element fieldsetElement = DOM.getParent(contentNode);
        DOM.setStyleAttribute(fieldsetElement, "overflow", "hidden");

        DOM.setStyleAttribute(fieldsetElement, "width", "0px");
        DOM.setStyleAttribute(fieldsetElement, "height", "0px");

        borderPaddingHorizontal += fieldsetElement.getOffsetWidth();
        borderPaddingVertical += fieldsetElement.getOffsetHeight();

        DOM.setStyleAttribute(fieldsetElement, "width", "");
        DOM.setStyleAttribute(fieldsetElement, "height", "");

        DOM.setStyleAttribute(fieldsetElement, "overflow", "visible");
    }

    protected int getContentNodeBorderPaddingsWidth() {
        if (contentNodeBorderPaddingsHor < 0) {
            detectContainerBorders();
        }
        return contentNodeBorderPaddingsHor;
    }

    protected int getContentNodeBorderPaddingsHeight() {
        if (contentNodeBorderPaddingsVer < 0) {
            detectContainerBorders();
        }
        return contentNodeBorderPaddingsVer;
    }

    @Override
    public RenderSpace getAllocatedSpace(Widget child) {
        int w = 0;
        int h = 0;

        if (width != null && !width.equals("")) {
            w = contentNode.getOffsetWidth() - getContentNodeBorderPaddingsWidth();
            if (w < 0) {
                w = 0;
            }
        }

        if (height != null && !height.equals("")) {
            h = contentNode.getOffsetHeight() - getContentNodeBorderPaddingsHeight();
            if (h < 0) {
                h = 0;
            }
        }

        return new RenderSpace(w, h, true);
    }

    @Override
    public void runHacks(boolean runGeckoFix) {
        client.runDescendentsLayout(this);

        Util.runWebkitOverflowAutoFix(contentNode);

    }

    @Override
    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONCLICK && DOM.eventGetTarget(event) == expander) {
            toggleExpand();
            if (collapsable) {
                if (expanded) {
                    client.updateVariable(id, "expand", true, true);
                } else {
                    client.updateVariable(id, "collapse", true, true);
                }
            }
            DOM.eventCancelBubble(event, true);
        } else {
            super.onBrowserEvent(event);
        }
    }
}
