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

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
//Class: RecoveryLogService
//------------------------------------------------------------------------------
/**
* RecoveryLogService class provides the ability to suspend and resume i/o to the log files,
* more specifically, log force and keypoint operations.
* 
*/
public class RLSControllerImpl implements RLSController {

    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(RLSControllerImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);
    
    /**
     * RecoveryLogService lock object
     */
    static Object SUSPEND_LOCK = new Object();
    
    /**
     * RLSSuspendTokenManager manages tokens and their alarms
     */
    private static RLSSuspendTokenManager _tokenManager = RLSSuspendTokenManager.getInstance();
    
    /**
     * True if i/o to the recovery log files is suspended
     */
    private static boolean _isSuspended;
    
   
    RLSControllerImpl() 
    {
    	if (tc.isEntryEnabled()) Tr.entry(tc, "RLSControllerImpl");
   	 	if (tc.isEntryEnabled()) Tr.exit(tc, "RLSControllerImpl", this);
    }
    
    /**
     * Cancels the corresponding suspend operation, identified by the supplied token.
     * 
     * If there are no outstanding suspend operation, then resumes i/o to the recovery log files.
     * 
     * @param token identifies the corresponding suspend operation to cancel
     * 
     * @exception RLSInvalidSuspendTokenException Thrown if token is null, invalid or has expired
     * 
     * @exception RecoveryLogServiceException Thrown if RecoveryLogService failed to resume
     */
    public void resume(RLSSuspendToken token) throws RLSInvalidSuspendTokenException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "resume", token);
        
        if (Configuration.isZOS())
        {
        	if (tc.isEventEnabled()) Tr.event(tc, "Operation not supported on ZOS - throwing UnsupportedOperationException");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume", "java.lang.UnsupportedOperationException");
        	throw new UnsupportedOperationException();
        }
        
        RLSControllerImpl.resumeRLS(token);
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
    }
    
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
    public void resume(byte[] tokenBytes) throws  RLSInvalidSuspendTokenException
    {
    	if (tc.isEntryEnabled()) Tr.entry(tc, "resume", RLSUtils.toHexString(tokenBytes));
        
    	if (Configuration.isZOS())
        {
        	if (tc.isEventEnabled()) Tr.event(tc, "Operation not supported on ZOS - throwing UnsupportedOperationException");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume", "java.lang.UnsupportedOperationException");
        	throw new UnsupportedOperationException();
        }
    	
        // RLSSuspendTokenImpl token = new RLSSuspendTokenImpl(tokenBytes);
        RLSSuspendToken token = Configuration.getRecoveryLogComponent()
            .createRLSSuspendToken(tokenBytes);
        
        RLSControllerImpl.resumeRLS(token);
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
    }
    
    /**
     * Suspend i/o to the recovery log files
     * 
     * @param timeout value in seconds after which this suspend operation will be cancelled.
     * A timeout value of zero indicates no timeout
     * 
     * @exception RLSTimeoutRangeException Thrown if timeout is not in the range 0 < timeout <= 1,000,000,000.   
     */
    public RLSSuspendToken suspend(int timeout) throws RLSTimeoutRangeException
	{
    	if (tc.isEntryEnabled()) Tr.entry(tc, "suspend", new Integer(timeout));
    	
    	if (Configuration.isZOS())
        {
        	if (tc.isEventEnabled()) Tr.event(tc, "Operation not supported on ZOS - throwing UnsupportedOperationException");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume", "java.lang.UnsupportedOperationException");
        	throw new UnsupportedOperationException();
        }
    	
    	RLSSuspendToken token = RLSControllerImpl.suspendRLS(timeout);
    	
    	if (tc.isEntryEnabled()) Tr.exit(tc, "suspend");
    	
    	return token;
	}
    
    //------------------------------------------------------------------------------
    // Method: RLSControllerImpl.suspendRLS     
    //------------------------------------------------------------------------------
    /**
     * Suspend i/o to the recovery log files
     * timeout : 0 = no timeout, > 0 and < 1,000,000,000 is timeout in seconds
     */
    private static RLSSuspendToken suspendRLS(int timeout) throws RLSTimeoutRangeException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "suspendRLS", new Integer(timeout));
        
        if (timeout < 0 || timeout > 1000000000)
        {
             if (tc.isEventEnabled()) Tr.event(tc, "Timeout value is out of range - throwing RLSTimeoutRangeException", new Integer(timeout));
             if (tc.isEntryEnabled()) Tr.exit(tc, "suspend", "RLSTimeoutRangeException");
        	throw new RLSTimeoutRangeException();
        }
        
        RLSSuspendToken token = null;
        
        synchronized(SUSPEND_LOCK)
        {
        	if(!Configuration._isSnapshotSafe)
        	{
        		// If the server is not configured to be snapshot safe
        		// then we should put out a warning message
        		Tr.warning(tc, "CWRLS0020_SNAPSHOT_SAFE");
        	}
        	
            // Notify the token manager that the RLS has been called to suspend
            token = _tokenManager.registerSuspend(timeout);
            
            Tr.info(tc, "CWRLS0001_SUSPEND_RLS", token);
                
            // RLS has not already been suspended
            // Set suspended flag true
            _isSuspended = true;
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "suspendRLS", token);
        
        // Return the RLSSuspendToken
        return token;
    }
    
    //------------------------------------------------------------------------------
    // Method: RecoveryLogService.resume     
    //------------------------------------------------------------------------------
    /**
     * Resume i/o to the recovery log files
     */
    private static void resumeRLS(RLSSuspendToken token) throws  RLSInvalidSuspendTokenException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "resumeRLS", token);
        
        synchronized(SUSPEND_LOCK)
        {
            try
            {
            	_tokenManager.registerResume(token);
            }
            catch (RLSInvalidSuspendTokenException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.resume", "120");
                if (tc.isEventEnabled()) Tr.event(tc, "Rethrow RLSInvalidSuspendTokenException - attempt to resume an invalid token", token);
                if (tc.isEntryEnabled()) Tr.exit(tc, "resumeRLS", exc);
                throw exc;
            }
                
            // Check if there are any outstanding suspends
            if (_tokenManager.isResumable())
            {
                if (tc.isEventEnabled()) Tr.event(tc, "There are no outstanding suspends - resuming recovery log service");
                
                Tr.info(tc, "CWRLS0002_RESUME_RLS", token);
                
                _isSuspended = false;
                
                // Notify all waiting threads that
                // normal service is resumed
                SUSPEND_LOCK.notifyAll();
            }
            else
            {
            	Tr.info(tc, "CWRLS0021_RESUME_ATTEMPT_RLS", token);
                if (tc.isEventEnabled()) Tr.event(tc, "There are still outstanding suspensions - recovery log service remains suspended");   
            }
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "resumeRLS");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLogService.isSuspended     
    //------------------------------------------------------------------------------
    /**
    * Returns true if recovery log file i/o is suspended,
    * false otherwise
    *
    * @return boolean isSuspended
    */
    static boolean isSuspended()
    {
        return _isSuspended;
    }
    
    //------------------------------------------------------------------------------
    // Method: RecoveryLogService.notifyTimeout     
    //------------------------------------------------------------------------------
    /**
    * Called by the RLSSuspendTokenManager in the event of 
    * a suspension timeout occurring
    * 
    * We need to check, if as a result of a suspension timing out,
    * whether we should resume the RLS.
    * 
    * We will resume only if there are no other outstanding active suspensions
    * 
    * @return boolean isSuspended
    */
    static void notifyTimeout()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "notifyTimeout");
        
        synchronized(SUSPEND_LOCK)
        {     
            // Check if there are any outstanding suspends
            if (_tokenManager.isResumable())
            {
                if (tc.isEventEnabled()) Tr.event(tc, "Resuming recovery log service following a suspension timeout");
               
                _isSuspended = false;
                
                Tr.info(tc, "CWRLS0023_RESUME_RLS");
                
                // Notify all waiting threads that
                // normal service is resumed
                SUSPEND_LOCK.notifyAll();
            }
           
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "notifyTimeout");
    }
}
