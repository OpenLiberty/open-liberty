/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.inbound.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.sib.ra.impl.SibRaUtils;

public class SibRaBusSecurityAction 
{
    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaBusSecurityAction.class);

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaBusSecurityAction.class.getName();
    
    
    
    //lohith liberty changes 
    @SuppressWarnings("unchecked")
    static void performBusSecurityAction (final String endpointName, final SIDestinationAddress destinationAddress)
    {/*
        final String methodName = "performBusSecurityAction";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, new Object[] {});
        }
   //     final AdminService adminService = AdminServiceFactory.getAdminService();
        
        // stop the endpoint by calling the jca MBean
        try
        {
        	//lohith liberty change 
          String processName = "Server1"; //adminService.getProcessName();
          ObjectName filter = new ObjectName("WebSphere:type=J2CMessageEndpoint,MessageDrivenBean=" + 
                          endpointName + ",process=" + processName + ",*");
          Set<ObjectName> listMdbs = adminService.queryNames(filter, null);
          final Iterator<ObjectName> iter = listMdbs.iterator();
          int numEPs = listMdbs.size();
          
          if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
              SibTr.debug(TRACE, "There are " + listMdbs.size() +" endpoints found");
          }
          
          if (numEPs == 0)
          {
              SibTr.error(TRACE, "NO_MBEAN_EXCEPTION_CWSIV0904", new Object[] { endpointName, destinationAddress});
          }
          else if (numEPs > 1)
          {
              SibTr.error(TRACE, "MULTIPLE_MBEAN_EXCEPTION_CWSIV0905", new Object[] { endpointName, destinationAddress});
          }
          else
          {
              AuthUtils au = AuthUtilsFactory.getInstance().getAuthUtils();
              au.runAsSystem(new BusSecurityAction<Object> () 
              {
                  public Object run() 
                  {
                      try
                      {
                          ObjectName objectName = iter.next ();
                          adminService.invoke(objectName, "pause", null, null);
                      }
                      catch (Exception exception)
                      {
                          FFDCFilter.processException(exception, CLASS_NAME + "." + methodName,
                                                  "1:100:1.2", this);
                          SibTr.error(TRACE, "INVOKE_MBEAN_EXCEPTION__CWSIV0903", new Object[] { endpointName, destinationAddress,
                                                      exception });
                      }
                      return null;
                  }
              });
          }
        }
        catch (Exception exception)
        {
          FFDCFilter.processException(exception, CLASS_NAME + "." + methodName,
                          "1:112:1.2");
          SibTr.error(TRACE, "INVOKE_MBEAN_EXCEPTION__CWSIV0903", new Object[] { endpointName, destinationAddress,
              exception });
        }
        
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) 
        {
            SibTr.exit(TRACE, methodName);
        }
    */}
}
