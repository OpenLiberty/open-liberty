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
import java.util.Enumeration;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatRuntimeException;
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
public class JMSConsumerServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static TopicConnectionFactory jmsTCFBindings;
    public static TopicConnectionFactory jmsTCFTCP;
    public static Topic jmsTopic;
    public static Topic jmsTopic2;
    public static Topic jmsTopic3;
    public static Queue jmsQueue;
    // public static Queue jmsReplyQueue;
    public static JMSContext jmsContext;
    public static JMSConsumer jmsConsumer;
    public static JMSProducer jmsProducer;
    public static boolean exceptionFlag;

    private static int DEFAULT_TIMEOUT = 30000;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQCFBindings = getQCFBindings();
            jmsQCFTCP = getQCFTCP();
            jmsQueue = getQueue("jndi_INPUT_Q");
            // jmsReplyQueue = getQueue("MDBREPLYQ");
            jmsTCFBindings = getTCFBindings();
            jmsTCFTCP = getTCFTCP();
            jmsTopic = getTopic("eis/topic1");
            jmsTopic2 = getTopic("eis/topic2");
            jmsTopic3 = getTopic("eis/topic3");

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
        final TraceComponent tc = Tr.register(JMSConsumerServlet.class); // injection
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
            System.out.println(" Start: " + test);
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" End: " + test);
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

    public void testCloseConsumer_B(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            JMSContext jmsContext = jmsQCFBindings.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();
            jmsConsumer.receive();

        } catch (JMSRuntimeException ex) {

            exceptionFlag = true;

        }

        if (!exceptionFlag)
            throw new WrongException("testCloseConsumer_B failed: Expected exception was not seen");

        jmsContext.close();

    }

    public void testCloseConsumer_TCP(HttpServletRequest request,
                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            JMSContext jmsContext = jmsQCFTCP.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();
            jmsConsumer.receive();

        } catch (JMSRuntimeException ex) {

            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testCloseConsumer_B failed: Expected exception was not seen");
        jmsContext.close();

    }

    public void testReceive_B(HttpServletRequest request,
                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage m = jmsContext.createTextMessage("testReceive_B");
        jmsProducer.send(jmsQueue, m);

        TextMessage msg = (TextMessage) jmsConsumer.receive();

        if (!(msg.getText().equals("testReceive_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testReceive_B failed: Expected message was not received");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testReceive_TCP(HttpServletRequest request,
                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage m = jmsContext.createTextMessage("testReceive_TCP");
        jmsProducer.send(jmsQueue, m);

        TextMessage msg = (TextMessage) jmsConsumer.receive();

        if (!(msg.getText().equals("testReceive_TCP")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testReceive_TCP failed: Expected message was not received");
        jmsConsumer.close();
        jmsContext.close();
    }

    public void testReceiveBody_B(HttpServletRequest request,
                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveBody_B");
        String msg1 = jmsConsumer.receiveBody(String.class);
        if (!(msg1.contains("testReceiveBody_B")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBody_B failed: Expected message was not received");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testReceiveBody_TCP(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsContext.createProducer().send(jmsQueue, "testReceiveBody_TCP");
        String msg1 = jmsConsumer.receiveBody(String.class);
        if (!(msg1.contains("testReceiveBody_TCP")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBody_TCP failed: Expected message was not received");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testReceiveBodyTimeOut_B(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveBodyTimeOut_B");
        String msg1 = jmsConsumer.receiveBody(String.class, DEFAULT_TIMEOUT);
        if (!(msg1.contains("testReceiveBodyTimeOut_B")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyTimeOut_B failed: Expected message was not received");
        jmsConsumer.close();
        jmsContext.close();

    }

    public void testReceiveBodyTimeOut_TCP(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveBodyTimeOut_TCP");
        String msg1 = jmsConsumer.receiveBody(String.class, DEFAULT_TIMEOUT);
        if (!(msg1.contains("testReceiveBodyTimeOut_TCP")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyTimeOut_TCP failed: Expected message was not received");

    }

    public void testReceiveBodyNoWait_B(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext(JMSContext.SESSION_TRANSACTED);
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveBodyNoWait_B");
        jmsContext.commit();
        String msg1 = jmsConsumer.receiveBodyNoWait(String.class);
        if (!(msg1.contains("testReceiveBodyNoWait_B")))
            exceptionFlag = true;
        jmsContext.commit();
        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyNoWait_B failed: Expected message was not received");

    }

    public void testReceiveBodyNoWait_TCP(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext(JMSContext.SESSION_TRANSACTED);
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveBodyNoWait_TCP");
        jmsContext.commit();
        String msg1 = jmsConsumer.receiveBodyNoWait(String.class);
        if (!(msg1.contains("testReceiveBodyNoWait_TCP")))
            exceptionFlag = true;
        jmsContext.commit();
        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyNoWait_TCP failed: Expected message was not received");

    }

    public void testReceiveWithTimeOut_B_SecOn(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveWithTimeOut_B_SecOn");
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg.getText().equals("testReceiveWithTimeOut_B_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testReceiveWithTimeOut_B_SecOn failed: Expected message was not received on time");

    }

    public void testReceiveWithTimeOut_TcpIp_SecOn(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveWithTimeOut_TcpIp_SecOn");
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg.getText().equals("testReceiveWithTimeOut_TcpIp_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testReceiveWithTimeOut_TcpIp_SecOn failed: Expected message was not received on time");

    }

    public void testReceiveNoWait_B_SecOn(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext(JMSContext.SESSION_TRANSACTED);
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveNoWait_B_SecOn");
        jmsContext.commit();
        TextMessage msg = (TextMessage) jmsConsumer.receiveNoWait();
        if (!(msg.getText().equals("testReceiveNoWait_B_SecOn")))
            exceptionFlag = true;

        jmsContext.commit();
        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testReceiveNoWait_B_SecOn failed: Expected message was not received");
    }

    public void testReceiveNoWait_TcpIp_SecOn(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext(JMSContext.SESSION_TRANSACTED);
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testReceiveNoWait_TcpIp_SecOn");
        jmsContext.commit();
        TextMessage msg = (TextMessage) jmsConsumer.receiveNoWait();
        if (!(msg.getText().equals("testReceiveNoWait_TcpIp_SecOn")))
            exceptionFlag = true;

        jmsContext.commit();
        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testReceiveNoWait_TcpIp_SecOn failed: Expected message was not received");

    }

    public void testReceiveBodyEmptyBody_B_SecOn(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);
        try {
            String msgBody = jmsConsumer.receiveBody(String.class);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyEmptyBody_B_SecOn failed: Expected message was not received");
    }

    public void testReceiveBodyEmptyBody_TcpIp_SecOn(
                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);
        try {
            String msgBody = jmsConsumer.receiveBody(String.class);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyEmptyBody_TcpIp_SecOn failed: Expected message was not received");
    }

    public void testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn(
                                                                  HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn");
        jmsProducer.send(jmsQueue, m);

        try {
            byte[] msgBody = jmsConsumer.receiveBody(byte[].class,
                                                     DEFAULT_TIMEOUT);
        } catch (MessageFormatRuntimeException e) {
            System.out.println("Expected MessageFormatRuntimeException was found in testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn");
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOn failed: Expected exception was not seen");
    }

    public void testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn");
        jmsProducer.send(jmsQueue, m);
        try {
            byte[] msgBody = jmsConsumer.receiveBody(byte[].class,
                                                     DEFAULT_TIMEOUT);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOn failed: Expected exception not seen");
    }

    public void testReceiveBodyNoWaitUnsupportedType_B_SecOn(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext(JMSContext.SESSION_TRANSACTED);
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        MapMessage m = jmsContext.createMapMessage();
        jmsProducer.send(jmsQueue, m);
        jmsContext.commit();
        try {
            String msgBody = jmsConsumer.receiveBodyNoWait(String.class);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;
        }

        jmsContext.commit();
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyNoWaitUnsupportedType_B_SecOn failed: Expected exception not seen");
    }

    public void testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext(JMSContext.SESSION_TRANSACTED);
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        MapMessage m = jmsContext.createMapMessage();
        jmsProducer.send(jmsQueue, m);
        jmsContext.commit();
        try {
            String msgBody = jmsConsumer.receiveBodyNoWait(String.class);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;
        }

        jmsContext.commit();
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOn failed: Expected exception not seen");
    }

    public void testReceiveTopic_B(HttpServletRequest request,
                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testReceiveTopic_B");
        jmsProducer.send(jmsTopic, msg);
        TextMessage msg1 = (TextMessage) jmsConsumer.receive();
        if (!(msg1.getText().equals("testReceiveTopic_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testReceiveTopic_B failed: Expected message was not received");
    }

    public void testReceiveTopic_TCP(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testReceiveTopic_TCP");
        jmsProducer.send(jmsTopic, msg);
        TextMessage msg1 = (TextMessage) jmsConsumer.receive();
        if (!(msg1.getText().equals("testReceiveTopic_TCP")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testReceiveTopic_TCP failed: Expected message was not received");

    }

    public void testReceiveBodyTopic_B(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveBodyTopic_B");
        String msg1 = jmsConsumer.receiveBody(String.class);
        if (!msg1.contains("testReceiveBodyTopic_B"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyTopic_B failed: Expected message was not received");

    }

    public void testReceiveBodyTopic_TCP(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveBodyTopic_TCP");
        String msg1 = jmsConsumer.receiveBody(String.class);
        if (!msg1.contains("testReceiveBodyTopic_TCP"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyTopic_TCP failed: Expected message was not received");

    }

    public void testReceiveBodyTimeOutTopic_B(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveBodyTimeOutTopic_B");
        String msg1 = jmsConsumer.receiveBody(String.class, DEFAULT_TIMEOUT);
        if (!msg1.contains("testReceiveBodyTimeOutTopic_B"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyTimeOutTopic_B failed: Expected message was not received");

    }

    public void testReceiveBodyTimeOutTopic_TCP(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveBodyTimeOutTopic_TCP");
        String msg1 = jmsConsumer.receiveBody(String.class, DEFAULT_TIMEOUT);
        if (!msg1.contains("testReceiveBodyTimeOutTopic_TCP"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveBodyTimeOutTopic_TCP failed: Expected message was not received");

    }

    public void testReceiveBodyNoWaitTopic_B(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext(JMSContext.SESSION_TRANSACTED);
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveBodyNoWaitTopic_B");
        jmsContext.commit();
        String msg1 = jmsConsumer.receiveBodyNoWait(String.class);
        if (!msg1.contains("testReceiveBodyNoWaitTopic_B"))
            exceptionFlag = true;

        jmsContext.commit();
        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testReceiveBodyNoWaitTopic_B failed: Expected message was not received");

    }

    public void testReceiveBodyNoWaitTopic_TCP(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext(JMSContext.SESSION_TRANSACTED);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveBodyNoWaitTopic_TCP");
        jmsContext.commit();

        String message = null;
        int sleepTime = 2000; // 2 seconds
        do {
            message = jmsConsumer.receiveBodyNoWait(String.class);
            Thread.sleep(20); // sleeping 20 milli seconds

        } while ((sleepTime -= 20) > 0 && message == null);

        if (!(message.contains("testReceiveBodyNoWaitTopic_TCP")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContext.commit();
        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testReceiveBodyNoWaitTopic_TCP failed: Expected message was not received");

    }

    public void testReceiveWithTimeOutTopic_B_SecOn(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveWithTimeOutTopic_B_SecOn");
        TextMessage msg1 = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg1.getText().equals("testReceiveWithTimeOutTopic_B_SecOn")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveWithTimeOutTopic_B_SecOn failed: Expected message was not received");

    }

    public void testReceiveWithTimeOutTopic_TcpIp_SecOn(
                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveWithTimeOutTopic_TcpIp_SecOn");
        TextMessage msg1 = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg1.getText().equals("testReceiveWithTimeOutTopic_TcpIp_SecOn")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testReceiveWithTimeOutTopic_TcpIp_SecOn failed: Expected message was not received");

    }

    public void testReceiveNoWaitTopic_B_SecOn(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext(JMSContext.SESSION_TRANSACTED);
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveNoWaitTopic_B_SecOn");
        jmsContext.commit();
        TextMessage msg1 = (TextMessage) jmsConsumer.receiveNoWait();
        if (!(msg1.getText().equals("testReceiveNoWaitTopic_B_SecOn")))
            exceptionFlag = true;

        jmsContext.commit();
        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testReceiveNoWaitTopic_B_SecOn failed: Expected message was not received");

    }

    public void testReceiveNoWaitTopic_TcpIp_SecOn(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext(JMSContext.SESSION_TRANSACTED);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsTopic, "testReceiveNoWaitTopic_TcpIp_SecOn");
        jmsContext.commit();

        TextMessage message = null;
        int sleepTime = 2000; // 2 seconds
        do {
            message = (TextMessage) jmsConsumer.receiveNoWait();
            Thread.sleep(20); // sleeping 20 milli seconds

        } while ((sleepTime -= 20) > 0 && message == null);

        if (!(message.getText().equals("testReceiveNoWaitTopic_TcpIp_SecOn")))
            exceptionFlag = true;

        jmsContext.commit();
        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testReceiveNoWaitTopic_TcpIp_SecOn failed: Expected message was not received");

    }

    public void testReceiveBodyEmptyBodyTopic_B_SecOn(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsTopic, m);
        try {
            String msgBody = jmsConsumer.receiveBody(String.class);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyEmptyBodyTopic_B_SecOn failed: Expected exception not seen");

    }

    public void testReceiveBodyEmptyBodyTopic_TcpIp_SecOn(
                                                          HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsTopic, m);
        try {
            String msgBody = jmsConsumer.receiveBody(String.class);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyEmptyBodyTopic_TcpIp_SecOn failed: Expected exception not seen");

    }

    public void testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn(
                                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn");
        jmsProducer.send(jmsTopic, m);
        try {
            byte[] msgBody = jmsConsumer.receiveBody(byte[].class,
                                                     DEFAULT_TIMEOUT);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_B_SecOn failed: Expected exception not seen");
    }

    public void testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn(
                                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn");
        jmsProducer.send(jmsTopic, m);

        try {
            byte[] msgBody = jmsConsumer.receiveBody(byte[].class,
                                                     DEFAULT_TIMEOUT);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;

        }
        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyWithTimeOutUnspecifiedTypeTopic_TcpIp_SecOn failed: Expected exception not seen");
    }

    public void testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn(
                                                                  HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext context = jmsTCFBindings.createContext(JMSContext.SESSION_TRANSACTED);
        JMSConsumer consumer = context.createConsumer(jmsTopic);
        JMSProducer producer = context.createProducer();
        MapMessage m = context.createMapMessage();
        producer.send(jmsTopic, m);
        context.commit();
        try {
            String msgBody = consumer.receiveBodyNoWait(String.class);
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;
        }

        context.commit();
        context.close();

        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyNoWaitUnsupportedTypeTopic_B_SecOn failed: Expected exception not seen");
    }

    public void testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext context = jmsTCFTCP.createContext(JMSContext.SESSION_TRANSACTED);
        System.out.println("DEBUG: context:" + context);
        JMSConsumer consumer = context.createConsumer(jmsTopic3);
        System.out.println("DEBUG: consumer:" + consumer);
        JMSProducer producer = context.createProducer();
        System.out.println("DEBUG: producer:" + producer);
        MapMessage m = context.createMapMessage();
        System.out.println("DEBUG: m:" + m);
        producer.send(jmsTopic3, m);
        System.out.println("DEBUG: sent to:" + jmsTopic3);
        context.commit();
        System.out.println("DEBUG: committed");
        try {
            String msgBody = consumer.receiveBodyNoWait(String.class);
            if (null == msgBody) {
                System.out.println("testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn: There was no message.");
            } else {
                System.out.println("testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn: No exception, body: " + msgBody);
            }
            // roll back so we can receive it again, if it was received the first time
            context.rollback();
            Message msg = consumer.receiveNoWait();
            if (null == msg) {
                System.out.println("DEBUG: Second receive returns null message.");
            } else {
                System.out.println("DEBUG: JMSType:" + msg.getJMSType());
                System.out.println("DEBUG: isBodyAssignableTo(String.class):" + msg.isBodyAssignableTo(String.class));
                System.out.println("DEBUG: msg:" + msg);
            }
        } catch (MessageFormatRuntimeException e) {
            exceptionFlag = true;
        }

        context.commit();
        context.close();

        if (!exceptionFlag)
            throw new WrongException("testReceiveBodyNoWaitUnsupportedTypeTopic_TcpIp_SecOn failed: Expected exception not seen");
    }

    public void testRDC_B(HttpServletRequest request,
                          HttpServletResponse response) throws Throwable {
        jmsContext = jmsQCFBindings.createContext();
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testRDC_B");
        jmsProducer.send(jmsQueue, msg);
        Thread.sleep(1000);

    }

    public void testRDC_TcpIp(HttpServletRequest request,
                              HttpServletResponse response) throws Throwable {
        jmsContext = jmsQCFTCP.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testRDC_TcpIp");
        jmsContext.createProducer().send(jmsQueue, msg);
        Thread.sleep(1000);

    }

    public void testCreateSharedDurableConsumer_create(
                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID1");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateSharedDurableConsumer_create");
        jmsProducer.send(jmsTopic, msg);

        jmsContext.close();
    }

    public void testCreateSharedDurableConsumer_consume(
                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID1");
        TextMessage msg = (TextMessage) jmsDurConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg != null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_consume failed");

        jmsDurConsumer.close();
        jmsContext.unsubscribe("SUBID1");
        jmsContext.close();

    }

    public void testCreateSharedDurableConsumer_create_TCP(
                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID2");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateSharedDurableConsumer_create_TCP");

        jmsProducer.send(jmsTopic, msg);

        jmsContext.close();

    }

    public void testCreateSharedDurableConsumer_consume_TCP(
                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID2");
        TextMessage msg = (TextMessage) jmsDurConsumer.receive(DEFAULT_TIMEOUT);

        if (!(msg != null)
            && msg.getText().equals(
                                    "testCreateSharedDurableConsumer_create_TCP"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_create_TCP failed: Expected message was not received");

        jmsDurConsumer.close();
        jmsContext.unsubscribe("SUBID2");
        jmsContext.close();

    }

    public void testCreateSharedDurableConsumerWithMsgSel_create(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID3", "Company = 'IBM'");
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateSharedDurableConsumerWithMsgSel_create");
        msg.setStringProperty("Company", "IBM");

        jmsProducer.send(jmsTopic, msg);

    }

    public void testCreateSharedDurableConsumerWithMsgSel_consume(
                                                                  HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID3", "Company = 'IBM'");
        TextMessage msg = (TextMessage) jmsDurConsumer.receive(DEFAULT_TIMEOUT);
        if (!msg.getText().equals(
                                  "testCreateSharedDurableConsumerWithMsgSel_create"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSel_create failed: Expected message was not received");

    }

    public void testCreateSharedDurableConsumerWithMsgSel_create_TCP(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID4", "Company = 'IBM'");
        jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateSharedDurableConsumerWithMsgSel_create_TCP");
        msg.setStringProperty("Company", "IBM");

        jmsProducer.send(jmsTopic, msg);

    }

    public void testCreateSharedDurableConsumerWithMsgSel_consume_TCP(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsDurConsumer = jmsContext.createSharedDurableConsumer(
                                                                            jmsTopic, "SUBID4", "Company = 'IBM'");
        TextMessage msg = (TextMessage) jmsDurConsumer.receive(DEFAULT_TIMEOUT);
        if (!msg.getText().equals(
                                  "testCreateSharedDurableConsumerWithMsgSel_create_TCP"))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSel_create_TCP failed: Expected message was not received");

    }

    public void testCreateSharedNonDurableConsumer_create(
                                                          HttpServletRequest request, HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID5");
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumer_create");

        jmsProducer.send(jmsTopic, msg);
        jmsContext.close();

    }

    public void testCreateSharedNonDurableConsumer_consume(
                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID5");
        TextMessage msg = (TextMessage) jmsNonDurConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_create failed: Expected message was not received");
        jmsContext.close();

    }

    public void testCreateSharedNonDurableConsumer_create_TCP(
                                                              HttpServletRequest request, HttpServletResponse response) throws Throwable {
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID6");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumer_create_TCP");
        jmsProducer.send(jmsTopic, msg);

        jmsContext.close();

    }

    public void testCreateSharedNonDurableConsumer_consume_TCP(
                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID6");

        TextMessage msg = (TextMessage) jmsNonDurConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_create_TCP failed: Expected message was not received");

        jmsNonDurConsumer.close();

        jmsContext.close();

    }

    public void testCreateSharedNonDurableConsumerWithMsgSel_create(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID7", "Team = 'WAS'");
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSel_create");
        msg.setStringProperty("Team", "WAS");

        jmsProducer.send(jmsTopic, msg);

        jmsContext.close();
    }

    public void testCreateSharedNonDurableConsumerWithMsgSel_consume(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID7", "Team = 'WAS'");
        TextMessage msg = (TextMessage) jmsNonDurConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSel_create failed: Expected message was not received");
        jmsContext.close();

    }

    public void testCreateSharedNonDurableConsumerWithMsgSel_create_TCP(
                                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID8", "Team = 'WAS'");
        jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSel_create_TCP");
        msg.setStringProperty("Team", "WAS");
        jmsProducer.send(jmsTopic, msg);

    }

    public void testCreateSharedNonDurableConsumerWithMsgSel_consume_TCP(
                                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsNonDurConsumer = jmsContext.createSharedConsumer(
                                                                        jmsTopic, "SUBID8", "Team = 'WAS'");
        TextMessage msg = (TextMessage) jmsNonDurConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSel_create failed: Expected message was not received");

    }

    public void testBasicMDBTopic(HttpServletRequest request,
                                  HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFBindings.createContext();
        jmsProducer = jmsContext.createProducer();
        int msgs = 3;

        for (int i = 0; i < msgs; i++) {
            jmsProducer.send(jmsTopic, "testBasicMDBTopic:" + i);
        }
    }

    public void testBasicMDBTopic_TCP(HttpServletRequest request,
                                      HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFTCP.createContext();
        jmsProducer = jmsContext.createProducer();
        int msgs = 3;

        for (int i = 0; i < msgs; i++) {
            jmsProducer.send(jmsTopic, "testBasicMDBTopic_TCP:" + i);
        }
    }

    public void testBasicMDBTopicDurShared(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createConsumer(jmsTopic2);
        jmsProducer = jmsContext.createProducer();
        int msgs = 3;

        for (int i = 0; i < msgs; i++) {
            jmsProducer.send(jmsTopic2, "testBasicMDBTopic:" + i);
        }
    }

    public void testBasicMDBTopicDurShared_TCP(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        jmsContext = jmsTCFTCP.createContext();
        jmsProducer = jmsContext.createProducer();
        int msgs = 3;

        for (int i = 0; i < msgs; i++) {
            jmsProducer.send(jmsTopic2, "testBasicMDBTopic_TCP:" + i);
        }
    }

    public void testSetMessageProperty_Bindings_SecOn(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer.setDisableMessageID(true);
        jmsProducer.send(jmsQueue, "testSetMessageProperty_Bindings_SecOn");

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (!(msg.getJMSMessageID() == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testSetMessageProperty_Bindings_SecOn failed: Expected message was not received");

    }

    public void testSetMessageProperty_TCP_SecOn(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer.setDisableMessageID(true);
        jmsProducer.send(jmsQueue, "testSetMessageProperty_TCp_SecOn");
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (!(msg.getJMSMessageID() == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testSetMessageProperty_TCp_SecOn failed: Expected message was not received");

    }

    public void testTopicName_temp_B(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsProducer = jmsContext.createProducer();

        Topic topic = jmsContext.createTopic("_tempTopic");
        TextMessage m1 = jmsContext.createTextMessage("testTopicName_temp_B");
        jmsConsumer = jmsContext.createConsumer(topic);
        jmsProducer.send(topic, m1);
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg.getText().equals("testTopicName_temp_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicName_temp_B failed: Expected message was not received");

    }

    public void testTopicName_temp_TCP(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        Topic topic = jmsContext.createTopic("_tempTopic");
        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage message = jmsContext.createTextMessage("testTopicName_temp_TCP");
        jmsProducer.send(topic, message);
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(msg.getText().equals("testTopicName_temp_TCP")))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testTopicName_temp_TCP failed: Expected message was not received");

        jmsConsumer.close();
        jmsContext.close();
    }

    public void testQueueNameCaseSensitive_Bindings(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsProducer = jmsContext.createProducer();
        try {
            Queue queue = jmsContext.createQueue("queue1");
            jmsProducer.send(queue, "testQueueNameCaseSensitive_Bindings");
        } catch (InvalidDestinationRuntimeException e) {
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameCaseSensitive_Bindings failed: expected exception was not seen");
    }

    public void testQueueNameCaseSensitive_TCP(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsProducer = jmsContext.createProducer();
        try {
            Queue queue = jmsContext.createQueue("queue1");
            jmsProducer.send(queue, "testQueueNameCaseSensitive_TCP");
        } catch (InvalidDestinationRuntimeException e) {
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameCaseSensitive_TCP failed: expected exception was not seen");

    }

    public static QueueConnectionFactory getQCFBindings() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;

    }

    public QueueConnectionFactory getQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        return cf1;

    }

    public Queue getQueue(String name) throws NamingException {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/"
                                                          + name);

        return queue;
    }

    public TopicConnectionFactory getTCFBindings() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");

        return cf1;

    }

    public TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf1");

        return cf1;

    }

    public Topic getTopic(String name) throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/"
                                                          + name);

        return topic;
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

    public class WrongException extends Exception {

        String str;

        public WrongException(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return "This is not the expected exception" + " " + str;
        }

    }

    public class StockObject implements Serializable {
        String stockName;
        double stockValue;

        public void setName(String stocknam) {
            stockName = stocknam;

        }

        public void setValue(double stockVal) {
            stockValue = stockVal;
        }

        public String toString(String stockn, double stockv) {
            String serializedObject = "";
            StockObject sobj = new StockObject();
            sobj.setName(stockn);
            sobj.setValue(stockv);

            // serialize the object
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream so = new ObjectOutputStream(bo);
                so.writeObject(sobj);
                so.flush();
                serializedObject = bo.toString();
            } catch (Exception e) {
                System.out.println(e);

            }
            return serializedObject;

        }
    }

}
