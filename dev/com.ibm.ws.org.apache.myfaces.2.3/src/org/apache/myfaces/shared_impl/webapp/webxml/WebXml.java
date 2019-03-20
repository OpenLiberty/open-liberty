/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.shared_impl.webapp.webxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;
import javax.faces.webapp.FacesServlet;

import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;

/**
 * @author Manfred Geiler (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class WebXml
{
    private static final Logger log = Logger.getLogger(WebXml.class.getName());


    private static long refreshPeriod;
    private long parsingTime;

    private Map _servlets = new HashMap();
    private Map _servletMappings = new HashMap();
    private Map _filters = new HashMap();
    private Map _filterMappings = new HashMap();

    private volatile List _facesServletMappings = null;
    private volatile List _facesExtensionsFilterMappings = null;
    
    private String _delegateFacesServlet = null;
    private boolean errorPagePresent = false;

    void addServlet(String servletName, String servletClass)
    {
        if (_servlets.get(servletName) != null)
        {
            log.warning("Servlet " + servletName + " defined more than once, first definition will be used.");
        }
        else
        {
            _servlets.put(servletName, servletClass);
        }
    }

    void addFilter(String filterName, String filterClass)
    {
        if (_filters.get(filterName) != null)
        {
            log.warning("Filter " + filterName + " defined more than once, first definition will be used.");
        }
        else
        {
            _filters.put(filterName, filterClass);
        }
    }

    boolean containsServlet(String servletName)
    {
        return _servlets.containsKey(servletName);
    }

    boolean containsFilter(String filterName)
    {
        return _filters.containsKey(filterName);
    }

    void addServletMapping(String servletName, String urlPattern)
    {
        List mappings = (List)_servletMappings.get(servletName);
        if (mappings == null)
        {
            mappings = new ArrayList();
            _servletMappings.put(servletName, mappings);
        }
        mappings.add(urlPattern);
    }

    void addFilterMapping(String filterName, String urlPattern)
    {
        List mappings = (List)_filterMappings.get(filterName);
        if (mappings == null)
        {
            mappings = new ArrayList();
            _filterMappings.put(filterName, mappings);
        }
        mappings.add(urlPattern);
    }

    public List getFacesServletMappings()
    {
        if (_facesServletMappings != null)
        {
            return _facesServletMappings;
        }

        List tempFacesServletMappings = new ArrayList();
        for (Iterator it = _servlets.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)it.next();
            String servletName = (String)entry.getKey();
            if (null == entry.getValue())
            {
                // the value is null in the case of jsp files listed as servlets
                // in cactus
                // <servlet>
                //   <servlet-name>JspRedirector</servlet-name>
                //   <jsp-file>/jspRedirector.jsp</jsp-file>
                // </servlet>
                continue;
            }

            Class servletClass = ClassUtils.simpleClassForName((String) entry.getValue(), false);
            if (servletClass == null)
            {
                if (log.isLoggable(Level.FINEST))
                {
                    log.finest("ignoring servlet " + servletName + " because its class could not be loaded");
                }
                continue;
            }

            if (FacesServlet.class.isAssignableFrom(servletClass) ||
                    DelegatedFacesServlet.class.isAssignableFrom(servletClass) ||
                    servletClass.getName().equals(_delegateFacesServlet))
            {
                List urlPatterns = (List)_servletMappings.get(servletName);
                if( urlPatterns != null )
                {
                    for (Iterator it2 = urlPatterns.iterator(); it2.hasNext(); )
                    {
                        String urlpattern = (String)it2.next();
                        tempFacesServletMappings.add(new ServletMapping(servletName, servletClass, urlpattern));
                        if (log.isLoggable(Level.FINEST))
                        {
                            log.finest("adding mapping for servlet + " + servletName + " urlpattern = " + urlpattern);
                        }
                    }
                }
            }
            else
            {
                if (log.isLoggable(Level.FINEST))
                {
                    log.finest("ignoring servlet + " + servletName + " " + servletClass + " (no FacesServlet)");
                }
            }
        }
        
        //Expose to all threads
        _facesServletMappings = tempFacesServletMappings;
        
        return _facesServletMappings;
    }

    /**
     * returns a list of org.apache.myfaces.shared_impl.webapp.webxml.FilterMapping representing a
     * extensions filter entry
     */
    public List getFacesExtensionsFilterMappings()
    {
        if (_facesExtensionsFilterMappings != null)
        {
            return _facesExtensionsFilterMappings;
        }

        List tempExtensionsFilterMappings = new ArrayList();
        for (Iterator it = _filters.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)it.next();
            String filterName = (String)entry.getKey();
            String filterClassName = (String)entry.getValue();
            
            if (!"org.apache.myfaces.component.html.util.ExtensionsFilter".equals(filterClassName) &&
                !"org.apache.myfaces.webapp.filter.ExtensionsFilter".equals(filterClassName))
            {
                // not an extensions filter
                continue;
            }
            
            Class filterClass = org.apache.myfaces.shared.util.ClassUtils.simpleClassForName(filterClassName);
            List urlPatterns = (List)_filterMappings.get(filterName);
            if( urlPatterns != null )
            {
                for (Iterator it2 = urlPatterns.iterator(); it2.hasNext(); )
                {
                    String urlpattern = (String)it2.next();
                    tempExtensionsFilterMappings.add(new org.apache.myfaces.shared_impl.webapp.webxml.FilterMapping(
                        filterName, filterClass, urlpattern));
                    if (log.isLoggable(Level.FINEST))
                    {
                        log.finest("adding mapping for filter + " + filterName + " urlpattern = " + urlpattern);
                    }
                }
            }
        }
        
        //Expose to all threads
        _facesExtensionsFilterMappings = tempExtensionsFilterMappings;
        
        return _facesExtensionsFilterMappings;
    }

    protected void setParsingTime(long parsingTime)
    {
        this.parsingTime = parsingTime;
    }
    
    private void setDelegateFacesServlet(String delegateFacesServlet)
    {
        this._delegateFacesServlet = delegateFacesServlet;
    }
    
    /**
     * Sets if, the web.xml contains an error-page entry
     * @param errorPagePresent
     */
    public void setErrorPagePresent(boolean errorPagePresent)
    {
        this.errorPagePresent = errorPagePresent;
    }
    
    /**
     * Determines, if the web.xml contains an error-page entry
     * @return
     */
    public boolean isErrorPagePresent()
    {
        return errorPagePresent;
    }

    protected boolean isOld(ExternalContext context)
    {
        if (refreshPeriod > 0)
        {
            long ttl = this.parsingTime + refreshPeriod;
            if (System.currentTimeMillis() > ttl)
            {
                long lastModified = WebXmlParser.getWebXmlLastModified(context);
                return lastModified == 0 || lastModified > ttl;
            }
        }
        return false;
    }

    private static final String WEB_XML_ATTR = WebXml.class.getName();
    public static WebXml getWebXml(ExternalContext context)
    {
        WebXml webXml = (WebXml)context.getApplicationMap().get(WEB_XML_ATTR);
        if (webXml == null)
        {
            init(context);
            webXml = (WebXml)context.getApplicationMap().get(WEB_XML_ATTR);
        }
        return webXml;
    }

    /**
     * should be called when initialising Servlet
     * @param context
     */
    public static void init(ExternalContext context)
    {
        WebXmlParser parser = new WebXmlParser(context);
        WebXml webXml = parser.parse();
        context.getApplicationMap().put(WEB_XML_ATTR, webXml);
        MyfacesConfig mfconfig = MyfacesConfig.getCurrentInstance(context);
        long configRefreshPeriod = mfconfig.getConfigRefreshPeriod();
        webXml.setParsingTime(System.currentTimeMillis());
        webXml.setDelegateFacesServlet(mfconfig.getDelegateFacesServlet());
        refreshPeriod = (configRefreshPeriod * 1000);
    }

    public static void update(ExternalContext context)
    {
        if (getWebXml(context).isOld(context))
        {
            WebXml.init(context);
        }
    }

}
