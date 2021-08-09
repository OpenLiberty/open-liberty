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

import javax.servlet.sip.annotation.SipServlet;

import com.ibm.ws.sip.container.annotation.SipMergeAction;

//TODO Liberty replace following includes:
//import org.eclipse.jst.j2ee.commonarchivecore.internal.MergeData;
//import org.eclipse.jst.j2ee.webapplication.Servlet;
//import org.eclipse.jst.j2ee.webapplication.WebapplicationFactory;
//import com.ibm.ws.amm.merge.common.data.WebAppData;
//import com.ibm.ws.amm.merge.webapp.manager.WebAppDataManager;
//import com.ibm.ws.logging.LoggerHelper;
//import com.ibm.ws.logging.WsLogger;
//import com.ibm.wsspi.amm.merge.MergeException;
//import com.ibm.wsspi.amm.scan.AnnotationScanner;
//import com.ibm.wsspi.amm.scan.ClassAnnotationTarget;
//import com.ibm.wsspi.amm.scan.util.info.AnnotationInfo;
//import com.ibm.wsspi.amm.scan.util.info.ClassInfo;
//import com.ibm.wsspi.amm.validate.ValidationException;

/**
 * SIP AMM extension, responsible for parsing @SipServlet annotation
 * and adding the relevant information to the web.xml model
 * 
 * @author asafz
 *
 */
@SuppressWarnings("unchecked")
public class SipServletThinMergeAction extends SipMergeAction {
	
	//SipServlet annotation attribute names constants
	private static final String DESCRIPTION = "description";
	private static final String NAME = "name";
	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#isClassTargetsSupported()
	 */
	public boolean isClassTargetsSupported () {
		return true;
	}
	
	/**
	 * Class Logger.
	 * using logger directly from classes that are related to the deploy process. 
	 * we cannot use our custom logger since it is not working with client side tracing when using CTS deployer
	 */
	private static final String className = SipServletThinMergeAction.class.getName(); 
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
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#getAnnotationClass()
	 */
	public Class<? extends Annotation> getAnnotationClass() {
		return	SipServlet.class;
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
			ClassInfo classInfo = classTarget.getApplicableClass();
			Class clazz = null;

			//check if we can an instance of the siplet class
			try {
				clazz = classInfo.getInstance();
			} catch (ClassNotFoundException exception) {
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "mergeClassTarget", "Can't init object for " + classInfo.getName(), exception);
				}

				return;
			} 

			// Validate that servlet class extends SipServlet 
			if (!isOffspringOfSipServlet(clazz)){
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "mergeClassTarget", clazz.getCanonicalName() + " does not extends SipServlet class");
				}
				return;
			}

			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "mergeClassTarget", "processing SipServlet annotatoion for siplet: " + clazz.getName());
			}
			
			//creating the new Servlet object
			WebapplicationFactory f = WebapplicationFactory.eINSTANCE;
			Servlet newServlet = f.createServlet();

			AnnotationInfo annotationInfo = classInfo.getAnnotation(getAnnotationClass());
			Set<String> values = annotationInfo.getValueNames();
			
			//looking for name attribute
			String servletName = getString(annotationInfo, NAME, DO_TRIM);
			if (servletName == null || servletName.length() == 0) {
				servletName = clazz.getSimpleName();
			}

			newServlet.setServletName(servletName);
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "mergeClassTarget", "servlet name was added to the web.xml model, value=" + servletName);
			}

			//looking for description attribute
			String description = getString(annotationInfo, DESCRIPTION, DO_NOT_TRIM);
			if (description!=null) {
				newServlet.setDescription(description);
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "mergeClassTarget", "servlet description was added to the web.xml model, value=" + description);
				}
			}

			//adding the information to the web.xml if the servlet does not already exist
			WebAppData wad = WebAppDataManager.getWebAppData(mergeData);
			Servlet existingServlet = wad.getServletNameToServletMap().get(servletName);
			if (existingServlet == null){
				String[] mapping = new String[]{"/" + servletName};
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "mergeClassTarget", "new servlet was added to the web.xml, name = " + servletName);
				}
				wad.addServlet(newServlet, classInfo.getName(), mapping, mergeData);
			}

			//adding context param to web.xml
			addContextParam(wad);
		}else{
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "mergeClassTarget", "SipServlet merge action will be skipped, sar-to-war was already processed SIP annotations");
			}
		}
	}*/
	
	/**
	 * check if the class is offspring of SipServlet class
	 * 
	 * @param clazz
	 * @return
	 */
	private boolean isOffspringOfSipServlet(Class clazz){

		if (clazz.getSuperclass() == null){
			return false;
		}
		
		if (clazz.getSuperclass().equals(javax.servlet.sip.SipServlet.class)){
			return true;
		}

		return isOffspringOfSipServlet(clazz.getSuperclass());
	}
}