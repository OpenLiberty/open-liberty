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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(configurationPid = "com.ibm.ws.javaee.dd.webext.WebExt",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class WebExtComponentImpl implements com.ibm.ws.javaee.dd.webext.WebExt {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.webext.WebExt delegate;

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "file-serving-attribute", target = "(id=unbound)")
     protected void setFile_serving_attribute(com.ibm.ws.javaee.dd.webext.Attribute value) {
          this.file_serving_attribute.add(value);
     }

     protected void unsetFile_serving_attribute(com.ibm.ws.javaee.dd.webext.Attribute value) {
          this.file_serving_attribute.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.webext.Attribute> file_serving_attribute = new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "invoker-attribute", target = "(id=unbound)")
     protected void setInvoker_attribute(com.ibm.ws.javaee.dd.webext.Attribute value) {
          this.invoker_attribute.add(value);
     }

     protected void unsetInvoker_attribute(com.ibm.ws.javaee.dd.webext.Attribute value) {
          this.invoker_attribute.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.webext.Attribute> invoker_attribute = new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "jsp-attribute", target = "(id=unbound)")
     protected void setJsp_attribute(com.ibm.ws.javaee.dd.webext.Attribute value) {
          this.jsp_attribute.add(value);
     }

     protected void unsetJsp_attribute(com.ibm.ws.javaee.dd.webext.Attribute value) {
          this.jsp_attribute.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.webext.Attribute> jsp_attribute = new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "mime-filter", target = "(id=unbound)")
     protected void setMime_filter(com.ibm.ws.javaee.dd.webext.MimeFilter value) {
          this.mime_filter.add(value);
     }

     protected void unsetMime_filter(com.ibm.ws.javaee.dd.webext.MimeFilter value) {
          this.mime_filter.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.webext.MimeFilter> mime_filter = new ArrayList<com.ibm.ws.javaee.dd.webext.MimeFilter>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "resource-ref", target = "(id=unbound)")
     protected void setResource_ref(com.ibm.ws.javaee.dd.commonext.ResourceRef value) {
          this.resource_ref.add(value);
     }

     protected void unsetResource_ref(com.ibm.ws.javaee.dd.commonext.ResourceRef value) {
          this.resource_ref.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonext.ResourceRef> resource_ref = new ArrayList<com.ibm.ws.javaee.dd.commonext.ResourceRef>();
     protected java.lang.String default_error_page_uri;
     protected java.lang.String context_root_uri;
     protected Boolean autoload_filters_value;
     protected Boolean auto_encode_requests_value;
     protected Boolean auto_encode_responses_value;
     protected Boolean enable_directory_browsing_value;
     protected Boolean enable_file_serving_value;
     protected Boolean pre_compile_jsps_value;
     protected Boolean enable_serving_servlets_by_class_name_value;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          enable_serving_servlets_by_class_name_value = (Boolean) config.get("enable-serving-servlets-by-class-name");
          autoload_filters_value = (Boolean) config.get("autoload-filters");
          pre_compile_jsps_value = (Boolean) config.get("pre-compile-jsps");
          auto_encode_responses_value = (Boolean) config.get("auto-encode-responses");
          context_root_uri = (java.lang.String) config.get("context-root");
          enable_directory_browsing_value = (Boolean) config.get("enable-directory-browsing");
          auto_encode_requests_value = (Boolean) config.get("auto-encode-requests");
          enable_file_serving_value = (Boolean) config.get("enable-file-serving");
          default_error_page_uri = (java.lang.String) config.get("default-error-page");
     }

     @Override
     public java.lang.String getVersion() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getVersion();
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.webext.ServletExtension> getServletExtensions() {
          // Not Used In Liberty -- returning default value or app configuration
          java.util.List<com.ibm.ws.javaee.dd.webext.ServletExtension> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.webext.ServletExtension>() : new ArrayList<com.ibm.ws.javaee.dd.webext.ServletExtension>(delegate.getServletExtensions());
          return returnValue;
     }

     @Override
     public java.lang.String getDefaultErrorPage() {
          if (delegate == null) {
               return default_error_page_uri == null ? null : default_error_page_uri;
          } else {
               return default_error_page_uri == null ? delegate.getDefaultErrorPage() : default_error_page_uri;
          }
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> getFileServingAttributes() {
          java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>() : new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>(delegate.getFileServingAttributes());
          returnValue.addAll(file_serving_attribute);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> getInvokerAttributes() {
          java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>() : new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>(delegate.getInvokerAttributes());
          returnValue.addAll(invoker_attribute);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> getJspAttributes() {
          java.util.List<com.ibm.ws.javaee.dd.webext.Attribute> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>() : new ArrayList<com.ibm.ws.javaee.dd.webext.Attribute>(delegate.getJspAttributes());
          returnValue.addAll(jsp_attribute);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.webext.MimeFilter> getMimeFilters() {
          java.util.List<com.ibm.ws.javaee.dd.webext.MimeFilter> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.webext.MimeFilter>() : new ArrayList<com.ibm.ws.javaee.dd.webext.MimeFilter>(delegate.getMimeFilters());
          returnValue.addAll(mime_filter);
          return returnValue;
     }

     @Override
     public boolean isSetReloadInterval() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? false : delegate.isSetReloadInterval();
     }

     @Override
     public int getReloadInterval() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? 0 : delegate.getReloadInterval();
     }

     @Override
     public boolean isSetContextRoot() {
          return (context_root_uri!= null);
     }

     @Override
     public java.lang.String getContextRoot() {
          if (delegate == null) {
               return context_root_uri == null ? null : context_root_uri;
          } else {
               return context_root_uri == null ? delegate.getContextRoot() : context_root_uri;
          }
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonext.ResourceRef> getResourceRefs() {
          java.util.List<com.ibm.ws.javaee.dd.commonext.ResourceRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonext.ResourceRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonext.ResourceRef>(delegate.getResourceRefs());
          returnValue.addAll(resource_ref);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.webext.ServletCacheConfig> getServletCacheConfigs() {
          // Not Used In Liberty -- returning default value or app configuration
          java.util.List<com.ibm.ws.javaee.dd.webext.ServletCacheConfig> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.webext.ServletCacheConfig>() : new ArrayList<com.ibm.ws.javaee.dd.webext.ServletCacheConfig>(delegate.getServletCacheConfigs());
          return returnValue;
     }

     @Override
     public boolean isSetAutoloadFilters() {
          return (autoload_filters_value!= null);
     }

     @Override
     public boolean isAutoloadFilters() {
          if (delegate == null) {
               return autoload_filters_value == null ? false : autoload_filters_value;
          } else {
               return autoload_filters_value == null ? delegate.isAutoloadFilters() : autoload_filters_value;
          }
     }

     @Override
     public boolean isSetAutoEncodeRequests() {
          return (auto_encode_requests_value!= null);
     }

     @Override
     public boolean isAutoEncodeRequests() {
          if (delegate == null) {
               return auto_encode_requests_value == null ? false : auto_encode_requests_value;
          } else {
               return auto_encode_requests_value == null ? delegate.isAutoEncodeRequests() : auto_encode_requests_value;
          }
     }

     @Override
     public boolean isSetAutoEncodeResponses() {
          return (auto_encode_responses_value!= null);
     }

     @Override
     public boolean isAutoEncodeResponses() {
          if (delegate == null) {
               return auto_encode_responses_value == null ? false : auto_encode_responses_value;
          } else {
               return auto_encode_responses_value == null ? delegate.isAutoEncodeResponses() : auto_encode_responses_value;
          }
     }

     @Override
     public boolean isSetEnableDirectoryBrowsing() {
          return (enable_directory_browsing_value!= null);
     }

     @Override
     public boolean isEnableDirectoryBrowsing() {
          if (delegate == null) {
               return enable_directory_browsing_value == null ? false : enable_directory_browsing_value;
          } else {
               return enable_directory_browsing_value == null ? delegate.isEnableDirectoryBrowsing() : enable_directory_browsing_value;
          }
     }

     @Override
     public boolean isSetEnableFileServing() {
          return (enable_file_serving_value!= null);
     }

     @Override
     public boolean isEnableFileServing() {
          if (delegate == null) {
               return enable_file_serving_value == null ? false : enable_file_serving_value;
          } else {
               return enable_file_serving_value == null ? delegate.isEnableFileServing() : enable_file_serving_value;
          }
     }

     @Override
     public boolean isSetPreCompileJsps() {
          return (pre_compile_jsps_value!= null);
     }

     @Override
     public boolean isPreCompileJsps() {
          if (delegate == null) {
               return pre_compile_jsps_value == null ? false : pre_compile_jsps_value;
          } else {
               return pre_compile_jsps_value == null ? delegate.isPreCompileJsps() : pre_compile_jsps_value;
          }
     }

     @Override
     public boolean isSetEnableReloading() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? false : delegate.isSetEnableReloading();
     }

     @Override
     public boolean isEnableReloading() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? false : delegate.isEnableReloading();
     }

     @Override
     public boolean isSetEnableServingServletsByClassName() {
          return (enable_serving_servlets_by_class_name_value!= null);
     }

     @Override
     public boolean isEnableServingServletsByClassName() {
          if (delegate == null) {
               return enable_serving_servlets_by_class_name_value == null ? false : enable_serving_servlets_by_class_name_value;
          } else {
               return enable_serving_servlets_by_class_name_value == null ? delegate.isEnableServingServletsByClassName() : enable_serving_servlets_by_class_name_value;
          }
     }

// Methods required to implement DeploymentDescriptor -- Not used in Liberty
    @Override
    public String getDeploymentDescriptorPath() {
        return null;
    }

    @Override
    public Object getComponentForId(String id) {
        return null;
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return null;
    }
// End of DeploymentDescriptor Methods -- Not used in Liberty
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.webext.WebExt delegate) {
          this.delegate = delegate;
     }
}
