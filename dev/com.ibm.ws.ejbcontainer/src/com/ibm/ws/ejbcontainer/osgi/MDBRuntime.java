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
package com.ibm.ws.ejbcontainer.osgi;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanOFactory;
import com.ibm.ejs.container.BeanOFactory.BeanOFactoryType;
import com.ibm.ejs.container.MessageEndpointCollaborator;
import com.ibm.tx.jta.XAResourceNotAvailableException;

/**
 * The interface between the core EJB container and the services provided by
 * the message-driven bean runtime environment.
 */
public interface MDBRuntime extends BeanRuntime {

    /**
     * Returns the BeanOFactory for the the specified bean type or null. <p>
     * 
     * @param type one of the supported types defined on {@link BeanOFactory}
     */
    BeanOFactory getBeanOFactory(BeanOFactoryType type);

    /**
     * Loads the MessageEndpointFactory implementation class. This method
     * should not be called unless EJBRuntime.setupBean(MESSAGE_DRIVEN)
     * succeeds. <p>
     * 
     * The returned class will vary depending on whether the MDB uses the
     * older style MessageListener or newer JCA MessageEndpoint. An exception
     * is thrown if the runtime does not support the appropriate implementation.
     * 
     * @param bmd bean metadata that is used to determine if the MDB uses the
     *            older style MessageListener or newer JCA MessageEndpoint.
     */
    Class<?> getMessageEndpointFactoryImplClass(BeanMetaData bmd) throws ClassNotFoundException;

    /**
     * Loads the MessageEndpoint implementation class. This method should not
     * be called unless EJBRuntime.setupBean(MESSAGE_DRIVEN) succeeds.
     * 
     * The returned class will vary depending on whether the MDB uses the
     * older style MessageListener or newer JCA MessageEndpoint. An exception
     * is thrown if the runtime does not support the appropriate implementation.
     * 
     * @param bmd bean metadata that is used to determine if the MDB uses the
     *            older style MessageListener or newer JCA MessageEndpoint.
     * @return the MessageListener impl class or null to indicate JCA MessageEndpoint.
     */
    Class<?> getMessageEndpointImplClass(BeanMetaData bmd) throws ClassNotFoundException;

    /**
     * Retrieves the message endpoint collaborator instance.
     *
     * @return The message endpoint collaborator instance.
     */
    public MessageEndpointCollaborator getMessageEndpointCollaborator();
    
    /**
     * Method to get the XAResource corresponding to an ActivationSpec from the RRSXAResourceFactory
     * 
     * @param activationSpecId The id of the ActivationSpec
     * @param xid Transaction branch qualifier
     * @return the XAResource
     */
    public XAResource getRRSXAResource(String activationSpecId, Xid xid) throws XAResourceNotAvailableException;
}
