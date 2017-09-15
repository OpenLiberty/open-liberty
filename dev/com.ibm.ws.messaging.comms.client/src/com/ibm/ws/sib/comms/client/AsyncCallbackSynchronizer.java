/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/*
 * This class provides a mechanism for synchronising asynchronous callbacks from the communications code back to application
 * code. Asynchronous callbacks occur (i) when asynchronous messages are available to be delivered to the application and
 * (ii) when asynchronous exceptions occur. To prevent deadlocks inside the communications code which can occur because
 * exception callbacks are made on different threads to asynchronous consumer message callbacks it is necessary to synchronise
 * callbacks from communications code to the application so that all asynchronous consumer message callbacks are mutually
 * exclusive with all asynchronous exception callbacks.
 *
 * It is important that enter and exit calls are properly matched so it is good practise to always perform the enter and exit
 * call using a pattern like:
 *
 * enterAsyncMessageCallback();
 * try {
 *   <call applications etc>
 * } finally {
 *   exitAsyncMessageCallback();
 * }
 */

//@ThreadSafe
public final class AsyncCallbackSynchronizer {
  private static final String CLASS_NAME = AsyncCallbackSynchronizer.class.getName();
  private static final TraceComponent tc = SibTr.register(AsyncCallbackSynchronizer.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

  //@start_class_string_prolog@
  public static final String $sccsid = "@(#) 1.4 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/AsyncCallbackSynchronizer.java, SIB.comms, WASX.SIB, uu1215.01 08/04/21 21:36:30 [4/12/12 22:13:39]";
  //@end_class_string_prolog@

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
  }

  //@GuardedBy("this")
  private int messageCallbackCount = 0; // Number of current asynchronous message callbacks in progress
  //@GuardedBy("this")
  private int exceptionCallbackCount = 0; // Number of current asynchronous exception callbacks in progress

  private String state () {
    return "messageCallbackCount="+messageCallbackCount+", exceptionCallbackCount="+exceptionCallbackCount;
  }

  // Method called by an asynchronous message callback before calling the application
  public synchronized void enterAsyncMessageCallback () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "enterAsyncMessageCallback", state());

    // Wait until there are no exception callbacks active
    while (exceptionCallbackCount > 0) {
      try {
        wait();
      } catch (InterruptedException e) {
        // No FFDC code needed
      }
    }

    // Increment the message callback counter
    messageCallbackCount++;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "enterAsyncMessageCallback", state());
  }

  // Method called by an asynchronous consumer callback after calling the application
  public synchronized void exitAsyncMessageCallback () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "exitAsyncMessageCallback", state());

    // Decrement the message callback counter
    if (messageCallbackCount > 0) {
      messageCallbackCount--;
    }

    // Notify any waiters when there are no more message callbacks
    if (messageCallbackCount == 0) {
      notifyAll();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "exitAsyncMessageCallback", state());
  }

  // Method called by an asynchronous exception callback before calling the application
  public synchronized void enterAsyncExceptionCallback () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "enterAsyncExceptionCallback", state());

    // Wait until there are no message callbacks active
    while (messageCallbackCount > 0) {
      try {
        wait();
      } catch (InterruptedException e) {
        // No FFDC code needed
      }
    }

    // Increment the exception callback counter
    exceptionCallbackCount++;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "enterAsyncExceptionCallback", state());
  }

  // Method called by an asynchronous exception callback after calling the application
  public synchronized void exitAsyncExceptionCallback () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "exitAsyncExceptionCallback", state());

    // Decrement the exception callback counter
    if (exceptionCallbackCount > 0) {
      exceptionCallbackCount--;
    }

    // Notify any waiters when there are no more exception callbacks
    if (exceptionCallbackCount == 0) {
      notifyAll();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "exitAsyncExceptionCallback", state());
  }
}
