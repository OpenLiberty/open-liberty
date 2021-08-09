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

import javax.servlet.sip.annotation.SipApplication;

import com.ibm.ws.sip.container.annotation.SipMergeAction;

//TODO Liberty replace following includes:
//import org.eclipse.jst.j2ee.common.ParamValue;
//import org.eclipse.jst.j2ee.commonarchivecore.internal.MergeData;
//import com.ibm.ws.amm.merge.common.data.WebAppData;
//import com.ibm.ws.amm.merge.webapp.manager.WebAppDataManager;
//import com.ibm.ws.logging.LoggerHelper;
//import com.ibm.ws.logging.WsLogger;
//import com.ibm.wsspi.amm.merge.MergeException;
//import com.ibm.wsspi.amm.scan.AnnotationScanner;
//import com.ibm.wsspi.amm.scan.PackageAnnotationTarget;
//import com.ibm.wsspi.amm.scan.util.info.AnnotationInfo;
//import com.ibm.wsspi.amm.scan.util.info.PackageInfo;
//import com.ibm.wsspi.amm.validate.ValidationException;

/**
 * SIP AMM extension, responsible for parsing @SipApplication annotation
 * and adding the relevant information to the web.xml model
 * 
 * @author asafz
 *
 */
@SuppressWarnings("unchecked")
public class SipApplicationThinMergeAction extends SipMergeAction {
	
	//SipServlet annotation attribute names constants
	private static final String NAME = "name";
	private static final String DISTRIBUTABLE = "distributable";
	private static final String LARGE_ICON = "largeIcon";
	private static final String SMALL_ICON = "smallIcon";
	
	/**
	 * Class Logger.
	 * using logger directly from classes that are related to the deploy process. 
	 * we cannot use our custom logger since it is not working with client side tracing when using CTS deployer
	 */
	private static final String className = SipApplicationThinMergeAction.class.getName(); 
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
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#isPackageTargetsSupported()
	 */
	public boolean isPackageTargetsSupported() {
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#getAnnotationClass()
	 */
	public Class<? extends Annotation> getAnnotationClass() {
		return	SipApplication.class;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#mergePackageTarget(org.eclipse.jst.j2ee.commonarchivecore.internal.MergeData, com.ibm.wsspi.amm.scan.AnnotationScanner, com.ibm.wsspi.amm.scan.PackageAnnotationTarget)
	 */
	//TODO Liberty consider where this functionality had to be added, as AbstractMergeAction no longer exists in Liberty
	/*public void mergePackageTarget(MergeData mergeData,	AnnotationScanner context, PackageAnnotationTarget packageTarget)
	throws MergeException, ValidationException {
		PackageInfo packageInfo = packageTarget.getApplicablePackage();
		
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.entering(className, "mergePackageTarget", packageInfo.getName());
		}
		
		AnnotationInfo annotationInfo = packageInfo.getAnnotation(getAnnotationClass());
		
		//check if sar-to-war was done before
		if (shouldParseAnnotations(mergeData)){
			WebAppData wad = WebAppDataManager.getWebAppData(mergeData);

			Set<String> values = annotationInfo.getValueNames();

			//looking for smallIcon attribute
			if (values.contains(SMALL_ICON)){
				String smallIcon = getString(annotationInfo, SMALL_ICON, DO_TRIM);
				wad.setSmallIcon(smallIcon);
				
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "mergePackageTarget", "small icon was added to the web.xml model, value=" + smallIcon);
				}
			}
			//looking for largeIcon attribute
			if (values.contains(LARGE_ICON)){
				String largeIcon = getString(annotationInfo, LARGE_ICON, DO_TRIM);
				wad.setLargeIcon(largeIcon);
				
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "mergePackageTarget", "large icon was added to the web.xml model, value=" + largeIcon);
				}
			}

			//looking for distributable attribute			
			if (values.contains(DISTRIBUTABLE)){
				boolean  distributable = getBoolean(annotationInfo, DISTRIBUTABLE);
				wad.setDistributable(distributable);
				
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "mergePackageTarget", "distributable was added to the web.xml model, value=" + distributable);
				}
				
			}

			//looking for name attribute			
			String name = null;
			if (values.contains(NAME)){
				name = getString(annotationInfo, NAME, DO_TRIM);
			}else{
				name = packageInfo.getName();
			}
			
			wad.setDisplayName(name);
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "mergePackageTarget", "display name was added to the web.xml model, value=" + name);
			}
			
			//adding context param to web.xml and the application name as the param value
			ParamValue param = addContextParam(wad);
			if (param.getValue() == null || param.getValue().length() == 0){
				param.setValue(name);
			}
		}else{
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "mergePackageTarget", "SipApplication merge action will be skipped, sar-to-war was already processed SIP annotations");
			}
		}
	}*/
}