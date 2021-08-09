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
package com.ibm.ws.sib.processor.impl;

/**
 * Constants used by the Durable Input/Output handlers.
 */
public interface DurableConstants {
  // Constants for message status
  public int STATUS_OK                    = 0;
  public int STATUS_SUB_ALREADY_EXISTS    = 1;
  public int STATUS_SUB_GENERAL_ERROR     = 2;
  public int STATUS_SUB_NOT_FOUND         = 3;
  public int STATUS_SUB_CARDINALITY_ERROR = 4;
  public int STATUS_NOT_AUTH_ERROR        = 5;
  public int STATUS_SUB_MISMATCH_ERROR    = 6;
  public int STATUS_SIB_LOCKED_ERROR      = 7;
    
  // Constants for message priority, timeouts, etc.
  public long CREATEDURABLE_RETRY_TIMEOUT = 3000;
  public long DELETEDURABLE_RETRY_TIMEOUT = 3000;
  public long CREATESTREAM_RETRY_TIMEOUT  = 3000;
  
  // 219870 public int  CREATEDURABLE_NUMTRIES      = 3;
  // 219870 public int  DELETEDURABLE_NUMTRIES      = 3;
  // 219870 public int  CREATESTREAM_NUMTRIES       = 3;
  
}
