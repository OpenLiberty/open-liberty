/*******************************************************************************
 * Copyright (c) 2005, 2024 IBM Corporation and others.
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

package com.ibm.ws.ssl.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.ConsoleWrapper;
import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.ws.ssl.config.SSLConfigManager;
import com.ibm.ws.ssl.config.ThreadManager;
import com.ibm.ws.ssl.config.WSKeyStore;
import com.ibm.ws.ssl.provider.AbstractJSSEProvider;
import com.ibm.wsspi.ssl.TrustManagerExtendedInfo;

/**
 * Implementation of an X509TrustManager.
 * <p>
 * This class is the TrustManager wrapper that delegates to the "real"
 * TrustManager configured for the system.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public final class WSX509TrustManager extends X509ExtendedTrustManager {
    private static final TraceComponent tc = Tr.register(WSX509TrustManager.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static final int MAX_MSG_LEN = 79;
    private static final String INDENT = "           ";

    private final TrustManager[] tm;
    private final String tsCfgAlias;
    private final String tsFile;
    private Map<String, Object> extendedInfo;
    private String peerHost;
    private final SSLConfig config;
    boolean isDoubleByteSystem = false;
    boolean isServer = true;
    boolean useCACertFile = false;
    boolean autoAccept = false;
    private final ConsoleWrapper stdin;
    private final PrintStream stdout;
    private TrustManager[] cacertsTM = null;

    private static String getSystemProperty(String propName) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(propName);
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty(propName);
            }
        });
    }

    /*
     * Constructor for unittesting
     */
    protected WSX509TrustManager(TrustManager[] tmArray, ConsoleWrapper stdin, PrintStream stdout, boolean isServer, boolean useCacerts) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "WSX509TrustManager");
        this.isServer = isServer;
        this.stdin = stdin;
        this.stdout = stdout;
        this.config = null;
        this.tsCfgAlias = null;
        this.tsFile = null;
        this.tm = tmArray.clone();
        this.autoAccept = false;
        this.useCACertFile = useCacerts;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "WSX509TrustManager");
    }

    /**
     * Constructor.
     *
     * @param tmArray
     * @param connectionInfo
     * @param sslConfig
     * @param trustStore
     * @param trustStoreFilename
     * @param trustStorePassword
     */
    public WSX509TrustManager(TrustManager[] tmArray, Map<String, Object> connectionInfo, SSLConfig sslConfig, String trustStoreAlias, String trustStoreFilename) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "WSX509TrustManager", new Object[] { connectionInfo, trustStoreFilename });
        tm = tmArray.clone();
        tsFile = trustStoreFilename;
        tsCfgAlias = trustStoreAlias;
        config = sslConfig;
        extendedInfo = connectionInfo;
        isServer = SSLConfigManager.getInstance().isServerProcess();
        stdin = new ConsoleWrapper(System.console(), System.err);
        stdout = System.out;
        autoAccept = getAutoAccept();

        String propVal = (String) sslConfig.get(Constants.SSLPROP_USE_DEFAULTCERTS);
        if (propVal != null && propVal.equalsIgnoreCase("true"))
            useCACertFile = true;

        if (extendedInfo != null) {
            peerHost = (String) extendedInfo.get(Constants.CONNECTION_INFO_REMOTE_HOST);

            for (TrustManager tmgr : tmArray)
                if (tmgr instanceof TrustManagerExtendedInfo) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Adding extended info to TrustManager " + tmgr.getClass().getName());
                    ((TrustManagerExtendedInfo) tmgr).setExtendedInfo(extendedInfo);
                    ((TrustManagerExtendedInfo) tmgr).setSSLConfig(sslConfig);
                }
        }

        if (useCACertFile) {
            try {
                cacertsTM = AbstractJSSEProvider.getDefaultTrustManager();
            } catch (Exception e) {
                String sslAlias = sslConfig.getProperty(Constants.SSLPROP_ALIAS);
                Tr.warning(tc, "ssl.cacerts.error.CWPKI0825W", new Object[] { sslAlias, e.getMessage() });
                useCACertFile = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "WSX509TrustManager");
    }

    /**
     * @return
     */
    private boolean getAutoAccept() {
        boolean acceptCert = false;

        String acceptProperty = getSystemProperty("autoAcceptSignerCertificate");
        if (acceptProperty != null) {
            acceptCert = Boolean.valueOf(acceptProperty);
        }

        return acceptCert;
    }

    /*
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.
     * X509Certificate[], java.lang.String)
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "checkClientTrusted");

        extendedInfo = JSSEHelper.getInstance().getInboundConnectionInfo();
        if (extendedInfo != null)
            peerHost = (String) extendedInfo.get(Constants.CONNECTION_INFO_REMOTE_HOST);

        // if we do not have host and port just go ahead and default it to unknown
        // to avoid null's
        if (peerHost == null || peerHost.equals("")) {
            peerHost = "unknown";
        }

        try {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                for (int j = 0; j < chain.length; j++) {
                    Tr.debug(tc, "chain[" + j + "]: " + chain[j].getSubjectDN());
                }
            }

            for (int i = 0; i < tm.length; i++) {
                if (tm[i] != null && tm[i] instanceof X509TrustManager) {
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Delegating to X509TrustManager implementation: " + tm[i].getClass().getName());
                        ((X509TrustManager) tm[i]).checkClientTrusted(chain, authType);

                    } catch (Exception e) {
                        processClientTrustError(chain, authType, e);
                    }
                }
            }

        } catch (Throwable t) {
            // here we want to try to catch any runtime exceptions that might occur,
            // print a message and rethrow
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception in checkClientTrusted.", new Object[] { t });
            FFDCFilter.processException(t, getClass().getName(), "checkClientTrusted", this, new Object[] { chain, authType });

            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof CertificateException) {
                throw (CertificateException) t;
            }
            throw new CertificateException(t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkClientTrusted");
    }

    /*
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.
     * X509Certificate[], java.lang.String)
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "checkServerTrusted");

        Map<String, Object> currentConnectionInfo = ThreadManager.getInstance().getOutboundConnectionInfoInternal();
        if (currentConnectionInfo != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "original peerHost: " + peerHost);
                Tr.debug(tc, "currentConnectionInfo: " + currentConnectionInfo);
            }
            peerHost = (String) currentConnectionInfo.get(Constants.CONNECTION_INFO_REMOTE_HOST);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "current peerHost: " + peerHost);
        } else {
            currentConnectionInfo = extendedInfo;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "currentConnectionInfo from extendedInfo: " + currentConnectionInfo);
        }

        extendedInfo = JSSEHelper.getInstance().getOutboundConnectionInfo();
        if (extendedInfo != null)
            peerHost = (String) extendedInfo.get(Constants.CONNECTION_INFO_REMOTE_HOST);

        // if we do not have host and port just go ahead and default it to unknown
        // to avoid null's
        if (peerHost == null || 0 == peerHost.length()) {
            peerHost = "unknown";
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Target host: " + peerHost);
            for (int j = 0; j < chain.length; j++) {
                Tr.debug(tc, "Certificate information:");
                Tr.debug(tc, "  Subject DN: " + chain[j].getSubjectDN());
                Tr.debug(tc, "  Issuer DN: " + chain[j].getIssuerDN());
                Tr.debug(tc, "  Serial number: " + chain[j].getSerialNumber());
            }
        }

        for (int i = 0; i < tm.length; i++) {
            if (tm[i] != null && tm[i] instanceof X509TrustManager) {
                // skip the default trust manager if configured to do so.
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Delegating to X509TrustManager implementation: " + tm[i].getClass().getName());
                    ((X509TrustManager) tm[i]).checkServerTrusted(chain, authType);
                } catch (CertificateException excpt) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Certificate Exception occurred: " + excpt.getMessage());
                    boolean certPathError = false;

                    // if the reason for the CertificateException is due to
                    // an expired date, don't accept it.
                    boolean dateValid = checkIfExpiredBeforeOrAfter(chain);
                    if (!dateValid) {
                        throw excpt;
                    }
                    if (excpt.getCause() != null && excpt.getCause() instanceof java.security.cert.CertPathValidatorException) {
                        certPathError = true;
                    }

                    try {

                        if (certPathError) {
                            if (useCACertFile) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "Establishing trust in the server with the configured truststore did not succeed, trying the cacerts trustmanager");
                                callCertFileTrustManager(chain, authType, null, 0, excpt);
                            } else {
                                processCertPathException(chain, authType, excpt, null, 0, null);
                            }
                        } else {
                            // Hostname verification error
                            Tr.error(tc, "ssl.client.handshake.error.CWPKI0825E", new Object[] { excpt });
                            throw excpt;
                        }
                    } catch (Exception ex) {
                        throw new CertificateException(ex.getMessage());
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Server is trusted by all X509TrustManagers.");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkServerTrusted");
    }

    /*
     * Method to prompt the user, asking if they want to accept the signer certificate. Then reads the
     * user's answer. If the user says yes we accept the certificate and add it to the keystore file.
     * If the user says no, exception is thrown.
     */
    protected boolean userAcceptedPrompt(X509Certificate[] chain) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "userAcceptedPrompt");
        boolean accepted = false;
        try {
            stdout.println("");
            stdout.println(TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.CWPKI0100I", "*** SSL SIGNER EXCHANGE PROMPT ***"));
            stdout.println(TraceNLSHelper.getInstance().getFormattedMessage("ssl.trustmanager.signer.prompt.CWPKI0101I",
                                                                            new Object[] { tsFile },
                                                                            "SSL signer from target host is not found in trust store "
                                                                                                     + tsFile
                                                                                                     + ".\n\nHere's the signer information (verify the digest value matches what is displayed at the server):"));
            for (int j = 0; j < chain.length; j++) {
                stdout.println("");
                String shaDigest = KeyStoreManager.getInstance().generateDigest(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256, chain[j]);
                stdout.println(TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.CWPKI0102I", "  Subject DN:    ")
                               + chain[j].getSubjectDN());
                stdout.println(TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.CWPKI0103I", "  Issuer DN:     ")
                               + chain[j].getIssuerDN());
                stdout.println(TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.CWPKI0104I", "  Serial number: ")
                               + chain[j].getSerialNumber());
                stdout.println(TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.CWPKI0109I", "  Expires: ") + chain[j].getNotAfter());
                stdout.println(TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.CWPKI0105I", "  SHA-256 digest:  ") + shaDigest);
                stdout.println("");
            }

            // ask the user if they want the certificate added to the truststore
            String prompt = TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.CWPKI0107I", "Add signer to the trust store now? (y/n) ");

            String answer = stdin.readText(prompt);
            if (answer != null)
                answer = answer.trim().toLowerCase();

            if (isYes(answer)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "User accepted the certificate, certificate added to the truststore.");
                accepted = true;
            } else {
                // don't trust the certificate
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "User did not accept the certificate so do not store it to the truststore.");
            }

        } catch (Exception ex) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Received the following while prompting user.", new Object[] { ex });
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "userAcceptedPrompt", accepted);
        return accepted;
    }

    /*
     * Method to check users answer from the prompt. Get the translated 'y' or 'yes' answer and compare it to
     * what the user entered.
     */
    boolean isYes(String read) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isYes", read);

        String expectedYesShortResponse = TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.answer.yes", "y");
        String expectedYesFullResponse = TraceNLSHelper.getInstance().getString("ssl.trustmanager.signer.prompt.answer.full.yes", "yes");
        boolean isItYes = ((expectedYesShortResponse != null && expectedYesShortResponse.length() > 0 && expectedYesShortResponse.equalsIgnoreCase(read)) || // return true if translated y matches
                           (expectedYesFullResponse != null && expectedYesFullResponse.length() > 0 && expectedYesFullResponse.equalsIgnoreCase(read))); // return true if translated yes matches

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isYes", isItYes);
        return isItYes;
    }

    /*
     * Method adds the certificate entry to the truststore.
     */
    void setCertificateToTruststore(X509Certificate[] chain) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setCertificateToTruststore");

        try {

            WSKeyStore wsks = KeyStoreManager.getInstance().getKeyStore(tsCfgAlias);
            if (wsks == null)
                throw (new Exception("Keystore " + tsCfgAlias + " does not exist in the configuration."));
            if (wsks.getReadOnly()) {
                issueMessage("ssl.keystore.readonly.CWPKI0810I",
                             new Object[] { tsCfgAlias },
                             "The " + tsCfgAlias + " keystore is read only and the certificate will not be written to the keystore file.  Trust will be accepted only for this connection.");
            } else {
                for (int j = 0; j < chain.length; j++) {
                    String alias = chain[j].getSubjectDN().getName();
                    alias = alias.trim();

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Adding alias \"" + alias + "\" to truststore \"" + tsFile + "\".");
                    wsks.setCertificateEntry(alias, chain[chain.length - 1]);
                    String shaDigest = KeyStoreManager.getInstance().generateDigest("SHA-1", chain[chain.length - 1]);

                    issueMessage("ssl.signer.add.to.local.truststore.CWPKI0308I", new Object[] { alias, tsFile, shaDigest }, "CWPKI0308I: Adding signer alias \"" + alias
                                                                                                                             + "\" to local keystore \"" + tsFile
                                                                                                                             + "\" with the following SHA digest: " + shaDigest);

                    //Certificate is set on the truststore now clear the caches, if the file monitor is on let it handle the change.
                    String trigger = wsks.getTrigger();
                    if (!trigger.equalsIgnoreCase("disabled"))
                        clearSSLCachesAndResetDefault();
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception while trying to write certificate to the truststore. Exception is " + e.getMessage());
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setCertificateToTruststore");
    }

    private void clearSSLCachesAndResetDefault() {
        Collection<File> fileCol = new HashSet<File>();
        File f = new File(tsFile);
        fileCol.add(f);
        try {
            com.ibm.ws.ssl.provider.AbstractJSSEProvider.clearSSLContextCache();
            com.ibm.ws.ssl.config.KeyStoreManager.getInstance().clearJavaKeyStoresFromKeyStoreMap();
            com.ibm.ws.ssl.config.SSLConfigManager.getInstance().resetDefaultSSLContextIfNeeded(fileCol);
            Tr.audit(tc, "ssl.keystore.modified.CWPKI0811I", fileCol.toArray());
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while trying to reload keystore file, exception is: " + e.getMessage());
            }
        }
    }

    protected boolean checkIfExpiredBeforeOrAfter(X509Certificate[] chain) {
        if (chain != null && chain[0] != null) {
            long currentTime = System.currentTimeMillis();
            long notBefore = chain[0].getNotBefore().getTime();
            long notAfter = chain[0].getNotAfter().getTime();

            if (notBefore > currentTime) {
                // print message current time is less than certificate start date.
                Tr.error(tc, "ssl.certificate.before.date.invalid.CWPKI0311E", new Object[] { chain[0].getSubjectDN(), new Date(notBefore) });
                return false;
            } else if (notAfter < currentTime) {
                // print message current time is after than certificate end date.
                Tr.error(tc, "ssl.certificate.end.date.invalid.CWPKI0312E", new Object[] { chain[0].getSubjectDN(), new Date(notAfter) });
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    private void printClientHandshakeError(SSLConfig cfg, String file, Exception e, X509Certificate[] chain, String host, int port) {
        String extendedMessage = e.getMessage();
        String subjectDN = "unknown";
        if (chain[0] != null)
            subjectDN = chain[0].getSubjectDN().toString();
        String alias = getProperty(Constants.SSLPROP_ALIAS, cfg, SSLConfigManager.getInstance().isServerProcess());
        if (host != null && port > 0) {
            String hostPort = host + ":" + port;
            Tr.error(tc, "ssl.client.handshake.error.CWPKI0823E", new Object[] { subjectDN, hostPort, file, alias, extendedMessage });
        } else {
            Tr.error(tc, "ssl.client.handshake.error.CWPKI0022E", new Object[] { subjectDN, file, alias, extendedMessage });
        }

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getAcceptedIssuers");
        X509Certificate[] allIssuers = null;
        List<X509Certificate> allIssuersList = new ArrayList<X509Certificate>();

        // check to see if we need to get the list from cacerts
        if (useCACertFile) {
            X509Certificate[] caIssuers = null;
            caIssuers = ((X509TrustManager) cacertsTM[0]).getAcceptedIssuers();
            if (caIssuers != null)
                allIssuersList.addAll(Arrays.asList(caIssuers));
        }

        if (tm[0] instanceof X509TrustManager) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Delegating to X509TrustManager Implementation: " + tm[0].getClass().getName());
            allIssuers = ((X509TrustManager) tm[0]).getAcceptedIssuers();

            if (allIssuers != null) {
                for (int j = 0; j < allIssuers.length; j++) {
                    if (!allIssuersList.contains(allIssuers[j]))
                        allIssuersList.add(allIssuers[j]);
                }
            }
        }

        X509Certificate[] rc = {};
        if (allIssuersList.size() > 0) {
            rc = allIssuersList.toArray(new X509Certificate[allIssuersList.size()]);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getAcceptedIssuers", rc);
        return rc;
    }

    protected void issueMessage(String key, Object[] args, String defaultMsg) {
        String msg = TraceNLSHelper.getInstance().getFormattedMessage(key, args, defaultMsg);
        printMessage(msg);
    }

    protected void printMessage(String msg) {
        // If the msg contains a character that would probably take up extra
        // space
        // on the console, half the maxLength
        int maxLength = MAX_MSG_LEN;

        if (isDoubleByteSystem(msg)) {
            maxLength /= 2;
        }

        printMessage(msg, maxLength, false);
    }

    private boolean isDoubleByteSystem(String sampleStr) {
        // We test by taking the sampleStr and writing it in UTF-8
        // form into a DataOutputStream. Since we know that DBCS
        // characters take 3 bytes and basic latin characters only
        // take 1 byte, we can determine if the system writes
        // double-byte.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeUTF(sampleStr);
            dos.flush();
        } catch (IOException exc) {
            return false;
        } finally {
            try {
                dos.close();
            } catch (IOException exc) {
                // do nothing
            }
        }

        // The check is if the size of the byte array is greater than
        // the length of the string + 10%
        byte[] bytes = baos.toByteArray();
        if (bytes.length > sampleStr.length() + (sampleStr.length() * .1)) {
            isDoubleByteSystem = true;
        } else {
            isDoubleByteSystem = false;
        }

        return isDoubleByteSystem;
    }

    private void printMessage(String msg, int maxLength, boolean indent) {
        int maxLocalLength = maxLength;
        if (indent) {
            System.out.print(INDENT);
            maxLocalLength -= INDENT.length();
        }

        if (msg.length() <= maxLocalLength) {
            System.out.println(msg);
        } else {
            int i = msg.lastIndexOf(' ', maxLocalLength);
            if (i == -1) {
                i = msg.indexOf(' ');
                if (i == -1) {
                    System.out.println(msg);
                    return;
                }
            }

            printMessage(msg.substring(0, i), maxLength, false);
            printMessage(msg.substring(i + 1), maxLength, true);
        }
    }

    // returns the property based on system prop, global prop, then properties
    // object prop
    private String getProperty(String propertyName, Properties prop, boolean processIsServer) {
        String value = null;

        if (prop != null) {
            // if client process, get system prop first, global prop second,
            // then sslconfig or keystore prop third (for override compatibility)
            if (!processIsServer) {
                value = getSystemProperty(propertyName);

                if (value == null) {
                    value = SSLConfigManager.getInstance().getGlobalProperty(propertyName);
                }
            }

            if (value == null) {
                value = prop.getProperty(propertyName);
            }
        } else {
            value = getSystemProperty(propertyName);

            if (value == null) {
                value = SSLConfigManager.getInstance().getGlobalProperty(propertyName);
            }
        }

        return value;
    }

    /*
     * Connection-sensitive verification.
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType,
                                   Socket socket) throws CertificateException {
        Tr.entry(tc, "checkClientTrusted", new Object[] { chain, authType, socket });

        try {
            for (int i = 0; i < tm.length; i++) {
                if (tm[i] != null && tm[i] instanceof X509TrustManager) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Delegating to X509TrustManager: " + tm[i].getClass().getName());

                    ((X509ExtendedTrustManager) tm[i]).checkClientTrusted(chain, authType, socket);
                }
            }
        } catch (CertificateException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Certificate Exception occurred: " + e.getMessage());

            processClientTrustError(chain, authType, e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkClientTrusted");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType,
                                   SSLEngine engine) throws CertificateException {
        Tr.entry(tc, "checkClientTrusted", new Object[] { chain, authType, engine });

        try {
            for (int i = 0; i < tm.length; i++) {
                if (tm[i] != null && tm[i] instanceof X509TrustManager) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Delegating to X509TrustManager: " + tm[i].getClass().getName());

                    ((X509ExtendedTrustManager) tm[i]).checkClientTrusted(chain, authType, engine);
                }
            }
        } catch (CertificateException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Certificate Exception from configured trustmanager occurred: " + e.getMessage());

            processClientTrustError(chain, authType, e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkClientTrusted");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType,
                                   Socket socket) throws CertificateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "checkServerTrusted", new Object[] { chain, authType, socket });

        String peerHost = null;
        int peerPort = 0;

        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            SSLSession session = sslSocket.getHandshakeSession();
            peerHost = session.getPeerHost();
            peerPort = session.getPeerPort();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Target host: " + peerHost);
            for (int j = 0; j < chain.length; j++) {
                Tr.debug(tc, "Certificate information:");
                Tr.debug(tc, "  Subject DN: " + chain[j].getSubjectDN());
                Tr.debug(tc, "  Issuer DN: " + chain[j].getIssuerDN());
                Tr.debug(tc, "  Serial number: " + chain[j].getSerialNumber());
            }
        }

        try {
            for (int i = 0; i < tm.length; i++) {
                if (tm[i] != null && tm[i] instanceof X509TrustManager) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Delegating to X509TrustManager implementation: " + tm[i].getClass().getName());

                    ((X509ExtendedTrustManager) tm[i]).checkServerTrusted(chain, authType, socket);
                }
            }
        } catch (CertificateException excpt) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Certificate Exception occurred: " + excpt.getMessage());

            processServerTrustError(chain, authType, peerHost, peerPort, excpt);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Server is trusted by all X509ExtendedTrustManager.");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkServerTrusted");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType,
                                   SSLEngine engine) throws CertificateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "checkServerTrusted", new Object[] { chain, authType, engine });

        String peerHost = null;
        int peerPort = 0;

        if (engine != null) {
            SSLSession session = engine.getHandshakeSession();
            peerHost = session.getPeerHost();
            peerPort = session.getPeerPort();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Target host: " + peerHost);
            for (int j = 0; j < chain.length; j++) {
                Tr.debug(tc, "Certificate information:");
                Tr.debug(tc, "  Subject DN: " + chain[j].getSubjectDN());
                Tr.debug(tc, "  Issuer DN: " + chain[j].getIssuerDN());
                Tr.debug(tc, "  Serial number: " + chain[j].getSerialNumber());
            }
        }

        try {
            for (int i = 0; i < tm.length; i++) {
                if (tm[i] != null && tm[i] instanceof X509TrustManager) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Delegating to X509TrustManager: " + tm[i].getClass().getName());

                    ((X509ExtendedTrustManager) tm[i]).checkServerTrusted(chain, authType, engine);
                }
            }
        } catch (CertificateException excpt) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Certificate Exception occurred: " + excpt.getMessage());
            processServerTrustError(chain, authType, peerHost, peerPort, excpt);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Server is trusted by all X509ExtendedTrustManager.");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkServerTrusted");
    }

    /**
     * @param excpt
     * @return
     */
    private boolean isCertPathError(CertificateException excpt) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isCertPathError", new Object[] { excpt });

        if (excpt.getCause() != null && (excpt.getCause() instanceof java.security.cert.CertPathValidatorException)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "isCertPathError cause is CertPathValidatorException " + true);
            return true;
        }

        if (excpt.getMessage().contains("SunCertPathBuilderException") || excpt.getMessage().contains("CertPathBuilderException")) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "isCertPathError, SunCertPathBuildException or CertPathBuilderException " + true);
            return true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isCertPathError " + false);
        return false;
    }

    /**
     * Called by checkClientTrusted methods when using the cacerts trustmanager.
     *
     */
    private void callCertFileTrustManagerClient(X509Certificate[] chain, String authType, Exception configTrustEx) throws CertificateException {

        try {
            ((X509TrustManager) cacertsTM[0]).checkClientTrusted(chain, authType);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Certificate Exception occurred while checking client trust with cacerts: " + e.getMessage());
            processClientException(chain, authType, configTrustEx, e);
        }
    }

    private void processClientException(X509Certificate[] chain, String authType, Exception configTrustEx, Exception caCertEx) throws CertificateException {

        Exception excpt = configTrustEx;
        if (excpt.getClass().toString().startsWith("class com.ibm.jsse2")) {
            if (excpt.getCause() != null)
                excpt = (Exception) excpt.getCause();
        }

        FFDCFilter.processException(excpt, getClass().getName(), "checkClientTrusted", this, new Object[] { chain, authType });

        printClientHandshakeError(config, tsFile, configTrustEx, chain, null, 0);
        if (useCACertFile) {
            String extendedMessage = caCertEx.getMessage();
            Tr.error(tc, "ssl.client.handshake.cacerts.error.CWPKI0828E", new Object[] { extendedMessage });
        }

        // Wrap exception in CertificateException if not a
        // CertificateException already
        if (excpt instanceof CertificateException) {
            throw (CertificateException) excpt;
        }

        throw new CertificateException(excpt.getMessage());

    }

    /**
     * called by the checkServerTrusted methods when using the cacerts trustmanager
     *
     */
    private void callCertFileTrustManager(X509Certificate[] chain, String authType, String host, int port, Exception configTrustEx) throws Exception {
        try {
            ((X509TrustManager) cacertsTM[0]).checkServerTrusted(chain, authType);
        } catch (Exception ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Certificate Exception while checking trust on cacerts file: " + ex.getMessage());
            processCertPathException(chain, authType, configTrustEx, host, port, ex);
        }
    }

    private void processCertPathException(X509Certificate[] chain, String authType, Exception excpt, String host, int port, Exception caCertEx) throws Exception {
        // If this is a client process then we may prompt the user to accept the signer certificate
        // this should only ever be called from the checkServerTrusted path
        if (!isServer) {
            if (!autoAccept) {
                // prompt user
                if (userAcceptedPrompt(chain)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "prompt user - adding certificate to the truststore.");
                    setCertificateToTruststore(chain);
                    return;
                }

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "autoacceptsigner - adding certificate to the truststore.");
                setCertificateToTruststore(chain);
                return;
            }
        }

        // IBM JDK will throw the exception in obfuscated code, get the cause
        Exception e = excpt;
        if (e.getClass().toString().startsWith("class com.ibm.jsse2")) {
            if (excpt.getCause() != null)
                e = (Exception) excpt.getCause();
        }

        // This the server print a message and rethrow the exception
        FFDCFilter.processException(e, getClass().getName(), "checkServerTrusted", this, new Object[] { chain, authType });
        printClientHandshakeError(config, tsFile, e, chain, host, port);
        if (useCACertFile) {
            String extendedMessage = caCertEx.getMessage();
            Tr.error(tc, "ssl.client.handshake.cacerts.error.CWPKI0828E", new Object[] { extendedMessage });
        }
        throw e;

    }

    private void processClientTrustError(X509Certificate[] chain, String authType, Exception configTrustEx) throws CertificateException {
        if (useCACertFile) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Establishing trust in the client with the configured truststore did not succeed, trying the JSSE default trustmanager");
            callCertFileTrustManagerClient(chain, authType, configTrustEx);
        } else {

            processClientException(chain, authType, configTrustEx, null);
        }
    }

    private void processServerTrustError(X509Certificate[] chain, String authType, String peerHost, int peerPort, CertificateException ex) throws CertificateException {

        boolean dateValid = checkIfExpiredBeforeOrAfter(chain);
        if (!dateValid) {
            throw ex;
        }

        try {
            if (isCertPathError(ex)) {
                if (useCACertFile) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Establishing trust in the server with the configured truststore did not succeed, trying the cacerts trustmanager");
                    callCertFileTrustManager(chain, authType, peerHost, peerPort, ex);
                } else {
                    processCertPathException(chain, authType, ex, peerHost, peerPort, null);
                }
            } else {
                // Hostname verification error
                String extendedMessage = ex.getMessage();
                String sanInfo = getSANInfoForHostnameVerificationError(peerHost, chain);
                Tr.error(tc, "ssl.client.handshake.error.CWPKI0824E", new Object[] { peerHost, sanInfo, extendedMessage });
                throw ex;
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Certificate Exception occurred while processing exception: " + e.getMessage());

            throw new CertificateException(e.getMessage());
        }
    }

    private String getSANInfoForHostnameVerificationError(String peerHost, X509Certificate[] chain) {
        int GENERAL_NAME_DNSNAME = 2;
        int GENERAL_NAME_IPADDRESS = 7;
        X509Certificate certificate = chain[0];
        boolean doesDnsNameExist = false;
        ArrayList<String> sanInfoList = new ArrayList<>();
        try {
            Collection<List<?>> subjectAltNames = certificate.getSubjectAlternativeNames();
            if (subjectAltNames != null) {
                for (List<?> sanEntry : subjectAltNames) {
                    Integer sanType = (Integer) sanEntry.get(0);
                    if (sanType == GENERAL_NAME_DNSNAME) {
                        sanInfoList.add("dnsName:" + sanEntry.get(1));
                        doesDnsNameExist = true;
                    }
                    if (sanType == GENERAL_NAME_IPADDRESS) {
                        sanInfoList.add("ipAddress:" + sanEntry.get(1));
                    }
                }
            }
        } catch (CertificateParsingException e) {
            // SAN cannot be decoded
        }
        String sanInfo = String.join(", ", sanInfoList);
        String output;
        if (Character.isDigit(peerHost.charAt(0)) || doesDnsNameExist) {
            output = "Subject Alternative Name [" + sanInfo + "]";
        } else {
            output = "subjectDN [" + certificate.getSubjectDN() + "]";
        }

        return output;
    }
}
