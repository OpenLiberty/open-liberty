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
package com.ibm.websphere.ssl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <p>
 * This contains most of the constants used for the SSL component.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class Constants {
    /**
     * Constructor.
     */
    private Constants() {
        // do nothing
    }

    private static final TraceComponent tc = Tr.register(Constants.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    /*** JSSE SYSTEM PROPERTIES ***/
    public static final String SYSTEM_SSLPROP_KEY_STORE = "javax.net.ssl.keyStore";
    public static final String SYSTEM_SSLPROP_KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    public static final String SYSTEM_SSLPROP_KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";
    public static final String SYSTEM_SSLPROP_KEY_STORE_PROVIDER = "javax.net.ssl.keyStoreProvider";
    public static final String SYSTEM_SSLPROP_TRUST_STORE = "javax.net.ssl.trustStore";
    public static final String SYSTEM_SSLPROP_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String SYSTEM_SSLPROP_TRUST_STORE_PROVIDER = "javax.net.ssl.trustStoreProvider";
    public static final String SYSTEM_SSLPROP_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

    /*** WAS SSL PROPERTIES ***/
    public static final String SSLPROP_ALIAS = "com.ibm.ssl.alias";
    public static final String SSLPROP_SSLTYPE = "com.ibm.ssl.sslType";
    public static final String SSLPROP_EXPIRED_WARNING = "com.ibm.ssl.daysBeforeExpireWarning";
    public static final String SSLPROP_ENABLED_CIPHERS = "com.ibm.ssl.enabledCipherSuites";
    public static final String SSLPROP_KEY_MANAGER = "com.ibm.ssl.keyManager";
    public static final String SSLPROP_PROTOCOL = "com.ibm.ssl.protocol";
    public static final String SSLPROP_CLIENT_AUTHENTICATION = "com.ibm.ssl.clientAuthentication";
    public static final String SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED = "com.ibm.ssl.clientAuthenticationSupported";
    public static final String SSLPROP_CONTEXT_PROVIDER = "com.ibm.ssl.contextProvider";
    public static final String SSLPROP_SECURITY_LEVEL = "com.ibm.ssl.securityLevel";
    public static final String SSLPROP_TRUST_MANAGER = "com.ibm.ssl.trustManager";
    public static final String SSLPROP_CUSTOM_TRUST_MANAGERS = "com.ibm.ssl.customTrustManagers";
    public static final String SSLPROP_CUSTOM_KEY_MANAGER = "com.ibm.ssl.customKeyManager";
    public static final String SSLPROP_VALIDATION_ENABLED = "com.ibm.ssl.validationEnabled";
    public static final String SSLPROP_DYNAMIC_SELECTION_INFO = "com.ibm.ssl.dynamicSelectionInfo";
    public static final String SSLPROP_EXCHANGE_SIGNER_PROMPT = "com.ibm.ssl.enableSignerExchangePrompt";
    public static final String SSLPROP_USE_INHERITABLE_THREAD_LOCAL = "com.ibm.ssl.useInheritableThreadLocalOnClient";
    public static final String SSLPROP_CONFIGURL_LOADED_FROM = "com.ibm.ssl.configURLLoadedFrom";
    public static final String SSLPROP_DEFAULT_ALIAS = "com.ibm.ssl.defaultAlias";
    public static final String SSLPROP_URL_HOSTNAME_VERIFICATION = "com.ibm.ssl.performURLHostNameVerification";
    public static final String SSLPROP_SKIP_DEFAULT_TM_WHEN_CUSTOM_TM_DEFINED = "com.ibm.ssl.skipDefaultTrustManagerWhenCustomDefined";
    public static final String SSLPROP_HOSTNAME_VERIFICATION = "com.ibm.ws.ssl.verifyHostname";
    public static final String SSLPROP_SKIP_HOSTNAME_VERIFICATION_FOR_HOSTS = "com.ibm.ws.ssl.skipHostnameVerificationForHosts";
    public static final String SSLPROP_USE_DEFAULTCERTS = "com.ibm.ws.ssl.trustDefaultCerts";
    public static final String SSLPROP_ENFORCE_CIPHER_ORDER = "com.ibm.ws.ssl.enforceCipherOrder";

    public static final String SSLPROP_AUTOACCEPT_SERVER_CERT = "com.ibm.ssl.autoaccept.server.certificates";
    public static final String SSLPROP_AUTOSTORE_SERVER_CERT = "com.ibm.ssl.autostore.server.certificates";
    public static final String SSLPROP_AUTOACCEPT_SERVER_CERT_FROM = "com.ibm.ssl.autoaccept.server.certificates.from";
    public static final String SSLPROP_AUTOSTORE_SERVER_CERT_FROM = "com.ibm.ssl.autostore.server.certificates.from";

    // DEFAULT CERTREQ SYSTEM PROPERTIES
    public static final String SSLPROP_DEFAULT_CERTREQ_ALIAS = "com.ibm.ssl.defaultCertReqAlias";
    public static final String SSLPROP_DEFAULT_CERTREQ_SUBJECTDN = "com.ibm.ssl.defaultCertReqSubjectDN";
    public static final String SSLPROP_DEFAULT_CERTREQ_DAYS = "com.ibm.ssl.defaultCertReqDays";
    public static final String SSLPROP_DEFAULT_CERTREQ_KEYSIZE = "com.ibm.ssl.defaultCertReqKeySize";

    // default certificate customization properties
    public static final String SSLPROP_ROOT_CERT_SUBJECTDN = "com.ibm.ssl.rootCertSubjectDN";
    public static final String SSLPROP_ROOT_CERT_DAYS = "com.ibm.ssl.rootCertValidDays";
    public static final String SSLPROP_ROOT_CERT_ALIAS = "com.ibm.ssl.rootCertAlias";
    public static final String SSLPROP_ROOT_CERT_KEYSIZE = "com.ibm.ssl.rootCertKeySize";

    // OCSP and CRL Properties
    public static final String SSLPROP_JSSE2_CHECK_REVOCATION = "com.ibm.jsse2.checkRevocation";
    public static final String SSLPROP_ENABLE_CRLDP = "com.ibm.security.enableCRLDP";
    public static final String SSLPROP_NULL_ENABLE_CRLDP = "com.ibm.security.enableNULLCRLDP";
    public static final String SSLPROP_LDAP_CERT_STORE_HOST = "com.ibm.security.ldap.certstore.host";
    public static final String SSLPROP_LDAP_CERT_STORE_PORT = "com.ibm.security.ldap.certstore.port";
    public static final String SSLPROP_OCSP_ENABLE = "ocsp.enable";
    public static final String SSLPROP_OCSP_RESPONDER_URL = "ocsp.responderURL";
    public static final String SSLPROP_OCSP_RESPONDER_CERT_SUBJECT_NAME = "ocsp.responderCertSubjectName";
    public static final String SSLPROP_OCSP_RESPONDER_CERT_ISSUER_NAME = "ocsp.responderCertIssuerName";
    public static final String SSLPROP_OCSP_RESPONDER_CERT_SERIAL_NUMBER = "ocsp.responderCertSerialNumber";

    /** JSSE KEYSTORE PROPERTIES **/
    public static final String SSLPROP_KEY_STORE_NAME = "com.ibm.ssl.keyStoreName";
    public static final String SSLPROP_KEY_STORE = "com.ibm.ssl.keyStore";
    public static final String SSLPROP_KEY_STORE_CLIENT_ALIAS = "com.ibm.ssl.keyStoreClientAlias";
    public static final String SSLPROP_KEY_STORE_SERVER_ALIAS = "com.ibm.ssl.keyStoreServerAlias";
    public static final String SSLPROP_KEY_STORE_PASSWORD = "com.ibm.ssl.keyStorePassword";
    public static final String SSLPROP_KEY_STORE_TYPE = "com.ibm.ssl.keyStoreType";
    public static final String SSLPROP_KEY_STORE_PROVIDER = "com.ibm.ssl.keyStoreProvider";
    public static final String SSLPROP_KEY_STORE_CUSTOM_CLASS = "com.ibm.ssl.keyStoreCustomClass";
    public static final String SSLPROP_KEY_STORE_FILE_BASED = "com.ibm.ssl.keyStoreFileBased";
    public static final String SSLPROP_KEY_STORE_HOST_LIST = "com.ibm.ssl.keyStoreHostList";
    public static final String SSLPROP_KEY_STORE_READ_ONLY = "com.ibm.ssl.keyStoreReadOnly";
    public static final String SSLPROP_KEY_STORE_INITIALIZE_AT_STARTUP = "com.ibm.ssl.keyStoreInitializeAtStartup";
    public static final String SSLPROP_KEY_STORE_CREATE_CMS_STASH = "com.ibm.ssl.keyStoreCreateCMSStash";
    public static final String SSLPROP_KEY_STORE_USE_FOR_ACCELERATION = "com.ibm.ssl.keyStoreUseForAcceleration";
    public static final String SSLPROP_KEY_STORE_SLOT = "com.ibm.ssl.keyStoreSlot";

    /** CRYPTO PROPERTIES **/
    public static final String SSLPROP_TOKEN_CONFIG_FILE = "com.ibm.ssl.tokenConfigFile";
    /** OLD CRYPTO PROPERTIES **/
    public static final String SSLPROP_TOKEN_ENABLED = "com.ibm.ssl.tokenEnabled";
    public static final String SSLPROP_TOKEN_LIBRARY = "com.ibm.ssl.tokenLibraryFile";
    public static final String SSLPROP_TOKEN_PASSWORD = "com.ibm.ssl.tokenPassword";
    public static final String SSLPROP_TOKEN_SLOT = "com.ibm.ssl.tokenSlot";
    public static final String SSLPROP_TOKEN_TYPE = "com.ibm.ssl.tokenType";

    /** JSSE TRUSTSTORE PROPERTIES **/
    public static final String SSLPROP_TRUST_STORE_NAME = "com.ibm.ssl.trustStoreName";
    public static final String SSLPROP_TRUST_STORE = "com.ibm.ssl.trustStore";
    public static final String SSLPROP_TRUST_STORE_PASSWORD = "com.ibm.ssl.trustStorePassword";
    public static final String SSLPROP_TRUST_STORE_PROVIDER = "com.ibm.ssl.trustStoreProvider";
    public static final String SSLPROP_TRUST_STORE_TYPE = "com.ibm.ssl.trustStoreType";
    public static final String SSLPROP_TRUST_STORE_CUSTOM_CLASS = "com.ibm.ssl.trustStoreCustomClass";
    public static final String SSLPROP_TRUST_STORE_FILE_BASED = "com.ibm.ssl.trustStoreFileBased";
    public static final String SSLPROP_TRUST_STORE_HOST_LIST = "com.ibm.ssl.trustStoreHostList";
    public static final String SSLPROP_TRUST_STORE_READ_ONLY = "com.ibm.ssl.trustStoreReadOnly";
    public static final String SSLPROP_TRUST_STORE_SLOT = "com.ibm.ssl.trustStoreSlot";
    public static final String SSLPROP_TRUST_STORE_INITIALIZE_AT_STARTUP = "com.ibm.ssl.trustStoreInitializeAtStartup";
    public static final String SSLPROP_TRUST_STORE_CREATE_CMS_STASH = "com.ibm.ssl.trustStoreCreateCMSStash";
    public static final String SSLPROP_TRUST_STORE_USE_FOR_ACCELERATION = "com.ibm.ssl.trustStoreUseForAcceleration";

    /*** CONNECTION INFO PROPERTIES ***/
    public static final String CONNECTION_INFO_DIRECTION = "com.ibm.ssl.direction";
    public static final String CONNECTION_INFO_REMOTE_HOST = "com.ibm.ssl.remoteHost";
    public static final String CONNECTION_INFO_REMOTE_PORT = "com.ibm.ssl.remotePort";
    public static final String CONNECTION_INFO_ENDPOINT_NAME = "com.ibm.ssl.endPointName";
    public static final String CONNECTION_INFO_CERT_MAPPING_HOST = "com.ibm.ssl.certMappingHost";
    public static final String CONNECTION_INFO_IS_WEB_CONTAINER_INBOUND = "com.ibm.ssl.isWebContainerInbound";

    /*** KEYSTORE TYPE CONSTANTS ***/
    public static final String KEYSTORE_TYPE_JKS = "JKS";
    public static final String KEYSTORE_TYPE_JCEKS = "JCEKS";
    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    public static final String KEYSTORE_TYPE_JAVACRYPTO = "PKCS11";
    public static final String KEYSTORE_TYPE_CMS = "CMSKS";
    public static final String KEYSTORE_TYPE_ISERIES = "IbmISeriesKeyStore";
    public static final String KEYSTORE_TYPE_JCERACFKS = "JCERACFKS";
    public static final String KEYSTORE_TYPE_RACFCRYPTO = "JCE4758KS";
    public static final String KEYSTORE_TYPE_JCECCAKS = "JCECCAKS";
    public static final String KEYSTORE_TYPE_JCECCARACFKS = "JCECCARACFKS";
    public static final String KEYSTORE_TYPE_JCEHYBRIDRACFKS = "JCEHYBRIDRACFKS";

    /** z/OS Keyring URI's **/
    public static final String SAFKEYRING_PREFIX = "safkeyring://";
    public static final String SAFKEYRING_HW_PREFIX = "safkeyringhw://";

    /** Default KeyStore Suffix **/
    public static final String DEFAULT_KEY_STORE = "DefaultKeyStore";
    public static final String DEFAULT_TRUST_STORE = "DefaultTrustStore";
    public static final String DEFAULT_ROOT_STORE = "DefaultRootStore";
    public static final String DEFAULT_DELETED_STORE = "DefaultDeletedStore";
    public static final String DEFAULT_SIGNERS_STORE = "DefaultSignersStore";
    public static final String RSA_TOKEN_KEY_STORE = "RSATokenKeyStore";
    public static final String RSA_TOKEN_TRUST_STORE = "RSATokenTrustStore";
    public static final String RSA_TOKEN_ROOT_STORE = "RSATokenRootStore";

    /** Default Certificate Types **/
    public static final String SSL_CERTIFICATE_TYPE = "SSL";
    public static final String RSA_CERTIFICATE_TYPE = "RSA";

    /*** Default Certificate Aliases **/
    public static final String DEFAULT_ROOT_CERTIFICATE_ALIAS = "root";
    public static final String DEFAULT_ROOT_CERTIFICATE_ALIAS_ZOS = "WebSphereCA";
    public static final String DEFAULT_CERTIFICATE_ALIAS = "default";

    /*** SSL PROTOCOL CONSTANTS ***/
    public static final String PROTOCOL_SSLV2 = "SSLv2";
    public static final String PROTOCOL_SSLV3 = "SSLv3";
    public static final String PROTOCOL_TLSV1 = "TLSv1";
    public static final String PROTOCOL_TLS = "TLS";
    public static final String PROTOCOL_SSL = "SSL";
    public static final String PROTOCOL_SSL_TLS = "SSL_TLS";
    public static final String PROTOCOL_SSL_TLS_V2 = "SSL_TLSv2";
    public static final String PROTOCOL_TLSV1_1 = "TLSv1.1";
    public static final String PROTOCOL_TLSV1_2 = "TLSv1.2";
    public static final String PROTOCOL_TLSV1_3 = "TLSv1.3";
    public static final String PROTOCOL_TLS_FIPS = PROTOCOL_TLSV1_2 + ", " + PROTOCOL_TLSV1_3;

    /*** SECURITY LEVEL CONSTANTS ***/
    public static final String SECURITY_LEVEL_HIGH = "HIGH";
    public static final String SECURITY_LEVEL_MEDIUM = "MEDIUM";
    public static final String SECURITY_LEVEL_LOW = "LOW";
    public static final String SECURITY_LEVEL_CUSTOM = "CUSTOM";

    /*** PROVIDER CONSTANTS ***/
    public static final String IBMJCE = "com.ibm.crypto.provider.IBMJCE";
    public static final String IBMJCE_NAME = "IBMJCE";
    public static final String IBMJCEFIPS = "com.ibm.crypto.fips.provider.IBMJCEFIPS";
    public static final String IBMJCEPlusFIPS = "com.ibm.crypto.plus.fips.provider.IBMJCEPlusFIPS";
    public static final String IBMJCEFIPS_NAME = "IBMJCEFIPS";
    public static final String IBMJCEPlusFIPS_NAME = "IBMJCEPlusFIPS";
    public static final String IBMJSSE2 = "com.ibm.jsse2.IBMJSSEProvider2";
    public static final String IBMJSSE2_NAME = "IBMJSSE2";
    public static final String IBMJSSE_NAME = "IBMJSSE";
    public static final String SUNJSSE_NAME = "SunJSSE";
    public static final String SUNJCE_NAME = "SunJCE";
    public static final String IBMJSSEFIPS_NAME = "IBMJSSEFIPS";
    public static final String IBMPKCS11Impl = "com.ibm.crypto.pkcs11impl.provider.IBMPKCS11Impl";
    public static final String IBMPKCS11Impl_NAME = "IBMPKCS11Impl";
    public static final String IBMCMS = "com.ibm.security.cmskeystore.CMSProvider";
    public static final String IBMCMS_NAME = "IBMCMSProvider";
    public static final String DEFAULT_JCE_PROVIDER = "DEFAULT_JCE_PROVIDER";
    public static final String IBMJCECCA_NAME = "IBMJCECCA";

    /*** FIPS CONSTANTS ***/
    public static final String USE_FIPS = "com.ibm.security.useFIPS";
    public static final String FIPS_ENABLED = "com.ibm.websphere.security.fips.enabled";
    public static final String FIPS_JCEPROVIDERS = "com.ibm.websphere.security.fips.jceProviders";
    public static final String FIPS_JSSEPROVIDERS = "com.ibm.websphere.security.fips.jsseProviders";
    public static final String USEFIPS_ENABLED = "USEFIPS_ENABLED";

    /*** INBOUND ENDPOINT CONSTANTS ***/
    public static final String ENDPOINT_CSIV2_MUTUALAUTH = "CSIV2_SSL_MUTUALAUTH_LISTENER_ADDRESS";
    public static final String ENDPOINT_CSIV2_SERVERAUTH = "CSIV2_SSL_SERVERAUTH_LISTENER_ADDRESS";
    public static final String ENDPOINT_SOAP_CONNECTOR_ADDRESS = "SOAP_CONNECTOR_ADDRESS";
    public static final String ENDPOINT_IPC_CONNECTOR_ADDRESS = "IPC_CONNECTOR_ADDRESS";
    public static final String ENDPOINT_ORB_SSL_LISTENER_ADDRESS = "ORB_SSL_LISTENER_ADDRESS";

    /*** OUTBOUND ENDPOINT CONSTANTS ***/
    public static final String ENDPOINT_IIOP = "IIOP";
    public static final String ENDPOINT_HTTP = "HTTP";
    public static final String ENDPOINT_SIP = "SIP";
    public static final String ENDPOINT_JMS = "JMS";
    public static final String ENDPOINT_BUS_CLIENT = "BUS_CLIENT";
    public static final String ENDPOINT_BUS_TO_BUS = "BUS_TO_BUS";
    public static final String ENDPOINT_BUS_TO_WEBSPHERE_MQ = "BUS_TO_WEBSPHERE_MQ";
    public static final String ENDPOINT_CLIENT_TO_WEBSPHERE_MQ = "CLIENT_TO_WEBSPHERE_MQ";
    public static final String ENDPOINT_LDAP = "LDAP";
    public static final String ENDPOINT_ADMIN_IIOP = "ADMIN_IIOP";
    public static final String ENDPOINT_ADMIN_SOAP = "ADMIN_SOAP";
    public static final String ENDPOINT_ADMIN_IPC = "ADMIN_IPC";
    public static final String ENDPOINT_WEBSERVICES_HTTP = "WEBSERVICES_HTTP";
    public static final String ENDPOINT_WEBSERVICES_JMS = "WEBSERVICES_JMS";

    /*** Alias used when finding javax.net.ssl.* System properties ***/
    public static final String DEFAULT_SYSTEM_ALIAS = "DefaultSystemProperties";
    public static final String DEFAULT_TRUST_MANAGER = "DefaultTrustManager";
    public static final String DEFAULT_KEY_MANAGER = "DefaultKeyManager";

    /*** SELECTION TYPE ***/
    public static final String SELECTION_TYPE_DIRECT = "direct";
    public static final String SELECTION_TYPE_THREAD = "thread";
    public static final String SELECTION_TYPE_DYNAMIC = "dynamic";

    /*** SOCKET FACTORY IMPLEMENTATIONS ***/
    public static final String SOCKET_FACTORY_JSSE_DEFAULT = "com.ibm.jsse2.SSLSocketFactoryImpl";
    public static final String SERVER_SOCKET_FACTORY_JSSE_DEFAULT = "com.ibm.jsse2.SSLServerSocketFactoryImpl";
    public static final String SOCKET_FACTORY_WAS_DEFAULT = "com.ibm.websphere.ssl.protocol.SSLSocketFactory";
    public static final String SERVER_SOCKET_FACTORY_WAS_DEFAULT = "com.ibm.websphere.ssl.protocol.SSLServerSocketFactory";

    /*** CONFIG STATE ***/
    public static final String CONFIG_STATE_DELETED = "deleted";
    public static final String CONFIG_STATE_CHANGED = "changed";

    /*** MISCELLANEOUS CONSTANTS ***/
    public static final String DEFAULT_KEYSTORE_PASSWORD = "WebAS";
    public static final String DEFAULT_CERT_EXPIRE_WARNING_DAYS = "60";
    public static final String SSLTYPE_JSSE = "JSSE";
    public static final String SSLTYPE_SSSL = "SSSL";
    public static final String DIRECTION_INBOUND = "inbound";
    public static final String DIRECTION_OUTBOUND = "outbound";
    public static final String DIRECTION_UNKNOWN = "unknown";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    // START OF UNUSED CONSTANTS
    //     unused in LIberty but not removed since this class is defined as an API

    // unknown cipher
    public static final String SSL_UNKNOWN_CIPHER = "UNKNOWN_CIPHER";

    /** SSL V2 cipher specifications */
    public static final String SSL_CK_RC4_128_WITH_MD5 = "SSL_CK_RC4_128_WITH_MD5",
                    SSL_CK_RC4_128_EXPORT40_WITH_MD5 = "SSL_CK_RC4_128_EXPORT40_WITH_MD5",
                    SSL_CK_RC2_128_CBC_WITH_MD5 = "SSL_CK_RC2_128_CBC_WITH_MD5",
                    SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5 = "SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5",
                    SSL_CK_DES_64_CBC_WITH_MD5 = "SSL_CK_DES_64_CBC_WITH_MD5",
                    SSL_CK_DES_192_EDE3_CBC_WITH_MD5 = "SSL_CK_DES_192_EDE3_CBC_WITH_MD5";

    /** SSL V3 cipher specifications */
    public static final String SSL_NULL_WITH_NULL_NULL = "SSL_NULL_WITH_NULL_NULL",
                    SSL_RSA_WITH_NULL_MD5 = "SSL_RSA_WITH_NULL_MD5",
                    SSL_RSA_WITH_NULL_SHA = "SSL_RSA_WITH_NULL_SHA",
                    SSL_RSA_EXPORT_WITH_RC4_40_MD5 = "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                    SSL_RSA_WITH_RC4_128_MD5 = "SSL_RSA_WITH_RC4_128_MD5",
                    SSL_RSA_WITH_RC4_128_SHA = "SSL_RSA_WITH_RC4_128_SHA",
                    SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5 = "SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5",
                    SSL_RSA_EXPORT_WITH_DES40_CBC_SHA = "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    SSL_RSA_WITH_DES_CBC_SHA = "SSL_RSA_WITH_DES_CBC_SHA",
                    SSL_RSA_WITH_3DES_EDE_CBC_SHA = "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                    SSL_RSA_WITH_AES_128_CBC_SHA = "SSL_RSA_WITH_AES_128_CBC_SHA",
                    SSL_RSA_WITH_AES_256_CBC_SHA = "SSL_RSA_WITH_AES_256_CBC_SHA",

                    SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA = "SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA",

                    SSL_DH_DSS_WITH_DES_CBC_SHA = "SSL_DH_DSS_WITH_DES_CBC_SHA",
                    SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA = "SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA",
                    SSL_DH_RSA_WITH_DES_CBC_SHA = "SSL_DH_RSA_WITH_DES_CBC_SHA",
                    SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA = "SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA",
                    SSL_DH_DSS_WITH_AES_128_CBC_SHA = "SSL_DH_DSS_WITH_AES_128_CBC_SHA",
                    SSL_DH_DSS_WITH_AES_256_CBC_SHA = "SSL_DH_DSS_WITH_AES_256_CBC_SHA",
                    SSL_DH_RSA_WITH_AES_128_CBC_SHA = "SSL_DH_RSA_WITH_AES_128_CBC_SHA",
                    SSL_DH_RSA_WITH_AES_256_CBC_SHA = "SSL_DH_RSA_WITH_AES_256_CBC_SHA",

                    SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA = "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
                    SSL_DHE_DSS_WITH_DES_CBC_SHA = "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                    SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA = "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                    SSL_DHE_DSS_WITH_RC4_128_SHA = "SSL_DHE_DSS_WITH_RC4_128_SHA",
                    SSL_DHE_DSS_WITH_AES_128_CBC_SHA = "SSL_DHE_DSS_WITH_AES_128_CBC_SHA",
                    SSL_DHE_DSS_WITH_AES_256_CBC_SHA = "SSL_DHE_DSS_WITH_AES_256_CBC_SHA",

                    SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA = "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    SSL_DHE_RSA_WITH_DES_CBC_SHA = "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                    SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA = "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    SSL_DHE_RSA_WITH_AES_128_CBC_SHA = "SSL_DHE_RSA_WITH_AES_128_CBC_SHA",
                    SSL_DHE_RSA_WITH_AES_256_CBC_SHA = "SSL_DHE_RSA_WITH_AES_256_CBC_SHA",

                    SSL_DH_anon_EXPORT_WITH_RC4_40_MD5 = "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                    SSL_DH_anon_WITH_RC4_128_MD5 = "SSL_DH_anon_WITH_RC4_128_MD5",
                    SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA = "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                    SSL_DH_anon_WITH_DES_CBC_SHA = "SSL_DH_anon_WITH_DES_CBC_SHA",
                    SSL_DH_anon_WITH_3DES_EDE_CBC_SHA = "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
                    SSL_DH_anon_WITH_AES_128_CBC_SHA = "SSL_DH_anon_WITH_AES_128_CBC_SHA",
                    SSL_DH_anon_WITH_AES_256_CBC_SHA = "SSL_DH_anon_WITH_AES_256_CBC_SHA";
    // END OF UNUSED CONSTANTS

    /**
     * This method adjusts the supported ciphers to include those appropriate
     * to the security level (HIGH, MEDIUM, LOW).
     *
     * @param supportedCiphers
     * @param securityLevel
     * @return String[]
     */
    public static String[] adjustSupportedCiphersToSecurityLevel(String[] supportedCiphers, String securityLevel) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "adjustSupportedCiphersToSecurityLevel", new Object[] { convertCipherListToString(supportedCiphers), securityLevel });
        }
        List<String> newCipherList = new ArrayList<String>();

        if (supportedCiphers != null && supportedCiphers.length > 0) {
            if (securityLevel == null) {
                securityLevel = Constants.SECURITY_LEVEL_HIGH;
            }

            if (securityLevel.equals(Constants.SECURITY_LEVEL_LOW)) {
                /**
                 * Security Level "high" will pare it down even more.
                 * Strict Level Criteria: Support integrity signing algorithms but not to perform encryption
                 */
                for (int i = 0; i < supportedCiphers.length; i++) {
                    if ((supportedCiphers[i].indexOf("_anon_") != -1
                         || supportedCiphers[i].indexOf("_NULL_") != -1)
                        && supportedCiphers[i].indexOf("_KRB5_") == -1
                        && supportedCiphers[i].indexOf("_AES_") == -1
                        && supportedCiphers[i].indexOf("DES") == -1
                        && supportedCiphers[i].indexOf("_RC") == -1
                        && supportedCiphers[i].indexOf("_EXPORT_") == -1) {
                        newCipherList.add(supportedCiphers[i]);
                    }
                }
            } else if (securityLevel.equals(Constants.SECURITY_LEVEL_MEDIUM)) {
                /**
                 * Security Level "medium" will pare it down remove "anonymous" and
                 * "NULL" ciphers.
                 */
                for (int i = 0; i < supportedCiphers.length; i++) {
                    if ((supportedCiphers[i].indexOf("40_") != -1
                         || supportedCiphers[i].indexOf("_DES_") != -1)
                        && supportedCiphers[i].indexOf("_anon_") == -1
                        && supportedCiphers[i].indexOf("_NULL_") == -1
                        && supportedCiphers[i].indexOf("_KRB5_") == -1
                        && supportedCiphers[i].indexOf("_RC4") == -1
                        && supportedCiphers[i].indexOf("_EXPORT_") == -1) {
                        newCipherList.add(supportedCiphers[i]);
                    }
                }
            } else {
                /** Security Level "high" will pare it down even more. */
                for (int i = 0; i < supportedCiphers.length; i++) {
                    if ((supportedCiphers[i].indexOf("128_") != -1
                         || supportedCiphers[i].indexOf("256_") != -1
                         || supportedCiphers[i].indexOf("CHACHA20_POLY1305_") != -1)
                        && supportedCiphers[i].indexOf("_anon_") == -1
                        && supportedCiphers[i].indexOf("_NULL_") == -1
                        && supportedCiphers[i].indexOf("_KRB5_") == -1
                        && supportedCiphers[i].indexOf("_RC4") == -1
                        && supportedCiphers[i].indexOf("_EXPORT_") == -1
                        && supportedCiphers[i].indexOf("_FIPS_") == -1
                        && supportedCiphers[i].indexOf("_3DES_") == -1) {
                        newCipherList.add(supportedCiphers[i]);
                    }
                }
            }
        }

        String[] rc = newCipherList.toArray(new String[newCipherList.size()]);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "adjustSupportedCiphersToSecurityLevel -> "
                        + convertCipherListToString(rc));
        }
        return rc;
    }

    /**
     * This method converts the cipher suite String[] to a space-delimited String.
     *
     * @param cipherList
     * @return String
     */
    public static String convertCipherListToString(String[] cipherList) {
        if (cipherList == null || cipherList.length == 0) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('(').append(cipherList.length).append(')');
        for (int i = 0; i < cipherList.length; i++) {
            sb.append(' ');
            sb.append(cipherList[i]);
        }

        return sb.toString();
    }

    public static final List<String> MULTI_PROTOCOL_LIST = Arrays.asList(new String[] {
                                                                                        PROTOCOL_TLSV1,
                                                                                        PROTOCOL_TLSV1_1,
                                                                                        PROTOCOL_TLSV1_2,
                                                                                        PROTOCOL_TLSV1_3
    });


    public static final List<String> FIPS_140_2_PROTOCOLS = Arrays.asList(new String[] {
                                                                                         PROTOCOL_TLSV1,
                                                                                         PROTOCOL_TLSV1_1
    });

    public static final List<String> FIPS_140_3_PROTOCOLS = Arrays.asList(new String[] {
                                                                                         PROTOCOL_TLSV1_2,
                                                                                         PROTOCOL_TLSV1_3
    });

    public boolean resolveDisableHostnameVerification(String targetHostname, String disabledVerifyHostname, Properties sslProps) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "resolveDisableHostnameVerification:  ", targetHostname, disabledVerifyHostname, sslProps);
        }
        boolean result = false;
        if (targetHostname != null && disabledVerifyHostname != null && sslProps != null) {
            if ("false".equalsIgnoreCase(disabledVerifyHostname) && sslProps != null) {
                String skipHostList = sslProps.getProperty("com.ibm.ws.ssl.skipHostnameVerificationForHosts");
                if (isSkipHostnameVerificationForHosts(targetHostname, skipHostList))
                    result = true;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "resolveDisableHostnameVerification:  " + result);
        }
        return result;
    }

    /**
     * https://datatracker.ietf.org/doc/html/rfc2830#section-3.6
     * The "*" wildcard character is allowed. If present, it applies only to the left-most name component.
     *
     * @param String host - target host
     * @param String skipHostList - comma separated list of hostnames with hostname verification disabled, e.g. "hello.com, world.com"
     */
    public static boolean isSkipHostnameVerificationForHosts(String remoteHost, String skipHostList) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isSkipHostnameVerificationForHosts", new Object[] { remoteHost, skipHostList });
        boolean skipHostnameVerification = false;
        if (remoteHost != null && skipHostList != null && !!!skipHostList.isEmpty()) {
            List<String> skipHosts = Arrays.asList(skipHostList.split("\\s*,\\s*"));

            for (String host : skipHosts) {
                if (host.startsWith("*.")) {
                    // escapes special characters for regex notation
                    String regex = host.replaceAll("([\\[\\]().+?^${}|\\\\])", "\\\\$1");
                    regex = "^" + regex.replace("*", ".+") + "$";
                    if (remoteHost.matches(regex)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Hostname verification is disabled as remote host [" + remoteHost + "] matches pattern [" + host + "]");
                        skipHostnameVerification = true;
                    }
                } else {
                    if (remoteHost.equalsIgnoreCase(host)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Hostname verification is disabled as remote host [" + remoteHost + "] matches [" + host + "]");
                        skipHostnameVerification = true;
                    }
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isSkipHostnameVerificationForHosts", new Object[] { skipHostnameVerification });
        return skipHostnameVerification;
    }

}
