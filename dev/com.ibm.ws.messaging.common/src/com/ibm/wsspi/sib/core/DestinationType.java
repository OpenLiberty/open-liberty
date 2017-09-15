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
 
package com.ibm.wsspi.sib.core;

/**
 DestinationType is a "Java typesafe enum", the values of which represent 
 different types of destination. It can be used by an application to make sure 
 that the destination the app is using is of the type that it is expecting.
 <p>
 This class has no security implications.
*/
public class DestinationType {
	
  /**
   A destination of type Queue is indicated by the value DestinationType.QUEUE
  */
  public final static DestinationType QUEUE 
  	= new DestinationType("Queue",0);
  
  /**
   A destination of type TopicSpace is indicated by the value 
   DestinationType.TOPICSPACE
  */
  public final static DestinationType TOPICSPACE 
  	= new DestinationType("TopicSpace",1);
  
  /**
   A destination of type Service is indicated by the value 
   DestinationType.SERVICE
  */
  public final static DestinationType SERVICE 
    = new DestinationType("Service",2);
  
  /**
   A destination of type Port is indicated by the value 
   DestinationType.PORT
  */
  public final static DestinationType PORT 
    = new DestinationType("Port",3);

  /**
   A destination of type Unknown is indicated by the value 
   DestinationType.UNKNOWN
   This will be used to describe destinations which are 
   foreign.
  */
  public final static DestinationType UNKNOWN 
    = new DestinationType("Unknown",4);
  
  /**
   Returns a string representing the DestinationType value 
   
   @return a string representing the DestinationType value
  */
  public final String toString() {
  	return name;
  }
  
  /**
   * Returns an integer value representing this DestinationType
   * 
   * @return an integer value representing this DestinationType
   */
  public final int toInt()
  {
    return value;
  }
  
  /**
   * Get the DestinationType represented by the given integer value;
   * 
   * @param value the integer representation of the required DestinationType
   * @return the DestinationType represented by the given integer value
   */
  public final static DestinationType getDestinationType(int value)
  {
    return set[value];
  }
  
  private final String name;
  private final int value;  
  private final static DestinationType[] set = {QUEUE,
                                                TOPICSPACE,
                                                SERVICE,
                                                PORT,
                                                UNKNOWN
                                                };	
  private DestinationType(String name, int value) {
  	this.name = name;
    this.value = value;
  }
}

