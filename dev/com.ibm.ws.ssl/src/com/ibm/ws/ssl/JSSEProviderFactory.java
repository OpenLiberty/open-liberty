/*******************************************************************************
 * Copyright (c) 1997, 2024 IBM Corporation and others.
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

package com.ibm.ws.ssl;

import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Provider;
import java.security.Security;
import java.util.Hashtable;

import javax.net.ssl.SSLContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.ws.ssl.provider.IBMJSSEProvider;
import com.ibm.ws.ssl.provider.SunJSSEProvider;

/**
 * Factory that creates and caches JSSEProvider objects.
 * <p>
 * This is the factory class that selects the currently active JSSEProvider. For v7, mostly IBMJSSE2 is used, but for the pluggable client it could be SunJSSE.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class JSSEProviderFactory {
    protected static final TraceComponent tc = Tr.register(JSSEProviderFactory.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static JSSEProvider defaultProvider = null;
    private static final Hashtable<String, JSSEProvider> providerCache = new Hashtable<String, JSSEProvider>();
    private static String trustManagerFactoryAlgorithm = null;
    private static String keyManagerFactoryAlgorithm = null;
    private static String defaultSSLSocketFactory = null;
    private static String defaultSSLServerSocketFactory = null;
    private static boolean fipsInitialized = false;

    private static String providerFromProviderList = null;

    /**
     * Access the default provider.
     *
     * @return JSSEProvider
     */
    public static JSSEProvider getInstance() {
        return getInstance(null);
    }

    /**
     * Access the provider for the given name. This will return null if no match
     * was found.
     *
     * @param inputProvider
     * @return JSSEProvider
     */
    public static synchronized JSSEProvider getInstance(String inputProvider) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getInstance: " + inputProvider);

        String contextProvider = inputProvider;
        if (contextProvider == null) {
            if (null != defaultProvider) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getInstance: " + defaultProvider);
                return defaultProvider;
            }
            contextProvider = getProviderFromProviderList();
        }

        // if still null, revert to default as usual.
        if (contextProvider == null) {
            contextProvider = Constants.IBMJSSE2_NAME;
        }

        JSSEProvider cachedProvider = providerCache.get(contextProvider);

        if (cachedProvider != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getInstance(cached) " + cachedProvider);
            return cachedProvider;
        }

        final String contextProviderPriv = contextProvider;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                Provider provider = Security.getProvider(contextProviderPriv);

                if (provider == null) {
                    try {
                        if (contextProviderPriv.equalsIgnoreCase(Constants.IBMJSSE_NAME)) {
                            provider = (Provider) Class.forName(Constants.IBMJSSE2).newInstance();
                        } else if (contextProviderPriv.equalsIgnoreCase(Constants.SUNJSSE_NAME)) {
                            provider = (Provider) Class.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance();
                        } else {
                            provider = (Provider) Class.forName(Constants.IBMJSSE2).newInstance();
                        }
                        if (provider != null) {
                            Security.addProvider(provider);
                        }
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Exception loading provider: " + contextProviderPriv + "; " + e);
                    }
                }
                return null;
            }
        });

        Provider[] providerList = Security.getProviders();

        for (int i = 0; i < providerList.length && null == cachedProvider; i++) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Provider name [" + i + "]: " + providerList[i].getName());

            if (providerList[i].getName().equalsIgnoreCase(contextProvider)) {
                if (contextProvider.equalsIgnoreCase(Constants.IBMJSSE2_NAME)) {
                    cachedProvider = new IBMJSSEProvider();
                    providerCache.put(Constants.IBMJSSE2_NAME, cachedProvider);
                    providerCache.put(contextProvider, cachedProvider);
                } else if (contextProvider.equalsIgnoreCase(Constants.IBMJSSE_NAME)) {
                    cachedProvider = new IBMJSSEProvider();
                    providerCache.put(Constants.IBMJSSE_NAME, cachedProvider);
                    providerCache.put(contextProvider, cachedProvider);
                } else if (contextProvider.equalsIgnoreCase(Constants.SUNJSSE_NAME)) {
                    cachedProvider = new SunJSSEProvider();
                    providerCache.put(Constants.SUNJSSE_NAME, cachedProvider);
                    providerCache.put(contextProvider, cachedProvider);
                } else {
                    cachedProvider = new IBMJSSEProvider();
                    providerCache.put(Constants.IBMJSSE_NAME, cachedProvider);
                    providerCache.put(contextProvider, cachedProvider);
                }
            }
        } // end-provider-loop

        // if still null, default to IBM JSSE provider
        if (cachedProvider == null) {
            cachedProvider = new IBMJSSEProvider();
            providerCache.put(Constants.IBMJSSE_NAME, cachedProvider);
            providerCache.put(contextProvider, cachedProvider);
        }

        if (null == inputProvider) {
            defaultProvider = cachedProvider;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getInstance: " + cachedProvider);
        return cachedProvider;
    }

    private static boolean validateProvider(final String provider) {
        boolean success = true;

        try {
            try {
                final String protocol = "SSL";

                AccessController.doPrivileged(new PrivilegedExceptionAction<SSLContext>() {
                    @Override
                    public SSLContext run() throws NoSuchAlgorithmException, NoSuchProviderException {
                        return SSLContext.getInstance(protocol, provider);
                    }
                });
            } catch (PrivilegedActionException e) {
                Exception ex = e.getException();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Error validating provider: " + provider + ", Exception: " + ex.getMessage(), new Object[] { ex });
                success = false;
            }
        } catch (Throwable e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Error validating provider: " + provider + ", Exception: " + e.getMessage(), new Object[] { e });
            success = false;
        }

        return success;
    }

    /**
     * Get the default SSLSocketFactory from Security.
     *
     * @return String
     */
    public static String getDefaultSSLSocketFactory() {
        if (defaultSSLSocketFactory == null) {
            defaultSSLSocketFactory = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return Security.getProperty("ssl.SocketFactory.provider");
                }
            });
        }

        return defaultSSLSocketFactory;
    }

    /**
     * Get the default SSLServerSocketFactory class from Security.
     *
     * @return String
     */
    public static String getDefaultSSLServerSocketFactory() {
        if (defaultSSLServerSocketFactory == null) {
            defaultSSLServerSocketFactory = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return Security.getProperty("ssl.ServerSocketFactory.provider");
                }
            });
        }

        return defaultSSLServerSocketFactory;
    }

    /**
     * Get the key manager factory algorithm default from Security.
     *
     * @return String
     */
    public static String getKeyManagerFactoryAlgorithm() {
        if (keyManagerFactoryAlgorithm == null) {
            keyManagerFactoryAlgorithm = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return Security.getProperty("ssl.KeyManagerFactory.algorithm");
                }
            });
        }

        return keyManagerFactoryAlgorithm;
    }

    /**
     * Get the trust manager factory algorithm default from Security.
     *
     * @return String
     */
    public static String getTrustManagerFactoryAlgorithm() {
        if (trustManagerFactoryAlgorithm == null) {
            trustManagerFactoryAlgorithm = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return Security.getProperty("ssl.TrustManagerFactory.algorithm");
                }
            });
        }

        return trustManagerFactoryAlgorithm;
    }

    /**
     * Initialize the IBM CMS provider.
     *
     * @throws Exception
     */
    public static void initializeIBMCMSProvider() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "initializeIBMCMSProvider");

        Provider provider = Security.getProvider(Constants.IBMCMS_NAME);

        if (provider != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "initializeIBMCMSProvider (already present)");
            return;
        }

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    Provider cmsprovider = (Provider) Class.forName(Constants.IBMCMS).newInstance();

                    if (cmsprovider != null) {
                        Security.addProvider(cmsprovider);
                    }
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Exception loading provider: " + Constants.IBMCMS);
                }

                return null;
            }
        });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "initializeIBMCMSProvider (provider initialized)");
    }

    /**
     * Initialize FIPS.
     *
     * @throws Exception
     */
    public static void initializeFips() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "initializeFips");

//        if (!fipsInitialized && !CryptoUtils.isFips140_3Enabled()) {
//            int ibmjcefips_position = 0;
//            int ibmjceplusfips_position = 0;
//            Provider[] provider_list = null;
//            Provider ibmjcefips = null;
//            Provider ibmjceplusfips = null;
//            Provider sun = null;
//
//            try {
//                System.setProperty("com.ibm.jsse2.JSSEFIPS", "true");
//                provider_list = Security.getProviders();
//
//                for (int i = 0; i < provider_list.length; i++) {
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                        Tr.debug(tc, "Provider[" + i + "]: " + provider_list[i].getName());
//                    if (provider_list[i].getName().equals("IBMJCEPlusFIPS")) {
//                        ibmjceplusfips_position = i;
//                        ibmjceplusfips = provider_list[i];
//                    } else if (provider_list[i].getName().equals("IBMJCEFIPS")) {
//                        ibmjcefips_position = i;
//                        ibmjcefips = provider_list[i];
//                    } else if (provider_list[i].getName().equals("SUN")) {
//                        sun = provider_list[i];
//                    }
//                }
//
//                if (ibmjceplusfips == null) {
//                    provider_list = Security.getProviders();
//
//                    try {
//                        ibmjceplusfips = (Provider) Class.forName(Constants.IBMJCEPlusFIPS).newInstance();
//
//                        if (sun != null) {
//                            insertProviderAt(sun, 1);
//                            insertProviderAt(ibmjceplusfips, 2);
//                        } else {
//                            insertProviderAt(ibmjceplusfips, 1);
//                        }
//                    } catch (Exception e) {
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                            Tr.debug(tc, "Exception loading provider: " + Constants.IBMJCEFIPS + "; " + e);
//                    }
//                } else if (ibmjceplusfips_position != 0) {
//                    // it's there but not the first, let's reorder it.
//                    provider_list = Security.getProviders();
//
//                    if (sun != null) {
//                        insertProviderAt(sun, 1);
//                        insertProviderAt(ibmjceplusfips, 2);
//                    } else {
//                        insertProviderAt(ibmjceplusfips, 1);
//                    }
//
//                } else if (ibmjcefips == null) {
//                    provider_list = Security.getProviders();
//
//                    try {
//                        ibmjcefips = (Provider) Class.forName(Constants.IBMJCEFIPS).newInstance();
//
//                        if (sun != null) {
//                            insertProviderAt(sun, 1);
//                            insertProviderAt(ibmjcefips, 2);
//                        } else {
//                            insertProviderAt(ibmjcefips, 1);
//                        }
//                    } catch (Exception e) {
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                            Tr.debug(tc, "Exception loading provider: " + Constants.IBMJCEFIPS + "; " + e);
//                    }
//                } else if (ibmjcefips_position != 0) {
//                    // it's there but not the first, let's reorder it.
//                    provider_list = Security.getProviders();
//
//                    if (sun != null) {
//                        insertProviderAt(sun, 1);
//                        insertProviderAt(ibmjcefips, 2);
//                    } else {
//                        insertProviderAt(ibmjcefips, 1);
//                    }
//                }
//
//                provider_list = Security.getProviders();
//
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    for (int i = 0; i < provider_list.length; i++) {
//                        Tr.debug(tc, "Provider[" + i + "]: " + provider_list[i].getName() + ", info: " + provider_list[i].getInfo());
//                    }
//                }
//
//                fipsInitialized = true;
//            } catch (Exception e) {
//                Tr.error(tc, "security.addprovider.error", new Object[] { e });
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                    Tr.debug(tc, "Exception caught adding IBMJCEFIPS provider.", new Object[] { e });
//                throw e;
//            }
//        } else if (!fipsInitialized) {
//                fipsInitialized = true;
//                Provider[] provider_list = Security.getProviders();
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    for (int i = 0; i < provider_list.length; i++) {
//                        Tr.debug(tc, "Provider[" + i + "]: " + provider_list[i].getName() + ", info: " + provider_list[i].getInfo());
//                    }
//                }
//        }
        if (!fipsInitialized) {
            fipsInitialized = true;
            Provider[] provider_list = Security.getProviders();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                for (int i = 0; i < provider_list.length; i++) {
                    Tr.debug(tc, "Provider[" + i + "]: " + provider_list[i].getName() + ", info: " + provider_list[i].getInfo());
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "initializeFips");
    }

    /**
     * Insert a provider into Security at the provided slot number.
     *
     * @param newProvider
     * @param slot
     */
    public static void insertProviderAt(Provider newProvider, int slot) {
        Provider[] provider_list = Security.getProviders();
        if (null == provider_list || 0 == provider_list.length) {
            return;
        }

        // add the new provider to the new list at the correct slot #.
        Provider[] newList = new Provider[provider_list.length + 2];
        newList[slot] = newProvider;

        int newListIndex = 1;
        // add the rest of the providers
        for (int i = 0; i < provider_list.length; i++) {
            Provider currentProvider = provider_list[i];

            if (currentProvider != null && !currentProvider.getName().equals(newProvider.getName())) {
                // keep incrementing until we find the first available slot.
                while (newList[newListIndex] != null) {
                    newListIndex++;
                }

                newList[newListIndex] = currentProvider;
                newListIndex++;
            }
        }

        removeAllProviders();

        // add the rest of the providers to the list.
        for (int i = 0; i < newList.length; i++) {
            Provider currentProvider = newList[i];

            if (currentProvider != null) {
                int position = Security.insertProviderAt(currentProvider, (i + 1));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, currentProvider.getName() + " provider added at position " + position);
            }
        }
    }

    /**
     * Remove all providers from Security.
     */
    public static void removeAllProviders() {
        Provider[] provider_list = Security.getProviders();

        for (int i = 0; i < provider_list.length; i++) {
            if (provider_list[i] != null) {
                String name = provider_list[i].getName();
                if (name != null) {
                    Security.removeProvider(name);
                }
            }
        }
    }

    // public static List<String> fipsJCEProviders()
    // {
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    // Tr.entry(tc, "fipsJCEProviders");
    // String [] fipsJCEProvidersList = { "IBMJCEFIPS" };
    //
    // if (fipsJCEProvidersObjectList == null)
    // {
    //
    // fipsJCEProvidersObjectList = new
    // ArrayList<String>(fipsJCEProvidersList.length);
    //
    // if (isFipsEnabled())
    // {
    // for (int i=0; i<fipsJCEProvidersList.length; i++ ) {
    // fipsJCEProvidersObjectList.add(fipsJCEProvidersList[i]);
    // }
    // }
    // }
    //
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    // Tr.exit(tc, "fipsJCEProviders: " + fipsJCEProvidersObjectList);
    // return fipsJCEProvidersObjectList;
    // }
    //
    // public static List<String> fipsJSSEProviders()
    // {
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    // Tr.entry(tc, "fipsJSSEProviders");
    // String [] fipsJSSEProvidersList = { "IBMJSSE2" };
    //
    // if (fipsJSSEProvidersObjectList == null)
    // {
    //
    // fipsJSSEProvidersObjectList = new
    // ArrayList<String>(fipsJSSEProvidersList.length);
    //
    // if (isFipsEnabled())
    // {
    // for (int i=0; i<fipsJSSEProvidersList.length; i++ ) {
    // fipsJSSEProvidersObjectList.add(fipsJSSEProvidersList[i]);
    // }
    // }
    // }
    //
    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    // Tr.exit(tc, "fipsJCEProviders: " + fipsJSSEProvidersObjectList);
    // return fipsJSSEProvidersObjectList;
    // }

    private static String getProviderFromProviderList() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getProviderFromProviderList");

        Provider[] providerList = Security.getProviders();

        for (int i = 0; i < providerList.length; i++) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Provider name [" + i + "]: " + providerList[i].getName());
            if (providerList[i].getName().equalsIgnoreCase(Constants.IBMJCEPlusFIPS_NAME)) {
                providerFromProviderList = Constants.IBMJCEPlusFIPS_NAME;
                break;
            } else if (providerList[i].getName().equalsIgnoreCase(Constants.IBMJSSE2_NAME)) {
                providerFromProviderList = Constants.IBMJSSE2_NAME;
                break;
            } else if (providerList[i].getName().equalsIgnoreCase(Constants.IBMJSSE_NAME)) {
                providerFromProviderList = Constants.IBMJSSE_NAME;
                break;
            } else if (providerList[i].getName().equalsIgnoreCase(Constants.SUNJSSE_NAME)) {
                providerFromProviderList = Constants.SUNJSSE_NAME;
                break;
            }
        }

        if (providerFromProviderList == null) {
            providerFromProviderList = Constants.IBMJSSE2_NAME;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getProviderFromProviderList -> " + providerFromProviderList);
        return providerFromProviderList;
    }
}
