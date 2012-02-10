/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 14.10.2009 14:22:13
 *
 * $Id$
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Filter;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

public class FilterLoader extends ComponentLoader {

    public FilterLoader(Context context) {
        super(context);
    }

    public Component loadComponent(ComponentsFactory factory, Element element, Component parent)
            throws InstantiationException, IllegalAccessException {
        final Filter filter = factory.createComponent("filter");
        initFilter(filter, element);
        return filter;
    }

    /**
     *
     */
    protected void initFilter(final Filter filter, final Element element) {
        assignXmlDescriptor(filter, element);
        loadId(filter, element);
        loadVisible(filter, element);
        loadStyleName(filter, element);

        String useMaxResults = element.attributeValue("useMaxResults");
        filter.setUseMaxResults(useMaxResults == null || Boolean.valueOf(useMaxResults));

        final String manualApplyRequired = element.attributeValue("manualApplyRequired");
        filter.setManualApplyRequired(BooleanUtils.toBooleanObject(manualApplyRequired));

        String datasource = element.attributeValue("datasource");
        if (!StringUtils.isBlank(datasource)) {
            CollectionDatasource ds = context.getDsContext().get(datasource);
            if (ds == null)
                throw new IllegalStateException("Cannot find data source by name: " + datasource);

            filter.setDatasource(ds);
        }

        assignFrame(filter);

        final IFrame frame = context.getFrame();
        final String applyTo = element.attributeValue("applyTo");
        if (!StringUtils.isEmpty(applyTo)) {
            context.addPostInitTask(new PostInitTask() {
                public void execute(Context context, IFrame window) {
                    Component c = frame.getComponent(applyTo);
                    if (c == null) {
                        throw new IllegalArgumentException("Cannot apply filter to component with id: " + applyTo);
                    }
                    filter.setApplyTo(c);
                }
            });
        }

        context.addPostInitTask(
                new PostInitTask() {
                    public void execute(Context context, IFrame window) {
                        filter.loadFiltersAndApplyDefault();
                    }
                }
        );
    }
}
