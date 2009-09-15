/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 04.09.2009 18:19:34
 *
 * $Id$
 */
package com.haulmont.cuba.toolkit.gwt.client.ui;

import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.RenderSpace;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.haulmont.cuba.toolkit.gwt.client.Tools;

public class IPageTreeTable extends IPageTable {

    private static int LEVEL_STEP_SIZE = 19;

    @Override
    protected ITableBody createBody() {
        return new IPageTreeTableBody();
    }

    protected class IPageTreeTableBody extends IPageTableBody {
        protected int groupColIndex =
                showRowHeaders ? 1 : 0;

        @Override
        protected void addRow(ITableRow row) {
            super.addRow(row);
            if (((IPageTreeTableRow) row).isExpanded()) {
                row.addStyleName("v-expanded");
            }
        }

        @Override
        protected ITableRow createRow(UIDL uidl) {
            final IPageTreeTableRow row = createRowInstance(uidl);
            if (!isCaptionRow(uidl)) {
                final int cells = DOM.getChildCount(row.getElement());
                for (int i = 0; i < cells; i++) {
                    final int w = IPageTreeTable.this
                            .getColWidth(getColKeyByIndex(i));
                    applyCellWidth(row, i, w);
                }
            }
            return row;
        }

        @Override
        protected IPageTreeTableRow createRowInstance(UIDL uidl) {
            if (isCaptionRow(uidl)) {
                return new IPageTreeTableCaptionRow(uidl, aligns);
            } else {
                return new IPageTreeTableRow(uidl, aligns);
            }
        }

        @Override
        public int getColWidth(int i) {
            if (initDone) {
                IPageTreeTableRow row = null;
                for (Object o : renderedRows) {
                    if (!(o instanceof IPageTreeTableCaptionRow)) {
                        row = (IPageTreeTableRow) o;
                        break;
                    }
                }
                if (row != null) {
                    final Element e = DOM.getChild(row.getElement(), i);
                    DOM.getElementPropertyInt(e, "offsetWidth");
                }
            }
            return 0;
        }

        @Override
        public void setColWidth(int colIndex, int w) {
            for (final Object o : renderedRows) {
                if (o instanceof IPageTreeTableCaptionRow) {
                    if (colIndex < groupColIndex) {
                        applyCellWidth((IPageTreeTableRow) o, colIndex, w);
                    } else {
                        int rowWidth = scrollbarWidthReserved > 0
                                ? calculatedWidth - scrollbarWidthReserved : calculatedWidth;
                        applyCellWidth((IPageTreeTableRow) o, groupColIndex, rowWidth);
                    }
                } else {
                    applyCellWidth((IPageTreeTableRow) o, colIndex, w);
                }
            }
        }

        protected void applyCellWidth(IPageTreeTableRow row,
                                      int colIndex, int w) {
            final Element cell = DOM.getChild(row.getElement(),
                    colIndex);
            int innerWidth = w;
            if (colIndex == groupColIndex) {
                if (row.hasChildren()) {
                    innerWidth -= (row.getLevel() * LEVEL_STEP_SIZE);
                } else {
                    innerWidth -= ((row.getLevel() + 1) * LEVEL_STEP_SIZE);
                }
            }
            DOM.setStyleAttribute(DOM.getFirstChild(cell), "width",
                    (innerWidth - CELL_CONTENT_PADDING) + "px");
            DOM.setStyleAttribute(cell, "width", w + "px");
        }

        private boolean isCaptionRow(UIDL uidl) {
            return uidl.hasAttribute("rowCaption");
        }

        protected class IPageTreeTableRow extends ITableRow {
            private boolean expanded;
            private int level;

            protected Element groupCell = null;

            public IPageTreeTableRow(UIDL uidl, char[] aligns) {
                super(uidl.getIntAttribute("key"));

                String rowStyle = uidl.getStringAttribute("rowstyle");
                if (rowStyle != null) {
                    addStyleName(CLASSNAME + "-row-" + rowStyle);
                }

                tHead.getColumnAlignments();
                int col = 0;

                // row header
                if (showRowHeaders) {
                    addCell(buildCaptionHtmlSnippet(uidl), aligns[col], "", col,
                            true);
                    col++;
                }

                if (uidl.hasAttribute("al")) {
                    actionKeys = uidl.getStringArrayAttribute("al");
                }

                if (uidl.hasAttribute("children") && uidl.getIntAttribute("children") > 0) {
                    groupCell = createGroupContainer();
                    if (uidl.hasAttribute("expanded")) {
                        expanded = true;
                    }
                }

                level = uidl.getIntAttribute("level");

                addCells(uidl, col);

                if (uidl.hasAttribute("selected") && !isSelected()) {
                    toggleSelection();
                }
            }

            @Override
            public void addCell(String text, char align, String style, int col, boolean textIsHTML) {
                // String only content is optimized by not using Label widget
                final Element td = DOM.createTD();
                final Element container = DOM.createDiv();
                String classNameTd = CLASSNAME + "-cell";
                String className = CLASSNAME + "-cell-content";
                if (allowMultiStingCells) {
                    classNameTd += " " + CLASSNAME + "-cell-wrap";
                }
                String classNameTdExt = null;
                if (style != null && !style.equals("")) {
                    className += " " + CLASSNAME + "-cell-content-" + style;
                    classNameTdExt = CLASSNAME + "-cell-" + style;
                }
                if (classNameTdExt != null) {
                    classNameTd += " " + classNameTdExt;
                }
                DOM.setElementProperty(td, "className", classNameTd);
                DOM.setElementProperty(container, "className", className);

                Element contentDiv = container;

                if (col == groupColIndex) {
                    if (groupCell != null) {
                        contentDiv = DOM.createDiv();

                        DOM.setStyleAttribute(container, "marginLeft", getLevel() * LEVEL_STEP_SIZE
                                + "px");

                        DOM.setElementProperty(contentDiv, "className", CLASSNAME + "-float");
                        DOM.appendChild(container, groupCell);
                        DOM.appendChild(container, contentDiv);
                    } else {
                        DOM.setStyleAttribute(container, "marginLeft", (getLevel() + 1) * LEVEL_STEP_SIZE
                                + "px");
                    }
                }

                setCellContent(contentDiv, text, textIsHTML);
                setCellAlignment(contentDiv, align);

                DOM.appendChild(td, container);
                DOM.appendChild(getElement(), td);
            }

            @Override
            public void addCell(Widget w, char align, String style, int col) {
                final Element td = DOM.createTD();
                final Element container = DOM.createDiv();
                String classNameTd = CLASSNAME + "-cell";
                String className = CLASSNAME + "-cell-content";
                if (allowMultiStingCells) {
                    classNameTd += " " + CLASSNAME + "-cell-wrap";
                }
                String classNameTdExt = null;
                if (style != null && !style.equals("")) {
                    className += " " + CLASSNAME + "-cell-content-" + style;
                    classNameTdExt = CLASSNAME + "-cell-" + style;
                }
                if (classNameTdExt != null) {
                    classNameTd += " " + classNameTdExt;
                }
                DOM.setElementProperty(td, "className", classNameTd);
                DOM.setElementProperty(container, "className", className);


                Element contentDiv = container;

                if (col == groupColIndex) {
                    int k;
                    if (groupCell != null) {
                        Element d = DOM.createDiv();
                        DOM.setStyleAttribute(d, "width", 9000 + "px");

                        contentDiv = DOM.createDiv();
                        DOM.setElementProperty(contentDiv, "className", CLASSNAME + "-float");
                        DOM.appendChild(d, groupCell);
                        DOM.appendChild(d, contentDiv);

                        DOM.appendChild(container, d);

                        k = getLevel();

                    } else {
                        k = getLevel() + 1;
                    }
                    DOM.setStyleAttribute(container, "marginLeft", k * LEVEL_STEP_SIZE
                            + "px");
                }

                setCellAlignment(contentDiv, align);

                DOM.appendChild(td, container);
                DOM.appendChild(getElement(), td);

                setCellContent(contentDiv, w, col);
            }

            @Override
            public RenderSpace getAllocatedSpace(Widget child) {
                int w = 0;
                int i = getColIndexOf(child);
                HeaderCell headerCell = tHead.getHeaderCell(i);
                if (headerCell != null) {
                    if (initializedAndAttached) {
                        w = headerCell.getWidth() - CELL_CONTENT_PADDING;
                    } else {
                        // header offset width is not absolutely correct value,
                        // but
                        // a best guess (expecting similar content in all
                        // columns ->
                        // if one component is relative width so are others)
                        w = headerCell.getOffsetWidth() - CELL_CONTENT_PADDING;
                    }
                }

                if (i == groupColIndex) {
                    w -= (getLevel() + 1) * LEVEL_STEP_SIZE;
                }

                return new RenderSpace(w, getRowHeight());
            }

            @Override
            public void onBrowserEvent(Event event) {
                final Element targetElement = DOM.eventGetTarget(event);
                //todo gorodnov: review this code when we will be use a multi selection
                if (Tools.isCheckbox(targetElement) || Tools.isRadio(targetElement))
                    return;

                switch (DOM.eventGetType(event)) {
                    case Event.ONCLICK:
                        handleClickEvent(event);
                        handleRowClick(event);
                        break;
                    case Event.ONDBLCLICK:
                        handleClickEvent(event);
                        break;
                    case Event.ONCONTEXTMENU:
                        handleRowClick(event);
                        showContextMenu(event);
                        break;
                    default:
                        break;
                }
            }

            public boolean hasChildren() {
                return (groupCell != null);
            }

            @Override
            protected void handleRowClick(Event event) {
                if (groupCell != null
                        && DOM.eventGetTarget(event) == groupCell) {
                    if (isExpanded()) {
                        client.updateVariable(paintableId, "collapse", getKey(), true);
                    } else {
                        client.updateVariable(paintableId, "expand", getKey(), true);
                    }
                    DOM.eventCancelBubble(event, true);
                } else {
                    super.handleRowClick(event);
                }
            }

            protected Element createGroupContainer() {
                Element groupContainer = DOM.createDiv();
                DOM.setInnerHTML(groupContainer, "&nbsp;");
                DOM.setElementProperty(groupContainer, "className", CLASSNAME + "-group-cell");
                return groupContainer;
            }

            public boolean isExpanded() {
                return expanded;
            }

            public int getLevel() {
                return level;
            }
        }

        protected class IPageTreeTableCaptionRow extends IPageTreeTableRow {
            public IPageTreeTableCaptionRow(UIDL uidl, char[] aligns) {
                super(uidl, aligns);
            }

            @Override
            protected void addCells(UIDL uidl, int col) {
                int columnCount = IPageTreeTable.this.tHead.getVisibleCellCount();

                final Element td = DOM.createTD();
                DOM.setElementAttribute(td, "colSpan", String.valueOf(columnCount));

                String classNameTd = CLASSNAME + "-cell";
                if (allowMultiStingCells) {
                    classNameTd += " " + CLASSNAME + "-cell-wrap";
                }
                DOM.setElementProperty(td, "className", classNameTd);

                final Element container = DOM.createDiv();
                DOM.setElementProperty(container, "className", CLASSNAME + "-caption-row-content");
                if (groupCell != null) {
                    final Element contentDiv = DOM.createDiv();
                    DOM.setStyleAttribute(container, "marginLeft", getLevel() * LEVEL_STEP_SIZE
                            + "px");
                    DOM.setElementProperty(contentDiv, "className", CLASSNAME + "-float");
                    DOM.setInnerText(contentDiv, uidl.getStringAttribute("rowCaption"));
                    DOM.appendChild(container, groupCell);
                    DOM.appendChild(container, contentDiv);
                } else {
                    DOM.setStyleAttribute(container, "marginLeft", (getLevel() + 1) * LEVEL_STEP_SIZE
                            + "px");
                    DOM.setInnerText(container, uidl.getStringAttribute("rowCaption"));
                }

                DOM.appendChild(td, container);
                DOM.appendChild(getElement(), td);
            }

            @Override
            protected void moveCol(int oldIndex, int newIndex) {
            }
        }
    }
}
