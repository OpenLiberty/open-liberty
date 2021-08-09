/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.Transaction.test;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public final class XAFlowCallbackControl {
    private static XAFlowCallback _callback;

    private static boolean _enabled;

    private static final TraceComponent tc = Tr.register(XAFlowCallbackControl.class,
                                                         TranConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

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
    public static void initialize() {
        if (System.getProperty("com.ibm.ws.Transaction.fvt") != null) {
            if (!initialize("com.ibm.ws.Transaction.test.impl.XAFlowCallbackImpl")) {
                initialize("com.ibm.ws.tx.test.impl.XAFlowCallbackImpl");
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAFlowCallbacks not enabled");
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean initialize(String classToLoad) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initialize", classToLoad);

        try {
            // Load using classloader that loaded this class, ie eclipse in case fvt tests in bundle
            _callback = (XAFlowCallback) Class.forName(classToLoad).newInstance();
            _enabled = true;
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Couldn't load " + classToLoad + " using default classloader: " + e.getLocalizedMessage());
        }

        try {
            if (!_enabled) {
                // Load using extension class loader in case fvt tests not in a bundle
                _callback = (XAFlowCallback) Class.forName(classToLoad, true, Thread.currentThread().getContextClassLoader()).newInstance();
                _enabled = true;
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Couldn't load " + classToLoad + " using " + Thread.currentThread().getContextClassLoader() + ": " + e.getLocalizedMessage());
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initialize", _enabled);
        return _enabled;
    }

    /**
     * This method will be used to wrapper any calls to the XAFlow callback mechanism
     * thus hopefully allowing a high performance route for the JIT to optimize when
     * isEnabled returns false.
     *
     * @return Whether the XAFlow callback class is present and therefore whether respective
     *         calls should be made.
     */
    public static final boolean isEnabled() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isEnabled", _enabled);
        }

        return _enabled;
    }

    public static boolean beforeXAFlow(int flowType, int flag) {
        if (tc.isEntryEnabled()) {
            StringBuffer params = new StringBuffer("Flow type=");

            switch (flowType) {
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

            switch (flag) {
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

    public static boolean afterXAFlow(int flowType, int flag) {
        if (tc.isEntryEnabled()) {
            StringBuffer params = new StringBuffer("Flow type=");

            switch (flowType) {
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

            switch (flag) {
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
