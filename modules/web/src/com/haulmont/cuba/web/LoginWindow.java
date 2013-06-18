/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web;

import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.web.sys.ActiveDirectoryHelper;
import com.haulmont.cuba.web.sys.auth.DomainAliasesResolver;
import com.haulmont.cuba.web.toolkit.VersionedThemeResource;
import com.vaadin.data.Property;
import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Sizeable;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

/**
 * Login window.
 * <p/>
 * Specific application should inherit from this class and create appropriate
 * instance in {@link DefaultApp#createLoginWindow()} method
 *
 * @author krivopustov
 * @version $Id$
 */
public class LoginWindow extends UIView implements Action.Handler {

    public static final String COOKIE_LOGIN = "rememberMe.Login";
    public static final String COOKIE_PASSWORD = "rememberMe.Password";
    public static final String COOKIE_REMEMBER_ME = "rememberMe";

    protected Log log = LogFactory.getLog(getClass());

    // must be 8 symbols
    private static final String PASSWORD_KEY = "25tuThUw";

    private static final char[] DOMAIN_SEPARATORS = new char[]{'\\', '@'};

    protected Connection connection;

    protected TextField loginField;
    protected PasswordField passwordField;
    protected AbstractSelect localesSelect;
    protected Locale loc;
    protected Map<String, Locale> locales;
    protected GlobalConfig globalConfig;
    protected WebConfig webConfig;

    protected boolean rememberMeAllowed = false;
    protected CheckBox rememberMe;
    protected boolean loginByRememberMe = false;

    protected Property.ValueChangeListener loginChangeListener;

    protected Button okButton;

    protected Messages messages;
    protected Configuration configuration;
    protected PasswordEncryption passwordEncryption;

    public LoginWindow(App app, Connection connection) {
        configuration = AppBeans.get(Configuration.NAME);
        messages = AppBeans.get(Messages.NAME);
        passwordEncryption = AppBeans.get(PasswordEncryption.NAME);

        globalConfig = configuration.getConfig(GlobalConfig.class);
        webConfig = configuration.getConfig(WebConfig.class);
        locales = globalConfig.getAvailableLocales();

        loc = resolveLocale(app);

        this.connection = connection;

        loginField = new TextField();
        passwordField = new PasswordField();
        localesSelect = new NativeSelect();
        okButton = new Button();

        rememberMeAllowed = !ActiveDirectoryHelper.useActiveDirectory() ||
                !ActiveDirectoryHelper.activeDirectorySupportedBySession();

        if (rememberMeAllowed)
            rememberMe = new CheckBox();

        setSizeFull();
        setBaseStyle("cuba-login");

        initUI();
//        vaadin7 commented
//        if (globalConfig.getTestMode()) {
//            WebWindowManager windowManager = app.getWindowManager();
//            windowManager.setDebugId(loginField, "loginField");
//            windowManager.setDebugId(passwordField, "pwdField");
//            windowManager.setDebugId(localesSelect, "localesField");
//            if (okButton != null) {
//                windowManager.setDebugId(okButton, "loginSubmitButton");
//            }
//        }

        addActionHandler(this);
    }

    protected Locale resolveLocale(App app) {
        Locale appLocale = messages.getTools().useLocaleLanguageOnly() ?
                Locale.forLanguageTag(app.getAppUI().getLocale().getLanguage()) : app.getAppUI().getLocale();

        for (Locale locale : locales.values()) {
            if (locale.equals(appLocale)) {
                return locale;
            }
        }
        return locales.values().iterator().next();
    }

    protected void initStandartUI(int formWidth, int formHeight, int fieldWidth, boolean localesSelectVisible) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setStyleName(getStyle("main-layout"));

        FormLayout loginFormLayout = new FormLayout();
        Panel form = new Panel(loginFormLayout);
        form.setStyleName(getStyle("form"));
        form.setWidth(Sizeable.SIZE_UNDEFINED,  Unit.PIXELS);
        form.setHeight(Sizeable.SIZE_UNDEFINED,  Unit.PIXELS);

        loginFormLayout.setSpacing(true);
        loginFormLayout.setWidth(Sizeable.SIZE_UNDEFINED,  Unit.PIXELS);

        HorizontalLayout welcomeLayout = new HorizontalLayout();
        welcomeLayout.setStyleName(getStyle("form-caption"));
        welcomeLayout.setWidth(Sizeable.SIZE_UNDEFINED,  Unit.PIXELS);
        welcomeLayout.setHeight(Sizeable.SIZE_UNDEFINED,  Unit.PIXELS);
        welcomeLayout.setSpacing(true);

        Label label = new Label(messages.getMessage(getMessagesPack(), "loginWindow.welcomeLabel", loc));
        label.setWidth(Sizeable.SIZE_UNDEFINED,  Unit.PIXELS);
        label.setStyleName(getStyle("caption"));

        VerticalLayout centerLayout = new VerticalLayout();
        centerLayout.setStyleName(getStyle("bottom"));
        centerLayout.setSpacing(false);
        centerLayout.setWidth(formWidth + "px");
        centerLayout.setHeight(formHeight + "px");

        centerLayout.setHeight(formHeight + "px");

        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setStyleName(getStyle("title"));
        titleLayout.setSpacing(true);

        Image logoImage = getLogoImage();
        if (logoImage != null) {
            titleLayout.addComponent(logoImage);
            titleLayout.setComponentAlignment(logoImage, Alignment.MIDDLE_LEFT);
        }
        if (!StringUtils.isBlank(label.getValue())) {
            titleLayout.addComponent(label);
            titleLayout.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
        }

        centerLayout.addComponent(titleLayout);
        centerLayout.setComponentAlignment(titleLayout, Alignment.MIDDLE_CENTER);

        centerLayout.addComponent(form);
        centerLayout.setComponentAlignment(form, Alignment.MIDDLE_CENTER);

        loginField.setCaption(messages.getMessage(getMessagesPack(), "loginWindow.loginField", loc));
        loginFormLayout.addComponent(loginField);
        loginField.setWidth(fieldWidth + "px");
        loginField.setStyleName(getStyle("username-field"));
        loginFormLayout.setComponentAlignment(loginField, Alignment.MIDDLE_CENTER);

        passwordField.setCaption(messages.getMessage(getMessagesPack(), "loginWindow.passwordField", loc));
        passwordField.setWidth(fieldWidth + "px");
        passwordField.setStyleName(getStyle("password-field"));
        loginFormLayout.addComponent(passwordField);
        loginFormLayout.setComponentAlignment(passwordField, Alignment.MIDDLE_CENTER);

        if (localesSelectVisible) {
            localesSelect.setCaption(messages.getMessage(getMessagesPack(), "loginWindow.localesSelect", loc));
            localesSelect.setWidth(fieldWidth + "px");
            localesSelect.setNullSelectionAllowed(false);
            loginFormLayout.addComponent(localesSelect);
            loginFormLayout.setComponentAlignment(localesSelect, Alignment.MIDDLE_CENTER);
        }

        if (rememberMeAllowed) {
            rememberMe.setCaption(messages.getMessage(getMessagesPack(), "loginWindow.rememberMe", loc));
            rememberMe.setStyleName(getStyle("remember-me"));
            loginFormLayout.addComponent(rememberMe);
            loginFormLayout.setComponentAlignment(rememberMe, Alignment.MIDDLE_CENTER);
        }

        okButton.setCaption(messages.getMessage(getMessagesPack(), "loginWindow.okButton", loc));
        okButton.addClickListener(new SubmitListener());
        okButton.setStyleName(getStyle("submit"));
        okButton.setIcon(new VersionedThemeResource("app/images/login-button.png"));

        loginFormLayout.addComponent(okButton);
        loginFormLayout.setComponentAlignment(okButton, Alignment.MIDDLE_CENTER);

        mainLayout.addComponent(centerLayout);
        mainLayout.setSizeFull();
        mainLayout.setComponentAlignment(centerLayout, Alignment.MIDDLE_CENTER);

        initFields();
        loginField.focus();

        Layout userHintLayout = createUserHint();
        if (userHintLayout != null) {
            VerticalLayout wrapLayout = new VerticalLayout();
            wrapLayout.setSpacing(true);
            wrapLayout.addComponent(mainLayout);
            wrapLayout.addComponent(userHintLayout);
            setContent(wrapLayout);
        } else {
            setContent(mainLayout);
        }
    }

    @Nullable
    protected Image getLogoImage() {
        final String loginLogoImagePath = messages.getMainMessage("loginWindow.logoImage", loc);
        if ("loginWindow.logoImage".equals(loginLogoImagePath))
            return null;

        return new Image(null, new VersionedThemeResource(loginLogoImagePath));
    }

    protected void initUI() {
        initStandartUI(310, -1, 125, configuration.getConfig(GlobalConfig.class).getLocaleSelectVisible());
    }

    protected void initRememberMe() {
        App app = App.getInstance();
        String rememberMeCookie = app.getCookieValue(COOKIE_REMEMBER_ME);
        if (Boolean.parseBoolean(rememberMeCookie)) {
            rememberMe.setValue(true);

            String login;
            String encodedLogin = app.getCookieValue(COOKIE_LOGIN) != null ? app.getCookieValue(COOKIE_LOGIN) : "";
            try {
                login = URLDecoder.decode(encodedLogin, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                login = encodedLogin;
            }

            loginField.setValue(login);
            passwordField.setValue(app.getCookieValue(COOKIE_PASSWORD) != null ? app.getCookieValue(COOKIE_PASSWORD) : "");
            loginByRememberMe = true;

            loginChangeListener = new Property.ValueChangeListener() {
                @Override
                public void valueChange(Property.ValueChangeEvent event) {
                    loginByRememberMe = false;
                }
            };

            loginField.addValueChangeListener(loginChangeListener);
            passwordField.addValueChangeListener(loginChangeListener);
        } else {
            rememberMe.setValue(false);
            loginChangeListener = null;
        }
    }

    protected void initFields() {
        String currLocale = messages.getTools().localeToString(loc);
        String selected = null;
        App app = App.getInstance();
        for (Map.Entry<String, Locale> entry : locales.entrySet()) {
            localesSelect.addItem(entry.getKey());
            if (messages.getTools().localeToString(entry.getValue()).equals(currLocale))
                selected = entry.getKey();
        }
        if (selected == null)
            selected = locales.keySet().iterator().next();
        localesSelect.setValue(selected);

        if (ActiveDirectoryHelper.useActiveDirectory()) {
            loginField.setValue(app.getPrincipal() == null ? "" : app.getPrincipal().getName());
            passwordField.setValue("");

            if (!ActiveDirectoryHelper.activeDirectorySupportedBySession())
                initRememberMe();
        } else {
            String defaultUser = webConfig.getLoginDialogDefaultUser();
            if (!StringUtils.isBlank(defaultUser) && !"<disabled>".equals(defaultUser))
                loginField.setValue(defaultUser);
            else
                loginField.setValue("");

            String defaultPassw = webConfig.getLoginDialogDefaultPassword();
            if (!StringUtils.isBlank(defaultPassw) && !"<disabled>".equals(defaultPassw))
                passwordField.setValue(defaultPassw);
            else
                passwordField.setValue("");

            initRememberMe();
        }
    }

    @Override
    public String getTitle() {
        return messages.getMessage(getMessagesPack(), "loginWindow.caption", loc);
    }

    public class SubmitListener implements Button.ClickListener {
        @Override
        public void buttonClick(Button.ClickEvent event) {
            doLogin();
        }
    }

    @Override
    public Action[] getActions(Object target, Object sender) {
        final Action[] actions = new Action[1];
        actions[0] = new ShortcutAction("Default key",
                ShortcutAction.KeyCode.ENTER, null);
        return actions;
    }

    @Override
    public void handleAction(Action action, Object sender, Object target) {
        if (sender == this) {
            doLogin();
        }
    }

    protected void login() {
        String login = loginField.getValue();
        try {
            // Login with AD if domain specified
            if (ActiveDirectoryHelper.useActiveDirectory() && StringUtils.containsAny(login, DOMAIN_SEPARATORS)) {
                Locale locale = getUserLocale();
                App.getInstance().setLocale(locale);

                String password = passwordField.getValue();
                if (loginByRememberMe && StringUtils.isNotEmpty(password))
                    password = decryptPassword(password);

                ActiveDirectoryHelper.getAuthProvider().authenticate(login, password, loc);
                login = convertLoginString(login);

                ((ActiveDirectoryConnection) connection).loginActiveDirectory(login, locale);
            } else {
                Locale locale = getUserLocale();
                App.getInstance().setLocale(locale);

                String value = passwordField.getValue() != null ? passwordField.getValue() : "";
                String passwd = loginByRememberMe ? value : passwordEncryption.getPlainHash(value);

                login(login, passwd, locale);
            }
        } catch (LoginException e) {
            log.info("Login failed: " + e.toString());

            new Notification(
                    messages.getMessage(getMessagesPack(), "loginWindow.loginFailed", loc),
                    e.getMessage(), Notification.Type.ERROR_MESSAGE)
            .show(getUI().getPage());

            if (loginByRememberMe) {
                loginByRememberMe = false;
                loginField.removeValueChangeListener(loginChangeListener);
                passwordField.removeValueChangeListener(loginChangeListener);
                loginChangeListener = null;
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * Convert userName to db form
     * In database users stores in form DOMAIN&#92;userName
     *
     * @param login Login string
     * @return login in form DOMAIN&#92;userName
     */
    private String convertLoginString(String login) {
        DomainAliasesResolver aliasesResolver = AppBeans.get(DomainAliasesResolver.NAME);
        int slashPos = login.indexOf("\\");
        if (slashPos >= 0) {
            String domainAlias = login.substring(0, slashPos);
            String domain = aliasesResolver.getDomainName(domainAlias).toUpperCase();
            String userName = login.substring(slashPos + 1);
            login = domain + "\\" + userName;
        } else {
            int atSignPos = login.indexOf("@");
            String domainAlias = login.substring(atSignPos + 1);
            String domain = aliasesResolver.getDomainName(domainAlias).toUpperCase();
            String userName = login.substring(0, atSignPos);
            login = domain + "\\" + userName;
        }
        return login;
    }

    protected void login(String login, String passwd, Locale locale) throws LoginException {
        connection.login(login, passwd, locale);
    }

    protected void handleException(Exception e) {
        if (e instanceof RuntimeException)
            throw (RuntimeException) e;
        else
            throw new RuntimeException(e);
    }

    protected void doLogin() {
        login();
        if (rememberMeAllowed) {
            App app = App.getInstance();
            if (Boolean.TRUE.equals(rememberMe.getValue())) {
                if (!loginByRememberMe) {
                    app.addCookie(COOKIE_REMEMBER_ME, String.valueOf(rememberMe.getValue()));

                    String login = loginField.getValue();
                    String password = passwordField.getValue() != null ? passwordField.getValue() : "";

                    String encodedLogin;
                    try {
                        encodedLogin = URLEncoder.encode(login, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        encodedLogin = login;
                    }

                    app.addCookie(COOKIE_LOGIN, StringEscapeUtils.escapeJava(encodedLogin));
                    if (!ActiveDirectoryHelper.useActiveDirectory())
                        app.addCookie(COOKIE_PASSWORD, passwordEncryption.getPlainHash(password));
                    else {
                        if (StringUtils.isNotEmpty(password))
                            app.addCookie(COOKIE_PASSWORD, encryptPassword(password));
                    }
                }
            } else {
                app.removeCookie(COOKIE_REMEMBER_ME);
                app.removeCookie(COOKIE_LOGIN);
                app.removeCookie(COOKIE_PASSWORD);
            }
        }
    }

    protected String encryptPassword(String password) {
        SecretKeySpec key = new SecretKeySpec(PASSWORD_KEY.getBytes(), "DES");
        IvParameterSpec ivSpec = new IvParameterSpec(PASSWORD_KEY.getBytes());
        String result;
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            result = new String(Hex.encodeHex(cipher.doFinal(password.getBytes())));
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return result;
    }

    // if decrypt password is impossible returns encrypted password
    protected String decryptPassword(String password) {
        SecretKeySpec key = new SecretKeySpec(PASSWORD_KEY.getBytes(), "DES");
        IvParameterSpec ivSpec = new IvParameterSpec(PASSWORD_KEY.getBytes());
        String result;
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            result = new String(cipher.doFinal(Hex.decodeHex(password.toCharArray())));
        } catch (Exception e) {
            return password;
        }
        return result;
    }

    protected Locale getUserLocale() {
        String lang = (String) localesSelect.getValue();
        return locales.get(lang);
    }

    protected Layout createUserHint() {
        boolean enableChromeFrame = webConfig.getUseChromeFramePlugin();
        WebBrowser browser = UI.getCurrent().getPage().getWebBrowser();

        if (enableChromeFrame && browser.getBrowserApplication() != null) {
            final Browser browserInfo = Browser.getBrowserInfo(browser.getBrowserApplication());
            if (browserInfo.isIE() && !browserInfo.isChromeFrame()) {
                final Layout layout = new VerticalLayout();
                layout.setStyleName(getStyle("user-hint"));
                layout.addComponent(new Label(messages.getMessage(getMessagesPack(), "chromeframe.hint", loc),
                        ContentMode.HTML));
                return layout;
            }
        }
        return null;
    }

    protected String getMessagesPack() {
        return AppConfig.getMessagesPack();
    }
}