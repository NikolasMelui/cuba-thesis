/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.DialogParams;
import com.haulmont.cuba.gui.WindowContext;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Represents a reusable part of a screen.
 * <p/> Has its own XML descriptor, but can be instantiated only inside a {@link Window}.
 * Includes functionality for work with datasources and other windows.
 *
 * @author abramov
 * @version $Id$
 */
public interface IFrame
        extends ExpandingLayout,
                Component.BelongToFrame,
                Component.Spacing,
                Component.Margin,
                Component.ActionsHolder {

    /** XML element name used to show a frame in an enclosing screen. */
    String NAME = "iframe";

    /**
     * @return {@link WindowContext} of the current frame or window
     */
    WindowContext getContext();

    /** For internal use only. Don't call from application code. */
    void setContext(WindowContext ctx);

    /**
     * @return {@link DsContext} of the current frame or window
     */
    DsContext getDsContext();

    /** For internal use only. Don't call from application code. */
    void setDsContext(DsContext dsContext);

    /**
     * @return the message pack associated with the frame, usually in XML descriptor
     */
    String getMessagesPack();

    /**
     * Set message pack for this frame.
     * @param name message pack name
     */
    void setMessagesPack(String name);

    /** For internal use only. Don't call from application code. */
    void registerComponent(Component component);

    /**
     * Check validity by invoking validators on all components which support them.
     * @return true if all components are in valid state
     */
    boolean isValid();

    /**
     * Check validity by invoking validators on all components which support them.
     * @throws ValidationException if some components are currently in invalid state
     */
    void validate() throws ValidationException;

    /**
     * @return {@link DialogParams} that will be used for opening next window in modal mode.
     * <p/> If called in <code>init()</code>
     * method of a screen, which is being opened in {@link WindowManager.OpenType#DIALOG} mode, affects the current
     * screen itself.
     */
    DialogParams getDialogParams();

    /**
     * Open a simple screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param openType    how to open the screen
     * @param params      parameters to pass to <code>init()</code> method of the screen's controller
     * @return created window
     */
    <T extends Window> T openWindow(String windowAlias, WindowManager.OpenType openType, Map<String, Object> params);

    /**
     * Open a simple screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param openType    how to open the screen
     * @return created window
     */
    <T extends Window> T openWindow(String windowAlias, WindowManager.OpenType openType);

    /**
     * Open an edit screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param item        entity to edit
     * @param openType    how to open the screen
     * @param params      parameters to pass to <code>init()</code> method of the screen's controller
     * @param parentDs    if this parameter is not null, the editor will commit edited instance into this
     *                    datasource instead of directly to database
     * @return created window
     */
    <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType,
                                    Map<String, Object> params, Datasource parentDs);

    /**
     * Open an edit screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param item        entity to edit
     * @param openType    how to open the screen
     * @param params      parameters to pass to <code>init()</code> method of the screen's controller
     * @return created window
     */
    <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType,
                                    Map<String, Object> params);

    /**
     * Open an edit screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param item        entity to edit
     * @param openType    how to open the screen
     * @param parentDs    if this parameter is not null, the editor will commit edited instance into this
     *                    datasource instead of directly to database
     * @return created window
     */
    <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType,
                                    Datasource parentDs);

    /**
     * Open an edit screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param item        entity to edit
     * @param openType    how to open the screen
     * @return created window
     */
    <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType);

    /**
     * Open a lookup screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param handler     is invoked when selection confirmed and the lookup screen closes
     * @param openType    how to open the screen
     * @param params      parameters to pass to <code>init()</code> method of the screen's controller
     * @return created window
     */
    <T extends Window> T openLookup(String windowAlias, Window.Lookup.Handler handler, WindowManager.OpenType openType,
                                    Map<String, Object> params);

    /**
     * Open a lookup screen.
     *
     * @param windowAlias screen ID as defined in <code>screens.xml</code>
     * @param handler     is invoked when selection confirmed and the lookup screen closes
     * @param openType    how to open the screen
     * @return created window
     */
    <T extends Window> T openLookup(String windowAlias, Window.Lookup.Handler handler, WindowManager.OpenType openType);

    /**
     * Load a frame registered in <code>screens.xml</code> and optionally show it inside a parent component of this
     * frame.
     * @param parent        if specified, all parent's subcomponents will be removed and the frame will be added
     * @param windowAlias   frame ID as defined in <code>screens.xml</code>
     * @return              frame's controller instance
     */
    <T extends IFrame> T openFrame(@Nullable Component parent, String windowAlias);

    /**
     * Load a frame registered in <code>screens.xml</code> and optionally show it inside a parent component of this
     * frame.
     * @param parent        if specified, all parent's subcomponents will be removed and the frame will be added
     * @param windowAlias   frame ID as defined in <code>screens.xml</code>
     * @param params        parameters to be passed into the frame's controller <code>init</code> method
     * @return              frame's controller instance
     */
    <T extends IFrame> T openFrame(@Nullable Component parent, String windowAlias, Map<String, Object> params);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Message dialog type.
     */
    enum MessageType {
        CONFIRMATION,
        WARNING
    }

    /**
     * Show a message dialog.
     * @param title         dialog title
     * @param message       message
     * @param messageType   type which may affect the dialog style
     */
    void showMessageDialog(String title, String message, MessageType messageType);

    /**
     * Show an options dialog.
     * @param title         dialog title
     * @param message       message
     * @param messageType   type which may affect the dialog style
     * @param actions       array of actions that represent options. For standard options consider use of
     * {@link DialogAction} instances.
     */
    void showOptionDialog(String title, String message, MessageType messageType, Action[] actions);

    /**
     * Show an options dialog.
     * @param title         dialog title
     * @param message       message
     * @param messageType   type which may affect the dialog style
     * @param actions       list of actions that represent options. For standard options consider use of
     * {@link DialogAction} instances.
     */
    void showOptionDialog(String title, String message, MessageType messageType, java.util.List<Action> actions);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Popup notification type.
     */
    enum NotificationType {
        TRAY,
        HUMANIZED,
        WARNING,
        ERROR
    }

    /**
     * Show a notification.
     * @param caption   notification message
     * @param type      type which may affect the popup style
     */
    void showNotification(String caption, NotificationType type);

    /**
     * Show a notification.
     * @param caption       notification message
     * @param description   notification description to show next to the message
     * @param type          type which may affect the popup style
     */
    void showNotification(String caption, String description, NotificationType type);
}
