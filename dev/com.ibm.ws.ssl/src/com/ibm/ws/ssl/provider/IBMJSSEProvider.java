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
import com.ibm.ws.crypto.common.CryptoProvider;
import com.ibm.ws.crypto.common.FipsUtils;
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
    //ublic static String IBM_JCE_Plus_FIPS_PROVIDER = "com.ibm.crypto.plus.provider.IBMJCEPlusFIPS";

    //private static boolean issuedBetaMessage = false;

    /**
     * Constructor.
     */
    public IBMJSSEProvider() {
        super();
//        String fipsON = AccessController.doPrivileged(new PrivilegedAction<String>() {
//            @Override
//            public String run() {
//                return System.getProperty("com.ibm.jsse2.usefipsprovider");
//            }
//        });
//
//        String ibmjceplusfipsprovider = AccessController.doPrivileged(new PrivilegedAction<String>() {
//            @Override
//            public String run() {
//                return System.getProperty("com.ibm.jsse2.usefipsProviderName");
//            }
//        });

//        if (tc.isDebugEnabled()) {
//            Tr.debug(tc, "provider: " + ibmjceplusfipsprovider);
//        }

//        if (isRunningBetaMode() && "true".equalsIgnoreCase(fipsON) && "IBMJCEPlusFIPS".equalsIgnoreCase(ibmjceplusfipsprovider)) {
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "fips is enabled and using IBMJCEPlusFIPS provider");
//                Tr.debug(tc, "key manager factory alg: " + JSSEProviderFactory.getKeyManagerFactoryAlgorithm());
//                Tr.debug(tc, "trust manager factory alg: " + JSSEProviderFactory.getTrustManagerFactoryAlgorithm());
//                Tr.debug(tc, "protocol: " + Constants.PROTOCOL_TLS);
//            }
//            initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
//                       Constants.SOCKET_FACTORY_WAS_DEFAULT, null, Constants.PROTOCOL_TLS);
//        } else {
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "protocol: " + Constants.PROTOCOL_SSL_TLS_V2);
//            }
//            initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
//                       Constants.SOCKET_FACTORY_WAS_DEFAULT, null, Constants.PROTOCOL_SSL_TLS_V2);
//        }
        String protocol = Constants.PROTOCOL_SSL;
        if (FipsUtils.isFIPSEnabled() && CryptoProvider.isIBMJCEPlusFIPSAvailable()) {
            protocol = Constants.PROTOCOL_TLS;
        }

//        if (FipsUtils.isFIPSEnabled()) {
//        initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
//                   Constants.SOCKET_FACTORY_WAS_DEFAULT, null, Constants.PROTOCOL_TLS);
//
//            //TODO - do we want to fallback to other provider or use the JDK default provider ??
//
//        } else {
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "protocol: " + Constants.PROTOCOL_SSL_TLS_V2);
//            }
//            initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
//                       Constants.SOCKET_FACTORY_WAS_DEFAULT, null, Constants.PROTOCOL_SSL_TLS_V2);
//        }

        if (CryptoProvider.isIBMJCEPlusFIPSAvailable()) {
            initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
                       Constants.SOCKET_FACTORY_WAS_DEFAULT, null, protocol);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created an IBM JSSE provider with protocol " + protocol);
        }
    }

//    boolean isRunningBetaMode() {
//        if (!ProductInfo.getBetaEdition()) {
//            return false;
//        } else {
//            // Running beta exception, issue message if we haven't already issued one for
//            // this class
//            if (!issuedBetaMessage) {
//                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
//                issuedBetaMessage = !issuedBetaMessage;
//            }
//            return true;
//        }
//    }

}
