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
package io.openliberty.jcache.internal.fat.testresource;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.junit.rules.ExternalResource;

import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.ApacheDSandKDC;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyServer;

/**
 * Testing resource that will start up a KDC that can be used for SPNEGO authentication.
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class KdcResource extends ExternalResource {

    private static final Class<?> c = KdcResource.class;
    private static final String IBM_JDK_KRB5_LOGIN = "ibmKrb5Login";
    private static final String SUN_JDK_KRB5_LOGIN = "sunKrb5Login";
    private static final String KDC_REALM = LdapKerberosUtils.DOMAIN;
    private static final String KDC_HOSTNAME = LdapKerberosUtils.HOSTNAME;
    private static final String JAAS_CONF_FILE = SPNEGOConstants.CLIENT_JAAS_CONFIG_FILE;

    private boolean ibmJdkV8Lower;
    private boolean sunOracleJdkV8Higher;
    private boolean otherSupportJdks;
    private boolean runTests;
    private Callable<Void> oneTimeSetup;

    private static final Oid KRB5_MECH_OID;
    private static final Oid SPNEGO_MECH_OID;

    static {
        final String KRB5_OID = "1.2.840.113554.1.2.2";
        final String SPNEGO_OID = "1.3.6.1.5.5.2";
        Oid krb5MechOid = null;
        Oid spnegoMechOid = null;
        try {
            krb5MechOid = new Oid(KRB5_OID);
            spnegoMechOid = new Oid(SPNEGO_OID);
        } catch (GSSException ex) {
            //do nothing
        }
        KRB5_MECH_OID = krb5MechOid;
        SPNEGO_MECH_OID = spnegoMechOid;
    }

    public KdcResource() {
        /* Default constructor. */
    }

    /**
     * Construct a {@link KdcResource} instance that includes a one-time setup
     * {@link Callable} that will be run after all other services have been
     * initialized.
     *
     * @param oneTimeSetup The one time setup {@link Callable} to call.
     */
    public KdcResource(Callable<Void> oneTimeSetup) {
        this.oneTimeSetup = oneTimeSetup;
    }

    @Override
    protected void before() throws Exception {
        final String thisMethod = "before";
        Log.info(c, thisMethod, "Configuring " + c.getSimpleName() + ".");

        runTests = isSupportedJDK() && isSupportedOS();

        /*
         * Start the KDC if we are running tests.
         */
        if (runTests) {
            ApacheDSandKDC.setupService();

            /*
             * Perform any one time setup.
             */
            if (oneTimeSetup != null) {
                Log.info(c, thisMethod, "Started running one time setup...");
                Future<Void> future = Executors.newSingleThreadExecutor().submit(oneTimeSetup);
                future.get();
                Log.info(c, thisMethod, "Finished running one time setup.");
            }
        }

        Log.info(c, thisMethod, "Configuring SpengoResource is complete.");
    }

    @Override
    protected void after() {
        final String thisMethod = "after";
        Log.info(c, thisMethod, "Tearing down " + c.getSimpleName() + "...");
        if (runTests) {
            try {
                ApacheDSandKDC.tearDownService();
            } catch (Exception e) {
                Log.error(c, thisMethod, e, "Failed to tear down KDC after test.");
            }
        }
        Log.info(c, thisMethod, "Tearing down " + c.getSimpleName() + " is complete.");
    }

    /**
     * Is this a supported JDK for testing?
     *
     * @return True if the JDK is supported.
     * @throws IOException if there is an error determing whether the JDK is supported.
     */
    private boolean isSupportedJDK() throws IOException {
        String thisMethod = "isSupportedJDK";
        boolean runTests = true;
        JavaInfo javaInfo = JavaInfo.forCurrentVM();

        ibmJdkV8Lower = javaInfo.vendor() == Vendor.IBM && javaInfo.majorVersion() <= 8;
        sunOracleJdkV8Higher = javaInfo.vendor() == Vendor.SUN_ORACLE && javaInfo.majorVersion() >= 8;
        otherSupportJdks = javaInfo.majorVersion() >= 11 || sunOracleJdkV8Higher;

        boolean isIbmJdk8 = javaInfo.vendor() == Vendor.IBM && javaInfo.majorVersion() == 8;

        Log.info(c, thisMethod, "The JDK used on this system is version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.vendor());
        if (!ibmJdkV8Lower && !otherSupportJdks && !sunOracleJdkV8Higher || isIbmJdk8) {
            Log.info(c, thisMethod, "The JDK used on this system is version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.vendor() +
                                    ". Because only IBM JDK version 7 or less, Oracle and Open JDK version 8 and higher and JDK version 11 are currently supported, no tests will be run.");
            runTests = false;
        }
        if (isHybridJDK(javaInfo)) {
            runTests = false;
        }
        Log.info(c, thisMethod, "The JDK vendor used is " + javaInfo.vendor() + " and version: " + javaInfo.majorVersion());

        if (!runTests) {
            Log.info(c, thisMethod, "=== JDK NOT SUPPORTED FOR SPNEGO FAT TESTS ===");
            Log.info(c, thisMethod, "=== SKIPPING SPNEGO FAT TESTS ===");
        }

        return runTests;
    };

    /**
     * Is this a supported OS for testing?
     *
     * @return True if the OS is supported.
     * @throws IOException if there is an error determing whether the OS is supported.
     */
    private boolean isSupportedOS() throws IOException {
        String thisMethod = "isSupportedOS";
        boolean runTests = true;

        // The ApacheDSAndKDC does not work correctly on MacOS
        // Skip the tests if we are not on one of the supported OSes
        String os = System.getProperty("os.name", "UNKNOWN").toUpperCase();
        if (os.contains("MAC") || os.contains("OSX")) {
            if (FATRunner.FAT_TEST_LOCALRUN) {
                throw new RuntimeException("Running on an unsupported os: " + os);
            } else {
                Log.info(c, thisMethod, "Skipping test because of unsupported os: " + os);
                runTests = false;
            }
        }

        if (!runTests) {
            Log.info(c, thisMethod, "=== OS NOT SUPPORTED FOR ApacheDSAndKDC SPNEGO FAT TESTS ===");
            Log.info(c, thisMethod, "=== SKIPPING ApacheDSAndKDC SPNEGO FAT TESTS ===");
        } else {
            Log.info(c, thisMethod, "Operating System is supported: " + os);
        }

        return runTests;

    }

    /**
     * Are we using the IBM/Oracle hybrid JDK?
     *
     * @param javaInfo The {@link JavaInfo} instance.
     * @return True if we are using the hybrid JDK.
     */
    private boolean isHybridJDK(JavaInfo javaInfo) {
        String thisMethod = "isHybridJDK";

        boolean hybridJdk = false;
        String javaRuntime = System.getProperty("java.runtime.version");
        Log.info(c, thisMethod, "The  current runtime version is: " + javaRuntime);

        if ((javaInfo.vendor() == Vendor.SUN_ORACLE) && javaRuntime.contains("SR")) {
            hybridJdk = true;
        } else {
            hybridJdk = false;
        }
        Log.info(c, thisMethod, "Hybrid JDK: " + hybridJdk);
        return hybridJdk;
    }

    /**
     * Create a token suitable for SPNEGO authentication with a Liberty server.
     *
     * @param server       The {@link LibertyServer} that contains the jaas.conf file used for login.
     * @param username     The username to login with.
     * @param password     The password to login with.
     * @param targetSpn    The target SPN.
     * @param krb5ConfFile The krb5.conf file to use for login.
     * @return The token.
     * @throws Exception If there was an issue creating the token.
     */
    public String createToken(LibertyServer server, String username, String password, String targetSpn, String krb5ConfFile) throws Exception {
        String method = "createToken";
        Log.info(c, method, "Target SPN: " + targetSpn);

        Subject subject = null;
        String krbName = username;
        subject = kerberosLogin(server, username, password, krb5ConfFile, null, null, null);

        return createToken(subject, krbName, targetSpn, true, GSSCredential.DEFAULT_LIFETIME, GSSCredential.INDEFINITE_LIFETIME,
                           GSSCredential.INDEFINITE_LIFETIME,
                           GSSContext.DEFAULT_LIFETIME, SPNEGO_MECH_OID);
    }

    /**
     * Create a token suitable for SPNEGO authentication.
     *
     * @param subject            The Subject from the Kerberos login.
     * @param userName           The username to login with.
     * @param targetSpn          The target SPN.
     * @param credentialDelegate Whether to delegate the credential.
     * @param credentialLifetime The credential lifetime.
     * @param credInitLifetime   The credential init lifetime.
     * @param credAcceptLifetime The credential accept lifetime.
     * @param contextLifetime    The context lifetime.
     * @param mechOid            The mechanism OID.
     * @return The token.
     * @throws Exception If there was an error generating the token.
     */
    private String createToken(Subject subject, final String userName, String targetSpn, boolean credentialDelegate, final int credentialLifetime,
                               final int credInitLifetime,
                               final int credAcceptLifetime, int contextLifetime, Oid mechOid) throws Exception {
        String thisMethod = "createToken";
        byte[] spnegoToken = new byte[0];
        GSSContext clientContext = null;
        try {
            final GSSManager manager = GSSManager.getInstance();

            GSSCredential clientGssCreds = (GSSCredential) Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws GSSException, Exception {
                    try {
                        Log.info(c, "run", "Creating GSS name");
                        GSSName gssName = manager.createName(userName, GSSName.NT_USER_NAME, KRB5_MECH_OID);
                        Log.info(c, "run", "Creating GSS credential");
                        GSSCredential gssCred = manager.createCredential(gssName.canonicalize(KRB5_MECH_OID),
                                                                         credentialLifetime,
                                                                         KRB5_MECH_OID,
                                                                         GSSCredential.INITIATE_ONLY);
                        Log.info(c, "run", "Adding mechanism element to GSS credential");
                        gssCred.add(gssName, credInitLifetime, credAcceptLifetime, SPNEGO_MECH_OID, GSSCredential.INITIATE_ONLY);
                        return gssCred;
                    } catch (GSSException gsse) {
                        Log.info(c, "run", "Caught GSSException: " + gsse);
                    } catch (Exception e) {
                        Log.info(c, "run", "Caught exception: " + e.getMessage());
                    }
                    return null;
                }
            });

            // check for null, meaning failure creating gssCred, need to throw exception since next call will fail
            if (clientGssCreds == null) {
                throw new GSSException(13);
            }

            Log.info(c, thisMethod, "Creating GSSName for SPN " + targetSpn);
            GSSName gssServerName = manager.createName(targetSpn, GSSName.NT_USER_NAME);

            Log.info(c, thisMethod, "Creating client context");
            clientContext = manager.createContext(gssServerName.canonicalize(mechOid),
                                                  mechOid,
                                                  clientGssCreds,
                                                  contextLifetime);
            clientContext.requestLifetime(contextLifetime);
            clientContext.requestCredDeleg(credentialDelegate);
            clientContext.requestReplayDet(false);
            Log.info(c, thisMethod, "Initializing security context and obtaining SPNEGO token");
            spnegoToken = clientContext.initSecContext(spnegoToken, 0, spnegoToken.length);

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.info(c, thisMethod, "Unexpected exception: " + ex.getMessage());
            throw ex;
        } finally {
            if (clientContext != null) {
                clientContext.dispose();
            }
        }

        String spnegoTokenStr = Base64Coder.encode(spnegoToken);

        Log.info(c, thisMethod, "spnegoToken: " + spnegoTokenStr);
        return spnegoTokenStr;
    }

    /**
     * Return a map of HTTP headers for use when making a SPNEGO authentication request to the Liberty server.
     *
     * @param authorizationHeader The authorization header to use.
     * @param userAgent           The user agent to use.
     * @param host                The host to use.
     * @param remoteAddr          The remote address to use.
     * @return The map of headers.
     */
    public static Map<String, String> setTestHeaders(String authorizationHeader, String userAgent, String host, String remoteAddr) {
        Map<String, String> headers = new HashMap<String, String>();
        if (authorizationHeader != null) {
            headers.put(SPNEGOConstants.HEADER_AUTHORIZATION, authorizationHeader);
        }
        if (userAgent != null) {
            headers.put(SPNEGOConstants.HEADER_USER_AGENT, userAgent);
        }
        if (host != null) {
            headers.put(SPNEGOConstants.HEADER_HOST, host);
        }
        if (remoteAddr != null) {
            headers.put(SPNEGOConstants.HEADER_REMOTE_ADDR, remoteAddr);
        }
        return headers;
    }

    /**
     * Should we run any tests using this {@link KdcResource}? This is usually determined based on running (or not running)
     * a supported JRE.
     */
    public final void assumeCanRunTests() {
        assumeTrue(runTests);
    }

    /**
     * Perform a kerberos login to the KDC.
     *
     * @param server                The server to get JAAS login configuration from.
     * @param userName              The user name to login with.
     * @param password              The password to login with.
     * @param krb5LoginConfig       The krb5.conf to login with.
     * @param realm                 The realm for the KDC.
     * @param kdcHostName           The KDC host name.
     * @param jaasLoginContextEntry The JAAS login context entry name to use.
     * @return The subject.
     * @throws Exception if there was an error logging in.
     */
    private Subject kerberosLogin(LibertyServer server, String userName, String password, String krb5LoginConfig, String realm, String kdcHostName,
                                  String jaasLoginContextEntry) throws Exception {
        final String thisMethod = "kerberosLogin";
        Log.info(c, thisMethod, "server: " + server + " userName: " + userName + " password: " + password +
                                " krb5LoginConfig: " + krb5LoginConfig + " realm: " + realm + " kdcHostName: " + kdcHostName +
                                " jaasLoginContextEntry: " + jaasLoginContextEntry);

        String loginContextEntry = setupLoginConfig(server, krb5LoginConfig, realm, kdcHostName, jaasLoginContextEntry);

        WSCallbackHandlerImpl wscbh = new WSCallbackHandlerImpl(userName, password);
        Subject subject = null;
        try {
            LoginContext lc = new LoginContext(loginContextEntry, wscbh);
            lc.login();
            subject = lc.getSubject();
        } catch (LoginException e) {
            Log.info(c, thisMethod, "Unexpected exception: " + e.getMessage());
            throw e;
        }

        Log.info(c, thisMethod, "Kerberos login subject: " + subject);
        return subject;
    }

    /**
     * Setup the login configuration for the specified server.
     *
     * @param server                The server to configure login configuration for.
     * @param krb5Config            The krb5.conf file to use.
     * @param realm                 The realm to use.
     * @param kdcHostName           The KDC hostname.
     * @param jaasLoginContextEntry The JAAS login context entry to use.
     * @return The login context entry being used.
     */
    private String setupLoginConfig(LibertyServer server, String krb5Config, String realm, String kdcHostName, String jaasLoginContextEntry) {
        final String thisMethod = "setupLoginConfig";
        Log.info(c, thisMethod, "krb5Config: " + krb5Config + " realm: " + realm + " kdcHostName: " + kdcHostName);

        String loginContextEntry = IBM_JDK_KRB5_LOGIN;
        String jaasLoginConfig = server.getServerRoot() + JAAS_CONF_FILE;

        if (realm != null && !realm.isEmpty()) {
            Log.info(c, thisMethod, "Setting system properties java.security.krb5.realm and java.security.krb5.kdc");
            System.setProperty("java.security.krb5.realm", realm);
            System.setProperty("java.security.krb5.kdc", (kdcHostName == null) ? KDC_HOSTNAME : kdcHostName);
        } else if (krb5Config != null) {
            Log.info(c, thisMethod, "Setting system property java.security.krb5.conf=" + krb5Config);
            System.setProperty("java.security.krb5.conf", krb5Config);
        }

        if (otherSupportJdks) {
            loginContextEntry = SUN_JDK_KRB5_LOGIN;
            System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
            System.setProperty("java.security.krb5.realm", (realm == null) ? KDC_REALM : realm);
            System.setProperty("java.security.krb5.kdc", (kdcHostName == null) ? KDC_HOSTNAME : kdcHostName);
        }

        System.setProperty("java.security.auth.login.config", jaasLoginConfig);

        if (jaasLoginContextEntry != null) {
            loginContextEntry = jaasLoginContextEntry;
        }

        enableKrb5Debug();

        Log.info(c, thisMethod, "Use jaasLoginContextEntry:" + loginContextEntry);
        return loginContextEntry;
    }

    /**
     * Enable KRB5 debug.
     */
    private void enableKrb5Debug() {
        if (ibmJdkV8Lower) {
            System.setProperty("com.ibm.security.krb5.Krb5Debug", "all");
            System.setProperty("com.ibm.security.jgss.debug", "all");
        } else {
            System.setProperty("sun.security.krb5.debug", "true");
            System.setProperty("sun.security.jgss.debug", "true");
        }
    }
}
