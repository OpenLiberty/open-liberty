/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.models.media.Schema;

public class Constants {
    // OSGi Version Constants
    public static final String OSGI_VERSION                 = "1.0";
    
    // Trace Constants
    public static final String TRACE_GROUP                  = "MPOPENAPI";
    public static final String TRACE_OPENAPI                = "io.openliberty.microprofile.openapi.internal.resources.OpenAPI";
    public static final String TRACE_VALIDATION             = "io.openliberty.microprofile.openapi.internal.resources.validation.ValidationMessages";

    // OpenAPI Document Default Constants
    public static final String DEFAULT_OPENAPI_DOC_TITLE    = "Generated API";
    public static final String DEFAULT_OPENAPI_DOC_VERSION  = "1.0";
    public static final String MERGED_OPENAPI_DOC_TITLE     = "Merged documentation";
    
    // File Suffix Constants
    public static final String FILE_SUFFIX_CLASS            = ".class";
    public static final String FILE_SUFFIX_WAR              = ".war";
    public static final String FILE_SUFFIX_JAR              = ".jar";
    public static final String FILE_SUFFIX_YAML             = ".yaml";
    public static final String FILE_SUFFIX_YML              = ".yml";
    public static final String FILE_SUFFIX_JSON             = ".json";

    public static final String STRING_PERIOD                = ".";
    public static final String STRING_COLON                 = ":";
    public static final String STRING_EMPTY                 = "";
    public static final String STRING_FORWARD_SLASH         = "/";
    public static final String STRING_BACK_SLASH            = "\\";

    public static final String PROTOCOL_HTTP                = "http";
    public static final String PROTOCOL_HTTPS               = "https";

    public static final String SCHEME_HTTP                  = PROTOCOL_HTTP + "://";
    public static final String SCHEME_HTTPS                 = PROTOCOL_HTTPS + "://";

    // Archive Directory Constants
    public static final String DIR_META_INF                 = "META-INF";
    public static final String DIR_WEB_INF                  = "WEB-INF";
    public static final String DIR_CLASSES                  = "classes";
    public static final String DIR_WEB_INF_CLASSES          = "WEB-INF/classes/";
    
    // OpenAPI Static File Constants
    public static final String STATIC_FILE_NAME             = "openapi";
    public static final String STATIC_FILE_META_INF_OPENAPI_YAML =
        STRING_FORWARD_SLASH +
        DIR_META_INF +
        STRING_FORWARD_SLASH +
        STATIC_FILE_NAME +
        FILE_SUFFIX_YAML;
    public static final String STATIC_FILE_WEB_INF_CLASSES_META_INF_OPENAPI_YAML =
        STRING_FORWARD_SLASH +
        DIR_WEB_INF +
        STRING_FORWARD_SLASH +
        DIR_CLASSES +
        STRING_FORWARD_SLASH +
        DIR_META_INF +
        STRING_FORWARD_SLASH +
        STATIC_FILE_NAME +
        FILE_SUFFIX_YAML; 
    public static final String STATIC_FILE_META_INF_OPENAPI_YML =
        STRING_FORWARD_SLASH +
        DIR_META_INF +
        STRING_FORWARD_SLASH +
        STATIC_FILE_NAME +
        FILE_SUFFIX_YML; 
    public static final String STATIC_FILE_WEB_INF_CLASSES_META_INF_OPENAPI_YML =
        STRING_FORWARD_SLASH +
        DIR_WEB_INF +
        STRING_FORWARD_SLASH +
        DIR_CLASSES +
        STRING_FORWARD_SLASH +
        DIR_META_INF +
        STRING_FORWARD_SLASH +
        STATIC_FILE_NAME +
        FILE_SUFFIX_YML;
    public static final String STATIC_FILE_META_INF_OPENAPI_JSON =
        STRING_FORWARD_SLASH +
        DIR_META_INF +
        STRING_FORWARD_SLASH +
        STATIC_FILE_NAME +
        FILE_SUFFIX_JSON;
    public static final String STATIC_FILE_WEB_INF_CLASSES_META_INF_OPENAPI_JSON =
        STRING_FORWARD_SLASH +
        DIR_WEB_INF +
        STRING_FORWARD_SLASH +
        DIR_CLASSES +
        STRING_FORWARD_SLASH +
        DIR_META_INF +
        STRING_FORWARD_SLASH +
        STATIC_FILE_NAME +
        FILE_SUFFIX_JSON; 

    public static final String JAX_RS_APP_PATH_ANNOTATION_CLASS_NAME = "javax.ws.rs.ApplicationPath";
    public static final String JAX_RS_PATH_ANNOTATION_CLASS_NAME     = "javax.ws.rs.Path";
    public static final String OPENAPI_SCHEMA_ANNOTATION_CLASS_NAME  = "org.eclipse.microprofile.openapi.annotations.media.Schema";
    public static final List<String> ANNOTATION_CLASS_NAMES          = Arrays.asList(
        JAX_RS_PATH_ANNOTATION_CLASS_NAME,
        JAX_RS_APP_PATH_ANNOTATION_CLASS_NAME,
        OPENAPI_SCHEMA_ANNOTATION_CLASS_NAME
    );

    // Schema Type Constants
    public static final String SCHEMA_TYPE_ARRAY             = Schema.SchemaType.ARRAY.toString();
    public static final String SCHEMA_TYPE_NULL              = "null";
    public static final String SCHEMA_TYPE_OBJECT            = Schema.SchemaType.OBJECT.toString();
    public static final String SCHEMA_TYPE_STRING            = Schema.SchemaType.STRING.toString();

    // Media Type Constants
    public static final String MEDIA_TYPE_APPLICATION_JS     = "application/javascript";
    public static final String MEDIA_TYPE_TEXT_JS            = "text/javascript";
    public static final String MEDIA_TYPE_TEXT_JSON          = "text/json";
    public static final String MEDIA_TYPE_IMAGE_PNG          = "image/png";

    // Format Query Parameter Constants
    public static final String FORMAT_PARAM_NAME             = "format";
    public static final String FORMAT_PARAM_VALUE_JSON       = "json";
    public static final String FORMAT_PARAM_VALUE_YAML       = "yaml";

    // Regular expression constants
    public static final Pattern REGEX_COMPONENT_KEY_PATTERN  = Pattern.compile("^[a-zA-Z0-9\\.\\-_]+$");
    public static final Pattern REGEX_TOKEN_PATTERN          = Pattern.compile("^[a-zA-Z0-9!#$%&'*+-.^_`|~]+$");
    public static final Pattern REGEX_EMAIL_PATTERN          = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");
    public static final Pattern REGEX_COMPONENT_REQUEST_URI  = Pattern.compile("^\\/openapi[\\/]?$"); 
    public static final Pattern REGEX_COMPONENT_PATH_INFO    = Pattern.compile("^\\/[a-zA-Z0-9()@:%_\\+.~]+[\\/]?$"); 
    
    // CSS Customization Constants
    public static final String HEADER_CSS_URL_KEY            = "header-css-url";
    public static final String HEADER_CSS_CONTENT_KEY        = "header-css-content";
    public static final String PATH_CSS_CUSTOM_HEADER        = "css/custom-header.css";
    public static final String PATH_CSS_DEFAULT_HEADER       = "css/default-header.css";
    public static final String PATH_CSS_IMAGES_CUSTOM_LOGO   = "css/images/custom-logo.png";
    public static final String PATH_IMAGES_CUSTOM_LOGO       = "images/custom-logo.png";
    public static final String EXPECTED_BACKGROUND_IMAGE     = "background-image: url(" + PATH_IMAGES_CUSTOM_LOGO + ");";
    public static final String DEFAULT_LOCATION_CSS          = "${server.config.dir}/mpopenapi/customization.css";
    public static final String CUSTOM_CSS_SECTION_START      = ".swagger-ui .headerbar ";
    public static final String CUSTOM_CSS_SECTION_END        = "}";

    // Platform support constants
    public static final String PLATFORM_SPI_PREFIX           = "mp.openapi.spi.platform.";
    public static final String COMPONENTS                    = "components";
    
    // Servlet constants
    public static final String SERVLET_NAME_APPLICATION      = "ApplicationServlet";
    public static final String SERVLET_NAME_PLATFORM         = "PlatformServlet";
    public static final String URL_PATTERN_ROOT              = "/";
    public static final String URL_PATTERN_PLATFORM          = "/platform";
    public static final String URL_PATTERN_PLATFORM_WILDCARD = "/platform/*";
    
    // Environment variable constants
    public static final String ENV_VAR_VCAP_APPLICATION      = "VCAP_APPLICATION";
    
    // Merge config constants
    public static final String MERGE_INCLUDE_CONFIG          = OASConfig.EXTENSIONS_PREFIX + "liberty.merged.include";
    public static final String MERGE_EXCLUDE_CONFIG          = OASConfig.EXTENSIONS_PREFIX + "liberty.merged.exclude";
    public static final String MERGE_INFO_CONFIG             = OASConfig.EXTENSIONS_PREFIX + "liberty.merged.info";
    
    // Misc. Constants
    public static final String RESPONSE_RANGE_SUCCESS        = "2XX";

    private Constants() {
        // This class is not meant to be instantiated.
    }
}
