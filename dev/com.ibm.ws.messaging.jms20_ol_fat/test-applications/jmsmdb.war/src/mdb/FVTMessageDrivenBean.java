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
import javax.jms.Message;
import javax.jms.MessageListener;

@MessageDriven
public class FVTMessageDrivenBean implements MessageListener {
	static int i=0;
	
    @Resource
    MessageDrivenContext ejbcontext;

    @SuppressWarnings("unused")
    @Resource
    private void setMessageDrivenContext(EJBContext ejbcontext) {
        System.out.println("TODO: remove this if we don't need it: setMessageDrivenContext invoked");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("TODO: remove this if we don't need it: postConstruct invoked");
    }
    /* Simple Basic onMessage Method for consuming Message.
     * Whenever Message will reach on defined Queue this method will be called.
	*/
    @Override
    public void onMessage(Message message) {
        try {
            i++;
        	System.out.println("The Message No Received="+i);
        	System.out.println((new StringBuilder()).append("Message received on mdb").append(message).toString());
        	System.out.println("JMSXDeliveryCount value is" + message.getIntProperty("JMSXDeliveryCount"));
        	
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
