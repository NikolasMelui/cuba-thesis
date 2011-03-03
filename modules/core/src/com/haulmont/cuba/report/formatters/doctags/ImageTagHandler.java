/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Artamonov Yuryi
 * Created: 26.02.11 12:11
 *
 * $Id$
 */
package com.haulmont.cuba.report.formatters.doctags;

import com.sun.star.awt.Size;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.drawing.XShape;
import com.sun.star.lang.*;
import com.sun.star.text.HoriOrientation;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextRange;
import org.apache.commons.lang.StringUtils;

import java.lang.Exception;
import java.util.UUID;
import java.util.regex.Matcher;

import static com.haulmont.cuba.report.formatters.oo.ODTUnoConverter.*;

/**
 * Handle images with format string: ${image:[Width]x[Height]}
 */
public class ImageTagHandler implements TagHandler {

    private static final String DRAWING_BITMAP_TABLE = "com.sun.star.drawing.BitmapTable";
    private static final String TEXT_GRAPHIC_OBJECT = "com.sun.star.text.TextGraphicObject";
    private static final int IMAGE_FACTOR = 27;

    /**
     * Insert image in Doc document
     *
     * @param xComponent    Document object
     * @param destination   Text
     * @param textRange     Place for insert
     * @param paramValue    Image URL
     * @param paramsMatcher Matcher for parameters regexp
     * @throws Exception
     */
    public void handleTag(XComponent xComponent, XText destination, XTextRange textRange,
                          Object paramValue, Matcher paramsMatcher) throws Exception {
        boolean inserted = false;
        if (paramValue != null) {
            String imageUrl = paramValue.toString();
            if (!StringUtils.isEmpty(imageUrl)) {
                int width = Integer.parseInt(paramsMatcher.group(1));
                int height = Integer.parseInt(paramsMatcher.group(2));
                try {
                    insertImage(xComponent, destination, textRange, imageUrl, width, height);
                    inserted = true;
                } catch (Exception ignored) { }
            }
        }
        if (!inserted)
            destination.insertString(textRange, "", true);
    }

    private void insertImage(XComponent document, XText destination, XTextRange textRange,
                             String imageUrl, int width, int height)
            throws Exception {
        XMultiServiceFactory xFactory = asXMultiServiceFactory(document);
        Object oBitmapTable = xFactory.createInstance(DRAWING_BITMAP_TABLE);
        Object oImage = xFactory.createInstance(TEXT_GRAPHIC_OBJECT);
        XNameContainer xContainer = asXNameContainer(oBitmapTable);

        String fieldName = UUID.randomUUID().toString();
        xContainer.insertByName(fieldName, imageUrl);
        String internalImageURL = (String) xContainer.getByName(fieldName);

        XPropertySet imageProperties = buildImageProperties(oImage, internalImageURL);
        XTextContent xTextContent = asXTextContent(oImage);
        destination.insertTextContent(textRange, xTextContent, true);
        setImageSize(width, height, oImage, imageProperties);
    }

    private void setImageSize(int width, int height, Object oImage, XPropertySet imageProperties)
            throws Exception {
        Size aActualSize = (Size) imageProperties.getPropertyValue("ActualSize");
        aActualSize.Height = height * IMAGE_FACTOR;
        aActualSize.Width = width * IMAGE_FACTOR;
        XShape xShape = asXShape(oImage);
        xShape.setSize(aActualSize);
    }

    private XPropertySet buildImageProperties(Object oImage, String internalImageURL)
            throws Exception {
        XPropertySet imageProperties = asXPropertySet(oImage);
        imageProperties.setPropertyValue("GraphicURL", internalImageURL);
        imageProperties.setPropertyValue("HoriOrient", HoriOrientation.NONE);
        imageProperties.setPropertyValue("VertOrient", HoriOrientation.NONE);
        imageProperties.setPropertyValue("HoriOrientPosition", 0);
        imageProperties.setPropertyValue("VertOrientPosition", 0);
        return imageProperties;
    }
}