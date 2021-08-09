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

import java.net.MalformedURLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.render.ResponseStateManager;
import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.renderkit.html.util.SharedStringBuilder;
import org.apache.myfaces.shared.util.ConcurrentLRUCache;
import org.apache.myfaces.shared.util.ExternalContextUtils;
import org.apache.myfaces.shared.util.StringUtils;
import org.apache.myfaces.shared.util.ViewProtectionUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 * A ViewHandlerSupport implementation for use with standard Java Servlet engines,
 * ie an engine that supports javax.servlet, and uses a standard web.xml file.
 */
public class DefaultViewHandlerSupport implements ViewHandlerSupport
{
    /**
     * Identifies the FacesServlet mapping in the current request map.
     */
    private static final String CACHED_SERVLET_MAPPING =
        DefaultViewHandlerSupport.class.getName() + ".CACHED_SERVLET_MAPPING";

    //private static final Log log = LogFactory.getLog(DefaultViewHandlerSupport.class);
    private static final Logger log = Logger.getLogger(DefaultViewHandlerSupport.class.getName());

    /**
     * Controls the size of the cache used to "remember" if a view exists or not.
     */
    @JSFWebConfigParam(defaultValue = "500", since = "2.0.2", group="viewhandler", tags="performance", 
            classType="java.lang.Integer",
            desc="Controls the size of the cache used to 'remember' if a view exists or not.")
    private static final String CHECKED_VIEWID_CACHE_SIZE_ATTRIBUTE = "org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE";
    private static final int CHECKED_VIEWID_CACHE_DEFAULT_SIZE = 500;

    /**
     * Enable or disable a cache used to "remember" if a view exists or not and reduce the impact of
     * sucesive calls to ExternalContext.getResource().
     */
    @JSFWebConfigParam(defaultValue = "true", since = "2.0.2", expectedValues="true, false", group="viewhandler", 
            tags="performance",
            desc="Enable or disable a cache used to 'remember' if a view exists or not and reduce the impact " +
                 "of sucesive calls to ExternalContext.getResource().")
    private static final String CHECKED_VIEWID_CACHE_ENABLED_ATTRIBUTE = 
        "org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED";
    private static final boolean CHECKED_VIEWID_CACHE_ENABLED_DEFAULT = true;
    
    private static final String VIEW_HANDLER_SUPPORT_SB = "oam.viewhandler.SUPPORT_SB";

    private volatile ConcurrentLRUCache<String, Boolean> _checkedViewIdMap = null;
    private Boolean _checkedViewIdCacheEnabled = null;
    
    private final String[] _faceletsViewMappings;
    private final String[] _contextSuffixes;
    private final String _faceletsContextSufix;
    private final boolean _initialized;
    
    public DefaultViewHandlerSupport()
    {
        _faceletsViewMappings = null;
        _contextSuffixes = null;
        _faceletsContextSufix = null;
        _initialized = false;
    }
    
    public DefaultViewHandlerSupport(FacesContext facesContext)
    {
        _faceletsViewMappings = getFaceletsViewMappings(facesContext);
        _contextSuffixes = getContextSuffix(facesContext);
        _faceletsContextSufix = getFaceletsContextSuffix(facesContext);
        _initialized = true;
    }

    public String calculateViewId(FacesContext context, String viewId)
    {
        //If no viewId found, don't try to derive it, just continue.
        if (viewId == null)
        {
            return null;
        }
        FacesServletMapping mapping = getFacesServletMapping(context);
        if (mapping == null || mapping.isExtensionMapping())
        {
            viewId = handleSuffixMapping(context, viewId);
        }
        else if(mapping.isPrefixMapping())
        {
            viewId = handlePrefixMapping(viewId,mapping.getPrefix());
            
            // A viewId that is equals to the prefix mapping on servlet mode is
            // considered invalid, because jsp vdl will use RequestDispatcher and cause
            // a loop that ends in a exception. Note in portlet mode the view
            // could be encoded as a query param, so the viewId could be valid.
            if (viewId != null && viewId.equals(mapping.getPrefix()) &&
                !ExternalContextUtils.isPortlet(context.getExternalContext()))
            {
                throw new InvalidViewIdException();
            }
        }
        else if (mapping.getUrlPattern().startsWith(viewId))
        {
            throw new InvalidViewIdException(viewId);
        }

        //if(viewId != null)
        //{
        //    return (checkResourceExists(context,viewId) ? viewId : null);
        //}

        return viewId;    // return null if no physical resource exists
    }
    
    public String calculateAndCheckViewId(FacesContext context, String viewId)
    {
        //If no viewId found, don't try to derive it, just continue.
        if (viewId == null)
        {
            return null;
        }
        FacesServletMapping mapping = getFacesServletMapping(context);
        if (mapping == null || mapping.isExtensionMapping())
        {
            viewId = handleSuffixMapping(context, viewId);
        }
        else if(mapping.isPrefixMapping())
        {
            viewId = handlePrefixMapping(viewId,mapping.getPrefix());

            if(viewId != null)
            {
                // A viewId that is equals to the prefix mapping on servlet mode is
                // considered invalid, because jsp vdl will use RequestDispatcher and cause
                // a loop that ends in a exception. Note in portlet mode the view
                // could be encoded as a query param, so the viewId could be valid.
                if (viewId.equals(mapping.getPrefix()) &&
                    !ExternalContextUtils.isPortlet(context.getExternalContext()))
                {
                    throw new InvalidViewIdException();
                }

                return (checkResourceExists(context,viewId) ? viewId : null);
            }
        }
        else if (mapping.getUrlPattern().startsWith(viewId))
        {
            throw new InvalidViewIdException(viewId);
        }
        else
        {
            if(viewId != null)
            {
                return (checkResourceExists(context,viewId) ? viewId : null);
            }
        }

        return viewId;    // return null if no physical resource exists
    }

    public String calculateActionURL(FacesContext context, String viewId)
    {
        if (viewId == null || !viewId.startsWith("/"))
        {
            throw new IllegalArgumentException("ViewId must start with a '/': " + viewId);
        }

        FacesServletMapping mapping = getFacesServletMapping(context);
        ExternalContext externalContext = context.getExternalContext();
        String contextPath = externalContext.getRequestContextPath();
        //StringBuilder builder = new StringBuilder(contextPath);
        StringBuilder builder = SharedStringBuilder.get(context, VIEW_HANDLER_SUPPORT_SB);
        // If the context path is root, it is not necessary to append it, otherwise
        // and extra '/' will be set.
        if (contextPath != null && !(contextPath.length() == 1 && contextPath.charAt(0) == '/') )
        {
            builder.append(contextPath);
        }
        if (mapping != null)
        {
            if (mapping.isExtensionMapping())
            {
                //See JSF 2.0 section 7.5.2 
                String[] contextSuffixes = _initialized ? _contextSuffixes : getContextSuffix(context); 
                boolean founded = false;
                for (String contextSuffix : contextSuffixes)
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
                    else if(viewId.lastIndexOf(".") != -1 )
                    {
                        builder.append(viewId.substring(0,viewId.lastIndexOf(".")));
                        builder.append(contextSuffixes[0]);
                    }
                    else
                    {
                        builder.append(viewId);
                        builder.append(contextSuffixes[0]);
                    }
                }
            }
            else
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
        if (ViewProtectionUtils.isViewProtected(context, viewId))
        {
            int index = builder.indexOf("?");
            if (index >= 0)
            {
                builder.append("&");
            }
            else
            {
                builder.append("?");
            }
            builder.append(ResponseStateManager.NON_POSTBACK_VIEW_TOKEN_PARAM);
            builder.append("=");
            ResponseStateManager rsm = context.getRenderKit().getResponseStateManager();
            builder.append(rsm.getCryptographicallyStrongTokenFromSession(context));
        }
        
        String calculatedActionURL = builder.toString();
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Calculated actionURL: '" + calculatedActionURL + "' for viewId: '" + viewId + "'");
        }
        return calculatedActionURL;
    }

    /**
     * Read the web.xml file that is in the classpath and parse its internals to
     * figure out how the FacesServlet is mapped for the current webapp.
     */
    protected FacesServletMapping getFacesServletMapping(FacesContext context)
    {
        Map<Object, Object> attributes = context.getAttributes();

        // Has the mapping already been determined during this request?
        FacesServletMapping mapping = (FacesServletMapping) attributes.get(CACHED_SERVLET_MAPPING);
        if (mapping == null)
        {
            ExternalContext externalContext = context.getExternalContext();
            mapping = calculateFacesServletMapping(externalContext.getRequestServletPath(),
                    externalContext.getRequestPathInfo());

            attributes.put(CACHED_SERVLET_MAPPING, mapping);
        }
        return mapping;
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
    protected static FacesServletMapping calculateFacesServletMapping(
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

    protected String[] getContextSuffix(FacesContext context)
    {
        String defaultSuffix = context.getExternalContext().getInitParameter(ViewHandler.DEFAULT_SUFFIX_PARAM_NAME);
        if (defaultSuffix == null)
        {
            defaultSuffix = ViewHandler.DEFAULT_SUFFIX;
        }
        return StringUtils.splitShortString(defaultSuffix, ' ');
    }
    
    protected String getFaceletsContextSuffix(FacesContext context)
    {
        String defaultSuffix = context.getExternalContext().getInitParameter(ViewHandler.FACELETS_SUFFIX_PARAM_NAME);
        if (defaultSuffix == null)
        {
            defaultSuffix = ViewHandler.DEFAULT_FACELETS_SUFFIX;
        }
        return defaultSuffix;
    }
    
    
    
    protected String[] getFaceletsViewMappings(FacesContext context)
    {
        String faceletsViewMappings= context.getExternalContext().getInitParameter(
                ViewHandler.FACELETS_VIEW_MAPPINGS_PARAM_NAME);
        if(faceletsViewMappings == null)    //consider alias facelets.VIEW_MAPPINGS
        {
            faceletsViewMappings= context.getExternalContext().getInitParameter("facelets.VIEW_MAPPINGS");
        }
        
        return faceletsViewMappings == null ? null : StringUtils.splitShortString(faceletsViewMappings, ';');
    }

    /**
     * Return the normalized viewId according to the algorithm specified in 7.5.2 
     * by stripping off any number of occurrences of the prefix mapping from the viewId.
     * <p/>
     * For example, both /faces/view.xhtml and /faces/faces/faces/view.xhtml would both return view.xhtml
     * F 
     */
    protected String handlePrefixMapping(String viewId, String prefix)
    {
        // If prefix mapping (such as "/faces/*") is used for FacesServlet, 
        // normalize the viewId according to the following
        // algorithm, or its semantic equivalent, and return it.
               
        // Remove any number of occurrences of the prefix mapping from the viewId. 
        // For example, if the incoming value was /faces/faces/faces/view.xhtml 
        // the result would be simply view.xhtml.
        
        if ("".equals(prefix))
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
        
        //now delete any remaining leading '/'
        // TODO: CJH: I don't think this is correct, considering that getActionURL() expects everything to
        // start with '/', and in the suffix case we only mess with the suffix and leave leading
        // slashes alone.  Please review...
        /*if(uri.startsWith("/"))
        {
            uri = uri.substring(1);
        }*/
        
        return uri;
    }
    
    /**
     * Return the viewId with any non-standard suffix stripped off and replaced with
     * the default suffix configured for the specified context.
     * <p/>
     * For example, an input parameter of "/foo.jsf" may return "/foo.jsp".
     */
    protected String handleSuffixMapping(FacesContext context, String requestViewId)
    {
        String[] faceletsViewMappings = _initialized ? _faceletsViewMappings : getFaceletsViewMappings(context);
        String[] jspDefaultSuffixes = _initialized ? _contextSuffixes : getContextSuffix(context);
        
        int slashPos = requestViewId.lastIndexOf('/');
        int extensionPos = requestViewId.lastIndexOf('.');
        
        StringBuilder builder = SharedStringBuilder.get(context, VIEW_HANDLER_SUPPORT_SB);
        
        //Try to locate any resource that match with the expected id
        for (String defaultSuffix : jspDefaultSuffixes)
        {
            //StringBuilder builder = new StringBuilder(requestViewId);
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
            
            if( faceletsViewMappings != null && faceletsViewMappings.length > 0 )
            {
                for (String mapping : faceletsViewMappings)
                {
                    if(mapping.startsWith("/"))
                    {
                        continue;   //skip this entry, its a prefix mapping
                    }
                    if(mapping.equals(candidateViewId))
                    {
                        return candidateViewId;
                    }
                    if(mapping.startsWith(".")) //this is a wildcard entry
                    {
                        builder.setLength(0); //reset/reuse the builder object 
                        builder.append(candidateViewId); 
                        builder.replace(candidateViewId.lastIndexOf('.'), candidateViewId.length(), mapping);
                        String tempViewId = builder.toString();
                        if(checkResourceExists(context,tempViewId))
                        {
                            return tempViewId;
                        }
                    }
                }
            }

            // forced facelets mappings did not match or there were no entries in faceletsViewMappings array
            if(checkResourceExists(context,candidateViewId))
            {
                return candidateViewId;
            }
        
        }
        
        //jsp suffixes didn't match, try facelets suffix
        String faceletsDefaultSuffix = _initialized ? _faceletsContextSufix : this.getFaceletsContextSuffix(context);
        if (faceletsDefaultSuffix != null)
        {
            for (String defaultSuffix : jspDefaultSuffixes)
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
            //StringBuilder builder = new StringBuilder(requestViewId);
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
            if(checkResourceExists(context,candidateViewId))
            {
                return candidateViewId;
            }
        }

        // Otherwise, if a physical resource exists with the name requestViewId let that value be viewId.
        if(checkResourceExists(context,requestViewId))
        {
            return requestViewId;
        }
        
        //Otherwise return null.
        return null;
    }
    
    protected boolean checkResourceExists(FacesContext context, String viewId)
    {
        try
        {
            if (isCheckedViewIdCachingEnabled(context))
            {
                Boolean resourceExists = getCheckedViewIDMap(context).get(
                        viewId);
                if (resourceExists == null)
                {
                    ViewDeclarationLanguage vdl = context.getApplication().getViewHandler()
                            .getViewDeclarationLanguage(context, viewId);
                    if (vdl != null)
                    {
                        resourceExists = vdl.viewExists(context, viewId);
                    }
                    else
                    {
                        // Fallback to default strategy
                        resourceExists = context.getExternalContext().getResource(
                                viewId) != null;
                    }
                    getCheckedViewIDMap(context).put(viewId, resourceExists);
                }
                return resourceExists;
            }
            else
            {
                ViewDeclarationLanguage vdl = context.getApplication().getViewHandler()
                            .getViewDeclarationLanguage(context, viewId);
                if (vdl != null)
                {
                    if (vdl.viewExists(context, viewId))
                    {
                        return true;
                    }
                }
                else
                {
                    // Fallback to default strategy
                    if (context.getExternalContext().getResource(viewId) != null)
                    {
                        return true;
                    }
                }
            }
        }
        catch(MalformedURLException e)
        {
            //ignore and move on
        }     
        return false;
    }

    private ConcurrentLRUCache<String, Boolean> getCheckedViewIDMap(FacesContext context)
    {
        if (_checkedViewIdMap == null)
        {
            int maxSize = getViewIDCacheMaxSize(context);
            _checkedViewIdMap = new ConcurrentLRUCache<String, Boolean>((maxSize * 4 + 3) / 3, maxSize);
        }
        return _checkedViewIdMap;
    }

    private boolean isCheckedViewIdCachingEnabled(FacesContext context)
    {
        if (_checkedViewIdCacheEnabled == null)
        {
            // first, check if the ProjectStage is development and skip caching in this case
            if (context.isProjectStage(ProjectStage.Development))
            {
                _checkedViewIdCacheEnabled = Boolean.FALSE;
            }
            else
            {
                // in all ohter cases, make sure that the cache is not explicitly disabled via context param
                _checkedViewIdCacheEnabled = WebConfigParamUtils.getBooleanInitParameter(context.getExternalContext(),
                        CHECKED_VIEWID_CACHE_ENABLED_ATTRIBUTE,
                        CHECKED_VIEWID_CACHE_ENABLED_DEFAULT);
            }

            if (log.isLoggable(Level.FINE))
            {
                log.log(Level.FINE, "MyFaces ViewID Caching Enabled="
                        + _checkedViewIdCacheEnabled);
            }
        }
        return _checkedViewIdCacheEnabled;
    }

    private int getViewIDCacheMaxSize(FacesContext context)
    {
        ExternalContext externalContext = context.getExternalContext();

        return WebConfigParamUtils.getIntegerInitParameter(externalContext,
                CHECKED_VIEWID_CACHE_SIZE_ATTRIBUTE, CHECKED_VIEWID_CACHE_DEFAULT_SIZE);
    }
}
