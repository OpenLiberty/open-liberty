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
package com.ibm.ws.sib.api.jms.impl.ute;

import javax.jms.Destination;
import javax.jms.JMSException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.websphere.sib.api.jms.JmsTopic;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.admin.LocalizationDefinition;
//import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.api.jms.impl.JmsDestinationImpl;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.api.jms.ute.UTEHelper;
import com.ibm.ws.sib.processor.Administrator;
import com.ibm.ws.sib.processor.SIMPAdmin;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationConfiguration;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;

/**
 * @author matrober
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class UTEHelperImpl implements UTEHelper
{

  // ***************************** TRACE **********************************
      
  private static TraceComponent tcInt =
    Tr.register(
      UTEHelperImpl.class,
      ApiJmsConstants.MSG_GROUP_INT,
      ApiJmsConstants.MSG_BUNDLE_INT);
      
 
  // *************************** PRIVATE STATE ****************************

/*  *//**
   * This is the ME.
   *//*
  private SIMPJsStandaloneEngine myME = null;*/

  /**
   * This is the admin interface to the ME (same object) which allows us to
   * create and delete destinations etc.
   */
  private Administrator admin = null;
  
  /**
   * A live connection to the standalone ME.
   */
  private SICoreConnection coreConnection = null;  

  // ********************** INTERFACE METHODS *****************************

  public SICoreConnectionFactory setupJmsTestEnvironment()
  {
    return setupJmsTestEnvironment(true);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.api.jms.impl.ute.UTEHelper#setupJmsTestEnvironment()
   */
 /* public SICoreConnectionFactory setupJmsTestEnvironment(boolean coldStart)
  {
    
    if (tcInt.isEntryEnabled())
      SibTr.entry(tcInt, "setupJmsTestEnvironment");
      
    SICoreConnectionFactory ccf = null;

    // We synchronize on this object to prevent lots of things creating
    // the ME at the same time.
    synchronized (this)
    {

      if (myME == null)
      {
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "ME not yet created.");
          
        System.out.println("***************************************");
        System.out.println("*  JMS Unit Test Environment Enabled  *");
        System.out.println("***************************************");

        //  This bit of magic allows us to function outside of a WAS environment.
        myME = getStandaloneEngine(coldStart);

      } //if

      // Now we have created it we can cast it to the SIMP object we really care
      // about.    
      ccf = (SICoreConnectionFactory) myME.getMessageProcessor();

      // Get hold of the admin interface for later use.
      enableDestinationCreation(ccf);

      try
      {      
        // create a connection to the standalone ME.
        coreConnection = ccf.createConnection(null, null, null);
      } catch(SIException sice)
      {        
        // No FFDC code needed 
        System.out.println("*** Failed to create a connection ***");
        sice.printStackTrace();
      }

    } //sync

    if (tcInt.isEntryEnabled()) SibTr.exit(tcInt, "setupJmsTestEnvironment");
    return ccf;

  } //setupJmsTestEnvironment
*/  
	/* 
	 * @see com.ibm.ws.sib.api.jms.impl.ute.UTEHelper#createDestination(DestinationDefinition)
	 */
  public void createDestination(com.ibm.ws.sib.admin.DestinationDefinition dd)throws JMSException
  {
		if (tcInt.isEntryEnabled()) SibTr.entry(tcInt, "createDestination(DestinationDefinition)");
  	try
  	{
  		Reliability defRel = dd.getDefaultReliability();
  		if(defRel == null)
  		{
				dd.setDefaultReliability(Reliability.ASSURED_PERSISTENT);
  		}
  		
  		Reliability maxRel = dd.getMaxReliability();
  		if(maxRel == null)
  		{
				dd.setMaxReliability(Reliability.ASSURED_PERSISTENT);
  		}
  		
			LocalizationDefinition dloc = null;//JsAdminFactory.getInstance().createLocalizationDefinition(dd.getName());
			//lohith liberty change
			admin.createDestinationLocalization(dd, dloc);  // createDestinationLocalization(dd, dloc, null, null, null, null);
  	}
		catch(Exception e)
		{
      // No FFDC code needed
			if (tcInt.isEntryEnabled()) SibTr.debug(tcInt, "Exception creating Destination Localisation", e);
			if (tcInt.isEntryEnabled()) SibTr.exit(tcInt, "createDestination(DestinationDefinition)");
			throw new RuntimeException("Unable to create Destination Localization", e);
		}
		if (tcInt.isEntryEnabled()) SibTr.exit(tcInt, "createDestination(DestinationDefinition)");
  }


  public void createDestination(Destination dest) 
    throws JMSException
  {
    // call the more specific method, supplying what we always used to use for
    // default reliability
    createDestination(dest, Reliability.ASSURED_PERSISTENT);
  }
  
  /* 
   * @see com.ibm.ws.sib.api.jms.impl.ute.UTEHelper#createDestination(Destination)
   */
  public void createDestination(Destination dest, Reliability defaultReliability) 
    throws JMSException
  {

    try
    {
    
      if (tcInt.isEntryEnabled())
        SibTr.entry(tcInt, "createDestination(Destination)");    
         
      com.ibm.wsspi.sib.core.DestinationType destType = null;
      String name = null;
  
      try
      {
  
        // Work out which type of Destination we want to create.
        if (dest instanceof JmsQueue)
        {
          destType = com.ibm.wsspi.sib.core.DestinationType.QUEUE;
          name = ((JmsQueue) dest).getQueueName();
  
        } else if (dest instanceof JmsTopic)
        {
          destType = com.ibm.wsspi.sib.core.DestinationType.TOPICSPACE;
          name = ((JmsTopic) dest).getTopicSpace();
        } else
        {
          destType = null;
          name = null;
        }
        
        if (tcInt.isDebugEnabled())
        {
          SibTr.debug(tcInt, "    name: "+name);
          SibTr.debug(tcInt, "destType: "+destType);
        }
          
  
        if (destType != null)
        {
          // Defer to the other method.
          createDestination(name, destType, defaultReliability);
        } else
        {
          throw new JMSException("Could not create Destination - destType was null");
        }
  
      } catch (JMSException e)
      {
        // No FFDC code needed
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "JMSException during createDestination", e);
          
        throw e;
  
      } catch (Exception f)
      {
        // No FFDC code needed
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "Exception during createDestionation", f);
          
        JMSException jmse = new JMSException("Exception from createDestination");
        jmse.setLinkedException(f);
        jmse.initCause(f);
        throw jmse;
  
      } //try

    } finally
    {    
      if (tcInt.isEntryEnabled())
         SibTr.exit(tcInt, "createDestination(Destination)");
    }//

  } //createDestination
  
  
  
  
  /**
   * @see com.ibm.ws.sib.api.jms.ute.UTEHelper#deleteDestination(Destination)
   */
  public void deleteDestination(Destination dest) throws JMSException
  {
      
    if (tcInt.isEntryEnabled())
      SibTr.entry(tcInt, "deleteDestination(Destination)");
    if (tcInt.isDebugEnabled())
      SibTr.debug(tcInt, "dest: "+dest);
      
    String name = ((JmsDestinationImpl)dest).getDestName();  
        
    try
    {      
      
      // Obtain information about the destination from the core connection.
      SIDestinationAddress sida = JmsServiceFacade.getSIDestinationAddressFactory().createSIDestinationAddress(name, null);        
      DestinationConfiguration dc = coreConnection.getDestinationConfiguration(sida);
        
      // If the Destination exists, then recreate it.        
      if (dc != null)
      {
        String uuid = dc.getUUID();  
        if (tcInt.isDebugEnabled()) SibTr.debug(tcInt, "delete name: "+name);
        if (tcInt.isDebugEnabled()) SibTr.debug(tcInt, "delete UUID: "+uuid);
          
        // The destination has been found, so delete it.
        admin.deleteDestinationLocalization(uuid, null);
             
      } else
      {
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "No object was returned from getDestinationConfiguration");
      }
          
      
    } catch (SINotPossibleInCurrentConfigurationException f)
    {
      // No FFDC code needed
      if (tcInt.isDebugEnabled())
        SibTr.debug(tcInt, "Destination does not exist: "+name, f);
               
    } catch (Exception e)
    {
      // No FFDC code needed
      if (tcInt.isDebugEnabled())
        SibTr.debug(tcInt, "Exception deleting", e);
      JMSException jmse =
        new JMSException("Exception received while deleting");
      jmse.setLinkedException(e);
      jmse.initCause(e);
      
      if (tcInt.isEntryEnabled())
        SibTr.exit(tcInt, "deleteDestination(Destination)");
      throw jmse;
    }

    if (tcInt.isEntryEnabled())
      SibTr.exit(tcInt, "deleteDestination(Destination)");

  }

    
  // *************************** IMPLEMENTATION METHODS ***************************

  /**
   * This method sets up the destination definitions. It is called 
   */
  public void enableDestinationCreation(SICoreConnectionFactory siccf)
  {
    if (tcInt.isEntryEnabled())
      SibTr.entry(tcInt, "enableDestinationCreation");
    try
    {

      if (admin == null)
      {          
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "Setting up destination definition objects.");
          
        // The same object implements the admin interface...      
        admin = ((SIMPAdmin)siccf).getAdministrator();
        
        if (tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "DestinationDefinition objects complete");
        
      }//if
      
    } catch(Exception e)
    {
      // No FFDC code needed
      if (tcInt.isDebugEnabled())
        SibTr.debug(tcInt, "Exception enabling destination creation", e);
    }//try

    if (tcInt.isEntryEnabled())
      SibTr.exit(tcInt, "enableDestinationCreation");
         
  }//enableDestinationCreation
  
  /**
   * Internal method to do the creation given a name a ddf. 
   */
  private void createDestination(String name, 
                                 com.ibm.wsspi.sib.core.DestinationType destType,
                                 Reliability defaultReliability)
    throws JMSException
  {
    if (tcInt.isEntryEnabled())
      SibTr.entry(tcInt, "createDestination(String, DestinationType)");
      
    if (tcInt.isDebugEnabled())
    {
      SibTr.debug(tcInt, "name: "+name);
      SibTr.debug(tcInt, "type: "+destType);
    }
    
    try
    {

      try
      {
        
        // Obtain information about the destination from the core connection.
        SIDestinationAddress sida = JmsServiceFacade.getSIDestinationAddressFactory().createSIDestinationAddress(name, null);        
        DestinationConfiguration dc = coreConnection.getDestinationConfiguration(sida);
        
        // If the Destination exists, then recreate it.        
        if (dc != null)
        {
          String uuid = dc.getUUID();  
          if (tcInt.isDebugEnabled()) SibTr.debug(tcInt, "delete UUID: "+uuid);
          
          // The destination has been found, so delete it.
          admin.deleteDestinationLocalization(uuid, null);
             
        } else
        {
          if (tcInt.isDebugEnabled())
            SibTr.debug(tcInt, "No object was returned from getDestinationConfiguration");
        }
        
        
      } catch(SINotPossibleInCurrentConfigurationException f)
      {
        // No FFDC code needed
        // This is OK. No action required.
      }      
      
      //lohith liberty change
      
      com.ibm.ws.sib.admin.DestinationDefinition adminDDF = null ; //JsAdminFactory.getInstance(.createDestinationDefinition(destType, name);          
      adminDDF.setMaxReliability(Reliability.ASSURED_PERSISTENT);
      adminDDF.setDefaultReliability(defaultReliability);

      // Set the default exception destination
      String excDestinationName = SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION+getMEName();
      adminDDF.setExceptionDestination(excDestinationName);

      //lohith liberty change
      LocalizationDefinition dloc = null; // JsAdminFactory.getInstance().createLocalizationDefinition(name);
      
      // Make sure the max message count is outside the range used by the tests.
      dloc.setDestinationHighMsgs(30000);

      //lohith liberty change
      admin.createDestinationLocalization(adminDDF, dloc ); //, null, null, null, null);
      
      
    } catch (Exception se)
    {
      // No FFDC code needed
      if (tcInt.isDebugEnabled())
        SibTr.debug(tcInt, "Exception creating", se);
        
      // NB. No need to NLS this because it is only for unit test.
      JMSException jmse =
        new JMSException("Exception received creating destination");
      jmse.setLinkedException(se);
      jmse.initCause(se);
      
      if (tcInt.isEntryEnabled())
         SibTr.exit(tcInt, "createDestination(String, DestinationDefinition)");
      throw jmse;
    }
    
    if (tcInt.isEntryEnabled())
       SibTr.exit(tcInt, "createDestination(String, DestinationDefinition)");

  } //createDestination

@Override
public String getMEName() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public SICoreConnectionFactory setupJmsTestEnvironment(boolean coldStart) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void stopME() {
	// TODO Auto-generated method stub
	
}


  /**
   * This method makes use of the MP team's backdoor route for creating an
   * ME. We put this in so that there is only one supported way of doing this,
   * (and mainly because we didn't think it was worth upgrading the one Admin
   * gave us to cope with the influx of WLM code).
   * 
   * @return
   */
 /* private SIMPJsStandaloneEngine getStandaloneEngine(boolean coldStart)
  {
    if (tcInt.isEntryEnabled()) SibTr.entry(tcInt, "getStandaloneEngine");
    try
    {
      
      myME = SIMPJsStandaloneFactory.getInstance().createNewMessagingEngine(UTE_BUSNAME, "jmsTestME", coldStart, true);
      
      myME.initializeMessageProcessor();
      
    } catch (Exception e)
    {     
      // No FFDC code needed
      if (tcInt.isEntryEnabled()) SibTr.debug(tcInt, "Exception getting standalone me", e);
      if (tcInt.isEntryEnabled()) SibTr.exit(tcInt, "getStandaloneEngine");
      
      throw new RuntimeException("Unable to create test environment engine", e);
      
    }//try
    
    if (tcInt.isEntryEnabled()) SibTr.exit(tcInt, "getStandaloneEngine");
    return myME;
       

  }//getStandaloneEngine
*/  
  /**
   * Stop the messaging engine
   * In the current implementation of the stand alone ME, stop()
   * causes a quiesce of the ME. This is being relied upon for
   * JmsConnectionTest to check that events are delivered to the
   * ExceptionListener.
   */
 /* public void stopME()
  {
    //System.out.println("*** stopME ***");
    //Thread.dumpStack();
    try
    {
      // Close off the connection.
      if (coreConnection != null) coreConnection.close();
    } catch(SIException sice)
    {
      // No FFDC code needed
      sice.printStackTrace();
    }
    
    if (myME != null) {
      myME.stop(JsConstants.ME_STOP_QUIESCE); // arg ignored, but pass quiesce
                                              // in case
                                              // they ever do the obvious update.

      // Need to stop the MessageStore as well, otherwise it holds onto the DB
      // lock
      // and the operation times out.
      ((MessageStore)myME.getMessageStore()).stop(JsConstants.ME_STOP_QUIESCE);
      
      // not sure if this is necessary, but it doesn't seem to do any harm
      myME.destroy();
    }

                                            
    // null the reference so that setupJmsTestEnvironment() can be used to create 
    // a new ME.
    myME = null;
    // null the reference so that enableDestinationCreation creates a link to the new
    // ME.
    admin = null;
  }
  */
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.api.jms.ute.UTEHelper#getMEName()
   */
  /*public String getMEName()
  {
    
    // Make sure it has been initialised.
    if (myME == null) setupJmsTestEnvironment();
        
    return myME.getName();
  }*/

}
