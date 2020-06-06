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
import java.util.Enumeration;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.MessageListener;
import javax.jms.MessageNotWriteableException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Context;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import java.util.Stack;
import java.util.Enumeration;
import java.util.Vector;

import java.util.concurrent.atomic.AtomicInteger;


public class JMS2AsyncSend extends ClientMain {

  public static void main(String[] args) {
    new JMS2AsyncSend().run();
  }

  @Override
  protected void setup() throws Exception {
    Util.TRACE_ENTRY();

    Util.TRACE_EXIT();
  }

  @ClientTest
  public void testNokia() throws Exception
  {
    Object coord = new Object();
    AtomicInteger ready = new AtomicInteger(0);
    int num_subs = 5;

    Util.TRACE("Starting subscribers...");
    for (int i=0;num_subs>i;++i)
    {
      new Thread("SUB"+i)
      {
        private String name;
        Thread setID(String n)
        {
          name = n;
          return this;
        }
        @Override
        public void run()
        {
          try
          {
            Properties env = new Properties();
            env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
            InitialContext ctx = new InitialContext(env);
            ConnectionFactory cf = (ConnectionFactory)ctx.lookup("java:comp/env/tcf_nokia");
            Util.TRACE(name+": cf="+cf);
            Topic topic = (Topic)ctx.lookup("java:comp/env/topic_nokia");
            Util.TRACE(name+": topic="+topic);
            Connection c = cf.createConnection();
            Session s = c.createSession(false,Session.AUTO_ACKNOWLEDGE);
            MessageConsumer subs = s.createConsumer(topic);
            subs.setMessageListener(new MessageListener()
                                    {
                                      private long count = 0;
                                      @Override
                                      public void onMessage(Message m)
                                      {
                                        ++count;
                                        Util.TRACE(name+": count="+count);
                                        TextMessage tm = (TextMessage)m;
                                        try
                                        {
                                          String t = tm.getText();
                                          if (t.startsWith("END OF BATCH"))
                                          {
                                            ready.getAndIncrement();
                                          }
                                        }
                                        catch (Exception gte)
                                        {
                                          Util.TRACE(name+": GTE",gte);
                                        }
                                      }
                                    }
                                   );
            c.start();

            ready.getAndIncrement();
            synchronized(coord)
            {
              coord.wait();
            }

            subs.setMessageListener(null);
            subs.close();
            s.close();
            c.setExceptionListener(null);
            c.stop();
            c.close();
            Util.TRACE(name+": exiting.");
          }
          catch (Exception e)
          {
            Util.TRACE(name+": ",e);
          }
        }
      }.setID("SUB"+i).start();
      Util.TRACE("Subscriber "+i+" started.");
    }

    Util.TRACE("Waiting for subscribers to get ready.");
    while (num_subs!=ready.get()) Thread.sleep(1000);

    Util.TRACE("Starting publishing loop");

    Properties env = new Properties();
    env.put(Context.PROVIDER_URL, "iiop://localhost:2809");
    InitialContext ctx = new InitialContext(env);
    ConnectionFactory cf = (ConnectionFactory)ctx.lookup("java:comp/env/tcf_nokia");
    Util.TRACE("PRD: cf="+cf);
    Topic topic = (Topic)ctx.lookup("java:comp/env/topic_nokia");
    Util.TRACE("PRD: topic="+topic);
    Connection c = cf.createConnection();
    Session s = c.createSession(true,Session.AUTO_ACKNOWLEDGE);
    //Session s = c.createSession(false,Session.AUTO_ACKNOWLEDGE);
    MessageProducer prod = s.createProducer(topic);
    c.start();

    long total = 0;

    for (int batch=0;1000>batch;++batch)
    {
      ready.set(0);
      Util.TRACE("PRD: batch #"+batch);
      for (int n=0;24>n;++n)
      {
        TextMessage tm = s.createTextMessage("batch="+batch+",message="+n+",xxxxxxxxxx");
        prod.send(tm);
        ++total;
      }
      TextMessage tm = s.createTextMessage("END OF BATCH "+batch);
      prod.send(tm);
      ++total;

      Util.TRACE("PRD: committing batch #"+batch+" (total messages: "+total+")");
      try 
      {
        s.commit();
      }
      catch (javax.jms.JMSException oe)
      {
        Util.TRACE("PRD: ",oe);
      }
      Util.TRACE("PRD: waiting");
      for (int w=0;60>w&&num_subs!=ready.get();++w) Thread.sleep(1000);
      if (num_subs!=ready.get()) Util.TRACE("PRD: Possible stall detected");
//      Util.TRACE("PRD: sleeping");
//      Thread.sleep(5000);
    }

    Util.TRACE("PRD: done.");
    Thread.sleep(1000);

    prod.close();
    s.close();
    c.stop();
    c.close();

    Util.TRACE("PRD: notifying subscribers.");

    synchronized(coord)
    {
      coord.notifyAll();
    }
    Thread.sleep(5000);
  }
}
