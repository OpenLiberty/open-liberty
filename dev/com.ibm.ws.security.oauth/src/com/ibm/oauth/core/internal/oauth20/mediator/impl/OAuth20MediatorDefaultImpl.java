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
package com.ibm.oauth.core.internal.oauth20.mediator.impl;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;

public class OAuth20MediatorDefaultImpl implements OAuth20Mediator {

    public void init(OAuthComponentConfiguration config) {
        // no config needed
    }

    public void mediateAuthorize(AttributeList attributeList)
            throws OAuth20MediatorException {
        // default implementation does nothing
    }

    public void mediateToken(AttributeList attributeList)
            throws OAuth20MediatorException {
        // default implementation does nothing
    }

    public void mediateResource(AttributeList attributeList)
            throws OAuth20MediatorException {
        // default implementation does nothing
    }

    public void mediateAuthorizeException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        // default implementation does nothing
    }

    public void mediateTokenException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        // default implementation does nothing
    }

    public void mediateResourceException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        // default implementation does nothing
    }

}
