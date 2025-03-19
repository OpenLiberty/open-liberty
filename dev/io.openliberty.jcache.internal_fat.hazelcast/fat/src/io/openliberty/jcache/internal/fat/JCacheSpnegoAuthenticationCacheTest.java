/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat;

import static com.ibm.ws.security.spnego.fat.config.SPNEGOConstants.FIREFOX;
import static com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils.BASE_DN;
import static com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils.DOMAIN;
import static com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient.DEFAULT_CONTEXT_ROOT;
import static com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient.DEFAULT_REALM;
import static com.ibm.ws.webcontainer.security.test.servlets.ServletClient.PROTECTED_SIMPLE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Spnego;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.ApacheDSandKDC;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.testresource.KdcResource;
import componenttest.annotation.SkipIfSysProp;

/**
 * Test the distributed authentication cache with GSS credentials generated from SPNEGO authentication.
 */
@SkipIfSysProp("skip.tests=true")
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class JCacheSpnegoAuthenticationCacheTest extends BaseTestCase {

    private static final String CANONICAL_HOSTNAME = "canhostname1";

    private static final String SPN = "HTTP/" + CANONICAL_HOSTNAME + "@" + DOMAIN;
    private static final String SPN_DN = "uid=" + CANONICAL_HOSTNAME + "," + BASE_DN;
    private static final String SPN_PWD = "httppwd";

    private static final String USER1_KRB5_PRINCIPAL = USER1_NAME + "@" + DOMAIN;

    private static final String JCACHE_HIT_USER1_SPNEGO = JCACHE_HIT + USER1_KRB5_PRINCIPAL;
    private static final String JCACHE_MISS_USER1_SPNEGO = JCACHE_MISS + USER1_KRB5_PRINCIPAL;
    private static final String IN_MEMORY_HIT_USER1_SPNEGO = IN_MEMORY_HIT + USER1_KRB5_PRINCIPAL;
    private static final String IN_MEMORY_MISS_USER1_SPNEGO = IN_MEMORY_MISS + USER1_KRB5_PRINCIPAL;

    private static BasicAuthClient basicAuthClient1;

    private static String krb5ConfFile;
    private static String spnKeytabFile;

    @Server("io.openliberty.jcache.internal.fat.spnego.auth.cache.1")
    public static LibertyServer server1;

    /**
     * Use a {@link KdcResource} for this test.
     *
     * We pass in a {@link Callable} to set up the configuration since we can't
     * do it in the {@link #beforeClass()} method as JUnit does not
     * guarantee the order in which {@link ClassRule} and {@link BeforeClass}
     * will run (it is JRE dependent). Our setup depends on the {@link KdcResource} being
     * configured, thus why we pass it in as a {@link Callable}.
     */
    @ClassRule
    public static KdcResource kdcResource = new KdcResource(new Callable<Void>() {

        @Override
        public Void call() throws Exception {
            /*
             * Create our KRB5 principle.
             */
            ApacheDSandKDC.createPrincipal(USER1_NAME, USER1_PASSWORD);

            /*
             * Create an SPN entry and keytab for that SPN entry.
             * Create the KRB5 configuration file.
             */
            ApacheDSandKDC.createSpnegoSPNEntry(SPN_DN, SPN, SPN_PWD);
            spnKeytabFile = ApacheDSandKDC.createSpnegoSPNKeytab(CANONICAL_HOSTNAME, SPN, SPN_PWD);
            krb5ConfFile = ApacheDSandKDC.getDefaultConfigFile();
            return null;
        }
    });

    @BeforeClass
    public static void beforeClass() throws Exception {
        assumeShouldNotSkipTests();

        /*
         * Transform apps for EE9+.
         */
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server1.getServerRoot() + "/apps/basicauth.war"));
        }
    }

    @Before
    public void before() throws Exception {
        String groupName = UUID.randomUUID().toString();

        /*
         * Start server 1.
         */
        server1.addInstalledAppForValidation("basicauth");
        startServer1(server1, groupName, null, null);
        basicAuthClient1 = new BasicAuthClient(server1, DEFAULT_REALM, "SimpleServlet", DEFAULT_CONTEXT_ROOT);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);
    }

    @After
    public void after() throws Exception {

        try {
            /*
             * We should not have cleared the authentication cache's JCache.
             */
            assertAuthCacheJCacheNotCleared(server1);
        } finally {
            /*
             * Stop servers in the reverse order they were started.
             */
            stopServer(server1);
        }
    }

    /**
     * Test caching of SPNEGO authentication credentials in the distributed JCache authentication cache. We will use
     * only one server since this test configures 'includeClientGSSCredentialInSubject = false', which results in a
     * GSS credential being put into the subject that cannot be serialized, and thus cannot be placed in the JCache
     * authentication cache. Instead it is inserted into the in-memory cache.
     *
     * <pre>
     * 1. Generate a new token to authenticate with spnego. Add it to the request. We expect that both the in-memory
     *    and the JCache auth cache will both have cache misses since this is the first request. We should also see the
     *    GSS credential in the subject returned in the response.
     * 2. Generate a new token to authenticate with spnego. Add it to the request. We expect that we will get a miss
     *    in the JCache auth cache, but a hit in the in-memory auth cache. We should also see the GSS credential in the
     *    subject returned in the response.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_ZOS) // Skip on z/OS due to connection refusal by kerberos client
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void authCache_spnego() throws Exception {
        /*
         * Can we run these tests?
         */
        kdcResource.assumeCanRunTests();

        /*
         * Update SPNEGO configuration.
         */
        ServerConfiguration config = server1.getServerConfiguration();
        Spnego spnego = config.getSpnego();
        spnego.krb5Config = krb5ConfFile;
        spnego.krb5Keytab = spnKeytabFile;
        spnego.servicePrincipalNames = SPN;
        spnego.canonicalHostName = "false";
        spnego.includeClientGSSCredentialInSubject = true;
        updateConfigDynamically(server1, config);

        /*
         * 1. Generate a new token to authenticate with spnego. Add it to the request. We expect that both the in-memory
         * and the JCache auth cache will both have cache misses since this is the first request. We should also see the
         * GSS credential in the subject returned in the response.
         */
        String token = kdcResource.createToken(server1, USER1_NAME, USER1_PASSWORD, SPN, krb5ConfFile);
        Map<String, String> headers = KdcResource.setTestHeaders("Negotiate " + token, FIREFOX, CANONICAL_HOSTNAME, null);
        String response = basicAuthClient1.accessProtectedServletWithValidHeaders(PROTECTED_SIMPLE, headers);
        assertResponseContainsGSSCredentials(true, response);
        assertResponseContainsCustomCredentials(response);
        assertJCacheSpnegoAuthCacheHit(false, server1);
        assertInMemorySpnegoAuthCacheHit(false, server1);

        /*
         * 2. Generate a new token to authenticate with spnego. Add it to the request. We expect that we will get a miss
         * in the JCache auth cache, but a hit in the in-memory auth cache. We should also see the GSS credential in the
         * subject returned in the response.
         */
        resetMarksInLogs(server1);
        token = kdcResource.createToken(server1, USER1_NAME, USER1_PASSWORD, SPN, krb5ConfFile);
        headers = KdcResource.setTestHeaders("Negotiate " + token, FIREFOX, CANONICAL_HOSTNAME, null);
        basicAuthClient1.resetClientState(); // Clear tokens, etc
        resetMarksInLogs(server1);
        response = basicAuthClient1.accessProtectedServletWithValidHeaders(PROTECTED_SIMPLE, headers);
        assertResponseContainsGSSCredentials(true, response);
        assertResponseContainsCustomCredentials(response);
        assertJCacheSpnegoAuthCacheHit(false, server1);
        assertInMemorySpnegoAuthCacheHit(true, server1);
    }

    /**
     * Test caching of SPNEGO authentication credentials in the distributed JCache authentication cache. We will use
     * only one server since this test configures 'includeClientGSSCredentialInSubject = false', which results in a
     * GSS credential being put into the subject that cannot be serialized, and thus cannot be placed in the JCache
     * authentication cache. Instead it is inserted into the in-memory cache.
     *
     * <pre>
     * 1. Generate a new token to authenticate with spnego. Add it to the request. We expect that both the in-memory
     *    and the JCache auth cache will both have cache misses since this is the first request. We should NOT see the
     *    GSS credential in the subject returned in the response since includeClientGSSCredentialInSubject=false. This
     *    setting also has allowed us to cache the subject in the JCache auth cache.
     * 2. Generate a new token to authenticate with spnego. Add it to the request. We expect that we will get a hit
     *    in the JCache auth cache since we were able to cache it in the first request. We should NOT see the
     *    GSS credential in the subject returned in the response since includeClientGSSCredentialInSubject=false.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    @SkipIfSysProp(SkipIfSysProp.OS_ZOS) // Skip on z/OS due to connection refusal by kerberos client
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void authCache_spnego_excludeGssCred() throws Exception {
        /*
         * Can we run these tests?
         */
        kdcResource.assumeCanRunTests();

        /*
         * Update SPNEGO configuration.
         */
        ServerConfiguration config = server1.getServerConfiguration();
        Spnego spnego = config.getSpnego();
        spnego.krb5Config = krb5ConfFile;
        spnego.krb5Keytab = spnKeytabFile;
        spnego.servicePrincipalNames = SPN;
        spnego.canonicalHostName = "false";
        spnego.includeClientGSSCredentialInSubject = false;
        updateConfigDynamically(server1, config);

        /*
         * 1. Generate a new token to authenticate with spnego. Add it to the request. We expect that both the in-memory
         * and the JCache auth cache will both have cache misses since this is the first request. We should NOT see the
         * GSS credential in the subject returned in the response since includeClientGSSCredentialInSubject=false. This
         * setting also has allowed us to cache the subject in the JCache auth cache.
         */
        String token = kdcResource.createToken(server1, USER1_NAME, USER1_PASSWORD, SPN, krb5ConfFile);
        Map<String, String> headers = KdcResource.setTestHeaders("Negotiate " + token, FIREFOX, CANONICAL_HOSTNAME, null);
        String response = basicAuthClient1.accessProtectedServletWithValidHeaders(PROTECTED_SIMPLE, headers);
        assertResponseContainsGSSCredentials(false, response);
        assertResponseContainsCustomCredentials(response);
        assertJCacheSpnegoAuthCacheHit(false, server1);
        assertInMemorySpnegoAuthCacheHit(false, server1);

        /*
         * 2. Generate a new token to authenticate with spnego. Add it to the request. We expect that we will get a hit
         * in the JCache auth cache since we were able to cache it in the first request. We should NOT see the
         * GSS credential in the subject returned in the response since includeClientGSSCredentialInSubject=false.
         */
        token = kdcResource.createToken(server1, USER1_NAME, USER1_PASSWORD, SPN, krb5ConfFile);
        headers = KdcResource.setTestHeaders("Negotiate " + token, FIREFOX, CANONICAL_HOSTNAME, null);
        basicAuthClient1.resetClientState(); // Clear tokens, etc
        resetMarksInLogs(server1);
        response = basicAuthClient1.accessProtectedServletWithValidHeaders(PROTECTED_SIMPLE, headers);
        assertResponseContainsGSSCredentials(false, response);
        assertResponseContainsCustomCredentials(response);
        assertJCacheSpnegoAuthCacheHit(true, server1);
    }

    /**
     * Assert whether there was or was not a SPNEGO auth (user/password) cache hit to the JCache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    private static void assertJCacheSpnegoAuthCacheHit(boolean expectCacheHit, LibertyServer server) throws Exception {
        if (expectCacheHit) {
            assertFalse("Request should have resulted in an JCache auth cache hit for: " + JCACHE_HIT_USER1_SPNEGO,
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_HIT_USER1_SPNEGO).isEmpty());
        } else {
            assertFalse("Request should have resulted in an JCache auth cache miss for: " + JCACHE_MISS_USER1_SPNEGO,
                        server.findStringsInLogsAndTraceUsingMark(JCACHE_MISS_USER1_SPNEGO).isEmpty());
        }
    }

    /**
     * Assert whether there was or was not a SPNEGO auth (user/password) cache hit to the in-memory cache.
     *
     * @param expectCacheHit Whether to expect there was a cache hit.
     * @param server         The server to check.
     * @throws Exception If the check failed for some unforeseen reason.
     */
    private static void assertInMemorySpnegoAuthCacheHit(boolean expectCacheHit, LibertyServer server) throws Exception {
        if (expectCacheHit) {
            assertFalse("Request should have resulted in an in-memory auth cache hit for: " + IN_MEMORY_HIT_USER1_SPNEGO,
                        server.findStringsInLogsAndTraceUsingMark(IN_MEMORY_HIT_USER1_SPNEGO).isEmpty());
        } else {
            assertFalse("Request should have resulted in an in-memory auth cache miss for: " + IN_MEMORY_MISS_USER1_SPNEGO,
                        server.findStringsInLogsAndTraceUsingMark(IN_MEMORY_MISS_USER1_SPNEGO).isEmpty());
        }
    }

    /**
     * Assert whether the response contains the subject with the GSS credential.
     *
     * @param expectContains Whether we expect the response to contain the GSS credential.
     * @param response       The response to check.
     * @throws Exception If there was an error getting the Java info for the server.
     */
    private static void assertResponseContainsGSSCredentials(boolean expectContains, String response) throws Exception {
        /*
         * The credential we look for depends on the version of Java we are using on the server.
         */
        if (expectContains) {
            assertTrue("Did not find the GSS private credential in the subject.", response.matches("(?s).*Private Credential:.*--- GSSCredential ---.*") ||
                                                                                  response.contains("Private Credential: [GSSCredential"));
        } else {
            assertFalse("Did not expect to find the GSS private credential in the subject.",
                        response.matches("(?s).*Private Credential:.*--- GSSCredential ---.*") || response.contains("Private Credential: [GSSCredential"));
        }
    }
}
