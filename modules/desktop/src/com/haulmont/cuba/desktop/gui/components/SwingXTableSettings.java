/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.desktop.DesktopConfig;
import com.haulmont.cuba.gui.components.Table;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;

/**
 * @author krivopustov
 * @version $Id$
 */
public class SwingXTableSettings implements TableSettings {

    private Log log = LogFactory.getLog(getClass());

    protected JXTable table;
    protected List<Table.Column> columns;

    public SwingXTableSettings(JXTable table, List<Table.Column> columns) {
        this.table = table;
        this.columns = columns;
    }

    @Override
    public boolean saveSettings(Element element) {
        element.addAttribute("horizontalScroll", String.valueOf(table.isHorizontalScrollEnabled()));

        saveFontPreferences(element);

        Element columnsElem = element.element("columns");
        if (columnsElem != null)
            element.remove(columnsElem);
        columnsElem = element.addElement("columns");

        List<TableColumn> columns = table.getColumns(true);
        Collections.sort(
                columns,
                new Comparator<TableColumn>() {
                    @Override
                    public int compare(TableColumn col1, TableColumn col2) {
                        if (col1 instanceof TableColumnExt && !((TableColumnExt) col1).isVisible())
                            return 1;
                        if (col2 instanceof TableColumnExt && !((TableColumnExt) col2).isVisible())
                            return -1;
                        int i1 = table.getColumnModel().getColumnIndex(col1.getIdentifier());
                        int i2 = table.getColumnModel().getColumnIndex(col2.getIdentifier());
                        return i1 - i2;
                    }
                }
        );

        for (TableColumn column : columns) {
            Element colElem = columnsElem.addElement("column");
            colElem.addAttribute("id", column.getIdentifier().toString());

            int width = column.getWidth();
            colElem.addAttribute("width", String.valueOf(width));

            if (column instanceof TableColumnExt) {
                Boolean visible = ((TableColumnExt) column).isVisible();
                colElem.addAttribute("visible", visible.toString());
            }
        }

        if (table.getRowSorter() != null) {
            List<? extends RowSorter.SortKey> sortKeys = table.getRowSorter().getSortKeys();
            if (!sortKeys.isEmpty()) {
                RowSorter.SortKey sortKey = sortKeys.get(0);
                columnsElem.addAttribute("sortColumn", String.valueOf(sortKey.getColumn()));
                columnsElem.addAttribute("sortOrder", sortKey.getSortOrder().toString());
            }
        }

        return true;
    }

    @Override
    public void apply(Element element, boolean sortable) {
        String horizontalScroll = element.attributeValue("horizontalScroll");
        if (!StringUtils.isBlank(horizontalScroll))
            table.setHorizontalScrollEnabled(Boolean.valueOf(horizontalScroll));

        loadFontPreferences(element);

        final Element columnsElem = element.element("columns");
        if (columnsElem == null)
            return;

        // do not allow dublicates
        Collection<Object> sequence = new LinkedHashSet<>();
        List<TableColumnExt> invisible = new ArrayList<>();
        for (Element colElem : Dom4j.elements(columnsElem, "column")) {
            String id = colElem.attributeValue("id");
            Table.Column column = getColumn(id);
            if (column != null) {
                sequence.add(column);

                TableColumnExt tableColumn = table.getColumnExt(column);

                String width = colElem.attributeValue("width");
                if ((width != null) && (tableColumn != null))
                    tableColumn.setPreferredWidth(Integer.valueOf(width));

                String visible = colElem.attributeValue("visible");
                if (visible != null && !Boolean.valueOf(visible))
                    invisible.add(tableColumn);
            }
        }
        table.setColumnSequence(sequence.toArray(new Object[sequence.size()]));

        for (TableColumnExt invisibleTableColumn : invisible) {
            invisibleTableColumn.setVisible(false);
        }

        if (sortable && table.getRowSorter() != null) {
            String sortColumn = columnsElem.attributeValue("sortColumn");
            if (sortColumn != null) {
                SortOrder sortOrder = SortOrder.valueOf(columnsElem.attributeValue("sortOrder"));
                table.getRowSorter().setSortKeys(Collections.singletonList(new RowSorter.SortKey(Integer.valueOf(sortColumn), sortOrder)));
            }
        }
    }

    protected void saveFontPreferences(Element element) {
        if (table.getFont() != null) {
            Font font = table.getFont();
            Map<TextAttribute, ?> attributes = font.getAttributes();
            // save content font
            element.addAttribute("fontFamily", font.getFamily());
            element.addAttribute("fontSize", Integer.toString(font.getSize()));
            element.addAttribute("fontStyle", Integer.toString(font.getStyle()));
            element.addAttribute("fontUnderline",
                    Boolean.toString(attributes.get(TextAttribute.UNDERLINE) == TextAttribute.UNDERLINE_ON));
        }
    }

    protected void loadFontPreferences(Element element) {
        // load font preferences
        String fontFamily = element.attributeValue("fontFamily");
        String fontSize = element.attributeValue("fontSize");
        String fontStyle = element.attributeValue("fontStyle");
        String fontUnderline = element.attributeValue("fontUnderline");
        if (!StringUtils.isBlank(fontFamily) &&
                !StringUtils.isBlank(fontSize) &&
                !StringUtils.isBlank(fontUnderline) &&
                !StringUtils.isBlank(fontStyle)) {

            try {
                int size = Integer.parseInt(fontSize);
                int style = Integer.parseInt(fontStyle);

                String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames();
                int fontIndex = Arrays.asList(availableFonts).indexOf(fontFamily);
                if (fontIndex < 0) {
                    log.debug("Unsupported font family, font settings not loaded");
                    return;
                }

                DesktopConfig desktopConfig = AppBeans.get(Configuration.class).getConfig(DesktopConfig.class);
                int sizeIndex = desktopConfig.getAvailableFontSizes().indexOf(size);

                if (sizeIndex < 0) {
                    log.debug("Unsupported font size, font settings not loaded");
                    return;
                }

                Boolean underline = BooleanUtils.toBooleanObject(fontUnderline);
                if (sizeIndex < 0) {
                    log.debug("Broken underline property in font definition, skip");
                }

                @SuppressWarnings("MagicConstant")
                Font font = new Font(fontFamily, style, size);
                if (underline != null && Boolean.TRUE.equals(underline)) {
                    Map<TextAttribute, Integer> attributes = new HashMap<>();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    font = font.deriveFont(attributes);
                }
                table.setFont(font);
            } catch (NumberFormatException ex) {
                log.debug("Broken font definition in user setting");
            }
        }
    }

    protected Table.Column getColumn(String id) {
        for (Table.Column column : columns) {
            if (column.getId().toString().equals(id))
                return column;
        }
        return null;
    }
}