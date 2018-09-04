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
import javax.jms.TextMessage;

@MessageDriven
public class RDC2MessageDrivenBean implements MessageListener {
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
    /*  onMessage Method for testing RedeliveryCount.
     *  Runtime Exception is thrown to ensure redelivery of message.
     *  Redelivery will be hapened till max value=5.
	*/
    @Override
    public void onMessage(Message message) {
    	i++;
        try {
        	System.out.println("The Message No ="+i);
        	if (i==1){
        	    System.out.println("Clearing message body");
        	    message.clearBody();
        	}
        	if(i==2){
        	    String text = ((TextMessage)message).getText();
        	    System.out.println("The message text upon redelivery is " + text);
        	}
        	    
        	System.out.println("JMSXDeliveryCount value is " + (message.getIntProperty("JMSXDeliveryCount")));
        
        	if((message.getBooleanProperty("JMSRedelivered")) && (message.getIntProperty("JMSXDeliveryCount") >= 2)){
        	        System.out.println("JMSRedelivered is set and JMSXDeliveryCount is > 2 as expected");
        	}
       
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
        throw new RuntimeException();
    }
}
