package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.Properties;


import javax.jms.JMSContext;

import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;




public class GetJMSResources {
	
	public static QueueConnectionFactory qcf = null;
	public static ConnectionFactory cf = null;
	public static Queue jmsQueue = null;
	public static Queue jmsQueueMO = null;
	public static String providerEndPoints = null;
	public static String busName = null;
	public static String durSubHome = null;
	public static Message message = null;
	public static int order = 0;
	
	
	
	public static void establishConnection() throws NamingException{
		
		System.out.println("Entered establishConnection");
		Properties env = new Properties();
		env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
	      InitialContext jndi = new InitialContext(env);
	      qcf = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
	cf = (ConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		
System.out.println("Connection Established Successfully");
	
	}	
	public static QueueConnectionFactory getQCF(){
		return qcf;
	}
	public static ConnectionFactory getCF(){
		return cf;
	}
	
	public static Message getJMSMessage(int order){
try{
		JMSContext jmsContext = qcf.createContext()	;
			message = jmsContext.createMessage();
			message.setIntProperty("Message_Order", order);
		}catch (Exception e){
			e.printStackTrace();
		}
		return message;
		
	}
	
	
	
	public static Queue getJMSQueue(){
		
		try{
			//JMSContext jmsContext = qcf.createContext()	;
			//jmsQueue = jmsContext.createQueue("QUEUE1");
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
		      InitialContext jndi = new InitialContext(env);
		      jmsQueue = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q");
		
			 
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return jmsQueue;
	}
	
public static Queue getexQueue(){
		
		try{
			//JMSContext jmsContext = qcf.createContext()	;
			//jmsQueue = jmsContext.createQueue("exQUEUE");
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
		      InitialContext jndi = new InitialContext(env);
		      jmsQueue = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q1");
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return jmsQueue;
	}
public static Queue getJMSQueueMO(){
		
		try{
			//JMSContext jmsContext = qcf.createContext()	;
			//jmsQueueMO = jmsContext.createQueue("QUEUE2");
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
		      InitialContext jndi = new InitialContext(env);
		      jmsQueueMO = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q2");
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return jmsQueueMO;
	}
	
	
	
}
