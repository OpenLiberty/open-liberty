/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.webext;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.commonext.ResourceRef;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDConstants;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDVersion;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIIgnoredAttributes;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIVersionAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyModule;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;web-ext>.
 */
@DDRootElement(name = "web-ext",
               versions = {
                            @DDVersion(versionString = "1.0", version = 10, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
                            @DDVersion(versionString = "1.1", version = 11, namespace = DDConstants.WEBSPHERE_EE_NS_URI),
               })
@DDIdAttribute
@DDXMIRootElement(name = "WebAppExtension",
                  namespace = "webappext.xmi",
                  version = 9,
                  primaryDDType = WebApp.class,
                  primaryDDVersions = { "2.2", "2.3", "2.4" },
                  refElementName = "webApp")
@DDXMIIgnoredAttributes({
                          @DDXMIIgnoredAttribute(name = "additionalClassPath", type = DDAttributeType.String, nillable = true),
                          @DDXMIIgnoredAttribute(name = "metadataComplete", type = DDAttributeType.Boolean, nillable = true)
})
@LibertyModule
public interface WebExt extends DeploymentDescriptor {

    static final String XML_EXT_NAME = "WEB-INF/ibm-web-ext.xml";
    static final String XMI_EXT_NAME = "WEB-INF/ibm-web-ext.xmi";

    /**
     * @return version="..." attribute value
     */
    @DDAttribute(name = "version", type = DDAttributeType.String)
    @DDXMIVersionAttribute
    @LibertyNotInUse
    String getVersion();

    /**
     * @return &lt;servlet> as a read-only list
     */
    @LibertyNotInUse
    @DDElement(name = "servlet")
    @DDXMIElement(name = "extendedServlets")
    List<ServletExtension> getServletExtensions();

    /**
     * @return &lt;default-error-page uri="...">, or null if unspecified
     */
    @DDAttribute(name = "uri", elementName = "default-error-page", type = DDAttributeType.String)
    @DDXMIAttribute(name = "defaultErrorPage", nillable = true)
    String getDefaultErrorPage();

    /**
     * @return &lt;file-serving-attribute> as a read-only list
     */
    @DDElement(name = "file-serving-attribute")
    @DDXMIElement(name = "fileServingAttributes")
    List<Attribute> getFileServingAttributes();

    /**
     * @return &lt;invoker-attribute> as a read-only list
     */
    @DDElement(name = "invoker-attribute")
    @DDXMIElement(name = "invokerAttributes")
    List<Attribute> getInvokerAttributes();

    /**
     * @return &lt;jsp-attribute> as a read-only list
     */
    @DDElement(name = "jsp-attribute")
    @DDXMIElement(name = "jspAttributes")
    List<Attribute> getJspAttributes();

    /**
     * @return &lt;mime-filter> as a read-only list
     */
    @DDElement(name = "mime-filter")
    @DDXMIElement(name = "mimeFilters")
    List<MimeFilter> getMimeFilters();

    /**
     * @return true if &lt;reload-interval value="..."/> is specified
     * @see #getReloadInterval
     */
    boolean isSetReloadInterval();

    /**
     * @return &lt;reload-interval value="..."/> if specified
     * @see #isSetReloadInterval
     */
    @LibertyNotInUse
    @DDAttribute(name = "value", elementName = "reload-interval", type = DDAttributeType.Int)
    @DDXMIAttribute(name = "reloadInterval")
    int getReloadInterval();

    /**
     * @return true if &lt;context-root uri="..."> is specified
     * @see #getContextRoot()
     */
    boolean isSetContextRoot();

    /**
     * @return &lt;context-root uri="...">, or null if unspecified
     * @see #isSetContextRoot
     */
    @DDAttribute(name = "uri", elementName = "context-root", type = DDAttributeType.String)
    @DDXMIAttribute(name = "contextRoot")
    String getContextRoot();

    /**
     * @return &lt;resource-ref> as a read-only list
     */
    @DDElement(name = "resource-ref")
    @DDXMIElement(name = "resourceRefExtensions")
    List<ResourceRef> getResourceRefs();

    /**
     * @return &lt;servlet-cache-config> as a read-only list
     */
    @LibertyNotInUse
    @DDElement(name = "servlet-cache-config")
    @DDXMIElement(name = "servletCacheConfigs")
    List<ServletCacheConfig> getServletCacheConfigs();

    /**
     * @return true if &lt;autoload-filters value="..."/> is specified
     * @see #isAutoloadFilters
     */
    boolean isSetAutoloadFilters();

    /**
     * @return &lt;autoload-filters value="..."/> if specified
     * @see #isSetAutoloadFilters
     */
    @DDAttribute(name = "value", elementName = "autoload-filters", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "autoLoadFilters")
    boolean isAutoloadFilters();

    /**
     * @return true if &lt;auto-encode-requests value="..."/> is specified
     * @see #isAutoEncodeRequests
     */
    boolean isSetAutoEncodeRequests();

    /**
     * @return &lt;auto-encode-requests value="..."/> if specified
     * @see #isSetAutoEncodeRequests
     */
    @DDAttribute(name = "value", elementName = "auto-encode-requests", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "autoRequestEncoding")
    boolean isAutoEncodeRequests();

    /**
     * @return true if &lt;auto-encode-responses value="..."/> is specified
     * @see #isAutoEncodeResponses
     */
    boolean isSetAutoEncodeResponses();

    /**
     * @return &lt;auto-encode-responses value="..."/> if specified
     * @see #isSetAutoEncodeResponses
     */
    @DDAttribute(name = "value", elementName = "auto-encode-responses", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "autoResponseEncoding")
    boolean isAutoEncodeResponses();

    /**
     * @return true if &lt;enable-directory-browsing value="..."/> is specified
     * @see #isEnableDirectoryBrowsing
     */
    boolean isSetEnableDirectoryBrowsing();

    /**
     * @return &lt;enable-directory-browsing value="..."/> if specified
     * @see #isSetEnableDirectoryBrowsing
     */
    @DDAttribute(name = "value", elementName = "enable-directory-browsing", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "directoryBrowsingEnabled")
    boolean isEnableDirectoryBrowsing();

    /**
     * @return true if &lt;enable-file-serving value="..."/> is specified
     * @see #isEnableFileServing
     */
    boolean isSetEnableFileServing();

    /**
     * @return &lt;enable-file-serving value="..."/> if specified
     * @see #isSetEnableFileServing
     */
    @DDAttribute(name = "value", elementName = "enable-file-serving", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "fileServingEnabled")
    boolean isEnableFileServing();

    /**
     * @return true if &lt;pre-compile-jsps value="..."/> is specified
     * @see #isPreCompileJsps
     */
    boolean isSetPreCompileJsps();

    /**
     * @return &lt;pre-compile-jsps value="..."/> if specified
     * @see #isSetPreCompileJsps
     */
    @DDAttribute(name = "value", elementName = "pre-compile-jsps", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "preCompileJSPs")
    boolean isPreCompileJsps();

    /**
     * @return true if &lt;enable-reloading value="..."/> is specified
     * @see #isEnableReloading
     */
    boolean isSetEnableReloading();

    /**
     * @return &lt;enable-reloading value="..."/> if specified
     * @see #isSetEnableReloading
     */
    @LibertyNotInUse
    @DDAttribute(name = "value", elementName = "enable-reloading", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "reloadingEnabled")
    boolean isEnableReloading();

    /**
     * @return true if &lt;enable-serving-servlets-by-class-name value="..."/> is specified
     * @see #isEnableServingServletsByClassName
     */
    boolean isSetEnableServingServletsByClassName();

    /**
     * @return &lt;enable-serving-servlets-by-class-name value="..."/> if specified
     * @see #isSetEnableServingServletsByClassName
     */
    @DDAttribute(name = "value", elementName = "enable-serving-servlets-by-class-name", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "serveServletsByClassnameEnabled")
    boolean isEnableServingServletsByClassName();
}
