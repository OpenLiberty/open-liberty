package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.Enumeration;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.NamingException;

public class TestCases_MessageListenerML {

	public JMSProducer jmsProducer = null;
	public JMSConsumer jmsConsumer = null;
	public static QueueSession session = null;
	public static QueueSession session1 = null;
	public QueueBrowser qb =null;
	public MessageConsumer mc1 = null;
	public Queue jmsQueue = null;
	public Queue jmsQueue3 = null;
	public Message message = null;
	public static QueueConnectionFactory qcf = null;
	public static QueueConnectionFactory qcf1 = null;
	public static QueueConnectionFactory qcf2 = null;
    public static QueueConnection con =null;
    public static QueueConnection con1 =null;
    public static QueueConnection con2 =null;
	public static JMSContext jmsContext = null;
	public static JMSContext jmsContext1 = null;
	public static JMSContext jmsContext2 = null;
	


	public static boolean exceptionFlag;

	public static int count = 0;
	public static int excount = 0;



	public String initialPrep() {

		try {
			GetJMSResourcesML.establishConnection();
		} catch (NamingException e) {
			e.printStackTrace();
		}	

		qcf = GetJMSResourcesML.getQCF();
		qcf1 = GetJMSResourcesML.getQCF1();
		qcf2 = GetJMSResourcesML.getQCF2();
		jmsQueue = GetJMSResourcesML.getJMSQueue();
		jmsQueue3 = GetJMSResourcesML.getJMSQueue3();
		return ("initial Prep done");

	}



public int setCounter(int counter) {
	count = counter;
	return count;
}
	
public void setexCounter(int exCounter) {
	excount = exCounter;
	System.out.println("exCounter:" + exCounter);
}


	
public static void resetCounter() {
	count = 0;
	excount = 0;

	
	
}


	public void testMessageListener_Contexttest() throws JMSException {
		
		MessageListnerMLContext ContextML = new MessageListnerMLContext();
		clearQueue(jmsQueue);
		ContextML.resetCounter();
		resetCounter();

		 exceptionFlag = false;
		 con2 = qcf2.createQueueConnection();
		con2.start();
		jmsContext1 = qcf.createContext();	
		jmsContext2 = qcf1.createContext();
        JMSProducer jp =  jmsContext1.createProducer();
        JMSConsumer jc =  jmsContext1.createConsumer(jmsQueue);
        QueueSession session2 = con2.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        TextMessage msg = jmsContext1.createTextMessage();        
        msg.setText("hello");
        jp.send(jmsQueue, msg);        
       
        
        jc.setMessageListener(ContextML);
  
       // Receive message sent by on Message on queue3 
        MessageConsumer mc1 = session2.createConsumer(jmsQueue3);
        mc1.receive(5000);
   

        
        if(count == 1){
        	System.out.println("Test case : testMessageListener_Contexttest passed");	
        }
        
        
        jmsContext1.close();
        jmsContext2.close();	
        
        if (con2 != null)
            con2.close(); 
	}
	
	


public void testMessageListener_Connectionttest() throws JMSException { 
	MessageListnerMLConnection ConnectionML = new MessageListnerMLConnection();
	clearQueue(jmsQueue);
	ConnectionML.resetCounter();
	resetCounter();
	exceptionFlag = false;
    con = qcf.createQueueConnection();
    con1 = qcf1.createQueueConnection();
    con2 = qcf2.createQueueConnection();
    con.start();
    con1.start();    
    con2.start();
    QueueSession session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    QueueSession session2 = con2.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageProducer mp = session.createProducer(jmsQueue);
    MessageConsumer mc = session.createConsumer(jmsQueue);
    TextMessage msg = session.createTextMessage();    
    msg.setText("hello");
    mp.send(msg); 
    mc.setMessageListener(ConnectionML); 

    MessageConsumer mc1 = session2.createConsumer(jmsQueue3);
    mc1.receive(5000);

    
    if(count == 1){
    	System.out.println("Test case : testMessageListener_Connectionttest passed");	
    }
   
    if (con != null)
        con.close();
    
    if (con1 != null)
        con1.close(); 
    
    if (con2 != null)
        con2.close(); 
       
}


 	



public void testMessageListener_sessiontest() throws JMSException {    
	MessageListnerMLSession SessionML = new MessageListnerMLSession();
	SessionML.resetCounter();
	resetCounter();
	clearQueue(jmsQueue);	   
	exceptionFlag = false;
    con = qcf.createQueueConnection();
    con1 = qcf1.createQueueConnection();
    con2 = qcf2.createQueueConnection();
    con.start();
    con1.start();
    con2.start();
     session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
     session1 = con1.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
     QueueSession  session2 = con2.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageProducer mp = session.createProducer(jmsQueue);
    MessageConsumer mc = session.createConsumer(jmsQueue);
    TextMessage msg = session.createTextMessage(); 
     msg.setText("hello");
     mp.send(msg);    
    mc.setMessageListener(SessionML);
    
    // Receive message sent by on Message on queue3 
    MessageConsumer mc1 = session2.createConsumer(jmsQueue3);
    mc1.receive(5000);

    if(count == 1){
    	System.out.println("Test case : testMessageListener_sessiontest passed");	
    }
   
    if (con != null)
        con.close();
    
    if (con1 != null)
        con1.close();
    
    if (con2 != null)
        con2.close();
   
       
}



public void clearQueue(Queue queue) throws JMSException {

	JMSContext jmsCxt = qcf.createContext();
	qb = jmsCxt.createBrowser(queue);
	JMSConsumer jmsCons = jmsCxt.createConsumer(queue);

	Enumeration e = qb.getEnumeration();

	int count = 0;
	// count number of messages
	while (e.hasMoreElements()) {
		e.nextElement();
		count++;
	}

	for (int i = count; count > 0; count--) {
		jmsCons.receiveNoWait();

	}
	
	jmsCxt.close();
	
}
	

}
