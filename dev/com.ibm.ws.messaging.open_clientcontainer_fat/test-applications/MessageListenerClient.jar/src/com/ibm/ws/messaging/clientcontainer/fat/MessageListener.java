/* ============================================================================
 * Copyright (c) 2019 IBM Corporation and others.
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
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.QueueSender;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.Context;

public class MessageListener extends ClientMain {
  public static void main(String[] args) {
    new MessageListener().run();
  }

  public Queue                  queueOne_ = null;
  public Queue                  queueTwo_ = null;
  public QueueConnectionFactory queueConnectionFactory_[] = { null, null, null };

  @Override
  public void setup() throws Exception {
    Util.TRACE_ENTRY();
    Properties env = new Properties();
    env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
    InitialContext jndi = new InitialContext(env);
    queueConnectionFactory_[0] = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
    queueConnectionFactory_[1] = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF1");
    queueConnectionFactory_[2] = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF2");
    queueOne_ = (Queue) jndi.lookup("java:comp/env/jndi_QUEUE_ONE");
    queueTwo_ = (Queue) jndi.lookup("java:comp/env/jndi_QUEUE_TWO");
    Util.TRACE_EXIT();
  }

  @ClientTest
  public void testMessageListenerContext() throws JMSException {
    try (QueueConnection replyConnection = queueConnectionFactory_[2].createQueueConnection();
         JMSContext context = queueConnectionFactory_[0].createContext();
         JMSContext unrelatedContext = queueConnectionFactory_[1].createContext();
        ) {
      replyConnection.start();
      clearQueue(queueOne_);
      clearQueue(queueTwo_);

      QueueSession replySession = replyConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      QueueSession replyReceiveSession = replyConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      QueueSender replySender = replySession.createSender(queueTwo_);
      Util.CODEPATH();
      JMSProducer producer = context.createProducer();
      Util.CODEPATH();
      JMSConsumer consumer = context.createConsumer(queueOne_);
      Util.CODEPATH();
      MessageListenerContext listener = new MessageListenerContext(context,unrelatedContext,replySession,replySender);
      Util.CODEPATH();
      TextMessage message = context.createTextMessage("testMessageListenerContext");
      Util.CODEPATH();
      producer.send(queueOne_, message);
      Util.CODEPATH();
      consumer.setMessageListener(listener);
      Util.CODEPATH();

      MessageConsumer replyConsumer = replyReceiveSession.createConsumer(queueTwo_);
      Util.CODEPATH();
      TextMessage result = (TextMessage)replyConsumer.receive(WAIT_TIME);
      Util.TRACE("result="+result);

      if (null!=result&&result.getText().equals("passed")) {
        reportSuccess();
      } else {
        reportFailure();
      }
    }
  }

  @ClientTest
  public void testMessageListenerConnection() throws JMSException {
    try (QueueConnection replyConnection = queueConnectionFactory_[2].createQueueConnection();
         QueueConnection connection = queueConnectionFactory_[0].createQueueConnection();
         QueueConnection unrelatedConnection = queueConnectionFactory_[1].createQueueConnection();
        ) {
      replyConnection.start();
      connection.start();
      unrelatedConnection.start();
      clearQueue(queueOne_);
      clearQueue(queueTwo_);

      QueueSession replySession = replyConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      QueueSession replyReceiveSession = replyConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      QueueSender replySender = replySession.createSender(queueTwo_);
      Util.CODEPATH();
      QueueSession session = connection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      MessageProducer producer = session.createProducer(queueOne_);
      Util.CODEPATH();
      MessageConsumer consumer = session.createConsumer(queueOne_);
      Util.CODEPATH();
      MessageListenerConnection listener = new MessageListenerConnection(connection,unrelatedConnection,replySession,replySender);
      Util.CODEPATH();
      TextMessage msg = session.createTextMessage("testMessageListenerConnection");
      Util.CODEPATH();
      producer.send(msg);
      Util.CODEPATH();
      consumer.setMessageListener(listener);
      Util.CODEPATH();

      MessageConsumer replyConsumer = replyReceiveSession.createConsumer(queueTwo_);
      Util.CODEPATH();
      TextMessage result = (TextMessage)replyConsumer.receive(WAIT_TIME);
      Util.TRACE("result="+result);

      if (null!=result&&result.getText().equals("passed")) {
        reportSuccess();
      } else {
        reportFailure();
      }
    }
  }

  @ClientTest
  public void testMessageListenerSession() throws JMSException {
    try (QueueConnection replyConnection = queueConnectionFactory_[2].createQueueConnection();
         QueueConnection connection = queueConnectionFactory_[0].createQueueConnection();
         QueueConnection unrelatedConnection = queueConnectionFactory_[1].createQueueConnection();
        ) {
      replyConnection.start();
      connection.start();
      unrelatedConnection.start();
      clearQueue(queueOne_);
      clearQueue(queueTwo_);

      QueueSession replySession = replyConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      QueueSession replyReceiveSession = replyConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      QueueSender replySender = replySession.createSender(queueTwo_);
      Util.CODEPATH();
      QueueSession session = connection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      QueueSession unrelatedSession = unrelatedConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Util.CODEPATH();
      MessageProducer producer = session.createProducer(queueOne_);
      Util.CODEPATH();
      MessageConsumer consumer = session.createConsumer(queueOne_);
      Util.CODEPATH();
      MessageListenerSession listener = new MessageListenerSession(session,unrelatedSession,replySession,replySender);
      Util.CODEPATH();
      TextMessage msg = session.createTextMessage("testMessageListenerSession");
      Util.CODEPATH();
      producer.send(msg);
      consumer.setMessageListener(listener);
      Util.CODEPATH();

      MessageConsumer replyConsumer = replyReceiveSession.createConsumer(queueTwo_);
      Util.CODEPATH();
      TextMessage result = (TextMessage)replyConsumer.receive(WAIT_TIME);
      Util.TRACE("result="+result);

      if (null!=result&&result.getText().equals("passed")) {
        reportSuccess();
      } else {
        reportFailure();
      }
    }
  }

  public void clearQueue(Queue queue) throws JMSException {
    Util.TRACE_ENTRY(queue);
    long numberCleared = -1;
    try (JMSContext context = queueConnectionFactory_[0].createContext(Session.SESSION_TRANSACTED)) {
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
