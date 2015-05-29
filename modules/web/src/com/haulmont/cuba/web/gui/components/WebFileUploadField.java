/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.web.toolkit.ui.CubaUpload;
import com.vaadin.ui.Upload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author abramov
 * @version $Id$
 */
public class WebFileUploadField extends WebAbstractComponent<CubaUpload> implements FileUploadField {

    private static final int BYTES_IN_MEGABYTE = 1048576;

    protected Log log = LogFactory.getLog(getClass());

    protected FileUploadingAPI fileUploading;
    protected Messages messages;

    protected String fileName;

    protected UUID fileId;

    protected FileOutputStream outputStream;
    protected UUID tempFileId;

    protected List<Listener> listeners = new ArrayList<>();

    public WebFileUploadField() {
        fileUploading = AppBeans.get(FileUploadingAPI.NAME);
        messages = AppBeans.get(Messages.NAME);

        component = createComponent();
        component.setReceiver(new com.vaadin.ui.Upload.Receiver() {
            @Override
            public OutputStream receiveUpload(String filename, String MIMEType) {
                fileName = filename;
                try {
                    tempFileId = fileUploading.createEmptyFile();
                    File tmpFile = fileUploading.getFile(tempFileId);
                    //noinspection ConstantConditions
                    outputStream = new FileOutputStream(tmpFile);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to receive file", e);
                }
                return outputStream;
            }
        });
        // Set single click upload functional
        component.setImmediate(true);

        component.addStartedListener(new Upload.StartedListener() {
            @Override
            public void uploadStarted(Upload.StartedEvent event) {
                Configuration configuration = AppBeans.get(Configuration.NAME);
                final long maxUploadSizeMb = configuration.getConfig(ClientConfig.class).getMaxUploadSizeMb();
                final long maxSize = maxUploadSizeMb * BYTES_IN_MEGABYTE;
                if (event.getContentLength() > maxSize) {
                    component.interruptUpload();

                    String warningMsg = messages.formatMainMessage("upload.fileTooBig.message", event.getFilename(), maxUploadSizeMb);
                    getFrame().showNotification(warningMsg, IFrame.NotificationType.WARNING);
                } else {
                    final Listener.Event e = new Listener.Event(event.getFilename());
                    for (Listener listener : listeners) {
                        listener.uploadStarted(e);
                    }
                }
            }
        });
        component.addFinishedListener(new Upload.FinishedListener() {
            @Override
            public void uploadFinished(Upload.FinishedEvent event) {
                if (outputStream != null)
                    try {
                        outputStream.close();
                        fileId = tempFileId;
                    } catch (IOException ignored) {
                    }
                final Listener.Event e = new Listener.Event(event.getFilename());
                for (Listener listener : listeners) {
                    listener.uploadFinished(e);
                }
            }
        });
        component.addSucceededListener(new Upload.SucceededListener() {
            @Override
            public void uploadSucceeded(Upload.SucceededEvent event) {
                final Listener.Event e = new Listener.Event(event.getFilename());
                for (Listener listener : listeners) {
                    listener.uploadSucceeded(e);
                }
            }
        });
        component.addFailedListener(new Upload.FailedListener() {
            @Override
            public void uploadFailed(Upload.FailedEvent event) {
                try {
                    // close and remove temp file
                    outputStream.close();
                    fileUploading.deleteFile(tempFileId);
                    tempFileId = null;
                } catch (Exception e) {
                    if (e instanceof FileStorageException) {
                        FileStorageException fse = (FileStorageException) e;
                        if (fse.getType() != FileStorageException.Type.FILE_NOT_FOUND)
                            log.warn(String.format("Could not remove temp file %s after broken uploading", tempFileId));
                    }
                    log.warn(String.format("Error while delete temp file %s", tempFileId));
                }
                final Listener.Event e = new Listener.Event(event.getFilename());
                for (Listener listener : listeners) {
                    listener.uploadFailed(e);
                }
            }
        });
        component.setButtonCaption(messages.getMessage(AppConfig.getMessagesPack(), "upload.submit"));
    }

    protected CubaUpload createComponent() {
        return new CubaUpload();
    }

    @Override
    public String getFileName() {
        String[] strings = fileName.split("[/\\\\]");
        return strings[strings.length - 1];
    }

    @Override
    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Get content bytes for uploaded file
     *
     * @return Bytes for uploaded file
     * @deprecated Please use {@link WebFileUploadField#getFileId()} method and {@link FileUploadingAPI}
     */
    @Override
    @Deprecated
    public byte[] getBytes() {
        byte[] bytes = null;
        try {
            if (fileId != null) {
                File file = fileUploading.getFile(fileId);
                FileInputStream fileInputStream = new FileInputStream(file);
                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                IOUtils.copy(fileInputStream, byteOutput);
                bytes = byteOutput.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return bytes;
    }

    @Override
    public String getCaption() {
        return component.getButtonCaption();
    }

    @Override
    public void setCaption(String caption) {
        component.setButtonCaption(caption);
    }

    @Override
    public String getDescription() {
        return component.getDescription();
    }

    @Override
    public void setDescription(String description) {
        component.setDescription(description);
    }

    /**
     * @return File id for uploaded file in {@link FileUploadingAPI}
     */
    @Override
    public UUID getFileId() {
        return fileId;
    }

    @Override
    public FileDescriptor getFileDescriptor() {
        if (fileId != null)
            return fileUploading.getFileDescriptor(fileId, fileName);
        else
            return null;
    }
}