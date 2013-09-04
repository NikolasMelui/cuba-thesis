/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui.client.fieldgrouplayout;

import com.haulmont.cuba.web.toolkit.ui.client.gridlayout.CubaGridLayoutWidget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ui.layout.ComponentConnectorLayoutSlot;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaFieldGroupLayoutWidget extends CubaGridLayoutWidget {

    public static final String CLASSNAME = "cuba-fieldgrouplayout";

    public CubaFieldGroupLayoutWidget() {
        setStyleName(CLASSNAME);
    }

    public class CubaFieldGroupLayoutCell extends CubaGridLayoutCell {

        public CubaFieldGroupLayoutCell(int row, int col) {
            super(row, col);
        }

        @Override
        protected ComponentConnectorLayoutSlot createComponentConnectorLayoutSlot(ComponentConnector component) {
            return new CubaFieldGroupLayoutComponentSlot(CubaFieldGroupLayoutWidget.CLASSNAME, component, getConnector());
        }
    }

    @Override
    public Cell createNewCell(int row, int col) {
        // CAUTION copied from VGridLayout.createNewCell(int row, int col)
        Cell cell = new CubaFieldGroupLayoutCell(row, col);
        cells[col][row] = cell;
        return cell;
    }
}