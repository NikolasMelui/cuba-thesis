/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.backgroundwork;

import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import org.apache.commons.lang.BooleanUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Window that indicates progress of the background task, shows progress bar and processed items' message.
 * <p/>
 * Background task should have &lt;T extends Number&gt; as the progress measure unit. Progress measure passed to the publish() method
 * is displayed in processed items'/percents' message. Total number of items should be specified before task execution.
 * <p/>
 * <p>On error:
 * <ul>
 * <li>Executes handle exception in background task</li>
 * <li>Closes background window</li>
 * <li>Shows Warning message if for background task specified owner window</li>
 * </ul>
 * <p/>
 *
 * @param <T> measure unit which shows progress of task
 * @param <V> result type
 *
 * @author ovchinnikov
 * @version $Id$
 */
public class BackgroundWorkProgressWindow<T extends Number, V> extends AbstractWindow {

    @Inject
    protected Label text;
    @Inject
    protected Label progressText;
    @Inject
    protected Button cancelButton;
    @Inject
    protected BackgroundWorker backgroundWorker;
    @Inject
    protected ProgressBar taskProgress;

    protected BackgroundTaskHandler<V> taskHandler;

    /**
     * Show modal window with message which will last until task completes.
     * Optionally cancel button can be displayed. By pressing cancel button user can cancel task execution.
     *
     * @param task            background task containing long operation
     * @param title           window title, optional
     * @param message         window message, optional
     * @param total           total number of items, that will be processed
     * @param cancelAllowed   show or not cancel button
     * @param percentProgress show progress in percents
     * @param <V>             task result type
     */
    public static <T extends Number, V> void show(BackgroundTask<T, V> task, @Nullable String title, @Nullable String message,
                                                  Number total, boolean cancelAllowed, boolean percentProgress) {
        if (task.getOwnerFrame() == null)
            throw new IllegalArgumentException("Task without owner cannot be run");

        Map<String, Object> params = new HashMap<>();
        params.put("task", task);
        params.put("title", title);
        params.put("message", message);
        params.put("total", total);
        params.put("cancelAllowed", cancelAllowed);
        params.put("percentProgress", percentProgress);

        task.getOwnerFrame().openWindow("backgroundWorkProgressWindow", WindowManager.OpenType.DIALOG, params);
    }

    /**
     * Show modal window with message which will last until task completes.
     * Optionally cancel button can be displayed. By pressing cancel button user can cancel task execution.
     *
     * @param task          background task containing long operation
     * @param title         window title, optional
     * @param message       window message, optional
     * @param total         total number of items, that will be processed
     * @param cancelAllowed show or not cancel button
     * @param <V>           task result type
     */
    public static <T extends Number, V> void show(BackgroundTask<T, V> task, @Nullable String title, @Nullable String message,
                                                  Number total, boolean cancelAllowed) {
        show(task, title, message, total, cancelAllowed, false);
    }

    /**
     * Show modal window with message which will last until task completes.
     * Cancel button is not shown.
     *
     * @param task    background task containing long operation
     * @param title   window title, optional
     * @param message window message, optional
     * @param total   total number of items, that will be processed
     * @param <V>     task result type
     */
    public static <T extends Number, V> void show(BackgroundTask<T, V> task, String title, String message, Number total) {
        show(task, title, message, total, false);
    }

    /**
     * Show modal window with default title and message which will last until task completes.
     * Cancel button is not shown.
     *
     * @param task          background task containing long operation
     * @param total         total number of items, that will be processed
     * @param cancelAllowed show or not cancel button
     * @param <V>           task result type
     */
    public static <T extends Number, V> void show(BackgroundTask<T, V> task, Number total, boolean cancelAllowed) {
        show(task, null, null, total, cancelAllowed);
    }

    /**
     * Show modal window with default title and message which will last until task completes.
     * Cancel button is not shown.
     *
     * @param task  background task containing long operation
     * @param total total number of items, that will be processed
     * @param <V>   task result type
     */
    public static <T extends Number, V> void show(BackgroundTask<T, V> task, Number total) {
        show(task, null, null, total, false);
    }

    @Override
    public void init(Map<String, Object> params) {
        getDialogParams().setWidth(500);
        final BackgroundTask<T, V> task = (BackgroundTask<T, V>) params.get("task");
        String title = (String) params.get("title");
        if (title != null) {
            setCaption(title);
        }

        String message = (String) params.get("message");
        if (message != null) {
            text.setValue(message);
        }

        Boolean cancelAllowedNullable = (Boolean) params.get("cancelAllowed");
        boolean cancelAllowed = BooleanUtils.isTrue(cancelAllowedNullable);

        Boolean percentProgressNullable = (Boolean) params.get("percentProgress");
        boolean percentProgress = BooleanUtils.isTrue(percentProgressNullable);

        cancelButton.setVisible(cancelAllowed);
        getDialogParams().setCloseable(cancelAllowed);

        final T total = (T) params.get("total");

        taskProgress.setValue(0.0f);

        WrapperTask<T, V> wrapperTask = new WrapperTask<>(task, total, percentProgress);

        taskHandler = backgroundWorker.handle(wrapperTask);
        taskHandler.execute();
    }

    public void cancel() {
        if (!taskHandler.cancel())
            close(Window.CLOSE_ACTION_ID);
    }

    protected class WrapperTask<T extends Number, V> extends LocalizedTaskWrapper<T, V> {

        protected Number total;
        protected boolean percentProgress = false;

        public WrapperTask(BackgroundTask<T, V> wrappedTask, Number total, boolean percentProgress) {
            super(wrappedTask, BackgroundWorkProgressWindow.this);
            this.total = total;
            this.percentProgress = percentProgress;

            showProgressText(0, 0);
        }

        protected void showProgressText(Number last, float progressValue) {
            if (!percentProgress)
                progressText.setValue(formatMessage("backgroundWorkProgress.progressTextFormat", last, total));
            else {
                int percentValue = (int)Math.ceil(progressValue * 100);
                progressText.setValue(formatMessage("backgroundWorkProgress.progressPercentFormat", percentValue));
            }
        }

        @Override
        public void progress(List<T> changes) {
            if (!changes.isEmpty()) {
                Number last = changes.get(changes.size() - 1);
                float value = last.floatValue() / total.floatValue();
                taskProgress.setValue(value);
                showProgressText(last, value);
            }
        }
    }
}