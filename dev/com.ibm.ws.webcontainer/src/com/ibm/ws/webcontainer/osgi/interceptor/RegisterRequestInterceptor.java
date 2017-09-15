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
package com.ibm.ws.webcontainer.osgi.interceptor;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;


/**
 *  This class listens for registration of RequestInterceptors.
 *      
 */
@Component(name = "com.ibm.ws.webcontainer.osgi.interceptor.RegisterRequestInterceptor",
           property = { "service.vendor=IBM"})
public class RegisterRequestInterceptor {
    
    private final static TraceComponent tc = Tr.register(RegisterRequestInterceptor.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    protected static final String CLASS_NAME = "com.ibm.ws.webcontainer.osgi.interceptor.RegisterRequestInterceptor";
    
    // Use ConcurrentServiceReferenceSet to observe service.ranking associated with services
    private static final ConcurrentServiceReferenceSet<RequestInterceptor> _FileNotFoundInterceptors = new ConcurrentServiceReferenceSet<RequestInterceptor>("RequestInterceptor");
    private static final ConcurrentServiceReferenceSet<RequestInterceptor> _AfterFilterInterceptors = new ConcurrentServiceReferenceSet<RequestInterceptor>("RequestInterceptor");
  
    @Activate
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterRequestInterceptor activated. context:"+context);
        }     
        _FileNotFoundInterceptors.activate(context);
        _AfterFilterInterceptors.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterRequestInterceptor de-activated.");
        }    
        _FileNotFoundInterceptors.deactivate(context);
        _AfterFilterInterceptors.deactivate(context);
    }

    
    @Reference(service=RequestInterceptor.class,policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setRequestInterceptor(ServiceReference<RequestInterceptor> reference) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterRequestInterceptor.setRequestInterceptor(), reference : " + reference + ", InterceptPoints: " + (String)reference.getProperty(RequestInterceptor.INTERCEPT_POINTS_PROPERTY));
        }
        String IPKey = (String)reference.getProperty(RequestInterceptor.INTERCEPT_POINTS_PROPERTY);
        if (IPKey!=null) {
            List<String> IPs = Arrays.asList(IPKey.split("\\s*, \\s*"));
            for (String IP : IPs) {
                if (IP.equals(RequestInterceptor.INTERCEPT_POINT_AFTER_FILTERS)) {
                    _AfterFilterInterceptors.addReference(reference);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "RegisterRequestInterceptor.setRequestInterceptor(), register for after-filter intercept point.");
                    }
                } else if(IP.equals(RequestInterceptor.INTERCEPT_POINT_FNF)) {
                    _FileNotFoundInterceptors.addReference(reference);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "RegisterRequestInterceptor.setRequestInterceptor(), register for on-filenotfound intercept point.");
                    }
                    
                } else if  (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "RegisterRequestInterceptor.setRequestInterceptor(), InterceptPoint not recognized : " + IP);
                } 
            } 
        }  else if  (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterRequestInterceptor.setRequestInterceptor(), reference ignored because no InterecptPoints defined");
        } 
         
    }
    
    protected void unsetRequestInterceptor(ServiceReference<RequestInterceptor> reference) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "RegisterRequestInterceptor.unsetRequestInterceptor(), reference:"+reference + ", InterceptPoints: " + (String)reference.getProperty(RequestInterceptor.INTERCEPT_POINTS_PROPERTY));
        String IPKey = (String)reference.getProperty(RequestInterceptor.INTERCEPT_POINTS_PROPERTY);
        if (IPKey!=null) {
            List<String> IPs = Arrays.asList(IPKey.split("\\s*, \\s*"));
            for (String IP : IPs) {
                if (IP.equals(RequestInterceptor.INTERCEPT_POINT_AFTER_FILTERS)) {
                    _AfterFilterInterceptors.removeReference(reference);
                } else if(IP.equals(RequestInterceptor.INTERCEPT_POINT_FNF)) {
                    _FileNotFoundInterceptors.removeReference(reference);
                } else if  (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "RegisterRequestInterceptor.unsetRequestInterceptor(), InterceptPoint not recognized : " + IP);
                } 
            } 
        }  else if  (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterRequestInterceptor.unsetRequestInterceptor(), reference ignored because no InterecptPoints defined");
        } 
    }
    
    public static boolean notifyRequestInterceptors(String interceptPoint, HttpServletRequest req,HttpServletResponse resp) {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() : InterceptPoint = " + interceptPoint);
        }
        
        boolean result = false;
        if (interceptPoint.equals(RequestInterceptor.INTERCEPT_POINT_AFTER_FILTERS)) {
            if (!_AfterFilterInterceptors.isEmpty()) { 
                
                Iterator<RequestInterceptor> afps = _AfterFilterInterceptors.getServices();
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() afps.hasNext() : " + afps.hasNext());
                }            
                ArrayList<RequestInterceptor> reverseAfps = new ArrayList<RequestInterceptor>();
                
                // reverse the order so highest ranked goes last
                while (afps.hasNext()) {
                   reverseAfps.add(0,afps.next());
                }
                afps = reverseAfps.iterator();
                  
                while (afps.hasNext() && !result) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                       Tr.debug(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() notify after filter interceptor.");
                    }            
                    result = afps.next().handleRequest(req, resp); 
                }    
                
            }  else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() no after filter interceptors.");
            }            

        } else if (interceptPoint.equals(RequestInterceptor.INTERCEPT_POINT_FNF)) {
            if (!_FileNotFoundInterceptors.isEmpty()) { 
                
                Iterator<RequestInterceptor> fnfps = _FileNotFoundInterceptors.getServices();
                
                while(fnfps.hasNext() && !result) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                       Tr.debug(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() notify file not found interceptor.");
                    }            
                    result = fnfps.next().handleRequest(req, resp); 
                }  
                               
            }  else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() no on-FNF interceptors.");
            }    
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() invalid intercept point.");
        } 
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "RegisterRequestInterceptor.notifyRequestInterceptors() : " + result);
        }
        return result;

    }
       
}
