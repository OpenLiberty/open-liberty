/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.config.ssl;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.Integrity;
import org.omg.CSIIOP.NoProtection;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfigurationNotAvailableException;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;

/**
 * Helper class to be used from the Yoko SocketFactory to create
 * the SSLServerSocketFactory and SSLSocketFactory instances.
 */
public class SSLConfig {
    private static final TraceComponent tc = Tr.register(SSLConfig.class);

    private static final OptionsKey NO_PROTECTION = new OptionsKey(NoProtection.value, NoProtection.value);
    private final JSSEHelper jsseHelper;

    public SSLConfig(JSSEHelper jsseHelper) {
        this.jsseHelper = jsseHelper;
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private SSLContext getSslContext(final String sslConfigName) throws SSLConfigurationNotAvailableException, SSLException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<SSLContext>() {
                @Override
                public SSLContext run() throws SSLConfigurationNotAvailableException, SSLException {
                    return jsseHelper.getSSLContext(sslConfigName, null, null, false);
                }
            });
        } catch (PrivilegedActionException pae) {
            assert SSLException.class.isAssignableFrom(SSLConfigurationNotAvailableException.class);
            throw (SSLException) pae.getCause();
        }
    }

    public SSLServerSocketFactory createSSLServerFactory(String sslConfigName) throws SSLConfigurationNotAvailableException, SSLException {
        return getSslContext(sslConfigName).getServerSocketFactory();
    }

    public SSLSocketFactory createSSLFactory(String sslConfigName) throws SSLConfigurationNotAvailableException, SSLException {
        return getSslContext(sslConfigName).getSocketFactory();
    }

    public String[] getCipherSuites(String sslAliasName, String[] candidateCipherSuites) throws SSLException {
        Properties props = jsseHelper.getProperties(sslAliasName);
        return getCipherSuites(sslAliasName, candidateCipherSuites, props);
    }

    public String[] getCipherSuites(String sslAliasName, String[] candidateCipherSuites, Properties props) throws SSLException {
        String enabledCipherString = props.getProperty(Constants.SSLPROP_ENABLED_CIPHERS);
        if (enabledCipherString != null) {
            String[] requested = enabledCipherString.split("[,\\s]+");
            OptionsKey options = getAssociationOptions(sslAliasName, props);
            return filter(candidateCipherSuites, requested, options);
        } else {
            String securityLevelString = props.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
            return Constants.adjustSupportedCiphersToSecurityLevel(candidateCipherSuites, securityLevelString);
        }
    }

    public String[] getSSLProtocol(Properties props) throws SSLException {
        String protocol = props.getProperty(Constants.SSLPROP_PROTOCOL);

        // protocol(s) need to be in an array
        String[] protocols = protocol.split(",");

        // we only want to set the protocol on the socket if it a specific protocol name
        // don't set to TLS or SSL
        if (protocols.length == 1) {
            if (protocols[0].equals(Constants.PROTOCOL_TLS) || protocols[0].equals(Constants.PROTOCOL_SSL)) {
                protocols = null;
            }
        }

        return protocols;
    }

    public boolean getEnforceCipherOrder(String sslAliasName) throws SSLException {
        Properties props = jsseHelper.getProperties(sslAliasName);
        String enforceCipherOrder = props.getProperty(Constants.SSLPROP_ENFORCE_CIPHER_ORDER);

        if (enforceCipherOrder != null)
            return Boolean.valueOf(enforceCipherOrder);

        return false;
    }

    /**
     * This method will warn if any requested cipher suites appear to not match the options
     *
     * @param candidateCipherSuites locally supported cipher suites
     * @param requested             cipher suites explicitly configured
     * @param options               association options configured
     * @return intersection of candidates and requested
     */
    private String[] filter(String[] candidateCipherSuites, String[] requested, OptionsKey options) {
        List<String> candidates = Arrays.asList(candidateCipherSuites);
        EnumSet<Options> supports = toOptions(options.supports, true);
        EnumSet<Options> requires = toOptions(options.requires, false);
        List<String> result = new ArrayList<String>(requested.length);
        for (String choice : requested) {
            //Issue a warning/debug message only
            matches(supports, requires, choice);
            if (candidates.contains(choice)) {
                result.add(choice);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public OptionsKey getAssociationOptions(final String sslAliasName) throws SSLException {
        if (sslAliasName == null) {
            return NO_PROTECTION;
        }

        try {
            Properties props = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws SSLException {
                    return jsseHelper.getProperties(sslAliasName);
                }
            });
            return getAssociationOptions(sslAliasName, props);
        } catch (PrivilegedActionException pae) {
            throw (SSLException) pae.getCause();
        }
    }

    OptionsKey getAssociationOptions(String sslAliasName, Properties props) throws SSLException {
        if (props == null) {
            Tr.warning(tc, "CSIv2_SSLCONFIG_DOES_NOT_EXISTS", sslAliasName);
            throw new SSLException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                                "CSIv2_SSLCONFIG_DOES_NOT_EXISTS",
                                                                new Object[] { sslAliasName },
                                                                "CWWKS9591W: The {0} SSL configuration does not exist.  This could be due to a missing SSL element or an invalid reference to a keystore or truststore element in the configuration."));
        }
        //TODO cf 167870 consider explicitly configuring options.
        String clientAuthRequiredString = props.getProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION);
        boolean isClientAuthRequired = "true".equalsIgnoreCase(clientAuthRequiredString);
        short clientAuthRequired = (isClientAuthRequired) ? EstablishTrustInClient.value : 0;
        String clientAuthSupportedString = props.getProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED);
        short clientAuthSupported = ("true".equalsIgnoreCase(clientAuthSupportedString) || isClientAuthRequired) ? EstablishTrustInClient.value : 0;
        String securityLevelString = props.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
        if (Constants.SECURITY_LEVEL_LOW.equals(securityLevelString)) {
            return new OptionsKey((short) (Integrity.value | EstablishTrustInTarget.value | clientAuthSupported), (short) (Integrity.value | clientAuthRequired));
        }
        //other choices are null (default to HIGH), HIGH, MEDIUM, and CUSTOM which we will treat as HIGH
        //n.b. MEDIUM and HIGH only differ in cipher strength, not association options.
        return new OptionsKey((short) (Integrity.value | Confidentiality.value | EstablishTrustInTarget.value
                                       | clientAuthSupported), (short) (Integrity.value | Confidentiality.value | clientAuthRequired));
    }

    //static final Pattern p = Pattern.compile("(?:(SSL)|(TLS))_([A-Z0-9]*)(_anon)?(_[a-zA-Z0-9]*)??(_EXPORT)?_WITH_([A-Z0-9]*)(?:_(\\d*))?([_a-zA-Z0-9]*)?_(?:(?:(SHA)(\\d*))|(MD5))");
    //Made "WITH" as optional in the above pattern to accommodate ciphers without the substring "WITH" in their cipher-names
    static final Pattern p = Pattern.compile("(?:(SSL)|(TLS))_([A-Z0-9]*)?(_anon)?(_[a-zA-Z0-9]*)??(_EXPORT)?(_WITH_)?(AES|RC4|DES40|3DES|NULL)(?:_(\\d*))?([_a-zA-Z0-9]*)?_(?:(?:(SHA)(\\d*))|(MD5))");
    //[1 null, 2 TLS, 3 ECDHE, 4 null, 5 _ECDSA, 6 null, 8 AES, 9 128, 10 _CBC, 11 SHA, 12 256, 13 null]
    
    private static final int SSL_INDEX = 1;
    private static final int TLS_INDEX = 2;
    private static final int KEY_NEGOTIATION_PROTOCOL_INDEX = 3;
    private static final int KEY_NEGOTIATION_PROTOCOL_ANON_INDEX = 4;
    private static final int KEY_NEGOTIATION_PROTOCOL_OTHER_INDEX = 5;
    private static final int KEY_NEGOTIATION_PROTOCOL_EXPORT_INDEX = 6;
    private static final int ENCRYPTION_ALGORITHM_INDEX = 8;
    private static final int ENCRYPTION_ALGORITHM_KEY_LENGTH_INDEX = 9;
    private static final int ENCRYPTION_ALGORITHM_OTHER_INDEX = 10;
    private static final int SHA_ALGORITHM_INDEX = 11;
    private static final int SHA_KEY_LENGTH_INDEX = 12;
    private static final int MD5_ALGORITHM_INDEX = 13;

    private static final int MINIMUM_STRONG_KEY_LENGTH = 128;

    public enum Options {
        integrity, confidentiality, establishTrustInTarget, strong, noexport, tls
    }

    /**
     * @param flags
     * @return
     */
    private EnumSet<Options> toOptions(short flags, boolean supports) {
        //TODO strong from MEDIUM/HIGH setting?
        EnumSet<Options> options = supports ? EnumSet.of(Options.noexport, Options.tls) : EnumSet.noneOf(Options.class);
        if ((flags & Integrity.value) == Integrity.value)
            options.add(Options.integrity);
        if ((flags & Confidentiality.value) == Confidentiality.value)
            options.add(Options.confidentiality);
        if ((flags & EstablishTrustInTarget.value) == EstablishTrustInTarget.value)
            options.add(Options.establishTrustInTarget);
        return options;
    }

    public static EnumSet<Options> getOptions(String cipherSuiteName) {
        EnumSet<Options> result = EnumSet.noneOf(Options.class);
        Matcher m = p.matcher(cipherSuiteName);
        if (m.matches()) {
            result.add(Options.integrity);
            if (m.group(TLS_INDEX) != null)
                result.add(Options.tls);
            if (m.group(KEY_NEGOTIATION_PROTOCOL_ANON_INDEX) == null)
                result.add(Options.establishTrustInTarget);
            if (m.group(KEY_NEGOTIATION_PROTOCOL_EXPORT_INDEX) == null)
                result.add(Options.noexport);
            if (!m.group(ENCRYPTION_ALGORITHM_INDEX).equals("NULL")) {
                result.add(Options.confidentiality);
                if (m.group(ENCRYPTION_ALGORITHM_KEY_LENGTH_INDEX) != null
                    && m.group(ENCRYPTION_ALGORITHM_KEY_LENGTH_INDEX).length() > 0
                    && Integer.parseInt(m.group(ENCRYPTION_ALGORITHM_KEY_LENGTH_INDEX)) >= MINIMUM_STRONG_KEY_LENGTH

                    && m.group(SHA_KEY_LENGTH_INDEX) != null
                    && m.group(SHA_KEY_LENGTH_INDEX).length() > 0
                    && Integer.parseInt(m.group(SHA_KEY_LENGTH_INDEX)) >= MINIMUM_STRONG_KEY_LENGTH)

                    result.add(Options.strong);

            }
        }
        return result;
    }

    /**
     * @param supports
     * @param requires
     * @param choice
     * @return
     */
    private static void matches(EnumSet<Options> supports, EnumSet<Options> requires, String choice) {
        EnumSet<Options> actual = getOptions(choice);

        boolean matchesRequires = actual.containsAll(requires);
        if (!matchesRequires) {
            Tr.warning(tc, "CSIv2_COMMON_CIPHER_SUITE_MISMATCH", choice, getOptions(choice), requires);
        }
        boolean matchesSupports = supports.containsAll(actual);
        if (!matchesSupports && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The " + choice + " requested cipher suite appears to have " + getOptions(choice) + " association options that do not match the specified " + supports
                         + " supported options.");
        }

    }

    /**
     * @return String - default SSL configuration alias name
     * @throws SSLException
     */
    public String getSSLAlias() throws SSLException {
        Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);

        Properties defaultSSLProps = null;
        defaultSSLProps = jsseHelper.getProperties(null, connectionInfo, null);
        if (defaultSSLProps != null)
            return defaultSSLProps.getProperty(Constants.SSLPROP_ALIAS);

        return null;
    }

    /**
     * @param host - Remote host of the connection
     * @param port - Remote port of the connection
     * @return String - default SSL configuration alias name
     * @throws SSLException
     */
    @FFDCIgnore(PrivilegedActionException.class)
    public String getSSLAlias(String host, int port) throws SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, String.valueOf(port));

        Properties sslProps = null;
        try {
            sslProps = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws SSLException {
                    return jsseHelper.getProperties(null, connectionInfo, null);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (SSLException) pae.getCause();
        }

        if (sslProps != null)
            return sslProps.getProperty(Constants.SSLPROP_ALIAS);

        return null;
    }

    /**
     * @param String - alias of SSL configuration being used
     * @return boolean
     */
    public boolean enableVerifyHostname(String sslAlias) {

        Properties sslProps = null;
        final String alias = sslAlias;
        try {
            sslProps = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws SSLException {
                    return jsseHelper.getProperties(alias, null, null);
                }
            });
        } catch (PrivilegedActionException pae) {
            // Can't get the properties so return false
            return false;
        }

        return Boolean.valueOf(sslProps.getProperty(Constants.SSLPROP_HOSTNAME_VERIFICATION, "true"));
    }

    /**
     * @param sslCfgAlias
     * @return
     */
    public Properties getSSLCfgProperties(String sslCfgAlias) {
        Properties sslProps = null;
        final String alias = sslCfgAlias;
        try {
            sslProps = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws SSLException {
                    return jsseHelper.getProperties(alias);
                }
            });
        } catch (PrivilegedActionException pae) {
            // Can't get the properties so return false
            return null;
        }
        return sslProps;
    }
}
