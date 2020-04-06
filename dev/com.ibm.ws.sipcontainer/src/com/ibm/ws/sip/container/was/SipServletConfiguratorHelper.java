/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.sip.annotation.SipApplicationKey;
import javax.servlet.sip.annotation.SipListener;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.container.service.annotations.FragmentAnnotations;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.sip.container.rules.Condition;
import com.ibm.ws.sip.container.was.extension.SipServletConfigFactoryImpl;
import com.ibm.ws.webcontainer.osgi.metadata.WebComponentMetaDataImpl;
import com.ibm.ws.webcontainer.servlet.ServletConfig;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class SipServletConfiguratorHelper implements ServletConfiguratorHelper {

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipServletConfiguratorHelper.class);
	
	private final ServletConfigurator configurator;

	private SipAppDesc _appDesc = null;
	public SipServletConfiguratorHelper(ServletConfigurator configurator) {
		this.configurator = configurator;
	}

	/** {@inheritDoc} */
	@Override
	public void configureInit() throws UnableToAdaptException {


	}

	/** {@inheritDoc} */
	@Override
	public void configureFromWebApp(WebApp webApp) throws UnableToAdaptException {

		if(webApp != null) {
			WebModuleInfo moduleInfo = (WebModuleInfo) configurator.getFromModuleCache(WebModuleInfo.class);
			//SipAppDesc inherits WebApp so if there was a sip.xml the class will be SipAppDesc and otherwise it will be WebApp
			if(webApp instanceof SipAppDesc) {
				_appDesc = (SipAppDesc) webApp;
			}
			if(_appDesc != null) {
				//setting the webAppName to be used as a key to identify the application.
				_appDesc.setWebAppName(moduleInfo.getName());

				SipAppDescManager.getInstance().addNewApp(_appDesc);
			}

		}


	}



	/** {@inheritDoc} */
	@Override
	public void configureFromWebFragment(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {


	}

	/** {@inheritDoc} */
	@Override
	public void configureFromAnnotations(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("Configuring from annotations");
		}
		WebAnnotations webAnnotations = configurator.getWebAnnotations();
		FragmentAnnotations fragmentAnnotations = webAnnotations.getFragmentAnnotations(webFragmentItem);

		if(webAnnotations != null && fragmentAnnotations != null){
			WebModuleInfo moduleInfo = (WebModuleInfo) configurator.getFromModuleCache(WebModuleInfo.class);

			SipAppAnnotationsInfo sipInfo = new SipAppAnnotationsInfo(moduleInfo.getName(), webAnnotations, fragmentAnnotations);
			SipAppDesc newApp = sipInfo.createSipAppDesc();
			_appDesc = newApp;
			//TODO Liberty Add a check for creation failure. this check fails on every web application
			/*       if(newApp == null){
	        	if (c_logger.isErrorEnabled() ) {
	        		c_logger.error("sip.app.deployment.failed", new Object[] { moduleInfo, sipInfo.getError() });
	            }
	        }*/
		}

	}

	/**
	 * Read SipApplicationKey annotations
	 * @param webAnnotations
	 * @param fragmentAnnotations
	 * @throws UnableToAdaptException
	 */
	private void processSipApplicationKeyAnnotations(WebAnnotations webAnnotations,FragmentAnnotations fragmentAnnotations) throws UnableToAdaptException {
		Set<String> appKeys = fragmentAnnotations.selectAnnotatedClasses(SipApplicationKey.class);

		System.out.println("SipApplicationKeys: " + appKeys);

	}

	/**
	 * Read SipListener annotations
	 * @param webAnnotations
	 * @param fragmentAnnotations
	 * @throws UnableToAdaptException
	 */
	private void processSipListenerAnnotations(WebAnnotations webAnnotations,FragmentAnnotations fragmentAnnotations) throws UnableToAdaptException {
		Set<String> listeners = fragmentAnnotations.selectAnnotatedClasses(SipListener.class);
		System.out.println("SipListeners: " + listeners);
	}





	/** {@inheritDoc} */
	@Override
	public void configureDefaults() throws UnableToAdaptException {


	}

	/** 
	 * Add the application data to the context.
	 * Create the servlet config and register it to the context
	 * {@inheritDoc} 
	 * */
	@Override
	public void finish() throws UnableToAdaptException {


		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("finishing servlet Config");
		}
		if(_appDesc == null) {
			return; 
		}
		
		WebAppConfiguration config = (WebAppConfiguration) configurator.getFromModuleCache(WebAppConfig.class);
    	Map<String, Condition> servletsPatterns = _appDesc.getServletsPatterns();
    	boolean appHasServletMappingRules = servletsPatterns != null && servletsPatterns.size() > 0;
    	boolean appHasMainServlet = _appDesc.hasMainServlet();
    	
    	if(appHasMainServlet){
        	SipServletDesc servletDesc = _appDesc.getSipServlet(_appDesc.getMainSipletName());
        	if (servletDesc != null) {
        		_appDesc.setMainServlet(servletDesc);        

        	} else {
        		if(c_logger.isErrorEnabled()){
                	Object[] params = {_appDesc.getMainSipletName()};
                	c_logger.error("error.no.main.serv", null, params);
                }
        		
        		throw new UnableToAdaptException("The application defined a main servlet that does not exist: " + _appDesc.getMainSipletName());
        	}
    	}
    	
    	if(appHasMainServlet && appHasServletMappingRules){
    		if(c_logger.isErrorEnabled()){
            	Object[] params = {_appDesc};
            	c_logger.error("cant.both.main.serv.mapping", null, params);
            }

    		throw new UnableToAdaptException("The application incorrectly defines both a main servlet " + _appDesc.getMainSipletName() + " and servlets mapping rules.");
    	}
    	else if(appHasServletMappingRules){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Application [" + _appDesc.getAppName() + "] has mapping rules and doesn't have a main servlet");
			}
			
    		for(Map.Entry<String, Condition> patternEntry : servletsPatterns.entrySet()){
    			String servletName = patternEntry.getKey();
    			SipServletDesc siplet = _appDesc.getSipServlet(servletName);
    			if(siplet != null){
                    siplet.setTriggeringCondition(patternEntry.getValue());
    			}
    			else{
                    if (c_logger.isErrorEnabled()){
                        Object[] args = { _appDesc, servletName };
                        c_logger.error(
                            "error.mapping.to.nonexisting.siplet",
                            Situation.SITUATION_CREATE,
                            args);
                    }
                    
                    throw new UnableToAdaptException("Mapping for nonexisting siplet: " + servletName + ", Application: " + _appDesc);
    			}
    		}
    	}
    	else if(!appHasMainServlet){
    		// JSR 289 - if there is only one servlet in 289 application 
    		// and no main servlet is defined, then make it mainServlet.
    		List<SipServletDesc> siplets = _appDesc.getSipServlets();
    		if (_appDesc.isJSR289Application() && _appDesc.getMainSiplet() == null && siplets.size() == 1) {
    			_appDesc.setMainSipletName(siplets.iterator().next().getName());	
    			_appDesc.setMainServlet(siplets.iterator().next());

    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug("Main servlet for application [" + _appDesc.getAppName() + "] " +
    						"defined to be  [" + _appDesc.getMainSiplet() + "]");
    			}
    		}
    		else if(siplets.size() > 1){
        		if(c_logger.isErrorEnabled()){
                	Object[] params = {_appDesc};
                	c_logger.error("must.define.main.serv", null, params);
                }
    			
        		throw new UnableToAdaptException("The application doesn't define a main servlet but has more than one siplet"); 
    		}
    	}
    	else{ // application has main serlvet only
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug("Main servlet for application [" + _appDesc.getAppName() + "] " +
    					"is defined to be  [" + _appDesc.getMainSiplet() + "]. No mapping rules.");
    		}
    	}
    	
    	
		//Creating servlets configuration
		//if the servlet already exists only update the data and don't create a new servlet
		for(SipServletDesc desc :_appDesc.getSipServlets()){
			//setting data to the configuration
			String servletId = "SIP" + configurator.generateUniqueId();
			IServletConfig cfg = config.getServletInfo(desc.getName());
			if (cfg == null ){
				cfg = new ServletConfig(servletId, config);	
			}
			IServletConfig servletConfig = SipServletConfigFactoryImpl.getInstance().createSipServletConfig(cfg);
			((ServletConfig)cfg).setName(desc.getName());
			
			servletConfig.setAddedToLoadOnStartup(desc.isServletLoadOnStartup());
			servletConfig.setServletName(desc.getName());

			servletConfig.setMetaData(new WebComponentMetaDataImpl(config.getMetaData()));
			servletConfig.setClassName(desc.getClassName());

			servletConfig.setLoadOnStartup(desc.getServletLoadOnStartup());    
			servletConfig.setInitParams(desc.getInitParams());

			//adding servlet to application
			config.addServletInfo(desc.getName(), servletConfig);
			List<String> mappings = config.getServletMappings(desc.getName());
			String servMapping = "/"+desc.getName();
			if(mappings == null || !mappings.contains(servMapping)) {
				config.addServletMapping(desc.getName(), servMapping);
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("mapping: "+  servMapping + " -> " +desc.getName());
				}
			}
		}
		
		config.setContextParams((HashMap<?, ?>) _appDesc.getAppContextParams());
	
		//adding application to the application manager
		SipAppDescManager.getInstance().addNewApp(_appDesc);

}

/** {@inheritDoc} */
@Override
public void configureWebBnd(WebBnd webBnd) throws UnableToAdaptException {


}

/** {@inheritDoc} */
@Override
public void configureWebExt(WebExt webExt) throws UnableToAdaptException {


}

}
