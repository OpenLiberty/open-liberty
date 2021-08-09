/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.jms.Destination;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.JMSSecurityRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.TemporaryQueue;
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
public class JMSContextServlet extends HttpServlet {

    public static final String JMXMessage = "This is MessagingMBeanServlet.";
    public final static String MBEAN_TYPE_ME = "WEMMessagingEngine";
    public boolean sessionValue = false;
    public boolean connectionStart = false;
    public boolean flag = false;
    public boolean compFlag = false;
    public boolean exp = false;
    public QueueConnectionFactory QCFBindings;
    public QueueConnectionFactory QCFTCP;
    public TopicConnectionFactory TCFBindings;
    public TopicConnectionFactory TCFTCP;
    public Queue queue;
    public Queue queue1;
    public Queue queue2;
    public Queue replyQ;
    public Topic topic;
    public JMSContext jmsContext;
    public JMSConsumer jmsConsumer;
    public JMSProducer jmsProducer;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {

            QCFBindings = getQCFBindings();
            QCFTCP = getQCFTCP();
            TCFBindings = getTCFBindings();
            TCFTCP = getTCFTCP();
            queue = getQueue("jndi_INPUT_Q");
            queue1 = getQueue("jndi_INPUT_Q1");
            queue2 = getQueue("jndi_INPUT_Q2");
            replyQ = getQueue("jndi_INPUT_Q3");
            topic = getTopic("eis/topic1");

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
        final TraceComponent tc = Tr.register(JMSContextServlet.class); // injection
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
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            System.out.println("Start Test: " + test);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println("End Test :" + test);
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            System.out.println("End Test :" + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testCreateContext_B_SecOn(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        if (!(jmsContext.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE)
            && (jmsContext.getAutoStart() == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateContext_B_SecOff failed");
        jmsContext.close();

    }

    public void testCreateContext_TCP_SecOn(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        if (!(jmsContext.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE)
            && (jmsContext.getAutoStart() == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateContext_B_SecOff failed");
        jmsContext.close();

    }

    public void testFailAuth_B_SecOn(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "userpwd";
        JMSContext jmsContext = null;
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        try {
            jmsContext = QCFBindings.createContext(userName, password, smode);
        } catch (JMSSecurityRuntimeException ex3) {
            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testFailAuth_B_SecOn failed");

    }

    public void testFailAuth_TCP_SecOn(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "userpwd";
        JMSContext jmsContext = null;
        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        try {
            jmsContext = QCFTCP.createContext(userName, password, smode);
        } catch (JMSSecurityRuntimeException ex3) {

            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testFailAuth_TCP_SecOn failed");

    }

    public void testcreateContextwithUser_B_SecOn(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = QCFBindings.createContext(userName, password);
        if (!(jmsContext.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE)
            && (jmsContext.getAutoStart() == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_B_SecOn failed");
        jmsContext.close();

    }

    public void testcreateContextwithUser_TCP_SecOn(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = QCFTCP.createContext(userName, password);
        if (!(jmsContext.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE)
            && (jmsContext.getAutoStart() == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_TCP_SecOn failed");

        jmsContext.close();

    }

    public void testcreateContextwithUserSessionMode_B_SecOn(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = QCFBindings.createContext(userName, password,
                                                          smode);
        if (!(jmsContext.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE)
            && (jmsContext.getAutoStart() == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_TCP_SecOn failed");

        jmsContext.close();

    }

    public void testcreateContextwithUserSessionMode_TCP_SecOn(
                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext;
        jmsContext = QCFTCP.createContext(userName, password, smode);
        if (!(jmsContext.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE)
            && (jmsContext.getAutoStart() == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_TCP_SecOn failed");

        jmsContext.close();

    }

    public void testCreateObjectMessage_B_SecOn(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        QueueBrowser qb = jmsContext.createBrowser(queue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue1);
        int msgs = getMessageCount(qb);

        for (int i = msgs; i > 0; i--) {
            jmsConsumer.receive(30000);
        }

        String serlzedObj = new StockObject("TEST STOCK", 5400).toString();
        ObjectMessage msg = jmsContext.createObjectMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setObject(serlzedObj);
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        jmsProducer = jmsContext.createProducer().send(queue1, msg);

        int numMsgs = getMessageCount(qb);

        jmsConsumer.receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateObjectMessage_B_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testCreateObjectMessage_TCP_SecOn(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        QueueBrowser qb = jmsContext.createBrowser(queue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue1);
        int msgs = getMessageCount(qb);

        for (int i = msgs; i > 0; i--) {
            jmsConsumer.receive(30000);
        }

        String serlzedObj = new StockObject("TEST STOCK", 5400).toString();
        ObjectMessage msg = jmsContext.createObjectMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setObject(serlzedObj);
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        jmsProducer = jmsContext.createProducer().send(queue1, msg);

        int numMsgs = getMessageCount(qb);

        jmsConsumer.receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateObjectMessage_TCP_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();
    }

    // 118061_6 Verify creation of Text Message from
    // JMSContext.createTextMessage(String text).

    public void testCreateTextMessageStr_B_SecOn(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContext = QCFBindings.createContext();
        jmsContext.createConsumer(queue1).receive(30000);
        String compare = "Hello";
        String str = "Hello this is a test case for TextMessage";
        TextMessage msg = jmsContext.createTextMessage(str);
        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(compare);

        jmsContext.createProducer().send(queue1, msg);
        QueueBrowser qb = jmsContext.createBrowser(queue1);

        int numMsgs = getMessageCount(qb);

        jmsContext.createConsumer(queue).receive(30000);

        System.out.println("Nummsgs:" + numMsgs);

        if (!(numMsgs == 1 && msg.getText().equals(compare)))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateTextMessageStr_B_SecOn failed");

        jmsContext.close();

    }

    public void testCreateTextMessageStr_TCP_SecOn(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        jmsContext.createConsumer(queue1).receive(30000);
        String compare = "Hello";
        String str = "Hello this is a test case for TextMessage";
        TextMessage msg = jmsContext.createTextMessage(str);
        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(compare);
        JMSProducer jmsProducer = jmsContext.createProducer().send(queue1, msg);
        QueueBrowser qb = jmsContext.createBrowser(queue1);
        Enumeration e = qb.getEnumeration();
        int numMsgs = 0;
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }
        if (!(numMsgs == 1 && msg.getText().equals(compare)))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateTextMessageStr_TCP_SecOn failed");
        jmsContext.close();

    }

    public void testJMSCorrelationIDAsBytes_B_SecOn(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean compare = false;
        boolean flag = false;
        // JMSConsumer jmsConsumer;
        // JMSProducer jmsProducer;
        JMSContext jmsContext = QCFBindings.createContext();
        Message msg = jmsContext.createMessage();
        byte[] defBytes = msg.getJMSCorrelationIDAsBytes();
        String startCorrel = msg.getJMSCorrelationID();

        // set and retrieve a byte[]
        byte[] testA = { 1, 2, 3, 4 };
        msg.setJMSCorrelationIDAsBytes(testA);

        byte[] resultA = msg.getJMSCorrelationIDAsBytes();

        boolean testComp = false;

        for (int i = 0; i < testA.length; i++) {
            if (testA[i] != resultA[i]) {
                compare = false;
            } else
                compare = true;
        }

        String correl = "CorrelID";

        // check that multiple set sequences always return the last set
        // value
        // a) set bytes then string
        msg.setJMSCorrelationIDAsBytes(testA);
        msg.setJMSCorrelationID(correl);
        byte[] resultE = msg.getJMSCorrelationIDAsBytes();

        // b) set string then bytes
        msg.setJMSCorrelationID(correl);
        msg.setJMSCorrelationIDAsBytes(testA);
        byte[] resultF = msg.getJMSCorrelationIDAsBytes();

        testComp = false;
        for (int j = 0; j < testA.length; j++) {
            if (testA[j] != resultF[j]) {
                flag = false;
            } else
                flag = true;
        }

        if (!(resultE != null && compare == true && flag == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSCorrelationIDAsBytes_B_SecOn failed");
        jmsContext.close();

    }

    public void testJMSCorrelationIDAsBytes_TCP_SecOn(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        boolean flag = false;
        boolean compare = false;
        // JMSConsumer jmsConsumer;
        // JMSProducer jmsProducer;
        JMSContext jmsContext = QCFTCP.createContext();
        Message msg = jmsContext.createMessage();

        byte[] defBytes = msg.getJMSCorrelationIDAsBytes();

        String startCorrel = msg.getJMSCorrelationID();

        // set and retrieve a byte[]
        byte[] testA = { 1, 2, 3, 4 };
        msg.setJMSCorrelationIDAsBytes(testA);

        byte[] resultA = msg.getJMSCorrelationIDAsBytes();

        boolean testComp = false;

        for (int i = 0; i < testA.length; i++) {
            if (testA[i] != resultA[i]) {
                compare = false;
            } else
                compare = true;
        }

        String correl = "CorrelID";

        // check that multiple set sequences always return the last set
        // value
        // a) set bytes then string
        msg.setJMSCorrelationIDAsBytes(testA);
        msg.setJMSCorrelationID(correl);
        byte[] resultE = msg.getJMSCorrelationIDAsBytes();

        // b) set string then bytes
        msg.setJMSCorrelationID(correl);
        msg.setJMSCorrelationIDAsBytes(testA);
        byte[] resultF = msg.getJMSCorrelationIDAsBytes();

        testComp = false;
        for (int j = 0; j < testA.length; j++) {
            if (testA[j] != resultF[j]) {
                flag = false;
            } else
                flag = true;
        }

        if (!(resultE != null && compare == true && flag == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSCorrelationIDAsBytes_TCP_SecOn failed");
        jmsContext.close();

    }

    public void testcreateBrowser_B_SecOn(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        QueueBrowser qb1 = jmsContext.createBrowser(queue);
        int msgCountPrev = getMessageCount(qb1);

        if (msgCountPrev > 0) {
            for (int i = msgCountPrev; i > 0; i--)
                jmsConsumer.receive(30000);
        }

        jmsProducer.send(queue, "Tester");

        Enumeration e = qb1.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        jmsConsumer.receive(30000);
        if (!(numMsgs == 1))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testcreateBrowser_B_SecOn failed");

        jmsConsumer.close();
        jmsContext.close();
    }

    public void testcreateBrowser_TCP_SecOn(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);
        jmsProducer.send(queue, "Tester");

        QueueBrowser qb1 = jmsContext.createBrowser(queue);

        Enumeration e = qb1.getEnumeration();

        int numMsgs = 0;

        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }
        if ((numMsgs == 1))
            exceptionFlag = true;
        if (!exceptionFlag)
            throw new WrongException("testcreateBrowser_TCP_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testcreateBrowser_MessageSelector_B_SecOn(
                                                          HttpServletRequest request, HttpServletResponse response) throws Throwable {

        final int NMSGS = 5;
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);
        TextMessage msg = jmsContext.createTextMessage("browse selector test message");

        QueueBrowser qb1 = null;
        try {

            qb1 = jmsContext.createBrowser(queue, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {

            ex3.printStackTrace();
        }

        QueueBrowser qb2 = null;

        try {
            qb2 = jmsContext.createBrowser(queue, "colour = 'blue'");
        } catch (InvalidDestinationRuntimeException ex3) {

            ex3.printStackTrace();
        }

        for (int i = 0; i < NMSGS; i++) {
            msg.setStringProperty("colour", "red");
            jmsProducer.send(queue, msg);

            msg.setStringProperty("colour", "blue");
            jmsProducer.send(queue, msg);
        }

        // now use two browsers to scan the queue

        Enumeration e1 = qb1.getEnumeration();
        Enumeration e2 = qb2.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        int nMsgRed = 0;
        int nWrongRed = 0;
        while (e1.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e1.nextElement();

            String colour1 = msg1.getStringProperty("colour");

            if (colour1 != null && colour1.equals("red"))
                nMsgRed++;
            else
                nWrongRed++;

        }

        int nMsgBlue = 0;
        int nWrongBlue = 0;

        while (e2.hasMoreElements()) {
            TextMessage msg2 = (TextMessage) e2.nextElement();
            String colour2 = msg2.getStringProperty("colour");

            if (colour2 != null && colour2.equals("blue"))
                nMsgBlue++;
            else
                nWrongBlue++;

        }

        if (!(nMsgRed == NMSGS && nMsgBlue == NMSGS))

            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testcreateBrowser_MessageSelector_B_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testcreateBrowser_MessageSelector_TCP_SecOn(
                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {

        final int NMSGS = 5;
        boolean exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);
        TextMessage msg = jmsContext.createTextMessage("browse selector test message");

        QueueBrowser qb1 = null;
        try {

            qb1 = jmsContext.createBrowser(queue, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {
            val1 = true;
            ex3.printStackTrace();
        }

        QueueBrowser qb2 = null;

        try {
            qb2 = jmsContext.createBrowser(queue, "colour = 'blue'");
        } catch (InvalidDestinationRuntimeException ex3) {
            val2 = true;
            ex3.printStackTrace();
        }

        for (int i = 0; i < NMSGS; i++) {
            msg.setStringProperty("colour", "red");
            jmsProducer.send(queue, msg);

            msg.setStringProperty("colour", "blue");
            jmsProducer.send(queue, msg);
        }

        // now use two browsers to scan the queue

        Enumeration e1 = qb1.getEnumeration();
        Enumeration e2 = qb2.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        int nMsgRed = 0;
        int nWrongRed = 0;
        while (e1.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e1.nextElement();

            String colour1 = msg1.getStringProperty("colour");

            if (colour1 != null && colour1.equals("red"))
                nMsgRed++;
            else
                nWrongRed++;

        }

        int nMsgBlue = 0;
        int nWrongBlue = 0;

        while (e2.hasMoreElements()) {
            TextMessage msg2 = (TextMessage) e2.nextElement();
            String colour2 = msg2.getStringProperty("colour");

            if (colour2 != null && colour2.equals("blue"))
                nMsgBlue++;
            else
                nWrongBlue++;

        }

        if (!(nMsgRed == NMSGS && nMsgBlue == NMSGS))

            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testcreateBrowser_MessageSelector_TCP_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testJMSProducerSendMessage_B_SecOn(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        Message msg = jmsContext.createMessage();
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");
        JMSProducer jmsProducer = jmsContext.createProducer().setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(queue2, msg);
        QueueBrowser qb = jmsContext.createBrowser(queue2);
        Enumeration e = qb.getEnumeration();
        int numMsgs = 0;
        while (e.hasMoreElements()) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        jmsConsumer.receive(30000);
        if (!(numMsgs == 1
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))

            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_B_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendMessage_TCP_SecOn(
                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);
        jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        Message msg = jmsContext.createMessage();
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");
        JMSProducer jmsProducer = jmsContext.createProducer().setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(queue2, msg);
        QueueBrowser qb = jmsContext.createBrowser(queue2);
        Enumeration e = qb.getEnumeration();
        int numMsgs = 0;
        while (e.hasMoreElements()) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        jmsConsumer.receive(30000);

        if (!(numMsgs == 1
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))

            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendMessage_Topic_B_SecOn(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("Test");
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");
        jmsProducer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(topic, msg);

        TextMessage recvdMessage = null;
        String msg1 = null;
        recvdMessage = (TextMessage) jmsConsumer.receive(30000);
        msg1 = recvdMessage.getText();

        jmsConsumer.receive(30000);

        if (!(recvdMessage.getText().equals("Test")
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_Topic_B_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendMessage_Topic_TCP_SecOn(
                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("Test");
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");
        jmsProducer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(topic, msg);

        TextMessage recvdMessage = null;
        String msg1 = null;
        recvdMessage = (TextMessage) jmsConsumer.receive(5000);
        msg1 = recvdMessage.getText();
        jmsConsumer.receive(30000);
        if (!(recvdMessage.getText().equals("Test")
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_Topic_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendTextMessage_B_SecOn(
                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        JMSProducer jmsProducer = jmsContext.createProducer().setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(queue2, "This is the messageBody");

        QueueBrowser qb = jmsContext.createBrowser(queue2);
        Enumeration e = qb.getEnumeration();
        int numMsgs = 0;
        while (e.hasMoreElements()) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }
        String msgBody = jmsConsumer.receive(30000).getBody(String.class);

        if (!(numMsgs == 1 && msgBody.equals("This is the messageBody")
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;
        jmsConsumer.receive(30000);
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_Topic_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendTextMessage_TCP_SecOn(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        jmsProducer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(queue2, "This is the messageBody");

        QueueBrowser qb = jmsContext.createBrowser(queue2);
        Enumeration e = qb.getEnumeration();
        int numMsgs = 0;
        while (e.hasMoreElements()) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }
        String msgBody = jmsConsumer.receive(30000).getBody(String.class);

        if (!(numMsgs == 1 && msgBody.equals("This is the messageBody")
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_Topic_TCP_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOn(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();
        Topic topic1 = null;
        try {
            jmsProducer.send(topic1, "This is the message body");
        } catch (InvalidDestinationRuntimeException ex) {
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOn failed");
        jmsContext.close();

    }

    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOn(
                                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();
        Topic topic1 = null;
        try {
            jmsProducer.send(topic1, "This is the message body");
        } catch (InvalidDestinationRuntimeException ex) {
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOn failed ");
        jmsContext.close();

    }

    public void testJMSProducerSendMapMessage_Topic_B_SecOn(
                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsConsumer.receive(30000);
        MapMessage mapMessage = jmsContext.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        jmsProducer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(topic, mapMessage);

        boolean correctMapBody = jmsConsumer.receive(30000).getBody(java.util.Map.class).containsValue(val);
        if (!(correctMapBody == true
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_Topic_B_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendMapMessage_Topic_TCP_SecOn(
                                                              HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        JMSProducer jmsProducer = jmsContext.createProducer();
        MapMessage mapMessage = jmsContext.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        jmsProducer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(topic, mapMessage);

        boolean correctMapBody = jmsConsumer.receive(10000).getBody(java.util.Map.class).containsValue(val);
        if (!(correctMapBody == true
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_Topic_TCP_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testJMSProducerSendByteMessage_B_SecOn(
                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        byte[] content = new byte[] { 127, 0 };
        jmsProducer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(queue2, content);

        QueueBrowser qb = jmsContext.createBrowser(queue2);

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;

        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        String recvdByteBody = Arrays.toString(jmsConsumer.receiveBodyNoWait(byte[].class));

        if (!(numMsgs == 1 && recvdByteBody.equals("[127, 0]")
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_B_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendByteMessage_TCP_SecOn(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        byte[] content = new byte[] { 127, 0 };
        jmsProducer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(queue2, content);

        QueueBrowser qb = jmsContext.createBrowser(queue2);

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;

        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        String recvdByteBody = Arrays.toString(jmsConsumer.receiveBodyNoWait(byte[].class));

        if (!(numMsgs == 1 && recvdByteBody.equals("[127, 0]")
              && jmsProducer.getJMSCorrelationID().equals("TestCorrelID") && jmsProducer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testJMSProducerSendObjectMessage_Topic_B_SecOn(
                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContext.createProducer();
        producer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(topic, (Serializable) objBody);

        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);

        if (!(msgRecvd.equals(objBody)
              && producer.getJMSCorrelationID().equals("TestCorrelID") && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendObjectMessage_Topic_B_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testJMSProducerSendObjectMessage_Topic_TCP_SecOn(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        jmsConsumer.receive(30000);
        Object objBody = new String("This is the Message body.");
        JMSProducer producer = jmsContext.createProducer();
        producer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").send(topic, (Serializable) objBody);

        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);

        if (!(msgRecvd.equals(objBody)
              && producer.getJMSCorrelationID().equals("TestCorrelID") && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendObjectMessage_Topic_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testPropertyExists_B_SecOn(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable

    {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();
        boolean notfound = jmsProducer.propertyExists("RandomProperty");

        jmsProducer.setProperty("SetString", "Tester");

        boolean found = jmsProducer.propertyExists("SetString");

        if (!(notfound == false && found == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testPropertyExists_B_SecOn failed");
        jmsContext.close();

    }

    public void testPropertyExists_TCP_SecOn(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();
        boolean notfound = jmsProducer.propertyExists("RandomProperty");

        jmsProducer.setProperty("SetString", "Tester");

        boolean found = jmsProducer.propertyExists("SetString");

        if (!(notfound == false && found == true))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testPropertyExists_TCP_SecOn failed");
        jmsContext.close();
    }

    public void testSetDisableMessageID_B_SecOn(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable

    {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage msg = jmsContext.createTextMessage();

        boolean defaultSetMessageID = jmsProducer.getDisableMessageID();

        jmsProducer.setDisableMessageID(true);

        jmsProducer.send(queue2, msg);

        String msgID = jmsConsumer.receive(30000).getJMSMessageID();

        if ((defaultSetMessageID == false && msgID == null))
            exceptionFlag = true;
        if (!exceptionFlag)
            throw new WrongException("testSetDisableMessageID_B_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testSetDisableMessageID_TCP_SecOn(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage msg = jmsContext.createTextMessage();

        boolean defaultSetMessageID = jmsProducer.getDisableMessageID();

        jmsProducer.setDisableMessageID(true);

        jmsProducer.send(queue2, msg);

        String msgID = jmsConsumer.receive(30000).getJMSMessageID();

        if ((defaultSetMessageID == false && msgID == null))
            exceptionFlag = true;
        if (!exceptionFlag)
            throw new WrongException("testSetDisableMessageID_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testSetDisableMessageTimestamp_B_SecOn(
                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage msg = jmsContext.createTextMessage();

        boolean defaultSetMessageTimestamp = jmsProducer.getDisableMessageTimestamp();

        jmsProducer.setDisableMessageTimestamp(true);

        jmsProducer.send(queue2, msg);

        long msgTS = jmsConsumer.receive(30000).getJMSTimestamp();

        if (!(defaultSetMessageTimestamp == false && msgTS == 0))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testSetDisableMessageTimestamp_B_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testSetDisableMessageTimestamp_TCP_SecOn(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage msg = jmsContext.createTextMessage();

        boolean defaultSetMessageTimestamp = jmsProducer.getDisableMessageTimestamp();

        jmsProducer.setDisableMessageTimestamp(true);

        jmsProducer.send(queue2, msg);

        long msgTS = jmsConsumer.receive(30000).getJMSTimestamp();

        if (!(defaultSetMessageTimestamp == false && msgTS == 0))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testSetDisableMessageTimestamp_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testPriority_B_SecOn(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage tmsg = jmsContext.createTextMessage();
        String output = null;
        for (int i = 0; i < 10; i++) {

            tmsg = jmsContext.createTextMessage();
            jmsProducer.setPriority(i);
            jmsProducer.send(queue2, tmsg);
        }
        TextMessage msgR = null;

        QueueBrowser qb = jmsContext.createBrowser(queue2);
        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;

        while (e.hasMoreElements() && numMsgs < 10) {
            msgR = (TextMessage) jmsConsumer.receive(30000);
            if (msgR.getJMSPriority() + numMsgs == 9) {

                exceptionFlag = true;

            } else {

                System.out.println("Message is not in order");
            }
            numMsgs++;
        }

        if (!exceptionFlag)
            throw new WrongException("testPriority_B_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testPriority_TCP_SecOn(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage tmsg = jmsContext.createTextMessage();
        String output = null;
        for (int i = 0; i < 10; i++) {

            tmsg = jmsContext.createTextMessage();
            jmsProducer.setPriority(i);
            jmsProducer.send(queue2, tmsg);
        }
        TextMessage msgR = null;

        QueueBrowser qb = jmsContext.createBrowser(queue2);
        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;

        while (e.hasMoreElements() && numMsgs < 10) {
            msgR = (TextMessage) jmsConsumer.receive(30000);
            if (msgR.getJMSPriority() + numMsgs == 9) {

                exceptionFlag = true;

            } else {

                System.out.println("Message is not in order");
            }
            numMsgs++;
        }

        if (!exceptionFlag)
            throw new WrongException("testPriority_TCP_SecOn failed");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testSetGetJMSReplyTo_B_SecOn(HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setJMSReplyTo(replyQ);
        TextMessage msg = jmsContext.createTextMessage("testSetGetJMSReplyTo");
        jmsProducer.setDisableMessageID(false);
        jmsProducer.send(queue2, msg);

        JMSContext jmsContRec = QCFBindings.createContext();
        JMSConsumer qr = jmsContRec.createConsumer(queue2);
        TextMessage recmsg = (TextMessage) qr.receive(30000);
        Destination d1 = jmsProducer.getJMSReplyTo();
        JMSProducer replyProd = jmsContRec.createProducer();
        TextMessage msg1 = jmsContRec.createTextMessage("testSetGetJMSReplyTo: Reply Msg");

        msg1.setJMSCorrelationID(recmsg.getJMSMessageID());
        replyProd.send(d1, msg1);
        JMSConsumer consumer = jmsContext.createConsumer(replyQ);
        TextMessage replymsg = (TextMessage) consumer.receive(30000);
        String replyID = replymsg.getJMSCorrelationID().toString();

        if (!(replyID.equals(msg.getJMSMessageID())))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testSetGetJMSReplyTo_B_SecOn failed");
        qr.close();
        consumer.close();
        jmsContext.close();

    }

    public void testSetGetJMSReplyTo_TCP_SecOn(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setJMSReplyTo(replyQ);
        TextMessage msg = jmsContext.createTextMessage("testSetGetJMSReplyTo");
        jmsProducer.setDisableMessageID(false);
        jmsProducer.send(queue2, msg);

        JMSContext jmsContRec = QCFTCP.createContext();
        JMSConsumer qr = jmsContRec.createConsumer(queue2);
        TextMessage recmsg = (TextMessage) qr.receive(30000);
        Destination d1 = jmsProducer.getJMSReplyTo();
        JMSProducer replyProd = jmsContRec.createProducer();
        TextMessage msg1 = jmsContRec.createTextMessage("testSetGetJMSReplyTo: Reply Msg");

        msg1.setJMSCorrelationID(recmsg.getJMSMessageID());
        replyProd.send(d1, msg1);
        JMSConsumer consumer = jmsContext.createConsumer(replyQ);
        TextMessage replymsg = (TextMessage) consumer.receive(30000);
        String replyID = replymsg.getJMSCorrelationID().toString();

        if (!(replyID.equals(msg.getJMSMessageID())))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testSetGetJMSReplyTo_TCP_SecOn failed");
        qr.close();
        consumer.close();
        jmsContext.close();

    }

    public void testCloseAll_B_SecOn(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsCont = null;
        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        jmsCont = cf1.createContext();
        try {
            jmsCont.close();

            jmsCont.start();

        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testCloseAll_B_SecOn failed ");
        jmsCont.close();

    }

    public void testCloseAll_TCP_SecOn(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsCont = null;

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        jmsCont = cf1.createContext();
        try {
            jmsCont.close();

            jmsCont.start();

        } catch (JMSRuntimeException ex) {

            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testCloseAll_TCP_SecOn failed ");
        jmsCont.close();

    }

    public void testCloseTempDest_B_SecOn(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsCont = null;

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        jmsCont = cf1.createContext();

        TemporaryQueue tempQ = jmsCont.createTemporaryQueue();

        try {
            jmsCont.close();
            jmsCont.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {

            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testCloseTempDest_B_SecOn failed ");
        jmsCont.close();

    }

    public void testCloseTempDest_TCP_SecOn(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsCont = null;
        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");
        jmsCont = cf1.createContext();
        TemporaryQueue tempQ = jmsCont.createTemporaryQueue();
        try {
            jmsCont.close();
            jmsCont.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testCloseTempDest_TCP_SecOn failed ");
        jmsCont.close();

    }

    public void testQueueConsumer_B_SecOn(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage msg = jmsContext.createTextMessage("Expected text - testCreateJmsProducerAndSend_B_SecOn");

        jmsProducer.send(queue2, msg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if (!(message.getText().equals("Expected text - testCreateJmsProducerAndSend_B_SecOn")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateJmsProducerAndSend_B_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testQueueConsumer_TCP_SecOn(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        TextMessage msg = jmsContext.createTextMessage("testCreateJmsProducerAndSend_TCP_SecOn");

        jmsProducer.send(queue2, msg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if (!(message.getText().equals("testCreateJmsProducerAndSend_TCP_SecOn")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateJmsProducerAndSend_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testCreateConsumerWithMsgSelectorTopic_B_SecOn(
                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic,
                                                            "Month = 'March'");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_B_SecOn");
        msg.setStringProperty("Month", "March");

        jmsProducer.send(topic, msg);
        TextMessage recmsg = (TextMessage) jmsConsumer.receive(10000);
        if (!recmsg.getStringProperty("Month").equals("March"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorTopic_B_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testCreateConsumerWithMsgSelectorTopic_TCP_SecOn(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContext = TCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic,
                                                            "Month = 'March'");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_TCP_SecOn");
        System.out.println("--testCreateConsumerWithMsgSelectorTopic_TCP_SecOn --msg1 --"
                           + msg);
        msg.setStringProperty("Month", "March");
        System.out.println("--testCreateConsumerWithMsgSelectorTopic_TCP_SecOn --msg2 --"
                           + msg);

        jmsProducer.send(topic, msg);
        TextMessage recmsg = (TextMessage) jmsConsumer.receive(10000);
        System.out.println("--testCreateConsumerWithMsgSelectorTopic_TCP_SecOn --msg3 --"
                           + recmsg);

        System.out.println("--testCreateConsumerWithMsgSelectorTopic_TCP_SecOn --msg4 --"
                           + recmsg.getStringProperty("Month").equals("March"));
        if (!recmsg.getStringProperty("Month").equals("March"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorTopic_TCP_SecOn failed ");
        jmsConsumer.close();
        jmsContext.close();
    }

    public int getMessageCount(QueueBrowser qb) throws JMSException {

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        return numMsgs;
    }

    public static JMSContext getContextFromQCFBindings() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        JMSContext jmsContext = cf1.createContext();

        return jmsContext;

    }

    public JMSContext getContextFromQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        JMSContext jmsContext = cf1.createContext();
        return jmsContext;

    }

    public static JMSContext getContextFromTCFBindings() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");

        JMSContext jmsContext = tcf1.createContext();

        return jmsContext;

    }

    public JMSContext getContextFromTCFTCP() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf1");

        JMSContext jmsContext = tcf1.createContext();
        return jmsContext;

    }

    public static QueueConnectionFactory getQCFBindings() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;

    }

    public Queue getQueue(String name) throws NamingException {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/"
                                                          + name);

        return queue;
    }

    public Topic getTopic(String name) throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/"
                                                          + name);

        return topic;
    }

    public static QueueConnectionFactory getQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        return cf1;

    }

    public TopicConnectionFactory getTCFBindings() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");

        return cf1;

    }

    public TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf1");

        return cf1;

    }

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {

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
            Message message = consumer.receive();
        }

        context.close();
    }

    private static class WrongException extends Exception {
        WrongException(String str) {
            super(str);
        }
    }

    private static class StockObject implements Serializable {
        final String stockName;
        final double stockValue;

        StockObject(String name, double value) {
            this.stockName = name;
            this.stockValue = value;
        }

        @Override
        public String toString() {
            // serialize the object
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream so = new ObjectOutputStream(bo);
                so.writeObject(this);
                so.flush();
                return bo.toString();
            } catch (Exception e) {
                System.out.println(e);
                return "";
            }
        }
    }

}
