/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Security;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.ssl.JSSEProviderFactory;

/**
 * <p>
 * This class represents a configured crytographic token device.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public final class WSPKCSInKeyStore {
    protected static final TraceComponent tc = Tr.register(WSPKCSInKeyStore.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static String pkcsType_ibm = "PKCS11IMPLKS";
    private static String pkcsType_oracle = "PKCS11";

    private static final String IBM_PKCS11_PROVIDER_CLASS_NAME = "com.ibm.crypto.pkcs11impl.provider.IBMPKCS11Impl";
    private static final String ORACLE_PKCS11_PROVIDER_CLASS_NAME = "sun.security.pkcs11.SunPKCS11";
    private static final String IBMPKCS11_PROVIDER_NAME = "IBMPKCS11";
    private static final String IBMPKCS11Impl_PROVIDER_NAME = "IBMPKCS11Impl";
    private static final String SUNPKCS11_PROVIDER_NAME = "SunPKCS11";

    private String pkcsProvider = IBMPKCS11Impl_PROVIDER_NAME;
    private String pkcsProviderClass = IBM_PKCS11_PROVIDER_CLASS_NAME;
    private String pkcsType = pkcsType_ibm;
    private Provider hwProvider = null;
    private KeyManagerFactory kmf;
    private KeyStore ks;
    private TrustManagerFactory tmf;
    private KeyStore ts;
    private JSSEProvider jsseProvider = null;

    private String tokenLib_key;
    private String tokenType_key;
    private String tokenLib_trust;
    private String tokenType_trust;

    // private Stack providerInstancePool = new Stack();
    private final int noOfProvidersCreated = 0;
    private BufferedReader fileReader = null;
    private final StringBuilder tokenConfigBuffer = new StringBuilder();
    private String nameAttribute = null;

    /**
     * Constructor.
     *
     * @param tokenConfigName
     * @throws Exception
     */
    public WSPKCSInKeyStore(String tokenConfigName, String ksProvider) throws Exception {
        if (ksProvider.equals(SUNPKCS11_PROVIDER_NAME)) {
            pkcsType = pkcsType_oracle;
            pkcsProvider = SUNPKCS11_PROVIDER_NAME;
            pkcsProviderClass = ORACLE_PKCS11_PROVIDER_CLASS_NAME;
        }

        initializePKCS11ImplProvider(tokenConfigName);
    }

    public void asKeyStore(String tokenType, String tokenlib, String tokenPwd) throws Exception {

        jsseProvider = JSSEProviderFactory.getInstance();

        try {
            if (tokenLib_key != null && tokenLib_key.compareToIgnoreCase(tokenlib) == 0 && ks != null) {
                // initialized already, use the initialized one
                return; // already initialized
            }

            if (tokenLib_trust != null && tokenlib.compareTo(tokenLib_trust) == 0 && ts != null) {
                kmf = jsseProvider.getKeyManagerFactoryInstance();
                ks = ts;
                kmf.init(ts, tokenPwd.toCharArray());
            } else {
                kmf = jsseProvider.getKeyManagerFactoryInstance();
                ks = KeyStore.getInstance(pkcsType, hwProvider.getName());
                // no need to load the keystore if it is used for pure acceleration
                // purpose
                ks.load(null, tokenPwd.toCharArray());
                kmf.init(ks, tokenPwd.toCharArray());
            }

            tokenLib_key = tokenlib;
            tokenType_key = tokenType;
        } catch (Exception e) {
            // log error
            kmf = null;
            ks = null;
            tokenLib_key = null;
            tokenType_key = null;
            throw e;
            // indicates PKCS library has already been initialized;Non-fatal error
            // per Anthony Nadalin.
            // not a good way to solve a problem, but Tivoli does not provide a better
            // way.
            // we will return for now.
        }
    }

    public void asTrustStore(String tokenType, String tokenlib, String tokenPwd) throws Exception {
        jsseProvider = JSSEProviderFactory.getInstance();

        try {
            if (tokenLib_trust != null && tokenLib_trust.compareToIgnoreCase(tokenlib) == 0 && ts != null) {
                // initialized already, use the initialized one
                return;
            }

            if (tokenLib_key != null && tokenlib.compareTo(tokenLib_key) == 0 && ks != null) {
                tmf = jsseProvider.getTrustManagerFactoryInstance();
                ts = ks;
                tmf.init(ks);
            } else {
                tmf = jsseProvider.getTrustManagerFactoryInstance();
                ts = KeyStore.getInstance(pkcsType, hwProvider.getName());
                ts.load(null, tokenPwd.toCharArray());
                tmf.init(ts);
            }

            tokenLib_trust = tokenlib;
            tokenType_trust = tokenType;

        } catch (Exception e) {
            // log error
            tmf = null;
            ts = null;
            tokenLib_trust = null;
            tokenType_trust = null;
            throw e;
            // indicates PKCS library has already been initialized;Non-fatal error
            // per Anthony Nadalin.
            // not a good way to solve a problem, but Tivoli does not provide a better
            // way.
            // we will return for now.
        }
    }

    public KeyManagerFactory getKMF() {
        return this.kmf;
    }

    public KeyStore getKS() {
        return this.ks;
    }

    public TrustManagerFactory getTMF() {
        return this.tmf;
    }

    public KeyStore getTS() {
        return this.ts;
    }

    public String getlibName_key() {
        return this.tokenLib_key;
    }

    public String getlibName_trust() {
        return this.tokenLib_trust;
    }

    public String gettokType_key() {
        return this.tokenType_key;
    }

    public String gettokType_trust() {
        return this.tokenType_trust;
    }

    /*
     * Initalizes the pkcs11 provider and add it to the Security provider list
     */
    public void initializePKCS11ImplProvider(String tokenConfigName) throws Exception {
        final String configFile = tokenConfigName;
        try {
            hwProvider = AccessController.doPrivileged(new PrivilegedExceptionAction<Provider>() {
                @Override
                public Provider run() throws Exception {
                    Provider p = createPKCS11Provider(configFile);
                    // if no provider found throw ProviderException
                    if (p == null) {
                        throw new ProviderException("No PKCS11 provider available.");
                    }
                    Security.addProvider(p);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "The provider: " + p + "is added at the end of the provider list");
                    return p;
                }
            });
        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Cannot initialize IBMPKCS11Impl provider: " + ex);
            FFDCFilter.processException(ex, getClass().getName(), "initializePKCS11ImplProvider");
            throw ex;
        }
    }

    /*
     * Create the pkcs11 provider and config file that will be loaded in the provider list
     */
    private Provider createPKCS11Provider(String configFileName) throws Exception {

        Provider provider = null;
        if (JavaInfo.majorVersion() >= 9) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The java version is 9 or higher so take new path to setup the pkcs11 provider.");
            provider = getProvider(configFileName);
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Calling legacy method to setup the pkcs11 provider.");
            provider = getProviderLegacy(configFileName);
        }
        return provider;
    }

    private Provider getProviderLegacy(String configFileName) throws Exception {
        Provider provider = null;
        Class pkcs11ProviderClass = Class.forName(pkcsProviderClass);
        if (pkcs11ProviderClass != null) {
            if (configFileName != null && configFileName.isEmpty() == false) {
                Constructor constructor = pkcs11ProviderClass.getDeclaredConstructor(new Class[] { java.lang.String.class });
                provider = (Provider) constructor.newInstance(new Object[] { configFileName });
            } else {
                provider = (Provider) pkcs11ProviderClass.newInstance();
            }
        }
        return provider;
    }

    private Provider getProvider(String configFileName) throws Exception {
        ServiceLoader sl = ServiceLoader.load(java.security.Provider.class);
        Iterator<Provider> iter = sl.iterator();
        Provider p = null;

        while (iter.hasNext()) {
            p = iter.next();
            if (p.getName().equals(pkcsProvider)) {
                Method configure = p.getClass().getMethod("configure", java.lang.String.class);
                configure.invoke(p, configFileName);
                break;
            }
        }
        return p;
    }

    private BufferedReader convertFileToBuffer(String tokenConfigName) throws Exception {

        StringBuilder sb = new StringBuilder();
        String inLine = null;
        try {
            if (fileReader == null) {
                fileReader = new BufferedReader(new FileReader(tokenConfigName));
                try {
                    while ((inLine = fileReader.readLine()) != null) {
                        String str = inLine.trim();
                        if (str.startsWith("name")) {
                            // save the name attribute of the token configuration
                            nameAttribute = str;
                        } else {
                            // Save the token configuration except name attribute
                            tokenConfigBuffer.append(str).append(LINE_SEPARATOR);
                        }
                    }
                } catch (IOException ioe) {
                    FFDCFilter.processException(ioe, getClass().getName(), "convertFileToBuffer");
                    throw ioe;
                } finally {
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException ioe) {
                            FFDCFilter.processException(ioe, getClass().getName(), "convertFileToBuffer");
                            throw ioe;
                        }
                    }
                }
            }
            // modify the name attribute to be a unique name
            sb.append(nameAttribute).append(noOfProvidersCreated).append(LINE_SEPARATOR).append(tokenConfigBuffer);
        } catch (FileNotFoundException fne) {
            FFDCFilter.processException(fne, getClass().getName(), "convertFileToBuffer");
            throw fne;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Name attribute and other card related info: " + nameAttribute + ":" + tokenConfigBuffer.toString());
        return new BufferedReader(new StringReader(sb.toString()));
    }

}
