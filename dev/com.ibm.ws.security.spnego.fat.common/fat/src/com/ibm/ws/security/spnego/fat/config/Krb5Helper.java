/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat.config;

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;

import componenttest.topology.impl.LibertyServer;

public class Krb5Helper {

    private final Class<?> thisClass = Krb5Helper.class;

    private static final String IBM_JDK_KRB5_LOGIN = "ibmKrb5Login";
    private static final String SUN_JDK_KRB5_LOGIN = "sunKrb5Login";
    public static final String SUN_JDK_KRB5_LOGIN_REFRESH_KRB5_CONFIG = "sunKrb5LoginRefreshKrb5Config";
    public static Oid KRB5_MECH_OID = null;
    public static Oid SPNEGO_MECH_OID = null;

    private String jaasConfFile = SPNEGOConstants.CLIENT_JAAS_CONFIG_FILE;

    /**
     * Performs a Kerberos login on the given server using the provided login configuration and user credentials.
     *
     * @param server
     * @param userName
     * @param password
     * @param krb5LoginConfig - Absolute file path of the Kerberos configuration file to be used for login. This value
     *            will be set as the value of the "java.security.krb5.conf" system property.
     * @return
     * @throws Exception
     */
    public Subject kerberosLogin(LibertyServer server, String userName, String password, String krb5LoginConfig) throws Exception {
        return kerberosLogin(server, userName, password, krb5LoginConfig, null, null, null);
    }

    /**
     * Performs a Kerberos login on the given server using the provided user credentials and KDC information.
     *
     * @param server
     * @param userName
     * @param password
     * @param realm - The Kerberos realm to be used for login. If not null, this value will be set as the value of the
     *            "java.security.krb5.realm" system property.
     * @param kdcHostName - The host name of the KDC to be used for login. If realm is not null and this value is not
     *            null, this value will be set as the value of the "java.security.krb5.kdc" system property.
     * @param jaasLoginContextEntry - JaasLoginContextEntry
     * @return
     * @throws Exception
     */
    public Subject kerberosLogin(LibertyServer server, String userName, String password, String realm, String kdcHostName) throws Exception {
        return kerberosLogin(server, userName, password, null, realm, kdcHostName, null);
    }

    /**
     * Performs a Kerberos login on the given server using the provided login configuration and user credentials.
     *
     * @param server
     * @param userName
     * @param password
     * @param krb5LoginConfig - Absolute file path of the Kerberos configuration file to be used for login. This value
     *            will be set as the value of the "java.security.krb5.conf" system property.
     * @param realm - The Kerberos realm to be used for login. If non-null, this value will be set as the value of the
     *            "java.security.krb5.realm" system property; the krb5LoginConfig value will not be used and the
     *            "java.security.krb5.conf" system property will not be set.
     * @param kdcHostName - The host name of the KDC to be used for login. If realm is not null and this value is not
     *            null, this value will be set as the value of the "java.security.krb5.kdc" system property.
     * @param jaasLoginContextEntry - JaasLoginContextEntry
     * @return
     * @throws Exception
     */
    public Subject kerberosLogin(LibertyServer server, String userName, String password, String krb5LoginConfig, String realm, String kdcHostName,
                                 String jaasLoginContextEntry) throws Exception {
        String thisMethod = "kerberosLogin";
        String loginContextEntry = setupLoginConfig(server, krb5LoginConfig, realm, kdcHostName, jaasLoginContextEntry);

        WSCallbackHandlerImpl wscbh = new WSCallbackHandlerImpl(userName, password);
        Subject subject = null;
        try {
            LoginContext lc = new LoginContext(loginContextEntry, wscbh);
            lc.login();
            subject = lc.getSubject();
        } catch (LoginException e) {
            Log.info(thisClass, thisMethod, "Unexpected exception: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
            throw e;
        }
        return subject;
    }

    /**
     * Sets various system properties in preparation for a Kerberos login on the specified server. The following system
     * properties are set:
     *
     * - java.security.krb5.realm : Set to realm if realm is not null and not empty.
     * - java.security.krb5.kdc : Set to kdcHostName if realm is not null and not empty, and if kdcHostName is not
     * null. If kdcHostName is null, a default host name of an existing KDC server is used (nc135019.tivlab.austin.ibm.com).
     * - java.security.krb5.conf : Set to krb5Config if realm is null or empty.
     * - java.security.auth.login.config : Set to the absolute path of the JAAS config file under the server root directory.
     *
     * Debug system properties:
     * - com.ibm.security.krb5.Krb5Debug: Set to "all" if the runtime Java vendor is IBM.
     * - com.ibm.security.jgss.debug: Set to "all" if the runtime Java vendor is IBM.
     * - sun.security.krb5.debug: Set to "true" if the runtime Java vendor is not IBM.
     * - sun.security.jgss.debug: Set to "true" if the runtime Java vendor is not IBM.
     *
     * @param server
     * @param krb5Config
     * @param realm
     * @param kdcHostName
     * @return jaasLoginContextEntry
     */
    public String setupLoginConfig(LibertyServer server, String krb5Config, String realm, String kdcHostName, String jaasLoginContextEntry) {
        String thisMethod = "setupLoginConfig";
        Log.info(thisClass, thisMethod, "krb5Config: " + krb5Config + " realm: " + realm + " kdcHostName: " + InitClass.getKDCHostnameMask(kdcHostName));
        String loginContextEntry = IBM_JDK_KRB5_LOGIN;
        String jaasLoginConfig = server.getServerRoot() + jaasConfFile;

        if (realm != null && !realm.isEmpty()) {
            Log.info(thisClass, thisMethod, "Setting system properties java.security.krb5.realm and java.security.krb5.kdc");
            System.setProperty("java.security.krb5.realm", realm);
            System.setProperty("java.security.krb5.kdc", (kdcHostName == null) ? InitClass.KDC_HOSTNAME : kdcHostName);
        } else if (krb5Config != null) {
            Log.info(thisClass, thisMethod, "Setting system property java.security.krb5.conf=" + krb5Config);
            System.setProperty("java.security.krb5.conf", krb5Config);
        }

        if (InitClass.OTHER_SUPPORT_JDKS) {
            loginContextEntry = SUN_JDK_KRB5_LOGIN;
            System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
            System.setProperty("java.security.krb5.realm", (realm == null) ? InitClass.KDC_REALM : realm);
            System.setProperty("java.security.krb5.kdc", (kdcHostName == null) ? InitClass.KDC_HOSTNAME : kdcHostName);
        }

        System.setProperty("java.security.auth.login.config", jaasLoginConfig);

        if (jaasLoginContextEntry != null)
            loginContextEntry = jaasLoginContextEntry;

        enableKrb5Debug();

        Log.info(thisClass, thisMethod, "Use jaasLoginContextEntry:" + loginContextEntry);
        return loginContextEntry;
    }

    private void enableKrb5Debug() {
        if (InitClass.IBM_JDK_V8_LOWER) {
            System.setProperty("com.ibm.security.krb5.Krb5Debug", "all");
            System.setProperty("com.ibm.security.jgss.debug", "all");
        } else {
            System.setProperty("sun.security.krb5.debug", "true");
            System.setProperty("sun.security.jgss.debug", "true");
        }
    }

    /**
     * Sets the JAAS configuration file to be used for JAAS login.
     *
     * @param filename - Path and filename, relative to the server root, of the JAAS configuration file to use.
     */
    public void setJaasConfFile(String filename) {
        Log.info(thisClass, "setJaasConfFile", "Setting JAAS config file to: " + filename);
        jaasConfFile = filename;
    }

    /**
     * Resets the JAAS configuration file to be used for JAAS login back to the default value.
     */
    public void resetJaasConfFile() {
        Log.info(thisClass, "resetJaasConfFile", "Resetting JAAS config file to: " + SPNEGOConstants.CLIENT_JAAS_CONFIG_FILE);
        jaasConfFile = SPNEGOConstants.CLIENT_JAAS_CONFIG_FILE;
    }

    /**
     * Creates a SPNEGO or Kerberos token base on mechOid for the provided Active Directory user using the SPN provided.
     *
     * @param subject
     * @param userName
     * @param spn
     * @param credentialDelegate
     * @return Base64 encoded SPNEGO or Kerberos token.
     * @throws Exception
     */
    public String createToken(Subject subject, final String userName, String spn, boolean credentialDelegate, Oid mechOid) throws Exception {
        return createToken(subject, userName, spn, credentialDelegate, GSSCredential.DEFAULT_LIFETIME, GSSCredential.INDEFINITE_LIFETIME,
                           GSSCredential.INDEFINITE_LIFETIME,
                           GSSContext.DEFAULT_LIFETIME, mechOid);
    }

    /**
     * Creates a SPNEGO or Kerberos token base on mechOid for the provided Active Directory user using the SPN provided.
     *
     * @param subject
     * @param userName
     * @param spn
     * @param credentialDelegate
     * @param credentialLifetime
     * @param credInitLifetime
     * @param credAcceptLifetime
     * @param contextLifetime
     * @return Base64 encoded SPNEGO or Kerberos token.
     * @throws Exception
     */
    public String createToken(Subject subject, final String userName, String spn, boolean credentialDelegate, final int credentialLifetime,
                              final int credInitLifetime,
                              final int credAcceptLifetime, int contextLifetime, Oid mechOid) throws Exception, GSSException {
        String thisMethod = "createToken";
        byte[] spnegoToken = new byte[0];
        GSSContext clientContext = null;
        try {
            final GSSManager manager = GSSManager.getInstance();

            GSSCredential clientGssCreds = (GSSCredential) Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws GSSException, Exception {
                    try {
                        Log.info(thisClass, "run", "Creating GSS name");
                        GSSName gssName = manager.createName(userName, GSSName.NT_USER_NAME, KRB5_MECH_OID);
                        Log.info(thisClass, "run", "Creating GSS credential");
                        GSSCredential gssCred = manager.createCredential(gssName.canonicalize(KRB5_MECH_OID),
                                                                         credentialLifetime,
                                                                         KRB5_MECH_OID,
                                                                         GSSCredential.INITIATE_ONLY);
                        Log.info(thisClass, "run", "Adding mechanism element to GSS credential");
                        gssCred.add(gssName, credInitLifetime, credAcceptLifetime, SPNEGO_MECH_OID, GSSCredential.INITIATE_ONLY);
                        return gssCred;
                    } catch (GSSException gsse) {
                        Log.info(thisClass, "run", "Caught GSSException: " + gsse);
                    } catch (Exception e) {
                        Log.info(thisClass, "run", "Caught exception: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
                    }
                    return null;
                }
            });

            // check for null, meaning failure creating gssCred, need to throw exception since next call will fail
            if (clientGssCreds == null) {
                throw new GSSException(13);
            }

            Log.info(thisClass, thisMethod, "Creating GSSName for SPN " + spn);
            GSSName gssServerName = manager.createName(spn, GSSName.NT_USER_NAME);

            Log.info(thisClass, thisMethod, "Creating client context");
            clientContext = manager.createContext(gssServerName.canonicalize(mechOid),
                                                  mechOid,
                                                  clientGssCreds,
                                                  contextLifetime);
            clientContext.requestLifetime(contextLifetime);
            clientContext.requestCredDeleg(credentialDelegate);
            clientContext.requestReplayDet(false);
            Log.info(thisClass, thisMethod, "Initializing security context and obtaining SPNEGO token");
            spnegoToken = clientContext.initSecContext(spnegoToken, 0, spnegoToken.length);

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.info(thisClass, thisMethod, "Unexpected exception: " + CommonTest.maskHostnameAndPassword(ex.getMessage()));
            throw ex;
        } finally {
            if (clientContext != null) {
                clientContext.dispose();
            }
        }

        String spnegoTokenStr = Base64Coder.encode(spnegoToken);

        Log.info(thisClass, thisMethod, "spnegoToken: " + spnegoTokenStr);
        return spnegoTokenStr;
    }

    public String mechOidString(Oid mechOid) {
        Oid[] spnegoOids = { SPNEGO_MECH_OID };
        if (mechOid != null && mechOid.containedIn(spnegoOids)) {
            return " SPNEGO token";
        } else {
            return " Kerberos token";
        }
    }

    static {
        final String KRB5_OID = "1.2.840.113554.1.2.2";
        final String SPNEGO_OID = "1.3.6.1.5.5.2";
        try {
            KRB5_MECH_OID = new Oid(KRB5_OID);
            SPNEGO_MECH_OID = new Oid(SPNEGO_OID);
        } catch (GSSException ex) {
            //do nothing
        }
    }
}
