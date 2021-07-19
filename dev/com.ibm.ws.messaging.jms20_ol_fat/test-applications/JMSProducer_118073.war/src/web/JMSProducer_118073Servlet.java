package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
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
public class JMSProducer_118073Servlet extends HttpServlet {
    public static QueueConnectionFactory QCFBindings;
    public static QueueConnectionFactory QCFTCP;

    public static TopicConnectionFactory TCFBindings;
    public static TopicConnectionFactory TCFTCP;

    public static boolean exceptionFlag;
    public static Queue queue;
    public static Queue queue1;

    public static Topic topic;
    public static Topic topic1;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {

            QCFBindings = getQCFBindings();
            TCFBindings = getTCFBindings();
            QCFTCP = getQCFTCP();
            TCFTCP = getTCFTCP();
            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

            queue1 = (Queue) new InitialContext().lookup("java:comp/env/eis/queue1");

            topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

            topic1 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");

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
        final TraceComponent tc = Tr.register(JMSProducer_118073Servlet.class); // injection
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

    public void testSetGetJMSReplyTo_B_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {

        exceptionFlag = false;

        JMSContext jmsContextSend = QCFBindings.createContext();
        JMSProducer producer = jmsContextSend.createProducer();
        producer.setJMSReplyTo(queue1);
        TextMessage msg = jmsContextSend.createTextMessage("testSetGetJMSReplyTo_B");
        producer.send(queue, msg);

        JMSContext jmsContextRec = QCFBindings.createContext();
        JMSConsumer qr = jmsContextRec.createConsumer(queue);
        TextMessage recmsg = (TextMessage) qr.receiveNoWait();

        System.out.println("testSetGetJMSReplyTo_B: ReplyTo destination is set to " + producer.getJMSReplyTo());

        Destination d1 = producer.getJMSReplyTo();
        System.out.println("Sending reply");
        JMSProducer replyProd = jmsContextRec.createProducer();
        TextMessage msg1 = jmsContextRec.createTextMessage("testSetGetJMSReplyTo_B: Reply Msg");
        msg1.setJMSCorrelationID(recmsg.getJMSMessageID());
        replyProd.send(d1, msg1);

        JMSConsumer consumer = jmsContextSend.createConsumer(queue1);
        TextMessage replymsg = (TextMessage) consumer.receiveNoWait();
        consumer.receiveNoWait();
        String replyID = new String(replymsg.getJMSCorrelationID());
        if (!(replyID.equals(msg.getJMSMessageID())))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testSetGetJMSReplyTo_B_SecOff failed");

        jmsContextSend.close();
        jmsContextRec.close();
    }

    public void testSetGetJMSReplyTo_TCP_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Exception {

        exceptionFlag = false;

        JMSContext jmsContextSend = QCFTCP.createContext();
        JMSProducer producer = jmsContextSend.createProducer();
        producer.setJMSReplyTo(queue1);
        TextMessage msg = jmsContextSend.createTextMessage("testSetGetJMSReplyTo_TCP");
        producer.send(queue, msg);

        JMSContext jmsContextRec = QCFTCP.createContext();
        JMSConsumer qr = jmsContextRec.createConsumer(queue);
        TextMessage recmsg = (TextMessage) qr.receiveNoWait();

        System.out.println("testSetGetJMSReplyTo_TCP: ReplyTo destination is set to " + producer.getJMSReplyTo());

        Destination d1 = producer.getJMSReplyTo();
        System.out.println("Sending reply");
        JMSProducer replyProd = jmsContextRec.createProducer();
        TextMessage msg1 = jmsContextRec.createTextMessage("testSetGetJMSReplyTo_TCP: Reply Msg");
        msg1.setJMSCorrelationID(recmsg.getJMSMessageID());
        replyProd.send(d1, msg1);

        JMSConsumer consumer = jmsContextSend.createConsumer(queue1);
        TextMessage replymsg = (TextMessage) consumer.receiveNoWait();
        consumer.receiveNoWait();
        String replyID = new String(replymsg.getJMSCorrelationID());
        if (!(replyID.equals(msg.getJMSMessageID())))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testSetGetJMSReplyTo_TCP_SecOff failed");

        jmsContextSend.close();
        jmsContextRec.close();
    }

    public void testSetGetJMSReplyTo_Topic_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        JMSContext jmsContext = TCFBindings.createContext();
        JMSProducer producer = jmsContext.createProducer();
        producer.setJMSReplyTo(topic1);
        String replyTo = "topic://Default.Topic?topicSpace=NewTopic2";
        if (!(replyTo.equals(producer.getJMSReplyTo().toString())))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testSetGetJMSReplyTo_Topic_B_SecOff failed");

        jmsContext.close();
    }

    public void testSetGetJMSReplyTo_Topic_TCP_SecOff(HttpServletRequest request,
                                                      HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        JMSContext jmsContext = TCFTCP.createContext();
        JMSProducer producer = jmsContext.createProducer();
        producer.setJMSReplyTo(topic1);
        String replyTo = "topic://Default.Topic?topicSpace=NewTopic2";
        if (!(replyTo.equals(producer.getJMSReplyTo().toString())))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testSetGetJMSReplyTo_Topic_TCP_SecOff failed");

        jmsContext.close();
    }

    public void testNullJMSReplyTo_B_SecOff(HttpServletRequest request,
                                            HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setJMSReplyTo(null);
        jmsProducer.send(queue, "testNullJMSReplyTo_B");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        TextMessage m = (TextMessage) jmsConsumer.receiveNoWait();

        Object o = m.getJMSReplyTo();
        if (!(o == null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testNullJMSReplyTo_B_SecOff failed");

        jmsContext.close();
    }

    public void testNullJMSReplyTo_TCP_SecOff(HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setJMSReplyTo(null);
        jmsProducer.send(queue, "testNullJMSReplyTo_TCP");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

        TextMessage m = (TextMessage) jmsConsumer.receiveNoWait();

        Object o = m.getJMSReplyTo();
        if (!(o == null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testNullJMSReplyTo_TCP_SecOff failed");

        jmsContext.close();
    }

    public void testSetAsync_B_SecOff(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        exceptionFlag = false;

        JMSContext jmsContext = QCFBindings.createContext();
        JMSProducer producer = jmsContext.createProducer();

        try {
            producer.setAsync(null);

        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;
            ex.printStackTrace();
        }

        if (!(exceptionFlag))
            throw new WrongException("testSetAsync_B_SecOff failed");

        jmsContext.close();

    }

    public void testSetAsync_TCP_SecOff(HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {

        exceptionFlag = false;

        JMSContext jmsContext = QCFTCP.createContext();
        JMSProducer producer = jmsContext.createProducer();

        try {
            producer.setAsync(null);

        } catch (JMSRuntimeException ex) {
            exceptionFlag = true;
            ex.printStackTrace();
        }

        if (!(exceptionFlag))
            throw new WrongException("testSetAsync_TCP_SecOff failed");

        jmsContext.close();

    }

    public void testGetAsync_B_SecOff(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        JMSContext jmsContext = QCFBindings.createContext();
        JMSProducer producer = jmsContext.createProducer();

        CompletionListener cl = producer.getAsync();
        if (!(null == cl))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testGetAsync_B_SecOff failed");

        jmsContext.close();
    }

    public void testGetAsync_TCP_SecOff(HttpServletRequest request,
                                        HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        JMSContext jmsContext = QCFTCP.createContext();
        JMSProducer producer = jmsContext.createProducer();

        CompletionListener cl = producer.getAsync();
        if (!(null == cl))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testGetAsync_TCP_SecOff failed");

        jmsContext.close();
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

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

}
