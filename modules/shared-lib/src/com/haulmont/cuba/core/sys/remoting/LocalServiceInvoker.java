/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.remoting;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public interface LocalServiceInvoker {

    LocalServiceInvocationResult invoke(LocalServiceInvocation invocation);
}
