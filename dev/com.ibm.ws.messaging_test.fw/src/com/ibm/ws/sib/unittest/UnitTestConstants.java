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
 package com.ibm.ws.sib.unittest;

public class UnitTestConstants
{
  /**
   * Name of the bus used in tests.
   */
  public static String BUS_NAME = "DEFAULT";
    
  /**
   * Name of the ME used in tests.
   */
  public static final String ME_NAME = "SIMPTestCase_ME";

  /**
   * The file store class name
   */
  public static final String FILE_STORE_CLASS = "com.ibm.ws.sib.msgstore.persistence.objectManager.PersistentMessageStoreImpl";
  
  /**
   * The DB class name
   */
  public static final String DB_STORE_CLASS = "com.ibm.ws.sib.msgstore.persistence.impl.PersistentMessageStoreImpl";
 
  /**
   * The store that the tests will use
   */
  public static String USE_DB_CLASS = FILE_STORE_CLASS;
  
}
