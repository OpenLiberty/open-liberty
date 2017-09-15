/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webxml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.javaee.dd.jsp.JSPConfig;
import com.ibm.ws.javaee.dd.jsp.JSPPropertyGroup;
import com.ibm.ws.javaee.dd.jsp.Taglib;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfigPropertyGroup;
import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.ws.jsp.runtime.metadata.JspComponentMetaData;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

//At the point that this config helper runs there isn't a WebApp instance yet, that's created later in DeployedModule 
//Config helpers need to put their info in the places that the WebApp will be passed when it's created 
//The config info hangs off the WebAppConfig and the DeployedModule is created which creates the WebApp which starts to connect the servlet context with the config

public class JspConfiguratorHelper implements ServletConfiguratorHelper, JspXmlExtConfig {

    private final ServletConfigurator configurator;
    private final JspOptions options;
    private String webAppVersion = "3.0";
    private HashMap<String, String> tagLibMap = null;
    private List<JspConfigPropertyGroup> jspPropertyGroups = null;
    //defect 400645
    private Properties webConProperties = new Properties();
    private boolean JCDIEnabled = false;

    public JspConfiguratorHelper(ServletConfigurator configurator) {
        this.configurator = configurator;
        options = new JspOptions(new java.util.Properties());
    }

    @Override
    public void configureInit() {
        // nothing at the moment
    }

    public void configureFromWebApp(WebApp webApp) {
        webAppVersion = webApp.getVersion();
        JSPConfig jspConfig = webApp.getJSPConfig();
        if (jspConfig != null) {
            for (Taglib tl : jspConfig.getTaglibs()) {
                if (tagLibMap == null) {
                    tagLibMap = new HashMap<String, String>();
                }
                tagLibMap.put(tl.getTaglibURI(), tl.getTaglibLocation());
            }

            for (JSPPropertyGroup propertyGroup : jspConfig.getJSPPropertyGroups()) {
                if (jspPropertyGroups == null) {
                    jspPropertyGroups = new ArrayList<JspConfigPropertyGroup>();
                }
                jspPropertyGroups.add(new JspConfigPropertyGroup(propertyGroup));
            }
        }
    }

    @Override
    public void configureFromWebFragment(WebFragmentInfo webFragmentItem) {
        WebFragment webFragment = webFragmentItem.getWebFragment();
        JSPConfig jspConfig = webFragment.getJSPConfig();
        if (jspConfig != null) {
            for (Taglib tl : jspConfig.getTaglibs()) {
                if (tagLibMap == null) {
                    tagLibMap = new HashMap<String, String>();
                }
                tagLibMap.put(tl.getTaglibURI(), tl.getTaglibLocation());
            }

            for (JSPPropertyGroup propertyGroup : jspConfig.getJSPPropertyGroups()) {
                if (jspPropertyGroups == null) {
                    jspPropertyGroups = new ArrayList<JspConfigPropertyGroup>();
                }
                jspPropertyGroups.add(new JspConfigPropertyGroup(propertyGroup));
            }
        }
    }

    @Override
    public void configureFromAnnotations(WebFragmentInfo webFragmentItem) {
        // nothing at the moment
    }

    @Override
    public void configureDefaults() throws UnableToAdaptException {
        // nothing at the moment
    }

    @Override
    public void configureWebBnd(WebBnd webBnd) throws UnableToAdaptException {
        // nothing at the moment
    }

    @Override
    public void configureWebExt(WebExt webExt) throws UnableToAdaptException {
        // nothing at the moment
    }

    public void finish() {
        configurator.addToModuleCache(JspXmlExtConfig.class, this);
        WebModuleMetaData wmmd = (WebModuleMetaData) configurator.getFromModuleCache(WebModuleMetaData.class);
        JspComponentMetaData jspmetaData = new JspComponentMetaData();
        jspmetaData.setApplicationDisplayName(wmmd.getConfiguration().getApplicationName()); 
        jspmetaData.setServlet2_2(this.getJspOptions().isServlet2_2());
        jspmetaData.setServletEngineReloadEnabled(this.getJspOptions().isServletEngineReloadEnabled());
        jspmetaData.setServletEngineReloadInterval(this.getJspOptions().getServletEngineReloadInterval());
        jspmetaData.setJspPropertyGroups(this.getJspPropertyGroups());
        jspmetaData.setJspTaglibs(this.getTagLibMap());
        jspmetaData.setLooseLibs(this.getJspOptions().getLooseLibMap());
        wmmd.setJspComponentMetadata(jspmetaData);

    }

    public Map<String, String> getTagLibMap() {
        if (tagLibMap != null) {
            return Collections.unmodifiableMap(tagLibMap);
        }
        else {
            return Collections.emptyMap();
        }
    }

    public List<JspConfigPropertyGroup> getJspPropertyGroups() {
        if (jspPropertyGroups != null) {
            return Collections.unmodifiableList(jspPropertyGroups);
        }
        else {
            return Collections.emptyList();
        }
    }

    public boolean isServlet24() {
        return webAppVersion.equals("2.4");
    }

    public boolean isServlet24_or_higher() {
        return getVersion(webAppVersion) >= 2.4;
    }

    public boolean isServlet25_or_higher() {
        return getVersion(webAppVersion) >= 2.5;
    }

    public JspOptions getJspOptions() {
        return options;
    }        

    private double getVersion(String v) {
        if (v != null) {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException e) {
            }
        }
        return 2.3;
    }    

    public List<String> getJspFileExtensions() {
        return Collections.emptyList();
    }

    public boolean containsServletClassName(String servletClassName) {
        return false;
    }

    //defect 400645
    public void setWebContainerProperties(Properties webConProperties) {
        this.webConProperties = webConProperties;
    }

    public Properties getWebContainerProperties() {
        return webConProperties;
    }
    //defect 400645

    @Override
    public boolean isJCDIEnabledForRuntimeCheck() {
        return JCDIEnabled;
    }

    @Override
    public void setJCDIEnabledForRuntimeCheck(boolean b) {
        JCDIEnabled = b;        
    }

}
