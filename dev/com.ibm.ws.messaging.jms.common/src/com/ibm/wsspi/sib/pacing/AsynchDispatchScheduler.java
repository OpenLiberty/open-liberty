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

package com.ibm.wsspi.sib.pacing;

/**
 * @author wallisgd
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * Interface implemented by the XD object returned on from MessagePacing.preAsynchDispatch()
 * method.
**/
public interface AsynchDispatchScheduler {
  /**
   * returns true if the message pacer requires the MDB dispatcher to suspend processing messages.
   * returns false if the message pacer requires the MDB dispatcher to continue processing messages.
   * 
   */
  boolean suspendAsynchDispatcher();
}
