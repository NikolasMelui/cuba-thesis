/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui.client.gridlayout;

import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ui.VGridLayout;
import com.vaadin.client.ui.layout.ComponentConnectorLayoutSlot;

/**
 * @author devyatkin
 * @version $Id$
 */
public class CubaGridLayoutWidget extends VGridLayout {

    public VGridLayout.Cell[][] getCellMatrix() {
        return cells;
    }

    public class CubaGridLayoutCell extends Cell {

        public CubaGridLayoutCell(int row, int col) {
            super(row, col);
        }

        @Override
        protected ComponentConnectorLayoutSlot createComponentConnectorLayoutSlot(ComponentConnector component) {
            return new CubaGridLayoutComponentSlot(VGridLayout.CLASSNAME, component, getConnector());
        }
    }

    @Override
    public Cell createNewCell(int row, int col) {
        // CAUTION copied from VGridLayout.createNewCell(int row, int col)
        Cell cell = new CubaGridLayoutCell(row, col);
        cells[col][row] = cell;
        return cell;
    }
}
