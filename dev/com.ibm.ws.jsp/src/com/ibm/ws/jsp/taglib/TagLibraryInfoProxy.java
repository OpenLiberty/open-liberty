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
package com.ibm.ws.jsp.taglib;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class TagLibraryInfoProxy {
    static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.TagLibraryInfoProxy";
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    private String jarName = null;
    private String tldPath = null;
    private TagLibraryInfoImpl tli = null;
    private GlobalTagLibraryCache globalTagLibraryCache = null;
    private boolean containsListenerDefs = false;
    private List eventListenerList = null;
    
    public TagLibraryInfoProxy(String jarName, String tldPath, boolean containsListenerDefs, GlobalTagLibraryCache globalTagLibraryCache) {
        this.jarName = jarName;
        this.tldPath = tldPath;
        this.globalTagLibraryCache = globalTagLibraryCache;
        this.containsListenerDefs = containsListenerDefs;
    }
    
    public synchronized TagLibraryInfoImpl getTagLibraryInfoImpl(String prefix) {
        if (tli == null) {
            loadTld();
        }
        if (tli == null) {
            return tli;
        }
        else {
            return tli.copy(prefix);
        }
    }

    public boolean containsListenerDefs() {
        return containsListenerDefs;
    }

    public synchronized List getEventListenerList() {
        if (eventListenerList == null) {
            loadTld();
        }
        return eventListenerList;
    }

    private void loadTld() {
        URL jarUrl = globalTagLibraryCache.getClassLoader().getResource(jarName);
        if (jarUrl != null) {
            JspInputSource tldInputSource = null;
            try {
                final boolean JCDIEnabledForRuntime=false;//set this to false since this configuration isn't used during runtime checking
                JspConfigurationManager configManager = new JspConfigurationManager(Collections.EMPTY_LIST, false, true, Collections.EMPTY_LIST, JCDIEnabledForRuntime);    
                TldParser tldParser = new TldParser(globalTagLibraryCache, configManager, false);
                URL url = new URL("jar:" + jarUrl.toString()+"!/");
                tldInputSource = globalTagLibraryCache.getJspInputSourceFactory().createJspInputSource(url, tldPath);
                tli = tldParser.parseTLD(tldInputSource, jarName);
                if (tli.getReliableURN() != null) {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "loadTld", "Global jar tld in proxy loaded for {0}", tli.getReliableURN());
                    }
                    tli.setURI(tli.getReliableURN());
                    if (containsListenerDefs) {
                        eventListenerList = new ArrayList();
                        eventListenerList.addAll(tldParser.getEventListenerList());
                    }
                }
                else {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                        logger.logp(Level.WARNING, CLASS_NAME, "loadTld", "jsp warning failed to find a uri tag in ["+tldInputSource.getAbsoluteURL()+"]");
                    }
                }
            }
            catch (JspCoreException e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "loadTld", "jsp warning failed to load tld in jar. uri = ["+tldInputSource.getAbsoluteURL()+"]", e);
                }
            }
            catch (IOException e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "loadTld", "jsp warning failed to load tld in jar. uri = ["+tldInputSource.getAbsoluteURL()+"]", e);
                }
            }
        }
        if (eventListenerList == null) {
            eventListenerList = Collections.EMPTY_LIST;
        }
    }
}
