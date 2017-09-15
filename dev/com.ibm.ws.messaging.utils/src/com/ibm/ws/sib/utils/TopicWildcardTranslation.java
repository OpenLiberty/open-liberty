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

package com.ibm.ws.sib.utils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Utility class that provides common functions to map between topic expression
 * syntaxes of SIB, EventBroker and MA0C.
 * Tests for this class can be found in com.ibm.ws.sib.api.jms.impl.TopicWildcardTranlationTest.
 */
public abstract class TopicWildcardTranslation
{
  
  // *************************** TRACE INITIALIZATION **************************
  
  private static TraceComponent tcInt = 
      Tr.register(TopicWildcardTranslation.class,
      UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);
  
  // ***************************** STATE VARIABLES *****************************
  
  private static TopicWildcardTranslation twt = null;
  
  /**
   * Obtain the singleton instance of the wildcard mapping class.
   * 
   * @return TopicWildcardTranslation singleton object
   * @throws Exception If it was not possible to instantiate the implementation class
   */
  public static TopicWildcardTranslation getInstance() throws Exception
  {
    if (tcInt.isEntryEnabled()) Tr.entry(tcInt, "getInstance");
    
    if (twt == null)
    {  
      try
      {
        Class cls = Class.forName(UtConstants.TWT_FACTORY_CLASS);
        twt = (TopicWildcardTranslation) cls.newInstance();
      }       
      catch(InstantiationException ie)
      {
        // No FFDC code needed
        if (tcInt.isDebugEnabled())Tr.debug(tcInt, "Unable to instantiate TopicWildcardTranslation", ie);
        if (tcInt.isEntryEnabled())Tr.exit(tcInt, "getInstance");
          
        twt = null;     

        throw ie;
      } 
    }
     
    if (tcInt.isEntryEnabled()) Tr.exit(tcInt, "getInstance");
    return twt;
    
  }// getInstance()

  
  
  /**
   * This method takes a valid SIB topic string and converts it to the
   * equivalent EventBroker/WBIMB topic syntax. Users of this method
   * include the Pub/Sub Bridge.<p>
   * 
   * The supplied parameter must be a valid SIB topic. No attempts are
   * made to validate the syntax of the parameter. <p>
   * 
   * Any topic that cannot be converted results in a ParseException being
   * thrown.<p>
   * 
   * Examples of the mappings handled by this method include the following;
   * 
   * <pre>
   *   SIB           EventBroker    Description
   * ---------------------------------------------------------
   * stock/IBM       stock/IBM      A non wildcard topic
   * stock//.        stock/#        stock and all descendents of stock
   * stock/*         stock/+        all children of stock
   * //*             #              everything 
   * </pre>   
   * 
   * @param sibTopic A valid SIB topic expression
   * @return String The EventBroker/WBIMB topic expression equivalent to the parameter.
   * @throws java.text.ParseException If it was not possible to translate the topic for any reason
   */
  public abstract String convertSIBToEventBroker(String sibTopic) throws java.text.ParseException;
  
  
  /**
   * This method takes a valid EventBroker/WBIMB topic string and converts it to the
   * equivalent SIB topic syntax. Users of this method include MQClientLink
   * and Pub/Sub Bridge.<p>
   * 
   * The supplied parameter must be a valid EventBroker topic. No attempts are
   * made to validate the syntax of the parameter.<p>
   * 
   * Any topic that cannot be converted results in a ParseException being thrown.<p>
   * 
   * Examples of the mappings handled by this method include the following;
   * 
   * <pre>
   *  EventBroker    SIB            Description   
   * ---------------------------------------------------------
   * stock/IBM       stock/IBM      A non wildcard topic
   * stock/#         stock//.       stock and all descendents of stock
   * stock/+         stock/*        all children of stock
   * #               //*            everything 
   * </pre>  
   * 
   * @param ebTopic A valid EventBroker topic expression
   * @return String The SIB equivalent of the topic expression supplied as the parameter.
   * @throws java.text.ParseException If it was not possible to translate the topic for any reason
   */
  public abstract String convertEventBrokerToSIB(String ebTopic) throws java.text.ParseException;
  
  
  /**
   * This method takes a valid MA0C topic string and converts it to the
   * equivalent SIB topic syntax. Users of this method include WPM JMS
   * implementation for support of MA88 URI strings (with brokerVer=0)<p>
   * 
   * The supplied parameter must be a valid MA0C topic. No attempts are made
   * to validate the syntax of the parameter.<p>
   * 
   * Any topic that cannot be converted results in a ParseException being thrown.
   * In particular the MA0C '?' wildcard is not supported. Topics containing this
   * character always throw ParseException.<p>
   * 
   * MA0C wildcards are character based and not level based. Any parameter
   * to this method which includes the '*' wildcard in the middle of a topic
   * but not surrounded by separator characters (eg.  'sto*ck') will throw a
   * ParseException. Valid use of the '*' wildcard includes the following;
   *    stock/*,  *,  stock&#47;*&#47;Hursley<p>
   * 
   * Examples of the mappings handled by this method include the following;
   * 
   * <pre>
   *  MA0C           SIB            Description   
   * ---------------------------------------------------------
   * stock/IBM       stock/IBM      A non wildcard topic
   * stock/*         stock/*        all descendents of stock
   * *               //*            everything 
   * </pre>  
   *  
   * @param ma0cTopic A valid MA0C topic exprssion
   * @return String The SIB equivalent of the topic expression supplied as the parameter.
   * @throws java.text.ParseException If it was not possible to translate the topic for any reason
   */
  public abstract String convertMA0CToSIB(String ma0cTopic) throws java.text.ParseException;
  
  /**
   * This method checks the syntax of the topic passed in as a parameter.<p>
   * 
   * The topic may contain wildcards (ie be a subscription topic) or not, as
   * required. The validation will be done using the regular expression
   * defined in feature 194466. 
   * 
   * @param sibTopic The topic in question
   * @return boolean Indicates whether this is a valid topic or not.
   */
  public abstract boolean isValidSIBTopic(String sibTopic);
  
}//class
