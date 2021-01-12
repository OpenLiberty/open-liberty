/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.configuration.jsse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.resource.ResourceManager;


/**
 * Holder for utility methods related to manipulating SSL settings, common
 * to the connection and listener factories (previously duplicated).
 */
public final class SSLUtils {

    static final String PKCS12_TYPE = "PKCS12";

    private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";
    private static final String DEFAULT_TRUST_STORE_TYPE = "JKS";

    private static final String HTTPS_CIPHER_SUITES = "https.cipherSuites";

    /**
     * By default, exclude NULL, anon, EXPORT, (3)DES, MD5, CBC and RC4 ciphersuites
     */
    private static final List<String> DEFAULT_CIPHERSUITE_FILTERS_EXCLUDE =
        Arrays.asList(new String[] {".*NULL.*",
                                    ".*anon.*",
                                    ".*EXPORT.*",
                                    ".*DES.*",
                                    ".*MD5",
                                    ".*CBC.*",
                                    ".*RC4.*"});

    private static volatile KeyManager[] defaultManagers;

    private SSLUtils() {
    }

    public static KeyManager[] getDefaultKeyStoreManagers(Logger log) {
        if (defaultManagers == null) {
            loadDefaultKeyManagers(log);
        }
        if (defaultManagers.length == 0) {
            return null;
        }
        return defaultManagers;
    }

    private static synchronized void loadDefaultKeyManagers(Logger log) {
        if (defaultManagers != null) {
            return;
        }

        String location = getKeystore(null, log);
        String keyStorePassword = getKeystorePassword(null, log);
        String keyPassword = getKeyPassword(null, log);
        String keyStoreType = getKeystoreType(null, log);
        InputStream is = null;

        try {
            if (location != null) {
                File file = new File(location);
                if (FileUtils.exists(file)) { //Liberty change - no longer needed at 3.3.3
                    is = Files.newInputStream(file.toPath());
                } else {
                    is = getResourceAsStream(location);
                }
            }

            if (is != null) {
                KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance(keyStoreType != null ? keyStoreType : KeyStore.getDefaultType());

                ks.load(is, (keyStorePassword != null) ? keyStorePassword.toCharArray() : null);
                kmf.init(ks, (keyPassword != null) ? keyPassword.toCharArray() : null);
                defaultManagers = kmf.getKeyManagers();
            } else {
                log.log(Level.FINER, "No default keystore {0}", location);
                defaultManagers = new KeyManager[0];
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Default key managers cannot be initialized: " + e.getMessage(), e);
            defaultManagers = new KeyManager[0];
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warning("Keystore stream cannot be closed: " + e.getMessage());
                }
            }
        }
    }

    // We don't cache the default TrustStore managers here (see above) for backwards compatibility reasons
    // We also return null rather than an empty array in case this changes using the default trust managers when
    // initing the SSLContext
    public static TrustManager[] getDefaultTrustStoreManagers(Logger log) {
        String location = getTruststore(null, log);
        String trustStorePassword = getTruststorePassword(null, log);
        String trustStoreType = getTrustStoreType(null, log, DEFAULT_TRUST_STORE_TYPE);
        InputStream is = null;

        try {
            if (location != null) {
                File file = new File(location);
                if (file.exists()) {
                    is = Files.newInputStream(file.toPath());
                } else {
                    is = getResourceAsStream(location);
                }
            }

            if (is != null) {
                TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance(trustStoreType);

                ks.load(is, (trustStorePassword != null) ? trustStorePassword.toCharArray() : null);
                tmf.init(ks);
                return tmf.getTrustManagers();
            } else {
                log.log(Level.FINER, "No default trust keystore {0}", location);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Default trust managers cannot be initialized: " + e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warning("Keystore stream cannot be closed: " + e.getMessage());
                }
            }
        }

        return null;
    }

    private static InputStream getResourceAsStream(String resource) {
        InputStream is = ClassLoaderUtils.getResourceAsStream(resource, SSLUtils.class);
        if (is == null) {
            Bus bus = BusFactory.getThreadDefaultBus(true);
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            if (rm != null) {
                is = rm.getResourceAsStream(resource);
            }
        }
        return is;
    }

    public static KeyManager[] loadKeyStore(KeyManagerFactory kmf,
                                               KeyStore ks,
                                               ByteArrayInputStream bin,
                                               String keyStoreLocation,
                                               String keyStorePassword,
                                               Logger log) {
        KeyManager[] keystoreManagers = null;
        try {
            ks.load(bin, keyStorePassword.toCharArray());
            kmf.init(ks, keyStorePassword.toCharArray());
            keystoreManagers = kmf.getKeyManagers();
            LogUtils.log(log,
                         Level.FINE,
                         "LOADED_KEYSTORE",
                         keyStoreLocation);
        } catch (Exception e) {
            LogUtils.log(log,
                         Level.WARNING,
                         "FAILED_TO_LOAD_KEYSTORE",
                         new Object[]{keyStoreLocation, e.getMessage()});
        }
        return keystoreManagers;
    }

    protected static byte[] loadFile(String fileName) throws IOException {
        if (fileName == null) {
            return null;
        }
        Path path = FileSystems.getDefault().getPath(fileName);
        return Files.readAllBytes(path);
    }

    public static String getKeystore(String keyStoreLocation, Logger log) {
        final String logMsg;
        if (keyStoreLocation != null) {
            logMsg = "KEY_STORE_SET";
        } else {
            keyStoreLocation = SystemPropertyAction.getProperty("javax.net.ssl.keyStore");
            if (keyStoreLocation != null) {
                logMsg = "KEY_STORE_SYSTEM_PROPERTY_SET";
            } else {
                keyStoreLocation =
                    SystemPropertyAction.getProperty("user.home") + "/.keystore";
                logMsg = "KEY_STORE_NOT_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreLocation);
        return keyStoreLocation;
    }

    public static String getKeystoreType(String keyStoreType, Logger log) {
        return getKeystoreType(keyStoreType, log, DEFAULT_KEYSTORE_TYPE);
    }

    public static String getKeystoreType(String keyStoreType, Logger log, String def) {
        final String logMsg;
        if (keyStoreType != null) {
            logMsg = "KEY_STORE_TYPE_SET";
        } else {
            keyStoreType = SystemPropertyAction.getProperty("javax.net.ssl.keyStoreType", null);
            if (keyStoreType == null) {
                keyStoreType = def;
                logMsg = "KEY_STORE_TYPE_NOT_SET";
            } else {
                logMsg = "KEY_STORE_TYPE_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreType);
        return keyStoreType;
    }

    public static String getKeystoreProvider(String keyStoreProvider, Logger log) {
        final String logMsg;
        if (keyStoreProvider != null) {
            logMsg = "KEY_STORE_PROVIDER_SET";
        } else {
            keyStoreProvider = SystemPropertyAction.getProperty("javax.net.ssl.keyStoreProvider", null);
            if (keyStoreProvider == null) {
                logMsg = "KEY_STORE_PROVIDER_NOT_SET";
            } else {
                logMsg = "KEY_STORE_PROVIDER_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreProvider);
        return keyStoreProvider;
    }

    public static String getKeystorePassword(String keyStorePassword,
                                             Logger log) {
        final String logMsg;
        if (keyStorePassword != null) {
            logMsg = "KEY_STORE_PASSWORD_SET";
        } else {
            keyStorePassword =
                SystemPropertyAction.getProperty("javax.net.ssl.keyStorePassword");
            logMsg = keyStorePassword != null
                     ? "KEY_STORE_PASSWORD_SYSTEM_PROPERTY_SET"
                     : "KEY_STORE_PASSWORD_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg);
        return keyStorePassword;
    }

    public static String getKeyPassword(String keyPassword, Logger log) {
        final String logMsg;
        if (keyPassword != null) {
            logMsg = "KEY_PASSWORD_SET";
        } else {
            keyPassword =
                SystemPropertyAction.getProperty("javax.net.ssl.keyPassword");
            if (keyPassword == null) {
                keyPassword =
                    SystemPropertyAction.getProperty("javax.net.ssl.keyStorePassword");
            }
            logMsg = keyPassword != null
                     ? "KEY_PASSWORD_SYSTEM_PROPERTY_SET"
                     : "KEY_PASSWORD_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg);
        return keyPassword;
    }

    public static String getKeystoreAlgorithm(
                                          String keyStoreMgrFactoryAlgorithm,
                                          Logger log) {
        final String logMsg;
        if (keyStoreMgrFactoryAlgorithm != null) {
            logMsg = "KEY_STORE_ALGORITHM_SET";
        } else {
            keyStoreMgrFactoryAlgorithm =
                KeyManagerFactory.getDefaultAlgorithm();
            logMsg = "KEY_STORE_ALGORITHM_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg, keyStoreMgrFactoryAlgorithm);
        return keyStoreMgrFactoryAlgorithm;
    }

    public static String getTrustStoreAlgorithm(
                                        String trustStoreMgrFactoryAlgorithm,
                                        Logger log) {
        final String logMsg;
        if (trustStoreMgrFactoryAlgorithm != null) {
            logMsg = "TRUST_STORE_ALGORITHM_SET";
        } else {
            trustStoreMgrFactoryAlgorithm =
                TrustManagerFactory.getDefaultAlgorithm();
            logMsg = "TRUST_STORE_ALGORITHM_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg, trustStoreMgrFactoryAlgorithm);
        return trustStoreMgrFactoryAlgorithm;
    }

    public static SSLContext getSSLContext(String protocol,
                                           KeyManager[] keyStoreManagers,
                                           TrustManager[] trustStoreManagers)
        throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance(protocol);
        ctx.init(keyStoreManagers, trustStoreManagers, null);
        return ctx;
    }

    public static String[] getSupportedCipherSuites(SSLContext context) {
        return context.getSocketFactory().getSupportedCipherSuites();
    }

    public static String[] getServerSupportedCipherSuites(SSLContext context) {
        return context.getServerSocketFactory().getSupportedCipherSuites();
    }

    public static String[] getCiphersuitesToInclude(List<String> cipherSuitesList,
                                           FiltersType filters,
                                           String[] defaultCipherSuites,
                                           String[] supportedCipherSuites,
                                           Logger log) {
        // CipherSuites are returned in the following priority:
        // 1) If we have defined explicit "cipherSuite" configuration
        // 2) If we have defined ciphersuites via a system property.
        // 3) The default JVM CipherSuites, if no filters have been defined
        // 4) Filter the supported cipher suites (*not* the default JVM CipherSuites)
        if (!(cipherSuitesList == null || cipherSuitesList.isEmpty())) {
            return getCiphersFromList(cipherSuitesList, log, false);
        }

        String[] cipherSuites = getSystemCiphersuites(log);
        if (cipherSuites != null) {
            return cipherSuites;
        }

        // If we have no explicit cipherSuites (for the include case as above), and no filters,
        // then just use the defaults
        if ((defaultCipherSuites != null && defaultCipherSuites.length != 0)
            && (filters == null || !(filters.isSetInclude() || filters.isSetExclude()))) {
            LogUtils.log(log, Level.FINE, "CIPHERSUITES_SET", Arrays.toString(defaultCipherSuites));
            return defaultCipherSuites;
        }

        LogUtils.log(log, Level.FINE, "CIPHERSUITES_NOT_SET");

        return getFilteredCiphersuites(filters, supportedCipherSuites, log, false);
    }

    public static String[] getFilteredCiphersuites(FiltersType filters,
                                           String[] supportedCipherSuites,
                                           Logger log, boolean exclude) {
        // We have explicit filters, so use the "include/exclude" cipherSuiteFilter configuration
        List<Pattern> includes = new ArrayList<>();
        List<Pattern> excludes = new ArrayList<>();

        if (filters != null) {
            // We must have an inclusion pattern specified or no ciphersuites are filtered
            compileRegexPatterns(includes, filters.getInclude(), true, log);

            if (filters.isSetExclude()) {
                // If we have specified excludes, then the default excludes are ignored
                compileRegexPatterns(excludes, filters.getExclude(), false, log);
            } else {
                // Otherwise use the default excludes, but remove from the default excludes any
                // ciphersuites explicitly matched by the inclusion filters
                List<String> filteredExcludes =
                    filterDefaultExcludes(filters.getInclude(), DEFAULT_CIPHERSUITE_FILTERS_EXCLUDE);
                compileRegexPatterns(excludes, filteredExcludes, false, log);
            }
        }

        List<String> filteredCipherSuites = new ArrayList<>();
        for (String supportedCipherSuite : supportedCipherSuites) {
            if (matchesOneOf(supportedCipherSuite, includes)
                && !matchesOneOf(supportedCipherSuite, excludes)) {
                LogUtils.log(log,
                             Level.FINE,
                             "CIPHERSUITE_INCLUDED",
                             supportedCipherSuite);
                if (!exclude) {
                    filteredCipherSuites.add(supportedCipherSuite);
                }
            } else {
                LogUtils.log(log,
                             Level.FINE,
                             "CIPHERSUITE_EXCLUDED",
                             supportedCipherSuite);
                if (exclude) {
                    filteredCipherSuites.add(supportedCipherSuite);
                }
            }
        }

        return getCiphersFromList(filteredCipherSuites, log, exclude);
    }

    private static List<String> filterDefaultExcludes(List<String> includes, List<String> defaultExcludes) {
        if (includes != null && !includes.isEmpty()) {
            // Filter the default exclusion filters to remove any that explicitly match the inclusion filters
            // e.g. if the user wants the NULL ciphersuite then remove it from the default excludes
            return defaultExcludes.stream()
                .filter(ex -> !includes.stream()
                    .anyMatch(inc -> inc.matches(ex)))
                .collect(Collectors.toList());
        }

        return defaultExcludes;
    }

    private static String[] getSystemCiphersuites(Logger log) {
        String jvmCipherSuites = System.getProperty(HTTPS_CIPHER_SUITES);
        if ((jvmCipherSuites != null) && (!jvmCipherSuites.isEmpty())) {
            LogUtils.log(log, Level.FINE, "CIPHERSUITES_SYSTEM_PROPERTY_SET", jvmCipherSuites);
            return jvmCipherSuites.split(",");
        }
        return null;

    }

    private static void compileRegexPatterns(List<Pattern> patterns, List<String> regexes,
                                             boolean include, Logger log) {
        if (regexes != null) {
            String msg = include
                         ? "CIPHERSUITE_INCLUDE_FILTER"
                         : "CIPHERSUITE_EXCLUDE_FILTER";
            for (String s : regexes) {
                LogUtils.log(log, Level.FINE, msg, s);
                patterns.add(Pattern.compile(s));
            }
        }
    }

    private static boolean matchesOneOf(String s, List<Pattern> patterns) {
        boolean matches = false;
        if (patterns != null) {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.matches()) {
                    matches = true;
                    break;
                }
            }
        }
        return matches;
    }

    private static String[] getCiphersFromList(List<String> cipherSuitesList,
                                               Logger log,
                                               boolean exclude) {
        String[] cipherSuites = cipherSuitesList.toArray(new String[0]);
        if (log.isLoggable(Level.FINE)) {
            LogUtils.log(log, Level.FINE,
                exclude ? "CIPHERSUITES_EXCLUDED" : "CIPHERSUITES_SET", String.join(", ", cipherSuites));
        }
        return cipherSuites;
    }

    public static String getTruststore(String trustStoreLocation, Logger log) {
        final String logMsg;
        if (trustStoreLocation != null) {
            logMsg = "TRUST_STORE_SET";
        } else {
            trustStoreLocation = SystemPropertyAction.getProperty("javax.net.ssl.trustStore");
            if (trustStoreLocation != null) {
                logMsg = "TRUST_STORE_SYSTEM_PROPERTY_SET";
            } else {
                logMsg = "TRUST_STORE_NOT_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, trustStoreLocation);
        return trustStoreLocation;
    }

    public static String getTrustStoreType(String trustStoreType, Logger log) {
        return getTrustStoreType(trustStoreType, log, DEFAULT_TRUST_STORE_TYPE);
    }

    public static String getTrustStoreType(String trustStoreType, Logger log, String def) {
        final String logMsg;
        if (trustStoreType != null) {
            logMsg = "TRUST_STORE_TYPE_SET";
        } else {
            //Can default to JKS
            trustStoreType = SystemPropertyAction.getProperty("javax.net.ssl.trustStoreType");
            if (trustStoreType == null) {
                trustStoreType = def;
                logMsg = "TRUST_STORE_TYPE_NOT_SET";
            } else {
                logMsg = "TRUST_STORE_TYPE_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, trustStoreType);
        return trustStoreType;
    }

    public static String getTruststorePassword(String trustStorePassword,
                                             Logger log) {
        final String logMsg;
        if (trustStorePassword != null) {
            logMsg = "TRUST_STORE_PASSWORD_SET";
        } else {
            trustStorePassword =
                SystemPropertyAction.getProperty("javax.net.ssl.trustStorePassword");
            logMsg = trustStorePassword != null
                     ? "TRUST_STORE_PASSWORD_SYSTEM_PROPERTY_SET"
                     : "TRUST_STORE_PASSWORD_NOT_SET";
        }
        LogUtils.log(log, Level.FINE, logMsg);
        return trustStorePassword;
    }

    public static String getTruststoreProvider(String trustStoreProvider, Logger log) {
        final String logMsg;
        if (trustStoreProvider != null) {
            logMsg = "TRUST_STORE_PROVIDER_SET";
        } else {
            trustStoreProvider = SystemPropertyAction.getProperty("javax.net.ssl.trustStoreProvider", null);
            if (trustStoreProvider == null) {
                logMsg = "TRUST_STORE_PROVIDER_NOT_SET";
            } else {
                logMsg = "TRUST_STORE_PROVIDER_SYSTEM_SET";
            }
        }
        LogUtils.log(log, Level.FINE, logMsg, trustStoreProvider);
        return trustStoreProvider;
    }

}
