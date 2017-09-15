/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.ws.jsp.webcontainerext.JSPExtensionFactory;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class InjectionServiceImpl implements WebAppInjectionClassListCollaborator {

    static final protected Logger logger = Logger.getLogger("com.ibm.ws.jsp");
    static final protected Level logLevel = Level.FINEST;
    private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.InjectionServiceImpl";
    
    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({JspCoreException.class})
    public List<String> getInjectionClasses(Container moduleContainer) {
        List<String> result = new ArrayList<String>();
        //WebAppConfig webAppConfig = null;
        WebAppConfiguration webAppConfiguration = null;
        JspXmlExtConfig jspConfig = null;
        try {
            webAppConfiguration = (WebAppConfiguration) moduleContainer.adapt(WebAppConfig.class);
            jspConfig = moduleContainer.adapt(JspXmlExtConfig.class);
        } catch (UnableToAdaptException e) {
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "exception getting injection classes for jsp", e);
            }
        }
        //if (webAppConfig!=null) {
        if (jspConfig!=null) {
            //List tagLibs = webAppConfig.getTagLibs();
            Map tagLibs = jspConfig.getTagLibMap();
            JspOptions options = jspConfig.getJspOptions();
            if (tagLibs!=null) {
                try {
                    GlobalTagLibraryCache globalCache = JSPExtensionFactory.getGlobalTagLibraryCache();
                    Map globalTagLibMap = globalCache.getGlobalTagLibMapForWebApp(moduleContainer, jspConfig);
                    
                    TagLibraryCache tagLibCacheList = new TagLibraryCache(moduleContainer, tagLibs, options, null, globalTagLibMap, globalCache.getImplicitTagLibPrefixMap(), 
                                                                                                  globalCache.getOptimizedTagConfigMap(), webAppConfiguration);
                    List<String> jspListeners = (List<String>)tagLibCacheList.getEventListenerList(); //List<String>
                    result.addAll(jspListeners);
                    List<String> tags = tagLibCacheList.getTagsList();
                    result.addAll(tags);
                    return result;
                } catch (JspCoreException e) {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "exception getting injection classes for jsp", e);
                    }
                }
            }
        }
        return result;
    }

}
