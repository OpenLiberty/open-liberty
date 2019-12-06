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
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.jms.IllegalStateRuntimeException;

public class MessageListenerSession implements MessageListener {
  protected QueueSession  relatedSession_ = null;
  protected QueueSession  unrelatedSession_ = null;
  protected QueueSession  replySession_ = null;
  protected QueueSender   replySender_ = null;

  MessageListenerSession(QueueSession r,QueueSession u,QueueSession rs,QueueSender s) {
    Util.TRACE_ENTRY(new Object[]{r,u,rs,s});
    relatedSession_ = r;
    unrelatedSession_ = u;
    replySession_ = rs;
    replySender_ = s;
    Util.TRACE_EXIT();
  }

  @Override
  public void onMessage(Message message) {
    Util.TRACE_ENTRY(message);
    boolean exceptionOnRelatedClose = false;
    boolean exceptionOnUnrelatedClose = false;

    try {
      exceptionOnRelatedClose = false;
      relatedSession_.close();
    } catch (javax.jms.IllegalStateException ise) {
      exceptionOnRelatedClose = true;
    } catch (Exception e) {
      Util.LOG(e);
    }

    Util.CODEPATH();
    try {
      exceptionOnUnrelatedClose = false;
      unrelatedSession_.close();
    } catch (javax.jms.IllegalStateException ise) {
      exceptionOnUnrelatedClose = true;
    } catch (Exception e) {
      Util.LOG(e);
    }

    Util.CODEPATH();
    if (exceptionOnRelatedClose==true
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
