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
package web;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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

  private static final MessagingFATServletUtilities util = new MessagingFATServletUtilities(CommsLPServlet.class);

  public void testQueueSendMessage(HttpServletRequest request, HttpServletResponse response) throws Throwable {
    util.ENTRY();

    QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
    Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

    QueueConnection con = cf1.createQueueConnection();
    con.start();
    util.TRACE("cf1="+Arrays.asList(cf1));
    util.TRACE("cf1 interfaces="+Arrays.asList(cf1.getClass().getInterfaces()));

    QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

    QueueSender send = sessionSender.createSender(queue);

    TextMessage msg = sessionSender.createTextMessage();
    msg.setStringProperty("COLOUR", "BLUE");
    msg.setText("Queue Message");

    send.send(msg);

    if (con != null) con.close();
    util.EXIT();
  }

  public void testQueueReceiveMessages(HttpServletRequest request, HttpServletResponse response) throws Throwable {
    util.ENTRY();
    QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
    Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

    QueueConnection con = cf1.createQueueConnection();
    con.start();
    util.TRACE("cf1="+Arrays.asList(cf1));
    util.TRACE("cf1 interfaces="+Arrays.asList(cf1.getClass().getInterfaces()));

    QueueSession session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

    QueueReceiver receive = session.createReceiver(queue);

    TextMessage msg = null;

    do {
      msg = (TextMessage) receive.receive(5000);
      if (null!=msg) util.ALWAYS("Received message:" + msg);
    } while (msg != null);

    if (con != null) con.close();

    util.EXIT();
  }

  public void createConnectionandSendMessage() throws NamingException, JMSException {
    util.ENTRY();

    util.CODEPATH();
    QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
    util.CODEPATH();
    Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

    util.CODEPATH();
    QueueConnection con = cf1.createQueueConnection();
    util.CODEPATH();
    con.start();
    util.TRACE("cf1="+Arrays.asList(cf1));
    util.TRACE("cf1 interfaces="+Arrays.asList(cf1.getClass().getInterfaces()));

    QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    util.CODEPATH();

    QueueSender send = sessionSender.createSender(queue);
    util.CODEPATH();

    TextMessage msg = sessionSender.createTextMessage();
    msg.setStringProperty("COLOUR", "BLUE");
    msg.setText("Sent Message");

    send.send(msg);
    util.CODEPATH();

    if (con != null) con.close();
    util.EXIT();
  }

  public void testQueueSendMessageExpectException(HttpServletRequest request, HttpServletResponse response) throws Throwable {
    util.ENTRY();
    Exception except = new Exception("Unexpectedly missing exception");
    try {
      util.CODEPATH();
      createConnectionandSendMessage();
    } catch (JMSException e) {
      util.CODEPATH();
      if (e.getCause().getClass().getName().equals("com.ibm.websphere.sib.core.exception.SIConnectionDroppedException")) {
        util.CODEPATH();
        try {
          createConnectionandSendMessage();
        } catch (Exception eInner) {
          util.CODEPATH();
          except = eInner;
        }
      } else {
        util.CODEPATH();
        except = e;
      }
    }
      
    if (except instanceof JMSException
        &&null!=except.getCause()
        &&except.getCause().getClass().getName().equals("com.ibm.websphere.sib.exception.SIResourceException")) {
      util.CODEPATH();
      util.ALWAYS("SIResourceException was correctly thrown");
    } else {
      util.CODEPATH();
      util.TRACE(except);
      throw except;
    }
    util.EXIT();
  }
}
