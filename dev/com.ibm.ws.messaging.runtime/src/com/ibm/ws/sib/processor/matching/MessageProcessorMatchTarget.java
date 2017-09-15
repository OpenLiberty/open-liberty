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

import com.ibm.ws.sib.matchspace.MatchTarget;

//------------------------------------------------------------------------------
// MessageProcessorMatchTarget Interface
//------------------------------------------------------------------------------
/**
 * This interface must be implemented by objects that are to be associated
 * with filters in the matching space.
 */ //---------------------------------------------------------------------------
public class MessageProcessorMatchTarget extends MatchTarget
{
  // Types start at 0 and, for efficiency, should be increased
  // densly (i.e. don't skip numbers).
  // MatchTarget types are processed in MessageProcessor from lowest
  // index value to highest, so order can be significant.
  public static final int ACL_TYPE = 0;
  public static final int JS_SUBSCRIPTION_TYPE = 1;
  public static final int JS_CONSUMER_TYPE = 2;
  public static final int JS_NEIGHBOUR_TYPE = 3;
  public static final int APPLICATION_SIG_TYPE = 4;
  
  public static final int MAX_TYPE = APPLICATION_SIG_TYPE;
  // edit when more are added
  public static final int NUM_TYPES = MAX_TYPE + 1;

  // Names for target types, for use in debugging statements
  public static final String[] TARGET_NAMES =
    { "acl", "js subscription", "js consumer", "js neighbour", "reg application" };

  // Constructor (the only one) requires a type

  protected MessageProcessorMatchTarget(int type)
  {
    super(type);
  }

}
