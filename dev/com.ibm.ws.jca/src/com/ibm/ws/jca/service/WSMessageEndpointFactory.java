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
package com.ibm.ws.jca.service;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.websphere.csi.J2EEName;

/**
 * The <code>com.ibm.ws.j2c.MessageEndpointFactory</code> interface extends
 * the <code>javax.resource.spi.endpoint.MessageEndpointFactory</code> and
 * adds methods to set the RecoveryID and JCA version. Since the MessageEndpoint
 * proxies and messageEndpointFactories will be implemented by the ejb container,
 * and perhaps someday the web container, and since they will need the
 * RecoveryID when ever the enlist an XAResource as part of a transacted
 * message delivery, the setRecoveryID method is being provided in an interface
 * supplied by the J2C component. The JCA version enables the container to
 * provide version-specific endpoint behaviors.
 *
 * <p><b>Requirement</b>: A given MessageEndpointFactory instance may only
 * be used for a single endpoint activation. The same MessageEndpointFactory
 * instance will then be needed later to deactivate the endpoint.
 */

public interface WSMessageEndpointFactory extends MessageEndpointFactory {

    /**
     * Method setJCAVersion.
     * <p>
     * The JCAVersion indicates what version of JCA specification the RA that
     * uses this MessageEndpointFactory requires compliance with.
     *
     * @param majorJCAVersion The JCA major version that specifies the behaviors of endpoints created
     *            by a MessageEndpointFactory.
     * @param minorJCAVersion The JCA minor version that specifies the behaviors of endpoints created
     *            by a MessageEndpointFactory.
     */
    void setJCAVersion(int majorJCAVersion, int minorJCAVersion);

    /**
     * Method setRecoveryID.
     * <p>
     * The transaction RecoveryID is obtained by the RALifeCycleManager as part of
     * It's endpoint activation sequence.
     * Since the implementor of the MessageEndpoint proxies will require the
     * RecoveryID when enlisting XAResources as part of transacted messages,
     * the RALifeCycle manager will set the recoveryID on a given MessageEndpoint
     * Factory proir to calling activate on the RA, and hence passing the
     * MessageEndpointFactory. This is required because inbound message can
     * begin to flow immediately upon activation.
     *
     * @param RecoveryID
     */
    void setRecoveryID(int RecoveryID) throws ResourceException;

    /**
     * RA_DOES_NOT_SUPPORT_XATRANSACTIONS is a constant used to inicate that the
     * RA's DD indicated that it did NOT support XATransations, therefore
     * the J2C runtime will not setup for XARecovery.
     */
    static public int RA_DOES_NOT_SUPPORT_XATRANSACTIONS = 0;

    /**
     * ERROR_DURING_TRAN_RECOVERY_SETUP is a constant used to inicate that the
     * J2C runtime encountered an error while trying to setup for transaction
     * recovery, therefore the J2C runtime cannot support recovery for this endpoint.
     */
    static public int ERROR_DURING_TRAN_RECOVERY_SETUP = 1;

    /**
     * Method setTranEnlistmentNotNeeded.
     * <p>
     * This method indicates that the MessageEndpointFactory should NOT enlist
     * an XAResource in a transaction.
     *
     * @param reason This is the reason code for why the MessageEndpointFactory
     *            does not need to enlist. Valid reason codes are:
     *            <p>
     *            MessageEndpointFactory.RA_DOES_NOT_SUPPORT_XATRANSACTIONS - this indicates
     *            that the ResourceAdapter has indicated that it does not support XATransactions.
     *            <p>
     *            MessageEndpointFactory.ERROR_DURING_TRAN_RECOVERY_SETUP - this indicates
     *            that the RALifeCycleManager encounter an error which prevented it from
     *            setting up tranaction recovery for this ResourceAdapter. The
     *            MessageEndpointFactory should throw an exception and log an appropriate
     *            message if the RA attempts to use an XAResource in a transaction. This
     *            can't be allowed since recovery setup has failed.
     *
     */
    void setTranEnlistmentNotNeeded(int reason);

    /**
     * Method setRAKey is used to pass the RAKey, which is a unique identifier
     * for the given ResourceAdapter instance, to the MessageEndPoint factory
     * for use with providing better trace and error messages.
     *
     * @param RAKey Unique key for the RA instance. Currently this is the
     *            configID of the RA.
     *
     */
    void setRAKey(String RAKey);

    /**
     * This method is called by the RALifeCycleManager when ever it Forcefully
     * deactivates an endpoint during an RA stop operation. This happens because
     * the application(s) has not been properly stopped prior to stopping the RA.
     *
     * The intent here is for the MessageEndpointFactory (i.e. container code) to
     * stop tracking it's deactivationKey such that if its containing application is
     * told to stop it will not try to deactivate an endpoint which is already
     * deactivated and for which there is no longer an entry.
     *
     */
    void messageEndpointForcefullyDeactivated();

    /**
     * @return Returns the J2EEName associated with this MessageEndpointFactory.
     */
    J2EEName getJ2EEName();

    /**
     * Returns the maximum number of message driven beans that may be active concurrently.
     * This method enables the Resource Adapter to match the message endpoint concurrency to the
     * value used for the message driven bean by the EJB container.
     * It is obtained from {@code ivMaxCreation} in {@code BeanMetaData}.
     *
     * Note: The value returned may vary over the life of a message endpoint factory in response to
     * dynamic configuration updates.
     *
     * @return int of the maximum concurrent instances for the message endpoint factory.
     */
    int getMaxEndpoints();

    /**
     * Returns the Activation Spec ID. This is the JNDIname of the ActivationSpec object for the RA when an MDB
     * is bound to a JCA 1.5 resource adapter rather than a JMS provider.
     * It is obtained from {@code ivActivationSpecJndiName} in {@code BeanMetaData}.
     *
     * @return String of the Activation Spec ID
     */
    String getActivationSpecId();
}
