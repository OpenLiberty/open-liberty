/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.impl;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.jboss.weld.module.web.servlet.WeldTerminalListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.web.interfaces.CDIWebRuntime;
import com.ibm.ws.cdi.web.interfaces.PostEventListenerProvider;
import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * Register WeldTeminalListener on the servlet context. This listener needs to be the last HttpSessionlistener.
 */
public abstract class AbstractTerminalListenerRegistration implements PostEventListenerProvider {

// because we use a package-info.java for trace options, just need this to register our group and message file
    private static final TraceComponent tc = Tr.register(AbstractTerminalListenerRegistration.class);

    protected abstract CDIWebRuntime getCDIWebRuntime();

    /** {@inheritDoc} */
    @Override
    public void registerListener(IServletContext isc) {

        CDIWebRuntime cdiWebRuntime = getCDIWebRuntime();
        if (cdiWebRuntime != null && cdiWebRuntime.isCdiEnabled(isc)) {

            BeanManager beanManager = cdiWebRuntime.getCurrentBeanManager();
            if (beanManager != null) {

                /*
                 * Workaround jira https://issues.jboss.org/browse/WELD-1874
                 * To make sure that the WeldTerminalListener has the correct beanManager we
                 * have to pass a BeanManagerImpl into the constructor, however we do not
                 * know if we have a BeanManagerImpl or a BeanManagerProxy.
                 */
                BeanManagerImpl beanManagerImpl = null;

                if (beanManager instanceof BeanManagerProxy) {
                    BeanManagerProxy proxy = (BeanManagerProxy) beanManager;
                    beanManagerImpl = proxy.delegate();
                } else if (beanManager instanceof BeanManagerImpl) {
                    beanManagerImpl = (BeanManagerImpl) beanManager;
                } else {
                    throw new RuntimeException("Unexpected beanManager instance.");
                }

                isc.addListener(new WeldTerminalListener(beanManagerImpl));
                //End of workaround.

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "added WeldTerminalListener to the servlet context");
            }

        }

    }

    /*
     * This method is called just before the AsyncListener onComplete. onError or onTimeout
     * methods are called. It registers an AsyncListener which will be run after
     * any application AsyncListenes.
     */
    @Override
    public void registerListener(IServletContext isc, AsyncContextImpl ac) {
        Object obj = isc.getAttribute(AbstractInitialListenerRegistration.WELD_INITIAL_LISTENER_ATTRIBUTE);
        if (obj != null) {
            WeldInitialListener wl = (WeldInitialListener) obj;
            WeldTerminalAsyncListener asyncListener = new WeldTerminalAsyncListener(wl, isc);
            ac.addListener(asyncListener, ac.getIExtendedRequest(), ac.getIExtendedResponse());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "added WeldInitialAsyncListener to the asyncContext");
        }
    }
}
