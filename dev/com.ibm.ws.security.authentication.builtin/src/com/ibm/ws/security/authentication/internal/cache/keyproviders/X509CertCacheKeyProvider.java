/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.internal.TraceConstants;
import com.ibm.ws.security.authentication.jaas.modules.CertificateLoginModule;

/**
 *
 */
public class X509CertCacheKeyProvider implements CacheKeyProvider {

    private static final TraceComponent tc = Tr.register(X509CertCacheKeyProvider.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @Override
    public Object provideKey(CacheContext cacheContext) {
        int certHash;
        Boolean isCollectiveCert = CertificateLoginModule.collectiveCertificate.get();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "collectiveCertificate=" + isCollectiveCert);
        if (isCollectiveCert == null || isCollectiveCert == false) {
            final java.security.cert.X509Certificate[] cert_chain = (java.security.cert.X509Certificate[]) cacheContext.getCertChain();
            if (cert_chain != null) {
                certHash = ((java.security.cert.Certificate) cert_chain[0]).hashCode();
                return certHash;
            }
        }
        return null;
    }

}
