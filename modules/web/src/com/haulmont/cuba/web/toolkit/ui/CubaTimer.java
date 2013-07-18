/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.toolkit.ui.client.timer.CubaTimerClientRpc;
import com.haulmont.cuba.web.toolkit.ui.client.timer.CubaTimerServerRpc;
import com.haulmont.cuba.web.toolkit.ui.client.timer.CubaTimerState;
import com.vaadin.ui.AbstractComponent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaTimer extends AbstractComponent implements CubaTimerServerRpc {

    protected final List<TimerListener> listeners = new LinkedList<>();

    protected boolean running = false;

    public CubaTimer() {
        registerRpc(this);
        // hide on client
        setWidth("0px");
        setHeight("0px");
    }

    @Override
    public CubaTimerState getState() {
        return (CubaTimerState) super.getState();
    }

    @Override
    protected CubaTimerState getState(boolean markAsDirty) {
        return (CubaTimerState) super.getState(markAsDirty);
    }

    public void setRepeating(boolean repeating) {
        getState().repeating = repeating;
    }

    public boolean isRepeating() {
        return getState(false).repeating;
    }

    public int getDelay() {
        return getState(false).delay;
    }

    public void setDelay(int delay) {
        getState().delay = delay;
    }

    public void start() {
        if (getDelay() <= 0)
            throw new IllegalStateException("Undefined delay for timer");

        if (!running) {
            getRpcProxy(CubaTimerClientRpc.class).setRunning(true);

            this.running = true;
        }
    }

    public void stop() {
        if (running) {
            getRpcProxy(CubaTimerClientRpc.class).setRunning(false);

            for (TimerListener listener : new ArrayList<>(listeners)) {
                listener.onStopTimer(this);
            }
            running = false;
        }
    }

    @Override
    public void onTimer() {
        try {
            for (TimerListener listener : new ArrayList<>(listeners)) {
                listener.onTimer(this);
            }
        } finally {
            getRpcProxy(CubaTimerClientRpc.class).requestCompleted();
        }
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        super.beforeClientResponse(initial);

        getState().listeners = listeners.size() > 0;
    }

    public void setTimerId(String id) {
        getState().timerId = id;
    }

    public void addTimerListener(TimerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            markAsDirty();
        }
    }

    public void removeTimerListener(TimerListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
            markAsDirty();
        }
    }

    public interface TimerListener {
        void onTimer(CubaTimer timer);

        void onStopTimer(CubaTimer timer);
    }
}