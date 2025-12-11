/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
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
 * JSSE provider for an IBM JDK.
 * <p>
 * This is the old IBMJSSE JSSEProvider implementation. This currently assumes
 * IBMJSSE2 is the replacement.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class IBMJSSEProvider extends AbstractJSSEProvider implements JSSEProvider {
    private static TraceComponent tc = Tr.register(IBMJSSEProvider.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    /**
     * Constructor.
     */
    public IBMJSSEProvider() {
        super();

        String protocol = Constants.PROTOCOL_SSL_TLS_V2;
        if (CryptoUtils.isFips140_3Enabled()) {
            protocol = Constants.PROTOCOL_TLS;
        }

        initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
                   Constants.SOCKET_FACTORY_WAS_DEFAULT, null, protocol);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created an IBM JSSE provider with protocol " + protocol);
        }
    }
}
