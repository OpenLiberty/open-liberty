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

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEProvider;
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
    public static String IBM_JCE_Plus_FIPS_PROVIDER = "com.ibm.crypto.provider.IBMJCEPlusFIPS";

    /**
     * Constructor.
     */
    public IBMJSSEProvider() {
        super();
        String fipsON = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("com.ibm.jsse2.usefipsprovider");
            }
        });

        String ibmjceplusfipsprovider = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("com.ibm.jsse2.usefipsProviderName");
            }
        });
        System.out.println("EFT: provider: " + ibmjceplusfipsprovider);

        if (fipsON != null && fipsON.equalsIgnoreCase("true") && ibmjceplusfipsprovider.equals("IBMJCEPlusFIPS")) {
            System.out.println("EFT: fipson and jceplusfips");
            System.out.println("     EFT: key manager factory alg: " + JSSEProviderFactory.getKeyManagerFactoryAlgorithm());
            System.out.println("     EFT: trust manager factory alg: " + JSSEProviderFactory.getTrustManagerFactoryAlgorithm());
            System.out.println("     EFT: protocol: " + Constants.PROTOCOL_TLS);
            initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
                       Constants.SOCKET_FACTORY_WAS_DEFAULT, null, Constants.PROTOCOL_TLS);
        } else {
            System.out.println("EFT: fips not on: " + fipsON);
            initialize(JSSEProviderFactory.getKeyManagerFactoryAlgorithm(), JSSEProviderFactory.getTrustManagerFactoryAlgorithm(), Constants.IBMJSSE2_NAME, null,
                       Constants.SOCKET_FACTORY_WAS_DEFAULT, null, Constants.PROTOCOL_SSL_TLS_V2);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created an IBM JSSE provider");
        }
    }

}
