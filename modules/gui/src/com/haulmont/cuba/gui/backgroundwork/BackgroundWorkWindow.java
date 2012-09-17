/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.backgroundwork;

import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import org.apache.commons.lang.BooleanUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Modal window wrapping around background task. Displays title, message and optional cancel button.
 * Window lasts until task completes, timeout timer elapses or user presses cancel button.
 * <p/>
 * When cancelled by user, does not interrupt task thread.
 * <p/>
 * <p>On error:
 * <ul>
 * <li>Executes handle exception in background task</li>
 * <li>Closes background window</li>
 * <li>Shows Warning message if for background task specified owner window</li>
 * </ul>
 * <p/>
 *
 * @author budarov
 * @version $Id$
 */
public class BackgroundWorkWindow<T, V> extends AbstractWindow {

    @Inject
    private Label text;

    @Inject
    private Button cancelButton;

    @Inject
    private BackgroundWorker backgroundWorker;

    private BackgroundTaskHandler<V> taskHandler;

    /**
     * Show modal window with message which will last until task completes.
     * Optionally cancel button can be displayed. By pressing cancel button user can cancel task execution.
     *
     * @param task          background task containing long operation
     * @param title         window title, optional
     * @param message       window message, optional
     * @param cancelAllowed show or not cancel button
     * @param <T>           task progress unit
     * @param <V>           task result type
     */
    public static <T, V> void show(BackgroundTask<T, V> task, @Nullable String title, @Nullable String message,
                                   boolean cancelAllowed) {
        Map<String, Object> params = new HashMap<>();
        params.put("task", task);
        params.put("title", title);
        params.put("message", message);
        params.put("cancelAllowed", cancelAllowed);
        task.getOwnerWindow().openWindow("sys$BackgroundWorkWindow", WindowManager.OpenType.DIALOG, params);
    }

    /**
     * Show modal window with message which will last until task completes.
     * Cancel button is not shown.
     *
     * @param task    background task containing long operation
     * @param title   window title, optional
     * @param message window message, optional
     * @param <T>     task progress unit
     * @param <V>     task result type
     */
    public static <T, V> void show(BackgroundTask<T, V> task, String title, String message) {
        show(task, title, message, false);
    }

    /**
     * Show modal window with default title and message which will last until task completes.
     * Cancel button is not shown.
     *
     * @param task          background task containing long operation
     * @param cancelAllowed show or not cancel button
     * @param <T>           task progress unit
     * @param <V>           task result type
     */
    public static <T, V> void show(BackgroundTask<T, V> task, boolean cancelAllowed) {
        show(task, null, null, cancelAllowed);
    }

    /**
     * Show modal window with default title and message which will last until task completes.
     * Cancel button is not shown.
     *
     * @param task background task containing long operation
     * @param <T>  task progress unit
     * @param <V>  task result type
     */
    public static <T, V> void show(BackgroundTask<T, V> task) {
        show(task, null, null, false);
    }

    @Override
    public void init(Map<String, Object> params) {
        getDialogParams().setWidth(500);
        BackgroundTask<T, V> task = (BackgroundTask<T, V>) params.get("task");
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
        cancelButton.setVisible(cancelAllowed);
        getDialogParams().setCloseable(cancelAllowed);

        BackgroundTask<T, V> wrapperTask = new LocalizedTaskWrapper<>(task, this);

        taskHandler = backgroundWorker.handle(wrapperTask);
        taskHandler.execute();
    }

    public void cancel() {
        if (!taskHandler.cancel())
            close(Window.CLOSE_ACTION_ID);
    }
}