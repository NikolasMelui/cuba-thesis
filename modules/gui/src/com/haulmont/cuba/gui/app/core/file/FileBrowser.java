/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 29.10.2009 12:24:21
 *
 * $Id$
 */
package com.haulmont.cuba.gui.app.core.file;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FileBrowser extends AbstractWindow {

    public FileBrowser(IFrame frame) {
        super(frame);
    }

    @Override
    protected void init(Map<String, Object> params) {
        super.init(params);
        final Table filesTable = getComponent("files");
        final CollectionDatasource filesDs = filesTable.getDatasource();
        filesTable.addAction(new CreateAction(filesTable, WindowManager.OpenType.DIALOG));
        filesTable.addAction(new EditAction(filesTable, WindowManager.OpenType.DIALOG));
        filesTable.addAction(new RemoveAction(filesTable));
        filesTable.addAction(new RefreshAction(filesTable));
        filesTable.addAction(new ExcelAction(filesTable, AppConfig.createExportDisplay()));

        Button uploadBtn = getComponent("multiupload");
        uploadBtn.setAction(new AbstractAction("files.multiupload") {

            public void actionPerform(Component component) {
                Map<String, Object> params = Collections.<String, Object>emptyMap();

                final Window window = frame.openEditor("multiuploadDialog", null,
                        WindowManager.OpenType.DIALOG,
                        params, null);

                window.addListener(new Window.CloseListener() {
                    public void windowClosed(String actionId) {
                        if (Window.COMMIT_ACTION_ID.equals(actionId) && window instanceof Window.Editor) {
                            List<FileDescriptor> items = ((MultiUploader) window).getFiles();
                            for (FileDescriptor fdesc : items) {
                                filesDs.addItem(fdesc);
                            }
                            if (items.size() > 0)
                                filesDs.commit();
                            filesTable.refresh();
                        }
                    }
                });
            }
        });

//        TODO generated column with download link and formatted file size
//        FileDownloadHelper.initGeneratedColumn(filesTable);
    }
}
