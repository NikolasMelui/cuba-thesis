/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.jmx;

import com.haulmont.cuba.core.entity.JmxInstance;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;

import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Set;

/**
 * @author artamonov
 * @version $Id$
 */
public abstract class JmxConnectionHelper {

    protected final static JmxInstance LOCAL_JMX_INSTANCE = new JmxInstance("Local");

    protected static MBeanServerConnection getConnection(JmxInstance instance) {
        if (ObjectUtils.equals(instance, LOCAL_JMX_INSTANCE))
            return getLocalConnection();
        else
            return getRemoteConnection(instance);
    }

    protected static MBeanServerConnection getLocalConnection() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    protected static MBeanServerConnection getRemoteConnection(JmxInstance instance) {
        MBeanServerConnectionFactoryBean factoryBean = new MBeanServerConnectionFactoryBean();
        try {
            factoryBean.setServiceUrl("service:jmx:rmi:///jndi/rmi://" + instance.getAddress() + "/jmxrmi");

            String username = instance.getLogin();
            if (StringUtils.isNotEmpty(username)) {
                Properties properties = new Properties();
                properties.put("jmx.remote.credentials", new String[]{username, instance.getPassword()});
                factoryBean.setEnvironment(properties);
            }

            factoryBean.afterPropertiesSet();

            return factoryBean.getObject();
        } catch (IOException e) {
            throw new JmxControlException(e);
        }
    }

    protected static ObjectName getObjectName(final MBeanServerConnection connection,
                                              final Class objectClass) throws IOException {

        Set<ObjectName> names = connection.queryNames(null, null);
        return (ObjectName) CollectionUtils.find(names, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                ObjectName objectName = (ObjectName) o;
                MBeanInfo info;
                try {
                    info = connection.getMBeanInfo(objectName);
                } catch (Exception e) {
                    throw new JmxControlException(e);
                }
                return StringUtils.equals(objectClass.getName(), info.getClassName());
            }
        });
    }

    protected static <T> T getProxy(MBeanServerConnection connection, ObjectName objectName, final Class<T> objectClass) {
        return JMX.newMBeanProxy(connection, objectName, objectClass, true);
    }
}