/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.SearchField;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.toolkit.ui.SearchSelect;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author artamonov
 * @version $Id$
 */
public class WebSearchField extends WebLookupField implements SearchField {

    protected int minSearchStringLength = 0;
    protected Mode mode = Mode.CASE_SENSITIVE;

    protected Entity newSettingValue = null;

    protected SearchNotifications searchNotifications = new SearchNotifications() {
        @Override
        public void notFoundSuggestions(String filterString) {
            String message = messages.formatMessage("com.haulmont.cuba.gui", "searchSelect.notFound", filterString);
            App.getInstance().getWindowManager().showNotification(message, defaultNotificationType);
        }

        @Override
        public void needMinSearchStringLength(String filterString, int minSearchStringLength) {
            String message = messages.formatMessage(
                    "com.haulmont.cuba.gui", "searchSelect.minimumLengthOfFilter", minSearchStringLength);
            App.getInstance().getWindowManager().showNotification(message, defaultNotificationType);
        }
    };

    protected IFrame.NotificationType defaultNotificationType = IFrame.NotificationType.TRAY;

    @Override
    protected void createComponent() {
        this.component = new SearchSelect() {
            @Override
            public void setPropertyDataSource(Property newDataSource) {
                if (newDataSource == null) {
                    super.setPropertyDataSource(null);
                } else {
                    super.setPropertyDataSource(new LookupPropertyAdapter(newDataSource) {
                        @Override
                        public Object getValue() {
                            final Object o = itemProperty.getValue();
                            return getKeyFromValue(o);
                        }

                        @Override
                        public void setValue(Object newValue) throws ReadOnlyException, ConversionException {
                            if (!optionsInitialization) {
                                Object v;

                                if (newSettingValue != null &&
                                        ObjectUtils.equals(newSettingValue.getId(), newValue))
                                    v = newSettingValue;
                                else
                                    v = getValueFromKey(newValue);

                                if (newValue != null) {
                                    if (v == null && !items.containsId(v)) {
                                        Object valueKey = WebSearchField.super.getValue();
                                        if (newValue == valueKey)
                                            v = getValueFromDs();
                                    }
                                } else  {
                                    optionRepaint();
                                }

                                itemProperty.setValue(v);
                            }
                        }
                    });
                }
            }
        };

        getSearchComponent().setFilterHandler(new SearchSelect.FilterHandler() {
            @Override
            public void onFilterChange(String newFilter) {
                String originalFilter = newFilter;
                if (mode == Mode.LOWER_CASE) {
                    newFilter = StringUtils.lowerCase(newFilter);
                } else if (mode == Mode.UPPER_CASE) {
                    newFilter = StringUtils.upperCase(newFilter);
                }

                if (!isRequired() && StringUtils.isEmpty(newFilter)) {
                    setValue(null);
                    if (optionsDatasource.getState() == Datasource.State.VALID) {
                        optionsDatasource.clear();
                    }
                    return;
                }

                if (StringUtils.length(newFilter) >= minSearchStringLength) {
                    optionsDatasource.refresh(Collections.singletonMap(SEARCH_STRING_PARAM, (Object) newFilter));
                    if (optionsDatasource.getState() == Datasource.State.VALID && optionsDatasource.size() == 1) {
                        Object id = optionsDatasource.getItemIds().iterator().next();
                        setValue(optionsDatasource.getItem(id));
                    }

                    if (searchNotifications != null) {
                        if (optionsDatasource.getState() == Datasource.State.VALID && optionsDatasource.size() == 0)
                            searchNotifications.notFoundSuggestions(originalFilter);
                    }
                } else {
                    if (optionsDatasource.getState() == Datasource.State.VALID) {
                        optionsDatasource.clear();
                    }

                    if (searchNotifications != null)
                        searchNotifications.needMinSearchStringLength(originalFilter, minSearchStringLength);
                }
            }
        });
    }

    @Override
    protected Object getKeyFromValue(Object value) {
        Object key = super.getKeyFromValue(value);
        if (value instanceof Entity && key == null && optionsDatasource.getItemIds().size() == 0)
            key = ((Entity) value).getId();
        return key;
    }

    @Override
    protected <T> T getValueFromKey(Object key) {
        T v = super.getValueFromKey(key);
        Object valueFromDs;
        if (datasource != null)
            valueFromDs = getValueFromDs();
        else
            valueFromDs = newSettingValue;

        if (key != null && v == null && optionsDatasource.getItemIds().size() == 0 &&
                valueFromDs instanceof Entity && key.equals(((Entity) valueFromDs).getId()))
            v = (T) valueFromDs;

        return v;
    }

    private SearchSelect getSearchComponent() {
        return (SearchSelect) component;
    }

    @Override
    public void setOptionsDatasource(CollectionDatasource datasource) {
        this.optionsDatasource = datasource;
        component.setContainerDataSource(new CarelessDsWrapper(datasource, true));

        if (captionProperty != null) {
            component.setItemCaptionPropertyId(optionsDatasource.getMetaClass().getProperty(captionProperty));
        }
    }

    @Override
    public void setValue(@Nullable Object value) {
        if (value instanceof Entity)
            newSettingValue = (Entity) value;
        else
            newSettingValue = null;

        super.setValue(value);
    }

    @Override
    public void setMinSearchStringLength(int searchStringLength) {
        this.minSearchStringLength = searchStringLength;
    }

    @Override
    public int getMinSearchStringLength() {
        return minSearchStringLength;
    }

    @Override
    public void setSearchNotifications(SearchNotifications searchNotifications) {
        this.searchNotifications = searchNotifications;
    }

    @Override
    public SearchNotifications getSearchNotifications() {
        return searchNotifications;
    }

    @Override
    public IFrame.NotificationType getDefaultNotificationType() {
        return defaultNotificationType;
    }

    @Override
    public void setDefaultNotificationType(IFrame.NotificationType defaultNotificationType) {
        this.defaultNotificationType = defaultNotificationType;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Allows set new item which not exists in options
     */
    protected class CarelessDsWrapper extends DsWrapper {

        protected CarelessDsWrapper(CollectionDatasource datasource, boolean autoRefresh) {
            super(datasource, autoRefresh);
        }

        @Override
        public Item getItem(Object itemId) {
            Item item = super.getItem(itemId);
            if (item == null &&
                    newSettingValue != null &&
                    optionsDatasource != null &&
                    optionsDatasource.getItem(newSettingValue.getId()) == null)
                item = getItemWrapper(newSettingValue);
            return item;
        }

        @Override
        public boolean containsId(Object itemId) {
            boolean contains = super.containsId(itemId);
            if (!contains && newSettingValue != null)
                return ObjectUtils.equals(newSettingValue.getId(), itemId);
            return contains;
        }

        @Override
        public Collection getItemIds() {
            Collection itemIds = super.getItemIds();
            if (newSettingValue != null &&
                    optionsDatasource != null &&
                    optionsDatasource.getItem(newSettingValue.getId()) == null) {
                itemIds = new HashSet(itemIds);
                itemIds.add(newSettingValue.getId());
            }
            return itemIds;
        }
    }
}