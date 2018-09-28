/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

/**
 *
 */
public class WebContainerElement extends ConfigElement {

    private String listeners;
    private Boolean decodeurlasutf8;
    private Boolean fileservingenabled;
    private Boolean disallowAllFileServing;
    private Boolean directorybrowsingenabled;
    private Boolean serveServletsByClassnameEnabled;
    private Boolean disallowserveservletsbyclassname;
    private String donotservebyclassname;
    private Boolean trusthostheaderport;
    private Boolean trusted;
    private Boolean extracthostheaderport;
    private String httpsindicatorheader;
    private Boolean ExposeWebInfOnDispatch;
    private Boolean decodeurlplussign;
    private String channelwritetype;
    private Boolean suppressHtmlRecursiveErrorOutput;
    private Boolean fileWrapperEvents;
    private String webgroupvhostnotfound;
    private Boolean invokeFiltersCompatibility;
    private Boolean defaultTraceRequestBehavior;
    private Boolean defaultHeadRequestBehavior;
    private Boolean tolerateSymbolicLinks;
    private Integer symbolicLinksCacheSize;
    private Boolean enableErrorExceptionTypeFirst;
    private Boolean enablemultireadofpostdata;
    private Boolean skipInputStreamRead;
    private String httpOnlyCookies;
    private Boolean invokeFilterInitAtStartup;
    private Boolean enablejspmappingoverride;
    //private Boolean com.ibm.ws.jsp.enabledefaultiselignoredintag;
    private Boolean enabledefaultiselignoredintag;
    private Boolean parseutf8postdata;
    private Boolean logservletcontainerinitializerclassloadingerrors;
    private Boolean allowincludesenderror;
    private Boolean skipmetainfresourcesprocessing;
    private Integer metainfresourcescachesize;

    private Boolean deferServletLoad;

    private Integer asyncmaxsizetaskpool;
    private Integer asyncpurgeinterval;
    private Integer asynctimeoutdefault;
    private Integer asynctimerthreads;

    private String xpoweredby;
    private Boolean disablexpoweredby;
    private Boolean enableDefaultServletRequestPathElements;
    private Boolean copyattributeskeyset;
    private String contentType;
    private Boolean mapFiltersToAsterisk;
    private Boolean DisableSetCharacterEncodingAfterParametersRead; //PM03928
    private Boolean provideqStringToWelcomeFile;
    private Boolean tolerateLocaleMismatchForServingFiles;
    private Boolean serveWelcomeFileFromExtendedDocumentRoot;
    private Boolean discernUnavailableServlet;
    private Boolean setUnencodedHTMLinSendError;
    private Boolean sendResponseToClientWhenResponseIsComplete;
    private Boolean sendResponseToClientAsPartOfSendRedirect;
    private Boolean keepUnReadPostDataAfterResponseSentToClient;
    private Boolean finishResponseOnClose;
    private Boolean globalListener;
    private Boolean checkForceWorkRejected; // only for testing purposes
    private Boolean enablePartialURLtoExtendedDocumentRoot;
    private Boolean checkedRingetRealPath;
    private Boolean disableServletContainerInitializersOnPreV8Apps;
    private Boolean ignoreInvalidQueryString;

    private Boolean reInitServletOnInitUnavailableException;
    private Boolean setContentLengthOnClose; //PM71666
    private Boolean allowdotsinname; //PM83452
    private Boolean useoriginalrequeststate; //PM88028
    private Boolean destroyservletonserviceunavailableexception; //PM98245
    private Boolean normalizerequesturi; //PI05525
    private String displaytextwhennoerrorpagedefined; //PI09474
    private Boolean throwExceptionForAddELResolver; //PM05903
    private Boolean evalExpressionFollowingTwoBackslashes; //PM81674
    private Boolean limitBuffer; //PK95332
    private Boolean allowdefaulterrorpage; //PI05845
    private Boolean preserveRequestParameterValues; //PI20210
    private Boolean writerOnEmptyBuffer; //PK90190
    private Boolean emptyServletMappings; //PI23529
    private Boolean parameterReturnEmptyString; //PK56156
    private Boolean redirectWithPathInfo; //PK23779
    private Boolean deferServletRequestListenerDestroyOnError; //PI26908
    private Boolean allowExpressionFactoryPerApp; //PI31922
    private Boolean useMaxRequestsizeforMultipart; //PI75528

    /**
     * @return the listeners
     */
    public String getListeners() {
        return listeners;
    }

    /**
     * @return the decodeurlasutf8
     */
    public Boolean getDecodeurlasutf8() {
        return decodeurlasutf8;
    }

    /**
     * @return the fileservingenabled
     */
    public Boolean getFileservingenabled() {
        return fileservingenabled;
    }

    /**
     * @return the disallowAllFileServing
     */
    public Boolean getDisallowAllFileServing() {
        return disallowAllFileServing;
    }

    /**
     * @return the directorybrowsingenabled
     */
    public Boolean getDirectorybrowsingenabled() {
        return directorybrowsingenabled;
    }

    /**
     * @return the serveServletsByClassnameEnabled
     */
    public Boolean getServeServletsByClassnameEnabled() {
        return serveServletsByClassnameEnabled;
    }

    /**
     * @return the disallowserveservletsbyclassname
     */
    public Boolean getDisallowserveservletsbyclassname() {
        return disallowserveservletsbyclassname;
    }

    /**
     * @return the donotservebyclassname
     */
    public String getDonotservebyclassname() {
        return donotservebyclassname;
    }

    /**
     * @return the trusthostheaderport
     */
    public Boolean getTrusthostheaderport() {
        return trusthostheaderport;
    }

    /**
     * @return the trusted
     */
    public Boolean getTrusted() {
        return trusted;
    }

    /**
     * @return the extracthostheaderport
     */
    public Boolean getExtracthostheaderport() {
        return extracthostheaderport;
    }

    /**
     * @return the httpsindicatorheader
     */
    public String getHttpsindicatorheader() {
        return httpsindicatorheader;
    }

    /**
     * @return the exposeWebInfOnDispatch
     */
    public Boolean getExposeWebInfOnDispatch() {
        return ExposeWebInfOnDispatch;
    }

    /**
     * @return the decodeurlplussign
     */
    public Boolean getDecodeurlplussign() {
        return decodeurlplussign;
    }

    /**
     * @return the channelwritetype
     */
    public String getChannelwritetype() {
        return channelwritetype;
    }

    /**
     * @return the suppressHtmlRecursiveErrorOutput
     */
    public Boolean getSuppressHtmlRecursiveErrorOutput() {
        return suppressHtmlRecursiveErrorOutput;
    }

    /**
     * @return the fileWrapperEvents
     */
    public Boolean getFilewrapperevents() {
        return fileWrapperEvents;
    }

    /**
     * @return the webgroupvhostnotfound
     */
    public String getWebgroupvhostnotfound() {
        return webgroupvhostnotfound;
    }

    /**
     * @return the defaultTraceRequestBehavior
     */
    public Boolean getDefaultTraceRequestBehavior() {
        return defaultTraceRequestBehavior;
    }

    /**
     * @return the defaultHeadRequestBehavior
     */
    public Boolean getDefaultHeadRequestBehavior() {
        return defaultHeadRequestBehavior;
    }

    /**
     * @return the tolerateSymbolicLinks
     */
    public Boolean getTolerateSymbolicLinks() {
        return tolerateSymbolicLinks;
    }

    /**
     * @return the symbolicLinksCacheSize
     */
    public Integer getSymbolicLinksCacheSize() {
        return symbolicLinksCacheSize;
    }

    /**
     * @return the enableErrorExceptionTypeFirst
     */
    public Boolean getEnableErrorExceptionTypeFirst() {
        return enableErrorExceptionTypeFirst;
    }

    /**
     * @return the enablemultireadofpostdata
     */
    public Boolean getEnablemultireadofpostdata() {
        return enablemultireadofpostdata;
    }

    /**
     * @return the skipInputStreamRead
     */
    public Boolean getSkipinputstreamread() {
        return skipInputStreamRead;
    }

    /**
     * @return the httpOnlyCookies
     */
    public String getHttponlycookies() {
        return httpOnlyCookies;
    }

    /**
     * @return the invokeFilterInitAtStartup
     */
    public Boolean getInvokeFilterInitAtStartup() {
        return invokeFilterInitAtStartup;
    }

    /**
     * @return the enablejspmappingoverride
     */
    public Boolean getEnablejspmappingoverride() {
        return enablejspmappingoverride;
    }

    /**
     * @return the enabledefaultiselignoredintag
     */
    public Boolean getEnabledefaultiselignoredintag() {
        return enabledefaultiselignoredintag;
    }

    /**
     * @return the parseutf8postdata
     */
    public Boolean getParseutf8postdata() {
        return parseutf8postdata;
    }

    /**
     * @return the logservletcontainerinitializerclassloadingerrors
     */
    public Boolean getLogservletcontainerinitializerclassloadingerrors() {
        return logservletcontainerinitializerclassloadingerrors;
    }

    /**
     * @return the allowincludesenderror
     */
    public Boolean getAllowincludesenderror() {
        return allowincludesenderror;
    }

    /**
     * @return the skipmetainfresourcesprocessing
     */
    public Boolean getSkipmetainfresourcesprocessing() {
        return skipmetainfresourcesprocessing;
    }

    /**
     * @return the metainfresourcescachesize
     */
    public Integer getMetainfresourcescachesize() {
        return metainfresourcescachesize;
    }

    public Boolean getDeferServletLoad() {
        return deferServletLoad;
    }

    /**
     * @return the asyncmaxsizetaskpool
     */
    public Integer getAsyncmaxsizetaskpool() {
        return asyncmaxsizetaskpool;
    }

    /**
     * @return the asyncpurgeinterval
     */
    public Integer getAsyncpurgeinterval() {
        return asyncpurgeinterval;
    }

    /**
     * @return the asynctimeoutdefault
     */
    public Integer getAsynctimeoutdefault() {
        return asynctimeoutdefault;
    }

    /**
     * @return the asynctimerthreads
     */
    public Integer getAsynctimerthreads() {
        return asynctimerthreads;
    }

    /**
     * @return the xpoweredby
     */
    public String getXpoweredby() {
        return xpoweredby;
    }

    /**
     * @return the disablexpoweredby
     */
    public Boolean getDisablexpoweredby() {
        return disablexpoweredby;
    }

    /**
     * @return the enableDefaultServletRequestPathElements
     */
    public Boolean getEnableDefaultServletRequestPathElements() {
        return enableDefaultServletRequestPathElements;
    }

    /**
     * @return the copyattributeskeyset
     */
    public Boolean getCopyattributeskeyset() {
        return copyattributeskeyset;
    }

    /**
     * @return the contentType
     */
    public String getContenttype() {
        return contentType;
    }

    /**
     * @return the mapfilterstoasterisk
     */
    public Boolean getMapfilterstoasterisk() {
        return mapFiltersToAsterisk;
    }

    /**
     * @return the invokeFiltersCompatibility
     */
    public Boolean getInvokefilterscompatibility() {
        return invokeFiltersCompatibility;
    }

    /**
     * @return the DisableSetCharacterEncodingAfterParametersRead
     */
    public Boolean getDisablesetcharacterencodingafterparametersread() {
        return DisableSetCharacterEncodingAfterParametersRead;
    }

    /**
     * @return the provideqStringToWelcomeFile
     */
    public Boolean getProvideqstringtowelcomefile() {
        return provideqStringToWelcomeFile;
    }

    /**
     * @return the tolerateLocaleMismatchForServingFiles
     */
    public Boolean getToleratelocalemismatchforservingfiles() {
        return tolerateLocaleMismatchForServingFiles;
    }

    /**
     * @return the tolerateLocaleMismatchForServingFiles
     */
    public Boolean getServewelcomefilefromextendeddocumentroot() {
        return serveWelcomeFileFromExtendedDocumentRoot;
    }

    /**
     * @return the discernUnavailableServlet
     */
    public Boolean getDiscernunavailableservlet() {
        return discernUnavailableServlet;
    }

    /**
     * @return the reInitServletOnInitUnavailableException
     */
    public Boolean getReInitServletOnInitUnavailableException() {
        return reInitServletOnInitUnavailableException;
    }

    /**
     * @return the setUnencodedHTMLinSendError
     */
    public Boolean getSetUnencodedhtmlinsenderror() {
        return setUnencodedHTMLinSendError;
    }

    /**
     * @return the sendResponseToClientWhenResponseIsComplete
     */
    public Boolean getSendresponsetoclientwhenresponseiscomplete() {
        return sendResponseToClientWhenResponseIsComplete;
    }

    /**
     * @return the sendResponseToClientAsPartOfSendRedirect
     */
    public Boolean getSendresponsetoclientaspartofsendredirect() {
        return sendResponseToClientAsPartOfSendRedirect;
    }

    /**
     * @return the keepUnReadPostDataAfterResponseSentToClient
     */
    public Boolean getKeepunreadpostdataafterresponsesenttoclient() {
        return keepUnReadPostDataAfterResponseSentToClient;
    }

    /**
     * @return the finishResponseOnClose
     */
    public Boolean getFinishresponseonclose() {
        return finishResponseOnClose;
    }

    /**
     * @return the globalListener
     */
    public Boolean getGloballistener() {
        return globalListener;
    }

    /**
     * @return the checkForceWorkRejected
     */
    public Boolean getCheckforceworkrejected() {
        return checkForceWorkRejected;
    }

    /**
     * @return the enablePartialURLtoExtendedDocumentRoot
     */
    public Boolean getEnablepartialurltoextendeddocumentroot() {
        return enablePartialURLtoExtendedDocumentRoot;
    }

    /**
     * @return the checkedRingetRealPath
     */
    public Boolean getCheckedringetrealpath() {
        return checkedRingetRealPath;
    }

    /**
     * @return the disableServletContainerInitializersOnPreV8Apps
     */
    public Boolean getdisableServletContainerInitializersOnPreV8Apps() {
        return disableServletContainerInitializersOnPreV8Apps;
    }

    public Boolean getIgnoreInvalidQueryString() {
        return ignoreInvalidQueryString;
    }

    //PM71666
    public Boolean getSetContentLengthOnClose() {
        return setContentLengthOnClose;
    }

    //PM83452,PM82876
    /**
     * @return the allowdotsinname
     */
    public Boolean getAllowdotsinname() {
        return allowdotsinname;
    }

    /**
     * @return the destroyservletonserviceunavailableexception
     */
    public Boolean getDestroyservletonserviceunavailableexception() {
        return destroyservletonserviceunavailableexception;
    } //PM98245

    /**
     * @return the normalizerequesturi
     */
    public Boolean getNormalizerequesturi() {
        return normalizerequesturi;
    } //PI05525

    /**
     * @return the displaytextwhennoerrorpagedefined
     */
    public String getDisplaytextwhennoerrorpagedefined() {
        return displaytextwhennoerrorpagedefined;
    } //PI09474

    /**
     * @return the throwExceptionForAddELResolver
     */
    public Boolean getThrowExceptionForAddELResolver() {
        return throwExceptionForAddELResolver;
    } //PM05903

    public Boolean getEvalExpressionFollowingTwoBackslashes() {
        return evalExpressionFollowingTwoBackslashes;
    } //PM81674

    public Boolean getLimitBuffer() {
        return limitBuffer;
    } //PM81674

    /**
     * @return the allowdefaulterrorpage
     */
    public Boolean getAllowdefaulterrorpage() {
        return allowdefaulterrorpage;
    } //PI05845

    public Boolean getPreserveRequestParameterValues() {
        return preserveRequestParameterValues;
    } //PI20210

    public Boolean getWriterOnEmptyBuffer() {
        return writerOnEmptyBuffer;
    } //PK90190

    public Boolean getEmptyServletMappings() {
        return emptyServletMappings;
    } //PI23529

    public Boolean getParameterReturnEmptyString() {
        return parameterReturnEmptyString;
    } //PK56156

    public Boolean getRedirectWithPathInfo() {
        return redirectWithPathInfo;
    } //PK23779

    /**
     * @return the deferservletrequestlistenerdestroyonerror
     */
    public Boolean getDeferServletRequestListenerDestroyOnError() {
        return deferServletRequestListenerDestroyOnError;
    } //PI26908

    public Boolean getAllowExpressionFactoryPerApp() {
        return allowExpressionFactoryPerApp;
    } //PI31922

    /**
     * @return the useMaxRequestsizeforMultipart
     */
    public Boolean getUseMaxRequestsizeforMultipart() {
        return useMaxRequestsizeforMultipart;
    }

    public final static String XML_ATTRIBUTE_NAME_LISTENERS = "listeners";

    public void setListeners(String s) {
        this.listeners = s;
    }

    public final static String XML_ATTRIBUTE_NAME_DECODE_URL_AS_UTF8 = "decodeurlasutf8";

    public void setDecodeurlasutf8(Boolean b) {
        this.decodeurlasutf8 = b;
    }

    public final static String XML_ATTRIBUTE_NAME_FILE_SERVING_ENABLED = "fileservingenabled";

    public void setFileservingenabled(Boolean b) {
        this.fileservingenabled = b;
    }

    public final static String XML_ATTRIBUTE_NAME_DISALLOW_ALL_FILE_SERVING = "com.ibm.ws.webcontainer.disallowAllFileServing";

    public void setDisallowAllFileServing(Boolean b) {
        this.disallowAllFileServing = b;
    }

    public final static String XML_ATTRIBUTE_NAME_DIRECTORY_BROWSING_ENABLED = "directorybrowsingenabled";

    public void setDirectorybrowsingenabled(Boolean b) {
        this.directorybrowsingenabled = b;
    }

    public final static String XML_ATTRIBUTE_NAME_SERVE_SERVLETS_BY_CLASSNAME_ENABLED = "serveServletsByClassnameEnabled";

    public void setServeServletsByClassnameEnabled(Boolean b) {
        this.serveServletsByClassnameEnabled = b;
    }

    public final static String XML_ATTRIBUTE_NAME_DISALLOW_SERVE_SERVLET_BY_CLASSNAME = "com.ibm.ws.webcontainer.disallowserveservletsbyclassname";

    public void setDisallowserveservletsbyclassname(Boolean b) {
        this.disallowserveservletsbyclassname = b;
    }

    public final static String XML_ATTRIBUTE_NAME_DO_NOT_SERVER_BY_CLASSNAME = "com.ibm.ws.webcontainer.donotservebyclassname";

    public void setDonotservebyclassname(String s) {
        this.donotservebyclassname = s;
    }

    public final static String XML_ATTRIBUTE_NAME_TRUST_HOST_HEADER_PORT = "trusthostheaderport";

    public void setTrusthostheaderport(Boolean b) {
        this.trusthostheaderport = b;
    }

    public final static String XML_ATTRIBUTE_NAME_TRUSTED = "trusted";

    public void setTrusted(Boolean b) {
        this.trusted = b;
    }

    public final static String XML_ATTRIBUTE_NAME_EXTRACT_HOST_HEADER_PORT = "com.ibm.ws.webcontainer.extracthostheaderport";

    public void setExtracthostheaderport(Boolean b) {
        this.extracthostheaderport = b;
    }

    public final static String XML_ATTRIBUTE_NAME_HTTPS_INDICATOR_HEADER = "httpsindicatorheader";

    public void setHttpsindicatorheader(String s) {
        this.httpsindicatorheader = s;
    }

    public final static String XML_ATTRIBUTE_NAME_EXPOSE_WEB_INF_ON_DISPATCH = "ExposeWebInfOnDispatch";

    public void setExposeWebInfOnDispatch(Boolean b) {
        this.ExposeWebInfOnDispatch = b;
    }

    public final static String XML_ATTRIBUTE_NAME_DECODE_URL_PLUS_SIGN = "com.ibm.ws.webcontainer.decodeurlplussign";

    public void setDecodeurlplussign(Boolean b) {
        this.decodeurlplussign = b;
    }

    public final static String XML_ATTRIBUTE_NAME_CHANNEL_WRITE_TYPE = "com.ibm.ws.webcontainer.channelwritetype";

    public void setChannelwritetype(String s) {
        this.channelwritetype = s;
    }

    public final static String XML_ATTRIBUTE_NAME_SUPPRESS_HTML_RECURSIVE_ERROR_OUTPUT = "com.ibm.ws.webcontainer.suppressHtmlRecursiveErrorOutput";

    public void setSuppressHtmlRecursiveErrorOutput(Boolean b) {
        this.suppressHtmlRecursiveErrorOutput = b;
    }

    public final static String XML_ATTRIBUTE_NAME_FILE_WRAPPER_EVENTS = "com.ibm.ws.webcontainer.fileWrapperEvents";

    public void setFilewrapperevents(Boolean b) {
        this.fileWrapperEvents = b;
    }

    public final static String XML_ATTRIBUTE_NAME_WEB_GROUP_HOST_NOT_FOUND = "com.ibm.ws.webcontainer.webgroupvhostnotfound";

    public void setWebgroupvhostnotfound(String s) {
        this.webgroupvhostnotfound = s;
    }

    public final static String XML_ATTRIBUTE_NAME_INVOKE_FILTERS_COMPATIBILITY = "com.ibm.ws.webcontainer.invokefilterscompatibility";

    public void setInvokefilterscompatibility(Boolean b) {
        this.invokeFiltersCompatibility = b;
    }

    public final static String XML_ATTRIBUTE_NAME_DEFAULT_TRACE_REQUEST_BEHAVIOR = "com.ibm.ws.webcontainer.DefaultTraceRequestBehavior";

    public void setDefaultTraceRequestBehavior(Boolean b) {
        this.defaultTraceRequestBehavior = b;
    }

    public final static String XML_ATTRIBUTE_NAME_DEFAULT_HEAD_REQUEST_BEHAVIOR = "com.ibm.ws.webcontainer.DefaultHeadRequestBehavior";

    public void setDefaultHeadRequestBehavior(Boolean b) {
        this.defaultHeadRequestBehavior = b;
    }

    public final static String XML_ATTRIBUTE_NAME_TOLERATE_SYMBOLIC_LINKS = "com.ibm.ws.webcontainer.TolerateSymbolicLinks";

    public void setTolerateSymbolicLinks(Boolean b) {
        this.tolerateSymbolicLinks = b;
    }

    public final static String XML_ATTRIBUTE_NAME_SYMBOLIC_LINKS_CACHE_SIZE = "com.ibm.ws.webcontainer.SymbolicLinksCacheSize";

    public void setSymbolicLinksCacheSize(Integer i) {
        this.symbolicLinksCacheSize = i;
    }

    public final static String XML_ATTRIBUTE_NAME_ENABLE_ERROR_EXCEPTION_TYPE_FIRST = "com.ibm.ws.webcontainer.enableErrorExceptionTypeFirst";

    public void setEnableErrorExceptionTypeFirst(Boolean b) {
        this.enableErrorExceptionTypeFirst = b;
    }

    public final static String XML_ATTRIBUTE_NAME_ENABLE_MULTI_READ_OF_POST_DATA = "com.ibm.ws.webcontainer.enablemultireadofpostdata";

    public void setEnablemultireadofpostdata(Boolean b) {
        this.enablemultireadofpostdata = b;
    }

    public final static String XML_ATTRIBUTE_NAME_SKIP_INPUT_STREAM_READ = "com.ibm.ws.webcontainer.skipinputstreamread";

    public void setSkipinputstreamread(Boolean b) {
        this.skipInputStreamRead = b;
    }

    public final static String XML_ATTRIBUTE_NAME_HTTP_ONLY_COOKIES = "com.ibm.ws.webcontainer.httponlycookies";

    public void setHttponlycookies(String b) {
        this.httpOnlyCookies = b;
    }

    public final static String XML_ATTRIBUTE_NAME_INVOKE_FILTER_AT_STARTUP = "com.ibm.ws.webcontainer.invokeFilterInitAtStartup";

    public void setInvokeFilterInitAtStartup(Boolean b) {
        this.invokeFilterInitAtStartup = b;
    }

    public final static String XML_ATTRIBUTE_NAME_ENABLE_JSP_MAPPING_OVERRIDE = "com.ibm.ws.webcontainer.enablejspmappingoverride";

    public void setEnablejspmappingoverride(Boolean b) {
        this.enablejspmappingoverride = b;
    }

    public final static String XML_ATTRIBUTE_NAME_ENABLE_DEFAULT_IS_EL_IGNORED_IN_TAG = "com.ibm.ws.jsp.enabledefaultiselignoredintag";

    public void setEnabledefaultiselignoredintag(Boolean b) {
        this.enabledefaultiselignoredintag = b;
    }

    public final static String XML_ATTRIBUTE_NAME_PARSE_UTF8_POST_DATA = "com.ibm.ws.webcontainer.parseutf8postdata";

    public void setParseutf8postdata(Boolean b) {
        this.parseutf8postdata = b;
    }

    public final static String XML_ATTRIBUTE_NAME_LOG_SERVLET_CONTAINER_INITIALIZER_CLASSLOADING_ERRORS = "com.ibm.ws.webcontainer.logservletcontainerinitializerclassloadingerrors";

    public void setLogservletcontainerinitializerclassloadingerrors(Boolean b) {
        this.logservletcontainerinitializerclassloadingerrors = b;
    }

    public final static String XML_ATTRIBUTE_NAME_ALLOWS_INCLUDES_END_ERROR = "com.ibm.ws.webcontainer.allowincludesenderror";

    public void setAllowincludesenderror(Boolean b) {
        this.allowincludesenderror = b;
    }

    public final static String XML_ATTRIBUTE_NAME_SKIP_META_RESOURCES_PROCESSING = "com.ibm.ws.webcontainer.skipmetainfresourcesprocessing";

    public void setSkipmetainfresourcesprocessing(Boolean b) {
        this.skipmetainfresourcesprocessing = b;
    }

    public final static String XML_ATTRIBUTE_NAME_META_INF_RESOURCES_CACHE_SIZE = "com.ibm.ws.webcontainer.metainfresourcescachesize";

    public void setMetainfresourcescachesize(Integer i) {
        this.metainfresourcescachesize = i;
    }

    public final static String XML_ATTRIBUTE_NAME_ASYNC_MAX_SIZE_TASK_POOL = "com.ibm.ws.webcontainer.asyncmaxsizetaskpool";

    public void setAsyncmaxsizetaskpool(Integer i) {
        this.asyncmaxsizetaskpool = i;
    }

    public final static String XML_ATTRIBUTE_NAME_DEFER_SERVLET_LOAD = "deferServletLoad";

    public void setDeferServletLoad(Boolean deferServletLoad) {
        this.deferServletLoad = deferServletLoad;
    }

    public final static String XML_ATTRIBUTE_NAME_ASYNC_PURGE_INTERVAL = "com.ibm.ws.webcontainer.asyncpurgeinterval";

    public void setAsyncpurgeinterval(Integer i) {
        this.asyncpurgeinterval = i;
    }

    public final static String XML_ATTRIBUTE_NAME_ASYNC_TIMOUT_DEFAULT = "com.ibm.ws.webcontainer.asynctimeoutdefault";

    public void setAsynctimeoutdefault(Integer i) {
        this.asynctimeoutdefault = i;
    }

    public final static String XML_ATTRIBUTE_NAME_ASYNC_TIMER_THREADS = "com.ibm.ws.webcontainer.asynctimerthreads";

    public void setAsynctimerthreads(Integer i) {
        this.asynctimerthreads = i;
    }

    public final static String XML_ATTRIBUTE_NAME_X_POWERED_BY = "com.ibm.ws.webcontainer.xpoweredby";

    public void setXpoweredby(String s) {
        this.xpoweredby = s;
    }

    public final static String XML_ATTRIBUTE_NAME_DISABLE_X_POWERED_BY = "com.ibm.ws.webcontainer.disablexpoweredby";

    public void setDisablexpoweredby(Boolean b) {
        this.disablexpoweredby = b;
    }

    public final static String XML_ATTRIBUTE_NAME_ENABLE_DEFAULT_SERVLET_REQUEST_PATH_ELEMENTS = "com.ibm.ws.webcontainer.enabledefaultservletrequestpathelements";

    public void setEnableDefaultServletRequestPathElements(Boolean e) {
        this.enableDefaultServletRequestPathElements = e;
    }

    public final static String XML_ATTRIBUTE_NAME_COPY_ATTRIBUTE_KEY_SET = "com.ibm.ws.webcontainer.copyattributeskeyset";

    public void setCopyattributeskeyset(Boolean e) {
        this.copyattributeskeyset = e;
    }

    public final static String XML_ATTRIBUTE_NAME_CONTENT_TYPE_COMPATIBILITY = "com.ibm.ws.webcontainer.contenttypecompatibility";

    public void setContenttype(String v) {
        this.contentType = v;
    }

    public final static String XML_ATTRIBUTE_NAME_MAP_FILTERS_TO_ASTERISK_ = "com.ibm.ws.webcontainer.mapfilterstoasterisk";

    public void setMapfilterstoasterisk(Boolean e) {
        this.mapFiltersToAsterisk = e;
    }

    public final static String XML_ATTRIBUTE_NAME_DISABLE_SET_CHARACTER_ENCODING_AFTER_PARAMETERS_READ = "com.ibm.ws.webcontainer.disablesetcharacterencodingafterparametersread";

    public void setDisablesetcharacterencodingafterparametersread(Boolean e) {
        this.DisableSetCharacterEncodingAfterParametersRead = e;
    }

    public final static String XML_ATTRIBUTE_NAME_PROVIDE_STRING_TO_WELCOME_FILE = "com.ibm.ws.webcontainer.provideqstringtowelcomefile";

    public void setProvideqstringtowelcomefile(Boolean e) {
        this.provideqStringToWelcomeFile = e;
    }

    public final static String XML_ATTRIBUTE_NAME_TOLERATE_LOCALE_MISMATCH_FOR_SERVING_FILES = "com.ibm.ws.webcontainer.toleratelocalemismatchforservingfiles";

    public void setToleratelocalemismatchforservingfiles(Boolean e) {
        this.tolerateLocaleMismatchForServingFiles = e;
    }

    public final static String XML_ATTRIBUTE_NAME_SERVER_WELCOME_FILE_FROM_EXTENDED_DOCUMENT_ROOT = "com.ibm.ws.webcontainer.servewelcomefilefromextendeddocumentroot";

    public void setServewelcomefilefromextendeddocumentroot(Boolean e) {
        this.serveWelcomeFileFromExtendedDocumentRoot = e;
    }

    public final static String XML_ATTRIBUTE_NAME_DISCERN_UNAVAILABLE_SERVLET = "com.ibm.ws.webcontainer.discernunavailableservlet";

    public void setDiscernunavailableservlet(Boolean e) {
        this.discernUnavailableServlet = e;
    }

    public final static String XML_ATTRIBUTE_NAME_REINIT_SERVLET_ON_INIT_UNAVAILABLE_EXCEPTION = "com.ibm.ws.webcontainer.reinitservletoninitunavailableexception";

    public void setReInitServletOnInitUnavailableException(Boolean e) {
        this.reInitServletOnInitUnavailableException = e;
    }

    public final static String XML_ATTRIBUTE_NAME_SET_UNENCODED_HTML_IN_SEND_ERROR = "com.ibm.ws.webcontainer.setunencodedhtmlinsenderror";

    public void setSetUnencodedhtmlinsenderror(Boolean e) {
        this.setUnencodedHTMLinSendError = e;
    }

    public final static String XML_ATTRIBUTE_NAME_SEND_RESPONSE_TO_CLIENT_WHEN_RESPONSE_IS_COMPLETE = "com.ibm.ws.webcontainer.sendresponsetoclientwhenresponseiscomplete";

    public void setSendresponsetoclientwhenresponseiscomplete(Boolean e) {
        this.sendResponseToClientWhenResponseIsComplete = e;
    }

    public final static String XML_ATTRIBUTE_NAME_SEND_RESPONSE_TO_CLIENT_AS_PART_OF_SEND_DIRECT = "com.ibm.ws.webcontainer.sendresponsetoclientaspartofsendredirect";

    public void setSendresponsetoclientaspartofsendredirect(Boolean e) {
        this.sendResponseToClientAsPartOfSendRedirect = e;
    }

    public final static String XML_ATTRIBUTE_NAME_KEEP_UNREAD_POST_DATA_AFTER_RESPONSE_SENT_TO_CLIENT = "com.ibm.ws.webcontainer.keepunreadpostdataafterresponsesenttoclient";

    public void setKeepunreadpostdataafterresponsesenttoclient(Boolean e) {
        this.keepUnReadPostDataAfterResponseSentToClient = e;
    }

    public final static String XML_ATTRIBUTE_NAME_FINISH_RESPONSE_ON_CLOSE = "com.ibm.ws.webcontainer.finishresponseonclose";

    public void setFinishresponseonclose(Boolean e) {
        this.finishResponseOnClose = e;
    }

    public final static String XML_ATTRIBUTE_NAME_GLOBAL_LISTENER = "com.ibm.webcontainer.fvt.listeners.globallistener";

    public void setGloballistener(Boolean e) {
        this.globalListener = e;
    }

    public final static String XML_ATTRIBUTE_NAME_CHECK_FORCE_WORK_REJECTED = "com.ibm.ws.webcontainer.checkforceworkrejected";

    public void setCheckforceworkrejected(Boolean e) {
        this.checkForceWorkRejected = e;
    }

    public final static String XML_ATTRIBUTE_NAME_ENABLE_ = "com.ibm.ws.webcontainer.enablepartialurltoextendeddocumentroot";

    public void setEnablepartialurltoextendeddocumentroot(Boolean e) {
        this.enablePartialURLtoExtendedDocumentRoot = e;
    }

    public final static String XML_ATTRIBUTE_NAME_CHECKED_R_IN_GET_REAL_PATH = "com.ibm.ws.webcontainer.checkedringetrealpath";

    public void setCheckedringetrealpath(Boolean e) {
        this.checkedRingetRealPath = e;
    }

    public final static String XML_ATTRIBUTE_NAME_DISABLE_SERVLET_CONTAINER_INITIALIZERS_ON_PRE_V8_APPS = "com.ibm.ws.webcontainer.disableServletContainerInitializersOnPreV8Apps";

    public void setdisableServletContainerInitializersOnPreV8Apps(Boolean e) {
        this.disableServletContainerInitializersOnPreV8Apps = e;
    }

    public final static String XML_ATTRIBUTE_NAME_IGNORE_INVALID_QUERY_STRING = "com.ibm.ws.webcontainer.ignoreInvalidQueryString";

    public void setIgnoreInvalidQueryString(Boolean e) {
        this.ignoreInvalidQueryString = e;
    }

    //PM71666
    public final static String XML_ATTRIBUTE_NAME_SET_CONTEXT_LENGTH_ON_CLOSE = "setcontentlengthonclose";

    public void setSetContentLengthOnClose(Boolean setContentLengthOnClose) {
        this.setContentLengthOnClose = setContentLengthOnClose;
    }

    /**
     * @param allowdotsinname the allowdotsinname to set
     */
    public final static String XML_ATTRIBUTE_NAME_ALLOW_DOTS_IN_NAME = "com.ibm.ws.webcontainer.allowdotsinname";

    public void setAllowdotsinname(Boolean allowdotsinname) {
        this.allowdotsinname = allowdotsinname;
    }

    //PM88028 Start
    /**
     * @return the useoriginalrequeststate
     */
    public Boolean getUseoriginalrequeststate() {
        return useoriginalrequeststate;
    }

    /**
     * @param useoriginalrequeststate the useoriginalrequeststate to set
     */
    public final static String XML_ATTRIBUTE_NAME_USE_ORIGINAL_REQUEST_STATE = "com.ibm.ws.webcontainer.useoriginalrequeststate";

    public void setUseoriginalrequeststate(Boolean useoriginalrequeststate) {
        this.useoriginalrequeststate = useoriginalrequeststate;
    }
    //PM88028 End

    /**
     * @param destroyservletonserviceunavailableexception the destroyservletonserviceunavailableexception to set
     */
    public final static String XML_ATTRIBUTE_NAME_DESTROY_SERVLET_ON_SERVICE_UNAVAILABLE_EXCEPTION = "com.ibm.ws.webcontainer.destroyservletonserviceunavailableexception";

    public void setDestroyservletonserviceunavailableexception(Boolean destroyservletonserviceunavailableexception) {
        this.destroyservletonserviceunavailableexception = destroyservletonserviceunavailableexception;
    } //PM98245

    /**
     * @param normalizerequesturi the normalizerequesturi to set
     */
    public final static String XML_ATTRIBUTE_NAME_NORMALIZE_REQUEST_URI = "com.ibm.ws.webcontainer.normalizerequesturi";

    public void setNormalizerequesturi(Boolean normalizerequesturi) {
        this.normalizerequesturi = normalizerequesturi;
    }

    /**
     * @param displaytextwhennoerrorpagedefined the displaytextwhennoerrorpagedefined to set
     */
    public final static String XML_ATTRIBUTE_NAME_DISPLAY_TEST_WHEN_NO_ERROR_PAGE_DEFINED = "com.ibm.ws.webcontainer.displaytextwhennoerrorpagedefined";

    public void setDisplaytextwhennoerrorpagedefined(String displaytextwhennoerrorpagedefined) {
        this.displaytextwhennoerrorpagedefined = displaytextwhennoerrorpagedefined;
    } //PI09474

    public final static String XML_ATTRIBUTE_NAME_THROW_EXCEPTION_FOR_ADD_EL_RESOLVER = "com.ibm.ws.jsp.throwExceptionForAddELResolver";

    public void setThrowExceptionForAddELResolver(Boolean throwExceptionForAddELResolver) {
        this.throwExceptionForAddELResolver = throwExceptionForAddELResolver;
    } //PM05903

    /**
     * @param evalExpressionFollowingTwoBackslashes the evalExpressionFollowingTwoBackslashes to set
     */
    public final static String XML_ATTRIBUTE_NAME_EVAL_EXPRESSION_FOLLOWING_TWO_BACKSLASHES = "com.ibm.ws.jsp.evalexpressionfollowingtwobackslashes";

    public void setEvalExpressionFollowingTwoBackslashes(Boolean evalExpressionFollowingTwoBackslashes) {
        this.evalExpressionFollowingTwoBackslashes = evalExpressionFollowingTwoBackslashes;
    } //PM81674

    /**
     * @param limitBuffer the limitBuffer to set
     */
    public final static String XML_ATTRIBUTE_NAME_LIMIT_BUFFER = "com.ibm.ws.jsp.limitBuffer";

    public void setLimitBuffer(Boolean limitBuffer) {
        this.limitBuffer = limitBuffer;
    } //PK95332

    /**
     * @param allowdefaulterrorpage the allowdefaulterrorpage to set
     */
    public final static String XML_ATTRIBUTE_NAME_ALLOW_DEFAULT_ERROR_PAGE = "com.ibm.ws.webcontainer.allowdefaulterrorpage";

    public void setAllowdefaulterrorpage(Boolean allowdefaulterrorpage) {
        this.allowdefaulterrorpage = allowdefaulterrorpage;
    } //PI05845

    /**
     * @param preserveRequestParameterValues to return a clone of the original request param values
     */
    public final static String XML_ATTRIBUTE_NAME_PRESERVE_REQUEST_PARAMETER_VALUES = "com.ibm.ws.webcontainer.preserverequestparametervalues";

    public void setPreserveRequestParameterValues(Boolean preserveRequestParameterValues) {
        this.preserveRequestParameterValues = preserveRequestParameterValues;
    } //PI20210

    /**
     * @param writerOnEmptyBuffer the writerOnEmptyBuffer to set
     */
    public final static String XML_ATTRIBUTE_NAME_WRITE_ON_EMPTY_BUFFER = "com.ibm.ws.jsp.getwriteronemptybuffer";

    public void setWriterOnEmptyBuffer(Boolean writerOnEmptyBuffer) {
        this.writerOnEmptyBuffer = writerOnEmptyBuffer;
    } //PK90190

    /**
     * @param emptyServletMappings - true to return an empty list when mappings are empty, false to return null
     */
    public final static String XML_ATTRIBUTE_NAME_EMPTY_SERVLET_MAPPINGS = "com.ibm.ws.webcontainer.emptyservletmappings";

    public void setEmptyServletMappings(Boolean emptyServletMappings) {
        this.emptyServletMappings = emptyServletMappings;
    } //PI23529

    /**
     * @param parameterReturnEmptyString - true to return an empty string when calling jsp:getProperty on a property that has
     *                                       not been set , false to return null
     */
    public final static String XML_ATTRIBUTE_NAME_PARAMETER_RETURN_EMPTY_STRING = "com.ibm.ws.jsp.getparameterreturnemptystring";

    public void setParameterReturnEmptyString(Boolean parameterReturnEmptyString) {
        this.parameterReturnEmptyString = parameterReturnEmptyString;
    } //PK56156

    /**
     * @param redirectWithPathInfo - true to allow redirect with the request URI's path info, false to redirect using
     *                                 only the request URI
     */
    public final static String XML_ATTRIBUTE_NAME_REDIRECT_WITH_PATH_INFO = "com.ibm.ws.webcontainer.redirectwithpathinfo";

    public void setRedirectWithPathInfo(Boolean redirectWithPathInfo) {
        this.redirectWithPathInfo = redirectWithPathInfo;
    } //PK23779

    /**
     * @param deferServletRequestListenerDestroyOnError the deferServletRequestListenerDestroyOnError to set
     */
    public final static String XML_ATTRIBUTE_NAME_DESTROY_SERVLET_REQUEST_LISTENER_DESTROY_ON_ERROR = "com.ibm.ws.webcontainer.deferservletrequestlistenerdestroyonerror";

    public void setDeferServletRequestListenerDestroyOnError(Boolean deferServletRequestListenerDestroyOnError) {
        this.deferServletRequestListenerDestroyOnError = deferServletRequestListenerDestroyOnError;
    } //PI26908

    /**
     * @param allowExpressionFactoryPerApp - true to allow multiple ExpressionFactory implementations, false to only use
     *                                         ExpressionFactory of first loaded application
     */
    public final static String XML_ATTRIBUTE_NAME_ALLOW_EXPRESSION_FACTORY_PER_APP = "com.ibm.ws.jsp.allowexpressionfactoryperapp";

    public void setAllowExpressionFactoryPerApp(Boolean allowExpressionFactoryPerApp) {
        this.allowExpressionFactoryPerApp = allowExpressionFactoryPerApp;
    } //PI31922

    /**
     * @param useMaxRequestsizeforMultipart the useMaxRequestsizeforMultipart to set
     */
    public final static String XML_ATTRIBUTE_NAME_USE_MAX_REQUEST_SIZE_FORM_MULTIPART = "com.ibm.ws.webcontainer.usemaxrequestsizeformultipart";

    public void setUseMaxRequestsizeforMultipart(Boolean useMaxRequestsizeforMultipart) {
        this.useMaxRequestsizeforMultipart = useMaxRequestsizeforMultipart;
    } //PI75528

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("WebContainerElement{");
        if (listeners != null)
            buf.append("listeners=\"" + listeners + "\" ");
        if (decodeurlasutf8 != null)
            buf.append("decodeurlasutf8=\"" + decodeurlasutf8 + "\" ");
        if (fileservingenabled != null)
            buf.append("fileservingenabled=\"" + fileservingenabled + "\" ");
        if (disallowAllFileServing != null)
            buf.append("disallowAllFileServing=\"" + disallowAllFileServing + "\" ");
        if (directorybrowsingenabled != null)
            buf.append("directorybrowsingenabled=\"" + directorybrowsingenabled + "\" ");
        if (serveServletsByClassnameEnabled != null)
            buf.append("serveServletsByClassnameEnabled=\"" + serveServletsByClassnameEnabled + "\" ");
        if (disallowserveservletsbyclassname != null)
            buf.append("disallowserveservletsbyclassname=\"" + disallowserveservletsbyclassname + "\" ");
        if (donotservebyclassname != null)
            buf.append("donotservebyclassname=\"" + donotservebyclassname + "\" ");
        if (trusthostheaderport != null)
            buf.append("trusthostheaderport=\"" + trusthostheaderport + "\" ");
        if (trusted != null)
            buf.append("trusted=\"" + trusted + "\" ");
        if (extracthostheaderport != null)
            buf.append("extracthostheaderport=\"" + extracthostheaderport + "\" ");
        if (httpsindicatorheader != null)
            buf.append("httpsindicatorheader=\"" + httpsindicatorheader + "\" ");
        if (ExposeWebInfOnDispatch != null)
            buf.append("ExposeWebInfOnDispatch=\"" + ExposeWebInfOnDispatch + "\" ");
        if (decodeurlplussign != null)
            buf.append("decodeurlplussign=\"" + decodeurlplussign + "\" ");
        if (channelwritetype != null)
            buf.append("channelwritetype=\"" + channelwritetype + "\" ");
        if (suppressHtmlRecursiveErrorOutput != null)
            buf.append("suppressHtmlRecursiveErrorOutput=\"" + suppressHtmlRecursiveErrorOutput + "\" ");
        if (fileWrapperEvents != null)
            buf.append("fileWrapperEvents=\"" + fileWrapperEvents + "\" ");
        if (webgroupvhostnotfound != null)
            buf.append("webgroupvhostnotfound=\"" + webgroupvhostnotfound + "\" ");
        if (invokeFiltersCompatibility != null)
            buf.append("invokeFiltersCompatibility=\"" + invokeFiltersCompatibility + "\" ");
        if (defaultTraceRequestBehavior != null)
            buf.append("defaultTraceRequestBehavior=\"" + defaultTraceRequestBehavior + "\" ");
        if (defaultHeadRequestBehavior != null)
            buf.append("defaultHeadRequestBehavior=\"" + defaultHeadRequestBehavior + "\" ");
        if (tolerateSymbolicLinks != null)
            buf.append("tolerateSymbolicLinks=\"" + tolerateSymbolicLinks + "\" ");
        if (symbolicLinksCacheSize != null)
            buf.append("symbolicLinksCacheSize=\"" + symbolicLinksCacheSize + "\" ");
        if (enableErrorExceptionTypeFirst != null)
            buf.append("enableErrorExceptionTypeFirst=\"" + enableErrorExceptionTypeFirst + "\" ");
        if (enablemultireadofpostdata != null)
            buf.append("enablemultireadofpostdata=\"" + enablemultireadofpostdata + "\" ");
        if (skipInputStreamRead != null)
            buf.append("skipInputStreamRead=\"" + skipInputStreamRead + "\" ");
        if (httpOnlyCookies != null)
            buf.append("httpOnlyCookies=\"" + httpOnlyCookies + "\" ");
        if (invokeFilterInitAtStartup != null)
            buf.append("invokeFilterInitAtStartup=\"" + invokeFilterInitAtStartup + "\" ");
        if (enablejspmappingoverride != null)
            buf.append("enablejspmappingoverride=\"" + enablejspmappingoverride + "\" ");
        if (enabledefaultiselignoredintag != null)
            buf.append("enabledefaultiselignoredintag=\"" + enabledefaultiselignoredintag + "\" ");
        if (parseutf8postdata != null)
            buf.append("parseutf8postdata=\"" + parseutf8postdata + "\" ");
        if (logservletcontainerinitializerclassloadingerrors != null)
            buf.append("logservletcontainerinitializerclassloadingerrors=\"" + logservletcontainerinitializerclassloadingerrors + "\" ");
        if (allowincludesenderror != null)
            buf.append("allowincludesenderror=\"" + allowincludesenderror + "\" ");
        if (skipmetainfresourcesprocessing != null)
            buf.append("skipmetainfresourcesprocessing=\"" + skipmetainfresourcesprocessing + "\" ");
        if (metainfresourcescachesize != null)
            buf.append("metainfresourcescachesize=\"" + metainfresourcescachesize + "\" ");

        if (deferServletLoad != null)
            buf.append("deferServletLoad=\"" + deferServletLoad + "\" ");

        if (asyncmaxsizetaskpool != null)
            buf.append("asyncmaxsizetaskpool=\"" + asyncmaxsizetaskpool + "\" ");
        if (asyncpurgeinterval != null)
            buf.append("asyncpurgeinterval=\"" + asyncpurgeinterval + "\" ");
        if (asynctimeoutdefault != null)
            buf.append("asynctimeoutdefault=\"" + asynctimeoutdefault + "\" ");
        if (asynctimerthreads != null)
            buf.append("asynctimerthreads=\"" + asynctimerthreads + "\" ");

        if (xpoweredby != null)
            buf.append("xpoweredby=\"" + xpoweredby + "\" ");
        if (disablexpoweredby != null)
            buf.append("disablexpoweredby=\"" + disablexpoweredby + "\" ");
        if (enableDefaultServletRequestPathElements != null)
            buf.append("enableDefaultServletRequestPathElements=\"" + enableDefaultServletRequestPathElements + "\" ");
        if (copyattributeskeyset != null)
            buf.append("copyattributeskeyset=\"" + copyattributeskeyset + "\" ");
        if (mapFiltersToAsterisk != null)
            buf.append("mapFiltersToAsterisk=\"" + mapFiltersToAsterisk + "\" ");
        if (DisableSetCharacterEncodingAfterParametersRead != null)
            buf.append("DisableSetCharacterEncodingAfterParametersRead=\"" + DisableSetCharacterEncodingAfterParametersRead + "\" ");
        if (provideqStringToWelcomeFile != null)
            buf.append("provideqStringToWelcomeFile=\"" + provideqStringToWelcomeFile + "\" ");
        if (tolerateLocaleMismatchForServingFiles != null)
            buf.append("tolerateLocaleMismatchForServingFiles=\"" + tolerateLocaleMismatchForServingFiles + "\" ");
        if (serveWelcomeFileFromExtendedDocumentRoot != null)
            buf.append("serveWelcomeFileFromExtendedDocumentRoot=\"" + serveWelcomeFileFromExtendedDocumentRoot + "\" ");
        if (discernUnavailableServlet != null)
            buf.append("discernUnavailableServlet=\"" + discernUnavailableServlet + "\" ");
        if (reInitServletOnInitUnavailableException != null)
            buf.append("reInitServletOnInitUnavailableException=\"" + reInitServletOnInitUnavailableException + "\" ");
        if (setUnencodedHTMLinSendError != null)
            buf.append("setUnencodedHTMLinSendError=\"" + setUnencodedHTMLinSendError + "\" ");
        if (sendResponseToClientWhenResponseIsComplete != null)
            buf.append("sendResponseToClientWhenResponseIsComplete=\"" + sendResponseToClientWhenResponseIsComplete + "\" ");
        if (sendResponseToClientAsPartOfSendRedirect != null)
            buf.append("sendResponseToClientAsPartOfSendRedirect=\"" + sendResponseToClientAsPartOfSendRedirect + "\" ");
        if (keepUnReadPostDataAfterResponseSentToClient != null)
            buf.append("keepUnReadPostDataAfterResponseSentToClient=\"" + keepUnReadPostDataAfterResponseSentToClient + "\" ");
        if (finishResponseOnClose != null)
            buf.append("finishResponseOnClose=\"" + finishResponseOnClose + "\" ");
        if (globalListener != null)
            buf.append("globalListener=\"" + globalListener + "\" ");
        if (checkForceWorkRejected != null)
            buf.append("checkForceWorkRejected=\"" + checkForceWorkRejected + "\" ");
        if (enablePartialURLtoExtendedDocumentRoot != null)
            buf.append("enablePartialURLtoExtendedDocumentRoot=\"" + enablePartialURLtoExtendedDocumentRoot + "\" ");
        if (checkedRingetRealPath != null)
            buf.append("checkedRingetRealPath=\"" + checkedRingetRealPath + "\" ");
        if (disableServletContainerInitializersOnPreV8Apps != null)
            buf.append("disableServletContainerInitializersOnPreV8Apps=\"" + disableServletContainerInitializersOnPreV8Apps + "\" ");
        if (ignoreInvalidQueryString != null)
            buf.append("ignoreInvalidQueryString=\"" + ignoreInvalidQueryString + "\" ");
        //PM71666
        if (setContentLengthOnClose != null)
            buf.append("setContentLengthOnClose=\"" + setContentLengthOnClose + "\" ");
        //PM83452 //PM82876
        if (allowdotsinname != null)
            buf.append("allowDotsinName=\"" + allowdotsinname + "\" ");
        //PM88028
        if (useoriginalrequeststate != null)
            buf.append("useoriginalrequeststate=\"" + useoriginalrequeststate + "\" ");
        //PM98245
        if (destroyservletonserviceunavailableexception != null)
            buf.append("destroyservletonserviceunavailableexception=\"" + destroyservletonserviceunavailableexception + "\" ");
        //PI05525
        if (normalizerequesturi != null)
            buf.append("normalizerequesturi=\"" + normalizerequesturi + "\" ");
        if (displaytextwhennoerrorpagedefined != null)
            buf.append("displaytextwhennoerrorpagedefined=\"" + displaytextwhennoerrorpagedefined + "\" ");
        //PI09474

        //PM05903
        if (throwExceptionForAddELResolver != null)
            buf.append("throwExceptionForAddELResolver=\"" + throwExceptionForAddELResolver + "\" ");

        //PM81674
        if (evalExpressionFollowingTwoBackslashes != null)
            buf.append("evalExpressionFollowingTwoBackslashes=\"" + evalExpressionFollowingTwoBackslashes + "\" ");

        //PK95332
        if (limitBuffer != null)
            buf.append("limitBuffer=\"" + limitBuffer + "\" ");

        //PI05845
        if (allowdefaulterrorpage != null)
            buf.append("allowdefaulterrorpage=\"" + allowdefaulterrorpage + "\" ");

        //PI20210
        if (preserveRequestParameterValues != null)
            buf.append("preserveRequestParameterValues=\"" + preserveRequestParameterValues + "\" ");

        if (writerOnEmptyBuffer != null)
            buf.append("writerOnEmptyBuffer=\"" + writerOnEmptyBuffer + "\" ");

        //PI23529
        if (emptyServletMappings != null)
            buf.append("emptyServletMappings=\"" + emptyServletMappings + "\" ");

        //PK56156
        if (parameterReturnEmptyString != null)
            buf.append("parameterReturnEmptyString=\"" + parameterReturnEmptyString + "\" ");

        // PK23779
        if (redirectWithPathInfo != null)
            buf.append("redirectWithPathInfo=\"" + redirectWithPathInfo + "\" ");

        //PI26908
        if (deferServletRequestListenerDestroyOnError != null)
            buf.append("deferServletRequestListenerDestroyOnError=\"" + deferServletRequestListenerDestroyOnError + "\" ");

        // PI31922
        if (allowExpressionFactoryPerApp != null)
            buf.append("allowExpressionFactoryPerApp=\"" + allowExpressionFactoryPerApp + "\" ");

        //PI75528
        if (useMaxRequestsizeforMultipart != null)
            buf.append("useMaxRequestsizeforMultipart=\"" + useMaxRequestsizeforMultipart + "\" ");

        buf.append("}");
        return buf.toString();
    }
}