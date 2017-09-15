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
package com.ibm.ws.sib.processor.impl;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsRecoveryMessagingEngine;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.comms.mq.MQLinkObject;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.WASConfiguration;
import com.ibm.ws.sib.processor.RecoveryProcessor;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageProcessorCorruptException;
import com.ibm.ws.sib.processor.impl.store.MessageProcessorStore;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.wsspi.sib.core.DestinationType;

public class RecoveryProcessorImpl implements RecoveryProcessor {
	
	MessageStore msgStore;
	MessageProcessorStore _persistentStore;
	private String busName;
	private String meUUId;
	DestinationManager destinationManager;

	
	private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
	
	@Override
	/* This method will recover the SIBus destinations from the message store 
     	 * @return HashMap The list of the recovered destinations from the message store 
     	 * */
	public synchronized HashMap<String,ArrayList<DestinationDefinition>> recoverSIBusDestinations() throws Exception
	{
		HashMap<String, ArrayList<DestinationDefinition>> destinationsMap=new HashMap<String,ArrayList<DestinationDefinition>>();
		if(_persistentStore!=null )
		{
			destinationManager=(DestinationManager) _persistentStore.findFirstMatchingItemStream(
			          new ClassEqualsFilter(DestinationManager.class));
		      // Sanity - A PersistentStore should not be in the MessageStore without
		      // a DestinationManager!
		      if (null == destinationManager)
		      {
		        SIMPMessageProcessorCorruptException e =
		          new SIMPMessageProcessorCorruptException(
		            nls.getFormattedMessage(
		              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
		              new Object[] {
		                "com.ibm.ws.sib.processor.impl.RecoveryProcessorImpl",
		                "1:2284:1.443" },
		              null));	        
		        throw e;
		      }
	    		NonLockingCursor cursor= initializeDestinationManagerDestinations(destinationManager);
			AbstractItem item = null;
			ArrayList<DestinationDefinition> destQueues=new ArrayList<DestinationDefinition>();
			ArrayList<DestinationDefinition> destTopicSpaces=new ArrayList<DestinationDefinition>();
			ArrayList<DestinationDefinition> destAlias=new ArrayList<DestinationDefinition>();
			ArrayList<DestinationDefinition> destForeign=new ArrayList<DestinationDefinition>();
			while (null != (item = cursor.next()))
			{
			      	BaseDestinationHandler dh = (BaseDestinationHandler) item;
			      	DestinationDefinition def=dh.definition;		      
			      	DestinationType type=DestinationType.getDestinationType(def.getDestinationType().toInt());
			            
			      	if(type.toString().equalsIgnoreCase("Queue"))
			      	{
					if( !(def.getName().endsWith(meUUId)) || !(def.getName().startsWith("_P")))//system destinations are not selected for recovery		    	  
			    		{
			    		destQueues.add(def);
			    		}		    	  
			    	
			      	}else if(type.toString().equalsIgnoreCase("TopicSpace"))
			      	{
			    		  destTopicSpaces.add(def);  
			    	 
			      	}else if(type.toString().equalsIgnoreCase("Alias"))
			      	{
			    	  destAlias.add(def);
			      	}else if(type.toString().equalsIgnoreCase("Foreign"))
			      	{
			    	  destForeign.add(def);
			      	}		      
			 }
			cursor.finished();
			destinationsMap.put("QUEUE", destQueues);
			destinationsMap.put("TOPICSPACE", destTopicSpaces);
			destinationsMap.put("ALIAS", destAlias);
			destinationsMap.put("FOREIGN", destForeign);
		}else
		{
		throw new MessageStoreException("Invalid Message store because MessageProcessorStore instance is null.");
		}
		return destinationsMap;
	}


	@Override
	/* This method will start the message store of type database when started in recovery mode
	 * @param recoveryME The instance of the JsRecoveryMessagingEngine 
     	 * @param jndiName The jndiName of the dataSource object referring to the database
     	 * @param authDataAlias The JAAS authentication data alias object of the data source referring to database 
     	 * @param schemaName The schema name of the database which will be used to read the messages store objects.
     	 * */
	public void startMessageDataStore(JsRecoveryMessagingEngine recoveryME,String jndiName,String authDataAlias,String schemaName) throws Exception 
	{
		final WASConfiguration configuration = WASConfiguration.getDefaultWasConfiguration();
		configuration.setCleanPersistenceOnStart(false);
		configuration.setCreateTablesAutomatically(false);
		configuration.setPersistentMessageStoreClassname(MessageStoreConstants.PERSISTENT_MESSAGE_STORE_CLASS_DATABASE);
		configuration.setDatasourceJndiName(jndiName);
		configuration.setAuthenticationAlias(authDataAlias);
		configuration.setDatabaseSchemaName(schemaName);
		this.busName=recoveryME.getBusName();
		msgStore=MessageStore.createInstance();
		String fullkey = MessageStoreConstants.STANDARD_PROPERTY_PREFIX+MessageStoreConstants.START_MODE;
		msgStore.setCustomProperty(fullkey, "RECOVERY");
		msgStore.initialize(configuration);
		msgStore.initialize(recoveryME, "recovery");			
		msgStore.start();			
		_persistentStore =(MessageProcessorStore) msgStore.findFirstMatching(new ClassEqualsFilter(MessageProcessorStore.class));
		this.meUUId=_persistentStore.getMessagingEngineUuid().toString();
	
	}


	@Override
	/* This method will recover the SIBus MQLinks from the message store 
     	 * @return HashMap The list of the recovered MQLinks from the message store 
     	 * */
	public List<MQLinkDefinition> recoverSIBusMQLinks() throws Exception 
	{
		ArrayList<MQLinkDefinition> mqLinks=new ArrayList<MQLinkDefinition>();
		if(msgStore!=null )
		{				
			NonLockingCursor cursor=initializeDestinationManagerMQLinks(destinationManager);
			AbstractItem item = null;
			while (null != (item = cursor.next()))
			{
			 MQLinkHandler mqLink = (MQLinkHandler) item;
			 if(mqLink!=null)
			 	{
				 MQLinkObject def=  mqLink.getMQLinkObject();
				 if(def!=null)
				 {
				 def.toString();
				 }
			    
			      	DataSlice slice = mqLink.getPersistentData().get(0);
			      	ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(slice.getBytes()));
			      	mqLink.restore(ois, ois.readInt());
			      
				} 
			   
			 }
			cursor.finished();
			}
			return mqLinks;		
	}


	@Override
	/* This method will recover the SIBus SIBLinks from the message store 
     	 * @return HashMap The list of the recovered SIBLinks from the message store 
     	 * */
	public List<VirtualLinkDefinition> recoverSIBusVirtualLinks() throws Exception 
	{
		
		NonLockingCursor cursor= initializeDestinationManagerSIBLinks(destinationManager);
	    	AbstractItem item = null;
	    	ArrayList<VirtualLinkDefinition> linkDefs=new ArrayList<VirtualLinkDefinition>(); 
	    	while (null != (item = cursor.next()))
	    	{
	      		LinkHandler linkHandler = null;
	      		linkHandler = (LinkHandler) item;
           		if (linkHandler.isToBeIgnored())
	      		{

	      		}
	      		else
	      		{
		    	linkHandler.restore(linkHandler.getPersistentData());
	      		}
	    	}
	    	cursor.finished();
		return linkDefs;
	}


	@Override
	/* This method will start the message store of type file system when started in recovery mode
	 * @param recoveryME The instance of the JsRecoveryMessagingEngine 
     	 * @param logDirectory The directory used for logging by the message store 
     	 * @param permLogDirectory The directory used for storing permanent objects by the message store 
     	 * @param tempLogDirectory The directory used for storing temporary objects by the message store 
     	 * */
	public void startMessageFileStore(JsRecoveryMessagingEngine recoveryME,String logDirectory,String permLogDirectory, String tempLogDirectory) throws Exception 
	{
		final WASConfiguration configuration = WASConfiguration.getDefaultWasConfiguration();
		configuration.setCleanPersistenceOnStart(false);
		configuration.setCreateTablesAutomatically(false);
		configuration.setPersistentMessageStoreClassname(MessageStoreConstants.PERSISTENT_MESSAGE_STORE_CLASS_OBJECTMANAGER);
		configuration.setObjectManagerLogDirectory(logDirectory);
		configuration.setObjectManagerPermanentStoreDirectory(permLogDirectory);
		configuration.setObjectManagerTemporaryStoreDirectory(tempLogDirectory);
		configuration.setCreateTablesAutomatically(false);
		this.busName=recoveryME.getBusName();
		msgStore=MessageStore.createInstance();
		String fullkey = MessageStoreConstants.STANDARD_PROPERTY_PREFIX+MessageStoreConstants.START_MODE;
		msgStore.setCustomProperty(fullkey, "RECOVERY");
		msgStore.initialize(configuration);
		msgStore.initialize(recoveryME, "recovery");			
		msgStore.start();
		_persistentStore =(MessageProcessorStore) msgStore.findFirstMatching(new ClassEqualsFilter(MessageProcessorStore.class));
		this.meUUId=_persistentStore.getMessagingEngineUuid().toString();		
	}


	@Override
	/*Method stops the message store and its child object threads
	*/
	public void stopMessageStore() throws Exception {
		msgStore.stop(0);
		
	}
	/*The method returns the messaging engine uuid from the messagestore
	*/
	public String getMEUUID()throws Exception
	{		
		return this.meUUId;
		
	}
	private NonLockingCursor initializeDestinationManagerDestinations(DestinationManager destinationManager) throws MessageStoreException{

	      return destinationManager.newNonLockingItemStreamCursor(new ClassEqualsFilter(BaseDestinationHandler.class));
	}
	private NonLockingCursor initializeDestinationManagerSIBLinks(DestinationManager destinationManager) throws MessageStoreException{
		
		return destinationManager.newNonLockingItemStreamCursor(new ClassEqualsFilter(LinkHandler.class));
	}
	private NonLockingCursor initializeDestinationManagerMQLinks(DestinationManager destinationManager) throws MessageStoreException{
		
		return destinationManager.newNonLockingItemStreamCursor(new ClassEqualsFilter(MQLinkHandler.class));
	}



}
