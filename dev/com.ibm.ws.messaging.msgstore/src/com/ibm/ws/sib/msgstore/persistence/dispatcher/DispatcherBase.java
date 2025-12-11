/* ==============================================================================
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ==============================================================================
 */
package com.ibm.ws.sib.msgstore.persistence.dispatcher;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.Dispatcher;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This is the abstract base class of the Dispatchers. They're ripe for
 * refactoring to share more of the logic between them.
 */
public abstract class DispatcherBase implements Dispatcher
{
    private static TraceComponent tc = SibTr.register(DispatcherBase.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    /**
     * Obtains the value of an integer configuration parameter given its name, the default value
     * and 'reasonable' minimum and maximum values.
     * @param msi The Message Store instance to obtain the parameters (may be null)
     * @param parameterName The parameter's name
     * @param defaultValue The default value
     * @param minValue A reasonable minimum value
     * @param maxValue A reasonable maximum value
     */
    protected static int obtainIntConfigParameter(MessageStoreImpl msi, String parameterName, String defaultValue, int minValue, int maxValue)
    {
        int value = Integer.parseInt(defaultValue);

        if (msi != null)
        {        
            String strValue = msi.getProperty(parameterName, defaultValue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(tc, parameterName + "=" + strValue);
            }; // end if
    
            try
            {
                value = Integer.parseInt(strValue);
                if ((value < minValue) || (value > maxValue))
                {
                    value = Integer.parseInt(defaultValue); 
    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        SibTr.debug(tc, "OVERRIDE: " + parameterName + "=" + strValue);
                    }; // end if
                }; // end if
            }
            catch (NumberFormatException nfexc)
            {
                //No FFDC Code Needed.
            }
        }; // end if

        return value;
    }


    /**
     * Obtains the value of a long integer configuration parameter given its name, the default value
     * and 'reasonable' minimum and maximum values.
     * @param msi The Message Store instance to obtain the parameters (may be null)
     * @param parameterName The parameter's name
     * @param defaultValue The default value
     * @param minValue A reasonable minimum value
     * @param maxValue A reasonable maximum value
     */
    protected static long obtainLongConfigParameter(MessageStoreImpl msi, String parameterName, String defaultValue, long minValue, long maxValue)
    {
        long value = Long.parseLong(defaultValue);
        
        if (msi != null)
        {
            String strValue = msi.getProperty(parameterName, defaultValue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(tc, parameterName + "=" + strValue);
            }; // end if
    
            try
            {
                value = Long.parseLong(strValue);
                if ((value < minValue) || (value > maxValue))
                {
                    value = Long.parseLong(defaultValue); 
    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        SibTr.debug(tc, "OVERRIDE: " + parameterName + "=" + strValue);
                    }; // end if
                }; // end if
            }
            catch (NumberFormatException nfexc)
            {
                //No FFDC Code Needed.
            }
        }; // end if

        return value;
    }

    @Deprecated
    public final void stop(int mode) {
        stop(mode, null);
    }
}
