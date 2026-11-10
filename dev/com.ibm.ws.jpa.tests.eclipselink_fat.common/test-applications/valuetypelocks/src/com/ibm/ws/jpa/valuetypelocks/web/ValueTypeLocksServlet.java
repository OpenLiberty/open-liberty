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

package com.ibm.ws.jpa.valuetypelocks.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.valuetypelocks.model.ValueTypeLocksEntity;

import componenttest.app.FATServlet;

/**
 * EclipseLink synchronizes on DatasourcePlatform.sequencesLock and Project.descriptorsLock.
 * These were declared as Object but initialized to Boolean.TRUE. Boolean is a value-based class,
 * and synchronizing on it fails with java.lang.IdentityException on a JVM with Java Value Types
 * (Project Valhalla) enabled. The locks must be identity objects that are unique per instance.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ValueTypeLocksServlet")
public class ValueTypeLocksServlet extends FATServlet {

    @PersistenceUnit(unitName = "VALUETYPELOCKS_RL")
    private EntityManagerFactory emf;

    @Test
    public void testEclipseLinkLocksAreIdentityObjects() throws Exception {
        // EclipseLink classes are not on the JUnit classpath, so they are accessed reflectively.
        final Class<?> jpaEmfClass = Class.forName("org.eclipse.persistence.jpa.JpaEntityManagerFactory");
        final Object jpaEmf = emf.unwrap(jpaEmfClass);
        final Object session = invoke(jpaEmf, "getDatabaseSession");
        assertNotNull("EclipseLink session", session);

        final Object sequencesLock = readField(invoke(session, "getDatasourcePlatform"), "sequencesLock");
        final Object descriptorsLock = readField(invoke(session, "getProject"), "descriptorsLock");

        assertIdentityObject("DatasourcePlatform.sequencesLock", sequencesLock);
        assertIdentityObject("Project.descriptorsLock", descriptorsLock);
        assertNotSame("sequencesLock and descriptorsLock must be distinct objects", sequencesLock, descriptorsLock);

        int count = 0;
        synchronized (sequencesLock) {
            count++;
        }
        synchronized (descriptorsLock) {
            count++;
        }
        assertEquals(2, count);

        final EntityManager em = emf.createEntityManager();
        try {
            final ValueTypeLocksEntity entity = new ValueTypeLocksEntity();
            entity.setName("valueTypeLocks");
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();

            assertTrue("Generated id should be non-zero", entity.getId() != 0);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    private static void assertIdentityObject(String name, Object lock) {
        assertNotNull(name + " is null", lock);
        assertFalse(name + " is a Boolean: " + lock, lock instanceof Boolean);
        assertFalse(name + " is a Number: " + lock, lock instanceof Number);
        assertFalse(name + " is a Character: " + lock, lock instanceof Character);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        final Method m = target.getClass().getMethod(methodName);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                final Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                // try the superclass
            }
        }
        throw new NoSuchFieldException(fieldName + " in " + target.getClass().getName());
    }
}
