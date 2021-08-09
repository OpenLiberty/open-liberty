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
package com.ibm.ws.sib.pmi.rm;

import com.ibm.websphere.sib.SIDestinationAddress;

/**
 * Class to be used by instrumentation points in the SIB layer to create
 * the context values needed on methods on SIBPmiRm class. If the component is
 * not SIB and not mediations then this class should not be used.
 */
public class SIBMediationContextValues
{
  private String busName;
  private String destinationName;
  private String className;
  private String methodName;
  private String mediationName;

  /**
   * Constructor to create the context values. This should only be invoked
   * when the <code>isComponentEnabled(int)</code> returns true
   * 
   * @param SIDestinationAddress
   * @param ClassName
   * @param MethodName
   * @param MediationName
   */
  public SIBMediationContextValues(SIDestinationAddress destinationAddress,
      String className, String methodName, String mediationName)
  {
    if (destinationAddress == null)
    {
      this.busName = "";
      this.destinationName = "";
    }
    else
    {
      this.busName = destinationAddress.getBusName();
      this.destinationName = destinationAddress.getDestinationName(); 
    }
    
    if (className == null)
    {
      this.className = "";
    }
    else
    {
      this.className = className;
    }
    
    if (methodName == null)
    {
      this.methodName = "";
    }
    else
    {
      this.methodName = methodName;
    }
    
    if (mediationName == null)
    {
      this.mediationName = "";
    }
    else
    {
      this.mediationName = mediationName;  
    }    
  }

  /**
   * @return String[] the values set in the constructor
   */
  public String[] getContextValues()
  {
    return new String[] { className, methodName, busName,
                          destinationName, mediationName };
  }
  
  /**
   * Returns the Mediation context names that will be used for registration
   * of the SIB component
   * @return
   */
  public static String[] getContextNames()
  {
    String[] contextNames = {"ClassName", "MethodName", "BusName", 
                             "DestinationName", "MediationName" };
    return contextNames;
  }
}
