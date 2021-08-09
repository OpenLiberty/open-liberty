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

/**
 * Class to be used by instrumentation points in the JMS layer to create
 * the context values needed on methods on SIBPmiRm class. If the component is
 * not JMS this class should not be used.
 */
public class JMSContextValues
{
  private String className;
  private String methodName;
 
  /**
   * Constructor to create the context values. This should only be invoked
   * when the <code>isComponentEnabled(int)</code> returns true
   * 
   * @param className
   * @param methodName
   */
  public JMSContextValues(String className, String methodName)
  {
    if (className == null)
      className = "";
    
    if (methodName == null)
      methodName = "";
    
    this.className = className;
    this.methodName = methodName;
  }

  /**
   * @return String[] the values set in the constructor
   */
  public String[] getContextValues()
  {
    return new String[] { className, methodName };
  }
  
  /**
   * Returns the context names that will be used for registration
   * of the JMS component
   * @return
   */
  public static String[] getContextNames()
  {
    String[] contextNames = {"ClassName", "MethodName"};
    return contextNames;
  }
}
