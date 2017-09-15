/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Registration;
import javax.servlet.ServletContext;

import com.ibm.ws.container.BaseConfiguration;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

@SuppressWarnings("unchecked")
public abstract class TargetConfig extends BaseConfiguration implements Registration.Dynamic {
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.servlet");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.servlet.TargetConfig";
    
    protected Map<String, String> initParams = null;
    protected String fileName;
    private String displayName;
    protected String name; // logical name
    protected String description;
    protected String smallIcon;
    protected String largeIcon;
    protected IServletContext context;
    private String className;
    private boolean asyncSupported;

    public boolean isAsyncSupported() {
    	if (!asyncSupported && initParams!= null && initParams.containsKey("com.ibm.ws.webcontainer.async-supported")) {
    		asyncSupported = Boolean.valueOf(initParams.get("com.ibm.ws.webcontainer.async-supported")).booleanValue();
    	}        
        return asyncSupported;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public TargetConfig(String id) {
        super(id);
    }

    public void addInitParameter(String name, String value) {
        if (initParams == null)
            initParams = new HashMap();
        initParams.put(name, value);
    }

    public String getInitParameter(String name) {
        if (initParams == null)
            return null;
        else
            return initParams.get(name);
    }

    /**
     * Returns the fileName.
     * 
     * @return String
     */
    public String getFileName() {
        return fileName;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

    public void setInitParams(Map params) {
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE,CLASS_NAME, "setInitParams","params->"+params); 
        if (initParams == null) {

            initParams = new HashMap<String, String>();
        }
        this.initParams.putAll(params);
    }

    public Map<String, String> getInitParameters() {
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.entering(CLASS_NAME, "getInitParameters"); 
    	
        if (initParams == null) {

            initParams = new HashMap<String, String>();
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.exiting(CLASS_NAME, "getInitParameters"); 
        return Collections.unmodifiableMap(initParams);
    }

    public Set<String> setInitParameters(Map<String, String> params) {    	
        if (this.context.isInitialized()) {
            throw new IllegalStateException();
        }

        if (initParams == null) {
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME, "setInitParameters", "no existing init params, so putAll");       
        	
            initParams = new HashMap<String, String>();
            this.initParams.putAll(params);
            return Collections.EMPTY_SET;
        } else {

            Set<Entry<String, String>> entrySet = params.entrySet();
            Set<String> conflictSet = null;
            boolean foundConflict = false;

            for (Entry<String, String> entry : entrySet) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null || value == null) {
                    throw new IllegalArgumentException();
                }
                if (initParams.containsKey(key)) {
                	
                	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME, "setInitParameters", "found existing param for key->["+key+"]");
                        
                    if (conflictSet == null) {
                        conflictSet = new HashSet<String>();
                    }

                    conflictSet.add(key);
                    foundConflict = true;

                } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "setInitParameters", "no existing param for key->["+key+"]");       
            	
            }

            if (!foundConflict) {
                //this.initParams = tempMap;
                //previously we were doing the above and this was replacing any existing params
                this.initParams.putAll(params);
                return Collections.EMPTY_SET;
            } else {
                return conflictSet;
            }
        }

    }

    public boolean setInitParameter(String key, String value) throws IllegalStateException, IllegalArgumentException {
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "setInitParameter", "key->[" + key + "] value->[" + value + "]");       
    	if (this.context.isInitialized()) {
            throw new IllegalStateException();
        }

        if (key == null || value == null) {
            throw new IllegalArgumentException();
        }

        if (initParams == null) {
            initParams = new HashMap<String, String>();
            this.initParams.put(key, value);
            return true;
        } else if (this.initParams.containsKey(key)) {
            return false;
        } else {
            this.initParams.put(key, value);
            return true;
        }

    }

    public Properties getParamsAsProperties() {
        Properties p = new Properties();
        if (initParams != null)
            p.putAll(initParams);
        return p;
    }

    /**
     * Sets the fileName.
     * 
     * @param fileName
     *            The fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the displayName.
     * 
     * @return String
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the displayName.
     * 
     * @param displayName
     *            The displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *            The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description.
     * 
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the largeIcon.
     * 
     * @return String
     */
    public String getLargeIcon() {
        return largeIcon;
    }

    /**
     * Returns the smallIcon.
     * 
     * @return String
     */
    public String getSmallIcon() {
        return smallIcon;
    }

    /**
     * Sets the description.
     * 
     * @param description
     *            The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the largeIcon.
     * 
     * @param largeIcon
     *            The largeIcon to set
     */
    public void setLargeIcon(String largeIcon) {
        this.largeIcon = largeIcon;
    }

    /**
     * Sets the smallIcon.
     * 
     * @param smallIcon
     *            The smallIcon to set
     */
    public void setSmallIcon(String smallIcon) {
        this.smallIcon = smallIcon;
    }

    /**
     * Associates the given IServletContext with this Servlet's config.
     * 
     * @param context
     */
    public void setIServletContext(IServletContext iServletContext) {
        this.context = iServletContext;
    }
    
    public void setServletContext(ServletContext webApp) {
        if (webApp instanceof ServletContextFacade){
            this.setIServletContext((ServletContextFacade)webApp);
        }
        else if (webApp instanceof IServletContext) {
            this.setIServletContext((ServletContextFacade) ((IServletContext) webApp).getFacade());
        }
        else {
            // This should NEVER happen
            throw new IllegalStateException("webApp is not a servletContextFacade or WebApp");
        }
        
        
        
        // begin defect 293789: add ability for components to register
        // ServletContextFactories
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "setServletContext", "set context to->[" + context + "] for->[" + this.hashCode() + "]");
    }
    
    /**
     * @return javax.servlet.ServletContext
     */
    public ServletContext getServletContext() {
        // System.out.println("ATTN: ServletConfig returning " + context);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "getServletContext", "returning->[" + context + "] for->[" + this.hashCode() + "]");

        return context;
    }
    
    /**
     * @return com.ibm.wsspi.webcontainer.servlet.IServletContext
     */
    public IServletContext getIServletContext() {
        // System.out.println("ATTN: ServletConfig returning " + context);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "getIServletContext", "returning->[" + context + "] for->[" + this.hashCode()+ "]");

        return context;
    }



    
    @Override
    public void setAsyncSupported(boolean isAsyncSupported) throws IllegalStateException {
        this.asyncSupported = isAsyncSupported;
    }

    public String toString(){
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("getInitParameters->");
        Map<String, String> initParameters = getInitParameters();
        if (initParameters==null||initParameters.isEmpty()){
            strBuilder.append("null or empty init parameters");
        }
        else {
            for (Entry<String,String> initParamEntry:initParameters.entrySet()){
                strBuilder.append("("+initParamEntry.getKey()+","+initParamEntry.getValue()+")"); 
            }
            strBuilder.append('\n');
            for (Entry<String,String> initParamEntry:initParameters.entrySet()){
                strBuilder.append("getInitParameter("+initParamEntry.getKey()+","+this.getInitParameter(initParamEntry.getKey())+")"); 
                strBuilder.append('\n');
            }
        }
        strBuilder.append("isAsyncSupported->"+this.isAsyncSupported()+"\n");
        strBuilder.append("getDescription->"+this.getDescription()+"\n");
        return strBuilder.toString();
    }
    

}
