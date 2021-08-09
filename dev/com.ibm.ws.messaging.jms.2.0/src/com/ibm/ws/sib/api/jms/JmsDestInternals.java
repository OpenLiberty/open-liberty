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
package com.ibm.ws.sib.api.jms;

/**
 * This interface provides some private deal options on the JmsDestination objects.
 * It is for the use of specific IBM internal customers and should not be used by customers.
 * 
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 * 
 * @author matrober
 */
public interface JmsDestInternals extends _FRPHelper
{
  
  /**
   * This method enables or disables the inclusion of the JMS Destination attribute
   * on messages sent using this destination. This option results in a smaller message
   * that does not contain the URI encoded version of the JmsDestination object.
   * 
   * The default for this property is false, which is indicated by the property not
   * being set in the internal map.
   * 
   */
  public void _setInhibitJMSDestination(boolean value);
  
  /**
   * Retrieves the current effective state of the inhibit JMS Destination property.
   * @return boolean
   */
  public boolean _getInhibitJMSDestination();
  
  /**
   * This method will be called internally and allows an instance of JmsDestinationImpl
   * to be marked as blocked. 
   * 
   * When a destination is in this state, an error message will be
   * returned to the user upon any attempt to produce or consume using the destination.
   * The error message returned to the user will be tailored to match the blocked destination
   * code set using this method. Possible reasons for this include an empty broker input stream 
   * on a message received by PSB from MA88.
   * 
   * The default value of the Integer property will be null, indicating that the destination
   * is not blocked. 
   * 
   * A list of pre-defined error codes can be found in JmsInternalConstants. This property is 
   * deliberately not a boolean flag to allow for future expansion of error messages. 
   * 
   * @param value The blocked destination code to be set.
   */
  public void setBlockedDestinationCode(Integer value);
  
  /**
   * This method will be called internally by JMS code to get the blocked destination
   * code.
   * 
   * @see #setBlockedDestinationCode(Integer)
   * 
   * @return Integer the blocked destination code.
   */
  public Integer getBlockedDestinationCode();

}
