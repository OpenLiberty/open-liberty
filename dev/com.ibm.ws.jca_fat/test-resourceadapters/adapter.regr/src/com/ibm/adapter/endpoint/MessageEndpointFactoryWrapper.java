/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.endpoint;

import java.lang.reflect.Method;
import java.util.Vector;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.ibm.adapter.ActivationSpecImpl;
import com.ibm.adapter.AdapterUtil;
import com.ibm.adapter.FVTAdapterImpl;
import com.ibm.adapter.test.FVTEndpoint1;
import com.ibm.adapter.test.FVTEndpoint2;
import com.ibm.adapter.tra.FVTXAResourceImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>
 * This class wraps the class MessageEndpointFactory. It has the references to
 * all the endpoint applications created by this MessageEndpointFactory.
 * </p>
 *
 */
public class MessageEndpointFactoryWrapper implements MessageEndpointFactory {

    /** the Endpoint factory from application server */
    private final MessageEndpointFactory factory;

    /** Endpoint applications from this factory */
    private final Vector endpoints = new Vector(5);

    /** ActivationSpec of this factory */
    private final ActivationSpecImpl activationSpec;

    private static final TraceComponent tc = Tr
                    .register(MessageEndpointFactoryWrapper.class);

    /** Adapter instance */
    private final FVTAdapterImpl adapter;

    /** Counter to keep track of max connections for throttling support */
    private int maxCounter = 0;

    /**
     * Constructor
     *
     * @param factory
     *            a MessageEndpintFactory instance
     * @param activationSpec
     *            an ActivationSpec instance
     */
    public MessageEndpointFactoryWrapper(FVTAdapterImpl adapter,
                                         MessageEndpointFactory factory, ActivationSpec activationSpec) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { factory, activationSpec });

        this.factory = factory;
        this.activationSpec = (ActivationSpecImpl) activationSpec;
        this.adapter = adapter;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * <p>
     * This is used to create a message endpoint. The message endpoint is
     * expected to implement the correct message listener type.
     * </p>
     *
     * @param xaResource
     *            an optional XAResource instance used to get transaction
     *            notifications when the message delivery is transacted.
     *
     * @return a message endpoint instance.
     */
    @Override
    public MessageEndpoint createEndpoint(XAResource resource) throws javax.resource.spi.UnavailableException {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "createEndpoint", new Object[] { this, resource }); // d174149

        MessageEndpoint endpoint = null;

        if (adapter.testMode) {
            // This is for testing purose.

            if (activationSpec.getName().equals("FVTEndpoint1")) {
                endpoint = new FVTEndpoint1();
            } else {
                endpoint = new FVTEndpoint2();
            }
        }
        // Modification started for LI2110.01
        else if (AdapterUtil.getThrottlingSupportFlag() == AdapterUtil.THROTTLING_MESSAGEINFLOW_SUPPORT) {
            AdapterUtil.setMessageFromRA("FAILURE"); // preset failure message
                                                     // in case failure
                                                     // occurs

            if (tc.isEntryEnabled())
                Tr.entry(tc,
                         "createEndpoint : Inside Throttling Support condition",
                         this);

            // increment the endpoint counter
            maxCounter++;

            if (maxCounter == 3) {
                // Create the message endpoint -- should get
                // UnavailableException
                boolean ueOccurred = false;
                try {
                    endpoint = factory.createEndpoint(resource);
                } catch (javax.resource.spi.UnavailableException Ue) {
                    ueOccurred = true;
                }

                if (ueOccurred) {
                    AdapterUtil.setMessageFromRA("FAILURE"); // preset in case
                                                             // failure
                                                             // occurs doing
                                                             // this
                                                             // release one endpoint from existing pool and retry. This
                                                             // should be successful.
                    endpoint = (MessageEndpoint) endpoints.lastElement();
                    endpoints.remove(endpoint);
                    endpoint.release();
                    // decrement the counter by 1
                    maxCounter--;
                    // create one endpoint again
                    endpoint = factory.createEndpoint(resource);
                    // send SUCCESSFUL message back to Testcase
                    AdapterUtil.setMessageFromRA("SUCCESS");
                    // reset the maxCounter
                    maxCounter = 0;

                    if (tc.isEntryEnabled())
                        Tr
                                        .exit(
                                              tc,
                                              "createEndpoint : exiting successfully Throttling Support condition",
                                              this);
                } else {
                    AdapterUtil.setMessageFromRA("FAILURE");
                    // reset the maxCounter
                    maxCounter = 0;
                }
            } else { // maxCounter is < 3 or > 3
                // Create the message endpoint
                endpoint = factory.createEndpoint(resource);
                if (maxCounter > 3) {
                    AdapterUtil.setMessageFromRA("FAILURE"); // Should have been
                                                             // successful
                                                             // before this
                                                             // reset the maxCounter
                    maxCounter = 0;
                }
            }
        } // end of LI2110.01 modification
        else {
            // Create the message endpoint
            endpoint = factory.createEndpoint(resource);
        }

        MessageEndpointWrapper endpointWrapper = new MessageEndpointWrapper(endpoint, this, resource);

        if (resource != null) {
            ((FVTXAResourceImpl) resource).setEndpoint(endpointWrapper);
        }

        // Add the message endpoint to the vector
        endpoints.add(endpointWrapper);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createEndPoint", endpointWrapper);
        return endpointWrapper;
    }

    @Override
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        return createEndpoint(xaResource);
    }

    /**
     * Remove the endpoint
     *
     * @param endpoint
     *            The endpoint going to be removed
     */
    public void removeEndpoint(MessageEndpoint endpoint) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeEndpoint", this);

        boolean removed = endpoints.remove(endpoint);
        if (tc.isEntryEnabled()) {
            if (removed) {
                Tr.exit(tc, "removeEndpoint");
            } else {
                Tr.exit(tc, "removeEndpoint", "Cannot remove endpoint object ");
            }
        }
    }

    /**
     * <p>
     * This is used to find out whether message deliveries to a target method on
     * a message listener interface that is implemented by a message endpoint
     * will be transacted or not. The message endpoint may indicate its
     * transacted delivery preferences (at a per method level) through its
     * deployment descriptor. The message delivery preferences must not change
     * during the lifetime of a message endpoint.
     * </p>
     *
     * @param method
     *            description of a target method. This information about the
     *            intended target method allows an application server to find
     *            out whether the target method call will be transacted or not.
     *
     * @return true, if message endpoint requires transacted message delivery
     *
     * @exception NoSuchMethodException
     *                indicates that the specified method does not exist on the
     *                target endpoint.
     */
    @Override
    public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException { // d174149
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isDeliveryTransacted", new Object[] { this, method });

        boolean transacted = factory.isDeliveryTransacted(method);

        if (tc.isEntryEnabled())
            Tr.entry(tc, "isDeliveryTransacted", new Boolean(transacted));

        return transacted;
    }

    /**
     * Returns the factory.
     *
     * @return MessageEndpointFactory
     */
    public MessageEndpointFactory getFactory() {
        return factory;
    }

    /**
     * Returns the activationSpec.
     *
     * @return ActivationSpec
     */
    public ActivationSpec getActivationSpec() {
        return activationSpec;
    }

    /**
     * toString method
     */
    public String introspectSelf() {
        String lineSeparator = System.getProperty("line.separator");

        // Add the activation spec
        StringBuffer str = new StringBuffer("ActivationSpec:" + activationSpec
                                            + lineSeparator);

        // Add the factory
        str.append("Factory = " + factory + lineSeparator);

        // Add the endpoint instance
        str.append(lineSeparator + "Endpoint instances" + lineSeparator);

        for (int i = endpoints.size() - 1; i >= 0; i--) {
            str.append(((MessageEndpointWrapper) endpoints.elementAt(i))
                            .toString()
                       + lineSeparator);
        }

        return str.toString();

    }

    /**
     * Returns the adapter.
     *
     * @return FVTAdapterImpl
     */
    public FVTAdapterImpl getAdapter() {
        return adapter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof MessageEndpointFactoryWrapper)) {
            return false;
        } else {
            MessageEndpointFactoryWrapper wrapper = (MessageEndpointFactoryWrapper) o;

            // in unit test mode, the factory instance is null. Therefore don't
            // compare factory in test mode.
            return adapter.equals(wrapper.getAdapter())
                   && activationSpec == wrapper.getActivationSpec()
                   && (adapter.testMode || factory
                                   .equals(wrapper.getFactory()));
        }

    }
}
