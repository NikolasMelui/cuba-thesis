/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.app;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.SoftDelete;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;
import com.haulmont.cuba.core.global.Metadata;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
@Service(EntityRestoreService.NAME)
public class EntityRestoreServiceBean implements EntityRestoreService {

    private Log log = LogFactory.getLog(EntityRestoreServiceBean.class);

    @Inject
    protected Persistence persistence;

    @Inject
    protected Metadata metadata;

    @Override
    public void restoreEntities(Collection<Entity> entities) {
        for (Entity entity : entities) {
            if (!(entity instanceof SoftDelete))
                continue;

            Transaction tx = persistence.createTransaction();
            try {
                persistence.getEntityManager().setSoftDeletion(false);
                restoreEntity(entity);
                tx.commit();
            } finally {
                tx.end();
            }
        }
    }

    protected void restoreEntity(Entity entity) {
        EntityManager em = persistence.getEntityManager();
        Entity reloadedEntity = em.find(entity.getClass(), entity.getId());
        if (reloadedEntity != null && ((SoftDelete) reloadedEntity).isDeleted()) {
            log.info("Restoring deleted entity " + entity);
            Date deleteTs = ((SoftDelete) reloadedEntity).getDeleteTs();
            ((SoftDelete) reloadedEntity).setDeleteTs(null);
            em.merge(reloadedEntity);
            restoreDetails(reloadedEntity, deleteTs);
        }
    }

    protected void restoreDetails(Entity entity, Date deleteTs) {
        EntityManager em = persistence.getEntityManager();
        MetaClass metaClass = metadata.getClassNN(entity.getClass());

        List<MetaProperty> properties = new ArrayList<>();
        fillProperties(metaClass, properties, OnDelete.class.getName());
        for (MetaProperty property : properties) {
            OnDelete annotation = property.getAnnotatedElement().getAnnotation(OnDelete.class);
            DeletePolicy deletePolicy = annotation.value();
            if (deletePolicy == DeletePolicy.CASCADE) {
                MetaClass detailMetaClass = property.getRange().asClass();
                if (!SoftDelete.class.isAssignableFrom(detailMetaClass.getJavaClass())) {
                    log.debug("Cannot restore " + property.getRange().asClass() + " because it is hard deleted");
                    continue;
                }
                MetaProperty inverseProp = property.getInverse();
                if (inverseProp == null) {
                    log.debug("Cannot restore " + property.getRange().asClass() + " because it has no inverse property for " + metaClass);
                    continue;
                }
                String jpql = "select e from " + detailMetaClass + " e where e." + inverseProp.getName() + ".id = ?1 " +
                        "and e.deleteTs >= ?2 and e.deleteTs <= ?3";
                Query query = em.createQuery(jpql);
                query.setParameter(1, entity.getId());
                query.setParameter(2, DateUtils.addMilliseconds(deleteTs, -100));
                query.setParameter(3, DateUtils.addMilliseconds(deleteTs, 1000));
                //noinspection unchecked
                List<Entity> list = query.getResultList();
                for (Entity detailEntity : list) {
                    if (entity instanceof SoftDelete) {
                        restoreEntity(detailEntity);
                    }
                }
            }
        }

        properties = new ArrayList<>();
        fillProperties(metaClass, properties, OnDeleteInverse.class.getName());
        for (MetaProperty property : properties) {
            OnDeleteInverse annotation = property.getAnnotatedElement().getAnnotation(OnDeleteInverse.class);
            DeletePolicy deletePolicy = annotation.value();
            if (deletePolicy == DeletePolicy.CASCADE) {
                MetaClass detailMetaClass = property.getDomain();
                if (!SoftDelete.class.isAssignableFrom(detailMetaClass.getJavaClass())) {
                    log.debug("Cannot restore " + property.getRange().asClass() + " because it is hard deleted");
                    continue;
                }
                List<MetaClass> metClassesToRestore = new ArrayList<>();
                metClassesToRestore.add(detailMetaClass);
                metClassesToRestore.addAll(detailMetaClass.getDescendants());
                for (MetaClass metaClassToRestore : metClassesToRestore) {
                    if (!metadata.getTools().isPersistent(metaClassToRestore))
                        continue;
                    String jpql = "select e from " + metaClassToRestore.getName() + " e where e." + property.getName()
                            + ".id = ?1 and e.deleteTs >= ?2 and e.deleteTs <= ?3";
                    Query query = em.createQuery(jpql);
                    query.setParameter(1, entity.getId());
                    query.setParameter(2, DateUtils.addMilliseconds(deleteTs, -100));
                    query.setParameter(3, DateUtils.addMilliseconds(deleteTs, 1000));
                    //noinspection unchecked
                    List<Entity> list = query.getResultList();
                    for (Entity detailEntity : list) {
                        if (entity instanceof SoftDelete) {
                            restoreEntity(detailEntity);
                        }
                    }
                }
            }
        }
    }

    protected void fillProperties(MetaClass metaClass, List<MetaProperty> properties, String annotationName) {
        properties.clear();
        MetaProperty[] metaProperties = (MetaProperty[]) metaClass.getAnnotations().get(annotationName);
        if (metaProperties != null)
            properties.addAll(Arrays.asList(metaProperties));
        for (MetaClass aClass : metaClass.getAncestors()) {
            metaProperties = (MetaProperty[]) aClass.getAnnotations().get(annotationName);
            if (metaProperties != null)
                properties.addAll(Arrays.asList(metaProperties));
        }
    }
}
