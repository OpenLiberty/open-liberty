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
package com.ibm.ws.sib.processor.runtime.impl;

/**
 * @author tevans
 *
 * Provides some default ControlAdapter behaviour
 */
public abstract class AbstractControlAdapter implements ControlAdapter
{
  //These methods are only used if the ControlAdapter is to be registered as an
  //MBean. If it is an MBean then it should extend AbstractRegisteredControlAdapter
  //instead of this class
  public String getUuid(){return null;}
  public String getConfigId(){return null;}
  public String getRemoteEngineUuid(){return null;}
  public void registerControlAdapterAsMBean(){}
  public void deregisterControlAdapterMBean(){} 
}
