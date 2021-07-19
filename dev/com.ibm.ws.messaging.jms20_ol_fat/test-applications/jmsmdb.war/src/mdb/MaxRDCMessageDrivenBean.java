/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package mdb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

@MessageDriven
public class MaxRDCMessageDrivenBean implements MessageListener {
    static int i = 0;

    @Resource
    MessageDrivenContext ejbcontext;

    @Resource(name = "jndi_JMS_BASE_QCF")
    QueueConnectionFactory qcf;
    @Resource(name = "jndi_INPUT_Q")
    Queue replyQueue;

    @SuppressWarnings("unused")
    @Resource
    private void setMessageDrivenContext(EJBContext ejbcontext) {
        System.out.println("TODO: remove this if we don't need it: setMessageDrivenContext invoked");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("TODO: remove this if we don't need it: postConstruct invoked");
    }

    /*
     * onMessage Method for testing RedeliveryCount.
     * Runtime Exception is thrown to ensure redelivery of message.
     * Redelivery will be hapened till max value=5.
     */

    @Override
    public void onMessage(Message message) {
        i++;
        try {
            System.out.println("MaxRDCMessageDrivenBean: The Message No = " + i);
            if (i == 1) {
                throw new RuntimeException();
            }
            if (i == 2) {
                throw new RuntimeException();
            }

            if (i == 3) {
                System.out.println("ERROR: Message has been redelivered after max RDC value was reached");

                String text = "ERROR: " + ((TextMessage) message).getText();
                sendToReplyQueue(text);
            }
        } catch (JMSException e) {

            e.printStackTrace();
        }
    }

    public void sendToReplyQueue(String text) {

        try {
            QueueConnection conn = qcf.createQueueConnection();
            conn.start();

            try {
                QueueSession session = conn.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
                QueueSender sender = session.createSender(replyQueue);

                TextMessage reply = session.createTextMessage();
                reply.setText(text);
                System.out.println("MDB sending reply: " + reply);
                sender.send(reply);
                System.out.println("MDB sent reply");

            } finally {
                conn.close();
            }
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

}
