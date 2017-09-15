/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.webext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class WebExtType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.webext.WebExt, DDParser.RootParsable {
    public WebExtType(String ddPath) {
        this(ddPath, false);
    }

    public WebExtType(String ddPath, boolean xmi) {
        this.xmi = xmi;
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    protected final boolean xmi;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType xmiRef;
    com.ibm.ws.javaee.ddmodel.StringType version;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.ServletExtensionType, com.ibm.ws.javaee.dd.webext.ServletExtension> servlet;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.AttributeType, com.ibm.ws.javaee.dd.webext.Attribute> file_serving_attribute;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.AttributeType, com.ibm.ws.javaee.dd.webext.Attribute> invoker_attribute;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.AttributeType, com.ibm.ws.javaee.dd.webext.Attribute> jsp_attribute;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.MimeFilterType, com.ibm.ws.javaee.dd.webext.MimeFilter> mime_filter;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType, com.ibm.ws.javaee.dd.commonext.ResourceRef> resource_ref;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.ServletCacheConfigType, com.ibm.ws.javaee.dd.webext.ServletCacheConfig> servlet_cache_config;
    com.ibm.ws.javaee.ddmodel.StringType default_error_page_uri;
    com.ibm.ws.javaee.ddmodel.IntegerType reload_interval_value;
    com.ibm.ws.javaee.ddmodel.StringType context_root_uri;
    com.ibm.ws.javaee.ddmodel.BooleanType autoload_filters_value;
    com.ibm.ws.javaee.ddmodel.BooleanType auto_encode_requests_value;
    com.ibm.ws.javaee.ddmodel.BooleanType auto_encode_responses_value;
    com.ibm.ws.javaee.ddmodel.BooleanType enable_directory_browsing_value;
    com.ibm.ws.javaee.ddmodel.BooleanType enable_file_serving_value;
    com.ibm.ws.javaee.ddmodel.BooleanType pre_compile_jsps_value;
    com.ibm.ws.javaee.ddmodel.BooleanType enable_reloading_value;
    com.ibm.ws.javaee.ddmodel.BooleanType enable_serving_servlets_by_class_name_value;
    com.ibm.ws.javaee.ddmodel.StringType additionalClassPath;
    com.ibm.ws.javaee.ddmodel.BooleanType metadataComplete;

    @Override
    public java.lang.String getVersion() {
        return xmi ? "XMI" : version != null ? version.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.ServletExtension> getServletExtensions() {
        if (servlet != null) {
            return servlet.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.lang.String getDefaultErrorPage() {
        return default_error_page_uri != null ? default_error_page_uri.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> getFileServingAttributes() {
        if (file_serving_attribute != null) {
            return file_serving_attribute.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> getInvokerAttributes() {
        if (invoker_attribute != null) {
            return invoker_attribute.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> getJspAttributes() {
        if (jsp_attribute != null) {
            return jsp_attribute.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.MimeFilter> getMimeFilters() {
        if (mime_filter != null) {
            return mime_filter.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isSetReloadInterval() {
        return reload_interval_value != null;
    }

    @Override
    public int getReloadInterval() {
        return reload_interval_value != null ? reload_interval_value.getIntValue() : 0;
    }

    @Override
    public boolean isSetContextRoot() {
        return context_root_uri != null;
    }

    @Override
    public java.lang.String getContextRoot() {
        return context_root_uri != null ? context_root_uri.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonext.ResourceRef> getResourceRefs() {
        if (resource_ref != null) {
            return resource_ref.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.ServletCacheConfig> getServletCacheConfigs() {
        if (servlet_cache_config != null) {
            return servlet_cache_config.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isSetAutoloadFilters() {
        return autoload_filters_value != null;
    }

    @Override
    public boolean isAutoloadFilters() {
        return autoload_filters_value != null ? autoload_filters_value.getBooleanValue() : false;
    }

    @Override
    public boolean isSetAutoEncodeRequests() {
        return auto_encode_requests_value != null;
    }

    @Override
    public boolean isAutoEncodeRequests() {
        return auto_encode_requests_value != null ? auto_encode_requests_value.getBooleanValue() : false;
    }

    @Override
    public boolean isSetAutoEncodeResponses() {
        return auto_encode_responses_value != null;
    }

    @Override
    public boolean isAutoEncodeResponses() {
        return auto_encode_responses_value != null ? auto_encode_responses_value.getBooleanValue() : false;
    }

    @Override
    public boolean isSetEnableDirectoryBrowsing() {
        return enable_directory_browsing_value != null;
    }

    @Override
    public boolean isEnableDirectoryBrowsing() {
        return enable_directory_browsing_value != null ? enable_directory_browsing_value.getBooleanValue() : false;
    }

    @Override
    public boolean isSetEnableFileServing() {
        return enable_file_serving_value != null;
    }

    @Override
    public boolean isEnableFileServing() {
        return enable_file_serving_value != null ? enable_file_serving_value.getBooleanValue() : false;
    }

    @Override
    public boolean isSetPreCompileJsps() {
        return pre_compile_jsps_value != null;
    }

    @Override
    public boolean isPreCompileJsps() {
        return pre_compile_jsps_value != null ? pre_compile_jsps_value.getBooleanValue() : false;
    }

    @Override
    public boolean isSetEnableReloading() {
        return enable_reloading_value != null;
    }

    @Override
    public boolean isEnableReloading() {
        return enable_reloading_value != null ? enable_reloading_value.getBooleanValue() : false;
    }

    @Override
    public boolean isSetEnableServingServletsByClassName() {
        return enable_serving_servlets_by_class_name_value != null;
    }

    @Override
    public boolean isEnableServingServletsByClassName() {
        return enable_serving_servlets_by_class_name_value != null ? enable_serving_servlets_by_class_name_value.getBooleanValue() : false;
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return deploymentDescriptorPath;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        this.idMap = parser.idMap;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "version".equals(localName)) {
                this.version = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "additionalClassPath".equals(localName)) {
                this.additionalClassPath = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "metadataComplete".equals(localName)) {
                this.metadataComplete = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "defaultErrorPage".equals(localName)) {
                this.default_error_page_uri = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "reloadInterval".equals(localName)) {
                this.reload_interval_value = parser.parseIntegerAttributeValue(index);
                return true;
            }
            if (xmi && "contextRoot".equals(localName)) {
                this.context_root_uri = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "autoLoadFilters".equals(localName)) {
                this.autoload_filters_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "autoRequestEncoding".equals(localName)) {
                this.auto_encode_requests_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "autoResponseEncoding".equals(localName)) {
                this.auto_encode_responses_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "directoryBrowsingEnabled".equals(localName)) {
                this.enable_directory_browsing_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "fileServingEnabled".equals(localName)) {
                this.enable_file_serving_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "preCompileJSPs".equals(localName)) {
                this.pre_compile_jsps_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "reloadingEnabled".equals(localName)) {
                this.enable_reloading_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "serveServletsByClassnameEnabled".equals(localName)) {
                this.enable_serving_servlets_by_class_name_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("version".equals(localName)) {
                // Allowed but ignored.
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "webApp".equals(localName)) {
            xmiRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("webApp", com.ibm.ws.javaee.dd.web.WebApp.class);
            parser.parse(xmiRef);
            return true;
        }
        if (xmi && "additionalClassPath".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType additionalClassPath = new com.ibm.ws.javaee.ddmodel.StringType();
            parser.parse(additionalClassPath);
            if (!additionalClassPath.isNil()) {
                this.additionalClassPath = additionalClassPath;
            }
            return true;
        }
        if (xmi && "metadataComplete".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType metadataComplete = new com.ibm.ws.javaee.ddmodel.BooleanType();
            parser.parse(metadataComplete);
            if (!metadataComplete.isNil()) {
                this.metadataComplete = metadataComplete;
            }
            return true;
        }
        if ((xmi ? "extendedServlets" : "servlet").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.ServletExtensionType servlet = new com.ibm.ws.javaee.ddmodel.webext.ServletExtensionType(xmi);
            parser.parse(servlet);
            this.addServlet(servlet);
            return true;
        }
        if ((xmi ? "fileServingAttributes" : "file-serving-attribute").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.AttributeType file_serving_attribute = new com.ibm.ws.javaee.ddmodel.webext.AttributeType(xmi);
            parser.parse(file_serving_attribute);
            this.addFileServingAttribute(file_serving_attribute);
            return true;
        }
        if ((xmi ? "invokerAttributes" : "invoker-attribute").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.AttributeType invoker_attribute = new com.ibm.ws.javaee.ddmodel.webext.AttributeType(xmi);
            parser.parse(invoker_attribute);
            this.addInvokerAttribute(invoker_attribute);
            return true;
        }
        if ((xmi ? "jspAttributes" : "jsp-attribute").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.AttributeType jsp_attribute = new com.ibm.ws.javaee.ddmodel.webext.AttributeType(xmi);
            parser.parse(jsp_attribute);
            this.addJspAttribute(jsp_attribute);
            return true;
        }
        if ((xmi ? "mimeFilters" : "mime-filter").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.MimeFilterType mime_filter = new com.ibm.ws.javaee.ddmodel.webext.MimeFilterType(xmi);
            parser.parse(mime_filter);
            this.addMimeFilter(mime_filter);
            return true;
        }
        if ((xmi ? "resourceRefExtensions" : "resource-ref").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType resource_ref = new com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType(xmi);
            parser.parse(resource_ref);
            this.addResourceRef(resource_ref);
            return true;
        }
        if ((xmi ? "servletCacheConfigs" : "servlet-cache-config").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.ServletCacheConfigType servlet_cache_config = new com.ibm.ws.javaee.ddmodel.webext.ServletCacheConfigType(xmi);
            parser.parse(servlet_cache_config);
            this.addServletCacheConfig(servlet_cache_config);
            return true;
        }
        if (!xmi && "default-error-page".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType default_error_page_uri = new com.ibm.ws.javaee.ddmodel.StringType();
            default_error_page_uri.obtainValueFromAttribute("uri");
            parser.parse(default_error_page_uri);
            this.default_error_page_uri = default_error_page_uri;
            return true;
        }
        if (xmi && "defaultErrorPage".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType default_error_page_uri = new com.ibm.ws.javaee.ddmodel.StringType();
            parser.parse(default_error_page_uri);
            if (!default_error_page_uri.isNil()) {
                this.default_error_page_uri = default_error_page_uri;
            }
            return true;
        }
        if (!xmi && "reload-interval".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.IntegerType reload_interval_value = new com.ibm.ws.javaee.ddmodel.IntegerType();
            reload_interval_value.obtainValueFromAttribute("value");
            parser.parse(reload_interval_value);
            this.reload_interval_value = reload_interval_value;
            return true;
        }
        if (!xmi && "context-root".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType context_root_uri = new com.ibm.ws.javaee.ddmodel.StringType();
            context_root_uri.obtainValueFromAttribute("uri");
            parser.parse(context_root_uri);
            this.context_root_uri = context_root_uri;
            return true;
        }
        if (!xmi && "autoload-filters".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType autoload_filters_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            autoload_filters_value.obtainValueFromAttribute("value");
            parser.parse(autoload_filters_value);
            this.autoload_filters_value = autoload_filters_value;
            return true;
        }
        if (!xmi && "auto-encode-requests".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType auto_encode_requests_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            auto_encode_requests_value.obtainValueFromAttribute("value");
            parser.parse(auto_encode_requests_value);
            this.auto_encode_requests_value = auto_encode_requests_value;
            return true;
        }
        if (!xmi && "auto-encode-responses".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType auto_encode_responses_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            auto_encode_responses_value.obtainValueFromAttribute("value");
            parser.parse(auto_encode_responses_value);
            this.auto_encode_responses_value = auto_encode_responses_value;
            return true;
        }
        if (!xmi && "enable-directory-browsing".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType enable_directory_browsing_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            enable_directory_browsing_value.obtainValueFromAttribute("value");
            parser.parse(enable_directory_browsing_value);
            this.enable_directory_browsing_value = enable_directory_browsing_value;
            return true;
        }
        if (!xmi && "enable-file-serving".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType enable_file_serving_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            enable_file_serving_value.obtainValueFromAttribute("value");
            parser.parse(enable_file_serving_value);
            this.enable_file_serving_value = enable_file_serving_value;
            return true;
        }
        if (!xmi && "pre-compile-jsps".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType pre_compile_jsps_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            pre_compile_jsps_value.obtainValueFromAttribute("value");
            parser.parse(pre_compile_jsps_value);
            this.pre_compile_jsps_value = pre_compile_jsps_value;
            return true;
        }
        if (!xmi && "enable-reloading".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType enable_reloading_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            enable_reloading_value.obtainValueFromAttribute("value");
            parser.parse(enable_reloading_value);
            this.enable_reloading_value = enable_reloading_value;
            return true;
        }
        if (!xmi && "enable-serving-servlets-by-class-name".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType enable_serving_servlets_by_class_name_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            enable_serving_servlets_by_class_name_value.obtainValueFromAttribute("value");
            parser.parse(enable_serving_servlets_by_class_name_value);
            this.enable_serving_servlets_by_class_name_value = enable_serving_servlets_by_class_name_value;
            return true;
        }
        return false;
    }

    void addServlet(com.ibm.ws.javaee.ddmodel.webext.ServletExtensionType servlet) {
        if (this.servlet == null) {
            this.servlet = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.ServletExtensionType, com.ibm.ws.javaee.dd.webext.ServletExtension>();
        }
        this.servlet.add(servlet);
    }

    void addFileServingAttribute(com.ibm.ws.javaee.ddmodel.webext.AttributeType file_serving_attribute) {
        if (this.file_serving_attribute == null) {
            this.file_serving_attribute = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.AttributeType, com.ibm.ws.javaee.dd.webext.Attribute>();
        }
        this.file_serving_attribute.add(file_serving_attribute);
    }

    void addInvokerAttribute(com.ibm.ws.javaee.ddmodel.webext.AttributeType invoker_attribute) {
        if (this.invoker_attribute == null) {
            this.invoker_attribute = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.AttributeType, com.ibm.ws.javaee.dd.webext.Attribute>();
        }
        this.invoker_attribute.add(invoker_attribute);
    }

    void addJspAttribute(com.ibm.ws.javaee.ddmodel.webext.AttributeType jsp_attribute) {
        if (this.jsp_attribute == null) {
            this.jsp_attribute = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.AttributeType, com.ibm.ws.javaee.dd.webext.Attribute>();
        }
        this.jsp_attribute.add(jsp_attribute);
    }

    void addMimeFilter(com.ibm.ws.javaee.ddmodel.webext.MimeFilterType mime_filter) {
        if (this.mime_filter == null) {
            this.mime_filter = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.MimeFilterType, com.ibm.ws.javaee.dd.webext.MimeFilter>();
        }
        this.mime_filter.add(mime_filter);
    }

    void addResourceRef(com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType resource_ref) {
        if (this.resource_ref == null) {
            this.resource_ref = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType, com.ibm.ws.javaee.dd.commonext.ResourceRef>();
        }
        this.resource_ref.add(resource_ref);
    }

    void addServletCacheConfig(com.ibm.ws.javaee.ddmodel.webext.ServletCacheConfigType servlet_cache_config) {
        if (this.servlet_cache_config == null) {
            this.servlet_cache_config = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.ServletCacheConfigType, com.ibm.ws.javaee.dd.webext.ServletCacheConfig>();
        }
        this.servlet_cache_config.add(servlet_cache_config);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("webApp", xmiRef);
        diag.describeIfSet("version", version);
        diag.describeIfSet("additionalClassPath", additionalClassPath);
        diag.describeIfSet("metadataComplete", metadataComplete);
        diag.describeIfSet(xmi ? "extendedServlets" : "servlet", servlet);
        diag.describeIfSet(xmi ? "fileServingAttributes" : "file-serving-attribute", file_serving_attribute);
        diag.describeIfSet(xmi ? "invokerAttributes" : "invoker-attribute", invoker_attribute);
        diag.describeIfSet(xmi ? "jspAttributes" : "jsp-attribute", jsp_attribute);
        diag.describeIfSet(xmi ? "mimeFilters" : "mime-filter", mime_filter);
        diag.describeIfSet(xmi ? "resourceRefExtensions" : "resource-ref", resource_ref);
        diag.describeIfSet(xmi ? "servletCacheConfigs" : "servlet-cache-config", servlet_cache_config);
        diag.describeIfSet(xmi ? "defaultErrorPage" : "default-error-page[@uri]", default_error_page_uri);
        diag.describeIfSet(xmi ? "reloadInterval" : "reload-interval[@value]", reload_interval_value);
        diag.describeIfSet(xmi ? "contextRoot" : "context-root[@uri]", context_root_uri);
        diag.describeIfSet(xmi ? "autoLoadFilters" : "autoload-filters[@value]", autoload_filters_value);
        diag.describeIfSet(xmi ? "autoRequestEncoding" : "auto-encode-requests[@value]", auto_encode_requests_value);
        diag.describeIfSet(xmi ? "autoResponseEncoding" : "auto-encode-responses[@value]", auto_encode_responses_value);
        diag.describeIfSet(xmi ? "directoryBrowsingEnabled" : "enable-directory-browsing[@value]", enable_directory_browsing_value);
        diag.describeIfSet(xmi ? "fileServingEnabled" : "enable-file-serving[@value]", enable_file_serving_value);
        diag.describeIfSet(xmi ? "preCompileJSPs" : "pre-compile-jsps[@value]", pre_compile_jsps_value);
        diag.describeIfSet(xmi ? "reloadingEnabled" : "enable-reloading[@value]", enable_reloading_value);
        diag.describeIfSet(xmi ? "serveServletsByClassnameEnabled" : "enable-serving-servlets-by-class-name[@value]", enable_serving_servlets_by_class_name_value);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
