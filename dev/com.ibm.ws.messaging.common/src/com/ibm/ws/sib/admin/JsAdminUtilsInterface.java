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

package com.ibm.ws.sib.admin;


/**
 * Public Admin utilities interface
 */
public interface JsAdminUtilsInterface {

  /*
   * Given a uuid of a messaging engine, find the name of that messaging engine 
   * Use this version when getting the UUID simply to output it to an exception 
   * insert, or error message. 
   */
  public String getMENameByUuidForMessage(String meUuid);

  /*
   * Given a uuid of a messaging engine, find the name of that messaging engine
   */
  public String getMENameByUuid(String meUuid);


  
  /*
   * Given a uuid of a MQ server bus member, find the name of that MQ server bus member
   */
  public String getMQServerBusMemberNameByUuid(String mqUuid);

  /*
   * Given a name of a messaging engine, find the uuid of that messaging engine
   */  
  public String getMEUUIDByName(String meName);

}

