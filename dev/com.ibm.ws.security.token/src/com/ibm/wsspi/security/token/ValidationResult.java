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
package com.ibm.wsspi.security.token;

/**
 * This interface represents the successful result of validating an LTPA token.
 * Stack products may use the interface to determine if a JAAS login is
 * necessary for the validated token or to obtain the WAS representation of
 * the user id, realm name, etc.
 * Instances of this interface are created by the
 * <code>WSSecurityPropagationHelper.validateToken( byte[] token )</code>
 * operation.
 *
 * @author IBM Corp.
 * @version 7.0.0
 * @since 7.0.0
 * @ibm-spi
 *
 */
public interface ValidationResult {

    /**
     * @return true if the result of validating the token indicates a JAAS login
     *         is required.
     */
    boolean requiresLogin();

    /**
     * @return the WAS unique id obtained by validating the token
     */
    String getUniqueId();

    /**
     * @return the WAS user id extracted from the WAS unique id.
     */
    String getUserFromUniqueId();

    /**
     * @return the WAS realm name extracted from the WAS unique id.
     */
    String getRealmFromUniqueId();

}
