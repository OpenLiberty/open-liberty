package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
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
public class JMSContext_118067Servlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static TopicConnectionFactory jmsTCFBindings;
    public static TopicConnectionFactory jmsTCFTCP;
    public static Queue jmsQueue;
    public static Queue queue;
    public static JMSContext jmsContext;
    public static JMSConsumer jmsConsumer;
    public static JMSProducer jmsProducer;

    public static Topic topic;
    public static Topic jmsTopic;
    public static Topic jmsTopic2;

    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQCFBindings = getQCF("jndi_JMS_BASE_QCF");
            jmsQCFTCP = getQCF("jndi_JMS_BASE_QCF1");
            jmsTCFBindings = getTCF("eis/tcf");
            jmsTCFTCP = getTCF("eis/tcf1");
            jmsQueue = getQueue();

            jmsTopic = getTopic("eis/topic1");
            jmsTopic2 = getTopic("eis/topic2");
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
        final TraceComponent tc = Tr.register(JMSContext_118067Servlet.class); // injection
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

    public void testCreateJmsProducerAndSend_B_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testCreateJmsProducerAndSend_B_SecOff");

        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Received message: " + msg.getText());
        if (!(msg != null && msg.getText().equals("testCreateJmsProducerAndSend_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateJmsProducerAndSend_B_SecOff failed: Expected message was not received");

    }

    public void testCreateJmsProducerAndSend_TCP_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testCreateJmsProducerAndSend_TCP_SecOff");

        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);

        System.out.println("Received message: " + msg.getText());
        if (!(msg != null && msg.getText().equals("testCreateJmsProducerAndSend_TCP_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateJmsProducerAndSend_TCP_SecOff failed: Expected message was not received");
    }

    public void testSetMessagePropertyBindings_Send(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.setDisableMessageID(true);
        jmsProducer.send(jmsQueue, "testSetMessagePropertyBindings_Send");

        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("testSetMessagePropertyBindings_Send: MESSAGE ID IS " + msg.getJMSMessageID());

        if (!(msg.getJMSMessageID() == null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testSetMessagePropertyBindings_Send failed: JMSMessageID was not null");

    }

    public void testSetMessagePropertyTcpIp_Send(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsProducer = jmsContext.createProducer();
        jmsProducer.setDisableMessageID(true);
        jmsProducer.send(jmsQueue, "testSetMessagePropertyTcpIp_Send");

        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("testSetMessagePropertyTcpIp_Send: MESSAGE ID IS " + msg.getJMSMessageID());

        if (!(msg.getJMSMessageID() == null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testSetMessagePropertyTcpIp_Send failed: JMSMessageID was not null");

    }

    public void testQueueNameNull_B(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        jmsProducer = jmsContext.createProducer();
        try {
            queue = jmsContext.createQueue(null);

            jmsProducer.send(queue, "testQueueNameNull_B");

            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m.getText());
        } catch (JMSRuntimeException e) {
            System.out.println("Expected JMSRuntimeException occured in testQueueNameNull_B");
            exceptionFlag = true;
            e.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameNull_B failed: Expected JMSRuntimeException was not seen");

    }

    public void testQueueNameNull_TcpIp(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        jmsProducer = jmsContext.createProducer();
        try {
            queue = jmsContext.createQueue(null);

            jmsProducer.send(queue, "testQueueNameNull_TCP");

            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m.getText());
        } catch (JMSRuntimeException e) {
            System.out.println("Expected JMSRuntimeException occured in testQueueNameNull_TcpIp");
            exceptionFlag = true;
            e.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameNull_TcpIp failed: Expected JMSRuntimeException was not seen");
    }

    public void testQueueNameEmptyString_B(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        try {
            queue = jmsContext.createQueue("");

            jmsProducer.send(queue, "testQueueNameEmpty_B");

        } catch (JMSRuntimeException e) {
            System.out.println("Expected JMSRuntimeException occured in testQueueNameEmptyString_B");
            exceptionFlag = true;
            e.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameEmptyString_B failed: Expected JMSRuntimeException was not seen");

    }

    public void testQueueNameEmptyString_TcpIp(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        try {
            queue = jmsContext.createQueue("");

            jmsProducer.send(queue, "testQueueNameEmptyString_TcpIp");

        } catch (JMSRuntimeException e) {
            System.out.println("Expected JMSRuntimeException occured in testQueueNameEmptyString_TcpIp");
            exceptionFlag = true;
            e.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameEmptyString_TcpIp failed: Expected JMSRuntimeException was not seen");
    }

    public void testQueueNameWildChars_B(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        jmsProducer = jmsContext.createProducer();
        try {
            queue = jmsContext.createQueue("ppp*");

            jmsProducer.send(queue, "testQueueNameWildChars_B");
            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m.getText());
        } catch (InvalidDestinationRuntimeException ex) {
            System.out.println("Expected InvalidDestinationRuntimeException seen in testQueueNameWildChars_B");
            exceptionFlag = true;
            ex.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameWildChars_B failed: Expected InvalidDestinationRuntimeException was not seen");

    }

    public void testQueueNameWildChars_TcpIp(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        jmsProducer = jmsContext.createProducer();
        try {
            queue = jmsContext.createQueue("ppp*");

            jmsProducer.send(queue, "testQueueNameWildChars_TcpIp");
            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m.getText());
        } catch (InvalidDestinationRuntimeException ex) {
            System.out.println("Expected InvalidDestinationRuntimeException seen in testQueueNameWildChars_TcpIp");
            exceptionFlag = true;
            ex.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameWildChars_TcpIp failed: Expected InvalidDestinationRuntimeException was not seen");

    }

    public void testQueueNameWithSpaces_B(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("abc xyz");

        jmsProducer.send(queue, "Sending testQueueNameWithSpaces_B");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("Sending testQueueNameWithSpaces_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueNameWithSpaces_B failed: Expected message was not received");

    }

    public void testQueueNameWithSpaces_TcpIp(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("abc xyz");

        jmsProducer.send(queue, "Sending testQueueNameWithSpaces_TcpIp");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("Sending testQueueNameWithSpaces_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueNameWithSpaces_TcpIp failed: Expected message was not received");

    }

    public void testQueueName_temp_B(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("_tempXYZ");

        jmsProducer.send(queue, "testQueueName_temp_B");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testQueueName_temp_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueName_temp_B failed: Expected message was not received");

    }

    public void testQueueName_temp_TcpIp(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("_tempXYZ");

        jmsProducer.send(queue, "testQueueName_temp_TcpIp");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testQueueName_temp_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueNameWithSpaces_TcpIp failed: Expected message was not received");

    }

    public void testQueueNameLong_B(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        jmsProducer.send(queue, "testQueueNameLong_B");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testQueueNameLong_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueNameLong_B failed: Expected message was not received");

    }

    public void testQueueNameLong_TcpIp(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        jmsProducer.send(queue, "testQueueNameLong_TcpIp");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testQueueNameLong_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueNameLong_TcpIp failed: Expected message was not received");

    }

    public void testQueueNameCaseSensitive_B(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        jmsProducer = jmsContext.createProducer();
        try {
            queue = jmsContext.createQueue("MYQUEUE");

            jmsProducer.send(queue, "testQueueNameCaseSensitive_B");
        } catch (JMSRuntimeException ex1) {
            System.out.println("Expected JMSRuntimeException occured in testQueueNameCaseSensitive_B");
            exceptionFlag = true;
            ex1.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testQueueNameCaseSensitive_B failed: Expected JMSRuntimeException was not seen");

    }

    public void testQueueNameCaseSensitive_TcpIp(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        jmsProducer = jmsContext.createProducer();
        try {
            queue = jmsContext.createQueue("MYQUEUE");

            jmsProducer.send(queue, "testQueueNameCaseSensitive_TcpIp");
        } catch (JMSRuntimeException ex1) {
            System.out.println("Expected JMSRuntimeException occured in testQueueNameCaseSensitive_TcpIp");
            exceptionFlag = true;
            ex1.printStackTrace();
        }
        if (!exceptionFlag)
            throw new WrongException("testQueueNameCaseSensitive_TcpIp failed: Expected JMSRuntimeException was not seen");

    }

    public void testQueueNameQUEUE_B(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFBindings.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("QUEUE/queue");

        jmsProducer.send(queue, "testQueueNameQUEUE_B");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testQueueNameQUEUE_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueNameQUEUE_B failed: Expected message was not received");

    }

    public void testQueueNameQUEUE_TcpIp(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsQCFTCP.createContext();

        jmsProducer = jmsContext.createProducer();
        queue = jmsContext.createQueue("QUEUE/queue");

        jmsProducer.send(queue, "testQueueNameQUEUE_TcpIp");
        jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testQueueNameQUEUE_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueNameQUEUE_TcpIp failed: Expected message was not received");
    }

    public void testTopicNameNull_B(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        try {

            topic = jmsContext.createTopic(null);
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameNull_B");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m1.getText());
        } catch (JMSRuntimeException ex) {
            System.out.println("Expected JMSRuntimeException seen in testTopicNameNull_B");
            exceptionFlag = true;
            ex.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testTopicNameNull_B failed: Expected JMSRuntimeException was not seen");
    }

    public void testTopicNameNull_TcpIp(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        try {

            topic = jmsContext.createTopic(null);
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameNull_TcpIP");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m1.getText());
        } catch (JMSRuntimeException ex) {
            System.out.println("Expected JMSRuntimeException seen in testTopicNameNull_TcpIp");
            exceptionFlag = true;
            ex.printStackTrace();
        }

        if (!exceptionFlag)
            throw new WrongException("testTopicNameNull_TcpIp failed: Expected JMSRuntimeException was not seen");

    }

    public void testTopicNameEmptyString_B(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        try {
            topic = jmsContext.createTopic("");
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameEmptyString_B");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println("Expected message: " + m1.getText());
        } catch (JMSRuntimeException ex) {
            System.out.println("Expected JMSRuntimeException seen in testTopicNameEmptyString_B");
            exceptionFlag = true;
            ex.printStackTrace();
        }

        if (exceptionFlag)
            throw new WrongException("testTopicNameEmptyString_B failed: Unexpected JMSRuntimeException was seen");

    }

    public void testTopicNameEmptyString_TcpIp(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        try {

            topic = jmsContext.createTopic("");
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameEmptyString_TcpIp");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println("Expected message: " + m1.getText());
        } catch (JMSRuntimeException ex) {
            System.out.println("Expected JMSRuntimeException seen in testTopicNameEmptyString_TcpIp");
            exceptionFlag = true;
            ex.printStackTrace();
        }
        if (exceptionFlag)
            throw new WrongException("testTopicNameEmptyString_TcpIp failed: Unexpected JMSRuntimeException was seen");

    }

    public void testTopicNameWildChars_B(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        try {

            topic = jmsContext.createTopic("ppp*");
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameWildChars_B");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m1.getText());
        } catch (JMSRuntimeException ex) {
            System.out.println("Expected JMSRuntimeException seen in testTopicNameWildChars_B");
            exceptionFlag = true;
            ex.printStackTrace();
        }
        if (!exceptionFlag)
            throw new WrongException("testTopicNameWildChars_B failed: Expected JMSRuntimeException was not seen");
    }

    public void testTopicNameWildChars_TcpIp(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        try {

            topic = jmsContext.createTopic("ppp*");
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameWildChars_TcpIp");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println(m1.getText());
        } catch (JMSRuntimeException ex) {
            System.out.println("Expected JMSRuntimeException seen in testTopicNameWildChars_TcpIp");
            exceptionFlag = true;
            ex.printStackTrace();
        }
        if (!exceptionFlag)
            throw new WrongException("testTopicNameWildChars_TcpIp failed: Expected JMSRuntimeException was not seen");

    }

    public void testTopicNameWithSpaces_B(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        topic = jmsContext.createTopic("New Topic");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicNameWithSpaces_B");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicNameWithSpaces_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicNameWithSpaces_B failed: Expected message was not received");

    }

    public void testTopicNameWithSpaces_TcpIp(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        topic = jmsContext.createTopic("New Topic");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicNameWithSpaces_TcpIp");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicNameWithSpaces_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicNameWithSpaces_TcpIp failed: Expected message was not received");

    }

    public void testTopicName_temp_B(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        topic = jmsContext.createTopic("_tempTopic");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicName_temp_B");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicName_temp_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicName_temp_B failed: Expected message was not received");

    }

    public void testTopicName_temp_TcpIp(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        topic = jmsContext.createTopic("_tempTopic");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicName_temp_TcpIp");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicName_temp_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicName_temp_TcpIp failed: Expected message was not received");

    }

    public void testTopicNameLong_B(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        topic = jmsContext.createTopic("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicNameLong_B");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicNameLong_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicNameLong_B failed: Expected message was not received");

    }

    public void testTopicNameLong_TcpIp(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        topic = jmsContext.createTopic("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicNameLong_TcpIp");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicNameLong_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicNameLong_TcpIp failed: Expected message was not received");

    }

    public void testTopicNameCaseSensitive_B(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        try {
            topic = jmsContext.createTopic("NEWTOPIC");
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameCaseSensitive_B");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println("Expected message: " + m1.getText());
        } catch (JMSRuntimeException ex1) {
            System.out.println("Expected JMSRuntimeException occured in testTopicNameCaseSensitive_B");
            exceptionFlag = true;
            ex1.printStackTrace();
        }
        if (exceptionFlag)
            throw new WrongException("testTopicNameCaseSensitive_B failed: Unexpected JMSRuntime Exception was seen");
    }

    public void testTopicNameCaseSensitive_TcpIp(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        try {
            topic = jmsContext.createTopic("NEWTOPIC");
            jmsConsumer = jmsContext.createConsumer(topic);

            jmsContext.createProducer().send(topic, "testTopicNameCaseSensitive_TcpIp");

            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);
            System.out.println("Expected message: " + m1.getText());
        } catch (JMSRuntimeException ex1) {
            System.out.println("Expected JMSRuntimeException occured in testTopicNameCaseSensitive_TcpIp");
            exceptionFlag = true;
            ex1.printStackTrace();
        }

        if (exceptionFlag)
            throw new WrongException("testTopicNameCaseSensitive_TcpIp failed: Unexpected JMSRuntime Exception was seen");
    }

    public void testTopicNameTOPIC_B(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        topic = jmsContext.createTopic("TOPIC/topic");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicNameTOPIC_B");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicNameTOPIC_B")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicNameTOPIC_B failed: Expected message was not received");

    }

    public void testTopicNameTOPIC_TcpIp(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        topic = jmsContext.createTopic("TOPIC/topic");
        jmsConsumer = jmsContext.createConsumer(topic);

        jmsContext.createProducer().send(topic, "testTopicNameTOPIC_TcpIp");

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Expected message: " + m.getText());

        if (!(m != null && m.getText().equals("testTopicNameTOPIC_TcpIp")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicNameTOPIC_TcpIp failed: Expected message was not received");
    }

    public QueueConnectionFactory getQCF(String name) throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/" + name);

        return cf1;

    }

    public Queue getQueue() throws NamingException {

        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/jndi_INPUT_Q");

        return queue;
    }

    public static TopicConnectionFactory getTCF(String name) throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/" + name);

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
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

}
