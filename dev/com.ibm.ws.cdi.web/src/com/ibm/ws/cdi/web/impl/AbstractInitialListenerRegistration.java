/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.impl;

import java.util.EventListener;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletRequestListener;

import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.manager.BeanManagerImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.web.factories.WeldListenerFactory;
import com.ibm.ws.cdi.web.interfaces.CDIWebConstants;
import com.ibm.ws.cdi.web.interfaces.CDIWebRuntime;
import com.ibm.ws.cdi.web.interfaces.PreEventListenerProvider;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * Register WeldInitalListener on the servlet context. This listener needs to be the first listener.
 */
public abstract class AbstractInitialListenerRegistration implements PreEventListenerProvider {

    public static final String CONVERSATION_FILTER_REGISTERED = WeldListenerFactory.getConversationFilter().getName() + ".registered";
// because we use a package-info.java for trace options, just need this to register our group and message file
    private static final TraceComponent tc = Tr.register(AbstractInitialListenerRegistration.class);

    public static final String WELD_INITIAL_LISTENER_ATTRIBUTE = "org.jboss.weld.servlet.WeldInitialListener";

    protected abstract ModuleMetaData getModuleMetaData(IServletContext isc);

    protected abstract CDIWebRuntime getCDIWebRuntime();

    /** {@inheritDoc} */
    @Override
    public void registerListener(IServletContext isc) {

        CDIWebRuntime cdiWebRuntime = getCDIWebRuntime();
        if (cdiWebRuntime != null && cdiWebRuntime.isCdiEnabled(isc)) {
            ModuleMetaData moduleMetaData = getModuleMetaData(isc);
            BeanManager beanManager = cdiWebRuntime.getModuleBeanManager(moduleMetaData);
            if (beanManager != null) {
                // check to see if the ConversationFilter is mapped.  If so we need to set a context init property
                // to prevent WeldInitialListener from doing conversation activation
                List<IFilterMapping> filterMappings = isc.getWebAppConfig().getFilterMappings();
                for (IFilterMapping filterMapping : filterMappings) {
                    IFilterConfig filterConfig = filterMapping.getFilterConfig();
                    if (CDIWebConstants.CDI_CONVERSATION_FILTER.equals(filterConfig.getFilterName())) {
                        isc.setInitParameter(CONVERSATION_FILTER_REGISTERED, Boolean.TRUE.toString());
                    }
                }
                /*
                 * Workaround jira https://issues.jboss.org/browse/WELD-1874
                 * To make sure that the WeldInitialListener has the correct beanManager we
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
                EventListener weldInitialListener = WeldListenerFactory.newWeldInitialListener(beanManagerImpl);
                isc.addListener(weldInitialListener);
                isc.setAttribute(WELD_INITIAL_LISTENER_ATTRIBUTE, weldInitialListener);
                //End of workaround

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "added WeldInitialListener to the servlet context");

                //Put bean manager on the servlet context
                isc.setAttribute(BeanManager.class.getName(), beanManager);
            }

        }

    }

    /*
     * This method is called just before the first AsyncListener is registered for
     * an Async Servlet Request. It registers an async listener which will be run before
     * any application AsyncListenes.
     */
    @Override
    public void registerListener(IServletContext isc, AsyncContextImpl ac) {
        Object obj = isc.getAttribute(WELD_INITIAL_LISTENER_ATTRIBUTE);
        if (obj != null) {
            ServletRequestListener wl = (ServletRequestListener) obj;
            WeldInitialAsyncListener asyncListener = new WeldInitialAsyncListener(wl, isc);
            ac.addListener(asyncListener, ac.getIExtendedRequest(), ac.getIExtendedResponse());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "added WeldInitialAsyncListener to the asyncContext");
        }
    }

}
