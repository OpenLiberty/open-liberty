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
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.websphere.csi.LocalTranConfigData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class is an implementation of GlobalTranConfigData that does not
 * depend on WCCM.
 */
public class BasicLocalTranConfigDataImpl
                implements LocalTranConfigData, LocalTransactionSettings
{
    private static final TraceComponent tc = Tr.register(BasicLocalTranConfigDataImpl.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    protected int boundary = LocalTranConfigData.BOUNDARY_BEAN_METHOD;
    protected int resolver = LocalTranConfigData.RESOLVER_APPLICATION;
    protected int unresolvedAction = LocalTranConfigData.UNRESOLVED_ROLLBACK;
    protected boolean isShareable = false;

    /**
     * Default constructor that is intended to be used by
     * a DefaultComponentMetaData object. Default values is used
     * for all config data.
     */
    public BasicLocalTranConfigDataImpl()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
    }

    // F743-14912
    /**
     * Creates a <code>LocalTranConfigData</code> with the specified resolver
     * and unresolvedAction settings.
     *
     * Big WAS does this using the WCCM bindings/extension data. However, the
     * WCCM bindings/extension data is not available in all cases (such as embeddable),
     * and so the needed LTC related information is passed in directly instead.
     *
     * @param resolver Represents the LTC resolver setting. It must be one of
     *            <code>LocalTranConfigData.RESOLVER_APPLICATION</code> or
     *            <code>LocalTranConfigData.RESOLVER_CONTAINER_AT_BOUNDARY</code>.
     *
     * @param unresolvedAction Represents the LTC unresolvedAction setting. It
     *            must be one of <code>LocalTranConfigData.UNRESOLVED_COMMIT</code>
     *            or <code>LocalTranConfigData.UNRESOLVED_ROLLBACK</code>.
     */
    public BasicLocalTranConfigDataImpl(int resolver, int unresolvedAction)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
        {
            Tr.debug(tc, "<init> with resolver " + resolver + " and unresolvedAction " + unresolvedAction);
        }
        this.resolver = resolver;
        this.unresolvedAction = unresolvedAction;
    }

    @Override
    public int getValueBoundary()
    {
        return boundary;
    }

    @Override
    public int getBoundary()
    {
        return boundary;
    }

    @Override
    public int getValueResolver()
    {
        return resolver;
    }

    @Override
    public int getResolver()
    {
        return resolver;
    }

    @Override
    public int getValueUnresolvedAction()
    {
        return unresolvedAction;
    }

    @Override
    public int getUnresolvedAction()
    {
        return unresolvedAction;
    }

    @Override
    public boolean isShareable()
    {
        return isShareable;
    }

    @Override
    public String toString()
    {
        String separator = AccessController.doPrivileged(new SystemGetPropertyPrivileged("line.separator", "\n"));
        String sep = "                                 ";

        StringBuffer sb = new StringBuffer();
        sb.append(separator).append(sep).append("      ****** LOCAL-TRANSACTION *******");

        sb.append(separator).append(sep).append("Boundary=");
        if (boundary == LocalTranConfigData.BOUNDARY_ACTIVITY_SESSION)
        {
            sb.append("ACTIVITY");
        }
        else if (boundary == LocalTranConfigData.BOUNDARY_BEAN_METHOD)
        {
            sb.append("BEAN_METHOD");
        }
        else
        {
            sb.append("UNKNOWN");
        }

        sb.append(separator).append(sep).append("Resolver=");
        if (resolver == LocalTranConfigData.RESOLVER_APPLICATION)
        {
            sb.append("APPLICATION");
        }
        else if (resolver == LocalTranConfigData.RESOLVER_CONTAINER_AT_BOUNDARY)
        {
            sb.append("CONTAINER_AT_BOUNDARY");
        }
        else
        {
            sb.append("UNKNOWN");
        }

        sb.append(separator).append(sep).append("UnResolvedAction=");
        if (unresolvedAction == LocalTranConfigData.UNRESOLVED_ROLLBACK)
        {
            sb.append("ROLLBACK");
        }
        else if (unresolvedAction == LocalTranConfigData.UNRESOLVED_COMMIT)
        {
            sb.append("COMMIT");
        }
        else
        {
            sb.append("UNKNOWN");
        }

        sb.append(separator).append(sep).append("isShareable=");
        if (isShareable)
        {
            sb.append("TRUE");
        }
        else
        {
            sb.append("FALSE");
        }

        return sb.toString();
    }
}
