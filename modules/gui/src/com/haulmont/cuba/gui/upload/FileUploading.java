/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.upload;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.remoting.ClusterInvocationSupport;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.annotation.ManagedBean;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author artamonov
 * @version $Id$
 */
@ManagedBean(FileUploadingAPI.NAME)
public class FileUploading implements FileUploadingAPI, FileUploadingMBean {

    protected Map<UUID, File> tempFiles = new ConcurrentHashMap<>();

    /**
     * Upload buffer size.
     * Default: 64 KB
     */
    protected static final int BUFFER_SIZE = 64 * 1024;

    protected Log log = LogFactory.getLog(getClass());

    protected static final String CORE_FILE_UPLOAD_CONTEXT = "/upload";

    protected String tempDir;

    // Using injection by name here, because an application project can define several instances
    // of ClusterInvocationSupport type to work with different middleware blocks
    @Resource(name = ClusterInvocationSupport.NAME)
    protected ClusterInvocationSupport clusterInvocationSupport;

    @Inject
    protected UuidSource uuidSource;

    @Inject
    protected TimeSource timeSource;

    @Inject
    protected UserSessionSource userSessionSource;

    @Inject
    public void setConfiguration(Configuration configuration) {
        tempDir = configuration.getConfig(GlobalConfig.class).getTempDir();
    }

    @Override
    public UUID saveFile(byte[] data) throws FileStorageException {
        checkNotNull(data, "No file content");

        UUID uuid = uuidSource.createUuid();
        File dir = new File(tempDir);
        File file = new File(dir, uuid.toString());
        try {
            if (file.exists()) {
                throw new FileStorageException(FileStorageException.Type.FILE_ALREADY_EXISTS, file.getAbsolutePath());
            }

            boolean failed = false;
            try (FileOutputStream os = new FileOutputStream(file)) {
                os.write(data);
            } catch (Exception ex) {
                failed = true;
            } finally {
                if (!failed)
                    tempFiles.put(uuid, file);
            }
        } catch (Exception e) {
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, file.getAbsolutePath());
        }

        return uuid;
    }

    @Override
    public UUID saveFile(InputStream stream, UploadProgressListener listener)
            throws FileStorageException {
        if (stream == null)
            throw new NullPointerException("Null input stream for save file");
        UUID uuid = uuidSource.createUuid();
        File dir = new File(tempDir);
        File file = new File(dir, uuid.toString());
        if (file.exists()) {
            throw new FileStorageException(FileStorageException.Type.FILE_ALREADY_EXISTS, file.getAbsolutePath());
        }
        try {
            boolean failed = false;
            try (FileOutputStream fileOutput = new FileOutputStream(file)) {
                byte buffer[] = new byte[BUFFER_SIZE];
                int bytesRead;
                int totalBytes = 0;
                while ((bytesRead = stream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    if (listener != null)
                        listener.progressChanged(uuid, totalBytes);
                }
            } catch (Exception ex) {
                failed = true;
                throw ex;
            } finally {
                if (!failed)
                    tempFiles.put(uuid, file);
            }
        } catch (Exception ex) {
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, file.getAbsolutePath(), ex);
        }
        return uuid;
    }

    @Override
    public UUID createEmptyFile() throws FileStorageException {
        UUID uuid = uuidSource.createUuid();
        File dir = new File(tempDir);
        File file = new File(dir, uuid.toString());

        if (file.exists()) {
            throw new FileStorageException(FileStorageException.Type.FILE_ALREADY_EXISTS, file.getAbsolutePath());
        }

        try {
            if (file.createNewFile())
                tempFiles.put(uuid, file);
            else
                throw new FileStorageException(FileStorageException.Type.FILE_ALREADY_EXISTS, file.getAbsolutePath());
        } catch (IOException ex) {
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, file.getAbsolutePath());
        }

        return uuid;
    }

    @Override
    public UUID createNewFileId() throws FileStorageException {
        UUID uuid = uuidSource.createUuid();
        File dir = new File(tempDir);
        File file = new File(dir, uuid.toString());
        if (file.exists()) {
            throw new FileStorageException(FileStorageException.Type.FILE_ALREADY_EXISTS, file.getAbsolutePath());
        }
        tempFiles.put(uuid, file);
        return uuid;
    }

    @Override
    public File getFile(UUID fileId) {
        if (tempFiles.containsKey(fileId))
            return tempFiles.get(fileId);
        else
            return null;
    }

    @Override
    public FileDescriptor getFileDescriptor(UUID fileId, String name) {
        File file = getFile(fileId);
        int fileSize = (int) file.length();

        FileDescriptor fDesc = new FileDescriptor();

        fDesc.setSize(fileSize);
        fDesc.setExtension(FilenameUtils.getExtension(name));
        fDesc.setName(name);
        fDesc.setCreateDate(timeSource.currentTimestamp());

        return fDesc;
    }

    @Override
    public void deleteFile(UUID fileId) throws FileStorageException {
        File file = tempFiles.remove(fileId);
        if (file != null) {
            if (file.exists()) {
                boolean res = file.delete();
                if (!res)
                    log.warn("Could not delete temp file " + file.getAbsolutePath());
            }
        }
    }

    @Override
    public void deleteFileLink(String fileName) {
        Map<UUID, File> clonedFileMap = new HashMap<>(tempFiles);
        Iterator<Map.Entry<UUID, File>> iterator = clonedFileMap.entrySet().iterator();
        UUID forDelete = null;
        while ((iterator.hasNext()) && (forDelete == null)) {
            Map.Entry<UUID, File> fileEntry = iterator.next();
            if (fileEntry.getValue().getAbsolutePath().equals(fileName)) {
                forDelete = fileEntry.getKey();
            }
        }
        if (forDelete != null)
            tempFiles.remove(forDelete);
    }

    @Override
    public void putFileIntoStorage(UUID fileId, FileDescriptor fileDescr) throws FileStorageException {
        try {
            uploadFileIntoStorage(fileId, fileDescr, null);
        } catch (InterruptedIOException e) {
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, fileDescr.getId().toString());
        }

        deleteFile(fileId);
    }

    private void uploadFileIntoStorage(UUID fileId, FileDescriptor fileDescr,
                                       @Nullable UploadToStorageProgressListener listener)
            throws FileStorageException, InterruptedIOException {
        File file = getFile(fileId);

        for (Iterator<String> iterator = clusterInvocationSupport.getUrlList().iterator(); iterator.hasNext(); ) {
            String url = iterator.next()
                    + CORE_FILE_UPLOAD_CONTEXT
                    + "?s=" + userSessionSource.getUserSession().getId()
                    + "&f=" + fileDescr.toUrlParam();

            HttpPost method = new HttpPost(url);
            FileEntity entity;
            if (listener != null)
                entity = new FileStorageProgressEntity(file, "application/octet-stream", fileId, listener);
            else
                entity = new FileEntity(file, "application/octet-stream");

            method.setEntity(entity);
            HttpClient client = new DefaultHttpClient();
            try {
                HttpResponse response = client.execute(method);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    break;
                } else {
                    log.debug("Unable to upload file to " + url + "\n" + response.getStatusLine());
                    if (statusCode == HttpStatus.SC_NOT_FOUND && iterator.hasNext())
                        log.debug("Trying next URL");
                    else
                        throw new FileStorageException(FileStorageException.Type.fromHttpStatus(statusCode), fileDescr.getName());
                }
            } catch (InterruptedIOException e) {
                log.trace("Uploading has been interrupted");
                throw e;
            } catch (IOException e) {
                log.debug("Unable to upload file to " + url + "\n" + e);
                if (iterator.hasNext())
                    log.debug("Trying next URL");
                else
                    throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, fileDescr.getName(), e);
            } finally {
                client.getConnectionManager().shutdown();
            }
        }
    }

    @Override
    public FileDescriptor putFileIntoStorage(final TaskLifeCycle<Long> taskLifeCycle)
            throws FileStorageException, InterruptedIOException {

        checkNotNull(taskLifeCycle);

        UUID fileId = (UUID) taskLifeCycle.getParams().get("fileId");
        String fileName = (String) taskLifeCycle.getParams().get("fileName");

        checkNotNull(fileId);
        checkNotNull(fileName);

        UploadToStorageProgressListener progressListener = new UploadToStorageProgressListener() {
            @Override
            public void progressChanged(UUID fileId, long uploadedBytes, long totalBytes) {
                taskLifeCycle.publish(uploadedBytes);
            }
        };

        FileDescriptor fileDescriptor = getFileDescriptor(fileId, fileName);
        uploadFileIntoStorage(fileId, fileDescriptor, progressListener);

        deleteFile(fileId);

        return fileDescriptor;
    }

    @Override
    public void clearTempDirectory() {
        try {
            File dir = new File(tempDir);
            File[] files = dir.listFiles();
            if (files == null)
                throw new IllegalStateException("Not a directory: " + tempDir);
            Date currentDate = timeSource.currentTimestamp();
            for (File file : files) {
                Date fileDate = new Date(file.lastModified());
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(fileDate);
                calendar.add(Calendar.DAY_OF_YEAR, 2);
                if (currentDate.compareTo(calendar.getTime()) > 0) {
                    deleteFileLink(file.getAbsolutePath());
                    if (!file.delete()) {
                        log.warn(String.format("Could not remove temp file %s", file.getName()));
                    }
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public String showTempFiles() {
        StringBuilder builder = new StringBuilder();
        Map<UUID, File> clonedFileMap = new HashMap<>(tempFiles);
        for (Map.Entry<UUID, File> fileEntry : clonedFileMap.entrySet()) {
            builder.append(fileEntry.getKey().toString()).append(" | ");
            Date lastModified = new Date(fileEntry.getValue().lastModified());
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            builder.append(formatter.format(lastModified)).append("\n");
        }
        return builder.toString();
    }
}