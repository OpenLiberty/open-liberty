/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/heuristics")
public class HeuristicsServlet extends FATServlet {

    TransactionManager tm;

    @Override
    public void init() {
        tm = TransactionManagerFactory.getTransactionManager();
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE001() {
        try {
            tm.begin();
            final Transaction tx = tm.getTransaction();

            tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));

            tm.commit();
            // Should have thrown HeuristicRollbackException
            fail("testHE001 failed to throw HeuristicRollbackException");
        } catch (HeuristicRollbackException e) {
            // As expected
            System.out.println("testHE001 caught HeuristicRollbackException successfuly");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE001 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE001 XAResource has forgotten<br>");
        } else {
            fail("testHE001 XAResource has NOT forgotten<br>");
        }

        System.out.println("testHE001 has completed successfuly<br>");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE002() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE002 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
            System.out.println("testHE002 caught HeuristicMixedException successfuly<br>");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE002 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE002 XAResource has forgotten<br>");
        } else {
            fail("testHE002 XAResource has NOT forgotten<br>");
        }

        System.out.println("testHE001 has completed successfuly<br>");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE003() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));
            tm.commit();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE003 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE003 XAResource has forgotten<br>");
        } else {
            fail("testHE003 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE004() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE004 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE004 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE004 XAResource has forgotten<br>");
        } else {
            fail("testHE004 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE005() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
            tm.commit();
            // Should have thrown HeuristicRollbackException
            fail("testHE005 failed to throw HeuristicRollbackException");
        } catch (HeuristicRollbackException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE005 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE005 XAResource has forgotten<br>");
        } else {
            fail("testHE005 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE006() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE006 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE006 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE006 XAResource has forgotten<br>");
        } else {
            fail("testHE006 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE007() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE007 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE007 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE007 XAResource has forgotten<br>");
        } else {
            fail("testHE007 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE008() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE008 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE008 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE008 XAResource has forgotten<br>");
        } else {
            fail("testHE008 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE009() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE009 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE009 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE009 XAResource has forgotten<br>");
        } else {
            fail("testHE009 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE010() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE010 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0010 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE010 XAResource has forgotten<br>");
        } else {
            fail("testHE010 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE011() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE011 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0011 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE011 XAResource has forgotten<br>");
        } else {
            fail("testHE011 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE012() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE012 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0012 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE012 XAResource has forgotten<br>");
        } else {
            fail("testHE012 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE013() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE013 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0013 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE013 XAResource has forgotten<br>");
        } else {
            fail("testHE013 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE014() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));
            tm.commit();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0014 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE014 XAResource has forgotten<br>");
        } else {
            fail("testHE014 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE015() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE015 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0015 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE015 XAResource has forgotten<br>");
        } else {
            fail("testHE015 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE016() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.commit();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0016 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE016 XAResource has forgotten<br>");
        } else {
            fail("testHE016 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE017() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE017 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0017 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE017 XAResource has forgotten<br>");
        } else {
            fail("testHE017 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE018() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE018 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0018 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE018 XAResource has forgotten<br>");
        } else {
            fail("testHE018 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE019() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0019 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE019 XAResource has forgotten<br>");
        } else {
            fail("testHE019 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE020() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0020 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE020 XAResource has forgotten<br>");
        } else {
            fail("testHE020 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE021() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0021 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE021 XAResource has forgotten<br>");
        } else {
            fail("testHE021 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE022() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0022 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE022 XAResource has forgotten<br>");
        } else {
            fail("testHE022 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE023() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0023 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE023 XAResource has forgotten<br>");
        } else {
            fail("testHE023 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE024() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0024 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE024 XAResource has forgotten<br>");
        } else {
            fail("testHE024 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE025() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0025 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE025 XAResource has forgotten<br>");
        } else {
            fail("testHE025 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE026() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0026 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE026 XAResource has forgotten<br>");
        } else {
            fail("testHE026 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE027() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0027 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE027 XAResource has forgotten<br>");
        } else {
            fail("testHE027 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE028() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0028 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE028 XAResource has forgotten<br>");
        } else {
            fail("testHE028 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE029() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0029 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE029 XAResource has forgotten<br>");
        } else {
            fail("testHE029 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE030() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0030 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE030 XAResource has forgotten<br>");
        } else {
            fail("testHE030 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE031() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0031 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE031 XAResource has forgotten<br>");
        } else {
            fail("testHE031 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE032() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0032 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE032 XAResource has forgotten<br>");
        } else {
            fail("testHE032 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE033() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0033 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE033 XAResource has forgotten<br>");
        } else {
            fail("testHE033 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE034() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0034 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE034 XAResource has forgotten<br>");
        } else {
            fail("testHE034 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE035() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0035 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE035 XAResource has forgotten<br>");
        } else {
            fail("testHE035 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testHE036() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl());
            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0036 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE036 XAResource has forgotten<br>");
        } else {
            fail("testHE036 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE043() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE043 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0043 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE043 XAResource has forgotten<br>");
        } else {
            fail("testHE043 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE044() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE044 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0044 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE044 XAResource has forgotten<br>");
        } else {
            fail("testHE044 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE045() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE045 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0045 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE045 XAResource has forgotten<br>");
        } else {
            fail("testHE045 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE046() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE046 failed to throw HeuristicMixedException");
        } catch (RollbackException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0046 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE046 XAResource has forgotten<br>");
        } else {
            fail("testHE046 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE047() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE047 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0047 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE047 XAResource has forgotten<br>");
        } else {
            fail("testHE047 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE048() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE048 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0048 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE048 XAResource has forgotten<br>");
        } else {
            fail("testHE048 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE049() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE049 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0049 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE049 XAResource has forgotten<br>");
        } else {
            fail("testHE049 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE050() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE050 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0050 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE050 XAResource has forgotten<br>");
        } else {
            fail("testHE050 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE051() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE051 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0051 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE051 XAResource has forgotten<br>");
        } else {
            fail("testHE051 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE052() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE052 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0052 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE052 XAResource has forgotten<br>");
        } else {
            fail("testHE052 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE053() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE053 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0053 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE053 XAResource has forgotten<br>");
        } else {
            fail("testHE053 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE054() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE054 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0054 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE054 XAResource has forgotten<br>");
        } else {
            fail("testHE054 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE055() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE055 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0055 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE055 XAResource has forgotten<br>");
        } else {
            fail("testHE055 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE056() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE056 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0056 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE056 XAResource has forgotten<br>");
        } else {
            fail("testHE056 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE057() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE057 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0057 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE057 XAResource has forgotten<br>");
        } else {
            fail("testHE057 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE058() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE058 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0058 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE058 XAResource has forgotten<br>");
        } else {
            fail("testHE058 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE059() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE059 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0059 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE059 XAResource has forgotten<br>");
        } else {
            fail("testHE059 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE060() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE060 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0060 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE060 XAResource has forgotten<br>");
        } else {
            fail("testHE060 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE061() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE061 failed to throw HeuristicMixedException");
        } catch (HeuristicMixedException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0061 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE061 XAResource has forgotten<br>");
        } else {
            fail("testHE061 XAResource has NOT forgotten<br>");
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testHE062() {

        try {
            tm.begin();
            tm.getTransaction().enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.getTransaction().enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
            tm.commit();
            // Should have thrown HeuristicMixedException
            fail("testHE062 failed to throw RollbackException");
        } catch (RollbackException e) {
            // As expected
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("testHE0062 unexpectedly caught exception: " + e);
        }

        if (XAResourceImpl.checkForgotten()) {
            // As expected
            System.out.println("testHE062 XAResource has forgotten<br>");
        } else {
            fail("testHE062 XAResource has NOT forgotten<br>");
        }
    }
}