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

import javax.jms.JMSException;

import com.ibm.websphere.sib.api.jms.JmsDestination;

/**
 * This interface provides a private deal interface to forward and reverse routing
 * path functionality from within JMS. It is not supported and should not be used
 * by non-IBM applications. 
 * 
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 * 
 * @author matrober
 */
public interface _FRPHelper extends JmsDestination
{
  
  /**
   * <b>UNSUPPORTED INTERFACE</b><br>
   * Define a new forward routing path (FRP) that messages sent to this JMS Destination
   * should follow.<p>
   * 
   * A message sent using this JMS Destination will first visit in order the destinations
   * defined in each of the elements of the forward routing path. Only once each of the
   * elements of the FRP has been visited will the message be sent to the destination
   * described by this JMS Destination object.<br><br>
   * 
   * Note that the elements of the forward routing path are not checked for validity
   * or existence of the destination they describe.<br><br>
   * 
   * Supplying an FRP parameter of null, or zero element array will be treated identically
   * and result in null being returned from getForwardRoutingPath. A JMSException will
   * be thrown if any of the elements of the array is null.
   * 
   * <font color="#ff0000">This definition is not the same as the semantics defined for
   * Forward Routing Paths in the WPM coreSPI</font>
   * 
   * <!-- Javadoc'd: matrober 120104 -->
   *  
   * @param forwardPath The new forward routing path to be stored.
   * @throws JMSException If any of the elements of the array parameter are null.
   * 
   * @see _FRPHelper#getForwardRoutingPath
   */  
  public void setForwardRoutingPath(String[] forwardPath) throws JMSException;

  
  
  /**
   * <b>UNSUPPORTED INTERFACE</b><br>
   * Obtain the current forward routing path defined for this JMS Destination.<p>
   * 
   * Note that this method returns null if no FRP has been defined.
   * 
   * @see _FRPHelper#setForwardRoutingPath
   * 
   * @return The array of names of destinations representing the forward routing path.
   */
  public String[] getForwardRoutingPath();
  
  
  /**
   * <b>UNSUPPORTED INTERFACE</b><br>
   * Define a new reverse routing path (RRP) that reply messages sent to this JMS
   * Destination should follow.<p>
   *
   * A reply message sent in response to a message sent with this JMS Destination
   * as the replyTo destination will first visit in order the destinations defined
   * in each of the elements of the reverse routing path. Only once each of the
   * elements of the RRP has been visited will the message be sent to the destination
   * described by this JMS Destination object.<br><br>
   * 
   * Note that the elements of the reverse routing path are not checked for validity
   * or existence of the destination they describe.<br><br>
   * 
   * Supplying an RRP parameter of null, or zero element array will be treated identically
   * and result in null being returned from getReverseRoutingPath. A JMSException will
   * be thrown if any of the elements of the array is null.
   * 
   * <font color="#ff0000">This definition is not the same as the semantics defined for
   * Forward Routing Paths in the WPM coreSPI</font>
   * 
   * <!-- Javadoc'd: matrober 120104 -->
   * 
   * @param reversePath The new reverse routing path to be stored
   * @throws JMSException If any of the elements of the parameter array are null.
   * 
   * @see _FRPHelper#getReverseRoutingPath
   */
  public void setReverseRoutingPath(String[] reversePath) throws JMSException;

  
  
  /**
   * <b>UNSUPPORTED INTERFACE</b><br>
   * Obtain the current reverse routing path defined for this JMS Destination.<p>
   * 
   * Note that this method returns null if no RRP has been defined.
   * 
   * @see _FRPHelper#setReverseRoutingPath
   * 
   * @return The array of names of destinations representing the reverse routing path.
   */
  public String[] getReverseRoutingPath();  

}
