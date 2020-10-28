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
    public Queue queue3;
    public Topic topic;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {

            QCFBindings = getQCFBindings();
            TCFBindings = getTCFBindings();
            QCFTCP = getQCFTCP();
            TCFTCP = getTCFTCP();
            queue = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q");

            queue1 = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q1");

            queue2 = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q2");

            queue3 = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q3");

            topic = (Topic) new InitialContext()
                            .lookup("java:comp/env/eis/topic1");

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
            System.out.println(" Starting : " + test);
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" Ending : " + test);
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Ending : " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testCreateContext_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        if (!(jmsContextQCFBindings.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE && jmsContextQCFBindings.getAutoStart() == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateContext_B_SecOff failed");

    }

    public void testCreateContext_TCP_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        if (!(jmsContextQCFTCP.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE && jmsContextQCFTCP.getAutoStart() == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateContext_TCP_SecOff failed");

    }

    public void testGetMetadata_B_SecOff(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        ConnectionMetaData metadata = jmsContextQCFBindings.getMetaData();
        if (!(metadata.getJMSVersion().equals("2.0")
              && metadata.getJMSMajorVersion() == 2
              && metadata.getJMSMinorVersion() == 0
              && metadata.getJMSProviderName().equals("IBM")
              && metadata.getProviderVersion().equals("1.0")
              && metadata.getProviderMajorVersion() == 1
              && metadata.getProviderMinorVersion() == 0))
        {
            exceptionFlag = true;

        }
        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testGetMetadata_B_SecOff failed");

    }

    public void testGetMetadata_TCP_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        ConnectionMetaData metadata = jmsContextQCFTCP.getMetaData();
        if (!(metadata.getJMSVersion().equals("2.0")
              && metadata.getJMSMajorVersion() == 2
              && metadata.getJMSMinorVersion() == 0
              && metadata.getJMSProviderName().equals("IBM")
              && metadata.getProviderVersion().equals("1.0")
              && metadata.getProviderMajorVersion() == 1
              && metadata.getProviderMinorVersion() == 0))
        {
            exceptionFlag = true;

        }

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testGetMetadata_TCP_SecOff failed");

    }

    public void testSetGetAutoStart_B_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        jmsContextQCFBindings.setAutoStart(false);

        if (!(jmsContextQCFBindings.getAutoStart() == false))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testSetGetAutoStart_B_SecOff failed");

    }

    public void testSetGetAutoStart_TCP_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        jmsContextQCFTCP.setAutoStart(false);

        if (!(jmsContextQCFTCP.getAutoStart() == false))
            exceptionFlag = true;
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testSetGetAutoStart_TCP_SecOff failed");

    }

    public void testcreateContextwithUser_B_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(userName, password);

        if (!(jmsContextQCFBindings.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE) && (jmsContextQCFBindings.getAutoStart() == true))

            exceptionFlag = true;
        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_B_SecOff failed");

    }

    public void testcreateContextwithUser_empty_B_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "";
        String password = "";

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(userName, password);

        if (!(jmsContextQCFBindings.getAutoStart() == true)
            && (jmsContextQCFBindings.getSessionMode() == jmsContextQCFBindings.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_empty_B_SecOff failed");

    }

    public void testcreateContextwithUserSessionMode_empty_B_SecOff(
                                                                    HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = "";
        String password = "";

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(userName, password);

        if (!(jmsContextQCFBindings.getAutoStart() == true
        && jmsContextQCFBindings.getSessionMode() == jmsContextQCFBindings.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserSessionMode_empty_B_SecOff failed");

    }

    public void testcreateContextwithUser_null_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = null;
        String password = null;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(userName, password);

        if (!(jmsContextQCFBindings.getAutoStart() == true)
            && (jmsContextQCFBindings.getSessionMode() == jmsContextQCFBindings.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_null_B_SecOff failed");

    }

    public void testcreateContextwithUserSessionMode_null_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = null;
        String password = null;

        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(userName, password, smode);

        if (!(jmsContextQCFBindings.getAutoStart() == true
        && jmsContextQCFBindings.getSessionMode() == jmsContextQCFBindings.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserSessionMode_null_B_SecOff failed");

    }

    public void testcreateContextwithUser_empty_TCP_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "";
        String password = "";

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(userName, password);

        if (!(jmsContextQCFTCP.getAutoStart() == true)
            && (jmsContextQCFTCP.getSessionMode() == jmsContextQCFTCP.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_empty_TCP_SecOff failed");

    }

    public void testcreateContextwithUserSessionMode_empty_TCP_SecOff(
                                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = "";
        String password = "";

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(userName, password);

        if (!(jmsContextQCFTCP.getAutoStart() == true
        && jmsContextQCFTCP.getSessionMode() == jmsContextQCFTCP.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserSessionMode_empty_TCP_SecOff failed");

    }

    public void testcreateContextwithUser_null_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = null;
        String password = null;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(userName, password);

        if (!(jmsContextQCFTCP.getAutoStart() == true)
            && (jmsContextQCFTCP.getSessionMode() == jmsContextQCFTCP.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_null_TCP_SecOff failed");

    }

    public void testcreateContextwithUserSessionMode_null_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = null;
        String password = null;

        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(userName, password, smode);

        if (!(jmsContextQCFTCP.getAutoStart() == true
        && jmsContextQCFTCP.getSessionMode() == jmsContextQCFTCP.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserSessionMode_null_TCP_SecOff failed");

    }

    public void testcreateContextwithUser_TCP_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(userName, password);

        if (!(jmsContextQCFTCP.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE) && (jmsContextQCFTCP.getAutoStart() == true))

            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUser_B_SecOff failed");

    }

    public void testcreateContextwithsessionMode_B_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        int smode = JMSContext.DUPS_OK_ACKNOWLEDGE;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(smode);

        if (!(jmsContextQCFBindings.getSessionMode() == smode && jmsContextQCFBindings.getAutoStart() == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithsessionMode_B_SecOff failed");

    }

    public void testcreateContextwithsessionMode_TCP_SecOff(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        int smode = JMSContext.DUPS_OK_ACKNOWLEDGE;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(smode);

        if (!(jmsContextQCFTCP.getSessionMode() == smode && jmsContextQCFTCP.getAutoStart() == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithsessionMode_TCP_SecOff failed");

    }

    public void testcreateContextwithInvalidsessionMode_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        int smode = JMSContext.CLIENT_ACKNOWLEDGE;

        JMSContext jmsContextQCFBindings = null;
        try {
            jmsContextQCFBindings = QCFBindings.createContext(smode);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testcreateContextwithInvalidsessionMode_B_SecOff failed: Expected exception was not thrown.");
    }

    public void testcreateContextwithNegsessionMode_B_SecOff(HttpServletRequest request,
                                                             HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        int smode = -1;
        int smod = 10000;
        boolean val = false;
        boolean val1 = false;

        try {
            QCFBindings.createContext(smode);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val = true;
        }

        try {
            QCFBindings.createContext(smod);
        } catch (JMSRuntimeException ex)
        {
            ex.printStackTrace();

            val1 = true;
        }
        System.out.println("Val:" + val);
        System.out.println("Val1:" + val1);
        if (!(val == true && val1 == true))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testcreateContextwithNegsessionMode_B_SecOff failed");

    }

    public void testcreateContextwithInvalidsessionMode_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        int smode = JMSContext.CLIENT_ACKNOWLEDGE;

        try {
            JMSContext jmsContextQCFTCP = QCFTCP.createContext(smode);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }
        if (exceptionFlag == false)
            throw new WrongException("testcreateContextwithInvalidsessionMode_TCP_SecOff failed: Expected exception was not thrown.");
    }

    public void testcreateContextwithNegsessionMode_TCP_SecOff(
                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        int smode = -1;
        int smod = 10000;
        boolean val = false;
        boolean val1 = false;

        try {
            QCFTCP.createContext(smode);
        } catch (JMSRuntimeException ex) {

            val = true;
        }
        try {
            QCFTCP.createContext(smod);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val1 = true;
        }
        System.out.println("Val:" + val);
        System.out.println("Val1:" + val1);
        if (!(val == true && val1 == true))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testcreateContextwithNegsessionMode_TCP_SecOff failed");

    }

    public void testcreateContextwithUserNegsessionMode_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        int smode = -1;
        int smod = 10000;
        String userName = "user1";
        String password = "user1pwd";

        boolean val = false;
        boolean val1 = false;

        try {
            QCFBindings.createContext(userName, password, smode);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val = true;
        }

        try {
            TCFBindings.createContext(userName, password, smod);
        } catch (JMSRuntimeException ex) {

            val1 = true;
        }
        System.out.println("Val:" + val);
        System.out.println("Val1:" + val1);
        if (!(val == true && val1 == true))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserNegsessionMode_B_SecOff failed");

    }

    public void testcreateContextwithUserNegsessionMode_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        int smode = -1;
        int smod = 10000;
        String userName = "user1";
        String password = "user1pwd";

        boolean val = false;
        boolean val1 = false;

        try {
            QCFTCP.createContext(userName, password, smode);

        } catch (JMSRuntimeException ex)
        {

            ex.printStackTrace();
            val = true;
        }

        try {
            TCFTCP.createContext(userName, password, smod);
        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            val1 = true;
        }
        System.out.println("Val:" + val);
        System.out.println("Val1:" + val1);
        if (!(val == true && val1 == true))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserNegsessionMode_TCP_SecOff failed");

    }

    public void testcreateContextwithUserSessionMode_B_SecOff(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(userName, password,
                                                                     smode);

        if (!(jmsContextQCFBindings.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE && jmsContextQCFBindings.getAutoStart() == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserSessionMode_B_SecOff failed");

    }

    public void testcreateContextwithUserSessionMode_TCP_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(userName, password,
                                                           smode);

        if (!(jmsContextQCFTCP.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE && jmsContextQCFTCP.getAutoStart() == true))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateContextwithUserSessionMode_TCP_SecOff failed");

    }

    public void testautoStart_B_SecOff(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        if (!(jmsContextQCFBindings.getAutoStart() == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testautoStart_B_SecOff failed");

    }

    public void testSetGetAutoStart_createContextwithUser_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        boolean val = false;
        boolean val1 = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext(userName, password);
        jmsContextQCFBindings.setAutoStart(false);

        if (jmsContextQCFBindings.getAutoStart() == false)
            val = true;

        jmsContextQCFBindings.setAutoStart(true);

        if (jmsContextQCFBindings.getAutoStart() == true)
            val1 = true;

        if (!(val == true && val1 == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testSetGetAutoStart_createContextwithUser_B_SecOff failed");

    }

    public void testSetGetAutoStart_createContextwithUser_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        boolean val = false;
        boolean val1 = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext(userName, password);
        jmsContextQCFTCP.setAutoStart(false);

        if (jmsContextQCFTCP.getAutoStart() == false)
            val = true;

        jmsContextQCFTCP.setAutoStart(true);

        if (jmsContextQCFTCP.getAutoStart() == true)
            val1 = true;

        if (!(val == true && val1 == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetGetAutoStart_createContextwithUser_TCP_SecOff failed");

    }

    public void testautoStart_TCP_SecOff(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        if (!(jmsContextQCFTCP.getAutoStart() == true))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testautoStart_TCP_SecOff failed");

    }

    public void testGetSessionMode_B_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        if (!(jmsContextQCFBindings.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetSessionMode_B_SecOff failed");

    }

    public void testGetSessionMode_TCP_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        if (!(jmsContextQCFTCP.getSessionMode() == JMSContext.AUTO_ACKNOWLEDGE))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testGetSessionMode_TCP_SecOff failed");

    }

    public void testClose_B_SecOff(HttpServletRequest request,
                                   HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        jmsContextQCFBindings.close();

        if ((exceptionFlag))
            throw new WrongException("testClose_B_SecOff failed.");

    }

    public void testClose_TCP_SecOff(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        jmsContextQCFTCP.close();

        if ((exceptionFlag))
            throw new WrongException("testClose_TCP_SecOff failed.");

    }

    public void testcreateContextfromJMSContext_B_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        try {
            jmsContextQCFBindings.createContext(smode);

        } catch (JMSRuntimeException ex3) {
            ex3.printStackTrace();
            exceptionFlag = true;

        }

        if (exceptionFlag == false)
            throw new WrongException("testcreateContextfromJMSContext_B_SecOff failed:Expected exception was not thrown.");

    }

    public void testcreateContextfromJMSContext_TCP_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();;
        try {
            jmsContextQCFTCP.createContext(smode);

        } catch (JMSRuntimeException ex3) {
            ex3.printStackTrace();
            exceptionFlag = true;

        }

        if (exceptionFlag == false)
            throw new WrongException("testcreateContextfromJMSContext_TCP_SecOff failed:Expected exception was not thrown.");

    }

    public void testsetClientID_B_SecOff(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();
        try {
            jmsContextTCFBindings.setClientID("TestID");

        } catch (IllegalStateRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextTCFBindings.close();
        if (exceptionFlag == false)
            throw new WrongException("testsetClientID_B_SecOff failed:Expected exception was not thrown.");

    }

    public void testsetClientID_TCP_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();
        try {
            jmsContextTCFTCP.setClientID("TestID");

        } catch (IllegalStateRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextTCFTCP.close();
        if (exceptionFlag == false)
            throw new WrongException("testsetClientID_TCP_SecOff failed:Expected exception was not thrown.");

    }

    public void testsetClientID_createContextUser_B_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = true;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextTCFBindings = TCFBindings.createContext(userName, password);
        try {
            jmsContextTCFBindings.setClientID("TestID");

        } catch (IllegalStateRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextTCFBindings.close();
        if (exceptionFlag == false)
            throw new WrongException("testsetClientID_createContextUser_B_SecOff failed:Expected exception was not thrown.");

    }

    public void testsetClientID_createContextUser_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = true;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextTCFTCP = TCFTCP.createContext(userName, password);
        try {
            jmsContextTCFTCP.setClientID("TestID");

        } catch (IllegalStateRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextTCFTCP.close();
        if (exceptionFlag == false)
            throw new WrongException("testsetClientID_createContextUser_TCP_SecOff failed:Expected exception was not thrown.");

    }

    public void testGetClientID_B_SecOff(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        String cid = null;
        String clid = "clientID";

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();
        cid = jmsContextTCFBindings.getClientID();

        if (!(cid.equals(clid)))
            exceptionFlag = true;

        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetClientID_B_SecOff failed");

    }

    public void testGetClientID_TCP_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        String cid = null;
        String clid = "clientID";

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();
        cid = jmsContextTCFTCP.getClientID();

        if (!(cid.equals(clid)))
            exceptionFlag = true;
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testGetClientID_TCP_SecOff failed");

    }

    public void testGetClientID_createContextUser_B_SecOff(HttpServletRequest request,

                                                           HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        String cid = null;
        String clid = "clientID";
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextTCFBindings = TCFBindings.createContext(userName, password);

        cid = jmsContextTCFBindings.getClientID();

        if (!(cid.equals(clid)))
            exceptionFlag = true;
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetClientID_createContextUser_B_SecOff failed");

    }

    public void testGetClientID_createContextUser_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String cid = null;
        String clid = "clientID";
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextTCFTCP = TCFTCP.createContext(userName, password);

        cid = jmsContextTCFTCP.getClientID();

        if (!(cid.equals(clid)))
            exceptionFlag = true;
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testGetClientID_createContextUser_TCP_SecOff failed");

    }

    public void testConnStartAuto_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();
        JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();
        TextMessage msg = jmsContextTCFBindings.createTextMessage("Hello");

        jmsProducerTCFBindings.send(topic, msg);

        TextMessage m1 = (TextMessage) jmsConsumerTCFBindings.receive(30000);
        if (!(m1.getText().equals("Hello")))
            exceptionFlag = true;

        jmsConsumerTCFBindings.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testConnStartAuto_B_SecOff failed");

    }

    public void testConnStartAuto_TCP_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();
        JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();
        TextMessage msg = jmsContextTCFTCP.createTextMessage("Hello");

        jmsProducerTCFTCP.send(topic, msg);

        TextMessage m1 = (TextMessage) jmsConsumerTCFTCP.receive(30000);
        if (!(m1.getText().equals("Hello")))
            exceptionFlag = true;
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testConnStartAuto_TCP_SecOff failed");

    }

    public void testConnStartAuto_createContextUser_B_SecOff(HttpServletRequest request,
                                                             HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextTCFBindings = TCFBindings.createContext(userName, password);
        JMSConsumer c1 = jmsContextTCFBindings.createConsumer(topic);

        TextMessage msg = jmsContextTCFBindings.createTextMessage("Hello");

        jmsContextTCFBindings.createProducer().send(topic, msg);

        TextMessage m1 = (TextMessage) c1.receive(30000);
        if (!(m1.getText().equals("Hello")))
            exceptionFlag = true;

        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testConnStartAuto_createContextUser_B_SecOff failed");

    }

    public void testConnStartAuto_createContextUser_TCP_SecOff(
                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextTCFTCP = TCFTCP.createContext(userName, password);
        JMSConsumer c1 = jmsContextTCFTCP.createConsumer(topic);

        TextMessage msg = jmsContextTCFTCP.createTextMessage("Hello");

        jmsContextTCFTCP.createProducer().send(topic, msg);

        TextMessage m1 = (TextMessage) c1.receive(30000);

        if (m1 == null)
            exceptionFlag = true;
        else {
            if ((!(m1.getText().equals("Hello"))))
                exceptionFlag = true;
        }

        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testConnStartAuto_createContextUser_TCP_SecOff failed, message received is :" + m1);

    }

    public void testConnStartAuto_createContextUserSessionMode_B_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextT = TCFBindings.createContext(userName, password,
                                                           smode);
        JMSConsumer jmsConsumer = jmsContextT.createConsumer(topic);

        TextMessage msg = jmsContextT.createTextMessage("Hello");

        jmsContextT.createProducer().send(topic, msg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if (!(message.getText().equals("Hello")))
            exceptionFlag = true;
        jmsContextT.close();
        if (exceptionFlag)
            throw new WrongException("testConnStartAuto_createContextUserSessionMode_B_SecOff failed");

    }

    public void testConnStartAuto_createContextUserSessionMode_TCP_SecOff(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        boolean exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;

        JMSContext jmsContextT = TCFTCP.createContext(userName, password,
                                                      smode);
        JMSConsumer jmsConsumer = jmsContextT.createConsumer(topic);

        TextMessage msg = jmsContextT.createTextMessage("Hello");

        jmsContextT.createProducer().send(topic, msg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if (!(message.getText().equals("Hello")))
            exceptionFlag = true;

        jmsContextT.close();
        if (exceptionFlag)
            throw new WrongException("testConnStartAuto_createContextUserSessionMode_TCP_SecOff failed");

    }

    // 118061_1 Verify creation of message from JMSContext. createMessage()

    public void testCreateMessage_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        Message msg = jmsContextQCFBindings.createMessage();

        jmsContextQCFBindings.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateMessage_B_SecOff failed");

    }

    public void testCreateMessage_TCP_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        emptyQueue(QCFTCP, queue);

        Message msg = jmsContextQCFTCP.createMessage();

        jmsContextQCFTCP.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateMessage_TCP_SecOff failed");

    }

    public void testCreateObjectMessage_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        ObjectMessage msg = jmsContextQCFBindings.createObjectMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setObject(new StockObject("TestStock", 1234.5));
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        jmsContextQCFBindings.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateObjectMessage_B_SecOff failed");

    }

    public void testCreateObjectMessage_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        ObjectMessage msg = jmsContextQCFTCP.createObjectMessage();
        msg.setBooleanProperty("BooleanValue", true);
        msg.setObject(new StockObject("TestStock", 1234.5));
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        jmsContextQCFTCP.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateObjectMessage_TCP_SecOff failed");

    }

    public void testCreateObjectMessageSer_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        ObjectMessage msg = jmsContextQCFBindings.createObjectMessage(new StockObject("TEST STOCK", 134.567));
        msg.setBooleanProperty("BooleanValue", true);

        msg.getObject();
        msg.getBody(StockObject.class);

        jmsContextQCFBindings.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateObjectMessageSer_B_SecOff failed");

    }

    public void testCreateObjectMessageSer_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        ObjectMessage msg = jmsContextQCFTCP.createObjectMessage(new StockObject("TEST STOCK", 134.567));
        msg.setBooleanProperty("BooleanValue", true);

        msg.getObject();
        msg.getBody(StockObject.class);

        jmsContextQCFTCP.createProducer().send(queue, msg);
        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateObjectMessageSer_TCP_SecOff failed");

    }

    // 118061_4 Verify creation of Stream Message from
    // JMSContext.createStreamMessage(), Perform operation for setdata and
    // reading data.

    public void testCreateStreamMessage_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        StreamMessage msg = jmsContextQCFBindings.createStreamMessage();

        msg.setBooleanProperty("BooleanValue", true);

        msg.writeBoolean(true);
        msg.writeString("Test case to create a Stream Message");

        msg.reset();

        jmsContextQCFBindings.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        StreamMessage msg1 = (StreamMessage) jmsContextQCFBindings.createConsumer(queue)
                        .receive(30000);

        System.out.println(msg1.readString());
        System.out.println(msg1.readBoolean());

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateStreamMessage_B_SecOff failed");

    }

    public void testCreateStreamMessage_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        StreamMessage msg = jmsContextQCFTCP.createStreamMessage();

        msg.setBooleanProperty("BooleanValue", true);

        msg.writeBoolean(true);
        msg.writeString("Test case to create a Stream Message");

        msg.reset();

        jmsContextQCFTCP.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        StreamMessage msg1 = (StreamMessage) jmsContextQCFTCP.createConsumer(queue)
                        .receive(30000);

        System.out.println(msg1.readString());
        System.out.println(msg1.readBoolean());

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateStreamMessage_TCP_SecOff failed");

    }

    // 118061_5 Verify creation of Text Message from
    // JMSContext.createTextMessage().Perform setText and getTest operations.

    public void testCreateTextMessage_B_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;

        String compare = "Hello this is a test case for TextMessage ";
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        TextMessage msg = jmsContextQCFBindings.createTextMessage();

        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(compare);
        if (msg.getText() == compare)
            flag = true;

        jmsContextQCFBindings.createProducer().send(queue, msg);
        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (numMsgs == 1)
            val = true;

        if (!(flag == true && val == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateTextMessage_B_SecOff failed");

    }

    public void testCreateTextMessage_TCP_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;

        String compare = "Hello this is a test case for TextMessage ";
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        TextMessage msg = jmsContextQCFTCP.createTextMessage();

        msg.setBooleanProperty("BooleanValue", true);
        msg.setText(compare);
        if (msg.getText() == compare)
            flag = true;

        jmsContextQCFTCP.createProducer().send(queue, msg);
        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (numMsgs == 1)
            val = true;

        if (!(flag == true && val == true))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateTextMessage_TCP_SecOff failed");

    }

    // 118061_6 Verify creation of Text Message from
    // JMSContext.createTextMessage(String text).

    public void testCreateTextMessageStr_B_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean comp = false;
        String compare = "Hello";
        String str = "Hello this is a test case for TextMessage";
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        TextMessage msg = jmsContextQCFBindings.createTextMessage(str);

        msg.setBooleanProperty("BooleanValue", true);

        msg.setText(compare);
        if (msg.getText() == compare)
            flag = true;

        jmsContextQCFBindings.createProducer().send(queue, msg);
        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (numMsgs == 1)
            comp = true;

        if (!(flag == true && comp == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateTextMessageStr_B_SecOff failed");

    }

    public void testCreateTextMessageStr_TCP_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean comp = false;
        String compare = "Hello";
        String str = "Hello this is a test case for TextMessage";
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        TextMessage msg = jmsContextQCFTCP.createTextMessage(str);

        msg.setBooleanProperty("BooleanValue", true);

        msg.setText(compare);
        if (msg.getText() == compare)
            flag = true;

        jmsContextQCFTCP.createProducer().send(queue, msg);
        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (numMsgs == 1)
            comp = true;

        if (!(flag == true && comp == true))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateTextMessageStr_TCP_SecOff failed");

    }

    // 118061_7 Verify creation of Map Message from
    // JMSContext.createMapMessage() .Perform set and get operation.
    public void testCreateMapMessage_B_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean comp = false;

        String valname = "Valuepair";
        long val1 = 22222222;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        MapMessage msg = jmsContextQCFBindings.createMapMessage();
        msg.setLong(valname, val1);
        if (msg.getLong(valname) == val1)
            flag = true;

        jmsContextQCFBindings.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (numMsgs == 1)
            comp = true;

        if (!(flag == true && comp == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateMapMessage_B_SecOff failed");

    }

    public void testCreateMapMessage_TCP_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean comp = false;

        String valname = "Valuepair";
        long val1 = 22222222;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        MapMessage msg = jmsContextQCFTCP.createMapMessage();
        msg.setLong(valname, val1);
        if (msg.getLong(valname) == val1)
            flag = true;

        jmsContextQCFTCP.createProducer().send(queue, msg);

        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (numMsgs == 1)
            comp = true;

        if (!(flag == true && comp == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateMapMessage_TCP_SecOff failed");

    }

    // 118061_8 Verify creation of ByteMessage from
    // JMSContext.createBytesMessage(). Peform writeBytes, readBytes and getBody
    // operation.

    public void testCreateBytesMessage_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        byte[] content = "test".getBytes();

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);

        BytesMessage msg = jmsContextQCFBindings.createBytesMessage();
        msg.writeBytes(content);

        msg.reset();
        System.out.print(msg.readBytes(content));
        System.out.print(msg.getBodyLength());

        jmsContextQCFBindings.createProducer().send(queue, msg);
        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        Message recv = jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testCreateBytesMessage_B_SecOff failed");

    }

    public void testCreateBytesMessage_TCP_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        byte[] content = "test".getBytes();

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        BytesMessage msg = jmsContextQCFTCP.createBytesMessage();
        msg.writeBytes(content);

        msg.reset();
        System.out.print(msg.readBytes(content));
        System.out.print(msg.getBodyLength());

        jmsContextQCFTCP.createProducer().send(queue, msg);
        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);
        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        Message recv = jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testCreateBytesMessage_B_SecOff failed");

    }

    // 118061_9 Test with JMSDestination - setJMSDestination and
    // getJMSDestination

    public void testJMSDestination_B_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        Message tmsg = jmsContextQCFBindings.createMessage();

        tmsg.setJMSDestination(queue1);

        jmsContextQCFBindings.createProducer().send(queue, tmsg);

        Message message = jmsContextQCFBindings.createConsumer(queue).receive(30000);

        Destination queueAfterReceive = message.getJMSDestination();

        String qrecv = queueAfterReceive.toString();

        if (!(qrecv.equalsIgnoreCase("queue://QUEUE1")))

            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSDestination_B_SecOff failed");

    }

    // 118061_9 Test with JMSDestination - setJMSDestination and
    // getJMSDestination

    public void testJMSDestination_TCP_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        Message tmsg = jmsContextQCFTCP.createMessage();

        tmsg.setJMSDestination(queue1);

        jmsContextQCFTCP.createProducer().send(queue, tmsg);

        Message message = jmsContextQCFTCP.createConsumer(queue).receive(30000);

        Destination queueAfterReceive = message.getJMSDestination();

        String qrecv = queueAfterReceive.toString();

        if (!(qrecv.equalsIgnoreCase("queue://QUEUE1")))

            exceptionFlag = true;

        jmsContextQCFTCP.createConsumer(queue).close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSDestination_TCP_SecOff failed");

    }

    public void testJMSDeliveryMode_B_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage("Hello");

        tmsg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);

        jmsContextQCFBindings.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(queue, tmsg);

        int dmodeAfter = jmsContextQCFBindings.createConsumer(queue).receive(30000).getJMSDeliveryMode();

        if (!(dmodeAfter == DeliveryMode.NON_PERSISTENT))
            exceptionFlag = true;

        jmsContextQCFBindings.createConsumer(queue).close();
        jmsContextQCFBindings.close();

        if ((exceptionFlag))
            throw new WrongException("testJMSDeliveryMode_B_SecOff failed");

    }

    public void testJMSDeliveryMode_TCP_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage("Hello");

        tmsg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);

        jmsContextQCFTCP.createProducer().setDeliveryMode(DeliveryMode.NON_PERSISTENT).send(queue, tmsg);

        int dmodeAfter = jmsContextQCFTCP.createConsumer(queue).receive(30000).getJMSDeliveryMode();
        System.out.println("---dmodeAfter--" + dmodeAfter);

        if (!(dmodeAfter == DeliveryMode.NON_PERSISTENT))
            exceptionFlag = true;

        jmsContextQCFTCP.createConsumer(queue).close();
        jmsContextQCFTCP.close();
        if ((exceptionFlag))
            throw new WrongException("testJMSDeliveryMode_TCP_SecOff failed");

    }

    // 118061_11 Verify set and get operation on Message header field
    // JMSMessageID

    // 118061_11 Test with JMSMessageID - setJMSMessageID ,getJMSMessageID and
    // setDisableMessageID

    public void testJMSMessageID_B_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;
        String msg = "Hello this is a test case for TextMessage ";

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        TextMessage tmsg = jmsContextQCFBindings.createTextMessage(msg);

        tmsg.setJMSMessageID("MSGID");

        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();

        jmsProducerQCFBindings.send(queue, tmsg);

        String msgid = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSMessageID();

        if (msgid != "MSGID" && msgid.startsWith("ID:"))
            flag = true;

        TextMessage tmessage = jmsContextQCFBindings.createTextMessage(msg);

        jmsProducerQCFBindings.setDisableMessageID(true).send(queue, tmessage);

        msgid = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSMessageID();

        if (msgid == null)
            val = true;

        if (!(flag == true && val == true))
            exceptionFlag = true;

        jmsContextQCFBindings.createConsumer(queue).close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSMessageID_B_SecOff failed");

    }

    public void testJMSMessageID_TCP_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;
        String msg = "Hello this is a test case for TextMessage ";

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage(msg);

        tmsg.setJMSMessageID("MSGID");

        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

        jmsProducerQCFTCP.send(queue, tmsg);

        String msgid = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSMessageID();
        System.out.println("1 ---------msgid---------" + msgid);
        System.out.println("2 ---------msgid startswith---------" + msgid.startsWith("ID:"));

        if (msgid != "MSGID" && msgid.startsWith("ID:"))
            flag = true;

        TextMessage tmessage = jmsContextQCFTCP.createTextMessage(msg);

        jmsProducerQCFTCP.setDisableMessageID(true).send(queue, tmessage);

        msgid = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSMessageID();
        System.out.println("1 ---------msgid---------" + msgid);

        if (msgid == null)
            val = true;

        System.out.println("1 ---------flag---------" + flag);
        System.out.println("2 ---------val---------" + val);
        if (!(flag == true && val == true))
            exceptionFlag = true;

        jmsContextQCFTCP.createConsumer(queue).close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSMessageID_TCP_SecOff failed");

    }

    // 118061_12 Verify set and get operation on Message header field
    // JMSTimeStamp

    public void testJMSTimestamp_B_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;
        String msg = "Hello this is a test case for TextMessage ";
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage(msg);

        tmsg.setJMSTimestamp(1234567);

        long beforeSend = System.currentTimeMillis();
        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
        jmsProducerQCFBindings.send(queue, tmsg);

        long afterSend = System.currentTimeMillis();

        long gts = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSTimestamp();

        if ((gts >= beforeSend) && (gts <= afterSend) && (gts != 1234567))
            flag = true;

        jmsProducerQCFBindings.setDisableMessageTimestamp(true);
        jmsProducerQCFBindings.send(queue, tmsg);

        long dts = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSTimestamp();

        if (dts == 0)
            val = true;

        if (!(flag == true && val == true))
            exceptionFlag = true;
        jmsContextQCFBindings.createConsumer(queue).close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSTimestamp_B_SecOff failed");

    }

    // 118061_12 Verify set and get operation on Message header field
    // JMSTimeStamp

    public void testJMSTimestamp_TCP_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;
        String msg = "Hello this is a test case for TextMessage ";
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage(msg);

        tmsg.setJMSTimestamp(1234567);

        long beforeSend = System.currentTimeMillis();
        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
        jmsProducerQCFTCP.send(queue, tmsg);

        long afterSend = System.currentTimeMillis();

        long gts = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSTimestamp();

        if ((gts >= beforeSend) && (gts <= afterSend) && (gts != 1234567))
            flag = true;

        jmsProducerQCFTCP.setDisableMessageTimestamp(true);
        jmsProducerQCFTCP.send(queue, tmsg);

        long dts = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSTimestamp();

        if (dts == 0)
            val = true;

        if (!(flag == true && val == true))
            exceptionFlag = true;

        jmsContextQCFTCP.createConsumer(queue).close();
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSTimestamp_TCP_SecOff failed");

    }

    // 118061_13 Test with JMSCorrelationID- setJMSCorrelationID
    // ,getJMSCorrelationID

    public void testJMSCorrelationID_B_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        boolean val = false;
        boolean val1 = false;
        String correl = "MyCorrelID";
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        String startCorrel = messageQCFBindings.getJMSCorrelationID();

        if (startCorrel == null) {

            messageQCFBindings.setJMSCorrelationID(correl);
            String got = messageQCFBindings.getJMSCorrelationID();

            if (correl.equals(got))
                val = true;
        }

        jmsContextQCFBindings.createProducer().send(queue, messageQCFBindings);

        String afterRecv = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSCorrelationID();

        if (afterRecv.equals(correl))
            val1 = true;

        if (!(val == true && val1 == true))
            exceptionFlag = true;

        jmsContextQCFBindings.createConsumer(queue).close();
        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSCorrelationID_B_SecOff failed");

    }

    // 118061_13 Test with JMSCorrelationID- setJMSCorrelationID
    // ,getJMSCorrelationID

    public void testJMSCorrelationID_TCP_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        boolean val = false;
        boolean val1 = false;
        String correl = "MyCorrelID";
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        String startCorrel = messageQCFTCP.getJMSCorrelationID();
        System.out.println("1 ------startCorrel ------" + startCorrel);
        System.out.println("2 ------correl ------" + correl);

        if (startCorrel == null) {

            messageQCFTCP.setJMSCorrelationID(correl);
            String got = messageQCFTCP.getJMSCorrelationID();
            System.out.println("3 ------got ------" + got);
            if (correl.equals(got))
                val = true;
        }
        System.out.println("4 ------messageQCFTCP ------" + messageQCFTCP);

        jmsContextQCFTCP.createProducer().send(queue, messageQCFTCP);

        String afterRecv = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSCorrelationID();
        System.out.println("4 ------afterRecv ------" + afterRecv);

        if (afterRecv.equals(correl))
            val1 = true;

        if (!(val == true && val1 == true))
            exceptionFlag = true;

        jmsContextQCFTCP.createConsumer(queue).close();
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSCorrelationID_B_SecOff failed");

    }

    // 118061_14 Test with JMSCorrelationID- setJMSCorrelationIDAsbytes
    // ,getJMSCorrelationIDAsBytes
    public void testJMSCorrelationIDAsBytes_B_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);

        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        byte[] defBytes = messageQCFBindings.getJMSCorrelationIDAsBytes();

        String startCorrel = messageQCFBindings.getJMSCorrelationID();

        // set and retrieve a byte[]
        byte[] testA = { 1, 2, 3, 4 };
        messageQCFBindings.setJMSCorrelationIDAsBytes(testA);

        byte[] resultA = messageQCFBindings.getJMSCorrelationIDAsBytes();

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
        messageQCFBindings.setJMSCorrelationIDAsBytes(testA);
        messageQCFBindings.setJMSCorrelationID(correl);
        byte[] resultE = messageQCFBindings.getJMSCorrelationIDAsBytes();

        // b) set string then bytes
        messageQCFBindings.setJMSCorrelationID(correl);
        messageQCFBindings.setJMSCorrelationIDAsBytes(testA);
        byte[] resultF = messageQCFBindings.getJMSCorrelationIDAsBytes();

        boolean testComp1 = false;
        for (int j = 0; j < testA.length; j++) {
            if (testA[j] != resultF[j]) {
                testComp = false;
            } else
                testComp = true;
        }

        if (resultE != null && testComp == true && testComp1 == true)

            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSCorrelationIDAsBytes_B_SecOff failed");

    }

    // 118061_14 Test with JMSCorrelationID- setJMSCorrelationIDAsbytes
    // ,getJMSCorrelationIDAsBytes
    public void testJMSCorrelationIDAsBytes_TCP_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        byte[] defBytes = messageQCFTCP.getJMSCorrelationIDAsBytes();

        String startCorrel = messageQCFTCP.getJMSCorrelationID();

        // set and retrieve a byte[]
        byte[] testA = { 1, 2, 3, 4 };
        messageQCFTCP.setJMSCorrelationIDAsBytes(testA);

        byte[] resultA = messageQCFTCP.getJMSCorrelationIDAsBytes();

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
        messageQCFTCP.setJMSCorrelationIDAsBytes(testA);
        messageQCFTCP.setJMSCorrelationID(correl);
        byte[] resultE = messageQCFTCP.getJMSCorrelationIDAsBytes();

        // b) set string then bytes
        messageQCFTCP.setJMSCorrelationID(correl);
        messageQCFTCP.setJMSCorrelationIDAsBytes(testA);
        byte[] resultF = messageQCFTCP.getJMSCorrelationIDAsBytes();

        boolean testComp1 = false;
        for (int j = 0; j < testA.length; j++) {
            if (testA[j] != resultF[j]) {
                testComp = false;
            } else
                testComp = true;
        }

        if (resultE != null && testComp == true && testComp1 == true)

            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSCorrelationIDAsBytes_TCP_SecOff failed");

    }

    public void testJMSReplyTo_B_SecOff(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        boolean val = false;
        boolean val1 = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        messageQCFBindings.setJMSReplyTo(queue);
        Queue gotReplyQueue = (Queue) messageQCFBindings.getJMSReplyTo();

        messageQCFBindings.setJMSReplyTo(topic);
        Topic gotReplyTopic = (Topic) messageQCFBindings.getJMSReplyTo();

        if (queue == gotReplyQueue && topic == gotReplyTopic)
            val = true;

        // now try setting a null destination and check the value stays null
        messageQCFBindings.setJMSReplyTo(null);
        if (messageQCFBindings.getJMSReplyTo() == null)
            val1 = true;

        if (!(val1 == true && val1 == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSReplyTo_B_SecOff failed");

    }

    public void testJMSReplyTo_TCP_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        boolean val = false;
        boolean val1 = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        messageQCFTCP.setJMSReplyTo(queue);
        Queue gotReplyQueue = (Queue) messageQCFTCP.getJMSReplyTo();

        messageQCFTCP.setJMSReplyTo(topic);
        Topic gotReplyTopic = (Topic) messageQCFTCP.getJMSReplyTo();

        if (queue == gotReplyQueue && topic == gotReplyTopic)
            val = true;

        // now try setting a null destination and check the value stays null
        messageQCFTCP.setJMSReplyTo(null);
        if (messageQCFTCP.getJMSReplyTo() == null)
            val1 = true;

        if (!(val1 == true && val1 == true))
            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSReplyTo_B_SecOff failed");

    }

    // 118061_16 Test with JMSRedelivered- setJMSRedelivered and
    // getJMSRedelivered

    public void testJMSRedelivered_B_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        if (messageQCFBindings.getJMSRedelivered() == true) {
            messageQCFBindings.setJMSRedelivered(false);
            jmsContextQCFBindings.createProducer().send(queue, messageQCFBindings);

            Message msgrecv = jmsContextQCFBindings.createConsumer(queue).receive(30000);

            if (!(msgrecv.getJMSRedelivered() != false))
                exceptionFlag = true;

        }

        else {

            messageQCFBindings.setJMSRedelivered(true);

            jmsContextQCFBindings.createProducer().send(queue, messageQCFBindings);

            Message msgrecv = jmsContextQCFBindings.createConsumer(queue).receive(30000);

            if (!(msgrecv.getJMSRedelivered() != true))
                exceptionFlag = true;

        }

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSRedelivered_B_SecOff failed");

    }

    public void testJMSRedelivered_TCP_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        if (messageQCFTCP.getJMSRedelivered() == true) {
            messageQCFTCP.setJMSRedelivered(false);
            jmsContextQCFTCP.createProducer().send(queue, messageQCFTCP);

            Message msgrecv = jmsContextQCFTCP.createConsumer(queue).receive(30000);

            if (!(msgrecv.getJMSRedelivered() != false))
                exceptionFlag = true;

        }

        else {

            messageQCFTCP.setJMSRedelivered(true);

            jmsContextQCFTCP.createProducer().send(queue, messageQCFTCP);

            Message msgrecv = jmsContextQCFTCP.createConsumer(queue).receive(30000);

            if (!(msgrecv.getJMSRedelivered() != true))
                exceptionFlag = true;

        }

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSRedelivered_B_SecOff failed");

    }

    // 118061_17 Verify set and get operation on Message header field JMSType

    public void testJMSType_B_SecOff(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;
        String type1 = "type 1";
        String type2 = "type 2";

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        messageQCFBindings.setJMSType(type1);

        jmsContextQCFBindings.createProducer().send(queue, messageQCFBindings);

        String t1 = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSType();

        messageQCFBindings.setJMSType(type2);

        jmsContextQCFBindings.createProducer().send(queue, messageQCFBindings);

        String t2 = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSType();

        if (!(t1.equals(type1) && t2.equals(type2)))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSType_B_secOff failed");

    }

    // 118061_17 Test with JMSType- setJMSType and getJMSType

    public void testJMSType_TCP_SecOff(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;
        String type1 = "type 1";
        String type2 = "type 2";

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        messageQCFTCP.setJMSType(type1);
        System.out.println("1 --------messageQCFTCP -------" + messageQCFTCP);

        jmsContextQCFTCP.createProducer().send(queue, messageQCFTCP);

        String t1 = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSType();
        System.out.println("2 -------t1 -------" + t1);

        messageQCFTCP.setJMSType(type2);

        jmsContextQCFTCP.createProducer().send(queue, messageQCFTCP);

        String t2 = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSType();
        System.out.println("3 --------t2 -------" + t2);

        if (!(t1.equals(type1) && t2.equals(type2)))
            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSType_TCP_secOff failed");

    }

    // 118061_18 Verify set and get operation on Message header field
    // JMSExpiration

    // 118061_18 Test with JMSExpiration- setJMSExpiration and getJMSExpiration

    public void testJMSExpiration_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        Message messageQCFBindings = jmsContextQCFBindings.createMessage();
        long defexpValue = messageQCFBindings.getJMSExpiration();

        messageQCFBindings.setJMSExpiration(1);

        jmsContextQCFBindings.createProducer().send(queue, messageQCFBindings);
        long expValue = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSExpiration();

        if (!(expValue == 0) && (defexpValue == 0))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSExpiration_B_SecOff failed");

    }

    public void testJMSExpiration_TCP_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        Message messageQCFTCP = jmsContextQCFTCP.createMessage();
        long defexpValue = messageQCFTCP.getJMSExpiration();

        messageQCFTCP.setJMSExpiration(1);

        jmsContextQCFTCP.createProducer().send(queue, messageQCFTCP);
        long expValue = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSExpiration();

        if (!(expValue == 0) && (defexpValue == 0))
            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSExpiration_TCP_SecOff failed");
        // jmsContextQCFTCP.createConsumer(queue).close();

    }

    // 118061_19 Test with JMSPriority- setJMSPriority and getJMSPriority

    public void testJMSPriority_B_SecOff(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        Message messageQCFBindings = jmsContextQCFBindings.createMessage();

        messageQCFBindings.setJMSPriority(9);

        jmsContextQCFBindings.createProducer().setPriority(1).send(queue, messageQCFBindings);
        int pri = jmsContextQCFBindings.createConsumer(queue).receive(30000)
                        .getJMSPriority();

        if (!(pri == 1))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSPriority_B_SecOff failed");
        // jmsContextQCFBindings.createConsumer(queue).close();

    }

    public void testJMSPriority_TCP_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable

    {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        Message messageQCFTCP = jmsContextQCFTCP.createMessage();

        messageQCFTCP.setJMSPriority(9);

        jmsContextQCFTCP.createProducer().setPriority(1).send(queue, messageQCFTCP);
        int pri = jmsContextQCFTCP.createConsumer(queue).receive(30000)
                        .getJMSPriority();

        System.out.println("--1 -- pri-----" + pri);

        if (!(pri == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSPriority_TCP_SecOff failed");

    }

    // 118061_20 Verify set and get operation on Message header field
    // JMSDeliveryTime

    public void testJMSDeliveryTime_B_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;
        long toSet = 12345;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        Message messageQCFBindings = jmsContextQCFBindings.createMessage();
        // long beforeSet = messageQCFBindings.getJMSDeliveryTime();
        messageQCFBindings.setJMSDeliveryTime(toSet);

        long afterSet = messageQCFBindings.getJMSDeliveryTime();

        if (!(afterSet == toSet))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSDeliveryTime_B_SecOff failed");

    }

    public void testJMSDeliveryTime_TCP_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable

    {

        boolean exceptionFlag = false;
        long toSet = 12345;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        Message messageQCFTCP = jmsContextQCFTCP.createMessage();
        // long beforeSet = messageQCFTCP.getJMSDeliveryTime();
        messageQCFTCP.setJMSDeliveryTime(toSet);

        long afterSet = messageQCFTCP.getJMSDeliveryTime();

        if (!(afterSet == toSet))
            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSDeliveryTime_TCP_SecOff failed");

    }

    // 118062_1_1 Creates a QueueBrowser object to peek at the messages on the
    // specified queue.

    public void testcreateBrowser_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue2);

        jmsContextQCFBindings.createProducer().send(queue2, "Tester");

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue2);

        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue2).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testcreateBrowser_B_SecOff failed");

    }

    public void testcreateBrowser_TCP_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue2);

        jmsContextQCFTCP.createProducer().send(queue2, "Tester");

        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue2);

        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue2).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testcreateBrowser_TCP_SecOff failed");

    }

    public void testcreateBrowserNEQueue_B_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        //emptyQueue(QCFBindings, queue);
        try {
            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue3);
        } catch (InvalidDestinationRuntimeException ex3) {
            ex3.printStackTrace();
            exceptionFlag = true;
        }

        finally {
            jmsContextQCFBindings.close();
            if (!(exceptionFlag))
                throw new WrongException("testcreateBrowserNEQueue_B_SecOff failed");

        }

    }

    public void testcreateBrowserNEQueue_TCP_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        //emptyQueue(QCFTCP, queue);
        try {
            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue3);
        } catch (InvalidDestinationRuntimeException ex3) {
            ex3.printStackTrace();
            exceptionFlag = true;
        } finally {
            jmsContextQCFTCP.close();
            if (!(exceptionFlag))
                throw new WrongException("testcreateBrowserNEQueue_TCP_SecOff failed");

        }
    }

    public void testcreateBrowser_MessageSelector_InvalidQ_B_SecOff(HttpServletRequest request,
                                                                    HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        try {
            QueueBrowser qb = jmsContextQCFBindings.createBrowser(null, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {
            ex3.printStackTrace();
            exceptionFlag = true;
        } finally {
            jmsContextQCFBindings.close();
            if (!(exceptionFlag))
                throw new WrongException("testcreateBrowser_MessageSelector_InvalidQ_B_SecOff failed");

        }
    }

    public void testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        try {
            QueueBrowser qb = jmsContextQCFTCP.createBrowser(null, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {
            ex3.printStackTrace();
            exceptionFlag = true;
        } finally {

            jmsContextQCFTCP.close();
            if (!(exceptionFlag))
                throw new WrongException("testcreateBrowser_MessageSelector_InvalidQ_TCP_SecOff failed");

        }
    }

    public void testcreateBrowser_MessageSelector_B_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        final int NMSGS = 5;

        //jmsContextQCFBindings.createConsumer(queue).receive(30000);

        emptyQueue(QCFBindings, queue);

        TextMessage msg = jmsContextQCFBindings
                        .createTextMessage("browse selector test message");

        QueueBrowser qb1 = null;
        try {

            qb1 = jmsContextQCFBindings.createBrowser(queue, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {
            ex3.printStackTrace();
        }

        QueueBrowser qb2 = null;

        try {
            qb2 = jmsContextQCFBindings.createBrowser(queue, "colour = 'blue'");
        } catch (InvalidDestinationRuntimeException ex3) {

            ex3.printStackTrace();
        }

        for (int i = 0; i < NMSGS; i++) {
            msg.setStringProperty("colour", "red");
            jmsContextQCFBindings.createProducer().send(queue, msg);

            msg.setStringProperty("colour", "blue");
            jmsContextQCFBindings.createProducer().send(queue, msg);
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

            if (colour1 != null && colour1.equals("red")) {
                System.out.println("browsed a message of the red colour: "
                                   + colour1);

                nMsgRed++;
            } else {

                nWrongRed++;
            }

        }
        System.out.println("correct msgs: " + nMsgRed + ", incorrect msgs: "
                           + nWrongRed);

        int nMsgBlue = 0;
        int nWrongBlue = 0;
        System.out.println("Examine the contents of 2nd enumeration");
        while (e2.hasMoreElements()) {
            TextMessage msg2 = (TextMessage) e2.nextElement();
            String colour2 = msg2.getStringProperty("colour");

            if (colour2 != null && colour2.equals("blue")) {
                System.out.println("browsed a message of the blue colour: "
                                   + colour2);

                nMsgBlue++;

            } else {

                nWrongBlue++;
            }

        }
        System.out.println("correct msgs: " + nMsgBlue + ", incorrect msgs: "
                           + nWrongBlue);

        if (!(nMsgRed == NMSGS && nMsgBlue == NMSGS) && (nWrongRed == 0 || nWrongBlue == 0))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if ((exceptionFlag))
            throw new WrongException("testcreateBrowser_MessageSelector_B_SecOff failed");

    }

    public void testcreateBrowser_MessageSelector_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        final int NMSGS = 5;

        // jmsContextQCFTCP.createConsumer(queue).receive(30000);

        emptyQueue(QCFTCP, queue);
        TextMessage msg = jmsContextQCFTCP
                        .createTextMessage("browse selector test message");

        QueueBrowser qb1 = null;
        try {

            qb1 = jmsContextQCFTCP.createBrowser(queue, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {
            ex3.printStackTrace();
        }

        QueueBrowser qb2 = null;

        try {
            qb2 = jmsContextQCFTCP.createBrowser(queue, "colour = 'blue'");
        } catch (InvalidDestinationRuntimeException ex3) {

            ex3.printStackTrace();
        }

        for (int i = 0; i < NMSGS; i++) {
            msg.setStringProperty("colour", "red");
            jmsContextQCFTCP.createProducer().send(queue, msg);

            msg.setStringProperty("colour", "blue");
            jmsContextQCFTCP.createProducer().send(queue, msg);
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

            if (colour1 != null && colour1.equals("red")) {
                System.out.println("browsed a message of the red colour: "
                                   + colour1);

                nMsgRed++;
            } else {

                nWrongRed++;
            }

        }
        System.out.println("correct msgs: " + nMsgRed + ", incorrect msgs: "
                           + nWrongRed);

        int nMsgBlue = 0;
        int nWrongBlue = 0;
        System.out.println("Examine the contents of 2nd enumeration");
        while (e2.hasMoreElements()) {
            TextMessage msg2 = (TextMessage) e2.nextElement();
            String colour2 = msg2.getStringProperty("colour");

            if (colour2 != null && colour2.equals("blue")) {
                System.out.println("browsed a message of the blue colour: "
                                   + colour2);

                nMsgBlue++;

            } else {

                nWrongBlue++;
            }

        }
        System.out.println("correct msgs: " + nMsgBlue + ", incorrect msgs: "
                           + nWrongBlue);

        if (!(nMsgRed == NMSGS && nMsgBlue == NMSGS) && (nWrongRed == 0 || nWrongBlue == 0))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if ((exceptionFlag))
            throw new WrongException("testcreateBrowser_MessageSelector_TCP_SecOff failed");

    }

    public void testcreateBrowser_MessageSelector_NullQueue_B_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        try {

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(null, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {

            ex3.printStackTrace();
            exceptionFlag = true;
        } finally
        {
            jmsContextQCFBindings.close();
            if (!(exceptionFlag))
                throw new WrongException("testcreateBrowser_MessageSelector_NullQueue_B_SecOff failed");

        }

    }

    public void testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)

                    throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        try {

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(null, "colour = 'red'");
        } catch (InvalidDestinationRuntimeException ex3) {

            ex3.printStackTrace();
            exceptionFlag = true;
        }

        finally {
            jmsContextQCFTCP.close();
            if (!(exceptionFlag))

                throw new WrongException("testcreateBrowser_MessageSelector_NullQueue_TCP_SecOff failed");

        }

    }

    public void testcreateBrowser_MessageSelector_Empty_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        //jmsContextQCFBindings.createConsumer(queue).receive(30000);
        emptyQueue(QCFBindings, queue);

        QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue, "");
        QueueBrowser qb2 = jmsContextQCFBindings.createBrowser(queue, "");
        QueueBrowser qb3 = jmsContextQCFBindings.createBrowser(queue, "");

        TextMessage msg = jmsContextQCFBindings
                        .createTextMessage("browse selector test message");

        for (int i = 0; i < 3; i++) {
            msg.setStringProperty("Role", "Tester");
            jmsContextQCFBindings.createProducer().send(queue, msg);

            msg.setStringProperty("Role", "Developer");
            jmsContextQCFBindings.createProducer().send(queue, msg);

            msg.setStringProperty("Role", "");
            jmsContextQCFBindings.createProducer().send(queue, msg);
        }

        Enumeration e1 = qb1.getEnumeration();

        int testVar = 0;
        int testWrongVar = 0;

        while (e1.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e1.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName != null && roleName.equals("Tester")) {
                System.out.println("browsed a message : " + roleName);
                testVar++;
            } else {

                testWrongVar++;
            }

        }

        Enumeration e2 = qb2.getEnumeration();

        int devVar = 0;
        int devWrongVar = 0;

        while (e2.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e2.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName != null && roleName.equals("Developer")) {
                System.out.println("browsed a message : " + roleName);
                devVar++;
            } else {

                devWrongVar++;
            }

        }

        Enumeration e3 = qb3.getEnumeration();

        int emptyVar = 0;
        int emptyWrongVar = 0;

        while (e3.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e3.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName != null && roleName.equals("")) {
                System.out.println("browsed a message : " + roleName);
                emptyVar++;
            } else {

                emptyWrongVar++;
            }

        }

        if (!(testVar == 3 && devVar == 3 && emptyVar == 3) && (testWrongVar == 0 || devWrongVar == 0 || emptyVar == 0))
            exceptionFlag = true;

        /*
         * for (int i = 0; i < 3; i++)
         * {
         * 
         * jmsContextQCFBindings.createConsumer(queue).receive(30000);
         * jmsContextQCFBindings.createConsumer(queue).receive(30000);
         * jmsContextQCFBindings.createConsumer(queue).receive(30000);
         * 
         * }
         */

        emptyQueue(QCFBindings, queue);

        jmsContextQCFBindings.close();
        if ((exceptionFlag))
            throw new WrongException("testcreateBrowser_MessageSelector_Empty_B_SecOff failed");

    }

    public void testcreateBrowser_MessageSelector_Empty_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        // jmsContextQCFTCP.createConsumer(queue).receive(30000);

        emptyQueue(QCFTCP, queue);

        QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue, "");
        QueueBrowser qb2 = jmsContextQCFTCP.createBrowser(queue, "");
        QueueBrowser qb3 = jmsContextQCFTCP.createBrowser(queue, "");

        TextMessage msg = jmsContextQCFTCP
                        .createTextMessage("browse selector test message");

        for (int i = 0; i < 3; i++) {
            msg.setStringProperty("Role", "Tester");
            jmsContextQCFTCP.createProducer().send(queue, msg);

            msg.setStringProperty("Role", "Developer");
            jmsContextQCFTCP.createProducer().send(queue, msg);

            msg.setStringProperty("Role", "");
            jmsContextQCFTCP.createProducer().send(queue, msg);
        }

        Enumeration e1 = qb1.getEnumeration();

        int testVar = 0;
        int testWrongVar = 0;

        while (e1.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e1.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName != null && roleName.equals("Tester")) {
                System.out.println("browsed a message : " + roleName);
                testVar++;
            } else {

                testWrongVar++;
            }

        }

        Enumeration e2 = qb2.getEnumeration();

        int devVar = 0;
        int devWrongVar = 0;

        while (e2.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e2.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName != null && roleName.equals("Developer")) {
                System.out.println("browsed a message : " + roleName);
                devVar++;
            } else {

                devWrongVar++;
            }

        }

        Enumeration e3 = qb3.getEnumeration();

        int emptyVar = 0;
        int emptyWrongVar = 0;

        while (e3.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e3.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName != null && roleName.equals("")) {
                System.out.println("browsed a message : " + roleName);
                emptyVar++;
            } else {

                emptyWrongVar++;
            }

        }

        if (!(testVar == 3 && devVar == 3 && emptyVar == 3) && (testWrongVar == 0 || devWrongVar == 0 || emptyVar == 0))
            exceptionFlag = true;

        /*
         * for (int i = 0; i < 3; i++)
         * {
         * 
         * jmsContextQCFTCP.createConsumer(queue).receive(30000);
         * jmsContextQCFTCP.createConsumer(queue).receive(30000);
         * jmsContextQCFTCP.createConsumer(queue).receive(30000);
         * 
         * }
         */

        emptyQueue(QCFTCP, queue);

        jmsContextQCFTCP.close();

        if ((exceptionFlag))
            throw new WrongException("testcreateBrowser_MessageSelector_Empty_TCP_SecOff failed");

    }

    public void testcreateBrowser_MessageSelector_Null_B_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);

        QueueBrowser clearBrowser = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = getMessageCount(clearBrowser);

        for (int i = numMsgs; i > 0; i--) {
            jmsContextQCFBindings.createConsumer(queue).receive(30000);
        }

        TextMessage msg = jmsContextQCFBindings
                        .createTextMessage("browse selector test message");

        msg.setStringProperty("Role", "Tester");
        jmsContextQCFBindings.createProducer().send(queue, msg);

        QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue, null);

        Enumeration e1 = qb1.getEnumeration();

        while (e1.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e1.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName.equals("Tester")) {
                System.out.println("browsed a message : " + roleName);
                exceptionFlag = true;
            }
        }

        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testcreateBrowser_MessageSelector_Null_B_SecOff failed");

    }

    public void testcreateBrowser_MessageSelector_Null_TCP_SecOff(
                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        emptyQueue(QCFTCP, queue);

        QueueBrowser clearBrowser = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = getMessageCount(clearBrowser);

        for (int i = numMsgs; i > 0; i--) {
            jmsContextQCFTCP.createConsumer(queue).receive(30000);
        }

        TextMessage msg = jmsContextQCFTCP
                        .createTextMessage("browse selector test message");

        msg.setStringProperty("Role", "Tester");
        jmsContextQCFTCP.createProducer().send(queue, msg);

        QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue, null);

        Enumeration e1 = qb1.getEnumeration();

        while (e1.hasMoreElements()) {
            TextMessage msg1 = (TextMessage) e1.nextElement();

            String roleName = msg1.getStringProperty("Role");

            if (roleName.equals("Tester")) {
                System.out.println("browsed a message : " + roleName);
                exceptionFlag = true;
            }
        }

        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testcreateBrowser_MessageSelector_Null_TCP_SecOff failed");

    }

    public void testcreateBrowser_MessageSelector_Invalid_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        //jmsContextQCFBindings.createConsumer(queue).receive(30000);

        emptyQueue(QCFBindings, queue);

        try {
            QueueBrowser invalidQB = jmsContextQCFBindings.createBrowser(queue,
                                                                         "bad selector");
        } catch (InvalidSelectorRuntimeException ex3) {

            ex3.printStackTrace();
            exceptionFlag = true;
        }

        finally {
            jmsContextQCFBindings.close();
            if (!(exceptionFlag))

                throw new WrongException("testcreateBrowser_MessageSelector_Null_B_SecOff failed");

        }

    }

    public void testcreateBrowser_MessageSelector_Invalid_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        //jmsContextQCFTCP.createConsumer(queue).receive(30000);

        emptyQueue(QCFTCP, queue);

        try {
            QueueBrowser invalidQB = jmsContextQCFTCP.createBrowser(queue,
                                                                    "bad selector");
        } catch (InvalidSelectorRuntimeException ex3) {

            ex3.printStackTrace();
            exceptionFlag = true;
        }

        finally {

            jmsContextQCFTCP.close();
            if (!(exceptionFlag))

                throw new WrongException("testcreateBrowser_MessageSelector_Null_TCP_SecOff failed");

        }

    }

    public void testcreateBrowser_getQueue_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        if (!(qb.getQueue() == queue))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testcreateBrowser_getQueue_B_SecOff failed");

    }

    public void testcreateBrowser_getQueue_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        if (!(qb.getQueue() == queue))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testcreateBrowser_getQueue_TCP_SecOff failed");

    }

    public void testcreateBrowser_close_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        jmsContextQCFBindings.createProducer().send(queue, "This is a test message.");

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        qb.close();

        int numMsgs1 = 0;

        try {
            Enumeration e1 = qb.getEnumeration();

            while (e1.hasMoreElements()) {
                TextMessage message = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

        } catch (JMSException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        } finally {

            //jmsContextQCFBindings.createConsumer(queue).receive(30000);
            emptyQueue(QCFBindings, queue);
            jmsContextQCFBindings.close();

            if (!(exceptionFlag))
                throw new WrongException("testcreateBrowser_close_B_SecOff failed");

        }

    }

    public void testcreateBrowser_close_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        emptyQueue(QCFTCP, queue);
        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        jmsContextQCFTCP.createProducer().send(queue, "This is a test message.");

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        qb.close();

        int numMsgs1 = 0;

        try {
            Enumeration e1 = qb.getEnumeration();

            while (e1.hasMoreElements()) {
                TextMessage message = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

        } catch (JMSException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        } finally {

            emptyQueue(QCFTCP, queue);
            jmsContextQCFTCP.close();
            if (!(exceptionFlag))
                throw new WrongException("testcreateBrowser_close_TCP_SecOff failed");

        }

    }

    public void testGetMessageSelector_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue, "colour = 'red'");

        String msgSelect = qb.getMessageSelector();

        if (!(msgSelect.equals("colour = 'red'")))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if ((exceptionFlag))
            throw new WrongException("testGetMessageSelector_B_SecOff failed");

    }

    public void testGetMessageSelector_TCP_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue, "colour = 'red'");

        String msgSelect = qb.getMessageSelector();

        if (!(msgSelect.equals("colour = 'red'")))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if ((exceptionFlag))
            throw new WrongException("testGetMessageSelector_TCP_SecOff failed");

    }

    // 118062_4_2 Test when no message selector exists for the message consumer,
    // it returns null
    public void testGetMessageSelector_Consumer_B_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        // jmsContextQCFBindings.createConsumer(queue).receive(30000);

        emptyQueue(QCFBindings, queue);

        JMSConsumer consumer = jmsContextQCFBindings.createConsumer(queue, "Color='red'");

        String msgSelect = consumer.getMessageSelector();

        if (msgSelect.equals("Color='red'"))
            flag = true;
        JMSConsumer consumer1 = jmsContextQCFBindings.createConsumer(queue);

        String msgSelect1 = consumer1.getMessageSelector();

        if (msgSelect1 == null)
            val = true;

        if (!(flag == true && val == true))
            exceptionFlag = true;

        consumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetMessageSelector_Consumer_B_SecOff failed");

    }

    public void testGetMessageSelector_Consumer_TCP_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;
        boolean flag = false;
        boolean val = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        //jmsContextQCFTCP.createConsumer(queue).receive(30000);

        emptyQueue(QCFTCP, queue);

        JMSConsumer consumer = jmsContextQCFTCP.createConsumer(queue, "Color='red'");

        String msgSelect = consumer.getMessageSelector();

        if (msgSelect.equals("Color='red'"))
            flag = true;
        JMSConsumer consumer1 = jmsContextQCFTCP.createConsumer(queue);

        String msgSelect1 = consumer1.getMessageSelector();

        if (msgSelect1 == null)
            val = true;

        if (!(flag == true && val == true))
            exceptionFlag = true;

        consumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testGetMessageSelector_Consumer_TCP_SecOff failed");

    }

    // 118062_4_3 Test when message selector is set to null, it returns null

    public void testGetMessageSelector_null_B_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue, null);

        String msgSelect = qb.getMessageSelector();

        if (msgSelect != null)
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetMessageSelector_null_B_SecOff failed");

    }

    public void testGetMessageSelector_null_TCP_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue, null);

        String msgSelect = qb.getMessageSelector();

        if (msgSelect != null)
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testGetMessageSelector_null_TCP_SecOff failed");

    }

    // 118062_4_4 Test when message selector is set to empty string, it returns
    // null

    public void testGetMessageSelector_Empty_B_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        //jmsContextQCFBindings.createConsumer(queue).receive(30000);

        emptyQueue(QCFBindings, queue);
        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue, "");
        String msgSelect = "";

        msgSelect = qb.getMessageSelector();

        if (msgSelect == null)
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetMessageSelector_Empty_B_SecOff failed");

    }

    public void testGetMessageSelector_Empty_TCP_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFBindings, queue);
        // jmsContextQCFTCP.createConsumer(queue).receive(30000);
        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue, "");
        String msgSelect = "";

        msgSelect = qb.getMessageSelector();

        if (msgSelect == null)
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetMessageSelector_Empty_TCP_SecOff failed");

    }

    // 118062_4_5 Test when message selector is not set , it returns null

    public void testGetMessageSelector_notSet_B_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        //  jmsContextQCFBindings.createConsumer(queue).receiveNoWait();
        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
        String msgSelect = "";

        msgSelect = qb.getMessageSelector();

        if (msgSelect == null)
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetMessageSelector_notSet_B_SecOff failed");

    }

    public void testGetMessageSelector_notSet_TCP_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        // jmsContextQCFTCP.createConsumer(queue).receiveNoWait();
        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
        String msgSelect = "";

        msgSelect = qb.getMessageSelector();

        if (msgSelect == null)
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetMessageSelector_notSet_B_SecOff failed");

    }

    //Defect 174395
    public void testTextMessageGetBody_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        String compare = "Hello this is a test case for TextMessage ";
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        TextMessage msg = jmsContextQCFBindings.createTextMessage();

        msg.setText(compare);

        try {
            System.out.println("The obj is :" + msg.getBody(Boolean.class));

        } catch (MessageFormatException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFBindings.close();
        if (!exceptionFlag)
            throw new WrongException("testTextMessageGetBody_B_SecOff failed");

    }

    //Defect 174387
    public void testByteMessageGetBody_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;
        byte[] content = "test".getBytes();

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);

        BytesMessage msg = jmsContextQCFBindings.createBytesMessage();
        msg.writeBytes(content);

        msg.reset();
        System.out.print(msg.readBytes(content));
        System.out.print(msg.getBodyLength());

        try {
            msg.getBody(StringBuffer.class);
        } catch (MessageFormatException mfe) {
            mfe.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFBindings.close();
        if (!exceptionFlag)
            throw new WrongException("testByteMessageGetBody_B_SecOff failed");

    }

    //Defect 174399
    public void testObjectMessageisBodyAssignable_B_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        ObjectMessage msg = jmsContextQCFBindings.createObjectMessage();

        msg.setObject(new StockObject("TESTSTOCK", 5467.123));
        msg.getObject();
        msg.getBody(java.io.Serializable.class);

        try {
            if (msg.isBodyAssignableTo(Boolean.class) == false)
                exceptionFlag = true;

        } catch (Exception ex) {
            System.out.println("Exception shouldn't be thrown");
            ex.printStackTrace();

        }

        jmsContextQCFBindings.close();
        if (exceptionFlag != true)
            throw new WrongException("testObjectMessageisBodyAssignable_B_SecOff failed");

    }

    //Defect 174397
    public void testObjectMessagegetBody_B_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        ObjectMessage msg = jmsContextQCFBindings.createObjectMessage();

        msg.setObject(new StockObject("TESTSTOCK", 678.97));
        msg.getObject();

        try {
            msg.getBody(HashMap.class);

        } catch (MessageFormatException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFBindings.close();
        if (!exceptionFlag)
            throw new WrongException("testObjectMessagegetBody_B_SecOff failed");

    }

    //Defect 174403
    public void testJMSReplyTo(HttpServletRequest request,
                               HttpServletResponse response) throws Throwable {
        boolean exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        TextMessage messageQCFBindings = jmsContextQCFBindings.createTextMessage();
        messageQCFBindings.setText("testJMSReplyTo");
        messageQCFBindings.setJMSReplyTo(queue);

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        JMSConsumer consumer = jmsContextQCFBindings.createConsumer(queue);

        producer.send(queue, messageQCFBindings);
        TextMessage rec_msg = (TextMessage) consumer.receive(30000);

        String replyQ = rec_msg.getJMSReplyTo().toString();
        System.out.println("ReplyTo Dest on received message is " + replyQ);
        System.out.println("Expected Dest is " + queue.toString());

        if (!(queue.toString().equals(replyQ))) {
            System.out.println("Expected value not received");
            exceptionFlag = true;
        }
        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testJMSReplyTo failed");
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

    public static QueueConnectionFactory getQCFBindings()
                    throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;

    }

    public static QueueConnectionFactory getQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        return cf1;

    }

    public static TopicConnectionFactory getTCFBindings()
                    throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf");

        return tcf1;

    }

    public static TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf1");

        return tcf1;

    }

    private static class WrongException extends Exception {
        WrongException(String str) {
            super(str);
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

    private static class StockObject implements Serializable {
        final String stockName;
        final double stockValue;

        StockObject(String stname, double stvalue) {
            // TODO Auto-generated constructor stub
            this.stockName = stname;
            this.stockValue = stvalue;
        }

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

}
