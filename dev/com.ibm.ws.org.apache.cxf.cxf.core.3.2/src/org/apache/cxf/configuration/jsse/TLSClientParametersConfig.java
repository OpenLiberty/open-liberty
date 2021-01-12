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

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.configuration.security.TLSClientParametersType;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This class provides the TLSClientParameters that programmatically
 * configure a HTTPConduit. It is initialized with the JAXB
 * type TLSClientParametersType that was used in the Spring configuration
 * of the http-conduit bean.
 */
@NoJSR250Annotations
@Trivial  // Liberty change: line is added
public final class TLSClientParametersConfig {
    private static Set<Class<?>> classes;
    private static JAXBContext context;

    private TLSClientParametersConfig() {
        //not constructed
    }

    private static synchronized JAXBContext getContext() throws JAXBException {
        if (context == null || classes == null) {
            Set<Class<?>> c2 = new HashSet<>();
            JAXBContextCache.addPackage(c2,
                                        PackageUtils.getPackageName(TLSClientParametersType.class),
                                        TLSClientParametersConfig.class.getClassLoader());
            CachedContextAndSchemas ccs
                = JAXBContextCache.getCachedContextAndSchemas(c2, null, null, null, false);
            classes = ccs.getClasses();
            context = ccs.getContext();
        }
        return context;
    }

    public static TLSClientParameters createTLSClientParametersFromType(TLSClientParametersType params)
        throws GeneralSecurityException,
               IOException {

        TLSClientParameters ret = new TLSClientParameters();
        boolean usingDefaults = params.isUseHttpsURLConnectionDefaultSslSocketFactory();

        TLSClientParametersTypeInternal iparams = null;
        if (params instanceof TLSClientParametersTypeInternal) {
            iparams = (TLSClientParametersTypeInternal)params;
        }

        if (params.isDisableCNCheck()) {
            ret.setDisableCNCheck(true);
        }
        if (params.isUseHttpsURLConnectionDefaultHostnameVerifier()) {
            ret.setUseHttpsURLConnectionDefaultHostnameVerifier(true);
        }
        if (params.isUseHttpsURLConnectionDefaultSslSocketFactory()) {
            ret.setUseHttpsURLConnectionDefaultSslSocketFactory(true);
        }
        if (params.isSetSecureSocketProtocol()) {
            ret.setSecureSocketProtocol(params.getSecureSocketProtocol());
        }
        if (params.isSetCipherSuitesFilter()) {
            ret.setCipherSuitesFilter(params.getCipherSuitesFilter());
        }
        if (params.isSetCipherSuites()) {
            ret.setCipherSuites(params.getCipherSuites().getCipherSuite());
        }
        if (params.isSetJsseProvider()) {
            ret.setJsseProvider(params.getJsseProvider());
        }
        if (params.isSetSecureRandomParameters() && !usingDefaults) {
            ret.setSecureRandom(
                TLSParameterJaxBUtils.getSecureRandom(
                        params.getSecureRandomParameters()));
        }
        if (params.isSetKeyManagers() && !usingDefaults) {
            if (!params.isSetCertAlias()) {
                ret.setKeyManagers(
                                    TLSParameterJaxBUtils.getKeyManagers(params.getKeyManagers()));
            } else {
                ret.setKeyManagers(
                                    TLSParameterJaxBUtils.getKeyManagers(params.getKeyManagers(),
                                                                         params.getCertAlias()));
            }
        }
        if (params.isSetTrustManagers() && !usingDefaults) {
            ret.setTrustManagers(
                TLSParameterJaxBUtils.getTrustManagers(
                        params.getTrustManagers(), params.isEnableRevocation()));
        }
        if (params.isSetCertConstraints()) {
            ret.setCertConstraints(params.getCertConstraints());
        }
        if (params.isSetSslCacheTimeout()) {
            ret.setSslCacheTimeout(params.getSslCacheTimeout());
        }
        if (params.isSetCertAlias()) {
            ret.setCertAlias(params.getCertAlias());
        }
        if (iparams != null && iparams.isSetKeyManagersRef() && !usingDefaults) {
            ret.setKeyManagers(iparams.getKeyManagersRef());
        }
        if (iparams != null && iparams.isSetTrustManagersRef() && !usingDefaults) {
            ret.setTrustManagers(iparams.getTrustManagersRef());
        }
        return ret;
    }



    public static Object createTLSClientParameters(String s) {

        StringReader reader = new StringReader(s);
        XMLStreamReader data = StaxUtils.createXMLStreamReader(reader);
        try {
            JAXBElement<TLSClientParametersType> type = JAXBUtils.unmarshall(getContext(),
                                                                             data,
                                                                             TLSClientParametersType.class);
            TLSClientParametersType cpt = type.getValue();
            return createTLSClientParametersFromType(cpt);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                StaxUtils.close(data);
            } catch (XMLStreamException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class TLSClientParametersTypeInternal extends TLSClientParametersType {
        private KeyManager[] keyManagersRef;
        private TrustManager[] trustManagersRef;

        public KeyManager[] getKeyManagersRef() {
            return keyManagersRef;
        }

        public void setKeyManagersRef(KeyManager[] keyManagersRef) {
            this.keyManagersRef = keyManagersRef;
        }

        public boolean isSetKeyManagersRef() {
            return this.keyManagersRef != null;
        }

        public TrustManager[] getTrustManagersRef() {
            return trustManagersRef;
        }

        public void setTrustManagersRef(TrustManager[] trustManagersRef) {
            this.trustManagersRef = trustManagersRef;
        }

        public boolean isSetTrustManagersRef() {
            return this.trustManagersRef != null;
        }

    }
}
