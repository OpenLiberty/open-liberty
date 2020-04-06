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
package com.ibm.ws.sip.container.annotation;

import java.util.logging.Logger;

//TODO Liberty replace "j2ee.common" classes
//import org.eclipse.jst.j2ee.common.ParamValue;
//import org.eclipse.jst.j2ee.common.internal.impl.CommonFactoryImpl;
//import org.eclipse.jst.j2ee.commonarchivecore.internal.MergeData;
//import org.eclipse.jst.j2ee.webapplication.WebApp;
//import org.eclipse.jst.j2ee.webapplication.WebFragment;

//TODO Liberty replace amm annotation process
//import com.ibm.ws.amm.merge.common.data.WebAppData;
//import com.ibm.ws.logging.LoggerHelper;
//import com.ibm.ws.logging.WsLogger;
//import com.ibm.wsspi.amm.merge.AbstractMergeAction;
//import com.ibm.wsspi.amm.scan.util.info.AnnotationInfo;
//import com.ibm.wsspi.amm.scan.util.info.AnnotationValue;

/**
 * Base class for SIP AMM merge actions extensions.
 * Every SIP annotation AMM extension need to extend this class
 * 
 * @author asafz
 *
 */
public abstract class SipMergeAction /*TODO Liberty extends AbstractMergeAction*/{
	
	/**
	 * Class Logger.
	 * using logger directly from classes that are related to the deploy process. 
	 * we cannot use our custom logger since it is not working with client side tracing when using CTS deployer
	 */
	private static final String className = SipMergeAction.class.getName(); 
	private static Logger c_logger = null;
    static 
	{
    	c_logger = Logger.getLogger(className);
    	/*TODO Liberty if(c_logger instanceof WsLogger){
    		LoggerHelper.addLoggerToGroup(c_logger, "Webui");
    	}*/
	}
    
	private static final String SIP_MODULE_INDICATOR = "META-INF/sip.sipmodule";
	private static final String SIP_ANNOTATION_MARKER_FILE = "WEB-INF/sipAnnotation.read";
	public static final String SIP_CONTAINER_CONTEXT_PARAM = "com.ibm.ws.sip.container.annotation";
	public static final boolean DO_TRIM = true;
	public static final boolean DO_NOT_TRIM = false;

	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.MergeAction#getApplicableTypes()
	 */
	public Class[] getApplicableTypes(){
		return (Class[])(new Class[] {
				/*TODO Liberty WebApp.class, WebFragment.class*/
		});
	}

	/**
	 * get String from annotation attribute
	 * 
	 * @param annotationInfo
	 * @param valueName - the name of the annotation attribute that we want to get
	 * @param doTrim
	 * @return
	 */
	//TODO Liberty replace method implementation
	/*protected String getString(AnnotationInfo annotationInfo, String valueName, boolean doTrim){
		AnnotationValue value = annotationInfo.getValue(valueName);
		if(value == null){
			return null;	
		}
			
		String stringValue = value.getStringValue();
		
		if(stringValue == null){
			return null;
		}
			
		if(doTrim){
			stringValue = stringValue.trim();
		}
			
		if(stringValue.isEmpty()){
			return null;
		}else{
			return stringValue;
		}
	}*/

	/**
	 * get Boolean from annotation attribute
	 * 
	 * @param annotationInfo
	 * @param valueName - the name of the annotation attribute that we want to get
	 * @param doTrim
	 * @return
	 */
	//TODO Liberty replace method implementation
	/*protected boolean getBoolean(AnnotationInfo annotationInfo, String valueName){
		AnnotationValue value = annotationInfo.getValue(valueName);
		if(value == null){
			return false;
		}

		Boolean booleanValue = value.getBooleanValue();
		if(booleanValue == null){
			return false;
		}

		return booleanValue;
	}*/
	
	/**
	 * add the sip context param to the web.xml module 
	 * this context param is used to indicate that this module 
	 * include SIP annotations and that it was processed by the SIP AMM
	 * extensions, this information is used lated during the application startup 
	 * 
	 * @param wad
	 * @return - the context param value
	 */
	//TODO Liberty replace method implementation
	/*protected ParamValue addContextParam(WebAppData wad){
		Collection<ParamValue> params = wad.getContextParams();
		for (ParamValue paramValue : params) {
			if (paramValue.getName().equals(SIP_CONTAINER_CONTEXT_PARAM)){
				//the context param was found return it
				return paramValue;
			}
		}
		
		//we did not find the sip context param, create one and return it
		ParamValue param = CommonFactoryImpl.init().createParamValue();
		param.setName(SIP_CONTAINER_CONTEXT_PARAM);
		param.setValue("");
		wad.addCtxParam(param);
		return param;
	}*/
	
	/**
	 * check if we need to parse annotations using amm, we can skip this parsing if we already went thru the 
	 * sar-to-war processing
	 * 
	 * @param mergeData
	 * @return
	 */
	//TODO Liberty replace method implementation
	/*protected boolean shouldParseAnnotations(MergeData mergeData){
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.entering(className, "shouldParseAnnotations");
		}
		//check if we already serialized sip annotations to the sip/web.xml files using sar-to-war. 
		//if we have sip annotations and we gone thru the sar-to-war process a marker files will be added to the web module
		if (mergeData.getModuleFile().containsFile(SIP_ANNOTATION_MARKER_FILE)){
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "shouldParseAnnotations", "found sip annotation marker in the module, skipping amm parsing");
			}
			return false;
		}
		
		//check if we already gone thru the sar-to-process for ear files
		if (mergeData.getModuleFile().getEARFile() != null && mergeData.getModuleFile().getEARFile().containsFile(SIP_MODULE_INDICATOR)){
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "shouldParseAnnotations", "found sip sar-to-war indicator in the ear file, skipping amm parsing");
			}
			return false;
		}
		
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.exiting(className, "shouldParseAnnotations", true);
		}
		
		return true;
	}*/
}