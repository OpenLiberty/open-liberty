/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ormdiag.enhancementerror.war;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.ormdiag.enhancementerror.jpa.EnhancementErrorEntity;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/EnhancementErrorServlet")
public class EnhancementErrorServlet extends FATServlet {
    @PersistenceContext(unitName = "ENHANCEMENT_ERROR_JTA")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    public void testJPAFunction() throws Exception {
        tx.begin();
        EnhancementErrorEntity entity = new EnhancementErrorEntity();
        entity.setStrData("Foo Bar");
        em.persist(entity);
        tx.commit();

        em.clear();

        EnhancementErrorEntity findEntity = em.find(EnhancementErrorEntity.class, entity.getId());
        Assert.assertNotNull(findEntity);
        Assert.assertNotSame(entity, findEntity);
        Assert.assertEquals(entity.getId(), findEntity.getId());
    }

    public void testInvalidFormatClassError() throws Exception {
        // Try to load com.ibm.ws.ormdiag.enhancementerror.jpa.BadClass -- should fail because it's a file containing 0 values.
        String cName = "com.ibm.ws.ormdiag.enhancementerror.jpa.BadClass";
        try {
            this.getClass().getClassLoader().loadClass(cName);
            Assert.fail("No java.lang.ClassFormatError was thrown.");
        } catch (java.lang.ClassFormatError cnfe) {
            // Expected
        } catch (Throwable t) {
            Assert.fail("Unexpected Exception " + t);
        }
    }
}
