/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.saml2;

import com.ibm.ws.security.sso.common.saml.propagation.PropagationHelperImpl;

/**
 * 
 * Provides methods to return the SAML token from the runAs subject.
 * 
 * @author International Business Machines Corp.
 * @version WAS 8.5.5.10
 * @ibm-api
 */

public class PropagationHelper {

    /**
     * Returns the <code>Saml20Token</code> from the runAs subject. Applications can use this method to retrieve the Saml20Token (possibly created during the web single sign on
     * process)
     * and then call the down stream REST-ful service by including the token in the HTTP header.
     * 
     * @return <code>Saml20Token</code>
     */

    public static Saml20Token getSaml20Token() {
        return PropagationHelperImpl.getSaml20Token();
    }

    /**
     * Returns the encoded <code>Saml20Token</code> from the runAs subject. Applications can use this method to retrieve the Saml20Token (possibly created during the web single
     * sign on process)
     * and then call the down stream REST-ful service by including the token in the HTTP header.
     * 
     * @param isCompressed if <code>true</code>, the the token data will be compressed first and then encoded.
     * @return encoded saml token
     */

    public static String getEncodedSaml20Token(boolean isCompressed) {// base64 encoded, or gzip and encoded
        return PropagationHelperImpl.getEncodedSaml20Token(isCompressed);
    }
}
