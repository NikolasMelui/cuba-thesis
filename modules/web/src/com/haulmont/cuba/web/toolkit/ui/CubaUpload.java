/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.toolkit.ui.client.upload.CubaUploadState;
import com.vaadin.ui.Upload;
import org.apache.commons.lang.StringUtils;

/**
 * @author artamonov
 * @version $Id$
 */
@Deprecated
public class CubaUpload extends Upload implements UploadComponent {

    @Override
    protected CubaUploadState getState() {
        return (CubaUploadState) super.getState();
    }

    @Override
    protected CubaUploadState getState(boolean markAsDirty) {
        return (CubaUploadState) super.getState(markAsDirty);
    }

    @Override
    public String getAccept() {
        return getState(false).accept;
    }

    /**
     * Note: this is just a hint for browser, user may select files that do not meet this property
     *
     * @param accept mime types, comma separated
     */
    @Override
    public void setAccept(String accept) {
        if (!StringUtils.equals(accept, getAccept())) {
            getState().accept = accept;
        }
    }

    @Override
    public void setCaption(String caption) {
        setButtonCaption(caption);
    }

    @Override
    public String getCaption() {
        return getButtonCaption();
    }
}