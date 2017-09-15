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
package com.ibm.ws.sib.api.jms.ute;

import javax.jms.Destination;
import javax.jms.JMSException;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;

/**
 * @author matrober
 *
 * This interface provides the methods required by the JMS Unit test environment.
 */
public interface UTEHelper
{
    
  /**
   * Name of the bus created by the UTE.
   */
  public static final String UTE_BUSNAME = "BigRedFunBus";
  
  /**
   * This method is responsible for setting up the JMS test environment
   * so that JMS operations can be tested outside of WAS.
   * 
   * Note that this method results in the instantiation of the ME (and
   * thus the Administrator interface).
   * 
   * @return SICoreConnectionFactory
   */
  public SICoreConnectionFactory setupJmsTestEnvironment();  

  /**
   * This method is responsible for setting up the JMS test environment
   * so that JMS operations can be tested outside of WAS.
   * 
   * Note that this method results in the instantiation of the ME (and
   * thus the Administrator interface).
   * 
   * @param coldStart flag to indicate if the MessageStore should be cold or warm started.
   * @return SICoreConnectionFactory
   */
  public SICoreConnectionFactory setupJmsTestEnvironment(boolean coldStart);  
  
  /**
   * Used by unit tests to set up a destination on which to put messages.
   * Note that you should not call createDestination before having called
   * setupJmsTestEnvironment.
   * 
   * @param dest
   * @throws JMSException
   */
  public void createDestination(Destination dest) throws JMSException;
  
  /**
   * Used by unit tests to set up a destination, specifying the default
   * reliability to be set on that destination.
   * Note that you should not call createDestination before having called
   * setupJmsTestEnvironment.
   * 
   * @param dest
   * @param defaultReliability the default reliability to set on the SI destination
   * @throws JMSException
   */
  public void createDestination(Destination dest, Reliability defaultReliability) throws JMSException;

  /**
   * Used by unit tests to create a Destination and pass in a Destination Definition.<P>
   * DestinationDefinition can be created using the following code:<P>
   * <code>
   * com.ibm.ws.sib.admin.DestinationDefinition adminDDF = JsAdminFactory.getInstance()
          .createDestinationDefinition(destType, name); 
   * </code>
   * 
   * @param dd
   * @throws JMSException
   */
  public void createDestination(DestinationDefinition dd) throws JMSException;
  
  /**
   * Used by unit tests to delete a destination.
   * Note that you should not call deleteDestination before having called
   * setupJmsTestEnvironment.
   * 
   * @param dest
   * @throws JMSException
   */
  public void deleteDestination(Destination dest) throws JMSException;
  
  /**
   * Return the name of the messaging engine that has been created for the
   * test environment.
   *
   */
  public String getMEName();
  
  /**
   * Shutdown the messaging engine
   *
   */
  public void stopME();
  
}
