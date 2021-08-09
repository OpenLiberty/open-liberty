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
package com.ibm.ws.webcontainer31.osgi.listener;


import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;


/**
 *  This class listens for registration of PreEventListenerProvider and PostEventListenerProvider services.
 *  
 *  During application start:
 *  PreEventListenerProviders are notified to register a listener, for a servlet context, before all other listeners.
 *  PostEventListenerProviders are notified to register a listener, of a servlet context, after all other listeners. 
 *    
 */
@Component(name = "com.ibm.ws.webcontainer31.osgi.listener.RegisterEventListenerProvider",
           property = { "service.vendor=IBM"})
public class RegisterEventListenerProvider {
    
    private final static TraceComponent tc = Tr.register(RegisterEventListenerProvider.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    protected static final String CLASS_NAME = "com.ibm.ws.webcontainer31.osgi.listener.RegisterEventListenerProvider";

    
    // Use ConcurrentServiceReferenceSet to observe service.ranking associated with services
    private static final ConcurrentServiceReferenceSet<PreEventListenerProvider> _PreEventListenerProviders = new ConcurrentServiceReferenceSet<PreEventListenerProvider>("PreEventListenerProvider");
    private static final ConcurrentServiceReferenceSet<PostEventListenerProvider> _PostEventListenerProviders = new ConcurrentServiceReferenceSet<PostEventListenerProvider>("PostEventListenerProvider");
  
    @Activate
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterEventListenerProvider activated. context:"+context);
        }     
        _PreEventListenerProviders.activate(context);
        _PostEventListenerProviders.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "RegisterEventListenerProvider de-activated.");
        _PreEventListenerProviders.deactivate(context);
        _PostEventListenerProviders.deactivate(context);
    }

    
    @Reference(service=PreEventListenerProvider.class)
    protected void setPreEventListenerProvider(ServiceReference<PreEventListenerProvider> reference) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterEventListenerProvider.setPreEventListenerProvider(), reference : " + reference);
         }
         _PreEventListenerProviders.addReference(reference);             
    }
    
    protected void unsetPreEventListenerProvider(ServiceReference<PreEventListenerProvider> reference) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "RegisterEventListenerProvider.unsetPreEventListenerProvider(), reference:"+reference);
        _PreEventListenerProviders.removeReference(reference);
    }
    
    public static void notifyPreEventListenerProviders(IServletContext isc) {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterEventListenerProvider notifyPreEventListenerProvider(IServletContext). _PreEventListenerProviders:"+_PreEventListenerProviders);
        }
        
        Iterator<PreEventListenerProvider> pelps = _PreEventListenerProviders.getServices();
              
        while (pelps.hasNext()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegisterEventListenerProvider notifyPreEventListenerProvider(IServletContext) notify listener.");
            }            
            pelps.next().registerListener(isc);
        }

    }
    
    public static boolean notifyPreEventListenerProviders(IServletContext isc,AsyncContextImpl ac) {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterEventListenerProvider notifyPreEventListenerProvider(IServletContext,AsyncContext). _PreEventListenerProviders:"+_PreEventListenerProviders);
        }
        boolean result = false;
        Iterator<PreEventListenerProvider> pelps = _PreEventListenerProviders.getServices();
           
        while (pelps.hasNext()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegisterEventListenerProvider notifyPreEventListenerProvider(IServletContext,AsyncContext) notify listener.");
            }            
            pelps.next().registerListener(isc,ac);
            result = true;
        }

        return result;
    }
     
    @Reference(service=PostEventListenerProvider.class)
    protected void setPostEventListenerProvider(ServiceReference<PostEventListenerProvider> reference) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "RegisterEventListenerProvider.setPostEventListenerProvider(), reference:"+reference.getClass().getName());   
        _PostEventListenerProviders.addReference(reference);
    }
    
    protected void unsetPostEventListenerProvider(ServiceReference<PostEventListenerProvider> reference) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "RegisterEventListenerProvider.unsetPostEventListenerProvider(), reference:"+reference);
         _PostEventListenerProviders.removeReference(reference);
    }

    public static void notifyPostEventListenerProviders(IServletContext isc) {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterEventListenerProvider notifyPostEventListenerProvider(IServletContext). _PostEventListenerProviders:"+_PostEventListenerProviders);
        }
        
        Iterator<PostEventListenerProvider> pelps = _PostEventListenerProviders.getServices();
        ArrayList<PostEventListenerProvider> reversePelps = new ArrayList<PostEventListenerProvider>();
               
        // reverse the order so highest ranked goes last
        while (pelps.hasNext()) {
            reversePelps.add(0,pelps.next());
        }
        for (PostEventListenerProvider pelp : reversePelps) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegisterEventListenerProvider notifyPostEventListenerProvider(IServletContext) notify listener.");
            }            
            pelp.registerListener(isc);
        }
    }
    
    /*
     * returns true if a post event listener was called.
     */
    public static boolean notifyPostEventListenerProviders(IServletContext isc,AsyncContextImpl ac) {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterEventListenerProvider notifyPostEventListenerProvider(IServletContext,AsyncContext). _PostEventListenerProviders:"+_PostEventListenerProviders);
        }
        boolean result = false;
        Iterator<PostEventListenerProvider> pelps = _PostEventListenerProviders.getServices();
        ArrayList<PostEventListenerProvider> reversePelps = new ArrayList<PostEventListenerProvider>();
               
        // reverse the order so highest ranked goes last
        while (pelps.hasNext()) {
            reversePelps.add(0,pelps.next());
        }
        if (!reversePelps.isEmpty()) {
            for (PostEventListenerProvider pelp : reversePelps) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "RegisterEventListenerProvider notifyPostEventListenerProvider(IServletContext,AsyncContext) notify listener.");
                }            
                pelp.registerListener(isc,ac);
            }    
            result=true;
        }
        return result;
    }

}
