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

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import javax.jms.CompletionListener;
import javax.jms.Message;

public class CompletionListenerBase implements CompletionListener, AutoCloseable {
  private final ReentrantLock syncLock_ = new ReentrantLock();
  private final Condition     notification_ = syncLock_.newCondition();
  private final Condition     notPaused_ = syncLock_.newCondition();
  private long                defaultMaximumWaitMilliseconds_ = 10000;
  public int                  completionCount_ = 0;
  public int                  exceptionCount_ = 0;
  public int                  expectedCompletionCount_ = 0;
  public int                  expectedExceptionCount_ = 0;
  protected boolean           paused_ = false;

  public void onCompletion(Message msg) {
    Util.TRACE_ENTRY();
    Util.TRACE_EXIT();
  }

  public void onException(Message msg, Exception exp) {
    Util.TRACE_ENTRY();
    Util.TRACE_EXIT();
  }

  public void close() {
    Util.TRACE_ENTRY();
    Util.TRACE_EXIT();
  }

  public void reset() {
    Util.TRACE_ENTRY();
    completionCount_ = 0;
    exceptionCount_ = 0;
    try {
      syncLock_.lock();
      notification_.signalAll();
      notPaused_.signalAll();
    } finally {
      syncLock_.unlock();
    }
    setPause(false);
    Util.TRACE_EXIT();
  }

  protected void checkPause() {
    Util.TRACE_ENTRY();
    try {
      syncLock_.lock();
      while (paused_) {
        try {
          notPaused_.await();
        } catch (InterruptedException e) {
          Util.LOG(e.getMessage());
          Util.TRACE(e);
        }
      }
    } finally {
      syncLock_.unlock();
      Util.TRACE_EXIT();
    }
  }

  /**
   * @param paused if true, requires the onCompletion and onException methods to block until set to false.
   */
  public void setPause(boolean paused) {
    Util.TRACE_ENTRY(paused);
    try {
      syncLock_.lock();
      paused_ = paused;
      if (!paused_) notPaused_.signal();
    } finally {
      syncLock_.unlock();
      Util.TRACE_EXIT(paused_);
    }
  }

  /**
   * @param expectedCompletionCount
   * @param expectedExceptionCount
   * @return
   */
  public synchronized boolean waitFor(long expectedCompletionCount ,long expectedExceptionCount) {
    return waitFor(defaultMaximumWaitMilliseconds_,expectedCompletionCount,expectedExceptionCount);
  }

  /**
   * @param maximumWaitMilliseconds time allowed for the condition to be met.
   * @param expectedCompletionCount
   * @param expectedExceptionCount
   * @return
   */
  public boolean waitFor(long maximumWaitMilliseconds, long expectedCompletionCount ,long expectedExceptionCount) {
    Util.TRACE_ENTRY("expectedCompletionCount="+expectedCompletionCount+",expectedExceptionCount="+expectedExceptionCount);
    try {
      syncLock_.lock();
      this.expectedCompletionCount_ = (int) expectedCompletionCount;
      this.expectedExceptionCount_ = (int) expectedExceptionCount;
      long endMilliseconds = System.currentTimeMillis() + maximumWaitMilliseconds;
      long remainingMilliseconds = endMilliseconds - System.currentTimeMillis();
      while (remainingMilliseconds > 0
            &&(expectedCompletionCount_ > completionCount_ || expectedExceptionCount_ > exceptionCount_)
            ) {
        try {
          notification_.await(remainingMilliseconds,java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          Util.LOG(e.getMessage());
          Util.TRACE(e);
        }
        remainingMilliseconds = endMilliseconds - System.currentTimeMillis();
      }
    } finally {
      syncLock_.unlock();
      boolean conditionMet = (completionCount_ == expectedCompletionCount_ && exceptionCount_ == expectedExceptionCount_);
      Util.TRACE_EXIT("conditionMet="+conditionMet+",completionCount="+completionCount_+",exceptionCount="+exceptionCount_);
      return conditionMet;
    }
  }

  public void notifyConditionMet() {
    Util.TRACE_ENTRY();
    try {
      syncLock_.lock();
      notification_.signal();
    } finally {
      syncLock_.unlock();
      Util.TRACE_EXIT();
    }
  }
}
