/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;

/**
 * 
 * 
 * WCCustomProperties contains static strings of all the custom properties in the
 * webcontainer.
 * 
 * @ibm-private-in-use
 */

public class WCCustomProperties {

    private static TraceComponent tc = Tr.register(WCCustomProperties.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public static Properties customProps = WebContainer.getWebContainerProperties();

    public static String DO_NOT_SERVE_BY_CLASSNAME;
    public static boolean SUPPRESS_WSEP_HEADER;
    public static boolean REDIRECT_CONTEXT_ROOT;

    public static boolean ERROR_EXCEPTION_TYPE_FIRST;
    public static String PREPEND_SLASH_TO_RESOURCE;
    public static String SESSION_REWRITE_IDENTIFIER;
    public static boolean KEEP_CONTENT_LENGTH;
    public static boolean SKIP_HEADER_FLUSH;
    public static String CONTENT_TYPE_COMPATIBILITY;
    public static boolean GET_SESSION_24_COMPATIBILITY;
    public static boolean OLD_DATE_FORMATTER;
    public static String OPTIMIZE_FILE_SERVING_SIZE_GLOBAL;
    public static int SYNC_FILE_SERVING_SIZE_GLOBAL;
    public static int MAPPED_BYTE_BUFFER_SIZE_GLOBAL;
    public static boolean DISABLE_MULTI_THREAD_CONN_MGMT;
    public static boolean DECODE_URL_AS_UTF8;
    public static boolean EXPOSE_WEB_INF_ON_DISPATCH;
    public static boolean DIRECTORY_BROWSING_ENABLED;
    public static String DISALLOW_ALL_FILE_SERVING;
    public static boolean FILE_SERVING_ENABLED;
    public static String DISALLOW_SERVE_SERVLETS_BY_CLASSNAME_PROP;

    public static boolean SERVE_SERVLETS_BY_CLASSNAME_ENABLED; //HEY YOU!!! BETTER NOT MESS WITH THE DEFAULT OF THIS PROPERTY
    public static boolean REDIRECT_WITH_PATH_INFO;
    public static boolean REMOVE_TRAILING_SERVLET_PATH_SLASH; //PK39337
    public static String LISTENERS;
    public static boolean GLOBALLISTENER; 
    public static boolean SERVLET_CASE_SENSITIVE; //PK42055
    public static String ENABLE_IN_PROCESS_CONNECTIONS;
    public static boolean SUPPRESS_SERVLET_EXCEPTION_LOGGING;

    public static String ERROR_PAGE_COMPATIBILITY;
    public static boolean MAP_FILTERS_TO_ASTERICK;
    public static boolean SUPPRESS_HTML_RECURSIVE_ERROR_OUTPUT;
    public static boolean THROW_MISSING_JSP_EXCEPTION;          //PK57843

    //638627 had to change default to true for CTS test case
    //If the default servlet is the target of a RequestDispatch.include() and the requested
    //resource does not exist, then the default servlet MUST throw
    //FileNotFoundException. If the exception isn't caught and handled, and the
    //response hasn't been committed, the status code MUST be set to 500.
    public static boolean MODIFIED_FNF_BEHAVIOR;
                   
    public static int SERVLET_DESTROY_WAIT_TIME;

    public static boolean FILE_WRAPPER_EVENTS;

    public static boolean DISABLE_SYSTEM_APP_GLOBAL_LISTENER_LOADING ; //PK66137	    

    public static boolean THROW_404_IN_PREFERENCE_TO_403; // PK64302
    public static boolean DISCERN_UNAVAILABLE_SERVLET; // PK76117
    public static boolean ASSUME_FILTER_SUCCESS_ON_SECURITY_ERROR; // PK76117
    public static boolean IGNORE_INVALID_QUERY_STRING; //PK75617
    public static boolean PROVIDE_QSTRING_TO_WELCOME_FILE; //PK78371
    public static String SUPPRESS_HEADERS_IN_REQUEST; //PK80362

    public static boolean DISPATCHER_RETHROW_SER; // PK79464

    // 18.0.0.4 Do not use ENABLE_DEFAULT_SERVLET_REQUEST_PATH_ELEMENTS.  Use SERVLET_PATH_FOR_DEFAULT_MAPPING instead going forward
    public static boolean ENABLE_DEFAULT_SERVLET_REQUEST_PATH_ELEMENTS; // PK80340

    public static boolean COPY_ATTRIBUTES_KEY_SET; //PK81452	
    public static boolean SUPPRESS_LAST_ZERO_BYTE_PACKAGE; // PK82794
    public static boolean DEFAULT_TRACE_REQUEST_BEHAVIOR; // PK83258.2
    public static boolean DEFAULT_HEAD_REQUEST_BEHAVIOR; // PK83258.2
    public static boolean INVOKE_FILTER_INIT_AT_START_UP; //PK86553
    public static boolean GET_WRITER_ON_EMPTY_BUFFER; //PK90190    

    public static boolean IGNORE_SESSION_STATIC_FILE_REQUEST; // PK89213
    public static boolean INVOKE_REQUEST_LISTENER_FOR_FILTER; // PK91120
    public static boolean FINISH_RESPONSE_ON_CLOSE; // PK89810
    public static boolean LIMIT_BUFFER; //PK95332

    //Start 7.0.0.9
    public static boolean IGNORE_INJECTION_FAILURE; // 596191

    public static String HTTPONLY_COOKIES; //F004323
    public static boolean REINIT_SERVLET_ON_INIT_UNAVAILABLE_EXCEPTION; //PM01373
    //End 7.0.0.9

    // Start 7.0.0.11
    public static boolean SERVE_WELCOME_FILE_FROM_EDR; // PM02985   
    public static boolean FILE_WRAPPER_EVENTS_LESS_DETAIL; // PK99400
    public static boolean SET_UNENCODED_HTML_IN_SENDERROR; // PM03788
    public static boolean DISABLE_SET_CHARACTER_ENCODING_AFTER_PARAMETERS_READ; // PM03928
    public static boolean THROW_EXCEPTION_FOR_ADDELRESOLVER; //PM05903
    public static boolean ENABLE_JSP_MAPPING_OVERRIDE; //PM07560
    public static boolean ENABLE_DEFAULT_IS_EL_IGNORED_IN_TAG; // PM08060
    public static boolean COMPLETE_RESPONSE_EARLY; //PM08760
    // End 7.0.0.11

    //Start 7.0.0.13
    public static boolean TOLERATE_LOCALE_MISMATCH_FOR_SERVING_FILES; //PM10362
    public static boolean ALLOW_PARTIAL_URL_TO_EDR; // PM17845  
    //End 7.0.0.13
    
    public static boolean CHECK_EDR_IN_GET_REAL_PATH; // DocumentRoots2

    //Start 7.0.0.15
    //PM22082 SECINT property will not be published.
    public static boolean ALLOW_DIRECTORY_INCLUDE; //PM22082
    public static boolean DISPATCHER_RETHROW_SERROR; //PM22919
    public static boolean COMPLETE_DATA_RESPONSE; //PM18453
    public static boolean COMPLETE_REDIRECT_RESPONSE; //PM18453
    public static boolean KEEP_UNREAD_DATA; //PM18453
    public static boolean PARSE_UTF8_POST_DATA; //PM20484
    
    public static boolean LOCALE_DEPENDENT_DATE_FORMATTER;//PM25931, this has been added if any customer is dependet on current behavior.
    //End 7.0.0.15
    
    //Start 7.0.0.19
    public static boolean IFMODIFIEDSINCE_NEWER_THAN_FILEMODIFIED_TIMESTAMP; //PM36341
    public static boolean ALLOW_QUERY_PARAM_WITH_NO_EQUAL; //PM35450

    //End 7.0.0.19

    //Begin 8.0
    //see WebAppRequestDispatcher where this is used for details.
    public static boolean KEEP_ORIGINAL_PATH_ELEMENTS;
    public static boolean LOG_SERVLET_CONTAINER_INITIALIZER_CLASSLOADER_ERRORS; //Servlet 3.0
    public static boolean ALLOW_INCLUDE_SEND_ERROR; //Servlet 3.0
    public static boolean SERVLET_30_FNF_BEHAVIOR;
    public static boolean SKIP_META_INF_RESOURCES_PROCESSING; //Servlet 3.0
    public static int META_INF_RESOURCES_CACHE_SIZE;
    public static boolean INIT_PARAM_CONFLICT_CHECK;
    public static boolean CHECK_REQUEST_OBJECT_IN_USE;
    public static boolean USE_WORK_MANAGER_FOR_ASYNC_CONTEXT_START;
    public static boolean RESET_BUFFER_ON_SET_STATUS;
    public static boolean TOLERATE_SYMBOLIC_LINKS;

    public static String X_POWERED_BY;
    public static boolean DISABLE_X_POWERED_BY;

    public static boolean DISABLE_SCI_FOR_PRE_V8_APPS;

    //Begin: Do not document
    public static boolean CHECK_FORCE_WORK_REJECTED;

    //End: Do not document

    public static boolean JSF_DISABLE_ALTERNATE_FACES_CONFIG_SEARCH; //JSF 2.0 for startup performance

    //undocumented
    public static boolean THROW_EXCEPTION_WHEN_UNABLE_TO_COMPLETE_OR_DISPATCH;
    //end undocumented

    //End 8.0
    
    //begin 8.0.0.1
    public static boolean ENABLE_EXACT_MATCH_J_SECURITY_CHECK;
    //end 8.0.0.1
    
    //Start 8.0.0.2
    public static boolean EXPRESSION_RETURN_EMPTY_STRING; //PM47661

    //End 8.0.0.2
    
    // Start 8.0.0.3
    public static boolean RETURN_DEFAULT_CONTEXT_PATH;  //PM47487 
    // Do not document INVOKE_FLUSH_AFTER_SERVICE
    public static boolean INVOKE_FLUSH_AFTER_SERVICE; //PM50111    
    public static boolean LOG_MULTIPART_EXCEPTIONS_ON_PARSEPARAMETER; //724365.2  
    public static int MAX_PARAM_PER_REQUEST; //724365

    //end 8.0.0.3
    
    //Start 8.0.0.4
    public static int MAX_DUPLICATE_HASHKEY_PARAMS; //728397 (PM58495)
    //End 8.0.0.4
    // Start 8.5
    public static boolean DEFER_SERVLET_LOAD;
    // End 8.5

    public static int ASYNC_MAX_SIZE_TASK_POOL;
    public static int ASYNC_PURGE_INTERVAL;
    public static int DEFAULT_ASYNC_SERVLET_TIMEOUT;
    public static int NUMBER_ASYNC_TIMER_THREADS;

    //Start 8.5.0.1
    public static boolean THROW_POSTCONSTRUCT_EXCEPTION;        //PM63754
    
    public static boolean INIT_FILTER_BEFORE_INIT_SERVLET ; //PM62909
    
    // PM97514 -- remove from use by webcontainer. Shifted to HttpDispatcher
    // public static boolean TRUSTED; //PM70260 -- removed by PM97514
    // public static boolean TRUST_HOST_HEADER_PORT;// PM70260 -- removed by PM97514
    // public static boolean EXTRACT_HOST_HEADER_PORT; //PM70260 -- removed by PM97514
    public static String HTTPS_INDICATOR_HEADER; //PM70260
    
    public static boolean ENABLE_TRACE_REQUESTS=false;
    

    //Start 8.5.0.2
    public static boolean REMOVE_ATTRIBUTE_FOR_NULL_OBJECT; //PM71991    
    public static boolean SUPPRESS_LOGGING_SERVICE_RUNTIME_EXCEP ; //739806 (PM79934, trad PM74090)
    public static boolean STRICT_SERVLET_MAPPING ; //add to revert changes added by 86353 
    public static boolean SET_CONTENT_LENGTH_ON_CLOSE;          //PM71666
    // Start 8.5.5
    public static boolean ALLOW_DOTS_IN_NAME; //PM3452, PM82876
    
    //Start 8.5.5.1
    public static boolean USE_ORIGINAL_REQUEST_STATE ; //PM88028
    public static boolean HANDLING_REQUEST_WITH_OVERRIDDEN_PATH ; //PM88028
    
    public static boolean DECODE_PARAM_VIA_REQ_ENCODING ;
    public static boolean PRINT_BYTEVALUE_AND_CHARPARAMDATA;
    public static boolean DENY_DUPLICATE_FILTER_IN_CHAIN ; //PM93069
    public static boolean VALIDATE_LOCALE_VALUES; 
    public static String  DISABLE_STATIC_MAPPING_CACHE;      //PM84305
    
    //Start 8.5.5.2
    public static boolean TRANSFER_CONTEXT_IN_ASYNC_SERVLET_REQUEST; //PM90834
    public static boolean DESTROY_SERVLET_ON_SERVICE_UNAVAILABLE_EXCEPTION; //PM98245 
    public static boolean NORMALIZE_REQUEST_URI; //PI05525
    public static boolean EVAL_EXPRESSION_FOLLOWING_TWO_BACKSLASHES; //PM81674
    public static boolean ALLOW_DEFAULT_ERROR_PAGE; //PI05845    

    
    //Start 8.5.5.3
    public static String DISPLAY_TEXT_WHEN_NO_ERROR_PAGE_DEFINED; // PI09474
    
    //Start 8.5.5.4
    public static boolean PRESERVE_REQUEST_PARAMETER_VALUES; //PI20210
    public static boolean APPEND_METAINF_RESOURCES_IN_LOOSE_LIB;  //PM99163
    public static boolean EMPTY_SERVLET_MAPPINGS; //PI23529    
    public static int SERVLET31_PRIVATE_BUFFERSIZE_FOR_LARGE_POST_DATA; // ContentLengthLong support - Internally private. May remove but will be useful for performance testing.
    
    //start 8.5.5.5
    public static boolean DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR; //PI26908
    
    // Start 8.5.5.6
    public static boolean ALLOW_EXPRESSION_FACTORY_PER_APP;
    public static boolean IGNORE_SEMICOLON_ON_REDIRECT_TO_WELCOME_PAGE; //PI31447
    public static boolean USE_SEMICOLON_AS_DELIMITER_IN_URI; //PI31292
    public static boolean INITIALIZE_CLASS_IN_HANDLES_TYPES_STARTUP;
    
    // Start 8.5.5.7
    public static boolean INVOKE_FLUSH_AFTER_SERVICE_FOR_STATIC_FILE;//PI38116
    
    // Start 8.5.5.9
    public static boolean DEFER_PROCESSING_INCOMPLETE_FILTERS_IN_WEB_XML;//PI42598
    public static boolean SET_ASYNC_DISPATCH_REQUEST_URI;//PI43752
    
    // Start 8.5.5.10
    public static boolean PARSE_PARTS_PARAMETERS_USING_REQUEST_ENCODING;//PI56833
    public static boolean KEEP_SEPARATOR_IN_MULTIPART_FORM_FIELDS;//PI57951
    public static boolean ENABLE_POST_ONLY_J_SECURITY_CHECK;    //PI60797
    
    // Start 8.5.5.11
    public static boolean STOP_APP_STARTUP_ON_LISTENER_EXCEPTION;//PI58875
    public static boolean INVOKE_FLUSH_AFTER_SERVICE_FOR_STATIC_FILE_RESPONSE_WRAPPER; //PI63193
    public static boolean ENCODE_DISPATCHED_REQUEST_URI; //PI67942
    
    //start 9.0
    public static boolean IGNORE_DISTRIBUTABLE;
    
    // start 9.0.0.1
    public static boolean INCLUDE_STACK_IN_DEFAULT_ERROR_PAGE;
    
    //start 16.0.0.3
    public static String ADD_STRICT_TRANSPORT_SECURITY_HEADER;
    
    //start 17.0.0.1
    public static boolean USE_MAXREQUESTSIZE_FOR_MULTIPART; //PI75528

    public static boolean ENABLE_MULTI_READ_OF_POST_DATA; //MultiRead
    
    //start 17.0.0.4
    public static boolean USE_ORIGINAL_QS_IN_FORWARD_IF_NULL; //PI81569
	
    //18.0.0.3
    public static String SERVLET_PATH_FOR_DEFAULT_MAPPING;

    //19.0.0.8
    public static boolean GET_REAL_PATH_RETURNS_QUALIFIED_PATH;
    
    //20.0.0.8
    public static boolean DECODE_URL_PLUS_SIGN;

    //21.0.0.1
    public static boolean REDIRECT_TO_RELATIVE_URL;

    //21.0.0.4
    public static boolean SET_HTML_CONTENT_TYPE_ON_ERROR; 

    static {
        setCustomPropertyVariables(); //initializes all the variables
    }
    
    private final static HashMap<String, String> FullyQualifiedPropertiesMap = new HashMap<String, String>();
    static { 
        WCCustomProperties.FullyQualifiedPropertiesMap.put("disallowallfileserving", "com.ibm.ws.webcontainer.disallowAllFileServing");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("disallowserveservletsbyclassname", "com.ibm.ws.webcontainer.disallowserveservletsbyclassname");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("donotservebyclassname", "com.ibm.ws.webcontainer.donotservebyclassname");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("extracthostheaderport", "com.ibm.ws.webcontainer.extracthostheaderport");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("decodeurlplussign", "com.ibm.ws.webcontainer.decodeurlplussign");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("suppresshtmlrecursiveerroroutput", "com.ibm.ws.webcontainer.suppressHtmlRecursiveErrorOutput");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("filewrapperevents", "com.ibm.ws.webcontainer.fileWrapperEvents");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("webgroupvhostnotfound", "com.ibm.ws.webcontainer.webgroupvhostnotfound");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("defaulttracerequestbehavior", "com.ibm.ws.webcontainer.DefaultTraceRequestBehavior");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("defaultheadrequestbehavior", "com.ibm.ws.webcontainer.DefaultHeadRequestBehavior");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("toleratesymboliclinks", "com.ibm.ws.webcontainer.TolerateSymbolicLinks");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("symboliclinkscachesize", "com.ibm.ws.webcontainer.SymbolicLinksCacheSize");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("enableerrorexceptiontypefirst", "com.ibm.ws.webcontainer.enableErrorExceptionTypeFirst");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("copyattributeskeyset", "com.ibm.ws.webcontainer.copyattributeskeyset");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("dispatcherrethrowser", "com.ibm.ws.webcontainer.dispatcherrethrowser");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("ignoresessiononstaticfilerequest", "com.ibm.ws.webcontainer.IgnoreSessiononStaticFileRequest");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("invokefilterinitatstartup", "com.ibm.ws.webcontainer.invokeFilterInitAtStartup");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("enablejspmappingoverride", "com.ibm.ws.webcontainer.enablejspmappingoverride");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("enabledefaultiselignoredintag", "com.ibm.ws.jsp.enabledefaultiselignoredintag");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("parseutf8postdata", "com.ibm.ws.webcontainer.parseutf8postdata");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("logservletcontainerinitializerclassloadingerrors", "com.ibm.ws.webcontainer.logservletcontainerinitializerclassloadingerrors");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("allowincludesenderror", "com.ibm.ws.webcontainer.allowincludesenderror");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("skipmetainfresourcesprocessing", "com.ibm.ws.webcontainer.skipmetainfresourcesprocessing");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("metainfresourcescachesize", "com.ibm.ws.webcontainer.metainfresourcescachesize");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("xpoweredby", "com.ibm.ws.webcontainer.xpoweredby");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("disablexpoweredby", "com.ibm.ws.webcontainer.disablexpoweredby");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("asyncmaxsizetaskpool", "com.ibm.ws.webcontainer.asyncmaxsizetaskpool");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("asyncpurgeinterval", "com.ibm.ws.webcontainer.asyncpurgeinterval");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("asynctimeoutdefault", "com.ibm.ws.webcontainer.asynctimeoutdefault");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("asynctimerthreads", "com.ibm.ws.webcontainer.asynctimerthreads");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("enableexactmatchjsecuritycheck", "com.ibm.ws.webcontainer.enableexactmatchjsecuritycheck");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("returndefaultcontextpath", "com.ibm.ws.webcontainer.returndefaultcontextpath");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("localedependentdateformatter", "com.ibm.ws.webcontainer.localedependentdateformatter");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("modifiedsincelaterthanfiletimestamp", "com.ibm.ws.webcontainer.modifiedsincelaterthanfiletimestamp");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("allowqueryparamwithnoequal", "com.ibm.ws.webcontainer.allowqueryparamwithnoequal");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("invokeflushafterservice", "com.ibm.ws.webcontainer.invokeflushafterservice");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("expressionreturnemptystring", "com.ibm.ws.jsp.expressionreturnemptystring");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("logmultipartexceptionsonparseparameter", "com.ibm.ws.webcontainer.logmultipartexceptionsonparseparameter");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("maxparamperrequest", "com.ibm.ws.webcontainer.maxparamperrequest");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("maxduplicatehashkeyparams", "com.ibm.ws.webcontainer.maxduplicatehashkeyparams");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("throwpostconstructexception", "com.ibm.ws.webcontainer.throwpostconstructexception");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("initfilterbeforeinitservlet","com.ibm.ws.webcontainer.initfilterbeforeinitservlet"); //PM62909
        WCCustomProperties.FullyQualifiedPropertiesMap.put("removeattributefornullobject","com.ibm.ws.webcontainer.removeattributefornullobject"); //PM71991
        WCCustomProperties.FullyQualifiedPropertiesMap.put("suppressloggingserviceruntimeexcep","com.ibm.ws.webcontainer.suppressloggingserviceruntimeexcep"); //739806 (PM79934, PM74090)
        WCCustomProperties.FullyQualifiedPropertiesMap.put("strictservletmapping","com.ibm.ws.webcontainer.strictservletmapping"); // add option to revert 86353
        WCCustomProperties.FullyQualifiedPropertiesMap.put("allowdotsinname","com.ibm.ws.webcontainer.allowdotsinname");  //PM3452, PM82876
        WCCustomProperties.FullyQualifiedPropertiesMap.put("useoriginalrequeststate","com.ibm.ws.webcontainer.useoriginalrequeststate"); //PM88028
        WCCustomProperties.FullyQualifiedPropertiesMap.put("handlingrequestwithoverridenpath", "com.ibm.ws.webcontainer.handlingrequestwithoverridenpath"); //PM88028, needed to revert PM71901 if required 
        WCCustomProperties.FullyQualifiedPropertiesMap.put("decodeparamviareqencoding","com.ibm.ws.webcontainer.decodeparamviareqencoding"); //PM92940
        WCCustomProperties.FullyQualifiedPropertiesMap.put("printbytevalueandcharparamdata","com.ibm.ws.webcontainer.printbytevalueandcharparamdata"); //PM92940 , debugging property
        WCCustomProperties.FullyQualifiedPropertiesMap.put("denyduplicatefilterinchain","com.ibm.ws.webcontainer.denyduplicatefilterinchain"); //PM93069
        WCCustomProperties.FullyQualifiedPropertiesMap.put("validatelocalevalues","com.ibm.ws.webcontainer.validatelocalevalues"); 
        WCCustomProperties.FullyQualifiedPropertiesMap.put("disablestaticmappingcache", "com.ibm.ws.webcontainer.disablestaticmappingcache"); //PM84305
        WCCustomProperties.FullyQualifiedPropertiesMap.put("transfercontextinasyncservletrequest","com.ibm.ws.webcontainer.transfercontextinasyncservletrequest"); //PM90834
        WCCustomProperties.FullyQualifiedPropertiesMap.put("destroyservletonserviceunavailableexception","com.ibm.ws.webcontainer.destroyservletonserviceunavailableexception"); //PM98245
        WCCustomProperties.FullyQualifiedPropertiesMap.put("normalizerequesturi","com.ibm.ws.webcontainer.normalizerequesturi"); //PI05525
        WCCustomProperties.FullyQualifiedPropertiesMap.put("displaytextwhennoerrorpagedefined","com.ibm.ws.webcontainer.displaytextwhennoerrorpagedefined");  //PI09474
        WCCustomProperties.FullyQualifiedPropertiesMap.put("evalexpressionfollowingtwobackslashes","com.ibm.ws.jsp.evalexpressionfollowingtwobackslashes");  //PM81674
        WCCustomProperties.FullyQualifiedPropertiesMap.put("allowdefaulterrorpage","com.ibm.ws.webcontainer.allowdefaulterrorpage");  //PI05845
        WCCustomProperties.FullyQualifiedPropertiesMap.put("preserverequestparametervalues","com.ibm.ws.webcontainer.preserverequestparametervalues");  //PI20210
        WCCustomProperties.FullyQualifiedPropertiesMap.put("appendmetainfresourcesinlooselib","com.ibm.ws.webcontainer.appendmetainfresourcesinlooselib");  //PM99163
        WCCustomProperties.FullyQualifiedPropertiesMap.put("emptyservletmappings","com.ibm.ws.webcontainer.emptyservletmappings"); //PI23529
        WCCustomProperties.FullyQualifiedPropertiesMap.put("servlet31privatebuffersizeforlargepostdata","servlet31.private.buffersizeforlargepostdata");  //PM99163
        WCCustomProperties.FullyQualifiedPropertiesMap.put("deferservletrequestlistenerdestroyonerror","com.ibm.ws.webcontainer.deferservletrequestlistenerdestroyonerror");  //PI26908
        WCCustomProperties.FullyQualifiedPropertiesMap.put("allowexpressionfactoryperapp","com.ibm.ws.jsp.allowexpressionfactoryperapp"); // PI31922
        WCCustomProperties.FullyQualifiedPropertiesMap.put("ignoresemicolononredirecttowelcomepage","com.ibm.ws.webcontainer.ignoresemicolononredirecttowelcomepage"); //PI31447
        WCCustomProperties.FullyQualifiedPropertiesMap.put("usesemicolonasdelimiterinuri","com.ibm.ws.webcontainer.usesemicolonasdelimiterinuri"); //PI31292  
        WCCustomProperties.FullyQualifiedPropertiesMap.put("initializeclassinhandlestypesstartup","com.ibm.ws.webcontainer.initializeclassinhandlestypesstartup"); // 160846
        WCCustomProperties.FullyQualifiedPropertiesMap.put("invokeflushafterserviceforstaticfile", "com.ibm.ws.webcontainer.invokeflushafterserviceforstaticfile"); //PI38116
        WCCustomProperties.FullyQualifiedPropertiesMap.put("deferprocessingincompletefiltersinwebxml", "com.ibm.ws.webcontainer.deferprocessingincompletefiltersinwebxml"); //PI42598
        WCCustomProperties.FullyQualifiedPropertiesMap.put("setasyncdispatchrequesturi", "com.ibm.ws.webcontainer.setasyncdispatchrequesturi"); //PI43752
        WCCustomProperties.FullyQualifiedPropertiesMap.put("parsepartsparametersusingrequestencoding", "com.ibm.ws.webcontainer.parsepartsparametersusingrequestencoding"); //PI56833
        WCCustomProperties.FullyQualifiedPropertiesMap.put("keepseparatormultipartformfield", "com.ibm.ws.webcontainer.keepseparatormultipartformfield"); //PI57951
        WCCustomProperties.FullyQualifiedPropertiesMap.put("enablepostonlyjsecuritycheck", "com.ibm.ws.webcontainer.enablepostonlyjsecuritycheck"); //PI60797
        WCCustomProperties.FullyQualifiedPropertiesMap.put("stopappstartuponlistenerexception", "com.ibm.ws.webcontainer.stopappstartuponlistenerexception"); //PI58875
        WCCustomProperties.FullyQualifiedPropertiesMap.put("invokeflushafterserviceforstaticfileresponsewrapper", "com.ibm.ws.webcontainer.invokeflushafterserviceforstaticfileresponsewrapper"); //PI63193
        WCCustomProperties.FullyQualifiedPropertiesMap.put("displaycustomizedexceptiontext", "com.ibm.ws.webcontainer.displaycustomizedexceptiontext"); //PI68061
        WCCustomProperties.FullyQualifiedPropertiesMap.put("addstricttransportsecurityheader", "com.ibm.ws.webcontainer.addstricttransportsecurityheader"); //PI67099
        WCCustomProperties.FullyQualifiedPropertiesMap.put("encodedispatchedrequesturi", "com.ibm.ws.webcontainer.encodedispatchedrequesturi"); //PI67942
        WCCustomProperties.FullyQualifiedPropertiesMap.put("usemaxrequestsizeformultipart", "com.ibm.ws.webcontainer.usemaxrequestsizeformultipart"); //PI75528
        WCCustomProperties.FullyQualifiedPropertiesMap.put("enablemultireadofpostdata", "com.ibm.ws.webcontainer.enablemultireadofpostdata");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("useoriginalqsinforwardifnull", "com.ibm.ws.webcontainer.useoriginalqsinforwardifnull"); //PI81569
        WCCustomProperties.FullyQualifiedPropertiesMap.put("servletdestroywaittime", "com.ibm.ws.webcontainer.servletdestroywaittime");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("servletpathfordefaultmapping", "com.ibm.ws.webcontainer.servletpathfordefaultmapping");     //4666
        WCCustomProperties.FullyQualifiedPropertiesMap.put("getrealpathreturnsqualifiedPath", "com.ibm.ws.webcontainer.getrealpathreturnsqualifiedPath");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("redirecttorelativeurl", "com.ibm.ws.webcontainer.redirecttorelativeurl");
        WCCustomProperties.FullyQualifiedPropertiesMap.put("sethtmlcontenttypeonerror", "com.ibm.ws.webcontainer.sethtmlcontenttypeonerror"); //PH34054
    }

    //some properties require "com.ibm.ws.webcontainer." on the front
    //propertyKey is lowerCase value
    private static String getStringKey(String propertyKey) {
        if (WCCustomProperties.FullyQualifiedPropertiesMap.containsKey(propertyKey)) {
            return WCCustomProperties.FullyQualifiedPropertiesMap.get(propertyKey).toLowerCase();
        } else {
            return propertyKey;
        }
    }
    
    //Gets called when config is activated/modified
    public static void setCustomProperties(Map<String, Object> d) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setCustomProperties");
        }

        customProps = WebContainer.getWebContainerProperties();
        String ky;
        String propertyKey;

        // For comparison purposes we need the mixed case key strings coming from metatype.xml and server.xml to be in lower case.
        Set<String> newKeySet = new HashSet<String>();                                                
        for (String key:d.keySet()) {                                                                 
            newKeySet.add(key.toLowerCase());                                                         
        }                                                                                             

        // go through each entry that is in the set of keys that we are getting from metatype.xml and server.xml
        for (Map.Entry<String, Object> entry : d.entrySet()) {
            ky = entry.getKey();
            Object value = entry.getValue();
        
            // using the lower case value of short or long name that was configured
            // see if the string maps to one of the short names in our table.
            // if it matches then get the long name back.  otherwise keep the name as is.
            propertyKey = getStringKey(ky.toLowerCase());
            
            if (!ky.equalsIgnoreCase(propertyKey)) {
                // found in map - check existence of fully qualified prop as well
                // if we mapped to a new long name, see if it is already in the passed in data because
                // configured long names have priority, and if it is in the table elsewhere, then don't use
                // this mapped short name value, because the long name was set in server.xml
                if (newKeySet.contains(propertyKey)) {                                           
                    continue;
                }
            }
            
            // store all values as lower case, so as to read them later in a consistent fashion
            propertyKey =  propertyKey.toLowerCase();
            
            if (value instanceof String) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Key: " + propertyKey + "  type: String  value: " + value);
                }
                customProps.setProperty(propertyKey, (String) value);

            } else if (value instanceof Boolean) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Key: " + propertyKey + "  type: Boolean  value: " + ((Boolean) value).toString());
                }
                customProps.setProperty(propertyKey, ((Boolean) value).toString());

            } else if (value instanceof Integer) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Key: " + propertyKey + "  type: Integer  value: " + ((Integer) value).toString());
                }
                customProps.setProperty(propertyKey, ((Integer) value).toString());
            } else {
                // don't update, not a custom property we care about here
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Key not processed due to type, Key: " + propertyKey);
                }
            }
        }

        setCustomPropertyVariables(); //Need to update all the variables.

        setCustomizedDefaultValues(); //Customize default value depending on servlet level, initial size....etc..

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setCustomProperties");
        }

    }

    public static void setCustomPropertyVariables() {
        String methodName = "setCustomPropertyVariables";
        
        DO_NOT_SERVE_BY_CLASSNAME = customProps.getProperty("com.ibm.ws.webcontainer.donotservebyclassname");
        SUPPRESS_WSEP_HEADER = (Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.suppresserrorpageodrheader"))).booleanValue();
        REDIRECT_CONTEXT_ROOT = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.redirectcontextroot")).booleanValue();

        ERROR_EXCEPTION_TYPE_FIRST = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.enableerrorexceptiontypefirst")).booleanValue();
        PREPEND_SLASH_TO_RESOURCE = customProps.getProperty("prependslashtoresource");
        SESSION_REWRITE_IDENTIFIER = customProps.getProperty("sessionrewriteidentifier");
        KEEP_CONTENT_LENGTH = (Boolean.valueOf(customProps.getProperty("keepcontentlength")).booleanValue());
        SKIP_HEADER_FLUSH = (Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.skipheaderflush"))).booleanValue();
        CONTENT_TYPE_COMPATIBILITY = customProps.getProperty("com.ibm.ws.webcontainer.contenttypecompatibility");
        GET_SESSION_24_COMPATIBILITY = (Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.getsession2_4compatibility"))).booleanValue();
        OLD_DATE_FORMATTER = (Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.olddateformatter"))).booleanValue();
        OPTIMIZE_FILE_SERVING_SIZE_GLOBAL = customProps.getProperty("com.ibm.ws.webcontainer.optimizefileservingsize");
        SYNC_FILE_SERVING_SIZE_GLOBAL = Integer.valueOf(customProps.getProperty("syncfileservingsize", "-1")).intValue();
        MAPPED_BYTE_BUFFER_SIZE_GLOBAL = Integer.valueOf(customProps.getProperty("mappedbytebuffersize", "-1")).intValue();
        DISABLE_MULTI_THREAD_CONN_MGMT = Boolean.valueOf(customProps.getProperty("disablemultithreadedservletconnectionmgmt")).booleanValue();
        DECODE_URL_AS_UTF8 = Boolean.valueOf(customProps.getProperty("decodeurlasutf8", "true")).booleanValue();
        EXPOSE_WEB_INF_ON_DISPATCH = Boolean.valueOf(customProps.getProperty("exposewebinfondispatch")).booleanValue();
        DIRECTORY_BROWSING_ENABLED = Boolean.valueOf(customProps.getProperty("directorybrowsingenabled")).booleanValue();
        DISALLOW_ALL_FILE_SERVING = customProps.getProperty("com.ibm.ws.webcontainer.disallowallfileserving");
        FILE_SERVING_ENABLED = Boolean.valueOf(customProps.getProperty("fileservingenabled", "true")).booleanValue();
        DISALLOW_SERVE_SERVLETS_BY_CLASSNAME_PROP = customProps.getProperty("com.ibm.ws.webcontainer.disallowserveservletsbyclassname");

        SERVE_SERVLETS_BY_CLASSNAME_ENABLED = Boolean.valueOf(customProps.getProperty("serveservletsbyclassnameenabled")).booleanValue(); //HEY YOU!!! BETTER NOT MESS WITH THE DEFAULT OF THIS PROPERTY
        REDIRECT_WITH_PATH_INFO = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.redirectwithpathinfo")).booleanValue();
        REMOVE_TRAILING_SERVLET_PATH_SLASH = (Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.removetrailingservletpathslash"))).booleanValue(); //PK39337
        LISTENERS = customProps.getProperty("listeners");
        GLOBALLISTENER = Boolean.valueOf(customProps.getProperty("com.ibm.webcontainer.fvt.listeners.globallistener"));
        SERVLET_CASE_SENSITIVE = (Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.servletcasesensitive"))).booleanValue(); //PK42055
        ENABLE_IN_PROCESS_CONNECTIONS = customProps.getProperty("enableinprocessconnections");
        SUPPRESS_SERVLET_EXCEPTION_LOGGING = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.suppressservletexceptionlogging")).booleanValue();

        ERROR_PAGE_COMPATIBILITY = customProps.getProperty("com.ibm.ws.webcontainer.contenttypecompatibility");
        MAP_FILTERS_TO_ASTERICK = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.mapfilterstoasterisk")).booleanValue();
        SUPPRESS_HTML_RECURSIVE_ERROR_OUTPUT = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.suppresshtmlrecursiveerroroutput")).booleanValue();
        THROW_MISSING_JSP_EXCEPTION = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.throwmissingjspexception")).booleanValue(); //PK57843

        //638627 had to change default to true for CTS test case
        //If the default servlet is the target of a RequestDispatch.include() and the requested
        //resource does not exist, then the default servlet MUST throw
        //FileNotFoundException. If the exception isn't caught and handled, and the
        //response hasn't been committed, the status code MUST be set to 500.
        MODIFIED_FNF_BEHAVIOR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty
                        ("com.ibm.ws.webcontainer.modifiedfilenotfoundexceptionbehavior", "true")).booleanValue(); //PK65408 
        SERVLET_DESTROY_WAIT_TIME = Integer.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.servletdestroywaittime", "60")).intValue();

        FILE_WRAPPER_EVENTS = Boolean.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.filewrapperevents", "false")).booleanValue();

        DISABLE_SYSTEM_APP_GLOBAL_LISTENER_LOADING = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty
                        ("com.ibm.ws.webcontainer.disablesystemappgloballistenerloading")).booleanValue(); //PK66137       

        THROW_404_IN_PREFERENCE_TO_403 = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.throw404inpreferenceto403")).booleanValue(); // PK64302
        DISCERN_UNAVAILABLE_SERVLET = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.discernunavailableservlet")).booleanValue(); // PK76117
        ASSUME_FILTER_SUCCESS_ON_SECURITY_ERROR = Boolean.valueOf(
                                                                  WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.assumefiltersuccessonsecurityerror")).booleanValue(); // PK76117
        IGNORE_INVALID_QUERY_STRING = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.ignoreinvalidquerystring")).booleanValue(); //PK75617
        PROVIDE_QSTRING_TO_WELCOME_FILE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.provideqstringtowelcomefile", "true")).booleanValue(); //PK78371
        SUPPRESS_HEADERS_IN_REQUEST = customProps.getProperty("com.ibm.ws.webcontainer.suppressheadersinrequest"); //PK80362

        DISPATCHER_RETHROW_SER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.dispatcherrethrowser", "true")).booleanValue(); // PK79464
        ENABLE_DEFAULT_SERVLET_REQUEST_PATH_ELEMENTS = Boolean.valueOf(
                                                                       WebContainer.getWebContainerProperties().getProperty(
                                                                                                                            "com.ibm.ws.webcontainer.enabledefaultservletrequestpathelements")).booleanValue(); // PK80340
        COPY_ATTRIBUTES_KEY_SET = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.copyattributeskeyset")).booleanValue(); //PK81452       
        SUPPRESS_LAST_ZERO_BYTE_PACKAGE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.suppresslastzerobytepackage")).booleanValue(); // PK82794
        DEFAULT_TRACE_REQUEST_BEHAVIOR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.defaulttracerequestbehavior")).booleanValue(); // PK83258.2
        DEFAULT_HEAD_REQUEST_BEHAVIOR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.defaultheadrequestbehavior")).booleanValue(); // PK83258.2
        INVOKE_FILTER_INIT_AT_START_UP = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.invokefilterinitatstartup", "false")).booleanValue(); //PK86553
        GET_WRITER_ON_EMPTY_BUFFER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.getwriteronemptybuffer", "false")).booleanValue(); //PK90190    

        IGNORE_SESSION_STATIC_FILE_REQUEST = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.ignoresessiononstaticfilerequest")).booleanValue(); // PK89213
        INVOKE_REQUEST_LISTENER_FOR_FILTER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.invokerequestlistenerforfilter")).booleanValue(); // PK91120
        FINISH_RESPONSE_ON_CLOSE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.finishresponseonclose")).booleanValue(); // PK89810
        LIMIT_BUFFER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.limitbuffer")).booleanValue(); //PK95332

        //Start 7.0.0.9
        IGNORE_INJECTION_FAILURE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.ignoreinjectionfailure", "false")).booleanValue(); // 596191

        HTTPONLY_COOKIES = customProps.getProperty("com.ibm.ws.webcontainer.httponlycookies"); //F004323
        REINIT_SERVLET_ON_INIT_UNAVAILABLE_EXCEPTION = Boolean.valueOf(WebContainer.getWebContainerProperties().
              getProperty("com.ibm.ws.webcontainer.reinitservletoninitunavailableexception","true")).booleanValue(); //PM01373
        
        //End 7.0.0.9

        // Start 7.0.0.11
        SERVE_WELCOME_FILE_FROM_EDR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.servewelcomefilefromextendeddocumentroot")).booleanValue(); // PM02985   
        FILE_WRAPPER_EVENTS_LESS_DETAIL = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.filewrappereventslessdetail")).booleanValue(); // PK99400
        SET_UNENCODED_HTML_IN_SENDERROR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.setunencodedhtmlinsenderror")).booleanValue(); // PM03788
        DISABLE_SET_CHARACTER_ENCODING_AFTER_PARAMETERS_READ = Boolean.valueOf(
                                                                               WebContainer.getWebContainerProperties().getProperty(
                                                                                                                                    "com.ibm.ws.webcontainer.disablesetcharacterencodingafterparametersread")).booleanValue(); // PM03928
        THROW_EXCEPTION_FOR_ADDELRESOLVER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.throwexceptionforaddelresolver")).booleanValue(); //PM05903
        ENABLE_JSP_MAPPING_OVERRIDE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.enablejspmappingoverride")).booleanValue(); //PM07560
        ENABLE_DEFAULT_IS_EL_IGNORED_IN_TAG = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.enabledefaultiselignoredintag")).booleanValue(); // PM08060
        COMPLETE_RESPONSE_EARLY = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.completeresponseearly")).booleanValue(); //PM08760
        // End 7.0.0.11

        //Start 7.0.0.13
        TOLERATE_LOCALE_MISMATCH_FOR_SERVING_FILES = Boolean.valueOf(
                                                                     WebContainer.getWebContainerProperties().getProperty(
                                                                                                                          "com.ibm.ws.webcontainer.toleratelocalemismatchforservingfiles",
                                                                                                                          "false")).booleanValue(); //PM10362
        ALLOW_PARTIAL_URL_TO_EDR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.enablepartialurltoextendeddocumentroot")).booleanValue(); // PM17845  
        //End 7.0.0.13
        
        CHECK_EDR_IN_GET_REAL_PATH = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.checkedringetrealpath","true")).booleanValue(); // DocumentRoots2

        //Start 7.0.0.15
        //PM22082 SECINT property will not be published.
        ALLOW_DIRECTORY_INCLUDE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.allowdirectoryinclude")).booleanValue(); //PM22082
        DISPATCHER_RETHROW_SERROR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.dispatcherrethrowserror")).booleanValue(); //PM22919
        COMPLETE_DATA_RESPONSE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.sendresponsetoclientwhenresponseiscomplete", "true")).booleanValue(); //PM18453
        COMPLETE_REDIRECT_RESPONSE = Boolean.valueOf(
                                                     WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.sendresponsetoclientaspartofsendredirect",
                                                                                                          "false")).booleanValue(); //PM18453
        KEEP_UNREAD_DATA = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.keepunreadpostdataafterresponsesenttoclient", "false")).booleanValue(); //PM18453
        PARSE_UTF8_POST_DATA = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.parseutf8postdata", "false")).booleanValue(); //PM20484
        
        LOCALE_DEPENDENT_DATE_FORMATTER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.localedependentdateformatter")).booleanValue(); //PM25931, this has been added if any customer is dependet on current behavior.
        //End 7.0.0.15
        
        //Start 7.0.0.19
        IFMODIFIEDSINCE_NEWER_THAN_FILEMODIFIED_TIMESTAMP = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.modifiedsincelaterthanfiletimestamp", "false")).booleanValue(); //PM36341
        //End 7.0.0.19

        //Begin 8.0
        //see WebAppRequestDispatcher where this is used for details.
        KEEP_ORIGINAL_PATH_ELEMENTS = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.keeporiginalpathelements", "true")).booleanValue();
        LOG_SERVLET_CONTAINER_INITIALIZER_CLASSLOADER_ERRORS = Boolean.valueOf(
                                                                               WebContainer.getWebContainerProperties().getProperty(
                                                                                                                                    "com.ibm.ws.webcontainer.logservletcontainerinitializerclassloadingerrors")).booleanValue(); //Servlet 3.0
        ALLOW_INCLUDE_SEND_ERROR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.allowincludesenderror")).booleanValue(); //Servlet 3.0
        SERVLET_30_FNF_BEHAVIOR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.servlet30filenotfoundbehavior", "true")).booleanValue(); //Servlet 3.0;
        SKIP_META_INF_RESOURCES_PROCESSING = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.skipmetainfresourcesprocessing")).booleanValue(); //Servlet 3.0
        META_INF_RESOURCES_CACHE_SIZE = Integer.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.metainfresourcescachesize", "20")).intValue();
        INIT_PARAM_CONFLICT_CHECK = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.initparamconflictcheck", "true")).booleanValue(); //Servlet 3.0;
        CHECK_REQUEST_OBJECT_IN_USE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.checkrequestobjectuse")).booleanValue();
        USE_WORK_MANAGER_FOR_ASYNC_CONTEXT_START = Boolean.valueOf(
                                                                   WebContainer.getWebContainerProperties().getProperty(
                                                                                                                        "com.ibm.ws.webcontainer.useworkmanagerforasynccontextstart",
                                                                                                                        "true")).booleanValue(); //Servlet 3.0;
        RESET_BUFFER_ON_SET_STATUS = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.resetbufferonsetstatus")).booleanValue();
        
        TOLERATE_SYMBOLIC_LINKS = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.toleratesymboliclinks")).booleanValue();

        X_POWERED_BY = WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.xpoweredby");
        
        DISABLE_SCI_FOR_PRE_V8_APPS = Boolean.valueOf(
                                                      WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.disableservletcontainerinitializersonprev8apps")).booleanValue();

        //Begin: Do not document
        CHECK_FORCE_WORK_REJECTED = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.checkforceworkrejected")).booleanValue();

        //End: Do not document

        JSF_DISABLE_ALTERNATE_FACES_CONFIG_SEARCH = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsf.disablealternatefacesconfigsearch")); //JSF 2.0 for startup performance

        //undocumented
        THROW_EXCEPTION_WHEN_UNABLE_TO_COMPLETE_OR_DISPATCH = Boolean.valueOf(
                                                                              WebContainer.getWebContainerProperties().getProperty(
                                                                                                                                   "com.ibm.ws.webcontainer.throwexceptionwhenunabletocompleteordispatch",
                                                                                                                                   "true")).booleanValue();
        //end undocumented

        //Start 8.0.0.1
        ENABLE_EXACT_MATCH_J_SECURITY_CHECK = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.enableexactmatchjsecuritycheck")).booleanValue();       //F011107

        //End 8.0.0.1
        
        //Start 8.0.0.2
        //EXPRESSION_RETURN_EMPTY_STRING should be a jsp property - 
        EXPRESSION_RETURN_EMPTY_STRING = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.expressionreturnemptystring")).booleanValue(); //PM47661
        //End 8.0.0.2
        //Start 8.0.0.3
        RETURN_DEFAULT_CONTEXT_PATH = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.returndefaultcontextpath","true")).booleanValue();  //PM47487
        // Do not document INVOKE_FLUSH_AFTER_SERVICE
        INVOKE_FLUSH_AFTER_SERVICE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.invokeflushafterservice" , "true")).booleanValue(); //PM50111    
        LOG_MULTIPART_EXCEPTIONS_ON_PARSEPARAMETER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.logmultipartexceptionsonparseparameter")).booleanValue(); //724365.2  
        MAX_PARAM_PER_REQUEST = Integer.valueOf( WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.maxparamperrequest", "10000")).intValue(); //724365

        //End 8.0.0.3
        
        //Start 8.0.0.4
        MAX_DUPLICATE_HASHKEY_PARAMS = Integer.valueOf( WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.maxduplicatehashkeyparams", "50")).intValue(); //728397 (PM58495)

        //End 8.0.0.4
        
        //Start Liberty
        DEFER_SERVLET_LOAD = Boolean.valueOf(customProps.getProperty("deferservletload")); //Liberty to override delayed load/init

        
        
        ASYNC_MAX_SIZE_TASK_POOL = Integer.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.asyncmaxsizetaskpool", "5000")).intValue();
        ASYNC_PURGE_INTERVAL = Integer.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.asyncpurgeinterval", "30000")).intValue();

        DEFAULT_ASYNC_SERVLET_TIMEOUT  = Integer.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.asynctimeoutdefault", "30000")).intValue();
        NUMBER_ASYNC_TIMER_THREADS   = Integer.valueOf(customProps.getProperty("com.ibm.ws.webcontainer.asynctimerthreads", "2")).intValue();
        
        //Start 8.5.0.1
        THROW_POSTCONSTRUCT_EXCEPTION = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.throwpostconstructexception", "true")).booleanValue();  //PM63754
        
        INIT_FILTER_BEFORE_INIT_SERVLET = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.initfilterbeforeinitservlet")).booleanValue(); //PM62909
        
        // PM97514 -- remove from use by webcontainer. Shifted to HttpDispatcher
        // TRUSTED = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("trusted", "true")).booleanValue();  //PM70260
        // TRUST_HOST_HEADER_PORT = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("trusthostheaderport", "false")).booleanValue();  //PM70260
        // EXTRACT_HOST_HEADER_PORT = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.extracthostheaderport", "false")).booleanValue();  //PM70260
        HTTPS_INDICATOR_HEADER = WebContainer.getWebContainerProperties().getProperty("httpsindicatorheader"); //PM70260
        if ((HTTPS_INDICATOR_HEADER != null) && HTTPS_INDICATOR_HEADER.trim().equals("")) HTTPS_INDICATOR_HEADER = null; // Eliminate String manipulation/compare on mainline path, getHeader does not accept empty str.
        
        ENABLE_TRACE_REQUESTS = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("enabletracerequests", "false")).booleanValue();  //71479
                
        //Start 8.5.0.2
        REMOVE_ATTRIBUTE_FOR_NULL_OBJECT = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.removeattributefornullobject", "true")).booleanValue(); //PM71991    
        SUPPRESS_LOGGING_SERVICE_RUNTIME_EXCEP = Boolean.valueOf( WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.suppressloggingserviceruntimeexcep", "false")).booleanValue(); //739806 , PM79934
        STRICT_SERVLET_MAPPING =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.strictservletmapping", "true")).booleanValue(); //add to revert 86353
        SET_CONTENT_LENGTH_ON_CLOSE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("setcontentlengthonclose", "true")).booleanValue();  //PM71666

        // Start 8.5.5
        ALLOW_DOTS_IN_NAME = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.allowdotsinname")).booleanValue();  //PM83452, PM82876
        // Start 8.5.5.1
        USE_ORIGINAL_REQUEST_STATE = Boolean.valueOf( WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.useoriginalrequeststate")).booleanValue(); //PM88028
        HANDLING_REQUEST_WITH_OVERRIDDEN_PATH = Boolean.valueOf( WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.handlingrequestwithoverridenpath")).booleanValue(); //PM88028, needed to revert PM71901 if required 
        
        DECODE_PARAM_VIA_REQ_ENCODING = Boolean.valueOf( WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.decodeparamviareqencoding")).booleanValue(); //PM92940
        PRINT_BYTEVALUE_AND_CHARPARAMDATA = Boolean.valueOf( WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.printbytevalueandcharparamdata")).booleanValue(); //PM92940, debugging property
        DENY_DUPLICATE_FILTER_IN_CHAIN =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.denyduplicatefilterinchain", "false")).booleanValue(); //PM93069
        VALIDATE_LOCALE_VALUES =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.validatelocalevalues", "true")).booleanValue(); 
        DISABLE_STATIC_MAPPING_CACHE = customProps.getProperty("com.ibm.ws.webcontainer.disablestaticmappingcache");     //PM84305
        
        // Start 8.5.5.2
        TRANSFER_CONTEXT_IN_ASYNC_SERVLET_REQUEST =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.transfercontextinasyncservletrequest", "false")).booleanValue(); //PM90834
        DESTROY_SERVLET_ON_SERVICE_UNAVAILABLE_EXCEPTION =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.destroyservletonserviceunavailableexception")).booleanValue(); //PM98245        
        NORMALIZE_REQUEST_URI =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.normalizerequesturi","false")).booleanValue(); //PI05525
        EVAL_EXPRESSION_FOLLOWING_TWO_BACKSLASHES = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.evalexpressionfollowingtwobackslashes")).booleanValue();  //PM81674
        ALLOW_DEFAULT_ERROR_PAGE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.allowdefaulterrorpage")).booleanValue();  //PI05845

        // Start 8.5.5.3
        DISPLAY_TEXT_WHEN_NO_ERROR_PAGE_DEFINED = customProps.getProperty("com.ibm.ws.webcontainer.displaytextwhennoerrorpagedefined"); // PI09474
        
        // Start 8.5.5.4 CD
        PRESERVE_REQUEST_PARAMETER_VALUES = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.preserverequestparametervalues")).booleanValue(); //PI20210
        APPEND_METAINF_RESOURCES_IN_LOOSE_LIB = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.appendmetainfresourcesinlooselib", "true")).booleanValue(); //PM99163
        EMPTY_SERVLET_MAPPINGS =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.emptyservletmappings")).booleanValue(); //PI23529
        SERVLET31_PRIVATE_BUFFERSIZE_FOR_LARGE_POST_DATA = Integer.valueOf(customProps.getProperty("servlet31.private.buffersizeforlargepostdata", (new Integer(Integer.MAX_VALUE/16)).toString())).intValue();
        if (SERVLET31_PRIVATE_BUFFERSIZE_FOR_LARGE_POST_DATA < 1) SERVLET31_PRIVATE_BUFFERSIZE_FOR_LARGE_POST_DATA = Integer.MAX_VALUE/16;
       
        //Start 8.5.5.5 CD
        DEFER_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR =  Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.deferservletrequestlistenerdestroyonerror")).booleanValue(); //PI26908
        
        // Start 8.5.5.6 CD
        ALLOW_EXPRESSION_FACTORY_PER_APP = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.jsp.allowexpressionfactoryperapp")).booleanValue(); // PI31922
        IGNORE_SEMICOLON_ON_REDIRECT_TO_WELCOME_PAGE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.ignoresemicolononredirecttowelcomepage")).booleanValue(); //PI31447
        USE_SEMICOLON_AS_DELIMITER_IN_URI = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.usesemicolonasdelimiterinuri")).booleanValue(); //PI31292
        INITIALIZE_CLASS_IN_HANDLES_TYPES_STARTUP = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.initializeclassinhandlestypesstartup", "true")).booleanValue(); // 160846
        
	// Start 8.5.5.7 CD
        INVOKE_FLUSH_AFTER_SERVICE_FOR_STATIC_FILE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.invokeflushafterserviceforstaticfile" , "true")).booleanValue(); //PI38116
        
        // Start 8.5.5.9 CD
        DEFER_PROCESSING_INCOMPLETE_FILTERS_IN_WEB_XML = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.deferprocessingincompletefiltersinwebxml" , "false")).booleanValue(); //PI42598
        SET_ASYNC_DISPATCH_REQUEST_URI = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.setasyncdispatchrequesturi")).booleanValue(); //PI43752
        
        // Start 8.5.5.10
        PARSE_PARTS_PARAMETERS_USING_REQUEST_ENCODING = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.parsepartsparametersusingrequestencoding")).booleanValue(); //PI56833
        KEEP_SEPARATOR_IN_MULTIPART_FORM_FIELDS = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.keepseparatormultipartformfield")).booleanValue(); //PI57951
        ENABLE_POST_ONLY_J_SECURITY_CHECK = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.enablepostonlyjsecuritycheck")).booleanValue();      //PI60797
        
        // Start 8.5.5.11
        INVOKE_FLUSH_AFTER_SERVICE_FOR_STATIC_FILE_RESPONSE_WRAPPER = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.invokeflushafterserviceforstaticfileresponsewrapper" , "true")).booleanValue(); //PI63193
        ENCODE_DISPATCHED_REQUEST_URI = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.encodedispatchedrequesturi", "false")).booleanValue(); //PI67942
        
        // Start 9.0
        IGNORE_DISTRIBUTABLE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("ignoredistributable")).booleanValue();    
        
        // Start 9.0.0.1
        INCLUDE_STACK_IN_DEFAULT_ERROR_PAGE = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("includestackindefaulterrorpage")).booleanValue();
        
        //Start 16.0.0.3
        ADD_STRICT_TRANSPORT_SECURITY_HEADER = customProps.getProperty("com.ibm.ws.webcontainer.addstricttransportsecurityheader");
        
        //Start 17.0.0.1
        USE_MAXREQUESTSIZE_FOR_MULTIPART = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.usemaxrequestsizeformultipart")).booleanValue();
        
        ENABLE_MULTI_READ_OF_POST_DATA = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.enablemultireadofpostdata")).booleanValue(); // MultiRead

        //Start 17.0.0.4
        USE_ORIGINAL_QS_IN_FORWARD_IF_NULL = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.useoriginalqsinforwardifnull")).booleanValue(); //PI81569

        //18.0.0.3
        SERVLET_PATH_FOR_DEFAULT_MAPPING = customProps.getProperty("com.ibm.ws.webcontainer.servletpathfordefaultmapping"); //4666

        // 19.0.0.8
        GET_REAL_PATH_RETURNS_QUALIFIED_PATH = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.getrealpathreturnsqualifiedpath", "true")).booleanValue();
        
        //21.0.0.1
        REDIRECT_TO_RELATIVE_URL = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.redirecttorelativeurl", "false")).booleanValue();

        //21.0.0.4
        SET_HTML_CONTENT_TYPE_ON_ERROR = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.sethtmlcontenttypeonerror", "true")).booleanValue();

        //Default for Servlet 5.0 +
        if(com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_50) {
            DISABLE_X_POWERED_BY = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.disablexpoweredby","true")).booleanValue();
            STOP_APP_STARTUP_ON_LISTENER_EXCEPTION = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.stopappstartuponlistenerexception" , "true")).booleanValue();
            DECODE_URL_PLUS_SIGN = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.decodeurlplussign", "false")).booleanValue(); 
            ALLOW_QUERY_PARAM_WITH_NO_EQUAL = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.allowqueryparamwithnoequal", "true")).booleanValue();
        } else {
            DISABLE_X_POWERED_BY = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.disablexpoweredby","false")).booleanValue();
            STOP_APP_STARTUP_ON_LISTENER_EXCEPTION = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.stopappstartuponlistenerexception" , "false")).booleanValue();
            DECODE_URL_PLUS_SIGN = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.decodeurlplussign", "true")).booleanValue();
            ALLOW_QUERY_PARAM_WITH_NO_EQUAL = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.allowqueryparamwithnoequal", "false")).booleanValue();
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "DISABLE_X_POWERED_BY [" + DISABLE_X_POWERED_BY + "], " +
                                     "STOP_APP_STARTUP_ON_LISTENER_EXCEPTION ["+ STOP_APP_STARTUP_ON_LISTENER_EXCEPTION + "], " +
                                     "DECODE_URL_PLUS_SIGN [" + DECODE_URL_PLUS_SIGN + "], " +
                                     "ALLOW_QUERY_PARAM_WITH_NO_EQUAL [" + ALLOW_QUERY_PARAM_WITH_NO_EQUAL + "]");
        }
    }

    private static void setCustomizedDefaultValues(){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Customized default values: ");
        }

        //18.0.0.4 SERVLET_PATH_FOR_DEFAULT_MAPPING has highest priority.  If not present AND ENABLE_DEFAULT_SERVLET_REQUEST_PATH_ELEMENTS is true, set SERVLET_PATH_FOR_DEFAULT_MAPPING
        if (SERVLET_PATH_FOR_DEFAULT_MAPPING == null || SERVLET_PATH_FOR_DEFAULT_MAPPING.isEmpty()){
            if (ENABLE_DEFAULT_SERVLET_REQUEST_PATH_ELEMENTS)
                SERVLET_PATH_FOR_DEFAULT_MAPPING = "true";
            else
                SERVLET_PATH_FOR_DEFAULT_MAPPING = ((com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_40) ? "true" : "false" );

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "servletpathfordefaultmapping = " + SERVLET_PATH_FOR_DEFAULT_MAPPING);
            }
        }
    }

}
