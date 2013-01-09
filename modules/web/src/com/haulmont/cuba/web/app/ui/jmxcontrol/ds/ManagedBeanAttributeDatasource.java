/*
 * Copyright (c) 2008-2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.app.ui.jmxcontrol.ds;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.data.DataService;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.data.impl.CollectionDatasourceImpl;
import com.haulmont.cuba.jmxcontrol.entity.ManagedBeanAttribute;
import com.haulmont.cuba.jmxcontrol.entity.ManagedBeanInfo;
import com.haulmont.cuba.web.jmx.JmxControlException;
import com.haulmont.cuba.web.jmx.JmxControlAPI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.UUID;

/**
 * @author budarov
 * @version $Id$
 */
public class ManagedBeanAttributeDatasource extends CollectionDatasourceImpl<ManagedBeanAttribute, UUID> {

    private Log log = LogFactory.getLog(getClass());

    private JmxControlAPI jmxControlAPI = AppBeans.get(JmxControlAPI.NAME);

    public ManagedBeanAttributeDatasource(DsContext context, DataService dataservice, String id,
                                          MetaClass metaClass, String viewName) {
        super(context, dataservice, id, metaClass, viewName);
    }

    @Override
    protected void loadData(Map<String, Object> params) {
        data.clear();

        Datasource mbeanDs = getDsContext().get("mbeanDs");
        ManagedBeanInfo mbean = (ManagedBeanInfo) mbeanDs.getItem();

        if (mbean != null) {
            try {
                mbean = jmxControlAPI.loadAttributes(mbean);
            } catch (JmxControlException e) {
                log.error(e);
            }

            if (mbean.getAttributes() != null) {
                for (ManagedBeanAttribute attr : mbean.getAttributes()) {
                    data.put(attr.getId(), attr);
                }
            }
        }
    }
}