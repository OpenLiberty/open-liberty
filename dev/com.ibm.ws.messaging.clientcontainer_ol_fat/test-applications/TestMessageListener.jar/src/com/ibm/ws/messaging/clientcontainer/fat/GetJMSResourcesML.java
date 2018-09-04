package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.Properties;


import javax.jms.JMSContext;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;




public class GetJMSResourcesML {
	
	public static QueueConnectionFactory qcf = null;
	public static QueueConnectionFactory qcf1 = null;
	public static QueueConnectionFactory qcf2 = null;
	public static Queue jmsQueue = null;
	public static Queue jmsQueue3 = null;
	public static Queue jmsQueueMO = null;
	public static String providerEndPoints = null;
	public static String busName = null;
	public static String durSubHome = null;
	public static Message message = null;
	public static int order = 0;
	
	
	
	public static void establishConnection() throws NamingException{

		Properties env = new Properties();
		env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
	      InitialContext jndi = new InitialContext(env);
	      qcf = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
	      qcf1 = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF1");
	      qcf2 = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF2");

	
	}	
	public static QueueConnectionFactory getQCF(){
		return qcf;
	}
	
	public static QueueConnectionFactory getQCF1(){
		return qcf1;
	}
	
	public static QueueConnectionFactory getQCF2(){
		return qcf2;
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
	
	
public static Queue getJMSQueue3(){
		
		try{
			//JMSContext jmsContext = qcf.createContext()	;
			//jmsQueue = jmsContext.createQueue("QUEUE1");
			Properties env = new Properties();
			env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
		      InitialContext jndi = new InitialContext(env);
		      jmsQueue3 = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q3");
		     
		
			 
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return jmsQueue3;
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
