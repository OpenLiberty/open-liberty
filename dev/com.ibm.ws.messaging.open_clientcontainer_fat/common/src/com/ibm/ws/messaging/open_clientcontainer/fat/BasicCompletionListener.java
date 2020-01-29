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
import javax.jms.Message;

public class BasicCompletionListener extends CompletionListenerBase implements CompletionListener {
  @Override
  public void onCompletion(Message msg) {
    Util.TRACE_ENTRY(msg);
    checkPause();
    Util.CODEPATH();
    completionCount_++;
    if (completionCount_ == expectedCompletionCount_) notifyConditionMet();
    Util.TRACE_EXIT("completionCount="+completionCount_);
  }

  @Override
  public void onException(Message msg, Exception exp) {
    Util.TRACE_ENTRY("exception="+(null!=exp?exp.toString():"<null>")+",message="+(null!=msg?msg.toString():"<null>"));
    checkPause();
    exceptionCount_++;
    if (exceptionCount_ == expectedExceptionCount_) notifyConditionMet();
    Util.TRACE_EXIT("exceptionCount="+exceptionCount_);
  }
}
