/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.downloader;

import com.vaadin.shared.communication.ClientRpc;

/**
 * @author artamonov
 * @version $Id$
 */
public interface CubaFileDownloaderClientRPC extends ClientRpc {

    void downloadFile(String resourceId);

    void viewDocument(String resourceId);
}