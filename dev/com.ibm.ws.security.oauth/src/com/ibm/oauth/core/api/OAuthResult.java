/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;

/**
 * Interface to encapsulate result information from request processing in OAuth component.
 *
 */
public interface OAuthResult {

    /**
     * The OAuth request succeeded.
     */
    public final int STATUS_OK = 0;
    /**
     * The OAuth request can not be performed due to an error.
     */
    public final int STATUS_FAILED = 1;

    /**
     * SPNEGO TAI challenge.
     */
    public final int TAI_CHALLENGE = 2;

    /**
     * Gets OAuth request status.
     * @return A code representing the status of the corresponding OAuth request.
     */
    int getStatus();

    /**
     * Gets the attributes list associated with the corresponding OAuth request.
     * @return the attributes list.
     */
    AttributeList getAttributeList();

    /**
     * Gets the OAuth protocol exception associated with the corresponding OAuth request.
     * @return the OAuth protocol exception if the status is {@link OAuthResult#STATUS_FAILED STATUS_FAILED}, null otherwise.
     */
    OAuthException getCause();
}
