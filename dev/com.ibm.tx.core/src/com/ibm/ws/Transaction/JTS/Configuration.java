package com.ibm.ws.Transaction.JTS;

/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.SystemException;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.impl.FailureScopeController;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;
import com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl;
import com.ibm.ws.recoverylog.spi.RecoveryLogManager;

public final class Configuration
{
    private static final TraceComponent tc = Tr.register(Configuration.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static String serverName;
    private static byte[] applId;
    private static int currentEpoch = 1;

    private static RecoveryLogManager _logManager;
    private static ClassLoader _classLoader;

    private static FailureScopeController _failureScopeController;

    /**
     * Sets the name of the server.
     * 
     * @param name The server name. Non-recoverable servers have null.
     */
    public static final void setServerName(String name)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setServerName", name);

        // Store the server name.
        serverName = name;
    }

    /**
     * Returns the name of the server.
     * <p>
     * Non-recoverable servers may not have a name, in which case the method returns
     * null.
     * 
     * @return The server name.
     */
    public static final String getServerName()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getServerName", serverName);
        return serverName;
    }

    /**
     * Determines whether the JTS instance is recoverable.
     * 
     * @return Indicates whether the JTS is recoverable.
     */
    public static final boolean isRecoverable()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isRecoverable");

        // This JTS is recoverable if there is a server name.
        // boolean result = (serverName != null);
        // JTA2 - we are recoverable if we have a working log...
        // We can have a serverName but no working log either because
        // a) the log config or store is invalid
        // b) the log config indicates no logging.
        //
        boolean result = false;
        if (_failureScopeController != null)
        {
            result = (_failureScopeController.getTransactionLog() != null);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isRecoverable", Boolean.valueOf(result));
        return result;
    }

    /**
     * Sets the current epoch value for this server instance.
     * 
     * Initially on a cold start the valus is 1, and this is
     * incremented on each warm start after extracting the previous
     * value from the transactions log. The epoch value is used to
     * create unique global transaction identifiers. On each cold
     * start we also create a new applId, so the applid and epoch
     * will guarantee uniqueness of a server instance.
     * 
     * @param number The new retry count.
     */
    public static final void setCurrentEpoch(int number)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setCurrentEpoch", number);

        currentEpoch = number;
    }

    /**
     * Returns the current epoch value for this server instance.
     * 
     * @return int value.
     */
    public static final int getCurrentEpoch()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getCurrentEpoch", currentEpoch);
        return currentEpoch;
    }

    /**
     * Sets the applId of the server.
     * 
     * @param name The applId. Non-recoverable servers may have an applId but no name.
     */
    public static final void setApplId(byte[] name)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setApplId", name);

        // Store the applId.
        applId = name;
    }

    /**
     * Returns the applId of the server.
     * <p>
     * Non-recoverable servers may have an applid but not a name.
     * 
     * @return The applId of the server.
     */
    public static final byte[] getApplId()
    {
        // Determine the applId.
        final byte[] result = applId;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getApplId", result);
        return result;
    }

    public static void setLogManager(RecoveryLogManager logManager)
    {
        _logManager = logManager;
    }

    public static RecoveryLogManager getLogManager()
    {
        return _logManager;
    }

    public static void setFailureScopeController(FailureScopeController fsm)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setFailureScopeController", fsm);
        _failureScopeController = fsm;
    }

    public static FailureScopeController getFailureScopeController()
    {
        try
        {
            if (_failureScopeController == null)
            {
                final RecoveryDirector recoveryDirector = RecoveryDirectorImpl.instance();
                if (recoveryDirector != null)
                {
                    final FailureScope currentFailureScope = recoveryDirector.currentFailureScope();
                    final FailureScopeController fsc = new FailureScopeController(currentFailureScope);
                    setFailureScopeController(fsc);
                }
            }
        } catch (SystemException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getFailureScopeController", _failureScopeController);
        return _failureScopeController;
    }
}