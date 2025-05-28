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
package org.apache.myfaces.config.webparameters;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.StateManager;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.lifecycle.ClientWindow;
import jakarta.faces.webapp.FacesServlet;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.util.lang.StringUtils;
import org.apache.myfaces.view.facelets.ELExpressionCacheMode;

/**
 * Holds all configuration init parameters (from web.xml) that are independent
 * from the core implementation. The parameters in this class are available to
 * all shared, component and implementation classes.
 * See RuntimeConfig for configuration infos that come from the faces-config
 * files and are needed by the core implementation.
 *
 * MyfacesConfig is meant for components that implement some of the extended features
 * of MyFaces. Anyhow, using the MyFaces Faces implementation is no precondition for using
 * MyfacesConfig in custom components. Upon using another Faces implementation
 * (or omitting the extended init parameters) all config properties will simply have
 * their default values.
 */
//CHECKSTYLE:OFF
public class MyfacesConfig
{
    private static final String APPLICATION_MAP_PARAM_NAME = MyfacesConfig.class.getName();

    /**
     * Set the time in seconds that check for updates of web.xml and faces-config descriptors and 
     * refresh the configuration.
     * This param is valid only if project stage is not production. Set this param to 0 disable this feature.
     */
    @JSFWebConfigParam(defaultValue="2",since="1.1", classType="java.lang.Long")
    public static final String CONFIG_REFRESH_PERIOD = "org.apache.myfaces.CONFIG_REFRESH_PERIOD";
    private static final long CONFIG_REFRESH_PERIOD_DEFAULT = 2;

    /**
     * Define if the input field that should store the state (jakarta.faces.ViewState) should render 
     * id="jakarta.faces.ViewState".
     * 
     * Faces API 1.2 defines a "jakarta.faces.ViewState" client parameter, that must be rendered as both the "name"
     * and the "id" attribute of the hidden input that is rendered for the purpose of state saving
     * (see ResponseStateManager.VIEW_STATE_PARAM).
     * Actually this causes duplicate id attributes and thus invalid XHTML pages when multiple forms are rendered on
     * one page. With the org.apache.myfaces.RENDER_VIEWSTATE_ID context parameter you can tune this behaviour.
     * <br/>Set it to
     * <ul><li>true - to render Faces 1.2 compliant id attributes (that might cause invalid XHTML), or</li>
     * <li>false - to omit rendering of the id attribute (which is only needed for very special 
     * AJAX/Javascript components)</li></ul>
     * Default value is: true (for backwards compatibility and Faces 1.2 compliancy) 
     */
    @JSFWebConfigParam(defaultValue="true", expectedValues="true, false, on, off, yes, no",since="1.1", 
            ignoreUpperLowerCase=true, group="state")
    public static final String RENDER_VIEWSTATE_ID = "org.apache.myfaces.RENDER_VIEWSTATE_ID";
    private static final boolean RENDER_VIEWSTATE_ID_DEFAULT = true;

    /**
     * Use "&amp;amp;" entity instead a plain "&amp;" character within HTML.
     * <p>W3C recommends to use the "&amp;amp;" entity instead of a plain "&amp;" character within HTML.
     * This also applies to attribute values and thus to the "href" attribute of &lt;a&gt; elements as well.
     * Even more, when XHTML is used as output the usage of plain "&amp;" characters is forbidden and would lead to
     * invalid XML code.
     * Therefore, since version 1.1.6 MyFaces renders the correct "&amp;amp;" entity for links.</p>
     * <p>The init parameter
     * org.apache.myfaces.STRICT_XHTML_LINKS makes it possible to restore the old behaviour and to make MyFaces
     * "bug compatible" to the Sun RI which renders plain "&amp;" chars in links as well.</p>
     * <p>
     * See: <a href="http://www.w3.org/TR/html401/charset.html#h-5.3.2">HTML 4.01 Specification</a>
     * See: <a href="http://issues.apache.org/jira/browse/MYFACES-1774">Jira: MYFACES-1774</a>
     * </p>
     */
    @JSFWebConfigParam(defaultValue="true", expectedValues="true, false, on, off, yes, no",since="1.1.6", 
            ignoreUpperLowerCase=true, group="render")
    public static final String STRICT_XHTML_LINKS = "org.apache.myfaces.STRICT_XHTML_LINKS";
    private static final boolean STRICT_XHTML_LINKS_DEFAULT = true;
    
    /**
     * This param renders the clear javascript on button necessary only for
     * compatibility with hidden fields feature of myfaces. This is done 
     * because jsf ri does not render javascript on onclick method for button,
     * so myfaces should do this.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false, on, off, yes, no",since="1.2.3",
            ignoreUpperLowerCase=true, group="render")
    public static final String RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON = 
        "org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON";
    private static final boolean RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON_DEFAULT= false;

    /**
     * Indicate if the facelet associated to the view should be reapplied when the view is refreshed.
     *  Default mode is "auto".
     * 
     * <p>This param is only valid when partial state saving is on.
     * If this is set as true, the tag-handlers are always reapplied before render view, like in facelets 1.1.x, 
     * allowing c:if work correctly to "toggle" components based on a value changed on invoke application phase. 
     * If the param is set as "auto", the implementation check if c:if, c:forEach, 
     * c:choose and ui:include with src=ELExpression is used on the page and if that so, mark the view
     * to be refreshed.</p> 
     */
    @JSFWebConfigParam(since="2.0", defaultValue="auto", expectedValues="true,false,auto", tags="performance", 
            ignoreUpperLowerCase=true, group="state")
    public final static String REFRESH_TRANSIENT_BUILD_ON_PSS = 
        "org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS"; 
    private final static String REFRESH_TRANSIENT_BUILD_ON_PSS_DEFAULT = "auto";

    /**
     * Enable or disable a special mode that enable full state for parent components containing c:if, c:forEach, 
     * c:choose and ui:include with src=ELExpression. By default is disabled(false).
     * 
     * <p>This param is only valid when partial state saving is on.
     * If this is set as true, parent components containing  c:if, c:forEach, 
     * c:choose and ui:include with src=ELExpression are marked to be restored fully, so state
     * is preserved between request.</p>
     */
    @JSFWebConfigParam(since="2.0", defaultValue="false", expectedValues="true, false, on, off, yes, no", 
            tags="performance", ignoreUpperLowerCase=true, group="state")
    public final static String REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE = 
        "org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE";
    private final static boolean REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE_DEFAULT = false;
    
    /**
     * If set to <code>true</code>, tag library XML files and faces config XML files using schema 
     * will be validated during application start up
     */
    @JSFWebConfigParam(since="2.0", expectedValues="true, false, on, off, yes, no", ignoreUpperLowerCase=true)
    public final static String VALIDATE_XML = "org.apache.myfaces.VALIDATE_XML";
    private final static boolean VALIDATE_XML_DEFAULT = false;
    
    /**
     * Wrap content inside script with xml comment to prevent old browsers to display it. By default it is true. 
     */
    @JSFWebConfigParam(since="2.0.1", expectedValues="true, false, on, off, yes, no", defaultValue="false",
            ignoreUpperLowerCase=true, group="render")
    public final static String WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG = 
        "org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG";
    private final static boolean WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG_DEFAULT = false;
    
    /**
     * Enable/disable DebugPhaseListener feature, with provide useful information about ValueHolder 
     * variables (submittedValue, localValue, value).
     * Note evaluate those getters for each component could cause some unwanted side effects when 
     * using "access" type scopes like on MyFaces CODI.
     * This param only has effect when project stage is Development.     
     */
    @JSFWebConfigParam(since="2.0.8")
    public final static String DEBUG_PHASE_LISTENER = "org.apache.myfaces.DEBUG_PHASE_LISTENER";
    private final static boolean DEBUG_PHASE_LISTENER_DEFAULT = false;
    
    /**
     * Change default getType() behavior for composite component EL resolver, from return null 
     * (see Faces 2_0 spec section 5_6_2_2) to
     * use the metadata information added by composite:attribute, ensuring components working with 
     * chained EL expressions to find the
     * right type when a getType() is called over the source EL expression.
     * 
     * To ensure strict compatibility with the spec set this param to true (by default is false, 
     * so the change is enabled by default). 
     */
    @JSFWebConfigParam(since="2.0.10", expectedValues="true, false", defaultValue="false", group="EL")
    public final static String STRICT_JSF_2_CC_EL_RESOLVER = 
        "org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER";
    private final static boolean STRICT_JSF_2_CC_EL_RESOLVER_DEFAULT = false;
    
    /**
     * Define the default content type that the default ResponseWriter generates, when no match can be derived from
     * HTTP Accept Header.
     */
    @JSFWebConfigParam(since="2.0.11,2.1.5", expectedValues="text/html, application/xhtml+xml", 
            defaultValue="text/html", group="render")
    public final static String DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE = 
        "org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE";
    private final static String DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE_DEFAULT = "text/html";

    /**
     * Enable or disable a cache used to "remember" the generated facelets unique ids and reduce 
     * the impact on memory usage, only active if jakarta.faces.FACELETS_REFRESH_PERIOD is -1 (no refresh).
     */
    @JSFWebConfigParam(defaultValue = "true", since = "2.0.13, 2.1.7", expectedValues="true, false", 
            group="viewhandler", tags="performance",
            desc="Enable or disable a cache used to 'remember' the generated facelets unique ids " + 
                 "and reduce the impact over memory usage.")
    public static final String VIEW_UNIQUE_IDS_CACHE_ENABLED = 
        "org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED";
    private static final boolean VIEW_UNIQUE_IDS_CACHE_ENABLED_DEFAULT = true;
    
    /**
     * Set the size of the cache used to store strings generated using SectionUniqueIdCounter
     * for component ids. If this is set to 0, no cache is used. By default is set to 200.
     */
    @JSFWebConfigParam(defaultValue = "100", since = "2.0.13, 2.1.7",
            group="viewhandler", tags="performance")
    public static final String COMPONENT_UNIQUE_IDS_CACHE_SIZE =
        "org.apache.myfaces.COMPONENT_UNIQUE_IDS_CACHE_SIZE";
    private static final int COMPONENT_UNIQUE_IDS_CACHE_SIZE_DEFAULT = 200;

    /**
     * If this param is set to true, a check will be done in Restore View Phase to check
     * if the viewId exists or not and if it does not exists, a 404 response will be thrown.
     * 
     * This is applicable in cases where all the views in the application are generated by a 
     * ViewDeclarationLanguage implementation.
     */
    @JSFWebConfigParam(since = "2.1.13", defaultValue="false", expectedValues="true,false", 
            group="viewhandler")
    public static final String STRICT_JSF_2_VIEW_NOT_FOUND = 
            "org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND";
    private final static boolean STRICT_JSF_2_VIEW_NOT_FOUND_DEFAULT = false;

    @JSFWebConfigParam(defaultValue = "false", since = "2.2.0", expectedValues="true, false", group="render",
            tags="performance",
            desc="Enable or disable an early flush which allows to send e.g. the HTML-Head to the client " +
                    "while the rest gets rendered. It's a well known technique to reduce the time for loading a page.")
    public static final String EARLY_FLUSH_ENABLED =
        "org.apache.myfaces.EARLY_FLUSH_ENABLED";
    private static final boolean EARLY_FLUSH_ENABLED_DEFAULT = false;
    
    /**
     * This param makes components like c:set, ui:param and templating components like ui:decorate,
     * ui:composition and ui:include to behave like the ones provided originally in facelets 1_1_x. 
     * See MYFACES-3810 for details.
     */
    @JSFWebConfigParam(since = "2.2.0", defaultValue="false", expectedValues="true,false", 
            group="viewhandler")
    public static final String STRICT_JSF_2_FACELETS_COMPATIBILITY = 
            "org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY";
    private final static boolean STRICT_JSF_2_FACELETS_COMPATIBILITY_DEFAULT = false;    
    
    /**
     * This param makes h:form component to render the view state and other hidden fields
     * at the beginning of the form. This also includes component resources with target="form",
     * but it does not include legacy 1.1 myfaces specific hidden field adition.
     */
    @JSFWebConfigParam(since = "2.2.4", defaultValue = "false", expectedValues = "true,false",
            group="render")
    public static final String RENDER_FORM_VIEW_STATE_AT_BEGIN =
            "org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN";
    private final static boolean RENDER_FORM_VIEW_STATE_AT_BEGIN_DEFAULT = false;
    
    /**
     * Defines whether flash scope is disabled, preventing add the Flash cookie to the response. 
     * 
     * <p>This is useful for applications that does not require to use flash scope, and instead uses other scopes.</p>
     */
    @JSFWebConfigParam(defaultValue="false",since="2.0.5")
    public static final String FLASH_SCOPE_DISABLED = "org.apache.myfaces.FLASH_SCOPE_DISABLED";
    private static final boolean FLASH_SCOPE_DISABLED_DEFAULT = false;
    
    /**
     * Defines the amount (default = 20) of the latest views are stored in session.
     * 
     * <p>Only applicable if state saving method is "server" (= default).
     * </p>
     * 
     */
    @JSFWebConfigParam(defaultValue="20",since="1.1", classType="java.lang.Integer", group="state", tags="performance")
    public static final String NUMBER_OF_VIEWS_IN_SESSION = "org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION";
    /**
     * Default value for <code>org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION</code> context parameter.
     */
    public static final int NUMBER_OF_VIEWS_IN_SESSION_DEFAULT = 20;    

    /**
     * Indicates the amount of views (default is not active) that should be stored in session between sequential
     * POST or POST-REDIRECT-GET if org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION is true.
     * 
     * <p>Only applicable if state saving method is "server" (= default). For example, if this param has value = 2 and 
     * in your custom webapp there is a form that is clicked 3 times, only 2 views
     * will be stored and the third one (the one stored the first time) will be
     * removed from session, even if the view can
     * store more sessions org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION.
     * This feature becomes useful for multi-window applications.
     * where without this feature a window can swallow all view slots so
     * the other ones will throw ViewExpiredException.</p>
     */
    @JSFWebConfigParam(since="2.0.6", classType="java.lang.Integer", group="state", tags="performance", 
            defaultValue = "4")
    public static final String NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION
            = "org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION";
    public static final Integer NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT = 4;
    
    /**
     * Indicate the max number of flash tokens stored into session. It is only active when 
     * jakarta.faces.CLIENT_WINDOW_MODE is enabled and jakarta.faces.STATE_SAVING_METHOD is set
     * to "server". Each flash token is associated to one client window id at
     * the same time, so this param is related to the limit of active client windows per session. 
     * By default is the same number as in 
     * (org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION / 
     * org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION) + 1 = 6.
     */
    @JSFWebConfigParam(since="2.2.6", group="state", tags="performance")
    public static final String NUMBER_OF_FLASH_TOKENS_IN_SESSION = 
            "org.apache.myfaces.NUMBER_OF_FLASH_TOKENS_IN_SESSION";

    /**
     * This parameter specifies whether or not the ImportHandler will be supported
     */
    @JSFWebConfigParam(since="2.2.9", defaultValue="false", expectedValues="true,false", group="EL")
    public static final String SUPPORT_EL_3_IMPORT_HANDLER = "org.apache.myfaces.SUPPORT_EL_3_IMPORT_HANDLER";
    private final static boolean SUPPORT_EL_3_IMPORT_HANDLER_DEFAULT = false;

    /**
     * This parameter specifies whether or not the Origin header app path should be checked 
     */
    @JSFWebConfigParam(since="2.3", defaultValue="false", expectedValues="true,false")
    public static final String STRICT_JSF_2_ORIGIN_HEADER_APP_PATH = 
            "org.apache.myfaces.STRICT_JSF_2_ORIGIN_HEADER_APP_PATH";
    private final static boolean STRICT_JSF_2_ORIGIN_HEADER_APP_PATH_DEFAULT = false;

    /**
     * Allow slash in the library name of a Resource. 
     */
    @JSFWebConfigParam(since="2.1.6, 2.0.12", defaultValue="false", 
            expectedValues="true, false", group="resources")
    public static final String STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME = 
            "org.apache.myfaces.STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME";
    private static final boolean STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME_DEFAULT = false;
    
    /**
     * Define the default buffer size that is used between Resource.getInputStream() and 
     * httpServletResponse.getOutputStream() when rendering resources using the default
     * ResourceHandler.
     */
    @JSFWebConfigParam(since="2.1.10, 2.0.16", defaultValue="2048", group="resources")
    public static final String RESOURCE_BUFFER_SIZE = "org.apache.myfaces.RESOURCE_BUFFER_SIZE";
    private static final int RESOURCE_BUFFER_SIZE_DEFAULT = 2048;
    
    /**
     * Validate if the managed beans and navigations rules are correct.
     * 
     * <p>For example, it checks if the managed bean classes really exists, or if the 
     * navigation rules points to existing view files.</p>
     */
    @JSFWebConfigParam(since="2.0", defaultValue="false", expectedValues="true, false")
    public static final String VALIDATE = "org.apache.myfaces.VALIDATE";
    
    /**
     * Defines if CDI should be used for annotation scanning to improve the startup performance.
     */
    @JSFWebConfigParam(since="2.2.9", tags = "performance", defaultValue = "false")
    public static final String USE_CDI_FOR_ANNOTATION_SCANNING
            = "org.apache.myfaces.annotation.USE_CDI_FOR_ANNOTATION_SCANNING";
    private static final boolean USE_CDI_FOR_ANNOTATION_SCANNING_DEFAULT = false;
    
    
    /**
     * Controls the size of the cache used to check if a resource exists or not. 
     * 
     * <p>See org.apache.myfaces.RESOURCE_HANDLER_CACHE_ENABLED for details.</p>
     */
    @JSFWebConfigParam(defaultValue = "500", since = "2.0.2", group="resources", 
            classType="java.lang.Integer", tags="performance")
    public static final String RESOURCE_HANDLER_CACHE_SIZE = 
        "org.apache.myfaces.RESOURCE_HANDLER_CACHE_SIZE";
    private static final int RESOURCE_HANDLER_CACHE_SIZE_DEFAULT = 500;

    /**
     * Enable or disable the cache used to "remember" if a resource handled by 
     * the default ResourceHandler exists or not.
     * 
     */
    @JSFWebConfigParam(defaultValue = "true", since = "2.0.2", group="resources", 
            expectedValues="true,false", tags="performance")
    public static final String RESOURCE_HANDLER_CACHE_ENABLED = 
        "org.apache.myfaces.RESOURCE_HANDLER_CACHE_ENABLED";
    private static final boolean RESOURCE_HANDLER_CACHE_ENABLED_DEFAULT = true;
    
    /**
     * Servlet context init parameter which defines which packages to scan
     * for beans, separated by commas.
     */
    @JSFWebConfigParam(since="2.0")
    public static final String SCAN_PACKAGES = "org.apache.myfaces.annotation.SCAN_PACKAGES";
 
    /**
     * Indicates the port used for websocket connections.
     */
    @JSFWebConfigParam(since = "2.3")
    public static final java.lang.String WEBSOCKET_ENDPOINT_PORT = "jakarta.faces.WEBSOCKET_ENDPOINT_PORT";

    @JSFWebConfigParam(defaultValue = "300000")
    public static final String WEBSOCKET_MAX_IDLE_TIMEOUT = "org.apache.myfaces.WEBSOCKET_MAX_IDLE_TIMEOUT";
    private static final long WEBSOCKET_MAX_IDLE_TIMEOUT_DEFAULT = 300000L;
    
    /**
     * Defines how to generate the csrf session token.
     */
    @JSFWebConfigParam(since="2.2.0", expectedValues="secureRandom, random", 
            defaultValue="secureRandom", group="state")
    public static final String RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN
            = "org.apache.myfaces.RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN";

    public static final String RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN_SECURE_RANDOM = "secureRandom";
    public static final String RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN_RANDOM = "random";
    private static final String RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN_DEFAULT = RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN_SECURE_RANDOM;

    /**
     * Define the time in minutes where the view state is valid when
     * client side state saving is used. By default it is set to 0
     * (infinite).
     */
    @JSFWebConfigParam(since="2.1.9, 2.0.15", defaultValue="0", group="state")
    public static final String CLIENT_VIEW_STATE_TIMEOUT = 
            "org.apache.myfaces.CLIENT_VIEW_STATE_TIMEOUT";
    private static final Long CLIENT_VIEW_STATE_TIMEOUT_DEFAULT = 0L;
   
    
    /**
     * Adds a random key to the generated view state session token.
     */
    @JSFWebConfigParam(since="2.1.9, 2.0.15", expectedValues="secureRandom, random", 
            defaultValue="secureRandom", group="state")
    public static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN
            = "org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN";
    public static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM = "secureRandom";
    public static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_RANDOM = "random";
    private static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_DEFAULT = RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM;
    
    /**
     * Set the default length of the random key added to the view state session token.
     * By default is 8. 
     */
    @JSFWebConfigParam(since="2.1.9, 2.0.15", defaultValue="8", group="state")
    public static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH
            = "org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH";
    private static final int RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH_DEFAULT = 8;
    
    /**
     * Sets the random class to initialize the secure random id generator. 
     * By default it uses java.security.SecureRandom
     */
    @JSFWebConfigParam(since="2.1.9, 2.0.15", group="state")
    public static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_CLASS
            = "org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_CLASS";
    
    /**
     * Sets the random provider to initialize the secure random id generator.
     */
    @JSFWebConfigParam(since="2.1.9, 2.0.15", group="state")
    public static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_PROVIDER
            = "org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_PROVIDER";
    
    /**
     * Sets the random algorithm to initialize the secure random id generator. 
     * By default is SHA1PRNG
     */
    @JSFWebConfigParam(since="2.1.9, 2.0.15", defaultValue="SHA1PRNG", group="state")
    public static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM
            = "org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM";
    private static final String RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM_DEFAULT = "SHA1PRNG";
    
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM = "secureRandom";
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_RANDOM = "random";
    
    /**
     * Defines how to generate the csrf session token.
     */
    @JSFWebConfigParam(since="2.2.0", expectedValues="secureRandom, random", defaultValue="secureRandom", group="state")
    public static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN
            = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN";
    private static final String RANDOM_KEY_IN_CSRF_SESSION_TOKEN_DEFAULT = RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM;
    
    /**
     * Indicates that the serialized state will be compressed before it is written to the session. By default true.
     * 
     * Only applicable if state saving method is "server" (= default) and if
     * <code>jakarta.faces.SERIALIZE_SERVER_STATE</code> is <code>true</code>.
     * If <code>true</code> (default) the serialized state will be compressed before it is written to the session.
     * If <code>false</code> the state will not be compressed.
     */
    @JSFWebConfigParam(defaultValue="true",since="1.1", expectedValues="true,false", group="state", tags="performance")
    public static final String COMPRESS_STATE_IN_SESSION = "org.apache.myfaces.COMPRESS_STATE_IN_SESSION";
    private static final boolean COMPRESS_STATE_IN_SESSION_DEFAULT = true;
    
    /**
     * Allow use flash scope to keep track of the views used in session and the previous ones,
     * so server side state saving can delete old views even if POST-REDIRECT-GET pattern is used.
     * 
     * <p>
     * Only applicable if state saving method is "server" (= default).
     * The default value is false.</p>
     */
    @JSFWebConfigParam(since="2.0.6", defaultValue="false", expectedValues="true, false", group="state")
    public static final String USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION
            = "org.apache.myfaces.USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION";
    private static final boolean USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION_DEFAULT = false;
    
    /**
     * Add autocomplete="..." to the view state hidden field. Set to "one-time-code" by default. (MYFACES-4721)
     */
    @JSFWebConfigParam(since="2.2.8, 2.1.18, 2.0.24", expectedValues="true, false, one-time-code", 
           defaultValue="one-time-code", group="state")
    public static final String AUTOCOMPLETE_OFF_VIEW_STATE = 
            "org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE";
    private static final String AUTOCOMPLETE_OFF_VIEW_STATE_DEFAULT = "one-time-code";
    
    /**
     * Set the max time in miliseconds set on the "Expires" header for a resource rendered by 
     * the default ResourceHandler.
     * (default to one week in miliseconds or 604800000) 
     */
    @JSFWebConfigParam(since="2.0", defaultValue="604800000", group="resources", tags="performance")
    public static final String RESOURCE_MAX_TIME_EXPIRES = "org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES";
    private static final long RESOURCE_MAX_TIME_EXPIRES_DEFAULT = 604800000L;
    

    /**
     * Indicate if the classes associated to components, converters, validators or behaviors
     * should be loaded as soon as they are added to the current application instance or instead
     * loaded in a lazy way.
     */
    @JSFWebConfigParam(defaultValue="true",since="2.0",tags="performance")
    public static final String LAZY_LOAD_CONFIG_OBJECTS = "org.apache.myfaces.LAZY_LOAD_CONFIG_OBJECTS";
    private static final boolean LAZY_LOAD_CONFIG_OBJECTS_DEFAULT = true;

    
    /**
     * Define a custom comparator class used to sort the ELResolvers.
     * 
     * <p>This is useful when it is necessary to put an ELResolver on top of other resolvers. Note set
     * this param override the default ordering described by Faces spec section 5. 
     * </p>
     */
    @JSFWebConfigParam(since = "1.2.10, 2.0.2", group="EL",
            desc = "The Class of an Comparator&lt;ELResolver&gt; implementation.")
    public static final String EL_RESOLVER_COMPARATOR = "org.apache.myfaces.EL_RESOLVER_COMPARATOR";
    
    @JSFWebConfigParam(since = "2.1.0", group="EL",
        desc="The Class of an java.util.function.Predicate&lt;ELResolver&gt; implementation."
             + "If used and returns false for a ELResolver instance, such resolver will not be installed in "
             + "ELResolvers chain. Use with caution - can break functionality defined in Faces specification "
             + "'ELResolver Instances Provided by Faces'")
    public static final String EL_RESOLVER_PREDICATE = "org.apache.myfaces.EL_RESOLVER_PREDICATE";

    @JSFWebConfigParam(defaultValue = "500", since = "2.0.2", group="viewhandler", tags="performance", 
            classType="java.lang.Integer",
            desc="Controls the size of the viewId related caches: " + 
                    "VIEWID_EXISTS_CACHE_ENABLED, VIEWID_PROTECTED_CACHE_ENABLED, VIEWID_DERIVE_CACHE_ENABLED")
    public static final String VIEWID_CACHE_SIZE = "org.apache.myfaces.VIEWID_CACHE_SIZE";
    private static final int VIEWID_CACHE_SIZE_DEFAULT = 500;

    @JSFWebConfigParam(defaultValue = "true", since = "2.0.2", expectedValues="true, false", group="viewhandler", 
            tags="performance",
            desc="Enable or disable the cache used to 'remember' if a view exists or not and reduce the impact " +
                 "of sucesive calls to ExternalContext.getResource().")
    public static final String VIEWID_EXISTS_CACHE_ENABLED = "org.apache.myfaces.VIEWID_EXISTS_CACHE_ENABLED";
    private static final boolean VIEWID_EXISTS_CACHE_ENABLED_DEFAULT = true;
    

    @JSFWebConfigParam(defaultValue = "true", since = "2.3-next", expectedValues="true, false", group="viewhandler", 
            tags="performance",
            desc="Enable or disable the cache used to 'remember' if a view is protected or not.")
    public static final String VIEWID_PROTECTED_CACHE_ENABLED = "org.apache.myfaces.VIEWID_PROTECTED_CACHE_ENABLED";
    private static final boolean VIEWID_PROTECTED_CACHE_ENABLED_DEFAULT = true;
    
    @JSFWebConfigParam(defaultValue = "true", since = "2.3-next", expectedValues="true, false", group="viewhandler", 
            tags="performance",
            desc="Enable or disable the cache used to 'remember' the derived viewId from the rawViewId.")
    public static final String VIEWID_DERIVE_CACHE_ENABLED = "org.apache.myfaces.VIEWID_DERIVE_CACHE_ENABLED";
    private static final boolean VIEWID_DERIVE_CACHE_ENABLED_DEFAULT = true;

    /**
     * Enforce f:validateBean to be called first before any Faces validator.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false", since = "2.2.10", group="validation")
    public final static String BEAN_BEFORE_JSF_VALIDATION
            = "org.apache.myfaces.validator.BEAN_BEFORE_JSF_VALIDATION";
    private final static boolean BEAN_BEFORE_JSF_VALIDATION_DEFAULT = false;

    /**
     * comma delimited list of plugin classes which can be hooked into myfaces
     */
    @JSFWebConfigParam(since = "2.0")
    public static final String FACES_INIT_PLUGINS = "org.apache.myfaces.FACES_INIT_PLUGINS";
    
    /**
     * If the flag is true, the algoritm skip jar scanning for faces-config files to check if the current
     * application requires FacesServlet to be added dynamically (servlet spec 3). This param can be set using 
     * a system property with the same name too.
     */
    @JSFWebConfigParam(since="2.2.10", expectedValues = "true, false", defaultValue = "false", 
            tags = "performance")
    public static final String INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN = 
            "org.apache.myfaces.INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN";
    private static final boolean INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN_DEFAULT = false;
    
    /**
     * If this param is set to true, the check for faces servlet mapping is not done 
     */
    @JSFWebConfigParam(since="2.0.3", defaultValue="false")
    public static final String INITIALIZE_ALWAYS_STANDALONE = "org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE";

    /**
     * This parameter specifies the ExpressionFactory implementation to use.
     */
    @JSFWebConfigParam(since="1.2.7", group="EL")
    public static final String EXPRESSION_FACTORY = "org.apache.myfaces.EXPRESSION_FACTORY";
   
    /**
     * Define how duplicate ids are checked when ProjectStage is Production, by default (auto) it only check ids of
     * components that does not encapsulate markup (like facelets UILeaf).
     *  
     * <ul>
     * <li>true: check all ids, including ids for components that are transient and encapsulate markup.</li>
     * <li>auto: (default) check ids of components that does not encapsulate markup (like facelets UILeaf). 
     * Note ids of UILeaf instances are generated by facelets vdl, start with "j_id", are never rendered 
     * into the response and UILeaf instances are never used as a target for listeners, so in practice 
     * there is no need to check such ids. This reduce the overhead associated with generate client ids.</li>
     * <li>false: do not do any check when ProjectStage is Production</li>
     * </ul>
     * <p> According to specification, identifiers must be unique within the scope of the nearest ancestor to 
     * the component that is a naming container.</p>
     */
    @JSFWebConfigParam(since="2.0.12, 2.1.6", defaultValue="auto", expectedValues="true, auto, false",
                       group="state", tags="performance")
    public static final String CHECK_ID_PRODUCTION_MODE
            = "org.apache.myfaces.CHECK_ID_PRODUCTION_MODE";
    private static final String CHECK_ID_PRODUCTION_MODE_DEFAULT = "auto";
    public static final String CHECK_ID_PRODUCTION_MODE_TRUE = "true";
    public static final String CHECK_ID_PRODUCTION_MODE_AUTO = "auto";
    
    @JSFWebConfigParam(since = "2.1", defaultValue = "false", expectedValues = "true, false", tags = "performance")
    public final static String MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW
            = "org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW";
    private static final boolean MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW_DEFAULT = false;
    
    /**
     * Indicates if expressions generated by facelets should be cached or not.
     * Default is noCache. There there are four modes:
     * 
     * <ul>
     * <li>alwaysRecompile (since 2.1.12): Only does not cache when the expression contains
     * a variable resolved using VariableMapper</li>
     * <li>always: Only does not cache when expressions are inside user tags or the
     * expression contains a variable resolved using VariableMapper</li>
     * <li>allowCset: Like always, but does not allow cache when ui:param
     * was used on the current template context</li>
     * <li>strict: Like allowCset, but does not allow cache when c:set with
     * var and value properties only is used on the current page context</li>
     * <li>noCache: All expression are created each time the view is built</li>
     * </ul>
     * 
     */
    @JSFWebConfigParam(since="2.0.8", defaultValue="noCache",
                       expectedValues="noCache, strict, allowCset, always, alwaysRecompile",
                       group="EL", tags="performance")
    public static final String CACHE_EL_EXPRESSIONS = "org.apache.myfaces.CACHE_EL_EXPRESSIONS";
    private static final String CACHE_EL_EXPRESSIONS_DEFAULT = ELExpressionCacheMode.noCache.name();
    
    /**
     * Wrap exception caused by calls to EL expressions, so information like
     * the location, expression string and tag name can be retrieved by
     * the ExceptionHandler implementation and used to output meaningful information about itself.
     * 
     * <p>Note in some cases this will wrap the original jakarta.el.ELException,
     * so the information will not be on the stack trace unless ExceptionHandler
     * retrieve checking if the exception implements ContextAware interface and calling getWrapped() method.
     * </p>
     * 
     */
    @JSFWebConfigParam(since="2.0.9, 2.1.3" , defaultValue="true", expectedValues="true, false")
    public static final String WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE
            = "org.apache.myfaces.WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE";
    private static final boolean WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE_DEFAULT = true;
    
    /**
     * Defines if the last-modified should be cached of the resources when the ProjectStage is Production.
     * If the cache is disabled, each last-modified request will read the last-modified from the file.
     */
    @JSFWebConfigParam(since="2.3-next" , defaultValue="true", expectedValues="true, false")
    public static final String RESOURCE_CACHE_LAST_MODIFIED
            = "org.apache.myfaces.RESOURCE_CACHE_LAST_MODIFIED";
    private static final boolean RESOURCE_CACHE_LAST_MODIFIED_DEFAULT = true;

    /**
     * Indicate if INFO logging of all the web config params should be done before initialize the webapp. 
     * <p>
     * If is set to "dev-only" mode, web config params are only logged in "Development" mode. 
     * If is set to "true", web config params are only logged in "Production" and "Development" mode. 
     * If is set to "false" mode, info logging does not occur in any mode.
     * </p> 
     */
    @JSFWebConfigParam(expectedValues="true, dev-only, false", defaultValue="dev-only")
    public static final String LOG_WEB_CONTEXT_PARAMS = "org.apache.myfaces.LOG_WEB_CONTEXT_PARAMS";
    private static final String LOG_WEB_CONTEXT_PARAMS_DEFAULT = "dev-only";
    

    public static final boolean AUTOMATIC_EXTENSIONLESS_MAPPING_DEFAULT = false;
    
    /**
     * Indicate the class implementing FacesInitializer interface that will
     * be used to setup MyFaces Core contexts.
     * <p>This is used when some custom task must be done specifically when
     * a myfaces web context is initialized or destroyed, or when MyFaces should
     * be initialized in some custom environment. 
     * </p>
     */
    @JSFWebConfigParam(since = "2.0.1", desc = "Class name of a custom FacesInitializer implementation.")
    public static final String FACES_INITIALIZER = "org.apache.myfaces.FACES_INITIALIZER";
    

    /**
     * Define the class implementing InjectionProvider interface to handle dependendy injection,
     * PostConstruct and PreDestroy callbacks.
     * 
     * <p>This also can be configured using a SPI entry (/META-INF/services/...).</p>
     */
    @JSFWebConfigParam(name="org.apache.myfaces.spi.InjectionProvider", since="2.2")
    public static final String INJECTION_PROVIDER = InjectionProvider.class.getName();
    
    @JSFWebConfigParam(name="org.apache.myfaces.WEBSOCKET_MAX_CONNECTIONS", since="2.3")
    public static final String WEBSOCKET_MAX_CONNECTIONS = "org.apache.myfaces.WEBSOCKET_MAX_CONNECTIONS";
    public static final Integer WEBSOCKET_MAX_CONNECTIONS_DEFAULT = 5000;
    
    
    /**
     * Defines if the clientbehavior scripts are passed as string or function to the faces.util.chain.
     * "As string" is actually the default behavior of both MyFaces (until 2.3-next) and Mojarra.
     * "As function" is quite usefull for CSP as no string needs to be evaluated as function.
     * 
     * Our faces.util.chain supports both of course.
     */
    @JSFWebConfigParam(name="org.apache.myfaces.RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING", since="2.3-next", defaultValue = "false")
    public static final String RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING = "org.apache.myfaces.RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING";
    public static final boolean RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING_DEFAULT = false;
    
    /**
     * Defines if a session should be created (if one does not exist) before response rendering.
     * When this parameter is set to true, a session will be created even when client side state 
     * saving or stateless views are used, which can lead to unintended resource consumption.
     * When this parameter is set to false, a session will only be created before response 
     * rendering if a view is not transient and server side state saving is in use.     
     */
    @JSFWebConfigParam(since="2.3.6", defaultValue="false", expectedValues="true,false")
    public static final String ALWAYS_FORCE_SESSION_CREATION = 
            "org.apache.myfaces.ALWAYS_FORCE_SESSION_CREATION";
    protected final static boolean ALWAYS_FORCE_SESSION_CREATION_DEFAULT = false;
    
    /**
     * Defines the {@link java.util.ResourceBundle.Control} to use for all
     * {@link java.util.ResourceBundle#getBundle(java.lang.String)} calls.
     */
    @JSFWebConfigParam(since="2.3-next")
    public static final String RESOURCE_BUNDLE_CONTROL = 
            "org.apache.myfaces.RESOURCE_BUNDLE_CONTROL";
    
    /**
     * Defines if ELResolvers should be traced on each request and logged. Only active on Development ProjectStage.
     */
    @JSFWebConfigParam(name="org.apache.myfaces.EL_RESOLVER_TRACING", since="4.0", defaultValue = "false")
    public static final String EL_RESOLVER_TRACING = "org.apache.myfaces.EL_RESOLVER_TRACING";
    public static final boolean EL_RESOLVER_TRACING_DEFAULT = false;

    @JSFWebConfigParam(name="org.apache.myfaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING", since="4.0.3, 4.1.1, 5.0")
    public static final String EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING =
            "org.apache.myfaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING";

    // we need it, applicationImpl not ready probably
    private ProjectStage projectStage = ProjectStage.Production;
    private boolean strictJsf2AllowSlashLibraryName;
    private long configRefreshPeriod = CONFIG_REFRESH_PERIOD_DEFAULT;
    private boolean renderViewStateId = RENDER_VIEWSTATE_ID_DEFAULT;
    private boolean strictXhtmlLinks = STRICT_XHTML_LINKS_DEFAULT;
    private boolean renderClearJavascriptOnButton = RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON_DEFAULT;
    private boolean refreshTransientBuildOnPSS = true;
    private boolean refreshTransientBuildOnPSSAuto = true;
    private boolean refreshTransientBuildOnPSSPreserveState = REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE_DEFAULT;
    private boolean validateXML = VALIDATE_XML_DEFAULT;
    private boolean wrapScriptContentWithXmlCommentTag = WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG_DEFAULT;
    private boolean debugPhaseListenerEnabled = DEBUG_PHASE_LISTENER_DEFAULT;
    private boolean strictJsf2CCELResolver = STRICT_JSF_2_CC_EL_RESOLVER_DEFAULT;
    private String defaultResponseWriterContentTypeMode = DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE_DEFAULT;
    private boolean viewUniqueIdsCacheEnabled = VIEW_UNIQUE_IDS_CACHE_ENABLED_DEFAULT;
    private int componentUniqueIdsCacheSize = COMPONENT_UNIQUE_IDS_CACHE_SIZE_DEFAULT;
    private boolean strictJsf2ViewNotFound = STRICT_JSF_2_VIEW_NOT_FOUND_DEFAULT;
    private boolean earlyFlushEnabled = EARLY_FLUSH_ENABLED_DEFAULT;
    private boolean strictJsf2FaceletsCompatibility = STRICT_JSF_2_FACELETS_COMPATIBILITY_DEFAULT;
    private boolean renderFormViewStateAtBegin = RENDER_FORM_VIEW_STATE_AT_BEGIN_DEFAULT;
    private boolean flashScopeDisabled = FLASH_SCOPE_DISABLED_DEFAULT;
    private Integer numberOfViewsInSession = NUMBER_OF_VIEWS_IN_SESSION_DEFAULT;
    private Integer numberOfSequentialViewsInSession = NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT;
    private Integer numberOfFlashTokensInSession;
    private Integer numberOfClientWindows;
    private boolean supportEL3ImportHandler = SUPPORT_EL_3_IMPORT_HANDLER_DEFAULT;
    private boolean strictJsf2OriginHeaderAppPath = STRICT_JSF_2_ORIGIN_HEADER_APP_PATH_DEFAULT;
    private int resourceBufferSize = RESOURCE_BUFFER_SIZE_DEFAULT;
    private boolean useCdiForAnnotationScanning = USE_CDI_FOR_ANNOTATION_SCANNING_DEFAULT;
    private boolean resourceHandlerCacheEnabled = RESOURCE_HANDLER_CACHE_ENABLED_DEFAULT;
    private int resourceHandlerCacheSize = RESOURCE_HANDLER_CACHE_SIZE_DEFAULT;
    private String scanPackages;
    private long websocketMaxIdleTimeout = WEBSOCKET_MAX_IDLE_TIMEOUT_DEFAULT;
    private Integer websocketEndpointPort;
    private String randomKeyInWebsocketSessionToken = RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN_DEFAULT;
    private long clientViewStateTimeout = CLIENT_VIEW_STATE_TIMEOUT_DEFAULT;
    private String randomKeyInViewStateSessionToken = RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_DEFAULT;
    private int randomKeyInViewStateSessionTokenLength = RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH_DEFAULT;
    private String randomKeyInViewStateSessionTokenSecureRandomClass;
    private String randomKeyInViewStateSessionTokenSecureRandomProvider;
    private String randomKeyInViewStateSessionTokenSecureRandomAlgorithm
            = RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM_DEFAULT;
    private String randomKeyInCsrfSessionToken = RANDOM_KEY_IN_CSRF_SESSION_TOKEN_DEFAULT;
    private boolean serializeStateInSession = false;
    private boolean compressStateInSession = COMPRESS_STATE_IN_SESSION_DEFAULT;
    private boolean useFlashScopePurgeViewsInSession = USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION_DEFAULT;
    private String autocompleteOffViewState = AUTOCOMPLETE_OFF_VIEW_STATE_DEFAULT;
    private long resourceMaxTimeExpires = RESOURCE_MAX_TIME_EXPIRES_DEFAULT;
    private boolean lazyLoadConfigObjects = LAZY_LOAD_CONFIG_OBJECTS_DEFAULT;
    private String elResolverComparator;
    private String elResolverPredicate;
    private boolean viewIdExistsCacheEnabled = VIEWID_EXISTS_CACHE_ENABLED_DEFAULT;
    private boolean viewIdProtectedCacheEnabled = VIEWID_PROTECTED_CACHE_ENABLED_DEFAULT;
    private boolean viewIdDeriveCacheEnabled = VIEWID_DERIVE_CACHE_ENABLED_DEFAULT;
    private int viewIdCacheSize = VIEWID_CACHE_SIZE_DEFAULT;
    private boolean beanBeforeJsfValidation = BEAN_BEFORE_JSF_VALIDATION_DEFAULT;
    private String facesInitPlugins;
    private boolean initializeSkipJarFacesConfigScan = INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN_DEFAULT;
    private String expressionFactory;
    private String checkIdProductionMode = CHECK_ID_PRODUCTION_MODE_DEFAULT;
    private boolean partialStateSaving = true;
    private String[] fullStateSavingViewIds;
    private int faceletsBufferSize = 1024;
    private boolean markInitialStateWhenApplyBuildView = MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW_DEFAULT;
    private String[] viewSuffix = new String[] { ViewHandler.DEFAULT_SUFFIX };
    private String[] faceletsViewMappings = new String[] {};
    private String faceletsViewSuffix = ViewHandler.DEFAULT_FACELETS_SUFFIX;
    private ELExpressionCacheMode elExpressionCacheMode;
    private boolean wrapTagExceptionsAsContextAware = WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE_DEFAULT;
    private boolean resourceCacheLastModified = RESOURCE_CACHE_LAST_MODIFIED_DEFAULT;
    private boolean logWebContextParams = false;
    private int websocketMaxConnections = WEBSOCKET_MAX_CONNECTIONS_DEFAULT;
    private boolean renderClientBehaviorScriptsAsString = RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING_DEFAULT;
    private boolean alwaysForceSessionCreation = ALWAYS_FORCE_SESSION_CREATION_DEFAULT;
    private ResourceBundle.Control resourceBundleControl;
    private boolean automaticExtensionlessMapping = AUTOMATIC_EXTENSIONLESS_MAPPING_DEFAULT;
    private boolean elResolverTracing = EL_RESOLVER_TRACING_DEFAULT;
    private long faceletsRefreshPeriod = -1;
    private List<String> exceptionTypesToIgnoreInLogging = new ArrayList<>();
    
    private static final boolean MYFACES_IMPL_AVAILABLE;
    private static final boolean RI_IMPL_AVAILABLE;

    static
    {
        boolean myfacesImplAvailable;
        try
        {
            ClassUtils.classForName("org.apache.myfaces.application.ApplicationImpl");
            myfacesImplAvailable = true;
        }
        catch (ClassNotFoundException e)
        {
            myfacesImplAvailable = false;
        }
        MYFACES_IMPL_AVAILABLE = myfacesImplAvailable;
        
        boolean riImplAvailable;
        try
        {
            ClassUtils.classForName("com.sun.faces.application.ApplicationImpl");
            riImplAvailable = true;
        }
        catch (ClassNotFoundException e)
        {
            riImplAvailable = false;
        }
        RI_IMPL_AVAILABLE = riImplAvailable;
    }

    public static MyfacesConfig getCurrentInstance()
    {
        return getCurrentInstance(FacesContext.getCurrentInstance().getExternalContext());
    }
    
    public static MyfacesConfig getCurrentInstance(FacesContext facesContext)
    {
        return getCurrentInstance(facesContext.getExternalContext());
    }
    
    public static MyfacesConfig getCurrentInstance(ExternalContext extCtx)
    {
        MyfacesConfig config = (MyfacesConfig) extCtx.getApplicationMap().get(APPLICATION_MAP_PARAM_NAME);
        if (config == null)
        {
            config = createAndInitializeMyFacesConfig(extCtx);
            extCtx.getApplicationMap().put(APPLICATION_MAP_PARAM_NAME, config);
        }

        return config;
    }
    
    public MyfacesConfig()
    {
        numberOfFlashTokensInSession = (NUMBER_OF_VIEWS_IN_SESSION_DEFAULT
                / NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT) + 1;
    }

    private static MyfacesConfig createAndInitializeMyFacesConfig(ExternalContext extCtx)
    {
        MyfacesConfig cfg = new MyfacesConfig();

        try
        {
            cfg.projectStage = ProjectStage.valueOf(getString(extCtx, ProjectStage.PROJECT_STAGE_PARAM_NAME, null));
        }
        catch (Exception e)
        {
            // ignore, it's logged in ApplicationImpl
        }
        if (cfg.projectStage == null)
        {
            cfg.projectStage = ProjectStage.Production;
        }

        cfg.renderClearJavascriptOnButton = getBoolean(extCtx, RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON,
                RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON_DEFAULT);

        cfg.renderViewStateId = getBoolean(extCtx, RENDER_VIEWSTATE_ID,
                RENDER_VIEWSTATE_ID_DEFAULT);
        
        cfg.strictXhtmlLinks = getBoolean(extCtx, STRICT_XHTML_LINKS,
                STRICT_XHTML_LINKS_DEFAULT);
        
        cfg.configRefreshPeriod = getLong(extCtx, CONFIG_REFRESH_PERIOD,
                CONFIG_REFRESH_PERIOD_DEFAULT);

        String refreshTransientBuildOnPSS = getString(extCtx, REFRESH_TRANSIENT_BUILD_ON_PSS, 
                REFRESH_TRANSIENT_BUILD_ON_PSS_DEFAULT);
        if (refreshTransientBuildOnPSS == null)
        {
            cfg.refreshTransientBuildOnPSS = false;
            cfg.refreshTransientBuildOnPSSAuto = false;
        }
        else if ("auto".equalsIgnoreCase(refreshTransientBuildOnPSS))
        {
            cfg.refreshTransientBuildOnPSS = true;
            cfg.refreshTransientBuildOnPSSAuto = true;
        }
        else if (refreshTransientBuildOnPSS.equalsIgnoreCase("true") || 
                refreshTransientBuildOnPSS.equalsIgnoreCase("on") || 
                refreshTransientBuildOnPSS.equalsIgnoreCase("yes"))
        {
            cfg.refreshTransientBuildOnPSS = true;
            cfg.refreshTransientBuildOnPSSAuto = false;
        }
        else
        {
            cfg.refreshTransientBuildOnPSS = false;
            cfg.refreshTransientBuildOnPSSAuto = false;
        }
        
        cfg.refreshTransientBuildOnPSSPreserveState = getBoolean(extCtx, REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE,
                REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE_DEFAULT);
        
        cfg.validateXML = getBoolean(extCtx, VALIDATE_XML, 
                VALIDATE_XML_DEFAULT);
        
        cfg.wrapScriptContentWithXmlCommentTag = getBoolean(extCtx, WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG, 
                WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG_DEFAULT);
        
        cfg.debugPhaseListenerEnabled = getBoolean(extCtx, DEBUG_PHASE_LISTENER,
                DEBUG_PHASE_LISTENER_DEFAULT);
                
        cfg.strictJsf2CCELResolver = getBoolean(extCtx, STRICT_JSF_2_CC_EL_RESOLVER,
                STRICT_JSF_2_CC_EL_RESOLVER_DEFAULT);
        
        cfg.defaultResponseWriterContentTypeMode = getString(extCtx, DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE,
                DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE_DEFAULT);

        cfg.viewUniqueIdsCacheEnabled = getBoolean(extCtx, VIEW_UNIQUE_IDS_CACHE_ENABLED,
                VIEW_UNIQUE_IDS_CACHE_ENABLED_DEFAULT);

        cfg.componentUniqueIdsCacheSize = getInt(extCtx, COMPONENT_UNIQUE_IDS_CACHE_SIZE, 
                COMPONENT_UNIQUE_IDS_CACHE_SIZE_DEFAULT);

        cfg.strictJsf2ViewNotFound = getBoolean(extCtx, STRICT_JSF_2_VIEW_NOT_FOUND,
                STRICT_JSF_2_VIEW_NOT_FOUND_DEFAULT);
        
        cfg.earlyFlushEnabled = getBoolean(extCtx, EARLY_FLUSH_ENABLED,
                EARLY_FLUSH_ENABLED_DEFAULT);
        if (cfg.projectStage != ProjectStage.Production)
        {
            cfg.earlyFlushEnabled = false;
        }

        cfg.strictJsf2FaceletsCompatibility = getBoolean(extCtx, STRICT_JSF_2_FACELETS_COMPATIBILITY, 
                STRICT_JSF_2_FACELETS_COMPATIBILITY_DEFAULT);
        
        cfg.renderFormViewStateAtBegin = getBoolean(extCtx, RENDER_FORM_VIEW_STATE_AT_BEGIN,
                RENDER_FORM_VIEW_STATE_AT_BEGIN_DEFAULT);
        
        cfg.flashScopeDisabled = getBoolean(extCtx, FLASH_SCOPE_DISABLED,
                FLASH_SCOPE_DISABLED_DEFAULT);
        
        cfg.strictJsf2AllowSlashLibraryName = getBoolean(extCtx, STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME,
                STRICT_JSF_2_ALLOW_SLASH_LIBRARY_NAME_DEFAULT);
        
        try
        {
            cfg.numberOfSequentialViewsInSession = getInt(extCtx, NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION,
                    NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT);
            if (cfg.numberOfSequentialViewsInSession == null
                    || cfg.numberOfSequentialViewsInSession < 0)
            {
                Logger.getLogger(MyfacesConfig.class.getName()).severe(
                        "Configured value for " + NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION
                          + " is not valid, must be an value >= 0, using default value ("
                          + NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT);
                cfg.numberOfSequentialViewsInSession = NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT;
            }
        }
        catch (Throwable e)
        {
            Logger.getLogger(MyfacesConfig.class.getName()).log(Level.SEVERE, "Error determining the value for "
                   + NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION
                   + ", expected an integer value > 0, using default value ("
                   + NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT + "): " + e.getMessage(), e);
        }        
        try
        {
            cfg.numberOfViewsInSession = getInt(extCtx, NUMBER_OF_VIEWS_IN_SESSION,
                    NUMBER_OF_VIEWS_IN_SESSION_DEFAULT);
            if (cfg.numberOfViewsInSession == null || cfg.numberOfViewsInSession <= 0)
            {
                Logger.getLogger(MyfacesConfig.class.getName()).severe(
                        "Configured value for " + NUMBER_OF_VIEWS_IN_SESSION
                          + " is not valid, must be an value > 0, using default value ("
                          + NUMBER_OF_VIEWS_IN_SESSION_DEFAULT);
                cfg.numberOfViewsInSession = NUMBER_OF_VIEWS_IN_SESSION_DEFAULT;
            }
        }
        catch (Throwable e)
        {
            Logger.getLogger(MyfacesConfig.class.getName()).log(Level.SEVERE, "Error determining the value for "
                   + NUMBER_OF_VIEWS_IN_SESSION
                   + ", expected an integer value > 0, using default value ("
                   + NUMBER_OF_VIEWS_IN_SESSION_DEFAULT + "): " + e.getMessage(), e);
        }

        int numberOfFlashTokensInSessionDefault;
        if (cfg.numberOfSequentialViewsInSession != null && cfg.numberOfSequentialViewsInSession > 0)
        {
            numberOfFlashTokensInSessionDefault = (cfg.numberOfViewsInSession
                    / cfg.numberOfSequentialViewsInSession) + 1;
        }
        else
        {
            numberOfFlashTokensInSessionDefault = cfg.numberOfViewsInSession + 1;
        }

        cfg.numberOfFlashTokensInSession = getInt(extCtx, NUMBER_OF_FLASH_TOKENS_IN_SESSION,
                numberOfFlashTokensInSessionDefault);

        cfg.numberOfClientWindows = getInt(extCtx,
                ClientWindow.NUMBER_OF_CLIENT_WINDOWS_PARAM_NAME, 
                10);
                        
        cfg.supportEL3ImportHandler = getBoolean(extCtx, SUPPORT_EL_3_IMPORT_HANDLER, 
                SUPPORT_EL_3_IMPORT_HANDLER_DEFAULT); 

        cfg.strictJsf2OriginHeaderAppPath = getBoolean(extCtx, STRICT_JSF_2_ORIGIN_HEADER_APP_PATH, 
                STRICT_JSF_2_ORIGIN_HEADER_APP_PATH_DEFAULT);

        cfg.resourceBufferSize = getInt(extCtx, RESOURCE_BUFFER_SIZE, 
                RESOURCE_BUFFER_SIZE_DEFAULT);
        
        cfg.useCdiForAnnotationScanning = getBoolean(extCtx, USE_CDI_FOR_ANNOTATION_SCANNING,
                USE_CDI_FOR_ANNOTATION_SCANNING_DEFAULT);
        
        cfg.resourceHandlerCacheEnabled = getBoolean(extCtx, RESOURCE_HANDLER_CACHE_ENABLED,
                RESOURCE_HANDLER_CACHE_ENABLED_DEFAULT);
        if (cfg.projectStage != ProjectStage.Production)
        {
            cfg.resourceHandlerCacheEnabled = false;
        }
        
        cfg.resourceHandlerCacheSize = getInt(extCtx, RESOURCE_HANDLER_CACHE_SIZE,
                RESOURCE_HANDLER_CACHE_SIZE_DEFAULT);
        
        cfg.scanPackages = getString(extCtx, SCAN_PACKAGES,
                null);
        
        String websocketEndpointPort = extCtx.getInitParameter(WEBSOCKET_ENDPOINT_PORT);
        if (websocketEndpointPort != null && !websocketEndpointPort.isEmpty())
        {
            cfg.websocketEndpointPort = Integer.valueOf(websocketEndpointPort);
        }
        
        cfg.websocketMaxIdleTimeout = getLong(extCtx, WEBSOCKET_MAX_IDLE_TIMEOUT,
                WEBSOCKET_MAX_IDLE_TIMEOUT_DEFAULT);
        
        cfg.randomKeyInWebsocketSessionToken = getString(extCtx, RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN,
                RANDOM_KEY_IN_WEBSOCKET_SESSION_TOKEN_DEFAULT);
        
        cfg.clientViewStateTimeout = getLong(extCtx, CLIENT_VIEW_STATE_TIMEOUT,
                CLIENT_VIEW_STATE_TIMEOUT_DEFAULT);
        if (cfg.clientViewStateTimeout < 0L)
        {
            cfg.clientViewStateTimeout = 0L;
        }
        
        cfg.randomKeyInViewStateSessionToken = getString(extCtx, RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN,
                RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_DEFAULT);
        
        cfg.randomKeyInViewStateSessionTokenLength = getInt(extCtx, RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH,
                RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_LENGTH_DEFAULT);
        
        cfg.randomKeyInViewStateSessionTokenSecureRandomClass = getString(extCtx,
                RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_CLASS,
                null);
        
        cfg.randomKeyInViewStateSessionTokenSecureRandomProvider = getString(extCtx,
                RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_PROVIDER,
                null);
        
        cfg.randomKeyInViewStateSessionTokenSecureRandomAlgorithm = getString(extCtx,
                RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM,
                RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM_DEFAULT);
        
        cfg.randomKeyInCsrfSessionToken = getString(extCtx, RANDOM_KEY_IN_CSRF_SESSION_TOKEN,
                RANDOM_KEY_IN_CSRF_SESSION_TOKEN_DEFAULT);
        
        cfg.serializeStateInSession = getBoolean(extCtx, StateManager.SERIALIZE_SERVER_STATE_PARAM_NAME,
                false);
        
        cfg.compressStateInSession = getBoolean(extCtx, COMPRESS_STATE_IN_SESSION,
                COMPRESS_STATE_IN_SESSION_DEFAULT);
        
        cfg.useFlashScopePurgeViewsInSession = getBoolean(extCtx, USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION,
                USE_FLASH_SCOPE_PURGE_VIEWS_IN_SESSION_DEFAULT);

        cfg.autocompleteOffViewState = getString(extCtx, AUTOCOMPLETE_OFF_VIEW_STATE,
                AUTOCOMPLETE_OFF_VIEW_STATE_DEFAULT);
        
        cfg.resourceMaxTimeExpires = getLong(extCtx, RESOURCE_MAX_TIME_EXPIRES,
                RESOURCE_MAX_TIME_EXPIRES_DEFAULT);
        
        cfg.lazyLoadConfigObjects = getBoolean(extCtx, LAZY_LOAD_CONFIG_OBJECTS,
                LAZY_LOAD_CONFIG_OBJECTS_DEFAULT);
        
        cfg.elResolverComparator = getString(extCtx, EL_RESOLVER_COMPARATOR,
                null);
        
        cfg.elResolverPredicate = getString(extCtx, EL_RESOLVER_PREDICATE,
                null);
        
        cfg.viewIdExistsCacheEnabled = getBoolean(extCtx, VIEWID_EXISTS_CACHE_ENABLED,
                VIEWID_EXISTS_CACHE_ENABLED_DEFAULT);
        if (cfg.projectStage == ProjectStage.Development)
        {
            cfg.viewIdExistsCacheEnabled = false;
        }
        
        cfg.viewIdProtectedCacheEnabled = getBoolean(extCtx, VIEWID_PROTECTED_CACHE_ENABLED,
                VIEWID_PROTECTED_CACHE_ENABLED_DEFAULT);
        if (cfg.projectStage == ProjectStage.Development)
        {
            cfg.viewIdProtectedCacheEnabled = false;
        }

        cfg.viewIdDeriveCacheEnabled = getBoolean(extCtx, VIEWID_DERIVE_CACHE_ENABLED,
                VIEWID_DERIVE_CACHE_ENABLED_DEFAULT);
        if (cfg.projectStage == ProjectStage.Development)
        {
            cfg.viewIdDeriveCacheEnabled = false;
        }
        
        cfg.viewIdCacheSize = getInt(extCtx, VIEWID_CACHE_SIZE,
                VIEWID_CACHE_SIZE_DEFAULT);

        cfg.beanBeforeJsfValidation = getBoolean(extCtx, BEAN_BEFORE_JSF_VALIDATION,
                BEAN_BEFORE_JSF_VALIDATION_DEFAULT);
        
        cfg.facesInitPlugins = getString(extCtx, FACES_INIT_PLUGINS,
                null);
        
        cfg.initializeSkipJarFacesConfigScan = getBoolean(extCtx, INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN,
                INITIALIZE_SKIP_JAR_FACES_CONFIG_SCAN_DEFAULT);
        
        cfg.expressionFactory = getString(extCtx, EXPRESSION_FACTORY,
                null);
        
        cfg.checkIdProductionMode = getString(extCtx, CHECK_ID_PRODUCTION_MODE,
                CHECK_ID_PRODUCTION_MODE_DEFAULT);
        
        // Per spec section 11.1.3, the default value for the partial state saving feature needs
        // to be true if 2.0, false otherwise.
        // lets ignore this on 3.x
        cfg.partialStateSaving = getBoolean(extCtx, StateManager.PARTIAL_STATE_SAVING_PARAM_NAME,
                true);
        
        if (cfg.partialStateSaving == false)
        {
            Logger.getLogger(MyfacesConfig.class.getName()).warning(
                "The configuration 'jakarta.faces.PARTIAL_STATE_SAVING' is deprecated " +
                    "as of Faces 4.1");
        }

        cfg.fullStateSavingViewIds = StringUtils.splitShortString(
                getString(extCtx, StateManager.FULL_STATE_SAVING_VIEW_IDS_PARAM_NAME, null),
                ',');

        if (cfg.fullStateSavingViewIds.length > 0)
        {
            Logger.getLogger(MyfacesConfig.class.getName()).warning(
                "The configuration 'jakarta.faces.FULL_STATE_SAVING_VIEW_IDS' is deprecated " +
                    "as of Faces 4.1");
        }
        
        cfg.faceletsBufferSize = getInt(extCtx, ViewHandler.FACELETS_BUFFER_SIZE_PARAM_NAME,
                1024);
        
        cfg.markInitialStateWhenApplyBuildView = getBoolean(extCtx, MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW,
                MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW_DEFAULT);

        cfg.viewSuffix = StringUtils.splitShortString(
                getString(extCtx, ViewHandler.DEFAULT_SUFFIX_PARAM_NAME, ViewHandler.DEFAULT_SUFFIX),
                ' ');
        
        cfg.faceletsViewMappings = StringUtils.splitShortString(
                getString(extCtx, ViewHandler.FACELETS_VIEW_MAPPINGS_PARAM_NAME, null),
                ';');
        
        cfg.faceletsViewSuffix = getString(extCtx, ViewHandler.FACELETS_SUFFIX_PARAM_NAME,
                ViewHandler.DEFAULT_FACELETS_SUFFIX);
        
        String elExpressionCacheMode = getString(extCtx, CACHE_EL_EXPRESSIONS,
                CACHE_EL_EXPRESSIONS_DEFAULT);    
        cfg.elExpressionCacheMode = Enum.valueOf(ELExpressionCacheMode.class, elExpressionCacheMode); 
 
        cfg.wrapTagExceptionsAsContextAware = getBoolean(extCtx, WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE,
                WRAP_TAG_EXCEPTIONS_AS_CONTEXT_AWARE_DEFAULT);
        
        cfg.resourceCacheLastModified = getBoolean(extCtx, RESOURCE_CACHE_LAST_MODIFIED,
                RESOURCE_CACHE_LAST_MODIFIED_DEFAULT);
        if (cfg.projectStage == ProjectStage.Development)
        {
            cfg.resourceCacheLastModified = false;
        }
        
        String logWebContextParams = getString(extCtx, LOG_WEB_CONTEXT_PARAMS,
                LOG_WEB_CONTEXT_PARAMS_DEFAULT);    

        switch(logWebContextParams)
        {
            case "dev-only": 
                if(cfg.projectStage == ProjectStage.Development)
                {
                    cfg.logWebContextParams = true;
                }
                else
                {
                    cfg.logWebContextParams = false;
                }
                break;
            case "true":
                if(cfg.projectStage == ProjectStage.Production || cfg.projectStage == ProjectStage.Development)
                {
                    cfg.logWebContextParams = true;
                }
                else
                {
                    cfg.logWebContextParams = false;
                }
                break;
            default: 
                cfg.logWebContextParams = false;
        }
        
        cfg.websocketMaxConnections = getInt(extCtx, WEBSOCKET_MAX_CONNECTIONS,
                WEBSOCKET_MAX_CONNECTIONS_DEFAULT);

        cfg.renderClientBehaviorScriptsAsString = getBoolean(extCtx, RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING,
                RENDER_CLIENTBEHAVIOR_SCRIPTS_AS_STRING_DEFAULT);

        cfg.alwaysForceSessionCreation = getBoolean(extCtx, ALWAYS_FORCE_SESSION_CREATION,
                ALWAYS_FORCE_SESSION_CREATION_DEFAULT);
        
        String resourceBundleControl = getString(extCtx, RESOURCE_BUNDLE_CONTROL, null);
        if (StringUtils.isNotBlank(resourceBundleControl))
        {
            cfg.resourceBundleControl = (ResourceBundle.Control) ClassUtils.newInstance(resourceBundleControl);
        }
        
        cfg.automaticExtensionlessMapping = getBoolean(extCtx, FacesServlet.AUTOMATIC_EXTENSIONLESS_MAPPING_PARAM_NAME,
                AUTOMATIC_EXTENSIONLESS_MAPPING_DEFAULT);

        cfg.elResolverTracing = getBoolean(extCtx, EL_RESOLVER_TRACING,
                EL_RESOLVER_TRACING_DEFAULT);
        
        if (cfg.projectStage == ProjectStage.Production)
        {
            cfg.faceletsRefreshPeriod = getLong(extCtx,
                    ViewHandler.FACELETS_REFRESH_PERIOD_PARAM_NAME,
                    -1);
        }
        else
        {
            cfg.faceletsRefreshPeriod = getLong(extCtx,
                    ViewHandler.FACELETS_REFRESH_PERIOD_PARAM_NAME,
                    0);
        }

        String[] exceptionTypesToIgnoreInLogging = StringUtils.splitShortString(
                getString(extCtx, EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING, ""),
                ',');
        for (String exceptionTypeToIgnoreInLogging : exceptionTypesToIgnoreInLogging)
        {
            if (StringUtils.isNotBlank(exceptionTypeToIgnoreInLogging))
            {
                cfg.exceptionTypesToIgnoreInLogging.add(exceptionTypeToIgnoreInLogging);
            }
        }
        
        return cfg;
    }

    private static boolean getBoolean(ExternalContext externalContext, String paramName, boolean defaultValue)
    {
        String value = externalContext.getInitParameter(paramName);
        if (value == null)
        {
            return defaultValue;
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes"))
        {
            return true;
        }
        else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("off") || value.equalsIgnoreCase("no"))
        {
            return false;
        }

        return defaultValue;
    }

    private static String getString(ExternalContext externalContext, String paramName, String defaultValue)
    {
        String value = externalContext.getInitParameter(paramName);
        if (value == null)
        {
            return defaultValue;
        }
        
        return value;
    }

    private static int getInt(ExternalContext externalContext, String paramName, int defaultValue)
    {
        String value = externalContext.getInitParameter(paramName);
        if (value == null)
        {
            return defaultValue;
        }

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
        }
        return defaultValue;
    }
    
    private static long getLong(ExternalContext externalContext, String paramName, long defaultValue)
    {
       String value = externalContext.getInitParameter(paramName);
       if (value == null)
       {
           return defaultValue;
       }

        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e)
        {
        }
        return defaultValue;
    }

    public boolean isMyfacesImplAvailable()
    {
        return MYFACES_IMPL_AVAILABLE;
    }

    public boolean isRiImplAvailable()
    {
        return RI_IMPL_AVAILABLE;
    }

    public boolean isStrictJsf2AllowSlashLibraryName()
    {
        return strictJsf2AllowSlashLibraryName;
    }

    public long getConfigRefreshPeriod()
    {
        return configRefreshPeriod;
    }

    public boolean isRenderViewStateId()
    {
        return renderViewStateId;
    }

    public boolean isStrictXhtmlLinks()
    {
        return strictXhtmlLinks;
    }

    public boolean isRenderClearJavascriptOnButton()
    {
        return renderClearJavascriptOnButton;
    }

    public boolean isRefreshTransientBuildOnPSS()
    {
        return refreshTransientBuildOnPSS;
    }

    public boolean isRefreshTransientBuildOnPSSAuto()
    {
        return refreshTransientBuildOnPSSAuto;
    }

    public boolean isRefreshTransientBuildOnPSSPreserveState()
    {
        return refreshTransientBuildOnPSSPreserveState;
    }

    public boolean isValidateXML()
    {
        return validateXML;
    }

    public boolean isWrapScriptContentWithXmlCommentTag()
    {
        return wrapScriptContentWithXmlCommentTag;
    }

    public boolean isDebugPhaseListenerEnabled()
    {
        return debugPhaseListenerEnabled;
    }

    public boolean isStrictJsf2CCELResolver()
    {
        return strictJsf2CCELResolver;
    }

    public String getDefaultResponseWriterContentTypeMode()
    {
        return defaultResponseWriterContentTypeMode;
    }

    public boolean isViewUniqueIdsCacheEnabled()
    {
        return viewUniqueIdsCacheEnabled;
    }

    public int getComponentUniqueIdsCacheSize()
    {
        return componentUniqueIdsCacheSize;
    }

    public boolean isStrictJsf2ViewNotFound()
    {
        return strictJsf2ViewNotFound;
    }

    public boolean isEarlyFlushEnabled()
    {
        return earlyFlushEnabled;
    }

    public boolean isStrictJsf2FaceletsCompatibility()
    {
        return strictJsf2FaceletsCompatibility;
    }

    public boolean isRenderFormViewStateAtBegin()
    {
        return renderFormViewStateAtBegin;
    }

    public boolean isFlashScopeDisabled()
    {
        return flashScopeDisabled;
    }

    public Integer getNumberOfViewsInSession()
    {
        return numberOfViewsInSession;
    }

    public Integer getNumberOfSequentialViewsInSession()
    {
        return numberOfSequentialViewsInSession;
    }

    public Integer getNumberOfFlashTokensInSession()
    {
        return numberOfFlashTokensInSession;
    }

    public boolean isSupportEL3ImportHandler()
    {
        return supportEL3ImportHandler;
    }

    public boolean isStrictJsf2OriginHeaderAppPath()
    {
        return strictJsf2OriginHeaderAppPath;
    }

    public int getResourceBufferSize()
    {
        return resourceBufferSize;
    }

    public boolean isUseCdiForAnnotationScanning()
    {
        return useCdiForAnnotationScanning;
    }

    public boolean isResourceHandlerCacheEnabled()
    {
        return resourceHandlerCacheEnabled;
    }

    public int getResourceHandlerCacheSize()
    {
        return resourceHandlerCacheSize;
    }

    public String getScanPackages()
    {
        return scanPackages;
    }

    public long getWebsocketMaxIdleTimeout()
    {
        return websocketMaxIdleTimeout;
    }   
    
    public Integer getWebsocketEndpointPort()
    {
        return websocketEndpointPort;
    }

    public long getClientViewStateTimeout()
    {
        return clientViewStateTimeout;
    }

    public String getRandomKeyInViewStateSessionToken()
    {
        return randomKeyInViewStateSessionToken;
    }

    public int getRandomKeyInViewStateSessionTokenLength()
    {
        return randomKeyInViewStateSessionTokenLength;
    }

    public String getRandomKeyInViewStateSessionTokenSecureRandomClass()
    {
        return randomKeyInViewStateSessionTokenSecureRandomClass;
    }

    public String getRandomKeyInViewStateSessionTokenSecureRandomProvider()
    {
        return randomKeyInViewStateSessionTokenSecureRandomProvider;
    }

    public String getRandomKeyInViewStateSessionTokenSecureRandomAlgorithm()
    {
        return randomKeyInViewStateSessionTokenSecureRandomAlgorithm;
    }

    public String getRandomKeyInCsrfSessionToken()
    {
        return randomKeyInCsrfSessionToken;
    }

    public boolean isSerializeStateInSession()
    {
        return serializeStateInSession;
    }

    public boolean isCompressStateInSession()
    {
        return compressStateInSession;
    }

    public boolean isUseFlashScopePurgeViewsInSession()
    {
        return useFlashScopePurgeViewsInSession;
    }

    public String getAutocompleteOffViewState()
    {
        if(autocompleteOffViewState.equals("false")){
            return null;
        }
        if(autocompleteOffViewState.equals("true")){
            return "off";
        }
        return autocompleteOffViewState; // return one-time-code
    }

    public long getResourceMaxTimeExpires()
    {
        return resourceMaxTimeExpires;
    }

    public boolean isLazyLoadConfigObjects()
    {
        return lazyLoadConfigObjects;
    }

    public String getElResolverComparator()
    {
        return elResolverComparator;
    }

    public String getElResolverPredicate()
    {
        return elResolverPredicate;
    }

    public int getViewIdCacheSize()
    {
        return viewIdCacheSize;
    }

    public boolean isBeanBeforeJsfValidation()
    {
        return beanBeforeJsfValidation;
    }

    public String getRandomKeyInWebsocketSessionToken()
    {
        return randomKeyInWebsocketSessionToken;
    }

    public String getFacesInitPlugins()
    {
        return facesInitPlugins;
    }

    public boolean isInitializeSkipJarFacesConfigScan()
    {
        return initializeSkipJarFacesConfigScan;
    }

    public String getExpressionFactory()
    {
        return expressionFactory;
    }

    public String getCheckIdProductionMode()
    {
        return checkIdProductionMode;
    }

    public boolean isPartialStateSaving()
    {
        return partialStateSaving;
    }

    public String[] getFullStateSavingViewIds()
    {
        return fullStateSavingViewIds;
    }

    public int getFaceletsBufferSize()
    {
        return faceletsBufferSize;
    }

    public boolean isMarkInitialStateWhenApplyBuildView()
    {
        return markInitialStateWhenApplyBuildView;
    }

    public String[] getViewSuffix()
    {
        return viewSuffix;
    }

    public String[] getFaceletsViewMappings()
    {
        return faceletsViewMappings;
    }

    public String getFaceletsViewSuffix()
    {
        return faceletsViewSuffix;
    }

    public boolean isViewIdExistsCacheEnabled()
    {
        return viewIdExistsCacheEnabled;
    }

    public boolean isViewIdProtectedCacheEnabled()
    {
        return viewIdProtectedCacheEnabled;
    }

    public boolean isViewIdDeriveCacheEnabled()
    {
        return viewIdDeriveCacheEnabled;
    }

    public ELExpressionCacheMode getELExpressionCacheMode()
    {
        return elExpressionCacheMode;
    }

    public boolean isWrapTagExceptionsAsContextAware()
    {
        return wrapTagExceptionsAsContextAware;
    }

    public boolean isResourceCacheLastModified()
    {
        return resourceCacheLastModified;
    }

    public boolean isLogWebContextParams()
    {
        return logWebContextParams;
    }

    public int getWebsocketMaxConnections()
    {
        return websocketMaxConnections;
    }

    public boolean isRenderClientBehaviorScriptsAsString()
    {
        return renderClientBehaviorScriptsAsString;
    }

    public boolean isAlwaysForceSessionCreation()
    {
        return alwaysForceSessionCreation;
    }

    public ProjectStage getProjectStage()
    {
        return projectStage;
    }

    public ResourceBundle.Control getResourceBundleControl()
    {
        return resourceBundleControl;
    }

    public boolean isAutomaticExtensionlessMapping()
    {
        return automaticExtensionlessMapping;
    }

    public Integer getNumberOfClientWindows()
    {
        return numberOfClientWindows;
    }

    public boolean isElResolverTracing()
    {
        return elResolverTracing;
    }

    public long getFaceletsRefreshPeriod()
    {
        return faceletsRefreshPeriod;
    }

    public List<String> getExceptionTypesToIgnoreInLogging()
    {
        return exceptionTypesToIgnoreInLogging;
    }
}

