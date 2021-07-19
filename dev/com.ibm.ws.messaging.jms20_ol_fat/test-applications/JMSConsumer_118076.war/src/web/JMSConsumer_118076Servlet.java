package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSConsumer_118076Servlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;
    public static Topic jmsTopic;

    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQCFBindings = getQCFBindings();
            jmsQCFTCP = getQCFTCP();
            jmsQueue = getQueue();
            jmsTopic = getTopic();

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
        final TraceComponent tc = Tr.register(JMSConsumer_118076Servlet.class); // injection
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

    public void testCloseConsumer_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();
            jmsConsumer.receive();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
            System.out.println("******THE EXCEPTION IN testCloseConsumer_B_SecOff IS : "
                               + ex.getClass().getName());
        }

        if (exceptionFlag == false)
            throw new WrongException("testCloseConsumer_TcpIp_SecOff failed: Expected exception did not occur");

    }

    public void testCloseConsumer_TcpIp_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContext = jmsQCFTCP.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();
            jmsConsumer.receive();

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
            System.out.println("******THE EXCEPTION IN testCloseConsumer_TcpIp_SecOff IS : "
                               + ex.getClass().getName());
        }

        if (exceptionFlag == false)
            throw new WrongException("testCloseConsumer_TcpIp_SecOff failed: Expected exception did not occur");

    }

    public void testCloseClosedConsumer_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsConsumer.close();
        try {
            jmsConsumer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        System.out.println("testCloseClosedConsumer_B_SecOff: Exception occurred is " + exceptionFlag);
        jmsContext.close();
        if (exceptionFlag == true)
            throw new WrongException("testCloseClosedConsumer_B_SecOff failed: Unexpected exception has occured");
    }

    public void testCloseClosedConsumer_TcpIp_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsConsumer.close();
        try {
            jmsConsumer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        System.out.println("testCloseClosedConsumer_TcpIp_SecOff: Exception occurred is " + exceptionFlag);
        jmsContext.close();
        if (exceptionFlag == true)
            throw new WrongException("testCloseClosedConsumer_B_SecOff failed: Unexpected exception has occured");
    }

    public void testGetMessageSelector_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        String valSelector = jmsConsumer.getMessageSelector();
        System.out.println("testGetMessageSelector_B_SecOff: Selector for consumerWithValidSelector is " + valSelector);

        jmsConsumer = jmsContext.createConsumer(jmsQueue, null);
        String nullSelector = jmsConsumer.getMessageSelector();
        System.out.println("testGetMessageSelector_B_SecOff: Selector for consumerWithNullSelector is " + nullSelector);

        jmsConsumer = jmsContext.createConsumer(jmsQueue, "");
        String emptySelector = jmsConsumer.getMessageSelector();
        System.out.println("testGetMessageSelector_B_SecOff: Selector for consumerWithEmptySelector is " + emptySelector);
        jmsContext.close();
        if (!(valSelector == "Test" && nullSelector == null && emptySelector == ""))
            throw new WrongException("testGetMessageSelector_B_SecOff failed : Selector value is incorrect");

    }

    public void testGetMessageSelector_TcpIp_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        String valSelector = jmsConsumer.getMessageSelector();
        System.out.println("testGetMessageSelector_TcpIp_SecOff: Selector for consumerWithValidSelector is " + valSelector);

        jmsConsumer = jmsContext.createConsumer(jmsQueue, null);
        String nullSelector = jmsConsumer.getMessageSelector();
        System.out.println("testGetMessageSelector_TcpIp_SecOff: Selector for consumerWithNullSelector is " + nullSelector);

        jmsConsumer = jmsContext.createConsumer(jmsQueue, "");
        String emptySelector = jmsConsumer.getMessageSelector();
        System.out.println("testGetMessageSelector_TcpIp_SecOff: Selector for consumerWithEmptySelector is " + emptySelector);
        jmsContext.close();
        if (!(valSelector == "Test" && nullSelector == null && emptySelector == "")) {
            throw new WrongException("testGetMessageSelector_TcpIp_SecOff failed : Selector value is incorrect");
        }
    }

    public void testSetMessageListener_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        try {
            jmsConsumer.setMessageListener(null);
        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;
            System.out.println("testSetMessageListener_B_SecOff: JMSRuntimeException was caught when trying to setMessageListener as expected");
        }
        jmsContext.close();
        if (exceptionFlag == false)
            throw new WrongException("testSetMessageListener_B_SecOff failed: Expected exception did not occur");
    }

    public void testSetMessageListener_TcpIp_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        try {
            jmsConsumer.setMessageListener(null);
        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;
            System.out.println("testSetMessageListener_TcpIp_SecOff: JMSRuntimeException was caught when trying to setMessageListener as expected");
        }
        jmsContext.close();
        if (exceptionFlag == false)
            throw new WrongException("testSetMessageListener_TcpIp_SecOff failed: Expected exception did not occur");
    }

    public void testGetMessageListener_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        try {
            jmsConsumer.getMessageListener();
        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;
            System.out.println("testGetMessageListener_B_SecOff: JMSRuntimeException was caught when trying to getMessageListener as expected");
        }
        jmsContext.close();
        if (exceptionFlag == false)
            throw new WrongException("testGetMessageListener_B_SecOff failed: Expected exception did not occur");
    }

    public void testGetMessageListener_TcpIp_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        try {
            jmsConsumer.getMessageListener();
        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;
            System.out.println("testGetMessageListener_TcpIp_SecOff: JMSRuntimeException was caught when trying to getMessageListener as expected");
        }
        jmsContext.close();
        if (exceptionFlag == false)
            throw new WrongException("testGetMessageListener_TcpIp_SecOff failed: Expected exception did not occur");
    }

    public void testSessionClose_IllegalStateException(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = true;
        ConnectionFactory cf1 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");

        Connection con = cf1.createConnection();
        con.start();
        Session session = con.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        session.close();

        try {
            session.createDurableConsumer(jmsTopic, "SUBID1");
            exceptionFlag = false;
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();

        }

        try {
            session.createDurableConsumer(jmsTopic, "SUBID2", "", true);
            exceptionFlag = false;
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();

        }

        try {
            session.createDurableSubscriber(jmsTopic, "SUBID3", "", true);
            exceptionFlag = false;
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();

        }

        try {
            session.createDurableSubscriber(jmsTopic, "SUBID4");
            exceptionFlag = false;
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();

        }

        con.close();
        if (!(exceptionFlag))
            throw new WrongException("testSessionClose_IllegalStateException failed: Unexpected exception has occured");
    }

    public void testTopicSession_Qrelated_IllegalStateException(HttpServletRequest request,
                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = true;
        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");

        TopicConnection con = cf1.createTopicConnection();
        con.start();
        TopicSession tsession = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        try {
            tsession.createBrowser(jmsQueue);
            exceptionFlag = false;
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();

        }

        try {
            tsession.createQueue("TestQ");
            exceptionFlag = false;
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();

        }

        try {
            tsession.createTemporaryQueue();
            exceptionFlag = false;
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();

        }

        con.close();
        if (!(exceptionFlag))
            throw new WrongException("testTopicSession_Qrelated_IllegalStateException failed: Unexpected exception has occured");
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

    public Queue getQueue() throws NamingException {

        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/jndi_INPUT_Q");

        return queue;
    }

    public Topic getTopic() throws NamingException {
        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");
        return topic;
    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }
}