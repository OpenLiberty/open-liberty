/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: RecoveryLogCallBack
//------------------------------------------------------------------------------
/**
* This interface is implemented by those objects that are to be registered as
* recovery log callback objects.
*/
public interface RecoveryLogCallBack
{
   //------------------------------------------------------------------------------
   // Method: RecoveryLogCallBack.recoveryStarted
   //------------------------------------------------------------------------------
   /**
   * A request to begin recovery procesing for the given failure scope has been 
   * received and is about to be processed.
   *
   * @param failureScope The failure scope for which recovery processing is about
   *                     to started.
   */
   void recoveryStarted(FailureScope failureScope);

   //------------------------------------------------------------------------------
   // Method: RecoveryLogCallBack.recoveryCompleted
   //------------------------------------------------------------------------------
   /**
   * Recovery processing for the given failure scope has been completed. This 
   * applies to "first pass" recovery only. Services may be retrying recovery that
   * could not be completed during first pass recovery processing after this call 
   * has been issued. (eg transaction service periodically trying to contact a db
   * that was not contactable.)
   *
   * @param failureScope The failure scope for which recovery processing has just
   *                     been completed.
   */
   void recoveryCompleted(FailureScope failureScope);

   //------------------------------------------------------------------------------
   // Method: RecoveryLogCallBack.terminateStarted
   //------------------------------------------------------------------------------
   /**
   * A request to terminate recovery procesing for the given failure scope has been 
   * received and is about to be processed.
   *
   * @param failureScope The failure scope for which recovery processing is about
   *                     to be terminated.
   */
   void terminateStarted(FailureScope failureScope);

   //------------------------------------------------------------------------------
   // Method: RecoveryLogCallBack.terminateCompleted
   //------------------------------------------------------------------------------
   /**
   * Termination of recovery processing for the given failure scope has just been
   * completed.
   *
   * @param failureScope The failure scope for which recovery processing has just
   *                     been terminated.
   */
   void terminateCompleted(FailureScope failureScope);
}
