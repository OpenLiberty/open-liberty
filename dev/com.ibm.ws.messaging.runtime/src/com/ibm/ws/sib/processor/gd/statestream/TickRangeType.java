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
package com.ibm.ws.sib.processor.gd.statestream;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

public class TickRangeType
{
  private static TraceComponent tc =
    SibTr.register(
        TickRangeType.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  public final static TickRangeType UNKNOWN = new TickRangeType("Unknown",0);
  public final static TickRangeType REQUESTED = new TickRangeType("Requested",1);
  public final static TickRangeType UNCOMMITTED = new TickRangeType("Uncommitted",2);
  public final static TickRangeType VALUE = new TickRangeType("Value",3);
  public final static TickRangeType DISCARDED = new TickRangeType("Discarded",4);
  public final static TickRangeType ACCEPTED = new TickRangeType("Accepted",5);
  public final static TickRangeType REJECTED = new TickRangeType("Rejected",6);
  public final static TickRangeType COMPLETED = new TickRangeType("Completed",7);
  public final static TickRangeType ERROR = new TickRangeType("Error",8);
  
  private final static TickRangeType[] set = {UNKNOWN,
                                              REQUESTED,
                                              UNCOMMITTED,
                                              VALUE,
                                              DISCARDED,
                                              ACCEPTED,
                                              REJECTED,
                                              COMPLETED,
                                              ERROR};
  
  private final String name;
  private final int value;  

  private TickRangeType(String name, int value)
  {
    this.name = name;
    this.value = value;
  }
  
  /**
   * Returns a string representing the TickRangeType value
   * 
   * @return a string representing the TickRangeType value
  */
  public final String toString()
  {
    return name;
  }
  
  /**
   * Returns an integer value representing this TickRangeType
   * 
   * @return an integer value representing this TickRangeType
   */
  public final int toInt()
  {
    return value;
  }
  
  /**
   * Get the TickRangeType represented by the given integer value;
   * 
   * @param value the integer representation of the required TickRangeType
   * @return the TickRangeType represented by the given integer value
   */
  public final static TickRangeType getTickRangeType(int value)
  {
    return set[value];
  }
  
  private final static TickRangeType nextState[][] = {
    // Existing state:-
    // UNKNOWN      REQUESTED    UNCOMMITTED VALUE      DISCARDED  ACCEPTED   REJECTED   COMPLETED  ERROR
     { UNKNOWN,     UNKNOWN,     ERROR,      ERROR,     ERROR,     ERROR,     ERROR,     ERROR,     ERROR },
     { REQUESTED,   REQUESTED,   ERROR,      ERROR,     ERROR,     ERROR,     ERROR,     ERROR,     ERROR },
     { UNCOMMITTED, UNCOMMITTED, ERROR,      ERROR,     ERROR,     ERROR,     ERROR,     ERROR,     ERROR },
     { VALUE,       VALUE,       VALUE,      VALUE,     ERROR,     ERROR,     ERROR,     COMPLETED, ERROR },
     { DISCARDED,   DISCARDED,   DISCARDED,  ERROR,     ERROR,     ERROR,     ERROR,     ERROR,     ERROR },
     { ACCEPTED,    ACCEPTED,    ERROR,      ACCEPTED,  ERROR,     ERROR,     ERROR,     ERROR,     ERROR },
     { REJECTED,    REJECTED,    ERROR,      REJECTED,  ERROR,     ERROR,     ERROR,     ERROR,     ERROR },
     { COMPLETED,   COMPLETED,   COMPLETED,  COMPLETED, COMPLETED, COMPLETED, COMPLETED, COMPLETED, ERROR },
     { ERROR,       ERROR,       ERROR,      ERROR,     ERROR,     ERROR,     ERROR,     ERROR,     ERROR }

     };


  public static TickRangeType stateTransition(TickRangeType toState, TickRangeType fromState)
  {
    TickRangeType newState = nextState[toState.toInt()][fromState.toInt()];

    // Check for ERRORState.
    if (newState == ERROR)
    {
      throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0008",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.TickRangeType",
              "1:141:1.3",
              fromState,
              toState },
            null));
    }
    
    return newState;
  }
}

