/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.global;

import com.haulmont.cuba.core.sys.AppContext;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Use static methods of this class to process Freemarker templates.
 * Does not cache templates.
 */
public class TemplateHelper {

    public static String processTemplate(String templateStr, Map<String, Object> parameterValues) {
        final StringTemplateLoader templateLoader = new StringTemplateLoader();
        templateLoader.putTemplate("template", templateStr);
        return __processTemplate(templateLoader, "template", parameterValues);
    }

    public static String processTemplateFromFile(String templatePath, Map<String, Object> parameterValues) {
        final FileTemplateLoader templateLoader;
        try {
            String rootPath = AppContext.getProperty("cuba.templateRootDir");
            if (StringUtils.isEmpty(rootPath)) rootPath = AppContext.getProperty("cuba.confDir");
            templateLoader = new FileTemplateLoader(new File(rootPath), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return __processTemplate(templateLoader, templatePath, parameterValues);
    }

    protected static String __processTemplate(TemplateLoader templateLoader, String templateName, Map<String, Object> parameterValues) {
        Map<String, Object> params = prepareParams(parameterValues);

        final StringWriter writer = new StringWriter();

        try {
            final Configuration configuration = new Configuration();
            configuration.setTemplateLoader(templateLoader);
            final Template template = configuration.getTemplate(templateName);
            template.process(params, writer);

            return writer.toString();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> prepareParams(Map<String, Object> parameterValues) {
        Map<String, Object> params = new HashMap<String, Object>(parameterValues);
        params.put("statics", BeansWrapper.getDefaultInstance().getStaticModels());

        for (String name : AppContext.getPropertyNames()) {
            params.put(name.replace(".", "_"), AppContext.getProperty(name));
        }
        return params;
    }


}
