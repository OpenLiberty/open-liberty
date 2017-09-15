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
* <p>
* This class provides the ability to suspend and resume logging through 
* the Recovery Log Service, allowing for the safe snapshotting of all 
* Recovery Log Service log files.
* </p>
*
* <p>
* When called to suspend by a client, an RLSSuspendToken is returned, and 
* if not already suspended, the Recovery Log Service will be suspended.
* </p>
*
* <p>
* When called to resume, the client must pass in the previously returned RLSSuspendToken.
* The Recovery Log Service will only be resumed when there are no outstanding RLSSuspendTokens
* i.e. for every suspend call, there must be a corresponding resume call OR the RLSSuspendToken
* has timed out.
* </p>
*
*/            
public class RecoveryLogService {

    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(RecoveryLogService.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);
    
    private static RLSController _rlsController;
    
	/**
     * Constructor declared private to prevent public instance creation
     */
    private RecoveryLogService() 
    {
    	 if (tc.isEntryEnabled()) Tr.entry(tc, "RecoveryLogService");
    	 if (tc.isEntryEnabled()) Tr.exit(tc, "RecoveryLogService", this);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLogService.suspend     
    //------------------------------------------------------------------------------
    /**
     * Suspend i/o to the recovery log files
     * 
     * @param timeout value in seconds after which this suspend operation will be cancelled.
     * A timeout value of zero indicates no timeout
     * 
     * @exception RLSTimeoutRangeException Thrown if timeout is not in the range 0 < timeout <= 1,000,000,000.
     *    
     * @exception RecoveryLogServiceException Thrown if RecoveryLogService failed to suspend
     */
    public static RLSSuspendToken suspend(int timeout) throws RLSTimeoutRangeException, RecoveryLogServiceException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "suspend", new Integer(timeout));
    
        RLSSuspendToken token = null;
        
        try
		{
        	token = getRLSController().suspend(timeout);
		}
        catch (RLSTimeoutRangeException exc)
		{
        	FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.suspend", "53");
            if (tc.isEventEnabled()) Tr.event(tc, "Rethrow RLSTimeoutRangeException");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "suspend");
        	throw exc;
		}
        catch (RecoveryLogServiceException exc)
		{
        	FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.suspend", "63");
            if (tc.isEventEnabled()) Tr.event(tc, "RecoveryLogServiceException caught trying to suspend RLS - rethrowing");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "suspend");
        	throw exc;	
		}
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "suspend", token);
        
        return token;
    }
    
    //------------------------------------------------------------------------------
    // Method: RecoveryLogService.resume     
    //------------------------------------------------------------------------------
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
    public static void resume(RLSSuspendToken token) throws  RLSInvalidSuspendTokenException, RecoveryLogServiceException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "resume", token);
        
        try
		{
        	getRLSController().resume(token);
		}
        catch (RLSInvalidSuspendTokenException exc)
		{
        	FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.resume", "81");
            if (tc.isEventEnabled()) Tr.event(tc, "Rethrow RLSInvalidSuspendTokenException");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
        	throw exc;
		}
        catch (RecoveryLogServiceException exc)
		{
        	FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.suspend", "91");
            if (tc.isEventEnabled()) Tr.event(tc, "RecoveryLogServiceException caught trying to resume RLS - rethrowing");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
        	throw exc;	
		}
		
        if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
    }
    
    //------------------------------------------------------------------------------
    // Method: RecoveryLogService.resume     
    //------------------------------------------------------------------------------
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
     * @exception RecoveryLogServiceException Thrown if RecoveryLogService failed to resume
     */
    public static void resume(byte[] tokenBytes) throws  RLSInvalidSuspendTokenException, RecoveryLogServiceException
    {
    	if (tc.isEntryEnabled()) Tr.entry(tc, "resume", RLSUtils.toHexString(tokenBytes));
        
        try
		{
        	getRLSController().resume(tokenBytes);
		}
        catch (RLSInvalidSuspendTokenException exc)
		{
        	FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.resume", "81");
            if (tc.isEventEnabled()) Tr.event(tc, "Rethrow RLSInvalidSuspendTokenException");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
        	throw exc;
		}
        catch (RecoveryLogServiceException exc)
		{
        	FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.suspend", "91");
            if (tc.isEventEnabled()) Tr.event(tc, "RecoveryLogServiceException caught trying to resume RLS - rethrowing");
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
        	throw exc;	
		}
		
        if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
    }
    
    /**
     * Returns the singleton RLSController to which calls to suspend and resume are
     * delegated.
     * 
     */
    private static RLSController getRLSController() throws RecoveryLogServiceException
	{
    	if (tc.isEntryEnabled()) Tr.entry(tc, "getRLSController");
        
    	RLSController retVal = null;
    	
    	if (_rlsController == null)
    	{
    		try
			{
    			retVal = RLSControllerFactory.getRLSController();
			}
    		catch (Exception exc)
			{
            	FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogService.getRLSController", "115");
                if (tc.isEventEnabled()) Tr.event(tc, "Rethrow as RecoveryLogServiceException");
            	if (tc.isEntryEnabled()) Tr.exit(tc, "getRLSController");
            	throw new RecoveryLogServiceException(exc);
			}
    	}
    	
    	if (tc.isEntryEnabled()) Tr.exit(tc, "getRLSController", retVal);
    	
    	return retVal;
	}
}
