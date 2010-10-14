/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: FONTANENKO VASILIY
 * Created: 12.10.2010 19:21:36
 *
 * $Id$
 */
package com.haulmont.cuba.report.formatters;

import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.report.Band;
import com.haulmont.cuba.report.ReportOutputType;
import com.haulmont.cuba.report.exception.ReportFormatterException;
import com.haulmont.cuba.report.formatters.tools.*;
import static com.haulmont.cuba.report.formatters.tools.ODTHelper.*;
import static com.haulmont.cuba.report.formatters.tools.ODTTableHelper.*;
import static com.haulmont.cuba.report.formatters.tools.ODTUnoConverter.*;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XIndexAccess;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.io.IOException;
import com.sun.star.io.XInputStream;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.table.XCell;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextTable;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XReplaceable;
import com.sun.star.util.XSearchDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DocFormatter extends AbstractFormatter {
    private OOOConnection connection;
    private Band rootBand;
    private FileDescriptor templateFileDescriptor;
    private ReportOutputType reportOutputType;
    private XComponent xComponent;

    private Log log = LogFactory.getLog(DocFormatter.class);

    public DocFormatter(FileDescriptor templateFileDescriptor, ReportOutputType reportOutputType) {
        String openOfficePath = ConfigProvider.getConfig(ServerConfig.class).getOpenOfficePath();
        try {
            connection = OOOConnector.createConnection(openOfficePath);
        } catch (Exception ex) {
            throw new RuntimeException("Please check OpenOffice path: " + openOfficePath);
        }
        this.templateFileDescriptor = templateFileDescriptor;
        this.reportOutputType = reportOutputType;
    }

    public byte[] createDocument(Band rootBand) {
        this.rootBand = rootBand;

        try {
            XInputStream xis = getXInputStream(templateFileDescriptor);
            xComponent = loadXComponent(connection.createXComponentLoader(), xis);
            // Handling tables
            fillTables();
            // Handling text
            replaceAllAliasesInDocument();
            // Saving document to output stream and closing
            return saveAndClose(xComponent);
        } catch (java.lang.Exception ex) {
            throw new java.lang.RuntimeException(ex);
        }
    }

    private byte[] saveAndClose(XComponent xComponent) throws IOException {
        OOOutputStream ooos = new OOOutputStream();
        String filterName;
        if (ReportOutputType.PDF.equals(reportOutputType)) {
            filterName = "writer_pdf_Export";
        } else {
            filterName = "writer8";
        }
        saveXComponent(xComponent, ooos, filterName);
        closeXComponent(xComponent);
        return ooos.toByteArray();
    }

    private void fillTables() throws com.sun.star.uno.Exception {
        String[] tablesNames = getTablesNames(xComponent);
        List<String> bandTables = new ArrayList<String>();
        Set<String> bandDefinitions = rootBand.getBandDefinitionNames();
        for (String tableName : tablesNames){
            if (bandDefinitions.contains(tableName))
                bandTables.add(tableName);
        }
        
        XDispatchHelper xDispatchHelper = connection.createXDispatchHelper();
        for (String tableName : bandTables) {
            Band band = findBand(rootBand, tableName);
            XTextTable xTextTable = getTableByName(xComponent, tableName);
            if (band != null) {
                // todo remove this hack!
                // try to select one cell without it workaround
                int columnCount = xTextTable.getColumns().getCount();
                if (columnCount < 2)
                    xTextTable.getColumns().insertByIndex(columnCount, 1);
                fillTable(tableName, band.getParentBand(), xTextTable, xDispatchHelper);
                // end of workaround ->
                if (columnCount < 2)
                    xTextTable.getColumns().removeByIndex(columnCount, 1);
            } else {
                if (haveValueExpressions(xTextTable))
                    deleteLastRow(xTextTable);
            }
        }
    }

    public boolean haveValueExpressions(XTextTable xTextTable) {
        int lastrow = xTextTable.getRows().getCount() - 1;
        try {
            for (int i = 0; i < xTextTable.getColumns().getCount(); i++) {
                String templateText = asXText(ODTTableHelper.getXCell(xTextTable, i, lastrow)).getString();
                if (templateText.matches(UNIVERSAL_ALIAS_PATTERN)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new ReportFormatterException(e);
        }
        return false;
    }

    private void fillTable(String name, Band parentBand, XTextTable xTextTable, XDispatchHelper xDispatchHelper) throws com.sun.star.uno.Exception {
        ClipBoardHelper.clear();
        for (int i = 0; i < parentBand.getChildren().size(); i++) {
            if (name.equals(parentBand.getChildren().get(i).getName())) {
                duplicateLastRow(xDispatchHelper, asXTextDocument(xComponent).getCurrentController(), xTextTable);
            }
        }
        int i = 0;
        for (Band child : parentBand.getChildren()) {
            if (name.equals(child.getName())) {
                fillRow(child, xTextTable, i);
                i++;
            }
        }
        deleteLastRow(xTextTable);
    }

    private void fillRow(Band band, XTextTable xTextTable, int row) throws com.sun.star.lang.IndexOutOfBoundsException, NoSuchElementException, WrappedTargetException {
        int colCount = xTextTable.getColumns().getCount();
        for (int col = 0; col < colCount; col++) {
            fillCell(band, getXCell(xTextTable, col, row));
        }
    }

    private void fillCell(Band band, XCell xCell) throws NoSuchElementException, WrappedTargetException {
        XEnumeration paragraphs = asXEnumerationAccess(xCell).createEnumeration();
        while (paragraphs.hasMoreElements()) {
            Object paragraph = paragraphs.nextElement();
            // todo: check here that paragraph is not table
            XEnumeration textPortions = asXEnumerationAccess(paragraph).createEnumeration();
            while (textPortions.hasMoreElements()) {
                XTextRange textPortion = asXTextRange(textPortions.nextElement());
                String portionText = textPortion.getString();
                textPortion.setString(insertBandDataToString(band, portionText));
            }
        }
    }

    /**
     * Replaces all aliases (${bandname.paramname} in document text).
     * If there is not appropriate band or alias is bad - throws RuntimeException
     */
    private void replaceAllAliasesInDocument() {
        XTextDocument xTextDocument = asXTextDocument(xComponent);
        XReplaceable xReplaceable = (XReplaceable) UnoRuntime.queryInterface(XReplaceable.class, xTextDocument);
        XSearchDescriptor searchDescriptor = xReplaceable.createSearchDescriptor();
        searchDescriptor.setSearchString(ALIAS_WITH_BAND_NAME_PATTERN);
        try {
            searchDescriptor.setPropertyValue("SearchRegularExpression", true);
            XIndexAccess indexAccess = xReplaceable.findAll(searchDescriptor);
            for (int i = 0; i < indexAccess.getCount(); i++) {
                XTextRange o = asXTextRange(indexAccess.getByIndex(i));
                String alias = unwrapParameterName(o.getString());
                String[] parts = alias.split("\\.");

                if (parts == null || parts.length < 2)
                    throw new ReportFormatterException("Bad alias : " + o.getString());

                String bandName = parts[0];
                Band band = bandName.equals("Root") ? rootBand : rootBand.getChildByName(bandName);

                if (band == null) throw new ReportFormatterException("No band for alias : " + alias);
                StringBuffer paramName = new StringBuffer();
                for (int j = 1; j < parts.length; j++) {
                    paramName.append(parts[j]);
                    if (j != parts.length - 1) paramName.append(".");
                }

                Object parameter = band.getParameter(paramName.toString());
                o.setString(parameter != null ? parameter.toString() : "");
            }
        } catch (Exception ex) {
            throw new ReportFormatterException(ex);
        }
    }

    private Band findBand(Band band, String name) {
        if (band.getName().equals(name)) {
            return band;
        }
        for (Band child : band.getChildren()) {
            Band fromChild = findBand(child, name);
            if (fromChild != null) {
                return fromChild;
            }
        }
        return null;
    }
}
