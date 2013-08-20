/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.logging.UIPerformanceLogger;
import com.haulmont.cuba.gui.xml.DeclarativeShortcutAction;
import com.haulmont.cuba.gui.xml.XmlInheritanceProcessor;
import com.haulmont.cuba.gui.xml.data.DsContextLoader;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author abramov
 * @version $Id$
 */
public class FrameLoader extends ContainerLoader implements ComponentLoader {

    public FrameLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent)
            throws InstantiationException, IllegalAccessException {

        final Map<String, Object> params = context.getParams();
        XmlInheritanceProcessor processor = new XmlInheritanceProcessor(element.getDocument(), params);
        element = processor.getResultRoot();

        ComponentLoaderContext parentContext = (ComponentLoaderContext) getContext();

        IFrame component = factory.createComponent("iframe");

        WindowCreationHelper.deployViews(element);

        final Element dsContextElement = element.element("dsContext");
        final DsContext dsContext;

        if (dsContextElement != null) {
            final DsContextLoader contextLoader =
                    new DsContextLoader(context.getDsContext().getDataSupplier());

            dsContext = contextLoader.loadDatasources(dsContextElement, parentContext.getDsContext());
        } else {
            dsContext = null;
        }
        ComponentLoaderContext newContext = new ComponentLoaderContext(
                dsContext == null ? parentContext.getDsContext() : dsContext,
                params);

        String frameId = parentContext.getCurrentIFrameId();
        if (parentContext.getFullFrameId() != null)
            frameId = parentContext.getFullFrameId() + "." + frameId;

        newContext.setFullFrameId(frameId);
        newContext.setFrame(component);
        newContext.setParent(parentContext);
        setContext(newContext);

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);
        loadStyleName(component, element);
        loadMessagesPack(component, element);
        loadActions(component, element);

        final Element layoutElement = element.element("layout");
        if (layoutElement == null)
            throw new DevelopmentException("Required element not found: layout", context.getFullFrameId());

        loadSubComponentsAndExpand(component, layoutElement);
        loadSpacing(component, layoutElement);
        loadMargin(component, layoutElement);
        loadWidth(component, layoutElement);
        loadHeight(component, layoutElement);
        loadStyleName(component, layoutElement);

        FrameContext frameContext = new FrameContext(component, params);
        component.setContext(frameContext);

        if (dsContext != null) {
            component.setDsContext(dsContext);

            for (Datasource ds : dsContext.getAll()) {
                if (ds instanceof DatasourceImplementation) {
                    ((DatasourceImplementation) ds).initialized();
                }
            }

            dsContext.setWindowContext(frameContext);
        }
        component = wrapByCustomClass(component, element, params, parentContext);

        parentContext.getPostInitTasks().addAll(newContext.getPostInitTasks());

        return component;
    }

    protected IFrame wrapByCustomClass(IFrame frame, Element element, Map<String, Object> params,
                                       ComponentLoaderContext parentContext) {
        final String screenClass = element.attributeValue("class");
        if (!StringUtils.isBlank(screenClass)) {
            try {
                Class<Window> aClass = scripting.loadClass(screenClass);
                if (aClass == null)
                    aClass = ReflectionHelper.getClass(screenClass);
                IFrame wrappingFrame = ((WrappedFrame) frame).wrapBy(aClass);

                String loggingId = context.getFullFrameId();

                if (wrappingFrame instanceof AbstractFrame) {
                    Element companionsElem = element.element("companions");
                    if (companionsElem != null) {
                        StopWatch companionStopWatch = new Log4JStopWatch(loggingId + "#" +
                                UIPerformanceLogger.LifeCycle.COMPANION,
                                Logger.getLogger(UIPerformanceLogger.class));
                        companionStopWatch.start();

                        initCompanion(companionsElem, (AbstractFrame) wrappingFrame);

                        companionStopWatch.stop();
                    }
                }
                parentContext.addPostInitTask(new FrameLoaderPostInitTask(wrappingFrame, params, true));

                StopWatch injectStopWatch = new Log4JStopWatch(loggingId + "#" +
                        UIPerformanceLogger.LifeCycle.INJECTION,
                        Logger.getLogger(UIPerformanceLogger.class));
                injectStopWatch.start();

                ControllerDependencyInjector dependencyInjector = new ControllerDependencyInjector(wrappingFrame, params);
                dependencyInjector.inject();

                injectStopWatch.stop();

                return wrappingFrame;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            parentContext.addPostInitTask(new FrameLoaderPostInitTask(frame, params, false));
            return frame;
        }
    }

    protected void initCompanion(Element companionsElem, AbstractFrame frame) {
        Element element = companionsElem.element(AppConfig.getClientType().toString().toLowerCase());
        if (element != null) {
            String className = element.attributeValue("class");
            if (!StringUtils.isBlank(className)) {
                Class aClass = scripting.loadClass(className);
                if (aClass == null)
                    throw new IllegalStateException("Class " + className + " is not found");
                Object companion;
                try {
                    companion = aClass.newInstance();
                    frame.setCompanion(companion);

                    CompanionDependencyInjector cdi = new CompanionDependencyInjector(frame, companion);
                    cdi.inject();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected <T> T invokeMethod(IFrame frame, String name, Object... params)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        List<Class> paramClasses = new ArrayList<>();
        for (Object param : params) {
            if (param == null) throw new IllegalStateException("Null parameter");

            final Class aClass = param.getClass();
            if (List.class.isAssignableFrom(aClass)) {
                paramClasses.add(List.class);
            } else if (Set.class.isAssignableFrom(aClass)) {
                paramClasses.add(Set.class);
            } else if (Map.class.isAssignableFrom(aClass)) {
                paramClasses.add(Map.class);
            } else {
                paramClasses.add(aClass);
            }
        }

        final Class<? extends IFrame> aClass = frame.getClass();
        Method method;
        try {
            method = aClass.getDeclaredMethod(name, paramClasses.toArray(new Class<?>[paramClasses.size()]));
        } catch (NoSuchMethodException e) {
            method = aClass.getMethod(name, paramClasses.toArray(new Class<?>[paramClasses.size()]));
        }
        method.setAccessible(true);
        //noinspection unchecked
        return (T) method.invoke(frame, params);
    }

    protected void loadMessagesPack(IFrame frame, Element element) {
        String msgPack = element.attributeValue("messagesPack");
        if (msgPack != null) {
            frame.setMessagesPack(msgPack);
            setMessagesPack(msgPack);
        } else {
            frame.setMessagesPack(msgPack);
            setMessagesPack(this.messagesPack);
        }
    }

    @Override
    protected Action loadDeclarativeAction(Component.ActionsHolder actionsHolder, Element element) {
        String shortcut = element.attributeValue("shortcut");
        if (!StringUtils.isBlank(shortcut)) {
            String id = element.attributeValue("id");
            if (id == null)
                throw new IllegalStateException("No action id provided");

            return new DeclarativeShortcutAction(
                    id,
                    loadResourceString(element.attributeValue("caption")),
                    loadResourceString(element.attributeValue("icon")),
                    element.attributeValue("enable"),
                    element.attributeValue("visible"),
                    element.attributeValue("invoke"),
                    shortcut,
                    actionsHolder
            );
        } else {
            return super.loadDeclarativeAction(actionsHolder, element);
        }
    }

    private class FrameLoaderPostInitTask implements PostInitTask {

        private IFrame frame;
        private Map<String, Object> params;
        private boolean wrapped;

        public FrameLoaderPostInitTask(IFrame frame, Map<String, Object> params, boolean wrapped) {
            this.frame = frame;
            this.params = params;
            this.wrapped = wrapped;
        }

        @Override
        public void execute(Context context, IFrame window) {
            if (wrapped) {
                String loggingId = ComponentsHelper.getFullFrameId(this.frame);
                StopWatch initStopWatch = new Log4JStopWatch(loggingId + "#" +
                        UIPerformanceLogger.LifeCycle.INIT,
                        Logger.getLogger(UIPerformanceLogger.class));
                initStopWatch.start();

                try {
                    ReflectionHelper.invokeMethod(this.frame, "init", params);
                } catch (NoSuchMethodException e) {
                    // do nothing
                }

                initStopWatch.stop();

                StopWatch uiPermissionsWatch = new Log4JStopWatch(loggingId + "#" +
                        UIPerformanceLogger.LifeCycle.UI_PERMISSIONS,
                        Logger.getLogger(UIPerformanceLogger.class));
                uiPermissionsWatch.start();

                // apply ui permissions
                WindowCreationHelper.applyUiPermissions(window);

                uiPermissionsWatch.stop();

                FrameLoader.this.context.executePostInitTasks();
            }
        }
    }
}
