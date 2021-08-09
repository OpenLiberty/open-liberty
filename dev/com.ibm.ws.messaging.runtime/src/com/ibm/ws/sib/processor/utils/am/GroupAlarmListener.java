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
package com.ibm.ws.sib.processor.utils.am;

import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.ws.sib.utils.SIBUuid12;

public interface GroupAlarmListener extends AlarmListener
{
  /**
   * Called at the begining of a group callback.
   * 
   * @param firstContext The first alarm context in the group
   */
  public void beginGroupAlarm(Object firstContext);
  
  /**
   * Called for each additional alarm in the group.
   * 
   * @param nextContext the alarm context object
   */
  public void addContext(Object nextContext);
  
  /**
   * Return the unique identifier for this alarm group
   * 
   * @return the unique identifier for this alarm group
   */
  public SIBUuid12 getGroupUuid();
  
  /**
   * Called by the alarm manager to inform that the alarm has been cancelled
   *
   */
  public void cancel();
}
