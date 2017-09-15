/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: LogProperties
//------------------------------------------------------------------------------
/**
* <p>
* An abstract representation of the properties associated with a recovery log.
* Different types of recovery log will require different implementations of  
* this interface to define their physical characteristics. 
* </p>
*
* <p>
* Instances of these implementations are created by the client service in order
* to define these characteristics.
* </p>
*/                                                                          
public interface LogProperties extends java.io.Serializable    /* @LIDB1578-22C*/
{
  //------------------------------------------------------------------------------
  // Method: LogProperties.logIdentifier
  //------------------------------------------------------------------------------
  /**
  * Returns the unique (within service) "Recovery Log Identifier" (RLI) value.
  *
  * @return int The unique RLI value.
  */
  public int logIdentifier();

  //------------------------------------------------------------------------------
  // Method: LogProperties.logName
  //------------------------------------------------------------------------------
  /**
  * Returns the unique (within service) "Recovery Log Name" (RLN). 
  *
  * @return String The unique RLN value.
  */
  public String logName();
}
