/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 31.08.2010 9:57:21
 *
 * $Id$
 */
package com.haulmont.cuba.toolkit.gwt.client.charts;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Window;

import java.util.HashSet;
import java.util.Set;

public class ResourcesLoader {

    private static Set<String> alreadyInjected;

    public static boolean injectCss(String host, String appUri, String path) {
        //todo gorodnov: implement this method
        return false;
    }

    public static boolean injectJs(String host, String appUri, String path) {
        appUri = (appUri == null) ? "" : appUri;
        path = (path == null) ? "" : path;
        String src = "";
        if (host != null) {
            src = getProtocol() + "//" + host; 
        }
        src += appUri + (appUri.endsWith("/") ? "" : "/") + "VAADIN/resources" + path;
        if (alreadyInjected != null && alreadyInjected.contains(src)) {
            return true;
        }
        Document doc = Document.get();
        ScriptElement script = doc.createScriptElement();
        script.setSrc(src + (src.contains("?") ? "&" : "?") + System.currentTimeMillis());
        script.setType("text/javascript");

        HeadElement head = doc.getElementsByTagName("head").getItem(0).cast();
        head.appendChild(script);

        if (alreadyInjected == null) {
            alreadyInjected = new HashSet<String>();
        }
        alreadyInjected.add(src);
        return false;
    }

    private static String getProtocol() {
        if (Window.Location.getProtocol().equals("https:")) {
          return "https:";
        }
        return "http:";
    }

}
