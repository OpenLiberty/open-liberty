/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.config.web;

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

import com.ibm.websphere.ejbcontainer.test.tools.FATSecurityHelper;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.ejbcontainer.async.fat.config.ejb.SingletonLocalBean;
import com.ibm.ws.ejbcontainer.async.fat.config.ejb.StatelessLocal;

import componenttest.app.FATServlet;

/**
 * Tests configuration of the EJBContainer asynchronous method context service. <p>
 */
@SuppressWarnings("serial")
@WebServlet("/AsyncConfigServlet")
public class AsyncConfigServlet extends FATServlet {
    private final static String CLASS_NAME = AsyncConfigServlet.class.getName();
    private final static Logger logger = Logger.getLogger(CLASS_NAME);

    @EJB
    StatelessLocal slBean;

    @EJB
    SingletonLocalBean sgBean;

    private <T> T lookupBean(Class<T> intf) throws NamingException {
        if (intf == StatelessLocal.class) {
            return intf.cast(slBean);
        }
        if (intf == SingletonLocalBean.class) {
            return intf.cast(sgBean);
        }
        throw new IllegalArgumentException("Unsupported bean type.");
    }

    /**
     * Test that setting asynchronous method ContextService to a custom ContextService
     * that propagates the security context will behave the same as the default
     * behavior when no ContextServcie is provided.
     */
    public void testAsyncContextServiceConfigSameAsNoConfig() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        logger.info("logged in as userA");

        StatelessLocal bean = lookupBean(StatelessLocal.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        logger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.role1Only();
        String auth = future.get();
        lc.logout();

        logger.info("role1Only called as: " + auth);
        assertEquals("role1Only has wrong Principal", "userA", auth);
    }

    /**
     * Test that setting asynchronous method ContextService to a custom ContextService
     * that propagates no contexts will result in the security context not
     * being propagated.
     */
    public void testAsyncContextServiceConfigNoPropagation() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        logger.info("logged in as userA");

        StatelessLocal bean = lookupBean(StatelessLocal.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        logger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        try {
            Future<String> future = bean.role1Only();
            String auth = future.get();
            lc.logout();

            logger.info("role1Only called as: " + auth);
            fail("expected exception, but role1Only was successful");
        } catch (ExecutionException ex) {
            lc.logout();
            logger.info("Caught expected exception: " + ex);
            Throwable cause = ex.getCause();
            assertTrue("Nested exception is EJBAccessException", cause instanceof EJBAccessException);
        }
    }

    /**
     * Test that an asynchronous configuration without any attributes will result
     * in the default behavior; the security context is propagated.
     */
    public void testAsynchronousConfigWithNoContextService() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        logger.info("logged in as userA");

        StatelessLocal bean = lookupBean(StatelessLocal.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        logger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.role1Only();
        String auth = future.get();
        lc.logout();

        logger.info("role1Only called as: " + auth);
        assertEquals("role1Only has wrong Principal", "userA", auth);
    }

    /**
     * Test that removing the setting for asynchronous configuration will result
     * in the default behavior; the security context is propagated.
     */
    public void testAsynchronousConfigRemoval() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        logger.info("logged in as userA");

        StatelessLocal bean = lookupBean(StatelessLocal.class);
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        logger.info("bean lookup successful");
        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.role1Only();
        String auth = future.get();
        lc.logout();

        logger.info("role1Only called as: " + auth);
        assertEquals("role1Only has wrong Principal", "userA", auth);
    }
}
