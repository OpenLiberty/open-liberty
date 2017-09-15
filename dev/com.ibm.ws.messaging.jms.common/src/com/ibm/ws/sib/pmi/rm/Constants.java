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

import com.ibm.ws.sib.utils.TraceGroups;

public class Constants 
{
  /**
   * Currently piggybacking on the util trace group.
   */
  public final static String MSG_GROUP  = TraceGroups.TRGRP_UTILS;
  
  /**
   * No messages for sib.pmi.rm
   */
  public final static String MSG_BUNDLE = null;
  
  /**
   * This is the implementation that will be used for all sib.pmi.rm calls.
   */
  public final static String SIB_PMI_RM_IMPL_CLASS 
    = "com.ibm.ws.sib.pmi.rm.impl.SIBPmiRmWsImpl";
    
  /**
   * This is the WAS RM implementation used for unit tests.
   */    
  public final static String PMI_RM_SIB_IMPL_TEST_CLASS 
    = "com.ibm.ws.sib.pmi.rm.impl.test.PmiRmSIBTestImpl";
}
