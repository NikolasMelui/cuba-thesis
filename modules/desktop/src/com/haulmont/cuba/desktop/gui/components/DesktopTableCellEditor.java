/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.desktop.sys.vcl.DatePicker.DatePicker;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Table;
import org.jdesktop.swingx.JXHyperlink;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.util.*;

/**
 * <p>$Id$</p>
 *
 * @author Alexander Budarov
 */
public class DesktopTableCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

    private static final long serialVersionUID = 5217563286634642347L;

    private Table.ColumnGenerator columnGenerator;
    private Component activeComponent;
    private Map<Integer, Component> cache = new HashMap<Integer, Component>();

    /*
     * true, if cells of this column hold editable content.
     * Swing treats keyboard, mouse, focus events for editable and not-editable cells differently.
     */
    private boolean editable;
    private DesktopAbstractTable desktopAbstractTable;
    private Border border;
    private Class<? extends com.haulmont.cuba.gui.components.Component> componentClass;

    private static final Set<Class> readOnlyComponentClasses = new HashSet<Class>(Arrays.asList(
            Label.class, Checkbox.class
    ));

    private static final Set<Class> inlineComponentClasses = new HashSet<Class>(Arrays.asList(
            Label.class, Checkbox.class
    ));

    public DesktopTableCellEditor(DesktopAbstractTable desktopAbstractTable, Table.ColumnGenerator columnGenerator,
                                  Class<? extends com.haulmont.cuba.gui.components.Component> componentClass) {
        this.desktopAbstractTable = desktopAbstractTable;
        this.columnGenerator = columnGenerator;
        this.componentClass = componentClass;
        this.editable = isEditableComponent(componentClass);
    }

    /*
     * If component is editable, it should gain focus from table.
     * Mouse events like mouse dragging are treated differently for editable columns.
     */
    protected boolean isEditableComponent(Class<? extends com.haulmont.cuba.gui.components.Component> componentClass) {
        if (componentClass == null) {
            return true;
        }
        for (Class readOnlyClass : readOnlyComponentClasses) {
            if (componentClass.isAssignableFrom(readOnlyClass)) {
                return false;
            }
        }
        return true;
    }

    /*
     * Inline components always fit in standard row height,
     * so there is no need to pack rows for desktop table.
     */
    public boolean isInline() {
        if (componentClass == null) {
            return false;
        }
        for (Class inlineClass : inlineComponentClasses) {
            if (componentClass.isAssignableFrom(inlineClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    protected Component getCellComponent(int row) {
        Entity item = desktopAbstractTable.getTableModel().getItem(row);

        StopWatch sw = new Log4JStopWatch("TableColumnGenerator." + desktopAbstractTable.getId());
        com.haulmont.cuba.gui.components.Component component = columnGenerator.generateCell(desktopAbstractTable, item.getId());
        sw.stop();

        Component comp;
        if (component == null)
            comp = new JLabel("");
        else
            comp = DesktopComponentsHelper.getComposition(component);
        cache.put(row, comp);
        return comp;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Component component = getCellComponent(row);
        applyStyle(component, table, true, true, row);

        String stylename = desktopAbstractTable.getStylename(table, row, column);
        desktopAbstractTable.applyStylename(isSelected, true, activeComponent, stylename);
        return component;
    }

    @Override
    public Object getCellEditorValue() {
        if (activeComponent != null) {
            // post specific event for handle focus lost
            // todo fix it for normal change focus with mouse
            FocusEvent focusEvent = new FocusEvent(activeComponent, FocusEvent.FOCUS_LOST, false, desktopAbstractTable.impl);
            activeComponent.dispatchEvent(focusEvent);
        }
        return "";
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        activeComponent = cache.get(row);
        if (activeComponent == null) {
            activeComponent = getCellComponent(row);
            cache.put(row, activeComponent);
        }

        applyStyle(activeComponent, table, isSelected, hasFocus, row);

        String stylename = desktopAbstractTable.getStylename(table, row, column);
        desktopAbstractTable.applyStylename(isSelected, hasFocus, activeComponent, stylename);

        return activeComponent;
    }

    public void clearCache() {
        cache.clear();
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(Border border) {
        this.border = border;
    }

    public void applyStyle(java.awt.Component component, JTable table, boolean isSelected, boolean hasFocus, int row) {
        if (!(component instanceof JComponent)) {
            return;
        }
        JComponent jcomponent = (JComponent) component;
        jcomponent.setOpaque(true);

        if (isSelected) {
            if (isTextField(jcomponent)) {
                // another JTextField dirty workaround. If use selectionBackground, then it's all blue
                jcomponent.setBackground(table.getBackground());
                jcomponent.setForeground(table.getForeground());
            } else {
                jcomponent.setBackground(table.getSelectionBackground());
                jcomponent.setForeground(table.getSelectionForeground());
            }
        } else {
            jcomponent.setForeground(table.getForeground());
            Color background = UIManager.getDefaults().getColor("Table:\"Table.cellRenderer\".background");
            if (row % 2 == 1) {
                Color alternateColor = UIManager.getDefaults().getColor("Table.alternateRowColor");
                if (alternateColor != null) {
                    background = alternateColor;
                }
            }
            jcomponent.setBackground(background);
        }

        jcomponent.setFont(table.getFont());

        assignBorder(table, isSelected, hasFocus, jcomponent);
    }

    private boolean isTextField(JComponent jcomponent) {
        if (jcomponent instanceof JTextField) {
            return true;
        }
        if (jcomponent instanceof JPanel) {
            Component[] panelChildren = jcomponent.getComponents();
            if ((panelChildren.length == 1 && panelChildren[0] instanceof JTextField)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLookupField(JComponent jcomponent) {
        if (jcomponent instanceof JComboBox) {
            return true;
        }
        if (jcomponent instanceof JPanel) {
            Component[] panelChildren = jcomponent.getComponents();
            if ((panelChildren.length == 1 && panelChildren[0] instanceof JComboBox)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDateField(JComponent jcomponent) {
        if (jcomponent instanceof JComboBox) {
            return true;
        }
        if (jcomponent instanceof JPanel) {
            Component[] panelChildren = jcomponent.getComponents();
            if ((panelChildren.length == 2 && panelChildren[0] instanceof DatePicker)) {
                return true;
            }
        }
        return false;
    }

    private void assignBorder(JTable table, boolean isSelected, boolean hasFocus, JComponent jcomponent) {
        if (isTextField(jcomponent)) {
            // looks like simple label when with empty border
        } else if (border != null) {
            jcomponent.setBorder(border);
        } else if (isLookupField(jcomponent) || isDateField(jcomponent) || jcomponent instanceof JXHyperlink) {
            // empty borders for fields except text fields in tables
            jcomponent.setBorder(new EmptyBorder(0, 0, 0, 0));
        } else {
            if (hasFocus) {
                Border border = null;
                if (isSelected) {
                    border = UIManager.getDefaults().getBorder("Table.focusSelectedCellHighlightBorder");
                }
                if (border == null) {
                    border = UIManager.getDefaults().getBorder("Table.focusCellHighlightBorder");
                }
                jcomponent.setBorder(border);
            } else {
                jcomponent.setBorder(getNoFocusBorder(jcomponent, table));
            }
        }
    }

    private Border getNoFocusBorder(JComponent jcomponent, JTable table) {
        return UIManager.getDefaults().getBorder("Table.cellNoFocusBorder");
    }
}
