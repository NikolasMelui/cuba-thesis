/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 28.09.2009 12:25:29
 *
 * $Id$
 */
package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.toolkit.Timer;
import com.haulmont.cuba.web.toolkit.ui.MultiUpload;
import com.haulmont.cuba.web.toolkit.ui.charts.Chart;
import com.haulmont.cuba.web.toolkit.ui.charts.ChartDataProvider;
import com.haulmont.cuba.web.toolkit.ui.charts.ChartDataProviderFactory;
import com.haulmont.cuba.web.toolkit.ui.charts.ChartException;
import com.vaadin.Application;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.VariableOwner;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.terminal.gwt.server.JsonPaintTarget;
import com.vaadin.ui.Component;
import com.vaadin.ui.Window;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.*;

@SuppressWarnings("serial")
public class CubaCommunicationManager extends CommunicationManager {

    private static final int BYTES_IN_MEGABYTE = 1048576;

    private long timerIdSequence = 0;

    private Map<String, Timer> id2Timer = new HashMap<String, Timer>();

    private Map<Timer, String> timer2Id = new HashMap<Timer, String>();

    private Log log = LogFactory.getLog(CubaCommunicationManager.class);

    public CubaCommunicationManager(Application application) {
        super(application);
    }

    @Override
    protected void paintAdditionalData(
            Request request,
            Response response,
            boolean repaintAll,
            PrintWriter writer,
            Window window,
            boolean analyzeLayouts
    ) throws PaintException {
        final App application = App.getInstance();

        writer.print(", \"timers\":[");

        final JsonPaintTarget paintTarget = new JsonPaintTarget(this, writer, false);

        final Set<Timer> timers = new HashSet<Timer>(application.getTimers().getAll(window));
        for (final Timer timer : timers) {
            if (repaintAll || timer != null && timer.isDirty()) {
                String timerId;
                if ((timerId = timer2Id.get(timer)) == null) {
                    timerId = timerId();
                    timer2Id.put(timer, timerId);
                    id2Timer.put(timerId, timer);
                }
                timer.paintTimer(paintTarget, timerId);
                if (timer.isStopped()) {
                    fireTimerStop(timer);
                }
            }
        }

        paintTarget.close();

        writer.print("]");
    }

    @Override
    public boolean handleVariableBurst(Object source, Application app, boolean success, String burst) {
        // extract variables to two dim string array
        final String[] tmp = burst.split(VAR_RECORD_SEPARATOR);
        final String[][] variableRecords = new String[tmp.length][4];
        for (int i = 0; i < tmp.length; i++) {
            variableRecords[i] = tmp[i].split(VAR_FIELD_SEPARATOR);
        }

        for (int i = 0; i < variableRecords.length; i++) {
            String[] variable = variableRecords[i];
            String[] nextVariable = null;
            if (i + 1 < variableRecords.length) {
                nextVariable = variableRecords[i + 1];
            }
            final VariableOwner owner = getVariableOwner(variable[VAR_PID]);
            if (owner != null && owner.isEnabled()) {
                Map<String, Object> m;
                if (nextVariable != null
                        && variable[VAR_PID].equals(nextVariable[VAR_PID])) {
                    // we have more than one value changes in row for
                    // one variable owner, collect em in HashMap
                    m = new HashMap<String, Object>();
                    m.put(variable[VAR_NAME],
                            convertVariableValue(variable[VAR_TYPE].charAt(0),
                                    variable[VAR_VALUE]));
                } else {
                    // use optimized single value map
                    m = Collections.singletonMap(
                            variable[VAR_NAME],
                            convertVariableValue(variable[VAR_TYPE].charAt(0),
                                    variable[VAR_VALUE]));
                }

                // collect following variable changes for this owner
                while (nextVariable != null
                        && variable[VAR_PID].equals(nextVariable[VAR_PID])) {
                    i++;
                    variable = nextVariable;
                    if (i + 1 < variableRecords.length) {
                        nextVariable = variableRecords[i + 1];
                    } else {
                        nextVariable = null;
                    }
                    m.put(variable[VAR_NAME],
                            convertVariableValue(variable[VAR_TYPE].charAt(0),
                                    variable[VAR_VALUE]));
                }
                try {
                    owner.changeVariables(source, m);

                    // Special-case of closing browser-level windows:
                    // track browser-windows currently open in client
                    if (owner instanceof Window
                            && ((Window) owner).getParent() == null) {
                        final Boolean close = (Boolean) m.get("close");
                        if (close != null && close.booleanValue()) {
                            closingWindowName = ((Window) owner).getName();
                        }
                    }
                } catch (Exception e) {
                    if (owner instanceof Component) {
                        handleChangeVariablesError(app, (Component) owner, e, m);
                    } else {
                        // TODO DragDropService error handling
                        throw new RuntimeException(e);
                    }
                }
            } else {
                Timer timer;
                if ((timer = id2Timer.get(variable[VAR_PID])) != null && !timer.isStopped()) {
                    fireTimer(timer);
                } else {
                    // Handle special case where window-close is called
                    // after the window has been removed from the
                    // application or the application has closed
                    if ("close".equals(variable[VAR_NAME])
                            && "true".equals(variable[VAR_VALUE])) {
                        // Silently ignore this
                        continue;
                    }

                    // Ignore variable change
                    String msg = "Warning: Ignoring variable change for ";
                    if (owner != null) {
                        msg += "disabled component " + owner.getClass();
                        String caption = ((Component) owner).getCaption();
                        if (caption != null) {
                            msg += ", caption=" + caption;
                        }
                    } else {
                        msg += "non-existent component, VAR_PID="
                                + variable[VAR_PID];
                        success = false;
                    }
                    logger.warning(msg);
                }
            }
        }
        return success;
    }

    private String timerId() {
        return "TID" + ++timerIdSequence;
    }

    private void fireTimer(Timer timer) {
        final List<Timer.Listener> listeners = timer.getListeners();
        for (final Timer.Listener listener : listeners) {
            listener.onTimer(timer);
        }
    }

    private void fireTimerStop(Timer timer) {
        final List<Timer.Listener> listeners = new ArrayList<Timer.Listener>(timer.getListeners());
        for (final Timer.Listener listener : listeners) {
            listener.onStopTimer(timer);
            timer.removeListener(listener);
        }
    }

    public void handleChartRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            App app) {
        try {
            if (request.getSession() == null) {
                accessDenied(response);
                return;
            }

            String chartId = request.getParameter("id");
            if (chartId == null) {
                badRequest(response);
                return;
            }

            UserSession userSession = app.getConnection().getSession();
            if (userSession == null) {
                internalError(response);
                return;
            }

            Chart chart = (Chart)idPaintableMap.get(chartId);
            if (chart == null) {
                log.warn(String.format("Non-existent chart component, VAR_PID=%s", chartId));
                internalError(response);
                return;
            }

            AppContext.setSecurityContext(new SecurityContext(userSession));

            String vendor = chart.getVendor();
            ChartDataProvider dataProvider = ChartDataProviderFactory.getDataProvider(vendor);

            dataProvider.handleDataRequest(request, response, chart);

            response.setStatus(HttpServletResponse.SC_OK);
        } catch(ChartException e) {
            log.error("Unable to handle data request: ", e);
            internalError(response);
        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            internalError(response);
        }
    }

    public VariableOwner getVariableComponent(String variable) {
        return super.getVariableOwner(variable);
    }

    /**
     * Set response status to SC_UNAUTHORIZED
     *
     * @param response
     */
    protected void accessDenied(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Set response status to SC_INTERNAL_SERVER_ERROR
     *
     * @param response
     */
    protected void internalError(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Set response status to SC_BAD_REQUEST
     *
     * @param response
     */
    protected void badRequest(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * Handle multiple file upload
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @param app      Application
     */
    public synchronized void handleMultiUpload(HttpServletRequest request, HttpServletResponse response,
                                               App app) {
        try {
            if (request.getSession() == null)
                accessDenied(response);
            else {
                if (ServletFileUpload.isMultipartContent(request)) {
                    UserSession userSession = app.getConnection().getSession();
                    if (userSession == null) {
                        internalError(response);
                        return;
                    }
                    final Integer maxUploadSizeMb = ConfigProvider.getConfig(ClientConfig.class).getMaxUploadSizeMb();

                    AppContext.setSecurityContext(new SecurityContext(userSession));

                    org.apache.commons.fileupload.FileItemFactory factory = new DiskFileItemFactory();
                    ServletFileUpload upload = new ServletFileUpload(factory);
                    upload.setSizeMax(maxUploadSizeMb * BYTES_IN_MEGABYTE);

                    String controlPid = request.getParameter("pid");
                    if (controlPid != null) {
                        // Get server-side component
                        final MultiUpload uploadComponent = (MultiUpload) idPaintableMap.get(controlPid);
                        if (uploadComponent != null) {
                            
                            org.apache.commons.fileupload.FileItemIterator iterator = upload.getItemIterator(request);
                            boolean find = false;
                            while (iterator.hasNext() && (!find)) {
                                org.apache.commons.fileupload.FileItemStream itemStream = iterator.next();
                                if (!itemStream.isFormField()) {
                                    uploadComponent.uploadingFile(itemStream, request.getContentLength());
                                    find = true;
                                }
                            }
                        }

                        response.setStatus(HttpServletResponse.SC_OK);
                    } else
                        badRequest(response);
                } else
                    badRequest(response);
            }
        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            internalError(response);
        }
    }
}
