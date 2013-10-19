/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.cuba.gui.components.MaskedField;
import com.haulmont.cuba.web.toolkit.ui.MaskedTextField;

/**
 * @author devyatkin
 * @version $Id$
 */
public class WebMaskedField extends WebAbstractTextField<MaskedTextField> implements MaskedField {

    @Override
    public void setMask(String mask) {
        component.setMask(mask);
    }

    @Override
    public String getMask() {
        return component.getMask();
    }

    @Override
    public void setValueMode(ValueMode mode) {
        component.setMaskedMode(mode == ValueMode.MASKED);
    }

    @Override
    public ValueMode getValueMode() {
        return component.isMaskedMode() ? ValueMode.MASKED : ValueMode.CLEAR;
    }

    @Override
    protected MaskedTextField createTextFieldImpl() {
        return new MaskedTextField();
    }

    @Override
    public void setDatatype(Datatype datatype) {
        //Do nothing
    }

    @Override
    protected Datatype getActualDatatype(){
        return null;
    }
}
