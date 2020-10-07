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
package com.ibm.ws.sip.container.annotation.thin;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;

import javax.servlet.sip.annotation.SipListener;

import com.ibm.ws.sip.container.annotation.SipMergeAction;

//TODO Liberty replace following includes:
//import org.eclipse.jst.j2ee.commonarchivecore.internal.MergeData;
//import com.ibm.ws.amm.merge.common.data.WebAppData;
//import com.ibm.ws.amm.merge.webapp.manager.WebAppDataManager;
//import com.ibm.ws.logging.LoggerHelper;
//import com.ibm.ws.logging.WsLogger;
//import com.ibm.wsspi.amm.merge.MergeException;
//import com.ibm.wsspi.amm.scan.AnnotationScanner;
//import com.ibm.wsspi.amm.scan.ClassAnnotationTarget;
//import com.ibm.wsspi.amm.validate.ValidationException;

/**
 * SIP AMM extension, responsible for parsing @SipListener annotation
 * and adding the relevant information to the web.xml model
 * 
 * @author asafz
 *
 */
@SuppressWarnings("unchecked")
public class SipListenerThinMergeAction extends SipMergeAction {
	/**
	 * Class Logger.
	 * using logger directly from classes that are related to the deploy process. 
	 * we cannot use our custom logger since it is not working with client side tracing when using CTS deployer
	 */
	private static final String className = SipListenerThinMergeAction.class.getName(); 
	private static Logger c_logger = null;
    static 
	{
    	c_logger = Logger.getLogger(className);
    	/*TODO Liberty if(c_logger instanceof WsLogger){
    		LoggerHelper.addLoggerToGroup(c_logger, "Webui");
    	}*/
	}
	
    /*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#isClassTargetsSupported()
	 */
	public boolean isClassTargetsSupported () {
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#getAnnotationClass()
	 */
	public Class<? extends Annotation> getAnnotationClass() {
		return	SipListener.class;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#mergeClassTarget(org.eclipse.jst.j2ee.commonarchivecore.internal.MergeData, com.ibm.wsspi.amm.scan.AnnotationScanner, com.ibm.wsspi.amm.scan.ClassAnnotationTarget)
	 */
	//TODO Liberty consider where this functionality had to be added, as AbstractMergeAction no longer exists in Liberty
	/*public void mergeClassTarget (MergeData mergeData, AnnotationScanner context, ClassAnnotationTarget classTarget) throws MergeException, ValidationException {
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.entering(className, "mergeClassTarget", classTarget.getApplicableClass());
		}
		
		//check if sar-to-war was done before
		if (shouldParseAnnotations(mergeData)){
			//adding context param to web.xml
			WebAppData wad = WebAppDataManager.getWebAppData(mergeData);
			addContextParam(wad);
		}else{
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "mergeClassTarget", "SipListener merge action will be skipped, sar-to-war was already processed SIP annotations");
			}
		}
	}*/
}