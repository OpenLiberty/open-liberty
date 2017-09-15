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
 * not SIB and not a MDB this class should not be used.
 */
public class SIBMDBContextValues
{
  private String busName;
  private String destinationName;
  private String messageSelector;
  private String mdbDiscriminiator;
  private final String provider = "SIB";

  /**
   * Constructor to create the context values. This should only be invoked
   * when the <code>isComponentEnabled(int)</code> returns true
   * 
   * @param SIDestinationAddress
   * @param MessageSelector
   * @param MdbDiscriminator
   */
  public SIBMDBContextValues(SIDestinationAddress destinationAddress,
      String messageSelector, String mdbDiscriminator)
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
    
    if (mdbDiscriminator == null)
    {
      this.mdbDiscriminiator = "";
    }
    else
    {
      this.mdbDiscriminiator = mdbDiscriminator;
    }
    
    if (messageSelector == null)
    {
      this.messageSelector = "";
    }
    else
    {
      this.messageSelector = messageSelector;
    }
  }

  /**
   * @return String[] the values set in the constructor
   */
  public String[] getContextValues()
  {
    return new String[] { busName, destinationName, 
                          messageSelector, mdbDiscriminiator, provider };
  }
  
  
  /**
   * Returns the MDB context names that will be used for registration
   * of the SIB component
   * @return
   */
  public static String[] getContextNames()
  {
    String[] contextNames = {"BusName", "DestinationName", 
                             "MessageSelector", "MdbDiscriminator", "Provider" };
    return contextNames;
  }
}
