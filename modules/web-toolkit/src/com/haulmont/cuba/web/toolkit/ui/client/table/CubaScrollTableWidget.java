/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.haulmont.cuba.web.toolkit.ui.client.Tools;
import com.haulmont.cuba.web.toolkit.ui.client.logging.ClientLogger;
import com.haulmont.cuba.web.toolkit.ui.client.logging.ClientLoggerFactory;
import com.vaadin.client.Focusable;
import com.vaadin.client.UIDL;
import com.vaadin.client.Util;
import com.vaadin.client.VConsole;
import com.vaadin.client.ui.ShortcutActionHandler;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.client.ui.VScrollTable;

import java.util.Iterator;

/**
 * @author devyatkin
 * @version $Id$
 */
public class CubaScrollTableWidget extends VScrollTable implements ShortcutActionHandler.ShortcutActionHandlerOwner {

    protected static final String WIDGET_CELL_CLASSNAME = "widget-container";

    protected ShortcutActionHandler shortcutHandler;

    protected boolean textSelectionEnabled = false;
    protected boolean allowPopupMenu = true;

    protected int sortClickCounter = 0;

    protected ClientLogger logger = ClientLoggerFactory.getLogger("CubaScrollTableWidget");

    protected VOverlay presentationsEditorPopup;

    protected Widget presentationsMenu;

    protected CubaScrollTableWidget() {
        // handle shortcuts
        DOM.sinkEvents(getElement(), Event.ONKEYDOWN);
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

        final int type = DOM.eventGetType(event);
        if (type == Event.ONKEYDOWN && shortcutHandler != null) {
            shortcutHandler.handleKeyboardEvent(event);
        }
    }

    public void setShortcutActionHandler(ShortcutActionHandler handler) {
        this.shortcutHandler = handler;
    }

    @Override
    public ShortcutActionHandler getShortcutActionHandler() {
        return shortcutHandler;
    }

    public void setPresentationsMenu(Widget presentationsMenu) {
        if (this.presentationsMenu != presentationsMenu) {
            Style presentationsIconStyle = ((CubaScrollTableHead) tHead).presentationsEditIcon.getElement().getStyle();
            if (presentationsMenu == null) {
                presentationsIconStyle.setDisplay(Style.Display.NONE);
            } else {
                presentationsIconStyle.setDisplay(Style.Display.BLOCK);
            }
        }
        this.presentationsMenu = presentationsMenu;
    }

    @Override
    protected VScrollTableBody createScrollBody() {
        return new CubaScrollTableBody();
    }

    @Override
    public void updateActionMap(UIDL mainUidl) {
        UIDL actionsUidl = mainUidl.getChildByTagName("actions");
        if (actionsUidl == null) {
            return;
        }

        final Iterator<?> it = actionsUidl.getChildIterator();
        while (it.hasNext()) {
            final UIDL action = (UIDL) it.next();
            final String key = action.getStringAttribute("key");
            final String caption = action.getStringAttribute("caption");
            if (!action.hasAttribute("kc")) {
                actionMap.put(key + "_c", caption);
                if (action.hasAttribute("icon")) {
                    // TODO need some uri handling ??
                    actionMap.put(key + "_i", client.translateVaadinUri(action
                            .getStringAttribute("icon")));
                } else {
                    actionMap.remove(key + "_i");
                }
            }
        }
    }

    @Override
    public void handleBodyContextMenu(ContextMenuEvent event) {
        if (allowPopupMenu) {
            super.handleBodyContextMenu(event);
        }
    }

    @Override
    protected int getDynamicBodyHeight() {
        if (totalRows <= 0) {
            return (int) Math.round(scrollBody.getRowHeight(true));
        }

        return (int) Math.round(totalRows * scrollBody.getRowHeight(true));
    }

    @Override
    protected TableHead createTableHead() {
        return new CubaScrollTableHead();
    }

    protected class CubaScrollTableHead extends TableHead {

        protected final SimplePanel presentationsEditIcon = GWT.create(SimplePanel.class);

        public CubaScrollTableHead() {
            presentationsEditIcon.getElement().setClassName("cuba-table-presentations-icon");

            DOM.setStyleAttribute(presentationsEditIcon.getElement(), "display", "none");

            Element columnSelector = (Element) getElement().getLastChild();
            DOM.insertChild(getElement(), presentationsEditIcon.getElement(), DOM.getChildIndex(getElement(), columnSelector));

            DOM.sinkEvents(presentationsEditIcon.getElement(), Event.ONCLICK);
        }

        @Override
        public void onBrowserEvent(Event event) {
            super.onBrowserEvent(event);

            if (event.getEventTarget().cast() == presentationsEditIcon.getElement()) {
                presentationsEditorPopup = new VOverlay();
                presentationsEditorPopup.setStyleName("cuba-table-presentations-editor");
                presentationsEditorPopup.setOwner(CubaScrollTableWidget.this);
                presentationsEditorPopup.setWidget(presentationsMenu);

                presentationsEditorPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
                    @Override
                    public void onClose(CloseEvent<PopupPanel> event) {
                        presentationsEditorPopup = null;
                    }
                });

                presentationsEditorPopup.setAutoHideEnabled(true);
                presentationsEditorPopup.showRelativeTo(presentationsEditIcon);
            }
        }

        @Override
        protected HeaderCell createHeaderCell(String cid, String caption) {
            return new CubaScrollTableHeaderCell(cid, caption);
        }
    }

    protected class CubaScrollTableHeaderCell extends HeaderCell {

        public CubaScrollTableHeaderCell(String colId, String headerText) {
            super(colId, headerText);
        }

        @Override
        protected void sortColumn() {
            // CAUTION copied from superclass
            // Added ability to reset sort order
            boolean reloadDataFromServer = true;

            if (cid.equals(sortColumn)) {
                if (sortColumn == null) {
                    // anyway sort ascending
                    client.updateVariable(paintableId, "sortascending", !sortAscending, false);
                } else if (sortAscending) {
                    if (sortClickCounter < 2) {
                        // special case for initial revert sorting instead of reset sort order
                        if (sortClickCounter == 0) {
                            client.updateVariable(paintableId, "sortascending", !sortAscending, false);
                        } else {
                            reloadDataFromServer = false;
                            sortClickCounter = 0;
                            sortColumn = null;
                            sortAscending = true;

                            client.updateVariable(paintableId, "resetsortorder", "", true);
                        }
                    } else {
                        client.updateVariable(paintableId, "sortascending", !sortAscending, false);
                    }
                } else {
                    if (sortClickCounter < 2) {
                        // special case for initial revert sorting instead of reset sort order
                        if (sortClickCounter == 0) {
                            client.updateVariable(paintableId, "sortascending", !sortAscending, false);
                        } else {
                            reloadDataFromServer = false;
                            sortClickCounter = 0;
                            sortColumn = null;
                            sortAscending = true;

                            client.updateVariable(paintableId, "resetsortorder", "", true);
                        }
                    } else {
                        reloadDataFromServer = false;
                        sortClickCounter = 0;
                        sortColumn = null;
                        sortAscending = true;

                        client.updateVariable(paintableId, "resetsortorder", "", true);
                    }
                }
                sortClickCounter++;
            } else {
                sortClickCounter = 0;

                // set table sorted by this column
                client.updateVariable(paintableId, "sortcolumn", cid, false);
            }

            if (reloadDataFromServer) {
                // get also cache columns at the same request
                scrollBodyPanel.setScrollPosition(0);
                firstvisible = 0;
                rowRequestHandler.setReqFirstRow(0);
                rowRequestHandler.setReqRows((int) (2 * pageLength
                        * cache_rate + pageLength));
                rowRequestHandler.deferRowFetch(); // some validation +
                // defer 250ms
                rowRequestHandler.cancel(); // instead of waiting
                rowRequestHandler.run(); // run immediately
            }
        }
    }

    protected class CubaScrollTableBody extends VScrollTableBody {

        protected Widget lastFocusedWidget = null;

        @Override
        protected VScrollTableRow createRow(UIDL uidl, char[] aligns2) {
            if (uidl.hasAttribute("gen_html")) {
                // This is a generated row.
                return new VScrollTableGeneratedRow(uidl, aligns2);
            }
            return new CubaScrollTableRow(uidl, aligns2);
        }

        protected class CubaScrollTableRow extends VScrollTableRow {

            public CubaScrollTableRow(UIDL uidl, char[] aligns) {
                super(uidl, aligns);
            }

            @Override
            protected void initCellWithWidget(final Widget w, char align,
                                              String style, boolean sorted, TableCellElement td) {
                super.initCellWithWidget(w, align, style, sorted, td);

                td.getFirstChildElement().addClassName(WIDGET_CELL_CLASSNAME);

                // Support for #PL-2080
                recursiveAddFocusHandler(w, w);
            }

            protected void recursiveAddFocusHandler(final Widget w, final Widget topWidget) {
                if (w instanceof HasWidgets) {
                    for (Widget child: (HasWidgets)w) {
                        recursiveAddFocusHandler(child, topWidget);
                    }
                } else if (w instanceof HasFocusHandlers) {
                    ((HasFocusHandlers) w).addFocusHandler(new FocusHandler() {
                        @Override
                        public void onFocus(FocusEvent event) {
                            if (childWidgets.indexOf(topWidget) < 0) {
                                return;
                            }

                            lastFocusedWidget = w;

                            if (logger.enabled) {
                                logger.log("onFocus: Focus widget in column: " + childWidgets.indexOf(topWidget));
                            }

                            if (!isSelected()) {
                                deselectAll();

                                toggleSelection();
                                setRowFocus(CubaScrollTableRow.this);

                                sendSelectedRows();
                            }
                        }
                    });
                }
            }

            protected void handleFocusForWidget() {
                if (lastFocusedWidget == null) {
                    return;
                }

                logger.log("Handle focus");

                if (isSelected()) {
                    if (lastFocusedWidget instanceof Focusable) {
                        ((Focusable) lastFocusedWidget).focus();

                        if (logger.enabled) {
                            logger.log("onSelect: Focus widget");
                        }
                    } else if (lastFocusedWidget instanceof com.google.gwt.user.client.ui.Focusable) {
                        ((com.google.gwt.user.client.ui.Focusable) lastFocusedWidget).setFocus(true);

                        if (logger.enabled) {
                            logger.log("onSelect: Focus GWT widget");
                        }
                    }
                }

                lastFocusedWidget = null;
            }

            @Override
            public void onBrowserEvent(Event event) {
                super.onBrowserEvent(event);

                if (event.getTypeInt() == Event.ONMOUSEUP) {
                    handleFocusForWidget();
                }
            }

            @Override
            protected Element getEventTargetTdOrTr(Event event) {
                final Element eventTarget = event.getEventTarget().cast();
                final Element eventTargetParent = DOM.getParent(eventTarget);
                Widget widget = Util.findWidget(eventTarget, null);
                final Element thisTrElement = getElement();

                if (widget != this) {
                    if (event.getTypeInt() == Event.ONMOUSEUP) {
                        if (widget instanceof Focusable || widget instanceof com.google.gwt.user.client.ui.Focusable) {
                            lastFocusedWidget = widget;
                        }
                    }
                    // find cell
                    Element tdElement = eventTargetParent;
                    while (DOM.getParent(tdElement) != thisTrElement) {
                        tdElement = DOM.getParent(tdElement);
                    }
                    return tdElement;
                }
                return getTdOrTr(eventTarget);
            }

            @Override
            protected void initCellWithText(String text, char align, String style, boolean textIsHTML,
                                            boolean sorted, String description, TableCellElement td) {
                super.initCellWithText(text, align, style, textIsHTML, sorted, description, td);

                Element tdElement = td.cast();
                Tools.textSelectionEnable(tdElement, textSelectionEnabled);
            }

            @Override
            protected void updateCellStyleNames(TableCellElement td, String primaryStyleName) {
                Element container = td.getFirstChild().cast();
                boolean isWidget = container.getClassName() != null
                        && container.getClassName().contains(WIDGET_CELL_CLASSNAME);

                super.updateCellStyleNames(td, primaryStyleName);

                if (isWidget) {
                    container.addClassName(WIDGET_CELL_CLASSNAME);
                }
            }

            @Override
            public void showContextMenu(Event event) {
                if (allowPopupMenu && enabled && actionKeys != null) {
                    // Show context menu if there are registered action handlers
                    int left = Util.getTouchOrMouseClientX(event)
                            + Window.getScrollLeft();
                    int top = Util.getTouchOrMouseClientY(event)
                            + Window.getScrollTop();

                    selectRowForContextMenuActions(event);

                    super.showContextMenu(left, top);
                }
            }

            protected void selectRowForContextMenuActions(Event event) {
                boolean clickEventSent = handleClickEvent(event, getElement(), false);
                if (isSelectable()) {
                    boolean currentlyJustThisRowSelected = selectedRowKeys
                            .size() == 1
                            && selectedRowKeys.contains(getKey());

                    if (!currentlyJustThisRowSelected) {
                        if (isSingleSelectMode()
                                || isMultiSelectModeDefault()) {
                            deselectAll();
                        }
                        toggleSelection();
                    } else if ((isSingleSelectMode() || isMultiSelectModeSimple())
                            && nullSelectionAllowed) {
                        toggleSelection();
                    }

                    selectionRangeStart = this;
                    setRowFocus(this);

                    // Queue value change
                    sendSelectedRows(true);
                }
                if (immediate || clickEventSent) {
                    client.sendPendingVariableChanges();
                }
            }
        }
    }
}