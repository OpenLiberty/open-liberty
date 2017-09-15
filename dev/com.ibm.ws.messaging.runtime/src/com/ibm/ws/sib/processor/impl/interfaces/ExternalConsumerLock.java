/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl.interfaces;

/**
 * Such objects are synchronized against by the consumer dispatch code.
 * The consumer dispatcher can ask the implementor whether this lock 
 * should be yielded or not while it cycles in it's tight message dispatch
 * loop.
 */
public interface ExternalConsumerLock
{
  /**
   * Indicates to the consumer dispatch code that the implementor wants this
   * external consumer lock released and yielded to someone else soon.
   * <p>
   * The consumer dispatcher uses this call to decide whether it should
   * temporarily break-out of it's tight message-dispatch loop in order to
   * free this lock, so someone else can get a turn using it.
   * <p>
   * This was specifically required by the mediation state machine so that 
   * the consumer dispatch code would not block an administrator's request
   * if there were huge numbers of messages on the pre-mediated input item 
   * stream.
   * 
   * @return true if this lock should be yielded to give another thread a turn
   * or false indicating that the consumer dispatch code can keep the lock and 
   * process another message.
   */
  boolean isLockYieldRequested();
}
