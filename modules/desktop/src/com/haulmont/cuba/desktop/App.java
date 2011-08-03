/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop;

import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.desktop.exception.*;
import com.haulmont.cuba.desktop.sys.*;
import com.haulmont.cuba.desktop.theme.DesktopTheme;
import com.haulmont.cuba.desktop.theme.DesktopThemeLoader;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.security.global.LoginException;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class App implements ConnectionListener {

    protected static App app;

    private Log log;

    protected JFrame frame;

    protected JMenuBar menuBar;

    protected Connection connection;

    protected DesktopWindowManager windowManager;

    private JTabbedPane tabsPane;

    private DisabledGlassPane glassPane;

    protected ExceptionHandlers exceptionHandlers;

    protected DesktopTheme theme;

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                app = new App(args);
                app.show();
                app.showLoginDialog();
            }
        });
    }

    public static App getInstance() {
        return app;
    }

    public App(String[] args) {
        try {
            System.setSecurityManager(null);
            initHomeDir();
            initLogging();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

        try {
            log.debug("Program arguments: " + Arrays.toString(args));

            initConnection();

            DesktopAppContextLoader contextLoader = new DesktopAppContextLoader(getDefaultAppPropertiesConfig(), args);
            contextLoader.load();

            initTheme();
            initUI();
            initExceptionHandling();
        } catch (Throwable t) {
            log.error("Error initializing application", t);
            System.exit(-1);
        }
    }

    public void show() {
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public JTabbedPane getTabsPane() {
        return tabsPane;
    }

    public void showLoginDialog() {
        LoginDialog loginDialog = new LoginDialog(frame, connection);
        loginDialog.setLocationRelativeTo(frame);
        loginDialog.open();
    }

    protected String getApplicationTitle() {
        return "CUBA Application";
    }

    protected String getDefaultAppPropertiesConfig() {
        return "/cuba-desktop-app.properties";
    }

    protected String getDefaultHomeDir() {
        return System.getProperty("user.home") + "/.haulmont/cuba";
    }

    protected String getDefaultLog4jConfig() {
        return "cuba-log4j.xml";
    }

    protected void initHomeDir() {
        String homeDir = System.getProperty(DesktopAppContextLoader.HOME_DIR_SYS_PROP);
        if (StringUtils.isBlank(homeDir)) {
            homeDir = getDefaultHomeDir();
        }
        homeDir = StrSubstitutor.replaceSystemProperties(homeDir);
        System.setProperty(DesktopAppContextLoader.HOME_DIR_SYS_PROP, homeDir);

        File file = new File(homeDir);
        if (!file.exists()) {
            boolean success = file.mkdirs();
            if (!success) {
                System.out.println("Unable to create home dir: " + homeDir);
                System.exit(-1);
            }
        }
        if (!file.isDirectory()) {
            System.out.println("Invalid home dir: " + homeDir);
            System.exit(-1);
        }
    }

    protected void initLogging() {
        String property = System.getProperty("log4j.configuration");
        if (StringUtils.isBlank(property)) {
            System.setProperty("log4j.configuration", getDefaultLog4jConfig());
        }
        log = LogFactory.getLog(App.class);
    }

    protected void initTheme() throws Exception {
        DesktopConfig config = ConfigProvider.getConfig(DesktopConfig.class);
        String themeName = config.getTheme();
        theme = DesktopThemeLoader.getInstance().loadTheme(themeName);
        theme.init();
    }

    public DesktopTheme getTheme() {
        return theme;
    }

    protected void initUI() {
        frame = new JFrame(getApplicationTitle());
        frame.setName("MainFrame");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        exit();
                    }
                }
        );

        glassPane = new DisabledGlassPane();
        JRootPane rootPane = SwingUtilities.getRootPane(frame);
        rootPane.setGlassPane(glassPane);

        frame.setContentPane(createStartContentPane());

        createMainWindowProperties().load();
    }

    protected MainWindowProperties createMainWindowProperties() {
        return new MainWindowProperties(frame);
    }

    protected void initConnection() {
        connection = new Connection();
        connection.addListener(this);
    }

    protected void exit() {
        createMainWindowProperties().save();
        AppContext.stopContext();
        System.exit(0);
    }

    protected Container createStartContentPane() {
        JPanel pane = new JPanel(new BorderLayout());
        menuBar = new JMenuBar();
        pane.add(menuBar, BorderLayout.NORTH);

        Locale loc = Locale.getDefault();

        JMenu menu = new JMenu(MessageProvider.getMessage(AppConfig.getMessagesPack(), "mainMenu.file", loc));
        menuBar.add(menu);

        JMenuItem item;

        item = new JMenuItem(MessageProvider.getMessage(AppConfig.getMessagesPack(), "mainMenu.connect", loc));
        item.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showLoginDialog();
                    }
                }
        );
        menu.add(item);

        item = new JMenuItem(MessageProvider.getMessage(AppConfig.getMessagesPack(), "mainMenu.exit", loc));
        item.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        exit();
                    }
                }
        );
        menu.add(item);

        return pane;
    }

    protected Container createContentPane() {
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(createTopPane(), BorderLayout.NORTH);
        pane.add(createCenterPane(), BorderLayout.CENTER);
        pane.add(createBottomPane(), BorderLayout.SOUTH);
        return pane;
    }

    protected JComponent createTopPane() {
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.add(createMenuBar(), BorderLayout.CENTER);
        return toolBar;
    }

    protected JComponent createMenuBar() {
        menuBar = new JMenuBar();

        JMenu menu = new JMenu(MessageProvider.getMessage(AppConfig.getMessagesPack(), "mainMenu.file"));
        menuBar.add(menu);

        JMenuItem item;

        item = new JMenuItem(MessageProvider.getMessage(AppConfig.getMessagesPack(), "mainMenu.disconnect"));
        item.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        connection.logout();
                    }
                }
        );
        menu.add(item);

        item = new JMenuItem(MessageProvider.getMessage(AppConfig.getMessagesPack(), "mainMenu.exit"));
        item.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        exit();
                    }
                }
        );
        menu.add(item);

        MenuBuilder builder = new MenuBuilder(connection.getSession(), menuBar);
        builder.build();

        return menuBar;
    }

    protected JComponent createBottomPane() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.gray));
        panel.setPreferredSize(new Dimension(0, 20));
        JLabel connectionStateLab = new JLabel("Connected");
        panel.add(connectionStateLab, BorderLayout.WEST);
        return panel;
    }

    protected JComponent createCenterPane() {
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(createTabsPane(), BorderLayout.CENTER);
        return pane;
    }

    protected JComponent createTabsPane() {
        tabsPane = new JTabbedPane();
        return tabsPane;
    }

    protected void initExceptionHandling() {
        exceptionHandlers = new ExceptionHandlers();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable throwable) {
                handleException(thread, throwable);
            }
        });

        System.setProperty("sun.awt.exception.handler", "com.haulmont.cuba.desktop.exception.AWTExceptionHandler");
    }

    public void handleException(Thread thread, Throwable throwable) {
        log.error("Exception in thread " + thread, throwable);
        exceptionHandlers.handle(thread, throwable);
    }

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
        } else {
            exceptionHandlers.getHandlers().clear();
        }
    }

    public void connectionStateChanged(Connection connection) throws LoginException {
        if (connection.isConnected()) {
            windowManager = new DesktopWindowManager();
            frame.setContentPane(createContentPane());
            frame.repaint();
            windowManager.setTabsPane(tabsPane);
            initExceptionHandlers(true);
        } else {
            windowManager = null;
            frame.setContentPane(createStartContentPane());
            frame.repaint();
            initExceptionHandlers(false);
            showLoginDialog();
        }
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }

    public JFrame getMainFrame() {
        return frame;
    }

    public void disable(@Nullable String message) {
        glassPane.activate(message);
    }

    public void enable() {
        glassPane.deactivate();
    }

    public Resources getResources() {
        return theme.getResources();
    }

    public void showNotificationPopup(String caption, IFrame.NotificationType type) {
        JPanel panel = new JPanel(new MigLayout("flowy"));
        panel.setBorder(BorderFactory.createLineBorder(Color.gray));

        switch (type) {
            case WARNING:
                panel.setBackground(Color.yellow);
                break;
            case ERROR:
                panel.setBackground(Color.orange);
                break;
            default:
                panel.setBackground(Color.cyan);
        }

        FontMetrics fontMetrics = frame.getGraphics().getFontMetrics();

        int height = (int) fontMetrics.getStringBounds(caption, frame.getGraphics()).getHeight();
        int width = 0;
        StringBuilder sb = new StringBuilder("<html>");
        String[] strings = caption.split("(<br>)|(<br/>)");
        for (String string : strings) {
            int w = (int) fontMetrics.getStringBounds(string, frame.getGraphics()).getWidth();
            width = Math.max(width, w);
            sb.append(string).append("<br/>");
        }
        sb.append("</html>");

        JLabel label = new JLabel(sb.toString());
        panel.add(label);

        int x = frame.getX() + frame.getWidth() - (50 + width);
        int y = frame.getY() + frame.getHeight() - (50 + ((height + 5) * strings.length));

        PopupFactory factory = PopupFactory.getSharedInstance();
        final Popup popup = factory.getPopup(frame, panel, x, y);
        popup.show();
        final Point location = MouseInfo.getPointerInfo().getLocation();
        final Timer timer = new Timer(3000, null);
        timer.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (!MouseInfo.getPointerInfo().getLocation().equals(location)) {
                            popup.hide();
                            timer.stop();
                        }
                    }
                }
        );
        timer.start();
    }

    public Locale getLocale() {
        if (getConnection().getSession() == null)
            return Locale.getDefault();
        else
            return getConnection().getSession().getLocale();
    }
}
