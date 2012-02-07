/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 29.12.2008 17:00:30
 *
 * $Id$
 */
package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.sys.auth.CubaAuthProvider;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

public class CubaHttpFilter implements Filter {
    private static Log log = LogFactory.getLog(CubaHttpFilter.class);

    private List<String> bypassUrls = new ArrayList<String>();
    private Filter activeDirectoryFilter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (ActiveDirectoryHelper.useActiveDirectory()) {
            try {
                activeDirectoryFilter = AppContext.getBean(CubaAuthProvider.NAME);
                activeDirectoryFilter.init(filterConfig);
            } catch (Exception e) {
                throw new ServletException(e);
            }
            // Fill bypassUrls
            String urls = ConfigProvider.getConfig(WebConfig.class).getCubaHttpFilterBypassUrls();
            String[] strings = urls.split("[, ]");
            for (String string : strings) {
                if (StringUtils.isNotBlank(string))
                    bypassUrls.add(string);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        request.setCharacterEncoding("UTF-8");

        String requestURI = request.getRequestURI();

        boolean filtered = false;

        if (ActiveDirectoryHelper.useActiveDirectory()) {
            // Active Directory integration
            if (!requestURI.endsWith("/"))
                requestURI = requestURI + "/";

            boolean bypass = false;
            for (String bypassUrl : bypassUrls) {
                if (requestURI.contains(bypassUrl)) {
                    bypass = true;
                    break;
                }
            }
            if (!bypass) {
                if (!checkApplicationSession(request)) {
                    log.debug("AD authentification");
                    activeDirectoryFilter.doFilter(request, response, chain);
                    filtered = true;
                }
            }
        }

        if (!filtered) {
            chain.doFilter(request, response);
        }
    }

    private boolean checkApplicationSession(HttpServletRequest request) {
        if (request.getSession() == null)
            return false;

        final HttpSession session = request.getSession(true);
        if (session == null)
            return false;

        if (isWebResourcesRequest(request))
            return true;

        WebApplicationContext applicationContext = CubaApplicationContext.getExistingApplicationContext(session);
        if (applicationContext == null)
            return false;

        final Collection<Application> applications = applicationContext.getApplications();

        for (Application app : applications) {
            String appPath = app.getURL().getPath();

            String servletPath = request.getContextPath();
            if (!servletPath.equals("/"))
                servletPath += "/";

            if (servletPath.equals(appPath)) {
                if (app.isRunning() && (app instanceof App)) {
                    if (((App) app).getConnection().isConnected())
                        return true;
                }
            }
        }

        return false;
    }

    private boolean isWebResourcesRequest(HttpServletRequest request) {
        return (request.getRequestURI() != null) && (request.getRequestURI().contains("/VAADIN/"));
    }

    @Override
    public void destroy() {
    }
}
