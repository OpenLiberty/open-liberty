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

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.jms.IllegalStateException;

public class MessageListenerConnection implements MessageListener {
  protected QueueConnection relatedConnection_ = null;
  protected QueueConnection unrelatedConnection_ = null;
  protected QueueSession    replySession_ = null;
  protected QueueSender     replySender_ = null;

  MessageListenerConnection(QueueConnection r,QueueConnection u,QueueSession rs,QueueSender s) {
    Util.TRACE_ENTRY(new Object[]{r,u,rs,s});
    relatedConnection_ = r;
    unrelatedConnection_ = u;
    replySession_ = rs;
    replySender_ = s;
    Util.TRACE_EXIT();
  }

  @Override
  public void onMessage(Message message) {
    Util.TRACE_ENTRY(message);
    boolean exceptionOnRelatedStop = false;
    boolean exceptionOnRelatedClose = false;
    boolean exceptionOnUnrelatedStop = false;
    boolean exceptionOnUnrelatedClose = false;

    try {
      exceptionOnRelatedStop = false;
      relatedConnection_.stop();
    } catch (IllegalStateException ise) {
      exceptionOnRelatedStop = true;
    } catch (Exception e) {
      Util.LOG(e);
    }

    Util.CODEPATH();
    try {
      exceptionOnUnrelatedStop = false;
      unrelatedConnection_.stop();
    } catch (IllegalStateException ise) {
      exceptionOnUnrelatedStop = true;
    } catch (Exception e) {
      Util.LOG(e);
    }

    Util.CODEPATH();
    try {
      exceptionOnRelatedClose = false;
      relatedConnection_.close();
    } catch (IllegalStateException ise) {
      exceptionOnRelatedClose = true;
    } catch (Exception e) {
      Util.LOG(e);
    }

    Util.CODEPATH();
    try {
      exceptionOnUnrelatedClose = false;
      unrelatedConnection_.close();
    } catch (IllegalStateException ise) {
      exceptionOnUnrelatedClose = true;
    } catch (Exception e) {
      Util.LOG(e);
    }

    Util.CODEPATH();
    if (exceptionOnRelatedStop==true
        &&exceptionOnUnrelatedStop==false
        &&exceptionOnRelatedClose==true
        &&exceptionOnUnrelatedClose==false
       ) {
      reply("passed");
    } else {
      reply("failed");
    }
    Util.TRACE_EXIT();
  }

  protected void reply(String text) {
    Util.TRACE_ENTRY(text);
    try {
      TextMessage reply = replySession_.createTextMessage(text);
      replySender_.send(reply);
    } catch (Exception e) {
      Util.LOG(e);
    } finally {
      Util.TRACE_EXIT();
    }
  }
}
