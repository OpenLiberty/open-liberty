package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;


import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class CommsLPServlet extends HttpServlet{
    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource
    private UserTransaction tran;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(CommsLPServlet.class); // injection engine doesn't like this at the class level
        Tr.entry(this, tc, test);
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    /**
     * Look up administered objects.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testAdminObjects(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        // TODO: would be nice to use javax.jms.Queue interface instead of java.util.Queue - is javax.jms package available in Liberty yet?

        Object queue = new InitialContext().lookup("java:comp/env/eis/queue1");

        Object topic = new InitialContext().lookup("java:comp/env/eis/topic1");

    }

    /**
     * Look up a connection factory and use it to perform interactions.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testConnectionFactory(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Object cf = new InitialContext().lookup("java:comp/env/eis/cf2");
        Object tcf = new InitialContext().lookup("java:comp/env/eis/tcf");
        Object qcf = new InitialContext().lookup("java:comp/env/eis/qcf");

    }

    /**
     * Test sharable connections.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSharing(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Object cf = new InitialContext().lookup("java:comp/env/eis/cf1");
    }

    public void testInvocationTopicConnectionFactory(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        TopicConnection con = cf1.createTopicConnection();

        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()) + "Test Topic Connection Factory");
        con.start();
        TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        //javax.jms.Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        TopicSubscriber sub = session.createSubscriber(topic);

        TopicPublisher publisher = session.createPublisher(topic);

        publisher.publish(session.createTextMessage("OM Namaha Shivayya"));

        TextMessage msg = (TextMessage) sub.receive(10000);
        if (null == msg) {
            throw new Exception("No message received");
        }
        if (sub != null)
            sub.close();
        if (con != null)
            con.close();

        System.out.println("Pub/Sub response from God " + msg);

    }

    public void testCreateDurableSubscriber(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        TopicConnection con = cf1.createTopicConnection();

        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()) + "Test Topic Connection Factory");
        con.start();
        TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        //javax.jms.Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        TopicSubscriber sub = session.createDurableSubscriber(topic, "DURATEST");

        TopicPublisher publisher = session.createPublisher(topic);

        publisher.publish(session.createTextMessage("OM Namaha Shivayya"));

        TextMessage msg = (TextMessage) sub.receive(10000);

        if (null == msg) {
            throw new Exception("No message received");
        }
        if (sub != null)
            sub.close();
        if (con != null)
            con.close();
        System.out.println("Pub/Sub response from God " + msg);

    }

    public void testPublisher(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        TopicConnection con = cf1.createTopicConnection();
        int msgs = 5;
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()) + "Test Topic Connection Factory");

        TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

        TopicPublisher publisher = session.createPublisher(topic);
        for (int i = 0; i < msgs; i++) {
            publisher.publish(session.createTextMessage("OM Namaha Shivayya" + i));
        }

        System.out.println("Published  messages " + msgs);

    }

    public void testReceiveDurableMessages(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        TopicConnection con = cf1.createTopicConnection();
        int msgs = 100;
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()) + "Test Topic Connection Factory");
        con.start();
        TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

        TopicSubscriber sub = session.createDurableSubscriber(topic, "DURATEST");
        TextMessage msg = null;
        do {
            msg = (TextMessage) sub.receive(10000);
            System.out.println("Received  messages " + msg);
        } while (msg != null);

        if (sub != null)
            sub.close();
        if (con != null)
            con.close();
    }

    public void testDurableUnSubscribe(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        TopicConnection con = cf1.createTopicConnection();
        int msgs = 100;
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()) + "Test Topic Connection Factory");
        con.start();
        TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

        TopicSubscriber sub = session.createDurableSubscriber(topic, "DURATEST");
        TextMessage msg = null;
        do {
            msg = (TextMessage) sub.receive(10000);
            System.out.println("Received  messages " + msg);
        } while (msg != null);

        if (sub != null)
            sub.close();

        session.unsubscribe("DURATEST");

        if (con != null)
            con.close();
    }

    /**
     * Test sharable connections.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testInvocationQueueConnectionFactory(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()));

        QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        System.out.println("********** 1");

        //Queue queue = sessionSender.createQueue("defaultQueue");
        System.out.println("********** 2");
        QueueSender send = sessionSender.createSender(queue);
        System.out.println("********** 3");

        send.send(sessionSender.createTextMessage("OM Namaha Shivayya"));
        System.out.println("********** 4");

        QueueReceiver rec = sessionSender.createReceiver(queue);
        TextMessage msg = (TextMessage) rec.receive(5000);
        System.out.println("Receive response from God " + msg);

        if (con != null)
            con.close();

    }

    public void testQueueSendMessage(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()));

        QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        
        QueueSender send = sessionSender.createSender(queue);
        
        TextMessage msg = sessionSender.createTextMessage();
        msg.setStringProperty("COLOR", "BLUE");
        msg.setText("Queue Message");

        send.send(msg);
        
        if (con != null)
            con.close();

    }

    public void testQueueReceiveMessages(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()));

        QueueSession session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        
        QueueReceiver receive = session.createReceiver(queue);

        TextMessage msg = null;

        do {
            msg = (TextMessage) receive.receive(5000);
            System.out.println("Received  messages " + msg);
        } while (msg != null);

        if (con != null)
            con.close();

        System.out.println("testQueueReceiveMessages exit successfully");

    }

    public void testQueueReceiveMessagesSelector(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()));

        QueueSession session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        System.out.println("********** 1");
        QueueReceiver receive = session.createReceiver(queue, "COLOR='BLUE'");

        TextMessage msg = null;

        do {
            msg = (TextMessage) receive.receive(5000);
            System.out.println("Received  messages " + msg);
        } while (msg != null);

        if (con != null)
            con.close();

        System.out.println("testQueueReceiveMessagesSelector exit successfully");

    }

    public void testQueueReceiveTransacted(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        ut.begin();

        QueueConnection con = cf1.createQueueConnection();
        con.start();
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()));

        QueueSession session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        System.out.println("********** 1");
        QueueReceiver receive = session.createReceiver(queue, "COLOR='BLUE'");

        TextMessage msg = null;

        do {
            msg = (TextMessage) receive.receive(5000);
            System.out.println("Received  messages " + msg);
        } while (msg != null);

        if (con != null)
            con.close();
        ut.commit();
        System.out.println("testQueueReceiveTransacted exit successfully");

    }
    
    
    
    public void createConnectionandSendMessage() throws NamingException, JMSException
    {
    	String expectedExceptionClass = "com.ibm.websphere.sib.exception.SIResourceException";
    	 	
    	
        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
         
        QueueConnection con = cf1.createQueueConnection();
        con.start();
        System.out.println(Arrays.asList(cf1));
        System.out.println(Arrays.asList(cf1.getClass().getInterfaces()));
        
        QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        System.out.println("********** 1");

        System.out.println("********** 2");
        QueueSender send = sessionSender.createSender(queue);
        System.out.println("********** 3");

        TextMessage msg = sessionSender.createTextMessage();
        msg.setStringProperty("COLOR", "BLUE");
        msg.setText("Sent Message");

      
    	  send.send(msg);
    	  System.out.println("********** 4");
    	
    	  if(con!=null)
    		  con.close();
    	       
      
    }
  
    
    public void testQueueSendMessageExpectException(HttpServletRequest request, HttpServletResponse response) throws Throwable {
    	String expectedExceptionClass = "com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException";
    	String expectedExceptionClass1 = "com.ibm.websphere.sib.exception.SIResourceException";
    	try{
    		createConnectionandSendMessage();
    	        
        }catch (Exception ex1){
    	
    	Throwable causeEx = ex1.getCause();
    	System.out.println(causeEx);
    	String actualException = causeEx.getClass().getName();
    	System.out.println("******THE EXCEPTION NAME IS : " + causeEx.getClass().getName());
    	ex1.printStackTrace();
    	if (actualException.equals(expectedExceptionClass))
    	{
    		System.out.println("SIConnection Dropped exception was thrown");
    		System.out.println("Retrying connection:");
    		try{
    			createConnectionandSendMessage();
    		}catch (JMSException ex){
    	    	
    	    	Throwable causeEx1 = ex.getCause();
    	    	String actualException1 = causeEx1.getClass().getName();
    	    	    	   
    	    	if(actualException1.equals(expectedExceptionClass1)){
    	    		System.out.println("Expected SIResource exception was thrown");
    	    	}
    	    	
    	    	else
    	    	{
    	    		System.out.println("Unexpected exception was thrown");
    	    		try {
    					throw new WrongException(actualException);
    				} catch (WrongException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    	    	}
    	
    	}
    	}
    	else if(actualException.equals(expectedExceptionClass1)){
    		System.out.println("Expected exception was thrown");
    	}
    	
    	else
    	{
    		System.out.println("Unexpected exception was thrown");
    		throw new WrongException(actualException);
    	}
    	
    }

     
        
    
    }  
    
    


    public class WrongException extends Exception {
		
		String str;
		public WrongException (String str){
			this.str = str;
		}
		
		public String toString(){
	        return "This is not the expected exception" + " " +str ;
	    }
		
	}
    }
