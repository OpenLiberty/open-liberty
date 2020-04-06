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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueSession;
import javax.jms.Queue;
import javax.jms.MessageProducer;

public class SessionCompletionListener extends CompletionListenerBase {
  public QueueSession       session_ = null;
  public QueueSession       unrelatedSession_ = null;
  protected Queue           queue_ = null;
  protected MessageProducer producer_ = null;
  public boolean            exceptionOnClose_ = false;
  public boolean            exceptionOnUnrelatedClose_ = false;
  public boolean            exceptionOnCommit_ = false;
  public boolean            exceptionOnRollback_ = false;
  public boolean            producerCreated_ = false;
  public boolean            exceptionOnProducerClose_ = false;

  SessionCompletionListener(QueueSession s,QueueSession u,Queue q) throws JMSException {
    Util.TRACE_ENTRY("session="+s+",unrelatedSession="+u+",queue="+q);
    session_ = s;
    unrelatedSession_ = u;
    queue_ = q;
    producer_ = session_.createProducer(queue_);
    Util.TRACE_EXIT();
  }

  @Override
  public void onCompletion(Message msg) {
    Util.TRACE_ENTRY();
    checkPause();
    try {
      exceptionOnUnrelatedClose_ = false;
      unrelatedSession_.close();
    } catch (JMSException e) {
      Util.TRACE("unrelated session close() raised exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnUnrelatedClose_ = true;
    }

    try {
      Util.CODEPATH();
      session_.close();
      exceptionOnClose_ = false;
    } catch (JMSException e) {
      Util.TRACE("close() raised expected exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnClose_ = true;
    }

    try {
      Util.CODEPATH();
      session_.commit();
      exceptionOnCommit_ = false;
    } catch (JMSException e) {
      Util.TRACE("commit() raised expected exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnCommit_ = true;
    }

    try {
      Util.CODEPATH();
      session_.rollback();
      exceptionOnRollback_ = false;
    } catch (JMSException e) {
      Util.TRACE("roillback() raised expected exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnRollback_ = true;
    }

    try {
      exceptionOnProducerClose_ = false;
      producer_.close();
    } catch (javax.jms.IllegalStateException e) {
      Util.TRACE("message producer close() raised expected exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnProducerClose_ = true;
    } catch (JMSException e) {
      Util.TRACE("message producer close() raised unexpected exception:"+e.getClass().getName()+" "+e.getMessage());
    }

    try {
      producerCreated_ = (session_.createProducer(queue_) != null);
    } catch (JMSException e) {
      producerCreated_ = false;
    }

    completionCount_++;
    if (completionCount_ == expectedCompletionCount_) notifyConditionMet();
    Util.TRACE_EXIT("completionCount=" + completionCount_);
  }

  @Override
  public void onException(Message msg, Exception exp) {
    Util.TRACE_ENTRY(new Object[] {msg, exp});
    checkPause();
    exceptionCount_++;
    if (exceptionCount_ == expectedExceptionCount_) notifyConditionMet();
    Util.TRACE_EXIT("exceptionCount=" + exceptionCount_);
  }

  @Override
  public void close() {
    Util.TRACE_ENTRY();
    try {
      session_.close();
    } catch (Exception e) {
      Util.LOG(e);
    }
    Util.TRACE_EXIT();
  }
}
