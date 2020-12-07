// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  07/16/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//  09/18/03   swai       d177221      Add a method, signalEISFailure, to signal the
//                                     TRA that the message provider is failing or
//                                     recovered.
//                                     This method will change an instance variable
//                                     (EISStatus) to STATUS_FAIL or STATUS_OK.
//                                     Add a getWorkDispatcher method for messageProvider to
//                                     expose its workDispatcher object instance.
//                                     Add a method, signalEISRecover to signal the TRA
//                                     that the message provider is recovered. This method
//                                     returns only after the EISTimer is back to normal state.
//                                     If the Timer is not in normal state, then the Timer won't
//                                     be able to rollback actve trans when signalEISFailure is
//                                     called.
//  --------------------------------------------------------------------
package com.ibm.websphere.ejbcontainer.test.rar.message;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.ejbcontainer.test.rar.work.FVTWorkDispatcher;

/**
 * <p>This interface defines the basic methods which send messages to the endpoint
 * applications via testing resource adapter. An implementation of this interface
 * serves as a base message provider. The work manager from the application server
 * won't be involved in sending messages from this message provider. For the message
 * provider which involves work manager, please refer to <code>FVTMessageProvider</code></p>
 *
 * <p>The target user of this message provider is WebSphere Container FVT.</p>
 */
public interface FVTBaseMessageProvider {

    /**
     * <p>This method sends a message to a particular endpoint application directly
     * without using work manager. </p>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as the endpoint instance used
     *            for this message delivery. If you don't provide deliveryId, you won't be able to retreive
     *            the information.
     * @param beanName the message endpoint name
     * @param message the message going to be delivered to the message endpoint
     * @param xaResource the XAResource object used for getting transaction notifications. Set
     *            to null if you don't want to get transaction notifications.
     *
     * @exception ResourceException
     */
    void sendDirectMessage(String deliveryId, String beanName, String message, XAResource resource) throws ResourceException;

    /**
     * <p>This method sends one or more messages to one or more endpoint applications directly
     * without using work manager. </p>
     *
     * @param deliveryId the ID related to this message delivery. You can use this message
     *            delivery ID to retrieve delivery-related information, such as endpoint instances used
     *            for this message delivery. If you don't provide deliveryId, you won't be able to retreive
     *            the information.
     * @param message the FVT message object.
     *
     * @exception ResourceException
     *
     * @see FVTMessage
     */
    void sendDirectMessage(String deliveryId, FVTMessage message) throws ResourceException;

    /**
     * <p>This method returns an array of test results associated with a particular
     * endpoint instance (identified by endpoint name and instance ID) in a specific
     * delivery.
     *
     * @param deliveryId the ID related to this message delivery. This message delivery ID
     *            is used to retrieve the endpoint wrapper instances.
     * @param beanName the message endpoint name
     * @param instanceID the Id of the message endpoint
     *
     * @return an array of test results associated with a particular endpoint instance.
     */
    MessageEndpointTestResults[] getTestResults(String deliveryId, String endpointName, int instanceId);

    /**
     * <p>This method returns the test result associated with the message delivery id. </p>
     *
     * <p>This method can only be used when there is only one test result added in this
     * message delivery. If there is more than one test results added in the delivery,
     * there is no guarantee which test result will be returned. Method callers should
     * be aware of this.</p>
     *
     * @param deliveryId the ID related to this message delivery. This message delivery ID
     *            is used to retrieve the endpoint wrapper instances.
     *
     * @return a message endpoint test result object.
     */
    MessageEndpointTestResults getTestResult(String deliveryId);

    /**
     * <p>This method hints the testing resource adapter to release all the information
     * related with this message delivery. TRA will remove the message delivery ID
     * from the hashmap. After calling this method, users cannot get any endpoint
     * information from this message delivery ID any more. </p>
     *
     * @param deliveryId the ID related to this message delivery.
     */
    void releaseDeliveryId(String deliveryId);

    /**
     * <p>Set the association between the resource adapter and the message provider.</p>
     *
     * <p>The FVTMessageProviderImpl is an administered object, which has no association
     * with the resource adapter instance. We have to use an external contract to associate
     * the FVTMessageProviderImpl instance with the testing resource adapter instance.</p>
     *
     * <p>This method associates this instance with a testing resource adapter instance.
     * It calls a helper class to get the ConnectionFactory object (In the testing
     * resource adapter case, the connection factory has type as javax.sql.DataSource),
     * and from this ConnectionFactory object, the resource adapter instance is retrieved.
     * After that, this resource adapter instance is set to this message provider instance.</p>
     *
     * <p>It is required that applications call this setResourceAdapter method after
     * looking up the administered object to establish the association between the
     * administered object and the resource adapter instance. <p>
     *
     * @param cfJndiName the connection factory JNDI name.
     */
    void setResourceAdapter(String cfJNDIName) throws ResourceAdapterInternalException;

    /**
     * <p>Getters of WorkDispatcher object instance.</p>
     */
    FVTWorkDispatcher getWorkDispatcher();

    //d177221 begin: swai
    /**
     * <p>Indicate to the TRA that the MessageProvider (underlying EIS) is failing.
     *
     * <p>This method set the instance variable, EISStatus to STATUS_FAIL. TRA checks this value to find
     * out if the EIS is failing or not. This simulate the EIS is failed.
     */
    void signalEISFailure() throws ResourceException;

    /**
     * <p>Indicate to the TRA that the MessageProvider (underlying EIS) is recovered.
     *
     * <p>This method set the instance variable, EISStatus to STATUS_OK. TRA checks this value to find
     * out if the EIS is recovered or not. This simulate the EIS is recovered.
     *
     */
    void signalEISRecover() throws ResourceException;
}