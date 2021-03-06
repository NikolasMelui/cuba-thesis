/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.desktop.sys.DesktopToolTipManager;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.haulmont.cuba.gui.upload.FileUploadingAPI.FileInfo;

public class DesktopFileUploadField extends DesktopAbstractComponent<JButton> implements FileUploadField {

    private static final int BYTES_IN_MEGABYTE = 1048576;

    protected FileUploadingAPI fileUploading;
    protected Messages messages;

    protected String icon;

    protected volatile boolean isUploadingState = false;

    protected String fileName;

    protected String description;

    protected UUID fileId;

    protected UUID tempFileId;

    protected List<Listener> listeners = new ArrayList<>();

    protected DropZone dropZone;
    protected String dropZonePrompt;

    public DesktopFileUploadField() {
        fileUploading = AppBeans.get(FileUploadingAPI.NAME);
        messages = AppBeans.get(Messages.NAME);

        final JFileChooser fileChooser = new JFileChooser();
        String caption = messages.getMessage(getClass(), "export.selectFile");
        impl = new JButton();
        impl.setAction(new AbstractAction(caption) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileChooser.showOpenDialog(impl) == JFileChooser.APPROVE_OPTION) {
                    uploadFile(fileChooser.getSelectedFile());
                }
            }
        });
    }

    protected void uploadFile(File file) {
        Configuration configuration = AppBeans.get(Configuration.NAME);
        final long maxUploadSizeMb = configuration.getConfig(ClientConfig.class).getMaxUploadSizeMb();
        final long maxSize = maxUploadSizeMb * BYTES_IN_MEGABYTE;

        if (file.length() > maxSize) {
            String warningMsg = messages.formatMainMessage("upload.fileTooBig.message", file.getName(), maxUploadSizeMb);

            getFrame().showNotification(warningMsg, IFrame.NotificationType.WARNING);
        } else {
            boolean success = true;
            try {
                isUploadingState = true;

                fileName = file.getAbsolutePath();
                notifyListenersStart(file);

                FileInfo fileInfo = fileUploading.createFile();
                tempFileId = fileInfo.getId();
                File tmpFile = fileInfo.getFile();

                FileUtils.copyFile(file, tmpFile);

                fileId = tempFileId;

                isUploadingState = false;
            } catch (Exception ex) {
                success = false;
                try {
                    fileUploading.deleteFile(tempFileId);
                    tempFileId = null;
                } catch (FileStorageException e) {
                    throw new RuntimeException("Unable to delete file from temp storage", ex);
                }
                notifyListenersFail(file, ex);
            } finally {
                notifyListenersFinish(file);
            }
            if (success) {
                notifyListenersSuccess(file);
            }
        }
    }

    protected void notifyListenersSuccess(File file) {
        final Listener.Event e = new Listener.Event(file.getName());
        for (Listener listener : listeners) {
            listener.uploadSucceeded(e);
        }
    }

    protected void notifyListenersFail(File file, Exception ex) {
        final Listener.Event failedEvent = new Listener.Event(file.getName(), ex);
        for (Listener listener : listeners) {
            listener.uploadFailed(failedEvent);
        }
    }

    protected void notifyListenersFinish(File file) {
        final Listener.Event finishedEvent = new Listener.Event(file.getName());
        for (Listener listener : listeners) {
            listener.uploadFinished(finishedEvent);
        }
    }

    protected void notifyListenersStart(File file) {
        final Listener.Event startedEvent = new Listener.Event(file.getName());
        for (Listener listener : listeners) {
            listener.uploadStarted(startedEvent);
        }
    }

    @Override
    public String getFileName() {
        String[] strings = fileName.split("[/\\\\]");
        return strings[strings.length - 1];
    }

    @Override
    public FileDescriptor getFileDescriptor() {
        if (fileId != null)
            return fileUploading.getFileDescriptor(fileId, fileName);
        else
            return null;
    }

    @Override
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
            throw new RuntimeException("Unable to read file content from temp storage", e);
        }

        return bytes;
    }

    @Override
    public UUID getFileId() {
        return fileId;
    }

    @Override
    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public String getCaption() {
        return impl.getText();
    }

    @Override
    public void setCaption(String caption) {
        impl.setText(caption);
    }

    @Override
    public String getDescription() {
        return impl.getToolTipText();
    }

    @Override
    public void setDescription(String description) {
        impl.setToolTipText(description);
        DesktopToolTipManager.getInstance().registerTooltip(impl);
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
        if (icon != null)
            impl.setIcon(App.getInstance().getResources().getIcon(icon));
        else
            impl.setIcon(null);
    }

    @Override
    public String getAccept() {
        // do nothing
        return null;
    }

    @Override
    public void setAccept(String accept) {
        // do nothing
    }

    @Override
    public DropZone getDropZone() {
        return dropZone;
    }

    @Override
    public void setDropZone(DropZone dropZone) {
        this.dropZone = dropZone;
    }

    @Override
    public String getDropZonePrompt() {
        return dropZonePrompt;
    }

    @Override
    public void setDropZonePrompt(String dropZonePrompt) {
        this.dropZonePrompt = dropZonePrompt;
    }
}