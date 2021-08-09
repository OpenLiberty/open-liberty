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

package com.ibm.ws.sib.processor.matching;

import java.security.Principal;

/**
 * @author Neil Young
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class TopicAcl extends MessageProcessorMatchTarget
{ 
  private String topic;
  
  private int operationType;

  private Principal principal;
  
	/**
	 * Constructor for TopicAcl.
	 * @param type
	 */
	public TopicAcl(String topic, 
                  int operationType, 
                  Principal principal) 
  {
		super(ACL_TYPE);
    this.topic = topic;
    this.operationType = operationType;
    this.principal = principal;
  }
  
  public String toString()
  {
    String theString = topic + ", " + operationType + ", ";
    if(principal == null)
    {
      theString = theString + "INHERIT-BLOCKER";
    }
    else
    {
      theString = theString + principal.toString();
    }
    return theString;
  }


/**
 * Returns the operationType.
 * @return int
 */
public int getOperationType() {
	return operationType;
}

/**
 * Returns the principal.
 * @return Principal
 */
public Principal getPrincipal() {
	return principal;
}

/**
 * Returns the topic.
 * @return String
 */
public String getTopic() {
	return topic;
}

}
