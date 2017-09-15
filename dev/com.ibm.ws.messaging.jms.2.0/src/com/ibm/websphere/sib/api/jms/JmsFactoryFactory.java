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
package com.ibm.websphere.sib.api.jms;


import javax.jms.*;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The JmsFactoryFactory class is the programmatic entry point into the JMS
 * implementation classes.<p>
 * 
 * We separate interfaces and implementation so that applications can be
 * compiled without having to have the implementation present, and this class
 * allows applications to obtain the top level implementation objects.
 * 
 * @ibm-api
 * @ibm-was-base
 */
public abstract class JmsFactoryFactory
{
	
	// *************************** TRACE INITIALIZATION **************************
  private static TraceComponent tcInt =
    Tr.register(
      JmsFactoryFactory.class,
      ApiJmsConstants.MSG_GROUP_INT,
      ApiJmsConstants.MSG_BUNDLE_INT);
      
  private static TraceNLS nls = TraceNLS.getTraceNLS(ApiJmsConstants.MSG_BUNDLE_INT);

   
	// ***************************** STATE VARIABLES *****************************
  private static JmsFactoryFactory instance = null;

  /**
   * Returns a singleton instance of the JmsFactoryFactory class.<p>
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @return The JmsFactoryFactory singleton object instance   
   * @throws JMSException If it was not possible to instantiate the implementation
   * class.
   */
  public static synchronized JmsFactoryFactory getInstance() throws JMSException
  {
  	
  	if (tcInt.isEntryEnabled())
      Tr.entry(tcInt, "getInstance");

    if (instance == null)
    {
      

      try
      {
        Class cls =
          Class.forName(ApiJmsConstants.CONNFACTORY_FACTORY_CLASS);
        instance = (JmsFactoryFactory) cls.newInstance();

      } catch (Exception e)
      {
        // No FFDC code needed
      	if (tcInt.isDebugEnabled())
          Tr.debug(tcInt, "Unable to instantiate JmsFactoryFactory", e);
        if (tcInt.isEntryEnabled())
          Tr.exit(tcInt, "getInstance");
          
        instance = null;
              
        JMSException jmse = new JMSException(nls.getFormattedMessage("UNABLE_TO_CREATE_FACTORY_CWSIA0001",
        																			new Object[] {"JmsFactoryFactoryImpl", "sib.api.jmsImpl.jar"},
        																			"!!!Unable to instantiate JmsFactoryFactoryImpl"));
        jmse.setLinkedException(e);
        jmse.initCause(e);
        throw jmse;

      }

    }//if

		if (tcInt.isEntryEnabled())
      Tr.exit(tcInt, "getInstance");
      
		// If we get this far, then the instance is initialized, since an exception would
		// have been thrown if it was not.
    return instance;
  }

  /**
   * Create a provider specific ConnectionFactory object.<p>
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @return A new JMS ConnectionFactory object.
   * @throws JMSException Indicates a failure to initialize the required classes. 
   */
  public abstract JmsConnectionFactory createConnectionFactory()
    throws JMSException;
    
  /**
   * Create a provider specific QueueConnectionFactory object.<p>
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @return A new JMS QueueConnectionFactory object.
   * @throws JMSException Indicates a failure to initialize the required classes. 
   */
  public abstract JmsQueueConnectionFactory createQueueConnectionFactory()
    throws JMSException;
    
  /**
   * Create a provider specific TopicConnectionFactory object.<p>
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @return A new JMS TopicConnectionFactory object.
   * @throws JMSException Indicates a failure to initialize the required classes. 
   */
  public abstract JmsTopicConnectionFactory createTopicConnectionFactory()
    throws JMSException;
    
  /**
   * Provides the ability to create a javax.jms.Queue object without creating
   * a JMS Session.<p>
   * 
   * This method is provided as a convenience to the application programmer, who
   * may wish to create these objects to bind into JNDI without making an active
   * Connection to the bus.<p>
   * 
   * The behaviour of this method is equivalent to session.createQueue(name).
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @param name The name of the Queue this object should reference.
   * @return A new JMS Queue (administered object).
   * @throws JMSException If the supplied parameter is not in the correct format.
   * @see javax.jms.Session#createQueue(String)
   */
  public abstract JmsQueue createQueue(String name)
    throws JMSException;

  /**
   * Provides the ability to create a javax.jms.Topic object without creating
   * a JMS Session.<p>
   * 
   * This method is provided as a convenience to the application programmer, who
   * may wish to create these objects to bind into JNDI without making an active
   * Connection to the bus.<p>
   * 
   * The behaviour of this method is equivalent to session.createTopic(name).
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   * 
   * @param name The name of the Topic this object should reference.
   * @return A new JMS Topic (administered object).
   * @throws JMSException If the supplied parameter is not in the correct format.
   * @see javax.jms.Session#createTopic(String)
   */
  public abstract JmsTopic createTopic(String name)
    throws JMSException;

  /**
   * Provides the ability to obtain a javax.jms.ConnectionMetaData object without
   * creating an active Connection to the bus.<p>
   * 
   * This method is provided as a convenience to the application programmer.
   * 
   * The behaviour of this method is equivalent to connection.getMetaData().
   * 
   * <!-- Javadoc'd: matrober 030903 -->
   *    
   * @return A provider implementation of the ConnectionMetaData interface.
   * @throws JMSException If the required information is not available.
   * @see javax.jms.Connection#getMetaData()
   */
  public abstract ConnectionMetaData getMetaData()
    throws JMSException;
    

}
