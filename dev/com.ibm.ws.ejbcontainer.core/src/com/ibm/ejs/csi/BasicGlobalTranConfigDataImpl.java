/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import java.security.AccessController;

import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
import com.ibm.tx.jta.embeddable.GlobalTransactionSettings;
import com.ibm.websphere.csi.GlobalTranConfigData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class is an implementation of GlobalTranConfigData that does not
 * depend on WCCM.
 */
public class BasicGlobalTranConfigDataImpl
                implements GlobalTranConfigData, GlobalTransactionSettings
{
    private static final TraceComponent tc = Tr.register(BasicGlobalTranConfigDataImpl.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    // matched the default componentTransactionTimeout value as specified in
    //  com.ibm.ejs.models.base.extensions.commonext.globaltran.impl.GlobalTransactionImpl
    protected int timeout = 0;
    protected boolean isSendWSAT = false;

    /**
     * Default constructor that is intended to be used by
     * a DefaultComponentMetaData object. Default values is used
     * for all config data.
     */
    public BasicGlobalTranConfigDataImpl()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
    }

    @Override
    public int getTransactionTimeout()
    {
        return timeout;
    }

    @Override
    public boolean isSendWSAT()
    {
        return isSendWSAT;
    }

    @Override
    public String toString()
    {
        String separator = AccessController.doPrivileged(new SystemGetPropertyPrivileged("line.separator", "\n"));
        String sep = "                                 ";

        StringBuilder sb = new StringBuilder();
        sb.append(separator).append(sep).append("      ****** GLOBAL-TRANSACTION *******");
        sb.append(separator).append(sep).append("Timeout=").append(timeout);
        sb.append(separator).append(sep).append("isSendWSAT=").append(isSendWSAT);

        return sb.toString();
    }
}
