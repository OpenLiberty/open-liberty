/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.inbound;
//Sanjay Liberty Changes
//import javax.resource.spi.InvalidPropertyException;
//import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapterInternalException;


/**
 * Interface implemented by resource adapter <code>ActivationSpec</code>
 * classes in order to supply API agnostic endpoint configuration information.
 */
public interface SibRaEndpointConfigurationProvider {

    /**
     * Returns the endpoint configuration for this activation.
     * 
     * @return the endpoint configuration
     * @throws InvalidPropertyException
     *             if the configuration is not valid
     * @throws ResourceAdapterInternalException
     *             if the configuration cannot be supplied for some other reason
     */
    SibRaEndpointConfiguration getEndpointConfiguration()
            throws InvalidPropertyException, ResourceAdapterInternalException;

    /**
     * Returns an object for invoking endpoints of the type supported by this
     * resource adapter.
     * 
     * @return an object for invoking endpoints
     * @throws ResourceAdapterInternalException
     *             if the invoker cannot be supplied
     */
    SibRaEndpointInvoker getEndpointInvoker()
            throws ResourceAdapterInternalException;

}
