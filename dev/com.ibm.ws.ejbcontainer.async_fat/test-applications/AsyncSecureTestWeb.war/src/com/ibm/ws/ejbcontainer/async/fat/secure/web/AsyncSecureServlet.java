/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.secure.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATSecurityHelper;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.ejbcontainer.async.fat.secure.ejb.SingletonLocalBean;
import com.ibm.ws.ejbcontainer.async.fat.secure.ejb.StatelessLocal;

import componenttest.app.FATServlet;

/**
 * Tests remote calls to EJB 3.1 Stateless Session Bean with Asynchronous annotations at the method level.
 * The methods are also marked with the RolesAllowed annotation to restrict their access to a single user/role.
 * The security context should be propagated, and if authorization fails an EJBAccessException will be returned.
 * <p>
 *
 * <b>Test Matrix:</b>
 * <ul>
 * <li>testValidRole1 - This test verifies that we can successfully call an async method as user1
 * when admin security is enabled and the method is configured to allow user1.
 * <li>testInvalidRole1 - This test verifies that we get an EJBAccessException when calling an async method as user2
 * when admin security is enabled and the method is only configured to allow user1.
 * <li>testValidRole2 - This test verifies that we can successfully call an async method as user2
 * when admin security is enabled and the method is configured to allow user2.
 * <li>testValidRole1Singleton - This test verifies that we can successfully call an async method as user1
 * when admin security is enabled and the method is configured to allow user1.
 * <li>testInvalidRole1Singleton - This test verifies that we get an EJBAccessException when calling an async method as user2
 * when admin security is enabled and the method is only configured to allow user1.
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/AsyncSecureServlet")
public class AsyncSecureServlet extends FATServlet {
    private final static String CLASS_NAME = AsyncSecureServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    StatelessLocal ivBean;

    @EJB
    SingletonLocalBean ivSingleton;

    private <T> T lookupBean(Class<T> intf) throws NamingException {
        if (intf == StatelessLocal.class) {
            return intf.cast(ivBean);
        }
        if (intf == SingletonLocalBean.class) {
            return intf.cast(ivSingleton);
        }
        throw new IllegalArgumentException("Unsupported bean type.");
    }

    /**
     * This test verifies that we can successfully call an async method as user1
     * when admin security is enabled and the method is configured to allow user1.
     */
    @Test
    public void testValidRole1() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        svLogger.info("logged in as userA");

        StatelessLocal bean = lookupBean(StatelessLocal.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        svLogger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.role1Only();
        String auth = future.get();
        lc.logout();

        svLogger.info("role1Only called as: " + auth);
        assertEquals("role1Only has wrong Principal", "userA", auth);
    }

    /**
     * This test verifies that we get an EJBAccessException when calling an async method as user2
     * when admin security is enabled and the method is only configured to allow user1.
     */
    @Test
    public void testInvalidRole1() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userB", "userBpass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        svLogger.info("logged in as userB");

        StatelessLocal bean = lookupBean(StatelessLocal.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        svLogger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        try {
            Future<String> future = bean.role1Only();
            String auth = future.get();
            lc.logout();

            svLogger.info("role1Only called as: " + auth);
            fail("expected exception, but role1Only was successful");
        } catch (ExecutionException ex) {
            lc.logout();
            svLogger.info("Caught expected exception: " + ex);
            Throwable cause = ex.getCause();
            assertTrue("Nested exception is EJBAccessException", cause instanceof EJBAccessException);
        }
    }

    /**
     * This test verifies that we can successfully call an async method as user2
     * when admin security is enabled and the method is configured to allow user2.
     */
    @Test
    public void testValidRole2() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userB", "userBpass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        svLogger.info("logged in as userB");

        StatelessLocal bean = lookupBean(StatelessLocal.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        svLogger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.role2Only();
        String auth = future.get();
        lc.logout();

        svLogger.info("role2Only called as: " + auth);
        assertEquals("role2Only has wrong Principal", "userB", auth);
    }

    /**
     * This test verifies that we can successfully call an async method as user1
     * when admin security is enabled and the method is configured to allow user1
     * on a singleton session bean.
     */
    @Test
    public void testValidRole1Singleton() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        svLogger.info("logged in as userA");

        SingletonLocalBean bean = lookupBean(SingletonLocalBean.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        svLogger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.role1Only();
        String auth = future.get();
        lc.logout();

        svLogger.info("role1Only called as: " + auth);
        assertEquals("role1Only has wrong Principal", "userA", auth);
    }

    /**
     * This test verifies that we get an EJBAccessException when calling an async method as user2
     * when admin security is enabled and the method is only configured to allow user1 on a
     * singleton session bean.
     */
    @Test
    public void testInvalidRole1Singleton() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userB", "userBpass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        svLogger.info("logged in as userB");

        SingletonLocalBean bean = lookupBean(SingletonLocalBean.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        svLogger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        try {
            Future<String> future = bean.role1Only();
            String auth = future.get();
            lc.logout();

            svLogger.info("role1Only called as: " + auth);
            fail("expected exception, but role1Only was successful");
        } catch (ExecutionException ex) {
            lc.logout();
            svLogger.info("Caught expected exception: " + ex);
            Throwable cause = ex.getCause();
            assertTrue("Nested exception is EJBAccessException", cause instanceof EJBAccessException);
        }
    }
}
