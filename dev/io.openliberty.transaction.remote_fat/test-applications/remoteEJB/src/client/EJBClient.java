/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import shared.TestBeanRemote;
import shared.Util;

@SuppressWarnings("serial")
public abstract class EJBClient extends FATServlet {

    protected static final String SERVER_NAME_PROPERTY = "wlp.server.name";

    @Resource
    UserTransaction ut;

    protected TestBeanRemote bean;

    @Override
    protected void before() {
        System.getProperties().entrySet().stream().forEach(e -> System.out.println("Prop: " + e.getKey() + " -> " + e.getValue()));
        System.getenv().entrySet().stream().forEach(e -> System.out.println("Env: " + e.getKey() + " -> " + e.getValue()));
    }

    @Test
    public void testMandatoryWith(HttpServletRequest request,
                                  HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
        ut.begin();

        String id = bean.mandatory();
        assertNotNull("Mandatory method ran without a tran", id);
        assertTrue("Mandatory method ran under a different tran", id.equals(Util.tranID()));

        // Let's allow the runtime to complete the transaction
    }

    @Test
    @ExpectedFFDC(value = { "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testMandatoryWithout(HttpServletRequest request,
                                     HttpServletResponse response) throws NotSupportedException, SystemException, NamingException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        try {
            bean.mandatory();
            fail("Mandatory method succeeded with no tran");
        } catch (EJBTransactionRequiredException e) {
            System.out.println("Got \"" + e + "\" as expected");
        }
    }

    @Test
    @ExpectedFFDC(value = { "com.ibm.websphere.csi.CSIException" })
    public void testNeverWith(HttpServletRequest request,
                              HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
        ut.begin();

        try {
            bean.never();
            fail("Never method succeeded with tran");
        } catch (EJBException e) {
            System.out.println("Got \"" + e + "\" as expected");
        }

        // Let's allow the runtime to complete the transaction
    }

    @Test
    public void testNeverWithout(HttpServletRequest request,
                                 HttpServletResponse response) throws NotSupportedException, SystemException, NamingException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        assertNull("Never method ran in a tran", bean.never());
    }

    @Test
    public void testNotSupportedWith(HttpServletRequest request,
                                     HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
        ut.begin();

        assertNull("NotSupported method ran under a tran", bean.notSupported());

        // Let's allow the runtime to complete the transaction
    }

    @Test
    public void testNotSupportedWithout(HttpServletRequest request,
                                        HttpServletResponse response) throws NotSupportedException, SystemException, NamingException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        assertNull("NotSupported method ran under a tran", bean.notSupported());
    }

    @Test
    public void testRequiredWith(HttpServletRequest request,
                                 HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
        ut.begin();

        String id = bean.required();
        assertNotNull("Required method ran without a tran", id);
        assertTrue("Required method ran under a different tran", id.equals(Util.tranID()));

        // Let's allow the runtime to complete the transaction
    }

    @Test
    public void testRequiredWithout(HttpServletRequest request,
                                    HttpServletResponse response) throws SystemException, NamingException {
        assertNotNull("required method ran without a tran", bean.required());
    }

    @Test
    public void testRequiresNewWith(HttpServletRequest request,
                                    HttpServletResponse response) throws SystemException, NotSupportedException, NamingException {
        ut.begin();

        String id = bean.requiresNew();
        assertNotNull("RequiresNew method ran without a tran", id);
        assertFalse("RequiresNew method ran under same tran", id.equals(Util.tranID()));

        // Let's allow the runtime to complete the transaction
    }

    @Test
    public void testRequiresNewWithout(HttpServletRequest request,
                                       HttpServletResponse response) throws SystemException, NamingException {
        assertNotNull("RequiresNew method ran without a tran", bean.requiresNew());
    }

    @Test
    public void testSupportsWith(HttpServletRequest request,
                                 HttpServletResponse response) throws NotSupportedException, SystemException, NamingException {
        ut.begin();

        String id = bean.supports();
        assertNotNull("Supports method ran without a tran", id);
        assertEquals("Supports method ran under same tran", id, Util.tranID());

        // Let's allow the runtime to complete the transaction
    }

    @Test
    public void testSupportsWithout(HttpServletRequest request,
                                    HttpServletResponse response) throws SystemException, NamingException {
        assertNull("Supports method ran with a tran", bean.supports());
    }
}