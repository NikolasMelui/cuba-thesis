/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 19.06.2009 12:04:31
 *
 * $Id$
 */
package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.NoSuchScreenException;
import com.haulmont.cuba.gui.ServiceLocator;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.DialogAction;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserSubstitution;
//import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.actions.ChangeSubstUserAction;
import com.haulmont.cuba.web.actions.DoNotChangeSubstUserAction;
import com.haulmont.cuba.web.exception.AccessDeniedHandler;
import com.haulmont.cuba.web.exception.EntityAccessExceptionHandler;
import com.haulmont.cuba.web.exception.NoSuchScreenHandler;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LinkHandler {
/*
    private static Log log = LogFactory.getLog(LinkHandler.class);
    private AppUI app;
    private Map<String, String> requestParams;

    public LinkHandler(AppUI app, Map<String, String> requestParams) {
        this.app = app;
        this.requestParams = requestParams;
    }

    public void handle() {
        try {
            String screenName = requestParams.get("screen");
            if (screenName == null) {
                log.warn("ScreenId not found in request parameters");
                return;
            }

            WindowConfig windowConfig = AppBeans.get(WindowConfig.class);
            final WindowInfo windowInfo = windowConfig.getWindowInfo(screenName);
            if (windowInfo == null) {
                log.warn("WindowInfo not found for screen: " + screenName);
                return;
            }

            UUID userId = getUUID(requestParams.get("user"));
            if (!(userId == null || app.getConnection().getSession().getCurrentOrSubstitutedUser().getId().equals(userId))) {
                final User substitutedUser = loadUser(userId, app.getConnection().getSession().getUser());
                if (substitutedUser != null)
                    app.getWindowManager().showOptionDialog(
                            MessageProvider.getMessage(getClass(), "toSubstitutedUser.title"),
                            getDialogMessage(substitutedUser),
                            IFrame.MessageType.CONFIRMATION,
                            new Action[]{
                                    new ChangeSubstUserAction(substitutedUser) {
                                        @Override
                                        public void doAfterChangeUser() {
                                            super.doAfterChangeUser();
                                            openWindow(windowInfo);
                                        }

                                        @Override
                                        public void doRevert() {
                                            super.doRevert();
                                            app.getAppWindow().executeJavaScript("window.close();");
                                        }

                                        @Override
                                        public String getCaption() {
                                            return MessageProvider.getMessage(getClass(), "action.switch");
                                        }
                                    },
                                    new DoNotChangeSubstUserAction() {
                                        @Override
                                        public void actionPerform(Component component) {
                                            super.actionPerform(component);
                                            app.getAppWindow().executeJavaScript("window.close();");
                                        }

                                        @Override
                                        public String getCaption() {
                                            return MessageProvider.getMessage(getClass(), "action.cancel");
                                        }
                                    }
                            });
                else {
                    User user = loadUser(userId);
                    app.getWindowManager().showOptionDialog(
                            MessageProvider.getMessage(getClass(), "warning.title"),
                            getWarningMessage(user),
                            IFrame.MessageType.WARNING,
                            new Action[]{
                                    new DialogAction(DialogAction.Type.OK) {
                                        @Override
                                        public void actionPerform(Component component) {
                                            app.getAppWindow().executeJavaScript("window.close();");
                                        }
                                    }
                            });
                }
            } else
                openWindow(windowInfo);
        } catch (AccessDeniedException e) {
            new AccessDeniedHandler().handle(e, app);
        } catch (NoSuchScreenException e) {
            new NoSuchScreenHandler().handle(e, app);
        } catch (EntityAccessException e) {
            new EntityAccessExceptionHandler().handle(e, app);
        }
    }

    private UUID getUUID(String id) {
        if (StringUtils.isBlank(id))
            return null;

        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            uuid = null;
        }
        return uuid;
    }

    private String getWarningMessage(User user) {
        if (user == null)
            return MessageProvider.getMessage(getClass(), "warning.userNotFound");
        return MessageProvider.formatMessage(
                getClass(),
                "warning.msg",
                StringUtils.isBlank(user.getName()) ? user.getLogin() : user.getName()
        );
    }

    private User loadUser(UUID userId, User user) {
        if (user.getId().equals(userId))
            return user;
        LoadContext loadContext = new LoadContext(UserSubstitution.class);
        LoadContext.Query query = new LoadContext.Query("select su from sec$UserSubstitution us join us.user u " +
                "join us.substitutedUser su where u.id = :id and su.id = :userId and " +
                "(us.endDate is null or us.endDate >= :currentDate) and (us.startDate is null or us.startDate <= :currentDate)");
        query.addParameter("id", user);
        query.addParameter("userId", userId);
        query.addParameter("currentDate", TimeProvider.currentTimestamp());
        loadContext.setQuery(query);
        List<User> users = ServiceLocator.getDataService().loadList(loadContext);
        return users.isEmpty() ? null : users.get(0);
    }
    
    private User loadUser(UUID userId) {
        LoadContext loadContext = new LoadContext(User.class);
        LoadContext.Query query = new LoadContext.Query("select u from sec$User u where u.id = :userId");
        query.addParameter("userId", userId);
        loadContext.setQuery(query);
        List<User> users = ServiceLocator.getDataService().loadList(loadContext);
        return users.isEmpty() ? null : users.get(0);
    }

    private String getDialogMessage(User user) {
        return MessageProvider.formatMessage(
                getClass(),
                "toSubstitutedUser.msg",
                StringUtils.isBlank(user.getName()) ? user.getLogin() : user.getName()
        );
    }

    private void openWindow(WindowInfo windowInfo) {
        String itemStr = requestParams.get("item");
        if (itemStr == null) {
            app.getWindowManager().openWindow(windowInfo, WindowManager.OpenType.NEW_TAB, getParamsMap());
        } else {
            EntityLoadInfo info = EntityLoadInfo.parse(itemStr);
            if (info == null) {
                log.warn("Invalid item definition: " + itemStr);
            } else {
                Entity entity = loadEntityInstance(info);
                if (entity != null)
                    app.getWindowManager().openEditor(windowInfo, entity, WindowManager.OpenType.NEW_TAB, getParamsMap());
                else
                    throw new EntityAccessException();
            }
        }
    }

    private Map<String, Object> getParamsMap() {
        Map<String, Object> params = new HashMap<String, Object>();
        String paramsStr = requestParams.get("params");
        if (paramsStr == null)
            return params;

        String[] entries = paramsStr.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                log.warn("Invalid parameter: " + entry);
                return params;       
            }
            String name = parts[0];
            String value = parts[1];
            EntityLoadInfo info = EntityLoadInfo.parse(value);
            if (info != null) {
                Entity entity = loadEntityInstance(info);
                if (entity != null)
                    params.put(name, entity);
            } else if (Boolean.TRUE.toString().equals(value) || Boolean.FALSE.toString().equals(value)) {
                params.put(name, BooleanUtils.toBoolean(value));
            } else {
                params.put(name, value);
            }
        }
        return params;
    }

    private Entity loadEntityInstance(EntityLoadInfo info) {
        DataService ds = ServiceLocator.getDataService();
        LoadContext ctx = new LoadContext(info.getMetaClass()).setId(info.getId());
        if (info.getViewName() != null)
            ctx.setView(info.getViewName());
        Entity entity;
        try {
            entity = ds.load(ctx);
        } catch (Exception e) {
            log.warn("Unable to load item: " + info, e);
            return null;
        }
        return entity;
    }*/
}