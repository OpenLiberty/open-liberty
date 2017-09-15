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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 * @author matrober
 *
 * This class abstracts the MessageProcessor admin functionality and presents
 * the necessary subset of it for use by the JMS Unit test environment.
 * 
 * The aim is to isolate use of the Administrator interface to this class in 
 * order to keep the production code as clean as possible.
 */
public abstract class UTEHelperFactory
{

  // ******************** SINGLETON *********************
  
  private static UTEHelper instance = null;
  
  // *************************** TRACE INITIALIZATION **************************
  private static TraceComponent tcInt =
    Tr.register(
      UTEHelperFactory.class,
      ApiJmsConstants.MSG_GROUP_INT,
      ApiJmsConstants.MSG_BUNDLE_INT);
 
  
  // **************** ENABLING FLAG *********************
  /**
   * Flag that enables or disables the JMS unit test environment.<p>
   * 
   * <pre>
   ****************************************************************************
   *    Unit Test Framework shortcut                                          *
   * The following var toggles the decision between the standard operation    *
   * inside of WebSphere (testEnv=false) and the outside of WebSphere         *
   * environment used to run standalone unit tests or during development.     *
   ****************************************************************************
   *
   * Note:
   * This flag can also be enabled automatically by setting the following
   * environment property;
   *   -Dcom.ibm.ws.sib.api.testenv=enabled
   * 
   * </pre>
   */
  public static boolean jmsTestEnvironmentEnabled = false;
  
  
  // *********************** IMPLEMENTATION ******************************
  
  /**
   * This method is used to return an instance of the UTEHelper.
   * @return UTEHelper
   * @throws IllegalStateException if called when the JMS Test environment is not enabled.
   */  
  public static synchronized UTEHelper getHelperInstance()
   throws java.lang.IllegalStateException
  {
    
    if (tcInt.isEntryEnabled()) SibTr.entry(tcInt, "getHelperInstance");
    
    // ************** CHECK TO SEE IF IT IS ENABLED *******************
    try
    {      
      // Initialise the test env flag from a system property if it has been set.
      String prop = System.getProperty("com.ibm.ws.sib.api.testenv");

      // Check that the text reads "enabled".      
      if ((prop != null) && ("enabled".equals(prop.toLowerCase())))
      {
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "TestEnv flag enabled by system property.");
        UTEHelperFactory.jmsTestEnvironmentEnabled = true;
    
      } else
      {
        if (tcInt.isDebugEnabled())
           SibTr.debug(tcInt, "testenv system property was present but did not enable testenv - "+prop);
      }//if
  
    } catch(SecurityException sce)
    {
      // No FFDC code needed
      if (tcInt.isDebugEnabled())
        SibTr.debug(tcInt, "Could not read system property due to SecurityException", sce);
    
    }//try
    
    
    // d250397 IGH it is valid to use a UTEHelper with UTE disabled
    // when running unit tests in remote client mode (see JMSTestCase)
    // so don't throw an exception here
/*
    if (!UTEHelperFactory.jmsTestEnvironmentEnabled) 
    {
      // NB. This is for unit test only and so need not be NLS'd.
      throw new java.lang.IllegalStateException("JMS Test Environment is not enabled.");
      
    }//if
*/    

    if (instance == null)
    {
      try
      {
        instance = (UTEHelper)Class.forName(
         "com.ibm.ws.sib.api.jms.impl.ute.UTEHelperImpl").newInstance();
      } catch (Exception e)
      {
        // No FFDC code needed
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "Couldn't find impl class", e);
          
        RuntimeException f = new java.lang.IllegalStateException("UTEHelperFactory.getHelperInstance()");
        f.initCause(e);
        
        if (tcInt.isEntryEnabled()) SibTr.exit(tcInt, "getHelperInstance");
        throw f;
      }
    }
    
    if (tcInt.isEntryEnabled()) SibTr.exit(tcInt, "getHelperInstance");
    return instance;
    
  }//getInstance
  
  
  // ****************** INTERFACE METHODS ***********************



}
