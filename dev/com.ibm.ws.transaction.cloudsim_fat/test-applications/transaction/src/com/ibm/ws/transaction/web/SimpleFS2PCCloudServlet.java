package com.ibm.ws.transaction.web;

import java.io.Serializable;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.app.FATServlet;

@WebServlet("/SimpleFS2PCCloudServlet")
public class SimpleFS2PCCloudServlet extends FATServlet {

    /**  */
    private static final String filter = "(testfilter=jon)";

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
}