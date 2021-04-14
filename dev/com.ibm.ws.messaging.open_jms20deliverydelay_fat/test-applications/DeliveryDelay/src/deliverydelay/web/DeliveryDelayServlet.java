/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package deliverydelay.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class DeliveryDelayServlet extends HttpServlet {

    QueueConnectionFactory jmsQCFBindings = null;
    QueueConnectionFactory jmsQCFTCP = null;
    Queue jmsQueue = null;
    Queue jmsQueue1 = null;
    Queue jmsQueue2 = null;

    TopicConnectionFactory jmsTCFBindings = null;
    TopicConnectionFactory jmsTCFTCP = null;
    Topic jmsTopic = null;
    Topic jmsTopic1 = null;
    Topic jmsTopic2 = null;

    public QueueConnectionFactory getQCF(String name) {
        QueueConnectionFactory qcf;
        try {
            qcf = (QueueConnectionFactory) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            qcf = null;
        }
        System.out.println("Queue connection factory '" + name + "' [ " + qcf + " ]");
        return qcf;
    }

    public static TopicConnectionFactory getTCF(String name) {
        TopicConnectionFactory tcf;
        try {
            tcf = (TopicConnectionFactory) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            tcf = null;
        }
        System.out.println("Topic connection factory '" + name + "' [ " + tcf + " ]");
        return tcf;
    }

    public Queue getQueue(String name) {
        Queue queue;
        try {
            queue = (Queue) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            queue = null;
        }
        System.out.println("Queue '" + name + "' [ " + queue + " ]");
        return queue;
    }

    public Topic getTopic(String name) {
        Topic topic;
        try {
            topic = (Topic) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            topic = null;
        }
        System.out.println("Topic '" + name + "' [ " + topic + " ]");
        return topic;
    }

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) 
        throws Exception {
       
        long messagesReceived = 0; 
        try (JMSContext jmsContext = qcf.createContext(JMSContext.SESSION_TRANSACTED)) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(q);
            while ( jmsConsumer.receiveNoWait() != null) {messagesReceived++;}
            jmsContext.commit();
        }
        if (messagesReceived != 0) 
            throw new Exception("Queue:"+q+ "contained "+messagesReceived+" messages");
    }

    public int getMessageCount(QueueBrowser qb) throws JMSException {
        int numMsgs = 0;

        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            e.nextElement();
            numMsgs++;
        }

        return numMsgs;
    }

    private static final long deliveryDelay = 10000;

    private void sendAndCheckDeliveryTime(
        Object producer,
        Destination dest,
        TextMessage send_msg) throws JMSException {

        long beforeSend = System.currentTimeMillis();

        if ( producer  instanceof JMSProducer ) {
            ((JMSProducer) producer).send(dest, send_msg);
        } else if ( producer instanceof MessageProducer ) {
            ((MessageProducer) producer).send(send_msg);
        } else {
            throw new IllegalArgumentException("Unknown producer type [ " + producer + " ] [ " + producer.getClass() + " ]");
        }

        long afterSend = System.currentTimeMillis();

        long sendDuration = afterSend - beforeSend;

        if ( sendDuration >= deliveryDelay ) {
            System.out.println(
                "WARNING : The time taken to send is : " + sendDuration +
                ", which more than delivery delay " + deliveryDelay + "."+
                " Please analyse the send time.");
        }
    }

    //

    @Override
    public void init() throws ServletException {
        super.init();

        jmsQCFBindings = getQCF("java:comp/env/jndi_JMS_BASE_QCF");
        jmsQCFTCP = getQCF("java:comp/env/jndi_JMS_BASE_QCF1");

        jmsQueue = getQueue("java:comp/env/eis/queue1");
        jmsQueue1 = getQueue("java:comp/env/eis/queue2");
        jmsQueue2 = getQueue("java:comp/env/eis/queue2");

        jmsTCFBindings = getTCF("java:comp/env/eis/tcf");
        jmsTCFTCP = getTCF("java:comp/env/eis/tcf1");

        jmsTopic = getTopic("java:comp/env/eis/topic1");
        jmsTopic1 = getTopic("java:comp/env/eis/topic2");
        jmsTopic2 = getTopic("java:comp/env/eis/topic3");

        if ( jmsQCFBindings == null ) {
            throw new ServletException("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF'");
        }
        if ( jmsQCFTCP == null ) {
            throw new ServletException("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1'");
        }

        if ( jmsQueue == null ) {
            throw new ServletException("Null queue 'java:comp/env/eis/queue1'");
        }
        if ( jmsQueue1 == null ) {
            throw new ServletException("Null queue 'java:comp/env/eis/queue2'");
        }
        if ( jmsQueue2 == null ) {
            throw new ServletException("Null queue 'java:comp/env/eis/queue2'");
        }

        if ( jmsTCFBindings == null ) {
            throw new ServletException("Null topic connection factory 'java:comp/env/eis/tcf'");
        }
        if ( jmsTCFTCP == null ) {
            throw new ServletException("Null topic connection factory 'java:comp/env/eis/tcf1'");
        }

        if ( jmsTopic == null ) {
            throw new ServletException("Null topic 'java:comp/env/eis/topic1'");
        }
        if ( jmsTopic1 == null ) {
            throw new ServletException("Null topic 'java:comp/env/eis/topic2'");
        }
        if ( jmsTopic2 == null ) {
            throw new ServletException("Null topic 'java:comp/env/eis/topic3'");
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
        TraceComponent tc = Tr.register(DeliveryDelayServlet.class);

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

    //

    public void testSetDeliveryDelay(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage sendMsg = jmsContext.createTextMessage("testSetDeliveryDelay");
        sendAndCheckDeliveryTime(jmsProducer, jmsQueue, sendMsg);

        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();
        if ( recMsg1 != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelay") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelay failed");
        }
    }

    public void testSetDeliveryDelay_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage sendMsg = jmsContext.createTextMessage("testSetDeliveryDelay_TCP");
        sendAndCheckDeliveryTime(jmsProducer, jmsQueue, sendMsg);

        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();
        if ( recMsg1 != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(30000);
        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelay_TCP") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelay_TCP failed");
        }
    }

    public void testSetDeliveryDelayTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = jmsContext.createTextMessage("testSetDeliveryDelayTopic");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic, sendMsg);

        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();
        if ( recMsg1 != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(30000);
        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopic") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelayTopic failed");
        }
    }

    public void testSetDeliveryDelayTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = jmsContext.createTextMessage("testSetDeliveryDelayTopic_TCP");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic, sendMsg);

        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();
        if ( recMsg1 != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(30000);
        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopic_TCP") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelayTopic_TCP failed");
        }
    }

    public void testSetDeliveryDelayTopicDurSub(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer durJmsConsumer = jmsContext.createDurableConsumer(jmsTopic, "subs");

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = jmsContext.createTextMessage("testSetDeliveryDelayTopicDurSub");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic, sendMsg);

        TextMessage recMsg1 = (TextMessage) durJmsConsumer.receiveNoWait();
        if ( recMsg1 != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) durJmsConsumer.receive(30000);
        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopicDurSub") ) {
            testFailed = true;
        }

        durJmsConsumer.close();

        jmsContext.unsubscribe("subs");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetDeliveryDelayTopicDurSub failed");
        }
    }

    public void testSetDeliveryDelayTopicDurSub_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer durJmsConsumer = jmsContext.createDurableConsumer(jmsTopic, "subs1");

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = jmsContext.createTextMessage("testSetDeliveryDelayTopicDurSub_Tcp");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic, sendMsg);

        TextMessage recMsg1 = (TextMessage) durJmsConsumer.receiveNoWait();
        if ( recMsg1 != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) durJmsConsumer.receive(30000);
        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopicDurSub_Tcp") ) {
            testFailed = true;
        }

        durJmsConsumer.close();

        jmsContext.unsubscribe("subs1");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetDeliveryDelayTopicDurSub_Tcp failed");
        }
    }

    public void testReceiveAfterDelayTopicDurSub(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();
        if ( recMsg1 == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelayTopic failed");
        }
    }

    public void testReceiveAfterDelayTopicDurSub_Tcp(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();
        if ( recMsg1 == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelayTopic_TCP failed");
        }
    }

    public void testDeliveryDelayForDifferentDelays(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        emptyQueue(jmsQCFBindings, queue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        jmsProducer.setDeliveryDelay(5000);
        jmsProducer.send(queue, "QueueBindingsMessage1");

        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(queue, "QueueBindingsMessage2");

        Thread.sleep(8000);

        jmsContext.close();
    }

    public void testDeliveryDelayForDifferentDelays_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        emptyQueue(jmsQCFTCP, queue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        jmsProducer.setDeliveryDelay(5000);
        jmsProducer.send(queue, "QueueTCPMessage1");

        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(queue, "QueueTCPMessage2");

        Thread.sleep(8000);

        jmsContext.close();
    }

    public void testDeliveryDelayForDifferentDelaysTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic");

        JMSProducer jmsProducer = jmsContext.createProducer();

        int delay = 14700;
        jmsProducer.setDeliveryDelay(delay);

        StreamMessage sm = jmsContext.createStreamMessage();
        String msgText = "TopicBindingsMessage1";
        sm.writeString(msgText);
        sm.writeLong( Calendar.getInstance().getTimeInMillis() + delay );
        jmsProducer.send(topic,sm);

        delay = 10100;
        jmsProducer.setDeliveryDelay(delay);

        sm = jmsContext.createStreamMessage();
        msgText = "TopicBindingsMessage2";
        sm.writeString(msgText);
        sm.writeLong( Calendar.getInstance().getTimeInMillis() + delay );
        jmsProducer.send(topic,sm);

        Thread.sleep(20000);

        jmsContext.close();
    }

    public void testDeliveryDelayForDifferentDelaysTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic");

        JMSProducer jmsProducer = jmsContext.createProducer();

        int delay = 15500;
        jmsProducer.setDeliveryDelay(delay);

        StreamMessage sm = jmsContext.createStreamMessage();
        String msgText = "TopicTCPMessage1";
        sm.writeString(msgText);
        sm.writeLong( Calendar.getInstance().getTimeInMillis() + delay );
        jmsProducer.send(topic,sm);

        delay = 11100;
        jmsProducer.setDeliveryDelay(delay);

        sm = jmsContext.createStreamMessage();
        msgText = "TopicTCPMessage2";
        sm.writeString(msgText);
        sm.writeLong( Calendar.getInstance().getTimeInMillis() + delay );
        jmsProducer.send(topic,sm);

        Thread.sleep(20000);

        jmsContext.close();
    }

    public void testDeliveryMultipleMsgs(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String failureReason = null;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsQueue1);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 = jmsContext.createTextMessage("testDeliveryMultipleMsgs1");
        sendAndCheckDeliveryTime(jmsProducer, jmsQueue, sendMsg1);
        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();

        TextMessage sendMsg2 = jmsContext.createTextMessage("testDeliveryMultipleMsgs2");
        sendAndCheckDeliveryTime(jmsProducer, jmsQueue1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) jmsConsumer1.receiveNoWait();

        if ( recMsg1 != null ) {
            failureReason = "Unexpected received message 1 [ " + recMsg1 + " ]";
        } else if ( recMsg2 != null ) {
            failureReason = "Unexpected received message 2 [ " + recMsg2 + " ]";
        }

        recMsg1 = (TextMessage) jmsConsumer.receive(30000);
        recMsg2 = (TextMessage) jmsConsumer1.receive(30000);

        if ( (recMsg1 == null) ||
             (recMsg1.getText() == null) ||
             !recMsg1.getText().equals("testDeliveryMultipleMsgs1") ) {
            failureReason = "Failed to receive message 1 [ " + recMsg1 + " ]";
        } else if ( (recMsg2 == null) ||
                    (recMsg2.getText() == null) ||
                    !recMsg2.getText().equals("testDeliveryMultipleMsgs2") ) {
            failureReason = "Failed to receive message 2 [ " + recMsg2 + " ]";
        }

        jmsConsumer.close();
        jmsConsumer1.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testDeliveryMultipleMsgs failed: " + failureReason);
        }
    }

    public void testDeliveryMultipleMsgs_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        String failureReason = null;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsQueue1);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 = jmsContext.createTextMessage("testDeliveryMultipleMsgs_Tcp1");
        sendAndCheckDeliveryTime(jmsProducer, jmsQueue, sendMsg1);
        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();

        TextMessage sendMsg2 = jmsContext.createTextMessage("testDeliveryMultipleMsgs_Tcp2");
        sendAndCheckDeliveryTime(jmsProducer, jmsQueue1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) jmsConsumer1.receiveNoWait();

        if ( recMsg1 != null ) {
            failureReason = "Unexpected received message 1 [ " + recMsg1 + " ]";
        } else if ( recMsg2 != null ) {
            failureReason = "Unexpected received message 2 [ " + recMsg2 + " ]";
        }

        recMsg1 = (TextMessage) jmsConsumer.receive(30000);
        recMsg2 = (TextMessage) jmsConsumer1.receive(30000);

        if ( (recMsg1 == null) ||
             (recMsg1.getText() == null) ||
             !recMsg1.getText().equals("testDeliveryMultipleMsgs_Tcp1") ) {
            failureReason = "Failed to receive message 1 [ " + recMsg1 + " ]";
        } else if ( (recMsg2 == null) ||
                    (recMsg2.getText() == null) ||
                    !recMsg2.getText().equals("testDeliveryMultipleMsgs_Tcp2") ) {
            failureReason = "Failed to receive message 2 [ " + recMsg2 + " ]";
        }

        jmsConsumer.close();
        jmsConsumer1.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testDeliveryMultipleMsgs_TCP failed: " + failureReason);
        }
    }

    public void testDeliveryMultipleMsgsTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String failureReason = null;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsTopic1);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 = jmsContext.createTextMessage("testDeliveryMultipleMsgsTopic1");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic, sendMsg1);
        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();

        TextMessage sendMsg2 = jmsContext.createTextMessage("testDeliveryMultipleMsgsTopic2");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) jmsConsumer1.receiveNoWait();

        if ( recMsg1 != null ) {
            failureReason = "Unexpected received message 1 [ " + recMsg1 + " ]";
        } else if ( recMsg2 != null ) {
            failureReason = "Unexpected received message 2 [ " + recMsg2 + " ]";
        }

        recMsg1 = (TextMessage) jmsConsumer.receive(30000);
        recMsg2 = (TextMessage) jmsConsumer1.receive(30000);

        if ( (recMsg1 == null) ||
             (recMsg1.getText() == null) ||
             !recMsg1.getText().equals("testDeliveryMultipleMsgsTopic1") ) {
            failureReason = "Failed to receive message 1 [ " + recMsg1 + " ]";
        } else if ( (recMsg2 == null) ||
                    (recMsg2.getText() == null) ||
                    !recMsg2.getText().equals("testDeliveryMultipleMsgsTopic2") ) {
            failureReason = "Failed to receive message 2 [ " + recMsg2 + " ]";
        }

        jmsConsumer.close();
        jmsConsumer1.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testDeliveryMultipleMsgsTopic failed: " + failureReason);
        }
    }

    public void testDeliveryMultipleMsgsTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String failureReason = null;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsTopic1);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 = jmsContext.createTextMessage("testDeliveryMultipleMsgsTopic_Tcp1");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic, sendMsg1);
        TextMessage recMsg1 = (TextMessage) jmsConsumer.receiveNoWait();

        TextMessage sendMsg2 = jmsContext.createTextMessage("testDeliveryMultipleMsgsTopic_Tcp2");
        sendAndCheckDeliveryTime(jmsProducer, jmsTopic1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) jmsConsumer1.receiveNoWait();

        if ( recMsg1 != null ) {
            failureReason = "Unexpected received message 1 [ " + recMsg1 + " ]";
        } else if ( recMsg2 != null ) {
            failureReason = "Unexpected received message 2 [ " + recMsg2 + " ]";
        }

        recMsg1 = (TextMessage) jmsConsumer.receive(30000);
        recMsg2 = (TextMessage) jmsConsumer1.receive(30000);

        if ( (recMsg1 == null) ||
             (recMsg1.getText() == null) ||
             !recMsg1.getText().equals("testDeliveryMultipleMsgsTopic_Tcp1") ) {
            failureReason = "Failed to receive message 1 [ " + recMsg1 + " ]";
        } else if ( (recMsg2 == null) ||
                    (recMsg2.getText() == null) ||
                    !recMsg2.getText().equals("testDeliveryMultipleMsgsTopic_Tcp2") ) {
            failureReason = "Failed to receive message 2 [ " + recMsg2 + " ]";
        }

        jmsConsumer.close();
        jmsConsumer1.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testDeliveryMultipleMsgsTopic_TCP failed: " + failureReason);
        }
    }

    public void testDeliveryDelayZeroAndNegativeValues(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(0);

        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsQueue1);

        jmsProducer.send(jmsQueue1, "Zero Delivery Delay");
        TextMessage msg2 = (TextMessage) jmsConsumer1.receiveNoWait();
        if ( msg2 == null ) {
            testFailed = true;
        }

        try {
            jmsProducer.setDeliveryDelay(-10);
            jmsProducer.send(jmsQueue, "Negative Delivery Delay");
            testFailed = true;
        } catch ( JMSRuntimeException e ) {
            // expected
        }

        jmsConsumer1.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValues failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValues_Tcp(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(0);

        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsQueue1);

        jmsProducer.send(jmsQueue1, "Zero Delivery Delay");
        TextMessage recMsg2 = (TextMessage) jmsConsumer1.receiveNoWait();

        if ( recMsg2 == null ) {
            testFailed = true;
        }

        try {
            jmsProducer.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSRuntimeException e ) {
            // expected
        }

        jmsConsumer1.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValues failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesTopic(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(0);

        jmsProducer.send(jmsTopic, "Zero Delivery Delay");
        TextMessage recMsg2 = (TextMessage) jmsConsumer1.receiveNoWait();

        if ( recMsg2 == null ) {
            testFailed = true;
        }

        try {
            jmsProducer.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSRuntimeException e ) {
            // expected
        }

        jmsConsumer1.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesTopic failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext(JMSContext.SESSION_TRANSACTED);

        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(0);

        jmsProducer.send(jmsTopic, "Zero Delivery Delay - testDeliveryDelayZeroAndNegativeValuesTopic_Tcp");

        jmsContext.commit();

        TextMessage recMsg2 = (TextMessage) jmsConsumer1.receiveNoWait();

        if ( recMsg2 == null ) {
            testFailed = true;
        }
        jmsContext.commit();

        try {
            jmsProducer.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSRuntimeException e ) {
            // expected
        }

        jmsContext.commit();
        jmsConsumer1.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesTopic_Tcp failed");
        }
    }

    public void testSettingMultipleProperties(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).setDisableMessageID(true);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        jmsProducer.send(jmsQueue, "testSettingMultipleProperties");
        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMsg == null) ||
             (recMsg.getText() == null) ||
             !recMsg.getText().equals("testSettingMultipleProperties") ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        } else {
            recMsg.getText();
            recMsg.getJMSMessageID();
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultipleProperties failed");
        }
    }

    public void testSettingMultipleProperties_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).setDisableMessageID(true);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        jmsProducer.send(jmsQueue, "testSettingMultipleProperties_Tcp");

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMsg == null) ||
             (recMsg.getText() == null) ||
             !recMsg.getText().equals("testSettingMultipleProperties_Tcp") ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultipleProperties failed");
        }
    }

    public void testSettingMultiplePropertiesTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).setDisableMessageID(true);

        jmsProducer.send(jmsTopic, "testSettingMultiplePropertiesTopic");

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        if ( (recMsg == null) ||
             (recMsg.getText() == null) ||
             !recMsg.getText().equals("testSettingMultiplePropertiesTopic") ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesTopic failed");
        }
    }

    public void testSettingMultiplePropertiesTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).setDisableMessageID(true);

        jmsProducer.send(jmsTopic, "testSettingMultiplePropertiesTopic_Tcp");
        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMsg == null) ||
             (recMsg.getText() == null) ||
             !recMsg.getText().equals("testSettingMultiplePropertiesTopic_Tcp") ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesTopic_Tcp failed");
        }
    }

    private static final int DELIVERY_DELAY = 2000;

    // testTransactedSend_B

    public void testTransactedSend_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext(Session.SESSION_TRANSACTED);
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(DELIVERY_DELAY);

        Message message = jmsContext.createTextMessage("testTransactedSend_B");
        jmsProducer.send(jmsQueue, message);
        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;

        Thread.sleep(1000);

        jmsContext.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(40000);

        jmsContext.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
             (recMsg.getText() != null) &&
             recMsg.getText().equals("testTransactedSend_B")) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                 (Math.abs(rec_time - time_after_commit) < 1000)) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSend_B failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    private String describeTimes(long afterSend, long afterCommit, long received) {
        return
            "After send [ " + afterSend + " ]" +
            "; after commit [ " + afterCommit + " ]" +
            "; received [ " + received + " ]";
    }

    public void testTransactedSend_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext(Session.SESSION_TRANSACTED);
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(DELIVERY_DELAY);

        Message message = jmsContext.createTextMessage("testTransactedSend_Tcp");
        jmsProducer.send(jmsQueue, message);
        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;

        Thread.sleep(1000);
        jmsContext.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(40000);
        jmsContext.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
             (recMsg.getText() != null) &&
             recMsg.getText().equals("testTransactedSend_Tcp") ) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                 (Math.abs(rec_time - time_after_commit) < 1000) ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSend_Tcp failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    public void testTransactedSendTopic_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext(Session.SESSION_TRANSACTED);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();

        Message message = jmsContext.createTextMessage("testTransactedSendTopic_B");

        jmsProducer.setDeliveryDelay(DELIVERY_DELAY);
        jmsProducer.send(jmsTopic, message);
        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;
        Thread.sleep(1000);
        jmsContext.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(40000);
        jmsContext.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
             (recMsg.getText() != null) &&
             recMsg.getText().equals("testTransactedSendTopic_B") ) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                 (Math.abs(rec_time - time_after_commit) < 1000) ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSendTopic_B failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    public void testTransactedSendTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext(Session.SESSION_TRANSACTED);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();

        Message message = jmsContext.createTextMessage("testTransactedSendTopic_Tcp");
        jmsProducer.setDeliveryDelay(DELIVERY_DELAY);
        jmsProducer.send(jmsTopic, message);
        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;
        Thread.sleep(1000);
        jmsContext.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(40000);
        jmsContext.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
             (recMsg.getText() != null) &&
             recMsg.getText().equals("testTransactedSendTopic_Tcp") ) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                 (Math.abs(rec_time - time_after_commit) < 1000) ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSendTopic_Tcp failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    public void testTiming_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTiming_B");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsQueue, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTiming_B")) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTiming_B failed");
        }
    }

    public void testTiming_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTiming_Tcp");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsQueue, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTiming_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTiming_Tcp failed");
        }
    }

    public void testTimingTopic_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTimingTopic_B");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsTopic, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopic_B") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopic_B failed");
        }
    }

    public void testTimingTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTimingTopic_Tcp");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsTopic, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopic_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopic_Tcp failed");
        }
    }

    public void testGetDeliveryDelay(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        jmsProducer.setDeliveryDelay(1000);

        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelay failed");
        }
    }

    public void testGetDeliveryDelay_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }
        jmsProducer.setDeliveryDelay(1000);

        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelay_TCP failed");
        }
    }

    public void testGetDeliveryDelayTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        jmsProducer.setDeliveryDelay(1000);
        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayTopic failed");
        }
    }

    public void testGetDeliveryDelayTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        jmsProducer.setDeliveryDelay(1000);

        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayTopic failed");
        }
    }

    // new tests for simplified API

    public void testPersistentMessage(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();

        emptyQueue(jmsQCFBindings, jmsQueue);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        jmsProducer.setDeliveryMode(DeliveryMode.PERSISTENT).setDeliveryDelay(1000);

        jmsProducer.send(jmsQueue, "testPersistentMessage_PersistentMsg");

        jmsProducer.setDeliveryDelay(1000).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        jmsProducer.send(jmsQueue1, "testPersistentMessage_NonPersistentMsg");

        jmsConsumer.close();
        jmsContext.close();
    }

    public void testPersistentMessageReceive(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsQueue);
        JMSConsumer jmsConsumer2 = jmsContext.createConsumer(jmsQueue1);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage recMsg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage recMsg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testPersistentMessage_PersistentMsg")) ||
             (recMsg2 != null) ) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageReceive failed");
        }
    }

    public void testPersistentMessage_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        emptyQueue(jmsQCFTCP, jmsQueue);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        jmsProducer.setDeliveryMode(DeliveryMode.PERSISTENT).setDeliveryDelay(1000);
        jmsProducer.send(jmsQueue, "testPersistentMessage_PersistentMsgTcp");

        jmsProducer.setDeliveryDelay(1000).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        jmsProducer.send(jmsQueue1, "testPersistentMessage_NonPersistentMsgTcp");

        jmsConsumer.close();
        jmsContext.close();
    }

    public void testPersistentMessageReceive_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(jmsQueue);
        JMSConsumer jmsConsumer2 = jmsContext.createConsumer(jmsQueue1);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage recMsg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage recMsg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) || 
              !recMsg1.getText().equals("testPersistentMessage_PersistentMsgTcp")) ||
             (recMsg2 != null) ) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageReceive_Tcp failed");
        }
    }

    public void testPersistentMessageTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(jmsTopic, "durPersMsg1");
        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "durPersMsg2");
        JMSProducer jmsProducer = jmsContext.createProducer();

        jmsProducer.setDeliveryMode(DeliveryMode.PERSISTENT) .setDeliveryDelay(1000);
        jmsProducer.send(jmsTopic, "testPersistentMessage_PersistentMsgTopic");

        jmsProducer.setDeliveryDelay(1000).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        jmsProducer.send(jmsTopic1, "testPersistentMessage_NonPersistentMsgTopic");

        // First half of test .. verification in the second half.
    }

    public void testPersistentMessageReceiveTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(jmsTopic, "durPersMsg1");
        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "durPersMsg2");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage recMsg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage recMsg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testPersistentMessage_PersistentMsgTopic")) ||
             (recMsg2 != null)) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContext.unsubscribe("durPersMsg1");
        jmsContext.unsubscribe("durPersMsg2");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageReceiveTopic failed");
        }
    }

    public void testPersistentMessageTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(jmsTopic, "durPersMsgTcp1");
        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "durPersMsgTcp2");
        JMSProducer jmsProducer = jmsContext.createProducer();

        jmsProducer.setDeliveryMode(DeliveryMode.PERSISTENT).setDeliveryDelay(1000);
        jmsProducer.send(jmsTopic, "testPersistentMessage_PersistentMsgTopicTcp");

        jmsProducer.setDeliveryDelay(1000).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        jmsProducer.send(jmsTopic1, "testPersistentMessage_NonPersistentMsgTopicTcp");

        // First half of test .. verification in the second half.
    }

    public void testPersistentMessageReceiveTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(jmsTopic, "durPersMsgTcp1");
        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "durPersMsgTcp2");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage recMsg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage recMsg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testPersistentMessage_PersistentMsgTopicTcp")) ||
             (recMsg2 != null) ) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContext.unsubscribe("durPersMsgTcp1");
        jmsContext.unsubscribe("durPersMsgTcp2");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageStoreReceiveTopic_Tcp failed");
        }
    }

    public void testTimeToLiveWithDeliveryDelay(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setTimeToLive(1000);
        jmsProducer.setDeliveryDelay(DELIVERY_DELAY);
        jmsProducer.send(jmsQueue, "testTimeToLiveWithDeliveryDelay");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        // TODO: Why is this test verification disabled?

        // if ( (recMsg == null) ||
        //      (recMsg.getText() == null) ||
        //      !recMsg.getText().equals("testTimeToLiveWithDeliveryDelay") ) {
        //     testFailed = true;
        // }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTimeToLiveWithDeliveryDelay failed");
        }
    }

    public void testReceiveBodyObjectMsgWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String abc = new String("testReceiveBodyObjectMsg_B_SecOff");
        ObjectMessage message = jmsContext.createObjectMessage();
        message.setObject(abc);
        jmsContext.createProducer().setDeliveryDelay(1000).send(jmsQueue, message);

        Object body = jmsConsumer.receiveBody(Serializable.class, 30000);

        if ( (body == null) || !body.equals(abc) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyObjectMsg failed");
        }
    }

    public void testReceiveBodyObjectMsgWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String abc = new String("testReceiveBodyObjectMsg_TcpIp_SecOff");
        ObjectMessage message = jmsContext.createObjectMessage();
        message.setObject(abc);
        jmsContext.createProducer().setDeliveryDelay(1000).send(jmsQueue, message);

        Object body = jmsConsumer.receiveBody(Serializable.class, 30000);

        if ( (body == null) || !body.equals(abc) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyObjectMsg_Tcp failed");
        }
    }

    public void testCloseConsumer(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();

            jmsConsumer.receive();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCloseConsumer failed");
        }
    }

    public void testCloseConsumer_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();

            jmsConsumer.receive();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCloseConsumer_Tcp failed");
        }
    }

    public void testQueueNameNullWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            Queue queue = jmsContext.createQueue(null);
            emptyQueue(jmsQCFBindings, queue);

            jmsProducer.setDeliveryDelay(1000).send(queue,"testQueueNameNull_B");

            JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

            try {
                TextMessage m = (TextMessage) jmsConsumer.receive(30000);
                m.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException e ) {
            // expected
        }

        if ( testFailed ) {
            throw new Exception("testQueueNameNull_B failed");
        }
    }

    public void testQueueNameNullWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            Queue queue = jmsContext.createQueue(null);
            emptyQueue(jmsQCFTCP, queue);

            jmsProducer.setDeliveryDelay(1000).send(queue, "testQueueNameNull_TCP");

            JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

            try {
                TextMessage m = (TextMessage) jmsConsumer.receive(30000);
                m.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException e ) {
            // expected
        }

        if ( testFailed ) {
            throw new Exception("testQueueNameNull_TcpIp failed");
        }
    }

    public void testTopicNameNullWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            Topic topic = jmsContext.createTopic(null);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

            try {
                jmsContext.createProducer().setDeliveryDelay(1000).send(topic, "testTopicNameNull_B");

                TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
                recMsg.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception( "testTopicNameNull_B failed");
        }
    }

    public void testTopicNameNullWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            Topic topic = jmsContext.createTopic(null);

            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

            try {
                jmsContext.createProducer()
                    .setDeliveryDelay(1000)
                    .send(topic, "testTopicNameNull_TcpIP");

                TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
                recMsg.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameNull_TcpIp failed");
        }
    }

    public void testAckOnClosedContextWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.
            setDeliveryDelay(1000).
            send(jmsQueue, "testAckOnClosedContext_B_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        jmsContext.close();

        try {
            jmsContext.acknowledge();
            testFailed = true;

        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testAckOnClosedContextWithDD failed");
        }
    }

    public void testAckOnClosedContextWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, "testAckOnClosedContext_B_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        jmsContext.close();

        try {
            jmsContext.acknowledge();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            // expected
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testAckOnClosedContextWithDD_Tcp failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");

        TextMessage message = jmsContext .createTextMessage("testCreateConsumerWithMsgSelector_B_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelector_B_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDD failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage message = jmsContext.createTextMessage("testCreateConsumerWithMsgSelector_TcpIp_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelector_TcpIp_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDD_Tcp failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDDTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage message = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_B_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelectorTopic_B_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDDTopic failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDDTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage message = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDDTopic_Tcp failed");
        }
    }

    public void testJMSPriorityWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        Message message = jmsContext.createMessage();
        message.setJMSPriority(9);
        jmsContext.createProducer()
            .setPriority(1)
            .setDeliveryDelay(1000)
            .send(jmsQueue, message);

        int pri = jmsConsumer.receive(30000).getJMSPriority();
        if ( pri != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSPriorityWithDD failed");
        }
    }

    public void testJMSPriorityWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        Message message = jmsContext.createMessage();
        message.setJMSPriority(9);

        jmsContext.createProducer()
            .setPriority(1)
            .setDeliveryDelay(1000)
            .send(jmsQueue, message);

        int pri = jmsConsumer.receive(30000).getJMSPriority();
        if ( pri != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSPriorityWithDD_Tcp failed");
        }
    }

    public void testConnStartAuto_createContextUserSessionModeWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsTCFBindings.createContext(userName, password, smode);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        TextMessage recMsg = jmsContext.createTextMessage("Hello");
        jmsContext.createProducer().setDeliveryDelay(1000).send(jmsTopic, recMsg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if ( (message == null) ||
             (message.getText() == null) ||
             !message.getText().equals("Hello") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testConnStartAuto_createContextUserSessionModeWithDD failed");
        }
    }

    public void testConnStartAuto_createContextUserSessionModeWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsTCFTCP.createContext(userName, password, smode);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        TextMessage recMsg = jmsContext.createTextMessage("Hello");
        jmsContext.createProducer()
            .setDeliveryDelay(1000)
            .send(jmsTopic, recMsg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if ( (message == null) ||
             (message.getText() == null) ||
             !message.getText().equals("Hello") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testConnStartAuto_createContextUserSessionModeWithDD_Tcp failed");
        }
    }

    public void testcreateBrowserWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue2);

        jmsContext.createProducer()
            .setDeliveryDelay(1000)
            .send(jmsQueue2, "Tester");

        Thread.sleep(2000);

        QueueBrowser queueBrowser = jmsContext.createBrowser(jmsQueue2);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue2);
        jmsConsumer.receive(30000);

        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testcreateBrowserWithDD failed");
        }
    }

    public void testcreateBrowserWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue2);

        jmsContext.createProducer()
            .setDeliveryDelay(1000)
            .send(jmsQueue2, "Tester");

        Thread.sleep(2000);

        QueueBrowser queueBrowser = jmsContext.createBrowser(jmsQueue2);

        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue2);
        jmsConsumer.receive(30000);

        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testcreateBrowserWithDD_Tcp failed");
        }
    }

    public void testInitialJMSXDeliveryCountWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, "testInitialJMSXDeliveryCount_B_SecOff");

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();

        if ( recMsg.getIntProperty("JMSXDeliveryCount") != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testInitialJMSXDeliveryCountWithDD failed");
        }
    }

    public void testInitialJMSXDeliveryCountWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        jmsProducer
            .setDeliveryDelay(1000)
            .send(jmsQueue, "testInitialJMSXDeliveryCount_TcpIp_SecOff");

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        if ( recMsg.getIntProperty("JMSXDeliveryCount") != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testInitialJMSXDeliveryCountWithDD_Tcp failed");
        }
    }

    public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, "");

        String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

        if ( !recvdMessage.equals("") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic failed");
        }
    }

    public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, "");

        String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

        if ( !recvdMessage.equals("") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_EmptyMessage_Topic_Tcp failed");
        }
    }

    public void testClearProperties_NotsetWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsProducer.setProperty("Name", "Tester").setDeliveryDelay(1000);
        jmsProducer.setProperty("ObjectType", new Integer(1414));

        jmsProducer.clearProperties();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testClearProperties_NotsetWithDD failed");
        }
    }

    public void testClearProperties_NotsetWithDD_Tcp(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsProducer.setProperty("Name", "Tester").setDeliveryDelay(1000);
        jmsProducer.setProperty("ObjectType", new Integer(1414));

        jmsProducer.clearProperties();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testClearProperties_NotsetWithDD_Tcp failed");
        }
    }

    public void testStartJMSContextWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsContext.setAutoStart(false);

        String outbound = "Hello World";

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, outbound);

        jmsContext.start();

        TextMessage receiveMsg = (TextMessage) jmsConsumer.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed == true ) {
            throw new Exception("testStartJMSContextWithDD failed");
        }
    }

    public void testStartJMSContextWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        jmsContext.setAutoStart(false);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String outbound = "Hello World";
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, outbound);

        jmsContext.start();

        TextMessage receiveMsg = (TextMessage) jmsConsumer.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testStartJMSContextWithDD_Tcp failed");
        }
    }

    public void testPTPTemporaryQueueWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempQ);

        jmsProducer.setDeliveryDelay(1000).send(tempQ, "testPTPTemporaryQueueWithDD");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equals("testPTPTemporaryQueueWithDD") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueueWithDD failed");
        }
    }

    public void testPTPTemporaryQueueWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempQ);

        jmsProducer.setDeliveryDelay(1000).send(tempQ, "testPTPTemporaryQueueWithDD_Tcp");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equalsIgnoreCase("testPTPTemporaryQueueWithDD_Tcp") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueueWithDD_Tcp failed");
        }
    }

    public void testTemporaryTopicPubSubWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        TemporaryTopic tempT = jmsContext.createTemporaryTopic();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempT);

        jmsProducer.setDeliveryDelay(1000).send(tempT, "testTemporaryTopicPubSubWithDD");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equals("testTemporaryTopicPubSubWithDD") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubWithDD failed");
        }
    }

    public void testTemporaryTopicPubSubWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        TemporaryTopic tempT = jmsContext.createTemporaryTopic();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempT);

        jmsProducer.setDeliveryDelay(1000).send(tempT, "testTemporaryTopicPubSubWithDD_Tcp");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equals("testTemporaryTopicPubSubWithDD_Tcp") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubWithDD_Tcp failed");
        }
    }

    public void testCommitLocalTransactionWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = null;
        JMSConsumer jmsConsumer = null;

        try {
            jmsContext = jmsQCFBindings.createContext(Session.SESSION_TRANSACTED);

            Message message = jmsContext.createTextMessage("testCommitLocalTransactionWithDD");
            emptyQueue(jmsQCFBindings, jmsQueue);

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(1000).send(jmsQueue,message);

            QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContext.commit();

            QueueBrowser qb1 = jmsContext.createBrowser(jmsQueue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsConsumer = jmsContext.createConsumer(jmsQueue);
            TextMessage rmsg = (TextMessage) jmsConsumer.receive(30000);

            jmsContext.commit();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        if ( jmsContext != null ) {
            jmsContext.close();
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransactionWithDD failed");
        }
    }

    public void testCommitLocalTransactionWithDD_Tcp(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = null;
        JMSConsumer jmsConsumer = null;

        try {
            jmsContext = jmsQCFTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(jmsQCFTCP, jmsQueue);

            Message message = jmsContext.createTextMessage("testCommitLocalTransactionWithDD_Tcp");
            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

            QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContext.commit();

            QueueBrowser qb1 = jmsContext.createBrowser(jmsQueue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.receive(30000);

            jmsContext.commit();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        if ( jmsContext != null ) {
            jmsContext.close();
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransactionWithDD_Tcp failed");
        }

    }

    public void testCreateSharedDurableConsumer_create_B_SecOff(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumer_consume_B_SecOff(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

        if ( tmsg == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableExpiry_B_SecOff failed");
        }
    }

    public void testCreateSharedNonDurableConsumer_create_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

        jmsContextSender.close();
    }

    public void testCreateSharedNonDurableConsumer_consume_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();
        if ( tmsg != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        // jmsContextReceiver.unsubscribe("SUBID");
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_create(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(jmsTopic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateUnSharedDurableConsumer_consume(
        HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(jmsTopic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

        if ( tmsg == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_consume failed");
        }
    }

    // CLASSIC

    public void testSetDeliveryDelayClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = sessionSender.createTextMessage("testSetDeliveryDelayClassicApi");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg);

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(30000);

        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayClassicApi") ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testSetDeliveryDelayClassicApi failed");
        }
    }

    public void testSetDeliveryDelayClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = sessionSender.createTextMessage("testSetDeliveryDelayClassicApi_Tcp");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg);
        TextMessage recMsg = (TextMessage) rec.receiveNoWait();

        if ( recMsg != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(30000);

        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayClassicApi_Tcp") ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testSetDeliveryDelayClassicApi_Tcp failed");
        }
    }

    public void testSetDeliveryDelayTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession session = con.createTopicSession(false,Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = session.createSubscriber(jmsTopic);

        TopicPublisher publisher = session.createPublisher(jmsTopic);
        publisher.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = session.createTextMessage("testSetDeliveryDelayTopicClassicApi");
        sendAndCheckDeliveryTime(publisher, jmsTopic, sendMsg);
        TextMessage recMsg = (TextMessage) sub.receiveNoWait();

        if ( recMsg != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) sub.receive(30000);

        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopicClassicApi") ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelayTopicClassicApi failed");
        }
    }

    public void testSetDeliveryDelayTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = session.createSubscriber(jmsTopic);

        TopicPublisher publisher = session.createPublisher(jmsTopic);
        publisher.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = session.createTextMessage("testSetDeliveryDelayTopicClassicApi_Tcp");
        sendAndCheckDeliveryTime(publisher, jmsTopic, sendMsg);
        TextMessage recMsg = (TextMessage) sub.receiveNoWait();

        if ( recMsg != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) sub.receive(30000);

        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopicClassicApi_Tcp") ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testReceiveAfterDelayTopicClassicApi failed");
        }
    }

    public void testSetDeliveryDelayTopicDurSubClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = session.createDurableSubscriber(jmsTopic, "dursub");

        TopicPublisher publisher = session.createPublisher(jmsTopic);
        publisher.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = session.createTextMessage("testSetDeliveryDelayTopicDurSubClassicApi");
        sendAndCheckDeliveryTime(publisher, jmsTopic, sendMsg);
        TextMessage recMsg = (TextMessage) sub.receiveNoWait();

        if ( recMsg != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) sub.receive(30000);

        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopicDurSubClassicApi") ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        session.unsubscribe("dursub");
        con.close();

        if ( testFailed ) {
            throw new Exception("testSetDeliveryDelayTopicDurSubClassicApi failed");
        }
    }

    public void testSetDeliveryDelayTopicDurSubClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = session.createDurableSubscriber(jmsTopic, "dursubtcp");

        TopicPublisher publisher = session.createPublisher(jmsTopic);
        publisher.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg = session.createTextMessage("testSetDeliveryDelayTopicDurSubClassicApi_Tcp");
        sendAndCheckDeliveryTime(publisher, jmsTopic, sendMsg);
        TextMessage recMsg = (TextMessage) sub.receiveNoWait();

        if ( recMsg != null ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) sub.receive(30000);

        if ( (recMsg2 == null) ||
             (recMsg2.getText() == null) ||
             !recMsg2.getText().equals("testSetDeliveryDelayTopicDurSubClassicApi_Tcp") ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        session.unsubscribe("dursubtcp");
        con.close();

        if ( testFailed ) {
            throw new Exception("testSetDeliveryDelayTopicDurSubClassicApi_Tcp failed");
        }
    }

    public void testDeliveryDelayForDifferentDelaysClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        emptyQueue(jmsQCFBindings, queue);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender send = sessionSender.createSender(queue);
        send.setDeliveryDelay(5000);
        send.send( sessionSender.createTextMessage("QueueBindingsMessage1-ClassicApi") );
        send.setDeliveryDelay(1000);
        send.send( sessionSender.createTextMessage("QueueBindingsMessage2-ClassicApi") );

        Thread.sleep(8000);

        send.close();
        con.close();
    }

    public void testDeliveryDelayForDifferentDelaysClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, queue);

        QueueSender send = sessionSender.createSender(queue);
        send.setDeliveryDelay(5000);
        send.send( sessionSender.createTextMessage("QueueTCPMessage1-ClassicApi") );
        send.setDeliveryDelay(1000);
        send.send( sessionSender.createTextMessage("QueueTCPMessage2-ClassicApi") );

        Thread.sleep(8000);

        send.close();
        con.close();
    }

    public void testDeliveryDelayForDifferentDelaysTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        Topic topic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic");

        TopicSession session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicPublisher publisher = session.createPublisher(topic);

        int delay = 15100;
        publisher.setDeliveryDelay(delay);

        StreamMessage sm = session.createStreamMessage();
        String msgText = "TopicBindingsMessage1-ClassicApi";
        sm.writeString(msgText);
        sm.writeLong( Calendar.getInstance().getTimeInMillis() + delay );
        publisher.publish(sm);

        delay = 11200;
        publisher.setDeliveryDelay(delay);

        sm = session.createStreamMessage();
        msgText = "TopicBindingsMessage2-ClassicApi";
        sm.writeString(msgText);
        sm.writeLong(Calendar.getInstance().getTimeInMillis()+delay);
        publisher.publish(sm);

        Thread.sleep(20000);

        con.close();
    }

    public void testDeliveryDelayForDifferentDelaysTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic");

        TopicSession session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicPublisher publisher = session.createPublisher(topic);

        int delay = 15900;
        publisher.setDeliveryDelay(delay);

        StreamMessage sm = session.createStreamMessage();
        String msgText = "TopicTCPMessage1-ClassicApi";
        sm.writeString(msgText);
        sm.writeLong(Calendar.getInstance().getTimeInMillis() + delay);
        publisher.publish(sm);

        delay = 10400;
        publisher.setDeliveryDelay(delay);

        sm = session.createStreamMessage();
        msgText = "TopicTCPMessage2-ClassicApi";
        sm.writeString(msgText);
        sm.writeLong(Calendar.getInstance().getTimeInMillis() + delay);
        publisher.publish(sm);

        Thread.sleep(20000);

        con.close();
    }

    public void testDeliveryMultipleMsgsClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        // QueueReceiver rec1 = sessionSender.createReceiver(queue1);
        //
        // This sets the delivery delay of all messages sent using that jmsProducer.
        // In classic API we can create sender for a single queue.

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi1");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg1);
        TextMessage recMsg1 = (TextMessage) rec.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi2");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) rec.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) rec.receive(30000);
        recMsg2 = (TextMessage) rec.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsClassicApi1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsClassicApi2")) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsClassicApi failed");
        }
    }

    public void testDeliveryMultipleMsgsClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        // QueueReceiver rec1 = sessionSender.createReceiver(queue1);
        //
        // This sets the delivery delay of all messages sent using that jmsProducer.
        // In classic API we can create sender for a single queue.

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi_Tcp1");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg1);
        TextMessage recMsg1 = (TextMessage) rec.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi_Tcp2");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) rec.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) rec.receive(30000);
        recMsg2 = (TextMessage) rec.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsClassicApi_Tcp1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsClassicApi_Tcp2")) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsClassicApi_Tcp failed");
        }
    }

    public void testDeliveryMultipleMsgsTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi1");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg1);
        TextMessage recMsg1 = (TextMessage) sub.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi2");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg2);
        TextMessage recMsg2 = (TextMessage) sub.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) sub.receive(30000);
        recMsg2 = (TextMessage) sub.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsTopicClassicApi1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsTopicClassicApi2")) ) {
            testFailed = true;
        }

        sub.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsTopicClassicApi failed");
        }
    }

    public void testDeliveryMultipleMsgsTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender =
            con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(deliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi_Tcp1");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg1);
        TextMessage recMsg1 = (TextMessage) sub.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi_Tcp2");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg2);
        TextMessage recMsg2 = (TextMessage) sub.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) sub.receive(30000);
        recMsg2 = (TextMessage) sub.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsTopicClassicApi_Tcp1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsTopicClassicApi_Tcp2")) ) {
            testFailed = true;
        }

        sub.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsTopicClassicApi_Tcp failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
        QueueSender send = sessionSender.createSender(jmsQueue1);

        send.send(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.send(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.publish(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.publish(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);
        send.send( sessionSender.createTextMessage("testSettingMultiplePropertiesClassicApi") );

        TextMessage recMsg = (TextMessage) rec.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesClassicApi")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);
        send.send( sessionSender.createTextMessage("testSettingMultiplePropertiesClassicApi_Tcp") );

        TextMessage recMsg = (TextMessage) rec.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesClassicApi_Tcp")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);

        send.publish( sessionSender.createTextMessage("testSettingMultiplePropertiesTopicClassicApi") );

        TextMessage recMsg = (TextMessage) sub.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesTopicClassicApi")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesTopicClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);

        send.publish( sessionSender.createTextMessage("testSettingMultiplePropertiesTopicClassicApi_Tcp") );

        TextMessage recMsg = (TextMessage) sub.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesTopicClassicApi_Tcp")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesTopicClassicApi failed");
        }
    }

    // testTransactedSend_B

    public void testTransactedSendClassicApi_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(DELIVERY_DELAY);
        send.send( sessionSender.createTextMessage("testTransactedSendClassicApi_B") );

        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;
        Thread.sleep(1000);
        sessionSender.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) rec.receive(40000);
        sessionSender.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
             (recMsg.getText() != null) &&
             recMsg.getText().equals("testTransactedSendClassicApi_B") ) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                  (Math.abs(rec_time - time_after_commit) < 1000)) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSendClassicApi_B failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    public void testTransactedSendClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(DELIVERY_DELAY);
        send.send( sessionSender.createTextMessage("testTransactedSendClassicApi_Tcp") );
        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;

        Thread.sleep(1000);
        sessionSender.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) rec.receive(40000);
        sessionSender.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
             (recMsg.getText() != null) &&
             recMsg.getText().equals("testTransactedSendClassicApi_Tcp") ) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                 (Math.abs(rec_time - time_after_commit) < 1000) ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSendClassicApi_Tcp failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    public void testTransactedSendTopicClassicApi_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(DELIVERY_DELAY);
        send.publish( sessionSender.createTextMessage("testTransactedSendTopicClassicApi_B") );
        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;

        Thread.sleep(1000);
        sessionSender.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) rec.receive(40000);
        sessionSender.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
             (recMsg.getText() != null) &&
             recMsg.getText().equals("testTransactedSendTopicClassicApi_B") ) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                 (Math.abs(rec_time - time_after_commit) < 1000) ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSendTopicClassicApi_B failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    public void testTransactedSendTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(DELIVERY_DELAY);
        send.publish( sessionSender.createTextMessage("testTransactedSendTopicClassicApi_Tcp") );
        long time_after_send = System.currentTimeMillis() + DELIVERY_DELAY;

        Thread.sleep(1000);
        sessionSender.commit();
        long time_after_commit = System.currentTimeMillis() + DELIVERY_DELAY;

        TextMessage recMsg = (TextMessage) rec.receive(40000);
        sessionSender.commit();

        long rec_time = 0L;
        if ( (recMsg != null) &&
              (recMsg.getText() != null) &&
              recMsg.getText().equals("testTransactedSendTopicClassicApi_Tcp") ) {
            rec_time = recMsg.getLongProperty("JMSDeliveryTime");
            if ( (Math.abs(rec_time - time_after_send) > 100) ||
                 (Math.abs(rec_time - time_after_commit) < 1000) ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testTransactedSendTopicClassicApi_Tcp failed: " +
                                describeTimes(time_after_send, time_after_commit, rec_time) );
        }
    }

    public void testTimingClassicApi_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);

        TextMessage sendMsg = sessionSender.createTextMessage("testTimingClassicApi_B");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingClassicApi_B") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingClassicApi_B failed");
        }
    }

    public void testTimingClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);
        TextMessage sendMsg = sessionSender.createTextMessage("testTimingClassicApi_Tcp");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingClassicApi_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingClassicApi_Tcp failed");
        }
    }

    public void testTimingTopicClassicApi_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);
        TextMessage sendMsg = sessionSender.createTextMessage("testTimingTopicClassicApi_B");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopicClassicApi_B") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopicClassicApi_B failed");
        }
    }

    public void testTimingTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);

        TextMessage sendMsg = sessionSender.createTextMessage("testTimingTopicClassicApi_Tcp");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopicClassicApi_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopicClassicApi_Tcp failed");
        }
    }

    public void testGetDeliveryDelayClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String failureReason = null;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            failureReason = "Incorrect default delivery dalay [ " + val + " ] expecting [ 0 ]";
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            failureReason = "Incorrect delivery dalay [ " + val + " ] expecting [ 1000 ]";
        }

        sessionSender.close();
        con.close();

        if ( failureReason != null ) {
            throw new Exception("testGetDeliveryDelayClassicApi failed: " + failureReason);
        }
    }

    public void testGetDeliveryDelayClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayClassicApi_Tcp failed");
        }
    }

    public void testGetDeliveryDelayClassicApiTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
        TopicPublisher send = sessionSender.createPublisher(jmsTopic);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayClassicApiTopic failed");
        }
    }

    public void testGetDeliveryDelayClassicApiTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayClassicApiTopic_Tcp failed");
        }
    }

    public void testPersistentMessageClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender producer1 = sessionSender.createSender(jmsQueue);
        emptyQueue(jmsQCFBindings, jmsQueue);

        QueueSender producer2 = sessionSender.createSender(jmsQueue1);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer1.setDeliveryDelay(1000);
        TextMessage msg1 = sessionSender.createTextMessage("testPersistentMessage_PersistentMsgClassicApi");
        producer1.send(msg1);

        producer2.setDeliveryDelay(1000);
        producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        TextMessage msg2 = sessionSender.createTextMessage("testPersistentMessage_NonPersistentMsgClassicApi");
        producer2.send(msg2);

        sessionSender.close();
        con.close();
    }

    public void testPersistentMessageReceiveClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver jmsConsumer1 = sessionSender.createReceiver(jmsQueue);
        QueueReceiver jmsConsumer2 = sessionSender.createReceiver(jmsQueue);

        QueueSender producer = sessionSender.createSender(jmsQueue1);

        TextMessage msg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage msg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((msg1 == null) ||
              (msg1.getText() == null) ||
              !msg1.getText().equals("testPersistentMessage_PersistentMsgClassicApi")) ||
             (msg2 != null) ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageReceiveClassicApi failed");
        }
    }

    public void testPersistentMessageClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender producer1 = sessionSender.createSender(jmsQueue);
        emptyQueue(jmsQCFBindings, jmsQueue);

        QueueSender producer2 = sessionSender.createSender(jmsQueue1);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer1.setDeliveryDelay(1000);
        TextMessage msg1 = sessionSender.createTextMessage("testPersistentMessage_PersistentMsgClassicApi");
        producer1.send(msg1);

        producer2.setDeliveryDelay(1000);
        producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        TextMessage msg2 = sessionSender.createTextMessage("testPersistentMessage_NonPersistentMsgClassicApi");
        producer2.send(msg2);

        sessionSender.close();
        con.close();
    }

    public void testPersistentMessageReceiveClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        MessageConsumer jmsConsumer1 = sessionSender.createConsumer(jmsQueue);
        MessageConsumer jmsConsumer2 = sessionSender.createConsumer(jmsQueue);

        QueueSender producer = sessionSender.createSender(jmsQueue1);

        TextMessage msg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage msg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((msg1 == null) ||
              (msg1.getText() == null) ||
              !msg1.getText().equals("testPersistentMessage_PersistentMsgClassicApi")) ||
             (msg2 != null) ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageReceiveClassicApi failed");
        }
    }

    public void testPersistentMessageTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber jmsConsumer1 = sessionSender.createDurableSubscriber(jmsTopic, "durPersMsgCA1");
        TopicSubscriber jmsConsumer2 = sessionSender.createDurableSubscriber(jmsTopic1, "durPersMsgCA2");

        TopicPublisher jmsProducer1 = sessionSender.createPublisher(jmsTopic);
        TopicPublisher jmsProducer2 = sessionSender.createPublisher(jmsTopic1);

        jmsProducer1.setDeliveryMode(DeliveryMode.PERSISTENT);
        jmsProducer1.setDeliveryDelay(1000);
        TextMessage msg1 = sessionSender.createTextMessage("testPersistentMessage_PersistentMsgTopicClassicApi");
        jmsProducer1.send(msg1);

        jmsProducer2.setDeliveryDelay(1000);
        jmsProducer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        TextMessage msg2 = sessionSender.createTextMessage("testPersistentMessage_NonPersistentMsgTopicClassicApi");
        jmsProducer2.send(msg2);

        con.close();
    }

    public void testPersistentMessageReceiveTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber jmsConsumer1 = sessionSender.createDurableSubscriber(jmsTopic, "durPersMsgCA1");
        TopicSubscriber jmsConsumer2 = sessionSender.createDurableSubscriber(jmsTopic1, "durPersMsgCA2");

        TopicPublisher jmsProducer = sessionSender.createPublisher(jmsTopic);

        TextMessage msg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage msg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((msg1 == null) ||
              (msg1.getText() == null) ||
              !msg1.getText().equals("testPersistentMessage_PersistentMsgTopicClassicApi")) ||
             (msg2 != null) ) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();

        sessionSender.unsubscribe("durPersMsgCA1");
        sessionSender.unsubscribe("durPersMsgCA2");
        sessionSender.close();

        con.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageReceiveTopicClassicApi failed");
        }
    }

    public void testPersistentMessageTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber jmsConsumer1 = sessionSender.createDurableSubscriber(jmsTopic, "durPersMsgCATcp1");
        TopicSubscriber jmsConsumer2 = sessionSender.createDurableSubscriber(jmsTopic1, "durPersMsgCATcp2");

        TopicPublisher jmsProducer1 = sessionSender.createPublisher(jmsTopic);
        TopicPublisher jmsProducer2 = sessionSender.createPublisher(jmsTopic1);

        jmsProducer1.setDeliveryMode(DeliveryMode.PERSISTENT);
        jmsProducer1.setDeliveryDelay(1000);
        TextMessage msg1 = sessionSender.createTextMessage("testPersistentMessage_PersistentMsgTopicClassicApiTcp");
        jmsProducer1.send(msg1);

        jmsProducer2.setDeliveryDelay(1000);
        jmsProducer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        TextMessage msg2 = sessionSender.createTextMessage("testPersistentMessage_NonPersistentMsgTopicClassicApiTcp");
        jmsProducer2.send(msg2);

        con.close();
    }

    public void testPersistentMessageReceiveTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber jmsConsumer1 = sessionSender.createDurableSubscriber(jmsTopic, "durPersMsgCATcp1");
        TopicSubscriber jmsConsumer2 = sessionSender.createDurableSubscriber(jmsTopic1, "durPersMsgCATcp2");

        TopicPublisher jmsProducer = sessionSender.createPublisher(jmsTopic);

        TextMessage msg1 = (TextMessage) jmsConsumer1.receive(30000);
        TextMessage msg2 = (TextMessage) jmsConsumer2.receive(30000);

        if ( ((msg1 == null) ||
              (msg1.getText() == null) ||
              !msg1.getText().equals("testPersistentMessage_PersistentMsgTopicClassicApiTcp")) ||
             (msg2 != null) ) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();

        sessionSender.unsubscribe("durPersMsgCATcp1");
        sessionSender.unsubscribe("durPersMsgCATcp2");
        sessionSender.close();

        con.close();

        if ( testFailed ) {
            throw new Exception("testPersistentMessageStoreReceiveTopicClassicApi_Tcp failed");
        }
    }

    public void testJSAD_Send_Message_P2PTest(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        emptyQueue(cf1, queue);

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender send = sessionSender.createSender(queue);
        String outbound = "Hello World from testJSAD_Send_Message_P2PTest";
        send.setDeliveryDelay(1000);
        send.send(sessionSender.createTextMessage(outbound));

        Queue queue1 = sessionSender.createQueue("QUEUE1");
        QueueReceiver rec = sessionSender.createReceiver(queue1);
        TextMessage receiveMsg = (TextMessage) rec.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testJSAD_Send_Message_P2PTest failed");
        }
    }

    /**
     * Basic point-to-point test with a single send to a queue and a receive
     * from the alias,
     */

    public void testJSAD_Receive_Message_P2PTest(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = sessionSender.createQueue("QUEUE1");
        emptyQueue(cf1, queue);
        QueueSender send = sessionSender.createSender(queue);

        String outbound = "Hello World from testJSAD_Receive_Message_P2PTest";
        send.setDeliveryDelay(1000);
        send.send(sessionSender.createTextMessage(outbound));

        Queue queue1 = sessionSender.createQueue("alias2Q1");

        QueueReceiver rec = sessionSender.createReceiver(queue1);

        TextMessage receiveMsg = (TextMessage) rec.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testJSAD_Receive_Message_P2PTest failed");
        }
    }

    public void testBasicTemporaryQueue(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession jmsSession = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        TemporaryQueue tempQ = jmsSession.createTemporaryQueue();

        QueueSender queueSender = jmsSession.createSender(tempQ);
        QueueReceiver queueReceiver = jmsSession.createReceiver(tempQ);

        TextMessage msg1 = jmsSession.createTextMessage("testBasicTemporaryQueue");
        queueSender.setDeliveryDelay(1000);
        queueSender.send(msg1);

        TextMessage recMessage = (TextMessage) queueReceiver.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("testBasicTemporaryQueue") ) {
            testFailed = true;
        }

        queueSender.close();
        queueReceiver.close();
        tempQ.delete();

        if ( testFailed ) {
            throw new Exception("testBasicTemporaryQueue failed");
        }
    }

    public void testSendMessageToQueue(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        emptyQueue(cf1, queue);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender send = sessionSender.createSender(queue);

        TextMessage msg = sessionSender.createTextMessage("ExceptionDestinationMessage");
        send.setDeliveryDelay(1000);
        send.send(msg);

        Thread.sleep(3000);

        con.close();
    }

    public void testReadMsgFromExceptionQueue(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_EXCEPTION_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession session = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = session.createReceiver(queue);
        TextMessage msg = (TextMessage) rec.receive(30000);

        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("ExceptionDestinationMessage") ) {
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testReadMsgFromExceptionQueue failed");
        }
    }

    public void testBytesMessage(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        boolean boolean1 = true;
        byte byte1 = 1;
        byte[] bytes1 = new byte[] { -3, -2, -1, 0, 1, 2, 3, 4, 5 };
        char char1 = '\u0001';
        double double1 = 1.0d;
        float float1 = 1.0f;
        int int1 = 1;
        long long1 = 1L;
        Integer integer1 = new Integer(1);
        short short1 = 1;
        String string1 = "one";

        PrintWriter out = response.getWriter();

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        emptyQueue(cf1, jmsQueue);

        QueueSession jmsSession = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        BytesMessage sendBytesMessage = jmsSession.createBytesMessage();

        out.println("Setting bytes");

        try {
            sendBytesMessage.writeBoolean(boolean1);
            sendBytesMessage.writeByte(byte1);
            sendBytesMessage.writeChar(char1);
            sendBytesMessage.writeDouble(double1);
            sendBytesMessage.writeFloat(float1);
            sendBytesMessage.writeInt(int1);
            sendBytesMessage.writeLong(long1);
            sendBytesMessage.writeShort(short1);
            sendBytesMessage.writeUTF(string1);
            sendBytesMessage.writeBytes(bytes1);
            sendBytesMessage.writeObject(integer1);

        } catch ( JMSException e ) {
            testFailed = true;
        }

        QueueSender send = jmsSession.createSender(jmsQueue);
        QueueReceiver queueReceiver = jmsSession.createReceiver(jmsQueue);
        send.setDeliveryDelay(1000);

        send.send(jmsQueue, sendBytesMessage);
        out.println("Sent bytes message [ " + sendBytesMessage + " ]");

        BytesMessage recBytesMessage = (BytesMessage) queueReceiver.receive(30000);
        out.println("Received bytes message [ " + recBytesMessage + " ]");

        if ( recBytesMessage.readBoolean() != boolean1) {
            testFailed = true;
        }
        if ( recBytesMessage.readByte() != byte1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readChar() != char1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readDouble() != double1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readFloat() != float1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readInt() != int1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readLong() != long1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readShort() != short1 ) {
            testFailed = true;
        }

        String s1 = recBytesMessage.readUTF();
        if ( (s1 == null) || !s1.equals(string1) ) {
            testFailed = true;
        }
        if ( recBytesMessage.readBytes(bytes1) != 9 ) {
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testBytesMessage failed");
        }
    }

    public void testComms_Send_Message_P2PTest_Default(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        emptyQueue(cf1, jmsQueue);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender send = sessionSender.createSender(jmsQueue);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue);
        String outbound = "testComms_Send_Message_P2PTest_Default";
        send.setDeliveryDelay(1000);
        send.send(jmsQueue, sessionSender.createTextMessage(outbound));

        TextMessage receiveMsg = (TextMessage) rec.receive(30000);
        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testComms_Send_Message_P2PTest_Default failed");
        }
    }

    public void testSendMessage(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        sendMessage(jmsQCFBindings);
    }
    
    public void testSendMessage_TCP(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        sendMessage(jmsQCFTCP);        
    }

    private void sendMessage(QueueConnectionFactory queueConnectionFactory)
        throws Exception {

        try (JMSContext jmsContext = queueConnectionFactory.createContext()) {
            emptyQueue(queueConnectionFactory, jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();

            long delayMilliseconds = deliveryDelay * 12;
            jmsProducer.setDeliveryDelay(delayMilliseconds);
            TextMessage sendMsg = jmsContext.createTextMessage(this.getClass().getName()+".testSendMessage() deliveryDelay="+delayMilliseconds+" milliseconds, sentAt:"+new Date());
            sendMsg.setLongProperty("MustArriveAfter",System.currentTimeMillis()+delayMilliseconds);
            sendAndCheckDeliveryTime(jmsProducer, jmsQueue, sendMsg);
        }
    }

    public void testReceiveMessage(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        receiveMessage(jmsQCFBindings);
    }

    public void testReceiveMessage_TCP(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        receiveMessage(jmsQCFTCP);
    }
    
    private void receiveMessage(QueueConnectionFactory queueConnectionFactory)
        throws Exception {

        try (JMSContext jmsContext = queueConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(deliveryDelay*12);
            if (receivedMessage == null)
                throw new Exception("No message received");
            if ( !receivedMessage.getText().startsWith(this.getClass().getName()+".testSendMessage() "))
                throw new Exception("Incorrect Message received:"+receivedMessage.getText());
            if (receivedMessage.getLongProperty("MustArriveAfter") > System.currentTimeMillis())
                throw new Exception("Message arrived too soon\n"
                                   +"MustArriveAfter:"+receivedMessage.getLongProperty("MustArriveAfter")+" time now:"+System.currentTimeMillis()+"\n"
                                   +"Message:"+receivedMessage.getText());        
        }      
    }

}
