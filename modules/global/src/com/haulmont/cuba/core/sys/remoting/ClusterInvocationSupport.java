/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.remoting;

import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class that holds the information about current cluster topology.
 * <p/>
 * Always inject or lookup this bean by name, not by type, because an application project can define several instances
 * of this type to work with different middleware blocks.
 *
 * @author krivopustov
 * @version $Id$
 */
public class ClusterInvocationSupport {

    /**
     * Default name for the bean instance used by the platform code.
     */
    public static final String NAME = "cuba_clusterInvocationSupport";

    public interface Listener {
        void urlListChanged(List<String> newUrlList);
    }

    private final Log log = LogFactory.getLog(getClass());

    private List<String> urls;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    protected String baseUrl = AppContext.getProperty("cuba.connectionUrlList");
    protected boolean randomPriority = Boolean.valueOf(AppContext.getProperty("cuba.randomServerPriority"));

    protected String servletPath = "remoting";

    private List<Listener> listeners = new ArrayList<Listener>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public void init() {
        urls = new ArrayList<>();
        String[] strings = baseUrl.split("[,;]");
        for (String string : strings) {
            if (!StringUtils.isBlank(string)) {
                urls.add(string + "/" + servletPath);
            }
        }
        if (urls.size() > 1 && randomPriority) {
            Collections.shuffle(urls);
        }
    }

    public List<String> getUrlList() {
        return urls;
    }

    public List<String> getUrlList(String serviceName) {
        lock.readLock().lock();
        try {
            List<String> list = new ArrayList<>(urls.size());
            for (String url : urls) {
                list.add(url + "/" + serviceName);
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    public synchronized void updateUrlPriority(String successfulUrl) {
        List<String> newList = new ArrayList<>();
        String url = successfulUrl.substring(0, successfulUrl.lastIndexOf("/"));
        newList.add(url);
        lock.writeLock().lock();
        try {
            for (String u : urls) {
                if (!u.equals(url)) {
                    newList.add(u);
                }
            }
            log.debug("Connection URL priority changed: " + urls + " -> " + newList);
            urls = newList;
            for (Listener listener : listeners) {
                listener.urlListChanged(Collections.unmodifiableList(urls));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
