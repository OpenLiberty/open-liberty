package com.ibm.ws.Transaction.test;

/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.TranConstants;

public final class XAFlowCallbackControl
{
    private static XAFlowCallback _callback;

    private static boolean _enabled = false;

    private static final TraceComponent tc = Tr.register(XAFlowCallbackControl.class,
                                                         TranConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

    private static final String _classToLoad = "com.ibm.ws.Transaction.test.impl.XAFlowCallbackImpl";

    /**
     * This method will attempt to load the test callback class
     * if the process has been started with the JVM property
     * "-Dcom.ibm.ws.Transaction.fvt=true" e.g. via setting
     * genericJvmArguments="-Dcom.ibm.ws.Transaction.fvt=true"
     * in server.xml jvmEntries
     * 
     * In case of any exceptions or errors we carry on regardless
     * and do not let it affect startup of the server.
     */
    public static void initialize()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        if (System.getProperty("com.ibm.ws.Transaction.fvt") != null)
        {
            try
            {
                // Load using classloader that loaded this class, ie eclipse in case fvt tests in bundle
                _callback = (XAFlowCallback) Class.forName(_classToLoad).newInstance();

                _enabled = true;
            } catch (ClassNotFoundException cnfe)
            {
                // No FFDC Code needed.
                // Normal case is not to load this class - it is only used by test groups
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught loading XAFlowCallbackImpl class", cnfe);
            } catch (Exception e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.Transaction.test.XAFlowCallbackControl", "56");
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught loading XAFlowCallbackImpl class", e);
            }

            try
            {
                if (!_enabled)
                {
                    // Load using extension class loader in case fvt tests not in a bundle
                    _callback = (XAFlowCallback) Class.forName(_classToLoad, true, Thread.currentThread().getContextClassLoader()).newInstance();

                    _enabled = true;
                }
            } catch (ClassNotFoundException cnfe)
            {
                // No FFDC Code needed.
                // Normal case is not to load this class - it is only used by test groups
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught loading XAFlowCallbackImpl class", cnfe);
            } catch (Exception e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.Transaction.test.XAFlowCallbackControl", "92");
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught loading XAFlowCallbackImpl class", e);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initialize");
    }

    /**
     * This method will be used to wrapper any calls to the XAFlow callback mechanism
     * thus hopefully allowing a high performance route for the JIT to optimize when
     * isEnabled returns false.
     * 
     * @return Whether the XAFlow callback class is present and therefore whether respective
     *         calls should be made.
     */
    public static final boolean isEnabled()
    {
        if (tc.isEntryEnabled())
        {
            Tr.entry(tc, "isEnabled");

            Tr.exit(tc, "isEnabled", new Boolean(_enabled));
        }

        return _enabled;
    }

    public static boolean beforeXAFlow(int flowType, int flag)
    {
        if (tc.isEntryEnabled())
        {
            StringBuffer params = new StringBuffer("Flow type=");

            switch (flowType)
            {
                case XAFlowCallback.FORGET:
                    params.append("FORGET, Flag=");
                    break;
                case XAFlowCallback.PREPARE:
                    params.append("PREPARE, Flag=");
                    break;
                case XAFlowCallback.COMMIT:
                    params.append("COMMIT, Flag=");
                    break;
                case XAFlowCallback.ROLLBACK:
                    params.append("ROLLBACK, Flag=");
                    break;
                default:
                    params.append("UNKNOWN, Flag=");
            }

            switch (flag)
            {
                case XAFlowCallback.FORGET_NORMAL:
                    params.append("FORGET_NORMAL");
                    break;
                case XAFlowCallback.PREPARE_NORMAL:
                    params.append("PREPARE_NORMAL");
                    break;
                case XAFlowCallback.PREPARE_1PC_OPT:
                    params.append("PREPARE_1PC_OPT");
                    break;
                case XAFlowCallback.COMMIT_2PC:
                    params.append("COMMIT_2PC");
                    break;
                case XAFlowCallback.COMMIT_1PC_OPT:
                    params.append("COMMIT_1PC_OPT");
                    break;
                case XAFlowCallback.ROLLBACK_NORMAL:
                    params.append("ROLLBACK_NORMAL");
                    break;
                case XAFlowCallback.ROLLBACK_DUE_TO_ERROR:
                    params.append("ROLLBACK_DUE_TO_ERROR");
                    break;
                default:
                    params.append("UNKNOWN");
            }

            Tr.entry(tc, "beforeXAFlow", params.toString());
        }

        boolean retval = _callback.beforeXAFlow(flowType, flag);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "beforeXAFlow", "return=" + retval);
        return retval;
    }

    public static boolean afterXAFlow(int flowType, int flag)
    {
        if (tc.isEntryEnabled())
        {
            StringBuffer params = new StringBuffer("Flow type=");

            switch (flowType)
            {
                case XAFlowCallback.FORGET:
                    params.append("FORGET, Flag=");
                    break;
                case XAFlowCallback.PREPARE:
                    params.append("PREPARE, Flag=");
                    break;
                case XAFlowCallback.COMMIT:
                    params.append("COMMIT, Flag=");
                    break;
                case XAFlowCallback.ROLLBACK:
                    params.append("ROLLBACK, Flag=");
                    break;
                default:
                    params.append("UNKNOWN, Flag=");
            }

            switch (flag)
            {
                case XAFlowCallback.AFTER_SUCCESS:
                    params.append("AFTER_SUCCESS");
                    break;
                case XAFlowCallback.AFTER_FAIL:
                    params.append("AFTER_FAIL");
                    break;
                default:
                    params.append("UNKNOWN");
            }

            Tr.entry(tc, "afterXAFlow", params.toString());
        }

        boolean retval = _callback.afterXAFlow(flowType, flag);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "afterXAFlow", "return=" + retval);
        return retval;
    }
}
