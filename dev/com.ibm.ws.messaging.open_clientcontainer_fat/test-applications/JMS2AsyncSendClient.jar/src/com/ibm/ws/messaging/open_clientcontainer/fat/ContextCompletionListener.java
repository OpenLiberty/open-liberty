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

import javax.jms.JMSRuntimeException;
import javax.jms.JMSContext;
import javax.jms.Message;

public class ContextCompletionListener extends CompletionListenerBase {
  public JMSContext context_;
  public JMSContext unrelatedContext_;
  public boolean    exceptionOnUnrelatedClose_ = false;
  public boolean    exceptionOnClose_ = false;
  public boolean    exceptionOnCommit_ = false;
  public boolean    exceptionOnRollback_ = false;
  public boolean    producerCreated_ = false;

  ContextCompletionListener(JMSContext c,JMSContext ur) {
    Util.TRACE_ENTRY(new Object[] {this, c, ur});
    context_ = c;
    unrelatedContext_ = ur;
    Util.TRACE_EXIT();
  }

  @Override
  public void onCompletion(Message msg) {
    Util.TRACE_ENTRY(msg);
    checkPause();
    try {
      Util.CODEPATH();
      unrelatedContext_.close();
      exceptionOnUnrelatedClose_ = false;
    } catch (Exception e) {
      Util.TRACE("unrelated context_ close() raised an exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnUnrelatedClose_ = true;
    }
    try {
      Util.CODEPATH();
      context_.close();
      exceptionOnClose_ = false;
    } catch (Exception e) {
      Util.TRACE("close() raised an exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnClose_ = true;
    }
    try {
      Util.CODEPATH();
      context_.commit();
      exceptionOnCommit_ = false;
    } catch (Exception e) {
      Util.TRACE("commit() raised an exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnCommit_ = true;
    }
    try {
      Util.CODEPATH();
      context_.rollback();
      exceptionOnRollback_ = false;
    } catch (Exception e) {
      Util.TRACE("roillback() raised an exception:"+e.getClass().getName()+" "+e.getMessage());
      exceptionOnRollback_ = true;
    }
    try {
      producerCreated_ = (context_.createProducer() != null);
    } catch (JMSRuntimeException e) {
      producerCreated_ = false;
    }
    completionCount_++;
    if (completionCount_ == expectedCompletionCount_) notifyConditionMet();
    Util.TRACE_EXIT("completionCount=" + completionCount_);
  }

  @Override
  public void onException(Message msg, Exception exp) {
    Util.TRACE_ENTRY(new Object[]{msg,exp});
    checkPause();
    exceptionCount_++;
    try {
      producerCreated_ = (context_.createProducer() != null);
    } catch (JMSRuntimeException e) {
      producerCreated_ = false;
    }
    if (exceptionCount_ == expectedExceptionCount_) notifyConditionMet();
    Util.TRACE_EXIT("exceptionCount=" + exceptionCount_);
  }

  @Override
  public void close() {
    Util.TRACE_ENTRY();
    try {
      context_.close();
    } catch (Exception e) {
      Util.LOG(e);
    }
    Util.TRACE_EXIT();
  }
}
