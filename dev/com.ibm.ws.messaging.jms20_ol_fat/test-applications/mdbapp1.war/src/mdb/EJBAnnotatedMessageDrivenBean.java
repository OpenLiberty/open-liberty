/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package mdb;

/**
 *
 */
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(
               name = "MDBQueue",
               activationConfig = {
                                   @ActivationConfigProperty(propertyName = "destinationType",
                                                             propertyValue = "javax.jms.Queue"),

                                   @ActivationConfigProperty(propertyName = "destinationLookup",
                                                             propertyValue = "Queue11/test"),

                                   @ActivationConfigProperty(propertyName = "acknowledgeMode",
                                                             propertyValue = "Auto-acknowledge"),

                                   @ActivationConfigProperty(propertyName = "connectionFactoryLookup",
                                                             propertyValue = "jndi_JMS_BASE_QCF"),

               })
public class EJBAnnotatedMessageDrivenBean implements MessageListener {

    @Override
    public void onMessage(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            System.out.println((new StringBuilder()).append(message).toString());
            System.out.println("Message received on Annotated MDB: " + msg.getText());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
