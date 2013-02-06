/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web;

import com.haulmont.cuba.web.gui.WebTimer;
import com.haulmont.cuba.web.toolkit.Timer;
import com.vaadin.ui.Window;

import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class AppTimers {
    private App app;

    public AppTimers(App app) {
        this.app = app;
    }

/*


    protected Map<Window, WindowTimers> windowTimers = new HashMap<Window, WindowTimers>();
    protected Map<Timer, Window> timerWindow = new HashMap<Timer, Window>();

    private boolean stopTimers = false;


    */
/**
     * Adds a timer on the application level
     * @param timer new timer
     *//*

    public void add(final Timer timer) {
        if (!timerWindow.containsKey(timer))
            add(timer, null, app.getCurrentWindow());
    }

    */
/**
     * Adds a timer for the defined window
     * @param timer new timer
     * @param owner component that owns a timer
     *//*

    public void add(final Timer timer, com.haulmont.cuba.gui.components.Window owner) {
        if (!timerWindow.containsKey(timer))
            add(timer, owner, app.getCurrentWindow());
    }

    */
/**
     * Do not use this method in application code
     *//*

    public void add(final Timer timer, Window mainWindow) {
        if (!timerWindow.containsKey(timer))
            add(timer, null, mainWindow);
    }

    */
/**
     * Do not use this method in application code
     *//*

    public void add(final Timer timer, com.haulmont.cuba.gui.components.Window owner, Window mainWindow) {
        WindowTimers wt = windowTimers.get(mainWindow);
        if (wt == null) {
            wt = new WindowTimers();
            windowTimers.put(mainWindow, wt);
        }

        if (wt.timers.add(timer))
        {
            timerWindow.put(timer, mainWindow);

            timer.addListener(new Timer.Listener() {
                public void onTimer(Timer timer) {
                }

                public void onStopTimer(Timer timer) {
                    Window window = timerWindow.remove(timer);
                    if (window != null)
                    {
                        WindowTimers wt = windowTimers.get(window);
                        if (wt != null) {
                            wt.timers.remove(timer);
                            if (timer instanceof WebTimer) {
                                wt.idTimers.remove(((WebTimer) timer).getId());
                            }
                        }
                    }
                }
            });
            if (timer instanceof WebTimer) {
                final WebTimer webTimer = (WebTimer) timer;
                if (owner != null) {
                    owner.addListener(new com.haulmont.cuba.gui.components.Window.CloseListener() {
                        public void windowClosed(String actionId) {
                            timer.stopTimer();
                        }
                    });
                }
                if (webTimer.getId() != null) {
                    wt.idTimers.put(webTimer.getId(), webTimer);
                }
            }
        }
    }

    protected void stopAll() {
        Set<Timer> timers = new HashSet<Timer>(timerWindow.keySet());
        for (final Timer timer : timers) {
            if (timer != null && !timer.isStopped()) {
                timer.stopTimer();
            }
        }
        stopTimers = true;
    }

    */
/**
     * Returns a timer by id
     * @param id timer id
     * @return timer or <code>null</code>
     *//*

    public Timer getTimer(String id) {
        Window currentWindow = app.getCurrentWindow();
        WindowTimers wt = windowTimers.get(currentWindow);
        if (wt != null) {
            return wt.idTimers.get(id);
        } else {
            return null;
        }
    }

    */
/**
     * Do not use this method in application code
     * @param currentWindow current window
     * @return collection of timers that applied for the current window
     *//*

    public Collection<Timer> getAll(Window currentWindow) {
        if (stopTimers) {
            try {
                return Collections.unmodifiableSet(timerWindow.keySet());
            } finally {
                stopTimers = false;
            }
        } else {
            WindowTimers wt = windowTimers.get(currentWindow);
            if (wt != null) {
                return Collections.unmodifiableSet(wt.timers);
            } else {
                return Collections.emptySet();
            }
        }
    }

    protected static class WindowTimers {
        protected Map<String, Timer> idTimers = new HashMap<String, Timer>();
        protected Set<Timer> timers = new HashSet<Timer>();
    }
*/

}
