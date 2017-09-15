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
 * An enumerated type of resource which is being registered/de-registered 
 * for dynamic runtime control.
 */
public class ControllableType {

  public final static ControllableType QUEUE_POINT = new ControllableType(JsConstants.MBEAN_TYPE_QP, 0);

  public final static ControllableType PUBLICATION_POINT = new ControllableType(JsConstants.MBEAN_TYPE_PP, 1);

//  public final static ControllableType MEDIATION_POINT = null;//new ControllableType(JsConstants.MBEAN_TYPE_MP, 2);

//  public final static ControllableType MEDIATION_EXECUTION_POINT = null;//new ControllableType(JsConstants.MBEAN_TYPE_MEP, 3);
  
//  public final static ControllableType MEDIATION_LOCALIZATION_POINT = null;//new ControllableType(JsConstants.MBEAN_TYPE_MMP, 4);
  
  public final static ControllableType SUBSCRIPTION_POINT = new ControllableType(JsConstants.MBEAN_TYPE_SP, 5);

  public final static ControllableType REMOTE_QUEUE_POINT = null;//new ControllableType(JsConstants.MBEAN_TYPE_RQP, 6);

  public final static ControllableType REMOTE_PUBLICATION_POINT =null;// new ControllableType(JsConstants.MBEAN_TYPE_RPP, 7);

  public final static ControllableType REMOTE_MEDIATION_POINT = null;//new ControllableType(JsConstants.MBEAN_TYPE_RMP, 8);

  public final static ControllableType REMOTE_SUBSCRIPTION_POINT = null;//new ControllableType(JsConstants.MBEAN_TYPE_RSP, 9);

  public final static ControllableType SIB_LINK = null;//new ControllableType(JsConstants.MBEAN_TYPE_SIB_LINK, 10);

  public final static ControllableType MQ_LINK = null;//new ControllableType(JsConstants.MBEAN_TYPE_MQ_LINK, 11);

  public final static ControllableType LINK_TRANSMITTER = null;//new ControllableType(JsConstants.MBEAN_TYPE_LINK_TRANSMITTER, 12);

  public final static ControllableType MQ_LINK_SENDER_CHANNEL =null;// new ControllableType(JsConstants.MBEAN_TYPE_MQ_LINK_SENDER_CHANNEL, 13);

  public final static ControllableType MQ_LINK_RECEIVER_CHANNEL = null;//new ControllableType(JsConstants.MBEAN_TYPE_MQ_LINK_RECEIVER_CHANNEL, 14);

  public final static ControllableType MQ_LINK_SENDER_CHANNEL_TRANSMITTER = null;//new ControllableType(JsConstants.MBEAN_TYPE_MQ_LINK_SENDER_CHANNEL_TRANSMITTER, 15);

  public final static ControllableType MQ_PSB_BROKER_PROFILE = null;//new ControllableType(JsConstants.MBEAN_TYPE_MQ_PSB_BROKER_PROFILE, 16);

  /**
  	* Returns a string representing the ControllableType 
  	* @return a string representing the ControllableType
  	*/
  public final String toString() {
    return name;
  }

  /**
  	* Returns an integer value representing this ControllableType
  	* @return an integer value representing this ControllableType
  	*/
  public final int toInt() {
    return value;
  }

  /**
  	* Get the ControllableType represented by the given integer value;
  	* @param value the integer representation of the required ControllableType
  	* @return the ControllableType represented by the given integer value
  	*/
  public final static ControllableType getControllableType(int value) {
    return set[value];
  }

  private final String name;
  private final int value;
  private final static ControllableType[] set =
    {
      QUEUE_POINT,
      PUBLICATION_POINT,
//      MEDIATION_POINT,
//      MEDIATION_EXECUTION_POINT,
//      MEDIATION_LOCALIZATION_POINT,
      SUBSCRIPTION_POINT,
      REMOTE_QUEUE_POINT,
      REMOTE_PUBLICATION_POINT,
      REMOTE_MEDIATION_POINT,
      REMOTE_SUBSCRIPTION_POINT,
      SIB_LINK,
      MQ_LINK,
      LINK_TRANSMITTER,
      MQ_LINK_SENDER_CHANNEL,
      MQ_LINK_RECEIVER_CHANNEL,
      MQ_LINK_SENDER_CHANNEL_TRANSMITTER,
      MQ_PSB_BROKER_PROFILE
    };

  /**   
  	* Private constructor.
  	*/
  private ControllableType(String name, int value) {
    this.name = name;
    this.value = value;
  }
}

