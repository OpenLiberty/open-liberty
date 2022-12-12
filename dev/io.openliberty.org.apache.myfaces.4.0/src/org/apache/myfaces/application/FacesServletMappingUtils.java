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

package org.apache.myfaces.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

import org.apache.myfaces.config.webparameters.MyfacesConfig;
import org.apache.myfaces.context.servlet.StartupFacesContextImpl;
import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.util.ExternalContextUtils;
import org.apache.myfaces.webapp.DelegatedFacesServlet;

public class FacesServletMappingUtils
{
    private static final String FACES_SERVLET_REGISTRATION = "org.apache.myfaces.FACES_SERVLET_REGISTRATION";
    private static final String SERVLET_REGISTRATIONS = "org.apache.myfaces.SERVLET_REGISTRATIONS";
    
    private static final String CURRENT_REQUEST_FACES_SERVLET = "org.apache.myfaces.CURRENT_FACES_SERVLET_MAPPING";
    
    /**
     * Wrapper for better performance
     */
    public static class ServletRegistrationInfo
    {
        private String className;
        private String[] mappings;
        private boolean facesServlet;
        private ServletRegistration registration;

        public ServletRegistrationInfo(ServletRegistration registration, boolean facesServlet)
        {
            this.className = registration.getClassName();
            this.facesServlet = facesServlet;
            this.registration = registration;

            Collection<String> mappingsCollection = registration.getMappings();
            mappings = mappingsCollection.toArray(new String[mappingsCollection.size()]);
            if (mappings == null)
            {
                mappings = new String[]{ };
            }
        }

        public String getClassName()
        {
            return className;
        }

        public String[] getMappings()
        {
            return mappings;
        }

        public boolean isFacesServlet()
        {
            return facesServlet;
        }
        
        public ServletRegistration getRegistration()
        {
            return registration;
        }
    }
    
    public static FacesServletMapping getCurrentRequestFacesServletMapping(FacesContext context)
    {
        Map<Object, Object> attributes = context.getAttributes();

        // Has the mapping already been determined during this request?
        FacesServletMapping mapping = (FacesServletMapping) attributes.get(CURRENT_REQUEST_FACES_SERVLET);
        if (mapping == null)
        {
            ExternalContext externalContext = context.getExternalContext();
            mapping = calculateFacesServletMapping(
                    context,
                    externalContext.getRequestServletPath(),
                    externalContext.getRequestPathInfo(),
                    true);

            attributes.put(CURRENT_REQUEST_FACES_SERVLET, mapping);
        }
        return mapping;
    }

    public static List<ServletRegistrationInfo> getServletRegistrations(FacesContext facesContext,
            ServletContext servletContext)
    {
        Map<String, Object> applicationMap = facesContext.getExternalContext().getApplicationMap();
        
        List<ServletRegistrationInfo> infos =
                (List<ServletRegistrationInfo>) applicationMap.get(SERVLET_REGISTRATIONS);
        if (infos == null)
        {
            infos = new ArrayList<>();
            
            Map<String, ? extends ServletRegistration> registrations = servletContext.getServletRegistrations();
            if (registrations != null)
            {
                for (ServletRegistration servletRegistration : registrations.values())
                {
                    ServletRegistrationInfo info = new ServletRegistrationInfo(servletRegistration,
                            isFacesServlet(facesContext, servletRegistration.getClassName()));

                    infos.add(info);
                }
            }
            
            infos = Collections.unmodifiableList(infos);
            if (!(facesContext instanceof StartupFacesContextImpl))
            {
                applicationMap.put(SERVLET_REGISTRATIONS, infos);
            }
        }

        return infos;
    }
    
    public static ServletRegistrationInfo getFacesServletRegistration(FacesContext facesContext,
            ServletContext servletContext)
    {
        Map<String, Object> applicationMap = facesContext.getExternalContext().getApplicationMap();
        
        ServletRegistrationInfo facesServletRegistration = (ServletRegistrationInfo)
                applicationMap.get(FACES_SERVLET_REGISTRATION);
        if (facesServletRegistration == null)
        {
            for (ServletRegistrationInfo info : getServletRegistrations(facesContext, servletContext))
            {
                if (info.isFacesServlet())
                {
                    facesServletRegistration = info;
                    break;
                }
            }

            if (facesServletRegistration != null && !(facesContext instanceof StartupFacesContextImpl))
            {
                applicationMap.put(FACES_SERVLET_REGISTRATION, facesServletRegistration);
            }
        }

        return facesServletRegistration;
    }

    public static boolean isFacesServlet(FacesContext facesContext, String servletClassName)
    {
        // shortcut to avoid class lookup
        if (FacesServlet.class.getName().equals(servletClassName))
        {
            return true;
        }

        Class servletClass = ClassUtils.simpleClassForName(servletClassName, false);
        if (servletClass != null)
        {
            MyfacesConfig config = MyfacesConfig.getCurrentInstance(facesContext);
            
            return FacesServlet.class.isAssignableFrom(servletClass)
                    || DelegatedFacesServlet.class.isAssignableFrom(servletClass)
                    || servletClass.getName().equals(config.getDelegateFacesServlet());
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
            List<ServletRegistrationInfo> servletRegistrations =
                    getServletRegistrations(facesContext, servletContext);
            if (servletRegistrations  != null)
            {
                FacesServletMapping facesExtensionMapping = null;
                FacesServletMapping facesPrefixMapping = null;
                FacesServletMapping facesExactMapping = null;

                for (ServletRegistrationInfo servletRegistration : servletRegistrations)
                {
                    try
                    {
                        if (servletRegistration.isFacesServlet())
                        {
                            for (String mapping : servletRegistration.getMappings())
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
                        else
                        {
                            //This is not a FacesServlet mapping. 
                            //It could be a non-faces request, we need to look for exact mapping to servletPath
                            //this happens with richfaces resources
                            for (String mapping : servletRegistration.getMappings())
                            {                                
                                if (mapping.startsWith("/") && mapping.endsWith("/*"))
                                {
                                    mapping = mapping.substring(0, mapping.length()-2);
                                }                                
                                if (mapping.equals(servletPath))
                                {
                                    return FacesServletMapping.createExactMapping(mapping); // MYFACES-4524
                                }
                            }
                       }
                    }
                    catch (Exception ex)
                    {
                        //No op
                    }
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
                ServletRegistrationInfo facesServletRegistration =
                        getFacesServletRegistration(facesContext, (ServletContext) context);
                if (facesServletRegistration != null)
                {
                    for (String mapping : facesServletRegistration.getMappings())
                    {
                        if (!mapping.contains("*") && prefixedExactMappingViewId.equals(mapping))
                        {
                            return FacesServletMapping.createExactMapping(prefixedExactMappingViewId);
                        }
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
                ServletRegistrationInfo facesServletRegistration =
                        getFacesServletRegistration(facesContext, (ServletContext) context);
                if (facesServletRegistration != null)
                {
                    for (String mapping : facesServletRegistration.getMappings())
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
