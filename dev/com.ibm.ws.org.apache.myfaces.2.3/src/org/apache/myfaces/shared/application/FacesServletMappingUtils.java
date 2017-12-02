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

package org.apache.myfaces.shared.application;

import java.util.Collection;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import org.apache.myfaces.shared.util.ExternalContextUtils;
import org.apache.myfaces.shared.webapp.webxml.DelegatedFacesServlet;
import org.apache.myfaces.shared.webapp.webxml.WebXml;

/**
 *
 */
public class FacesServletMappingUtils
{

    public static FacesServletMapping calculateGenericFacesServletMapping(
        FacesContext facesContext, String servletPath, String pathInfo)
    {
        if (ExternalContextUtils.isPortlet(facesContext.getExternalContext()))
        {
            return calculateFacesServletMapping(servletPath, pathInfo);
        }
        else
        {
            Object context = facesContext.getExternalContext().getContext();
            if (context instanceof ServletContext)
            {
                if (pathInfo != null)
                {
                    // If there is a "extra path", it's definitely no extension mapping.
                    // Now we just have to determine the path which has been specified
                    // in the url-pattern, but that's easy as it's the same as the
                    // current servletPath. It doesn't even matter if "/*" has been used
                    // as in this case the servletPath is just an empty string according
                    // to the Servlet Specification (SRV 4.4).
                    return createMappingFromServletRegistration(facesContext, 
                            (ServletContext)context, servletPath, pathInfo, false);
                }
                else
                {
                    // In the case of extension mapping, no "extra path" is available.
                    // Still it's possible that prefix-based mapping has been used.
                    // Actually, if there was an exact match no "extra path"
                    // is available (e.g. if the url-pattern is "/faces/*"
                    // and the request-uri is "/context/faces").
                    int slashPos = servletPath.lastIndexOf('/');
                    int extensionPos = servletPath.lastIndexOf('.');
                    if (extensionPos > -1 && extensionPos > slashPos)
                    {
                        String extension = servletPath.substring(extensionPos);
                        return FacesServletMapping.createExtensionMapping(extension);
                    }
                    else
                    {
                        // There is no extension in the given servletPath and therefore
                        // we assume that it's an exact match using prefix-based mapping.
                        return createMappingFromServletRegistration(facesContext, 
                                (ServletContext)context, servletPath, pathInfo, false);
                    }
                }
            }
            else
            {
                return calculateFacesServletMapping(servletPath, pathInfo);
            }
        }
        //return null;
    }
    
    public static FacesServletMapping calculateFacesServletMapping(
        FacesContext facesContext, String servletPath, String pathInfo)
    {
        if (ExternalContextUtils.isPortlet(facesContext.getExternalContext()))
        {
            return calculateFacesServletMapping(servletPath, pathInfo);
        }
        else
        {
            Object context = facesContext.getExternalContext().getContext();
            if (context instanceof ServletContext)
            {
                if (pathInfo != null)
                {
                    // If there is a "extra path", it's definitely no extension mapping.
                    // Now we just have to determine the path which has been specified
                    // in the url-pattern, but that's easy as it's the same as the
                    // current servletPath. It doesn't even matter if "/*" has been used
                    // as in this case the servletPath is just an empty string according
                    // to the Servlet Specification (SRV 4.4).
                    return createMappingFromServletRegistration(facesContext, 
                            (ServletContext)context, servletPath, pathInfo, true);
                }
                else
                {
                    // In the case of extension mapping, no "extra path" is available.
                    // Still it's possible that prefix-based mapping has been used.
                    // Actually, if there was an exact match no "extra path"
                    // is available (e.g. if the url-pattern is "/faces/*"
                    // and the request-uri is "/context/faces").
                    int slashPos = servletPath.lastIndexOf('/');
                    int extensionPos = servletPath.lastIndexOf('.');
                    if (extensionPos > -1 && extensionPos > slashPos)
                    {
                        String extension = servletPath.substring(extensionPos);
                        return FacesServletMapping.createExtensionMapping(extension);
                    }
                    else
                    {
                        // There is no extension in the given servletPath and therefore
                        // we assume that it's an exact match using prefix-based mapping.
                        return createMappingFromServletRegistration(facesContext, 
                                (ServletContext)context, servletPath, pathInfo, true);
                    }
                }
            }
            else
            {
                return calculateFacesServletMapping(servletPath, pathInfo);
            }
        }
        //return null;
    }
    
    private static FacesServletMapping createMappingFromServletRegistration(FacesContext facesContext, 
            ServletContext servletContext, String servletPath, String pathInfo, boolean allowExactMatch)
    {
        try
        {
            Map<String, ? extends ServletRegistration> map = servletContext.getServletRegistrations();
            if (map != null)
            {
                for (Map.Entry<String, ? extends ServletRegistration> entry : map.entrySet())
                {
                    try
                    {
                        if (isFacesServlet(facesContext, (String)entry.getValue().getClassName()))
                        {
                            Collection<String> mappings = entry.getValue().getMappings();
                            FacesServletMapping extensionMapping = null;
                            FacesServletMapping prefixMapping = null;
                            FacesServletMapping exactMapping = null;
                            for (String mapping : mappings)
                            {
                                if (mapping.startsWith("*."))
                                {
                                    // extension mapping, use it.
                                    extensionMapping = FacesServletMapping.createExtensionMapping(
                                            mapping.substring(1));
                                }
                                else if (mapping.startsWith("/") && mapping.endsWith("/*"))
                                {
                                    // prefix mapping, use it.
                                    prefixMapping = FacesServletMapping.createPrefixMapping(
                                            mapping.substring(0, mapping.length()-2));
                                }
                                else if (allowExactMatch && mapping.startsWith("/") && mapping.equals(servletPath))
                                {
                                    exactMapping = FacesServletMapping.createPrefixMapping(servletPath);
                                }
                            }
                            // Choose exact mapping if preferred.
                            if (allowExactMatch && exactMapping != null)
                            {
                                return exactMapping;
                            }
                            else if (prefixMapping != null)
                            {
                                return prefixMapping;
                            }
                            else if (extensionMapping != null)
                            {
                                return extensionMapping;
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        //No op
                    }
                }
                return FacesServletMapping.createPrefixMapping(servletPath);
            }
            else
            {
                return FacesServletMapping.createPrefixMapping(servletPath);
            }
        }
        catch(Exception ex)
        {
            return FacesServletMapping.createPrefixMapping(servletPath);
        }
    }
    
    public static boolean isFacesServlet(FacesContext facesContext, String servletClassName)
    {
        Class servletClass = org.apache.myfaces.shared.util.ClassUtils.simpleClassForName(
                servletClassName);
        return (FacesServlet.class.isAssignableFrom(servletClass) ||
                            DelegatedFacesServlet.class.isAssignableFrom(servletClass) ||
                            servletClass.getName().equals(
                                    WebXml.getWebXml(facesContext.getExternalContext()).getDelegateFacesServlet()));
    }
    
    /**
     * Determines the mapping of the FacesServlet in the web.xml configuration
     * file. However, there is no need to actually parse this configuration file
     * as runtime information is sufficient.
     *
     * @param servletPath The servletPath of the current request
     * @param pathInfo    The pathInfo of the current request
     * @return the mapping of the FacesServlet in the web.xml configuration file
     */
    private static FacesServletMapping calculateFacesServletMapping(
        String servletPath, String pathInfo)
    {
        if (pathInfo != null)
        {
            // If there is a "extra path", it's definitely no extension mapping.
            // Now we just have to determine the path which has been specified
            // in the url-pattern, but that's easy as it's the same as the
            // current servletPath. It doesn't even matter if "/*" has been used
            // as in this case the servletPath is just an empty string according
            // to the Servlet Specification (SRV 4.4).
            return FacesServletMapping.createPrefixMapping(servletPath);
        }
        else
        {
            // In the case of extension mapping, no "extra path" is available.
            // Still it's possible that prefix-based mapping has been used.
            // Actually, if there was an exact match no "extra path"
            // is available (e.g. if the url-pattern is "/faces/*"
            // and the request-uri is "/context/faces").
            int slashPos = servletPath.lastIndexOf('/');
            int extensionPos = servletPath.lastIndexOf('.');
            if (extensionPos > -1 && extensionPos > slashPos)
            {
                String extension = servletPath.substring(extensionPos);
                return FacesServletMapping.createExtensionMapping(extension);
            }
            else
            {
                // There is no extension in the given servletPath and therefore
                // we assume that it's an exact match using prefix-based mapping.
                return FacesServletMapping.createPrefixMapping(servletPath);
            }
        }
    }
    
    public static FacesServletMapping calculateFacesServletMappingFromPrefixedExactMappingViewId(
        FacesContext facesContext, String prefixedExactMappingViewId)
    {
        FacesServletMapping mapping = null;
        if (!ExternalContextUtils.isPortlet(facesContext.getExternalContext()))
        {
            Object context = facesContext.getExternalContext().getContext();
            if (context instanceof ServletContext)
            {
                Map<String, ? extends ServletRegistration> map = ((ServletContext)context).getServletRegistrations();
                if (map != null)
                {
                    for (Map.Entry<String, ? extends ServletRegistration> entry : map.entrySet())
                    {
                        try
                        {
                            if (isFacesServlet(facesContext, (String)entry.getValue().getClassName()))
                            {
                                Collection<String> mappings = entry.getValue().getMappings();
                                for (String m : mappings)
                                {
                                    if ((!m.contains("*")) && prefixedExactMappingViewId.equals(m))
                                    {
                                        mapping = FacesServletMapping.createPrefixMapping(prefixedExactMappingViewId);
                                        break;
                                    }
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            //No op
                        }
                    }
                }
            }
        }
        return mapping;
    }
}
