/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.executors.impl;

import com.haulmont.cuba.core.global.TimeProvider;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.WatchDog;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * WatchDog for BackgroundWorker
 *
 * @author artamonov
 * @version $Id$
 */
public abstract class TasksWatchDog implements WatchDog {

    private final Set<TaskHandlerImpl> watches;

    public TasksWatchDog() {
        watches = new LinkedHashSet<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void cleanupTasks() {
        if (!AppContext.isStarted())
            return;

        long actual = TimeProvider.currentTimestamp().getTime();

        List<BackgroundTaskHandler> forRemove = new LinkedList<>();
        for (TaskHandlerImpl task : watches) {
            if (task.isCancelled() || task.isDone()) {
                forRemove.add(task);
            } else if (checkHangup(actual, task)) {
                task.close();
                forRemove.add(task);
            }
        }

        watches.removeAll(forRemove);
    }

    protected abstract boolean checkHangup(long actualTimeMs, TaskHandlerImpl taskHandler);

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stopTasks() {
        if (!AppContext.isStarted())
            return;

        for (TaskHandlerImpl task : watches) {
            task.close();
        }
        watches.clear();
    }

    @Override
    public synchronized int getActiveTasksCount() {
        return watches.size();
    }

    /**
     * {@inheritDoc}
     *
     * @param backroundTask Task handler
     */
    @Override
    public synchronized void manageTask(TaskHandlerImpl backroundTask) {
        watches.add(backroundTask);
    }
}