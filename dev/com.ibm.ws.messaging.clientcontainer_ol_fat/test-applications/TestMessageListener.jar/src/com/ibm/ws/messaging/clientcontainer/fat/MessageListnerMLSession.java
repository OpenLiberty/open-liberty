package com.ibm.ws.messaging.clientcontainer.fat;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

public class MessageListnerMLSession implements MessageListener{
	public static int counter = 0;
	public static int exCounter = 0;
	public int exCnt = 0;
	public int excCnt=0;
	public int Sessioncounterclose = 0;
	public int SessionexCounterclose = 0;
	public int SessioncountercloseUnrelated = 0;
	public int SessionCounterexcloseUnrelated = 0;
	

	public void resetCounter()
		{
			counter =0;
			exCounter =0;
			Sessioncounterclose = 0;
			SessionexCounterclose = 0;
			SessioncountercloseUnrelated = 0;
			SessionCounterexcloseUnrelated = 0;
			
		}


	@Override
	public void onMessage(Message message) {
			
		
		
// CLose related session		
		
		try {					
			TestCases_MessageListenerML.session.close();
			Sessioncounterclose++;	
		} catch (Exception e) {
			if((e.getClass().getName().toString().equals("javax.jms.IllegalStateException"))){
				System.out.println("inside exception while closeing related session ");
				SessionexCounterclose++;		
			}
				e.printStackTrace();
		}				
		
		
// CLose Unrelated session		
		
				try {					
					TestCases_MessageListenerML.session1.close();
					SessioncountercloseUnrelated++;	
					System.out.println("unrelated session close successfully ");
				} catch (Exception e) {
					if((e.getClass().getName().toString().equals("javax.jms.IllegalStateException"))){
						SessionCounterexcloseUnrelated++;		
					}
						e.printStackTrace();
			}			
		

		if(Sessioncounterclose == 0 && SessionexCounterclose ==1 && SessioncountercloseUnrelated == 1 && SessionCounterexcloseUnrelated == 0 )
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
