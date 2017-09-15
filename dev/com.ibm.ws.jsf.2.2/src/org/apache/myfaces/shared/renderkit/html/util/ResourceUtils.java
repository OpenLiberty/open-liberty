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
package org.apache.myfaces.shared.renderkit.html.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.faces.FacesWrapper;

import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.resource.ContractResource;

/**
 * @since 4.0.1
 */
public class ResourceUtils
{
    public final static String JAVAX_FACES_LIBRARY_NAME = "javax.faces";
    public final static String JSF_JS_RESOURCE_NAME = "jsf.js";

    public final static String MYFACES_JS_RESOURCE_NAME = "oamSubmit.js";
    public final static String MYFACES_JS_RESOURCE_NAME_UNCOMPRESSED = "oamSubmit-uncompressed.js";
    public final static String MYFACES_LIBRARY_NAME = "org.apache.myfaces";
    private final static String RENDERED_MYFACES_JS = "org.apache.myfaces.RENDERED_MYFACES_JS";

    public final static String JSF_MYFACES_JSFJS_MINIMAL = "minimal";
    public final static String JSF_MYFACES_JSFJS_MINIMAL_MODERN = "minimal-modern";
    public final static String JSF_MYFACES_JSFJS_NORMAL = "normal";
    
    public final static String JSF_UNCOMPRESSED_JS_RESOURCE_NAME = "jsf-uncompressed.js";
    public final static String JSF_MINIMAL_JS_RESOURCE_NAME = "jsf-minimal.js";
    public final static String JSF_MINIMAL_MODERN_JS_RESOURCE_NAME = "jsf-minimal-modern.js";
    
    public final static String JSF_MYFACES_JSFJS_I18N = "jsf-i18n.js";
    public final static String JSF_MYFACES_JSFJS_EXPERIMENTAL = "jsf-experimental.js";
    public final static String JSF_MYFACES_JSFJS_LEGACY = "jsf-legacy.js";

    private final static String RENDERED_STYLESHEET_RESOURCES_SET = 
        "org.apache.myfaces.RENDERED_STYLESHEET_RESOURCES_SET";
    private final static String RENDERED_SCRIPT_RESOURCES_SET = "org.apache.myfaces.RENDERED_SCRIPT_RESOURCES_SET";
    private final static String RENDERED_JSF_JS = "org.apache.myfaces.RENDERED_JSF_JS";
    public final static String HEAD_TARGET = "head";
    public final static String BODY_TARGET = "body";
    public final static String FORM_TARGET = "form";

    public static final String JAVAX_FACES_OUTPUT_COMPONENT_TYPE = "javax.faces.Output";
    public static final String JAVAX_FACES_TEXT_RENDERER_TYPE = "javax.faces.Text";
    public static final String DEFAULT_SCRIPT_RENDERER_TYPE = "javax.faces.resource.Script";
    public static final String DEFAULT_STYLESHEET_RENDERER_TYPE = "javax.faces.resource.Stylesheet";

    /**
     * Return a set of already rendered resources by this renderer on the current
     * request. 
     * 
     * @param facesContext
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Boolean> getRenderedStylesheetResources(FacesContext facesContext)
    {
        Map<String, Boolean> map = (Map<String, Boolean>) facesContext.getAttributes().get(
                RENDERED_STYLESHEET_RESOURCES_SET);
        if (map == null)
        {
            map = new HashMap<String, Boolean>();
            facesContext.getAttributes().put(RENDERED_STYLESHEET_RESOURCES_SET,map);
        }
        return map;
    }
    
    /**
     * Return a set of already rendered resources by this renderer on the current
     * request. 
     * 
     * @param facesContext
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Boolean> getRenderedScriptResources(FacesContext facesContext)
    {
        Map<String, Boolean> map = (Map<String, Boolean>) facesContext.getAttributes().get(
                RENDERED_SCRIPT_RESOURCES_SET);
        if (map == null)
        {
            map = new HashMap<String, Boolean>();
            facesContext.getAttributes().put(RENDERED_SCRIPT_RESOURCES_SET,map);
        }
        return map;
    }
    
    public static void markScriptAsRendered(FacesContext facesContext, String libraryName, String resourceName)
    {
        getRenderedScriptResources(facesContext).put(
                libraryName != null ? libraryName+'/'+resourceName : resourceName, Boolean.TRUE);
        if (JAVAX_FACES_LIBRARY_NAME.equals(libraryName) &&
            JSF_JS_RESOURCE_NAME.equals(resourceName))
        {
            // If we are calling this method, it is expected myfaces core is being used as runtime and note
            // oamSubmit script is included inside jsf.js, so mark this one too.
            getRenderedScriptResources(facesContext).put(
                    MYFACES_LIBRARY_NAME+'/'+MYFACES_JS_RESOURCE_NAME, Boolean.TRUE);
        }
    }
    
    public static void markStylesheetAsRendered(FacesContext facesContext, String libraryName, String resourceName)
    {
        getRenderedStylesheetResources(facesContext).put(
                libraryName != null ? libraryName+'/'+resourceName : resourceName, Boolean.TRUE);
    }
    
    public static boolean isRenderedScript(FacesContext facesContext, String libraryName, String resourceName)
    {
        return getRenderedScriptResources(facesContext).containsKey(
                libraryName != null ? libraryName+'/'+resourceName : resourceName);
    }
    
    public static boolean isRenderedStylesheet(FacesContext facesContext, String libraryName, String resourceName)
    {
        return getRenderedStylesheetResources(facesContext).containsKey(
                libraryName != null ? libraryName+'/'+resourceName : resourceName);
    }
    
    public static void writeScriptInline(FacesContext facesContext, ResponseWriter writer, String libraryName, 
            String resourceName) throws IOException
    {
        if (!ResourceUtils.isRenderedScript(facesContext, libraryName, resourceName))
        {
            if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isRiImplAvailable())
            {
                //Use more compatible way.
                UIComponent outputScript = facesContext.getApplication().
                    createComponent(facesContext, JAVAX_FACES_OUTPUT_COMPONENT_TYPE, DEFAULT_SCRIPT_RENDERER_TYPE);
                outputScript.getAttributes().put(JSFAttr.NAME_ATTR, resourceName);
                outputScript.getAttributes().put(JSFAttr.LIBRARY_ATTR, libraryName);
                outputScript.encodeAll(facesContext);
            }
            else
            {
                //Fast shortcut, don't create component instance and do what HtmlScriptRenderer do.
                Resource resource = facesContext.getApplication().getResourceHandler().createResource(
                        resourceName, libraryName);
                markScriptAsRendered(facesContext, libraryName, resourceName);
                writer.startElement(HTML.SCRIPT_ELEM, null);
                writer.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT , null);
                writer.writeURIAttribute(HTML.SRC_ATTR, resource.getRequestPath(), null);
                writer.endElement(HTML.SCRIPT_ELEM);
            }
        }
    }
    
    public static void renderDefaultJsfJsInlineIfNecessary(FacesContext facesContext, ResponseWriter writer) 
        throws IOException
    {
        if (facesContext.getAttributes().containsKey(RENDERED_JSF_JS))
        {
            return;
        }
        
        // Check first if we have lucky, we are using myfaces and the script has
        // been previously rendered
        if (isRenderedScript(facesContext, JAVAX_FACES_LIBRARY_NAME, JSF_JS_RESOURCE_NAME))
        {
            facesContext.getAttributes().put(RENDERED_JSF_JS, Boolean.TRUE);
            return;
        }

        // Check if this is an ajax request. If so, we don't need to include it, because that was
        // already done and in the worst case, jsf script was already loaded on the page.
        if (facesContext.getPartialViewContext() != null && 
                (facesContext.getPartialViewContext().isPartialRequest() ||
                 facesContext.getPartialViewContext().isAjaxRequest() )
            )
        {
            return;
        }
        

        // Here we have two cases:
        // 1. The standard script could be put in another target (body or form).
        // 2. RI is used so it is not used our set to check for rendered resources
        //    and we don't have access to that one.
        // 
        // Check if we have the resource registered on UIViewRoot "body" or "form" target
        // is not safe, because the page could not use h:body or h:form. 
        // Anyway, there is no clear reason why page authors could do the first one, 
        // but the second one could be.
        //
        // Finally we need to force render the script. If the script has been already rendered
        // (RI used and rendered as a h:outputScript not relocated), nothing will happen because the default
        // script renderer will check if the script has been rendered first.
        if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isRiImplAvailable())
        {
            //Use more compatible way.
            UIComponent outputScript = facesContext.getApplication().
                    createComponent(facesContext, JAVAX_FACES_OUTPUT_COMPONENT_TYPE, DEFAULT_SCRIPT_RENDERER_TYPE);
            outputScript.getAttributes().put(JSFAttr.NAME_ATTR, JSF_JS_RESOURCE_NAME);
            outputScript.getAttributes().put(JSFAttr.LIBRARY_ATTR, JAVAX_FACES_LIBRARY_NAME);
            outputScript.encodeAll(facesContext);
        }
        else
        {
            //Fast shortcut, don't create component instance and do what HtmlScriptRenderer do.
            Resource resource = facesContext.getApplication().getResourceHandler().createResource(
                    JSF_JS_RESOURCE_NAME, JAVAX_FACES_LIBRARY_NAME);
            markScriptAsRendered(facesContext, JAVAX_FACES_LIBRARY_NAME, JSF_JS_RESOURCE_NAME);
            writer.startElement(HTML.SCRIPT_ELEM, null);
            writer.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);
            writer.writeURIAttribute(HTML.SRC_ATTR, resource.getRequestPath(), null);
            writer.endElement(HTML.SCRIPT_ELEM);
        }

        //mark as rendered
        facesContext.getAttributes().put(RENDERED_JSF_JS, Boolean.TRUE);
        return;
    }

    public static void renderMyfacesJSInlineIfNecessary(FacesContext facesContext, ResponseWriter writer)
        throws IOException
    {
        if (facesContext.getAttributes().containsKey(RENDERED_MYFACES_JS))
        {
            return;
        }


        //we only are allowed to do this on partial requests
        //because on normal requests a static viewroot still could mean that a full page refresh is performed
        //only in a ppr case this means we have the script already loaded and parsed
        if (facesContext.getPartialViewContext() != null && 
                (facesContext.getPartialViewContext().isPartialRequest() ||
                 facesContext.getPartialViewContext().isAjaxRequest() )
            )
        {
            return;
        }
        // Check first if we have lucky, we are using myfaces and the script has
        // been previously rendered
        if (isRenderedScript(facesContext, MYFACES_LIBRARY_NAME, MYFACES_JS_RESOURCE_NAME))
        {
            facesContext.getAttributes().put(RENDERED_MYFACES_JS, Boolean.TRUE);
            return;
        }

        // Here we have two cases:
        // 1. The standard script could be put in another target (body or form).
        // 2. RI is used so it is not used our set to check for rendered resources
        //    and we don't have access to that one.
        //
        // Check if we have the resource registered on UIViewRoot "body" or "form" target
        // is not safe, because the page could not use h:body or h:form.
        // Anyway, there is no clear reason why page authors could do the first one,
        // but the second one could be.
        //
        // Finally we need to force render the script. If the script has been already rendered
        // (RI used and rendered as a h:outputScript not relocated), nothing will happen because the default
        // script renderer will check if the script has been rendered first.
        if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isRiImplAvailable())
        {
            //Use more compatible way.
            UIComponent outputScript = facesContext.getApplication().
                    createComponent(facesContext, JAVAX_FACES_OUTPUT_COMPONENT_TYPE, DEFAULT_SCRIPT_RENDERER_TYPE);
            outputScript.getAttributes().put(JSFAttr.NAME_ATTR, MYFACES_JS_RESOURCE_NAME);
            outputScript.getAttributes().put(JSFAttr.LIBRARY_ATTR, MYFACES_LIBRARY_NAME);
            outputScript.encodeAll(facesContext);
        }
        else
        {
            //Fast shortcut, don't create component instance and do what HtmlScriptRenderer do.
            Resource resource = facesContext.getApplication().getResourceHandler().createResource(
                    MYFACES_JS_RESOURCE_NAME, MYFACES_LIBRARY_NAME);
            markScriptAsRendered(facesContext, MYFACES_LIBRARY_NAME, MYFACES_JS_RESOURCE_NAME);
            writer.startElement(HTML.SCRIPT_ELEM, null);
            writer.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);
            writer.writeURIAttribute(HTML.SRC_ATTR, resource.getRequestPath(), null);
            writer.endElement(HTML.SCRIPT_ELEM);
        }

        //mark as rendered
        facesContext.getAttributes().put(RENDERED_MYFACES_JS, Boolean.TRUE);
        return;
    }

    public static String getContractName(Resource resource)
    {
        while (resource != null)
        {
            if (resource instanceof ContractResource)
            {
                return ((ContractResource)resource).getContractName();
            }
            else if (resource instanceof FacesWrapper)
            {
                resource = (Resource)((FacesWrapper)resource).getWrapped();
            }
            else
            {
                resource = null;
            }
        }
        return null;
    }
}
