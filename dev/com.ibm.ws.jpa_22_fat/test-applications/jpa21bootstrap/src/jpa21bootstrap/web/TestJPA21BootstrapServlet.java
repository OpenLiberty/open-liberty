/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jpa21bootstrap.web;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jpa21bootstrap.entity.SimpleTestEntity;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestJPA21Bootstrap")
public class TestJPA21BootstrapServlet extends FATServlet {
    @PersistenceContext(unitName = "JPAPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    @Mode(TestMode.LITE)
    public void bootstrapTest21() throws Exception {
        System.out.println("STARTING bootstrapTest21...");

        try {
            tx.begin();
            SimpleTestEntity entity = new SimpleTestEntity();
            entity.setStrData("Cat Dog");
            em.persist(entity);
            tx.commit();

            em.clear();

            SimpleTestEntity findEntity = em.find(SimpleTestEntity.class, entity.getId());
            Assert.assertNotNull(findEntity);
            Assert.assertNotSame(entity, findEntity);
            Assert.assertEquals(entity.getId(), findEntity.getId());
        } finally {
            System.out.println("ENDING bootstrapTest21.");
        }

    }

//    @Test
//    public void testServer1() throws Exception {
//        System.out.println("Test is running.");
//    }
//
//    @Test
//    @Mode(TestMode.LITE)
//    public void liteTest() throws Exception {
//        System.out.println("LITE test is running.");
//    }
//
//    @Test
//    @Mode(TestMode.FULL)
//    public void testFull() throws Exception {
//        System.out.println("This test should only run in Full or higher mode!");
//    }
//
//    @Test
//    @Mode(TestMode.QUARANTINE)
//    public void testQuarantine() throws Exception {
//        System.out.println("This test should only run in Quarantine mode!");
//    }
}
