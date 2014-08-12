/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.toolkit.gwt.client.impl;

import com.google.gwt.dom.client.Element;

public class ToolsImpl {

    public native int parseSize(String s) /*-{
        try {
            var result = /^(\d+)(%|px|em|ex|in|cm|mm|pt|pc)$/.exec(s);
            return parseInt(result[1]);
        } catch (e) {
            return -1;
        }
    }-*/;

    public native String format(String message) /*-{
        return message.replace(/\[br\]/g, "<br/>")
                .replace(/\[b\]/g, "<b>")
                .replace(/\[\/b\]/g, "</b>")
                .replace(/\[i\]/g, "<i>")
                .replace(/\[\/i\]/g, "</i>");
    }-*/;

    public native void setInnerHTML(Element elem, String text) /*-{
        elem.innerHTML = text; //todo gorodnov: support line breaks
    }-*/;

    public native void setInnerText(Element elem, String text) /*-{
        while (elem.firstChild) {
            elem.removeChild(elem.firstChild);
        }
        if (text != null) {
            var arr = new Array();
            arr = text.replace(/\r/g, "").split("\n");
            if (arr.length > 0) {
                for (var i = 0; i < arr.length; i++) {
                    if (arr[i]) {
                        elem.appendChild($doc.createTextNode(arr[i]));
                        elem.appendChild($doc.createElement("br"));
                    }
                }
            } else {
                elem.appendChild($doc.createTextNode(text));
            }
        }
    }-*/;

    public native boolean isRadio(Element e) /*-{
        return (e && e.tagName.toUpperCase() == "INPUT" && e.type == "radio");
    }-*/;

    public native boolean isCheckbox(Element e) /*-{
        return (e && e.tagName.toUpperCase() == "INPUT" && e.type == "checkbox");
    }-*/;

    public native void alert(String msg)/*-{
        $wnd.alert(msg);
    }-*/;

    public native void textSelectionEnable(Element el, boolean b) /*-{

        // DISCLAIMER: Do not use jQuery disable text selection pack, it caches html nodes and we have memory leaks

        if (typeof document.falseFunction != "function") {
            document.falseFunction = function() {
                return false;
            };
        }

        var walkEach = function(element, action) {
            if (typeof element != "undefined") {
                action(element);
                var children = element.childNodes;
                if (typeof children != "undefined") {
                    for (var i = 0; i < children.length; i++)
                        if (children[i].nodeType == 1)
                            walkEach(children[i], action);
                }
            }
        };

        if (!b) {
            // disable
            if ($wnd.jQuery.browser.mozilla) {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.style.MozUserSelect = "none";
                    x.style.setProperty("user-select", "none");
                });
            } else if ($wnd.jQuery.browser.msie) {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.onselectstart = document.falseFunction;
                    if (x.style.setProperty) {
                        x.style.setProperty("user-select", "none");
                    } else {
                        x.style.setAttribute("user-select", "none");
                    }
                });
            } else if ($wnd.jQuery.browser.webkit) {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.style.webkitUserSelect = "none";
                    x.style.setProperty("user-select", "none");
                });
            } else {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.addEventListener("selectstart", document.falseFunction, true);
                    x.style.setProperty("user-select", "none");
                });
            }
        } else {
            // enable
            if ($wnd.jQuery.browser.mozilla) {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.style.MozUserSelect = "";
                    x.style.setProperty("user-select", "");
                });
            } else if ($wnd.jQuery.browser.msie) {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.onselectstart = null;
                    if (x.style.setProperty) {
                        x.style.setProperty("user-select", "");
                    } else {
                        x.style.setAttribute("user-select", "");
                    }
                });
            } else if ($wnd.jQuery.browser.webkit) {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.style.webkitUserSelect = "";
                    x.style.setProperty("user-select", "");
                });
            } else {
                walkEach(el, function(x) {
                    if (typeof x.style == "undefined")
                        x.style = {};
                    x.removeEventListener("selectstart", document.falseFunction, true);
                    x.style.setProperty("user-select", "");
                });
            }
        }

        walkEach = null;
    }-*/;

    public native void removeElementWithEvents(Element el) /*-{
        $wnd.jQuery(el).remove();
    }-*/;

    public native void updatePrimaryAndDependentStyleNames(Element elem,
                                                           String newPrimaryStyle) /*-{
        var classes = elem.className.split(/\s+/);
        if (!classes) {
            return;
        }

        var oldPrimaryStyle = classes[0];
        var oldPrimaryStyleLen = oldPrimaryStyle.length;

        classes[0] = newPrimaryStyle;
        for (var i = 1, n = classes.length; i < n; i++) {
            var name = classes[i];
            if (name.length > oldPrimaryStyleLen
                    && name.charAt(oldPrimaryStyleLen) == '-'
                    && name.indexOf(oldPrimaryStyle) == 0) {
                classes[i] = newPrimaryStyle + name.substring(oldPrimaryStyleLen);
            }
        }
        elem.className = classes.join(" ");
    }-*/;

    public native boolean hasStyleName(Element el, String style) /*-{
        var classes = elem.className.split(/\s+/);
        if (!classes) {
            return false;
        }
        for (var i = 0; i < classes.length; i++) {
            if (classes[i] == style) return true;
        }
        return false;
    }-*/;

    public native void fixFlashTitleIEJS() /*-{
        var originalTitle = $doc.title.split("#")[0];
        var fixFlashTitleIEWorking = false;
        $doc.attachEvent('onpropertychange', function (evt) {
            if(evt.propertyName == 'title' && $doc.title != originalTitle && !fixFlashTitleIEWorking) {
                fixFlashTitleIEWorking = true;
                $doc.title = originalTitle;
                fixFlashTitleIEWorking = false;
            }
        });
    }-*/;
}
