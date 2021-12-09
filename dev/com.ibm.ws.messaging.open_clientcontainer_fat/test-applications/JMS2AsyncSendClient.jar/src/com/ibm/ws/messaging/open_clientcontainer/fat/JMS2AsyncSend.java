/* ============================================================================
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.Enumeration;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageNotWriteableException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Context;

public class JMS2AsyncSend extends ClientMain {
  public static void main(String[] args) {
    new JMS2AsyncSend().run();
  }

  private QueueConnectionFactory          queueConnectionFactory_ = null;
  private ConnectionFactory               connectionFactory_ = null;
  private Queue                           queueOne_ = null;
  private Queue                           depthLimitedQueue_ = null;
  private BasicCompletionListener         completionListener_ = null;
  private MessageOrderCompletionListener  messageOrderListener_ = null;

  @Override
  protected void setup() throws Exception {
    Util.TRACE_ENTRY();

    Properties env = new Properties();
    env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
    InitialContext jndi = new InitialContext(env);
    queueConnectionFactory_ = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
    connectionFactory_ = (ConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
    queueOne_ = (Queue) jndi.lookup("java:comp/env/jndi_QUEUE_ONE");
    depthLimitedQueue_ = (Queue) jndi.lookup("java:comp/env/jndi_DEPTH_LIMITED_QUEUE");

    completionListener_ = new BasicCompletionListener();
    messageOrderListener_ = new MessageOrderCompletionListener();

    Util.TRACE_EXIT();
  }

  @ClientTest
  public void testJMS2NoAsync() throws JMSException {
    completionListener_.reset();
    clearQueue(queueOne_);

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      JMSProducer producer = context.createProducer();
      JMSConsumer consumer = context.createConsumer(queueOne_);

      Util.CODEPATH();
      producer.setAsync(null);
      producer.send(queueOne_, "testJMS2NoAsync");
      Util.CODEPATH();

      String messageReceived = consumer.receiveBody(String.class, WAIT_TIME);
      Util.TRACE("message=" + messageReceived);

      if (null != messageReceived
          && messageReceived.equals("testJMS2NoAsync")
          && completionListener_.completionCount_ == 0
          && completionListener_.exceptionCount_ == 0
         ) {
        reportSuccess();
      } else {
        reportFailure();
      }

      Util.TRACE("clearing queue");
      clearQueue(queueOne_);
    }
  }

  @ClientTest
  public void testJMS2SetAsync() throws JMSException, InterruptedException {
    completionListener_.reset();
    clearQueue(queueOne_);

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      JMSProducer producer = context.createProducer();
      JMSConsumer consumer = context.createConsumer(queueOne_);
      producer.setAsync(completionListener_);

      producer.send(queueOne_, "testJMS2SetAsync");

      boolean conditionMet = completionListener_.waitFor(1, 0);

      Util.TRACE("message sent");

      String messageReceived = consumer.receiveBody(String.class, WAIT_TIME);

      Util.TRACE("completionCount="+completionListener_.completionCount_
                +",exceptionCount="+completionListener_.exceptionCount_
                +"message="+messageReceived
                );

      if (null!=messageReceived && messageReceived.equals("testJMS2SetAsync") && conditionMet == true) {
        reportSuccess();
      } else {
        reportFailure();
      }
      clearQueue(queueOne_);
    }
  }

  @ClientTest
  public void testJMS2GetAsync() {
    try (JMSContext context = queueConnectionFactory_.createContext()) {
      JMSProducer producer = context.createProducer();
      producer.setAsync(completionListener_);
      boolean success = (producer.getAsync() == completionListener_);
      if (success) {
        producer.setAsync(null);
        success = (producer.getAsync() == null);
      }
      if (success) {
        reportSuccess();
      } else {
        reportFailure();
      }
    }
  }

  @ClientTest
  public void testJMS2CompletionListener() throws JMSException, InterruptedException {
    clearQueue(depthLimitedQueue_);
    completionListener_.reset();

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      JMSProducer producer = context.createProducer();
      producer.setAsync(completionListener_);

      for (int i = 0; i < 5; i++) {
        TextMessage textMessage = context.createTextMessage("testJMS2CompletionListener:"+new java.util.Date());
        producer.send(depthLimitedQueue_, textMessage);
      }

      boolean completed = completionListener_.waitFor(5, 0);
      Util.TRACE("completed="+completed
              +"completionCount="+completionListener_.completionCount_
              +",exceptionCount="+completionListener_.exceptionCount_
              );

      TextMessage textMessage = context.createTextMessage("testJMS2CompletionListener:"+new java.util.Date());
      producer.send(depthLimitedQueue_, textMessage);

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
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS2ExceptionUndefinedQueue() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      Queue queue = context.createQueue("QUEUE_DOES_NOT_EXIST");
      JMSProducer producer = context.createProducer();

      producer.setAsync(completionListener_);

      Message message = context.createMessage();
      message.setIntProperty("Message_Order", 1);
      producer.send(queue, message);

      if (completionListener_.waitFor(0, 1)) {
        reportSuccess();
      } else {
        reportFailure();
      }
    }
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS2MessageOrderingSingleProducer() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    messageOrderListener_.reset();
    messageOrderListener_.setExpectedMessageCount(5);
    int i = 0;
    int outOfOrderCount = 0;

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      JMSProducer producer = context.createProducer();
      JMSConsumer consumer = context.createConsumer(queueOne_);
      producer.setAsync(messageOrderListener_);
      try {
        for (i = 0; i < messageOrderListener_.getExpectedMessageCount(); i++) {
          Message message = context.createMessage();
          message.setIntProperty("Message_Order", i);
          producer.send(queueOne_, message);
          int messageOrder = consumer.receive(WAIT_TIME).getIntProperty("Message_Order");
          if (i!=messageOrder) outOfOrderCount++;
          Util.TRACE("Message_Order for "+i+" = "+messageOrder);
        }
      } catch (Exception e) {
        Util.LOG(e);
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
        reportFailure();
      }
      clearQueue(queueOne_);
    }
    messageOrderListener_.reset();
  }

  @ClientTest
  public void testJMS2MessageOrderingMultipleProducers() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    messageOrderListener_.reset();
    messageOrderListener_.setExpectedMessageCount(5);
    int i = 0;
    int outOfOrderCount = 0;

    try (JMSContext context = queueConnectionFactory_.createContext()) {

      JMSProducer msgProd[] = {  context.createProducer()
                                ,context.createProducer()
                                ,context.createProducer()
                                ,context.createProducer()
                                ,context.createProducer()
                              };
      for (i=0;5>i;++i) msgProd[i].setAsync(messageOrderListener_);

      JMSConsumer consumer = context.createConsumer(queueOne_);

      for (i=0;5>i;++i) {
        Message message = context.createMessage();
        message.setIntProperty("Message_Order", i);
        msgProd[i].send(queueOne_,message);
      }

      for (i=0;5>i;++i) {
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
        reportFailure();
      }
      clearQueue(queueOne_);
    }
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
  // So all we can test for with multiple sessions (contexts) is ordering of messages from each session.

  @ClientTest
  public void testJMS2MessageOrderingMultipleContexts() throws JMSException, InterruptedException {
    Util.setLevel(Level.FINEST);
    clearQueue(queueOne_);
    completionListener_.reset();
    int outOfOrderCount = 0;

    try (JMSContext context  = queueConnectionFactory_.createContext();
         JMSContext context1 = queueConnectionFactory_.createContext();
         JMSContext context2 = queueConnectionFactory_.createContext();
         JMSContext context3 = queueConnectionFactory_.createContext();
        ) {
      JMSProducer producer[] = { context1.createProducer()
                                ,context2.createProducer()
                                ,context3.createProducer()
                               };
      int order[] = { -1,-1,-1 };
      for (int i=0;3>i;++i) {
        producer[i].setAsync(completionListener_);
        producer[i].setDeliveryMode(javax.jms.DeliveryMode.PERSISTENT);
        producer[i].setTimeToLive(10000);
      }
      JMSConsumer consumer = context.createConsumer(queueOne_);

      for (int i=0;15>i;++i) {
        if (5==i||8==i||14==i||1==i) continue;   // unequal number per session
        Message message = context.createTextMessage("testJMS2MessageOrderingMultipleContexts");
        message.setIntProperty("Session_Number",i%3);
        message.setIntProperty("Message_Order",i);
        producer[i%3].send(queueOne_, message);
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
    }
    clearQueue(queueOne_);
    completionListener_.reset();
    Util.setLevel(Level.INFO);
  }

  @ClientTest
  public void testJMS2MessageOrderingSyncAsyncMix() throws Exception {
    clearQueue(queueOne_);
    messageOrderListener_.reset();
    messageOrderListener_.setExpectedMessageCount(5);
    int i = 0;
    int outOfOrderCount = 0;

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      JMSProducer producer = context.createProducer();
      producer.setAsync(messageOrderListener_);
      JMSConsumer consumer = context.createConsumer(queueOne_);

      Util.CODEPATH();
      for (i = 0; i < messageOrderListener_.getExpectedMessageCount(); i++) {
        Message message = context.createMessage();
        message.setIntProperty("Message_Order", i);
        producer.send(queueOne_, message);
        int messageOrder = consumer.receive(WAIT_TIME).getIntProperty("Message_Order");
        if (i!=messageOrder) outOfOrderCount++;
        Util.TRACE("Message_Order for "+i+" = "+messageOrder);
      }
      Util.CODEPATH();

      producer.setAsync(null);
      for (i = 0; i < 5; i++) {
        Message msg = context.createMessage();
        msg.setStringProperty("Feature", "AsyncSend");
        msg.setIntProperty("Order", i);
        producer.send(queueOne_, msg);
        Message rcvd = consumer.receive(WAIT_TIME);
        int order = rcvd.getIntProperty("Order");
        String str = rcvd.getStringProperty("Feature");
        if (!(str.equals("AsyncSend") && order == i)) outOfOrderCount++;
      }
      Util.CODEPATH();

      boolean conditionMet = messageOrderListener_.waitFor(messageOrderListener_.getExpectedMessageCount(),0);
      int messageOrderCount = messageOrderListener_.getMessageOrderCount();
      Util.TRACE("outOfOrderCount="+outOfOrderCount
                +",completionCount="+messageOrderListener_.completionCount_
                +",exceptionCount="+messageOrderListener_.exceptionCount_
                +",messageOrderCount="+messageOrderCount
                );
      if (outOfOrderCount == 0 && conditionMet == true && messageOrderCount == messageOrderListener_.getExpectedMessageCount() ) {
        reportSuccess();
      } else {
        reportFailure();
      }

      clearQueue(queueOne_);
      messageOrderListener_.reset();
    }
  }

  @ClientTest
  public void testJMS2TransactionAndListener() throws JMSException, InterruptedException, Exception {
    clearQueue(depthLimitedQueue_);

    JMSContext context = queueConnectionFactory_.createContext(Session.SESSION_TRANSACTED);
    JMSProducer producer = context.createProducer();
    producer.setAsync(completionListener_);
    TextMessage textMessage = context.createTextMessage("testJMS2TransactionAndListener");

    for (int i=0;5>i;++i) producer.send(depthLimitedQueue_, textMessage);
    completionListener_.waitFor(5, 0);
    context.commit();
    Util.CODEPATH();

    // Because this is a locally transacted session this send will succeed and the exception will be raised on the subsequent
    // commit (onException must NOT be called)
    producer.send(depthLimitedQueue_, textMessage);

    boolean conditionMet = completionListener_.waitFor(6, 0);

    boolean exceptionOnCommit = false;
    try {
      context.commit();
    } catch (javax.jms.JMSRuntimeException e) {
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
    if (conditionMet == true && exceptionOnCommit == true ) {
      reportSuccess();
    } else {
      reportFailure();
    }
    clearQueue(depthLimitedQueue_);
  }

  @ClientTest
  public void testJMS2Close() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    try (JMSContext ctxLocal = queueConnectionFactory_.createContext()) {  // auto close
      String text = "testJMS2Close.";
      while (13000 < text.length()) text += "testJMS2Close.";
      TextMessage msg = ctxLocal.createTextMessage(text);
      JMSProducer producer = ctxLocal.createProducer();
      producer.setAsync(completionListener_);

      Util.CODEPATH();
      for (int i = 0; i < 100; i++) {
        producer.send(queueOne_, msg);
      }
      Util.CODEPATH();
    }
    if (completionListener_.waitFor(100, 0)) {
      reportSuccess();
    } else {
      reportFailure();
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS2Commit() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    try (JMSContext transactedContext = queueConnectionFactory_.createContext(Session.SESSION_TRANSACTED)) {
      JMSProducer messageProducerTrans = transactedContext.createProducer();
      messageProducerTrans.setAsync(completionListener_);
      for (int i = 0; i < 100; i++) {
        Message message = transactedContext.createMessage();
        message.setIntProperty("Message_Order", i);
        messageProducerTrans.send(queueOne_, message);
      }
      Util.CODEPATH();
      transactedContext.commit();
      Util.CODEPATH();

      JMSConsumer messageConsumerTrans = transactedContext.createConsumer(queueOne_);
      for (int i = 99; i > 0; i--) {
        Util.TRACE("Message " + i + ":"+messageConsumerTrans.receive(WAIT_TIME));
      }

      if (completionListener_.waitFor(100, 0)) {
        reportSuccess();
      } else {
        reportFailure();
      }
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS2RollBack() throws JMSException, InterruptedException {
    clearQueue(queueOne_);
    completionListener_.reset();

    try (JMSContext transactedContext = queueConnectionFactory_.createContext(Session.SESSION_TRANSACTED)) {
      Message message = transactedContext.createTextMessage("testJMS2RollBack");
      JMSProducer transactedProducer = transactedContext.createProducer();
      transactedProducer.setAsync(completionListener_);

      Util.CODEPATH();
      for (int i = 0; i < 100; i++) {
        transactedProducer.send(queueOne_, message);
      }

      boolean conditionMet = completionListener_.waitFor(100, 0);
      Util.TRACE("completionCount="+completionListener_.completionCount_+",exceptionCount_="+completionListener_.exceptionCount_);

      transactedContext.rollback();
      Util.CODEPATH();

      QueueBrowser qb = transactedContext.createBrowser(queueOne_);
      Enumeration en = qb.getEnumeration();

      int messageCount = 0;
      for (messageCount=0;en.hasMoreElements();en.nextElement()) messageCount++;
      Util.TRACE("messageCount="+messageCount);

      if (conditionMet == true && messageCount == 0) {
        reportSuccess();
      } else {
        reportFailure();
      }
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  interface MessageTypeCreator { public String typeName(); public Message create(); }

  @ClientTest
  public void testJMS2MessageTypes() throws JMSException, InterruptedException {
    String result = "";
    clearQueue(queueOne_);

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      MessageTypeCreator[] typeCreator = new MessageTypeCreator[] {
                                      new MessageTypeCreator() { public String typeName() { return "BytesMessage"; }
                                                                 public Message create() { return context.createBytesMessage(); }
                                                               }
                                     ,new MessageTypeCreator() { public String typeName() { return "MapMessage"; }
                                                                 public Message create() { return context.createMapMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "ObjectMessage"; }
                                                                 public Message create() { return context.createObjectMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "StreamMessage"; }
                                                                 public Message create() { return context.createStreamMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "TextMessage"; }
                                                                 public Message create() { return context.createTextMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "Message"; }
                                                                 public Message create() { return context.createMessage(); }
                                                              }
                                                                  };
      JMSConsumer consumer = context.createConsumer(queueOne_);
      JMSProducer producer = context.createProducer();
      producer.setAsync(completionListener_);

      for (int i=0;typeCreator.length>i;++i) {
        Util.TRACE("Message type: "+typeCreator[i].typeName());
        completionListener_.reset();
        Message message = typeCreator[i].create();
        message.setBooleanProperty("Value", true);
        message.setJMSCorrelationID("CORREL");
        producer.send(queueOne_,message);

        completionListener_.waitFor(1, 0);

        message.setBooleanProperty("Value", false);

        Message messageReceived = consumer.receive(WAIT_TIME);
        Util.CODEPATH();
        boolean propertyValSet = (null!=messageReceived
                                  && messageReceived.getBooleanProperty("Value") == true
                                  && messageReceived.getJMSCorrelationID().equals("CORREL")
                                 );

        producer.send(queueOne_,message);

        boolean conditionMet = completionListener_.waitFor(2, 0);

        messageReceived = consumer.receive(WAIT_TIME);
        Util.CODEPATH();
        boolean propertyValSetAgain = (messageReceived.getBooleanProperty("Value") == false);


        if (propertyValSet == false || propertyValSetAgain == false || conditionMet == false) {
          result += ","+typeCreator[i].typeName()+" failed";
        }
      }
    }

    if (0==result.length()) {
      reportSuccess();
    } else {
      reportFailure(result.substring(1));
    }
    clearQueue(queueOne_);
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS2MessageTypesOrder() throws JMSException, InterruptedException {
    String result = "";
    clearQueue(queueOne_);

    try (JMSContext context = queueConnectionFactory_.createContext()) {
      MessageTypeCreator[] typeCreator = new MessageTypeCreator[] {
                                      new MessageTypeCreator() { public String typeName() { return "BytesMessage"; }
                                                                 public Message create() { return context.createBytesMessage(); }
                                                               }
                                     ,new MessageTypeCreator() { public String typeName() { return "MapMessage"; }
                                                                 public Message create() { return context.createMapMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "ObjectMessage"; }
                                                                 public Message create() { return context.createObjectMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "StreamMessage"; }
                                                                 public Message create() { return context.createStreamMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "TextMessage"; }
                                                                 public Message create() { return context.createTextMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "Message"; }
                                                                 public Message create() { return context.createMessage(); }
                                                              }
                                                                  };
      for (int i=0;typeCreator.length>i;++i) {
        Util.TRACE("Message type: "+typeCreator[i].typeName());
        int exceptionsCaught = 0;
        JMSProducer producer = context.createProducer();
        // Create a separate MessageOrderCompletionListener object for each iteration of the loop so that if we time out waiting
        // for one iteration the remaining callbacks don't interfere with the subsequent loop iterations.
        MessageOrderCompletionListener messageOrderListener = new MessageOrderCompletionListener();
        producer.setAsync(messageOrderListener);
        messageOrderListener.setExpectedMessageCount(30);
        Util.CODEPATH();

        for (int order = 0; order < messageOrderListener.getExpectedMessageCount(); order++) {
          Message message = typeCreator[i].create();
          message.setBooleanProperty("Value", true);
          message.setJMSCorrelationID("CORREL");
          message.setIntProperty("Message_Order",order);
          producer.send(queueOne_,message);

          if (2==order||(messageOrderListener.getExpectedMessageCount()-2)==order) {
            try {
              boolean value = message.getBooleanProperty("Value");
              String cid = message.getJMSCorrelationID();
              Util.TRACE("[Should not see this] Message "+order+": "+value+","+cid);
            } catch (JMSException e) {
              Util.TRACE("Expected JMSException raised for message order "+order+": "+e.getClass().getName()+" "+e.getMessage());
              exceptionsCaught++;
            }
          }
        }
        // Testing indicates the default (10s) is too short a wait period to reliably process a lot of messages so we give a maximum
        // of 30s here to accomodate this.  30s being arbitrarily chosen so as not to elongate the overall maximum test time too
        // greatly.
        boolean conditionMet = messageOrderListener.waitFor(30000,messageOrderListener.getExpectedMessageCount(),0);
        int messageOrderCount = messageOrderListener.getMessageOrderCount();
        Util.TRACE("exceptionsCaught="+exceptionsCaught
                  +",completionCount="+messageOrderListener.completionCount_
                  +",exceptionCount="+messageOrderListener.exceptionCount_
                  +",conditionMet="+conditionMet
                  +",messageOrderCount="+messageOrderCount
                  );
        if (conditionMet == false || exceptionsCaught != 2 || messageOrderCount != messageOrderListener.getExpectedMessageCount()) {
          result += ","+typeCreator[i].typeName()+" failed (conditionMet="+conditionMet+"["+messageOrderListener.completionCount_
                   +","+messageOrderListener.exceptionCount_+"]"
                   +",exceptionsCaught="+exceptionsCaught+",messageOrderCount="+messageOrderCount
                  ;
          Util.CODEPATH();
        }
        clearQueue(queueOne_);
      }
    }

    if (0==result.length()) {
      reportSuccess();
    } else {
      // Note: If the failure message indicates less than the expected message count has been received (in [] after conditionMet)
      // then it is likely simply the time taken to process messages and we've timed out (in the waitFor() call) before all have
      // been processed.  This may happen if build/test machine is slow for some reason.
      reportFailure(result.substring(1));
    }
    clearQueue(queueOne_);
  }

  @ClientTest
  public void testJMS2InvalidDestination() throws JMSException {
    String result = "";

    try (JMSContext context = connectionFactory_.createContext()) {
      MessageTypeCreator[] typeCreator = new MessageTypeCreator[] {
                                      new MessageTypeCreator() { public String typeName() { return "BytesMessage"; }
                                                                 public Message create() { return context.createBytesMessage(); }
                                                               }
                                     ,new MessageTypeCreator() { public String typeName() { return "MapMessage"; }
                                                                 public Message create() { return context.createMapMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "ObjectMessage"; }
                                                                 public Message create() { return context.createObjectMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "StreamMessage"; }
                                                                 public Message create() { return context.createStreamMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "TextMessage"; }
                                                                 public Message create() { return context.createTextMessage(); }
                                                              }
                                     ,new MessageTypeCreator() { public String typeName() { return "Message"; }
                                                                 public Message create() { return context.createMessage(); }
                                                              }
                                                                  };
      JMSProducer producer = context.createProducer();
      producer.setAsync(completionListener_);

      for (int i=0;typeCreator.length>i;++i) {
        Util.TRACE("Message type: "+typeCreator[i].typeName());
        completionListener_.reset();
        Message message = typeCreator[i].create();
        boolean exceptionCaught = false;
        try {
          producer.send(null,message);
        } catch (InvalidDestinationRuntimeException ex) {
          exceptionCaught = true;
        }

        if (exceptionCaught == false || completionListener_.completionCount_ != 0 || completionListener_.exceptionCount_ != 0) {
          result += ","+typeCreator[i].typeName()+" failed";
          Util.CODEPATH();
        }
      }
    }
    if (0==result.length()) {
      reportSuccess();
    } else {
      reportFailure(result.substring(1));
    }
    completionListener_.reset();
  }

  @ClientTest
  public void testJMS2ContextInListener() throws JMSException, InterruptedException {
    try (ContextCompletionListener contextListener = new ContextCompletionListener(
                                                                   queueConnectionFactory_.createContext(Session.SESSION_TRANSACTED)
                                                                  ,queueConnectionFactory_.createContext(Session.SESSION_TRANSACTED)
                                                                                  )
        ) {
      clearQueue(queueOne_);

      Message message = contextListener.context_.createTextMessage("testJMS2ContextInListener");
      JMSProducer transactedProducer = contextListener.context_.createProducer();
      transactedProducer.setAsync(contextListener);

      transactedProducer.send(queueOne_, message);

      boolean conditionMet = contextListener.waitFor(1, 0);

      contextListener.context_.commit();

      QueueBrowser qb = contextListener.context_.createBrowser(queueOne_);
      Enumeration en = qb.getEnumeration();

      int messageCount = 0;
      for (messageCount=0;en.hasMoreElements();en.nextElement()) messageCount++;

      Util.TRACE("completionCount="+contextListener.completionCount_
                +",exceptionCount="+contextListener.exceptionCount_
                +",messageCount="+messageCount
                +",exceptionOnUnrelatedClose="+contextListener.exceptionOnUnrelatedClose_
                +",exceptionOnClose="+contextListener.exceptionOnClose_
                +",exceptionOnCommit="+contextListener.exceptionOnCommit_
                +",exceptionOnRollback="+contextListener.exceptionOnRollback_
                +",producerCreated="+contextListener.producerCreated_
                );

      if (   contextListener.exceptionOnUnrelatedClose_ == false
          && contextListener.exceptionOnClose_ == true
          && contextListener.exceptionOnCommit_ == true
          && contextListener.exceptionOnRollback_ == true
          && contextListener.producerCreated_ == true
          && conditionMet == true
          && messageCount == 1
         ) {
        reportSuccess();
      } else {
        reportFailure();
      }
      clearQueue(queueOne_);
    }
  }

  @ClientTest
  public void testJMS2DefaultConnectionFactory() throws Exception {
    boolean exceptionCaught = false;
    try {
      ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/DefaultJMSConnectionFactory");
    } catch (NameNotFoundException e) {
      Util.TRACE("Expected NameNotFoundException caught.");
      exceptionCaught = true;
    }

    if (exceptionCaught) {
      reportSuccess();
    } else {
      reportFailure();
    }
  }

  @ClientTest
  public void testJMS2DefaultConnectionFactoryVariation() throws Exception {
    boolean exceptionCaught = false;
    try {
      ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/myCF");
    } catch (NamingException e) {
      Util.TRACE("Expected NamingException caught.");
      exceptionCaught = true;
    }

    if (exceptionCaught) {
      reportSuccess();
    } else {
      reportFailure();
    }
  }

  public void clearQueue(Queue queue) throws JMSException {
    Util.TRACE_ENTRY();
    long numberCleared = -1;
    try (JMSContext context = queueConnectionFactory_.createContext(Session.SESSION_TRANSACTED)) {
      Util.CODEPATH();
      JMSConsumer consumer = context.createConsumer(queue);
      Message message;
      do {
        message = consumer.receiveNoWait();
        numberCleared++;
      } while (message != null);
      Util.CODEPATH();
      context.commit();
    } finally {
      Util.TRACE_EXIT("numberCleared="+numberCleared);
    }
  }
}
