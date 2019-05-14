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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared.util.ExternalContextUtils;
import org.apache.myfaces.shared.webapp.webxml.DelegatedFacesServlet;
import org.apache.myfaces.shared.webapp.webxml.WebXml;

public class FacesServletMappingUtils
{
    private static final String FACES_SERVLET_REGISTRATION = "org.apache.myfaces.FACES_SERVLET_REGISTRATION";
    private static final String FACES_SERVLET_MAPPINGS = "org.apache.myfaces.FACES_SERVLET_MAPPINGS";
    
    public static ServletRegistration getFacesServletRegistration(FacesContext facesContext,
            ServletContext servletContext)
    {
        Map<String, Object> applicationMap = facesContext.getExternalContext().getApplicationMap();
        
        ServletRegistration servletRegistration = (ServletRegistration) applicationMap.get(FACES_SERVLET_REGISTRATION);
        if (servletRegistration == null)
        {
            Map<String, ? extends ServletRegistration> registrations = servletContext.getServletRegistrations();
            if (registrations != null)
            {
                for (Map.Entry<String, ? extends ServletRegistration> entry : registrations.entrySet())
                {
                    if (isFacesServlet(facesContext, entry.getValue().getClassName()))
                    {
                        servletRegistration = entry.getValue();
                        break;
                    }
                }
            }
            
            applicationMap.put(FACES_SERVLET_REGISTRATION, servletRegistration);
        }

        return servletRegistration;
    }
    
    public static String[] getFacesServletMappings(FacesContext facesContext,ServletContext servletContext)
    {
        Map<String, Object> applicationMap = facesContext.getExternalContext().getApplicationMap();
        
        String[] mappings = (String[]) applicationMap.get(FACES_SERVLET_MAPPINGS);
        if (mappings == null)
        {
            ServletRegistration servletRegistration = getFacesServletRegistration(facesContext, servletContext);
            if (servletRegistration != null)
            {
                Collection<String> mappingsCollection = servletRegistration.getMappings();
                mappings = mappingsCollection.toArray(new String[mappingsCollection.size()]);
            }
            
            if (mappings == null)
            {
                mappings = new String[]{ };
            }

            applicationMap.put(FACES_SERVLET_MAPPINGS, mappings);
        }
        
        return mappings;
    }
    
    public static boolean isFacesServlet(FacesContext facesContext, String servletClassName)
    {
        Class servletClass = ClassUtils.simpleClassForName(servletClassName, false);
        if (servletClass != null)
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            
            return FacesServlet.class.isAssignableFrom(servletClass)
                    || DelegatedFacesServlet.class.isAssignableFrom(servletClass)
                    || servletClass.getName().equals(WebXml.getWebXml(externalContext).getDelegateFacesServlet());
        }
        return false;
    }
    
    
    
    

    public static FacesServletMapping calculateFacesServletMapping(
        FacesContext facesContext, String servletPath, String pathInfo, boolean allowExactMapping)
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
                            (ServletContext)context, servletPath, pathInfo, allowExactMapping);
                }
                else
                {
                    // In the case of extension mapping, no "extra path" is available.
                    // Still it's possible that prefix-based mapping has been used.
                    // Actually, if there was an exact match no "extra path"
                    // is available (e.g. if the url-pattern is "/faces/*"
                    // and the request-uri is "/context/faces").
                    String extension = extractExtensionFromUrl(servletPath);
                    if (extension != null)
                    {
                        return FacesServletMapping.createExtensionMapping(extension);
                    }
                    else
                    {
                        // There is no extension in the given servletPath and therefore
                        // we assume that it's an exact match using prefix-based mapping.
                        return createMappingFromServletRegistration(facesContext, 
                                (ServletContext)context, servletPath, pathInfo, allowExactMapping);
                    }
                }
            }
            else
            {
                return calculateFacesServletMapping(servletPath, pathInfo);
            }
        }
    }
    
    private static FacesServletMapping createMappingFromServletRegistration(FacesContext facesContext, 
            ServletContext servletContext, String servletPath, String pathInfo, boolean allowExactMatch)
    {
        try
        {
            ServletRegistration facesServletRegistration = getFacesServletRegistration(
                    facesContext, servletContext);
            if (facesServletRegistration != null)
            {
                FacesServletMapping facesExtensionMapping = null;
                FacesServletMapping facesPrefixMapping = null;
                FacesServletMapping facesExactMapping = null;

                try
                {
                    String[] mappings = getFacesServletMappings(facesContext, servletContext);
                    for (String mapping : mappings)
                    {
                        if (isExtensionMapping(mapping))
                        {
                            facesExtensionMapping = FacesServletMapping.createExtensionMapping(
                                    extractExtension(mapping));
                        }
                        else if (isPrefixMapping(mapping))
                        {
                            facesPrefixMapping = FacesServletMapping.createPrefixMapping(
                                    extractPrefix(mapping));
                        }
                        else if (allowExactMatch && mapping.startsWith("/") && mapping.equals(servletPath))
                        {
                            facesExactMapping = FacesServletMapping.createExactMapping(servletPath);
                        }
                    }
                }
                catch (Exception ex)
                {
                    //No op
                }

                // Choose exact mapping if preferred.
                if (allowExactMatch && facesExactMapping != null)
                {
                    return facesExactMapping;
                }
                else if (facesPrefixMapping != null)
                {
                    return facesPrefixMapping;
                }
                else if (facesExtensionMapping != null)
                {
                    return facesExtensionMapping;
                }
                else
                {
                    return FacesServletMapping.createPrefixMapping(servletPath);
                }
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

    /**
     * Determines the mapping of the FacesServlet in the web.xml configuration
     * file. However, there is no need to actually parse this configuration file
     * as runtime information is sufficient.
     *
     * @param servletPath The servletPath of the current request
     * @param pathInfo    The pathInfo of the current request
     * @return the mapping of the FacesServlet in the web.xml configuration file
     */
    private static FacesServletMapping calculateFacesServletMapping(String servletPath, String pathInfo)
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
            String extension = extractExtensionFromUrl(servletPath);
            if (extension != null)
            {
                return FacesServletMapping.createExtensionMapping(extension);
            }
            else
            {
                // There is no extension in the given servletPath and therefore
                // we assume that it's an exact match using prefix-based mapping.
                return FacesServletMapping.createExactMapping(servletPath);
            }
        }
    }
    
    public static FacesServletMapping getExactMapping(FacesContext facesContext, String prefixedExactMappingViewId)
    {
        if (!ExternalContextUtils.isPortlet(facesContext.getExternalContext()))
        {
            Object context = facesContext.getExternalContext().getContext();
            if (context instanceof ServletContext)
            {
                String[] mappings = getFacesServletMappings(facesContext, (ServletContext) context);
                for (String mapping : mappings)
                {
                    if (!mapping.contains("*") && prefixedExactMappingViewId.equals(mapping))
                    {
                        return FacesServletMapping.createExactMapping(prefixedExactMappingViewId);
                    }
                }
            }
        }

        return null;
    }
    
    
    public static FacesServletMapping getGenericPrefixOrSuffixMapping(FacesContext facesContext)
    {
        if (!ExternalContextUtils.isPortlet(facesContext.getExternalContext()))
        {
            Object context = facesContext.getExternalContext().getContext();
            if (context instanceof ServletContext)
            {
                String[] mappings = getFacesServletMappings(facesContext, (ServletContext) context);
                for (String mapping : mappings)
                {
                    if (isExtensionMapping(mapping))
                    {
                        String extension = extractExtension(mapping);
                        return FacesServletMapping.createExtensionMapping(extension);
                    }
                    else if (isPrefixMapping(mapping))
                    {
                        String prefix = extractPrefix(mapping);
                        return FacesServletMapping.createPrefixMapping(prefix);
                    }
                }
            }
        }
        
        return null;
    }
    
    
    
    private static String extractExtensionFromUrl(String url)
    {
        int slashPos = url.lastIndexOf('/');
        int extensionPos = url.lastIndexOf('.');
        if (extensionPos > -1 && extensionPos > slashPos)
        {
            return url.substring(extensionPos);
        }
        
        return null;
    }
    
    private static boolean isExtensionMapping(String mapping)
    {
        return mapping.startsWith("*.");
    }
    
    private static String extractExtension(String mapping)
    {
        return mapping.substring(1);
    }
    
    private static boolean isPrefixMapping(String mapping)
    {
        return mapping.startsWith("/") && mapping.endsWith("/*");
    }
    
    private static String extractPrefix(String mapping)
    {
        return mapping.substring(0, mapping.length() - 2);
    }
}
