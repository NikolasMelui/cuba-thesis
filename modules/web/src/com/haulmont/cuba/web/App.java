/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 03.12.2008 14:37:46
 *
 * $Id$
 */
package com.haulmont.cuba.web;

import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.ServiceLocator;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.exception.*;
import com.haulmont.cuba.web.log.AppLog;
import com.haulmont.cuba.web.sys.ActiveDirectoryHelper;
import com.haulmont.cuba.web.sys.LinkHandler;
import com.haulmont.cuba.web.toolkit.Timer;
import com.vaadin.Application;
import com.vaadin.service.ApplicationContext;
import com.vaadin.terminal.Terminal;
import com.vaadin.terminal.gwt.server.AbstractApplicationServlet;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.ui.Window;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class of the web application. Each client connection has its own App.
 * Use {@link #getInstance()} static method to obtain the reference to the current App instance
 * throughout the application code.
 * <p>
 * Specific application should inherit from this class and set derived class name
 * in <code>application</code> servlet parameter of <code>web.xml</code>
 */
public abstract class App extends Application
        implements ApplicationContext.TransactionListener, HttpServletRequestListener {
    private static final long serialVersionUID = -3435976475534930050L;

    public static final Pattern WIN_PATTERN = Pattern.compile("win([0-9]{1,4})");

//    private static final Pattern BAD_WIN_PATTERN = Pattern.compile("win([0-9]{1,4})_.+");

    private static Log log = LogFactory.getLog(App.class);

    public static final String THEME_NAME = "peyto";

    public static final String LAST_REQUEST_PARAMS_ATTR = "lastRequestParams";

    public static final String LAST_REQUEST_ACTION_ATTR = "lastRequestAction";

    public static final List<String> ACTION_NAMES = Arrays.asList("open", "login");

    public static final String USER_SESSION_ATTR = "userSessionId";

    protected Connection connection;
    private WebWindowManager windowManager;

    private AppLog appLog;

    protected ExceptionHandlers exceptionHandlers;

    private static ThreadLocal<App> currentApp = new ThreadLocal<App>();

    protected transient ThreadLocal<String> currentWindowName = new ThreadLocal<String>();

    protected LinkHandler linkHandler;

    protected AppTimers timers;

    protected transient Map<Object, Long> requestStartTimes = new WeakHashMap<Object, Long>();

    private static volatile boolean viewsDeployed;

    private volatile String contextName;

    private transient HttpServletResponse response;

    private transient HttpSession httpSession;

    private AppCookies cookies;

    private BackgroundTaskManager backgroundTaskManager;

    protected boolean testModeRequest = false;

    protected String clientAddress;

    protected WebConfig webConfig;

    static {
        AppContext.setProperty(AppConfig.CLIENT_TYPE_PROP, ClientType.WEB.toString());
    }

    protected App() {
        webConfig = ConfigProvider.getConfig(WebConfig.class);
        appLog = new AppLog();
        connection = createConnection();
        windowManager = createWindowManager();
        exceptionHandlers = new ExceptionHandlers(this);
        cookies = new AppCookies() {
            protected void addCookie(Cookie cookie) {
                response.addCookie(cookie);
            }
        };
        cookies.setCookiesEnabled(true);
        timers = new AppTimers(this);
        backgroundTaskManager = new BackgroundTaskManager();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        currentWindowName = new ThreadLocal<String>();
        requestStartTimes = new WeakHashMap<Object, Long>();
    }

    public void onRequestStart(HttpServletRequest request, HttpServletResponse response) {
        this.response = response;
        cookies.updateCookies(request);
        if (ConfigProvider.getConfig(GlobalConfig.class).getTestMode()) {
            String paramName = webConfig.getTestModeParamName();
            testModeRequest = (paramName == null || request.getParameter(paramName) != null);
        }
    }

    public void onRequestEnd(HttpServletRequest request, HttpServletResponse response) {
        testModeRequest = false;
    }

    public static Application.SystemMessages getSystemMessages() {
        return compileSystemMessages(Locale.getDefault());
    }

    public static CubaSystemMessages compileSystemMessages(Locale locale) {
        CubaSystemMessages msgs = new CubaSystemMessages();

        String webContext = AppContext.getProperty("cuba.webContextName");

        if (AppContext.isStarted()) {
            String messagePack = AppConfig.getMessagesPack();

            msgs.setSessionExpiredCaption(MessageProvider.getMessage(
                    messagePack, "sessionExpiredCaption", locale));
            msgs.setSessionExpiredMessage(MessageProvider.getMessage(
                    messagePack, "sessionExpiredMessage", locale));

            msgs.setCommunicationErrorCaption(MessageProvider.getMessage(
                    messagePack, "communicationErrorCaption", locale));
            msgs.setCommunicationErrorMessage(MessageProvider.getMessage(
                    messagePack, "communicationErrorMessage", locale));

            msgs.setInternalErrorCaption(MessageProvider.getMessage(
                    messagePack, "internalErrorCaption", locale));
            msgs.setInternalErrorMessage(MessageProvider.getMessage(
                    messagePack, "internalErrorMessage", locale));

            msgs.setUiBlockingMessage(MessageProvider.getMessage(
                    messagePack, "uiBlockingMessage", locale));
        }

        msgs.setInternalErrorURL("/" + webContext + "?restartApp");
        msgs.setOutOfSyncNotificationEnabled(false);
        return msgs;
    }

    public static class CubaSystemMessages extends Application.CustomizedSystemMessages {

        private String uiBlockingMessage = "";

        public String getUiBlockingMessage() {
            return uiBlockingMessage;
        }

        public void setUiBlockingMessage(String uiBlockingMessage) {
            this.uiBlockingMessage = uiBlockingMessage;
        }
    }

    protected abstract boolean loginOnStart(HttpServletRequest request);

    protected abstract Connection createConnection();

    /**
     * Can be overridden in descendant to create an application-specific {@link WebWindowManager}
     */
    protected WebWindowManager createWindowManager() {
        return new WebWindowManager(this);
    }

    /**
     * Current App instance. Can be invoked anywhere in application code.
     */
    public static App getInstance() {
        App app = currentApp.get();
        if (app == null)
            throw new IllegalStateException("No App bound to the current thread. This may be the result of hot-deployment.");
        return app;
    }

    public static boolean isBound() {
        return currentApp.get() != null;
    }

    public static String generateWebWindowName() {
        Double d = Math.random() * 10000;
        return "win" + d.intValue();
    }

    /**
     * Can be overridden in descendant to add application-specific exception handlers
     */
    protected void initExceptionHandlers(boolean isConnected) {
        if (isConnected) {
            exceptionHandlers.addHandler(new NoUserSessionHandler()); // must be the first handler
            exceptionHandlers.addHandler(new SilentExceptionHandler());
            exceptionHandlers.addHandler(new UniqueConstraintViolationHandler());
            exceptionHandlers.addHandler(new AccessDeniedHandler());
            exceptionHandlers.addHandler(new NoSuchScreenHandler());
            exceptionHandlers.addHandler(new DeletePolicyHandler());
            exceptionHandlers.addHandler(new NumericOverflowExceptionHandler());
            exceptionHandlers.addHandler(new OptimisticExceptionHandler());
            exceptionHandlers.addHandler(new JPAOptimisticExceptionHandler());
            exceptionHandlers.addHandler(new ReportExceptionHandler());
            exceptionHandlers.addHandler(new FileMissingExceptionHandler());
            exceptionHandlers.addHandler(new InvalidValueExceptionHandler());
        } else {
            exceptionHandlers.getHandlers().clear();
        }
    }

    protected void checkDeployedViews() {
        if (!viewsDeployed) {
            deployViews();
            viewsDeployed = true;
        }
    }

    /**
     * DEPRECATED: use cuba.viewsConfig application property
     */
    @Deprecated
    protected void deployViews() {
    }

    /**
     * Should be overridden in descendant to create an application-specific main window
     */
    protected AppWindow createAppWindow() {
        AppWindow appWindow = new AppWindow(connection);

        Timer timer = createSessionPingTimer(true);
        if (timer != null)
            timers.add(timer, appWindow);

        return appWindow;
    }

    public AppWindow getAppWindow() {
        String name = currentWindowName.get();
        //noinspection deprecation
        Window window = name == null ? getMainWindow() : getWindow(name);
        if (window instanceof AppWindow)
            return (AppWindow) window;
        else
            return null;
    }

    /**
     * Don't use this method in application code.<br>
     * Use {@link #getAppWindow} instead
     */
    @Deprecated
    @Override
    public Window getMainWindow() {
        return super.getMainWindow();
    }

    @Override
    public void removeWindow(Window window) {
        super.removeWindow(window);
        if (window instanceof AppWindow) {
            connection.removeListener((AppWindow) window);
        }
    }

    /**
     * Get current connection object
     */
    public Connection getConnection() {
        return connection;
    }

    public WebWindowManager getWindowManager() {
        return windowManager;
    }

    public AppLog getAppLog() {
        return appLog;
    }

    protected String createWindowName(boolean main) {
        String name = main ? AppContext.getProperty("cuba.web.mainWindowName") : AppContext.getProperty("cuba.web.loginWindowName");
        if (StringUtils.isBlank(name))
            name = generateWebWindowName();
        return name;
    }

    public void userSubstituted(Connection connection) {
    }

    public void terminalError(Terminal.ErrorEvent event) {
        GlobalConfig config = ConfigProvider.getConfig(GlobalConfig.class);
        if (config.getTestMode()) {
            String fileName = AppContext.getProperty("cuba.testModeExceptionLog");
            if (!StringUtils.isBlank(fileName)) {
                try {
                    FileOutputStream stream = new FileOutputStream(fileName);
                    try {
                        stream.write(ExceptionUtils.getStackTrace(event.getThrowable()).getBytes());
                    } finally {
                        stream.close();
                    }
                } catch (Exception e) {
                    log.debug(e);
                }
            }
        }

        if (event instanceof AbstractApplicationServlet.RequestError) {
            log.error("RequestError:", event.getThrowable());
        } else {
            exceptionHandlers.handle(event);
            getAppLog().log(event);
        }
    }

    public void transactionStart(Application application, Object transactionData) {
        HttpServletRequest request = (HttpServletRequest) transactionData;

        this.httpSession = request.getSession();

        httpSession.setMaxInactiveInterval(webConfig.getHttpSessionExpirationTimeoutSec());

        setClientAddress(request);

        if (log.isTraceEnabled()) {
            log.trace("requestStart: [@" + Integer.toHexString(System.identityHashCode(request)) + "] " +
                    request.getRequestURI() +
                    (request.getUserPrincipal() != null ? " [" + request.getUserPrincipal() + "]" : "") +
                    " from " + clientAddress);
        }

        if (application == App.this) {
            currentApp.set((App) application);
        }
        application.setLocale(request.getLocale());

        if (ActiveDirectoryHelper.useActiveDirectory())
            setUser(request.getUserPrincipal());

        if (contextName == null) {
            contextName = request.getContextPath().substring(1);
        }

        String requestURI = request.getRequestURI();
        String windowName = request.getParameter("windowName");

        setupCurrentWindowName(requestURI, windowName);

        String action = (String) httpSession.getAttribute(LAST_REQUEST_ACTION_ATTR);

        if (!connection.isConnected() &&
                !(("login".equals(action)) || auxillaryUrl(requestURI))) {
            if (loginOnStart(request))
                setupCurrentWindowName(requestURI, windowName);
        }

        if (connection.isConnected()) {
            UserSession userSession = connection.getSession();
            if (userSession != null) {
                AppContext.setSecurityContext(new SecurityContext(userSession));
                application.setLocale(userSession.getLocale());
            }
            requestStartTimes.put(transactionData, System.currentTimeMillis());
        }

        processExternalLink(request, requestURI);
    }

    protected void setClientAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X_FORWARDED_FOR");
        if (!StringUtils.isBlank(xForwardedFor)) {
            String[] strings = xForwardedFor.split(",");
            clientAddress = strings[strings.length-1].trim();
        } else {
            clientAddress = request.getRemoteAddr();
        }
    }

    public static boolean auxillaryUrl(String uri) {
        return uri.contains("/UIDL/") || uri.contains("/APP/") || uri.contains("/VAADIN/");
    }

    private void setupCurrentWindowName(String requestURI, String windowName) {
        //noinspection deprecation
        if (StringUtils.isEmpty(windowName))
            currentWindowName.set(getMainWindow() == null ? null : getMainWindow().getName());
        else
            currentWindowName.set(windowName);

        String[] parts = requestURI.split("/");
        boolean contextFound = false;
        for (String part : parts) {
            if (StringUtils.isEmpty(part)) {
                continue;
            }
            if (part.equals(contextName) && !contextFound) {
                contextFound = true;
                continue;
            }
            if (contextFound && part.equals("UIDL")) {
                continue;
            }
            Matcher m = WIN_PATTERN.matcher(part);
            if (m.matches()) {
                currentWindowName.set(part);
                break;
            }
        }
    }

    private void processExternalLink(HttpServletRequest request, String requestURI) {
        String action = (String) request.getSession().getAttribute(LAST_REQUEST_ACTION_ATTR);

        if ("open".equals(action) && !auxillaryUrl(requestURI)) {
            Map<String, String> params = (Map<String, String>) request.getSession().getAttribute(LAST_REQUEST_PARAMS_ATTR);
            if (params == null) {
                log.warn("Unable to process the external link: lastRequestParams not found in session");
                return;
            }
            LinkHandler linkHandler = new LinkHandler(this, params);
            if (connection.isConnected())
                linkHandler.handle();
            else
                this.linkHandler = linkHandler;
        }
    }

    public void transactionEnd(Application application, Object transactionData) {
        HttpServletRequest request = (HttpServletRequest) transactionData;
        if (connection.isConnected()) {
            UserSession userSession = connection.getSession();
            if (userSession != null) {
                request.getSession().setAttribute(USER_SESSION_ATTR, userSession);
            } else {
                request.getSession().setAttribute(USER_SESSION_ATTR, null);
            }
        } else {
            request.getSession().setAttribute(USER_SESSION_ATTR, null);
        }

        Long start = requestStartTimes.remove(transactionData);
        if (start != null) {
            long t = System.currentTimeMillis() - start;
            if (t > (webConfig.getLogLongRequestsThresholdSec() * 1000)) {
                log.warn(String.format("Too long request processing [%d ms]: ip=%s, url=%s",
                        t, ((HttpServletRequest)transactionData).getRemoteAddr(), ((HttpServletRequest)transactionData).getRequestURI()));
            }
        }

        if (application == App.this) {
            currentApp.set(null);
            currentApp.remove();
        }

        AppContext.setSecurityContext(null);

        HttpSession httpSession = ((HttpServletRequest) transactionData).getSession();
        httpSession.setAttribute(LAST_REQUEST_ACTION_ATTR, null);
        httpSession.setAttribute(LAST_REQUEST_PARAMS_ATTR, null);

        if (log.isTraceEnabled()) {
            log.trace("requestEnd: [@" + Integer.toHexString(System.identityHashCode(transactionData)) + "]");
        }
    }

    public BackgroundTaskManager getTaskManager() {
        return backgroundTaskManager;
    }

    public void addBackgroundTask(Thread task) {
        backgroundTaskManager.addTask(task);
    }

    public void removeBackgroundTask(Thread task) {
        backgroundTaskManager.removeTask(task);
    }

    public void cleanupBackgroundTasks() {
        backgroundTaskManager.cleanupTasks();
    }

    Window getCurrentWindow() {
        String name = currentWindowName.get();
        return (name == null ? getMainWindow() : getWindow(name));
    }

    public AppTimers getTimers() {
        return timers;
    }

    /**
     * Adds a timer on the application level
     * @param timer new timer
     */
    public void addTimer(Timer timer) {
        timers.add(timer);
    }

    /**
     * Adds a timer for the defined window
     * @param timer new timer
     * @param owner component that owns a timer
     */
    public void addTimer(final Timer timer, com.haulmont.cuba.gui.components.Window owner) {
        timers.add(timer, owner);
    }

    protected Timer createSessionPingTimer(final boolean connected) {
        int sessionExpirationTimeout = webConfig.getHttpSessionExpirationTimeoutSec();
        int sessionPingPeriod = sessionExpirationTimeout / 3;
        if (sessionPingPeriod > 0) {
            Timer timer = new Timer(sessionPingPeriod * 1000, true);
            timer.addListener(new Timer.Listener() {
                public void onTimer(Timer timer) {
                    if (connected) {
                        UserSessionService service = ServiceLocator.lookup(UserSessionService.NAME);
                        service.pingSession();
                    }
                    log.debug("Ping session");
                }

                public void onStopTimer(Timer timer) {
                }
            });
            return timer;
        }
        return null;
    }

    public AppCookies getCookies() {
        return cookies;
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public String getCookieValue(String name) {
        return cookies.getCookieValue(name);
    }

    public int getCookieMaxAge(String name) {
        return cookies.getCookieMaxAge(name);
    }

    public void addCookie(String name, String value, int maxAge) {
        cookies.addCookie(name, value, maxAge);
    }

    public void addCookie(String name, String value) {
        cookies.addCookie(name, value);
    }

    public void removeCookie(String name) {
        cookies.removeCookie(name);
    }

    public boolean isCookiesEnabled() {
        return cookies.isCookiesEnabled();
    }

    public void setCookiesEnabled(boolean cookiesEnabled) {
        cookies.setCookiesEnabled(cookiesEnabled);
    }

    public boolean isTestModeRequest() {
        return testModeRequest;
    }

    public String getClientAddress() {
        return clientAddress;
    }
}
