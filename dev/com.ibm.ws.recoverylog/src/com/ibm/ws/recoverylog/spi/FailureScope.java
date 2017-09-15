/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
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
// Interface: FailureScope
//------------------------------------------------------------------------------
/**
* The "failure scope" is defined as a potential region of failure (such as an
* application server) All operations that take place within the WebSphere
* deployment do so under a given failure scope. For example, a transaction 
* running on application server 1 is operating under the failure scope for 
* server 1. 
*/                                                                          
public interface FailureScope
{
  //------------------------------------------------------------------------------
  // Method: FailureScope.isContainedBy
  //------------------------------------------------------------------------------
  /**
  * Returns true if the target failure scope is encompassed by failureScope. For
  * example, if the target failure scope identifies a server region inside a z/OS
  * scalable server identified by failureScope then this method returns true.
  *
  * @param failureScope Failure scope to test
  *
  * @return boolean Flag indicating if the target failure scope is contained by the
  *                 specified failure scope
  */
  public boolean isContainedBy(FailureScope failureScope);
  
  //------------------------------------------------------------------------------
  // Method: FailureScope.serverName
  //------------------------------------------------------------------------------
  /**
  * Returns the name of the server identified by this failure scope.
  *
  * @return String The associated server name
  */
  public String serverName();

  //------------------------------------------------------------------------------
  // Method: FileFailureScope.isSameExecutionZone
  //------------------------------------------------------------------------------
  /**
  * Returns true if this failure scope represents the same general recovery scope as
  * the input parameter.  For instance, if more than one FailureScope was created
  * which referenced the same server, they would be in the same execution zone.
  *
  * @param anotherScope Failure scope to test
  *
  * @return boolean Flag indicating if the target failure scope represents the 
  *                 same logical failure scope as the specified failure scope.
  */
  public boolean isSameExecutionZone(FailureScope anotherScope);
}
