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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.tx.jta.ut.util.XAResourceImpl;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
public class XAServlet extends HttpServlet {
    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource
    private UserTransaction tran;

    private TransactionManager tm;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BundleContext bundleContext = FrameworkUtil.getBundle(HttpServlet.class).getBundleContext();
        ServiceReference<TransactionManager> tranMgrRef = bundleContext.getServiceReference(TransactionManager.class);
        tm = bundleContext.getService(tranMgrRef);

        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testXA001(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tm.commit();
    }

    public void testXA002(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            // Should have rolledback
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    public void testXA003(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    public void testXA004(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    public void testXA005(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tx.setRollbackOnly();

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    public void testXA006(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tm.rollback();
    }

    public void testXA007(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        tx.setRollbackOnly();
        tm.rollback();
    }

    public void testXA008(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        tm.commit();
    }

    public void testXA009(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (RollbackException e) {
            // As expected
        }
    }

    public void testXA010(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    public void testXA011(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    public void testXA012(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAException.XA_RBROLLBACK));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURHAZ));
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setPrepareAction(XAResource.XA_RDONLY));
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    public void testXA013(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURRB));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURMIX));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XA_HEURCOM));

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
        }
    }

    public void testXA014(HttpServletRequest request, HttpServletResponse response) throws Exception {
        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURRB));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURMIX));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setRollbackAction(XAException.XA_HEURCOM));

        tm.rollback();
    }

    private static final int HEURISTIC_RETRY_INTERVAL = 10;
    private static final int SUITABLE_DELAY = 5;

    public void testXA015(HttpServletRequest request, HttpServletResponse response) throws Exception {
        XAResourceImpl.clear();

        final int commitRepeatCount = 3;

        tm.begin();
        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XAER_RMFAIL).setCommitRepeatCount(commitRepeatCount));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl().setCommitAction(XAException.XAER_RMFAIL).setCommitRepeatCount(commitRepeatCount));
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());
        tx.enlistResource(new XAResourceImpl());

        try {
            tm.commit();
            throw new Exception();
        } catch (HeuristicMixedException e) {
            // As expected
            Thread.sleep(1000 * ((1 + commitRepeatCount) * HEURISTIC_RETRY_INTERVAL + SUITABLE_DELAY));
            if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Committed count: " + XAResourceImpl.committedCount());
            }
        }
    }
}
