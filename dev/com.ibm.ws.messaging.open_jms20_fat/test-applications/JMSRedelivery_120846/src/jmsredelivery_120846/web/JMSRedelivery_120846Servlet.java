/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmsredelivery_120846.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSRedelivery_120846Servlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;
    public static Queue jmsQueue1;
    public static Queue jmsQueue2;
    public static Queue jmsQueue10;
    public static Queue jmsQueue11;
    public static boolean exceptionFlag;

    // for defect 175486
    public static TopicConnectionFactory jmsTopicCF;
    public static Topic jmsTopic1;
    public static Topic jmsTopic2;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQCFBindings = getQCFBindings();
            jmsQCFTCP = getQCFTCP();
            jmsQueue = getQueue("jndi_INPUT_Q");
            jmsQueue1 = getQueue("eis/queue1");
            jmsQueue2 = getQueue("eis/queue2");
            jmsQueue10 = getQueue("queue/test");
            jmsQueue11 = getQueue("Queue11/test");
            //for defect 175486
            jmsTopicCF = getTCF();
            jmsTopic1 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
            jmsTopic2 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");

        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(JMSRedelivery_120846Servlet.class); // injection
        // engine
        // doesn't
        // like
        // this
        // at
        // the
        // class
        // level
        Tr.entry(this, tc, test);
        try {
            System.out.println("Start: " + test);
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println("End: " + test);
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Error: " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testInitialJMSXDeliveryCount_B_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSProducer producer = jmsContext.createProducer();

        producer.send(jmsQueue, "testInitialJMSXDeliveryCount_B_SecOff");

        JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);

        TextMessage msg = (TextMessage) consumer.receive(5000);
        System.out.println(msg.getText());

        if (!(msg.getIntProperty("JMSXDeliveryCount") == 1)) {
            System.out.println("The received message is : " + msg.getText() + " and the Initial JMSXDeliveryCount value expected is 1 but actual value is "
                               + msg.getIntProperty("JMSXDeliveryCount"));
            exceptionFlag = true;
        }

        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testInitialJMSXDeliveryCount_B_SecOff failed");
    }

    public void testInitialJMSXDeliveryCount_TcpIp_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSProducer producer = jmsContext.createProducer();

        producer.send(jmsQueue, "testInitialJMSXDeliveryCount_TcpIp_SecOff");

        JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);

        TextMessage msg = (TextMessage) consumer.receive(5000);
        System.out.println(msg.getText());

        if (!(msg.getIntProperty("JMSXDeliveryCount") == 1)) {
            System.out.println("The received message is : " + msg.getText() + " and the Initial JMSXDeliveryCount value expected is 1 but actual value is "
                               + msg.getIntProperty("JMSXDeliveryCount"));
            exceptionFlag = true;
        }

        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testInitialJMSXDeliveryCount_TcpIp_SecOff failed");
    }

    public void testRDC_B(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;

        JMSContext context = jmsQCFBindings.createContext();

        Queue replyQueue = (Queue) new InitialContext()
                        .lookup("java:comp/env/jndi_INPUT_Q");

        emptyQueue(jmsQCFBindings, jmsQueue1);
        emptyQueue(jmsQCFBindings, replyQueue);
        TextMessage msg = context.createTextMessage("testRDC_B");
        context.createProducer().send(jmsQueue1, msg);

        JMSConsumer receiver = context.createConsumer(replyQueue);
        MapMessage rec_msg;
        rec_msg = (MapMessage) receiver.receive(30000);

        System.out.println("Message text : " + rec_msg.getStringProperty("msgText"));
        System.out.println("Is message redelivered : " + rec_msg.getBooleanProperty("redelivered"));
        System.out.println("JMSDeliveryCount value is : " + rec_msg.getIntProperty("deliveryCount"));

        if (!(rec_msg.getStringProperty("msgText").equals("testRDC_B") && rec_msg.getBooleanProperty("redelivered") && rec_msg.getIntProperty("deliveryCount") == 2))
            exceptionFlag = true;

        context.close();
        if (exceptionFlag)
            throw new WrongException("testRDC_B failed");
    }

    public void testRDC_TcpIp(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {

        JMSContext context = jmsQCFBindings.createContext();
        Queue replyQueue = (Queue) new InitialContext()
                        .lookup("java:comp/env/jndi_INPUT_Q");
        //here jmsQCFBindings points to jndi_JMS_BASE_QCF which is the CF used to connect for remote connections (check server.xml). 
        //The reason for this is the MDB uses QCF with this name and it expects all transactions should use the same.
        emptyQueue(jmsQCFBindings, jmsQueue1);
        emptyQueue(jmsQCFBindings, replyQueue);
        TextMessage msg = context.createTextMessage("testRDC_TcpIp");

        context.createProducer().send(jmsQueue1, msg);

        JMSConsumer receiver = context.createConsumer(replyQueue);
        MapMessage rec_msg;
        rec_msg = (MapMessage) receiver.receive(30000);

        System.out.println("Message text : " + rec_msg.getStringProperty("msgText"));
        System.out.println("Is message redelivered : " + rec_msg.getBooleanProperty("redelivered"));
        System.out.println("JMSDeliveryCount value is : " + rec_msg.getIntProperty("deliveryCount"));

        if (!(rec_msg.getStringProperty("msgText").equals("testRDC_TcpIp") && rec_msg.getBooleanProperty("redelivered") && rec_msg.getIntProperty("deliveryCount") == 2))
            exceptionFlag = true;

        context.close();
        if (exceptionFlag)
            throw new WrongException("testRDC_TcpIp failed");

    }

    public void testMaxRDC_B(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;

        JMSContext context = jmsQCFBindings.createContext();

        Queue replyQueue = (Queue) new InitialContext()
                        .lookup("java:comp/env/jndi_INPUT_Q");

        emptyQueue(jmsQCFBindings, jmsQueue2);
        emptyQueue(jmsQCFBindings, replyQueue);
        TextMessage msg = context.createTextMessage("testMaxRDC_B");
        context.createProducer().send(jmsQueue2, msg);

        JMSConsumer receiver = context.createConsumer(replyQueue);
        TextMessage rec_msg;
        rec_msg = (TextMessage) receiver.receive(30000);

        if (rec_msg != null) {
            exceptionFlag = true;
            System.out.println("Message exists : " + rec_msg.getText());

        }

        context.close();
        if (exceptionFlag)
            throw new WrongException("testMaxRDC_B failed");
    }

    public void testMaxRDC_TcpIp(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;

        JMSContext context = jmsQCFBindings.createContext();
        //here jmsQCFBindings points to jndi_JMS_BASE_QCF which is the CF used to connect for remote connections (check server.xml). 
        //The reason for this is the MDB uses QCF with this name and it expects all transactions should use the same.
        Queue replyQueue = (Queue) new InitialContext()
                        .lookup("java:comp/env/jndi_INPUT_Q");

        emptyQueue(jmsQCFBindings, jmsQueue2);
        emptyQueue(jmsQCFBindings, replyQueue);
        TextMessage msg = context.createTextMessage("testMaxRDC_TcpIp");
        context.createProducer().send(jmsQueue2, msg);

        JMSConsumer receiver = context.createConsumer(replyQueue);
        TextMessage rec_msg;
        rec_msg = (TextMessage) receiver.receive(30000);

        if (rec_msg != null) {
            exceptionFlag = true;
            System.out.println("Message exists : " + rec_msg.getText());

        }

        context.close();
        if (exceptionFlag)
            throw new WrongException("testMaxRDC_TcpIp failed");
    }

    public void testQueueSendMDB(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;

        JMSContext context = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue10);

        TextMessage msg = context.createTextMessage("testQueueSendMDB");
        context.createProducer().send(jmsQueue10, msg);

        System.out.println("Sent message: " + msg.getText());

        context.close();

    }

    public void testAnnotatedMDB(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;

        JMSContext context = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue11);

        TextMessage msg = context.createTextMessage("testAnnotatedMDB");
        context.createProducer().send(jmsQueue11, msg);

        System.out.println("Sent message: " + msg.getText());

    }

//  For defect 175486
    public void testTopicSendMDB(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;
        System.out.println("Topic Conection factory" + jmsTopicCF);
        JMSContext context = jmsTopicCF.createContext();

        JMSConsumer jmsConsumer = context.createSharedConsumer(jmsTopic1, "DURATEST1");
        JMSProducer jmsProducer = context.createProducer();

        TextMessage msg = context.createTextMessage("testTopicSendMDB");

        jmsProducer.send(jmsTopic1, msg);

        context.close();

    }

    public void testTopicAnnotatedMDB(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;

        JMSContext context = jmsTopicCF.createContext();

        JMSConsumer jmsConsumer = context.createSharedConsumer(jmsTopic2, "DURATEST1");
        JMSProducer jmsProducer = context.createProducer();

        TextMessage msg = context.createTextMessage("testTopicAnnotatedMDB");

        jmsProducer.send(jmsTopic2, msg);

        context.close();

    }

    public void testTargetChain_B(HttpServletRequest request, HttpServletResponse response) throws Throwable
    {
        exceptionFlag = false;
        JMSContext context = jmsQCFTCP.createContext();
        JMSProducer jmsProducer = context.createProducer();

        TextMessage msg = context.createTextMessage("testin_TargetTransportChain");

        jmsProducer.send(jmsQueue1, msg);

        context.close();

    }

    public static QueueConnectionFactory getQCFBindings() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;

    }

    public QueueConnectionFactory getQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        return cf1;

    }

    public Queue getQueue(String name) throws NamingException {

        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/" + name);

        return queue;
    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

    public void emptyQueue(QueueConnectionFactory qcf, Queue q)
                    throws Exception {

        JMSContext context = qcf.createContext();
        QueueBrowser qb = context.createBrowser(q);
        Enumeration e = qb.getEnumeration();
        JMSConsumer consumer = context.createConsumer(q);
        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        for (int i = 0; i < numMsgs; i++) {
            Message msg = consumer.receive();
        }

        context.close();
    }

    public static TopicConnectionFactory getTCF()
                    throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf");

        return tcf1;

    }

}
