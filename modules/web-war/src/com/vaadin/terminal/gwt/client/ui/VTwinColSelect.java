/*
 * Copyright 2010 IT Mill Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.terminal.gwt.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.vaadin.terminal.gwt.client.UIDL;

import java.util.ArrayList;
import java.util.Iterator;

public class VTwinColSelect extends VOptionGroupBase {

    private static final String CLASSNAME = "v-select-twincol";

    private static final int VISIBLE_COUNT = 10;

    private static final int DEFAULT_COLUMN_COUNT = 10;

    private final ListBox options;

    private final ListBox selections;

    private final VButton add;

    private final VButton remove;

    private FlowPanel buttons;

    private Panel panel;

    private boolean widthSet = false;

    public VTwinColSelect() {
        super(CLASSNAME);
        options = new ListBox();
        options.addClickHandler(this);
        selections = new ListBox();
        selections.addClickHandler(this);
        options.setVisibleItemCount(VISIBLE_COUNT);
        selections.setVisibleItemCount(VISIBLE_COUNT);
        options.setStyleName(CLASSNAME + "-options");
        selections.setStyleName(CLASSNAME + "-selections");
        buttons = new FlowPanel();
        buttons.setStyleName(CLASSNAME + "-buttons");
        add = new VButton();
        add.setText(">>");
        add.addClickHandler(this);
        remove = new VButton();
        remove.setText("<<");
        remove.addClickHandler(this);
        panel = ((Panel) optionsContainer);
        panel.add(options);
        buttons.add(add);
        final HTML br = new HTML("<span/>");
        br.setStyleName(CLASSNAME + "-deco");
        buttons.add(br);
        buttons.add(remove);
        panel.add(buttons);
        panel.add(selections);
    }

    @Override
    protected void buildOptions(UIDL uidl) {
        final boolean enabled = !isDisabled() && !isReadonly();
        options.setMultipleSelect(isMultiselect());
        selections.setMultipleSelect(isMultiselect());
        options.setEnabled(enabled);
        selections.setEnabled(enabled);
        add.setEnabled(enabled);
        remove.setEnabled(enabled);
        options.clear();
        selections.clear();
        for (final Iterator i = uidl.getChildIterator(); i.hasNext();) {
            final UIDL optionUidl = (UIDL) i.next();
            if (optionUidl.hasAttribute("selected")) {
                selections.addItem(optionUidl.getStringAttribute("caption"),
                        optionUidl.getStringAttribute("key"));
            } else {
                options.addItem(optionUidl.getStringAttribute("caption"),
                        optionUidl.getStringAttribute("key"));
            }
        }

        int cols = -1;
        if (getColumns() > 0) {
            cols = getColumns();
        } else if (!widthSet) {
            cols = DEFAULT_COLUMN_COUNT;
        }

        if (cols >= 0) {
            options.setWidth(cols + "em");
            selections.setWidth(cols + "em");
//            buttons.setWidth("3.5em");
            optionsContainer.setWidth((2 * cols + 4) + "em");
        }
        if (getRows() > 0) {
            options.setVisibleItemCount(getRows());
            selections.setVisibleItemCount(getRows());

        }

    }

    @Override
    protected String[] getSelectedItems() {
        final ArrayList<String> selectedItemKeys = new ArrayList<String>();
        for (int i = 0; i < selections.getItemCount(); i++) {
            selectedItemKeys.add(selections.getValue(i));
        }
        return selectedItemKeys.toArray(new String[selectedItemKeys.size()]);
    }

    private boolean[] getItemsToAdd() {
        final boolean[] selectedIndexes = new boolean[options.getItemCount()];
        for (int i = 0; i < options.getItemCount(); i++) {
            if (options.isItemSelected(i)) {
                selectedIndexes[i] = true;
            } else {
                selectedIndexes[i] = false;
            }
        }
        return selectedIndexes;
    }

    private boolean[] getItemsToRemove() {
        final boolean[] selectedIndexes = new boolean[selections.getItemCount()];
        for (int i = 0; i < selections.getItemCount(); i++) {
            if (selections.isItemSelected(i)) {
                selectedIndexes[i] = true;
            } else {
                selectedIndexes[i] = false;
            }
        }
        return selectedIndexes;
    }

    @Override
    public void onClick(ClickEvent event) {
        super.onClick(event);
        if (event.getSource() == add) {
            final boolean[] sel = getItemsToAdd();
            for (int i = 0; i < sel.length; i++) {
                if (sel[i]) {
                    final int optionIndex = i
                            - (sel.length - options.getItemCount());
                    selectedKeys.add(options.getValue(optionIndex));

                    // Move selection to another column
                    final String text = options.getItemText(optionIndex);
                    final String value = options.getValue(optionIndex);
                    selections.addItem(text, value);
                    selections.setItemSelected(selections.getItemCount() - 1,
                            true);
                    options.removeItem(optionIndex);
                }
            }
            client.updateVariable(id, "selected", selectedKeys
                    .toArray(new String[selectedKeys.size()]), isImmediate());

        } else if (event.getSource() == remove) {
            final boolean[] sel = getItemsToRemove();
            for (int i = 0; i < sel.length; i++) {
                if (sel[i]) {
                    final int selectionIndex = i
                            - (sel.length - selections.getItemCount());
                    selectedKeys.remove(selections.getValue(selectionIndex));

                    // Move selection to another column
                    final String text = selections.getItemText(selectionIndex);
                    final String value = selections.getValue(selectionIndex);
                    options.addItem(text, value);
                    options.setItemSelected(options.getItemCount() - 1, true);
                    selections.removeItem(selectionIndex);
                }
            }
            client.updateVariable(id, "selected", selectedKeys
                    .toArray(new String[selectedKeys.size()]), isImmediate());
        } else if (event.getSource() == options) {
            // unselect all in other list, to avoid mistakes (i.e wrong button)
            final int c = selections.getItemCount();
            for (int i = 0; i < c; i++) {
                selections.setItemSelected(i, false);
            }
        } else if (event.getSource() == selections) {
            // unselect all in other list, to avoid mistakes (i.e wrong button)
            final int c = options.getItemCount();
            for (int i = 0; i < c; i++) {
                options.setItemSelected(i, false);
            }
        }
    }

    @Override
    public void setHeight(String height) {
        super.setHeight(height);
        if ("".equals(height)) {
            options.setHeight("");
            selections.setHeight("");
        } else {
            setFullHeightInternals();
        }
    }

    private void setFullHeightInternals() {
        options.setHeight("100%");
        selections.setHeight("100%");
    }

    @Override
    public void setWidth(String width) {
        super.setWidth(width);
        if (!"".equals(width) && width != null) {
            setRelativeInternalWidths();
        }
    }

    private void setRelativeInternalWidths() {
        DOM.setStyleAttribute(getElement(), "position", "relative");
        int buttonsWidth = buttons.getOffsetWidth();
        int w = (getOffsetWidth() - buttonsWidth) / 2;
        if (w < 0) { w = 0; }
        options.setWidth(w + "px");
        selections.setWidth(w + "px");
        widthSet = true;
    }

    @Override
    protected void setTabIndex(int tabIndex) {
        options.setTabIndex(tabIndex);
        selections.setTabIndex(tabIndex);
        add.setTabIndex(tabIndex);
        remove.setTabIndex(tabIndex);
    }

    public void focus() {
        options.setFocus(true);
    }
}
