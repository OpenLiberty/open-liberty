/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Enumeration;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSDCFSecurityServlet extends HttpServlet {
    static final TraceComponent tc = Tr.register(JMSDCFSecurityServlet.class);
   
    /** @return the methodName of the caller. */
    private static final String methodName() { return new Exception().getStackTrace()[1].getMethodName(); }
    
    private final class TestException extends Exception {
        TestException(String message) {
            super(new Date() +" "+message);
        }
        TestException(String message, Throwable cause) {
            super(new Date() +" "+message, cause);
        }
    }
    
    private Queue queue;
    private Topic topic;
   
    @Override
    public void init() throws ServletException {
        Tr.entry(this, tc, "init");
        
        super.init();
        try {
            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
            topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

        } catch (NamingException e) {
             Tr.error(tc, "NamingException", e);
             e.printStackTrace();
             Tr.exit(this, tc, "init",e);
             throw new ServletException("Naming Exception setting queue and topic", e);
        }

        Tr.exit(this, tc, "init");
    }

    @Resource(lookup = "java:comp/DefaultJMSConnectionFactory")
    ConnectionFactory defaultConnectionFactory;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        Tr.entry(this, tc, "doGet", new Object[] {request, response});
        String test = request.getParameter("test");
        Tr.debug(this, tc, test);
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
         try {
            System.out.println(" Start: " + test);
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class)
                      .invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" End: " + test);
            Tr.exit(this, tc, "doGet", test);
       
         } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Error: " + test);
            x.printStackTrace(out);
            out.println("</pre>");
            Tr.exit(this, tc, "doGet", x);           
        }
    }

    public void testP2P_TCP_SecOn(HttpServletRequest request,
                                  HttpServletResponse response) 
        throws TestException, JMSException {

        // Use the default context so that the message is produced and consumed using the 
        // servlet transaction which is committed after the servlet returns.
        try (JMSContext jmsContext = defaultConnectionFactory.createContext()) {
            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + new Date());

            jmsContext.createProducer().send(queue, sentMessage);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(500);
            
            if (receivedMessage == null)                    
                throw new TestException("No message received, sent:"+sentMessage);
            if (!receivedMessage.getText().equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessage+"\n sent:"+sentMessage);
        }       
    }

    public void testPubSub_TCP_SecOn(HttpServletRequest request,
                                     HttpServletResponse response) 
        throws TestException, JMSException {

        // It is possible that a later subscriber in may receive a publication made from this test,
        // because the published message does not commit until after doGet() ends.
        try (JMSContext jmsContext = defaultConnectionFactory.createContext()) { 
            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + new Date());
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            jmsContext.createProducer().send(topic, sentMessage);

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(500);
            if (receivedMessage == null)                    
                throw new TestException("No message received, sent:"+sentMessage);
            if (!receivedMessage.getText().equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessage+"\n sent:"+sentMessage);
        }
    }

    public void testPubSubDurable_TCP_SecOn(HttpServletRequest request,
                                            HttpServletResponse response)
        throws TestException, JMSException {

        final String subscriptionName = methodName();
        // It is possible that a later subscriber in may receive a publication made from this test,
        // because the published message does not commit until after doGet() ends.
        try (JMSContext jmsContext = defaultConnectionFactory.createContext()) {
            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + new Date());
            JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, subscriptionName);
            jmsContext.createProducer().send(topic, sentMessage);

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(500);
            
            // Remove the durable subscription, in case the test is repeated.
            jmsConsumer.close();
            jmsContext.unsubscribe(subscriptionName);
            
            if (receivedMessage == null)                    
                throw new TestException("No message received, sent:"+sentMessage);
            if (!receivedMessage.getText().equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessage+"\n sent:"+sentMessage);
        }
    }

    public void testP2PMQ_TCP_SecOn(HttpServletRequest request,
                                    HttpServletResponse response) 
        throws TestException {

        try {
            defaultConnectionFactory.createContext();

        } catch (java.lang.RuntimeException e) {
            String causeExceptionName = e.getCause().getClass().getName();           
            if (!(causeExceptionName.equals("com.ibm.wsspi.sib.core.exception.SIAuthenticationException")))
               throw new TestException("Wrong exception thrown, exception:"+ e.getMessage(), e);   
        }
    } 
}