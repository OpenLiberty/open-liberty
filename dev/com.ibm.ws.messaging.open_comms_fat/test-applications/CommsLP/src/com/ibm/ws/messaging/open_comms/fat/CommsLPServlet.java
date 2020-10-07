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
package com.ibm.ws.messaging.open_comms.fat;

import java.util.Arrays;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.annotation.WebServlet;
import componenttest.app.FATServlet;

@WebServlet("/CommsLP")
public class CommsLPServlet extends FATServlet {
  private static final long serialVersionUID = 7709282314904580334L;

  public void testQueueSendMessage(HttpServletRequest request, HttpServletResponse response) throws Throwable {
    Util.TRACE_ENTRY();
    QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
    Util.CODEPATH();
    Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
    Util.CODEPATH();
    QueueConnection con = cf1.createQueueConnection();
    Util.CODEPATH();
    con.start();

    QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    QueueSender send = sessionSender.createSender(queue);
    TextMessage msg = sessionSender.createTextMessage();
    msg.setStringProperty("COLOUR", "BLUE");
    msg.setText("Queue Message");

    send.send(msg);
    Util.CODEPATH();

    if (con != null) con.close();
    Util.TRACE_EXIT();
  }

  public void testQueueReceiveMessages(HttpServletRequest request, HttpServletResponse response) throws Throwable {
    Util.TRACE_ENTRY();
    QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
    Util.CODEPATH();
    Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
    Util.CODEPATH();
    QueueConnection con = cf1.createQueueConnection();
    Util.CODEPATH();
    con.start();

    QueueSession session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    Util.CODEPATH();
    QueueReceiver receive = session.createReceiver(queue);
    Util.CODEPATH();
    TextMessage msg = null;

    do {
      msg = (TextMessage) receive.receive(5000);
      if (null!=msg) Util.ALWAYS("Received message:" + msg);
    } while (msg != null);

    if (con != null) con.close();

    Util.TRACE_EXIT();
  }

  protected void createConnectionandSendMessage() throws NamingException, JMSException {
    Util.TRACE_ENTRY();
    QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
    Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
    QueueConnection con = cf1.createQueueConnection();
    Util.CODEPATH();
    con.start();

    QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    Util.CODEPATH();
    QueueSender send = sessionSender.createSender(queue);
    Util.CODEPATH();
    TextMessage msg = sessionSender.createTextMessage();
    msg.setStringProperty("COLOUR", "BLUE");
    msg.setText("Sent Message");

    Util.CODEPATH();
    send.send(msg);
    Util.CODEPATH();

    if (con != null) con.close();
    Util.TRACE_EXIT();
  }

  public void testQueueSendMessageExpectException(HttpServletRequest request, HttpServletResponse response) throws Throwable {
    Util.TRACE_ENTRY();
    Exception except = new Exception("Unexpectedly missing exception");
    try {
      Util.CODEPATH();
      createConnectionandSendMessage();
    } catch (JMSException e) {
      Util.CODEPATH();
      if (e.getCause().getClass().getName().endsWith(".SIConnectionDroppedException")) {
        Util.CODEPATH();
        try {
          createConnectionandSendMessage();
        } catch (Exception eInner) {
          Util.CODEPATH();
          except = eInner;
        }
      } else {
        Util.CODEPATH();
        except = e;
      }
    }
      
    if (except instanceof JMSException
        &&null!=except.getCause()
        &&except.getCause().getClass().getName().endsWith(".SIResourceException")) {
      Util.ALWAYS("SIResourceException was correctly thrown");
    } else {
      Util.TRACE(except);
      throw except;
    }
    Util.TRACE_EXIT();
  }
}
