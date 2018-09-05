package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.InvalidDestinationException;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.MessageNotWriteableRuntimeException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
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
public class JMSProducerServlet extends HttpServlet {
    public static final String JMXMessage = "This is MessagingMBeanServlet.";

    public final static String MBEAN_TYPE_ME = "WEMMessagingEngine";

    public static boolean sessionValue = false;
    public static boolean connectionStart = false;
    public static boolean flag = false;
    public static boolean compFlag = false;
    public static boolean exp = false;
    public static QueueConnectionFactory QCFBindings;
    public static QueueConnectionFactory QCFTCP;

    public static TopicConnectionFactory TCFBindings;
    public static TopicConnectionFactory TCFTCP;

    public static boolean exceptionFlag;
    public static Queue queue;
    public static Queue queue1;
    public static Queue queue2;
    public static Queue queue3;
    public static Topic topic;
    public static Topic topic2;

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

            topic2 = (Topic) new InitialContext()
                            .lookup("java:comp/env/eis/topic5");

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
        final TraceComponent tc = Tr.register(JMSProducerServlet.class); // injection
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
            System.out.println(" Starting : " + test);
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

    // 118071_1 JMSProducer send(Destination destination,Message message)
    // 118071_1_1_Q : Sends the message to the specified queue using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.
    // Applications using the simplified API may also set these message headers
    // on the JMSProducer. Any message headers set using these methods will
    // override any values that have been set directly on the message.

    public void testJMSProducerSendMessage_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);

        Message msg = jmsContextQCFBindings.createMessage();
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, msg);

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == (queue1)))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        Message msg = jmsContextQCFTCP.createMessage();
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, msg);

        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == (queue1)))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_TCP_SecOff failed");

    }

    // 118071_1_3_Q InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid queue
    public void testJMSProducerSendMessage_InvalidDestination_B_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        Message msg = jmsContextQCFBindings.createMessage();

        Queue queue = null;
        JMSProducer producer = jmsContextQCFBindings.createProducer();
        try {
            producer.send(queue, msg);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_InvalidDestination_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_InvalidDestination_TCP_SecOff(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        Message msg = jmsContextQCFTCP.createMessage();

        Queue queue = null;
        JMSProducer producer = jmsContextQCFTCP.createProducer();
        try {
            producer.send(queue, msg);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_InvalidDestination_TCP_SecOff failed");

    }

    // 118071_1_2_Q MessageFormatRuntimeException - if an invalid message is
    // specified.
    // 118071_1_4_Q Test with message as null

    public void testJMSProducerSendMessage_NullMessage_B_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        Message msg = null;

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        try {
            producer.send(queue, msg);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFBindings.close();

        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_NullMessage_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_NullMessage_TCP_SecOff(
                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        Message msg = null;

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        try {
            producer.send(queue, msg);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_NullMessage_TCP_SecOff failed");

    }

    // 118071_1_5_Q Test with message as empty string
    public void testJMSProducerSendMessage_EmptyMessage_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        TextMessage tmsg = jmsContextQCFBindings.createTextMessage("");

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        try {
            producer.send(queue, tmsg);
        } catch (Exception ex) {
            ex.printStackTrace();

        }

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(qb);

        jmsContextQCFBindings.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_EmptyMessage_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_EmptyMessage_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        TextMessage tmsg = jmsContextQCFTCP.createTextMessage("");

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        try {
            producer.send(queue, tmsg);
        } catch (Exception ex) {
            ex.printStackTrace();

        }

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(qb);

        jmsContextQCFTCP.createConsumer(queue).receive(30000);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_EmptyMessage_TCP_SecOff failed");

    }

    // 118071_1_6_Q MessageNotWriteableRuntimeException - if this JMSProducer
    // has been configured to set a message property, but the message's
    // properties are read-only

    public void testJMSProducerSendMessage_NotWriteable_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        StreamMessage msg = jmsContextQCFBindings.createStreamMessage();
        msg.reset();

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        producer.send(queue, msg);
        StreamMessage msg1 = (StreamMessage) jmsConsumer.receive(30000);
        producer.setProperty("Role", "Tester");

        try {
            producer.send(queue, msg1);
        } catch (MessageNotWriteableRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_NotWriteable_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_NotWriteable_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        StreamMessage msg = jmsContextQCFTCP.createStreamMessage();
        msg.reset();

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        producer.send(queue, msg);
        StreamMessage msg1 = (StreamMessage) jmsConsumer.receive(30000);
        producer.setProperty("Role", "Tester");

        try {
            producer.send(queue, msg1);
        } catch (MessageNotWriteableRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_NotWriteable_TCP_SecOff failed");

    }

    // 118071_1_7_T Sends a message to the specified topic using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.
    public void testJMSProducerSendMessage_Topic_B_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        TextMessage msg = jmsContextTCFBindings.createTextMessage("Test");
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, msg);

        TextMessage recvdMessage = null;
        String msg1 = null;
        recvdMessage = (TextMessage) jmsConsumer.receive(30000);
        msg1 = recvdMessage.getText();

        if (!(msg1.equals("Test")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_Topic_TCP_SecOff(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        TextMessage msg = jmsContextTCFTCP.createTextMessage("Test");
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, msg);

        TextMessage recvdMessage = null;
        String msg1 = null;
        recvdMessage = (TextMessage) jmsConsumer.receive(30000);
        msg1 = recvdMessage.getText();

        if (!(msg1.equals("Test")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_NullMessage_Topic_B_SecOff(
                                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        Message msg = null;

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        try {
            producer.send(topic, msg);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_NullMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_NullMessage_Topic_TCP_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        Message msg = null;

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        try {
            producer.send(topic, msg);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_NullMessage_Topic_TCP_SecOff failed");

    }

    // //118071_1_9_T InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid topic

    public void testJMSProducerSendMessage_InvalidDestinationTopic_B_SecOff(
                                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        Message msg = jmsContextTCFBindings.createMessage();

        Topic topic1 = null;

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        try {
            producer.send(topic1, msg);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextTCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_InvalidDestinationTopic_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_InvalidDestinationTopic_TCP_SecOff(
                                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        Message msg = jmsContextTCFTCP.createMessage();

        Topic topic1 = null;

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        try {
            producer.send(topic1, msg);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextTCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMessage_InvalidDestinationTopic_B_SecOff failed");

    }

    // 118071_1_10_T MessageNotWriteableRuntimeException - if this JMSProducer
    // has been configured to set a message property, but the message's
    // properties are read-only

    // Bindings and SecurityOff

    public void testJMSProducerSendMessage_NotWriteableTopic_B_SecOff(HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        StreamMessage msg = jmsContextTCFBindings.createStreamMessage();
        msg.reset();

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        producer.send(topic, msg);
        StreamMessage msg1 = (StreamMessage) jmsConsumer.receive(30000);
        producer.setProperty("Role", "Tester");

        try {
            producer.send(topic, msg1);
        } catch (MessageNotWriteableRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (!(exceptionFlag = true))
            throw new WrongException("testJMSProducerSendMessage_NotWriteableTopic_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_NotWriteableTopic_TCP_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        StreamMessage msg = jmsContextTCFTCP.createStreamMessage();
        msg.reset();

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        producer.send(topic, msg);
        StreamMessage msg1 = (StreamMessage) jmsConsumer.receive(30000);
        producer.setProperty("Role", "Tester");

        try {
            producer.send(topic, msg1);
        } catch (MessageNotWriteableRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if (!(exceptionFlag = true))
            throw new WrongException("testJMSProducerSendMessage_NotWriteableTopic_TCP_SecOff failed");

    }

    // 118071_1_12_T Test with message as empty string

    public void testJMSProducerSendMessage_EmptyMessage_Topic_B_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        TextMessage tmsg = jmsContextTCFBindings.createTextMessage("");

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer producer = jmsContextTCFBindings.createProducer();

        producer.send(topic, tmsg);

        TextMessage recvdMessage = null;
        String msg1 = null;

        recvdMessage = (TextMessage) jmsConsumer.receive(30000);
        msg1 = recvdMessage.getText();

        if (!(msg1.equals("")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_EmptyMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendMessage_EmptyMessage_Topic_TCP_SecOff(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        TextMessage tmsg = jmsContextTCFTCP.createTextMessage("");

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer producer = jmsContextTCFTCP.createProducer();

        producer.send(topic, tmsg);

        TextMessage recvdMessage = null;
        String msg1 = null;

        recvdMessage = (TextMessage) jmsConsumer.receive(30000);
        msg1 = recvdMessage.getText();

        if (!(msg1.equals("")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMessage_EmptyMessage_Topic_B_SecOff failed");

    }

    // 118071_2 JMSProducer send(Destination destination, String body)
    // 118071_2_1_Q Send a TextMessage with the specified body to the specified
    // queue, using any send options, message properties and message headers
    // that have been defined on this JMSProducer.

    public void testJMSProducerSendTextMessage_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, "This is the messageBody");

        int numMsgs = 0;

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        numMsgs = getMessageCount(qb);
        String msgBody = jmsConsumer.receive(30000).getBody(String.class);

        if (!(numMsgs == 1 && msgBody.equals("This is the messageBody")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, "This is the messageBody");

        int numMsgs = 0;

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        numMsgs = getMessageCount(qb);
        String msgBody = jmsConsumer.receive(30000).getBody(String.class);

        if (!(numMsgs == 1 && msgBody.equals("This is the messageBody")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_TCP_SecOff failed");

    }

    // 118071_2_3_Q InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid queue

    public void testJMSProducerSendTextMessage_InvalidDestination_B_SecOff(
                                                                           HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        Message msg = jmsContextQCFBindings.createMessage();

        Queue queue = null;
        JMSProducer producer = jmsContextQCFBindings.createProducer();
        try {
            producer.send(queue, "This is the messageBody");
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendTextMessage_InvalidDestination_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_InvalidDestination_TCP_SecOff(
                                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        Message msg = jmsContextQCFTCP.createMessage();

        Queue queue = null;
        JMSProducer producer = jmsContextQCFTCP.createProducer();
        try {
            producer.send(queue, "This is the messageBody");
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFTCP.close();

        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendTextMessage_InvalidDestination_TCP_SecOff failed");

    }

    // 118071_2_4_Queue If a null value is specified for body then a
    // TopicextMessage with no body will be sent.

    public void testJMSProducerSendTextMessage_NullMessageBody_B_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        String msg = null;
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, msg);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(qb);

        jmsConsumer.receive(30000);

        if (!(numMsgs == 1
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_NullMessageBody_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_NullMessageBody_TCP_SecOff(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        String msg = null;
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, msg);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(qb);

        jmsConsumer.receive(30000);

        if (!(numMsgs == 1
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_NullMessageBody_TCP_SecOff failed");

    }

    // 118071_2_5_Queue Test with empty string for the body

    public void testJMSProducerSendTextMessage_EmptyMessage_B_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        try {
            producer.send(queue, "");
        } catch (Exception ex) {
            ex.printStackTrace();

        }

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(qb);

        String recvdMessage = jmsConsumer.receive(30000).getBody(
                                                                 String.class);

        if (!(recvdMessage.equals("") && numMsgs == 1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_EmptyMessage_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_EmptyMessage_TCP_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        try {
            producer.send(queue, "");
        } catch (Exception ex) {
            ex.printStackTrace();

        }

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;

        numMsgs = getMessageCount(qb);

        String recvdMessage = jmsConsumer.receive(30000).getBody(
                                                                 String.class);

        if (!(recvdMessage.equals("") && numMsgs == 1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_EmptyMessage_TCP_SecOff failed");

    }

    // 118071_2_6_Topic :Send a TextMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.
    public void testJMSProducerSendTextMessage_Topic_B_SecOff(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType")
                        .send(topic, "This is message body");

        String recvdMessage = jmsConsumer.receive(30000).getBody(
                                                                 String.class);

        if (!(recvdMessage.equals("This is message body")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_Topic_TCP_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType")
                        .send(topic, "This is message body");

        String recvdMessage = jmsConsumer.receive(30000).getBody(
                                                                 String.class);

        if (!(recvdMessage.equals("This is message body")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_Topic_TCP_SecOff failed");

    }

    // 118071_2_8_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid topic
    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOff(
                                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        Topic topic1 = null;

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        try {
            producer.send(topic1, "This is the message body");
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextTCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOff(
                                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        Topic topic1 = null;

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        try {
            producer.send(topic1, "This is the message body");
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextTCFTCP.close();

        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOff failed");

    }

    // 118071_2_9_Topic If a null value is specified for body then a TextMessage
    // with no body will be sent.
    public void testJMSProducerSendTextMessage_NullMessage_Topic_B_SecOff(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextTCFBindings.createProducer();

        String msg = null;
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, msg);

        jmsConsumer.receive(30000);

        if (!(producer.getJMSCorrelationID().equals("TestCorrelID")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_NullMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_NullMessage_Topic_TCP_SecOff(
                                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        JMSProducer producer = jmsContextTCFTCP.createProducer();

        String msg = null;
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, msg);

        jmsConsumer.receive(30000);

        if (!(producer.getJMSCorrelationID().equals("TestCorrelID")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_NullMessage_Topic_B_SecOff failed");

    }

    // 118071_2_10_Topic Test with empty string for the body
    public void testJMSProducerSendTextMessage_EmptyMessage_Topic_B_SecOff(
                                                                           HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer producer = jmsContextTCFBindings.createProducer();

        producer.send(topic, "");

        String recvdMessage = jmsConsumer.receive(30000).getBody(
                                                                 String.class);

        if (!(recvdMessage.equals("")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_EmptyMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendTextMessage_EmptyMessage_Topic_TCP_SecOff(
                                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer producer = jmsContextTCFTCP.createProducer();

        producer.send(topic, "");

        String recvdMessage = jmsConsumer.receive(30000).getBody(
                                                                 String.class);

        if (!(recvdMessage.equals("")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendTextMessage_EmptyMessage_Topic_B_SecOff failed");

    }

    // 118071_3 JMSProducer send(Destination destination,Map<String,Object>
    // body)
    // 118071_3_1_Queue Send a MapMessage with the specified body to the
    // specified queue, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.
    public void testJMSProducerSendMapMessage_B_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextQCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, mapMessage);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        numMsgs = getMessageCount(qb);

        boolean correctMapBody = jmsConsumer.receive(30000)
                        .getBody(java.util.Map.class).containsValue(val);
        if (!(numMsgs == 1 && correctMapBody == true
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_B_SecOff failed");

    }

    public void testJMSProducerSendMapMessage_TCP_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextQCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, mapMessage);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;
        numMsgs = getMessageCount(qb);

        boolean correctMapBody = jmsConsumer.receive(30000)
                        .getBody(java.util.Map.class).containsValue(val);
        if (!(numMsgs == 1 && correctMapBody == true
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_B_SecOff failed");

    }

    // 118071_3_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue

    public void testJMSProducerSendMapMessage_InvalidDestination_B_SecOff(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextQCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").setJMSReplyTo(queue1)
                            .send(null, mapMessage);
        } catch (InvalidDestinationRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMapMessage_InvalidDestination_B_SecOff failed");

    }

    public void testJMSProducerSendMapMessage_InvalidDestination_TCP_SecOff(
                                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextQCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").setJMSReplyTo(queue1)
                            .send(null, mapMessage);
        } catch (InvalidDestinationRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMapMessage_InvalidDestination_TCP_SecOff failed");

    }

    // 118071_3_4_Queue If a null value is specified then a MapMessage with no
    // map entries will be sent.

    public void testJMSProducerSendMapMessage_Null_B_SecOff(HttpServletRequest request,
                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        boolean compare = false;
        boolean flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = null;

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").setJMSReplyTo(queue1).send(queue, mapMessage);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            compare = true;

        }

        jmsConsumer.receive(30000);
        // JMSProducer producer = jmsContext.createProducer();
        MapMessage mapMessage1 = jmsContextQCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = null;

        mapMessage1.setObject(propName, val);

        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, mapMessage1);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);

        if ((numMsgs == 1
             && producer.getJMSCorrelationID().equals("TestCorrelID")
             && producer.getJMSType().equals("NewTestType")
             && producer.getJMSReplyTo() == queue1))
            flag = true;

        if (!(compare == true && flag == true))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_Null_B_SecOff failed");

    }

    public void testJMSProducerSendMapMessage_Null_TCP_SecOff(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        boolean compare = false;
        boolean flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = null;

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").setJMSReplyTo(queue1).send(queue, mapMessage);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            compare = true;
        }

        // JMSProducer producer = jmsContext.createProducer();
        MapMessage mapMessage1 = jmsContextQCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = null;

        mapMessage1.setObject(propName, val);

        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, mapMessage1);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);

        if (numMsgs == 1
            && producer.getJMSCorrelationID().equals("TestCorrelID")
            && producer.getJMSType().equals("NewTestType")
            && producer.getJMSReplyTo() == queue1)
            flag = true;

        if (!(compare == true && flag == true))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_Null_B_SecOff failed");

    }

    // 118071_3_5_Topic Send a MapMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendMapMessage_Topic_B_SecOff(HttpServletRequest request,
                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextTCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, mapMessage);

        boolean correctMapBody = jmsConsumer.receive(30000)
                        .getBody(java.util.Map.class).containsValue(val);

        if (!(correctMapBody == true
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))

            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendMapMessage_Topic_TCP_SecOff(
                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextTCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, mapMessage);

        boolean correctMapBody = jmsConsumer.receive(30000)
                        .getBody(java.util.Map.class).containsValue(val);

        if (!(correctMapBody == true
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))

            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessage_Topic_TCP_SecOff failed");

    }

    // 118071_3_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue

    public void testJMSProducerSendMapMessageTopic_InvalidDestination_B_SecOff(
                                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextTCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextTCFBindings.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").send(null, mapMessage);
        } catch (InvalidDestinationRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMapMessageTopic_InvalidDestination_TCP_SecOff failed");

    }

    public void testJMSProducerSendMapMessageTopic_InvalidDestination_TCP_SecOff(
                                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        MapMessage mapMessage = jmsContextTCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextTCFTCP.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").send(null, mapMessage);
        } catch (InvalidDestinationRuntimeException ex) {

            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendMapMessageTopic_InvalidDestination_TCP_SecOff failed");

    }

    // 118071_3_8_Topic If a null value is specified then a MapMessage with no
    // map entries will be sent.
    public void testJMSProducerSendMapMessageTopic_Null_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        // MapMessage mapMessage= null;

        /*
         * JMSProducer producer = jmsContext.createProducer(); try{
         * producer.setJMSCorrelationID("TestCorrelID")
         * .setJMSType("NewTestType") .send(topic,mapMessage);
         * }catch(Exception ex){ ex.printStackTrace(); }
         */

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        MapMessage mapMessage = jmsContextTCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = null;

        mapMessage.setObject(propName, val);

        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(topic)
                        .send(topic, mapMessage);

        //  System.out.println(jmsConsumer.receiveBodyNoWait(java.util.Map.class).toString());

        if (!(producer.getJMSCorrelationID().equals("TestCorrelID")
        && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessageTopic_Null_B_SecOff failed");

    }

    public void testJMSProducerSendMapMessageTopic_Null_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        // MapMessage mapMessage= null;

        /*
         * JMSProducer producer = jmsContext.createProducer(); try{
         * producer.setJMSCorrelationID("TestCorrelID")
         * .setJMSType("NewTestType") .send(topic,mapMessage);
         * }catch(Exception ex){ ex.printStackTrace(); }
         */

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        MapMessage mapMessage = jmsContextTCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = null;

        mapMessage.setObject(propName, val);

        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(topic)
                        .send(topic, mapMessage);

        //  System.out.println(jmsConsumer.receiveBodyNoWait(java.util.Map.class).toString());

        if (!(producer.getJMSCorrelationID().equals("TestCorrelID")
        && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendMapMessageTopic_Null_TCP_SecOff failed");

    }

    // 118071_4 JMSProducer send(Destination destination,byte[] body)
    // 118071_4_1_Queue Send a BytesMessage with the specified body to the
    // specified queue, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.
    public void testJMSProducerSendByteMessage_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, content);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);

        String recvdByteBody = Arrays.toString(jmsConsumer
                        .receiveBodyNoWait(byte[].class));
        //  System.out.println("Received Bytes[] body" + recvdByteBody);

        if (!(numMsgs == 1 && recvdByteBody.equals("[127, 0]")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_B_SecOff failed");

    }

    public void testJMSProducerSendByteMessage_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, content);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);

        String recvdByteBody = Arrays.toString(jmsConsumer
                        .receiveBodyNoWait(byte[].class));
        //  System.out.println("Received Bytes[] body" + recvdByteBody);

        if (!(numMsgs == 1 && recvdByteBody.equals("[127, 0]")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_TCP_SecOff failed");

    }

    // 118071_4_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid destination.

    public void testJMSProducerSendByteMessage_InvalidDestination_B_SecOff(
                                                                           HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").setJMSReplyTo(queue1)
                            .send(null, content);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendByteMessage_InvalidDestination_B_SecOff failed");

    }

    public void testJMSProducerSendByteMessage_InvalidDestination_TCP_SecOff(
                                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").setJMSReplyTo(queue1)
                            .send(null, content);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendByteMessage_InvalidDestination_TCP_SecOff failed");

    }

    // 118071_4_4_Queue If a null value is specified then a BytesMessage with no
    // body will be sent.
    public void testJMSProducerSendByteMessage_Null_B_SecOff(HttpServletRequest request,
                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        byte[] content = null;

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID").setJMSType("NewTestType").setJMSReplyTo(queue1).send(queue, content);

        Message msg = jmsConsumer.receive(30000);

        if ((msg.getBody(byte[].class) == null
             && producer.getJMSCorrelationID().equals("TestCorrelID")
             && producer.getJMSType().equals("NewTestType")
             && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (!exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_Null_B_SecOff failed");

    }

    public void testJMSProducerSendByteMessage_Null_TCP_SecOff(
                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        byte[] content = null;

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, content);

        Message msg = jmsConsumer.receive(30000);

        if ((msg.getBody(byte[].class) == null
             && producer.getJMSCorrelationID().equals("TestCorrelID")
             && producer.getJMSType().equals("NewTestType")
             && producer.getJMSReplyTo() == queue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (!exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_Null_TCP_SecOff failed");

    }

    // 118071_4_5_Topic Send a BytesMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendByteMessage_Topic_B_SecOff(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, content);

        String recvdByteBody = Arrays.toString(jmsConsumer
                        .receiveBodyNoWait(byte[].class));
        // System.out.println("Received Bytes[] body" + recvdByteBody);

        if (!(recvdByteBody.equals("[127, 0]")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendByteMessage_Topic_TCP_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, content);

        String recvdByteBody = Arrays.toString(jmsConsumer
                        .receiveBodyNoWait(byte[].class));
        // System.out.println("Received Bytes[] body" + recvdByteBody);

        if (!(recvdByteBody.equals("[127, 0]")
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_Topic_B_SecOff failed");

    }

    // 118071_4_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid destination.

    public void testJMSProducerSendByteMessage_InvalidDestination_Topic_B_SecOff(
                                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").send(null, content);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendByteMessage_InvalidDestination_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendByteMessage_InvalidDestination_Topic_TCP_SecOff(
                                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        byte[] content = new byte[] { 127, 0 };

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").send(null, content);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendByteMessage_InvalidDestination_Topic_TCP_SecOff failed");

    }

    // 118071_4_8_Topic If a null value is specified then a BytesMessage with no
    // body will be sent.

    public void testJMSProducerSendByteMessage_Null_Topic_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        byte[] content = null;

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, content);

        Message msg = jmsConsumer.receive(30000);

        if (msg.getBody(byte[].class) == null
            && msg.getJMSCorrelationID().equals("TestCorrelID")
            && msg.getJMSType().equals("NewTestType"))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (!exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_Null_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendByteMessage_Null_Topic_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        byte[] content = null;

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").send(topic, content);

        Message msg = jmsConsumer.receive(30000);

        if (msg.getBody(byte[].class) == null
            && msg.getJMSCorrelationID().equals("TestCorrelID")
            && msg.getJMSType().equals("NewTestType"))
            exceptionFlag = true;

        exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (!exceptionFlag)
            throw new WrongException("testJMSProducerSendByteMessage_Null_Topic_TCP_SecOff failed");

    }

    // 118071_5 JMSProducer send(Destination destination,Serializable body)
    // 118071_5_1_Queue Send an ObjectMessage with the specified body to the
    // specified queue using any send options, message properties and message
    // headers that have been defined on this JMSProducer.
    public void testJMSProducerSendObjectMessage_B_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        numMsgs = getMessageCount(qb);
        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);
        // System.out.println("Received Object body" + msgRecvd);

        if (!(numMsgs == 1 && msgRecvd.equals(objBody)
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))

            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendObjectMessage_B_SecOff failed");

    }

    public void testJMSProducerSendObjectMessage_TCP_SecOff(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;
        numMsgs = getMessageCount(qb);
        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);
        // System.out.println("Received Object body" + msgRecvd);

        if (!(numMsgs == 1 && msgRecvd.equals(objBody)
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")
              && producer.getJMSReplyTo() == queue1))

            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendObjectMessage_TCP_B_SecOff failed");

    }

    // 118071_5_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue.
    public void testJMSProducerSendObjectMessage_InvalidDestination_B_SecOff(
                                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").setJMSReplyTo(queue1)
                            .send(null, (Serializable) objBody);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_InvalidDestination_B_SecOff failed");

    }

    public void testJMSProducerSendObjectMessage_InvalidDestination_TCP_SecOff(
                                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").setJMSReplyTo(queue1)
                            .send(null, (Serializable) objBody);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_InvalidDestination_TCP_SecOff failed");

    }

    // 118071_4_4_Queue If a null value is specified then a BytesMessage with no
    // body will be sent.

    public void testJMSProducerSendObjectMessage_Null_B_SecOff(
                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        //jmsConsumer.receive(30000);

        Object objBody = null;

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);
        try {
            Object msgRecvd = jmsConsumer
                            .receiveBodyNoWait(Serializable.class);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_Null_B_SecOff failed");

    }

    public void testJMSProducerSendObjectMessage_Null_TCP_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        //jmsConsumer.receive(30000);

        Object objBody = null;

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType").setJMSReplyTo(queue1)
                        .send(queue, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);
        try {
            Object msgRecvd = jmsConsumer
                            .receiveBodyNoWait(Serializable.class);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_Null_TCP_SecOff failed");

    }

    // 118071_5_5_Topic Send an ObjectMessage with the specified body to the
    // specified topic using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendObjectMessage_Topic_B_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType")
                        .send(topic, (Serializable) objBody);

        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);
        //  System.out.println("Received Object body" + msgRecvd);

        if (!(msgRecvd.equals(objBody)
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))

            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendObjectMessage_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendObjectMessage_Topic_TCP_SecOff(
                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType")
                        .send(topic, (Serializable) objBody);

        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);
        //  System.out.println("Received Object body" + msgRecvd);

        if (!(msgRecvd.equals(objBody)
              && producer.getJMSCorrelationID().equals("TestCorrelID")
              && producer.getJMSType().equals("NewTestType")))

            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testJMSProducerSendObjectMessage_Topic_TCP_SecOff failed");

    }

    // 118071_5_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue.
    public void testJMSProducerSendObjectMessage_InvalidDestination_Topic_B_SecOff(
                                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType")
                            .send(null, (Serializable) objBody);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_InvalidDestination_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendObjectMessage_InvalidDestination_Topic_TCP_SecOff(
                                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        //jmsConsumer.receive(30000);

        Object objBody = new String("This is the Message body.");

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        try {
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType")
                            .send(null, (Serializable) objBody);
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_InvalidDestination_Topic_TCP_SecOff failed");

    }

    // 118071_5_8_Topic If a null value is specified then an ObjectMessage with
    // no body will be sent.
    public void testJMSProducerSendObjectMessage_Null_Topic_B_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        //jmsConsumer.receive(30000);

        Object objBody = null;

        JMSProducer producer = jmsContextTCFBindings.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType")
                        .send(topic, (Serializable) objBody);

        try {
            Object msgRecvd = jmsConsumer
                            .receiveBodyNoWait(Serializable.class);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_Null_Topic_B_SecOff failed");

    }

    public void testJMSProducerSendObjectMessage_Null_Topic_TCP_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic2);
        //jmsConsumer.receive(30000);

        Object objBody = null;

        JMSProducer producer = jmsContextTCFTCP.createProducer();
        producer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType")
                        .send(topic2, (Serializable) objBody);

        try {
            Object msgRecvd = jmsConsumer
                            .receiveBodyNoWait(Serializable.class);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendObjectMessage_Null_Topic_TCF_SecOff failed");

    }

    // 118073_1_1 Clears any message properties set on this JMSProducer
    /*
     * public void testClearProperties(HttpServletRequest request,
     * HttpServletResponse response) throws Throwable {
     * exceptionFlag=false;
     * 
     * JMSContext jmsContextQCFBindings = QCFBindings .createContext();
     * JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
     * 
     * jmsProducer.setProperty("BooleanValue", true);
     * 
     * jmsProducer.setProperty("StringValue", "Tester");
     * 
     * byte propValue = 100;
     * 
     * jmsProducer.setProperty("ByteValue", propValue);
     * 
     * jmsProducer.setProperty("DoubleValue", 123.4);
     * 
     * jmsProducer.setProperty("FloatValue", 123.4f);
     * 
     * jmsProducer.setProperty("IntValue", 11111);
     * 
     * jmsProducer.setProperty("LongValue", 1234567890123456L);
     * 
     * short propShort = 32760;
     * 
     * jmsProducer.setProperty("ShortValue", propShort);
     * 
     * jmsProducer.setProperty("ObjectValue", new Integer(1414));
     * 
     * boolean bval = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("Before clearProperties, boolean value set is :"
     * + bval);
     * 
     * String sval = jmsProducer.getStringProperty("StringValue");
     * System.out.println("Before clearProperties , String value set is :"
     * + sval);
     * 
     * Byte byval = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("Before clearProperties , Byte value set is :"
     * + byval);
     * 
     * double dval = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("Before clearProperties , Double value set is :"
     * + dval);
     * 
     * float fval = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("Before clearProperties , Float value set is :"
     * + fval);
     * 
     * int ival = jmsProducer.getIntProperty("IntValue");
     * System.out.print("Before clearProperties , Integer value set is :"
     * + ival);
     * 
     * long lval = jmsProducer.getLongProperty("LongValue");
     * System.out.print("Before clearProperties , Long value set is :"
     * + lval);
     * 
     * short shval = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("Before clearProperties , Short value set is :"
     * + shval);
     * 
     * Object oval = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("Before clearProperties , Object value set is :"
     * + oval);
     * 
     * jmsProducer.clearProperties();
     * 
     * boolean bval1 = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("After clearProperties, boolean value set is :"
     * + bval1);
     * 
     * String sval1 = jmsProducer.getStringProperty("StringValue");
     * System.out.println("After clearProperties , String value set is :"
     * + sval1);
     * 
     * Byte byval1 = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("After clearProperties , Byte value set is :"
     * + byval1);
     * 
     * double dval1 = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("After clearProperties , Double value set is :"
     * + dval1);
     * 
     * float fval1 = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("After clearProperties , Float value set is :"
     * + fval1);
     * 
     * int ival1 = jmsProducer.getIntProperty("IntValue");
     * System.out.print("After clearProperties , Integer value set is :"
     * + ival1);
     * 
     * long lval1 = jmsProducer.getLongProperty("LongValue");
     * System.out.print("After clearProperties , Long value set is :"
     * + lval1);
     * 
     * short shval1 = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("After clearProperties , Short value set is :"
     * + shval1);
     * 
     * Object oval1 = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("After clearProperties , Object value set is :"
     * + oval1);
     * 
     * // if(bval1==false && sval1==null)
     * // setflag("testClearProperties", true);
     * 
     * if (jmsContext != null)
     * jmsContext.close();
     * } catch (Exception ex) {
     * ex.printStackTrace();
     * }
     * 
     * }
     * 
     * public void testClearProperties_TCP(HttpServletRequest request,
     * HttpServletResponse response) throws Throwable {
     * try {
     * QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
     * .lookup("java:comp/env/jndi_JMS_BASE_QCF1");
     * 
     * Queue queue = (Queue) new InitialContext()
     * .lookup("java:comp/env/jndi_INPUT_Q");
     * 
     * JMSContext jmsContext = cf1.createContext();
     * JMSProducer jmsProducer = jmsContext.createProducer();
     * 
     * jmsProducer.setProperty("BooleanValue", true);
     * 
     * jmsProducer.setProperty("StringValue", "Tester");
     * 
     * jmsProducer.setProperty("ByteValue", 100);
     * 
     * jmsProducer.setProperty("DoubleValue", 123.4);
     * 
     * jmsProducer.setProperty("FloatValue", 123.4f);
     * 
     * jmsProducer.setProperty("IntValue", 11111);
     * 
     * jmsProducer.setProperty("LongValue", 1234567890123456L);
     * 
     * jmsProducer.setProperty("ShortValue", 32760);
     * 
     * jmsProducer.setProperty("ObjectValue", new Integer(1414));
     * 
     * boolean bval = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("Before clearProperties, boolean value set is :"
     * + bval);
     * 
     * String sval = jmsProducer.getStringProperty("StringValue");
     * System.out.println("Before clearProperties , String value set is :"
     * + sval);
     * 
     * Byte byval = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("Before clearProperties , Byte value set is :"
     * + byval);
     * 
     * double dval = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("Before clearProperties , Double value set is :"
     * + dval);
     * 
     * float fval = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("Before clearProperties , Float value set is :"
     * + fval);
     * 
     * int ival = jmsProducer.getIntProperty("IntValue");
     * System.out.print("Before clearProperties , Integer value set is :"
     * + ival);
     * 
     * long lval = jmsProducer.getLongProperty("LongValue");
     * System.out.print("Before clearProperties , Long value set is :"
     * + lval);
     * 
     * short shval = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("Before clearProperties , Short value set is :"
     * + shval);
     * 
     * Object oval = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("Before clearProperties , Object value set is :"
     * + oval);
     * 
     * jmsProducer.clearProperties();
     * 
     * boolean bval1 = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("After clearProperties, boolean value set is :"
     * + bval1);
     * 
     * String sval1 = jmsProducer.getStringProperty("StringValue");
     * System.out.println("After clearProperties , String value set is :"
     * + sval1);
     * 
     * Byte byval1 = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("After clearProperties , Byte value set is :"
     * + byval1);
     * 
     * double dval1 = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("After clearProperties , Double value set is :"
     * + dval1);
     * 
     * float fval1 = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("After clearProperties , Float value set is :"
     * + fval1);
     * 
     * int ival1 = jmsProducer.getIntProperty("IntValue");
     * System.out.print("After clearProperties , Integer value set is :"
     * + ival1);
     * 
     * long lval1 = jmsProducer.getLongProperty("LongValue");
     * System.out.print("After clearProperties , Long value set is :"
     * + lval1);
     * 
     * short shval1 = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("After clearProperties , Short value set is :"
     * + shval1);
     * 
     * Object oval1 = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("After clearProperties , Object value set is :"
     * + oval1);
     * 
     * // if(bval1==false && sval1==null)
     * setflag("testClearProperties_TCP", true);
     * 
     * if (jmsContext != null)
     * jmsContext.close();
     * } catch (Exception ex) {
     * ex.printStackTrace();
     * }
     * 
     * }
     */
    // 118073_1_2 Test invoking clearProperties() when there are no properties
    // set
    // 118073_1_3 Test invoking clearProperties() soon after clearProperties()
    // have been invoked

    public void testClearProperties_Notset_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        try {
            jmsProducer.clearProperties();
            flag = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            flag = false;
        }

        jmsProducer.setProperty("Name", "Tester");
        jmsProducer.setProperty("ObjectType", new Integer(1414));

        jmsProducer.clearProperties();

        try {
            jmsProducer.clearProperties();
            compFlag = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            compFlag = false;
        }

        if (!(flag == true && compFlag == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testClearProperties_Notset_B_SecOff failed");

    }

    public void testClearProperties_Notset_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        try {
            jmsProducer.clearProperties();
            flag = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            flag = false;
        }

        jmsProducer.setProperty("Name", "Tester");
        jmsProducer.setProperty("ObjectType", new Integer(1414));

        jmsProducer.clearProperties();

        try {
            jmsProducer.clearProperties();
            compFlag = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            compFlag = false;
        }

        if (!(flag == true && compFlag == true))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testClearProperties_Notset_TCP_SecOff failed");

    }

    // 118073_2 boolean propertyExists(String name)
    // 118073_2_1 Returns true if a message property with the specified name has
    // been set on this JMSProducer

    public void testPropertyExists_B_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable

    {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean notfound = jmsProducer.propertyExists("RandomProperty");

        jmsProducer.setProperty("SetString", "Tester");

        boolean found = jmsProducer.propertyExists("SetString");

        if (!(notfound == false && found == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testPropertyExists_B_SecOff failed");

    }

    public void testPropertyExists_TCP_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable

    {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean notfound = jmsProducer.propertyExists("RandomProperty");

        jmsProducer.setProperty("SetString", "Tester");

        boolean found = jmsProducer.propertyExists("SetString");

        if (!(notfound == false && found == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testPropertyExists_TCP_SecOff failed");

    }

    // 118073_2_2 Test by passing name as empty string

    public void testPropertyExists_emptyString_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable

    {
        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        // jmsProducer.setProperty("", "Tester");

        boolean found = jmsProducer.propertyExists("");

        if (!(found == false))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testPropertyExists_emptyString_B_SecOff failed");

    }

    public void testPropertyExists_emptyString_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable

    {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        // jmsProducer.setProperty("", "Tester");

        boolean found = jmsProducer.propertyExists("");

        if (!(found == false))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testPropertyExists_emptyString_TCP_SecOff failed");

    }

    // 118073_2_3 Test by passing name as null

    public void testPropertyExists_null_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable

    {
        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        // jmsProducer.setProperty(null, "Tester");

        boolean found = jmsProducer.propertyExists(null);

        if (!(found == false))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testPropertyExists_null_B_SecOff failed");

    }

    public void testPropertyExists_null_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable

    {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        // jmsProducer.setProperty(null, "Tester");

        boolean found = jmsProducer.propertyExists(null);

        if (!(found == false))
            exceptionFlag = true;
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testPropertyExists_null_TCP_SecOff failed");

    }

    // 118073_3 JMSProducer setDisableMessageID(boolean value)
    // 118073_3_2 Message IDs are enabled by default.
    // 118073_4_1 Gets an indication of whether message IDs are disabled.

    public void testSetDisableMessageID_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable

    {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage msg = jmsContextQCFBindings.createTextMessage();

        boolean defaultSetMessageID = jmsProducer.getDisableMessageID();

        jmsProducer.setDisableMessageID(true);

        jmsProducer.send(queue, msg);

        String msgID = jmsConsumer.receive(30000).getJMSMessageID();

        if (!(defaultSetMessageID == false && msgID == null))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testSetDisableMessageID_B_SecOff failed");

    }

    public void testSetDisableMessageID_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage msg = jmsContextQCFTCP.createTextMessage();

        boolean defaultSetMessageID = jmsProducer.getDisableMessageID();

        jmsProducer.setDisableMessageID(true);

        jmsProducer.send(queue, msg);

        String msgID = jmsConsumer.receive(30000).getJMSMessageID();

        if (!(defaultSetMessageID == false && msgID == null))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetDisableMessageID_TCP_SecOff failed");

    }

    // 118073_5 JMSProducer setDisableMessageTimestamp(boolean value)
    // 118073_6 boolean getDisableMessageTimestamp()

    public void testSetDisableMessageTimestamp_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage msg = jmsContextQCFBindings.createTextMessage();

        boolean defaultSetMessageTimestamp = jmsProducer
                        .getDisableMessageTimestamp();

        jmsProducer.setDisableMessageTimestamp(true);

        jmsProducer.send(queue, msg);

        long msgTS = jmsConsumer.receive(30000).getJMSTimestamp();

        if (!(defaultSetMessageTimestamp == false && msgTS == 0))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testSetDisableMessageTimestamp_B_SecOff failed");

    }

    public void testSetDisableMessageTimestamp_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage msg = jmsContextQCFTCP.createTextMessage();

        boolean defaultSetMessageTimestamp = jmsProducer
                        .getDisableMessageTimestamp();

        jmsProducer.setDisableMessageTimestamp(true);

        jmsProducer.send(queue, msg);

        long msgTS = jmsConsumer.receive(30000).getJMSTimestamp();

        if (!(defaultSetMessageTimestamp == false && msgTS == 0))
            exceptionFlag = true;

        jmsConsumer.close();

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetDisableMessageTimestamp_TCP_SecOff failed");

    }

    // 118073_7 JMSProducer setDeliveryMode(int deliveryMode)
    // 118073_7_1 Specifies the delivery mode of messages that are sent using
    // this JMSProducer

    public void testSetDeliveryMode_persistent_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean defaultValue = false;
        if (jmsProducer.getDeliveryMode() == DeliveryMode.PERSISTENT)

            defaultValue = true;

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        jmsProducer.send(queue, tmsg);

        jmsContextQCFBindings.close();

    }

    public void testSetDeliveryMode_nonpersistent_B_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean setValue = false;

        jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        if (jmsProducer.getDeliveryMode() == DeliveryMode.NON_PERSISTENT)

            setValue = true;

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        jmsProducer.send(queue, tmsg);
        jmsContextQCFBindings.close();

    }

    public void testBrowseDeliveryMode_persistent_B_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        jmsConsumer.receive(30000);

        qb.close();

        jmsConsumer.close();

        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testBrowseDeliveryMode_persistent_B_SecOff failed");

    }

    public void testBrowseDeliveryMode_nonpersistent_B_SecOff(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages

        if (!(numMsgs == 0))
            exceptionFlag = true;

        qb.close();

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testBrowseDeliveryMode_nonpersistent_B_SecOff failed");

    }

    public void testSetDeliveryMode_nonpersistent_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean setValue = false;

        jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        if (jmsProducer.getDeliveryMode() == DeliveryMode.NON_PERSISTENT)

            setValue = true;

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        jmsProducer.send(queue, tmsg);

        jmsContextQCFTCP.close();
    }

    public void testSetDeliveryMode_persistent_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean defaultValue = false;
        if (jmsProducer.getDeliveryMode() == DeliveryMode.PERSISTENT)

            defaultValue = true;

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        jmsProducer.send(queue, tmsg);

        jmsContextQCFTCP.close();
    }

    public void testBrowseDeliveryMode_persistent_TCP_SecOff(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages
        numMsgs = getMessageCount(qb);

        if (!(numMsgs == 1))
            exceptionFlag = true;

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        jmsConsumer.receive(30000);

        qb.close();
        jmsConsumer.close();

        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testBrowseDeliveryMode_persistent_TCP_SecOff failed");

    }

    public void testBrowseDeliveryMode_nonpersistent_TCP_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);

        int numMsgs = 0;
        // count number of messages

        if (!(numMsgs == 0))
            exceptionFlag = true;

        qb.close();

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testBrowseDeliveryMode_nonpersistent_TCP_SecOff failed");

    }

    // 118073_7_3 Test with deliveryMode as -1
    // 118073_7_4 Test with deliveryMode with the largest number possible for
    // int range
    // 118073_7_5 Test with deliveryMode as 0

    public void testDeliveryMode_Invalid_B_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        int delMode1 = 0;
        int delMode2 = 0;
        int delMode3 = 0;
        int counter = 0;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        try {
            jmsProducer.setDeliveryMode(-1);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();

            delMode1 = jmsProducer.getDeliveryMode();
            counter++;
        }

        try {
            jmsProducer.setDeliveryMode(2147483647);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            counter++;
            delMode2 = jmsProducer.getDeliveryMode();

        }

        try {
            jmsProducer.setDeliveryMode(0);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            counter++;
            delMode3 = jmsProducer.getDeliveryMode();

        }

        if (!(counter == 3 && delMode1 == DeliveryMode.PERSISTENT
              && delMode2 == DeliveryMode.PERSISTENT
              && delMode3 == DeliveryMode.PERSISTENT))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testDeliveryMode_Invalid_B_SecOff failed");

    }

    public void testDeliveryMode_Invalid_TCP_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        int delMode1 = 0;
        int delMode2 = 0;
        int delMode3 = 0;
        int counter = 0;

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        try {
            jmsProducer.setDeliveryMode(-1);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();

            delMode1 = jmsProducer.getDeliveryMode();
            counter++;
        }

        try {
            jmsProducer.setDeliveryMode(2147483647);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            counter++;
            delMode2 = jmsProducer.getDeliveryMode();

        }

        try {
            jmsProducer.setDeliveryMode(0);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            counter++;
            delMode3 = jmsProducer.getDeliveryMode();

        }

        if (!(counter == 3 && delMode1 == DeliveryMode.PERSISTENT
              && delMode2 == DeliveryMode.PERSISTENT
              && delMode3 == DeliveryMode.PERSISTENT))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testDeliveryMode_Invalid_TCP_SecOff failed");

    }

    // 118073_9 JMSProducer setPriority(int priority)
    // 118073_9_1 Specifies the priority of messages that are sent using this
    // JMSProducer
    // 118073_9_2 Priority is set to 4 by default.
    // 118073_10_1 Return the priority of messages that are sent using this
    // JMSProducer

    public void testSetPriority_B_SecOff(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        String output = null;
        for (int i = 0; i < 10; i++) {

            tmsg = jmsContextQCFBindings.createTextMessage();
            jmsProducer.setPriority(i);
            jmsProducer.send(queue, tmsg);
            output = "Total Message No=" + (i) + "  " + "Sent";

            System.out.println(output);
        }
        TextMessage msgR = null;

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;

        // count number of messages
        while (e.hasMoreElements() && numMsgs < 10) {
            msgR = (TextMessage) jmsConsumer.receive(30000);
            if (msgR.getJMSPriority() + numMsgs == 9) {

                System.out
                                .print("Message has been received in correct order. The priority is :"
                                       + msgR.getJMSPriority());;
                flag = true;

            } else {
                flag = false;

            }
            numMsgs++;
        }

        if (!(flag == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetPriority_B_SecOff failed");

    }

    public void testSetPriority_TCP_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        String output = null;
        for (int i = 0; i < 10; i++) {

            tmsg = jmsContextQCFTCP.createTextMessage();
            jmsProducer.setPriority(i);
            jmsProducer.send(queue, tmsg);
            output = "Total Message No=" + (i) + "  " + "Sent";

            System.out.println(output);
        }
        TextMessage msgR = null;

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;

        // count number of messages
        while (e.hasMoreElements() && numMsgs < 10) {
            msgR = (TextMessage) jmsConsumer.receive(30000);
            if (msgR.getJMSPriority() + numMsgs == 9) {

                System.out
                                .print("Message has been received in correct order. The priority is :"
                                       + msgR.getJMSPriority());;
                flag = true;

            } else {
                flag = false;

            }
            numMsgs++;
        }

        if (!(flag == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetPriority_TCP_SecOff failed");

    }

    // 118073_9_2 Priority is set to 4 by default.
    // 118073_9_3 Test setPriority with -1
    // 118073_9_4 test setPriority with boundary values set for int

    public void testSetPriority_default_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        jmsProducer.send(queue, tmsg);
        boolean defPriority = false;
        boolean recvdPriority = false;

        if (jmsProducer.getPriority() == 4)
            defPriority = true;

        if (jmsConsumer.receive(30000).getJMSPriority() == 4)
            recvdPriority = true;

        if (!(defPriority == true && recvdPriority == true))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetPriority_default_B_SecOff failed");

    }

    public void testSetPriority_default_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        jmsProducer.send(queue, tmsg);
        boolean defPriority = false;
        boolean recvdPriority = false;

        if (jmsProducer.getPriority() == 4)
            defPriority = true;

        if (jmsConsumer.receive(30000).getJMSPriority() == 4)
            recvdPriority = true;

        if (!(defPriority == true && recvdPriority == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetPriority_default_TCP_SecOff failed");

    }

    // 118073_9_3 Test setPriority with -1
    // 118073_9_4 test setPriority with boundary values set for int

    public void testSetPriority_variation_B_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        flag = false;
        compFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        try {
            jmsProducer.setPriority(-1);
        } catch (JMSRuntimeException ex) {
            flag = true;
            ex.printStackTrace();
        }

        try {
            jmsProducer.setPriority(2147483647);
        } catch (JMSRuntimeException ex) {
            compFlag = true;
            ex.printStackTrace();
        }

        if (!(flag == true && compFlag == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetPriority_variation_B_SecOff failed");

    }

    public void testSetPriority_variation_TCP_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        compFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        try {
            jmsProducer.setPriority(-1);
        } catch (JMSRuntimeException ex) {
            flag = true;
            ex.printStackTrace();
        }

        try {
            jmsProducer.setPriority(2147483647);
        } catch (JMSRuntimeException ex) {
            compFlag = true;
            ex.printStackTrace();
        }

        if (!(flag == true && compFlag == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetPriority_variation_TCP_SecOff failed");

    }

    // 118073_11 JMSProducer setTimeToLive(long timeToLive)

    // 118073_11_1 Specifies the time to live of messages that are sent using
    // this JMSProducer. This is used to determine the expiration time of a
    // message.
    // 118073_11_2 Clients should not receive messages that have expired;
    // however, JMS does not guarantee that this will not happen.
    // 118073_11_3 Time to live is set to zero by default, which means a message
    // never expires.
    // 118073_12_1 the message time to live in milliseconds; a value of zero
    // means that a message never expires.

    public void testSetTimeToLive_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        long defaultTimeToLive = jmsProducer.getTimeToLive();

        int shortTTL = 500;

        jmsProducer.setTimeToLive(shortTTL);
        jmsProducer.send(queue, tmsg);

        boolean expired = false;
        boolean notExpired = false;

        try {

            Thread.sleep(shortTTL + 10000);
        } catch (InterruptedException e) {
        }

        Message recvd = jmsConsumer.receive(30000);
        if (recvd != null) {

            System.out.println("Message dint expire which is not expected");
            // this is an error, pull out some relevant info

        } else {
            System.out
                            .println("message expired as expected with set timetolive");
            expired = true;
        }

        jmsProducer.setTimeToLive(0);

        jmsProducer.send(queue, tmsg);

        try {

            Thread.sleep(shortTTL + 10000);
        } catch (InterruptedException e) {
        }

        recvd = jmsConsumer.receive(30000);
        if (recvd != null) {

            System.out.println("Message dint expire as expected");
            notExpired = true;
        } else {
            System.out
                            .println("message expired but not expected with time to live 0");

        }

        if (!(defaultTimeToLive == 0 && expired == true && notExpired == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetTimeToLive_B_SecOff failed");

    }

    public void testSetTimeToLive_TCP_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        long defaultTimeToLive = jmsProducer.getTimeToLive();

        int shortTTL = 500;

        jmsProducer.setTimeToLive(shortTTL);
        jmsProducer.send(queue, tmsg);

        boolean expired = false;
        boolean notExpired = false;

        try {

            Thread.sleep(shortTTL + 10000);
        } catch (InterruptedException e) {
        }

        Message recvd = jmsConsumer.receive(30000);
        if (recvd != null) {

            System.out.println("Message dint expire which is not expected");
            // this is an error, pull out some relevant info

        } else {
            System.out
                            .println("message expired as expected with set timetolive");
            expired = true;
        }

        jmsProducer.setTimeToLive(0);

        jmsProducer.send(queue, tmsg);

        try {

            Thread.sleep(shortTTL + 10000);
        } catch (InterruptedException e) {
        }

        recvd = jmsConsumer.receive(30000);
        if (recvd != null) {

            System.out.println("Message dint expire as expected");
            notExpired = true;
        } else {
            System.out
                            .println("message expired but not expected with time to live 0");

        }

        if (!(defaultTimeToLive == 0 && expired == true && notExpired == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetTimeToLive_TCP_SecOff failed");

    }

    // 118073_11_4 Test with timeToLive as -1
    // 118073_11_6 Test with timeToLive set to boundary values for long

    public void testSetTimeToLive_Variation_B_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        compFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
        try {
            jmsProducer.setTimeToLive(-1);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            flag = true;
        }

        try {

            jmsProducer.setTimeToLive(9223372036854775807L);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            compFlag = true;
        }

        if (!(flag == true && compFlag == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetTimeToLive_Variation_B_SecOff failed");

    }

    public void testSetTimeToLive_Variation_TCP_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        compFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        try {
            jmsProducer.setTimeToLive(-1);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            flag = true;
        }

        try {

            jmsProducer.setTimeToLive(9223372036854775807L);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            compFlag = true;
        }

        if (!(flag == true && compFlag == true))
            exceptionFlag = true;

        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetTimeToLive_Variation_TCP_SecOff failed");

    }

    // 118073_13_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified boolean value.
    // 118073_13_2 Verify when this method is invoked when , it will replace any
    // property of the same name that is already set on the message being sent.
    // 118073_13_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetBooleanProperty_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        compFlag = false;
        exp = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName1 = "MyProp";
        boolean propValue = false;
        boolean propValueAfter = true;

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        tmsg.setBooleanProperty("MyProp", false);

        jmsProducer.setProperty("MyProp", true);

        jmsProducer.send(queue, tmsg);

        TextMessage msgrecvd = (TextMessage) jmsConsumer.receive(30000);

        if (msgrecvd.getBooleanProperty("MyProp") == true)
            flag = true;

        try {
            jmsProducer.setProperty("", false);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, true);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            compFlag = true;
        }

        if (!(flag == true && exp == true && compFlag == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetBooleanProperty_B_SecOff failed");

    }

    public void testSetBooleanProperty_TCP_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        compFlag = false;
        exp = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName1 = "MyProp";
        boolean propValue = false;
        boolean propValueAfter = true;

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        tmsg.setBooleanProperty("MyProp", false);

        jmsProducer.setProperty("MyProp", true);

        jmsProducer.send(queue, tmsg);

        TextMessage msgrecvd = (TextMessage) jmsConsumer.receive(30000);

        if (msgrecvd.getBooleanProperty("MyProp") == true)
            flag = true;

        try {
            jmsProducer.setProperty("", false);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, true);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            compFlag = true;
        }

        if (!(flag == true && exp == true && compFlag == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetBooleanProperty_TCP_SecOff failed");

    }

    // 118073_14_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testGetBooleanProperty_MFRE_B_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        compFlag = false;
        exp = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "MyProp";
        byte propValue = 21;
        jmsProducer.setProperty(propName, propValue);

        try {
            jmsProducer.getBooleanProperty("MyProp");
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            compFlag = true;
        }

        try {
            jmsProducer.getBooleanProperty("");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.getBooleanProperty(null);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(compFlag == true && flag == true && exp == true))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetBooleanProperty_MFRE_B_SecOff failed");

    }

    public void testGetBooleanProperty_MFRE_TCP_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        compFlag = false;
        exp = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "MyProp";
        byte propValue = 21;
        jmsProducer.setProperty(propName, propValue);

        try {
            jmsProducer.getBooleanProperty("MyProp");
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            compFlag = true;
        }

        try {
            jmsProducer.getBooleanProperty("");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.getBooleanProperty(null);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(compFlag == true && flag == true && exp == true))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testGetBooleanProperty_MFRE_TCP_SecOff failed");

    }

    // 118073_15 JMSProducer setProperty(String name, byte value)
    // 118073_15_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified byte value.
    // 118073_15_2 Test when this invoked it will replace any property of the
    // same name that is already set on the message being sent.

    public void testSetByteProperty_B_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Message msg = jmsContextQCFBindings.createMessage();
        String propName = "MyProp";
        byte propValue = 21;
        byte propValue1 = 100;

        msg.setByteProperty(propName, propValue);
        // byte b = msg.getByteProperty("MyProp");

        jmsProducer.setProperty(propName, propValue1);

        jmsProducer.send(queue, msg);

        if (!(jmsConsumer.receive(30000).getByteProperty(propName) == propValue1))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetByteProperty_B_SecOff failed");

    }

    public void testSetByteProperty_TCP_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Message msg = jmsContextQCFTCP.createMessage();
        String propName = "MyProp";
        byte propValue = 21;
        byte propValue1 = 100;

        msg.setByteProperty(propName, propValue);
        // byte b = msg.getByteProperty("MyProp");

        jmsProducer.setProperty(propName, propValue1);

        jmsProducer.send(queue, msg);

        if (!(jmsConsumer.receive(30000).getByteProperty(propName) == propValue1))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetByteProperty_TCP_SecOff failed");

    }

    // 118073_15_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetByteProperty_variation_B_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        //emptyQueue(QCFBindings, queue);
        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Message msg = jmsContextQCFBindings.createMessage();
        String propName = "";
        byte propValue = 10;
        try {
            jmsProducer.setProperty(propName, propValue);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        try {
            jmsProducer.setProperty(null, propValue);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetByteProperty_variation_B_SecOff failed");

    }

    public void testSetByteProperty_variation_TCP_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        // emptyQueue(QCFTCP, queue);
        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Message msg = jmsContextQCFTCP.createMessage();
        String propName = "";
        byte propValue = 10;
        try {
            jmsProducer.setProperty(propName, propValue);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        try {
            jmsProducer.setProperty(null, propValue);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetByteProperty_variation_TCP_SecOff failed");

    }

    // 118073_16_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.
    // byte getByteProperty(String name)

    public void testGetByteProperty_MFRE_B_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "PropName";

        jmsProducer.setProperty(propName, 376790);
        try {
            jmsProducer.getByteProperty(propName);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetByteProperty_MFRE_B_SecOff failed");

    }

    public void testGetByteProperty_MFRE_TCP_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "PropName";

        jmsProducer.setProperty(propName, 376790);
        try {
            jmsProducer.getByteProperty(propName);
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetByteProperty_MFRE_TCP_SecOff failed");

    }

    // 118073_17 JMSProducer setProperty(String name, short value)
    // 118073_17_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified short value.
    // 118073_17_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetShortProperty_B_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        short val = 21;
        tmsg.setShortProperty("MyProp", val);

        String propName = "MyProp";
        short propValue = 21;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        Short sval = jmsConsumer.receive(30000).getShortProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetShortProperty_B_SecOff failed");

    }

    public void testSetShortProperty_TCP_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        short val = 21;
        tmsg.setShortProperty("MyProp", val);

        String propName = "MyProp";
        short propValue = 21;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        Short sval = jmsConsumer.receive(30000).getShortProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetShortProperty_TCP_SecOff failed");

    }

    // 118073_17_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetShortProperty_Null_B_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
        short sval = 20;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }
        if (!(exp == true && flag == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetShortProperty_Null_B_SecOff failed");

    }

    public void testSetShortProperty_Null_TCP_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        short sval = 20;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }
        if (!(exp == true && flag == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetShortProperty_Null_B_SecOff failed");

    }

    // 118073_17_4 Test with "value" set as 0
    // 118073_17_5 Test with "value" set as -1
    // 118073_17_6 Test with "value" set to boundary values for short

    public void testSetShortProperty_Variation_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "ShortValue";

        short sval1 = -1;
        short sval2 = 0;
        short sval3 = 127;

        jmsProducer.setProperty(propName, sval1);
        short val1 = jmsProducer.getShortProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        short val2 = jmsProducer.getShortProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        short val3 = jmsProducer.getShortProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetShortProperty_Variation_B_SecOff failed");

    }

    public void testSetShortProperty_Variation_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "ShortValue";

        short sval1 = -1;
        short sval2 = 0;
        short sval3 = 127;

        jmsProducer.setProperty(propName, sval1);
        short val1 = jmsProducer.getShortProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        short val2 = jmsProducer.getShortProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        short val3 = jmsProducer.getShortProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetShortProperty_Variation_TCP_SecOff failed");

    }

    // 118073_18_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testGetShortProperty_MFRE_B_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "PropertyBoolean";
        boolean propValue = true;

        jmsProducer.setProperty(propName, propValue);

        try {
            jmsProducer.getShortProperty(propName);
        } catch (MessageFormatRuntimeException ex) {

            exceptionFlag = true;
            ex.printStackTrace();

        }
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetShortProperty_MFRE_B_SecOff failed");

    }

    public void testGetShortProperty_MFRE_TCP_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "PropertyBoolean";
        boolean propValue = true;

        jmsProducer.setProperty(propName, propValue);

        try {
            jmsProducer.getShortProperty(propName);
        } catch (MessageFormatRuntimeException ex) {

            exceptionFlag = true;
            ex.printStackTrace();

        }
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetShortProperty_MFRE_TCP_SecOff failed");

    }

    // 118073_21 JMSProducer setProperty(String name, int value)
    // 118073_21_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified int value.
    // 118073_21_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetIntProperty_B_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        int val = 21;
        tmsg.setIntProperty("MyProp", val);

        String propName = "MyProp";
        int propValue = 21;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        int sval = jmsConsumer.receive(30000).getIntProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetIntProperty_B_SecOff failed");

    }

    public void testSetIntProperty_TCP_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        int val = 21;
        tmsg.setIntProperty("MyProp", val);

        String propName = "MyProp";
        int propValue = 21;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        int sval = jmsConsumer.receive(30000).getIntProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetIntProperty_TCP_SecOff failed");

    }

    // 118073_19_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetIntProperty_Null_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
        int sval = 20;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(exp == true && flag == true))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetIntProperty_Null_B_SecOff failed");

    }

    public void testSetIntProperty_Null_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        int sval = 20;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(exp == true && flag == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetIntProperty_Null_TCP_SecOff failed");

    }

    // 118073_19_4 Test with "value" set as 0
    // 118073_19_5 Test with "value" set as -1
    // 118073_19_6 Test with "value" set to boundary values for short

    public void testSetIntProperty_Variation_B_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "intValue";

        int sval1 = -1;
        int sval2 = 0;
        int sval3 = 2147483647;

        jmsProducer.setProperty(propName, sval1);
        int val1 = jmsProducer.getIntProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        int val2 = jmsProducer.getIntProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        int val3 = jmsProducer.getIntProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetIntProperty_Variation_B_SecOff failed");

    }

    public void testSetIntProperty_Variation_TCP_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "intValue";

        int sval1 = -1;
        int sval2 = 0;
        int sval3 = 2147483647;

        jmsProducer.setProperty(propName, sval1);
        int val1 = jmsProducer.getIntProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        int val2 = jmsProducer.getIntProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        int val3 = jmsProducer.getIntProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetIntProperty_Variation_TCP_SecOff failed");

    }

    // 118073_22_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.
    public void testSetIntProperty_MFRE_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        long val = 1234567890123456L;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // int sval =
            // jmsConsumer.receive(30000).getIntProperty("propName");
            int sval = jmsProducer.getIntProperty("propName");

        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testSetIntProperty_MFRE_B_SecOff failed");

    }

    public void testSetIntProperty_MFRE_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        // JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        long val = 1234567890123456L;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // int sval =
            // jmsConsumer.receive(30000).getIntProperty("propName");
            int sval = jmsProducer.getIntProperty("propName");

        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testSetIntProperty_MFRE_TCP_SecOff failed");

    }

    // 118073_23 JMSProducer setProperty(String name, long value)
    // 118073_23_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified long value.
    // 118073_23_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetLongProperty_B_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        long val = 1234567890123456L;
        tmsg.setLongProperty("MyProp", val);

        String propName = "MyProp";
        long propValue = 1234567890654321L;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        long sval = jmsConsumer.receive(30000).getLongProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetLongProperty_B_SecOff failed");

    }

    public void testSetLongProperty_TCP_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        long val = 1234567890123456L;
        tmsg.setLongProperty("MyProp", val);

        String propName = "MyProp";
        long propValue = 1234567890654321L;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        long sval = jmsConsumer.receive(30000).getLongProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetLongProperty_TCP_SecOff failed");

    }

    // 118073_23_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetLongProperty_Null_B_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
        long val = 1234567890654321L;

        try {

            jmsProducer.setProperty("", val);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, val);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetLongProperty_Null_B_SecOff failed");

    }

    public void testSetLongProperty_Null_TCP_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        long val = 1234567890654321L;

        try {

            jmsProducer.setProperty("", val);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, val);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetLongProperty_Null_TCP_SecOff failed");

    }

    // 118073_23_4 Test when "value" is set as 0
    // 118073_23_5 Test when 'value" is set as -1
    // 118073_23_6 Test when "value" is set to boundary values allowed for long

    public void testSetLongProperty_Variation_B_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "LongValue";

        long sval1 = -1;
        long sval2 = 0;
        long sval3 = 9223372036854775807L;

        jmsProducer.setProperty(propName, sval1);
        long val1 = jmsProducer.getLongProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        long val2 = jmsProducer.getLongProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        long val3 = jmsProducer.getLongProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetLongProperty_Variation_B_SecOff failed");

    }

    // 118073_19_4 Test with "value" set as 0
    // 118073_19_5 Test with "value" set as -1
    // 118073_19_6 Test with "value" set to boundary values for short

    public void testSetLongProperty_Variation_TCP_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "LongValue";

        long sval1 = -1;
        long sval2 = 0;
        long sval3 = 9223372036854775807L;

        jmsProducer.setProperty(propName, sval1);
        long val1 = jmsProducer.getLongProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        long val2 = jmsProducer.getLongProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        long val3 = jmsProducer.getLongProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetLongProperty_Variation_TCP_SecOff failed");

    }

    // 118073_24_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testSetLongProperty_MFRE_B_SecOff(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // long sval =
            // jmsConsumer.receive(30000).getLongProperty("propName");
            long sval = jmsProducer.getLongProperty("propName");

        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContextQCFBindings.close();

        if (!(exceptionFlag))
            throw new WrongException("testSetLongProperty_MFRE_B_SecOff failed");

    }

    public void testSetLongProperty_MFRE_TCP_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // long sval =
            // jmsConsumer.receive(30000).getLongProperty("propName");
            long sval = jmsProducer.getLongProperty("propName");

        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testSetLongProperty_MFRE_TCP_SecOff failed");

    }

    // 118073_25 JMSProducer setProperty(String name, float value)
    // 118073_25_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified float value.
    // 118073_25_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.
    public void testSetFloatProperty_B_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        float val = 123.4f;
        tmsg.setFloatProperty("MyProp", val);

        String propName = "MyProp";
        float propValue = 213.4f;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        float sval = jmsConsumer.receive(30000).getFloatProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;

        jmsConsumer.close();

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetFloatProperty_B_SecOff failed");

    }

    public void testSetFloatProperty_TCP_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        float val = 123.4f;
        tmsg.setFloatProperty("MyProp", val);

        String propName = "MyProp";
        float propValue = 213.4f;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        float sval = jmsConsumer.receive(30000).getFloatProperty("MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetFloatProperty_TCP_SecOff failed");

    }

    // 118073_25_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetFloatProperty_Null_B_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
        float sval = 123.4f;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetFloatProperty_Null_B_SecOff failed");

    }

    public void testSetFloatProperty_Null_TCP_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        float sval = 123.4f;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetFloatProperty_Null_TCP_SecOff failed");

    }

    // 118073_25_4 Test with "value" set to 0
    // 118073_25_5 Test with "value" set to -1
    // 118073_25_6 Test with "value" set to boundary values for float.

    public void testSetFloatProperty_Variation_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "floatValue";

        float sval1 = -1;
        float sval2 = 0;
        float sval3 = 214748364788888888888889999999999000000.1234567889999999999999999999F;

        jmsProducer.setProperty(propName, sval1);
        float val1 = jmsProducer.getFloatProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        float val2 = jmsProducer.getFloatProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        float val3 = jmsProducer.getFloatProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetFloatProperty_Variation_B_SecOff failed");

    }

    public void testSetFloatProperty_Variation_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "floatValue";

        float sval1 = -1;
        float sval2 = 0;
        float sval3 = 214748364788888888888889999999999000000.1234567889999999999999999999F;

        jmsProducer.setProperty(propName, sval1);
        float val1 = jmsProducer.getFloatProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        float val2 = jmsProducer.getFloatProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        float val3 = jmsProducer.getFloatProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetFloatProperty_Variation_TCP_SecOff failed");

    }

    // 118073_26_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testSetFloatProperty_MFRE_B_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // float sval =
            // jmsConsumer.receive(30000).getFloatProperty("propName");
            float sval = jmsProducer.getFloatProperty("propName");
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testSetFloatProperty_MFRE_B_SecOff failed");

    }

    public void testSetFloatProperty_MFRE_TCP_SecOff(HttpServletRequest request,
                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // float sval =
            // jmsConsumer.receive(30000).getFloatProperty("propName");
            float sval = jmsProducer.getFloatProperty("propName");
        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testSetFloatProperty_MFRE_TCP_SecOff failed");

    }

    // 118073_27 JMSProducer setProperty(String name, double value)
    // 118073_27_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified double value.
    // 118073_27_2 Test when this method is invoked will replace any property of
    // the same name that is already set on the message being sent.

    public void testSetDoubleProperty_B_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        double val = 1.111e2;
        tmsg.setDoubleProperty("MyProp", val);

        String propName = "MyProp";
        double propValue = 1.234e2;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        double sval = jmsConsumer.receive(30000).getDoubleProperty(
                                                                   "MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetDoubleProperty_B_SecOff failed");

    }

    public void testSetDoubleProperty_TCP_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        double val = 1.111e2;
        tmsg.setDoubleProperty("MyProp", val);

        String propName = "MyProp";
        double propValue = 1.234e2;

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        double sval = jmsConsumer.receive(30000).getDoubleProperty(
                                                                   "MyProp");

        if (!(sval == propValue))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetDoubleProperty_TCP_SecOff failed");

    }

    // 118073_27_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetDoubleProperty_Null_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        // JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
        double sval = 1.234e2;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetDoubleProperty_Null_B_SecOff failed");

    }

    public void testSetDoubleProperty_Null_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        double sval = 1.234e2;

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetDoubleProperty_Null_TCP_SecOff failed");

    }

    // 118073_27_4 Test with value set to 0
    // 118073_27_5 Test with value set to -1
    // 118073_27_6 Test with value set to boundary values allowed for double

    public void testSetDoubleProperty_Variation_B_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "doubleValue";

        double sval1 = -1;
        double sval2 = 0;
        double sval3 = 2147483647888888888888899999999990000008888888888888888.1234567889999999e100;

        jmsProducer.setProperty(propName, sval1);
        double val1 = jmsProducer.getDoubleProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        double val2 = jmsProducer.getDoubleProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        double val3 = jmsProducer.getDoubleProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetDoubleProperty_Variation_B_SecOff failed");

    }

    public void testSetDoubleProperty_Variation_TCP_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "doubleValue";

        double sval1 = -1;
        double sval2 = 0;
        double sval3 = 2147483647888888888888899999999990000008888888888888888.1234567889999999e100;

        jmsProducer.setProperty(propName, sval1);
        double val1 = jmsProducer.getDoubleProperty(propName);

        jmsProducer.setProperty(propName, sval2);
        double val2 = jmsProducer.getDoubleProperty(propName);
        jmsProducer.setProperty(propName, sval3);
        double val3 = jmsProducer.getDoubleProperty(propName);

        if (!(val1 == sval1 && val2 == sval2 && val3 == sval3))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetDoubleProperty_Variation_TCP_SecOff failed");

    }

    // 118073_28_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testSetDoubleProperty_MFRE_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // double sval =
            // jmsConsumer.receive(30000).getDoubleProperty("propName");
            double sval = jmsProducer.getDoubleProperty("propName");

        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFBindings.close();
        if (!(exceptionFlag))
            throw new WrongException("testSetDoubleProperty_MFRE_B_SecOff failed");

    }

    public void testSetDoubleProperty_MFRE_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        // JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        // jmsProducer.send(queue, tmsg);

        try {

            // double sval =
            // jmsConsumer.receive(30000).getDoubleProperty("propName");
            double sval = jmsProducer.getDoubleProperty("propName");

        } catch (MessageFormatRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testSetDoubleProperty_MFRE_TCP_SecOff failed");

    }

    // 118073_29 JMSProducer setProperty(String name, String value)
    // 118073_29_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified String value.
    // 118073_29_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetStringProperty_B_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        String val = "EmployeeID";
        tmsg.setStringProperty("MyProp", val);

        String propName = "MyProp";
        String propValue = "EmployeeName";

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        String sval = jmsConsumer.receive(30000).getStringProperty(
                                                                   "MyProp");

        if (!(sval.equals(propValue)))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetStringProperty_B_SecOff failed");

    }

    public void testSetStringProperty_TCP_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        String val = "EmployeeID";
        tmsg.setStringProperty("MyProp", val);

        String propName = "MyProp";
        String propValue = "EmployeeName";

        jmsProducer.setProperty(propName, propValue);

        jmsProducer.send(queue, tmsg);

        String sval = jmsConsumer.receive(30000).getStringProperty(
                                                                   "MyProp");

        if (!(sval.equals(propValue)))
            exceptionFlag = true;
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testSetStringProperty_TCP_SecOff failed");

    }

    // 118073_29_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetStringProperty_Null_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        exp = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
        String sval = "EmployeeName";

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }
        if (!(flag == true && exp == true))

            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetStringProperty_Null_B_SecOff failed");

    }

    public void testSetStringProperty_Null_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        exp = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        String sval = "EmployeeName";

        try {

            jmsProducer.setProperty("", sval);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, sval);

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }
        if (!(flag == true && exp == true))

            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetStringProperty_Null_TCP_SecOff failed");

    }

    // No MessageFormatException should be thrown when property set as Boolean
    // is read as a String.

    public void testGetStringProperty_B_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        String sval = jmsProducer.getStringProperty("propName");

        if (!(sval.equals("true")))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetStringProperty_B_SecOff failed");

    }

    public void testGetStringProperty_TCP_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        boolean val = true;

        jmsProducer.setProperty("propName", val);

        String sval = jmsProducer.getStringProperty("propName");

        if (!(sval.equals("true")))
            exceptionFlag = true;
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testGetStringProperty_TCP_SecOff failed");

    }

    // 118073_31 JMSProducer setProperty(String name,Object value)
    // 118073_32_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified Java object value.
    // 118073_32_2 Verify that this method works only for the objectified
    // primitive object types (Integer, Double, Long ...) and String objects.
    // 118073_32_3 Test this will replace any property of the same name that is
    // already set on the message being sent.

    public void testSetObjectProperty_B_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        emptyQueue(QCFBindings, queue);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        tmsg.setObjectProperty("MyProp", new Integer(1444));

        String propName = "MyProp";

        jmsProducer.setProperty(propName, new Integer(2000));

        jmsProducer.send(queue, tmsg);

        Object sval = jmsConsumer.receive(30000).getObjectProperty(
                                                                   "MyProp");

        if (!(sval.equals(2000)))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_B_SecOff failed");

    }

    public void testSetObjectProperty_TCP_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        emptyQueue(QCFTCP, queue);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        tmsg.setObjectProperty("MyProp", new Integer(1444));

        String propName = "MyProp";

        jmsProducer.setProperty(propName, new Integer(2000));

        jmsProducer.send(queue, tmsg);

        Object sval = jmsConsumer.receive(30000).getObjectProperty(
                                                                   "MyProp");

        if (!(sval.equals(2000)))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_TCP_SecOff failed");

    }

    // 118073_32_4 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetObjectProperty_Null_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        try {

            jmsProducer.setProperty("", new Integer(1000));
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, new Integer(1000));

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_Null_B_SecOff failed");

    }

    public void testSetObjectProperty_Null_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        exp = false;
        flag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        try {

            jmsProducer.setProperty("", new Integer(1000));
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            exp = true;
        }

        try {
            jmsProducer.setProperty(null, new Integer(1000));

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            flag = true;
        }

        if (!(flag == true && exp == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_Null_TCP_SecOff failed");

    }

    // 118073_32_5 

    public void testSetObjectProperty_NullObject_B_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Object obj = null;

        try {

            jmsProducer.setProperty("Tester", obj);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_NullObject_B_SecOff failed");

    }

    public void testSetObjectProperty_NullObject_TCP_SecOff(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Object obj = null;

        try {

            jmsProducer.setProperty("Tester", obj);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_NullObject_TCP_SecOff failed");

    }

    // 118073_33_2 if there is no property by this name, a null value is
    // returned

    public void testSetObjectProperty_NullValue_B_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Object obj = jmsProducer.getObjectProperty("Name");

        if (!(obj == null))
            exceptionFlag = true;

        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_NullValue_B_SecOff failed");

    }

    public void testSetObjectProperty_NullValue_TCP_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        // JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Object obj = jmsProducer.getObjectProperty("Name");

        if (!(obj == null))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetObjectProperty_NullValue_TCP_SecOff failed");

    }

    // 118073_31 Set<String> getPropertyNames()
    // 118073_31_1 Returns an unmodifiable Set view of the names of all the
    // message properties that have been set on this JMSProducer.
    // 118073_31_2 JMS standard header fields are not considered properties and
    // are not returned in this Set.

    public void testGetPropertyNames_B_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        flag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        //JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        int intVal = 1000;

        jmsProducer.setProperty("Role", "Tester");

        jmsProducer.setProperty("Bill", intVal);

        Set<String> en = jmsProducer.getPropertyNames();

        int counter = 0;

        while (en.iterator().hasNext() && counter < 1) {

            if (en.contains("Role") && en.contains("Bill"))
                flag = true;

            counter++;
        }

        if (!(flag == true))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testGetPropertyNames_B_SecOff failed");

    }

    public void testGetPropertyNames_TCP_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        compFlag = false;
        flag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //emptyQueue(QCFTCP, queue);
        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        int intVal = 1000;

        jmsProducer.setProperty("Role", "Tester");

        jmsProducer.setProperty("Bill", intVal);

        jmsProducer.setJMSCorrelationID("correlID");

        Set<String> en = jmsProducer.getPropertyNames();

        int counter = 0;

        while (en.iterator().hasNext() && counter < 1) {

            if (en.contains("Role") && en.contains("Bill"))
                flag = true;

            counter++;
        }

        if (!(flag == true))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testGetPropertyNames_TCP_SecOff failed");

    }

    // 118073_31_3 java.lang.UnsupportedOperationException results when attempts
    // are made to modify the returned collection

    public void testGetPropertyNames_Exception_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        // emptyQueue(QCFBindings, queue);
        // JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        int intVal = 1000;

        jmsProducer.setProperty("Role", "Tester");

        jmsProducer.setProperty("Bill", intVal);

        Set<String> en = jmsProducer.getPropertyNames();

        int counter = 0;

        while (en.iterator().hasNext() && counter < 1) {
            try {
                en.remove("Bill");
            } catch (UnsupportedOperationException ex) {
                ex.printStackTrace();
                exceptionFlag = true;
            }
            counter++;
        }
        jmsContextQCFBindings.close();

        if (!(exceptionFlag))
            throw new WrongException("testGetPropertyNames_Exception_B_SecOff failed");

    }

    public void testGetPropertyNames_Exception_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        //emptyQueue(QCFTCP, queue);
        //JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue);

        //jmsConsumer.receive(30000);

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        int intVal = 1000;

        jmsProducer.setProperty("Role", "Tester");

        jmsProducer.setProperty("Bill", intVal);

        Set<String> en = jmsProducer.getPropertyNames();

        int counter = 0;

        while (en.iterator().hasNext() && counter < 1) {
            try {
                en.remove("Bill");
            } catch (UnsupportedOperationException ex) {
                ex.printStackTrace();
                exceptionFlag = true;
            }
            counter++;
        }
        jmsContextQCFTCP.close();
        if (!(exceptionFlag))
            throw new WrongException("testGetPropertyNames_Exception_TCP_SecOff failed");

    }

    // 118073_32 JMSProducer setJMSCorrelationIDAsBytes(byte[] correlationID)
    // 118073_32_1 Specifies that messages sent using this JMSProducer will have
    // their JMSCorrelationID header value set to the specified correlation ID,
    // where correlation ID is specified as an array of bytes.
    // 118073_33_1 Returns the JMSCorrelationID header value that has been set
    // on this JMSProducer, as an array of bytes.

    public void testSetJMSCorrelationIDAsBytes_B_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        // set and retrieve a byte[]
        byte[] testA = { 1, 2, 3, 4 };
        jmsProducer.setJMSCorrelationIDAsBytes(testA);

        byte[] resultA = jmsProducer.getJMSCorrelationIDAsBytes();

        int wrongCount = 0;
        int rightCount = 0;

        for (int i = 0; i < testA.length; i++) {
            if (testA[i] != resultA[i]) {
                wrongCount++;
            } else
                rightCount++;
        }

        if (!(rightCount == 4))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetJMSCorrelationIDAsBytes_B_SecOff failed");

    }

    public void testSetJMSCorrelationIDAsBytes_TCP_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        // set and retrieve a byte[]
        byte[] testA = { 1, 2, 3, 4 };
        jmsProducer.setJMSCorrelationIDAsBytes(testA);

        byte[] resultA = jmsProducer.getJMSCorrelationIDAsBytes();

        int wrongCount = 0;
        int rightCount = 0;

        for (int i = 0; i < testA.length; i++) {
            if (testA[i] != resultA[i]) {
                wrongCount++;
            } else
                rightCount++;
        }

        if (!(rightCount == 4))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetJMSCorrelationIDAsBytes_B_SecOff failed");

    }

    // 118073_34 JMSProducer setJMSCorrelationID(String correlationID)
    // 118073_34_1 Specifies that messages sent using this JMSProducer will have
    // their JMSCorrelationID header value set to the specified correlation ID,
    // where correlation ID is specified as a String.

    // 118073_35 String getJMSCorrelationID()
    // 118073_35_1 Returns the JMSCorrelationID header value that has been set
    // on this JMSProducer, as a String.

    public void testSetJMSCorrelationID_B_SecOff(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {

        String correl = "MyCorrelID";
        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        jmsProducer.setJMSCorrelationID(correl);

        String setCID = jmsProducer.getJMSCorrelationID();

        if (!(correl.equals(setCID)))
            exceptionFlag = true;
        jmsContextQCFBindings.close();

        if (exceptionFlag)
            throw new WrongException("testSetJMSCorrelationID_B_SecOff failed");

    }

    public void testSetJMSCorrelationID_TCP_SecOff(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        String correl = "MyCorrelID";
        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        jmsProducer.setJMSCorrelationID(correl);

        String setCID = jmsProducer.getJMSCorrelationID();

        if (!(correl.equals(setCID)))
            exceptionFlag = true;
        jmsContextQCFTCP.close();

        if (exceptionFlag)
            throw new WrongException("testSetJMSCorrelationID_TCP_SecOff failed");

    }

    // 118073_34_2 Test what JMSCorrelationID can hold as its value

    public void testSetJMSCorrelationID_Value_B_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        String CorrelID = "ID:ffff";

        exceptionFlag = false;

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Message msg = jmsContextQCFBindings.createMessage();
        msg.setJMSCorrelationID("ID:aaaa");

        jmsProducer.setJMSCorrelationID(CorrelID);

        String setCID = jmsProducer.getJMSCorrelationID();

        if (!(CorrelID.equals(setCID)))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetJMSCorrelationID_Value_B_SecOff failed");

    }

    public void testSetJMSCorrelationID_Value_TCP_SecOff(HttpServletRequest request,
                                                         HttpServletResponse response) throws Throwable {

        String CorrelID = "ID:ffff";

        exceptionFlag = false;

        JMSContext jmsContextQCFTCP = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Message msg = jmsContextQCFTCP.createMessage();
        msg.setJMSCorrelationID("ID:aaaa");

        jmsProducer.setJMSCorrelationID(CorrelID);

        String setCID = jmsProducer.getJMSCorrelationID();

        if (!(CorrelID.equals(setCID)))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetJMSCorrelationID_Value_TCP_SecOff failed");

    }

    // 118073_36 JMSProducer setJMSType(String type)
    // 118073_36_1 Returns the JMSType header value that has been set on this
    // JMSProducer.
    // 118073_37 String getJMSType()
    // 118073_37_1 Returns the JMSType header value that has been set on this
    // JMSProducer.

    public void testSetJMSType_B_SecOff(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable

    {

        String type1 = "type 1";
        String type2 = "type 2";
        exceptionFlag = false;
        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        jmsProducer.setJMSType(type1);

        String t1 = jmsProducer.getJMSType();

        jmsProducer.setJMSType(type2);

        String t2 = jmsProducer.getJMSType();

        if (!(t1.equals(type1) && t2.equals(type2)))
            exceptionFlag = true;
        jmsContextQCFBindings.close();
        if (exceptionFlag)
            throw new WrongException("testSetJMSType_B_SecOff failed");

    }

    public void testSetJMSType_TCP_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable

    {

        String type1 = "type 1";
        String type2 = "type 2";
        exceptionFlag = false;
        JMSContext jmsContextQCFTCP = QCFTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        jmsProducer.setJMSType(type1);

        String t1 = jmsProducer.getJMSType();

        jmsProducer.setJMSType(type2);

        String t2 = jmsProducer.getJMSType();

        if (!(t1.equals(type1) && t2.equals(type2)))
            exceptionFlag = true;
        jmsContextQCFTCP.close();
        if (exceptionFlag)
            throw new WrongException("testSetJMSType_TCP_SecOff failed");

    }

    //Defect 175517
    public void testQueueSender_InvalidDestinationNE_B_SecOff(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable
    {

        exceptionFlag = false;
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        boolean flag4 = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");

        QueueConnection con = cf1.createQueueConnection();
        con.start();
        QueueSession qsession = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        QueueSender qsender = qsession.createSender(null);

        TextMessage tmsg = qsession.createTextMessage("Hello");
        try {
            qsender.send(tmsg);

        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag1 = true;
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();

        }

        try {
            qsender.send(null, tmsg);

        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag2 = true;
        }

        try {
            qsender.send(tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);

        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag3 = true;
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
        }

        try {
            qsender.send(null, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 1, 50000);

        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag4 = true;
        }

        if (flag1 == true && flag2 == true && flag3 == true && flag4 == true)
            exceptionFlag = true;

        con.close();
        if (!(exceptionFlag))
            throw new WrongException("testJMSProducerSendTextMessage_InvalidDestinationNE_B_SecOff failed");

    }

    //MessageProducer.send combinations test - createProducer(null) 

    public void testMessageProducerWithNullDestination(HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        boolean flag4 = false;

        ConnectionFactory cf1 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");

        Connection con = cf1.createConnection();
        con.start();
        Session session = con.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        MessageProducer producer = session.createProducer(null);

        TextMessage tmsg = session.createTextMessage("Hello");

        //send(Destination destination, Message message)

        ////destination set to null 
        try {
            producer.send(null, tmsg);
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag1 = true;
        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        ////destination set to invalid queue
        try {
            flag1 = false;
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg);
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag1 = true;
        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        ////destination set to valid queue
        try {
            flag1 = false;
            producer.send(queue, tmsg);
            flag1 = true;
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();

        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        //send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive)

        ////destination set to null 
        try {
            producer.send(null, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag2 = true;
        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        ////destination set to Invalid queue 
        try {
            flag2 = false;
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag2 = true;
        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        ////destination set to valid queue 
        try {
            flag2 = false;
            producer.send(queue, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            flag2 = true;
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();

        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        //send(Message message)

        try {
            producer.send(tmsg);
        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag3 = true;
        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        //send(Message message, int deliveryMode, int priority, long timeToLive)

        try {
            producer.send(tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag4 = true;
        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        if (flag1 == true && flag2 == true && flag3 == true && flag4 == true)
            exceptionFlag = true;

        con.close();
        if (!(exceptionFlag))
            throw new WrongException("testMessageProducerWithNullDestination failed");
    }

    //MessageProducer.send combinations test - createProducer(valid_queue) 

    public void testMessageProducerWithValidDestination(HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        boolean flag4 = false;

        ConnectionFactory cf1 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");

        Connection con = cf1.createConnection();
        con.start();
        Session session = con.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        MessageProducer producer = session.createProducer(queue1);

        TextMessage tmsg = session.createTextMessage("Hello");

        //send(Destination destination, Message message)

        ////destination set to null 
        try {
            producer.send(null, tmsg);
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag1 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        ////destination set to invalid queue
        try {
            flag1 = false;
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg);
        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag1 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        ////destination set to valid queue
        try {
            flag1 = false;
            producer.send(queue, tmsg);

        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag1 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        //send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive)

        ////destination set to null 
        try {
            producer.send(null, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            flag2 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        ////destination set to Invalid queue 
        try {
            flag2 = false;
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag2 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        ////destination set to valid queue 
        try {
            flag2 = false;
            producer.send(queue, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);

        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
            flag2 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        //send(Message message)

        try {

            producer.send(tmsg);
            flag3 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        //send(Message message, int deliveryMode, int priority, long timeToLive)

        try {
            producer.send(tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            flag4 = true;

        } catch (JMSException ex) {
            ex.printStackTrace();

        }

        if (flag1 == true && flag2 == true && flag3 == true && flag4 == true)
            exceptionFlag = true;

        con.close();
        if (!(exceptionFlag))
            throw new WrongException("testMessageProducerWithNullDestination failed");
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

    public void testSendWithNullBody(HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextQCFBindings = QCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        byte[] byteBody = null;
        Map<String, Object> Mapbody = null;
        Message message = null;
        String body = null;

        try {
            jmsProducer.send(queue, byteBody);
            System.out.println("Sent empty byte body");
        } catch (MessageFormatRuntimeException e) {
            e.printStackTrace();
        }
        try {
            jmsProducer.send(queue, Mapbody);
            System.out.println("Sent empty Map body");
        } catch (MessageFormatRuntimeException e) {
            e.printStackTrace();
        }
        try {
            jmsProducer.send(queue, message);
            System.out.println("Sent empty message body");
        } catch (MessageFormatRuntimeException e) {
            e.printStackTrace();
        }
        try {
            jmsProducer.send(queue, body);
            System.out.println("Sent empty string body");
        } catch (MessageFormatRuntimeException e) {
            e.printStackTrace();
        }
    }

}
