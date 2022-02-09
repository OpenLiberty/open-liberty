/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.spi;

/**
 * SPI to be used by the com.ibm.wsspi.security.openidconnect.IDTokenMediator SPI
 * implementations to be able to obtain the third-party ID token and fully customize
 * the new ID token using the third-party token claims.
 */
public interface ThirdPartyContext​ {

    /**
     * This method should return the third-party ID token.
     *
     * @return The third-party ID token.
     */
    public String getIdToken();
}
