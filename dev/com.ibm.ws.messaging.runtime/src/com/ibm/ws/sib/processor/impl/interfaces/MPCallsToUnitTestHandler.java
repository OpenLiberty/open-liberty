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

import com.ibm.ws.sib.processor.impl.AsynchDeletionThread;

/**
 * The unit tests requiring call-backs from MP code implement this interface,
 * then set it into the MessageProcessor.
 * 
 */
public interface MPCallsToUnitTestHandler
{
  /**
   * Used by the code to report a failure in a unit test, if the system
   * is running within the junit test framework.
   * <p>
   * If the system is not running in the junit framework, then nothing 
   * happens.
   * 
   * @param e The exception which is causing the failure.
   */
  public void unitTestFailure( String textDescription , Exception e );

  /**
   * Used by the async deletion thread to tell the unit tests that it is
   * ready to run.
   * <p>
   * The unit tests can choose to block this thread to finely control 
   * when things get deleted.
   */
  public void asyncDeletionThreadReadyToStart();
  
  /**
   * Used by the async deletion thread to synchronize with teardown
   */
  public Object getAsynchLock(AsynchDeletionThread adt);
  
}
