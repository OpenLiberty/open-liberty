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
package javax.faces.application;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.faces.FacesException;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

/**
 * A ViewHandler manages the component-tree-creation and component-tree-rendering parts of a request lifecycle (ie
 * "create view", "restore view" and "render response").
 * <p>
 * A ViewHandler is responsible for generating the component tree when a new view is requested; see method "createView".
 * <p>
 * When the user performs a "postback", ie activates a UICommand component within a view, then the ViewHandler is
 * responsible for recreating a view tree identical to the one used previously to render that view; see method
 * "restoreView".
 * <p>
 * And the ViewHandler is also responsible for rendering the final output to be sent to the user by invoking the
 * rendering methods on components; see method "renderView".
 * <p>
 * This class also isolates callers from the underlying request/response system. In particular, this class does not
 * explicitly depend upon the javax.servlet apis. This allows JSF to be used on servers that do not implement the
 * servlet API (for example, plain CGI).
 * <p>
 * Examples:
 * <ul>
 * <li>A JSP ViewHandler exists for using "jsp" pages as the presentation technology. This class then works together
 * with a taghandler class and a jsp servlet class to implement the methods on this abstract class definition.
 * <li>A Facelets ViewHandler instead uses an xml file to define the components and non-component data that make up a
 * specific view.
 * </ul>
 * Of course there is no reason why the "template" needs to be a textual file. A view could be generated based on data
 * in a database, or many other mechanisms.
 * <p>
 * This class is expected to be invoked via the concrete implementation of {@link javax.faces.lifecycle.Lifecycle}.
 * <p>
 * For the official specification for this class, see <a
 * href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>.
 */
public abstract class ViewHandler
{
    public static final String CHARACTER_ENCODING_KEY = "javax.faces.request.charset";
    public static final String DEFAULT_FACELETS_SUFFIX = ".xhtml";
    public static final String DEFAULT_SUFFIX = ".xhtml .view.xml .jsp";
    
    /**
     * Indicate the default suffixes, separated by spaces to derive the default file URI 
     * used by JSF to create views and render pages. 
     */
    @JSFWebConfigParam(defaultValue=".xhtml .view.xml .jsp", since="1.1", group="viewhandler")
    public static final String DEFAULT_SUFFIX_PARAM_NAME = "javax.faces.DEFAULT_SUFFIX";
    
    /**
     * The default extension used to handle facelets pages.
     */
    @JSFWebConfigParam(defaultValue=".xhtml", since="2.0", group="viewhandler")
    public static final String FACELETS_SUFFIX_PARAM_NAME = "javax.faces.FACELETS_SUFFIX";
    
    /**
     * Set of extensions handled by facelets, separated by ';'.
     */
    @JSFWebConfigParam(since="2.0", group="viewhandler")
    public static final String FACELETS_VIEW_MAPPINGS_PARAM_NAME = "javax.faces.FACELETS_VIEW_MAPPINGS";
    // TODO: Notify EG on that last constant. Using the Facelets' param as well for backward compatiblity is 
    //       silly. If an application uses Facelets then they'll be using facelets.jar. Once they chose to 
    //       remove that JAR, they ought to be aware that some changes could be needed, like fixing their 
    //       context-param. -= Simon Lessard =-

    @JSFWebConfigParam(since="2.2")
    public static final java.lang.String DISABLE_FACELET_JSF_VIEWHANDLER_PARAM_NAME = 
        "DISABLE_FACELET_JSF_VIEWHANDLER";
    
    /**
     * Define the default buffer size value passed to ExternalContext.setResponseBufferResponse() and in a
     * servlet environment to HttpServletResponse.setBufferSize().
     */
    @JSFWebConfigParam(since = "2.0", alias = "facelets.BUFFER_SIZE", classType = "java.lang.Integer",
            tags = "performance", defaultValue="1024",
            desc = "Define the default buffer size value passed to ExternalContext.setResponseBufferResponse() and in "
                   + "a servlet environment to HttpServletResponse.setBufferSize()")
    public static final java.lang.String FACELETS_BUFFER_SIZE_PARAM_NAME = "javax.faces.FACELETS_BUFFER_SIZE";
    
    /**
     * Set of class names, separated by ';', implementing TagDecorator interface, used to transform
     * a view definition in a facelet abstract syntax tree, that is used later to generate a component tree.
     */
    @JSFWebConfigParam(since = "2.0", alias = "facelets.DECORATORS")
    public static final java.lang.String FACELETS_DECORATORS_PARAM_NAME = "javax.faces.FACELETS_DECORATORS";
    
    /**
     * Set of .taglib.xml files, separated by ';' that should be loaded by facelet engine.
     */
    @JSFWebConfigParam(since = "2.0",
            desc = "Set of .taglib.xml files, separated by ';' that should be loaded by facelet engine.",
            alias = "facelets.LIBRARIES")
    public static final java.lang.String FACELETS_LIBRARIES_PARAM_NAME = "javax.faces.FACELETS_LIBRARIES";
    
    /**
     * Define the period used to refresh the facelet abstract syntax tree from the view definition file. 
     *
     * <p>By default is infinite (no active).</p>
     */
    @JSFWebConfigParam(since = "2.0", defaultValue = "-1", alias = "facelets.REFRESH_PERIOD",
            classType = "java.lang.Long", tags = "performance")
    public static final java.lang.String FACELETS_REFRESH_PERIOD_PARAM_NAME = "javax.faces.FACELETS_REFRESH_PERIOD";

    /**
     * Skip comments found on a facelet file.
     */
    @JSFWebConfigParam(since = "2.0", alias = "facelets.SKIP_COMMENTS")
    public static final java.lang.String FACELETS_SKIP_COMMENTS_PARAM_NAME = "javax.faces.FACELETS_SKIP_COMMENTS";
    
    /**
     * @since JSF 1.2
     */
    public String calculateCharacterEncoding(FacesContext context)
    {
        String encoding = null;
        ExternalContext externalContext = context.getExternalContext();
        String contentType = externalContext.getRequestHeaderMap().get("Content-Type");
        int indexOf = contentType == null ? -1 : contentType.indexOf("charset");
        if (indexOf != -1)
        {
            String tempEnc = contentType.substring(indexOf); // charset=UTF-8
            encoding = tempEnc.substring(tempEnc.indexOf('=') + 1); // UTF-8
            if (encoding.length() == 0)
            {
                encoding = null;
            }
        }
        if (encoding == null)
        {
            boolean sessionAvailable = externalContext.getSession(false) != null;
            if (sessionAvailable)
            {
                Object sessionParam = externalContext.getSessionMap().get(CHARACTER_ENCODING_KEY);
                if (sessionParam != null)
                {
                    encoding = sessionParam.toString();
                }
            }
        }

        return encoding;
    }

    /**
     * Return the Locale object that should be used when rendering this view to the current user.
     * <p>
     * Some request protocols allow an application user to specify what locale they prefer the response to be in. For
     * example, HTTP requests can specify the "accept-language" header.
     * <p>
     * Method {@link javax.faces.application.Application#getSupportedLocales()} defines what locales this JSF
     * application is capable of supporting.
     * <p>
     * This method should match such sources of data up and return the Locale object that is the best choice for
     * rendering the current application to the current user.
     */
    public abstract Locale calculateLocale(FacesContext context);

    /**
     * Return the id of an available render-kit that should be used to map the JSF components into user presentation.
     * <p>
     * The render-kit selected (eg html, xhtml, pdf, xul, ...) may depend upon the user, properties associated with the
     * request, etc.
     */
    public abstract String calculateRenderKitId(FacesContext context);

    /**
     * Build a root node for a component tree.
     * <p>
     * When a request is received, this method is called if restoreView returns null, ie this is not a "postback". In
     * this case, a root node is created and then renderView is invoked. It is the responsibility of the renderView
     * method to build the full component tree (ie populate the UIViewRoot with descendant nodes).
     * <p>
     * This method is also invoked when navigation occurs from one view to another, where the viewId passed is the id of
     * the new view to be displayed. Again it is the responsibility of renderView to then populate the viewroot with
     * descendants.
     * <p>
     * The locale and renderKit settings are inherited from the current UIViewRoot that is configured before this method
     * is called. That means of course that they do NOT get set for GET requests, including navigation that has the
     * redirect flag set.
     */
    public abstract UIViewRoot createView(FacesContext context, String viewId);

    /**
     * @param context
     * @param input
     * @return
     * 
     * @since 2.0
     */
    public String deriveViewId(FacesContext context, String input)
    {
        //The default implementation of this method simply returns rawViewId unchanged.
        return input;
    }
    
    /**
     * 
     * @param context
     * @param rawViewId
     * @return
     * @since 2.1
     */
    public String deriveLogicalViewId(FacesContext context, String rawViewId)
    {
        return rawViewId;
    }

    /**
     * Returns a URL, suitable for encoding and rendering, that (if activated) will cause the JSF
     * request processing lifecycle for the specified viewId to be executed
     */
    public abstract String getActionURL(FacesContext context, String viewId);

    /**
     * Return a JSF action URL derived from the viewId argument that is suitable to be used as
     * the target of a link in a JSF response. Compiliant implementations must implement this method
     * as specified in section JSF.7.5.2. The default implementation simply calls through to
     * getActionURL(javax.faces.context.FacesContext, java.lang.String), passing the arguments context and viewId.
     * 
     * @param context
     * @param viewId
     * @param parameters
     * @param includeViewParams
     * @return
     * 
     * @since 2.0
     */
    public String getBookmarkableURL(FacesContext context, String viewId, Map<String, List<String>> parameters,
                                     boolean includeViewParams)
    {
        return getActionURL(context, viewId);
    }

    /**
     * Return the ViewDeclarationLanguage instance used for this ViewHandler  instance.
     * <P>
     * The default implementation of this method returns null.
     * 
     * @param context
     * @param viewId
     * @return
     * 
     * @since 2.0
     */
    public ViewDeclarationLanguage getViewDeclarationLanguage(FacesContext context, String viewId)
    {
        // TODO: In some places like RestoreViewExecutor, we are calling deriveViewId
        // TODO: after call restoreViewSupport.calculateViewId
        // Maybe this method should be called from here, because it is supposed that
        // calculateViewId "calculates the view id!"
        
        // here we return null to support pre jsf 2.0 ViewHandlers (e.g. com.sun.facelets.FaceletViewHandler),
        // because they don't provide any ViewDeclarationLanguage,
        // but in the default implementation (ViewHandlerImpl) we return vdlFactory.getViewDeclarationLanguage(viewId)
        return null;
    }

    /**
     * Return a JSF action URL derived from the viewId argument that is suitable to be used by
     * the NavigationHandler to issue a redirect request to the URL using a NonFaces request.
     * Compiliant implementations must implement this method as specified in section JSF.7.5.2.
     * The default implementation simply calls through to
     * getActionURL(javax.faces.context.FacesContext, java.lang.String), passing the arguments context and viewId.
     * 
     * @param context
     * @param viewId
     * @param parameters
     * @param includeViewParams
     * @return
     * 
     * @since 2.0
     */
    public String getRedirectURL(FacesContext context, String viewId, Map<String, List<String>> parameters,
                                 boolean includeViewParams)
    {
        return getActionURL(context, viewId);
    }

    /**
     * Returns a URL, suitable for encoding and rendering, that (if activated)
     * will retrieve the specified web application resource.
     */
    public abstract String getResourceURL(FacesContext context, String path);

    /**
     * Initialize the view for the request processing lifecycle.
     * <P>
     * This method must be called at the beginning of the Restore View Phase of the Request
     * Processing Lifecycle. It is responsible for performing any per-request initialization
     * necessary to the operation of the lifycecle.
     * <P>
     * The default implementation must perform the following actions. If
     * ExternalContext.getRequestCharacterEncoding() returns null, call
     * calculateCharacterEncoding(javax.faces.context.FacesContext) and pass the result,
     * if non-null, into the ExternalContext.setRequestCharacterEncoding(java.lang.String) method.
     * If ExternalContext.getRequestCharacterEncoding() returns non-null take no action.
     * 
     * @since JSF 1.2
     */
    public void initView(FacesContext context) throws FacesException
    {
        String encoding = this.calculateCharacterEncoding(context);
        if (encoding != null)
        {
            try
            {
                context.getExternalContext().setRequestCharacterEncoding(encoding);
            }
            catch (UnsupportedEncodingException uee)
            {
                throw new FacesException(uee);
            }
        }
    }

    /**
     *  Perform whatever actions are required to render the response view to the
     *  response object associated with the current FacesContext.
     *  <P>
     *  Otherwise, the default implementation must obtain a reference to the
     *  ViewDeclarationLanguage for the viewId of the argument viewToRender and call its
     *  ViewDeclarationLanguage.renderView(javax.faces.context.FacesContext, javax.faces.component.UIViewRoot)
     *  method, returning the result and not swallowing any exceptions thrown by that method.
     */
    public abstract void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException;

    /**
     * Perform whatever actions are required to restore the view associated with the
     * specified FacesContext and viewId. It may delegate to the restoreView of the
     * associated StateManager to do the actual work of restoring the view. If there
     * is no available state for the specified viewId, return null.
     * <P>
     * Otherwise, the default implementation must obtain a reference to the
     * ViewDeclarationLanguage for this viewId and call its
     * ViewDeclarationLanguage.restoreView(javax.faces.context.FacesContext, java.lang.String)
     * method, returning the result and not swallowing any exceptions thrown by that method.
     */
    public abstract UIViewRoot restoreView(FacesContext context, String viewId);

    /**
     * Take any appropriate action to either immediately write out the current state information
     * (by calling StateManager.writeState(javax.faces.context.FacesContext, java.lang.Object),
     * or noting where state information should later be written.
     * <P>
     * This method must do nothing if the current request is an Ajax request. When responding
     * to Ajax requests, the state is obtained by calling StateManager.getViewState(javax.faces.context.FacesContext)
     * and then written into the Ajax response during
     * final encoding (UIViewRoot.encodeEnd(javax.faces.context.FacesContext).
     */
    public abstract void writeState(FacesContext context) throws IOException;

    /**
     * @since 2.2
     * @param urlPattern 
     */
    public void addProtectedView(String urlPattern)
    {
    }
    
    /**
     * @since 2.2
     * @param urlPattern 
     */
    public boolean removeProtectedView(String urlPattern)
    {
        return false;
    }

    /**
     * @since 2.2
     * @return 
     */
    public Set<String> getProtectedViewsUnmodifiable()
    {
        Set<String> set = Collections.emptySet();
        return Collections.unmodifiableSet(set);
    }
    
    /**
     * Return a JSF URL that represents a websocket connection for the passed channel and channelToken
     * 
     * @since 2.3
     * @param context
     * @param channelAndToken
     * @return 
     */
    public abstract String getWebsocketURL(FacesContext context, String channelAndToken);

    /**
     * @since 2.3
     * @param facesContext
     * @param path
     * @param options
     * @return 
     */
    public Stream<java.lang.String> getViews(FacesContext facesContext, String path, ViewVisitOption... options)
    {
        return getViews(facesContext, path, Integer.MAX_VALUE, options);
    }
    
    
    /**
     * 
     * @since 2.3
     * @param facesContext
     * @param path
     * @param maxDepth
     * @param options
     * @return 
     */
    public Stream<java.lang.String> getViews(FacesContext facesContext, String path, 
            int maxDepth, ViewVisitOption... options)
    {
        return Stream.empty();
    }

}
