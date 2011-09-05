/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.app.core.credits;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.core.global.ScriptingProvider;
import com.haulmont.cuba.core.sys.AppContext;
import freemarker.template.utility.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import java.util.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class CreditsLoader {

    private Log log = LogFactory.getLog(getClass());

    private List<CreditsItem> items = new ArrayList<CreditsItem>();

    private Map<String, String> licenses = new HashMap<String, String>();

    public List<CreditsItem> getItems() {
        return items;
    }

    public CreditsLoader load() {
        String configProperty = AppContext.getProperty("cuba.creditsConfig");
        if (StringUtils.isBlank(configProperty)) {
            log.info("Property cuba.creditsConfig is empty");
            return this;
        }

        StrTokenizer tokenizer = new StrTokenizer(configProperty);
        String[] locations = tokenizer.getTokenArray();

        for (String location : locations) {
            String xml = ScriptingProvider.getResourceAsString(location);
            if (xml == null) {
                log.debug("Resource " + location + " not found, ignore it");
                continue;
            }
            Element rootElement = Dom4j.readDocument(xml).getRootElement();
            loadLicenses(rootElement);
            loadConfig(rootElement);
        }

        Collections.sort(items);

        return this;
    }

    private void loadLicenses(Element rootElement) {
        Element licensesEl = rootElement.element("licenses");
        for (Element element : Dom4j.elements(licensesEl)) {
            licenses.put(element.attributeValue("id"), element.getText());
        }
    }

    private void loadConfig(Element rootElement) {
        Element itemsEl = rootElement.element("items");
        if (itemsEl == null)
            return;

        for (Element element : Dom4j.elements(itemsEl)) {
            CreditsItem item = new CreditsItem(element.attributeValue("name"), element.attributeValue("web"), loadLicense(element));
            if (items.contains(item)) {
                items.set(items.indexOf(item), item);
            } else {
                items.add(item);
            }
        }
    }

    private String loadLicense(Element element) {
        String licenseRef = element.attributeValue("license");
        if (licenseRef != null) {
            String license = licenses.get(licenseRef);
            if (license == null)
                throw new IllegalStateException("License text for " + licenseRef + " not found");
            return license;
        } else {
            Element licenseEl = element.element("license");
            if (licenseEl == null)
                throw new IllegalStateException("Neither license attribute, nor license element is not set for " + element.attributeValue("name"));
            return licenseEl.getText();
        }
    }
}
