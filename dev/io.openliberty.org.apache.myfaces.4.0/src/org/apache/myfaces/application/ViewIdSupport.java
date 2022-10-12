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

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.FacesException;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.render.ResponseStateManager;
import jakarta.faces.view.ViewDeclarationLanguage;
import org.apache.myfaces.config.MyfacesConfig;

import org.apache.myfaces.util.lang.ConcurrentLRUCache;
import org.apache.myfaces.core.api.shared.lang.SharedStringBuilder;
import org.apache.myfaces.util.ExternalContextUtils;
import org.apache.myfaces.util.lang.StringUtils;
import org.apache.myfaces.util.UrlPatternMatcher;

/**
 * A ViewHandlerSupport implementation for use with standard Java Servlet engines,
 * ie an engine that supports jakarta.servlet, and uses a standard web.xml file.
 */
public class ViewIdSupport
{
    private static final String INSTANCE_KEY = ViewIdSupport.class.getName();
    
    private static final String JAKARTA_SERVLET_INCLUDE_SERVLET_PATH = "jakarta.servlet.include.servlet_path";

    private static final String JAKARTA_SERVLET_INCLUDE_PATH_INFO = "jakarta.servlet.include.path_info";
    
    private static final Logger log = Logger.getLogger(ViewIdSupport.class.getName());
    
    private static final String VIEW_HANDLER_SUPPORT_SB = "oam.viewhandler.SUPPORT_SB";
    
    private MyfacesConfig config;
    
    private volatile ConcurrentLRUCache<String, Boolean> viewIdExistsCache;
    private volatile ConcurrentLRUCache<String, String> viewIdDeriveCache;
    private volatile ConcurrentLRUCache<String, Boolean> viewIdProtectedCache;

    public static ViewIdSupport getInstance(FacesContext facesContext)
    {
        ViewIdSupport viewIdSupport = (ViewIdSupport)
                facesContext.getExternalContext().getApplicationMap().get(INSTANCE_KEY);
        if (viewIdSupport == null)
        {
            viewIdSupport = new ViewIdSupport(facesContext);
            facesContext.getExternalContext().getApplicationMap().put(INSTANCE_KEY, viewIdSupport);
        }

        return viewIdSupport;
    }
    
    protected ViewIdSupport(FacesContext facesContext)
    {
        config = MyfacesConfig.getCurrentInstance(facesContext);

        int viewIdCacheSize = config.getViewIdCacheSize();
        if (config.isViewIdExistsCacheEnabled())
        {
            viewIdExistsCache = new ConcurrentLRUCache<>((viewIdCacheSize * 4 + 3) / 3, viewIdCacheSize);
        }
        if (config.isViewIdDeriveCacheEnabled())
        {
            viewIdDeriveCache = new ConcurrentLRUCache<>((viewIdCacheSize * 4 + 3) / 3, viewIdCacheSize);
        }
        if (config.isViewIdProtectedCacheEnabled())
        {
            viewIdProtectedCache = new ConcurrentLRUCache<>((viewIdCacheSize * 4 + 3) / 3, viewIdCacheSize);
        }
    }

    public String deriveLogicalViewId(FacesContext context, String rawViewId)
    {
        return deriveViewId(context, rawViewId, false);
    }
    
    public String deriveViewId(FacesContext context, String viewId)
    {
        return deriveViewId(context, viewId, true);
    }
    
    protected String deriveViewId(FacesContext context, String rawViewId, boolean checkViewExists)
    {
        //If no viewId found, don't try to derive it, just continue.
        if (rawViewId == null)
        {
            return null;
        }

        String viewId = null;
        if (viewIdDeriveCache != null)
        {
            viewId = viewIdDeriveCache.get(rawViewId);
        }

        if (viewId == null)
        {
            FacesServletMapping mapping = FacesServletMappingUtils.getCurrentRequestFacesServletMapping(context);
            if (mapping == null || mapping.isExtensionMapping())
            {
                viewId = handleSuffixMapping(context, rawViewId);
            }
            else if (mapping.isExactMapping())
            {
                // if the current request is a exact mapping and the viewId equals the exact viewId
                if (rawViewId.equals(mapping.getExact()))
                {
                    viewId = handleSuffixMapping(context, rawViewId + ".jsf");
                }
                // otherwise lets try to resolve a possible mapping for the requested viewId
                else
                {
                    viewId = rawViewId;
                }
            }
            else if (mapping.isPrefixMapping())
            {
                viewId = handlePrefixMapping(rawViewId, mapping.getPrefix());

                // A viewId that is equals to the prefix mapping on servlet mode is
                // considered invalid, because jsp vdl will use RequestDispatcher and cause
                // a loop that ends in a exception. Note in portlet mode the view
                // could be encoded as a query param, so the viewId could be valid.
                if (viewId != null
                        && viewId.equals(mapping.getPrefix())
                        && !ExternalContextUtils.isPortlet(context.getExternalContext()))
                {
                    throw new InvalidViewIdException();
                }

                // In JSF 2.3 some changes were done in the VDL to avoid the jsp vdl
                // RequestDispatcher redirection (only accept viewIds with jsp extension).
                // If we have this case
                if (viewId != null && viewId.equals(mapping.getPrefix()))
                {
                    viewId = handleSuffixMapping(context, viewId + ".jsf");
                }
            }
            else if (mapping.getUrlPattern().startsWith(rawViewId))
            {
                throw new InvalidViewIdException(rawViewId);
            }
            
            if (viewId != null && viewIdDeriveCache != null)
            {
                viewIdDeriveCache.put(rawViewId, viewId);
            }
        }
        
        if (viewId != null && checkViewExists)
        {
            return isViewExistent(context, viewId) ? viewId : null;
        }
        
        return viewId; // return null if no physical resource exists
    }
    
    
    
    /**
     * Return a string containing a webapp-relative URL that the user can invoke
     * to render the specified view.
     * <p>
     * URLs and ViewIds are not quite the same; for example a url of "/foo.jsf"
     * or "/faces/foo.jsp" may be needed to access the view "/foo.jsp". 
     */
    public String calculateActionURL(FacesContext context, String viewId)
    {
        if (viewId == null || !viewId.startsWith("/"))
        {
            throw new IllegalArgumentException("ViewId must start with a '/': " + viewId);
        }

        FacesServletMapping mapping = FacesServletMappingUtils.getCurrentRequestFacesServletMapping(context);
        ExternalContext externalContext = context.getExternalContext();
        String contextPath = externalContext.getRequestContextPath();
        StringBuilder builder = SharedStringBuilder.get(context, VIEW_HANDLER_SUPPORT_SB);
        // If the context path is root, it is not necessary to append it, otherwise
        // and extra '/' will be set.
        if (contextPath != null && !(contextPath.length() == 1 && contextPath.charAt(0) == '/') )
        {
            builder.append(contextPath);
        }
        
        // In JSF 2.3 we could have cases where the viewId can be bound to an url-pattern that is not
        // prefix or suffix, but exact mapping. In this part we need to take the viewId and check if
        // the viewId is bound or not with a mapping.
        if (mapping != null && mapping.isExactMapping())
        {
            String exactMappingViewId = calculateExactMapping(context, viewId);
            if (exactMappingViewId != null && !exactMappingViewId.isEmpty())
            {
                // if the current exactMapping already matches the requested viewId -> same view, skip....
                if (!mapping.getExact().equals(exactMappingViewId))
                {
                    // different viewId -> lets try to lookup a exact mapping
                    mapping = FacesServletMappingUtils.getExactMapping(context, exactMappingViewId);

                    // no exactMapping for the requested viewId available BUT the current view is a exactMapping
                    // we need a to search for a prefix or extension mapping
                    if (mapping == null)
                    {
                        mapping = FacesServletMappingUtils.getGenericPrefixOrSuffixMapping(context);
                        if (mapping == null)
                        {
                            throw new IllegalStateException(
                                    "No generic (either prefix or suffix) servlet-mapping found for FacesServlet."
                                    + "This is required serve views, that are not exact mapped.");
                        }
                    }
                }
            }
        }

        if (mapping != null)
        {
            if (mapping.isExactMapping())
            {
                builder.append(mapping.getExact());
            }
            else if (mapping.isExtensionMapping())
            {
                //See JSF 2.0 section 7.5.2 
                boolean founded = false;
                for (String contextSuffix : config.getViewSuffix())
                {
                    if (viewId.endsWith(contextSuffix))
                    {
                        builder.append(viewId.substring(0, viewId.indexOf(contextSuffix)));
                        builder.append(mapping.getExtension());
                        founded = true;
                        break;
                    }
                }
                if (!founded)
                {   
                    //See JSF 2.0 section 7.5.2
                    // - If the argument viewId has an extension, and this extension is mapping, 
                    // the result is contextPath + viewId
                    //
                    // -= Leonardo Uribe =- It is evident that when the page is generated, the derived 
                    // viewId will end with the 
                    // right contextSuffix, and a navigation entry on faces-config.xml should use such id,
                    // this is just a workaroud
                    // for usability. There is a potential risk that change the mapping in a webapp make 
                    // the same application fail,
                    // so use viewIds ending with mapping extensions is not a good practice.
                    if (viewId.endsWith(mapping.getExtension()))
                    {
                        builder.append(viewId);
                    }
                    else if(viewId.lastIndexOf('.') != -1 )
                    {
                        builder.append(viewId.substring(0, viewId.lastIndexOf('.')));
                        builder.append(config.getViewSuffix()[0]);
                    }
                    else
                    {
                        builder.append(viewId);
                        builder.append(config.getViewSuffix()[0]);
                    }
                }
            }
            else if (mapping.isPrefixMapping())
            {
                builder.append(mapping.getPrefix());
                builder.append(viewId);
            }
        }
        else
        {
            builder.append(viewId);
        }
        
        
        //JSF 2.2 check view protection.
        if (isViewProtected(context, viewId))
        {
            int index = builder.indexOf("?");
            if (index >= 0)
            {
                builder.append('&');
            }
            else
            {
                builder.append('?');
            }
            builder.append(ResponseStateManager.NON_POSTBACK_VIEW_TOKEN_PARAM);
            builder.append('=');
            ResponseStateManager rsm = context.getRenderKit().getResponseStateManager();
            builder.append(rsm.getCryptographicallyStrongTokenFromSession(context));
        }
        
        String calculatedActionURL = builder.toString();
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Calculated actionURL: '" + calculatedActionURL + "' for viewId: '" + viewId + '\'');
        }
        return calculatedActionURL;
    }
    
    private String calculateExactMapping(FacesContext context, String viewId)
    {
        String prefixedExactMapping = null;
        for (String contextSuffix : config.getViewSuffix())
        {
            if (viewId.endsWith(contextSuffix))
            {
                prefixedExactMapping = viewId.substring(0, viewId.length() - contextSuffix.length());
                break;
            }
        }
        return prefixedExactMapping == null ? viewId : prefixedExactMapping;
    }

    /**
     * Return the normalized viewId according to the algorithm specified in 7.5.2 
     * by stripping off any number of occurrences of the prefix mapping from the viewId.
     * <p>
     * For example, both /faces/view.xhtml and /faces/faces/faces/view.xhtml would both return view.xhtml
     * </p>
     */
    protected String handlePrefixMapping(String viewId, String prefix)
    {
        // If prefix mapping (such as "/faces/*") is used for FacesServlet, 
        // normalize the viewId according to the following
        // algorithm, or its semantic equivalent, and return it.
               
        // Remove any number of occurrences of the prefix mapping from the viewId. 
        // For example, if the incoming value was /faces/faces/faces/view.xhtml 
        // the result would be simply view.xhtml.
        
        if (StringUtils.isBlank(prefix))
        {
            // if prefix is an empty string (Spring environment), we let it be "//"
            // in order to prevent an infinite loop in uri.startsWith(-emptyString-).
            // Furthermore a prefix of "//" is just another double slash prevention.
            prefix = "//";
        }
        else
        {
            // need to make sure its really /faces/* and not /facesPage.xhtml
            prefix = prefix + '/'; 
        }
        
        String uri = viewId;
        while (uri.startsWith(prefix) || uri.startsWith("//")) 
        {
            if (uri.startsWith(prefix))
            {
                // cut off only /faces, leave the trailing '/' char for the next iteration
                uri = uri.substring(prefix.length() - 1);
            }
            else
            {
                // uri starts with '//' --> cut off the leading slash, leaving
                // the second slash to compare for the next iteration
                uri = uri.substring(1);
            }
        }

        return uri;
    }
    
    /**
     * Return the viewId with any non-standard suffix stripped off and replaced with
     * the default suffix configured for the specified context.
     * <p>
     * For example, an input parameter of "/foo.jsf" may return "/foo.jsp".
     * </p>
     */
    protected String handleSuffixMapping(FacesContext context, String requestViewId)
    {        
        int slashPos = requestViewId.lastIndexOf('/');
        int extensionPos = requestViewId.lastIndexOf('.');
        
        StringBuilder builder = SharedStringBuilder.get(context, VIEW_HANDLER_SUPPORT_SB);
        
        //Try to locate any resource that match with the expected id
        for (String defaultSuffix : config.getViewSuffix())
        {
            builder.setLength(0);
            builder.append(requestViewId);
           
            if (extensionPos > -1 && extensionPos > slashPos)
            {
                builder.replace(extensionPos, requestViewId.length(), defaultSuffix);
            }
            else
            {
                builder.append(defaultSuffix);
            }

            String candidateViewId = builder.toString();
            
            if (config.getFaceletsViewMappings() != null && config.getFaceletsViewMappings().length > 0 )
            {
                for (String mapping : config.getFaceletsViewMappings())
                {
                    if (mapping.startsWith("/"))
                    {
                        continue;   //skip this entry, its a prefix mapping
                    }
                    if (mapping.equals(candidateViewId))
                    {
                        return candidateViewId;
                    }
                    if (mapping.startsWith(".")) //this is a wildcard entry
                    {
                        builder.setLength(0); //reset/reuse the builder object 
                        builder.append(candidateViewId); 
                        builder.replace(candidateViewId.lastIndexOf('.'), candidateViewId.length(), mapping);
                        String tempViewId = builder.toString();
                        if (isViewExistent(context, tempViewId))
                        {
                            return tempViewId;
                        }
                    }
                }
            }

            // forced facelets mappings did not match or there were no entries in faceletsViewMappings array
            if (isViewExistent(context,candidateViewId))
            {
                return candidateViewId;
            }
        }
        
        //jsp suffixes didn't match, try facelets suffix
        String faceletsDefaultSuffix = config.getFaceletsViewSuffix();
        if (faceletsDefaultSuffix != null)
        {
            for (String defaultSuffix : config.getViewSuffix())
            {
                if (faceletsDefaultSuffix.equals(defaultSuffix))
                {
                    faceletsDefaultSuffix = null;
                    break;
                }
            }
        }
        if (faceletsDefaultSuffix != null)
        {
            builder.setLength(0);
            builder.append(requestViewId);
            
            if (extensionPos > -1 && extensionPos > slashPos)
            {
                builder.replace(extensionPos, requestViewId.length(), faceletsDefaultSuffix);
            }
            else
            {
                builder.append(faceletsDefaultSuffix);
            }
            
            String candidateViewId = builder.toString();
            if (isViewExistent(context,candidateViewId))
            {
                return candidateViewId;
            }
        }

        // Otherwise, if a physical resource exists with the name requestViewId let that value be viewId.
        if (isViewExistent(context,requestViewId))
        {
            return requestViewId;
        }
        
        //Otherwise return null.
        return null;
    }
    
    /**
     * Check if a view exists
     * 
     * @param facesContext
     * @param viewId
     * @return 
     */
    public boolean isViewExistent(FacesContext facesContext, String viewId)
    {
       try
        {
            Boolean resourceExists = null;
            if (viewIdExistsCache != null)
            {
                resourceExists = viewIdExistsCache.get(viewId);
            }

            if (resourceExists == null)
            {
                ViewDeclarationLanguage vdl = facesContext.getApplication().getViewHandler()
                        .getViewDeclarationLanguage(facesContext, viewId);
                if (vdl != null)
                {
                    resourceExists = vdl.viewExists(facesContext, viewId);
                }
                else
                {
                    // Fallback to default strategy
                    resourceExists = facesContext.getExternalContext().getResource(viewId) != null;
                }

                if (viewIdExistsCache != null)
                {
                    viewIdExistsCache.put(viewId, resourceExists);
                }
            }

            return resourceExists;
        }
        catch (MalformedURLException e)
        {
            //ignore and move on
        }     
        return false;
    }

    /**
     * <p>
     * Calculates the view id from the given faces context by the following algorithm
     * </p>
     * <ul>
     * <li>lookup the viewid from the request attribute "jakarta.servlet.include.path_info"
     * <li>if null lookup the value for viewid by {@link jakarta.faces.context.ExternalContext#getRequestPathInfo()}
     * <li>if null lookup the value for viewid from the request attribute "jakarta.servlet.include.servlet_path"
     * <li>if null lookup the value for viewid by {@link jakarta.faces.context.ExternalContext#getRequestServletPath()}
     * <li>if null throw a {@link jakarta.faces.FacesException}
     * </ul>
     */
    public String calculateViewId(FacesContext facesContext)
    {
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();

        boolean traceEnabled = log.isLoggable(Level.FINEST);
        
        String viewId = null;
        if (ExternalContextUtils.isPortlet(externalContext))
        {
            viewId = (String) externalContext.getRequestPathInfo();
        }
        else
        {
            viewId = (String) requestMap.get(JAKARTA_SERVLET_INCLUDE_PATH_INFO);
            if (viewId != null)
            {
                if (traceEnabled)
                {
                    log.finest("Calculated viewId '" + viewId + "' from request param '"
                            + JAKARTA_SERVLET_INCLUDE_PATH_INFO + '\'');
                }
            }
            else
            {
                viewId = externalContext.getRequestPathInfo();
                if (viewId != null && traceEnabled)
                {
                    log.finest("Calculated viewId '" + viewId + "' from request path info");
                }
            }
    
            if (viewId == null)
            {
                viewId = (String) requestMap.get(JAKARTA_SERVLET_INCLUDE_SERVLET_PATH);
                if (viewId != null && traceEnabled)
                {
                    log.finest("Calculated viewId '" + viewId + "' from request param '"
                            + JAKARTA_SERVLET_INCLUDE_SERVLET_PATH + '\'');
                }
            }
        }
        
        if (viewId == null)
        {
            viewId = externalContext.getRequestServletPath();
            if (viewId != null && traceEnabled)
            {
                log.finest("Calculated viewId '" + viewId + "' from request servlet path");
            }
        }

        if (viewId == null)
        {
            throw new FacesException("Could not determine view id.");
        }

        return viewId;
    }
    
    
    
    public boolean isViewProtected(FacesContext context, String viewId)
    {
        if (viewId == null)
        {
            return false;
        }
        
        Boolean protectedView = null;
        if (viewIdProtectedCache != null)
        {
            protectedView = viewIdProtectedCache.get(viewId);
        }
        
        if (protectedView == null)
        {
            protectedView = false;
            
            Set<String> protectedViews = context.getApplication().getViewHandler().getProtectedViewsUnmodifiable();
            if (!protectedViews.isEmpty())
            {
                for (String urlPattern : protectedViews)
                {
                    if (UrlPatternMatcher.match(viewId, urlPattern))
                    {
                        protectedView = true;
                        break;
                    }
                }
            }
            
            if (viewIdProtectedCache != null)
            {
                viewIdProtectedCache.put(viewId, protectedView);
            }
        }
         
        return protectedView;
    }
}
