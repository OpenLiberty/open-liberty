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

package com.ibm.ws.sib.matchspace.tools;

import com.ibm.ws.sib.matchspace.InvalidTopicSyntaxException;

/** Implementations of this interface check the syntax of topic expressions where a 
 * topic is used as root Identifier in a MatchSpace.
 * 
 **/

public interface TopicSyntaxChecker 
{
	  /** checkTopicSyntax: Rules out syntactically inappropriate wildcard usages and
	   * determines if there are any wildcards
	   * @param topic the topic to check
	   * @return true if topic contains wildcards 
	   * @throws InvalidTopicSyntaxException if topic is syntactically invalid
	   */
	  public boolean checkTopicSyntax(String topic)
	    throws InvalidTopicSyntaxException;

	  /**Checks the topic for any wildcards as a Event topic can not 
	   * contain wildcard characters.
	   * 
	   * @param topic  The topic to be checked
	   *
	   * @throws InvalidTopicSyntaxException if topic is syntactically invalid
	   */
	  public void checkEventTopicSyntax(String topic)
	    throws InvalidTopicSyntaxException;
}

