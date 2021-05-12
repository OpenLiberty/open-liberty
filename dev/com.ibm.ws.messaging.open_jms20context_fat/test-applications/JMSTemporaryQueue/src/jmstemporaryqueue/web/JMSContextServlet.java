/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmstemporaryqueue.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSContextServlet extends HttpServlet {

    public static QueueConnectionFactory qcfBindings;
    public static QueueConnectionFactory qcfTCP;

    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Queue queue;
    public static Topic topic;
    public static Topic topic2;
    
    /** @return the methodName of the caller. */
    private static final String methodName() { return new Exception().getStackTrace()[1].getMethodName(); }
    
    private final class TestException extends Exception {
        TestException(String message) {
            super(new Date() +" "+message);
        }
    }
    
    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws JMSException {
        JMSContext context = qcf.createContext();

        QueueBrowser qb = context.createBrowser(q);
        Enumeration<?> e = qb.getEnumeration();

        JMSConsumer consumer = context.createConsumer(q);

        int numMsgs = 0;
        while ( e.hasMoreElements() ) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        for ( int i = 0; i < numMsgs; i++ ) {
            Message message = consumer.receive();
        }

        context.close();
    }

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            qcfBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF':\n" + qcfBindings);

        try {
            qcfTCP = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1':\n" + qcfTCP);

        try {
            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'java:comp/env/jndi_INPUT_!':\n" + queue);

        try {
            tcfBindings = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf':\n" + tcfBindings);

        try {
            tcfTCP = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf1':\n" + tcfTCP);

        try {
            topic = (Topic) new InitialContext().lookup("eis/topic1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'eis/topic1':\n" + topic);

        try {
            topic2 = (Topic) new InitialContext().lookup("eis/topic2");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'eis/topic2':\n" + topic2);

        if ( qcfBindings == null ) {
            throw new ServletException("Null 'qcfBindings'");
        }
        if ( qcfTCP == null ) {
            throw new ServletException("Null 'qcfTCP'");
        }

        if ( tcfBindings == null ) {
            throw new ServletException("Null 'tcfBindings'");
        }
        if ( tcfTCP == null ) {
            throw new ServletException("Null 'tcfTCP'");
        }

        if ( queue == null ) {
            throw new ServletException("Null 'queue'");
        }
        if ( topic == null ) {
            throw new ServletException("Null 'topic'");
        }
        if ( topic2 == null ) {
            throw new ServletException("Null 'topic2'");
        }
    }

    /**
     * Handle a GET request to this servlet: Invoke the test method specified as
     * request paramater "test".
     *
     * The test method throws an exception when it fails.  If no exception
     * is thrown by the test method, indicate success through the response
     * output.  If an exception is thrown, omit the success indication.
     * Instead, display an error indication and display the exception stack
     * to the response output.
     *
     * @param request The HTTP request which is being processed.
     * @param response The HTTP response which is being processed.
     *
     * @throws ServletException Thrown in case of a servlet processing error.
     * @throws IOException Thrown in case of an input/output error.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String test = request.getParameter("test");

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(JMSContextServlet.class);

        Tr.entry(this, tc, test);
        try {
            System.out.println(" Starting : " + test);
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class)
                      .invoke(this, request, response);
          
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" Ending : " + test);
            Tr.exit(this, tc, test);

        } catch ( Throwable e ) {
            if ( e instanceof InvocationTargetException ) {
                e = e.getCause();
            }

            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Ending : " + test);
            System.out.println(" <ERROR> " + e.getMessage() + " </ERROR>");
            e.printStackTrace(out);
            out.println("</pre>");

            Tr.exit(this, tc, test, e);
        }
    }

    /*****************************************************************************************************
     * Publish subscribe tests.                                                                          * 
     *****************************************************************************************************/
    
    public void testReceiveMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveMessageTopicSecOff_TCP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
     
        // Use the default context so that the message is produced and consumed using the 
        // servlet transaction which is committed after the servlet returns.
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {  
            // Create the consumer first, JMS does not guarantee when the first message will be received
            // but in practice the subscription will have been made before the message has been sent. 
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            jmsProducer.send(topic, "");
            // The receive call blocks indefinitely until a message is produced or until the JMSConsumer is closed.
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive();
            if (!receivedMessage.getText().equals(""))
                throw new TestException("Wrong message received:"+receivedMessage);
        }
    }

    public void testReceiveMessageTopicTranxSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveMessageTopicTranxSecOff(tcfBindings);
    }

    public void testReceiveMessageTopicTranxSecOff_TCP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveMessageTopicTranxSecOff(tcfTCP);
    }

    private void testReceiveMessageTopicTranxSecOff(TopicConnectionFactory topicConnectionFactory)
            throws NamingException, NotSupportedException, SystemException, HeuristicMixedException,
            HeuristicRollbackException, RollbackException, JMSException, TestException {
       
        UserTransaction userTransaction = null;
        JMSContext jmsContext = null;
        try {
            userTransaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            userTransaction.begin();

            jmsContext = topicConnectionFactory.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage(methodName()+" at "+new Date());
            jmsProducer.send(topic, sentMessage);

            userTransaction.commit();

            userTransaction.begin();
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive();
            userTransaction.commit();
            if (!receivedMessage.getText().equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessage+" sent:"+sentMessage);

        } finally {
            // If we reach here in the event of an error, the transaction might not have been completed.
            if (userTransaction != null && userTransaction.getStatus() == Status.STATUS_ACTIVE)
                userTransaction.rollback();
            if (jmsContext != null)
                jmsContext.close();
        }
    }

    public void testReceiveTimeoutMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveTimeoutMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveTimeoutMessageTopicSecOff_TCP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveTimeoutMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveTimeoutMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumerTCFTCP = jmsContext.createConsumer(topic);
            JMSProducer jmsProducerTCFTCP = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + new Date());
            jmsProducerTCFTCP.send(topic, sentMessage);
            TextMessage receivedMessage = (TextMessage) jmsConsumerTCFTCP.receive(30000);
            if (!receivedMessage.getText().equals(sentMessage.getText()))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
        }
    }

    public void testReceiveNoWaitMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveNoWaitMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveNoWaitMessageTopicSecOff_TCP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveNoWaitMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveNoWaitMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage(methodName()+" at "+new Date());
            jmsProducer.send(topic, sentMessage);
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receiveNoWait();
            if (!receivedMessage.getText().equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessage+" sent:"+sentMessage);
        }
    }

    public void testReceiveNoWaitNullMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveNoWaitNullMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveNoWaitNullMessageTopicSecOff_TCP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveNoWaitNullMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveNoWaitNullMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

            Message receivedMessage = jmsConsumer.receiveNoWait();
            if (receivedMessage != null)
                throw new TestException("Wrong message received:"+receivedMessage);
        }
    }

    public void testReceiveBodyTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage(methodName()+" at "+new Date());
            jmsProducer.send(topic, sentMessage);
            String receivedMessageBody = jmsConsumer.receiveBody(String.class);
            if (!receivedMessageBody.equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessage); 
        }
    }

    public void testReceiveBodyTransactionTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTransactionTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTransactionTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTransactionTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTransactionTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws NamingException, NotSupportedException, SystemException, HeuristicMixedException,
            HeuristicRollbackException, RollbackException, TestException {
       
        UserTransaction userTransaction = null;
        JMSContext jmsContext = null;
        try {
            userTransaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            userTransaction.begin();

            jmsContext = topicConnectionFactory.createContext();

            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            String sentMessageBody = methodName()+" at "+new Date();
            jmsProducer.send(topic, sentMessageBody);

            userTransaction.commit();

            userTransaction.begin();
            String receivedMessageBody = jmsConsumer.receiveBody(String.class);
            userTransaction.commit();
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessageBody); 
           
        } finally {
            if (userTransaction != null && userTransaction.getStatus() == Status.STATUS_ACTIVE)
                userTransaction.rollback();
            if (jmsContext != null)
                jmsContext.close();

        }
    }

    public void testReceiveBodyTextMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTextMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTextMessageTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTextMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTextMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory) 
        throws InterruptedException, JMSException, TestException {
   
        try (JMSContext jmsContext = topicConnectionFactory.createContext();) {          
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage(methodName()+" at "+new Date());
            jmsProducer.send(topic, sentMessage);
            String receivedMessageBody = jmsConsumer.receiveBody(String.class);
            if (!receivedMessageBody.equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessage);
        }
    }

    public void testReceiveBodyObjectMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyObjectMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyObjectMessageTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyObjectMessageTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyObjectMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory) 
        throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {            
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            Object sentMessageBody = new String(methodName()+" at "+new Date());
            ObjectMessage sentMessage = jmsContext.createObjectMessage();
            sentMessage.setObject((Serializable) sentMessageBody);
            jmsProducer.send(topic, sentMessage);

            Object receivedMessageBody = jmsConsumer.receiveBody(Serializable.class);        
            if (receivedMessageBody == null)
                throw new TestException("No message received, sent:"+sentMessage); 
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessageBody); 
        }
    }

    public void testReceiveBodyMapMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyMapMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyMapMessageTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyMapMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyMapMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, Exception {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            MapMessage sentMessage = jmsContext.createMapMessage();
            sentMessage.setString("Name", "IBM");
            sentMessage.setString("Team", "WAS");

            jmsProducer.send(topic, sentMessage);
            Map<?, ?> receivedMessageBody = jmsConsumer.receiveBody(Map.class);
            if (!receivedMessageBody.equals(sentMessage.getBody(Map.class)))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessage);
        }
    }

    public void testReceiveBodyByteMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyByteMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyByteMessageTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyByteMessageTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyByteMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            byte[] data = new String(methodName() + " at " + new Date()).getBytes();
            BytesMessage sentMessage = jmsContext.createBytesMessage();
            sentMessage.writeBytes(data);

            jmsProducer.send(topic, sentMessage);
            byte[] receivedMessageBody = jmsConsumer.receiveBody(byte[].class);
            if (!Arrays.equals(receivedMessageBody, sentMessage.getBody(byte[].class)))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessage);
        }
    }

    public void testReceiveBodyTimeOutTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTimeOutTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTimeOutTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyTimeOutTopicSecOff(TopicConnectionFactory topicConnectionFactory) throws TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            String sentMessageBody = methodName()+" at "+new Date();
            jmsProducer.send(topic, sentMessageBody);
            String receivedMessageBody = jmsConsumer.receiveBody(String.class, 30000);
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessageBody);
        }
    }

    public void testReceiveBodyTimeOutTransactionTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTimeOutTransactionTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutTransactionTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyTimeOutTransactionTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws NamingException, NotSupportedException, SystemException, HeuristicMixedException,
            HeuristicRollbackException, RollbackException, TestException {
        
        UserTransaction userTransaction = null;
        JMSContext jmsContext = null;
        try {
            userTransaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            userTransaction.begin();

            jmsContext = topicConnectionFactory.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
  
            String sentMessageBody = methodName()+" at "+new Date();
            jmsProducer.send(topic, sentMessageBody);
            
            userTransaction.commit();

            userTransaction.begin();
            String receivedMessageBody = jmsConsumer.receiveBody(String.class, 30000);
            userTransaction.commit();           
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessageBody);
            
        } finally {
            if (userTransaction != null && userTransaction.getStatus() == Status.STATUS_ACTIVE)
                userTransaction.rollback();
            if (jmsContext != null)
                jmsContext.close();
        }
    }

    public void testReceiveBodyTimeOutTextMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodytimeOutTextMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodytimeOutTextMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodytimeOutTextMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {

        try (JMSContext jmsContext = topicConnectionFactory.createContext();) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + new Date());
            jmsProducer.send(topic, sentMessage);
            String receivedMessageBody = jmsConsumer.receiveBody(String.class, 30000);
            if (!receivedMessageBody.equals(sentMessage.getText()))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessage);
        }
    }

    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutObjectMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutObjectMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTimeOutObjectMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {

        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            Object sentMessageBody = new String(methodName() + " at " + new Date());
            ObjectMessage sentMessage = jmsContext.createObjectMessage();
            sentMessage.setObject((Serializable) sentMessageBody);
            jmsProducer.send(topic, sentMessage);

            Object receivedMessageBody = jmsConsumer.receiveBody(Serializable.class, 30000);
            if (receivedMessageBody == null)
                throw new TestException("No message received, sent:" + sentMessage);
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessageBody);
        }
    }

    public void testReceiveBodyTimeOutMapMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyTimeOutMapMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutMapMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTimeOutMapMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {

        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            MapMessage sentMessage = jmsContext.createMapMessage();
            sentMessage.setString("Name", "IBM");
            sentMessage.setString("Team", "WAS");

            jmsProducer.send(topic, sentMessage);
            Map<?, ?> receivedMessageBody = jmsConsumer.receiveBody(Map.class, 30000);
            if (!receivedMessageBody.equals(sentMessage.getBody(Map.class)))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessage);
        }
    }

    public void testReceiveBodyTimeOutByteMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutByteMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutByteMessageTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyTimeOutByteMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            byte[] data = new String(methodName() + " at " + new Date()).getBytes();
            BytesMessage sentMessage = jmsContext.createBytesMessage();
            sentMessage.writeBytes(data);

            jmsProducer.send(topic, sentMessage);
            byte[] receivedMessageBody = jmsConsumer.receiveBody(byte[].class, 30000);
            if (!Arrays.equals(receivedMessageBody, sentMessage.getBody(byte[].class)))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessage);
        }
    }
    
    public void testReceiveBodyNoWaitTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyNoWaitTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyNoWaitTopicSecOff_TCPIP(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyNoWaitTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyNoWaitTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, InterruptedException, TestException {

        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage(methodName()+" at "+new Date());
            jmsProducer.send(topic, sentMessage);
                  
            // JMS does not specify when the message is available to be received, 
            // so repeat attempts to receive the message.
            String receivedMessageBody = null;
            for (int i = 0; i<10 &&  receivedMessageBody == null; i++) {
              receivedMessageBody = jmsConsumer.receiveBodyNoWait(String.class);
              Thread.sleep(100);
            }

            if (receivedMessageBody == null)  
                throw new TestException("No message received sent:"+sentMessage);
            if (!receivedMessageBody.equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessage); 
        }
    }

    public void testReceiveBodyNoWaitTransactionTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitTransactionTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitTransactionTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyNoWaitTransactionTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws NamingException, NotSupportedException, SystemException, HeuristicMixedException,
            HeuristicRollbackException, RollbackException, JMSException, InterruptedException, TestException {
       
        UserTransaction userTransaction = null;
        JMSContext jmsContext = null;
        try {
            userTransaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            userTransaction.begin();

            jmsContext = topicConnectionFactory.createContext();

            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            String sentMessageBody = methodName()+" at "+new Date();
            jmsProducer.send(topic, sentMessageBody);

            userTransaction.commit();

            userTransaction.begin();           
            String receivedMessageBody = null;
            for (int i = 0; i<10 &&  receivedMessageBody == null; i++) {
              receivedMessageBody = jmsConsumer.receiveBodyNoWait(String.class);
              Thread.sleep(100);
            }
            userTransaction.commit();
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessageBody); 
           
        } finally {
            if (userTransaction != null && userTransaction.getStatus() == Status.STATUS_ACTIVE)
                userTransaction.rollback();
            if (jmsContext != null)
                jmsContext.close();

        }
    }
    
    public void testReceiveBodyNoWaitTextMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyNoWaitTextMessageTopicSecOff(tcfTCP);
    }

    public void testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitTextMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyNoWaitTextMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory) 
        throws JMSException, InterruptedException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext();) {          
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage(methodName()+" at "+new Date());
            jmsProducer.send(topic, sentMessage);
            
            String receivedMessageBody = null;
            for (int i = 0; i<10 &&  receivedMessageBody == null; i++) {
              receivedMessageBody = jmsConsumer.receiveBodyNoWait(String.class);
              Thread.sleep(100);
            }
            if (!receivedMessageBody.equals(sentMessage.getText()))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessage);
        }
    }
    
    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_B(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitObjectMessageTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitObjectMessageTopicSecOff(tcfTCP);
    }
        
    private void testReceiveBodyNoWaitObjectMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, InterruptedException, TestException {
        
        try (JMSContext jmsContextTCFBindings = topicConnectionFactory.createContext()) {           
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

            Object sentMessageBody = new String(methodName()+" at "+new Date());
            ObjectMessage sentMessage = jmsContextTCFBindings.createObjectMessage();
            sentMessage.setObject((Serializable) sentMessageBody);
            jmsProducer.send(topic, sentMessage);

            Object receivedMessageBody = null;
            for (int i = 0; i<10 && receivedMessageBody == null; i++) {
              receivedMessageBody = jmsConsumer.receiveBodyNoWait(Serializable.class);
              Thread.sleep(100);
            }

            if (receivedMessageBody == null)
                throw new TestException("No message received, sent:"+sentMessage); 
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessageBody);
        }
    }

    public void testReceiveBodyNoWaitMapMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMapMessageTopicSecOff(tcfBindings);
        
    }

    public void testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMapMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyNoWaitMapMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, InterruptedException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            MapMessage sentMessage = jmsContext.createMapMessage();
            sentMessage.setString("Name", "IBM");
            sentMessage.setString("Team", "WAS");

            jmsProducer.send(topic, sentMessage);
            Map<?, ?> receivedMessageBody = null;
            for (int i = 0; i<10 && receivedMessageBody == null; i++) {
              receivedMessageBody = jmsConsumer.receiveBodyNoWait(Map.class);
              Thread.sleep(100);
            }
            
            if (receivedMessageBody == null)
                throw new TestException("No message received, sent:"+sentMessage); 
            if (!receivedMessageBody.equals(sentMessage.getBody(Map.class)))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessage);
        }
    }
    
    public void testReceiveBodyNoWaitByteMessageTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyNoWaitByteMessageTopicSecOff(tcfBindings);

    }

    public void testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitByteMessageTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyNoWaitByteMessageTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, InterruptedException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            byte[] data = new String(methodName() + " at " + new Date()).getBytes();
            BytesMessage sentMessage = jmsContext.createBytesMessage();
            sentMessage.writeBytes(data);

            jmsProducer.send(topic, sentMessage);
            byte[] receivedMessageBody = jmsConsumer.receiveBodyNoWait(byte[].class);
            for (int i = 0; i<10 && receivedMessageBody == null; i++) {
              receivedMessageBody = jmsConsumer.receiveBodyNoWait(byte[].class);
              Thread.sleep(100);
            }
            if (receivedMessageBody == null)
                throw new TestException("No message received, sent:"+sentMessage); 
            if (!Arrays.equals(receivedMessageBody, sentMessage.getBody(byte[].class)))
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessage);
        }
    }
    
    public void testReceiveBodyMFENoBodyTopicSecOff_B (
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyMFENoBodyTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyMFENoBodyTopicSecOff_TCP (
            HttpServletRequest request, HttpServletResponse response) throws Exception {
            testReceiveBodyMFENoBodyTopicSecOff(tcfTCP);       
    }
    
    private void testReceiveBodyMFENoBodyTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage();
            jmsProducer.send(topic, sentMessage);
            try {
                String receivedMessageBody = jmsConsumer.receiveBody(String.class);
                // Should not reach here.
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessage);

            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }

    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyMFEUnspecifiedTypeTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyMFEUnspecifiedTypeTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyMFEUnspecifiedTypeTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage();
            jmsProducer.send(topic, sentMessage);

            try {
                byte[] receivedMessageBody = jmsConsumer.receiveBody(byte[].class);
                // Should not reach here.
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessage);
            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }

    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_B(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        testReceiveBodyMFEUnsupportedTypeTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        testReceiveBodyMFEUnsupportedTypeTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyMFEUnsupportedTypeTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            StreamMessage sentMessage = jmsContext.createStreamMessage();
            jmsProducer.send(topic, sentMessage);

            try {
                Object receivedMessageBody = jmsConsumer.receiveBody(Object.class);
                // Should not reach here.
                throw new TestException("Wrong message received:" + receivedMessageBody + "sent:" + sentMessage);
            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }

    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutMFENoBodyTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutMFENoBodyTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTimeOutMFENoBodyTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage();
            jmsProducer.send(topic, sentMessage);
            try {
                String receivedMessageBody = jmsConsumer.receiveBody(String.class, 30000);
                // Should not reach here.
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessage);

            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }
    
    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage();
            jmsProducer.send(topic, sentMessage);

            try {
                byte[] receivedMessageBody = jmsConsumer.receiveBody(byte[].class, 30000);
                // Should not reach here.
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessage);
            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }
    
    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            StreamMessage sentMessage = jmsContext.createStreamMessage();
            jmsProducer.send(topic, sentMessage);

            try {
                Object receivedMessageBody = jmsConsumer.receiveBody(Object.class, 30000);
                // Should not reach here.
                throw new TestException("Wrong message received:" + receivedMessageBody + "sent:" + sentMessage);
            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }

    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMFENoBodyTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMFENoBodyTopicSecOff(tcfTCP);
    }

    private void testReceiveBodyNoWaitMFENoBodyTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException, InterruptedException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage();
            jmsProducer.send(topic, sentMessage);
            try {
                // Repeat attempts to receive the message.
                String receivedMessageBody = null;
                for (int i = 0; i<10 && receivedMessageBody == null; i++) {
                  receivedMessageBody = jmsConsumer.receiveBodyNoWait(String.class);
                  Thread.sleep(100);
                }
                // Should not reach here.
                // We reach here if we either didn't receive a message after 10 attempts or we received 
                // the TextMessage with no body, instead of throwing MessageFormatRuntimeException.  
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessage);

            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }
    
    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff(tcfBindings);
    }

    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException, InterruptedException {
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            TextMessage sentMessage = jmsContext.createTextMessage();
            jmsProducer.send(topic, sentMessage);

            try {   
                // Repeat attempts to receive the message.
                byte[] receivedMessageBody = null;
                for (int i = 0; i<10 && receivedMessageBody == null; i++) {
                  receivedMessageBody = jmsConsumer.receiveBodyNoWait(byte[].class);
                  Thread.sleep(100);
                }
                // Should not reach here.
                // We reach here if we either didn't receive a message after 10 attempts or we received 
                // the TextMessage with no body, instead of throwing MessageFormatRuntimeException.  
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessage);

            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }

    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff(tcfBindings);      
    }

    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff(tcfTCP);
    }
    
    private void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException, InterruptedException {
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
            JMSProducer jmsProducer = jmsContext.createProducer();

            StreamMessage sentMessage = jmsContext.createStreamMessage();
            jmsProducer.send(topic, sentMessage);

            try {
                // Repeat attempts to receive the message.
                Object receivedMessageBody = null;
                for (int i = 0; i<10 && receivedMessageBody == null; i++) {
                  receivedMessageBody = jmsConsumer.receiveBodyNoWait(Object.class);
                  Thread.sleep(100);
                }
                // Should not reach here.
                // We reach here if we either didn't receive a message after 10 attempts or we received 
                // the TextMessage with no body, instead of throwing MessageFormatRuntimeException.  
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessage);
            
            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }

    /*****************************************************************************************************
     * Point to Point tests.                                                                             * 
     *****************************************************************************************************/

    public void testStartJMSContextSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue);

        jmsContextQCFBindings.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
        jmsProducerQCFBindings.send(queue, outbound);

        jmsContextQCFBindings.start();

        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);
        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testStartJMSContextSecOnBinding failed");
        }
    }

    public void testStartJMSContextSecOffTCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue);

        jmsContextQCFTCP.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
        jmsProducerQCFTCP.send(queue, outbound);

        jmsContextQCFTCP.start();

        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFTCP.receive(30000);
        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testStartJMSContextSecOffTCP failed");
        }
    }

    public void testStartJMSContextStartSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            jmsContextQCFBindings.start();
            jmsContextQCFBindings.start();

            jmsContextQCFBindings.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testStartJMSContextStartSecOffBinding failed");
        }
    }

    public void testStartJMSContextStartSecOffTCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            jmsContextQCFTCP.start();
            jmsContextQCFTCP.start();

            jmsContextQCFTCP.close();

        } catch (Exception e) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testStartJMSContextStartSecOffTCP failed");
        }
    }

    public void testStopJMSContextSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            jmsContextQCFBindings.start();

            jmsContextQCFBindings.stop();
            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testStopJMSContextSecOffBinding failed");
        }
    }

    public void testStopJMSContextSecOffTCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.stop();
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testStopJMSContextSecOffTCP failed");
        }
    }

    public void testCommitLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration<?> e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFBindings.commit();
            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration<?> e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);

            jmsContextQCFBindings.commit();

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransaction_B failed");
        }
    }

    public void testCommitLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");
            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration<?> e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration<?> e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
            jmsConsumerQCFTCP.receive(30000);

            jmsContextQCFTCP.commit();

            jmsConsumerQCFTCP.close();
            jmsContextQCFTCP.close();

        } catch (Exception e) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransaction_TCP failed");
        }
    }

    public void testCommitNonLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration<?> e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFBindings.commit();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCommitNonLocalTransaction_B failed");
        }
    }

    public void testCommitNonLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration<?> e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration<?> e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCommitNonLocalTransaction_B failed");
        }
    }

    public void testRollbackLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            jmsContextQCFBindings.commit();

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration<?> e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);

            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration<?> e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFBindings.rollback();

            QueueBrowser qb2 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration<?> e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while ( e2.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e2.nextElement();
                numMsgs2++;
            }

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testRollbackLocalTransaction_B failed");
        }
    }

    public void testRollbackLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);
            jmsContextQCFTCP.commit();

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration<?> e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFTCP.receive(30000);

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration<?> e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFTCP.rollback();

            QueueBrowser qb2 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration<?> e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while ( e2.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e2.nextElement();
                numMsgs2++;
            }

            jmsConsumerQCFTCP.close();
            jmsContextQCFTCP.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testRollbackLocalTransaction_TCP failed");
        }
    }

    public void testRollbackNonLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();

            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.rollback();

            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRollbackNonLocalTransaction_B failed");
        }
    }

    public void testRollbackNonLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            jmsContextQCFTCP.rollback();
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRollbackNonLocalTransaction_TCP failed");
        }
    }

    public void testRecoverNonLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.recover();

            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRecoverNonLocalTransaction_B failed");
        }
    }

    public void testRecoverNonLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

            jmsProducerQCFTCP.send(queue, message);

            jmsContextQCFTCP.recover();

            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRecoverNonLocalTransaction_TCP failed");
        }
    }

    public void testCreateTemporaryQueueSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if ( tempQ == null ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryQueueSecOffBinding failed");
        }

        jmsContextQCFBindings.close();
    }

    public void testCreateTemporaryQueueSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if ( tempQ == null ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryQueueSecOffTCPIP failed");
        }
    }

    public void testTemporaryQueueLifetimeSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

            jmsContextQCFBindings.close();
            jmsContextQCFBindings.createBrowser(tempQ);

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryQueueLifetimeSecOff_B failed");
        }
    }

    public void testTemporaryQueueLifetimeSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            jmsContextQCFTCP.close();
            jmsContextQCFTCP.createBrowser(tempQ);

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryQueueLifetimeSecOff_TCPIP failed");
        }
    }

    public void testgetTemporaryQueueNameSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.getQueueName() == null) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testgetTemporaryQueueNameSecOffBinding failed");
        }
    }

    public void testgetTemporaryQueueNameSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.getQueueName() == null) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testgetTemporaryQueueNameSecOffTCPIP failed");
        }
    }

    public void testToStringTemporaryQueueNameSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.toString() == null ) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testToStringTemporaryQueueNameSecOffBinding failed");
        }
    }

    public void testToStringTemporaryQueueNameSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.toString() == null) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testToStringTemporaryQueueNameSecOffTCPIP failed");
        }
    }

    public void testDeleteTemporaryQueueNameSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFBindings.createBrowser(tempQ);
            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryQueueNameSecOffBinding failed");
        }
    }

    public void testDeleteTemporaryQueueNameSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFTCP.createBrowser(tempQ);
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryQueueNameSecOffTCPIP failed");
        }
    }

    public void testDeleteExceptionTemporaryQueueNameSecOFF_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFBindings.createBrowser(tempQ);
            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryQueueNameSecOFF_B failed");
        }
    }

    public void testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFTCP.createBrowser(tempQ);
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP failed");
        }
    }

    public void testPTPTemporaryQueue_Binding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(tempQ);

        jmsProducerQCFBindings.send(tempQ, "hello world");
        TextMessage recMessage = (TextMessage) jmsConsumerQCFBindings.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("hello world") ) {
            testFailed = true;
        }

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueue_Binding failed");
        }
    }

    public void testPTPTemporaryQueue_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(tempQ);

        jmsProducerQCFTCP.send(tempQ, "hello world");
        TextMessage recMessage = (TextMessage) jmsConsumerQCFTCP.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equalsIgnoreCase("hello world") ) {
            testFailed = true;
        }

        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueue_TCP failed");
        }
    }

    public void testCreateTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if ( tempT == null ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryTopicSecOffBinding failed");
        }

        jmsContextTCFBindings.close();
    }

    public void testCreateTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if ( tempT == null ) {
            testFailed = true;
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryTopicSecOffTCPIP failed");
        }
    }

    public void testTemporaryTopicLifetimeSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            jmsContextTCFBindings.close();
            tempT.delete();

        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicLifetimeSecOff_B failed");
        }
    }

    public void testTemporaryTopicLifetimeSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            jmsContextTCFTCP.close();
            tempT.delete();

        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicLifetimeSecOff_TCPIP failed");
        }
    }

    public void testGetTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if ( (tempT == null) || (tempT.getTopicName() == null) ) {
            testFailed = true;
        }

        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetTemporaryTopicSecOffBinding failed");
        }
    }

    public void testGetTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if ( (tempT == null) || (tempT.getTopicName() == null) ) {
            testFailed = true;
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetTemporaryTopicSecOffTCPIP failed");
        }
    }

    public void testToStringTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if ( (tempT == null) || (tempT.toString() == null) ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testToStringTemporaryTopicSecOffBinding failed");
        }

        jmsContextTCFBindings.close();
    }

    public void testToStringeTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if ((tempT == null) || (tempT.toString() == null) ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testToStringeTemporaryTopicSecOffTCPIP failed");
        }

        jmsContextTCFTCP.close();
    }

    public void testDeleteTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            jmsContextTCFBindings.close();
            tempT.delete();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryTopicSecOffBinding failed");
        }
    }

    public void testDeleteTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            jmsContextTCFTCP.close();
            tempT.delete();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryTopicSecOffTCPIP failed");
        }
    }

    public void testDeleteExceptionTemporaryTopicSecOff_B(
         HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            jmsContextTCFBindings.createConsumer(tempT);
            tempT.delete();
            jmsContextTCFBindings.close();

            testFailed = true;

        } catch ( Exception e ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryTopicSecOff_B failed");
        }
    }

    public void testDeleteExceptionTemporaryTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            jmsContextTCFTCP.createConsumer(tempT);
            tempT.delete();
            jmsContextTCFTCP.close();

            testFailed = true;

        } catch ( Exception e ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryTopicSecOff_TCPIP failed");
        }
    }

    public void testTemporaryTopicPubSubSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
        JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();
        JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(tempT);

        jmsProducerTCFBindings.send(tempT, "hello world");
        TextMessage recMessage = (TextMessage) jmsConsumerTCFBindings.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("hello world") ){
            testFailed = true;
        }

        jmsConsumerTCFBindings.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubSecOff_B failed");
        }
    }

    public void testTemporaryTopicPubSubSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
        JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();
        JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(tempT);

        jmsProducerTCFTCP.send(tempT, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerTCFTCP.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("hello world") ) {
            testFailed = true;
        }

        jmsConsumerTCFTCP.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubSecOff_TCPIP failed");
        }
    }
}
