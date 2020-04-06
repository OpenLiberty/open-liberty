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
package com.ibm.oauth.core.api.oauth20.mediator;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;

/**
 * This interface is used as a callback during the OAuth20 processing to
 * perform customized post processing.
 * 
 */
public interface OAuth20Mediator {
    /**
     * 
     * This method is called by a factory when an instance of this object is
     * created. The configuration object will allow the mediator to
     * initialize itself.
     * 
     * @param config Configuration entity for the component instance
     */
    public void init(OAuthComponentConfiguration config);

    /**
     * This method is called by the core component after basic message
     * validation and processing to allow any post custom processing by the component
     * consumer in processAuthorization method.
     * @param attributeList provides the attributes related to the flow
     * @throws OAuth20MediatorException
     */
    public void mediateAuthorize(AttributeList attributeList)
            throws OAuth20MediatorException;

    /**
     * This method is called by the core component after basic message
     * validation and processing to allow any post custom processing by the component
     * consumer in processTokenRequest method.
     * @param attributeList provides the attributes related to the flow
     * @throws OAuth20MediatorException
     */
    public void mediateToken(AttributeList attributeList)
            throws OAuth20MediatorException;

    /**
     * This method is called by the core component after basic message
     * validation and processing to allow any post custom processing by the component
     * consumer in processResourceRequest method.
     * @param attributeList provides the attributes related to the flow
     * @throws OAuth20MediatorException
     */
    public void mediateResource(AttributeList attributeList)
            throws OAuth20MediatorException;

    /**
     * This method is called by the core component when protocol exception happens
     * to allow any post custom processing by the component consumer in processAuthorization method.
     * @param attributeList provides the attributes related to the flow
     * @param exception OAuth protocol exception
     * @throws OAuth20MediatorException
     */
    public void mediateAuthorizeException(AttributeList attributeList, OAuthException exception)
            throws OAuth20MediatorException;

    /**
     * This method is called by the core component when protocol exception happens
     * to allow any post custom processing by the component consumer in processTokenRequest method.
     * @param attributeList provides the attributes related to the flow
     * @param exception OAuth protocol exception
     * @throws OAuth20MediatorException
     */
    public void mediateTokenException(AttributeList attributeList, OAuthException exception)
            throws OAuth20MediatorException;

    /**
     * This method is called by the core component when protocol exception happens
     * to allow any post custom processing by the component consumer in processResourceRequest method.
     * @param attributeList provides the attributes related to the flow
     * @param exception OAuth protocol exception
     * @throws OAuth20MediatorException
     */
    public void mediateResourceException(AttributeList attributeList, OAuthException exception)
            throws OAuth20MediatorException;
}
