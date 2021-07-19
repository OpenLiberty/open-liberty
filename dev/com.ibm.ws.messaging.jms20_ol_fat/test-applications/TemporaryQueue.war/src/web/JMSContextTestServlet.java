package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
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
import javax.transaction.UserTransaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSContextTestServlet extends HttpServlet {

    public static QueueConnectionFactory qcfBindings;
    public static QueueConnectionFactory qcfTCP;

    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Queue queue;
    public static Topic topic;
    public static Topic topic2;

    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException { // TODO
        // Auto-generated method stub

        super.init();
        try {
            qcfBindings = getQCFBindings();
            qcfTCP = getQCFTCP();
            queue = getQueue();
            tcfBindings = getTCFBindings();
            tcfTCP = getTCFTCP();
            topic = getTopic("eis/topic1");
            topic2 = getTopic("eis/topic2");

        } catch (NamingException e) { // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static QueueConnectionFactory getQCFBindings()
                    throws NamingException {

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

    public static TopicConnectionFactory getTCFBindings()
                    throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf");

        return tcf1;

    }

    public TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf1");

        return tcf1;

    }

    public Topic getTopic(String name) throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup(name);

        return topic;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(JMSContextTestServlet.class);
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
            System.out.println(" End: " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    // ======================= receive =================================================//
    public void testReceiveMessageTopicSecOff_B(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();

            Message msg = jmsContextTCFBindings.createTextMessage("test");
            msg.clearBody();

            jmsProducerTCFBindings.send(topic, "");

            jmsConsumerTCFBindings.receive();

            jmsConsumerTCFBindings.close();
            jmsContextTCFBindings.close();
        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveMessageTopicSecOff_B failed");

    }

    public void testReceiveMessageTopicSecOff_TCP(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();
            Message msg = jmsContextTCFTCP.createTextMessage("test");
            msg.clearBody();
            jmsProducerTCFTCP.send(topic, "");
            jmsConsumerTCFTCP.receive();
            jmsConsumerTCFTCP.close();
            jmsContextTCFTCP.close();
        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveMessageTopicSecOff_TCP failed");
    }

    public void testReceiveMessageTopicTranxSecOff_B(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();
            Message msg = jmsContextTCFBindings.createTextMessage("testReceiveMessageTopicTranxSecOff_B");

            jmsProducerTCFBindings.send(topic, msg);
            ut.commit();
            ut.begin();
            jmsConsumerTCFBindings.receive();

            ut.commit();
            jmsConsumerTCFBindings.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveMessageTopicTranxSecOff_B failed");

    }

    public void testReceiveMessageTopicTranxSecOff_TCP(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();

            Message msg = jmsContextTCFTCP.createTextMessage("testReceiveMessageTopicTranxSecOff_B");

            jmsProducerTCFTCP.send(topic, msg);
            ut.commit();
            ut.begin();

            jmsConsumerTCFTCP.receive();
            ut.commit();
            jmsConsumerTCFTCP.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveMessageTopicTranxSecOff_TCP failed");
    }

    // =====================================
    public void testReceiveTimeoutMessageTopicSecOff_B(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();
            Message msg = jmsContextTCFBindings.createTextMessage("test");

            msg.clearBody();
            jmsProducerTCFBindings.send(topic, "");
            jmsConsumerTCFBindings.receive(30000);
            jmsConsumerTCFBindings.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveTimeoutMessageTopicSecOff_B failed");

    }

    public void testReceiveTimeoutMessageTopicSecOff_TCP(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();
            Message msg = jmsContextTCFTCP.createTextMessage("test");

            msg.clearBody();
            jmsProducerTCFTCP.send(topic, "");
            jmsConsumerTCFTCP.receive(30000);

            jmsConsumerTCFTCP.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = false;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveTimeoutMessageTopicSecOff_TCP failed");

    }

    public void testReceiveNoWaitMessageTopicSecOff_B(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            Message msg = jmsContextTCFBindings.createTextMessage("test");

            p1.send(topic, msg);
            jmsConsumer.receiveNoWait();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveNoWaitMessageTopicSecOff_B failed");

    }

    public void testReceiveNoWaitMessageTopicSecOff_TCP(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            Message msg = jmsContextTCFTCP.createTextMessage("test");

            p1.send(topic, msg);
            jmsConsumer.receiveNoWait();

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveNoWaitMessageTopicSecOff_TCP failed");

    }

    public void testReceiveNoWaitNullMessageTopicSecOff_B(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            jmsConsumer.receiveNoWait();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveNoWaitNullMessageTopicSecOff_B failed");

    }

    public void testReceiveNoWaitNullMessageTopicSecOff_TCP(HttpServletRequest request,
                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            jmsConsumer.receiveNoWait();
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveNoWaitNullMessageTopicSecOff_TCP failed");

    }

    // ===== receiveBoby<Class T>   

    public void testReceiveBodyTopicSecOff_B(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            p1.send(topic, "testing");
            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTopicSecOff_B failed");

    }

    public void testReceiveBodyTopicSecOff_TCPIP(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testing");
            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {
            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyTransactionTopicSecOff_B(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");
            ut.commit();
            ut.begin();
            jmsConsumer.receiveBody(String.class);

            ut.commit();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTransactionTopicSecOff_B failed");

    }

    public void testReceiveBodyTransactionTopicSecOff_TCPIP(HttpServletRequest request,
                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            ut.begin();
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");
            ut.commit();
            ut.begin();
            jmsConsumer.receiveBody(String.class);
            ut.commit();
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTransactionTopicSecOff_TCPIP failed");

    }

//====
    public void testReceiveBodyTextMessageTopicSecOff_B(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            TextMessage m1 = jmsContextTCFBindings.createTextMessage("testReceiveBodyTextMessageTopicSecOff_B");

            p1.send(topic, m1);
            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTextMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyTextMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            TextMessage m1 = jmsContextTCFTCP.createTextMessage("testReceiveBodyTextMessageTopicSecOff_TCPIP");

            p1.send(topic, m1);

            jmsConsumer.receiveBody(String.class);
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTextMessageTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyObjectMessageTopicSecOff_B(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_B");
            ObjectMessage m1 = jmsContextTCFBindings.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);

            Object msg = jmsConsumer.receiveBody(Serializable.class);
            msg.equals(abc);

            jmsConsumer.close();

            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyObjectMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyObjectMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_TCPIP");
            ObjectMessage m1 = jmsContextTCFTCP.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);

            Object msg = jmsConsumer.receiveBody(Serializable.class);
            msg.equals(abc);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyObjectMessageTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyMapMessageTopicSecOff_B(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            MapMessage message = jmsContextTCFBindings.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);

            jmsConsumer.receiveBody(java.util.Map.class);

            jmsConsumer.close();

            jmsContextTCFBindings.close();
        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyMapMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyMapMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            MapMessage message = jmsContextTCFTCP.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);
            jmsConsumer.receiveBody(java.util.Map.class);
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyMapMessageTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyByteMessageTopicSecOff_B(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFBindings.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBody(byte[].class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyByteMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyByteMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            byte[] data = new byte[] { 127, 0 };

            BytesMessage message = jmsContextTCFTCP.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBody(byte[].class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyByteMessageTopicSecOff_TCPIP failed");

    }

    // ==================================================================================
    // ===== receiveBoby<Class T, tmeout>   

    public void testReceiveBodyTimeOutTopicSecOff_B(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            p1.send(topic, "testing");
            jmsConsumer.receiveBody(String.class, 30000);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutTopicSecOff_B failed");

    }

    public void testReceiveBodyTimeOutTopicSecOff_TCPIP(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testing");
            jmsConsumer.receiveBody(String.class, 30000);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyTimeOutTransactionTopicSecOff_B(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            ut.begin();
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");
            ut.commit();
            ut.begin();
            jmsConsumer.receiveBody(String.class, 30000);
            ut.commit();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutTransactionTopicSecOff_B failed");

    }

    public void testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP(HttpServletRequest request,
                                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            ut.begin();
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");
            ut.commit();
            ut.begin();
            jmsConsumer.receiveBody(String.class, 30000);
            ut.commit();
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP failed");
    }

    public void testReceiveBodyTimeOutTextMessageTopicSecOff_B(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            TextMessage m1 = jmsContextTCFBindings.createTextMessage("testReceiveBodyTimeOutTextMessageTopicSecOff_B");

            p1.send(topic, m1);
            jmsConsumer.receiveBody(String.class, 30000);
            jmsConsumer.close();
            jmsContextTCFBindings.close();
        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutTextMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            TextMessage m1 = jmsContextTCFTCP.createTextMessage("testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP");

            p1.send(topic, m1);
            jmsConsumer.receiveBody(String.class, 30000);

            jmsConsumer.close();
            jmsContextTCFTCP.close();
        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP failed");
    }

    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_B(HttpServletRequest request,
                                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            Object abc = new String("testReceiveBodyTimeOutObjectMessageTopicSecOff_B");
            ObjectMessage m1 = jmsContextTCFBindings.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);
            Object msg = jmsConsumer.receiveBody(Serializable.class, 30000);
            msg.equals(abc);

            jmsConsumer.close();

            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutObjectMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            Object abc = new String("testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP");
            ObjectMessage m1 = jmsContextTCFTCP.createObjectMessage();
            m1.setObject((Serializable) abc);

            p1.send(topic, m1);
            Object msg = jmsConsumer.receiveBody(Serializable.class, 30000);
            msg.equals(abc);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyTimeOutMapMessageTopicSecOff_B(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            MapMessage message = jmsContextTCFBindings.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);
            jmsConsumer.receiveBody(java.util.Map.class, 30000);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutMapMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            MapMessage message = jmsContextTCFTCP.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);
            jmsConsumer.receiveBody(java.util.Map.class, 30000);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP failed");
    }

    public void testReceiveBodyTimeOutByteMessageTopicSecOff_B(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFBindings.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBody(byte[].class, 30000);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutByteMessageTopicSecOff_B failed");
    }

    public void testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFTCP.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBody(byte[].class, 30000);
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP failed");
    }

    //-------------------------================================================================

    // ----------------------- ================= receiveNowait(class c)

    public void testReceiveBodyNoWaitTopicSecOff_B(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            p1.send(topic, "testing");
            jmsConsumer.receiveBodyNoWait(String.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitTopicSecOff_B failed");

    }

    public void testReceiveBodyNoWaitTopicSecOff_TCPIP(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testing");
            jmsConsumer.receiveBodyNoWait(String.class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyNoWaitTransactionTopicSecOff_B(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            ut.begin();
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            p1.send(topic, "testReceiveBodyNoWaitTransactionTopicSecOff_B");
            ut.commit();
            ut.begin();

            jmsConsumer.receiveBodyNoWait(String.class);

            ut.commit();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitTransactionTopicSecOff_B failed");

    }

    public void testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP(HttpServletRequest request,
                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            ut.begin();
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");
            ut.commit();
            ut.begin();

            jmsConsumer.receiveBodyNoWait(String.class);
            ut.commit();

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyNoWaitTextMessageTopicSecOff_B(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            TextMessage m1 = jmsContextTCFBindings.createTextMessage("testReceiveBodyTextMessageTopicSecOff_B");

            p1.send(topic, m1);
            jmsConsumer.receiveBodyNoWait(String.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitTextMessageTopicSecOff_B failed");
    }

    public void testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            TextMessage m1 = jmsContextTCFTCP.createTextMessage("testReceiveBodyTextMessageTopicSecOff_TCPIP");
            p1.send(topic, m1);
            jmsConsumer.receiveBodyNoWait(String.class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_B(HttpServletRequest request,
                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_B");
            ObjectMessage m1 = jmsContextTCFBindings.createObjectMessage();
            m1.setObject((Serializable) abc);

            p1.send(topic, m1);
            Object msg = jmsConsumer.receiveBodyNoWait(Serializable.class);
            msg.equals(abc);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitObjectMessageTopicSecOff_B failed");
    }

    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_TCPIP");
            ObjectMessage m1 = jmsContextTCFTCP.createObjectMessage();
            m1.setObject((Serializable) abc);

            p1.send(topic, m1);
            Object msg = jmsConsumer.receiveBodyNoWait(Serializable.class);
            msg.equals(abc);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP failed");
    }

    public void testReceiveBodyNoWaitMapMessageTopicSecOff_B(HttpServletRequest request,
                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            MapMessage message = jmsContextTCFBindings.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);
            jmsConsumer.receiveBodyNoWait(java.util.Map.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitMapMessageTopicSecOff_B failed");
    }

    public void testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            MapMessage message = jmsContextTCFTCP.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);
            jmsConsumer.receiveBodyNoWait(java.util.Map.class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP failed");

    }

    public void testReceiveBodyNoWaitByteMessageTopicSecOff_B(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFBindings.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBodyNoWait(byte[].class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitByteMessageTopicSecOff_B failed");

    }

    public void testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP(HttpServletRequest request,
                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFTCP.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBodyNoWait(byte[].class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP failed");
    }

    //------------------------ ==========================================================

    //-- simulation of mfe

    public void testReceiveBodyMFENoBodyTopicSecOff_B(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(String.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyMFENoBodyTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyMFENoBodyTopicSecOff_TCP(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(String.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyMFENoBodyTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(byte[].class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP(HttpServletRequest request,
                                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBody(byte[].class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_B(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        StreamMessage m = jmsContextTCFBindings.createStreamMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBody(Object.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyMFEUnsupportedTypeTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP(HttpServletRequest request,
                                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        StreamMessage m = jmsContextTCFTCP.createStreamMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBody(Object.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    //===================
    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_B(HttpServletRequest request,
                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBody(String.class, 30000);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyTimeOutMFENoBodyTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBody(String.class, 30000);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP failed");
        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(byte[].class, 30000);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP(HttpServletRequest request,
                                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBody(byte[].class, 30000);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        StreamMessage m = jmsContextTCFBindings.createStreamMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(Object.class, 30000);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP(HttpServletRequest request,
                                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        StreamMessage m = jmsContextTCFTCP.createStreamMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(Object.class, 30000);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    //------------------------======================
    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_B(HttpServletRequest request,
                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBodyNoWait(String.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyNoWaitMFENoBodyTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic12");

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBodyNoWait(String.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B(HttpServletRequest request,
                                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();

        TextMessage m = jmsContextTCFBindings.createTextMessage();

        p1.send(topic, m);

        try {
            jmsConsumer.receiveBodyNoWait(byte[].class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();
    }

    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP(HttpServletRequest request,
                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic12");
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer producer = jmsContextTCFTCP.createProducer();
        TextMessage message = jmsContextTCFTCP.createTextMessage();

        producer.send(topic, message);

        try {
            jmsConsumer.receiveBodyNoWait(String.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B(HttpServletRequest request,
                                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        StreamMessage m = jmsContextTCFBindings.createStreamMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBodyNoWait(Object.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B failed");

        jmsConsumer.close();
        jmsContextTCFBindings.close();

    }

    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP(HttpServletRequest request,
                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        StreamMessage m = jmsContextTCFTCP.createStreamMessage();

        p1.send(topic, m);
        try {
            jmsConsumer.receiveBodyNoWait(Object.class);
        } catch (MessageFormatRuntimeException e) {

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    //=============================

    // ============================================118077 =====================================

    // ====================================

    // -----------------118066

    public void testStartJMSContextSecOffBinding(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue);

        jmsContextQCFBindings.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
        jmsProducerQCFBindings.send(queue, outbound);
        jmsContextQCFBindings.start();

        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings
                        .createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFBindings
                        .receive(30000);

        String inbound = "";
        inbound = receiveMsg.getText();

        if (outbound.equals(inbound))
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException("testStartJMSContextSecOnBinding failed");

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

    }

    public void testStartJMSContextSecOffTCP(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue);
        jmsContextQCFTCP.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
        jmsProducerQCFTCP.send(queue, outbound);
        jmsContextQCFTCP.start();

        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFTCP.receive(30000);

        String inbound = "";
        inbound = receiveMsg.getText();

        if (outbound.equals(inbound))
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException("testStartJMSContextSecOffTCP failed");

        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();

    }

    public void testStartJMSContextStartSecOffBinding(
                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            jmsContextQCFBindings.start();
            jmsContextQCFBindings.start();

            jmsContextQCFBindings.close();

        } catch (Exception e) {
            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testStartJMSContextStartSecOffBinding failed");

    }

    public void testStartJMSContextStartSecOffTCP(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.close();

        } catch (Exception e) {
            e.printStackTrace();
            exceptionFlag = true;

        }
        if (exceptionFlag == true)
            throw new WrongException("testStartJMSContextStartSecOffTCP failed");

    }

    public void testStopJMSContextSecOffBinding(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            jmsContextQCFBindings.start();

            jmsContextQCFBindings.stop();
            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testStopJMSContextSecOffBinding failed");
    }

    public void testStopJMSContextSecOffTCP(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.stop();
            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testStopJMSContextSecOffTCP failed");
    }

    // ----------------------------------------------------------------------
    // --------------- 118065 ----------------------------------------------

    public void testCommitLocalTransaction_B(HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings
                            .createTextMessage("Hello World");
            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFBindings.commit();
            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count number of messages
            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }
            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings
                            .createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);

            jmsContextQCFBindings.commit();

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testCommitLocalTransaction_B failed");

    }

    public void testCommitLocalTransaction_TCP(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");
            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count * number of messages
            while (e1.hasMoreElements()) {
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
            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testCommitLocalTransaction_TCP failed");

    }

    public void testCommitNonLocalTransaction_B(HttpServletRequest request,
                                                HttpServletResponse response) throws Exception {

        exceptionFlag = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings
                            .createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();

                numMsgs++;
            }

            jmsContextQCFBindings.commit();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }

        if (exceptionFlag == false)
            throw new WrongException("testCommitNonLocalTransaction_B failed");
    }

    public void testCommitNonLocalTransaction_TCP(HttpServletRequest request,
                                                  HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;

            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }
            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testCommitNonLocalTransaction_B failed");

    }

    public void testRollbackLocalTransaction_B(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings
                            .createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.commit();

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;

            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }
            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings
                            .createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);

            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;

            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFBindings.rollback();

            QueueBrowser qb2 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while (e2.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e2.nextElement();

                numMsgs2++;
            }

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();
        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testRollbackLocalTransaction_B failed");

    }

    public void testRollbackLocalTransaction_TCP(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);
            jmsContextQCFTCP.commit();

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            // count number of messages
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFTCP.receive(30000);

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count number of messages
            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFTCP.rollback();

            QueueBrowser qb2 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while (e2.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e2.nextElement();

                numMsgs2++;
            }

            jmsConsumerQCFTCP.close();
            jmsContextQCFTCP.close();

        } catch (Exception e) {
            e.printStackTrace();
            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testRollbackLocalTransaction_TCP failed");

    }

    public void testRollbackNonLocalTransaction_B(HttpServletRequest request,
                                                  HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings
                            .createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();

            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.rollback();

            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testRollbackNonLocalTransaction_B failed");
    }

    public void testRollbackNonLocalTransaction_TCP(HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);
            jmsContextQCFTCP.rollback();

            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testRollbackNonLocalTransaction_TCP failed");

    }

    public void testRecoverNonLocalTransaction_B(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings
                            .createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.recover();
            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testRecoverNonLocalTransaction_B failed");

    }

    public void testRecoverNonLocalTransaction_TCP(HttpServletRequest request,
                                                   HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");
            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

            jmsProducerQCFTCP.send(queue, message);

            jmsContextQCFTCP.recover();

            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testRecoverNonLocalTransaction_TCP failed");

    }

    // ========================= 118068 ===========================

    public void testCreateTemporaryQueueSecOffBinding(
                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        // Create the temp queue
        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if (tempQ != null)
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateTemporaryQueueSecOffBinding failed");

        jmsContextQCFBindings.close();

    }

    // ==========================================
    public void testCreateTemporaryQueueSecOffTCPIP(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();
        if (tempQ != null)
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateTemporaryQueueSecOffTCPIP failed");

        jmsContextQCFTCP.close();
    }

    public void testTemporaryQueueLifetimeSecOff_B(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

            jmsContextQCFBindings.close();
            jmsContextQCFBindings.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testTemporaryQueueLifetimeSecOff_B failed");

    }

    public void testTemporaryQueueLifetimeSecOff_TCPIP(
                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            jmsContextQCFTCP.close();
            jmsContextQCFTCP.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testTemporaryQueueLifetimeSecOff_TCPIP failed");

    }

    public void testgetTemporaryQueueNameSecOffBinding(
                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if (tempQ.getQueueName() != (null))
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException(
                            "testgetTemporaryQueueNameSecOffBinding failed");

        jmsContextQCFBindings.close();

    }

    public void testgetTemporaryQueueNameSecOffTCPIP(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if (tempQ.getQueueName() != (null))
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException(
                            "testgetTemporaryQueueNameSecOffTCPIP failed");
        jmsContextQCFTCP.close();

    }

    public void testToStringTemporaryQueueNameSecOffBinding(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if (tempQ.toString() != (null))
            exceptionFlag = false;
        else
            exceptionFlag = true;
        if (exceptionFlag == true)
            throw new WrongException(
                            "testToStringTemporaryQueueNameSecOffBinding failed");

        jmsContextQCFBindings.close();
    }

    public void testToStringTemporaryQueueNameSecOffTCPIP(
                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if (tempQ.toString() != (null))
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException(
                            "testToStringTemporaryQueueNameSecOffTCPIP failed");

        jmsContextQCFTCP.close();

    }

    public void testDeleteTemporaryQueueNameSecOffBinding(
                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();
            tempQ.delete();
            jmsContextQCFBindings.createBrowser(tempQ);

            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testDeleteTemporaryQueueNameSecOffBinding failed");

    }

    public void testDeleteTemporaryQueueNameSecOffTCPIP(
                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            tempQ.delete();
            jmsContextQCFTCP.createBrowser(tempQ);

            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testDeleteTemporaryQueueNameSecOffTCPIP failed");

    }

    public void testDeleteExceptionTemporaryQueueNameSecOFF_B(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

            tempQ.delete();
            jmsContextQCFBindings.createBrowser(tempQ);
            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testDeleteExceptionTemporaryQueueNameSecOFF_B failed");

    }

    public void testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP(
                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            tempQ.delete();
            jmsContextQCFTCP.createBrowser(tempQ);
            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP failed");

    }

    public void testPTPTemporaryQueue_Binding(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();

        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(tempQ);

        jmsProducerQCFBindings.send(tempQ, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerQCFBindings.receive(30000);

        if (recMessage.getText() == "hello world") {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testPTPTemporaryQueue_Binding failed");

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

    }

    public void testPTPTemporaryQueue_TCP(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(tempQ);

        jmsProducerQCFTCP.send(tempQ, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerQCFTCP.receive(30000);

        if (recMessage.getText().equalsIgnoreCase("hello world")) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testPTPTemporaryQueue_TCP failed");
        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();
    }

    // -------------------- Temporary topic

    public void testCreateTemporaryTopicSecOffBinding(
                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if (tempT != null) {
            exceptionFlag = false;

        } else {

            exceptionFlag = true;

        }

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateTemporaryTopicSecOffBinding failed");
        jmsContextTCFBindings.close();

    }

    public void testCreateTemporaryTopicSecOffTCPIP(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if (tempT != null) {
            exceptionFlag = false;

        } else {

            exceptionFlag = true;

        }

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateTemporaryTopicSecOffTCPIP failed");
        jmsContextTCFTCP.close();

    }

    public void testTemporaryTopicLifetimeSecOff_B(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

            jmsContextTCFBindings.close();
            tempT.delete();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testTemporaryTopicLifetimeSecOff_B failed");

    }

    public void testTemporaryTopicLifetimeSecOff_TCPIP(
                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

            jmsContextTCFTCP.close();
            tempT.delete();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testTemporaryTopicLifetimeSecOff_TCPIP failed");

    }

    public void testGetTemporaryTopicSecOffBinding(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if (tempT.getTopicName() != (null)) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testGetTemporaryTopicSecOffBinding failed");
        jmsContextTCFBindings.close();

    }

    public void testGetTemporaryTopicSecOffTCPIP(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if (tempT.getTopicName() != (null)) {
            exceptionFlag = false;

        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testGetTemporaryTopicSecOffTCPIP failed");

        jmsContextTCFTCP.close();

    }

    public void testToStringTemporaryTopicSecOffBinding(
                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if (tempT.toString() != (null)) {
            exceptionFlag = false;

        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testToStringTemporaryTopicSecOffBinding failed");

        jmsContextTCFBindings.close();

    }

    public void testToStringeTemporaryTopicSecOffTCPIP(
                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if (tempT.toString() != (null)) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testToStringeTemporaryTopicSecOffTCPIP failed");

        jmsContextTCFTCP.close();

    }

    public void testDeleteTemporaryTopicSecOffBinding(
                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

            jmsContextTCFBindings.close();
            tempT.delete();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testDeleteTemporaryTopicSecOffBinding failed");

    }

    public void testDeleteTemporaryTopicSecOffTCPIP(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

            jmsContextTCFTCP.close();
            tempT.delete();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException(
                            "testDeleteTemporaryTopicSecOffTCPIP failed");

    }

    public void testDeleteExceptionTemporaryTopicSecOff_B(
                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        try {

            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            jmsContextTCFBindings.createConsumer(tempT);

            tempT.delete();
            jmsContextTCFBindings.close();
        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testDeleteExceptionTemporaryTopicSecOff_B failed");

    }

    public void testDeleteExceptionTemporaryTopicSecOff_TCPIP(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        try {

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            jmsContextTCFTCP.createConsumer(tempT);

            tempT.delete();
            jmsContextTCFTCP.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException(
                            "testDeleteExceptionTemporaryTopicSecOff_TCPIP failed");

    }

    public void testTemporaryTopicPubSubSecOff_B(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();

        JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(tempT);

        jmsProducerTCFBindings.send(tempT, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerTCFBindings.receive(30000);

        if (recMessage.getText() == "hello world") {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testTemporaryTopicPubSubSecOff_B failed");

        jmsConsumerTCFBindings.close();
        jmsContextTCFBindings.close();

    }

    public void testTemporaryTopicPubSubSecOff_TCPIP(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        // create resources on the queue
        JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();

        JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(tempT);

        jmsProducerTCFTCP.send(tempT, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerTCFTCP.receive(30000);

        if (recMessage.getText().equals("hello world")) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException(
                            "testTemporaryTopicPubSubSecOff_TCPIP failed");
        jmsConsumerTCFTCP.close();
        jmsContextTCFTCP.close();

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

    // ===============================================================================================
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

}
