/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.core.entity.FileDescriptor;

import java.util.UUID;

public interface FileUploadField extends Component, Component.HasCaption, Component.BelongToFrame, Component.HasIcon,
        Component.HasDropZone {

    String NAME = "upload";

    interface Listener {
        class Event {
            String filename;
            Exception exception;

            public Event(String filename) {
                this.filename = filename;
            }

            public Event(String filename, Exception exception) {
                this.filename = filename;
                this.exception = exception;
            }

            public String getFilename() {
                return filename;
            }

            public Exception getException() {
                return exception;
            }
        }

        void uploadStarted(Event event);
        void uploadFinished(Event event);

        void uploadSucceeded(Event event);
        void uploadFailed(Event event);
    }

    class ListenerAdapter implements Listener {
        @Override
        public void uploadStarted(Event event) {
        }

        @Override
        public void uploadFinished(Event event) {
        }

        @Override
        public void uploadSucceeded(Event event) {
        }

        @Override
        public void uploadFailed(Event event) {
        }
    }

    /**
     * Get id for uploaded file in {@link com.haulmont.cuba.gui.upload.FileUploading}
     * @return File Id
     */
    UUID getFileId();
    String getFileName();
    FileDescriptor getFileDescriptor();

    /**
     * Get content bytes for uploaded file
     * @return Bytes for uploaded file
     * @deprecated Please use {@link FileUploadField#getFileId()} method and {@link com.haulmont.cuba.gui.upload.FileUploading}
     */
    byte[] getBytes();

    void addListener(Listener listener);
    void removeListener(Listener listener);

    /**
     * Returns comma separated types of files.
     * @return comma separated types of files
     */
    String getAccept();

    /**
     * Sets the mask for files to filter them in the file selection dialog.<br/>
     * Example: <pre>{@code fileUpload.setAccept(".png,.jpeg")}</pre>
     * @param accept comma separated types of files
     */
    void setAccept(String accept);
}