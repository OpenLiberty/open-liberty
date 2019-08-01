/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.extension;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;

import com.ibm.ws.sip.container.servlets.ServletConfigWrapper;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * This class is extending our ServletConfigWrapper and implementing IServletConfig
 * so we can use it with the WebContainer ExtensionProcessor and ServletWrapper
 * mechanism, to get it eventually to the SipServlet init method
 * 
 * @author Nitzan
 */
public class SipServletConfig extends ServletConfigWrapper implements IServletConfig{
	
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -8974662866524380715L;
	
	/**
	 * This is the object we will delegate all IServletConfig interface methods to 
	 */
	protected IServletConfig _servletConfig;

	/**
	 * Ctor
	 * @param impl
	 */
	public SipServletConfig(ServletConfig impl) {
		//The config object will be used in the ServletConfigWrapper, where most
		//methods delegate to it and some will be override in ServletConfigWrapper
		//according to SIP internal logic
		super(impl);
		_servletConfig = (IServletConfig)impl;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#getMappings()
	 */
	/*TODO Liberty - return type has changed
	 public Collection<String> getMappings() {
		return _servletConfig.getMappings();
	}*/
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setClassName(java.lang.String)
	 */
	public void setClassName(String arg0) {
		_servletConfig.setClassName(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#getFileName()
	 */
	public String getFileName() {
		return _servletConfig.getFileName();
	}
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#isLoadOnStartup()
	 */
	public boolean isLoadOnStartup() {
		return _servletConfig.isLoadOnStartup();
	}
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setFileName(java.lang.String)
	 */
	public void setFileName(String arg0) {
		_servletConfig.setFileName( arg0);
	}
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setInitParams(java.util.Map)
	 */
	public void setInitParams(Map arg0) {
		_servletConfig.setInitParams( arg0);
	}
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setIsJsp(boolean)
	 */
	public void setIsJsp(boolean arg0) {
		_servletConfig.setIsJsp(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setServletContext(javax.servlet.ServletContext)
	 */
	public void setServletContext(ServletContext arg0) {
		_servletConfig.setServletContext(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setServletName(java.lang.String)
	 */
	public void setServletName(String arg0) {
		_servletConfig.setServletName(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setDisplayName(java.lang.String)
	 */
	public void setDisplayName(String arg0) {
		_servletConfig.setDisplayName(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#getClassName()
	 */
	public String getClassName() {
		return _servletConfig.getClassName();
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setStartUpWeight(java.lang.Integer)
	 */
	public void setStartUpWeight(Integer arg0) {
		_servletConfig.setStartUpWeight(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#addAttribute(java.lang.Object, java.lang.Object)
	 */
	public void addAttribute(Object arg0, Object arg1) {
		_servletConfig.addAttribute(arg0, arg1);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setAttributes(java.util.Map)
	 */
	public void setAttributes(Map arg0) {
		_servletConfig.setAttributes(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#removeAttribute(java.lang.Object)
	 */
	public Object removeAttribute(Object arg0) {
		return _servletConfig.removeAttribute(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#getMetaData()
	 */
	public WebComponentMetaData getMetaData() {
		return _servletConfig.getMetaData();
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#isCachingEnabled()
	 */
	public boolean isCachingEnabled() {
		return _servletConfig.isCachingEnabled();
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setIsCachingEnabled(boolean)
	 */
	public void setIsCachingEnabled(boolean arg0) {
		_servletConfig.setIsCachingEnabled(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setStatisticsEnabled(boolean)
	 */
	public void setStatisticsEnabled(boolean arg0) {
		_servletConfig.setStatisticsEnabled(arg0);
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#isStatisticsEnabled()
	 */
	public boolean isStatisticsEnabled() {
		return _servletConfig.isStatisticsEnabled();
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#isJsp()
	 */
	public boolean isJsp() {
		return _servletConfig.isJsp();
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#getStartUpWeight()
	 */
	public int getStartUpWeight() {
		return _servletConfig.getStartUpWeight();
	}
	
	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#setDescription(java.lang.String)
	 */
	public void setDescription(String arg0) {
		_servletConfig.setDescription(arg0);
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletConfig#addMapping(java.lang.String)
	 */
	public void addMapping(String arg0) {
		_servletConfig.addMapping(arg0);
	}

      public void setMetaData(WebComponentMetaData metaData){
		_servletConfig.setMetaData(metaData);
	}

    @Override
    public Servlet getServlet() {
        return _servletConfig.getServlet();
    }

    @Override
    public Class<? extends Servlet> getServletClass() {
        return _servletConfig.getServletClass();
    }

    @Override
    public IServletWrapper getServletWrapper() {
        return _servletConfig.getServletWrapper();
    }

    @Override
    public boolean isInternal() {
        return _servletConfig.isInternal();
    }

    @Override
    public boolean isSingleThreadModelServlet() {
        return _servletConfig.isSingleThreadModelServlet();
    }

    @Override
    public void setInternal(boolean isInternal) {
        _servletConfig.setInternal(isInternal);
    }

    @Override
    public void setMappings(List<String> mappings) {
        _servletConfig.setMappings(mappings);
    }

    @Override
    public void setServlet(Servlet servlet) {
        _servletConfig.setServlet(servlet);
    }

    @Override
    public void setServletClass(Class<? extends Servlet> servletClass) {
        _servletConfig.setServletClass(servletClass);
    }

    @Override
    public void setServletWrapper(IServletWrapper wrapper) {
        _servletConfig.setServletWrapper(wrapper);
    }

    @Override
    public void setSingleThreadModelServlet(boolean isSTM) {
        _servletConfig.setSingleThreadModelServlet(isSTM);
    }

    @Override
    public void setLoadOnStartup(int arg0) {
        _servletConfig.setLoadOnStartup(arg0);
    }

    @Override
    public Set<String> addMapping(String... arg0) {
        return _servletConfig.addMapping(arg0);
    }

    @Override
    public Map<String, String> getInitParameters() {
        return _servletConfig.getInitParameters();
    }

    @Override
    public String getName() {
        return _servletConfig.getName();
    }

    @Override
    public boolean setInitParameter(String arg0, String arg1) {
        return _servletConfig.setInitParameter(arg0, arg1);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> arg0) {
        return _servletConfig.setInitParameters(arg0);
    }

    @Override
    public void setAsyncSupported(boolean arg0) {
        _servletConfig.setAsyncSupported(arg0);
    }
    
    public boolean isWeightChanged(){
        return _servletConfig.isWeightChanged();
    }
    
    public void setAddedToLoadOnStartup(boolean addedToLoadOnStartup) {
        _servletConfig.setAddedToLoadOnStartup(addedToLoadOnStartup);
    }

    public boolean isAddedToLoadOnStartup() {
        return _servletConfig.isAddedToLoadOnStartup();
    }

    @Override
    public boolean isAsyncSupported() {
        return _servletConfig.isAsyncSupported();
    }

	@Override
	public void setMultipartConfig(MultipartConfigElement arg0) {
		_servletConfig.setMultipartConfig(arg0);
	}

	@Override
	public void setRunAsRole(String arg0) {
		_servletConfig.setRunAsRole(arg0);
	}

	@Override
	public Set<String> setServletSecurity(ServletSecurityElement arg0) {
		// TODO Auto-generated method stub
		return _servletConfig.setServletSecurity(arg0);
	}

	@Override
	public String getRunAsRole() {
		// TODO Auto-generated method stub
		return _servletConfig.getRunAsRole();
	}
	
	@Override
	public MultipartConfigElement getMultipartConfig() {
	    // TODO Auto-generated method stub
	    return _servletConfig.getMultipartConfig();
	}
	
	@Override
	public void setMultipartBaseLocation(File arg0) {
	    // TODO Auto-generated method stub
	    _servletConfig.setMultipartBaseLocation(arg0);
	}

	@Override
	public File getMultipartBaseLocation() {
	    // TODO Auto-generated method stub
	    return _servletConfig.getMultipartBaseLocation();
	}

	@Override
	public Set<String> addMapping(CheckContextInitialized checkContextInitialized, String... mappingURI) {
        return _servletConfig.addMapping(checkContextInitialized, mappingURI);
	}

	@Override
	public ServletSecurityElement getServletSecurity() {
		// TODO Auto-generated method stub
		return _servletConfig.getServletSecurity();
	}

	@Override
	public boolean isClassDefined() {
		// TODO Auto-generated method stub
		return _servletConfig.isClassDefined();
	}

	@Override
	public boolean isEnabled() {
		return _servletConfig.isEnabled();
	}

	@Override
	public List<String> getMappings() {

		return _servletConfig.getMappings();
	}

}
