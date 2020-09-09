/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.message;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.transaction.xa.XAResource;

import com.ibm.adapter.FVTAdapterImpl;

/**
 * <p>
 * This interface defines the basic methods which send messages to the endpoint
 * applications via testing resource adapter. An implementation of this
 * interface serves as a base message provider. The work manager from the
 * application server won't be involved in sending messages from this message
 * provider. For the message provider which involves work manager, please refer
 * to <code>FVTMessageProvider</code>
 * </p>
 *
 * <p>
 * The target user of this message provider is WebSphere Container FVT.
 * </p>
 */
public interface FVTBaseMessageProvider {

    /**
     * <p>
     * This method sends a message to a particular endpoint application directly
     * without using work manager.
     * </p>
     *
     * @param deliveryId
     *                       the ID related to this message delivery. You can use this
     *                       message delivery ID to retrieve delivery-related information,
     *                       such as the endpoint instance used for this message delivery.
     *                       If you don't provide deliveryId, you won't be able to retreive
     *                       the information.
     * @param beanName
     *                       the message endpoint name
     * @param message
     *                       the message going to be delivered to the message endpoint
     * @param xaResource
     *                       the XAResource object used for getting transaction
     *                       notifications. Set to null if you don't want to get
     *                       transaction notifications.
     *
     * @exception ResourceException
     */
    void sendDirectMessage(String deliveryId, String beanName, String message,
                           XAResource resource) throws ResourceException;

    /**
     * <p>
     * This method sends one or more messages to one or more endpoint
     * applications directly without using work manager.
     * </p>
     *
     * @param deliveryId
     *                       the ID related to this message delivery. You can use this
     *                       message delivery ID to retrieve delivery-related information,
     *                       such as endpoint instances used for this message delivery. If
     *                       you don't provide deliveryId, you won't be able to retreive
     *                       the information.
     * @param message
     *                       the FVT message object.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendDirectMessage(String deliveryId, FVTMessage message) throws ResourceException;

    /**
     * <p>
     * This method hints the testing resource adapter to release all the
     * information related with this message delivery. TRA will remove the
     * message delivery ID from the hashmap. After calling this method, users
     * cannot get any endpoint information from this message delivery ID any
     * more.
     * </p>
     *
     * @param deliveryId
     *                       the ID related to this message delivery.
     */
    void releaseDeliveryId(String deliveryId);

    /**
     * <p>
     * Set the association between the resource adapter and the message
     * provider.
     * </p>
     *
     * <p>
     * The FVTMessageProviderImpl is an administered object, which has no
     * association with the resource adapter instance. We have to use an
     * external contract to associate the FVTMessageProviderImpl instance with
     * the testing resource adpater instance.
     * </p>
     *
     * <p>
     * This method asssociates this instance with a testing resource adapter
     * instance. It calls a helper class to get the ConnectionFactory object (In
     * the testing resource adapter case, the connection factory has type as
     * javax.sql.DataSource), and from this ConnectionFactory object, the
     * resource adpater instance is retrieved. After that, this resource adapter
     * instance is set to this message provider instance.
     * </p>
     *
     * <p>
     * It is required that applications call this setResourceAdapter method
     * after looking up the administered object to establish the association
     * between the administered object and the resource adapter instance.
     * <p>
     *
     * @param cfJndiName
     *                       the connection factory JNDI name.
     */
    void setResourceAdapter(String cfJNDIName) throws ResourceAdapterInternalException;

    void setResourceAdapter(FVTAdapterImpl ra);

    // d177221 begin: swai
    /**
     * <p>
     * Indicate to the TRA that the MessageProvider (underlying EIS) is failing.
     *
     * <p>
     * This method set the instance variable, EISStatus to STATUS_FAIL. TRA
     * checks this value to find out if the EIS is failing or not. This simulate
     * the EIS is failed.
     */

    void signalEISFailure() throws ResourceException;

    /**
     * <p>
     * Indicate to the TRA that the MessageProvider (underlying EIS) is
     * recovered.
     *
     * <p>
     * This method set the instance variable, EISStatus to STATUS_OK. TRA checks
     * this value to find out if the EIS is recovered or not. This simulate the
     * EIS is recovered.
     *
     */
    void signalEISRecover() throws ResourceException;

}
