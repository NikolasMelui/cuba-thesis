/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml.layout;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Timer;

/**
 * Factory to create UI components in client independent manner.
 * <p/> An instance of the factory can be injected into screen controllers or obtained through {@link com.haulmont.cuba.core.global.AppBeans}.
 *
 * @author Abramov
 * @version $Id$
 */
public interface ComponentsFactory {

    String NAME = "cuba_ComponentsFactory";

    <T extends Component> T createComponent(String name);

    <T extends Timer> T createTimer();
}
