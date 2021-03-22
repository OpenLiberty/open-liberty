/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;

public abstract class PkixTrustEngineConfig {
    public TraceComponent tcCommon = Tr.register(PkixTrustEngineConfig.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);;
    private final static String UTF8 = "UTF-8";
    public static final Object KEY_PROVIDER_ID = "id";
    protected String commonProviderId = null;
    private ConfigurationAdmin commonConfigAdmin = null;

    public static final String KEY_trustedIssuers = "trustedIssuers";
    public static final String KEY_pkixTrustEngine = "pkixTrustEngine";
    public static final String KEY_trustEngine_x509cert = "x509Certificate";
    public static final String KEY_trustEngine_crl = "crl";
    public static final String KEY_trustEngine_trustAnchor = "trustAnchor";
    public static final String KEY_trustEngine_path = "path";

    //Trust Engine Metadata
    protected List<String> pkixX509List = Collections.synchronizedList(new ArrayList<String>());
    protected List<String> pkixCrlList = Collections.synchronizedList(new ArrayList<String>());
    protected String trustAnchorName = null;
    protected String[] trustedIssuers = null;
    protected boolean isPkixTrustEngineEnabled = false;
    private String bundleLocation;

    /**
     * @param props
     * @throws SamlException
     */
    protected void processPkixTrustEngine(Map<String, Object> props,
                                          ConfigurationAdmin configAdmin, String bundleLocation) throws Exception {
        commonProviderId = (String) props.get(KEY_PROVIDER_ID);
        this.commonConfigAdmin = configAdmin;
        this.bundleLocation = bundleLocation;

        // reset stored pkix data
        pkixX509List = Collections.synchronizedList(new ArrayList<String>());
        pkixCrlList = Collections.synchronizedList(new ArrayList<String>());
        isPkixTrustEngineEnabled = false;

        String[] engines = (String[]) props.get(KEY_pkixTrustEngine);
        String pkixTrustEngine = engines != null && engines.length > 0 ? engines[0] : null;

        if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
            Tr.debug(tcCommon, "pkixTrustEngine pid:" + pkixTrustEngine +
                               (engines != null ? "  size:" + engines.length : "  null"));
        }
        if (pkixTrustEngine == null || pkixTrustEngine.isEmpty())
            return;

        isPkixTrustEngineEnabled = true;

        processPkixTrustEngineData(pkixTrustEngine);
    }

    /**
     * @param string
     * @throws SamlException
     */
    public void processPkixTrustEngineData(String trustEngine) {
        Configuration config = null;
        try {
            config = commonConfigAdmin.getConfiguration(trustEngine, bundleLocation);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                Tr.debug(tcCommon, "Invalid saml websso trust engine configuration", trustEngine);
            }
            return;
        }
        Dictionary<String, Object> trustEngineProps = config.getProperties();
        if (trustEngineProps == null) {
            // Somehow at some dynamic updating situations, this could be null
            // No further handling needed
            return;
        }
        trustAnchorName = (String) trustEngineProps.get(KEY_trustEngine_trustAnchor);
        if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
            Tr.debug(tcCommon, "trustAnchor = " + trustAnchorName);
        }

        String[] certs = (String[]) trustEngineProps.get(KEY_trustEngine_x509cert);
        if (certs == null || certs.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                Tr.debug(tcCommon, "No X509Certificates were defined in the trust engine configuration. ");
            }
        } else {
            for (String certPid : certs) {
                //pids.add(certPid); //TODO
                Configuration certConfig = null;
                try {
                    certConfig = commonConfigAdmin.getConfiguration(certPid, bundleLocation);
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                        Tr.debug(tcCommon, "Invalid X509 Certificate configuration", certPid);
                    }
                    continue;
                }
                if (certConfig == null || certConfig.getProperties() == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                        Tr.debug(tcCommon, "NULL X509 Certificate configuration", certPid);
                    }
                    continue;
                }
                String certPath = (String) certConfig.getProperties().get(KEY_trustEngine_path);
                pkixX509List.add(certPath);

                if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                    Tr.debug(tcCommon, "Added x509 cert path: " + certPath);
                }
            }
        }

        String[] crls = (String[]) trustEngineProps.get(KEY_trustEngine_crl);
        if (crls == null || crls.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                Tr.debug(tcCommon, "No CRLs were defined in the trust engine configuration. ");
            }
        } else {
            for (String crlPid : crls) {
                //pids.add(crlPid); //TODO
                Configuration crlConfig = null;
                try {
                    crlConfig = commonConfigAdmin.getConfiguration(crlPid, bundleLocation);
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                        Tr.debug(tcCommon, "Invalid CRL configuration", crlPid);
                    }
                    continue;
                }
                if (crlConfig == null || crlConfig.getProperties() == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                        Tr.debug(tcCommon, "NULL CRL configuration", crlPid);
                    }
                    continue;
                }
                String crlPath = (String) crlConfig.getProperties().get(KEY_trustEngine_path);
                pkixCrlList.add(crlPath);

                if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                    Tr.debug(tcCommon, "Added crl path: " + crlPath);
                }
            }
        }

        trustedIssuers = trim((String[]) trustEngineProps.get(KEY_trustedIssuers));
        if (trustedIssuers != null) {
            for (int iI = 0; iI < trustedIssuers.length; iI++) {
                try {
                    trustedIssuers[iI] = URLDecoder.decode(trustedIssuers[iI], UTF8);
                    if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                        Tr.debug(tcCommon, "trustedIssuer[" + iI + "] = " + trustedIssuers[iI]);
                    }
                } catch (UnsupportedEncodingException e) {
                    // TODO
                    // This should not happen since the encoding is UTF-8
                    if (TraceComponent.isAnyTracingEnabled() && tcCommon.isDebugEnabled()) {
                        Tr.debug(tcCommon, "get an unexected Exception:" + e);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        String result = "\nproviderId:" + commonProviderId
                        + "\ntrustedIssuers:" + (trustedIssuers == null ? "null" : trustedIssuers.length)
                        + (!isPkixTrustEngineEnabled ? ";" : "\npkixTrustEngine enabled"
                                                             + "\nx509 cert list:" + pkixX509List.toString()
                                                             + "\ncrl list:" + pkixCrlList.toString());
        if (tcCommon.isDebugEnabled()) {
            Tr.debug(tcCommon, result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getCRLList()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<X509CRL> getX509Crls() {
        Collection<X509CRL> crls = new ArrayList<X509CRL>();
        InputStream inStream = null;
        try {
            Iterator<String> it = pkixCrlList.iterator();
            while (it.hasNext()) {
                final String crlPath = it.next();
                //inStream = new FileInputStream(crlPath);
                try {
                    inStream = (InputStream) java.security.AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws FileNotFoundException {
                            return new FileInputStream(crlPath);

                        }
                    });

                } catch (PrivilegedActionException pae) {
                    FileNotFoundException e = (FileNotFoundException) pae.getException();
                    throw e;
                }
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509CRL crl = (X509CRL) cf.generateCRL(inStream);
                if (crl != null) {
                    crls.add(crl);
                }
            }
        } catch (FileNotFoundException e) {

        } catch (CRLException e) {

        } catch (CertificateException e) {

        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
;
                }
            }
        }
        return crls;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addX509Certs(Collection<X509Certificate> trustAnchors) {

        InputStream inStream = null;
        try {
            Iterator<String> it = pkixX509List.iterator();
            while (it.hasNext()) {
                final String certPath = it.next();
                //inStream = new FileInputStream(certPath);
                try {
                    inStream = (InputStream) java.security.AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws FileNotFoundException {
                            return new FileInputStream(certPath);

                        }
                    });

                } catch (PrivilegedActionException pae) {
                    FileNotFoundException e = (FileNotFoundException) pae.getException();
                    throw e;
                }
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
                if (cert != null) {
                    trustAnchors.add(cert);
                }
            }

        } catch (FileNotFoundException e) {

        } catch (CertificateException e) {

        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {

                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getTrustAnchor()
     */
    public abstract Collection<X509Certificate> getPkixTrustAnchors();

    public String[] trim(final String[] originals) {
        if (originals == null || originals.length == 0)
            return null;
        String[] tmpResults = new String[originals.length];
        int iCnt = 0;
        for (int iI = 0; iI < originals.length; iI++) {
            String original = trim(originals[iI]);
            if (original != null)
                tmpResults[iCnt++] = original;
        }
        if (iCnt == 0)
            return null;
        String[] results = new String[iCnt];
        System.arraycopy(tmpResults, 0, results, 0, iCnt);
        return results;
    }

    public String trim(String original) {
        if (original == null)
            return null;
        String result = original.trim();
        if (result.isEmpty())
            return null;
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getTrustedIssuers()
     */
    public String[] getPkixTrustedIssuers() {
        return trustedIssuers == null ? null : trustedIssuers.clone();
    }

    protected void unexpectedCall() {
        Exception exception = new Exception("unexpected call void");
        if (tcCommon.isDebugEnabled()) {
            Tr.debug(tcCommon, "\nThe method is not implemented but get Called\n" +
                               SamlUtil.dumpStackTrace(exception, 8));
        } ;
    }

    protected <T> T unexpectedCall(T value) {
        Exception exception = new Exception("unexpected call:" + value);
        if (tcCommon.isDebugEnabled()) {
            Tr.debug(tcCommon, "\nThe method is not implemented but get Called\n" +
                               SamlUtil.dumpStackTrace(exception, 8));
        } ;
        return value;
    }

}
