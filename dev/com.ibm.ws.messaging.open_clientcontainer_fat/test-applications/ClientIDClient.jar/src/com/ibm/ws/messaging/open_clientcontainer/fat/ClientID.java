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
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.IllegalStateException;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.InvalidClientIDRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ClientID extends ClientMain {
  public static void main(String[] args) {
    new ClientID().run();
  }

  public QueueConnectionFactory queueConnectionFactory_ = null;
  public ConnectionFactory      connectionFactory_ = null;
  public TopicConnectionFactory topicConnectionFactory_ = null;
  public Topic                  topic_ = null;

  @Override
  public void setup() throws Exception {
    Util.TRACE_ENTRY();

    Properties env = new Properties();
    env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
    InitialContext jndi = new InitialContext(env);
    connectionFactory_ = (ConnectionFactory) jndi.lookup("jndi_JMS_BASE_CF");
    queueConnectionFactory_ = (QueueConnectionFactory) jndi.lookup("java:comp/env/jndi_JMS_BASE_QCF");
    topicConnectionFactory_ = (TopicConnectionFactory) jndi.lookup("java:comp/env/eis/tcf");
    topic_ = (Topic) jndi.lookup("java:comp/env/jndi_TOPIC_ONE");

    Util.TRACE_EXIT();
  }

  @ClientTest
  public void testSetClientID() throws JMSException {
      ConnectionFactory[] connectionFactories = {connectionFactory_ ,queueConnectionFactory_ ,topicConnectionFactory_};
      String reason = "";

      for (ConnectionFactory connectionFactory : connectionFactories) {
          try (JMSContext jmsContext = connectionFactory_.createContext()) {
              String clientIdSet = "ClientID"+connectionFactory.getClass().getSimpleName();
              String clientIdGet = null;

              try {
                  jmsContext.setClientID(clientIdSet);
              } catch (IllegalStateRuntimeException e) {
                  reason += ", failed to set client ID:"+clientIdSet;
                  Util.LOG(e);
              } catch (Exception e) {
                  reason += ", set client ID:"+clientIdSet+" unexpected exception: "+e.getClass().getName();
                  Util.LOG(e);
              }

              try {
                  clientIdGet = jmsContext.getClientID();
              } catch (JMSRuntimeException e) {
                  reason += ", failed to get client ID:"+clientIdSet;
                  Util.LOG(e);
              } catch (Exception e) {
                  reason += ", get client ID:"+clientIdSet+" unexpected exception: "+e.getClass().getName();
                  Util.LOG(e);
              }
              if (null==clientIdGet||!clientIdGet.equals(clientIdSet)) {
                  reason += ","+clientIdGet+" != "+ clientIdSet;
              }
          }
      }

      if (0==reason.length()) {
          reportSuccess();
      } else {
          reportFailure(reason.substring(1));
      }
  }

  @ClientTest
  public void testSetClientIDTwice() throws JMSException {
      ConnectionFactory[] connectionFactories = {connectionFactory_ ,queueConnectionFactory_ ,topicConnectionFactory_};     
      String     reason = "";

      for (ConnectionFactory connectionFactory : connectionFactories) {
          try (JMSContext jmsContext0 = connectionFactory_.createContext();
               JMSContext jmsContext1 = connectionFactory_.createContext()) {
              String clientIdSet = "ClientID"+connectionFactory.getClass().getSimpleName();
              String clientIdGet = null;

              try {
                  jmsContext0.setClientID(clientIdSet);
              } catch (IllegalStateRuntimeException e) {
                  reason += ", failed to set client ID:"+clientIdSet;
                  Util.LOG(e);
              }
              try {
                  jmsContext1.setClientID(clientIdSet);
                  reason+=", second set client ID succeeded:"+clientIdSet;
              } catch (InvalidClientIDRuntimeException e) {
                  // exception expected
                  Util.TRACE(e);
              } catch (Exception e) {
                  reason += ", second set client ID"+clientIdSet+" unexpected exception: "+e.getClass().getName();
                  Util.LOG(e);
              }

              try {
                  clientIdGet = jmsContext0.getClientID();
              } catch (JMSRuntimeException e) {
                  reason += ", failed to get client ID:"+clientIdSet;
                  Util.LOG(e);
              } catch (Exception e) {
                  reason += ", get client ID:"+clientIdSet+" unexpected exception: "+e.getClass().getName();
                  Util.LOG(e);
              }
              if (null==clientIdGet||!clientIdGet.equals(clientIdSet)) {
                  reason += ","+clientIdGet+" != "+clientIdSet;
              }
          }
      }

    if (0==reason.length()) {
      reportSuccess();
    } else {
      reportFailure(reason.substring(1));
    }
  }

  @ClientTest
  public void testDurableSubscriberWithoutClientID() throws JMSException {
    try (Connection      connection = connectionFactory_.createConnection();
         TopicConnection topicConnection = topicConnectionFactory_.createTopicConnection()) {
    
        Session         session = connection.createSession();
        TopicSession    topicSession = topicConnection.createTopicSession(true, 1);
        String          reason = "";

        // Session ---------------------------------------------------------------------------------------------------------------------

        Util.CODEPATH();
        try {
            MessageConsumer messageConsumer = session.createDurableConsumer(topic_, "consumer name");
            reason += ",session durable consumer created";
        } catch (IllegalStateException e) {
            Util.TRACE("Expected IllegalStateException raised.", e);
        } catch (Exception e) {
            reason += ",session createDurableConsumer raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            MessageConsumer messageConsumer = session.createDurableConsumer(topic_, "consumer name (selective)", "message_selector", true);
            reason += ",session durable consumer (with message selector) created";
        } catch (IllegalStateException e) {
            Util.TRACE("Expected IllegalStateException raised.", e);
        } catch (Exception e) {
            reason += ",session createDurableConsumer (with message selector) raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            TopicSubscriber topicSubscriber = session.createDurableSubscriber(topic_, "subscriber_name");
            reason += ",session durable subscriber created";
        } catch (IllegalStateException e) {
            Util.TRACE("Expected IllegalStateException raised.", e);
        } catch (Exception e) {
            reason += ",session createDurableSubscriber raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            TopicSubscriber topicSubscriber = session.createDurableSubscriber(topic_, "subscriber name (selective)", "message_selector", true);
            reason += ",session durable subscriber (with message selector) created";
        } catch (IllegalStateException e) {
            Util.TRACE("Expected IllegalStateException raised.",e);
        } catch (Exception e) {
            reason += ",session createDurableSubscriber (with message selector) raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            MessageConsumer messageConsumer = session.createSharedConsumer(topic_, "shared consumer name");
        } catch (Exception e) {
            reason += ",session createSharedConsumer raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            MessageConsumer messageConsumer = session.createSharedConsumer(topic_, "shared consumer name (selective)", "message_selector");
        } catch (Exception e) {
            reason += ",session createSharedConsumer (with message selector) raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            MessageConsumer messageConsumer = session.createSharedDurableConsumer(topic_, "shared durable consumer name");
            messageConsumer.close();
            session.unsubscribe("shared durable consumer name");
        } catch (Exception e) {
            reason += ",session createSharedDurableConsumer raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            MessageConsumer messageConsumer = session.createSharedDurableConsumer(topic_, "shared durable consumer name (selective)", "message_selector");
            messageConsumer.close();
            session.unsubscribe("shared durable consumer name (selective)");
        } catch (Exception e) {
            reason += ",session createSharedDurableConsumer (with message selector) raised "+e.getClass().getName();
            Util.LOG(e);
        }

        // TopicSession ----------------------------------------------------------------------------------------------------------------

        Util.CODEPATH();
        try {
            TopicSubscriber topicSubscriber = topicSession.createDurableSubscriber(topic_, "subscriber name");
            reason += ",topic durable consumer created";
        } catch (IllegalStateException e) {
            Util.TRACE("Expected IllegalStateException raised.", e);
        } catch (Exception e) {
            reason += ",topic createDurableSubscriber raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            TopicSubscriber topicSubscriber = topicSession.createDurableSubscriber(topic_, "subscriber name (selective)", "message_selector", true);
            reason += ",topic durable consumer (with message selector) created";
        } catch (IllegalStateException e) {
            Util.TRACE("Expected IllegalStateException raised.", e);
        } catch (Exception e) {
            reason += ",topic createDurableSubscriber (with message selector) raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic_);
        } catch (Exception e) {
            reason += ",topic createSubscriber raised "+e.getClass().getName();
            Util.LOG(e);
        }

        Util.CODEPATH();
        try {
            TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic_, "message_selector", true);
        } catch (Exception e) {
            reason += ",topic createSubscriber (with message selector) raised "+e.getClass().getName();
            Util.LOG(e);
        }

        if (0==reason.length()) {
            reportSuccess();
        } else {
            reportFailure(reason.substring(1));
        }
    }
  }
}
