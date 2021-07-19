package com.ibm.ws.messaging.clientcontainer.fat;


import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.InvalidClientIDRuntimeException;
import javax.jms.IllegalStateException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ClientIDTestcase {
	
	public JMSProducer jmsProducer = null;
	public JMSConsumer jmsConsumer = null;
	public Queue jmsQueue = null;
	public Topic jmsTopic = null;
	public Message message = null;
	public static QueueConnectionFactory qcf = null;
	public static ConnectionFactory cf = null;
	public static TopicConnectionFactory tcf = null;

	public static JMSContext jmsContext = null;
	public static QueueBrowser qb = null;

	
	public String initialPrep1() {

		try {
		
		Properties env = new Properties();
		env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
	    InitialContext jndi = new InitialContext(env);
	    cf = (ConnectionFactory) jndi.lookup("jndi_JMS_BASE_CF");
	    qcf = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
	    tcf = (TopicConnectionFactory) jndi.lookup("java:comp/env/eis/tcf");
	    
       jmsQueue = (Queue) jndi.lookup("java:comp/env/jndi_INPUT_Q");
       jmsTopic = (Topic) jndi.lookup("java:comp/env/jndi_INPUT_T");
		
	    } catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return ("initial Connection done");
	}	
	
	// For the defect 174439
	public void testSetClientID() throws JMSException
	 {

		JMSContext jmsContext = cf.createContext();
		JMSContext jmsContextQ = qcf.createContext();
		JMSContext jmsContextT = tcf.createContext();
		String clientId =null;
		String clientIdQ =null;
		String clientIdT =null;
		boolean flag = false;
		
		try{
			jmsContext.setClientID("ClientID1");
			flag = true;
			
		}catch(IllegalStateRuntimeException e){
			flag = false;			
			e.printStackTrace();
		}
		
		try{
			jmsContextQ.setClientID("ClientID2");
			flag = true;
			
		}catch(IllegalStateRuntimeException e){
			flag = false;			
			e.printStackTrace();
		}
		
		try{
			jmsContextT.setClientID("ClientID3");
			flag = true;
			
		}catch(IllegalStateRuntimeException e){
			flag = false;			
			e.printStackTrace();
		}
		
		try {
			clientId = jmsContext.getClientID();
		}catch(JMSRuntimeException e){
			e.printStackTrace();
		}
		
		try {
			clientIdQ = jmsContextQ.getClientID();
		}catch(JMSRuntimeException e){
			e.printStackTrace();
		}	
		
		try {
			clientIdT = jmsContextT.getClientID();
		}catch(JMSRuntimeException e){
			e.printStackTrace();
		}	
		

		if (clientId.equals("ClientID1") && clientIdQ.equals("ClientID2") && clientIdT.equals("ClientID3") && flag == true ) {
			
				System.out.println("Test Case: testSetClientID Passed"); 
		}
		else {	
			System.out.println("Test Case: testSetClientID failed");
		}
		jmsContext.close();
		jmsContextQ.close();
		jmsContextT.close();
		

	 }
	
	
	//For the defect 174439
	public void testSetClientIDID() throws JMSException
	 {

		JMSContext jmsContext = cf.createContext();
		JMSContext jmsContextQ = qcf.createContext();
		JMSContext jmsContextT = tcf.createContext();
		JMSContext jmsContext1 = cf.createContext();
		JMSContext jmsContextQ1 = qcf.createContext();
		JMSContext jmsContextT1 = tcf.createContext();
		String clientId =null;
		String clientIdQ =null;
		String clientIdT =null;
		boolean flag = false;
		
		try{
			jmsContext.setClientID("ClientID1");
			
		}catch(IllegalStateRuntimeException e){
			e.printStackTrace();
		}
		
		try{
			jmsContext1.setClientID("ClientID1");
		}
		catch(InvalidClientIDRuntimeException e){
			flag = true;
			e.printStackTrace();			
		}
		
		 
		try{
			jmsContextQ.setClientID("ClientID2");
			
		}catch(IllegalStateRuntimeException e){
			e.printStackTrace();
		}
		
		try{
			jmsContextQ1.setClientID("ClientID2");
		}
		catch(InvalidClientIDRuntimeException e){
			flag = true;
			e.printStackTrace();			
		}
		
		try{
			jmsContextT.setClientID("ClientID3");
			
		}catch(IllegalStateRuntimeException e){
			e.printStackTrace();
		}
		
		try{
			jmsContextT1.setClientID("ClientID3");
		}
		catch(InvalidClientIDRuntimeException e){
			flag = true;
			e.printStackTrace();			
		}
		
		
		try {
			clientId = jmsContext.getClientID();
		}catch(JMSRuntimeException e){
			e.printStackTrace();
		}
		
		try {
			clientIdQ = jmsContextQ.getClientID();
		}catch(JMSRuntimeException e){
			e.printStackTrace();
		}	
		
		try {
			clientIdT = jmsContextT.getClientID();
		}catch(JMSRuntimeException e){
			e.printStackTrace();
		}	
		

		if (clientId.equals("ClientID1") && clientIdQ.equals("ClientID2") && clientIdT.equals("ClientID3") && flag == true ) {
			
				System.out.println("Test Case: testSetClientIDID Passed"); 
		}
		else {	
			System.out.println("Test Case: testSetClientID failed");
		}
		jmsContext.close();

		
	 }
	
// For defect 174254 , 174257 "& 174881
	

	public void testDurableSubscriberClientIDEmpty() throws JMSException
	 {

	
		Connection jmsConnection = cf.createConnection();
		Session jmsSession = jmsConnection.createSession();
		
		
		TopicConnection topicConnection = tcf.createTopicConnection();		
		TopicSession topicSession = topicConnection.createTopicSession(true, 1);
		
		
		MessageConsumer mc = null;
		TopicSubscriber ts = null;
		
		boolean flag = true;
		
		// =======================JMS Session methods 
		
		try {
	
			mc = jmsSession.createDurableConsumer(jmsTopic, "topic5");
			flag = false;
		}
		catch(IllegalStateException e)
		{
				e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
				
		try {
	
			mc = jmsSession.createDurableConsumer(jmsTopic, "topic5" , "sel1", true);
			flag = false;
		}
		catch(IllegalStateException e)
		{
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			ts = jmsSession.createDurableSubscriber(jmsTopic, "topic5");
			flag = false;
		}
		catch(IllegalStateException e)
		{
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			ts = jmsSession.createDurableSubscriber(jmsTopic, "topic5" , "sel1", true);
			flag = false;
		}
		catch(IllegalStateException e)
		{
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		
		try {
	
			mc = jmsSession.createSharedConsumer(jmsTopic, "topic6");			
		}
		catch(JMSException e)
		{
			flag = false;
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			mc = jmsSession.createSharedConsumer(jmsTopic, "topic5" , "sel1");			
		}
		catch(JMSException e)
		{
			flag = false;
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			mc = jmsSession.createSharedDurableConsumer(jmsTopic, "topic5");			
		}
		catch(JMSException e)
		{
			flag = false;
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			mc = jmsSession.createSharedDurableConsumer(jmsTopic, "topic6" , "sel1");			
		}
		catch(JMSException e)
		{
			flag = false;
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		//==================================================================		
		// ============================= Topic Session ======================		
	
		try {
	
			ts = topicSession.createDurableSubscriber(jmsTopic, "topic5");
			flag = false;
		}
		catch(javax.jms.IllegalStateException  e)
		{
			e.printStackTrace();
		}		
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			ts = topicSession.createDurableSubscriber(jmsTopic, "topic5" , "sel1", true);
			flag = false;
		}
		catch(javax.jms.IllegalStateException  e)
		{
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			ts = topicSession.createSubscriber(jmsTopic);
			
		}
		catch(JMSException  e)
		{
			flag = false;
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
		
		try {
	
			ts = topicSession.createSubscriber(jmsTopic, "topic5" , true);
			
		}
		catch(JMSException  e)
		{
			flag = false;
			e.printStackTrace();
		}
		catch (Exception e) {			
	        flag = false;
	        e.printStackTrace();
	    }
	

		if (flag == true ) {			
			System.out.println("Test Case: testDurableSubscriberClientIDEmpty Passed"); 
		}
		else {	
			System.out.println("Test Case: testDurableSubscriberClientIDEmpty failed");

		}		

	 }
	


}
