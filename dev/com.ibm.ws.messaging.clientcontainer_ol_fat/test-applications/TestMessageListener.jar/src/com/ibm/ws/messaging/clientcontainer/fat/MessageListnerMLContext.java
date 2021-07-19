package com.ibm.ws.messaging.clientcontainer.fat;

import javax.jms.IllegalStateRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;


public class MessageListnerMLContext implements MessageListener{
	public static int counter = 0;
	public static int exCounter = 0;
	public int exCnt = 0;
	public int excCnt=0;
	public int Contextcounter = 0;
	public int ContextexCounter = 0;
	public int ContextcounterUnrelated = 0;
	public int ContextexCounterUnrelated = 0;
	public int Contextcounterclose = 0;
	public int ContextexCounterclose = 0;
	public int ContextcountercloseUnrelated = 0;
	public int ContextexCountercloseUnrelated = 0;
	

	public void resetCounter()
		{
			counter =0;
			exCounter =0;
Contextcounter = 0;
ContextexCounter = 0;
ContextcounterUnrelated = 0;
ContextexCounterUnrelated = 0;
Contextcounterclose = 0;
ContextexCounterclose = 0;
ContextcountercloseUnrelated = 0;
ContextexCountercloseUnrelated = 0;
			
		}



	@Override
	public void onMessage(Message message) {
		
      
		
// stop related context	
		try {		
		TestCases_MessageListenerML.jmsContext1.stop();		
		Contextcounter++;
		} catch (IllegalStateRuntimeException e) {
			System.out.println("inside exception while stoping related context ");
			ContextexCounter++;		
			e.printStackTrace();
		}
		
		
		
//  stop unrelated context
		
		try {					
			TestCases_MessageListenerML.jmsContext2.stop();	
			ContextcounterUnrelated++;	
			System.out.println("unrelated context stop successfully ");
		} catch (IllegalStateRuntimeException e) {
			ContextexCounterUnrelated++;		
			e.printStackTrace();
		}		
		
		
// CLose related context		
		
		try {					
			TestCases_MessageListenerML.jmsContext1.close();
			Contextcounterclose++;	
			} catch (IllegalStateRuntimeException e) {
				System.out.println("inside exception while closeing related context ");
				ContextexCounterclose++;		
				e.printStackTrace();
		}				
		
		
// CLose Unrelated context		
		
				try {					
					TestCases_MessageListenerML.jmsContext2.close();
					ContextcountercloseUnrelated++;	
					System.out.println("unrelated context close successfully ");
					} catch (IllegalStateRuntimeException e) {
						ContextexCountercloseUnrelated++;		
						e.printStackTrace();
			}			
		
		

		
		if(Contextcounter == 0 && ContextexCounter ==1 && ContextcounterUnrelated == 1 && ContextexCounterUnrelated == 0 && Contextcounterclose == 0 && ContextexCounterclose ==1 && ContextcountercloseUnrelated ==1 && ContextexCountercloseUnrelated == 0)
		{
			counter++;
			
		new TestCases_MessageListenerML().setCounter(counter);
		
		new TestCases_MessageListenerML().setexCounter(exCounter);
		
		 String text = "Test passed";
         sendToReplyQueue(text);
		
		
		}else{
			exCounter++;
			
			new TestCases_MessageListenerML().setCounter(counter);
			
			new TestCases_MessageListenerML().setexCounter(exCounter);
			
		}

		
	}
	
	
	 public void sendToReplyQueue(String text) {

	        try {
	            QueueConnection conn = GetJMSResourcesML.qcf2.createQueueConnection();
	            conn.start();

	            try {
	                QueueSession session = conn.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
	                QueueSender sender = session.createSender( GetJMSResourcesML.jmsQueue3);

	                TextMessage reply = session.createTextMessage();
	                reply.setText(text);
	                sender.send(reply);

	            } finally {
	                conn.close();
	            }
	        } catch (Exception x) {
	            throw new RuntimeException(x);
	        }
	    }

	
	

	}
