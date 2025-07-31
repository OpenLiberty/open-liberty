/*******************************************************************************
 * Copyright (c) 1997, 2025 IBM Corporation and others.
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

package com.ibm.ws.ssl.provider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.ssl.JSSEProviderFactory;

/**
 * JSSE Provider wrapper for the Sun JDK.
 * <p>
 * This is the SunJSSE JSSEProvider implementation used for the pluggable client.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class SunJSSEProvider extends AbstractJSSEProvider implements JSSEProvider {
    private static TraceComponent tc = Tr.register(SunJSSEProvider.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    /**
     * Constructor.
     */
    public SunJSSEProvider() {
        super();
        String protocol = Constants.PROTOCOL_SSL;
        if (CryptoUtils.isFips140_3Enabled() && CryptoUtils.isSemeruFips()) {
            protocol = Constants.PROTOCOL_TLS;
        }

        initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.SUNJSSE_NAME, null,
                   "com.sun.net.ssl.internal.ssl.SSLSocketFactoryImpl", null, protocol);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created a Sun JSSE provider with protocol " + protocol);
        }
    }

}
