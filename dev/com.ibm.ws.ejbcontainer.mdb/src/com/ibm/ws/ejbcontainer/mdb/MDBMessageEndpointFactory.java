/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb;

import javax.resource.spi.endpoint.MessageEndpointFactory;

/**
 * MDBMessageEnpointFactory provides a mechanism to make additional
 * information about the message-driven bean MessageEndpointFactory
 * available to resource adapter implementations.
 */
public interface MDBMessageEndpointFactory extends MessageEndpointFactory {

    /**
     * Returns the maximum number of message-driven beans that may be active
     * concurrently. <p>
     * 
     * This method enables the JMS Resource Adapter to match the message
     * endpoint concurrency to the value used for the message-driven bean
     * by the EJB container. <p>
     * 
     * Note: The value returned may vary over the life of a message endpoint
     * factory in response to dynamic configuration updates. <p>
     * 
     * @return Returns the maximum concurrent instances for the message endpoint factory.
     */
    int getMaxEndpoints();

    /**
     * Returns an opaque object to uniquely represent the message endpoint factory.
     * This object overrides hashCode and equals to allow its use as the key in a
     * hashMap for use by the caller. <p>
     * 
     * This object will return the same hashCode and compare equal to all other
     * objects returned by calling this method from any component executing against
     * the same message endpoint factory in the same application server. <p>
     * 
     * The toString method returns a String that might be usable by a human reader
     * to usefully understand where the message-driven bean associated with the
     * message endpoint factory is defined. The toString result is otherwise not
     * defined. Specifically, there is no forward or backward compatibility
     * guarantee of the results of toString. <p>
     * 
     * The object is not necessarily serializable, and has no defined behavior
     * outside the virtual machine whence it was obtained. <p>
     * 
     * @return Returns an opaque object to uniquely represent the message endpoint factory.
     */
    Object getMDBKey();

    /**
     * Returns the Activation Spec ID
     * 
     * @return Returns the Activation Spec ID
     */
    String getActivationSpecId();
}
