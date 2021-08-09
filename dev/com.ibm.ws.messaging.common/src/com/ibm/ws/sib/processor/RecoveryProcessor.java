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
package com.ibm.ws.sib.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsRecoveryMessagingEngine;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
/* This class starts the SIBus messages store in recovery mode and reads the stored messaging engine uuid 
 * and its destinations and links. 
 * */
public interface RecoveryProcessor 
{
	/* This method will start the message store of type database when started in recovery mode
	 * @param recoveryME The instance of the JsRecoveryMessagingEngine 
     	 * @param jndiName The jndiName of the dataSource object referring to the database
     	 * @param authDataAlias The JAAS authentication data alias object of the data source referring to database 
     	 * @param schemaName The schema name of the database which will be used to read the messages store objects.
     	 * */
	public void startMessageDataStore(JsRecoveryMessagingEngine recoveryME,String jndiName,String authDataAlias,String schemaName)throws Exception;
	
	/* This method will start the message store of type file system when started in recovery mode
	 * @param recoveryME The instance of the JsRecoveryMessagingEngine 
     	 * @param logDirectory The directory used for logging by the message store 
     	 * @param permLogDirectory The directory used for storing permanent objects by the message store 
     	 * @param tempLogDirectory The directory used for storing temporary objects by the message store 
     	 * */
	public void startMessageFileStore(JsRecoveryMessagingEngine recoveryME,String logDirectory,String permLogDirectory,String tempLogDirectory)throws Exception;
	
	/* This method will recover the SIBus destinations from the message store 
     	 * @return HashMap The list of the recovered destinations from the message store 
     	 * */
	public HashMap<String,ArrayList<DestinationDefinition>> recoverSIBusDestinations() throws Exception;
	/* This method will recover the SIBus SIBLinks from the message store 
     	 * @return HashMap The list of the recovered SIBLinks from the message store 
     	 * */
	public List<VirtualLinkDefinition> recoverSIBusVirtualLinks() throws Exception;
	/* This method will recover the SIBus MQLinks from the message store 
     	 * @return HashMap The list of the recovered MQLinks from the message store 
     	 * */
	public List<MQLinkDefinition> recoverSIBusMQLinks() throws Exception;
	/* This method will recover and return the stored messaging engine UUID from message store  
     	 * @return String The recovered MEUUID from the message store 
     	 * */
	public String getMEUUID()throws Exception;
	/* This method will stop the message store which is running in recovery mode and all its child threads  
     	 * */
	public void stopMessageStore()throws Exception;
	
}
