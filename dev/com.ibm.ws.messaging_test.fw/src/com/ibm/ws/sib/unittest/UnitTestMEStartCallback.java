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

public interface UnitTestMEStartCallback
{
  public void stop(boolean mpOnly, int shutdownMode, boolean closeConnection) 
  throws Exception;
  
  public void createME(UnitTestMEStarter starter, boolean coldStart, boolean mpOnly) throws Exception;
  
  public void start(UnitTestMEStarter starter, boolean coldStart, boolean mpOnly) throws Exception;
  
  public void coldStart();
}
