/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components;

/**
 * @author abramov
 */
public interface LookupField extends OptionsField, Component.HasInputPrompt {

    String NAME = "lookupField";

    Object getNullOption();
    void setNullOption(Object nullOption);

    FilterMode getFilterMode();
    void setFilterMode(FilterMode mode);

    /**
     * @return true if the component handles new options entered by user.
     * @see LookupField.NewOptionHandler
     */
    boolean isNewOptionAllowed();
    /**
     * Makes the component handle new options entered by user.
     * @see LookupField.NewOptionHandler
     */
    void setNewOptionAllowed(boolean newOptionAllowed);

    /**
     * @return current handler
     */
    NewOptionHandler getNewOptionHandler();
    /**
     * Set handler.
     * @param newOptionHandler handler instance
     */
    void setNewOptionHandler(NewOptionHandler newOptionHandler);

    enum FilterMode {
            NO,
            STARTS_WITH,
            CONTAINS
    }

    /**
     * Interface to be implemented if {@link #setNewOptionAllowed(boolean)} is set to true.
     */
    public interface NewOptionHandler {
        /**
         * Called when user enters a value which is not in the options list, and presses Enter.
         * @param caption value entered by user
         */
        void addNewOption(String caption);
    }
}