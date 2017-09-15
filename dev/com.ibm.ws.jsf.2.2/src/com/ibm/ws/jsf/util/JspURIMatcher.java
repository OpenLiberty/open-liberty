/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.configuration.JspConfigPropertyGroup;
import com.ibm.ws.jsp.runtime.metadata.JspComponentMetaData;
import com.ibm.ws.webcontainer.util.URIMatcher;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * @author todd
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class JspURIMatcher extends URIMatcher {

    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf");
    private static final String CLASS_NAME = "com.ibm.ws.jsf.util.JspURIMatcher";

    private Map<String, Object> sortedMap = new TreeMap<String, Object>();

    public JspURIMatcher(IServletContext webapp) {
        super();
        addImplicitJspMappings();
        addJspFileExtensions(webapp);
        addPropertyGroupExtensions(webapp);
        addJspMappedServlets(webapp);
    }

    private void addImplicitJspMappings() {
        for (int i = 0; i < Constants.STANDARD_JSP_EXTENSIONS.length; i++) {
            String uri = Constants.STANDARD_JSP_EXTENSIONS[i];
            put(uri, "implicitJspExtensions");
        }

    }

    private void addJspFileExtensions(IServletContext webapp) {
        WebAppConfig webGroupCfg = webapp.getWebAppConfig();
        Map jspPropsMap = webGroupCfg.getJspAttributes();
        String list = (String) jspPropsMap.get("jsp.file.extensions");
        if (list != null) {
            StringTokenizer st = new StringTokenizer(list, ": ;");
            while (st.hasMoreTokens()) {
                String ext = st.nextToken();
                put(ext, "jsp.file.extension");
            }
        }
    }

    private void addPropertyGroupExtensions(IServletContext webapp) {
        WebAppConfig webGroupCfg = webapp.getWebAppConfig();

        JspComponentMetaData jspMetadata = (JspComponentMetaData) ((WebAppConfigExtended) webGroupCfg).getMetaData().getJspComponentMetadata();
        List jspPropertyGroups = jspMetadata.getJspPropertyGroups();

        // first add url mappings from web.xml jsp properties group
        for (Iterator itr = jspPropertyGroups.iterator(); itr.hasNext();) {
            JspConfigPropertyGroup jspPropertyGroup = (JspConfigPropertyGroup) itr.next();
            for (Iterator itr2 = jspPropertyGroup.getUrlPatterns().iterator(); itr2.hasNext();) {
                String urlPattern = (String) itr2.next();
                put(urlPattern, "jspPropertyGroupURI");
            }
        }
    }

    private void addJspMappedServlets(IServletContext webapp) {
        WebAppConfig config = webapp.getWebAppConfig();
        for (Iterator<IServletConfig> itr = config.getServletInfos(); itr.hasNext();) {
            IServletConfig sconfig = itr.next();
            if (sconfig.isJsp()) {
                Collection<String> mappings = sconfig.getMappings();
                if (mappings != null && mappings.isEmpty() == false) {
                    Iterator<String> mappingsIterator = mappings.iterator();
                    while (mappingsIterator.hasNext()) {
                        // begin 238795: correct ClassCastException when iterating over mappings.
                        //String urlPattern = (String) mappingsIterator.next();
                        //put(urlPattern, "jspMappedServlets");
                        String urlPattern = mappingsIterator.next();

                        put(urlPattern, "jspMappedServlets");
                        // end 238795: correct ClassCastException when iterating over mappings.
                    }
                }
            }
        }

    }

    public void put(String uri, Object target) {
        try {
            super.put(uri, target);
            log.logp(Level.FINE, CLASS_NAME, "put", "adding [" + uri + "] target =[" + target + "]");
            sortedMap.put(uri, target);
        } catch (Exception e) {
            log.logp(Level.FINEST, CLASS_NAME, "put", "problem occured when adding new target [" + uri + "] target =[" + target + "]", e);
        }
    }

    public String toString() {
        return sortedMap.toString();
    }

}