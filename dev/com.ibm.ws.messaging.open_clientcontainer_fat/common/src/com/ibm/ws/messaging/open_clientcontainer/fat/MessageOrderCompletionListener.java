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

import javax.jms.CompletionListener;
import javax.jms.JMSException;
import javax.jms.Message;

public class MessageOrderCompletionListener extends CompletionListenerBase implements CompletionListener {
  protected int messagesExpected_ = 0;
  protected int messageOrderIndex_ = 0;

  public void setExpectedMessageCount(int num) {
    messagesExpected_ = num;
    messageOrderIndex_ = 0;
  }

  public int getExpectedMessageCount() {
    return messagesExpected_;
  }

  public int getMessageOrderCount() {
    return messageOrderIndex_;
  }

  @Override
  public void onCompletion(Message msg) {
    Util.TRACE_ENTRY(msg);
    checkPause();
    try {
      Util.TRACE("Message_Order=" + msg.getIntProperty("Message_Order"));
      if (messageOrderIndex_ < messagesExpected_ && msg.getIntProperty("Message_Order") == messageOrderIndex_) messageOrderIndex_++;
      completionCount_++;
      if (completionCount_ == expectedCompletionCount_) notifyConditionMet();
    } catch (JMSException e) {
      Util.LOG(e);
    }
    Util.TRACE_EXIT("completionCount="+completionCount_);
  }

  @Override
  public void onException(Message msg, Exception exp) {
    Util.TRACE_ENTRY(new Object[] {msg, exp});
    checkPause();
    exceptionCount_++;
    if (exceptionCount_ == expectedExceptionCount_) notifyConditionMet();
    Util.TRACE_EXIT("exceptionCount="+exceptionCount_);
  }
}
