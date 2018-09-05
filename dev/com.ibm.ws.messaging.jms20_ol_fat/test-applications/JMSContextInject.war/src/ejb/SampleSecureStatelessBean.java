package ejb;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSSessionMode;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless
public class SampleSecureStatelessBean {

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF")
    @JMSSessionMode(JMSContext.SESSION_TRANSACTED)
    JMSContext jmscontext;

    public String hello() {
        return "EJBMessage";
    }

    public void sendMessage(String text) {
        System.out.println("sending message");

        Queue queue;
        try {

            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

            Message msg = jmscontext.createTextMessage(text);
            jmscontext.createProducer().send(queue, msg);

            System.out.println("Sent message");
        } catch (NamingException e) {
            e.printStackTrace();
        }

    }
}
