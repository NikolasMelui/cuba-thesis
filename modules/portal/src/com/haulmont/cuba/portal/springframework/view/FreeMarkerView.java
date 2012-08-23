/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.portal.springframework.view;

import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.portal.config.PortalConfig;
import freemarker.template.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author artamonov
 * @version $Id$
 */
public class FreeMarkerView extends org.springframework.web.servlet.view.freemarker.FreeMarkerView {

    @Inject
    protected Messages messages;

    protected Log log = LogFactory.getLog(FreeMarkerView.class);

    @Override
    protected SimpleHash buildTemplateModel(
            Map<String, Object> model,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        PortalConfig config = ConfigProvider.getConfig(PortalConfig.class);

        SimpleHash context = super.buildTemplateModel(model, request, response);
        context.put("userSession", AppContext.getSecurityContext().getSession());
        context.put("messages", messages);
        context.put("message", new MessageMethod());
        context.put("theme", config.getTheme());
        return context;
    }

    @Override
    protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response)
            throws IOException, TemplateException {
        CharArrayWriter printWriter;
        printWriter = new CharArrayWriter();
        try {
            template.process(model, printWriter);
            response.getWriter().write(printWriter.toCharArray());
        } catch (IOException e) {
            log.error("IO Exception", e);
        } catch (TemplateException e) {
            log.error("Template exception", e);
        }
    }

    public class MessageMethod implements TemplateMethodModel {

        @Override
        public Object exec(List args) throws TemplateModelException {
            if (args.size() == 2)
                return MessageProvider.getMessage((String) args.get(0), (String) args.get(1));
            else if (args.size() == 1) {
                return MessageProvider.getMessage((Enum) args.get(0));
            } else
                throw new TemplateModelException("Wrong arguments");
        }
    }
}
