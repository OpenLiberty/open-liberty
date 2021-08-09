/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.consumer;

import javax.net.ssl.SSLContext;

import org.openid4java.association.AssociationException;
import org.openid4java.association.AssociationSessionType;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.html.HtmlResolver;
import org.openid4java.discovery.xri.XriResolver;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.server.RealmVerifierFactory;
import org.openid4java.util.HttpFetcherFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openid20.OpenidClientConfig;

/**
 * This class get an instance of ConsumerManager which will be used by the OpenidAuthenticatorImpl
 */
public class ConsumerManagerFactory {
    static final TraceComponent tc = Tr.register(ConsumerManagerFactory.class);

    public ConsumerManager consumerManager;

    public ConsumerManagerFactory(ConsumerManager consumerManager) {
        this.consumerManager = consumerManager;
    }

    /**
     * @param openidClientConfig
     * @param sslContext
     * @return
     */
    public ConsumerManager getConsumerManager(OpenidClientConfig openidClientConfig, SSLContext sslContext) {
        HttpFetcherFactory httpFetcherFactory = getHttpFetcherFactory(sslContext, openidClientConfig);
        YadisResolver yadisResolver = new YadisResolver(httpFetcherFactory);
        RealmVerifierFactory realmFactory = new RealmVerifierFactory(yadisResolver);
        HtmlResolver htmlResolver = new HtmlResolver(httpFetcherFactory);
        XriResolver xriResolver = Discovery.getXriResolver();
        Discovery discovery = new Discovery(htmlResolver, yadisResolver, xriResolver);
        consumerManager = createConsumerManager(httpFetcherFactory, realmFactory, discovery, openidClientConfig);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "discover socket time out: " + consumerManager.getDiscovery().getYadisResolver().getHttpFetcher().getRequestOptions().getSocketTimeout());
            Tr.debug(tc, "discover conection time out: " + consumerManager.getDiscovery().getYadisResolver().getHttpFetcher().getRequestOptions().getConnTimeout());
        }
        return consumerManager;
    }

    /**
     * @param httpFetcherFactory
     * @param realmFactory
     * @param discovery
     * @param openidClientConfig
     * @return
     */
    protected ConsumerManager createConsumerManager(HttpFetcherFactory httpFetcherFactory, RealmVerifierFactory realmFactory, Discovery discovery,
                                                    OpenidClientConfig openidClientConfig) {
        ConsumerManager cm = new ConsumerManager(realmFactory, discovery, httpFetcherFactory);
        cm.setSocketTimeout((int) openidClientConfig.getSocketTimeout());
        cm.setConnectTimeout((int) openidClientConfig.getConnectTimeout());
        cm.setAllowStateless(openidClientConfig.getAllowStateless());
        cm.setMaxAssocAttempts(openidClientConfig.getMaxAssociationAttemps());
        AssociationSessionType associationSessionType = null;
        if (openidClientConfig.getMaxAssociationAttemps() > 0) {
            try {
                associationSessionType = AssociationSessionType.create(openidClientConfig.getSessionEncryptionType(), openidClientConfig.getSignatureAlgorithm());
                cm.setPrefAssocSessEnc(associationSessionType);
                cm.setMinAssocSessEnc(associationSessionType);
            } catch (AssociationException e) {
                //                if (!openidClientConfig.getAllowStateless()) {
                //                    Tr.error(tc,  e.getLocalizedMessage());                
                //                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Un-expected exception while performing association type create", e);
                }
            }
        }
        cm.setAssociations(new InMemoryConsumerAssociationStore());
        cm.setNonceVerifier(new InMemoryNonceVerifier((int) openidClientConfig.getNonceValidTime()));
        cm.setMaxNonceAge((int) openidClientConfig.getNonceValidTime());
        cm.setImmediateAuth(openidClientConfig.isCheckImmediate());
        // do not delete the below debug - FAT uses
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isImmediateAuth:" + cm.isImmediateAuth());
        }
        return cm;
    }

    /**
     * @param sslContext
     * @param openidClientConfig
     * @return
     */
    protected HttpFetcherFactory getHttpFetcherFactory(SSLContext sslContext, OpenidClientConfig openidClientConfig) {
        HttpFetcherFactory httpFetcherFactory = null;
        if (openidClientConfig.isHostNameVerificationEnabled()) {
            httpFetcherFactory = new OpenidHttpFetcherFactory(sslContext, openidClientConfig);
        } else {
            httpFetcherFactory = new OpenidHttpFetcherFactory(sslContext, new DefaultHostnameVerifier(), openidClientConfig);
        }
        return httpFetcherFactory;
    }
}
