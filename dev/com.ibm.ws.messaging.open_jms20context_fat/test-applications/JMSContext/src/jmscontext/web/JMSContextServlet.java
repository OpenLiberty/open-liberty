/*******************************************************************************
v * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmscontext.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.jms.BytesMessage;
import javax.jms.ConnectionMetaData;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.InvalidSelectorRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.StreamMessage;
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

    public QueueConnectionFactory jmsQCFBindings;
    public QueueConnectionFactory jmsQCFTCP;
    public Queue queue;
    public Queue queue1;
    public Queue queue2;
    public Queue queue3;

    public TopicConnectionFactory jmsTCFBindings;
    public TopicConnectionFactory jmsTCFTCP;
    public Topic topic;

    public QueueConnectionFactory getQCF(String name) {
        QueueConnectionFactory qcf;
        try {
            qcf = (QueueConnectionFactory) new InitialContext().lookup(name);
        } catch (NamingException e) {
            e.printStackTrace();
            qcf = null;
        }
        System.out.println("Queue connection factory '" + name + "' [ " + qcf + " ]");
        return qcf;
    }

    public Queue getQueue(String name) {
        Queue queue;
        try {
            queue = (Queue) new InitialContext().lookup(name);
        } catch (NamingException e) {
            e.printStackTrace();
            queue = null;
        }
        System.out.println("Queue '" + name + "' [ " + queue + " ]");
        return queue;
    }

    public static TopicConnectionFactory getTCF(String name) {
        TopicConnectionFactory tcf;
        try {
            tcf = (TopicConnectionFactory) new InitialContext().lookup(name);
        } catch (NamingException e) {
            e.printStackTrace();
            tcf = null;
        }
        System.out.println("Topic connection factory '" + name + "' [ " + tcf + " ]");
        return tcf;
    }

    public Topic getTopic(String name) {
        Topic topic;
        try {
            topic = (Topic) new InitialContext().lookup(name);
        } catch (NamingException e) {
            e.printStackTrace();
            topic = null;
        }
        System.out.println("Topic '" + name + "' [ " + topic + " ]");
        return topic;
    }

    public int getMessageCount(QueueBrowser qb) throws JMSException {
        int numMsgs = 0;
        Enumeration e = qb.getEnumeration();
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }
        return numMsgs;
    }

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext jmsContext = qcf.createContext();

        try {
            QueueBrowser qb = jmsContext.createBrowser(q);
            Enumeration e = qb.getEnumeration();

            JMSConsumer jmsConsumer = jmsContext.createConsumer(q);

            try {
                int numMsgs = 0;
                while (e.hasMoreElements()) {
                    e.nextElement();
                    numMsgs++;
                }

                for (int msgNo = 0; msgNo < numMsgs; msgNo++) {
                    jmsConsumer.receive();
                }

            } finally {
                jmsConsumer.close();
            }

        } finally {
            jmsContext.close();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();

        jmsQCFBindings = getQCF("java:comp/env/jndi_JMS_BASE_QCF");
        jmsQCFTCP = getQCF("java:comp/env/jndi_JMS_BASE_QCF1");
        queue = getQueue("java:comp/env/jndi_INPUT_Q");
        queue1 = getQueue("java:comp/env/eis/queue1");
        queue2 = getQueue("java:comp/env/eis/queue2");
        queue3 = getQueue("java:comp/env/eis/queue3");

        jmsTCFBindings = getTCF("java:comp/env/eis/tcf");
        jmsTCFTCP = getTCF("java:comp/env/eis/tcf1");
        topic = getTopic("java:comp/env/eis/topic1");

        if (jmsQCFBindings == null) {
            throw new ServletException("Null 'jmsQCFBindings'");
        }
        if (jmsQCFTCP == null) {
            throw new ServletException("Null 'jmsQCFTCP'");
        }
        if (queue == null) {
            throw new ServletException("Null 'queue'");
        }
        if (queue1 == null) {
            throw new ServletException("Null 'queue1'");
        }
        if (queue2 == null) {
            throw new ServletException("Null 'queue2'");
        }
        if (queue3 == null) {
            throw new ServletException("Null 'queue3'");
        }

        if (jmsTCFBindings == null) {
            throw new ServletException("Null 'jmsTCFBindings'");
        }
        if (jmsTCFTCP == null) {
            throw new ServletException("Null 'jmsTCFTCP'");
        }
        if (topic == null) {
            throw new ServletException("Null 'topic'");
        }
    }

    /**
     * Handle a GET request to this servlet: Invoke the test method specified as
     * request paramater "test".
     *
     * The test method throws an exception when it fails. If no exception
     * is thrown by the test method, indicate success through the response
     * output. If an exception is thrown, omit the success indication.
     * Instead, display an error indication and display the exception stack
     * to the response output.
     *
     * @param request  The HTTP request which is being processed.
     * @param response The HTTP response which is being processed.
     *
     * @throws ServletException Thrown in case of a servlet processing error.
     * @throws IOException      Thrown in case of an input/output error.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String test = request.getParameter("test");

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(JMSContextServlet.class);

        Tr.entry(this, tc, test);
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);

            System.out.println(" Starting : " + test);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" Ending : " + test);
            Tr.exit(this, tc, test);

        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
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

    public void testCreateContext_B_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        if ((jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) ||
            !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateContext_B_SecOff failed");
        }
    }

    public void testCreateContext_TCP_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        if ((jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) ||
            !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateContext_TCP_SecOff failed");
        }
    }

    private boolean verifyMetadata(ConnectionMetaData metadata) throws JMSException {
        String targetJmsVersion = (isJakartaMessaging30()) ? "3.0" : "2.0";
        int targetJmsMajorVersion = (isJakartaMessaging30()) ? 3 : 2;
        return (metadata.getJMSVersion().equals(targetJmsVersion) &&
                (metadata.getJMSMajorVersion() == targetJmsMajorVersion) &&
                (metadata.getJMSMinorVersion() == 0) &&
                metadata.getJMSProviderName().equals("IBM") &&
                metadata.getProviderVersion().equals("1.0") &&
                (metadata.getProviderMajorVersion() == 1) &&
                (metadata.getProviderMinorVersion() == 0));
    }

    private static boolean isJakartaMessaging30() {
        Class clazz = null;
        try {
            clazz = Class.forName("jakarta.jms.JMSContext");
        } catch (Throwable t) {
            // Expect CNFE, but could be linkage error.
        }
        return clazz != null;
    }

    public void testGetMetadata_B_SecOff(
                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        if (!verifyMetadata(jmsContext.getMetaData())) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMetadata_B_SecOff failed");
        }
    }

    public void testGetMetadata_TCP_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        if (!verifyMetadata(jmsContext.getMetaData())) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMetadata_TCP_SecOff failed");
        }
    }

    public void testSetGetAutoStart_B_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        jmsContext.setAutoStart(false);

        boolean testFailed = false;
        if (jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testSetGetAutoStart_B_SecOff failed");
        }
    }

    public void testSetGetAutoStart_TCP_SecOff(
                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        jmsContext.setAutoStart(false);

        boolean testFailed = false;
        if (jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testSetGetAutoStart_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithUser_B_SecOff(
                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = jmsQCFBindings.createContext(userName, password);

        boolean testFailed = false;
        if ((jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) || !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUser_B_SecOff failed");
        }
    }

    public void testcreateContextwithUser_empty_B_SecOff(
                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "";
        String password = "";
        JMSContext jmsContext = jmsQCFBindings.createContext(userName, password);

        boolean testFailed = false;
        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUser_empty_B_SecOff failed");
        }
    }

    public void testcreateContextwithUserSessionMode_empty_B_SecOff(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "";
        String password = "";
        JMSContext jmsContext = jmsQCFBindings.createContext(userName, password);

        boolean testFailed = false;
        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUserSessionMode_empty_B_SecOff failed");
        }
    }

    public void testcreateContextwithUser_null_B_SecOff(
                                                        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = null;
        String password = null;
        JMSContext jmsContext = jmsQCFBindings.createContext(userName, password);

        boolean testFailed = false;
        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUser_null_B_SecOff failed");
        }
    }

    public void testcreateContextwithUserSessionMode_null_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = null;
        String password = null;
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsQCFBindings.createContext(userName, password, smode);

        boolean testFailed = false;
        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUserSessionMode_null_B_SecOff failed");
        }
    }

    public void testcreateContextwithUser_empty_TCP_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        String userName = "";
        String password = "";
        JMSContext jmsContext = jmsQCFTCP.createContext(userName, password);

        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUser_empty_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithUserSessionMode_empty_TCP_SecOff(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        String userName = "";
        String password = "";
        JMSContext jmsContext = jmsQCFTCP.createContext(userName, password);

        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUserSessionMode_empty_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithUser_null_TCP_SecOff(
                                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = null;
        String password = null;
        JMSContext jmsContext = jmsQCFTCP.createContext(userName, password);

        boolean testFailed = false;
        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUser_null_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithUserSessionMode_null_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        String userName = null;
        String password = null;
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsQCFTCP.createContext(userName, password, smode);

        if (!jmsContext.getAutoStart() ||
            (jmsContext.getSessionMode() != jmsContext.AUTO_ACKNOWLEDGE)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUserSessionMode_null_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithUser_TCP_SecOff(
                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = jmsQCFTCP.createContext(userName, password);

        boolean testFailed = false;
        if ((jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) ||
            !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUser_B_SecOff failed");
        }
    }

    public void testcreateContextwithsessionMode_B_SecOff(
                                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        int smode = JMSContext.DUPS_OK_ACKNOWLEDGE;
        JMSContext jmsContext = jmsQCFBindings.createContext(smode);

        boolean testFailed = false;
        if ((jmsContext.getSessionMode() != smode) || !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithsessionMode_B_SecOff failed");
        }
    }

    public void testcreateContextwithsessionMode_TCP_SecOff(
                                                            HttpServletRequest request, HttpServletResponse response) throws Exception {

        int smode = JMSContext.DUPS_OK_ACKNOWLEDGE;
        JMSContext jmsContext = jmsQCFTCP.createContext(smode);

        boolean testFailed = false;
        if ((jmsContext.getSessionMode() != smode) || !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithsessionMode_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithInvalidsessionMode_B_SecOff(HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = null;
        try {
            // Using CLIENT_ACKNOWLEDGE should result in a non-transacted, auto-acknowledged
            // session being used and no exception.
            int smode = JMSContext.CLIENT_ACKNOWLEDGE;
            jmsContext = jmsQCFBindings.createContext(smode);

        } catch (JMSRuntimeException ex) {
            Exception exception = new Exception("testcreateContextwithInvalidsessionMode_B_SecOff failed: JMSRuntimeException was thrown.", ex);
            throw exception;

        } finally {
            if (jmsContext != null)
                jmsContext.close();
        }
    }

    public void testcreateContextwithNegsessionMode_B_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        int smode0 = -1;
        int smode1 = 10000;

        boolean testFailed = false;

        try {
            jmsQCFBindings.createContext(smode0);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // expected
        }

        try {
            jmsQCFBindings.createContext(smode1);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // expected
        }

        if (testFailed) {
            throw new Exception("testcreateContextwithNegsessionMode_B_SecOff failed");
        }
    }

    public void testcreateContextwithInvalidsessionMode_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {
        JMSContext jmsContext = null;
        try {
            // Using CLIENT_ACKNOWLEDGE should result in a non-transacted, auto-acknowledged
            // session being used and no exception.
            int smode = JMSContext.CLIENT_ACKNOWLEDGE;
            jmsContext = jmsQCFTCP.createContext(smode);

        } catch (JMSRuntimeException ex) {
            throw new Exception("testcreateContextwithInvalidsessionMode_TCP_SecOff failed: JMSRuntime exception was thrown.", ex);

        } finally {
            if (jmsContext != null)
                jmsContext.close();
        }

    }

    public void testcreateContextwithNegsessionMode_TCP_SecOff(
                                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        int smode0 = -1;
        int smode1 = 10000;

        boolean testFailed = false;

        try {
            jmsQCFTCP.createContext(smode0);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // Expected
        }

        try {
            jmsQCFTCP.createContext(smode1);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // Expected
        }

        if (testFailed) {
            throw new Exception("testcreateContextwithNegsessionMode_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithUserNegsessionMode_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        int smode0 = -1;
        int smode1 = 10000;

        boolean testFailed = false;

        try {
            jmsQCFBindings.createContext(userName, password, smode0);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // expected
        }

        try {
            jmsTCFBindings.createContext(userName, password, smode1);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // expected
        }

        if (testFailed) {
            throw new Exception("testcreateContextwithUserNegsessionMode_B_SecOff failed");
        }
    }

    public void testcreateContextwithUserNegsessionMode_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        int smode0 = -1;
        int smode1 = 10000;
        String userName = "user1";
        String password = "user1pwd";

        boolean testFailed = false;

        try {
            jmsQCFTCP.createContext(userName, password, smode0);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // expected
        }

        try {
            jmsTCFTCP.createContext(userName, password, smode1);
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            // expected
        }

        if (testFailed) {
            throw new Exception("testcreateContextwithUserNegsessionMode_TCP_SecOff failed");
        }
    }

    public void testcreateContextwithUserSessionMode_B_SecOff(
                                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsQCFBindings.createContext(userName, password, smode);

        boolean testFailed = false;
        if ((jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) ||
            !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUserSessionMode_B_SecOff failed");
        }
    }

    public void testcreateContextwithUserSessionMode_TCP_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsQCFTCP.createContext(userName, password, smode);

        boolean testFailed = false;
        if ((jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) ||
            !jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateContextwithUserSessionMode_TCP_SecOff failed");
        }
    }

    public void testautoStart_B_SecOff(
                                       HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        if (!jmsContext.getAutoStart()) {
            testFailed = true;
        }
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testautoStart_B_SecOff failed");
        }
    }

    public void testSetGetAutoStart_createContextwithUser_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = jmsQCFBindings.createContext(userName, password);

        boolean testFailed = false;

        jmsContext.setAutoStart(false);
        if (jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.setAutoStart(true);
        if (!jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testSetGetAutoStart_createContextwithUser_B_SecOff failed");
        }
    }

    public void testSetGetAutoStart_createContextwithUser_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContext = jmsQCFTCP.createContext(userName, password);

        boolean testFailed = false;

        jmsContext.setAutoStart(false);
        if (jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.setAutoStart(true);
        if (!jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testSetGetAutoStart_createContextwithUser_TCP_SecOff failed");
        }
    }

    public void testautoStart_TCP_SecOff(
                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        if (!jmsContext.getAutoStart()) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testautoStart_TCP_SecOff failed");
        }
    }

    public void testGetSessionMode_B_SecOff(
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        if (jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetSessionMode_B_SecOff failed");
        }
    }

    public void testGetSessionMode_TCP_SecOff(
                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;

        if (jmsContext.getSessionMode() != JMSContext.AUTO_ACKNOWLEDGE) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetSessionMode_TCP_SecOff failed");
        }
    }

    public void testClose_B_SecOff(
                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        jmsContext.close();
    }

    public void testClose_TCP_SecOff(
                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        jmsContext.close();
    }

    public void testcreateContextfromJMSContext_B_SecOff(
                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            int smode = JMSContext.AUTO_ACKNOWLEDGE;
            jmsContext.createContext(smode);
            testFailed = true;
        } catch (JMSRuntimeException ex3) {
            // expected
        }

        if (testFailed) {
            throw new Exception("testcreateContextfromJMSContext_B_SecOff failed: Expected exception was not thrown.");
        }
    }

    public void testcreateContextfromJMSContext_TCP_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();;

        boolean testFailed = false;
        try {
            int smode = JMSContext.AUTO_ACKNOWLEDGE;
            jmsContext.createContext(smode);
            testFailed = true;
        } catch (JMSRuntimeException ex3) {
            // expected
        }

        if (testFailed) {
            throw new Exception("testcreateContextfromJMSContext_TCP_SecOff failed: Expected exception was not thrown.");
        }
    }

    public void testsetClientID_B_SecOff(
                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.setClientID("TestID");
            testFailed = true;
        } catch (IllegalStateRuntimeException ex) {
            // expected
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testsetClientID_B_SecOff failed: Expected exception was not thrown.");
        }
    }

    public void testsetClientID_TCP_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.setClientID("TestID");
            testFailed = true;
        } catch (IllegalStateRuntimeException ex) {
            // expected
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testsetClientID_TCP_SecOff failed: Expected exception was not thrown.");
        }
    }

    public void testsetClientID_createContextUser_B_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = jmsTCFBindings.createContext(userName, password);

        boolean testFailed = false;
        try {
            jmsContext.setClientID("TestID");
            testFailed = true;
        } catch (IllegalStateRuntimeException ex) {
            // expected
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testsetClientID_createContextUser_B_SecOff failed: Expected exception was not thrown.");
        }
    }

    public void testsetClientID_createContextUser_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = jmsTCFTCP.createContext(userName, password);

        boolean testFailed = false;
        try {
            jmsContext.setClientID("TestID");
            testFailed = true;
        } catch (IllegalStateRuntimeException ex) {
            // expected
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testsetClientID_createContextUser_TCP_SecOff failed: Expected exception was not thrown.");
        }
    }

    public void testGetClientID_B_SecOff(
                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        String expectedId = "clientID";

        JMSContext jmsContext = jmsTCFBindings.createContext();
        String actualId = jmsContext.getClientID();

        boolean testFailed = false;
        if (!actualId.equals(expectedId)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetClientID_B_SecOff failed");
        }
    }

    public void testGetClientID_TCP_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        String expectedId = "clientID";

        JMSContext jmsContext = jmsTCFTCP.createContext();
        String actualId = jmsContext.getClientID();

        boolean testFailed = false;
        if (!actualId.equals(expectedId)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetClientID_TCP_SecOff failed");
        }
    }

    public void testGetClientID_createContextUser_B_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        String expectedId = "clientID";

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContext = jmsTCFBindings.createContext(userName, password);

        String actualId = jmsContext.getClientID();

        boolean testFailed = false;
        if (!actualId.equals(expectedId)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetClientID_createContextUser_B_SecOff failed");
        }
    }

    public void testGetClientID_createContextUser_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        String expectedId = "clientID";

        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContext = jmsTCFTCP.createContext(userName, password);

        String actualId = jmsContext.getClientID();

        boolean testFailed = false;
        if (!actualId.equals(expectedId)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetClientID_createContextUser_TCP_SecOff failed");
        }
    }

    public void testConnStartAuto_B_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("Hello");
        jmsProducer.send(topic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(30000);

        if ((recmsg == null) ||
            (recmsg.getText() == null) ||
            !recmsg.getText().equals("Hello")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testConnStartAuto_B_SecOff failed");
        }
    }

    public void testConnStartAuto_TCP_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("Hello");
        jmsProducer.send(topic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ((recmsg == null) ||
            (recmsg.getText() == null) ||
            !recmsg.getText().equals("Hello")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testConnStartAuto_TCP_SecOff failed");
        }
    }

    public void testConnStartAuto_createContextUser_B_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContext = jmsTCFBindings.createContext(userName, password);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        TextMessage sendmsg = jmsContext.createTextMessage("Hello");

        jmsContext.createProducer().send(topic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ((recmsg == null) ||
            (recmsg.getText() == null) ||
            !recmsg.getText().equals("Hello")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testConnStartAuto_createContextUser_B_SecOff failed");
        }
    }

    public void testConnStartAuto_createContextUser_TCP_SecOff(
                                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContext = jmsTCFTCP.createContext(userName, password);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        TextMessage sendmsg = jmsContext.createTextMessage("Hello");
        jmsContext.createProducer().send(topic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ((recmsg == null) ||
            (recmsg.getText() == null) ||
            !recmsg.getText().equals("Hello")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testConnStartAuto_createContextUser_TCP_SecOff failed");
        }
    }

    public void testConnStartAuto_createContextUserSessionMode_B_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsTCFBindings.createContext(userName, password, smode);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        TextMessage sendmsg = jmsContext.createTextMessage("Hello");
        jmsContext.createProducer().send(topic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ((recmsg == null) ||
            (recmsg.getText() == null) ||
            !recmsg.getText().equals("Hello")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testConnStartAuto_createContextUserSessionMode_B_SecOff failed");
        }
    }

    public void testConnStartAuto_createContextUserSessionMode_TCP_SecOff(
                                                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsTCFTCP.createContext(userName, password, smode);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        TextMessage sendmsg = jmsContext.createTextMessage("Hello");
        jmsContext.createProducer().send(topic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ((recmsg == null) ||
            (recmsg.getText() == null) ||
            !recmsg.getText().equals("Hello")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testConnStartAuto_createContextUserSessionMode_TCP_SecOff failed");
        }
    }

    // 118061_1 Verify creation of message from JMSContext. createMessage()

    public void testCreateMessage_B_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg = jmsContext.createMessage();
        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateMessage_B_SecOff failed");
        }
    }

    public void testCreateMessage_TCP_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg = jmsContext.createMessage();
        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateMessage_TCP_SecOff failed");
        }
    }

    public void testCreateObjectMessage_B_SecOff(
                                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        ObjectMessage msg = jmsContext.createObjectMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setObject(new StockObject("TestStock", 1234.5));
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateObjectMessage_B_SecOff failed");
        }
    }

    public void testCreateObjectMessage_TCP_SecOff(
                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        ObjectMessage msg = jmsContext.createObjectMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setObject(new StockObject("TestStock", 1234.5));
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowserjmsQCFTCP = jmsContext.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserjmsQCFTCP);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateObjectMessage_TCP_SecOff failed");
        }
    }

    public void testCreateObjectMessageSer_B_SecOff(
                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        ObjectMessage msg = jmsContext.createObjectMessage(new StockObject("TEST STOCK", 134.567));
        msg.setBooleanProperty("BooleanValue", true);
        msg.getObject();
        msg.getBody(StockObject.class);
        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateObjectMessageSer_B_SecOff failed");
        }
    }

    public void testCreateObjectMessageSer_TCP_SecOff(
                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        ObjectMessage msg = jmsContext.createObjectMessage(new StockObject("TEST STOCK", 134.567));
        msg.setBooleanProperty("BooleanValue", true);
        msg.getObject();
        msg.getBody(StockObject.class);
        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateObjectMessageSer_TCP_SecOff failed");
        }
    }

    // 118061_4 Verify creation of Stream Message from
    // JMSContext.createStreamMessage(), Perform operation for setdata and
    // reading data.

    public void testCreateStreamMessage_B_SecOff(
                                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        StreamMessage sendmsg = jmsContext.createStreamMessage();
        sendmsg.setBooleanProperty("BooleanValue", true);
        sendmsg.writeBoolean(true);
        sendmsg.writeString("Test case to create a Stream Message");
        sendmsg.reset();
        jmsContext.createProducer().send(queue, sendmsg);

        QueueBrowser queueBrowserjmsQCFBindings = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowserjmsQCFBindings);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        StreamMessage recmsg = (StreamMessage) jmsConsumer.receive(30000);

        recmsg.readString();
        recmsg.readBoolean();

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateStreamMessage_B_SecOff failed");
        }
    }

    public void testCreateStreamMessage_TCP_SecOff(
                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        StreamMessage msg = jmsContext.createStreamMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.writeBoolean(true);
        msg.writeString("Test case to create a Stream Message");
        msg.reset();
        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowserjmsQCFTCP = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowserjmsQCFTCP);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        StreamMessage recmsg = (StreamMessage) jmsConsumer.receive(30000);

        recmsg.readString();
        recmsg.readBoolean();

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsContext.close();
        if (testFailed) {
            throw new Exception("testCreateStreamMessage_TCP_SecOff failed");
        }
    }

    // 118061_5 Verify creation of Text Message from
    // JMSContext.createTextMessage().Perform setText and getTest operations.

    public void testCreateTextMessage_B_SecOff(
                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        String msgText = "Hello this is a test case for TextMessage ";
        TextMessage msg = jmsContext.createTextMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(msgText);
        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateTextMessage_B_SecOff failed");
        }
    }

    public void testCreateTextMessage_TCP_SecOff(
                                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        String msgText = "Hello this is a test case for TextMessage ";

        TextMessage msg = jmsContext.createTextMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(msgText);
        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateTextMessage_TCP_SecOff failed");
        }
    }

    // 118061_6 Verify creation of Text Message from
    // JMSContext.createTextMessage(String text).

    public void testCreateTextMessageStr_B_SecOff(
                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        String msgText0 = "Hello this is a test case for TextMessage";
        String msgText1 = "Hello";

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        TextMessage msg = jmsContext.createTextMessage(msgText0);
        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(msgText1);

        boolean testFailed = false;

        if (msg.getText() != msgText1) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg);
        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        if (numMsgs != 1) {
            testFailed = true;
        }

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateTextMessageStr_B_SecOff failed");
        }
    }

    public void testCreateTextMessageStr_TCP_SecOff(
                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        String msgText0 = "Hello this is a test case for TextMessage";
        String msgText1 = "Hello";

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        TextMessage msg = jmsContext.createTextMessage(msgText0);
        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(msgText1);

        boolean testFailed = false;

        if (msg.getText() != msgText1) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg);
        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateTextMessageStr_TCP_SecOff failed");
        }
    }

    // 118061_7 Verify creation of Map Message from
    // JMSContext.createMapMessage() .Perform set and get operation.

    public void testCreateMapMessage_B_SecOff(
                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        String name = "Valuepair";
        long value = 22222222;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        MapMessage msg = jmsContext.createMapMessage();
        msg.setLong(name, value);

        boolean testFailed = false;
        if (msg.getLong(name) != value) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateMapMessage_B_SecOff failed");
        }
    }

    public void testCreateMapMessage_TCP_SecOff(
                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        String name = "Valuepair";
        long value = 22222222;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        MapMessage msg = jmsContext.createMapMessage();
        msg.setLong(name, value);

        boolean testFailed = false;
        if (msg.getLong(name) != value) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg);

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        jmsConsumer.receive(30000);

        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateMapMessage_TCP_SecOff failed");
        }
    }

    // 118061_8 Verify creation of ByteMessage from
    // JMSContext.createBytesMessage(). Peform writeBytes, readBytes and getBody
    // operation.

    public void testCreateBytesMessage_B_SecOff(
                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        byte[] content = "test".getBytes();

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        BytesMessage sendmsg = jmsContext.createBytesMessage();
        sendmsg.writeBytes(content);
        sendmsg.reset();
        sendmsg.readBytes(content);
        sendmsg.getBodyLength();

        jmsContext.createProducer().send(queue, sendmsg);
        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        Message recmsg = jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateBytesMessage_B_SecOff failed");
        }
    }

    public void testCreateBytesMessage_TCP_SecOff(
                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        byte[] content = "test".getBytes();

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        BytesMessage sendmsg = jmsContext.createBytesMessage();
        sendmsg.writeBytes(content);
        sendmsg.reset();
        sendmsg.readBytes(content);
        sendmsg.getBodyLength();

        jmsContext.createProducer().send(queue, sendmsg);
        QueueBrowser queueBrowser = jmsContext.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        Message recmsg = jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateBytesMessage_B_SecOff failed");
        }
    }

    // 118061_9 Test with JMSDestination - setJMSDestination and
    // getJMSDestination

    public void testJMSDestination_B_SecOff(
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg0 = jmsContext.createMessage(); // TODO: What is this message for?

        Message msg1 = jmsContext.createMessage();
        msg1.setJMSDestination(queue1);

        boolean testFailed = false;
        if (!msg1.getJMSDestination().toString().equalsIgnoreCase("queue://QUEUE1")) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        Message recmsg = jmsConsumer.receive(30000);

        Destination jmsDestination = recmsg.getJMSDestination();

        if (!jmsDestination.toString().equalsIgnoreCase("queue://newQueue")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSDestination_B_SecOff failed:" +
                                " destination [ " + msg1.getJMSDestination() + ", " + jmsDestination + " ]");
        }
    }

    // 118061_9 Test with JMSDestination - setJMSDestination and
    // getJMSDestination

    public void testJMSDestination_TCP_SecOff(
                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg0 = jmsContext.createMessage(); // TODO: What is this message for?

        Message msg1 = jmsContext.createMessage();
        msg1.setJMSDestination(queue1);

        boolean testFailed = false;
        if (!msg1.getJMSDestination().toString().equalsIgnoreCase("queue://QUEUE1")) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        Message message = jmsConsumer.receive(30000);

        Destination jmsDestination = message.getJMSDestination();

        if (!jmsDestination.toString().equalsIgnoreCase("queue://newQueue")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSDestination_TCP_SecOff failed:" +
                                " destination [ " + msg1.getJMSDestination() + ", " + jmsDestination + " ]");
        }
    }

    public void testJMSDeliveryMode_B_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        TextMessage msg = jmsContext.createTextMessage("Hello");
        msg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);

        jmsContext.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        int dmode = jmsConsumer.receive(30000).getJMSDeliveryMode();

        boolean testFailed = false;
        if (dmode != DeliveryMode.NON_PERSISTENT) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSDeliveryMode_B_SecOff failed");
        }
    }

    public void testJMSDeliveryMode_TCP_SecOff(
                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        TextMessage msg = jmsContext.createTextMessage("Hello");
        msg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
        jmsContext.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        int dmode = jmsConsumer.receive(30000).getJMSDeliveryMode();

        boolean testFailed = false;
        if (dmode != DeliveryMode.NON_PERSISTENT) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSDeliveryMode_TCP_SecOff failed");
        }
    }

    // 118061_11 Verify set and get operation on Message header field
    // JMSMessageID

    // 118061_11 Test with JMSMessageID - setJMSMessageID ,getJMSMessageID and
    // setDisableMessageID

    public void testJMSMessageID_B_SecOff(
                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        String msgText = "Hello this is a test case for TextMessage";
        TextMessage msg0 = jmsContext.createTextMessage(msgText);
        msg0.setJMSMessageID("MSGID");
        jmsProducer.send(queue, msg0);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        String msgid0 = jmsConsumer.receive(30000).getJMSMessageID();

        boolean testFailed = false;
        if (!msgid0.startsWith("ID:")) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageID(true);

        TextMessage msg1 = jmsContext.createTextMessage(msgText);
        jmsProducer.send(queue, msg1);

        String msgid1 = jmsConsumer.receive(30000).getJMSMessageID();

        if (msgid1 != null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSMessageID_B_SecOff failed");
        }
    }

    public void testJMSMessageID_TCP_SecOff(
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        String msgText = "Hello this is a test case for TextMessage";
        TextMessage msg0 = jmsContext.createTextMessage(msgText);
        msg0.setJMSMessageID("MSGID");
        jmsProducer.send(queue, msg0);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        String msgid0 = jmsConsumer.receive(30000).getJMSMessageID();

        boolean testFailed = false;
        if (!msgid0.startsWith("ID:")) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageID(true);

        TextMessage msg1 = jmsContext.createTextMessage(msgText);
        jmsProducer.send(queue, msg1);

        String msgid1 = jmsConsumer.receive(30000).getJMSMessageID();

        if (msgid1 != null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSMessageID_TCP_SecOff failed");
        }
    }

    // 118061_12 Verify set and get operation on Message header field
    // JMSTimeStamp

    public void testJMSTimestamp_B_SecOff(
                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        String msgText = "Hello this is a test case for TextMessage ";

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg0 = jmsContext.createMessage();

        TextMessage msg1 = jmsContext.createTextMessage(msgText);
        msg1.setJMSTimestamp(1234567);

        long beforeSend = System.currentTimeMillis();
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(queue, msg1);
        long afterSend = System.currentTimeMillis();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        long timestamp0 = jmsConsumer.receive(30000).getJMSTimestamp();

        if ((timestamp0 < beforeSend) ||
            (timestamp0 > afterSend) ||
            (timestamp0 == 1234567)) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageTimestamp(true);
        jmsProducer.send(queue, msg1);

        long timestamp1 = jmsConsumer.receive(30000).getJMSTimestamp();

        if (timestamp1 != 0) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSTimestamp_B_SecOff failed");
        }
    }

    // 118061_12 Verify set and get operation on Message header field
    // JMSTimeStamp

    public void testJMSTimestamp_TCP_SecOff(
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        String msgText = "Hello this is a test case for TextMessage ";

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg0 = jmsContext.createMessage();

        TextMessage msg1 = jmsContext.createTextMessage(msgText);
        msg1.setJMSTimestamp(1234567);

        long beforeSend = System.currentTimeMillis();
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(queue, msg1);
        long afterSend = System.currentTimeMillis();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        long timestamp0 = jmsConsumer.receive(30000).getJMSTimestamp();

        if ((timestamp0 < beforeSend) ||
            (timestamp0 > afterSend) ||
            (timestamp0 == 1234567)) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageTimestamp(true);
        jmsProducer.send(queue, msg1);

        long timestamp1 = jmsConsumer.receive(30000).getJMSTimestamp();

        if (timestamp1 != 0) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSTimestamp_TCP_SecOff failed");
        }
    }

    // 118061_13 Test with JMSCorrelationID- setJMSCorrelationID
    // ,getJMSCorrelationID

    public void testJMSCorrelationID_B_SecOff(
                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg = jmsContext.createMessage();
        String defaultCorrel = msg.getJMSCorrelationID();

        boolean testFailed = false;

        if (defaultCorrel != null) {
            testFailed = true;
        }

        String correl = "MyCorrelID";

        msg.setJMSCorrelationID(correl);
        String setCorrel = msg.getJMSCorrelationID();

        if (!correl.equals(setCorrel)) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        String recCorrel = jmsConsumer.receive(30000).getJMSCorrelationID();

        if (!correl.equals(recCorrel)) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSCorrelationID_B_SecOff failed");
        }
    }

    // 118061_13 Test with JMSCorrelationID- setJMSCorrelationID
    // ,getJMSCorrelationID

    public void testJMSCorrelationID_TCP_SecOff(
                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        String correl = "MyCorrelID";

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg = jmsContext.createMessage();
        String defaultCorrel = msg.getJMSCorrelationID();

        boolean testFailed = false;

        if (defaultCorrel != null) {
            testFailed = true;
        }

        msg.setJMSCorrelationID(correl);
        String setCorrel = msg.getJMSCorrelationID();

        if (!correl.equals(setCorrel)) {
            testFailed = true;
        }

        jmsContext.createProducer().send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        String recCorrel = jmsConsumer.receive(30000).getJMSCorrelationID();

        if (!correl.equals(recCorrel)) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSCorrelationID_B_SecOff failed");
        }
    }

    // 118061_14 Test with JMSCorrelationID- setJMSCorrelationIDAsbytes
    // ,getJMSCorrelationIDAsBytes

    public void testJMSCorrelationIDAsBytes_B_SecOff(
                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

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
                testComp = false;
            } else
                testComp = true;
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

        boolean testComp1 = false;
        for (int j = 0; j < testA.length; j++) {
            if (testA[j] != resultF[j]) {
                testComp = false;
            } else
                testComp = true;
        }

        if (resultE != null && testComp && testComp1) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSCorrelationIDAsBytes_B_SecOff failed");
        }
    }

    private boolean equals(byte[] bytesA, byte[] bytesB) {
        if (bytesA == null) {
            return (bytesB == null);
        } else if (bytesB == null) {
            return false;
        } else {
            int lengthA = bytesA.length;
            int lengthB = bytesB.length;
            if (lengthA != lengthB) {
                return false;
            } else {
                for (int byteNo = 0; byteNo < lengthA; byteNo++) {
                    if (bytesA[byteNo] != bytesB[byteNo]) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    // 118061_14 Test with JMSCorrelationID- setJMSCorrelationIDAsbytes
    // ,getJMSCorrelationIDAsBytes
    public void testJMSCorrelationIDAsBytes_TCP_SecOff(
                                                       HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg = jmsContext.createMessage();

        byte[] defBytes = msg.getJMSCorrelationIDAsBytes();
        String defCorrel = msg.getJMSCorrelationID();

        byte[] testA = { 1, 2, 3, 4 };
        msg.setJMSCorrelationIDAsBytes(testA);
        byte[] resultA = msg.getJMSCorrelationIDAsBytes();

        boolean testFailed = false;

        if (!equals(testA, resultA)) {
            testFailed = true;
        }

        String correl = "CorrelID";

        // check that multiple set sequences always return the last set value

        msg.setJMSCorrelationIDAsBytes(testA);
        msg.setJMSCorrelationID(correl);
        byte[] resultB = msg.getJMSCorrelationIDAsBytes();

        if (equals(testA, resultB)) {
            testFailed = true;
        }

        msg.setJMSCorrelationID(correl);
        msg.setJMSCorrelationIDAsBytes(testA);
        byte[] resultC = msg.getJMSCorrelationIDAsBytes();

        if (!equals(testA, resultC)) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSCorrelationIDAsBytes_TCP_SecOff failed");
        }
    }

    public void testJMSReplyTo_B_SecOff(
                                        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg = jmsContext.createMessage();

        msg.setJMSReplyTo(queue);
        if (msg.getJMSReplyTo() != queue) {
            testFailed = true;
        }

        msg.setJMSReplyTo(topic);
        if (msg.getJMSReplyTo() != topic) {
            testFailed = true;
        }

        msg.setJMSReplyTo(null);
        if (msg.getJMSReplyTo() != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSReplyTo_B_SecOff failed");
        }
    }

    public void testJMSReplyTo_TCP_SecOff(
                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg = jmsContext.createMessage();

        msg.setJMSReplyTo(queue);
        if (msg.getJMSReplyTo() != queue) {
            testFailed = true;
        }

        msg.setJMSReplyTo(topic);
        if (msg.getJMSReplyTo() != topic) {
            testFailed = true;
        }

        msg.setJMSReplyTo(null);
        if (msg.getJMSReplyTo() != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSReplyTo_B_SecOff failed");
        }
    }

    // 118061_16 Test with JMSRedelivered- setJMSRedelivered and
    // getJMSRedelivered

    public void testJMSRedelivered_B_SecOff(
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        Message msg = jmsContext.createMessage();

        boolean defRedelivered = msg.getJMSRedelivered();

        msg.setJMSRedelivered(!defRedelivered);

        jmsContext.createProducer().send(queue, msg);

        Message recmsg = jmsConsumer.receive(30000);
        if (recmsg.getJMSRedelivered() == defRedelivered) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSRedelivered_B_SecOff failed");
        }
    }

    public void testJMSRedelivered_TCP_SecOff(
                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        Message msg = jmsContext.createMessage();

        boolean defRedelivered = msg.getJMSRedelivered();
        msg.setJMSRedelivered(!defRedelivered);
        jmsContext.createProducer().send(queue, msg);

        Message recmsg = jmsConsumer.receive(30000);
        if (recmsg.getJMSRedelivered() == defRedelivered) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSRedelivered_B_SecOff failed");
        }
    }

    // 118061_17 Verify set and get operation on Message header field JMSType

    public void testJMSType_B_SecOff(
                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        Message msg = jmsContext.createMessage();

        String type1 = "type 1";
        msg.setJMSType(type1);
        jmsContext.createProducer().send(queue, msg);

        String t1 = jmsConsumer.receive(30000).getJMSType();

        if (!t1.equals(type1)) {
            testFailed = true;
        }

        String type2 = "type 2";
        msg.setJMSType(type2);
        jmsContext.createProducer().send(queue, msg);

        String t2 = jmsConsumer.receive(30000).getJMSType();

        if (!t2.equals(type2)) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSType_B_secOff failed");
        }
    }

    // 118061_17 Test with JMSType- setJMSType and getJMSType

    public void testJMSType_TCP_SecOff(
                                       HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        Message msg = jmsContext.createMessage();

        String type1 = "type 1";
        msg.setJMSType(type1);
        jmsContext.createProducer().send(queue, msg);
        String t1 = jmsConsumer.receive(30000).getJMSType();

        if (!t1.equals(type1)) {
            testFailed = true;
        }

        String type2 = "type 2";
        msg.setJMSType(type2);
        jmsContext.createProducer().send(queue, msg);
        String t2 = jmsConsumer.receive(30000).getJMSType();

        if (!t2.equals(type2)) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSType_TCP_secOff failed");
        }
    }

    // 118061_18 Verify set and get operation on Message header field
    // JMSExpiration

    // 118061_18 Test with JMSExpiration- setJMSExpiration and getJMSExpiration

    public void testJMSExpiration_B_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg = jmsContext.createMessage();
        long defexp = msg.getJMSExpiration();

        if (defexp != 0) {
            testFailed = true;
        }

        msg.setJMSExpiration(1);
        jmsContext.createProducer().send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        long exp = jmsConsumer.receive(30000).getJMSExpiration();

        if (exp != 0) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSExpiration_B_SecOff failed; defexp [ " + defexp + " ] exp [ " + exp + " ]");
        }
    }

    public void testJMSExpiration_TCP_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg = jmsContext.createMessage();
        long defexp = msg.getJMSExpiration();

        if (defexp != 0) {
            testFailed = true;
        }

        msg.setJMSExpiration(1);
        jmsContext.createProducer().send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        long exp = jmsConsumer.receive(30000).getJMSExpiration();

        if (exp != 0) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSExpiration_TCP_SecOff failed: defexp [ " + defexp + " ] exp [ " + exp + " ]");
        }
    }

    // 118061_19 Test with JMSPriority- setJMSPriority and getJMSPriority

    public void testJMSPriority_B_SecOff(
                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg = jmsContext.createMessage();
        msg.setJMSPriority(9);

        jmsContext.createProducer().setPriority(1).send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        int pri = jmsConsumer.receive(30000).getJMSPriority();

        boolean testFailed = false;
        if (pri != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSPriority_B_SecOff failed");
        }
    }

    public void testJMSPriority_TCP_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg = jmsContext.createMessage();
        msg.setJMSPriority(9);

        jmsContext.createProducer().setPriority(1).send(queue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        int pri = jmsConsumer.receive(30000).getJMSPriority();

        boolean testFailed = false;
        if (pri != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSPriority_TCP_SecOff failed");
        }
    }

    // 118061_20 Verify set and get operation on Message header field
    // JMSDeliveryTime

    public void testJMSDeliveryTime_B_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        Message msg = jmsContext.createMessage();

        long toSet = 12345;
        msg.setJMSDeliveryTime(toSet);
        long afterSet = msg.getJMSDeliveryTime();

        boolean testFailed = false;
        if (afterSet != toSet) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSDeliveryTime_B_SecOff failed");
        }
    }

    public void testJMSDeliveryTime_TCP_SecOff(
                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        Message msg = jmsContext.createMessage();

        long toSet = 12345;
        msg.setJMSDeliveryTime(toSet);
        long afterSet = msg.getJMSDeliveryTime();

        boolean testFailed = false;
        if (afterSet != toSet) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSDeliveryTime_TCP_SecOff failed");
        }
    }

    // 118062_1_1 Creates a QueueBrowser object to peek at the messages on the
    // specified queue.

    public void testcreateBrowser_B_SecOff(
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue2);

        jmsContext.createProducer().send(queue2, "Tester");

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue2);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_B_SecOff failed");
        }
    }

    public void testcreateBrowser_TCP_SecOff(
                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue2);

        jmsContext.createProducer().send(queue2, "Tester");

        QueueBrowser queueBrowser = jmsContext.createBrowser(queue2);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue2);
        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (numMsgs != 1) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_TCP_SecOff failed");
        }
    }

    public void testcreateBrowserNEQueue_B_SecOff(
                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            QueueBrowser qb = jmsContext.createBrowser(null);
            testFailed = true;

        } catch (InvalidDestinationRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowserNEQueue_B_SecOff failed");
            }
        }
    }

    public void testcreateBrowserNEQueue_TCP_SecOff(
                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            QueueBrowser qb = jmsContext.createBrowser(null);
            testFailed = true;

        } catch (InvalidDestinationRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowserNEQueue_TCP_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_MessageSelector_InvalidQ_B_SecOff(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        try {
            QueueBrowser qb = jmsContext.createBrowser(null, "colour = 'red'");
            testFailed = true;

        } catch (InvalidDestinationRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_MessageSelector_InvalidQ_B_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        try {
            QueueBrowser qb = jmsContext.createBrowser(null, "colour = 'red'");
            testFailed = true;

        } catch (InvalidDestinationRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_MessageSelector_B_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        TextMessage sendmsg = jmsContext.createTextMessage("browse selector test message");

        int numMsgs = 5;

        for (int msgNo = 0; msgNo < numMsgs; msgNo++) {
            sendmsg.setStringProperty("colour", "red");
            jmsContext.createProducer().send(queue, sendmsg);

            sendmsg.setStringProperty("colour", "blue");
            jmsContext.createProducer().send(queue, sendmsg);
        }

        int nMsgRed = 0;
        int nWrongRed = 0;

        QueueBrowser qb1 = jmsContext.createBrowser(queue, "colour = 'red'");
        Enumeration e1 = qb1.getEnumeration();
        while (e1.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e1.nextElement();
            String colour = recmsg.getStringProperty("colour");

            if ((colour != null) && colour.equals("red")) {
                nMsgRed++;
            } else {
                nWrongRed++;
            }
        }

        int nMsgBlue = 0;
        int nWrongBlue = 0;

        QueueBrowser qb2 = jmsContext.createBrowser(queue, "colour = 'blue'");
        Enumeration e2 = qb2.getEnumeration();
        while (e2.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e2.nextElement();
            String colour = recmsg.getStringProperty("colour");

            if ((colour != null) && colour.equals("blue")) {
                nMsgBlue++;
            } else {
                nWrongBlue++;
            }
        }

        if (((nMsgRed != numMsgs) || (nWrongRed != 0)) ||
            ((nMsgBlue != numMsgs) || (nWrongBlue != 0))) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_MessageSelector_B_SecOff failed");
        }
    }

    public void testcreateBrowser_MessageSelector_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        int numMsgs = 5;

        TextMessage sendmsg = jmsContext.createTextMessage("browse selector test message");

        for (int msgNo = 0; msgNo < numMsgs; msgNo++) {
            sendmsg.setStringProperty("colour", "red");
            jmsContext.createProducer().send(queue, sendmsg);

            sendmsg.setStringProperty("colour", "blue");
            jmsContext.createProducer().send(queue, sendmsg);
        }

        int nMsgRed = 0;
        int nWrongRed = 0;

        QueueBrowser qb1 = jmsContext.createBrowser(queue, "colour = 'red'");
        Enumeration e1 = qb1.getEnumeration();
        while (e1.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e1.nextElement();
            String colour = recmsg.getStringProperty("colour");

            if ((colour != null) && colour.equals("red")) {
                nMsgRed++;
            } else {
                nWrongRed++;
            }
        }

        int nMsgBlue = 0;
        int nWrongBlue = 0;

        QueueBrowser qb2 = qb2 = jmsContext.createBrowser(queue, "colour = 'blue'");
        Enumeration e2 = qb2.getEnumeration();
        while (e2.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e2.nextElement();
            String colour = recmsg.getStringProperty("colour");

            if ((colour != null) && colour.equals("blue")) {
                nMsgBlue++;
            } else {
                nWrongBlue++;
            }
        }

        if (((nMsgRed != numMsgs) || (nWrongRed != 0)) ||
            ((nMsgBlue != numMsgs) || (nWrongBlue != 0))) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_MessageSelector_TCP_SecOff failed");
        }
    }

    public void testcreateBrowser_MessageSelector_NullQueue_B_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;

        try {
            jmsContext.createBrowser(null, "colour = 'red'");
            testFailed = true;

        } catch (InvalidDestinationRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_MessageSelector_NullQueue_B_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;

        try {
            jmsContext.createBrowser(null, "colour = 'red'");
            testFailed = true;

        } catch (InvalidDestinationRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_MessageSelector_Empty_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        TextMessage sendmsg = jmsContext.createTextMessage("browse selector test message");

        for (int msgNo = 0; msgNo < 3; msgNo++) {
            sendmsg.setStringProperty("Role", "Tester");
            jmsContext.createProducer().send(queue, sendmsg);

            sendmsg.setStringProperty("Role", "Developer");
            jmsContext.createProducer().send(queue, sendmsg);

            sendmsg.setStringProperty("Role", "");
            jmsContext.createProducer().send(queue, sendmsg);
        }

        int testVar = 0;
        int testWrongVar = 0;

        QueueBrowser qb1 = jmsContext.createBrowser(queue, "");
        Enumeration e1 = qb1.getEnumeration();
        while (e1.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e1.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName != null) && roleName.equals("Tester")) {
                testVar++;
            } else {
                testWrongVar++;
            }
        }

        int devVar = 0;
        int devWrongVar = 0;

        QueueBrowser qb2 = jmsContext.createBrowser(queue, "");
        Enumeration e2 = qb2.getEnumeration();
        while (e2.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e2.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName != null) && roleName.equals("Developer")) {
                devVar++;
            } else {
                devWrongVar++;
            }
        }

        int emptyVar = 0;
        int emptyWrongVar = 0;

        QueueBrowser qb3 = jmsContext.createBrowser(queue, "");
        Enumeration e3 = qb3.getEnumeration();
        while (e3.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e3.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName != null) && roleName.equals("")) {
                emptyVar++;
            } else {
                emptyWrongVar++;
            }
        }

        boolean testFailed = false;
        if (!((testVar == 3) && (devVar == 3) && (emptyVar == 3)) ||
            ((testWrongVar == 0) || (devWrongVar == 0) || (emptyWrongVar == 0))) {
            testFailed = true;
        }

        emptyQueue(jmsQCFBindings, queue);

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_MessageSelector_Empty_B_SecOff failed:" +
                                " test [ " + testVar + " " + testWrongVar + " ]" +
                                " dev [ " + devVar + " " + devWrongVar + " ] " +
                                " empty [ " + emptyVar + " " + emptyWrongVar + " ]");
        }
    }

    public void testcreateBrowser_MessageSelector_Empty_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        TextMessage sendmsg = jmsContext.createTextMessage("browse selector test message");

        for (int msgNo = 0; msgNo < 3; msgNo++) {
            sendmsg.setStringProperty("Role", "Tester");
            jmsContext.createProducer().send(queue, sendmsg);

            sendmsg.setStringProperty("Role", "Developer");
            jmsContext.createProducer().send(queue, sendmsg);

            sendmsg.setStringProperty("Role", "");
            jmsContext.createProducer().send(queue, sendmsg);
        }

        int testVar = 0;
        int testWrongVar = 0;

        QueueBrowser qb1 = jmsContext.createBrowser(queue, "");
        Enumeration e1 = qb1.getEnumeration();
        while (e1.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e1.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName != null) && roleName.equals("Tester")) {
                testVar++;
            } else {
                testWrongVar++;
            }
        }

        int devVar = 0;
        int devWrongVar = 0;

        QueueBrowser qb2 = jmsContext.createBrowser(queue, "");
        Enumeration e2 = qb2.getEnumeration();
        while (e2.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e2.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName != null) && roleName.equals("Developer")) {
                devVar++;
            } else {
                devWrongVar++;
            }
        }

        int emptyVar = 0;
        int emptyWrongVar = 0;

        QueueBrowser qb3 = jmsContext.createBrowser(queue, "");
        Enumeration e3 = qb3.getEnumeration();
        while (e3.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e3.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName != null) && roleName.equals("")) {
                emptyVar++;
            } else {
                emptyWrongVar++;
            }
        }

        boolean testFailed = false;
        if (!((testVar == 3) && (devVar == 3) && (emptyVar == 3)) ||
            ((testWrongVar == 0) || (devWrongVar == 0) || (emptyWrongVar == 0))) {
            testFailed = true;
        }

        emptyQueue(jmsQCFTCP, queue);

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_MessageSelector_Empty_TCP_SecOff failed:" +
                                " test [ " + testVar + " " + testWrongVar + " ]" +
                                " dev [ " + devVar + " " + devWrongVar + " ] " +
                                " empty [ " + emptyVar + " " + emptyWrongVar + " ]");
        }
    }

    public void testcreateBrowser_MessageSelector_Null_B_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        TextMessage msg = jmsContext.createTextMessage("browse selector test message");
        msg.setStringProperty("Role", "Tester");
        jmsContext.createProducer().send(queue, msg);

        boolean testFailed = false;

        QueueBrowser qb = jmsContext.createBrowser(queue, null);
        Enumeration e = qb.getEnumeration();
        while (e.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName == null) || !roleName.equals("Tester")) {
                testFailed = true;
            }
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_MessageSelector_Null_B_SecOff failed");
        }
    }

    public void testcreateBrowser_MessageSelector_Null_TCP_SecOff(
                                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        TextMessage msg = jmsContext.createTextMessage("browse selector test message");
        msg.setStringProperty("Role", "Tester");
        jmsContext.createProducer().send(queue, msg);

        boolean testFailed = false;

        QueueBrowser qb = jmsContext.createBrowser(queue, null);
        Enumeration e = qb.getEnumeration();
        while (e.hasMoreElements()) {
            TextMessage recmsg = (TextMessage) e.nextElement();
            String roleName = recmsg.getStringProperty("Role");

            if ((roleName == null) || !roleName.equals("Tester")) {
                testFailed = true;
            }
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_MessageSelector_Null_TCP_SecOff failed");
        }
    }

    public void testcreateBrowser_MessageSelector_Invalid_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        boolean testFailed = false;

        try {
            QueueBrowser invalidQB = jmsContext.createBrowser(queue, "bad selector");
            testFailed = true;

        } catch (InvalidSelectorRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_MessageSelector_Null_B_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_MessageSelector_Invalid_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        boolean testFailed = false;

        try {
            QueueBrowser invalidQB = jmsContext.createBrowser(queue, "bad selector");
            testFailed = true;

        } catch (InvalidSelectorRuntimeException ex3) {
            // expected

        } finally {
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_MessageSelector_Null_TCP_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_getQueue_B_SecOff(
                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        QueueBrowser qb = jmsContext.createBrowser(queue);

        boolean testFailed = false;
        if (qb.getQueue() != queue) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_getQueue_B_SecOff failed");
        }
    }

    public void testcreateBrowser_getQueue_TCP_SecOff(
                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        QueueBrowser qb = jmsContext.createBrowser(queue);

        boolean testFailed = false;
        if (qb.getQueue() != queue) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testcreateBrowser_getQueue_TCP_SecOff failed");
        }
    }

    public void testcreateBrowser_close_B_SecOff(
                                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        jmsContext.createProducer().send(queue, "This is a test message.");

        QueueBrowser qb = jmsContext.createBrowser(queue);

        Enumeration e0 = qb.getEnumeration();
        int numMsgs = 0;
        while (e0.hasMoreElements()) {
            TextMessage message = (TextMessage) e0.nextElement();
            numMsgs++;
        }

        qb.close();

        boolean testFailed = false;

        try {
            Enumeration e1 = qb.getEnumeration();

            int numMsgs1 = 0;
            while (e1.hasMoreElements()) {
                TextMessage message = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            testFailed = true;

        } catch (JMSException ex) {
            // expected

        } finally {
            emptyQueue(jmsQCFBindings, queue);

            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_close_B_SecOff failed");
            }
        }
    }

    public void testcreateBrowser_close_TCP_SecOff(
                                                   HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        jmsContext.createProducer().send(queue, "This is a test message.");

        QueueBrowser qb = jmsContext.createBrowser(queue);

        Enumeration e0 = qb.getEnumeration();
        int numMsgs0 = 0;
        while (e0.hasMoreElements()) {
            TextMessage message = (TextMessage) e0.nextElement();
            numMsgs0++;
        }

        qb.close();

        boolean testFailed = false;

        try {
            Enumeration e1 = qb.getEnumeration();
            int numMsgs1 = 0;
            while (e1.hasMoreElements()) {
                TextMessage message = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            testFailed = true;

        } catch (JMSException ex) {
            // expected

        } finally {
            emptyQueue(jmsQCFTCP, queue);
            jmsContext.close();

            if (testFailed) {
                throw new Exception("testcreateBrowser_close_TCP_SecOff failed");
            }
        }
    }

    public void testGetMessageSelector_B_SecOff(
                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        QueueBrowser qb = jmsContext.createBrowser(queue, "colour = 'red'");
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (!msgSelect.equals("colour = 'red'")) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_B_SecOff failed");
        }
    }

    public void testGetMessageSelector_TCP_SecOff(
                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        QueueBrowser qb = jmsContext.createBrowser(queue, "colour = 'red'");
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (!msgSelect.equals("colour = 'red'")) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_TCP_SecOff failed");
        }
    }

    // 118062_4_2 Test when no message selector exists for the message consumer,
    // it returns null

    public void testGetMessageSelector_Consumer_B_SecOff(
                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        JMSConsumer jmsConsumer0 = jmsContext.createConsumer(queue, "Color='red'");
        String msgSelect0 = jmsConsumer0.getMessageSelector();
        if (!msgSelect0.equals("Color='red'")) {
            testFailed = true;
        }

        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(queue);
        String msgSelect1 = jmsConsumer1.getMessageSelector();

        if (msgSelect1 != null) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer0.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_Consumer_B_SecOff failed");
        }
    }

    public void testGetMessageSelector_Consumer_TCP_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        JMSConsumer jmsConsumer0 = jmsContext.createConsumer(queue, "Color='red'");
        String msgSelect0 = jmsConsumer0.getMessageSelector();

        if (!msgSelect0.equals("Color='red'")) {
            testFailed = true;
        }

        JMSConsumer jmsConsumer1 = jmsContext.createConsumer(queue);
        String msgSelect1 = jmsConsumer1.getMessageSelector();

        if (msgSelect1 != null) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer0.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_Consumer_TCP_SecOff failed");
        }
    }

    // 118062_4_3 Test when message selector is set to null, it returns null

    public void testGetMessageSelector_null_B_SecOff(
                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        QueueBrowser qb = jmsContext.createBrowser(queue, null);
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (msgSelect != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_null_B_SecOff failed");
        }
    }

    public void testGetMessageSelector_null_TCP_SecOff(
                                                       HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        QueueBrowser qb = jmsContext.createBrowser(queue, null);
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (msgSelect != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_null_TCP_SecOff failed");
        }
    }

    // 118062_4_4 Test when message selector is set to empty string, it returns
    // null

    public void testGetMessageSelector_Empty_B_SecOff(
                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        QueueBrowser qb = jmsContext.createBrowser(queue, "");
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (msgSelect != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_Empty_B_SecOff failed");
        }
    }

    public void testGetMessageSelector_Empty_TCP_SecOff(
                                                        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFBindings, queue);

        QueueBrowser qb = jmsContext.createBrowser(queue, "");
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (msgSelect != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_Empty_TCP_SecOff failed");
        }
    }

    // 118062_4_5 Test when message selector is not set , it returns null

    public void testGetMessageSelector_notSet_B_SecOff(
                                                       HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        QueueBrowser qb = jmsContext.createBrowser(queue);
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (msgSelect != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_notSet_B_SecOff failed");
        }
    }

    public void testGetMessageSelector_notSet_TCP_SecOff(
                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, queue);

        QueueBrowser qb = jmsContext.createBrowser(queue);
        String msgSelect = qb.getMessageSelector();

        boolean testFailed = false;
        if (msgSelect != null) {
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testGetMessageSelector_notSet_B_SecOff failed");
        }
    }

    //Defect 174395
    public void testTextMessageGetBody_B_SecOff(
                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        TextMessage msg = jmsContext.createTextMessage();
        String msgText = "Hello this is a test case for TextMessage ";
        msg.setText(msgText);

        boolean testFailed = false;
        try {
            msg.getBody(Boolean.class);
            testFailed = true;
        } catch (MessageFormatException ex) {
            // expected
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testTextMessageGetBody_B_SecOff failed");
        }
    }

    // Defect 174387

    public void testByteMessageGetBody_B_SecOff(
                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        byte[] content = "test".getBytes();
        BytesMessage msg = jmsContext.createBytesMessage();
        msg.writeBytes(content);

        msg.reset();
        msg.readBytes(content);
        msg.getBodyLength();

        boolean testFailed = false;
        try {
            msg.getBody(StringBuffer.class);
            testFailed = true;
        } catch (MessageFormatException mfe) {
            // expected
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testByteMessageGetBody_B_SecOff failed");
        }
    }

    //Defect 174399
    public void testObjectMessageisBodyAssignable_B_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        ObjectMessage msg = jmsContext.createObjectMessage();

        msg.setObject(new StockObject("TESTSTOCK", 5467.123));
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        boolean testFailed = false;

        try {
            if (msg.isBodyAssignableTo(Boolean.class)) {
                testFailed = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testObjectMessageisBodyAssignable_B_SecOff failed");
        }
    }

    //Defect 174397
    public void testObjectMessagegetBody_B_SecOff(
                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        ObjectMessage msg = jmsContext.createObjectMessage();
        msg.setObject(new StockObject("TESTSTOCK", 678.97));
        msg.getObject();

        boolean testFailed = false;
        try {
            msg.getBody(HashMap.class);
            testFailed = true;
        } catch (MessageFormatException ex) {
            // expected
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testObjectMessagegetBody_B_SecOff failed");
        }
    }

    // Defect 174403

    public void testJMSReplyTo(
                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, queue);

        TextMessage msg = jmsContext.createTextMessage();
        msg.setText("testJMSReplyTo");
        msg.setJMSReplyTo(queue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(queue, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(30000);

        String replyQ = recmsg.getJMSReplyTo().toString();

        boolean testFailed = false;
        if (!queue.toString().equals(replyQ)) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testJMSReplyTo failed");
        }
    }

    private static class StockObject implements Serializable {
        final String stockName;
        final double stockValue;

        StockObject(String stname, double stvalue) {
            this.stockName = stname;
            this.stockValue = stvalue;
        }
    }
}
