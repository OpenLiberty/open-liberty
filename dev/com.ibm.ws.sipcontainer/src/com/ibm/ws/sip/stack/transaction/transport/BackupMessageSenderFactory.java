/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.transport;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.util.ObjectPool;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * Class that creates appropriate object for the INAPTRProcessor interface. It
 * will examine the target SipURI and decides which implementation should be
 * used.
 * 
 * @author Anat Fradin, Dec 6, 2006
 * 
 */
public class BackupMessageSenderFactory {
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(BackupMessageSenderFactory.class);

    /**
     * The singleton instance
     */
	public static BackupMessageSenderFactory s_instance = null;
    /**
     * The String of the class of the Naptr Sender within WAS. 
     */
    private final static String NAPTR_SENDER_CLASS_STR = "com.ibm.ws.sip.stack.naptr.NaptrSenderStack";
    
    /**
     * The class of the Naptr Sender within WAS. 
     */
    private static Class s_naptrSenderClass;
    
    /** 
     * pool of NaptrSender objects 
     */
    private static ObjectPool s_naptrSenderPool = null;
    
    /** 
     * flag indicatinf if naptr sender class was found 
     */
    private static boolean s_naptrSenderFound = false;
    
    private static boolean s_naptrSenderEnabled = false;
    
	/**
	 * Ctor
	 */
    private BackupMessageSenderFactory(){
        try {
			s_naptrSenderClass = Class.forName(NAPTR_SENDER_CLASS_STR);
			s_naptrSenderFound = true;
			s_naptrSenderEnabled = isStackNaptrAutoResolveEnabled();
			
            int maxPoolSize = ApplicationProperties.getProperties().getInt(StackProperties.MAX_NAPTR_SENDER_POOL_SIZE);
	        s_naptrSenderPool = new ObjectPool(s_naptrSenderClass,null,maxPoolSize);
		} catch (ClassNotFoundException e) {
			if (c_logger.isTraceDebugEnabled()) {

				StringBuffer buff = new StringBuffer();
				buff.append("Could not find the class required for naptr: ");
				buff.append(s_naptrSenderClass);
				
				c_logger.traceDebug(buff.toString());
			}
		}
    }

	/**
	 * Decides which implementation of the INAPTRProcessor should be created.
	 * @return
	 */
	public IBackupMessageSender getBackupSender() {
		if (s_naptrSenderEnabled && s_naptrSenderFound) {
			IBackupMessageSender responseSender = (IBackupMessageSender) s_naptrSenderPool.get();
			responseSender.setIsPoolable(true);
			return responseSender;
		} 
		else {
			//Will return Sender if Naptr is disabled.
			return DefaultBackupMessageSender.getInstance();
		}
	}
	
	/**
	 * @return if NAPRT auto resolve is enabled.
	 */
	private static boolean isStackNaptrAutoResolveEnabled() {
		boolean result = false;
		
		SipPropertiesMap sipProp = ApplicationProperties.getProperties();
		
		boolean autoResolve;
		
		autoResolve = sipProp.getBoolean(StackProperties.DNS_SERVER_AUTO_RESOLVE);
		
		if (autoResolve) {
			String[] dnsServers = (String[])sipProp.getObject(StackProperties.DNSSERVERNAMES);
			if (dnsServers.length == 0) {
				result = false;
			} else {
				result = true;
			}
		}
		return result;
	}	
	
	 /**
	  * Method that notifies the SenderFactory that Client ended to use
	  * the sender as final response was received.
	  *
	  */
	 public void finishToUseSender(IBackupMessageSender sender){
		 if (sender == null){
			 return;
		 }
		 if (sender.isPoolable() && s_naptrSenderFound) {
			 // clean and put back to the queue only if poolabe
			sender.cleanItself();
			s_naptrSenderPool.putBack(sender);
		}
	 }
	 
	 public static BackupMessageSenderFactory instance(){
		 if (s_instance == null){
			 synchronized(BackupMessageSenderFactory.class){
				 if (s_instance == null){
					 s_instance = new BackupMessageSenderFactory();
				 }
			 }
		 }
		 return s_instance;
	 }
}
