/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 30.10.2009 16:16:30
 *
 * $Id$
 */
package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Locator;
import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.FileStorageMBean;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.FileTypesHelper;
import com.haulmont.cuba.web.App;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

public class FileDownloadServlet extends HttpServlet {

    private Log log = LogFactory.getLog(FileDownloadServlet.class);

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        App app = getExistingApplication(request, response);
        if (app == null || !app.getConnection().isConnected()) {
            error(response);
            return;
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(request.getParameter("s"));
        } catch (Exception e) {
            error(response);
            return;
        }
        if (!sessionId.equals(app.getConnection().getSession().getId())) {
            error(response);
            return;
        }

        UUID fileId;
        try {
            fileId = UUID.fromString(request.getParameter("f"));
        } catch (Exception e) {
            error(response);
            return;
        }

        boolean attach = Boolean.valueOf(request.getParameter("a"));

        FileDescriptor fd = null;

        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();
            fd = em.find(FileDescriptor.class, fileId);
            tx.commit();
        } finally {
            tx.end();
        }

        String fileName;
        try {
            fileName = URLEncoder.encode(fd.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        response.setHeader("Content-Type", getContentType(fd));

        String contentDisposition = "filename=" + fileName;
        if (attach)
            contentDisposition = "attachment; " + fileName;
        response.setHeader("Content-Disposition", contentDisposition);

        byte[] data;
        FileStorageMBean mbean = Locator.lookupMBean(FileStorageMBean.class, FileStorageMBean.OBJECT_NAME);
        try {
            data = mbean.getAPI().loadFile(fd);
        } catch (FileStorageException e) {
            log.error("Unable to download file", e);
            error(response);
            return;
        }

        ServletOutputStream os = response.getOutputStream();
        os.write(data, 0, data.length);
        os.flush();
        os.close();
    }

    protected String getContentType(FileDescriptor fd) {
        String type = FileTypesHelper.getMIMEType("." + fd.getExtension().toLowerCase());
        if ("application/pdf".equals(type))
            return type;
        else
            return FileTypesHelper.DEFAULT_MIME_TYPE;
    }

    private void error(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private App getExistingApplication(HttpServletRequest request,
                                               HttpServletResponse response)
    {
        // Ensures that the session is still valid
        final HttpSession session = request.getSession(true);

        // Gets application list for the session.
        final Collection applications = WebApplicationContext
                .getApplicationContext(session).getApplications();

        // Search for the application (using the application URI) from the list
        for (final Iterator i = applications.iterator(); i.hasNext();) {
            final Application a = (Application) i.next();
            final String aPath = a.getURL().getPath();

            String servletPath = request.getContextPath();
            if (!servletPath.endsWith("/")) {
                servletPath += "/";
            }

            if (servletPath.equals(aPath)) {
                // Found a running application
                if (a.isRunning() && a instanceof App) {
                    return (App) a;
                }
                break;
            }
        }

        // Existing application not found
        return null;
    }
}
