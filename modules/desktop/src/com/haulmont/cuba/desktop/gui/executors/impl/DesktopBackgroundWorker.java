/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.executors.impl;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.executors.*;
import com.haulmont.cuba.gui.executors.impl.TaskExecutor;
import com.haulmont.cuba.gui.executors.impl.TaskHandlerImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Desktop implementation of {@link BackgroundWorker}
 */
@ManagedBean(BackgroundWorker.NAME)
public class DesktopBackgroundWorker implements BackgroundWorker {

    private Log log = LogFactory.getLog(DesktopBackgroundWorker.class);

    private WatchDog watchDog;

    @Inject
    public DesktopBackgroundWorker(WatchDog watchDog) {
        this.watchDog = watchDog;
    }

    @Override
    public <T, V> BackgroundTaskHandler<V> handle(BackgroundTask<T, V> task) {
        checkNotNull(task);

        // create task handler
        DesktopTaskExecutor<T, V> taskExecutor = new DesktopTaskExecutor<>(task);
        TaskHandlerImpl<T, V> taskHandler = new TaskHandlerImpl<>(taskExecutor, watchDog);

        taskExecutor.setTaskHandler(taskHandler);

        return taskHandler;
    }

    /**
     * Task runner
     */
    private class DesktopTaskExecutor<T, V> extends SwingWorker<V, T> implements TaskExecutor<T, V> {

        private BackgroundTask<T, V> runnableTask;
        private Runnable finalizer;

        private volatile V result;
        private volatile Exception taskException = null;

        private UUID userId;

        private volatile boolean isClosed = false;
        private volatile boolean doneHandled = false;

        private Map<String, Object> params;
        private TaskHandlerImpl<T, V> taskHandler;

        private DesktopTaskExecutor(BackgroundTask<T, V> runnableTask) {
            this.runnableTask = runnableTask;

            UserSessionSource sessionSource = AppBeans.get(UserSessionSource.NAME);
            this.userId = sessionSource.getUserSession().getId();

            this.params = runnableTask.getParams() != null ?
                    Collections.unmodifiableMap(runnableTask.getParams()) :
                    Collections.<String, Object>emptyMap();
        }

        @Override
        protected final V doInBackground() throws Exception {
            Thread.currentThread().setName("BackgroundTaskThread");
            try {
                if (!Thread.currentThread().isInterrupted()) {
                    // do not run any activity if canceled before start
                    result = runnableTask.run(new TaskLifeCycle<T>() {
                        @SafeVarargs
                        @Override
                        public final void publish(T... changes) {
                            handleProgress(changes);
                        }

                        @Override
                        public boolean isInterrupted() {
                            return Thread.currentThread().isInterrupted();
                        }

                        @Override
                        @Nonnull
                        public Map<String, Object> getParams() {
                            return params;
                        }
                    });
                }
            } catch (Exception ex) {
                log.error("Exception occurred in background task", ex);
                if (!(ex instanceof InterruptedException) && !isCancelled()) {
                    taskException = ex;
                }
            } finally {
                watchDog.removeTask(taskHandler);
            }

            return result;
        }

        @Override
        protected final void process(List<T> chunks) {
            runnableTask.progress(chunks);
            // Notify listeners
            for (BackgroundTask.ProgressListener<T, V> listener : runnableTask.getProgressListeners()) {
                listener.onProgress(chunks);
            }
        }

        @Override
        protected final void done() {
            if (isCancelled()) {
                // handle cancel from edt before execution start
                log.trace("Done statement is not processed because it is canceled task");
                return;
            }

            if (isClosed) {
                log.trace("Done statement is not processed because it is already closed");
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Done task. User: " + userId);
            }

            try {
                if (this.taskException == null) {
                    runnableTask.done(result);
                    // Notify listeners
                    for (BackgroundTask.ProgressListener<T, V> listener : runnableTask.getProgressListeners()) {
                        listener.onDone(result);
                    }
                } else {
                    boolean handled = runnableTask.handleException(taskException);
                    if (!handled) {
                        log.error("Unhandled exception in background task", taskException);
                    }
                }
            } finally {
                if (finalizer != null) {
                    finalizer.run();
                    finalizer = null;
                }

                isClosed = true;
                doneHandled = true;
            }
        }

        @Override
        public final void startExecution() {
            execute();
        }

        @Override
        public final boolean cancelExecution() {
            if (isClosed) {
                log.trace("Cancel will not be processed because it is already closed");
                return false;
            }

            if (log.isDebugEnabled()) {
                log.debug("Cancel task. User: " + userId);
            }

            boolean isCanceledNow = cancel(true);

            if (log.isTraceEnabled()) {
                if (isCanceledNow) {
                    log.trace("Task was cancelled. User: " + userId);
                } else {
                    log.trace("Cancellation of task isn't processed. User: " + userId);
                }
            }

            this.isClosed = isCanceledNow;
            if (!doneHandled) {
                log.trace("Done was not handled. Return 'true' as canceled status. User: " + userId);

                this.isClosed = true;
                return true;
            }

            return isCanceledNow;
        }

        @Override
        public final V getResult() {
            V result;
            try {
                result = get();
                this.done();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Interrupted or execution exception in background task", e);
                return null;
            }
            return result;
        }

        @Override
        public final BackgroundTask<T, V> getTask() {
            return runnableTask;
        }

        @Override
        public final boolean inProgress() {
            return !isClosed;
        }

        @Override
        public final void setFinalizer(Runnable finalizer) {
            this.finalizer = finalizer;
        }

        @Override
        public final Runnable getFinalizer() {
            return finalizer;
        }

        @SafeVarargs
        @Override
        public final void handleProgress(T... changes) {
            publish(changes);
        }

        public void setTaskHandler(TaskHandlerImpl<T,V> taskHandler) {
            this.taskHandler = taskHandler;
        }
    }
}