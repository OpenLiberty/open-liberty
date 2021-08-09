/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package ejb.inboundsec;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.xa.Xid;

import com.ibm.adapter.endpoint.MessageEndpointTestResultsImpl;
import com.ibm.adapter.message.FVTMessage;
import com.ibm.adapter.message.FVTMessageProvider;
import com.ibm.adapter.message.WorkInformation;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class SampleSessionImpl implements SampleSessionLocal {

    @Resource(name = "jms/InsecAdminObject")
    private FVTMessageProvider messageProvider;

    /**
     * This method internally looks up the FVTMessageProvider administered
     * object from the JNDI registry and uses it to send a message to the
     * Message End Point which is a Message Driven Bean SampleMdb. Once the
     * message is sent we get the corresponding MessageEndpointTestResults
     * object from the FVTMessageProvider and use it to validate that a message
     * has been sent successfully.
     *
     * @param deliveryId
     *            The deliveryId used for sending the message
     * @param messageText
     *            The text to be contained in the message.
     * @param state
     *            The state of the Work instance upto which we should wait
     * @param waitTime
     *            The maximum time to wait for the Work to reach the specified
     *            state.
     * @param xid
     *            The xid of the transaction associated with the parent Work
     * @param workExecutionType
     *            The method used for executing the parent work.
     * @param wi
     *            The workinformation that is used to configure the details of
     *            the SeurityContext
     * @return Whether the sending of the message was successful(true) or
     *         not(false).
     */
    @Override
    public MessageEndpointTestResultsImpl sendMessage(String deliveryId,
                                                      String messageText, int state, int waitTime, Xid xid,
                                                      int workExecutionType, WorkInformation wi) throws Exception {
        MessageEndpointTestResultsImpl testResults = null;
        try {
            messageProvider.setResourceAdapter("eis/InsecTestDataSource");

            FVTMessage message = new FVTMessage();
            message.addTestResult("InsecEndPoint");
            message.add("InsecEndPoint", messageText);
            messageProvider.sendJCA16MessageWait(deliveryId, message, state,
                                                 waitTime, xid, workExecutionType, wi);
            testResults = (MessageEndpointTestResultsImpl) messageProvider
                            .getTestResult(deliveryId);
        } catch (Exception e) {
            throw e;
        }
        Exception ex = messageProvider.getWork(deliveryId).getWorkException();
        if (ex != null) {
            throw ex;
        }
        return testResults;

    }

    /**
     * This method internally looks up the FVTMessageProvider administered
     * object from the JNDI registry and uses it to send a message to the
     * Message End Point which is a Message Driven Bean SampleMdb. The message
     * is delivered twice as it is used to create a parent as well as nested
     * work and then submit the parent work to the work manager. Once the
     * message is sent we get the corresponding MessageEndpointTestResults array
     * object from the FVTMessageProvider and use it to validate that a message
     * has been sent successfully.
     *
     * @param deliveryId
     *            The deliveryId used for sending the message
     * @param messageText
     *            The text to be contained in the message.
     * @param state
     *            The state of the Work instance upto which we should wait
     * @param waitTime
     *            The maximum time to wait for the Work to reach the specified
     *            state.
     * @param xid
     *            The xid of the transaction associated with the parent Work
     * @param childXid
     *            The xid of the transaction associated with the child work.
     * @param workExecutionType
     *            The method used for executing the parent work.
     * @param nestedDoWorkType
     *            The method used for executing the child work.
     * @param wi
     *            The workinformation that is used to configure the details of
     *            the SeurityContext
     * @return an array of MessageEndpointTestResultsImpl objects
     * @throws Exception
     */
    @Override
    public MessageEndpointTestResultsImpl[] sendNestedMessage(
                                                              String deliveryId, String messageText, int state, int waitTime,
                                                              Xid xid, Xid childXid, int workExecutionType, int nestedDoWorkType,
                                                              WorkInformation wi) throws Exception {
        MessageEndpointTestResultsImpl[] testResults = new MessageEndpointTestResultsImpl[2];
        String childDeliveryId = deliveryId + "_child";
        try {
            messageProvider.setResourceAdapter("eis/InsecTestDataSource");

            FVTMessage message = new FVTMessage();
            message.addTestResult("InsecEndPoint");
            message.add("InsecEndPoint", messageText);

            messageProvider.sendJCA16MessageWaitNestedWork(deliveryId, message,
                                                           wi, state, xid, childXid, workExecutionType,
                                                           nestedDoWorkType);

            testResults[0] = (MessageEndpointTestResultsImpl) messageProvider
                            .getTestResult(deliveryId);
            testResults[1] = (MessageEndpointTestResultsImpl) messageProvider
                            .getTestResult(childDeliveryId);
        } catch (Exception e) {
            throw e;
        }
        Exception ex = messageProvider.getWork(deliveryId).getWorkException();
        Exception childEx = messageProvider.getWork(childDeliveryId)
                        .getWorkException();
        if (ex != null) {
            throw ex;
        }
        if (childEx != null) {
            throw childEx;
        }
        return testResults;
    }

}
