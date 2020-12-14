/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.work;

import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointFactoryWrapper;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointWrapper;

/**
 * <p>This class extends FVTGeneralWorkImpl class. An object of this class represents a simple work,
 * sending one message to one endpoint application.</p>
 */
public class FVTSimpleWorkImpl extends FVTGeneralWorkImpl {
    private final static String CLASSNAME = FVTSimpleWorkImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** The message endpoint factory */
    protected MessageEndpointFactoryWrapper endpointFactory;

    /** This is for transacted delivery */
    protected FVTXAResourceImpl xaResource;

    /** The message going to be sent */
    Message message;

    /** Endpoint name */
    protected String endpointName;

    /**
     * Constructor
     *
     * @param endpointFactory a MessageEndpointFactory instance
     * @param resource an FVTXAResourceImpl instance
     */
    public FVTSimpleWorkImpl(String workName, String endpointName, MessageEndpointFactoryWrapper endpointFactory) {
        super(workName);
        this.endpointName = endpointName;
        this.endpointFactory = endpointFactory;
    }

    /**
     * Constructor
     *
     * @param workName the name of the work, also called the delviery ID
     * @param endpointName the endpoint name
     * @param endpointFactory a MessageEndpointFactory instance
     * @param resource an FVTXAResourceImpl instance
     */
    public FVTSimpleWorkImpl(String workName,
                             String endpointName,
                             MessageEndpointFactoryWrapper endpointFactory,
                             FVTXAResourceImpl resource) {
        this(workName, endpointName, endpointFactory);
        this.xaResource = resource;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        svLogger.entering(CLASSNAME, "run", this);

        try {
            // obtain a message endpoint and narrow it to the correct type.
            // ActivationSpec has endpoint message listener type information.
            instance = (MessageEndpointWrapper) endpointFactory.createEndpoint(xaResource);

            // Add the test result.
            instance.addTestResult();

            MessageListener endpoint = (MessageListener) instance.getEndpoint();
            firstInstanceKey = endpointName + "0";

            // deliver the message
            endpoint.onMessage(message);

            ((MessageEndpoint) endpoint).release();

        } catch (UnavailableException ue) {
            svLogger.info("Unable to create endpoint application: " + ue);
            new WorkRuntimeException(ue);
        }
    }

    /**
     * Returns the endpointFactory.
     *
     * @return MessageEndpointFactory
     */
    public MessageEndpointFactory getEndpointFactory() {
        return endpointFactory;
    }

    /**
     * Returns the xaResource.
     *
     * @return FVTXAResourceImpl
     */
    public FVTXAResourceImpl getXaResource() {
        return xaResource;
    }

    /**
     * Sets the endpointFactory.
     *
     * @param endpointFactory The endpointFactory to set
     */
    public void setEndpointFactory(MessageEndpointFactoryWrapper endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    /**
     * Sets the xaResource.
     *
     * @param xaResource The xaResource to set
     */
    public void setXaResource(FVTXAResourceImpl xaResource) {
        this.xaResource = xaResource;
    }

    /**
     * Sets the message.
     *
     * @param message The message to set
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Recycle this object for reuse
     *
     * @param workName the name of the work, also called the delviery ID
     * @param endpointName the endpoint name
     * @param endpointFactory a MessageEndpointFactory instance
     * @param resource an FVTXAResourceImpl instance
     *
     * @return this intance
     */
    public FVTSimpleWorkImpl recycle(String workName,
                                     String endpointName,
                                     MessageEndpointFactoryWrapper endpointFactory,
                                     FVTXAResourceImpl resource) {

        svLogger.entering(CLASSNAME, "recycle", new Object[] { this, workName, endpointName, endpointFactory, resource });

        // If passed-in workName is null, use the hash code of the work as the workName.
        if (workName == null || workName.equals("") || workName.trim().equals(""))
            name = "" + this.hashCode();

        // Set the state to INITIAL state.
        state = INITIAL;

        this.endpointFactory = endpointFactory;

        instance = null;
        firstInstanceKey = null;

        this.xaResource = resource;

        svLogger.exiting(CLASSNAME, "recycle", this);

        return this;
    }
}