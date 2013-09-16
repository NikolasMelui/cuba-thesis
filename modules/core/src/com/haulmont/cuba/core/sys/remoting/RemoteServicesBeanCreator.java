/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.remoting;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author krivopustov
 * @version $Id$
 */
public class RemoteServicesBeanCreator implements BeanFactoryPostProcessor, ApplicationContextAware {

    private Log log = LogFactory.getLog(RemoteServicesBeanCreator.class);

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.info("Configuring remote services");

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        ApplicationContext coreContext = context.getParent();
        Map<String,Object> services = coreContext.getBeansWithAnnotation(Service.class);
        for (Map.Entry<String, Object> entry : services.entrySet()) {
            String serviceName = entry.getKey();
            Object service = entry.getValue();

            List<String> intfNames = new ArrayList<>();
            List<Class> interfaces = ClassUtils.getAllInterfaces(service.getClass());
            for (Class intf : interfaces) {
                if (intf.getName().endsWith("Service"))
                    intfNames.add(intf.getName());
            }
            if (intfNames.size() == 0) {
                log.warn("Bean " + serviceName + " has @Service annotation but no interfaces named '*Service'. Ignoring it.");
            } else if (intfNames.size() > 1) {
                log.warn("Bean " + serviceName + " has @Service annotation but more than one interface named '*Service'. Ignoring it.");
            } else {
                BeanDefinition definition = new RootBeanDefinition(HttpServiceExporter.class);
                MutablePropertyValues propertyValues = definition.getPropertyValues();
                propertyValues.add("service", service);
                propertyValues.add("serviceInterface", intfNames.get(0));
                registry.registerBeanDefinition("/" + serviceName, definition);
                log.debug("Bean " + serviceName + " configured for export via HTTP");
            }
        }
    }
}
