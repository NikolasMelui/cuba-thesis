/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * Author: Ilya Grachev
 * Created: 25.02.2010 18:51:03
 */
package com.haulmont.cuba.gui.data.impl;

import com.haulmont.cuba.gui.components.AggregationInfo;
import com.haulmont.cuba.gui.aggregation.Aggregation;
import com.haulmont.cuba.gui.aggregation.Aggregations;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.Range;
import com.haulmont.chile.core.datatypes.Datatypes;

import java.util.*;

public abstract class AggregatableDelegate<K> {
    protected AggregationInfo<MetaPropertyPath>[] aggregationInfos;

    public Map<Object, String> aggregate(AggregationInfo<MetaPropertyPath>[] aggregationInfos, Collection<K> itemIds) {
        if (aggregationInfos == null || aggregationInfos.length == 0) {
            throw new NullPointerException("Aggregation must be executed at least by one field");
        }

        this.aggregationInfos = aggregationInfos;

        return doAggregation(itemIds);
    }

    protected Map<Object, String> doAggregation(Collection<K> itemIds) {
        final Map<Object, String> aggregationResults = new HashMap<Object, String>();
        for (final AggregationInfo<MetaPropertyPath> aggregationInfo : aggregationInfos) {

            final Aggregation aggregation = Aggregations.getInstance()
                    .get(aggregationInfo.getPropertyPath().getRangeJavaClass());

            final Object value = doPropertyAggregation(aggregationInfo, aggregation, itemIds);

            String formattedValue;
            if (aggregationInfo.getFormatter() != null) {
                formattedValue = aggregationInfo.getFormatter().format(value);
            } else {
                MetaPropertyPath propertyPath = aggregationInfo.getPropertyPath();
                final Range range = propertyPath.getRange();
                if (range.isDatatype()) {
                    formattedValue = Datatypes.getInstance().get(aggregation.getJavaClass()).format(value);
                } else {
                    formattedValue = value.toString();
                }
            }

            aggregationResults.put(aggregationInfo.getPropertyPath(), formattedValue);
        }
        return aggregationResults;
    }

    protected Object doPropertyAggregation(
            AggregationInfo<MetaPropertyPath> aggregationInfo,
            Aggregation aggregation,
            Collection<K> itemIds
    ) {
        List items = valuesByProperty(aggregationInfo.getPropertyPath(), itemIds);
        switch (aggregationInfo.getType()) {
            case COUNT:
                return aggregation.count(items);
            case AVG:
                return aggregation.avg(items);
            case MAX:
                return aggregation.max(items);
            case MIN:
                return aggregation.min(items);
            case SUM:
                return aggregation.sum(items);
            default:
                throw new IllegalArgumentException(String.format("Unknown aggregation type: %s",
                        aggregationInfo.getType()));
        }
    }

    protected List valuesByProperty(MetaPropertyPath propertyPath, Collection<K> itemIds) {
        final List<Object> values = new ArrayList<Object>(itemIds.size());
        for (final K itemId : itemIds) {
            final Object value = getItemValue(propertyPath, itemId);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    public abstract Object getItem(K itemId);

    public abstract Object getItemValue(MetaPropertyPath property, K itemId);
}
