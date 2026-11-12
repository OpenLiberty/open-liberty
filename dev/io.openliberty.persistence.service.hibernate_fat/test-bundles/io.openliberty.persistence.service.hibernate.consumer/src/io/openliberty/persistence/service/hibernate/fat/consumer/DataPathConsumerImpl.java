/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.persistence.service.hibernate.fat.consumer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import io.openliberty.persistence.service.hibernate.fat.consumer.internal.model.AnnotatedItem;
import io.openliberty.persistence.service.hibernate.fat.consumer.internal.model.UnannotatedItem;
import io.openliberty.persistence.service.hibernate.fat.persistence.test.DataPathConsumer;
import jakarta.persistence.EntityManager;

/**
 * Operations on the unit that {@link DataPathService} created from a DatabaseStore.
 */
public class DataPathConsumerImpl implements DataPathConsumer {

    private final PersistenceServiceUnit pu;
    private final DataPathService service;

    DataPathConsumerImpl(PersistenceServiceUnit pu, DataPathService service) {
        this.pu = pu;
        this.service = service;
    }

    @Override
    public String getTablePrefix() {
        return service.getTablePrefix();
    }

    @Override
    public void insertAnnotated(long id, String name) {
        EntityManager em = pu.createEntityManager();
        try {
            em.persist(new AnnotatedItem(id, name));
        } finally {
            em.close();
        }
    }

    @Override
    public String findAnnotatedName(long id) {
        EntityManager em = pu.createEntityManager();
        try {
            AnnotatedItem item = em.find(AnnotatedItem.class, id);
            return item == null ? null : item.getName();
        } finally {
            em.close();
        }
    }

    @Override
    public void insertUnannotated(long id, String name, boolean active, String... tags) {
        EntityManager em = pu.createEntityManager();
        try {
            UnannotatedItem item = new UnannotatedItem();
            item.id = id;
            item.name = name;
            item.active = active;
            item.tags.addAll(Arrays.asList(tags));
            em.persist(item);
        } finally {
            em.close();
        }
    }

    @Override
    public String findUnannotatedName(long id) {
        EntityManager em = pu.createEntityManager();
        try {
            UnannotatedItem item = em.find(UnannotatedItem.class, id);
            return item == null ? null : item.name;
        } finally {
            em.close();
        }
    }

    @Override
    public int findUnannotatedTagCount(long id) {
        EntityManager em = pu.createEntityManager();
        try {
            UnannotatedItem item = em.find(UnannotatedItem.class, id);
            return item == null ? -1 : item.tags.size();
        } finally {
            em.close();
        }
    }

    @Override
    public long countUnannotatedByActive(boolean active) {
        EntityManager em = pu.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(i) FROM UnannotatedItem i WHERE i.active = :a", Long.class)
                            .setParameter("a", active)
                            .getSingleResult();
        } finally {
            em.close();
        }
    }

    @Override
    public String generateDDL() {
        StringWriter out = new StringWriter();
        pu.generateDDL(out);
        return out.toString();
    }

    @Override
    public void createUnitWithGeneratedEntityResource() throws Exception {
        // Jakarta Data registers the bytes of a class that it generated for a record
        // as an in-memory resource whose name is the class file path. The name does not
        // start with "mappingfile-", so it must be registered as a resource and must not
        // be treated as an ORM mapping file.
        byte[] classBytes;
        try (InputStream in = AnnotatedItem.class.getResourceAsStream("AnnotatedItem.class")) {
            classBytes = readAll(in);
        }
        List<InMemoryMappingFile> generated = new ArrayList<>();
        generated.add(new InMemoryMappingFile(classBytes, "generated/GeneratedForRecord.class"));

        Map<String, Object> extra = new HashMap<>();
        extra.put("io.openliberty.persistence.internal.generatedEntities", generated);

        PersistenceServiceUnit other = service.createUnit(extra);
        try {
            EntityManager em = other.createEntityManager();
            try {
                // The unit must be usable and see the same table as the first unit.
                em.find(AnnotatedItem.class, -1L);
                em.find(UnannotatedItem.class, -1L);
            } finally {
                em.close();
            }
        } finally {
            other.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) != -1;)
            out.write(buf, 0, n);
        return out.toByteArray();
    }
}
