/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012,2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 * 
 * ============================================================================
 *
 * Change activity:
 *
 * Reason          Date      Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 93171           07022013 chetbhat Increasing the message receive wait period
 * ============================================================================
 */
package mdb;

import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven
public class FATTopic2MessageDrivenBean implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("Message Received in MDB2: "
                               + ((TextMessage) message).getText());

        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
