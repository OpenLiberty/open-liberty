/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
//Class: RLSController
//------------------------------------------------------------------------------
/**
 * The RecoveryLogService delegates calls to suspend and resume to an implementation of RLSController. 
 */
interface RLSController {

	 /**
     * Suspend i/o to the recovery log files
     * 
     * @param timeout value in seconds after which this suspend operation will be cancelled.
     * A timeout value of zero indicates no timeout
     * 
     * @exception RLSTimeoutRangeException Thrown if timeout is not in the range 0 < timeout <= 1,000,000,000.   
     */
	RLSSuspendToken suspend(int timeout) throws RLSTimeoutRangeException;
	
	/**
     * Cancels the corresponding suspend operation, identified by the supplied token.
     * 
     * If there are no outstanding suspend operation, then resumes i/o to the recovery log files.
     * 
     * @param token identifies the corresponding suspend operation to cancel
     * 
     * @exception RLSInvalidSuspendTokenException Thrown if token is null, invalid or has expired
     * 
     */
	void resume(RLSSuspendToken token) throws RLSInvalidSuspendTokenException;
	
	 /**
     * Cancels the corresponding suspend operation, identified by the supplied token byte array.
     * 
     * If there are no outstanding suspend operation, then resumes i/o to the recovery log files.
     * 
     * @param tokenBytes a byte array representation of the RLSSuspendToken, identifying the 
     * corresponding suspend operation to cancel
     * 
     * @exception RLSInvalidSuspendTokenException Thrown if the token byte array is null, invalid or has expired
     * 
     */
	void resume(byte[] tokenBytes) throws RLSInvalidSuspendTokenException;
}
