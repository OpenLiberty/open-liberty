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
package com.ibm.ws.sib.api.jms.impl;

import javax.jms.Queue;

import com.ibm.websphere.sib.api.jms.JmsDestination;
import com.ibm.websphere.sib.Reliability;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * This class is used to hold the properties used to create core ConsumerSession
 * objects (whether durable or non-durable).
 */
public class ConsumerProperties
{
  
  private JmsDestination dest = null;
  private DestinationType destType = null;
    
  private String selector = null;
  private Reliability reliability = null;
  private boolean readAhead = false;  
  private boolean noLocal = false;
  private Reliability unrecovReliability = null;
           
  // Extra props for durable consumers.
  private String subName = null;
  private String clientID = null;
  private boolean supportsMultipleConsumers = false;
  private String durableSubscriptionHome = null;
  
  // Props for cluster control
  private boolean gatherMessages = false;
    
  /**
   * Constructor that lets you set all the properties.
   */
  ConsumerProperties(JmsDestination _dest,
                DestinationType _destType,
                String _sel,
                Reliability _rel,
                boolean _readAhead,
                boolean _recovExpress,
                boolean _noLocal,
                String _subName,
                String _clientID,
                boolean _supportsMultiple,
                String _durSubHome)
  {    
    
    dest = _dest;
    selector = _sel;
    noLocal = _noLocal;
    supportsMultipleConsumers = _supportsMultiple;
    clientID = _clientID;
    subName = _subName;
    
    
    setDestinationType(_destType);
    setReliability(_rel);
    setReadAhead(_readAhead);
    setRecovExpress(_recovExpress);
    setDurableSubscriptionHome(_durSubHome);
      
  }//constructor
        
  /**
   * @return String
   */
  public String getClientID()
  {
    return clientID;
  }

  /**
   * @return JmsDestination
   */
  public JmsDestination getJmsDestination()
  {
    return dest;
  }

  /**
   * @return DestinationFilter
   */
  public DestinationType getDestinationType()
  {
    return destType;
  }

  /**
   * @return boolean
   */
  public boolean noLocal()
  {
    return noLocal;
  }

  /**
   * @return boolean
   */
  public boolean readAhead()
  {
    return readAhead;
  }

  /**
   * @return Reliability
   */
  public Reliability getReliability()
  {
    return reliability;
  }

  /**
   * @return String
   */
  public String getSelector()
  {
    return selector;
  }

  /**
   * @return String
   */
  public String getSubName()
  {
    return subName;
  }

  /**
   * @return boolean
   */
  public boolean supportsMultipleConsumers()
  {
    return supportsMultipleConsumers;
  }
  
  /**
   * @return boolean
   */
  public boolean isGatherMessages()
  {
    return gatherMessages;
  } 
  
  /** 
   * @param gather
   */
  public void setGatherMessages(boolean gather)
  {
    gatherMessages = gather;
  }

  /**
   * @param type
   */
  public void setDestinationType(DestinationType type)
  {
    destType = type;
  }

  /**
   * @param rel
   */
  public void setReliability(Reliability rel)
  {
    reliability = rel;
  }

  /**
   * @param b
   */
  public void setRecovExpress(boolean b)
  {    
    if (b)
    {
      unrecovReliability = Reliability.NONE;
    } else
    {
      unrecovReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
    }
  }
  
  /**
   * Returns a printable form of the information stored in this object.
   * @return String
   */
  public String debug()
  {
    String data = null;
    
    StringBuffer sb = new StringBuffer();
    
    sb.append("ConsumerProperties");
    sb.append("\n");
    sb.append("------------------");
    sb.append("\n");
    
    sb.append("Destination: ");
    sb.append(getJmsDestination());
    sb.append("\n");
    
    sb.append("DestType: ");
    sb.append(getDestinationType());
    sb.append("\n");
    
    sb.append("Selector: ");
    sb.append(getSelector());
    sb.append("\n");
    
    sb.append("NoLocal: ");
    sb.append(noLocal());
    sb.append("\n");
    
    sb.append("Reliablity: ");
    sb.append(getReliability());
    sb.append("\n");
    
    sb.append("ClientID: ");
    sb.append(getClientID());
    sb.append("\n");
    
    sb.append("SubName: ");
    sb.append(getSubName());
    sb.append("\n");
    
    sb.append("DurableSubHome: ");
    sb.append(getDurableSubscriptionHome());
    sb.append("\n");
    
    sb.append("ReadAhead: ");
    sb.append(readAhead());
    sb.append("\n");
    
    sb.append("UnrecoverableReliability: ");
    sb.append(getUnrecovReliability());
    sb.append("\n");
    
    sb.append("Supports Multiple: ");
    sb.append(supportsMultipleConsumers());
    sb.append("\n");
    
    if (dest instanceof Queue)
    {
      sb.append("GatherMessages: ");
      sb.append(isGatherMessages());
      sb.append("\n");
      
    }
    
    data = sb.toString();
    return data;    
   
  }

  /**
   * @param b
   */
  public void setReadAhead(boolean b)
  {
    readAhead = b;
  }

  /**
   * @return String
   */
  public String getDurableSubscriptionHome()
  {
    return durableSubscriptionHome;
  }

  /**
   * @param string
   */
  public void setDurableSubscriptionHome(String string)
  {
    durableSubscriptionHome = string;
  }

  /**
   * @return Reliability
   */
  public Reliability getUnrecovReliability()
  {
    return unrecovReliability;
  }

}
