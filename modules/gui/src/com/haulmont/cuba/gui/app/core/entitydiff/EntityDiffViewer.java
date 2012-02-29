/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.app.core.entitydiff;

import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.EntitySnapshot;
import com.haulmont.cuba.core.global.EntityDiff;
import com.haulmont.cuba.core.global.EntityPropertyDiff;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DatasourceListener;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * <p>$Id$</p>
 *
 * @author artamonov
 */
public class EntityDiffViewer extends AbstractFrame {
    private static final long serialVersionUID = -6858393916181794311L;

    @Inject
    private EntitySnapshotsDatasource snapshotsDs;

    @Inject
    private Datasource<EntityDiff> entityDiffDs;

    @Inject
    private DiffTreeDatasource<EntityPropertyDiff> diffDs;

    @Inject
    private Table snapshotsTable;

    @Inject
    private TreeTable diffTable;

    @Inject
    private Label itemStateLabel;

    @Inject
    private Label valuesHeader;

    @Inject
    private Component itemStateField;

    @Inject
    private Component diffValuesField;

    public EntityDiffViewer(IFrame frame) {
        super(frame);
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        diffTable.setStyleProvider(new DiffStyleProvider());

        diffDs.addListener(new DatasourceListener<EntityPropertyDiff>() {
            @Override
            public void itemChanged(Datasource<EntityPropertyDiff> ds,
                                    EntityPropertyDiff prevItem, EntityPropertyDiff item) {
                boolean valuesVisible = (item != null) && (item.hasStateValues());
                boolean stateVisible = (item != null) && (item.hasStateValues() && item.itemStateVisible());

                valuesHeader.setVisible(stateVisible || valuesVisible);
                itemStateField.setVisible(stateVisible);
                diffValuesField.setVisible(valuesVisible);

                if (item != null) {
                    EntityPropertyDiff.ItemState itemState = item.getItemState();
                    if (itemState != EntityPropertyDiff.ItemState.Normal) {
                        String messageCode = "ItemState." + itemState.toString();
                        itemStateLabel.setValue(getMessage(messageCode));
                        itemStateLabel.setVisible(true);
                    } else {
                        itemStateField.setVisible(false);
                    }
                }
            }

            @Override
            public void stateChanged(Datasource<EntityPropertyDiff> ds,
                                     Datasource.State prevState, Datasource.State state) {
            }

            @Override
            public void valueChanged(EntityPropertyDiff source, String property,
                                     Object prevValue, Object value) {
            }
        });
    }

    @SuppressWarnings("unused")
    public void compareSnapshoots() {
        entityDiffDs.setItem(null);

        EntitySnapshot firstSnap = null;
        EntitySnapshot secondSnap = null;

        Set selected = snapshotsTable.getSelected();
        Object[] selectedItems = selected.toArray();
        if ((selected.size() == 2)) {
            firstSnap = (EntitySnapshot) selectedItems[0];
            secondSnap = (EntitySnapshot) selectedItems[1];
        } else if (selected.size() == 1) {
            secondSnap = (EntitySnapshot) selectedItems[0];
            firstSnap = snapshotsDs.getFirstSnapshot();
            if (firstSnap == secondSnap)
                firstSnap = null;
        }

        if ((secondSnap != null) || (firstSnap != null)) {
            EntityDiff diff = diffDs.loadDiff(firstSnap, secondSnap);
            entityDiffDs.setItem(diff);
        }

        diffTable.refresh();
        diffTable.expandAll();
    }

    public void loadVersions(BaseEntity entity) {
        snapshotsDs.setEntity(entity);
        snapshotsDs.refresh();

        snapshotsTable.repaint();
    }
}
