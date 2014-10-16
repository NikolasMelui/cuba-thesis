/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.web.sys.LinkHandler;
import com.haulmont.cuba.gui.TestIdManager;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.server.*;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Locale;
import java.util.Map;

/**
 * Single window / page of web application. Root component of Vaadin layout.
 *
 * @author artamonov
 * @version $Id$
 */
@PreserveOnRefresh
public class AppUI extends UI implements ErrorHandler {

    public static final String APPLICATION_CLASS_CONFIG_KEY = "Application";

    public static final String LAST_REQUEST_ACTION_ATTR = "lastRequestAction";

    public static final String LAST_REQUEST_PARAMS_ATTR = "lastRequestParams";

    private final static Log log = LogFactory.getLog(AppUI.class);

    protected final App app;

    protected boolean applicationInitRequired = false;

    protected TestIdManager testIdManager = new TestIdManager();

    protected boolean testMode = false;

    public AppUI() {
        log.trace("Creating UI " + this);
        if (!App.isBound()) {
            app = createApplication();

            VaadinSession vSession = VaadinSession.getCurrent();
            vSession.setAttribute(App.class, app);

            // set root error handler for all session
            vSession.setErrorHandler(new ErrorHandler() {
                @Override
                public void error(com.vaadin.server.ErrorEvent event) {
                    try {
                        app.getExceptionHandlers().handle(event);
                        app.getAppLog().log(event);
                    } catch (Throwable e) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        log.error("Error handling exception\nOriginal exception:\n"
                                        + ExceptionUtils.getStackTrace(event.getThrowable())
                                        + "\nException in handlers:\n"
                                        + ExceptionUtils.getStackTrace(e)
                        );
                    }
                }
            });

            applicationInitRequired = true;
        } else {
            app = App.getInstance();
        }

        Configuration configuration = AppBeans.get(Configuration.NAME);
        testMode = configuration.getConfig(GlobalConfig.class).getTestMode();

        // do not grab focus
        setTabIndex(-1);

        initJsLibraries();
    }

    /**
     * Dynamically init external JS libraries.
     * You should create JavaScriptExtension class and extend UI object here. <br/>
     *
     * Example: <br/>
     * <pre><code>
     * JavaScriptExtension:
     *
     * {@literal @}JavaScript("resources/jquery/jquery-1.10.2.min.js")
     * public class JQueryIntegration extends AbstractJavaScriptExtension {
     *
     *     {@literal @}Override
     *     public void extend(AbstractClientConnector target) {
     *         super.extend(target);
     *     }
     *
     *     {@literal @}Override
     *     protected Class&lt;? extends ClientConnector&gt; getSupportedParentType() {
     *         return UI.class;
     *     }
     * }
     *
     * AppUI:
     *
     * protected void initJsLibraries() {
     *     new JQueryIntegration().extend(this);
     * }</code></pre>
     *
     * If you want to include scripts to generated page statically see {@link com.haulmont.cuba.web.sys.CubaBootstrapListener}.
     */
    protected void initJsLibraries() {
    }

    protected App createApplication() {
        String applicationClass = getApplicationClass();
        App application;
        try {
            Class<?> aClass = getClass().getClassLoader().loadClass(applicationClass);
            application = (App) aClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new Error(String.format("Unable to create application '%s'", applicationClass), e);
        }

        return application;
    }

    protected String getApplicationClass() {
        DeploymentConfiguration vConf = VaadinService.getCurrent().getDeploymentConfiguration();
        return vConf.getApplicationOrSystemProperty(APPLICATION_CLASS_CONFIG_KEY,
                DefaultApp.class.getCanonicalName());
    }

    @Override
    protected void init(VaadinRequest request) {
        log.debug("Initializing AppUI");
        if (applicationInitRequired) {
            app.init();

            MessageTools messageTools = AppBeans.get(MessageTools.NAME);
            Locale locale = messageTools.trimLocale(request.getLocale());
            app.setLocale(locale);

            applicationInitRequired = false;
        }
        // init error handlers
        setErrorHandler(this);
        // open login or main window
        app.initView(this);

        processExternalLink(request);
    }

    @Override
    public void handleRequest(VaadinRequest request) {
        processExternalLink(request);
    }

    public void showView(UIView view) {
        setContent(view);
        getPage().setTitle(view.getTitle());
    }

    /**
     * @return current AppUI
     */
    public static AppUI getCurrent() {
        return (AppUI) UI.getCurrent();
    }

    /**
     * @return this App instance
     */
    public App getApp() {
        return app;
    }

    /**
     * @return AppWindow instance or null if not logged in
     */
    public AppWindow getAppWindow() {
        Component currentUIView = getContent();
        if (currentUIView instanceof AppWindow) {
            return (AppWindow) currentUIView;
        } else {
            return null;
        }
    }

    public TestIdManager getTestIdManager() {
        return testIdManager;
    }

    public boolean isTestMode() {
        return testMode;
    }

    @Override
    public void error(com.vaadin.server.ErrorEvent event) {
        try {
            app.getExceptionHandlers().handle(event);
            app.getAppLog().log(event);
        } catch (Throwable e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            log.error("Error handling exception\nOriginal exception:\n"
                    + ExceptionUtils.getStackTrace(event.getThrowable())
                    + "\nException in handlers:\n"
                    + ExceptionUtils.getStackTrace(e)
            );
        }
    }

    public void processExternalLink(VaadinRequest request) {
        String action = (String) request.getWrappedSession().getAttribute(LAST_REQUEST_ACTION_ATTR);

        Configuration configuration = AppBeans.get(Configuration.NAME);
        WebConfig webConfig = configuration.getConfig(WebConfig.class);
        if (webConfig.getLinkHandlerActions().contains(action)) {
            //noinspection unchecked
            Map<String, String> params =
                    (Map<String, String>) request.getWrappedSession().getAttribute(LAST_REQUEST_PARAMS_ATTR);
            if (params == null) {
                log.warn("Unable to process the external link: lastRequestParams not found in session");
                return;
            }
            LinkHandler linkHandler = AppBeans.getPrototype(LinkHandler.NAME, app, action, params);
            if (app.connection.isConnected()) {
                linkHandler.handle();
            } else {
                app.linkHandler = linkHandler;
            }
        }
    }

    @Override
    public void detach() {
        log.trace("Detaching UI " + this);
        super.detach();
    }
}