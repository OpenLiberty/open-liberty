/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.app.FATServlet;

public class Base2PCCloudServlet extends FATServlet {

    /**  */
    private static final String filter = "(testfilter=jon)";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        XAResourceImpl.setStateFile(System.getenv("WLP_OUTPUT_DIR") + "/../shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);
        super.doGet(request, response);
    }

    public void commitSuicide(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        Runtime.getRuntime().halt(0);
    }

    public void dumpState(HttpServletRequest request,
                          HttpServletResponse response) throws Exception {

        XAResourceImpl.dumpState();
    }

    public void setupRecCore(HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory
                        .getXAResourceInfo(2);

        try {
            tm.begin();

            final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo1)
                            .setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo3);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            System.out.println("NYTRACE: ImplodeSimpleFS2PCServlet caught exc: " + e);
            e.printStackTrace();
        }
    }

    public void setupRec001(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResource(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            XAResourceImpl.dumpState();
            Runtime.getRuntime().halt(XAResourceImpl.DIE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec001(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec001 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec001 failed");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec002(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo1)
                            .setPrepareAction(
                                              XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec002(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec002 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec002 failed: not all resources recovered");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec002 failed: resource 1 not rolled back");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec003(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResource(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo2)
                            .setPrepareAction(
                                              XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec003(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec003 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec003 failed: not all resources recovered");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec004(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory
                        .getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory
                        .getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory
                        .getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo1)
                            .setPrepareAction(
                                              XAException.XA_RBROLLBACK);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                            .getXAResourceImpl(xaResInfo2)
                            .setRollbackAction(
                                               XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance()
                            .getXAResource(xaResInfo3);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec004(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec004 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec004 failed: not all resources recovered");
            }

            if (!new XAResourceImpl(2).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec004 failed: resource 2 not rolled back");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec005(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();

        XAResourceImpl.clear();

        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setPrepareAction(XAException.XA_RBROLLBACK);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec005(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec005 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec005 failed: not all resources recovered");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec005 failed: resource 1 not rolled back");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec006(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();

        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).setPrepareAction(XAException.XA_RBROLLBACK);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec006(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec006 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec006 failed: not all resources recovered");
            }

            if (!new XAResourceImpl(0).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec006 failed: resource 0 not rolled back");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec007(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec007(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec007 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec007 failed: not all resources committed");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec008(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec008(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec008 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec008 failed: not all resources committed");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec009(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec009(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec009 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
                throw new Exception("Rec009 failed: not all resources recovered");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec010(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec010(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec010 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(0).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec010 failed: resource 0 not rolled back");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec011(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setCommitAction(XAException.XA_HEURRB);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec011(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec011 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(0).inState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec011 failed: resource 0 not committed");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec011 failed: resource 1 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec012(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setCommitAction(XAException.XA_HEURMIX);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec012(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec012 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(0).inState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec012 failed: resource 0 not committed");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec012 failed: resource 1 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec013(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setCommitAction(XAException.XA_HEURCOM);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec013(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec013 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(0).inState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec013 failed: resource 0 not committed");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec013 failed: resource 1 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec014(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setCommitAction(XAException.XA_HEURHAZ);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec014(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec014 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(0).inState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec014 failed: resource 0 not committed");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec014 failed: resource 1 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec015(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setPrepareAction(XAException.XA_RBROLLBACK);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).setRollbackAction(XAException.XA_HEURRB);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec015(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec015 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec015 failed: resource 1 not rolledback");
            }

            if (!new XAResourceImpl(2).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec015 failed: resource 2 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec016(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setPrepareAction(XAException.XA_RBROLLBACK);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).setRollbackAction(XAException.XA_HEURCOM);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec016(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec016 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec016 failed: resource 1 not rolledback");
            }

            if (!new XAResourceImpl(2).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec016 failed: resource 2 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec017(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setPrepareAction(XAException.XA_RBROLLBACK);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).setRollbackAction(XAException.XA_HEURMIX);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec017(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec017 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec017 failed: resource 1 not rolledback");
            }

            if (!new XAResourceImpl(2).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec017 failed: resource 2 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec018(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setPrepareAction(XAException.XA_RBROLLBACK);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).setRollbackAction(XAResourceImpl.DIE);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).setRollbackAction(XAException.XA_HEURHAZ);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
            tm.enlist(xaRes3, recoveryId3);

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec018(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec018 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!new XAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
                throw new Exception("Rec018 failed: resource 1 not rolledback");
            }

            if (!new XAResourceImpl(2).inState(XAResourceImpl.FORGOTTEN)) {
                throw new Exception("Rec018 failed: resource 2 not forgotten");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec047(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        try {
            tm.begin();

            for (int i = 0; i < 4; i++) {
                final Serializable xaResInfo;
                final XAResourceImpl xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo = XAResourceInfoFactory.getXAResourceInfo(i));
                if (i == 0) {
                    xaRes.setPrepareAction(XAResourceImpl.DIE);
                }
                final int recoveryId = tm.registerResourceInfo(filter, xaResInfo);
                tm.enlist(xaRes, recoveryId);
            }

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec047(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 4) {
                throw new Exception("Rec047 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            for (int i = 1; i < 4; i++) {
                if (!new XAResourceImpl(i).inState(XAResourceImpl.ROLLEDBACK)) {
                    throw new Exception("Rec047 failed: resource " + i + " not rolledback");
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec048(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        try {
            tm.begin();

            for (int i = 0; i < 4; i++) {
                final Serializable xaResInfo;
                final XAResourceImpl xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo = XAResourceInfoFactory.getXAResourceInfo(i));
                if (i == 0) {
                    xaRes.setCommitAction(XAResourceImpl.DIE);
                }
                final int recoveryId = tm.registerResourceInfo(filter, xaResInfo);
                tm.enlist(xaRes, recoveryId);
            }

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec048(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 4) {
                throw new Exception("Rec048 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            for (int i = 1; i < 4; i++) {
                if (!new XAResourceImpl(i).inState(XAResourceImpl.COMMITTED)) {
                    throw new Exception("Rec048 failed: resource " + i + " not committed");
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec050(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        try {
            tm.begin();

            for (int i = 0; i < 10; i++) {
                final Serializable xaResInfo;
                final XAResourceImpl xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo = XAResourceInfoFactory.getXAResourceInfo(i));
                if (i == 0) {
                    xaRes.setPrepareAction(XAResourceImpl.DIE);
                }
                final int recoveryId = tm.registerResourceInfo(filter, xaResInfo);
                tm.enlist(xaRes, recoveryId);
            }

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec050(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 10) {
                throw new Exception("Rec050 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            for (int i = 1; i < 10; i++) {
                if (!new XAResourceImpl(i).inState(XAResourceImpl.ROLLEDBACK)) {
                    throw new Exception("Rec050 failed: resource " + i + " not rolledback");
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec051(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        try {
            tm.begin();

            for (int i = 0; i < 10; i++) {
                final Serializable xaResInfo;
                final XAResourceImpl xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo = XAResourceInfoFactory.getXAResourceInfo(i));
                if (i == 0) {
                    xaRes.setCommitAction(XAResourceImpl.DIE);
                }
                final int recoveryId = tm.registerResourceInfo(filter, xaResInfo);
                tm.enlist(xaRes, recoveryId);
            }

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec051(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 10) {
                throw new Exception("Rec051 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec051 failed: not all resources committed");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupRec090(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        //        initialize();

        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
        final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).setCommitAction(XAResourceImpl.DIE);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2, 1);
            tm.enlist(xaRes2, recoveryId2);

            final XAResource xaRes3 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo3);
            final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3, -1);
            tm.enlist(xaRes3, recoveryId3);

            // prepare order should be 3,2,1
            // commit order should be 2,1(die)
            // recover commit order should be 2,1,3
            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkRec090(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 3) {
                throw new Exception("Rec090 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED | XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec090 failed: not all resources committed");
            }

            // At this point all resources should be recovered and they should have committed in the right order - 2,1,3
            final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
            final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);
            final Serializable xaResInfo3 = XAResourceInfoFactory.getXAResourceInfo(2);

            int commitOrder = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1).getCommitOrder();
            if (commitOrder != 2) {
                throw new Exception("Rec090 failed: 1st resource had commit order " + commitOrder);
            }

            commitOrder = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo2).getCommitOrder();
            if (commitOrder != 1) {
                throw new Exception("Rec090 failed: 2nd resource had commit order " + commitOrder);
            }

            commitOrder = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).getCommitOrder();
            if (commitOrder != 3) {
                throw new Exception("Rec090 failed: 3rd resource had commit order " + commitOrder);
            }
        } finally {
            XAResourceImpl.clear();
        }
    }
}
