/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.toolkit.ui;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.BaseTheme;

/**
 * @author krivopustov
 * @version $Id$
 */
public class CubaRowsCount extends CustomComponent {

    protected Button prevButton;
    protected Button nextButton;
    protected Label label;
    protected Button countButton;

    public CubaRowsCount() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(false);
        layout.setMargin(new MarginInfo(false, true, false, true));

        setCompositionRoot(layout);

        CubaPlaceHolder expander = new CubaPlaceHolder();
        expander.setWidth("100%");
        layout.addComponent(expander);
        layout.setExpandRatio(expander, 1);

        HorizontalLayout contentLayout = new HorizontalLayout();
        contentLayout.setSpacing(true);
        contentLayout.setHeight("-1px");

        prevButton = new Button("<");
        prevButton.setWidth("-1px");
        prevButton.setStyleName("cuba-paging-change-page");
        contentLayout.addComponent(prevButton);

        label = new Label();
        label.setWidth("-1px");
        contentLayout.addComponent(label);

        countButton = new Button("[?]");
        countButton.setWidth("-1px");
        countButton.setStyleName(BaseTheme.BUTTON_LINK);
        contentLayout.addComponent(countButton);

        nextButton = new Button(">");
        nextButton.setWidth("-1px");
        nextButton.setStyleName("cuba-paging-change-page");
        contentLayout.addComponent(nextButton);

        layout.addComponent(contentLayout);

        layout.setWidth("100%");
        setWidth("100%");
    }

    public Label getLabel() {
        return label;
    }

    public Button getCountButton() {
        return countButton;
    }

    public Button getPrevButton() {
        return prevButton;
    }

    public Button getNextButton() {
        return nextButton;
    }
}