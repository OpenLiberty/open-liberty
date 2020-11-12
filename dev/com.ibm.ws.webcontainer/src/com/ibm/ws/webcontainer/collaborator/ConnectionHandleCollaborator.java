/*******************************************************************************
 * Copyright (c) 2005,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.collaborator;

import org.osgi.framework.ServiceReference;

import com.ibm.ejs.j2c.HandleList;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.handle.HandleListInterface;
import com.ibm.ws.threadContext.ConnectionHandleAccessorImpl;
import com.ibm.ws.threadContext.ThreadContext;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.collaborator.IConnectionCollaborator;

/**
 * Connection Handle Collaborator
 */
public class ConnectionHandleCollaborator implements IConnectionCollaborator{
    TraceComponent tc = Tr.register(ConnectionHandleCollaborator.class);

    private final ThreadContext<HandleListInterface> threadContext;

    @SuppressWarnings("unchecked")
    public ConnectionHandleCollaborator() {
        // get the thread context
        ThreadContext<?> thctx = ConnectionHandleAccessorImpl.getConnectionHandleAccessor().getThreadContext();
        threadContext = (ThreadContext<HandleListInterface>) thctx;
    }

    /*
     * preinvoke
     *
     */
    public void preInvoke(HandleList hl, boolean singleThread) throws CSIException
    {
        // if it's a single thread servlet, then we can reuse handles
        if (singleThread || WCCustomProperties.DISABLE_MULTI_THREAD_CONN_MGMT)
            hl.reAssociate();

        threadContext.beginContext(hl);
    }


    /*
     * postinvoke
     *
     */
    public void postInvoke(HandleList hl, boolean singleThread) throws CSIException
    {
        // take the handle list off the thread context
        HandleList threadHandleList = (HandleList)threadContext.endContext();

        // make sure our handle lists match
        if (threadHandleList != hl)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "postInvoke", "unexpected - web app request dispatcher handleList context doesn't match thread handleList context " + hl);
        }
        else
        {
            // if it's a single thread servlet, then we can reuse handles
            if (singleThread || WCCustomProperties.DISABLE_MULTI_THREAD_CONN_MGMT)
            {
                // process handle list
                try
                {
                    threadHandleList.parkHandle();
                }
                catch (RuntimeException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    FFDCFilter.processException(e, getClass().getName(), "95", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        Tr.debug(this, tc, "postInvoke", "unexpected - error manipulating connection handles. See any previous errors related to Managed Connection.");
                    }

                    // Eat exception. ConnectionManager will log an Error Message. Re-Throwing
                    //at this point may cause container to "rollback". Client will probably get
                    //exception later when trying to re-use handle - possibly in the pre-invoke
                }
            }
            else
            {
                // multi thread...force a drop of the handles
                threadHandleList.close();
            }
        }
    }
}
