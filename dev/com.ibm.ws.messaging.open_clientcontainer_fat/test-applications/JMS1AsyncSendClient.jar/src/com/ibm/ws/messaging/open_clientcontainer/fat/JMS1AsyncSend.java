/* ============================================================================
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 * ============================================================================
 */
package com.ibm.ws.messaging.open_clientcontainer.fat;

import java.util.Properties;
import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
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
import javax.jms.ConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;
import javax.naming.InitialContext;
import javax.naming.Context;

public class JMS1AsyncSend extends ClientMain {
  public static void main(String[] args) {
    new JMS1AsyncSend().run();
  }

  private QueueConnectionFactory         queueConnectionFactory_ = null;
  private QueueConnection                queueConnection_ = null;
  private Connection                     connection_ = null;
  private Queue                          queueOne_ = null;
  private Queue                          depthLimitedQueue_ = null;
  private BasicCompletionListener        completionListener_ = null;
  private MessageOrderCompletionListener messageOrderListener_ = null;

  @Override
  protected void setup() throws Exception {
    Util.TRACE_ENTRY();

    Properties env = new Properties();
    env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
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
    completionListener_ = new BasicCompletionListener();
    Util.CODEPATH();
    messageOrderListener_ = new MessageOrderCompletionListener();

    Util.TRACE_EXIT();
  }

  @ClientTest
  public void testJMS1AsyncSend() throws JMSException {
    final String messageText = "testJMS1AsyncSend";
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    TextMessage textMessage = session.createTextMessage("testJMS1AsyncSend");
    textMessage.setText(messageText);

    MessageProducer producer = session.createProducer(queueOne_);
    MessageConsumer consumer = session.createConsumer(queueOne_);
    producer.send(textMessage, completionListener_);

    boolean conditionMet = completionListener_.waitFor(1, 0);

    TextMessage messageReceived = (TextMessage)consumer.receive(WAIT_TIME);
    if (null==messageReceived) {
      Util.TRACE("No message received!");
    } else {
      Util.TRACE("Message text \"" + messageReceived.getText() + "\" received.");
    }

    if (!messageReceived.getText().equals(messageText)) {
      reportFailure("Incorrect message text \""+messageReceived.getText()+"\" received.");
    } else if (!conditionMet) {
      reportFailure("Message completion notification not received.");
    } else {
      reportSuccess();
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  // Case where the acknowledgement is not received, the JMS provider would notify the application by invoking the
  // CompletionListener's onException method.

  @ClientTest
  public void testJMS1ExceptionMessageThreshhold() throws JMSException {
    clearQueue(depthLimitedQueue_);
    completionListener_.reset();

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageProducer producer = session.createProducer(depthLimitedQueue_);
    TextMessage message = session.createTextMessage("testJMS1ExceptionMessageThreshhold");

    for (int i = 0; i < 6; i++) producer.send(message, completionListener_);

    if (completionListener_.waitFor(5,1)) {
      reportSuccess();
    } else {
      reportFailure("Expected completion & exception notification not recevied.");
    }
    clearQueue(depthLimitedQueue_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1AsyncSendException() throws JMSException {
    Util.setLevel(Level.FINEST);
    boolean exceptionCaught = false;
    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

    try {
      MessageProducer producer = session.createProducer(null);
      producer.send(queueOne_, null, completionListener_);
    } catch (MessageFormatException e) {
      exceptionCaught = true;
    }
    if (exceptionCaught == true && completionListener_.completionCount_ == 0 && completionListener_.exceptionCount_ == 0) {
      reportSuccess();
    } else {
      reportFailure("Expected exception not raised without notification.");
    }
    Util.setLevel(Level.INFO);
  }

  @ClientTest
  public void testJMS1MessageOrderingSingleProducer() throws JMSException, InterruptedException {
    messageOrderListener_.reset();
    clearQueue(queueOne_);
    messageOrderListener_.setExpectedMessageCount(5);
    int outOfOrderCount = 0;

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageConsumer consumer = session.createConsumer(queueOne_);
    MessageProducer producer = session.createProducer(null);

    for (int i = 0; i < messageOrderListener_.getExpectedMessageCount(); i++) {
      Message message = session.createMessage();
      message.setIntProperty("Message_Order",i);
      producer.send(queueOne_, message, DeliveryMode.PERSISTENT, 0, 10000, messageOrderListener_);
      int messageOrder = consumer.receive(WAIT_TIME).getIntProperty("Message_Order");
      if (i!=messageOrder) outOfOrderCount++;
      Util.TRACE("Message_Order for "+i+" = "+messageOrder);
    }

    boolean conditionMet = messageOrderListener_.waitFor(messageOrderListener_.getExpectedMessageCount(), 0);

    Util.TRACE("outOfOrderCount="+outOfOrderCount
               +",completionCount="+messageOrderListener_.completionCount_
               +",exceptionCount="+messageOrderListener_.exceptionCount_
               );
    int messageOrderCount = messageOrderListener_.getMessageOrderCount();
    Util.TRACE("messageOrderCount=" + messageOrderCount);

    if (outOfOrderCount == 0 && conditionMet == true && messageOrderCount == messageOrderListener_.getExpectedMessageCount()) {
      reportSuccess();
    } else {
      reportFailure("Failed to receive messages in order.");
    }
    clearQueue(queueOne_);
    messageOrderListener_.reset();
  }

  @ClientTest
  public void testJMS1MessageOrderingMultipleProducers() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    messageOrderListener_.reset();
    messageOrderListener_.setExpectedMessageCount(5);
    int outOfOrderCount = 0;

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

    MessageProducer producer[] = {  session.createProducer(queueOne_)
                                   ,session.createProducer(queueOne_)
                                   ,session.createProducer(queueOne_)
                                   ,session.createProducer(queueOne_)
                                   ,session.createProducer(queueOne_)
                                 };
    MessageConsumer consumer = session.createConsumer(queueOne_);

    for (int i=0;messageOrderListener_.getExpectedMessageCount()>i;++i) {
      Message message = session.createMessage();
      message.setIntProperty("Message_Order",i);
      producer[i].send(message, messageOrderListener_);
      int messageOrder = consumer.receive(WAIT_TIME).getIntProperty("Message_Order");
      if (i!=messageOrder) outOfOrderCount++;
      Util.TRACE("Message_Order for "+i+" = "+messageOrder);
    }

    boolean conditionMet = messageOrderListener_.waitFor(messageOrderListener_.getExpectedMessageCount(), 0);
    int messageOrderCount = messageOrderListener_.getMessageOrderCount();
    Util.TRACE("outOfOrderCount="+outOfOrderCount
              +",completionCount="+messageOrderListener_.completionCount_
              +",exceptionCount="+messageOrderListener_.exceptionCount_
              +",messageOrderCount=" + messageOrderCount
              );
    if (outOfOrderCount == 0 && conditionMet == true && messageOrderCount == messageOrderListener_.getExpectedMessageCount()) {
      reportSuccess();
    } else {
      reportFailure("Failed to receive messages in order.");
    }
    clearQueue(queueOne_);
    messageOrderListener_.reset();
  }

  // From "Java Message Service" Version 2.0 revision a (March 2015):
  //
  //   6.2.9. Message order
  //
  //   [...]
  //
  //   JMS defines that messages sent by a session to a destination must be received in the order in which they were sent[...]
  //
  //   JMS does not define order of message receipt across destinations or across a destination's messages sent from multiple
  //   sessions. This aspect of a session's input message stream order is timing-dependent. It is not under application control
  //
  // So all we can test for with multiple sessions is ordering of messages from each session.

  @ClientTest
  public void testJMS1MessageOrderingMultipleSessions() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageConsumer consumer = session.createConsumer(queueOne_);
    QueueSession producerSession[] = { queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE)
                                      ,queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE)
                                      ,queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE)
                                     };
    MessageProducer producer[] = { producerSession[0].createProducer(queueOne_)
                                  ,producerSession[1].createProducer(queueOne_)
                                  ,producerSession[2].createProducer(queueOne_)
                                 };
    int order[] = { -1,-1,-1 };
    int outOfOrderCount = 0;

    for (int i=0;15>i;++i) {
      if (5==i||8==i||14==i||1==i) continue;   // unequal number per session
      Message message = session.createTextMessage("testJMS1MessageOrderingMultipleSessions");
      message.setIntProperty("Session_Number",i%3);
      message.setIntProperty("Message_Order",i);
      producer[i%3].send(message, DeliveryMode.PERSISTENT, 0, 10000, completionListener_);
    }

    boolean conditionMet = completionListener_.waitFor(11, 0);

    for(int i=0;11>i;++i) {
      Message message = consumer.receive(WAIT_TIME);
      if (null==message) {
        Util.TRACE("Failed to receive message "+i);
        ++outOfOrderCount;      // force failure
        break;
      }
      int sessionNumber = message.getIntProperty("Session_Number");
      int messageOrder = message.getIntProperty("Message_Order");
      Util.TRACE("i="+i+",Session_Number="+sessionNumber+",Message_Order="+messageOrder);
      if (order[sessionNumber]>=messageOrder) outOfOrderCount++;
      order[sessionNumber] = messageOrder;
    }

    Util.TRACE("outOfOrderCount="+outOfOrderCount
              +",completionCount="+completionListener_.completionCount_
              +",exceptionCount="+completionListener_.exceptionCount_
              );
    if (outOfOrderCount == 0 && conditionMet == true ) {
      reportSuccess();
    } else {
      reportFailure("Failed to receive messages in order.");
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1CloseSession() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueConnection connection = queueConnectionFactory_.createQueueConnection();
    QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    String text = "testJMS1CloseSession.";
    while (13000 < text.length()) text += "testJMS1CloseSession.";
    TextMessage message = session.createTextMessage(text);
    MessageProducer producer = session.createProducer(queueOne_);

    Util.CODEPATH();
    for (int i = 0; i < 100; i++) producer.send(message, completionListener_);
    Util.CODEPATH();
    session.close();
    Util.CODEPATH();

    if (completionListener_.waitFor(100, 0)) {
      reportSuccess();
    } else {
      reportFailure("Failed to receive expected completion notification.");
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1CloseConnection() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueConnection connection = queueConnectionFactory_.createQueueConnection();
    QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    String text = "testJMS1CloseConnection.";
    while (13000 < text.length()) text += "testJMS1CloseConnection.";
    TextMessage message = session.createTextMessage(text);
    MessageProducer producer = session.createProducer(queueOne_);

    Util.CODEPATH();
    for (int i = 0; i < 100; i++) producer.send(message, completionListener_);
    Util.CODEPATH();
    connection.close();
    Util.CODEPATH();

    if (completionListener_.waitFor(100, 0)) {
      reportSuccess();
    } else {
      reportFailure("Failed to receive expected completion notification.");
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1AsyncSendUnidentifiedProducerUnidentifiedDestination() throws JMSException, InterruptedException {
    completionListener_.reset();
    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    TextMessage textMessage = session.createTextMessage("testAsyncSendUnidentifiedProducerUnidentifiedDestination");
    MessageProducer producer = session.createProducer(null);
    boolean exceptionCaught = false;

    try {
      producer.send(null, textMessage, completionListener_);
    } catch (InvalidDestinationException e) {
      exceptionCaught = true;
    }
    Util.TRACE("exceptionCaught="+exceptionCaught
              +",completionCount="+completionListener_.completionCount_
              +",exceptionCount="+completionListener_.exceptionCount_
              );
    if (exceptionCaught == true && completionListener_.completionCount_ == 0 && completionListener_.exceptionCount_ == 0) {
      reportSuccess();
    } else {
      reportFailure("Expected exception not raised without notification.");
    }
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1AsyncSendNullListener() throws JMSException {
    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    TextMessage textMessage = session.createTextMessage("testAsyncSendNullListener");
    MessageProducer producer = session.createProducer(queueOne_);
    try {
      producer.send(textMessage, null);
      reportFailure("Expected exception not raise.");
    } catch (IllegalArgumentException e) {
      reportSuccess();
    }
  }

  @ClientTest
  public void testJMS1AsyncSendNoDestination() throws JMSException {
    completionListener_.reset();
    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    TextMessage textMessage = session.createTextMessage("testAsyncSend5ArgNoDestination");
    MessageProducer producer = session.createProducer(null);
    String result = "";

    try {
      producer.send(textMessage, completionListener_);
      result += ",2 argument send test failed";
    } catch (UnsupportedOperationException e) {
      Util.CODEPATH();
    }
    try {
      producer.send(null, textMessage, completionListener_);
      result += ",3 argument send test failed";
    } catch (InvalidDestinationException e) {
      Util.CODEPATH();
    }
    try {
      producer.send(textMessage, DeliveryMode.PERSISTENT, 0, 10000, completionListener_);
      result += ",5 argument send test failed";
    } catch (UnsupportedOperationException e) {
      Util.CODEPATH();
    }
    try {
      producer.send(null, textMessage, DeliveryMode.PERSISTENT, 0, 10000, completionListener_);
      result += ",6 argument send test failed";
    } catch (InvalidDestinationException e) {
      Util.CODEPATH();
    }

    if (0==result.length()) {
      reportSuccess();
    } else {
      reportFailure(result.substring(1));
    }
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1CompletionListener() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageProducer producer = session.createProducer(null);

    for (int i = 0; i < 5; i++) {
      TextMessage textMessage = session.createTextMessage("testJMS1CompletionListener:"+new java.util.Date());
      producer.send(depthLimitedQueue_, textMessage, completionListener_);
    }

    boolean completed = completionListener_.waitFor(5, 0);
    Util.TRACE("completed="+completed
              +"completionCount="+completionListener_.completionCount_
              +",exceptionCount="+completionListener_.exceptionCount_
              );

    TextMessage textMessage = session.createTextMessage("testJMS1CompletionListener:"+new java.util.Date());
    producer.send(depthLimitedQueue_, textMessage, completionListener_);

    boolean conditionMet = completionListener_.waitFor(5, 1);
    Util.TRACE("conditionMet="+conditionMet
              +"completionCount="+completionListener_.completionCount_
              +",exceptionCount="+completionListener_.exceptionCount_
              );

    if (completed && conditionMet) {
      reportSuccess();
    } else {
      reportFailure();
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1SessionInListener() throws Exception, JMSException, InterruptedException {
    try (SessionCompletionListener sessionListener = new SessionCompletionListener(queueConnection_.createQueueSession(true,0)
                                                                                  ,queueConnection_.createQueueSession(true,0)
                                                                                  ,queueOne_
                                                                                  )
        ) {
      clearQueue(queueOne_);

      MessageProducer producer = sessionListener.session_.createProducer(null);
      MessageConsumer consumer = sessionListener.session_.createConsumer(queueOne_);
      TextMessage textMessage = sessionListener.session_.createTextMessage("testJMS1SessionInListener");

      producer.send(queueOne_, textMessage, sessionListener);

      boolean conditionMet = sessionListener.waitFor(1, 0);

      sessionListener.session_.commit();

      Util.CODEPATH();
      QueueBrowser qb = sessionListener.session_.createBrowser(queueOne_);
      Enumeration e1 = qb.getEnumeration();

      int messageCount = 0;
      for (messageCount=0;e1.hasMoreElements();e1.nextElement()) messageCount++;

      Util.TRACE("messageCount="+messageCount
                +",completionCount="+sessionListener.completionCount_
                +",exceptionCount="+sessionListener.exceptionCount_
                +",exceptionOnClose="+sessionListener.exceptionOnClose_
                +",exceptionOnCommit="+sessionListener.exceptionOnCommit_
                +",exceptionOnRollback="+sessionListener.exceptionOnRollback_
                +",producerCreated="+sessionListener.producerCreated_
                +",exceptionOnProducerClose="+sessionListener.exceptionOnProducerClose_
                );

      if (conditionMet == true
          && messageCount == 1
          && sessionListener.exceptionOnUnrelatedClose_ == false
          && sessionListener.exceptionOnClose_ == true
          && sessionListener.exceptionOnCommit_ == true
          && sessionListener.exceptionOnRollback_ == true
          && sessionListener.producerCreated_ == true
          && sessionListener.exceptionOnProducerClose_ == true
         ) {
        reportSuccess();
      } else {
        reportFailure();
      }
      clearQueue(queueOne_);
    }
  }

  @ClientTest
  public void testJMS1TransactionAndListener() throws Exception, JMSException, InterruptedException {
    clearQueue(depthLimitedQueue_);
    QueueSession session = queueConnection_.createQueueSession(true,javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageProducer producer = session.createProducer(depthLimitedQueue_);
    TextMessage textMessage = session.createTextMessage("testJMS1TransactionAndListener");

    for (int i=0;5>i;++i) producer.send(textMessage, completionListener_);
    completionListener_.waitFor(5, 0);
    session.commit();
    Util.CODEPATH();

    // Because this is a locally transacted session this send will succeed and the exception will be raised on the subsequent
    // commit (onException must NOT be called)
    producer.send(textMessage, completionListener_);

    boolean conditionMet = completionListener_.waitFor(6, 0);

    boolean exceptionOnCommit = false;
    try {
      session.commit();
      Util.CODEPATH();
    } catch (JMSException e) {
      if (null!=e.getCause()
          &&"com.ibm.wsspi.sib.core.exception.SIRollbackException".equals(e.getCause().getClass().getName())
         ) {
        Throwable t = e.getCause();
        if (null!=t.getCause()
            &&"com.ibm.wsspi.sib.core.exception.SILimitExceededException".equals(t.getCause().getClass().getName())
           ) {
          Util.TRACE("commit failed with expected exception: "+t.getCause().getClass().getName());
          exceptionOnCommit = true;
        } else {
          throw e;
        }
      } else {
        throw e;
      }
    }

    Util.TRACE("completionCount="+completionListener_.completionCount_
              +",exceptionCount="+completionListener_.exceptionCount_
              );
    if (conditionMet == true && exceptionOnCommit == true) {
      reportSuccess();
    } else {
      reportFailure();
    }
    clearQueue(depthLimitedQueue_);
  }

  @ClientTest
  public void testJMS1TimeToLive() throws JMSException, InterruptedException {
    String result = "";
    int messageCount = -1;
    boolean exceptionCaught = false;

    clearQueue(queueOne_);
    completionListener_.reset();
    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageProducer producer = session.createProducer(null);
    MessageConsumer consumer = session.createConsumer(queueOne_);
    TextMessage textMessage = session.createTextMessage("testJMS1TimeToLive");

    producer.send(queueOne_, textMessage, DeliveryMode.NON_PERSISTENT, 0, 500, completionListener_);
    Util.CODEPATH();

    boolean conditionMet = completionListener_.waitFor(1, 0);  // ensure it is on the queue before we start waiting
    Thread.sleep(3000);                                        // wait plenty of time for it to expire
    Util.CODEPATH();

    try (QueueBrowser qb = session.createBrowser(queueOne_)) {
      Enumeration en = qb.getEnumeration();
      for (messageCount=0;en.hasMoreElements();en.nextElement()) messageCount++;
    }

    Message messageReceived = (Message) consumer.receiveNoWait();
    Util.TRACE("conditionMet="+conditionMet+",messageCount="+messageCount+",messageReceived="+messageReceived);

    if (messageCount != 0 || messageReceived != null || conditionMet == false ) {
      result += ",positive TTL test failed";
    }
    completionListener_.reset();

    // Try with negative timeToLive

    Util.CODEPATH();
    MessageProducer producer2 = session.createProducer(queueOne_);
    try {
      producer2.send(textMessage, DeliveryMode.NON_PERSISTENT, 0, -100, completionListener_);
    } catch (JMSException e) {
      exceptionCaught = true;
    }
    conditionMet = completionListener_.waitFor(100, 1, 0);   // wait for 100ms while queue remains empty

    Util.CODEPATH();
    messageCount = -1;
    try (QueueBrowser qb = session.createBrowser(queueOne_)) {
      Enumeration en = qb.getEnumeration();
      for (messageCount=0;en.hasMoreElements();en.nextElement()) messageCount++;
    }

    messageReceived = (Message)consumer.receiveNoWait();
    Util.TRACE("conditionMet="+conditionMet+",messageCount="+messageCount+",messageReceived="+messageReceived);

    if (exceptionCaught == false || messageCount != 0 || messageReceived != null || conditionMet == true) {
      result += ",negative TTL test failed";
    }
    completionListener_.reset();

    if (0==result.length()) {
      reportSuccess();
    } else {
      reportFailure(result.substring(1));
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1Priority() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    TextMessage textMessage = session.createTextMessage("testJMS1Priority");
    MessageConsumer consumer = session.createConsumer(queueOne_);
    MessageProducer producer = session.createProducer(null);

    for (int priority = 0; priority < 10; priority++) {
      producer.send(queueOne_, textMessage, DeliveryMode.PERSISTENT, priority, 10000, completionListener_);
    }

    completionListener_.waitFor(10, 0);
    Util.CODEPATH();

    QueueBrowser qb = session.createBrowser(queueOne_);
    Enumeration en = qb.getEnumeration();
    int messageNumber = 0;
    boolean priorityCorrect = true;
    for (;true==priorityCorrect&&en.hasMoreElements()&&10>messageNumber;++messageNumber) {
      TextMessage messageReceived = (TextMessage)consumer.receive(WAIT_TIME);
      Util.TRACE("messageNumber="+messageNumber+",messageReceived.getJMSPriority()="+messageReceived.getJMSPriority());
      priorityCorrect = (messageReceived.getJMSPriority() + messageNumber == 9);
    }

    if (messageNumber == 10 && priorityCorrect == true) {
      reportSuccess();
    } else {
      reportFailure();
    }
    completionListener_.reset();
    clearQueue(queueOne_);
  }

  @ClientTest
  public void testJMS1NegativePriority() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();
    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    TextMessage textMessage = session.createTextMessage("testJMS1NegativePriority");
    MessageProducer producer = session.createProducer(null);

    textMessage = session.createTextMessage();
    Util.CODEPATH();

    boolean exceptionCaught = false;
    try {
      producer.send(queueOne_, textMessage, DeliveryMode.PERSISTENT, -2 /* priority */, 10000, completionListener_);
    } catch (JMSException e) {
      exceptionCaught = true;
    }

    if (exceptionCaught == true) {
      reportSuccess();
    } else {
      reportFailure();
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1DeliveryMode() throws JMSException, InterruptedException {
    boolean exceptionOnNonPersistent = false;
    boolean exceptionOnPersistent = false;
    boolean exceptionOnBad = false;
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    TextMessage textMessage = session.createTextMessage("testJMS1DeliveryMode");
    MessageProducer producer = session.createProducer(queueOne_);
    MessageConsumer consumer = session.createConsumer(queueOne_);

    // good delivery modes
    try {
      exceptionOnNonPersistent = false;
      producer.send(textMessage, DeliveryMode.NON_PERSISTENT, 0, 100000, completionListener_);
    } catch (JMSException ignored) {
      exceptionOnNonPersistent = true;
    }

    try {
      exceptionOnPersistent = false;
      producer.send(textMessage, DeliveryMode.PERSISTENT, 0, 100000, completionListener_);
    } catch (JMSException ignored) {
      exceptionOnPersistent = true;
    }

    boolean conditionMet = completionListener_.waitFor(2,0);

    // bad delivery mode
    try {
      exceptionOnBad = false;
      producer.send(textMessage, 9 /* bad mode */, 0, 10000, completionListener_);
    } catch (JMSException e) {
      exceptionOnBad = true;
    }

    if (exceptionOnNonPersistent == false
        && exceptionOnPersistent == false
        && exceptionOnBad == true
        && conditionMet == true
       ) {
      reportSuccess();
    } else {
      reportFailure();
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS1NullEmptyMessage() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageConsumer consumer = session.createConsumer(queueOne_);
    MessageProducer producer = session.createProducer(queueOne_);
    TextMessage textMessage = session.createTextMessage("");
    producer.send(textMessage, completionListener_);

    boolean conditionMet = completionListener_.waitFor(1,0);

    String messageReceived = consumer.receive(WAIT_TIME).getBody(String.class);
    Util.TRACE("Message received length:" + messageReceived.length());

    boolean exceptionCaught = false;
    try {
      producer.send(null, completionListener_);
    } catch (MessageFormatException e) {
      exceptionCaught = true;
    }

    if (conditionMet == true && messageReceived.equals("") && exceptionCaught == true) {
      reportSuccess();
    } else {
      reportFailure();
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  public void clearQueue(Queue queue) throws JMSException {
    Util.TRACE_ENTRY();
    long numberCleared = -1;
    queueConnection_.start();
    QueueSession session = queueConnection_.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    MessageConsumer consumer = session.createConsumer(queue);
    Message message;
    Util.CODEPATH();
    do {
      message = consumer.receiveNoWait();
      numberCleared++;
    } while (message != null);
    session.close();
    Util.TRACE_EXIT("numberCleared="+numberCleared);
  }
}
