/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 17.03.2009 11:36:02
 *
 * $Id$
 */
package com.haulmont.cuba.security.app;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.*;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service providing current user settings functionality:
 * an application can save/load some "setting" (plain or XML string) for current user.
 * <br>Ususally used by UI forms and components. 
 */
@Service(UserSettingService.NAME)
public class UserSettingServiceBean implements UserSettingService
{
    @Inject
    private UserSessionSource userSessionSource;

    @Inject
    private Metadata metadata;

    public String loadSetting(String name) {
        return loadSetting(null, name);
    }

    public String loadSetting(ClientType clientType, String name) {
        String value;
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();

            Query q = em.createQuery(
                    "select s from sec$UserSetting s where s.user.id = ?1 and s.name =?2 and s.clientType = ?3");
            q.setParameter(1, userSessionSource.getUserSession().getUser().getId());
            q.setParameter(2, name);
            q.setParameter(3, clientType == null ? null : clientType.getId());
            q.setView(new View(UserSetting.class, false).addProperty("value"));

            List<UserSetting> list = q.getResultList();

            value = list.isEmpty() ? null : list.get(0).getValue();

            tx.commit();

        } finally {
            tx.end();
        }
        return value;
    }

    public void saveSetting(String name, String value) {
        saveSetting(null, name, value);
    }

    public void saveSetting(ClientType clientType, String name, String value) {
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();

            Query q = em.createQuery(
                    "select s from sec$UserSetting s where s.user.id = ?1 and s.name =?2 and s.clientType = ?3");
            q.setParameter(1, userSessionSource.getUserSession().getUser().getId());
            q.setParameter(2, name);
            q.setParameter(3, clientType == null ? null : clientType.getId());
            q.setView(new View(UserSetting.class, false).addProperty("value"));

            List<UserSetting> list = q.getResultList();
            if (list.isEmpty()) {
                UserSetting us = new UserSetting();
                em.setView(new View(User.class, false));
                us.setUser(em.find(User.class, userSessionSource.getUserSession().getUser().getId()));
                us.setName(name);
                us.setClientType(clientType);
                us.setValue(value);
                em.persist(us);
            } else {
                UserSetting us = list.get(0);
                us.setValue(value);
            }

            tx.commit();
        } finally {
            tx.end();
        }
    }

    public void copySettings(User fromUser, User toUser) {
        Map<UUID, Presentation> presentationsMap = copyPresentations(fromUser, toUser);
        copyUserFolders(fromUser, toUser, presentationsMap);
        Map<UUID, FilterEntity> filtersMap = copyFilters(fromUser, toUser);

        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();

            Query deleteSettingsQuery = em.createQuery("delete from sec$UserSetting s where s.user.id = ?1");
            deleteSettingsQuery.setParameter(1, toUser);
            deleteSettingsQuery.executeUpdate();
            tx.commitRetaining();
            em = PersistenceProvider.getEntityManager();
            Query q = em.createQuery("select s from sec$UserSetting s where s.user.id = ?1");
            q.setParameter(1, fromUser);
            List<UserSetting> fromUserSettings = q.getResultList();
            for (UserSetting currSetting : fromUserSettings) {
                UserSetting newSetting = metadata.create(UserSetting.class);
                newSetting.setUser(toUser);
                newSetting.setClientType(currSetting.getClientType());
                newSetting.setName(currSetting.getName());

                try {
                    Document doc = Dom4j.readDocument(currSetting.getValue());

                    List<Element> components = doc.getRootElement().element("components").elements("component");
                    for (Element component : components) {
                        Attribute presentationAttr = component.attribute("presentation");
                        if (presentationAttr != null) {
                            UUID presentationId = UUID.fromString(presentationAttr.getValue());
                            Presentation newPresentation = presentationsMap.get(presentationId);
                            if (newPresentation != null) {
                                presentationAttr.setValue(newPresentation.getId().toString());
                            }
                        }
                        Element defaultFilterEl = component.element("defaultFilter");
                        if (defaultFilterEl != null) {
                            Attribute idAttr = defaultFilterEl.attribute("id");
                            if (idAttr != null) {
                                UUID filterId = UUID.fromString(idAttr.getValue());
                                FilterEntity newFilter = filtersMap.get(filterId);
                                if (newFilter != null) {
                                    idAttr.setValue(newFilter.getId().toString());
                                }
                            }
                        }
                    }

                    newSetting.setValue(Dom4j.writeDocument(doc, true));
                } catch (Exception e) {
                    newSetting.setValue(currSetting.getValue());
                }
                em.persist(newSetting);
            }

            tx.commit();
        } finally {
            tx.end();
        }
    }


    private Map<UUID, Presentation> copyPresentations(User fromUser, User toUser) {
        Map<UUID, Presentation> presentationMap = new HashMap<UUID, Presentation>();
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();
            Query delete = em.createQuery();
            delete.setQueryString("delete from sec$Presentation p where p.user.id=?1");
            delete.setParameter(1, toUser);
            delete.executeUpdate();
            Query selectQuery = em.createQuery();
            selectQuery.setQueryString("select p from sec$Presentation p where p.user.id=?1");
            selectQuery.setParameter(1, fromUser);
            List<Presentation> presentations = selectQuery.getResultList();
            for (Presentation presentation : presentations) {
                Presentation newPresentation = metadata.create(Presentation.class);
                newPresentation.setUser(toUser);
                newPresentation.setComponentId(presentation.getComponentId());
                newPresentation.setAutoSave(presentation.getAutoSave());
                newPresentation.setName(presentation.getName());
                newPresentation.setXml(presentation.getXml());
                presentationMap.put(presentation.getId(), newPresentation);
                em.persist(newPresentation);
            }
            tx.commit();
            return presentationMap;

        } finally {
            tx.end();
        }
    }


    private void copyUserFolders(User fromUser, User toUser, Map<UUID, Presentation> presentationsMap) {
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();
            Query deleteSettingsQuery = em.createQuery("delete from sec$SearchFolder s where s.user.id = ?1");
            deleteSettingsQuery.setParameter(1, toUser);
            deleteSettingsQuery.executeUpdate();
            Query q = em.createQuery("select s from sec$SearchFolder s where s.user.id = ?1");
            q.setParameter(1, fromUser);
            List<SearchFolder> fromUserFolders = q.getResultList();
            Map<SearchFolder, SearchFolder> copiedFolders = new HashMap<SearchFolder, SearchFolder>();
            for (SearchFolder searchFolder : fromUserFolders) {
                copyFolder(searchFolder, toUser, copiedFolders, presentationsMap);
            }
            tx.commit();
        } finally {
            tx.end();
        }
    }

    private SearchFolder copyFolder(SearchFolder searchFolder,
                                    User toUser,
                                    Map<SearchFolder, SearchFolder> copiedFolders,
                                    Map<UUID, Presentation> presentationsMap) {
        SearchFolder newFolder;
        if (searchFolder.getUser() == null)
            return searchFolder;
        newFolder = copiedFolders.get(searchFolder);
        if (newFolder != null)
            return null;
        newFolder = metadata.create(SearchFolder.class);
        newFolder.setUser(toUser);
        newFolder.setApplyDefault(searchFolder.getApplyDefault());
        newFolder.setFilterComponentId(searchFolder.getFilterComponentId());
        newFolder.setFilterXml(searchFolder.getFilterXml());
        newFolder.setItemStyle(searchFolder.getItemStyle());
        newFolder.setName(searchFolder.getName());
        newFolder.setTabName(searchFolder.getTabName());
        newFolder.setSortOrder(searchFolder.getSortOrder());
        newFolder.setIsSet(searchFolder.getIsSet());
        newFolder.setEntityType(searchFolder.getEntityType());
        SearchFolder copiedFolder = copiedFolders.get(searchFolder.getParent());
        if (searchFolder.getParent() != null) {
            if (copiedFolder != null) {
                newFolder.setParent(copiedFolder);
            } else {
                SearchFolder newParent = getParent((SearchFolder) searchFolder.getParent(), toUser, copiedFolders, presentationsMap);
                newFolder.setParent(newParent);
            }
        }
        if (searchFolder.getPresentation() != null) {
            if (searchFolder.getPresentation().getUser() == null) {
                newFolder.setPresentation(searchFolder.getPresentation());
            } else {
                Presentation newPresentation = presentationsMap.get(searchFolder.getPresentation().getId());
                newFolder.setPresentation(newPresentation);
            }
        }
        copiedFolders.put(searchFolder, newFolder);
        EntityManager em = PersistenceProvider.getEntityManager();
        em.persist(newFolder);
        return newFolder;
    }

    private SearchFolder getParent(SearchFolder parentFolder, User toUser, Map<SearchFolder, SearchFolder> copiedFolders, Map<UUID, Presentation> presentationMap) {
        if (parentFolder == null) {
            return null;
        }
        if (parentFolder.getUser() == null) {
            return parentFolder;
        }
        return copyFolder(parentFolder, toUser, copiedFolders, presentationMap);
    }

    private Map<UUID, FilterEntity> copyFilters(User fromUser, User toUser) {
        Map<UUID, FilterEntity> filtersMap = new HashMap<UUID, FilterEntity>();
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();
            Query deleteFiltersQuery = em.createQuery("delete from sec$Filter f where f.user.id = ?1");
            deleteFiltersQuery.setParameter(1, toUser);
            deleteFiltersQuery.executeUpdate();
            Query q = em.createQuery("select f from sec$Filter f where f.user.id = ?1");
            q.setParameter(1, fromUser);
            List<FilterEntity> fromUserFilters = q.getResultList();

            for (FilterEntity filter : fromUserFilters) {
                FilterEntity newFilter = metadata.create(FilterEntity.class);
                newFilter.setUser(toUser);
                newFilter.setCode(filter.getCode());
                newFilter.setName(filter.getName());
                newFilter.setComponentId(filter.getComponentId());
                newFilter.setXml(filter.getXml());
                filtersMap.put(filter.getId(), newFilter);
                em.persist(newFilter);
            }
            tx.commit();
            return filtersMap;
        } finally {
            tx.end();
        }

    }
}
