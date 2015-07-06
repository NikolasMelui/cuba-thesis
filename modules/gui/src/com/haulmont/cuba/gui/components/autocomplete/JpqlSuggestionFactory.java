/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.autocomplete;

import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.jpql.DomainModel;
import com.haulmont.cuba.core.sys.jpql.DomainModelBuilder;
import com.haulmont.cuba.gui.components.autocomplete.impl.HintProvider;
import com.haulmont.cuba.gui.components.autocomplete.impl.HintRequest;
import com.haulmont.cuba.gui.components.autocomplete.impl.HintResponse;
import com.haulmont.cuba.gui.components.autocomplete.impl.Option;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Chevelev
 * @version $Id$
 */
public class JpqlSuggestionFactory {

//    public JpqlSuggestionFactory(int senderCursorPosition, int prefixLength) {
//        this.prefixLength = prefixLength;
//
//        startPosition = senderCursorPosition - prefixLength;
//        endPosition = senderCursorPosition;
//    }

    protected static Suggestion produce(AutoCompleteSupport sender, String value, String description, int senderCursorPosition, int prefixLength) {
        String valueSuffix = value.substring(prefixLength);
        String displayedValue;
        if (description == null) {
            displayedValue = value;
        } else {
            displayedValue = value + " (" + description + ")";
        }
        int startPosition = senderCursorPosition - prefixLength;

        return new Suggestion(sender, displayedValue, value, valueSuffix, startPosition, senderCursorPosition);
    }

    public static List<Suggestion> requestHint(String query, int queryPosition, AutoCompleteSupport sender,
                                               int senderCursorPosition) {
        return requestHint(query, queryPosition, sender, senderCursorPosition, null);
    }

    public static List<Suggestion> requestHint(String query, int queryPosition, AutoCompleteSupport sender,
                                               int senderCursorPosition, @Nullable HintProvider provider) {
        MetadataTools metadataTools = AppBeans.get(MetadataTools.NAME);
        MessageTools messageTools = AppBeans.get(MessageTools.NAME);
        DomainModelBuilder builder = new DomainModelBuilder(metadataTools, messageTools);
        DomainModel domainModel = builder.produce();
        if (provider == null) {
            provider = new HintProvider(domainModel);
        }
        try {
            HintRequest request = new HintRequest();
            request.setQuery(query);
            request.setPosition(queryPosition);
            HintResponse response = provider.requestHint(request);
            String prefix = response.getLastWord();
            List<Option> options = response.getOptionObjects();

            List<Suggestion> result = new ArrayList<Suggestion>();
            for (Option option : options) {
                Suggestion suggestion = JpqlSuggestionFactory.produce(sender, option.getValue(), option.getDescription(), senderCursorPosition, prefix == null ? 0 : prefix.length());
                result.add(suggestion);
            }
            return result;
        } catch (org.antlr.runtime.RecognitionException e) {
            throw new RuntimeException(e);
        }
    }
}