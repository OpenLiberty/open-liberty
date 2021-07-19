package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class GetJMSResourcesCAPI {

	public static QueueConnectionFactory qcf = null;
	public static ConnectionFactory cf = null;
	public static QueueConnection queueConnection = null;
	public static Connection connection = null;
	public static QueueSession queueSession = null;
	public static Session session = null;
	public static QueueSession queueSessionTransacted = null;

	public static MessageProducer messageProducer = null;
	public static MessageProducer messageProducerMO = null;
	public static MessageConsumer messageConsumer = null;
	public static MessageConsumer messageConsumerMO = null;
	public static Queue jmsQueue = null;
	public static Queue jmsQueueMO = null;
	public static Queue exQueue = null;
	public static String providerEndPoints = null;
	public static String busName = null;
	public static String durSubHome = null;
	public static Message message = null;
	public static int order = 0;

	
	
	
public static void establishConnection() throws NamingException, JMSException{
		
		System.out.println("Entered establishConnection");
		Properties env = new Properties();
		env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
	      InitialContext jndi = new InitialContext(env);
	      qcf = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
	
	      cf = (ConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
			queueConnection = qcf.createQueueConnection();
		connection = cf.createConnection();
			queueSession = queueConnection.createQueueSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			session = connection.createSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			System.out.println("Connection Established Successfully");
		


	
	}	

	public static QueueConnectionFactory getQCF() {
		return qcf;
	}

	public static QueueSession getQueueSession() {
		return queueSession;
	}

	public static QueueConnection getQueueConnection() {
		return queueConnection;
	}
	public static ConnectionFactory getCF() {
		return cf;
	}

	public static Session getSession() {
		return session;
	}

	public static Connection getConnection() {
		return connection;
	}
	public static Message getJMSMessage(int order) {
		try {

			message = queueSession.createMessage();
			message.setIntProperty("Message_Order", order);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;

	}

	public static Queue getJMSQueue() {

		try {
			//JMSContext jmsContext = qcf.createContext()	;
			//jmsQueue = jmsContext.createQueue("QUEUE1");
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
		      InitialContext jndi = new InitialContext(env);
		      jmsQueue = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return jmsQueue;
	}

	public static Queue getexQueue() {

		try {
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
		      InitialContext jndi = new InitialContext(env);
		      jmsQueue = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q1");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return jmsQueue;
	}

	public static Queue getJMSQueueMO() {

		try {
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
		      InitialContext jndi = new InitialContext(env);
		      jmsQueueMO = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q2");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return jmsQueueMO;
	}

}
