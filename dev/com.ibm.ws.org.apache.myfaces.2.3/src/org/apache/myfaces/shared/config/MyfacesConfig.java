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
package org.apache.myfaces.shared.config;


import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 * Holds all configuration init parameters (from web.xml) that are independent
 * from the core implementation. The parameters in this class are available to
 * all shared, component and implementation classes.
 * See RuntimeConfig for configuration infos that come from the faces-config
 * files and are needed by the core implementation.
 *
 * MyfacesConfig is meant for components that implement some of the extended features
 * of MyFaces. Anyhow, using the MyFaces JSF implementation is no precondition for using
 * MyfacesConfig in custom components. Upon using another JSF implementation
 * (or omitting the extended init parameters) all config properties will simply have
 * their default values.
 */
public class MyfacesConfig
{
    private static final String APPLICATION_MAP_PARAM_NAME = MyfacesConfig.class.getName();

    /**
     * Set the virtual path used to serve resources using tomahawk addResource API. Note ExtensionsFilter should
     * be able to receive request on the prefix used here.
     */
    @JSFWebConfigParam(tags="tomahawk")
    public static final String  INIT_PARAM_RESOURCE_VIRTUAL_PATH = "org.apache.myfaces.RESOURCE_VIRTUAL_PATH";
    public static final String  INIT_PARAM_RESOURCE_VIRTUAL_PATH_DEFAULT = "/faces/myFacesExtensionResource";

    /**
     * If true, rendered HTML code will be formatted, so that it is "human readable".
     * i.e. additional line separators and whitespace will be written, that do not
     * influence the HTML code. Default: "true"
     */
    @JSFWebConfigParam(defaultValue="true", expectedValues="true, false, on, off, yes, no",since="1.1",
            ignoreUpperLowerCase=true, group="render")
    private static final String  INIT_PARAM_PRETTY_HTML = "org.apache.myfaces.PRETTY_HTML";
    private static final boolean INIT_PARAM_PRETTY_HTML_DEFAULT = true;

    /**
     * This parameter tells MyFaces if javascript code should be allowed in the rendered HTML output.
     * If javascript is allowed, command_link anchors will have javascript code 
     * that submits the corresponding form.
     * If javascript is not allowed, the state saving info and nested parameters ill be 
     * added as url parameters.
     * Default: "true"
     */
    @JSFWebConfigParam(defaultValue="true", expectedValues="true, false, on, off, yes, no",since="1.1",
            ignoreUpperLowerCase=true, group="render")
    private static final String  INIT_PARAM_ALLOW_JAVASCRIPT = "org.apache.myfaces.ALLOW_JAVASCRIPT";
    private static final boolean INIT_PARAM_ALLOW_JAVASCRIPT_DEFAULT = true;

    /**
     * Deprecated: tomahawk specific param to detect javascript, but it is no longer valid anymore.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false, on, off, yes, no",since="1.1",
            ignoreUpperLowerCase=true, deprecated=true, tags="tomahawk", group="render")
    private static final String  INIT_PARAM_DETECT_JAVASCRIPT = "org.apache.myfaces.DETECT_JAVASCRIPT";
    private static final boolean INIT_PARAM_DETECT_JAVASCRIPT_DEFAULT = false;

    /**
     * If true, a javascript function will be rendered that is able to restore the 
     * former vertical scroll on every request. Convenient feature if you have pages
     * with long lists and you do not want the browser page to always jump to the top
     * if you trigger a link or button action that stays on the same page.
     * Default: "false"
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false, on, off, yes, no",since="1.1", 
            ignoreUpperLowerCase=true, tags="tomahawk")
    private static final String  INIT_PARAM_AUTO_SCROLL = "org.apache.myfaces.AUTO_SCROLL";
    private static final boolean INIT_PARAM_AUTO_SCROLL_DEFAULT = false;

    /**
     * Tomahawk specific: A class implementing the
     * org.apache.myfaces.shared.renderkit.html.util.AddResource
     * interface. It is responsible to
     * place scripts and css on the right position in your HTML document.
     * Default: "org.apache.myfaces.shared.renderkit.html.util.DefaultAddResource"
     * Follow the description on the MyFaces-Wiki-Performance page to enable
     * StreamingAddResource instead of DefaultAddResource if you want to
     * gain performance.
     */
    @JSFWebConfigParam(defaultValue="org.apache.myfaces. renderkit.html.util. DefaultAddResource",since="1.1",
            desc="Tomahawk specific: Indicate the class responsible to place scripts and css using " +
                 "tomahawk AddResource API", tags="tomahawk")
    private static final String INIT_PARAM_ADD_RESOURCE_CLASS = "org.apache.myfaces.ADD_RESOURCE_CLASS";
    private static final String INIT_PARAM_ADD_RESOURCE_CLASS_DEFAULT = 
        "org.apache.myfaces.renderkit.html.util.DefaultAddResource";

    /**
     * Tomahawk specific: A very common problem in configuring MyFaces-web-applications
     * is that the Extensions-Filter is not configured at all
     * or improperly configured. This parameter will check for a properly
     * configured Extensions-Filter if it is needed by the web-app.
     * In most cases this check will work just fine, there might be cases
     * where an internal forward will bypass the Extensions-Filter and the check
     * will not work. If this is the case, you can disable the check by setting
     * this parameter to false.
     * 
     * In tomahawk for JSF 2.0 since version 1.1.11, this param is set by default to false, otherwise is true.
     */
    @JSFWebConfigParam(defaultValue="for JSF 2.0 since 1.1.11 false, otherwise true", 
            expectedValues="true, false, on, off, yes, no",since="1.1", ignoreUpperLowerCase=true,
            desc="Tomahawk specific: This parameter will check for a properly configured Extensions-Filter if " +
                 "it is needed by the web-app.", tags="tomahawk")
    private static final String  INIT_CHECK_EXTENSIONS_FILTER = "org.apache.myfaces.CHECK_EXTENSIONS_FILTER";
    private static final boolean INIT_CHECK_EXTENSIONS_FILTER_DEFAULT = false;

    /**
     * Tomahawk specific: Interpret "readonly" property as "disable" for select components like t:selectOneRow.
     */
    @JSFWebConfigParam(defaultValue="true", expectedValues="true, false, on, off, yes, no",since="1.1", 
            ignoreUpperLowerCase=true, tags="tomahawk", group="render")
    private static final String INIT_READONLY_AS_DISABLED_FOR_SELECT = 
        "org.apache.myfaces.READONLY_AS_DISABLED_FOR_SELECTS";
    private static final boolean INIT_READONLY_AS_DISABLED_FOR_SELECT_DEFAULT = true;

    /**
     * Set the time in seconds that check for updates of web.xml and faces-config descriptors and 
     * refresh the configuration.
     * This param is valid only if project stage is not production. Set this param to 0 disable this feature.
     */
    @JSFWebConfigParam(defaultValue="2",since="1.1", classType="java.lang.Long")
    public static final String INIT_PARAM_CONFIG_REFRESH_PERIOD = "org.apache.myfaces.CONFIG_REFRESH_PERIOD";
    public static final long INIT_PARAM_CONFIG_REFRESH_PERIOD_DEFAULT = 2;

    /**
     * Set the view state using a javascript function instead a hidden input field.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false, on, off, yes, no",since="1.1", 
            ignoreUpperLowerCase=true, deprecated=true, group="state")
    private static final String  INIT_PARAM_VIEWSTATE_JAVASCRIPT = "org.apache.myfaces.VIEWSTATE_JAVASCRIPT";
    private static final boolean INIT_PARAM_VIEWSTATE_JAVASCRIPT_DEFAULT = false;

    /**
     * Define if the input field that should store the state (javax.faces.ViewState) should render 
     * id="javax.faces.ViewState".
     * 
     * JSF API 1.2 defines a "javax.faces.ViewState" client parameter, that must be rendered as both the "name"
     * and the "id" attribute of the hidden input that is rendered for the purpose of state saving
     * (see ResponseStateManager.VIEW_STATE_PARAM).
     * Actually this causes duplicate id attributes and thus invalid XHTML pages when multiple forms are rendered on
     * one page. With the org.apache.myfaces.RENDER_VIEWSTATE_ID context parameter you can tune this behaviour.
     * <br/>Set it to
     * <ul><li>true - to render JSF 1.2 compliant id attributes (that might cause invalid XHTML), or</li>
     * <li>false - to omit rendering of the id attribute (which is only needed for very special 
     * AJAX/Javascript components)</li></ul>
     * Default value is: true (for backwards compatibility and JSF 1.2 compliancy) 
     */
    @JSFWebConfigParam(defaultValue="true", expectedValues="true, false, on, off, yes, no",since="1.1", 
            ignoreUpperLowerCase=true, group="state")
    private static final String  INIT_PARAM_RENDER_VIEWSTATE_ID = "org.apache.myfaces.RENDER_VIEWSTATE_ID";
    private static final boolean INIT_PARAM_RENDER_VIEWSTATE_ID_DEFAULT = true;

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
    private static final String  INIT_PARAM_STRICT_XHTML_LINKS = "org.apache.myfaces.STRICT_XHTML_LINKS";
    private static final boolean INIT_PARAM_STRICT_XHTML_LINKS_DEFAULT = true;
    
    /**
     * This param renders the clear javascript on button necessary only for
     * compatibility with hidden fields feature of myfaces. This is done 
     * because jsf ri does not render javascript on onclick method for button,
     * so myfaces should do this.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false, on, off, yes, no",since="1.2.3",
            ignoreUpperLowerCase=true, group="render")
    private static final String INIT_PARAM_RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON = 
        "org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON";
    private static final boolean INIT_PARAM_RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON_DEFAULT= false;

    /**
     * This param renders hidden fields at the end of h:form for link params when h:commandLink + f:param is used,
     * instead use javascript to create them. Set this param to true also enables 
     * org.apache.myfaces.RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON 
     * automatically to ensure consistency. This feature is required to support Windows Mobile 6, because in 
     * this environment, document.createElement() and form.appendChild() javascript methods are not supported.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false, on, off, yes, no",since="1.2.9",
            ignoreUpperLowerCase=true, group="render")
    private static final String INIT_PARAM_RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS = 
        "org.apache.myfaces.RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS";
    private static final boolean INIT_PARAM_RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS_DEFAULT= false;
    
    /**
     * Add a code that save the form before submit using a
     * link (call to window.external.AutoCompleteSaveForm(form) ). It's a bug on IE.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false, on, off, yes, no",since="1.1",
            ignoreUpperLowerCase=true, group="render")
    private static final String INIT_PARAM_SAVE_FORM_SUBMIT_LINK_IE = "org.apache.myfaces.SAVE_FORM_SUBMIT_LINK_IE";
    private static final boolean INIT_PARAM_SAVE_FORM_SUBMIT_LINK_IE_DEFAULT = false;
    
    /**
     * Define an alternate class name that will be used to initialize MyFaces, instead the default 
     * javax.faces.webapp.FacesServlet.
     * 
     * <p>This helps MyFaces to detect the mappings and other additional configuration used to setup the 
     * environment, and prevent abort initialization if no FacesServlet config is detected.
     * </p>
     */
    @JSFWebConfigParam(since="1.2.7")
    private static final String INIT_PARAM_DELEGATE_FACES_SERVLET = "org.apache.myfaces.DELEGATE_FACES_SERVLET";

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
    public final static String INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS = 
        "org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS"; 
    public final static String INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS_DEFAULT = "auto";

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
    public final static String INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE = 
        "org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE";
    public final static boolean INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE_DEFAULT = false;
    
    /**
     * If set to <code>true</code>, tag library XML files and faces config XML files using schema 
     * will be validated during application start up
     */
    @JSFWebConfigParam(since="2.0", expectedValues="true, false, on, off, yes, no", ignoreUpperLowerCase=true)
    public final static String INIT_PARAM_VALIDATE_XML = "org.apache.myfaces.VALIDATE_XML";
    public final static boolean INIT_PARAM_VALIDATE_XML_DEFAULT = false;
    
    /**
     * Wrap content inside script with xml comment to prevent old browsers to display it. By default it is true. 
     */
    @JSFWebConfigParam(since="2.0.1", expectedValues="true, false, on, off, yes, no", defaultValue="false",
            ignoreUpperLowerCase=true, group="render")
    public final static String INIT_PARAM_WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG = 
        "org.apache.myfaces.WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG";
    public final static boolean INIT_PARAM_WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG_DEFAULT = false;
    
    /**
     * If set true, render the form submit script inline, as in myfaces core 1.2 and earlier versions 
     */
    @JSFWebConfigParam(since="2.0.2", expectedValues="true, false, on, off, yes, no", defaultValue="false", 
            ignoreUpperLowerCase=true, group="render")
    public final static String INIT_PARAM_RENDER_FORM_SUBMIT_SCRIPT_INLINE = 
        "org.apache.myfaces.RENDER_FORM_SUBMIT_SCRIPT_INLINE";
    public final static boolean INIT_PARAM_RENDER_FORM_SUBMIT_SCRIPT_INLINE_DEFAULT = false;
    
    /**
     * Enable/disable DebugPhaseListener feature, with provide useful information about ValueHolder 
     * variables (submittedValue, localValue, value).
     * Note evaluate those getters for each component could cause some unwanted side effects when 
     * using "access" type scopes like on MyFaces CODI.
     * This param only has effect when project stage is Development.     
     */
    @JSFWebConfigParam(since="2.0.8")
    public final static String INIT_PARAM_DEBUG_PHASE_LISTENER = "org.apache.myfaces.DEBUG_PHASE_LISTENER";
    public final static boolean INIT_PARAM_DEBUG_PHASE_LISTENER_DEFAULT = false;
    
    /**
     * DEPRECATED: No longer used in JSF 2.3, because PartialViewContext now contemplates update resources
     * in ajax request. Detect if a target (usually head) should be update for the current view in an ajax render 
     * operation. This is activated if a css or js resource is added dynamically by effect of a refresh 
     * (c:if, ui:include src="#{...}" or a manipulation of the tree). This ensures ajax updates of content 
     * using ui:include will be consistent. Note this behavior is a myfaces specific extension, so to 
     * ensure strict compatibility with the spec, set this param to false (default false).
     * 
     * @deprecated 
     */
    @JSFWebConfigParam(since="2.0.10", expectedValues="true, false", defaultValue="false",deprecated = true)
    public final static String INIT_PARAM_STRICT_JSF_2_REFRESH_TARGET_AJAX = 
        "org.apache.myfaces.STRICT_JSF_2_REFRESH_TARGET_AJAX";
    public final static boolean INIT_PARAM_STRICT_JSF_2_REFRESH_TARGET_AJAX_DEFAULT = false;
    
    /**
     * Change default getType() behavior for composite component EL resolver, from return null 
     * (see JSF 2_0 spec section 5_6_2_2) to
     * use the metadata information added by composite:attribute, ensuring components working with 
     * chained EL expressions to find the
     * right type when a getType() is called over the source EL expression.
     * 
     * To ensure strict compatibility with the spec set this param to true (by default is false, 
     * so the change is enabled by default). 
     */
    @JSFWebConfigParam(since="2.0.10", expectedValues="true, false", defaultValue="false", group="EL")
    public final static String INIT_PARAM_STRICT_JSF_2_CC_EL_RESOLVER = 
        "org.apache.myfaces.STRICT_JSF_2_CC_EL_RESOLVER";
    public final static boolean INIT_PARAM_STRICT_JSF_2_CC_EL_RESOLVER_DEFAULT = false;
    
    /**
     * Define the default content type that the default ResponseWriter generates, when no match can be derived from
     * HTTP Accept Header.
     */
    @JSFWebConfigParam(since="2.0.11,2.1.5", expectedValues="text/html, application/xhtml+xml", 
            defaultValue="text/html", group="render")
    public final static String INIT_PARAM_DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE = 
        "org.apache.myfaces.DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE";
    public final static String INIT_PARAM_DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE_DEFAULT = "text/html";

    /**
     * Enable or disable a cache used to "remember" the generated facelets unique ids and reduce 
     * the impact on memory usage, only active if javax.faces.FACELETS_REFRESH_PERIOD is -1 (no refresh).
     */
    @JSFWebConfigParam(defaultValue = "true", since = "2.0.13, 2.1.7", expectedValues="true, false", 
            group="viewhandler", tags="performance",
            desc="Enable or disable a cache used to 'remember'  the generated facelets unique ids " + 
                 "and reduce the impact over memory usage.")
    public static final String INIT_PARAM_VIEW_UNIQUE_IDS_CACHE_ENABLED = 
        "org.apache.myfaces.VIEW_UNIQUE_IDS_CACHE_ENABLED";
    public static final boolean INIT_PARAM_VIEW_UNIQUE_IDS_CACHE_ENABLED_DEFAULT = true;
    
    /**
     * Set the size of the cache used to store strings generated using SectionUniqueIdCounter
     * for component ids. If this is set to 0, no cache is used. By default is set to 100.
     */
    @JSFWebConfigParam(defaultValue = "100", since = "2.0.13, 2.1.7",
            group="viewhandler", tags="performance")
    public static final String INIT_PARAM_COMPONENT_UNIQUE_IDS_CACHE_SIZE =
        "org.apache.myfaces.COMPONENT_UNIQUE_IDS_CACHE_SIZE";
    public static final int INIT_PARAM_COMPONENT_UNIQUE_IDS_CACHE_SIZE_DEFAULT = 100;

    /**
    * If set false, myfaces won't support JSP and javax.faces.el. JSP are deprecated in JSF 2.X, javax.faces.el in 
    * in JSF 1.2. Default value is true. 
    * 
    * If this property is set is false, JSF 1.1 VariableResolver and PropertyResolver config (replaced in JSF 1.2 by
    * ELResolver) and all related logic for JSP is skipped, making EL evaluation faster.  
    */
    @JSFWebConfigParam(since="2.0.13,2.1.7", expectedValues="true,false", defaultValue="true",
         desc="If set false, myfaces won't support JSP and javax.faces.el. JSP are deprecated in " +
         "JSF 2.X, javax.faces.el in in JSF 1.2. Default value is true.",
         group="EL", tags="performance ")
    public final static String INIT_PARAM_SUPPORT_JSP_AND_FACES_EL = "org.apache.myfaces.SUPPORT_JSP_AND_FACES_EL";
    public final static boolean INIT_PARAM_SUPPORT_JSP_AND_FACES_EL_DEFAULT = true;
    
    @JSFWebConfigParam(since = "2.3", expectedValues="true,false", defaultValue="true",
         desc="If set false, myfaces won't support ManagedBeans anymore. ManagedBeans are deprecated in " +
         "JSF 2.3, Default value is true.",
         group="EL", tags="performance ")
    public static final String INIT_PARAM_SUPPORT_MANAGED_BEANS = "org.apache.myfaces.SUPPORT_MANAGED_BEANS";
    public final static boolean INIT_PARAM_SUPPORT_MANAGED_BEANS_DEFAULT = true;
    
    /**
     * When the application runs inside Google Application Engine container (GAE),
     * indicate which jar files should be scanned for files (faces-config, facelets taglib
     * or annotations). It accept simple wildcard patterns like myfavoritejsflib-*.jar or 
     * myfavoritejsflib-1.1.?.jar. By default, all the classpath is scanned for files 
     * annotations (so it adds an small delay on startup).
     */
    @JSFWebConfigParam(since = "2.1.8, 2.0.14", expectedValues="none, myfavoritejsflib-*.jar",
            tags="performance, GAE")
    public static final String INIT_PARAM_GAE_JSF_JAR_FILES = "org.apache.myfaces.GAE_JSF_JAR_FILES";
    public final static String INIT_PARAM_GAE_JSF_JAR_FILES_DEFAULT = null;

    /**
     * When the application runs inside Google Application Engine container (GAE),
     * indicate which jar files should be scanned for annotations. This param overrides
     * org.apache.myfaces.GAE_JSF_JAR_FILES behavior that tries to find faces-config.xml or
     * files ending with .faces-config.xml in /META-INF folder and if that so, try to
     * find JSF annotations in the whole jar file. It accept simple wildcard patterns 
     * like myfavoritejsflib-*.jar or myfavoritejsflib-1.1.?.jar.
     * By default, all the classpath is scanned for annotations (so it adds an small
     * delay on startup).
     */
    @JSFWebConfigParam(since = "2.1.8, 2.0.14", expectedValues="none, myfavoritejsflib-*.jar",
            tags="performance, GAE")
    public static final String INIT_PARAM_GAE_JSF_ANNOTATIONS_JAR_FILES = 
            "org.apache.myfaces.GAE_JSF_ANNOTATIONS_JAR_FILES";
    public final static String INIT_PARAM_GAE_JSF_ANNOTATIONS_JAR_FILES_DEFAULT = null;
    
    /**
     * If this param is set to true, a check will be done in Restore View Phase to check
     * if the viewId exists or not and if it does not exists, a 404 response will be thrown.
     * 
     * This is applicable in cases where all the views in the application are generated by a 
     * ViewDeclarationLanguage implementation.
     */
    @JSFWebConfigParam(since = "2.1.13", defaultValue="false", expectedValues="true,false", 
            group="viewhandler")
    public static final String INIT_PARAM_STRICT_JSF_2_VIEW_NOT_FOUND = 
            "org.apache.myfaces.STRICT_JSF_2_VIEW_NOT_FOUND";
    public final static boolean INIT_PARAM_STRICT_JSF_2_VIEW_NOT_FOUND_DEFAULT = false;

    @JSFWebConfigParam(defaultValue = "false", since = "2.2.0", expectedValues="true, false", group="render",
            tags="performance",
            desc="Enable or disable an early flush which allows to send e.g. the HTML-Head to the client " +
                    "while the rest gets rendered. It's a well known technique to reduce the time for loading a page.")
    private static final String INIT_PARAM_EARLY_FLUSH_ENABLED =
        "org.apache.myfaces.EARLY_FLUSH_ENABLED";
    private static final boolean INIT_PARAM_EARLY_FLUSH_ENABLED_DEFAULT = false;
    
    /**
     * This param makes components like c:set, ui:param and templating components like ui:decorate,
     * ui:composition and ui:include to behave like the ones provided originally in facelets 1_1_x. 
     * See MYFACES-3810 for details.
     */
    @JSFWebConfigParam(since = "2.2.0", defaultValue="false", expectedValues="true,false", 
            group="viewhandler")
    public static final String INIT_PARAM_STRICT_JSF_2_FACELETS_COMPATIBILITY = 
            "org.apache.myfaces.STRICT_JSF_2_FACELETS_COMPATIBILITY";
    public final static boolean INIT_PARAM_STRICT_JSF_2_FACELETS_COMPATIBILITY_DEFAULT = false;    
    
    /**
     * This param makes h:form component to render the view state and other hidden fields
     * at the beginning of the form. This also includes component resources with target="form",
     * but it does not include legacy 1.1 myfaces specific hidden field adition.
     */
    @JSFWebConfigParam(since = "2.2.4", defaultValue = "false", expectedValues = "true,false",
            group="render")
    public static final String INIT_PARAM_RENDER_FORM_VIEW_STATE_AT_BEGIN =
            "org.apache.myfaces.RENDER_FORM_VIEW_STATE_AT_BEGIN";
    public final static boolean INIT_PARAM_RENDER_FORM_VIEW_STATE_AT_BEGIN_DEFAULT = false;
    
    /**
     * Defines whether flash scope is disabled, preventing add the Flash cookie to the response. 
     * 
     * <p>This is useful for applications that does not require to use flash scope, and instead uses other scopes.</p>
     */
    @JSFWebConfigParam(defaultValue="false",since="2.0.5")
    public static final String INIT_PARAM_FLASH_SCOPE_DISABLED = "org.apache.myfaces.FLASH_SCOPE_DISABLED";
    public static final boolean INIT_PARAM_FLASH_SCOPE_DISABLED_DEFAULT = false;
    
    /**
     * Defines the amount (default = 20) of the latest views are stored in session.
     * 
     * <p>Only applicable if state saving method is "server" (= default).
     * </p>
     * 
     */
    @JSFWebConfigParam(defaultValue="20",since="1.1", classType="java.lang.Integer", group="state", tags="performance")
    public static final String INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION = "org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION";

    /**
     * Default value for <code>org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION</code> context parameter.
     */
    public static final int INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT = 20;    

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
    public static final String INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION
            = "org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION";
    public static final Integer INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT = 4;
    
    /**
     * Indicate the max number of flash tokens stored into session. It is only active when 
     * javax.faces.CLIENT_WINDOW_MODE is enabled and javax.faces.STATE_SAVING_METHOD is set
     * to "server". Each flash token is associated to one client window id at
     * the same time, so this param is related to the limit of active client windows per session. 
     * By default is the same number as in 
     * (org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION / 
     * org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION) + 1 = 6.
     */
    @JSFWebConfigParam(since="2.2.6", group="state", tags="performance")
    static final String INIT_PARAM_NUMBER_OF_FLASH_TOKENS_IN_SESSION = 
            "org.apache.myfaces.NUMBER_OF_FLASH_TOKENS_IN_SESSION";
    
    /**
     * Indicate the max number of client window ids stored into session by faces flow. It is only active when 
     * javax.faces.CLIENT_WINDOW_MODE is enabled and javax.faces.STATE_SAVING_METHOD is set
     * to "server". This param is related to the limit of active client 
     * windows per session, and it is used to cleanup flow scope beans when a client window or view becomes 
     * invalid. 
     * By default is the same number as in 
     * (org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION / 
     * org.apache.myfaces.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION) + 1 = 6.
     */
    @JSFWebConfigParam(since="2.2.6", group="state", tags="performance")
    static final String INIT_PARAM_NUMBER_OF_FACES_FLOW_CLIENT_WINDOW_IDS_IN_SESSION = 
            "org.apache.myfaces.FACES_FLOW_CLIENT_WINDOW_IDS_IN_SESSION";
    
    /**
     * This parameter specifies whether or not the ImportHandler will be supported
     */
    @JSFWebConfigParam(since="2.2.9", defaultValue="false", expectedValues="true,false", group="EL")
    protected static final String SUPPORT_EL_3_IMPORT_HANDLER = "org.apache.myfaces.SUPPORT_EL_3_IMPORT_HANDLER";
    public final static boolean SUPPORT_EL_3_IMPORT_HANDLER_DEFAULT = false;

    private boolean _prettyHtml;
    private boolean _detectJavascript;
    private boolean _allowJavascript;
    private boolean _autoScroll;
    private String _addResourceClass;
    private String _resourceVirtualPath;
    private boolean _checkExtensionsFilter;
    private boolean _readonlyAsDisabledForSelect;
    private long _configRefreshPeriod;
    private boolean _viewStateJavascript;
    private boolean _renderViewStateId;
    private boolean _strictXhtmlLinks;
    private boolean _renderClearJavascriptOnButton;
    private boolean renderHiddenFieldsForLinkParams;
    private boolean _saveFormSubmitLinkIE;
    private String _delegateFacesServlet;
    private boolean _refreshTransientBuildOnPSS;
    private boolean _refreshTransientBuildOnPSSAuto;
    private boolean refreshTransientBuildOnPSSPreserveState;
    private boolean _validateXML;
    private boolean _wrapScriptContentWithXmlCommentTag;
    private boolean _renderFormSubmitScriptInline;
    private boolean _debugPhaseListenerEnabled;
    private boolean _strictJsf2RefreshTargetAjax;
    private boolean _strictJsf2CCELResolver;
    private String _defaultResponseWriterContentTypeMode;
    private boolean _viewUniqueIdsCacheEnabled;
    private int _componentUniqueIdsCacheSize;
    private boolean _supportJSPAndFacesEL;
    private boolean _supportManagedBeans;
    private String _gaeJsfJarFiles;
    private String _gaeJsfAnnotationsJarFiles;
    private boolean _strictJsf2ViewNotFound;
    private boolean _earlyFlushEnabled;
    private boolean _strictJsf2FaceletsCompatibility;
    private boolean _renderFormViewStateAtBegin;
    private boolean _flashScopeDisabled;
    private Integer _numberOfViewsInSession;
    private Integer _numberOfSequentialViewsInSession;
    private Integer _numberOfFlashTokensInSession;
    private Integer _numberOfFacesFlowClientWindowIdsInSession;
    private boolean _supportEL3ImportHandler;

    private static final boolean TOMAHAWK_AVAILABLE;
    private static final boolean MYFACES_IMPL_AVAILABLE;
    private static final boolean RI_IMPL_AVAILABLE;

    static
    {
        boolean tomahawkAvailable;
        try
        {
            ClassUtils.classForName("org.apache.myfaces.webapp.filter.ExtensionsFilter");
            tomahawkAvailable = true;
        }
        catch (ClassNotFoundException e)
        {
            tomahawkAvailable = false;
        }
        TOMAHAWK_AVAILABLE = tomahawkAvailable;
    }

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
    }

    static
    {
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

    public static MyfacesConfig getCurrentInstance(ExternalContext extCtx)
    {
        MyfacesConfig myfacesConfig = (MyfacesConfig) extCtx
                .getApplicationMap().get(APPLICATION_MAP_PARAM_NAME);
        if (myfacesConfig == null)
        {

            myfacesConfig = createAndInitializeMyFacesConfig(extCtx);

            extCtx.getApplicationMap().put(APPLICATION_MAP_PARAM_NAME, myfacesConfig);

        }

        return myfacesConfig;
    }
    
    public MyfacesConfig()
    {
        setPrettyHtml(INIT_PARAM_PRETTY_HTML_DEFAULT);
        setAllowJavascript(INIT_PARAM_ALLOW_JAVASCRIPT_DEFAULT);
        setRenderClearJavascriptOnButton(INIT_PARAM_RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON_DEFAULT);
        setRenderHiddenFieldsForLinkParams(INIT_PARAM_RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS_DEFAULT);
        setSaveFormSubmitLinkIE(INIT_PARAM_SAVE_FORM_SUBMIT_LINK_IE_DEFAULT);
        setReadonlyAsDisabledForSelect(INIT_READONLY_AS_DISABLED_FOR_SELECT_DEFAULT);
        setRenderViewStateId(INIT_PARAM_RENDER_VIEWSTATE_ID_DEFAULT);
        setStrictXhtmlLinks(INIT_PARAM_STRICT_XHTML_LINKS_DEFAULT);
        setConfigRefreshPeriod(INIT_PARAM_CONFIG_REFRESH_PERIOD_DEFAULT);        
        setViewStateJavascript(INIT_PARAM_VIEWSTATE_JAVASCRIPT_DEFAULT);        
        setRefreshTransientBuildOnPSS(true);
        setRefreshTransientBuildOnPSSAuto(true);
        setRefreshTransientBuildOnPSSPreserveState(INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE_DEFAULT);
        setValidateXML(INIT_PARAM_VALIDATE_XML_DEFAULT);
        setWrapScriptContentWithXmlCommentTag(INIT_PARAM_WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG_DEFAULT);
        setDetectJavascript(INIT_PARAM_DETECT_JAVASCRIPT_DEFAULT);
        setAutoScroll(INIT_PARAM_AUTO_SCROLL_DEFAULT);
        setAddResourceClass(INIT_PARAM_ADD_RESOURCE_CLASS_DEFAULT);
        setResourceVirtualPath(INIT_PARAM_RESOURCE_VIRTUAL_PATH_DEFAULT);
        //The default is true but we'll let it false because it depends if 
        //tomahawk is on classpath and no test environment is set
        setCheckExtensionsFilter(false);
        setRenderFormSubmitScriptInline(INIT_PARAM_RENDER_FORM_SUBMIT_SCRIPT_INLINE_DEFAULT);
        setDebugPhaseListenerEnabled(INIT_PARAM_DEBUG_PHASE_LISTENER_DEFAULT);
        setStrictJsf2RefreshTargetAjax(INIT_PARAM_STRICT_JSF_2_REFRESH_TARGET_AJAX_DEFAULT);
        setStrictJsf2CCELResolver(INIT_PARAM_STRICT_JSF_2_CC_EL_RESOLVER_DEFAULT);
        setDefaultResponseWriterContentTypeMode(INIT_PARAM_DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE_DEFAULT);
        setViewUniqueIdsCacheEnabled(INIT_PARAM_VIEW_UNIQUE_IDS_CACHE_ENABLED_DEFAULT);
        setComponentUniqueIdsCacheSize(INIT_PARAM_COMPONENT_UNIQUE_IDS_CACHE_SIZE_DEFAULT);
        setSupportJSPAndFacesEL(INIT_PARAM_SUPPORT_JSP_AND_FACES_EL_DEFAULT);
        setSupportManagedBeans(INIT_PARAM_SUPPORT_MANAGED_BEANS_DEFAULT);
        setGaeJsfJarFiles(INIT_PARAM_GAE_JSF_JAR_FILES_DEFAULT);
        setGaeJsfAnnotationsJarFiles(INIT_PARAM_GAE_JSF_ANNOTATIONS_JAR_FILES_DEFAULT);
        setStrictJsf2ViewNotFound(INIT_PARAM_STRICT_JSF_2_VIEW_NOT_FOUND_DEFAULT);
        setEarlyFlushEnabled(INIT_PARAM_EARLY_FLUSH_ENABLED_DEFAULT);
        setStrictJsf2FaceletsCompatibility(INIT_PARAM_STRICT_JSF_2_FACELETS_COMPATIBILITY_DEFAULT);
        setRenderFormViewStateAtBegin(INIT_PARAM_RENDER_FORM_VIEW_STATE_AT_BEGIN_DEFAULT);
        setFlashScopeDisabled(INIT_PARAM_FLASH_SCOPE_DISABLED_DEFAULT);
        setNumberOfViewsInSession(INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT);
        setNumberOfSequentialViewsInSession(INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT);
        setNumberOfFlashTokensInSession(
                (INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT / 
                        INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT)+1);
        setSupportEL3ImportHandler(SUPPORT_EL_3_IMPORT_HANDLER_DEFAULT);                        
    }

    private static MyfacesConfig createAndInitializeMyFacesConfig(ExternalContext extCtx)
    {
        
        MyfacesConfig myfacesConfig = new MyfacesConfig();

        myfacesConfig.setPrettyHtml(getBooleanInitParameter(extCtx, INIT_PARAM_PRETTY_HTML,
                                                            INIT_PARAM_PRETTY_HTML_DEFAULT));
        myfacesConfig.setAllowJavascript(getBooleanInitParameter(extCtx, INIT_PARAM_ALLOW_JAVASCRIPT,
                                                                 INIT_PARAM_ALLOW_JAVASCRIPT_DEFAULT));

        myfacesConfig.setRenderClearJavascriptOnButton(getBooleanInitParameter(extCtx, 
                                                            INIT_PARAM_RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON,
                                                            INIT_PARAM_RENDER_CLEAR_JAVASCRIPT_FOR_BUTTON_DEFAULT));

        myfacesConfig.setRenderHiddenFieldsForLinkParams(getBooleanInitParameter(extCtx, 
                INIT_PARAM_RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS,
                INIT_PARAM_RENDER_HIDDEN_FIELDS_FOR_LINK_PARAMS_DEFAULT));

        myfacesConfig.setSaveFormSubmitLinkIE(getBooleanInitParameter(extCtx, INIT_PARAM_SAVE_FORM_SUBMIT_LINK_IE,
                                                            INIT_PARAM_SAVE_FORM_SUBMIT_LINK_IE_DEFAULT));
        
        myfacesConfig.setReadonlyAsDisabledForSelect(getBooleanInitParameter(extCtx, 
                                                                 INIT_READONLY_AS_DISABLED_FOR_SELECT,
                                                                 INIT_READONLY_AS_DISABLED_FOR_SELECT_DEFAULT));
        myfacesConfig.setRenderViewStateId(getBooleanInitParameter(extCtx, INIT_PARAM_RENDER_VIEWSTATE_ID,
                                                                   INIT_PARAM_RENDER_VIEWSTATE_ID_DEFAULT));
        myfacesConfig.setStrictXhtmlLinks(getBooleanInitParameter(extCtx, INIT_PARAM_STRICT_XHTML_LINKS,
                                                                  INIT_PARAM_STRICT_XHTML_LINKS_DEFAULT));
        myfacesConfig.setRenderFormSubmitScriptInline(getBooleanInitParameter(extCtx,
                                                                  INIT_PARAM_RENDER_FORM_SUBMIT_SCRIPT_INLINE,
                                                                  INIT_PARAM_RENDER_FORM_SUBMIT_SCRIPT_INLINE_DEFAULT));
        
        myfacesConfig.setConfigRefreshPeriod(getLongInitParameter(extCtx, INIT_PARAM_CONFIG_REFRESH_PERIOD,
                INIT_PARAM_CONFIG_REFRESH_PERIOD_DEFAULT));

        myfacesConfig.setViewStateJavascript(getBooleanInitParameter(extCtx, INIT_PARAM_VIEWSTATE_JAVASCRIPT,
                INIT_PARAM_VIEWSTATE_JAVASCRIPT_DEFAULT));

        myfacesConfig.setDelegateFacesServlet(extCtx.getInitParameter(INIT_PARAM_DELEGATE_FACES_SERVLET));
        
        String refreshTransientBuildOnPSS = getStringInitParameter(extCtx, 
                INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS, 
                INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS_DEFAULT);
        
        if (refreshTransientBuildOnPSS == null)
        {
            myfacesConfig.setRefreshTransientBuildOnPSS(false);
            myfacesConfig.setRefreshTransientBuildOnPSSAuto(false);
        }
        else if ("auto".equalsIgnoreCase(refreshTransientBuildOnPSS))
        {
            myfacesConfig.setRefreshTransientBuildOnPSS(true);
            myfacesConfig.setRefreshTransientBuildOnPSSAuto(true);
        }
        else if (refreshTransientBuildOnPSS.equalsIgnoreCase("true") || 
                refreshTransientBuildOnPSS.equalsIgnoreCase("on") || 
                refreshTransientBuildOnPSS.equalsIgnoreCase("yes"))
        {
            myfacesConfig.setRefreshTransientBuildOnPSS(true);
            myfacesConfig.setRefreshTransientBuildOnPSSAuto(false);
        }
        else
        {
            myfacesConfig.setRefreshTransientBuildOnPSS(false);
            myfacesConfig.setRefreshTransientBuildOnPSSAuto(false);
        }
        
        myfacesConfig.setRefreshTransientBuildOnPSSPreserveState(getBooleanInitParameter(extCtx,
                INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE, 
                INIT_PARAM_REFRESH_TRANSIENT_BUILD_ON_PSS_PRESERVE_STATE_DEFAULT));
        
        myfacesConfig.setValidateXML(getBooleanInitParameter(extCtx, INIT_PARAM_VALIDATE_XML, 
                INIT_PARAM_VALIDATE_XML_DEFAULT));
        
        myfacesConfig.setWrapScriptContentWithXmlCommentTag(getBooleanInitParameter(extCtx, 
                INIT_PARAM_WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG, 
                INIT_PARAM_WRAP_SCRIPT_CONTENT_WITH_XML_COMMENT_TAG_DEFAULT));
        
        myfacesConfig.setDebugPhaseListenerEnabled(getBooleanInitParameter(extCtx, INIT_PARAM_DEBUG_PHASE_LISTENER,
                INIT_PARAM_DEBUG_PHASE_LISTENER_DEFAULT));
        
        myfacesConfig.setStrictJsf2RefreshTargetAjax(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                INIT_PARAM_STRICT_JSF_2_REFRESH_TARGET_AJAX, INIT_PARAM_STRICT_JSF_2_REFRESH_TARGET_AJAX_DEFAULT));
        
        myfacesConfig.setStrictJsf2CCELResolver(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                INIT_PARAM_STRICT_JSF_2_CC_EL_RESOLVER, INIT_PARAM_STRICT_JSF_2_CC_EL_RESOLVER_DEFAULT));
        
        myfacesConfig.setDefaultResponseWriterContentTypeMode(WebConfigParamUtils.getStringInitParameter(
                extCtx, INIT_PARAM_DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE,
                INIT_PARAM_DEFAULT_RESPONSE_WRITER_CONTENT_TYPE_MODE_DEFAULT));

        myfacesConfig.setViewUniqueIdsCacheEnabled(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                INIT_PARAM_VIEW_UNIQUE_IDS_CACHE_ENABLED, INIT_PARAM_VIEW_UNIQUE_IDS_CACHE_ENABLED_DEFAULT));
        myfacesConfig.setComponentUniqueIdsCacheSize(
                WebConfigParamUtils.getIntegerInitParameter(extCtx,
                INIT_PARAM_COMPONENT_UNIQUE_IDS_CACHE_SIZE, 
                INIT_PARAM_COMPONENT_UNIQUE_IDS_CACHE_SIZE_DEFAULT));
        myfacesConfig.setSupportJSPAndFacesEL(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                INIT_PARAM_SUPPORT_JSP_AND_FACES_EL, INIT_PARAM_SUPPORT_JSP_AND_FACES_EL_DEFAULT));
        
        myfacesConfig.setSupportManagedBeans(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                INIT_PARAM_SUPPORT_MANAGED_BEANS, INIT_PARAM_SUPPORT_MANAGED_BEANS_DEFAULT));
        
        myfacesConfig.setGaeJsfJarFiles(WebConfigParamUtils.getStringInitParameter(extCtx, 
                INIT_PARAM_GAE_JSF_JAR_FILES, INIT_PARAM_GAE_JSF_JAR_FILES_DEFAULT));
        myfacesConfig.setGaeJsfAnnotationsJarFiles(WebConfigParamUtils.getStringInitParameter(extCtx, 
                INIT_PARAM_GAE_JSF_ANNOTATIONS_JAR_FILES, INIT_PARAM_GAE_JSF_ANNOTATIONS_JAR_FILES_DEFAULT));

        myfacesConfig.setStrictJsf2ViewNotFound(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                INIT_PARAM_STRICT_JSF_2_VIEW_NOT_FOUND, INIT_PARAM_STRICT_JSF_2_VIEW_NOT_FOUND_DEFAULT));
        
        myfacesConfig.setEarlyFlushEnabled(WebConfigParamUtils.getBooleanInitParameter(extCtx,
                INIT_PARAM_EARLY_FLUSH_ENABLED, INIT_PARAM_EARLY_FLUSH_ENABLED_DEFAULT));


        myfacesConfig.setStrictJsf2FaceletsCompatibility(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                INIT_PARAM_STRICT_JSF_2_FACELETS_COMPATIBILITY, 
                INIT_PARAM_STRICT_JSF_2_FACELETS_COMPATIBILITY_DEFAULT));
        
        myfacesConfig.setRenderFormViewStateAtBegin(WebConfigParamUtils.getBooleanInitParameter(extCtx,
                INIT_PARAM_RENDER_FORM_VIEW_STATE_AT_BEGIN,
                INIT_PARAM_RENDER_FORM_VIEW_STATE_AT_BEGIN_DEFAULT));
        
        myfacesConfig.setFlashScopeDisabled(WebConfigParamUtils.getBooleanInitParameter(extCtx,
                INIT_PARAM_FLASH_SCOPE_DISABLED,
                INIT_PARAM_FLASH_SCOPE_DISABLED_DEFAULT));
        
        try
        {
            myfacesConfig.setNumberOfSequentialViewsInSession(WebConfigParamUtils.getIntegerInitParameter(
                    extCtx, 
                    INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION,
                    INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT));
            Integer views = myfacesConfig.getNumberOfSequentialViewsInSession();
            if (views == null || views < 0)
            {
                Logger.getLogger(MyfacesConfig.class.getName()).severe(
                        "Configured value for " + INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION
                          + " is not valid, must be an value >= 0, using default value ("
                          + INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT);
                views = INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT;
            }
        }
        catch (Throwable e)
        {
            Logger.getLogger(MyfacesConfig.class.getName()).log(Level.SEVERE, "Error determining the value for "
                   + INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION
                   + ", expected an integer value > 0, using default value ("
                   + INIT_PARAM_NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION_DEFAULT + "): " + e.getMessage(), e);
        }        
        try
        {
            myfacesConfig.setNumberOfViewsInSession(WebConfigParamUtils.getIntegerInitParameter(
                        extCtx, 
                        INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION,
                        INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT));
            Integer views = myfacesConfig.getNumberOfViewsInSession();
            if (views == null || views <= 0)
            {
                Logger.getLogger(MyfacesConfig.class.getName()).severe(
                        "Configured value for " + INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION
                          + " is not valid, must be an value > 0, using default value ("
                          + INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT);
                views = INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT;
            }
        }
        catch (Throwable e)
        {
            Logger.getLogger(MyfacesConfig.class.getName()).log(Level.SEVERE, "Error determining the value for "
                   + INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION
                   + ", expected an integer value > 0, using default value ("
                   + INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT + "): " + e.getMessage(), e);
        }

        Integer numberOfFlashTokensInSessionDefault;
        Integer i = myfacesConfig.getNumberOfSequentialViewsInSession();
        int j = myfacesConfig.getNumberOfViewsInSession();
        if (i != null && i.intValue() > 0)
        {
            numberOfFlashTokensInSessionDefault = (j / i.intValue()) + 1;
        }
        else
        {
            numberOfFlashTokensInSessionDefault = j + 1;
        }
        myfacesConfig.setNumberOfFlashTokensInSession(WebConfigParamUtils.getIntegerInitParameter(
                        extCtx, 
                        INIT_PARAM_NUMBER_OF_FLASH_TOKENS_IN_SESSION, numberOfFlashTokensInSessionDefault));
        myfacesConfig.setNumberOfFacesFlowClientWindowIdsInSession(WebConfigParamUtils.getIntegerInitParameter(
                        extCtx, 
                        INIT_PARAM_NUMBER_OF_FACES_FLOW_CLIENT_WINDOW_IDS_IN_SESSION, 
                        numberOfFlashTokensInSessionDefault));
                        
        myfacesConfig.setSupportEL3ImportHandler(WebConfigParamUtils.getBooleanInitParameter(extCtx, 
                       SUPPORT_EL_3_IMPORT_HANDLER, 
                       SUPPORT_EL_3_IMPORT_HANDLER_DEFAULT));                        
        
        if (TOMAHAWK_AVAILABLE)
        {
            myfacesConfig.setDetectJavascript(getBooleanInitParameter(extCtx, INIT_PARAM_DETECT_JAVASCRIPT,
                    INIT_PARAM_DETECT_JAVASCRIPT_DEFAULT));
            myfacesConfig.setAutoScroll(getBooleanInitParameter(extCtx, INIT_PARAM_AUTO_SCROLL,
                    INIT_PARAM_AUTO_SCROLL_DEFAULT));
                        
            myfacesConfig.setAddResourceClass(getStringInitParameter(extCtx, INIT_PARAM_ADD_RESOURCE_CLASS,
                    INIT_PARAM_ADD_RESOURCE_CLASS_DEFAULT));
            myfacesConfig.setResourceVirtualPath(getStringInitParameter(extCtx, INIT_PARAM_RESOURCE_VIRTUAL_PATH,
                    INIT_PARAM_RESOURCE_VIRTUAL_PATH_DEFAULT));

            myfacesConfig.setCheckExtensionsFilter(getBooleanInitParameter(extCtx, INIT_CHECK_EXTENSIONS_FILTER,
                    INIT_CHECK_EXTENSIONS_FILTER_DEFAULT));
            /*
            if(RI_IMPL_AVAILABLE)
            {
                if(log.isLoggable(Level.INFO))
                {
                    log.info("Starting up Tomahawk on the RI-JSF-Implementation.");
                }
            }

            if(MYFACES_IMPL_AVAILABLE)
            {
                if(log.isLoggable(Level.INFO))
                {
                    log.info("Starting up Tomahawk on the MyFaces-JSF-Implementation");
                }
            }*/
        }
        /*
        else
        {
            if (log.isLoggable(Level.INFO))
            {
                log.info("Tomahawk jar not available. Autoscrolling, DetectJavascript, "+
                "AddResourceClass and CheckExtensionsFilter are disabled now.");
            }
        }*/

        /*
        if(RI_IMPL_AVAILABLE && MYFACES_IMPL_AVAILABLE)
        {
            log.severe("Both MyFaces and the RI are on your classpath. Please make sure to"+
            " use only one of the two JSF-implementations.");
        }*/
        return myfacesConfig;
    }

    private static boolean getBooleanInitParameter(ExternalContext externalContext,
                                                   String paramName,
                                                   boolean defaultValue)
    {
        String strValue = externalContext.getInitParameter(paramName);
        if (strValue == null)
        {
            //if (log.isLoggable(Level.INFO)) log.info("No context init parameter '" + 
            // paramName + "' found, using default value " + defaultValue);
            return defaultValue;
        }
        else if (strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("on") || 
                strValue.equalsIgnoreCase("yes"))
        {
            return true;
        }
        else if (strValue.equalsIgnoreCase("false") || strValue.equalsIgnoreCase("off") || 
                strValue.equalsIgnoreCase("no"))
        {
            return false;
        }
        else
        {
            //if (log.isLoggable(Level.WARNING)) log.warning("Wrong context init parameter '" + 
            //paramName + "' (='" + strValue + "'), using default value " + defaultValue);
            return defaultValue;
        }
    }

    private static String getStringInitParameter(ExternalContext externalContext,
                                                 String paramName,
                                                 String defaultValue)
    {
        String strValue = externalContext.getInitParameter(paramName);
        if (strValue == null)
        {
            //if (log.isLoggable(Level.INFO)) log.info("No context init parameter '" + paramName +
            //"' found, using default value " + defaultValue); //defaultValue==null should not be 
            //a problem here
            return defaultValue;
        }
        
        return strValue;
    }

    private static long getLongInitParameter(ExternalContext externalContext,
                                                  String paramName,
                                                  long defaultValue)
    {
       String strValue = externalContext.getInitParameter(paramName);
       if (strValue == null)
       {
           //if (log.isLoggable(Level.INFO)) log.info("No context init parameter '" +paramName +
           //"' found, using default value " +defaultValue);
           return defaultValue;
       }
       else
       {
           try
           {
               return Long.parseLong(strValue);
           }
           catch (NumberFormatException e)
           {
               //if (log.isLoggable(Level.WARNING)) log.warning("Wrong context init parameter '" +
               //paramName + "' (='" + strValue + "'), using default value " + defaultValue);
           }
           return defaultValue;
       }
    }
        
     private void setResourceVirtualPath( String resourceVirtualPath )
     {
         this._resourceVirtualPath = resourceVirtualPath;
    }

     public String getResourceVirtualPath()
     {
         return this._resourceVirtualPath;
     }

    public boolean isPrettyHtml()
    {
        return _prettyHtml;
    }

    private void setPrettyHtml(boolean prettyHtml)
    {
        _prettyHtml = prettyHtml;
    }

    public boolean isDetectJavascript()
    {
        return _detectJavascript;
    }

    private void setDetectJavascript(boolean detectJavascript)
    {
        _detectJavascript = detectJavascript;
    }

    private void setReadonlyAsDisabledForSelect(boolean readonlyAsDisabledForSelect)
    {
        _readonlyAsDisabledForSelect = readonlyAsDisabledForSelect;
    }

    public boolean isReadonlyAsDisabledForSelect()
    {
        return _readonlyAsDisabledForSelect;
    }


   public long getConfigRefreshPeriod()
   {
       return _configRefreshPeriod;
   }

   public void setConfigRefreshPeriod(long configRefreshPeriod)
   {
       _configRefreshPeriod = configRefreshPeriod;
   }

    /**
     * JSF API 1.2 defines a "javax.faces.ViewState" client parameter, that must be rendered as both the "name"
     * and the "id" attribute of the hidden input that is rendered for the purpose of state saving
     * (see ResponseStateManager.VIEW_STATE_PARAM).
     * Actually this causes duplicate id attributes and thus invalid XHTML pages when multiple forms are rendered on
     * one page. With the {@link #INIT_PARAM_RENDER_VIEWSTATE_ID} context parameter you can tune this behaviour.
     * <p>Set it to:</p>
     * <ul><li>true - to render JSF 1.2 compliant id attributes (that might cause invalid XHTML), or</li>
     * <li>false - to omit rendering of the id attribute (which is only needed for very special AJAX/Javascript 
     * components)</li></ul>
     * <p>Default value is: true (for backwards compatibility and JSF 1.2 compliancy) </p>
     * @return true, if the client state hidden input "javax.faces.ViewState" id attribute should be rendered
     */
    public boolean isRenderViewStateId()
    {
        return _renderViewStateId;
    }

    public void setRenderViewStateId(boolean renderViewStateId)
    {
        _renderViewStateId = renderViewStateId;
    }

    /**
     * <p>W3C recommends to use the "&amp;amp;" entity instead of a plain "&amp;" character within HTML.
     * This also applies to attribute values and thus to the "href" attribute of &lt;a&gt; elements as well.
     * Even more, when XHTML is used as output the usage of plain "&amp;" characters is forbidden and would lead to
     * invalid XML code.
     * Therefore, since version 1.1.6 MyFaces renders the correct "&amp;amp;" entity for links.</p>
     * <p>The init parameter
     * {@link #INIT_PARAM_STRICT_XHTML_LINKS} makes it possible to restore the old behaviour and to make MyFaces
     * "bug compatible" to the Sun RI which renders plain "&amp;" chars in links as well.</p>
     * @see <a href="http://www.w3.org/TR/html401/charset.html#h-5.3.2">HTML 4.01 Specification</a>
     * @see <a href="http://issues.apache.org/jira/browse/MYFACES-1774">Jira: MYFACES-1774</a>
     * @return true if ampersand characters ("&amp;") should be correctly rendered as "&amp;amp;" entities 
     *         within link urls (=default), false for old (XHTML incompatible) behaviour
     */
    public boolean isStrictXhtmlLinks()
    {
        return _strictXhtmlLinks;
    }

    public void setStrictXhtmlLinks(boolean strictXhtmlLinks)
    {
        _strictXhtmlLinks = strictXhtmlLinks;
    }

    public boolean isTomahawkAvailable()
    {
        return TOMAHAWK_AVAILABLE;
    }

    public boolean isMyfacesImplAvailable()
    {
        return MYFACES_IMPL_AVAILABLE;
    }

    public boolean isRiImplAvailable()
    {
        return RI_IMPL_AVAILABLE;
    }

    /**
     * @deprecated 
     */
    public boolean isAllowJavascript()
    {
        return _allowJavascript;
    }

    private void setAllowJavascript(boolean allowJavascript)
    {
        _allowJavascript = allowJavascript;
    }

    public boolean isAutoScroll()
    {
        return _autoScroll;
    }

    private void setAutoScroll(boolean autoScroll)
    {
        _autoScroll = autoScroll;
    }

    private void setAddResourceClass(String addResourceClass)
    {
        _addResourceClass = addResourceClass;
    }

    public String getAddResourceClass()
    {
        return _addResourceClass;
    }

    /**
     * ExtensionFilter needs access to AddResourceClass init param without having
     * an ExternalContext at hand.
     */
    public static String getAddResourceClassFromServletContext(ServletContext servletContext)
    {
        String addResourceClass = servletContext.getInitParameter(INIT_PARAM_ADD_RESOURCE_CLASS);

        return addResourceClass == null ? INIT_PARAM_ADD_RESOURCE_CLASS_DEFAULT : addResourceClass;
    }

    /**
     * Should the environment be checked so that the ExtensionsFilter will work properly. 
     */
    public boolean isCheckExtensionsFilter()
    {
        return _checkExtensionsFilter;
    }

    public void setCheckExtensionsFilter(boolean extensionsFilter)
    {
        _checkExtensionsFilter = extensionsFilter;
    }

    /**
     * 
     */
    public boolean isViewStateJavascript()
    {
        return _viewStateJavascript;
    }

    private void setViewStateJavascript(boolean viewStateJavascript)
    {
        _viewStateJavascript = viewStateJavascript;
    }

    public void setRenderClearJavascriptOnButton(
            boolean renderClearJavascriptOnButton)
    {
        _renderClearJavascriptOnButton = renderClearJavascriptOnButton;
    }

    /**
     * This param renders the clear javascript on button necessary only for
     * compatibility with hidden fields feature of myfaces. This is done 
     * because jsf ri does not render javascript on onclick method for button,
     * so myfaces should do this.
     * 
     * @return
     */
    public boolean isRenderClearJavascriptOnButton()
    {
        return _renderClearJavascriptOnButton;
    }

    public boolean isRenderHiddenFieldsForLinkParams()
    {
        return renderHiddenFieldsForLinkParams;
    }

    public void setRenderHiddenFieldsForLinkParams(
            boolean renderHiddenFieldsForLinkParams)
    {
        this.renderHiddenFieldsForLinkParams = renderHiddenFieldsForLinkParams;
    }

    public void setSaveFormSubmitLinkIE(boolean saveFormSubmitLinkIE)
    {
        _saveFormSubmitLinkIE = saveFormSubmitLinkIE;
    }

    /**
     * Add a code that save the form when submit a form using a
     * link. It's a bug on IE.
     * 
     * @return
     */
    public boolean isSaveFormSubmitLinkIE()
    {
        return _saveFormSubmitLinkIE;
    }
    
    public String getDelegateFacesServlet()
    {
        return _delegateFacesServlet;
    }
    
    public void setDelegateFacesServlet(String delegateFacesServlet)
    {
        _delegateFacesServlet = delegateFacesServlet;
    }

    public boolean isRefreshTransientBuildOnPSS()
    {
        return _refreshTransientBuildOnPSS;
    }

    public void setRefreshTransientBuildOnPSS(boolean refreshTransientBuildOnPSS)
    {
        this._refreshTransientBuildOnPSS = refreshTransientBuildOnPSS;
    }

    public boolean isRefreshTransientBuildOnPSSAuto()
    {
        return _refreshTransientBuildOnPSSAuto;
    }

    public void setRefreshTransientBuildOnPSSAuto(
            boolean refreshTransientBuildOnPSSAuto)
    {
        this._refreshTransientBuildOnPSSAuto = refreshTransientBuildOnPSSAuto;
    }

    public boolean isRefreshTransientBuildOnPSSPreserveState()
    {
        return refreshTransientBuildOnPSSPreserveState;
    }

    public void setRefreshTransientBuildOnPSSPreserveState(
            boolean refreshTransientBuildOnPSSPreserveState)
    {
        this.refreshTransientBuildOnPSSPreserveState = refreshTransientBuildOnPSSPreserveState;
    }
    
    public boolean isValidateXML()
    {
        return _validateXML;
    }

    public void setValidateXML(boolean validateXML)
    {
        _validateXML = validateXML;
    }

    public boolean isWrapScriptContentWithXmlCommentTag()
    {
        return _wrapScriptContentWithXmlCommentTag;
    }

    public void setWrapScriptContentWithXmlCommentTag(
            boolean wrapScriptContentWithXmlCommentTag)
    {
        this._wrapScriptContentWithXmlCommentTag = wrapScriptContentWithXmlCommentTag;
    }

    public boolean isRenderFormSubmitScriptInline()
    {
        return _renderFormSubmitScriptInline;
    }

    public void setRenderFormSubmitScriptInline(
            boolean renderFormSubmitScriptInline)
    {
        _renderFormSubmitScriptInline = renderFormSubmitScriptInline;
    }

    public boolean isDebugPhaseListenerEnabled()
    {
        return _debugPhaseListenerEnabled;
    }

    public void setDebugPhaseListenerEnabled(boolean debugPhaseListener)
    {
        this._debugPhaseListenerEnabled = debugPhaseListener;
    }

    public boolean isStrictJsf2RefreshTargetAjax()
    {
        return _strictJsf2RefreshTargetAjax;
    }

    public void setStrictJsf2RefreshTargetAjax(boolean strictJsf2RefreshTargetAjax)
    {
        this._strictJsf2RefreshTargetAjax = strictJsf2RefreshTargetAjax;
    }

    public boolean isStrictJsf2CCELResolver()
    {
        return _strictJsf2CCELResolver;
    }

    public void setStrictJsf2CCELResolver(boolean strictJsf2CCELResolver)
    {
        this._strictJsf2CCELResolver = strictJsf2CCELResolver;
    }

    public String getDefaultResponseWriterContentTypeMode()
    {
        return _defaultResponseWriterContentTypeMode;
    }

    public void setDefaultResponseWriterContentTypeMode(
            String defaultResponseWriterContentTypeMode)
    {
        this._defaultResponseWriterContentTypeMode = defaultResponseWriterContentTypeMode;
    }

    public boolean isViewUniqueIdsCacheEnabled()
    {
        return _viewUniqueIdsCacheEnabled;
    }

    public void setViewUniqueIdsCacheEnabled(boolean viewUniqueIdsCacheEnabled)
    {
        _viewUniqueIdsCacheEnabled = viewUniqueIdsCacheEnabled;
    }

    public boolean isSupportJSPAndFacesEL()
    {
        return _supportJSPAndFacesEL;
    }

    public void setSupportJSPAndFacesEL(boolean supportJSPANDFacesEL)
    {
        _supportJSPAndFacesEL = supportJSPANDFacesEL;
    }

    public boolean isSupportManagedBeans()
    {
        return _supportManagedBeans;
    }

    public void setSupportManagedBeans(boolean supportManagedBeans)
    {
        _supportManagedBeans = supportManagedBeans;
    }
    
    public int getComponentUniqueIdsCacheSize()
    {
        return _componentUniqueIdsCacheSize;
    }

    public void setComponentUniqueIdsCacheSize(int componentUniqueIdsCacheSize)
    {
        this._componentUniqueIdsCacheSize = componentUniqueIdsCacheSize;
    }

    public String getGaeJsfJarFiles()
    {
        return _gaeJsfJarFiles;
    }

    public void setGaeJsfJarFiles(String gaeJsfJarFiles)
    {
        this._gaeJsfJarFiles = gaeJsfJarFiles;
    }

    public String getGaeJsfAnnotationsJarFiles()
    {
        return _gaeJsfAnnotationsJarFiles;
    }

    public void setGaeJsfAnnotationsJarFiles(String gaeJsfAnnotationsJarFiles)
    {
        this._gaeJsfAnnotationsJarFiles = gaeJsfAnnotationsJarFiles;
    }

    public boolean isStrictJsf2ViewNotFound()
    {
        return _strictJsf2ViewNotFound;
    }

    public void setStrictJsf2ViewNotFound(boolean strictJsf2ViewNotFound)
    {
        this._strictJsf2ViewNotFound = strictJsf2ViewNotFound;
    }

    public boolean isEarlyFlushEnabled()
    {
        return _earlyFlushEnabled;
    }

    public void setEarlyFlushEnabled(boolean earlyFlushEnabled)
    {
        this._earlyFlushEnabled = earlyFlushEnabled;
    }

    public boolean isStrictJsf2FaceletsCompatibility()
    {
        return _strictJsf2FaceletsCompatibility;
    }

    public void setStrictJsf2FaceletsCompatibility(boolean strictJsf2FaceletsCompatibility)
    {
        this._strictJsf2FaceletsCompatibility = strictJsf2FaceletsCompatibility;
    }

    public boolean isRenderFormViewStateAtBegin()
    {
        return _renderFormViewStateAtBegin;
    }

    public void setRenderFormViewStateAtBegin(boolean renderFormViewStateAtBegin)
    {
        this._renderFormViewStateAtBegin = renderFormViewStateAtBegin;
    }

    public boolean isFlashScopeDisabled()
    {
        return _flashScopeDisabled;
    }

    public void setFlashScopeDisabled(boolean flashScopeDisabled)
    {
        this._flashScopeDisabled = flashScopeDisabled;
    }

    /**
     * @return the _numberOfViewsInSession
     */
    public Integer getNumberOfViewsInSession()
    {
        return _numberOfViewsInSession;
    }

    /**
     * @param numberOfViewsInSession the _numberOfViewsInSession to set
     */
    public void setNumberOfViewsInSession(Integer numberOfViewsInSession)
    {
        this._numberOfViewsInSession = numberOfViewsInSession;
    }

    /**
     * @return the _numberOfSequentialViewsInSession
     */
    public Integer getNumberOfSequentialViewsInSession()
    {
        return _numberOfSequentialViewsInSession;
    }

    /**
     * @param numberOfSequentialViewsInSession the _numberOfSequentialViewsInSession to set
     */
    public void setNumberOfSequentialViewsInSession(Integer numberOfSequentialViewsInSession)
    {
        this._numberOfSequentialViewsInSession = numberOfSequentialViewsInSession;
    }

    /**
     * @return the _numberOfFlashTokensInSession
     */
    public Integer getNumberOfFlashTokensInSession()
    {
        return _numberOfFlashTokensInSession;
    }

    /**
     * @param numberOfFlashTokensInSession the _numberOfFlashTokensInSession to set
     */
    public void setNumberOfFlashTokensInSession(Integer numberOfFlashTokensInSession)
    {
        this._numberOfFlashTokensInSession = numberOfFlashTokensInSession;
    }

    /**
     * @return the _numberOfFacesFlowClientWindowIdsInSession
     */
    public Integer getNumberOfFacesFlowClientWindowIdsInSession()
    {
        return _numberOfFacesFlowClientWindowIdsInSession;
    }

    /**
     * @param numberOfFacesFlowClientWindowIdsInSession the _numberOfFacesFlowClientWindowIdsInSession to set
     */
    public void setNumberOfFacesFlowClientWindowIdsInSession(Integer numberOfFacesFlowClientWindowIdsInSession)
    {
        this._numberOfFacesFlowClientWindowIdsInSession = numberOfFacesFlowClientWindowIdsInSession;
    }
    
    /**
     * @return the _supportEL3ImportHandler
     */
    public boolean isSupportEL3ImportHandler()
    {
        return _supportEL3ImportHandler;
    }
    
    /**
     * @param supportEL3ImportHandler the _supportEL3ImportHandler to set
     */
    public void setSupportEL3ImportHandler(boolean supportEL3ImportHandler)
    {
        this._supportEL3ImportHandler = supportEL3ImportHandler;
    }
}
