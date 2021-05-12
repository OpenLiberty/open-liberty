/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.wim.adapter.ldap.context.ContextManager.InitializeResult;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.wim.exception.EntityAlreadyExistsException;
import com.ibm.wsspi.security.wim.exception.EntityHasDescendantsException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.InvalidInitPropertyException;
import com.ibm.wsspi.security.wim.exception.OperationNotSupportedException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;

@SuppressWarnings("restriction")
public class ContextManagerTest {

    private static final String BASE_ENTRY = "o=ibm,c=us";
    private static EmbeddedApacheDS failoverLdapServer;
    private static final String NON_EXISTENT_DN = "uid=newuser,o=invalid,c=invalid";
    private static EmbeddedApacheDS primaryLdapServer;
    private static final String USER_DN = "uid=user1," + BASE_ENTRY;

    @BeforeClass
    public static void setUp() throws Exception {
        primaryLdapServer = new EmbeddedApacheDS("primaryLdap");
        primaryLdapServer.addPartition("testing", BASE_ENTRY);
        primaryLdapServer.startServer();

        failoverLdapServer = new EmbeddedApacheDS("failoverLdap");
        failoverLdapServer.addPartition("testing", BASE_ENTRY);
        failoverLdapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = primaryLdapServer.newEntry(BASE_ENTRY);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        primaryLdapServer.add(entry);
        failoverLdapServer.add(entry);

        /*
         * Create the user and group.
         */
        entry = primaryLdapServer.newEntry(USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "user1");
        entry.add("sn", "user1");
        entry.add("cn", "user1");
        entry.add("userPassword", "password");
        primaryLdapServer.add(entry);
        failoverLdapServer.add(entry);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (primaryLdapServer != null) {
            primaryLdapServer.stopService();
        }
        if (failoverLdapServer != null) {
            failoverLdapServer.stopService();
        }
    }

    /**
     * Test {@link ContextManager#createDirContext()}.
     */
    @Test
    public void createDirContext() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.initialize();

        TimedDirContext ctx = null;
        try {
            assertNotNull(cm.createDirContext(USER_DN, "password".getBytes()));
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    /**
     * Test {@link ContextManager#createSubcontext(String, Attributes)}.
     */
    @Test
    public void createSubContext() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.initialize();

        /*
         * Create a new branch.
         */
        String subcontextDN = "ou=subcontext," + BASE_ENTRY;
        Attributes attrs = new BasicAttributes();
        attrs.put(new BasicAttribute("objectclass", "organizationalunit"));
        attrs.put(new BasicAttribute("ou", "subcontext"));
        cm.createSubcontext(subcontextDN, attrs);

        /*
         * Verify the entry exists.
         */
        assertNotNull(primaryLdapServer.lookup(subcontextDN));

        /*
         * Now delete it.
         */
        cm.destroySubcontext(subcontextDN);

        /*
         * Verify the entry no longer exists.
         */
        try {
            primaryLdapServer.lookup(subcontextDN);
            fail("Expected LdapNoSuchObjectException.");
        } catch (LdapNoSuchObjectException e) {
            // passed
        }
    }

    /**
     * Test {@link ContextManager#createSubcontext(String, Attributes)} handling of NameAlreadyBoundExceptions.
     */
    @Test
    public void createSubContext_EntityAlreadyExistsException() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.initialize();

        try {
            /*
             * Try to create a user that already exists.
             */
            Attributes attrs = new BasicAttributes();
            attrs.put(new BasicAttribute("objectclass", "inetorgperson"));
            attrs.put(new BasicAttribute("uid", "user1"));
            attrs.put(new BasicAttribute("sn", "user1"));
            attrs.put(new BasicAttribute("cn", "user1"));
            cm.createSubcontext(USER_DN, attrs);

            fail("Expected EntityAlreadyExistsException.");
        } catch (EntityAlreadyExistsException e) {
            assertEquals("Expected cause to be NameAlreadyBoundException.", NameAlreadyBoundException.class, e.getCause().getClass());
        }
    }

    /**
     * Test {@link ContextManager#createSubcontext(String, Attributes)} handling of NameNotFoundExceptions.
     */
    @Test
    public void createSubContext_EntityNotFoundException() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.initialize();

        try {
            /*
             * Try to create on an invalid DN.
             */
            Attributes attrs = new BasicAttributes();
            attrs.put(new BasicAttribute("objectclass", "organizationalunit"));
            attrs.put(new BasicAttribute("ou", "subcontext"));
            cm.createSubcontext(NON_EXISTENT_DN, attrs);

            fail("Expected EntityNotFoundException.");
        } catch (EntityNotFoundException e) {
            assertEquals("Expected cause to be NameNotFoundException.", NameNotFoundException.class, e.getCause().getClass());
        }
    }

    /**
     * Test {@link ContextManager#createSubcontext(String, Attributes)} handling of general NamingExceptions.
     */
    @Test
    public void createSubContext_WIMSystemException() throws Exception {
        /*
         * There are no credentials specified, so the context has no permissions to update.
         */
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.initialize();

        try {
            /*
             * Try to create on an invalid DN.
             */
            Attributes attrs = new BasicAttributes();
            attrs.put(new BasicAttribute("objectclass", "organizationalunit"));
            attrs.put(new BasicAttribute("ou", "subcontext"));
            cm.createSubcontext(NON_EXISTENT_DN, attrs);

            fail("Expected WIMSystemException.");
        } catch (WIMSystemException e) {
            assertEquals("Expected cause to be NoPermissionException.", NoPermissionException.class, e.getCause().getClass());
        }
    }

    /**
     * Test {@link ContextManager#destroySubcontext(String)} handling of ContextNotEmptyExceptions.
     */
    @Test
    public void destroySubContext_EntityHasDescendantsException() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.initialize();

        try {
            /*
             * The base entry has users and groups on it, so it can't be deleted.
             */
            cm.destroySubcontext(BASE_ENTRY);

            fail("Expected EntityHasDescendantsException.");
        } catch (EntityHasDescendantsException e) {
            assertEquals("Expected cause to be ContextNotEmptyException.", ContextNotEmptyException.class, e.getCause().getClass());
        }
    }

    /**
     * Test {@link ContextManager#destroySubcontext(String)} handling of NameNotFoundExceptions.
     */
    @Test
    public void destroySubContext_EntityNotFoundException() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.initialize();

        try {
            /*
             * Try to destroy on an invalid DN.
             */
            cm.destroySubcontext(NON_EXISTENT_DN);

            fail("Expected EntityNotFoundException.");
        } catch (EntityNotFoundException e) {
            assertEquals("Expected cause to be NameNotFoundException.", NameNotFoundException.class, e.getCause().getClass());
        }
    }

    /**
     * Test {@link ContextManager#destroySubcontext(String)} handling of general NamingExceptions.
     */
    @Test
    public void destroySubContext_WIMSystemException() throws Exception {
        /*
         * There are no credentials specified, so the context has no permissions to delete.
         */
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.initialize();

        try {
            cm.destroySubcontext(USER_DN);

            fail("Expected WIMSystemException.");
        } catch (WIMSystemException e) {
            assertEquals("Expected cause to be NoPermissionException.", NoPermissionException.class, e.getCause().getClass());
        }
    }

    /**
     * Test behavior of LDAP server fail-over when the context pool is enabled.
     */
    @Test
    public void failover_ContextPoolEnabled() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.addFailoverServer("localhost", failoverLdapServer.getLdapServer().getPort());
        cm.setWriteToSecondary(false);
        cm.setContextPool(true, 1, 1, 0, 1000l, 1000l);
        cm.initialize();

        /*
         * Get a context to the primary LDAP server. We should always have
         * permission to write to the primary.
         */
        TimedDirContext ctx1 = cm.getDirContext();
        String host1 = (String) ctx1.getEnvironment().get(Context.PROVIDER_URL);
        cm.checkWritePermission(ctx1);

        /*
         * Mark the context as having encountered some connectivity failure.
         */
        Thread.sleep(2000);
        TimedDirContext ctx2 = cm.reCreateDirContext(ctx1, "Some failure"); // TODO DEADLOCK WHEN MAX POOL SIZE IS 1
        String host2 = (String) ctx2.getEnvironment().get(Context.PROVIDER_URL);
        assertFalse("Expected fail-over server.", host1.equals(host2));
        assertNotSame("Expected a new context instance.", ctx1, ctx2);

        /*
         * Check write permissions on the fail-over LDAP server. There should be none.
         */
        try {
            cm.checkWritePermission(ctx2);
            fail("Expected OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Passed
        }
        try {
            cm.createSubcontext("blah", null);
            fail("Expected OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Passed
        }

        /*
         * Set write to secondary to true now, and retry. Modifying after initialize is not something
         * we would / should do in production, but it makes it easier to test the setting.
         */
        cm.setWriteToSecondary(true);
        cm.checkWritePermission(ctx2);
    }

    /**
     * Test behavior of LDAP server fail-over when the context pool is disabled.
     */
    @Test
    public void failover_ContextPoolDisabled() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.addFailoverServer("localhost", failoverLdapServer.getLdapServer().getPort());
        cm.setWriteToSecondary(false);
        cm.setContextPool(false, null, null, null, null, null);
        cm.initialize();

        /*
         * Get a context to the primary LDAP server. We should be able to write there.
         */
        TimedDirContext oldCtx = cm.getDirContext();
        String host1 = (String) oldCtx.getEnvironment().get(Context.PROVIDER_URL);
        cm.checkWritePermission(oldCtx);

        /*
         * Mark the context as having encountered some connectivity failure.
         */
        TimedDirContext newCtx = cm.reCreateDirContext(oldCtx, "Some failure");
        String host2 = (String) newCtx.getEnvironment().get(Context.PROVIDER_URL);
        assertFalse("Expected fail-over server.", host1.equals(host2));
        assertNotSame("Expected a new context instance.", oldCtx, newCtx);

        /*
         * Check write permissions on the fail-over LDAP server. There should be none.
         */
        try {
            cm.checkWritePermission(newCtx);
            fail("Expected OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Passed
        }
        try {
            cm.createSubcontext("blah", null);
            fail("Expected OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Passed
        }

        /*
         * Set write to secondary to true now, and retry. Modifying after initialize is not something
         * we would / should do in production, but it makes it easier to test the setting.
         */
        cm.setWriteToSecondary(true);
        cm.checkWritePermission(oldCtx);
    }

    /**
     * Test that when the context pool is enabled that {@link ContextManager#getDirContext()}
     * will return contexts from the pool.
     */
    @Test
    public void getDirContext_ContextPoolEnabled() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.setContextPool(true, 0, 3, 3, 10000l, 10000l);
        cm.initialize();

        Set<TimedDirContext> contexts = new HashSet<TimedDirContext>();

        /*
         * Get the maximum number of contexts from the pool. If we get more
         * than the maximum, we will wait forever as there is not maximum wait
         * time for the getDirContext() call.
         */
        for (int idx = 0; idx < 3; idx++) {
            TimedDirContext ctx = cm.getDirContext();
            assertFalse("Expected new context instance.", contexts.contains(ctx));
            contexts.add(ctx);
        }

        /*
         * Release all of the contexts.
         */
        for (TimedDirContext ctx : contexts) {
            cm.releaseDirContext(ctx);
        }

        /*
         * Get the maximum number of contexts from the pool again. These
         * should be the same instances we got the first time since the
         * preferred pool size is 3; which prevents them from being closed
         * on release.
         */
        for (int idx = 0; idx < 3; idx++) {
            TimedDirContext ctx = cm.getDirContext();
            assertTrue("Expected same context instance.", contexts.contains(ctx));
            contexts.add(ctx);
        }
    }

    /**
     * Test that when the context pool is enabled and return to primary is enabled, that after
     * fail-over has occurred that {@link ContextManager#getDirContext()} will return contexts
     * on the primary.
     */
    @Test
    public void getDirContext_ContextPoolEnabled_Failover_ReturnToPrimary() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.addFailoverServer("localhost", failoverLdapServer.getLdapServer().getPort());
        cm.setWriteToSecondary(false);
        cm.setContextPool(true, 2, 1, 0, 10000l, 1l);
        cm.setReturnToPrimary(true);
        cm.setQueryInterval(1);
        cm.initialize();

        /*
         * Get context that is connected to primary server.
         */
        TimedDirContext ctx1 = cm.getDirContext();
        String host1 = (String) ctx1.getEnvironment().get(Context.PROVIDER_URL);

        /*
         * Re-create the context, which should now be connected to the
         * fail-over server.
         */
        TimedDirContext ctx2 = cm.reCreateDirContext(ctx1, "some failure");
        String host2 = (String) ctx2.getEnvironment().get(Context.PROVIDER_URL);
        assertFalse("Expected fail-over server.", host1.equals(host2));

        /*
         * Request new context. This time it should check whether we can return
         * to the primary server.
         */
        TimedDirContext ctx3 = cm.getDirContext();
        String host3 = (String) ctx3.getEnvironment().get(Context.PROVIDER_URL);
        assertEquals("Expected return to primary server.", host1, host3);
    }

    /**
     * Test that when the context pool is enabled context failure handling results in
     * expected behavior for recreating the context pool.
     */
    @Test
    public void getDirContext_ContextPoolEnabled_ContextFailure() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.setContextPool(true, 1, 1, 0, 10000l, 1l);
        cm.initialize();

        /*
         * Get a context from the pool. This will populate the pool.
         *
         * We will release the context so that if the next step doesn't generate
         * a new pool, we will retrieving this one.
         */
        TimedDirContext ctx1 = cm.getDirContext();
        cm.releaseDirContext(ctx1);
        Thread.sleep(1100);

        /*
         * Simulate a failure on the first context. A new context pool will be created
         * since it is more than 1 second after the initial context pool creation.
         */
        TimedDirContext ctx2 = cm.reCreateDirContext(ctx1, "some failure");
        assertTrue("Expected new context pool with new timestamp.", ctx1.getPoolTimestamp() < ctx2.getPoolTimestamp());
        assertNotSame("Expected new context instance.", ctx1, ctx2);
        cm.releaseDirContext(ctx2);

        /*
         * Simulate another failure on the first context. This will NOT create a new context pool since it
         * uses the first context (from the original context pool which has been replaced). Instead, it
         * will grab a context from the new pool.
         */
        TimedDirContext ctx3 = cm.reCreateDirContext(ctx1, "some failure");
        assertSame("Expected same context instance.", ctx2, ctx3);
    }

    /**
     * Ensure that when the context pool is enabled that expired contexts are not
     * returned from {@link ContextManager#getDirContext()}.
     */
    @Test
    public void getDirContext_ContextPoolEnabled_Timeout() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.setContextPool(true, 0, 3, 3, 1l, 1000l);
        cm.initialize();

        Set<TimedDirContext> contexts = new HashSet<TimedDirContext>();

        /*
         * Get the maximum number of contexts from the pool. If we get more
         * than the maximum, we will wait forever as there is not maximum wait
         * time for the getDirContext() call.
         */
        for (int idx = 0; idx < 3; idx++) {
            TimedDirContext ctx = cm.getDirContext();
            assertFalse("Expected new context instance.", contexts.contains(ctx));
            contexts.add(ctx);
        }

        /*
         * Release all of the contexts.
         */
        for (TimedDirContext ctx : contexts) {
            cm.releaseDirContext(ctx);
        }

        /*
         * Let the contexts expire. The pool rounds times to seconds, so we
         * need to let a few seconds pass before continuing on.
         */
        Thread.sleep(2000);

        /*
         * Get the maximum number of contexts from the pool again. These
         * should be new instances since the pool timeout is something really
         * small, so the contexts will have expired.
         */
        for (int idx = 0; idx < 3; idx++) {
            TimedDirContext ctx = cm.getDirContext();
            assertFalse("Expected new context instance.", contexts.contains(ctx));
            contexts.add(ctx);
        }
    }

    /**
     * Call {@link ContextManager#initialize()} having set the minimum configuration.
     */
    @Test
    public void initialize_minimumConfiguration() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.initialize();

        /*
         * Get a context.
         */
        TimedDirContext ctx = cm.getDirContext();
        assertNotNull("Expected to get a TimedDirContext instance.", ctx);

        /*
         * Search will fail as the anonymous bind has no permissions on our
         * embedded LDAP server.
         */
        try {
            ctx.search(BASE_ENTRY, "uid=user1", null);
            fail("Expected NoPermissionException");
        } catch (NoPermissionException e) {
            // Passed.
        }
    }

    /**
     * Call {@link ContextManager#initialize()} without having set the minimum configuration.
     */
    @Test
    public void initialize_MissingPrimaryServer() throws Exception {
        ContextManager cm = new ContextManager();
        assertEquals(InitializeResult.MISSING_PRIMARY_SERVER, cm.initialize());
    }

    /**
     * Call {@link ContextManager#initialize()} with a bind DN, but no bind password.
     */
    @Test
    public void initialize_MissingPassword() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), null);

        assertEquals(InitializeResult.MISSING_PASSWORD, cm.initialize());
    }

    /**
     * Call {@link ContextManager#setContextPool(boolean, Integer, Integer, Integer, Long, Long)}
     * with a initial context pool size that is larger than the maximum context pool size.
     */
    @Test
    public void setContextPool_InitPoolSizeTooBig() throws Exception {
        ContextManager cm = new ContextManager();

        try {
            cm.setContextPool(true, 10, 1, 3, 1000l, 1000l);
            fail("Expected InvalidInitPropertyException.");
        } catch (InvalidInitPropertyException e) {
            // Passed
        }
    }

    /**
     * Call {@link ContextManager#setContextPool(boolean, Integer, Integer, Integer, Long, Long)}
     * with a preferred context pool size that is larger than the maximum context pool size.
     */
    @Test
    public void setContextPool_PreferredPoolSizeTooBig() throws Exception {
        ContextManager cm = new ContextManager();

        try {
            cm.setContextPool(true, 0, 10, 3, 1000l, 1000l);
            fail("Expected InvalidInitPropertyException.");
        } catch (InvalidInitPropertyException e) {
            // Passed
        }
    }

    @Test
    public void testToString() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        SerializableProtectedString bindPassword = new SerializableProtectedString(EmbeddedApacheDS.getBindPassword().toCharArray());
        cm.setSimpleCredentials(EmbeddedApacheDS.getBindDN(), bindPassword);
        cm.setSSLAlias("sslAlias");
        cm.setSSLEnabled(true);
        cm.setConnectTimeout(12345l);
        cm.setReadTimeout(12345l);
        cm.setJndiOutputEnabled(true);
        cm.addFailoverServer("localhost", failoverLdapServer.getLdapServer().getPort());
        cm.setContextPool(true, 0, 1, 3, 1000l, 2000l);
        cm.setWriteToSecondary(true);
        cm.setQueryInterval(3000l);
        cm.setReferral("ignore");
        cm.setReturnToPrimary(true);
        cm.setBinaryAttributeNames("attr1 attr2 attr3");
        cm.initialize();

        String toString = cm.toString();
        assertTrue("Did not find Bind DN in toString: " + toString, toString.contains("iBindDN=" + EmbeddedApacheDS.getBindDN()));
        assertTrue("Did not find Bind Password in toString: " + toString, toString.contains("iBindPassword=*****"));
        assertTrue("Did not find SSL Alias in toString: " + toString, toString.contains("iSSLAlias=sslAlias"));
        assertTrue("Did not find SSL Enabled in toString: " + toString, toString.contains("iSSLEnabled=true"));
        assertTrue("Did not find Connect Timeout in toString: " + toString, toString.contains("iConnectTimeout=12345"));
        assertTrue("Did not find Read Timeout in toString: " + toString, toString.contains("iReadTimeout=12345"));
        assertTrue("Did not find JndiOutputEnabled in toString: " + toString, toString.contains("iJndiOutputEnabled=true"));
        assertTrue("Did not find Primary Server in toString: " + toString, toString.contains("iPrimaryServer=localhost:" + primaryLdapServer.getLdapServer().getPort()));
        assertTrue("Did not find Failover Servers in toString: " + toString,
                   toString.contains("iFailoverServers=[localhost:" + failoverLdapServer.getLdapServer().getPort() + "]"));
        assertTrue("Did not find Context Pool Enabled in toString: " + toString, toString.contains("iContextPoolEnabled=true"));
        assertTrue("Did not find Initial Pool Size in toString: " + toString, toString.contains("iInitPoolSize=0"));
        assertTrue("Did not find Preferred Pool Size in toString: " + toString, toString.contains("iPrefPoolSize=1"));
        assertTrue("Did not find Maximum Pool Size in toString: " + toString, toString.contains("iMaxPoolSize=3"));
        assertTrue("Did not find Pool Timeout in toString: " + toString, toString.contains("iPoolTimeOut=1000"));
        assertTrue("Did not find Pool Wait Time in toString: " + toString, toString.contains("iPoolWaitTime=2000"));
        assertTrue("Did not find Write To Secondary in toString: " + toString, toString.contains("iWriteToSecondary=true"));
        assertTrue("Did not find Query Interval in toString: " + toString, toString.contains("iQueryInterval=3000"));
        assertTrue("Did not find Referral in toString: " + toString, toString.contains("iReferral=ignore"));
        assertTrue("Did not find Return to Primary in toString: " + toString, toString.contains("iReturnToPrimary=true"));
    }

    @Test
    public void testToStringKRB5() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        cm.setKerberosCredentials("UnitTestLdap", null, "testPrincipal", "DummyTicketCache", null);
        cm.initialize();

        String toString = cm.toString();
        assertTrue("Found Bind DN in toString: " + toString, toString.contains("iBindDN=null"));
        assertTrue("Did not find Primary Server in toString: " + toString, toString.contains("iPrimaryServer=localhost:" + primaryLdapServer.getLdapServer().getPort()));
        assertTrue("Did not find bindAuthMeach in toString: " + toString, toString.contains("bindAuthMechanism=" + ConfigConstants.CONFIG_BIND_AUTH_KRB5));
        assertTrue("Did not find krb5Principal in toString: " + toString, toString.contains("krb5Principal=testPrincipal"));
        assertTrue("Did not find krb5TicketCache in toString: " + toString, toString.contains("krb5TicketCache=DummyTicketCache"));
    }

    /**
     * Test setting the connection timeout. For JNDI this appears to cover the entire amount of time it
     * takes to bind (open connection and read the bind response), not just connect. Notice that the
     * error message mentions read timeout, but that we did not set it. It must be setting the read timeout
     * from the connect timeout when doing a bind (which creates a new connection to the LDAP server).
     *
     * I did try to get a root cause of SocketTimeoutException by using a non-routable IP, but it was flaky and
     * sometimes the network returned no route to host before the timeout could occur.
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void connectTimeout() throws Exception {

        ServerSocket serverSocket = new ServerSocket(0);

        try {
            /*
             * Configure the context manager with a 500 ms connect timeout.
             * Bumped up from a 100ms timeout as that's probably too much precision to expect from our build machines
             * Looking for a balance of testing the timeout without making the unit test take a long time
             */
            long expectedTimeout = 500L;
            ContextManager cm = new ContextManager();
            cm.setPrimaryServer("localhost", serverSocket.getLocalPort());
            cm.setContextPool(false, null, null, null, null, null);
            cm.setConnectTimeout(expectedTimeout);
            cm.initialize();

            long time = System.nanoTime();
            try {
                cm.createDirContext(USER_DN, "password".getBytes());
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // javax.naming.NamingException: LDAP response read timed out, timeout used:500ms.
                time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time);
                assertTrue("Expected connect timeout to be " + expectedTimeout + " ms but was " + time,
                           time >= expectedTimeout && time <= (expectedTimeout + 200));
            }

            /*
             * Configure the context manager with a 1000 ms connect timeout.
             */
            expectedTimeout = 1000L;
            cm.setConnectTimeout(expectedTimeout);
            cm.initialize();

            time = System.nanoTime();
            try {
                cm.createDirContext(USER_DN, "password".getBytes());
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // javax.naming.NamingException: LDAP response read timed out, timeout used:1000ms.
                time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time);
                assertTrue("Expected connect timeout to be " + expectedTimeout + " millisecond but was " + time,
                           time >= expectedTimeout && time <= (expectedTimeout + 200));
            }
        } finally {
            serverSocket.close();
        }
    }

    /**
     * Test setting the read timeout.
     *
     * @throws Exception If the test fails for some reason.
     */
    @Test
    public void readTimeout() throws Exception {

        /*
         * Configure the context manager with a 100 ms read timeout.
         */
        long expectedTimeout = 100L;
        ContextManager cm = new ContextManager();
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.setContextPool(false, null, null, null, null, null);
        cm.setReadTimeout(expectedTimeout);
        cm.initialize();

        TimedDirContext ctx = cm.createDirContext(USER_DN, "password".getBytes());

        long time = System.currentTimeMillis();
        try {
            ctx.search(BASE_ENTRY, "objectclass=*", null);
        } catch (NamingException e) {
            time = System.currentTimeMillis() - time;
            assertTrue("Expected connect timeout to be " + expectedTimeout + " millisecond.", time >= expectedTimeout && time <= (expectedTimeout + 100));
        }
        ctx.close();

        /*
         * Configure the context manager with a 500 ms read timeout.
         */
        expectedTimeout = 500L;
        cm.setReadTimeout(expectedTimeout);
        cm.initialize();

        ctx = cm.createDirContext(USER_DN, "password".getBytes());
        time = System.currentTimeMillis();
        try {
            ctx.search(BASE_ENTRY, "objectclass=*", null);
        } catch (NamingException e) {
            time = System.currentTimeMillis() - time;
            assertTrue("Expected connect timeout to be " + expectedTimeout + " millisecond.", time >= expectedTimeout && time <= (expectedTimeout + 100));
        }
    }

    /**
     * Test {@link ContextManager#createDirContext()} with new BindAuthMechanism set to simple
     */
    @Test
    public void createDirContextSetSimpleBindAuthentication() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setBindAuthMechanism(ConfigConstants.CONFIG_AUTHENTICATION_TYPE_SIMPLE);
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.initialize();

        TimedDirContext ctx = null;
        try {
            assertNotNull(cm.createDirContext(USER_DN, "password".getBytes()));
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    /**
     * Test {@link ContextManager#createDirContext()} with new BindAuthMechanism set to none
     */
    @Test
    public void createDirContextSetNoneBindAuthentication() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setBindAuthMechanism(ConfigConstants.CONFIG_AUTHENTICATION_TYPE_NONE);
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.initialize();

        TimedDirContext ctx = null;
        try {
            assertNotNull(cm.createDirContext(USER_DN, "password".getBytes()));
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    /**
     * Call {@link ContextManager#initialize()} with GSSAPI set, but no principal name
     */
    @Test
    public void initialize_MissingKrb5PrincipalName() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());

        assertEquals(InitializeResult.MISSING_KRB5_PRINCIPAL_NAME, cm.initialize());
    }

    /**
     * Call {@link ContextManager#initialize()} with GSSAPI set, but null principal name
     */
    @Test
    public void initialize_NullKrb5PrincipalName() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.setKerberosCredentials("UnitTestLdap", null, null, "badFileName", null);

        assertEquals(InitializeResult.MISSING_KRB5_PRINCIPAL_NAME, cm.initialize());
    }


    /**
     * Call {@link ContextManager#initialize()} with GSSAPI set, but empty principal name
     */
    @Test
    public void initialize_EmptyKrb5PrincipalName() throws Exception {
        ContextManager cm = new ContextManager();
        cm.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        cm.setPrimaryServer("localhost", primaryLdapServer.getLdapServer().getPort());
        cm.setKerberosCredentials("UnitTestLdap", null, "", "badFileName", null);

        assertEquals(InitializeResult.MISSING_KRB5_PRINCIPAL_NAME, cm.initialize());
    }
}
