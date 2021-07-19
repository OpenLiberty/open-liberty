package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import ejb.SampleSecureStatelessBean;

@SuppressWarnings("serial")
public class JMSContextInjectServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;

    public static boolean exceptionFlag;

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF")
    private JMSContext jmsContextQueue;

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF1")
    private JMSContext jmsContextQueueTCP;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/qcf")
    private JMSContext jmsContextQueueNew;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/qcf1")
    private JMSContext jmsContextQueueNewTCP;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf")
    private JMSContext jmsContextTopic;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf1")
    private JMSContext jmsContextTopicTCP;

    @EJB
    SampleSecureStatelessBean statelessBean;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQCFBindings = getQCFBindings();
            jmsQCFTCP = getQCFTCP();
            jmsQueue = getQueue();

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
        final TraceComponent tc = Tr.register(JMSContextInjectServlet.class); // injection
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

    public void testP2P_B_SecOff(HttpServletRequest request,
                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextQueue
                        .createTextMessage("testP2P_B_SecOff");
        jmsContextQueue.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testP2P_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testP2P_B_SecOff failed: Expected message was not received");

    }

    public void testP2P_TCP_SecOff(HttpServletRequest request,
                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextQueueTCP
                        .createTextMessage("testP2P_TCP_SecOff");
        jmsContextQueueTCP.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testP2P_TCP_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testP2P_TCP_SecOff failed: Expected message was not received");

    }

    public void testPubSub_B_SecOff(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopic
                        .createTextMessage("testPubSub_B_SecOff");
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);
        jmsContextTopic.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSub_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSub_B_SecOff failed: Expected message was not received");

    }

    public void testPubSub_TCP_SecOff(HttpServletRequest request,
                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopicTCP
                        .createTextMessage("testPubSub_TCP_SecOff");
        Topic jmsTopic = jmsContextTopicTCP.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopicTCP.createConsumer(jmsTopic);
        jmsContextTopicTCP.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSub_TCP_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSub_TCP_SecOff failed: Expected message was not received");

    }

    public void testPubSubDurable_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopic
                        .createTextMessage("testPubSubDurable_B_SecOff");
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createDurableConsumer(jmsTopic, "sub1");
        jmsContextTopic.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSubDurable_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSubDurable_B_SecOff failed: Expected message was not received");

    }

    public void testPubSubDurable_TCP_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopicTCP
                        .createTextMessage("testPubSubDurable_TCP_SecOff");
        Topic jmsTopic = jmsContextTopicTCP.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopicTCP.createDurableConsumer(jmsTopic, "sub2");
        jmsContextTopicTCP.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSubDurable_TCP_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSubDurable_TCP_SecOff failed: Expected message was not received");

    }

    public void testNegativeSetters_B_SecOff(HttpServletRequest request,
                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        boolean ex1 = false;
        boolean ex2 = false;
        boolean ex3 = false;
        boolean ex4 = false;
        boolean ex5 = false;
        boolean ex6 = false;
        boolean ex7 = false;
        boolean ex8 = false;
        boolean ex9 = false;
        boolean ex10 = false;

        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic, "sub1");

        try {
            jmsContextTopic.setClientID("cid");
        } catch (IllegalStateRuntimeException e) {
            ex1 = true;
            System.out.println("Expected exception seen on calling setClientID method");
        }

        try {
            jmsContextTopic.setExceptionListener(null);
        } catch (IllegalStateRuntimeException e) {
            ex2 = true;
            System.out.println("Expected exception seen on calling setExceptionListener method");
        }

        try {
            jmsContextTopic.stop();
        } catch (IllegalStateRuntimeException e) {
            ex3 = true;
            System.out.println("Expected exception seen on calling stop method");
        }

        try {
            jmsContextTopic.acknowledge();
        } catch (IllegalStateRuntimeException e) {
            ex4 = true;
            System.out.println("Expected exception seen on calling acknowledge method");
        }

        try {
            jmsContextTopic.commit();
        } catch (IllegalStateRuntimeException e) {
            ex5 = true;
            System.out.println("Expected exception seen on calling commit method");
        }

        try {
            jmsContextTopic.rollback();
        } catch (IllegalStateRuntimeException e) {
            ex6 = true;
            System.out.println("Expected exception seen on calling rollback method");
        }

        try {
            jmsContextTopic.recover();
        } catch (IllegalStateRuntimeException e) {
            ex7 = true;
            System.out.println("Expected exception seen on calling recover method");
        }

        try {
            jmsContextTopic.setAutoStart(true);
        } catch (IllegalStateRuntimeException e) {
            ex8 = true;
            System.out.println("Expected exception seen on calling setAutoStart method");
        }

        try {
            jmsContextTopic.start();
        } catch (IllegalStateRuntimeException e) {
            ex9 = true;
            System.out.println("Expected exception seen on calling start method");
        }

        try {
            jmsContextTopic.close();
        } catch (IllegalStateRuntimeException e) {
            ex10 = true;
            System.out.println("Expected exception seen on calling close method");
        }

        if (!(ex1 && ex2 && ex3 && ex4 && ex5 && ex6 && ex7 && ex8 && ex9 && ex10))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testNegativeSetters_B_SecOff failed: Expected exception was not seen");

    }

    public void testNegativeSetters_TCP_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        boolean ex1 = false;
        boolean ex2 = false;
        boolean ex3 = false;
        boolean ex4 = false;
        boolean ex5 = false;
        boolean ex6 = false;
        boolean ex7 = false;
        boolean ex8 = false;
        boolean ex9 = false;
        boolean ex10 = false;

        try {
            jmsContextTopicTCP.setClientID("cid");
        } catch (IllegalStateRuntimeException e) {
            ex1 = true;
            System.out.println("Expected exception seen on calling setClientID method");
        }

        try {
            jmsContextTopicTCP.setExceptionListener(null);
        } catch (IllegalStateRuntimeException e) {
            ex2 = true;
            System.out.println("Expected exception seen on calling setExceptionListener method");
        }

        try {
            jmsContextTopicTCP.stop();
        } catch (IllegalStateRuntimeException e) {
            ex3 = true;
            System.out.println("Expected exception seen on calling stop method");
        }

        try {
            jmsContextTopicTCP.acknowledge();
        } catch (IllegalStateRuntimeException e) {
            ex4 = true;
            System.out.println("Expected exception seen on calling acknowledge method");
        }

        try {
            jmsContextTopicTCP.commit();
        } catch (IllegalStateRuntimeException e) {
            ex5 = true;
            System.out.println("Expected exception seen on calling commit method");
        }

        try {
            jmsContextTopicTCP.rollback();
        } catch (IllegalStateRuntimeException e) {
            ex6 = true;
            System.out.println("Expected exception seen on calling rollback method");
        }

        try {
            jmsContextTopicTCP.recover();
        } catch (IllegalStateRuntimeException e) {
            ex7 = true;
            System.out.println("Expected exception seen on calling recover method");
        }

        try {
            jmsContextTopicTCP.setAutoStart(true);
        } catch (IllegalStateRuntimeException e) {
            ex8 = true;
            System.out.println("Expected exception seen on calling setAutoStart method");
        }

        try {
            jmsContextTopicTCP.start();
        } catch (IllegalStateRuntimeException e) {
            ex9 = true;
            System.out.println("Expected exception seen on calling start method");
        }

        try {
            jmsContextTopicTCP.close();
        } catch (IllegalStateRuntimeException e) {
            ex10 = true;
            System.out.println("Expected exception seen on calling close method");
        }

        if (!(ex1 && ex2 && ex3 && ex4 && ex5 && ex6 && ex7 && ex8 && ex9 && ex10))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testNegativeSetters_TCP_SecOff failed: Expected exception was not seen");

    }

    public void testMessageOrder_B_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        int msgOrder[] = new int[10];
        int redFlag = 0;

        JMSProducer producer1 = jmsContextQueue.createProducer();
        JMSProducer producer2 = jmsContextQueueNew.createProducer();
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        Message msg = null;
        int msgRcvd = 0;
        for (int i = 0; i < 10; i++)
        {

            if (i % 2 == 0) {
                msg = jmsContextQueue.createMessage();
                msg.setIntProperty("Message_Order", i);
                producer1.send(jmsQueue, msg);
            }
            else {
                msg = jmsContextQueueNew.createMessage();
                msg.setIntProperty("Message_Order", i);
                producer2.send(jmsQueue, msg);
            }

            msgRcvd = jmsConsumer.receive(1000).getIntProperty("Message_Order");
            System.out.println("Received message number : " + msgRcvd);
            msgOrder[i] = msgRcvd;
        }

        for (int i = 0; i < 10; i++) {
            System.out.println("Retrieving Message Order:" + msgOrder[i]);
            if (msgOrder[i] == i)
                System.out.println("msgOrder:" + msgOrder[i]);
            else
                redFlag++;
        }

        if (!(redFlag == 0))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testMessageOrder_B_SecOff failed: Messages were not received in the expected order");

    }

    public void testMessageOrder_TCP_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        int msgOrder[] = new int[10];
        int redFlag = 0;

        JMSProducer producer1 = jmsContextQueueTCP.createProducer();
        JMSProducer producer2 = jmsContextQueueNewTCP.createProducer();
        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);
        Message msg = null;
        int msgRcvd = 0;
        for (int i = 0; i < 10; i++)
        {

            if (i % 2 == 0) {
                msg = jmsContextQueueTCP.createMessage();
                msg.setIntProperty("Message_Order", i);
                producer1.send(jmsQueue, msg);
            }
            else {
                msg = jmsContextQueueNewTCP.createMessage();
                msg.setIntProperty("Message_Order", i);
                producer2.send(jmsQueue, msg);
            }

            msgRcvd = jmsConsumer.receive(1000).getIntProperty("Message_Order");
            System.out.println("Received message number : " + msgRcvd);
            msgOrder[i] = msgRcvd;
        }

        for (int i = 0; i < 10; i++) {
            System.out.println("Retrieving Message Order:" + msgOrder[i]);
            if (msgOrder[i] == i)
                System.out.println("msgOrder:" + msgOrder[i]);
            else
                redFlag++;
        }

        if (!(redFlag == 0))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testMessageOrder_TCP_SecOff failed: Messages were not received in the expected order");

    }

    public void testGetAutoStart_B_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        if (!(jmsContextQueue.getAutoStart()))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testGetAutoStart_B_SecOff failed: getAutoStart did not return true");

    }

    public void testGetAutoStart_TCP_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        if (!(jmsContextQueueTCP.getAutoStart()))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testGetAutoStart_TCP_SecOff failed: getAutoStart did not return true");

    }

    public void testBrowser_B_SecOff(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextQueue.createTextMessage("testBrowser_B_SecOff");
        jmsContextQueue.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        QueueBrowser qb = jmsContextQueue.createBrowser(jmsQueue);

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        TextMessage message1 = (TextMessage) jmsConsumer.receive(1000);
        System.out.println("Received message: " + message1.getText());

        if (!(numMsgs == 1 && message1 != null && message1.getText().equals("testBrowser_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testBrowser_B_SecOff failed: Expected message was not received");

    }

    public void testBrowser_TCP_SecOff(HttpServletRequest request,
                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextQueueTCP.createTextMessage("testBrowser_TCP_SecOff");
        jmsContextQueueTCP.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);
        QueueBrowser qb = jmsContextQueueTCP.createBrowser(jmsQueue);

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        TextMessage message1 = (TextMessage) jmsConsumer.receive(1000);
        System.out.println("Received message: " + message1.getText());

        if (!(numMsgs == 1 && message1 != null && message1.getText().equals("testBrowser_TCP_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testBrowser_TCP_SecOff failed: Expected message was not received");

    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void testEJBCallSecOff(HttpServletRequest request,
                                  HttpServletResponse response) throws Throwable {

        String message = statelessBean.hello();
        System.out.println(message);
        statelessBean.sendMessage(message);

        TextMessage rec_msg = (TextMessage) jmsContextQueue.createConsumer(jmsQueue).receive(30000);
        System.out.println("Received Message is :" + rec_msg.getText());
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

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
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