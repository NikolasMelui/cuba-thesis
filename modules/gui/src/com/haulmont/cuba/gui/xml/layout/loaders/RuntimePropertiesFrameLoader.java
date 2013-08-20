/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.logging.UIPerformanceLogger;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import java.io.InputStream;
import java.util.Collections;

/**
 * @author devyatkin
 * @version $Id$
 */
public class RuntimePropertiesFrameLoader extends IFrameLoader {

    private static final String DEFAULT_DESCRIPTOR = "/com/haulmont/cuba/gui/runtimeprops/runtime-properties-frame.xml";

    public RuntimePropertiesFrameLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent)
            throws InstantiationException, IllegalAccessException {

        String src = element.attributeValue("src");
        if (src == null)
            src = DEFAULT_DESCRIPTOR;
        String runtimeDs = element.attributeValue("runtimeDs");
        context.getParams().put("runtimeDs", runtimeDs);
        String categoriesDs = element.attributeValue("categoriesDs");
        context.getParams().put("categoriesDs", categoriesDs);
        String rows = element.attributeValue("rows");
        context.getParams().put("rows", rows);
        String cols = element.attributeValue("cols");
        context.getParams().put("cols", cols);
        String fieldWidth = element.attributeValue("fieldWidth");
        context.getParams().put("fieldWidth", fieldWidth);

        String screenPath = StringUtils.equals(src, DEFAULT_DESCRIPTOR) ? "runtimeProperties" : src;

        if (element.attributeValue("id") != null)
            screenPath = element.attributeValue("id");

        String frameId = screenPath;

        if (context.getFrame() != null) {
            String parentId = context.getFullFrameId();
            if (StringUtils.isNotEmpty(parentId))
                screenPath = parentId + "." + screenPath;
        }

        StopWatch loadDescriptorWatch = new Log4JStopWatch(screenPath + "#" +
                UIPerformanceLogger.LifeCycle.LOAD_DESCRIPTOR,
                Logger.getLogger(UIPerformanceLogger.class));
        loadDescriptorWatch.start();

        final LayoutLoader loader = new LayoutLoader(context, factory, LayoutLoaderConfig.getFrameLoaders());
        loader.setLocale(getLocale());
        loader.setMessagesPack(getMessagesPack());

        InputStream stream = resources.getResourceAsStream(src);
        if (stream == null) {
            stream = getClass().getResourceAsStream(src);
            if (stream == null)
                throw new DevelopmentException("Bad template path", context.getFullFrameId(),
                        Collections.<String,Object>singletonMap("Src", src));
        }

        final IFrame component;
        try {
            context.setCurrentIFrameId(frameId);
            component = (IFrame) loader.loadComponent(stream, parent, context.getParams());
        } finally {
            IOUtils.closeQuietly(stream);
        }
        if (component.getMessagesPack() == null) {
            component.setMessagesPack(messagesPack);
        }

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);

        loadAlign(component, element);

        loadHeight(component, element, ComponentsHelper.getComponentHeigth(component));
        loadWidth(component, element, ComponentsHelper.getComponentWidth(component));

        if (context.getFrame() != null)
            component.setFrame(context.getFrame());

        return component;
    }
}
