/* ============================================================================
 * Copyright (c) 2019, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 * ============================================================================
 */
package com.ibm.ws.messaging.open_clientcontainer.fat;

import static javax.jms.DeliveryMode.NON_PERSISTENT;
import static javax.jms.DeliveryMode.PERSISTENT;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static javax.naming.Context.PROVIDER_URL;

import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

public class JMS1AsyncSend extends ClientMain {
    public static void main(String[] args) {
        new JMS1AsyncSend().run();
    }

    private QueueConnectionFactory queueConnectionFactory_ = null;
    private QueueConnection queueConnection_ = null;
    private Connection connection_ = null;
    private Queue queueOne_ = null;
    private Queue depthLimitedQueue_ = null;

    /** @return the methodName of the caller. */
    private static final String methodName() {
        return new Exception().getStackTrace()[1].getMethodName();
    }

    private final class TestException extends Exception {
        private static final long serialVersionUID = 1L;

        TestException(String message) {
            super(new Date() + " " + message);
        }

        TestException(String message, Throwable cause) {
            super(new Date() + " " + message, cause);
        }
    }

    @Override
    protected void setup() throws Exception {
        Util.TRACE_ENTRY();

        Properties env = new Properties();
        env.put(PROVIDER_URL, "iiop://localhost:2809");
        InitialContext jndi = new InitialContext(env);
        Util.CODEPATH();
        queueConnectionFactory_ = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Util.CODEPATH();
        ConnectionFactory cf = (ConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Util.CODEPATH();
        queueOne_ = (Queue) jndi.lookup("java:comp/env/jndi_QUEUE_ONE");
        Util.CODEPATH();
        depthLimitedQueue_ = (Queue) jndi.lookup("java:comp/env/jndi_DEPTH_LIMITED_QUEUE");
        Util.CODEPATH();
        queueConnection_ = queueConnectionFactory_.createQueueConnection();
        Util.CODEPATH();
        connection_ = cf.createConnection();
        Util.CODEPATH();
        queueConnection_.start();
        Util.CODEPATH();
        connection_.start();
        Util.CODEPATH();

        Util.TRACE_EXIT();
    }

    @ClientTest
    public void testJMS1AsyncSend() throws JMSException, TestException {
    	
    	Util.CODEPATH();

    	try (QueueConnection connection = queueConnectionFactory_.createQueueConnection();
		     QueueSession session = connection.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
    		
    		// Use a temporary Queue so that it is automatically cleaned up whne the Connection is closed.
    		Queue queue = session.createTemporaryQueue();
    		
            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date());
            MessageProducer producer = session.createProducer(queue);
            MessageConsumer consumer = session.createConsumer(queue);
            BasicCompletionListener completionListener = new BasicCompletionListener();
            
            connection.start();
            
            
            Util.LOG("Sending Message with CompletionListener");
            producer.send(sentMessage, completionListener);
            
            Util.LOG("Message sent. Waiting for CompletionListener to be invoked");

            if (!completionListener.waitFor(1, 0)) {
                throw new TestException("Completion listener not notified, sent:" + sentMessage + " completionListener.formattedState:" + completionListener.formattedState());
            }
            
            Util.TRACE("Sent Message: ", sentMessage);

            Util.LOG("Receiving Message");
            TextMessage receivedMessage = (TextMessage) consumer.receive(WAIT_TIME);

            if (null == receivedMessage) {
                Util.LOG("No message received.");
                throw new TestException("Message not received, sent:\n" + sentMessage + "\ncompletionListener.formattedState:\n" + completionListener.formattedState());
            } else {
            	Util.LOG("Message received");
                Util.TRACE("receivedMessage:" + receivedMessage);
            }

            if (!receivedMessage.getText().equals(sentMessage.getText()))
                throw new TestException("Incorrect message received, receivedMessage:" + receivedMessage + "\n sentMessage:" + sentMessage);

        }
    	// Deal with possible exceptions
    	catch (JMSException | TestException ex) {
    		Util.LOG("Exception thrown during test", ex);
    		throw ex;
    	}
    	// Deal with unchecked Exceptions
    	catch (Throwable t) {
    		Util.LOG("Unchecked Exception thrown during test", t);
    		TestException newException = new TestException("Unexpected Exception thrown during test", t);
    		throw newException;
    	}

        reportSuccess();
    }

    // Case where the acknowledgement is not received, the JMS provider would notify
    // the application by invoking the
    // CompletionListener's onException method.

    @ClientTest
    public void testJMS1ExceptionMessageThreshhold() throws JMSException, TestException {
        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageProducer producer = session.createProducer(depthLimitedQueue_);

            BasicCompletionListener completionListener = new BasicCompletionListener();
            for (int i = 0; i < 6; i++) {
                TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date() + " Sequence:" + i);
                producer.send(sentMessage, completionListener);
            }

            if (!completionListener.waitFor(5, 1)) {
                throw new TestException("Expected completion & exception notification not recevied, completionListener.formattedState:" + completionListener.formattedState());
            }

        } finally {
            clearQueue(depthLimitedQueue_, methodName(), 5);
        }

        reportSuccess();
    }

  @ClientTest
  public void testJMS1AsyncSendException() throws JMSException, TestException {
      //Util.setLevel(Level.FINEST);
	  
	  Util.CODEPATH();

      try ( QueueConnection queueConnection = queueConnectionFactory_.createQueueConnection();
    		QueueSession queueSession = queueConnection.createQueueSession(false, AUTO_ACKNOWLEDGE)) {  
          
    	  Util.TRACE("Getting Test objects");
    	  
    	  BasicCompletionListener completionListener = new BasicCompletionListener();
          Queue queue = queueSession.createTemporaryQueue();
          
          queueConnection.start();
          
          try {
              MessageProducer producer = queueSession.createProducer(null);

              Util.TRACE("Sending null Message. Expected to fail");
              producer.send(queue, null, completionListener);
              
              Util.LOG("Send message did not throw expected Exception");
              throw new TestException("Expected MessageFormatException not thrown, completionListener state="+completionListener.formattedState());
          } catch (MessageFormatException e) {
              if (completionListener.completionCount_ != 0 )
                  throw new TestException("Non zero completionCount completionListener.formattedState:"+completionListener.formattedState());
              if (completionListener.exceptionCount_ != 0)         
                  throw new TestException("Non zero exceptionCount completionListener.formattedState:"+completionListener.formattedState());
          }       
      }
      catch ( JMSException | TestException e) {
    	  Util.LOG("Exception thrown during test", e);
    	  throw e;
      }

      reportSuccess();
      //Util.setLevel(Level.INFO);
  }

    @ClientTest
    public void testJMS1MessageOrderingSingleProducer() throws JMSException, InterruptedException, TestException {
        
        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageConsumer consumer = session.createConsumer(queueOne_);
            MessageProducer producer = session.createProducer(null);

            MessageOrderCompletionListener messageOrderListener = new MessageOrderCompletionListener();
            messageOrderListener.setExpectedMessageCount(5);

            for (int i = 0; i < messageOrderListener.getExpectedMessageCount(); i++) {
                TextMessage sentMessage = session.createTextMessage(methodName()+" Sequence:"+i+" at "+new Date());
                sentMessage.setIntProperty("Message_Order", i);
                producer.send(queueOne_, sentMessage, PERSISTENT, 0, 10000, messageOrderListener);
            }
            
            for (int i = 0; i < messageOrderListener.getExpectedMessageCount(); i++) {
                TextMessage receivedMessage = (TextMessage) consumer.receive(WAIT_TIME);
                int messageOrder = receivedMessage.getIntProperty("Message_Order");
                Util.TRACE("i:"+i+"receievdMessage:"+ receivedMessage);
                if (i != messageOrder)
                    throw new TestException("Message receivedOut of order i:"+i +"\nreceivedMessage:"+receivedMessage);            
            }

            Util.TRACE(messageOrderListener.formattedState());
            if (!messageOrderListener.waitFor(messageOrderListener.getExpectedMessageCount(), 0))
                throw new TestException("Incorrect completion notifications messageOrderListener.formattedState:" + messageOrderListener.formattedState());
            if (messageOrderListener.getMessageOrderCount() != messageOrderListener.getExpectedMessageCount())
                throw new TestException("Invalid messageOrderCount:" + messageOrderListener.formattedState());

        } finally {
            clearQueue(queueOne_, methodName(), 0);
        }
        
        reportSuccess();
    }

    @ClientTest
    public void testJMS1MessageOrderingMultipleProducers() throws JMSException, InterruptedException, TestException {
        
        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageProducer producer[] = { session.createProducer(queueOne_), 
                                           session.createProducer(queueOne_), 
                                           session.createProducer(queueOne_),
                                           session.createProducer(queueOne_), 
                                           session.createProducer(queueOne_) };
            MessageConsumer consumer = session.createConsumer(queueOne_);

            MessageOrderCompletionListener messageOrderListener = new MessageOrderCompletionListener();
            messageOrderListener.setExpectedMessageCount(5);

            for (int i = 0; messageOrderListener.getExpectedMessageCount() > i; ++i) {
                TextMessage sentMessage = session.createTextMessage(methodName()+" Sequence:"+i+" at "+new Date());
                sentMessage.setIntProperty("Message_Order", i);
                producer[i].send(sentMessage, messageOrderListener);              
            }
            
            for (int i = 0; messageOrderListener.getExpectedMessageCount() > i; ++i) {
                TextMessage receivedMessage = (TextMessage) consumer.receive(WAIT_TIME);
                int messageOrder = receivedMessage.getIntProperty("Message_Order");
                Util.TRACE("i:"+i+"receievdMessage:"+ receivedMessage);
                if (i != messageOrder)
                    throw new TestException("Message receivedOut of order i:"+i +"\nreceivedMessage:"+receivedMessage);   
            }

            Util.TRACE(messageOrderListener.formattedState());
            if (!messageOrderListener.waitFor(messageOrderListener.getExpectedMessageCount(), 0)) 
                throw new TestException("Incorrect completion notifications messageOrderListener.formattedState:" + messageOrderListener.formattedState());
            if (messageOrderListener.getMessageOrderCount() != messageOrderListener.getExpectedMessageCount())
                throw new TestException("Invalid messageOrderCount:" + messageOrderListener.formattedState());
            
        } finally {
            clearQueue(queueOne_, methodName(), 0);
        }
        reportSuccess();
    }

    /**
     From "Java Message Service" Version 2.0 revision a (March 2015):
     <p>
     6.2.9. Message order
     <blockQuote>
     [...]
  
     JMS defines that messages sent by a session to a destination must be received in the order in which they were sent[...]
  
     JMS does not define order of message receipt across destinations or across a destination's messages sent from multiple
     sessions. This aspect of a session's input message stream order is timing-dependent. It is not under application control
  
     So all we can test for with multiple sessions is ordering of messages from each session.
     </blockquote>
    */
    @ClientTest
    public void testJMS1MessageOrderingMultipleSessions() throws JMSException, InterruptedException, TestException {

        QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE);
        QueueSession producerSessions[] = { queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE),
                                            queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE), 
                                            queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE) };
        try {
            MessageProducer producer[] = { producerSessions[0].createProducer(queueOne_), 
                                           producerSessions[1].createProducer(queueOne_),
                                           producerSessions[2].createProducer(queueOne_) };
            MessageConsumer consumer = session.createConsumer(queueOne_);
            int order[] = { -1, -1, -1 };

            BasicCompletionListener completionListener = new BasicCompletionListener();
            for (int i = 0; 15 > i; ++i) {
                if (5 == i || 8 == i || 14 == i || 1 == i)
                    continue; // unequal number per session
                TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date() + " Sequence:" + i);
                sentMessage.setIntProperty("Session_Number", i % 3);
                sentMessage.setIntProperty("Message_Order", i);
                producer[i % 3].send(sentMessage, PERSISTENT, 0, 10000, completionListener);
            }

            Util.TRACE(completionListener.formattedState());
            if (!completionListener.waitFor(11, 0))
                throw new TestException("Incorrect completion notifications completionListener.formattedState:" + completionListener.formattedState());

            for (int i = 0; 11 > i; ++i) {
                Message receivedMessage = consumer.receive(WAIT_TIME);
                if (null == receivedMessage) {
                    Util.TRACE("i=" + i, "receivedMessage" + receivedMessage);
                    throw new TestException("Failed to receive message " + i);
                }
                int sessionNumber = receivedMessage.getIntProperty("Session_Number");
                int messageOrder = receivedMessage.getIntProperty("Message_Order");
                Util.TRACE("i=" + i + ",Session_Number=" + sessionNumber + ",Message_Order=" + messageOrder);
                if (order[sessionNumber] >= messageOrder)
                    throw new TestException(
                            "Message receivedOutOfOrder i=" + i + ",Session_Number=" + sessionNumber + ",Message_Order=" + messageOrder + "\nreceivedMessage:" + receivedMessage);
                order[sessionNumber] = messageOrder;
            }

        } finally {
            session.close();
            for (QueueSession producerSession : producerSessions)
                producerSession.close();
            clearQueue(queueOne_, methodName(), 0);
        }

        reportSuccess();
    }

    @ClientTest
    public void testJMS1CloseSession() throws JMSException, InterruptedException, TestException {

        try (QueueConnection connection = queueConnectionFactory_.createQueueConnection()) {
            QueueSession session = connection.createQueueSession(false, AUTO_ACKNOWLEDGE);
            StringBuffer sentMessageText = new StringBuffer(methodName() + " at " + new Date());
            while (sentMessageText.length() < 13000)
                sentMessageText.append("testJMS1CloseSession.");
            TextMessage sentMessage = session.createTextMessage(sentMessageText.toString());
            MessageProducer producer = session.createProducer(queueOne_);

            Util.CODEPATH();
            BasicCompletionListener completionListener = new BasicCompletionListener();
            for (int i = 0; i < 100; i++)
                producer.send(sentMessage, completionListener);
            Util.CODEPATH();
            session.close();
            Util.CODEPATH();
            
            Util.TRACE(completionListener.formattedState());
            if (!completionListener.waitFor(100, 0))
                throw new TestException("Expected completion notification not received, completionListener.formattedState:" + completionListener.formattedState());

        } finally {
            clearQueue(queueOne_, methodName(), 100);
        }

        reportSuccess();
    }

    @ClientTest
    public void testJMS1CloseConnection() throws JMSException, InterruptedException, TestException {

        try (QueueConnection connection = queueConnectionFactory_.createQueueConnection()) {
            QueueSession session = connection.createQueueSession(false, AUTO_ACKNOWLEDGE);
            StringBuffer sentMessageText = new StringBuffer(methodName() + " at " + new Date());
            while (sentMessageText.length() < 13000)
                sentMessageText.append("testJMS1CloseConnection.");
            TextMessage sentMessage = session.createTextMessage(sentMessageText.toString());
            MessageProducer producer = session.createProducer(queueOne_);

            Util.CODEPATH();
            BasicCompletionListener completionListener = new BasicCompletionListener();
            for (int i = 0; i < 100; i++)
                producer.send(sentMessage, completionListener);
            Util.CODEPATH();
            connection.close();
            Util.CODEPATH();
            
            Util.TRACE(completionListener.formattedState());
            if (!completionListener.waitFor(100, 0))
                throw new TestException("Failed to receive expected completion notifications completionListener.formattedState:" + completionListener.formattedState());

        } finally {
            clearQueue(queueOne_, methodName(), 100);
        }

        reportSuccess();
    }

    @ClientTest
    public void testJMS1AsyncSendUnidentifiedProducerUnidentifiedDestination() throws JMSException, InterruptedException, TestException {

    	Util.CODEPATH();
    	
        try ( QueueConnection queueConnection = queueConnectionFactory_.createQueueConnection();
        	  QueueSession session = queueConnection.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
        	
        	Util.TRACE("Getting Test objects");
            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date());
            MessageProducer producer = session.createProducer(null);
            
            queueConnection.start();
            
            BasicCompletionListener completionListener = new BasicCompletionListener();
            try {
            	Util.TRACE("Sending message to null Destination. Expected to fail");
                producer.send(null, sentMessage, completionListener);
                
                Util.LOG("Send message did not throw expected Exception");
                throw new TestException("InvalidDestinationException not thrown");
            } catch (InvalidDestinationException e) {
                // Expected Exception.
                Util.TRACE(e);
            }
            
            Util.TRACE(completionListener.formattedState());
            if (!completionListener.waitFor(0, 0))
                throw new TestException("Unexpected completion notification received, completionListener.formattedState:" + completionListener.formattedState());
        }
        catch (JMSException | TestException e) {
        	
        	Util.LOG("Exception thrown during test", e);
        	throw e;
        	
        }

        reportSuccess();
    }

    @ClientTest
    public void testJMS1AsyncSendNullListener() throws JMSException, TestException {
        
    	Util.CODEPATH();
    	
        try (QueueConnection queueConnection = queueConnectionFactory_.createQueueConnection();
        	 QueueSession session = queueConnection.createQueueSession(false, AUTO_ACKNOWLEDGE)) {

        	Util.TRACE("Creating test objects");
        	
        	Queue queue = session.createTemporaryQueue();
            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date());
            MessageProducer producer = session.createProducer(queue);
            
            queueConnection.start();
            
            try {
            	Util.LOG("Sending message with null CompletionListener");
                producer.send(sentMessage, null);
                throw new TestException("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
                // Expected Exception.
                Util.TRACE(e);
            }
        }
        catch (JMSException | TestException e) {
        	
        	Util.LOG("Exception thrown during test", e);
        	throw e;
        	
        }
        
        reportSuccess();
    }

    @ClientTest
    public void testJMS1AsyncSendNoDestination() throws JMSException, TestException {
    	
    	Util.CODEPATH();

        try (QueueConnection queueConnection = queueConnectionFactory_.createQueueConnection();
        	 QueueSession session = queueConnection.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
        	
        	Util.TRACE("Creating test objects");
        	
            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date());
            MessageProducer producer = session.createProducer(null);
            BasicCompletionListener completionListener = new BasicCompletionListener();

            queueConnection.start();
            
            try {
            	Util.LOG("Test no destination method 1");
                producer.send(sentMessage, completionListener);
                throw new TestException("UnsupportedOperationException 1 not thrown");
            } catch (UnsupportedOperationException e) {
                // Expected Exception.
                Util.TRACE(e);
            }
            try {
            	Util.LOG("Test null destination method 1");
                producer.send(null, sentMessage, completionListener);
                throw new TestException("InvalidDestinationException 1 not thrown");
            } catch (InvalidDestinationException e) {
                // Expected Exception.
                Util.TRACE(e);
            }
            try {
            	Util.LOG("Test no destination method 2");
                producer.send(sentMessage, PERSISTENT, 0, 10000, completionListener);
                throw new TestException("UnsupportedOperationException 2 not thrown");
            } catch (UnsupportedOperationException e) {
                // Expected Exception.
                Util.TRACE(e);
            }
            try {
            	Util.LOG("Test null destination method 2");
                producer.send(null, sentMessage, PERSISTENT, 0, 10000, completionListener);
                throw new TestException("InvalidDestinationException 2 not thrown");
            } catch (InvalidDestinationException e) {
                // Expected Exception.
                Util.TRACE(e);
            }
        }
        catch (JMSException | TestException e) {
        	
        	Util.LOG("Exception thrown during test", e);
        	throw e;
        	
        }

        reportSuccess();
    }

  @ClientTest
    public void testJMS1CompletionListener() throws JMSException, InterruptedException, TestException {

        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageProducer producer = session.createProducer(null);
            BasicCompletionListener completionListener = new BasicCompletionListener();
            for (int i = 0; i < 5; i++) {
                TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date() + " Sequence:" + i);
                producer.send(depthLimitedQueue_, sentMessage, completionListener);
            }

            if (!completionListener.waitFor(5, 0))
                throw new TestException("Excpected completion events not seen, completionListener.formattedState:"+completionListener.formattedState());
            Util.TRACE("completionListener.formattedState:"+completionListener.formattedState());

            // The sixth message exceeds the maximum queue depth and will not be sent.
            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date() + " Sequence:6");
            producer.send(depthLimitedQueue_, sentMessage, completionListener);

            if (!completionListener.waitFor(5, 1))
                throw new TestException("Excpected exception event not seen, completionListener.formattedState:"+completionListener.formattedState());
            Util.TRACE("completionListener.formattedState:"+completionListener.formattedState());

         } finally {
            clearQueue(depthLimitedQueue_, methodName(), 5);
        }
        
        reportSuccess();
    }

  @ClientTest
  public void testJMS1SessionInListener() throws Exception, JMSException, InterruptedException {

      try (QueueSession session1 = queueConnection_.createQueueSession(true,0);
           QueueSession session2 = queueConnection_.createQueueSession(true,0)
           ) {

          SessionCompletionListener sessionCompletionListener = new SessionCompletionListener(session1 ,session2 ,queueOne_);
          MessageProducer producer = sessionCompletionListener.session_.createProducer(null);
          TextMessage sentMessage = sessionCompletionListener.session_.createTextMessage(methodName() + " at " + new Date());

          producer.send(queueOne_, sentMessage, sessionCompletionListener);

          if (!sessionCompletionListener.waitFor(1, 0))
              throw new TestException("Expected notifications not received, sessionCompletionListener.formattedState:" + sessionCompletionListener.formattedState());

          session1.commit();

          Util.CODEPATH();
          QueueBrowser qb = session1.createBrowser(queueOne_);
          Enumeration<?> e1 = qb.getEnumeration();
          int messageCount = 0;
          for (messageCount=0;e1.hasMoreElements();e1.nextElement()) messageCount++;
          Util.TRACE("messageCount="+messageCount);
          if(messageCount != 1)
              throw new TestException("Incorrect number of messages received messageCount="+messageCount);

          Util.TRACE("sessionCompletionListener.formattedState="+sessionCompletionListener.formattedState());

          if (!(   sessionCompletionListener.exceptionOnUnrelatedClose_ == false
                  && sessionCompletionListener.exceptionOnClose_ == true
                  && sessionCompletionListener.exceptionOnCommit_ == true
                  && sessionCompletionListener.exceptionOnRollback_ == true
                  && sessionCompletionListener.producerCreated_ == true
                  && sessionCompletionListener.exceptionOnProducerClose_ == true))
              throw new TestException("Incorrect sessionCompletionListener state:"+sessionCompletionListener.formattedState());       
      } finally {
          clearQueue(queueOne_, methodName(), 1);
      } 
      reportSuccess();
  }

  @ClientTest
  public void testJMS1TransactionAndListener() throws Exception, JMSException, InterruptedException {

      try (QueueSession session = queueConnection_.createQueueSession(true, AUTO_ACKNOWLEDGE)) {
          MessageProducer producer = session.createProducer(depthLimitedQueue_);
          TextMessage sentMessage = session.createTextMessage(methodName()+" at "+new Date());
          BasicCompletionListener completionListener = new BasicCompletionListener();
          for (int i=0;5>i;++i) producer.send(sentMessage, completionListener);
          if (!completionListener.waitFor(5, 0))
              throw new TestException("First 5 messages not received, completionLister.formattedState():"+completionListener.formattedState());
          session.commit();
          Util.CODEPATH();

          // The Session is locally transacted, so the send will succeed. An SILimitExceededException exception will be raised
          // when the transaction commits. The completionLister.onException() must NOT be called.
          producer.send(sentMessage, completionListener);

          Util.TRACE("completionListener.formattedState:"+completionListener.formattedState());
          if (!completionListener.waitFor(6, 0))
              throw new TestException("Sixth message not sent, completionLister.formattedState():"+completionListener.formattedState());

          try {
              session.commit();
              Util.CODEPATH();
              throw new TestException("SIRollbackException not thrown");

          } catch (JMSException e) {
              if (null!=e.getCause()
                      &&"com.ibm.wsspi.sib.core.exception.SIRollbackException".equals(e.getCause().getClass().getName())
                      ) {
                  Throwable t = e.getCause();
                  if (null!=t.getCause()
                          &&"com.ibm.wsspi.sib.core.exception.SILimitExceededException".equals(t.getCause().getClass().getName())
                          ) {
                      Util.TRACE("commit failed with expected exception: "+t.getCause().getClass().getName());
                  } else {
                      throw new TestException("Unexpected SIRollbackException cause"+t,e);
                  }
              } else {
                  throw new TestException("Unexpected JMSException cause",e);
              }
          }

          Util.TRACE("completionListener.formattedState:"+completionListener.formattedState());
          if (!completionListener.waitFor(6, 0))
              throw new TestException("Incorrect notofocations, completionLister.formattedState():"+completionListener.formattedState());

      } finally {
          clearQueue(depthLimitedQueue_, methodName(), 5);
      }

      reportSuccess();
  }

    @ClientTest
    public void testJMS1NegativeTimeToLive() throws JMSException, InterruptedException, TestException {
        
        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageProducer producer = session.createProducer(queueOne_);
            MessageConsumer consumer = session.createConsumer(queueOne_);

            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date());
            BasicCompletionListener completionListener = new BasicCompletionListener();

            try {
                producer.send(sentMessage, NON_PERSISTENT, 0, -100, completionListener);
                throw new TestException("Send with negative time to live succeeded.");
            } catch (JMSException e) {
                // Expected Exception.
                Util.TRACE(e);
            }
            Util.CODEPATH();
            
            // Wait for 100ms while queue remains empty, expectedCompletionCount should remain zero, not advance to 1.
            if(completionListener.waitFor(100, 1, 0))
                throw new TestException("Completion listener saw unexpected completion fired completionListener.formattedState:"+completionListener.formattedState());
            Util.CODEPATH();

            Message receivedMessage = consumer.receiveNoWait();
            Util.TRACE("messageReceived="+receivedMessage);
            if (receivedMessage != null)
                throw new TestException("Unexpected message, receivedMessage:"+receivedMessage+"\nsentMessage:"+sentMessage);
          
        } finally {
            clearQueue(queueOne_, methodName(), 0);
        }  
        
        reportSuccess();
    }

    /**
     * JMS Specification Version 2.0 revision a
     * <p>
     * 3.4.10. JMSPriority...
     *
     * <blockquote> JMS does not require that a provider strictly implement priority
     * ordering of messages; however, it should do its best to deliver expedited
     * messages ahead of normal messages. </blockquote>
     */
    @ClientTest
    public void testJMS1Priority() throws JMSException, InterruptedException, TestException {

        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageConsumer consumer = session.createConsumer(queueOne_);
            MessageProducer producer = session.createProducer(null);

            // Send 10 messages with increasing priority. The last message should be received first, assuming that all
            // of the messages are available for receipt when the first (last sent) message is consumed. The session is 
            // AUTO_ACKNOWLEDGE so this will not be true if some of the later messages are buffered.

            BasicCompletionListener completionListener = new BasicCompletionListener();
            long sequence = 0;
            for (int priority = 0; priority < 10; priority++) {
                TextMessage sentMessage = session.createTextMessage(methodName()+" at "+new Date() +" Sequence:"+sequence++);
                producer.send(queueOne_, sentMessage, PERSISTENT, priority, 10000, completionListener);
            }

            completionListener.waitFor(10, 0);
            Util.CODEPATH();

            // Poll the queue until the highest priority message is available at the head.
            QueueBrowser queueBrowser = session.createBrowser(queueOne_); 
            for (int iPoll = 10; ((TextMessage) queueBrowser.getEnumeration().nextElement()).getJMSPriority() != 9  && iPoll > 0; iPoll--) {
                Thread.sleep(10);
            }

            for (int expectedPriority = 9; expectedPriority >= 0; expectedPriority--) {
                TextMessage receivedMessage = (TextMessage)consumer.receiveNoWait();
                Util.TRACE("expectedPriority="+expectedPriority+" ,messageReceived.getJMSPriority()="+receivedMessage.getJMSPriority());
                if (receivedMessage.getJMSPriority() != expectedPriority)
                    throw new TestException("Incorrect message received expectedPriority:"+expectedPriority+ "\n receivedMessage:"+receivedMessage);
            }

        } finally {
            clearQueue(queueOne_, methodName(), 0);
        }

        reportSuccess();
    }

    @ClientTest
    public void testJMS1NegativePriority() throws JMSException, InterruptedException, TestException {

        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageProducer producer = session.createProducer(null);

            BasicCompletionListener completionListener = new BasicCompletionListener();
            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date());

            Util.CODEPATH();

            try {
                producer.send(queueOne_, sentMessage, PERSISTENT, -2 /* priority */, 10000, completionListener);
                throw new TestException("JMSException not thrown");
            } catch (JMSException e) {
                // Expected Exception.
                Util.TRACE(e);
            }

        } finally {
            clearQueue(queueOne_, methodName(), 0);
        }
        reportSuccess();
    }

  @ClientTest
  public void testJMS1DeliveryMode() throws JMSException, InterruptedException, TestException {
    
        try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
            MessageProducer producer = session.createProducer(queueOne_);
            MessageConsumer consumer = session.createConsumer(queueOne_);
            BasicCompletionListener completionListener = new BasicCompletionListener();

            // Good delivery modes, will send messages.
            try {
                TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date() + " DeliveryMode:NON_PERSISTENT");
                producer.send(sentMessage, NON_PERSISTENT, 0, 100000, completionListener);
            } catch (JMSException jmsException) {
                throw new TestException("Unexpected exception", jmsException);
            }

            try {
                TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date() + " DeliveryMode:PERSISTENT");
                producer.send(sentMessage, PERSISTENT, 0, 100000, completionListener);
            } catch (JMSException jmsException) {
                throw new TestException("Unexpected exception", jmsException);
            }

            if (!completionListener.waitFor(2, 0))
                throw new TestException("Expected completion notifications (2,0)a not found completionLister.formattedState:"+completionListener.formattedState());
           
            // Bad delivery mode, will not send messages.
            try {
                TextMessage sentMessage = session.createTextMessage(methodName() + " at " + new Date() + " DeliveryMode:9");
                producer.send(sentMessage, 9 /* bad mode */, 0, 10000, completionListener);
                throw new TestException("Send message with bad delivery mode\nsentMessage:"+sentMessage);
            } catch (JMSException e) {
                // Expected Exception.
            }
           
            if (!completionListener.waitFor(2, 0))
                throw new TestException("Expected completion notifications (2,0)b not found completionLister.formattedState:"+completionListener.formattedState());
           
        } finally {
            clearQueue(queueOne_, methodName(), 2);
        }

        reportSuccess();
  }

  @ClientTest
  public void testJMS1NullEmptyMessage() throws JMSException, InterruptedException, TestException {

      try (QueueSession session = queueConnection_.createQueueSession(false, AUTO_ACKNOWLEDGE)) {
          MessageConsumer consumer = session.createConsumer(queueOne_);
          MessageProducer producer = session.createProducer(queueOne_);
          TextMessage sentMessage = session.createTextMessage("");
          BasicCompletionListener completionListener = new BasicCompletionListener();
          producer.send(sentMessage, completionListener);

          if (!completionListener.waitFor(1, 0))
              throw new TestException("Expected completion notification (1,0)a not found completionLister.formattedState:" + completionListener.formattedState());

          Message receivedMessage = consumer.receive(WAIT_TIME);
          if (receivedMessage == null)
              throw new TestException("No message received, sent:"+sentMessage);
          Util.TRACE(receivedMessage);
          String receivedMessageText = receivedMessage.getBody(String.class);         
          if (!receivedMessageText.equals(""))
              throw new TestException("Wrong messageReceived receivedMessage:"+receivedMessage+"\n sentMessage:"+sentMessage);
                    
          try {
              producer.send(null, completionListener);
              throw new TestException("Unexpected sucecess sending null message.");
          } catch (MessageFormatException e) {
              // Expected exception.
          }
          
          if (!completionListener.waitFor(1, 0))
              throw new TestException("Expected completion notification (1,0)b not found completionLister.formattedState:" + completionListener.formattedState());

      } finally {
          clearQueue(queueOne_, "", 0);
      }
      reportSuccess();
  }

  // Original version of the clearQueue() method. Remove when no longer used.
  public void clearQueue(Queue queue, String messagePrefix, long numberExpected) throws JMSException, TestException {
	  clearQueue(queueConnection_, queue, messagePrefix, numberExpected);
  }

	  
  public void clearQueue(Connection connection, Queue queue, String messagePrefix, long numberExpected) throws JMSException, TestException {
      Util.TRACE_ENTRY(new Object[] {queue, messagePrefix, numberExpected});

      long numberCleared = 0;
      try (Session session = connection.createSession(false, AUTO_ACKNOWLEDGE)) {
          MessageConsumer consumer = session.createConsumer(queue);
         
          for (TextMessage message = (TextMessage) consumer.receiveNoWait(); message != null; message = (TextMessage) consumer.receiveNoWait()) {
              numberCleared++;
              if (numberCleared > numberExpected)
                 Util.LOG("Cleared unexpected Message:" + message);
              else if (!message.getText().startsWith(messagePrefix)) 
                 Util.LOG("Cleared incorrect Message:" + message);
              else
                 Util.TRACE("Cleared expected Message:" + message);         
          }
      }

      Util.TRACE_EXIT("numberCleared=" + numberCleared);
      if (numberCleared > numberExpected)
          throw new TestException("Cleared messages, numberCleared:" + numberCleared);
  }

}
