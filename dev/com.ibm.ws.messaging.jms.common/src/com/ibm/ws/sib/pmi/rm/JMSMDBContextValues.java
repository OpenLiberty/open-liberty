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
 * Cclass to be used by instrumentation points in the JMS layer to create
 * the context values needed on methods on SIBPmiRm class. If the component is
 * not JMS and a MDB this class should not be used.
 */
public class JMSMDBContextValues
{
  private String destinationName;
  private String messageSelector;
  private final String provider = "Default Messaging";

  /**
   * Constructor to create the context values. This should only be invoked
   * when the <code>isComponentEnabled(int)</code> returns true
   * 
   * @param DestinationName
   * @param MessageSelector
   */
  public JMSMDBContextValues(String destinationName, String messageSelector)
  {
    if (destinationName == null)
    {
      this.destinationName = "";
    }
    else
    {
      this.destinationName = destinationName;
    }
    
    if (messageSelector == null)
      messageSelector = "";
    
    this.messageSelector = messageSelector;
  }

  /**
   * @return String[] the values set in the constructor
   */
  public String[] getContextValues()
  {
    return new String[] { destinationName, messageSelector, provider};
  }
  
  /**
   * Returns the MDB context names that will be used for registration
   * of the JMS component
   * @return
   */
  public static String[] getContextNames()
  {
    String[] contextNames = {"DestinationName", "MessageSelector", "Provider" };
    return contextNames;
  }
}
