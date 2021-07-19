package com.ibm.ws.messaging.clientcontainer.fat;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;



public class MessageListnerMLConnection implements MessageListener{
	public static int counter = 0;
	public static int exCounter = 0;
	public int exCnt = 0;
	public int excCnt=0;
	public int Connectioncounter = 0;
	public int ConnectionexCounter = 0;
	public int ConnectioncounterUnrelated = 0;
	public int ConnectionexCounterUnrelated = 0;
	public int Connectioncounterclose = 0;
	public int ConnectionexCounterclose = 0;
	public int ConnectioncountercloseUnrelated = 0;
	public int ConnectionexCountercloseUnrelated = 0;
	

	public void resetCounter()
		{
			counter =0;
			exCounter =0;
			Connectioncounter = 0;
			ConnectionexCounter = 0;
			ConnectioncounterUnrelated = 0;
			ConnectionexCounterUnrelated = 0;
			Connectioncounterclose = 0;
			ConnectionexCounterclose = 0;
			ConnectioncountercloseUnrelated = 0;
			ConnectionexCountercloseUnrelated = 0;
			
		}



	@Override
	public void onMessage(Message message) {
		
      
		
// stop related connection	
		try {		
		TestCases_MessageListenerML.con.stop();		
		Connectioncounter++;
		} catch (Exception e) {
			System.out.println("inside exception while stoping related Connection ");
		if((e.getClass().getName().toString().equals("javax.jms.IllegalStateException"))){
				ConnectionexCounter++;
		}
			
					
			e.printStackTrace();
		}
		
		
		
//  stop unrelated connection
		
		try {					
			TestCases_MessageListenerML.con1.stop();	
			ConnectioncounterUnrelated++;	
			System.out.println("unrelated Connection stop successfully ");
		} catch (Exception e) {
			if((e.getClass().getName().toString().equals("javax.jms.IllegalStateException"))){
			ConnectionexCounterUnrelated++;		
			}
			e.printStackTrace();
		}		
		
		
// CLose related connection		
		
		try {					
			TestCases_MessageListenerML.con.close();
			Connectioncounterclose++;	
		} catch (Exception e) {
			if((e.getClass().getName().toString().equals("javax.jms.IllegalStateException"))){
				System.out.println("inside exception while closeing related Connection ");
				ConnectionexCounterclose++;		
			}
				e.printStackTrace();
		}				
		
		
// CLose Unrelated connection		
		
				try {					
					TestCases_MessageListenerML.con1.close();
					ConnectioncountercloseUnrelated++;	
					System.out.println("unrelated Connection close successfully ");
				} catch (Exception e) {
					if((e.getClass().getName().toString().equals("javax.jms.IllegalStateException"))){
						ConnectionexCountercloseUnrelated++;		
					}
						e.printStackTrace();
			}			
		
		

		
		if(Connectioncounter == 0 && ConnectionexCounter ==1 && ConnectionexCounterUnrelated == 0 && ConnectioncounterUnrelated == 1 && Connectioncounterclose == 0 && ConnectionexCounterclose ==1 && ConnectioncountercloseUnrelated ==1 && ConnectionexCountercloseUnrelated == 0)
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
