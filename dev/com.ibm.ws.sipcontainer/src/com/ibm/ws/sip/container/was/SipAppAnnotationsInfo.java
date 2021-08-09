/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.sip.annotation.SipApplication;
import javax.servlet.sip.annotation.SipApplicationKey;
import javax.servlet.sip.annotation.SipListener;
import javax.servlet.sip.annotation.SipServlet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.FragmentAnnotations;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.MethodInfo;
import com.ibm.wsspi.anno.info.PackageInfo;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * 
 * @author anatf
 *
 * This class is responsible to read all defined by the JSR 289 annotations .
 */
public class SipAppAnnotationsInfo {

	private static final TraceComponent tc = Tr.register(SipAppAnnotationsInfo.class);
    
	private String _appName;
	private String _appDescription;
	private String _mainServlet;
	
	// Name of the related Web Application
	private String _webAppName = null;
	
	private static String NAME = "name";
	private static String DISPLAY_NAME = "displayName";
	private static String DESCRIPTION = "description";
	
	//Related to the @SipApplication Annotation
	private static String SESSION_TIMEOUT = "sessionTimeout";
	private static String DISTRIBUTABLE = "distributable";
	private static String PROXY_TIMEOUT = "proxyTimeout";
	private static String MAIN_SERVLET = "mainServlet";
	private static String LARGE_ICON = "largeIcon";
	private static String SMALL_ICON = "smallIcon";
	
	//Related to the @SipServlet
	private static String LOAD_ON_STARTUP= "loadOnStartup";
	private static String APP_NAME = "applicationName";
	
	/** Reference to the annotations */
	private WebAnnotations _webAnnotations = null;
	private FragmentAnnotations _fragmentAnnotations = null;
	
	private String error = null;
	
	/**
	 * Ctor
	 * @param webAppName 
	 * @param webAnnotations
	 * @param fragmentAnnotations2 
	 * @throws UnableToAdaptException 
	 */
	public SipAppAnnotationsInfo(String moduleName, WebAnnotations webAnnotations, FragmentAnnotations fragmentAnnotations) throws UnableToAdaptException {
		_webAnnotations = webAnnotations;
		_fragmentAnnotations = fragmentAnnotations;
		_webAppName = moduleName;
	}

	/**
	 * Creates appropriate SipAppDesc object.
	 * @return
	 */
    public SipAppDesc createSipAppDesc() {
    	    	
    	if(_fragmentAnnotations == null || _webAnnotations == null)
			return null;
    	
//    	First, try to get the existing SipAppDesc according the the module name
    	SipAppDesc newApp = SipAppDescManager.getInstance().getSipAppDesc(_webAppName);
    	try {
    			//Try to craete new SipAppDes - look for the @SipApplication annotation
    		newApp = readSipApplicationAnnotations(newApp);
			
			if(newApp != null){
				readSipServlets(newApp);
		    	readSipListeners(newApp);
		    	createSipAppKeys(newApp);
	    	}
			else{
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
		            Tr.debug(tc, "createSipAppDesc", "Application has no appropriate DD , neigther @SipApplication annotation" );
		        }
			}
		} catch (UnableToAdaptException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "createSipAppDesc", "UnableToAdaptException = " + e.getStackTrace());
	        }
		}
        
    	return newApp;
	}

    /**
     * Read information about @SipListener annotated classes
     * @param fragmentAnnotations
     * @throws UnableToAdaptException
     */
	private void readSipListeners(SipAppDesc application) throws UnableToAdaptException {
		Set<String> sipListenerClassesName = _fragmentAnnotations.selectAnnotatedClasses(SipListener.class);
		
    	if(sipListenerClassesName.isEmpty()){
    		 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    	            Tr.debug(tc, "processSipServletAnnotations", "No Listeners found ");
    	        }
    		return;
    	}
    	
        List<String> _sipListeners = new LinkedList<String>();
    	
        
		for (String cName : sipListenerClassesName) {
            final String fullyQualifiedClassName = cName;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@SipListener found on class {0}", fullyQualifiedClassName);
            }

             _sipListeners.add(fullyQualifiedClassName);
        }
        
        application.setSipListeners(_sipListeners);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processSipServletAnnotations", "Listeners = " + sipListenerClassesName);
        }
	}
    
	/**
     * Read and store in SipAppDesc information about SipApplicationKey annotated method
     * @param fragmentAnnotations
     * @throws UnableToAdaptException
     */
    private void createSipAppKeys(SipAppDesc app) throws UnableToAdaptException {
    	
    	AnnotationTargets_Targets targets = _webAnnotations.getAnnotationTargets();
    	Set<String> annotatedClasses = targets.getClassesWithMethodAnnotation(SipApplicationKey.class.getName());
    	
        for (String cName : annotatedClasses) {
            ClassInfo classInfo = _webAnnotations.getClassInfo(cName);
            
            List<? extends MethodInfo> annotatedMethods =  classInfo.getDeclaredMethods();
            
            for (MethodInfo mName : annotatedMethods) {
            	if(mName.isDeclaredAnnotationPresent()){
            		AnnotationInfo annotation = mName.getDeclaredAnnotation(SipApplicationKey.class);
            		if(annotation != null){
							app.setAppKeyInfo(cName,mName.getName());
//							should be only one annotated as @SipApplicationKey method
							return;
            		}
            	}
            }
        }
	}

	/**
     * Read SipApplication annotations
	 * @param sipApp 
     * @param webAnnotations
     * @param fragmentAnnotations
     * @throws UnableToAdaptException
     */
	private SipAppDesc readSipApplicationAnnotations(SipAppDesc sipApp) throws UnableToAdaptException {
		
		int sessionTimeout;
		int proxyTimeout;
		boolean distributable;
//		TODO Anat: we need to read smallIcon and largeIcon from Application annotation
//		String smallIcon, largeIcon;

		if(_webAnnotations == null){
			return null;
		}
	
		SipAppDesc appDesc = sipApp;
		
		AnnotationTargets_Targets targets = _webAnnotations.getAnnotationTargets();
        Set<String> appSessions = targets.getAnnotatedPackages(SipApplication.class.getName());
        
        for (String targetClassName : appSessions) {
        	PackageInfo info = _webAnnotations.getInfoStore().getPackageInfo(targetClassName);
            final String fullyQualifiedClassName = targetClassName;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@SipApplication found on class {0}", fullyQualifiedClassName);
            }

            AnnotationInfo runAsAnnotation = info.getAnnotation(SipApplication.class.getName());
            if(runAsAnnotation != null){
            	AnnotationValue value = runAsAnnotation.getValue(NAME);
            	if(value != null){
            		_appName = value.getStringValue();
            		value = runAsAnnotation.getValue(DISPLAY_NAME);
            		String displayName = null;
            		
            		if(value != null){
            			displayName = value.getStringValue();
            		}
            		else{
            			displayName = _appName;
            		}
            		
            		if(appDesc == null){
            			//create new SipAppDesc, there is no sip.xml 
	            		appDesc = new SipAppDesc(displayName,_appName);
	            		appDesc.setWebAppName(_webAppName);
	            		SipAppDescManager.getInstance().addNewApp(appDesc);
            		}
            		else{
            			// If sip.xml didn't configure app-name, use the annotation app-name
            			if(appDesc.getAppNameMember() == null){
            				appDesc.setAppName(_appName);
            			}
            			
            			// If sip.xml exists, it should have the same application name as @SipApplication annotation
            			if(!appDesc.getAppName().equalsIgnoreCase(_appName)){
            				 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            			            Tr.debug(tc, "Application name in sip.xml should match the @SipApplication");
            			        }
            				error = "Application name in sip.xml should match the @SipApplication";
            				return null;
            			}
            		}
            	}
            	else if (appDesc == null ){
            		// If there is no sip.xml and no @SipApplication - application should not be deployed
       				 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
       			            Tr.debug(tc, "sip.xml or @SipApplication not found.");
       			        }
       				error = "sip.xml or @SipApplication not found.";
            		return null;
            	}
            	
            	value = runAsAnnotation.getValue(DESCRIPTION);
            	if(value != null){
            		_appDescription = value.getStringValue();
            		if(appDesc.getDescription() == null || !appDesc.getDescription().equalsIgnoreCase(_appDescription)){
            			appDesc.setDescription(_appDescription);
            		}
            	}
            	
            	value = runAsAnnotation.getValue(SESSION_TIMEOUT);
            	if(value != null){
            		sessionTimeout = value.getIntValue();
            		if(appDesc.getAppSessionTTL() != sessionTimeout){
                		appDesc.setAppSessionTTL(sessionTimeout);
            		}
            	}
            	
            	value = runAsAnnotation.getValue(PROXY_TIMEOUT);
            	if(value != null){
            		proxyTimeout = value.getIntValue();
            		if(appDesc.getProxyTimeout() != proxyTimeout){
            			appDesc.setProxyTimeout(proxyTimeout);
            		}
            	}
            	
            	value = runAsAnnotation.getValue(DISTRIBUTABLE);
            	if(value != null){
            		distributable = value.getBoolean();
            		if(appDesc.isDistributed() != distributable){
            			appDesc.setIsDistributed(distributable);
            		}
            	}
            	
            	value = runAsAnnotation.getValue(MAIN_SERVLET);
            	if(value != null){
            		_mainServlet = value.getStringValue();
            		if(_mainServlet !=  null && !_mainServlet.isEmpty()){
            			if(appDesc.getMainSipletName() == null || !appDesc.getMainSipletName().equalsIgnoreCase(_mainServlet)){
                			appDesc.setMainSipletName(_mainServlet);
                		}	
            		}
            	}
            	
            	value = runAsAnnotation.getValue(SMALL_ICON);
            	if(value != null){
					// We do not use this value in SipContainer
					// smallIcon = value.getStringValue();
            	}
            	
            	value = runAsAnnotation.getValue(LARGE_ICON);
            	if(value != null){
					// We do not use this value in SipContainer
					// largeIcon = value.getStringValue();
            	} 
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "After loading annotations, SipApplications: " + appSessions);
        }
    	
    	return appDesc;
    }
	
	/**
	 * Read information about @SipServlet annotated classes
	 * @param fragmentAnnotations
	 * @throws UnableToAdaptException 
	 */
	private void readSipServlets(SipAppDesc application) throws UnableToAdaptException {
        
		String servletname = null;
		String applicationName = null;
		String description = null;
		
        // The default values for load-on-startup.
		int loadOnStartup = -1;
		boolean hasLoadOnStartupTag = false;
		AnnotationValue value = null;
    	 
		Set<String> sipServletClassesName = _fragmentAnnotations.selectAnnotatedClasses(SipServlet.class);
		
        for (String targetClassName : sipServletClassesName) {
        	
            ClassInfo classInfo = _webAnnotations.getClassInfo(targetClassName);
            final String fullyQualifiedClassName = targetClassName;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "@SipServlet found on class {0}", fullyQualifiedClassName);
            }

             AnnotationInfo annotatedServlet = classInfo.getAnnotation(SipServlet.class);

             value = annotatedServlet.getValue(APP_NAME);
             if(value != null){
             	applicationName = value.getStringValue();
//             	TODO Anat : associate this serlvet to another Application ?!
             }
             
             value = annotatedServlet.getValue(NAME);
             if(value != null){
             	servletname = value.getStringValue();
             }
             
             // set short class name if servlet name wasn't specified.
             if(servletname.equals("")){
            	 String packageName = classInfo.getPackageName();
            	 // extract short class name 
            	 servletname = classInfo.getName().split(packageName)[1].substring(1);
             }
             
             value = annotatedServlet.getValue(DESCRIPTION);
             if(value != null){
             	description = value.getStringValue();
             	if(description != null){
             		_appDescription = description;
             	}
             }
             
             value = annotatedServlet.getValue(LOAD_ON_STARTUP);
             if(value != null){
             	loadOnStartup = value.getInteger();
             	
             	if (!annotatedServlet.isValueDefaulted(LOAD_ON_STARTUP)) {
                 	hasLoadOnStartupTag = true;
             	}
             }

             
             SipServletDesc siplet = null;
             if(servletname.equalsIgnoreCase(_mainServlet)){
             	siplet = new SipServletDesc(application, servletname, fullyQualifiedClassName, true, loadOnStartup, hasLoadOnStartupTag);
             	application.setMainSipletName(_mainServlet);
             }
             else{
             	siplet = new SipServletDesc(application, servletname, fullyQualifiedClassName, false, loadOnStartup, hasLoadOnStartupTag);
             }
             application.addSipServlet(siplet);
             
             if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                 Tr.debug(tc, "processSipServletAnnotations", "New Siplet = " + siplet);
             }
        }
             
	}

	/**
	 * Returns error reason.
	 * @return
	 */
	public String getError() {
		return error;
	}
	
	
}
