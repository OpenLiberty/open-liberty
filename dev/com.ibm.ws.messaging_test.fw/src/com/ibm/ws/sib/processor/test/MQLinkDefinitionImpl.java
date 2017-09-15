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
package com.ibm.ws.sib.processor.test;

import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.MQLinkReceiverChannelDefinition;
import com.ibm.ws.sib.admin.MQLinkSenderChannelDefinition;

import com.ibm.ws.sib.utils.SIBUuid8;

public class MQLinkDefinitionImpl implements MQLinkDefinition
{
  SIBUuid8 mqlinkuuid;
  
  public MQLinkDefinitionImpl(SIBUuid8 mqlinkuuid)
  {
    this.mqlinkuuid = mqlinkuuid;
  }
  
  public boolean getAdoptable()
  {
    return false;
  }

  public int getBatchSize()
  {
    return 0;
  }

  public String getConfigId()
  {
    return null;
  }

  public String getDescription()
  {
     return null;
  }

  public int getHeartBeat()
  {
    return 0;
  }

  public String getInitialState()
  {
    return null;
  }

  public int getMaxMsgSize()
  {
    return 0;
  }

  public String getName()
  {
    return null;
  }

  public String getNpmSpeed()
  {
    return null;
  }

  public String getQmName()
  {
    return null;
  }

  public MQLinkReceiverChannelDefinition getReceiverChannel()
  {
    return null;
  }

  public MQLinkSenderChannelDefinition getSenderChannel()
  {
    return null;
  }

  public long getSequenceWrap()
  {
    return 0;
  }

  public String getTargetUuid()
  {
    return null;
  }

  public SIBUuid8 getUuid()
  {
    return mqlinkuuid;
  }

}
