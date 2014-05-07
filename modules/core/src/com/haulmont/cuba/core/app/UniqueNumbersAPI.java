/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.app;

/**
 * Provides sequences of unique numbers based on database sequences.
 *
 * @author krivopustov
 * @version $Id$
 */
public interface UniqueNumbersAPI {

    String NAME = "cuba_UniqueNumbers";
    
    /**
     * Returns the next sequence value.
     *
     * @param domain    sequence identifier
     * @return          next value
     */
    long getNextNumber(String domain);

    /**
     * Returns the current value of the sequence. For some implementations (particularly PostgreSQL)
     * {@link #getNextNumber(String)} must be called at least once beforehand.
     *
     * @param domain    sequence identifier
     * @return          current value
     */
    long getCurrentNumber(String domain);

    /**
     * Set current value for the sequence.
     *
     * @param domain    sequence identifier
     * @param value     value
     */
    void setCurrentNumber(String domain, long value);

    /**
     * Removes database sequence with specified identifier
     * Sequence exist check is not performed, so database can throw an exception
     * @param domain sequence identifier
     */
    void deleteDbSequence(String domain);
}
