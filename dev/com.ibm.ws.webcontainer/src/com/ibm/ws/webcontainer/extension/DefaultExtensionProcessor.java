/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.extension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.util.WSUtil;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.exception.IncludeFileNotFoundException;
import com.ibm.ws.webcontainer.osgi.interceptor.RegisterRequestInterceptor;
import com.ibm.ws.webcontainer.osgi.interceptor.RequestInterceptor;
import com.ibm.ws.webcontainer.osgi.servlet.EntryServletWrapper;
import com.ibm.ws.webcontainer.servlet.FileServletWrapper;
import com.ibm.ws.webcontainer.servlet.ZipFileServletWrapper;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.ws.webcontainer.util.EntryResource;
import com.ibm.ws.webcontainer.util.ZipFileResource;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.webapp.WebAppErrorReport;
import com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher;
import com.ibm.ws.webcontainer.webapp.WebGroup; //PM79476
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.webcontainer.IPlatformHelper;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.ServletUtil;
import com.ibm.wsspi.webcontainer.util.URIMatcher;
import com.ibm.wsspi.webcontainer.webapp.NamespaceInvoker;

/**
 * @author asisin
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings("unchecked")
public abstract class DefaultExtensionProcessor extends WebExtensionProcessor implements NamespaceInvoker, javax.servlet.Servlet
{
	
	public static final String PARAM_DEFAULT_PAGE = "default.page";
	public static final String PARAM_BUFFER_SIZE = "bufferSize";
	// PK24615 add trailing "/" to WEB-INF_DIR and META-INF-DIR to ensure matching directories only
	public static final String WEB_INF_DIR = "WEB-INF/";
	public static final String META_INF_DIR = "META-INF/";

    private static TraceNLS nls = TraceNLS.getTraceNLS(DefaultExtensionProcessor.class, "com.ibm.ws.webcontainer.resources.Messages");
        protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.extension");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor";

	private int defaultBufferSize = 4096;
	private String esiControl = null;

	// begin pq65763
	String extendedDocumentRoot=null;
	// end pq65763
	String preFragmentExtendedDocumentRoot=null;
	
	//	 defect 220552: begin add patternList vars used to serve or deny requests related to DefaultExtensionProcessor
	private static final String DEFAULT_MAPPING = "/*";

	protected List patternList = new ArrayList();

	private static int optimizeFileServingSizeGlobal=-1;
//	private static int mappedByteBufferSizeGlobal;
	
	private int optimizeFileServingSize=1000000;
//	private int mappedByteBufferSize=-1;
	
	private static boolean useOriginalRequestState = WCCustomProperties.USE_ORIGINAL_REQUEST_STATE; //PM88028 
	private static boolean handlingRequestWithOverridenPath = WCCustomProperties.HANDLING_REQUEST_WITH_OVERRIDDEN_PATH; // PM88028 // will be a custom property to revert PM71901 if required
	
	private static final List DEFAULT_DENY_EXTENSIONS = new ArrayList();
	static {
		DEFAULT_DENY_EXTENSIONS.add("*.jsp");
		DEFAULT_DENY_EXTENSIONS.add("*.jsv");
		DEFAULT_DENY_EXTENSIONS.add("*.jsw");
		DEFAULT_DENY_EXTENSIONS.add("*.jspx");
		try {
			String sizeStr = WCCustomProperties.OPTIMIZE_FILE_SERVING_SIZE_GLOBAL;
			if (sizeStr!=null)
				optimizeFileServingSizeGlobal = Integer.valueOf(WCCustomProperties.OPTIMIZE_FILE_SERVING_SIZE_GLOBAL);
		}catch (NumberFormatException nfe){
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(nfe, "com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor.<init>", "65");
			
		}
		
	}
	protected URIMatcher denyPatterns = null;
	//	 defect 220552: end add patternList vars used to serve or deny requests related to DefaultExtensionProcessor

	
	
	// begin pq70834
	protected boolean redirectToWelcomeFile = false;
	// end pq70834
	protected WebComponentMetaData cmd;
	protected IPlatformHelper platformHelper;
	
	private Map params;
	
	protected List welcomeFileList;
        
        protected WebApp _webapp;
private ICollaboratorHelper collabHelper;
	private IWebAppNameSpaceCollaborator webAppNameSpaceCollab;
	private IWebAppSecurityCollaborator secCollab;
        private boolean exposeWebInfOnDispatch;  //PK36447
    
	/**
	 * 
	 */
	public DefaultExtensionProcessor(IServletContext webapp, HashMap params)
	{
		super (webapp);
                _webapp = (WebApp) webapp;
		this.params = params;
		collabHelper = _webapp.getCollaboratorHelper();
		webAppNameSpaceCollab =  collabHelper.getWebAppNameSpaceCollaborator();
	    secCollab = collabHelper.getSecurityCollaborator();
		platformHelper = WebContainer.getWebContainer().getPlatformHelper();
		welcomeFileList = (List) webapp.getAttribute(WebApp.WELCOME_FILE_LIST);
		init();
	}

	private void init()
	{
		// If there is a "bufferSize" Init Parameter set, use it as the new
		// read/write buffer size.
		
		
		//PK36447
		exposeWebInfOnDispatch = WCCustomProperties.EXPOSE_WEB_INF_ON_DISPATCH;
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"init", "exposeWebInfOnDispatch ---> true");
		String stringBufferSize = getInitParameter(PARAM_BUFFER_SIZE);
		if (stringBufferSize != null)
		{
			try
			{
				int tempBufferSize = (Integer.parseInt(stringBufferSize));
				defaultBufferSize = tempBufferSize;
			}
			catch (NumberFormatException ex)
			{
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.servlet.SimpleFileServlet.init", "65", this);
			}
		}

		// begin pq65763
		extendedDocumentRoot = getInitParameter("extendedDocumentRoot");
		// end pq65763
		preFragmentExtendedDocumentRoot = getInitParameter("preFragmentExtendedDocumentRoot");
		
		//	 defect 220552: begin look up and parse init attributed related to file serving patterns and process
		String fileServingExtensions = getInitParameter("file.serving.patterns.allow");
		if (fileServingExtensions != null) {
			patternList = parseFileServingExtensions(fileServingExtensions);
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
				logger.logp(Level.FINE, CLASS_NAME,"init", "URI patterns for FileServing =[" + patternList +"]");
			}
		} else {
			this.patternList.add(DEFAULT_MAPPING);
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
				logger.logp(Level.FINE, CLASS_NAME,"init", "Default URI pattern for FileServing =[" + patternList +"]");
			}
		}

		String fileServingExtensionsDenied = getInitParameter("file.serving.patterns.deny");
		if (fileServingExtensionsDenied != null) {
			List list = parseFileServingExtensions(fileServingExtensionsDenied);
			list.addAll(DEFAULT_DENY_EXTENSIONS);
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
				logger.logp(Level.FINE, CLASS_NAME,"init", "Denied URI patterns for FileServing =[" + list +"]");
			}
			denyPatterns = createURIMatcher(list);
		} else {
			List list = DEFAULT_DENY_EXTENSIONS;
			denyPatterns = createURIMatcher(list);
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
				logger.logp(Level.FINE, CLASS_NAME,"init", "Default denied patterns for FileServing =[" + list +"]");
			}
		}
		// defect 220552: end look up and parse init attributed related to file serving patterns and process

		String esiTimeout = (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return System.getProperties().getProperty ("com.ibm.servlet.file.esi.timeOut","300");
			}
		});
                
		if (!esiTimeout.equals("0"))
		{
			esiControl = "max-age=" + esiTimeout + "," +
						 "cacheid=\"URL\"," +
						 "content=\"ESI/1.0+\"";
		}
		esiControl = (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return System.getProperties().getProperty ("com.ibm.servlet.file.esi.control",esiControl);
			}
		});
		// begin pq70834
		String redirectToWelcomeFileStr = getInitParameter("redirectToWelcomeFile");
		if( redirectToWelcomeFileStr != null){
			redirectToWelcomeFile =  redirectToWelcomeFileStr.equalsIgnoreCase("true");	
		}
		optimizeFileServingSize=getFileServingIntegerAttribute("com.ibm.ws.webcontainer.optimizefileservingsize",optimizeFileServingSizeGlobal);
//		mappedByteBufferSize=getFileServingIntegerAttribute("mappedByteBufferSize",mappedByteBufferSizeGlobal);
		// end pq70834
	}

	private int getFileServingIntegerAttribute (String attributeKey, int defaultValue){
		int integerAttribute=defaultValue;
		try {
			 if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	            	logger.logp(Level.FINE, CLASS_NAME,"getFileServingIntegerAttribute", "attributeKey->"+attributeKey);
			}
			String integerAttributeStr = getInitParameter(attributeKey);
			if (integerAttributeStr!=null){
				 if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
		            	logger.logp(Level.FINE, CLASS_NAME,"getFileServingIntegerAttribute", "integerAttributeStr->"+integerAttributeStr);
				}
				integerAttribute = Integer.valueOf(integerAttributeStr).intValue();
			}
		}
		catch (NumberFormatException nfe){
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(nfe, "com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor.init", "65", this);
			 if(logger.isLoggable(Level.SEVERE))
	            	logger.logp(Level.SEVERE, CLASS_NAME,"getFileServingIntegerAttribute", "NumberFormatException.for.file.size.at.which.you.switch.to.optimized.file.serving");
		}
		return integerAttribute;
	}

	/**
	 * @param PARAM_BUFFER_SIZE
	 * @return
	 */
	private String getInitParameter(String param)
	{
		return (String) params.get(param);
	}

    public String getName (){
    	return CLASS_NAME;
    }
    

	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.RequestProcessor#handleRequest(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
    public void handleRequest(ServletRequest request, ServletResponse response) throws Exception {
        Object token = null;
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpServletRequest httpRequest = null; //PM88028
        StringBuffer path =null;
        WebAppDispatcherContext dispatchContext =null;
        String pathInfo = null; // PK64302
        FileNotFoundException fnf = null;
        
    	
//		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
//         	logger.entering(CLASS_NAME,"handleRequest : request--->" + req.getRequestURI()+ "<---");
//		}
		
        try {
        	//IExtendedRequest wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(request);
                //PM88028 Start
                IExtendedRequest wasreq = null;
                
                if(useOriginalRequestState){

                    WebContainerRequestState reqStateSaveRequest = WebContainerRequestState.getInstance(false);
                    if(reqStateSaveRequest != null){                        
                        httpRequest = (HttpServletRequest)reqStateSaveRequest.getAttribute("unFilteredRequestObject");
                        if(httpRequest != null) {
                            wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(httpRequest);  
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                logger.entering(CLASS_NAME,"handleRequest : useOriginalRequestState request--->" + httpRequest.getRequestURI()+ "<---");
                            }
                        }
                    } 
                }
                //if it is null then get it from the req
                if(httpRequest == null)
                {
                    httpRequest = (HttpServletRequest) request;
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                        logger.entering(CLASS_NAME,"handleRequest : request--->" + httpRequest.getRequestURI()+ "<---");
                    }
                    wasreq = (IExtendedRequest) ServletUtil.unwrapRequest(request);
                }
            
		dispatchContext = (WebAppDispatcherContext)wasreq.getWebAppDispatcherContext();
			
        
            //LIBERTY: Talk to Namespace
                //webAppNameSpaceCollab.preInvoke(cmd);
			
            //remove since the preInvoke is done in WebAppFilterManager
			//The preInvoke in WebAppFilterManager calls with doAuth/enforceSecurity=true which will setup
			//the metadata for sync to thread just as well as a call with doAuth=false would.
			//securityPreInvokes.push(secCollab.preInvoke(req, resp, null, false));
        
            token = ThreadIdentityManager.runAsServer();
        
            boolean isInclude = false; // is this an include request?
            ServletContext context = _webapp;
            String fileSystemPath = null; // actual OS dependent path
            path = new StringBuffer(); // constructed path

            String servletPath = null; // PK64302
          //PM88028 change req to httpReques
            if (httpRequest.getAttribute(WebAppRequestDispatcher.REQUEST_URI_INCLUDE_ATTR) != null) {
                isInclude = true;
                servletPath = (String) httpRequest.getAttribute(WebAppRequestDispatcher.SERVLET_PATH_INCLUDE_ATTR);
                pathInfo = (String) httpRequest.getAttribute(WebAppRequestDispatcher.PATH_INFO_INCLUDE_ATTR);
            }// PM79476 (PM71901) Start
            else if(dispatchContext!= null && !handlingRequestWithOverridenPath){ //always here
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "relative uri -->["+ dispatchContext.getRelativeUri()+"]");
                }
                servletPath = dispatchContext.getServletPath();
                if(servletPath!= null) {servletPath = WebGroup.stripURL(servletPath,false);}                    
                
                pathInfo = dispatchContext.getPathInfo();
                if(pathInfo!= null) {pathInfo = WebGroup.stripURL(pathInfo,false);}
                if(pathInfo == ""){pathInfo = null;}
                
            }// PM71901 End
            else { // never be here 
                // not get these from request as these can be overridden
                servletPath = httpRequest.getServletPath();
                pathInfo = httpRequest.getPathInfo();
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            	logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "servletPath [" + servletPath +"] pathInfo [" + pathInfo +"]");
            }
            
            path.append(servletPath);

            if (pathInfo != null) {
            	int semicolon = pathInfo.indexOf(';');
                if (semicolon != -1)
                    pathInfo = pathInfo.substring(0, semicolon);
               
                path.append(pathInfo);
            }

            //PK36447

            String temporaryPath = WSUtil.resolveURI(path.toString());
            String upperMatchString = temporaryPath.toUpperCase();
            boolean forbidden = false;
            if (!exposeWebInfOnDispatch && (upperMatchString.startsWith("/WEB-INF/") || upperMatchString.equals("/WEB-INF")))
                forbidden = true;

            //We check META-INF by default in the JSP codepath
            else if (upperMatchString.startsWith("/META-INF/") || upperMatchString.equals("/META-INF"))
                forbidden = true;
            
            // PK24615 change conditionals from "indexOf() != -1" to "startsWith()"
            if (forbidden) {
                int statusCode;
                //We don't want to give out more information than required.  On tWAS, this code lived in the wrapper
                //and so returning a 403 would have meant that the file was, in fact, found.  Adding this check to maintain
                //the same behavior
                //If the path was encoded and illegal, then just return a 404.
                if (temporaryPath.equals(path.toString())) {
                    statusCode = HttpServletResponse.SC_FORBIDDEN;
                } else {
                    statusCode = HttpServletResponse.SC_NOT_FOUND;
                }
                resp.sendError(
                    statusCode,
                    MessageFormat.format(nls.getString("File.not.found", "File not found: {0}"), new Object[] { path.toString()}));
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.exiting(CLASS_NAME,"handleRequest","Forbidden-WEB-INF/META-INF");
                return;
            }
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"handleRequest"," after resolving the uri: path ---> " + path);

            
            //Liberty ArchiveAPI container to be used instead of File system.
            Container webModuleContainer = _webapp.getModuleContainer();

            /*
             * In Liberty, we'll leave the file system path as just the request path so we can keep 
             * the checks for illegal characters in the same code
             */
            fileSystemPath = path.toString();

            // PQ79698 part1 starts
            if (!isValidFilePath(fileSystemPath)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, nls.getString("File.name.contains.illegal.character", "File path contains illegal character."));
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                	logger.exiting(CLASS_NAME,"handleRequest","Forbidden-invalid file path");
                return;
            }
            // PQ79698 part1 ends
			// defect 220552: begin implement the deny feature for specified URI patterns
			else{
				Object matchedURI = denyPatterns.match(path.toString());
				if(matchedURI != null){	
					if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
						logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "Attempted to serve URI that matches denied URL pattern URI =[" + path.toString() + "] matched [" + matchedURI + "]");
					}
					resp.sendError(HttpServletResponse.SC_FORBIDDEN,							
							MessageFormat.format(nls.getString(
							"File.not.found", "File not found: {0}"),
							new Object[] { path.toString() }));
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    	logger.exiting(CLASS_NAME,"handleRequest","Forbidden-denied pattern");
						return;
				}
			}
			// defect 220552: end implement the deny feature for specified URI patterns

            boolean fileIsDirectoryInDocumentRoot = false;         // PM17845

            File file = null;
            Entry entry = null;
            boolean requestForRoot = false;
           
            //Don't call getEntry for the root container's / as there is no entry.
            if(fileSystemPath.equals("/")){      
                requestForRoot = true;
            } 
            else {
                entry = webModuleContainer.getEntry(fileSystemPath);
            }
            
            //We failed to find something at the path, either in the filesystem or via the container.
            if ((file != null && !file.exists()) || (webModuleContainer != null && entry == null && !requestForRoot)) {
            	
            	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
    				logger.logp(Level.FINE, CLASS_NAME,"handleRequest","file does not exist --> " + path);
            	DocumentRootUtils docRoot = new DocumentRootUtils(context,extendedDocumentRoot,preFragmentExtendedDocumentRoot);
            	try {
            	    docRoot.handleDocumentRoots(path.toString());
            	} catch (FileNotFoundException fnfe) {
    				throw new FileNotFoundException(MessageFormat.format(nls.getString("File.not.found","File not found: {0}"), new Object[]{path}));
            	}
            	if (docRoot.isDirectory()) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    	logger.exiting(CLASS_NAME,"handleRequest","matchedfile is a directory");
    		    	if (!docRoot.isMatchedFromEDR() || WCCustomProperties.ALLOW_PARTIAL_URL_TO_EDR ) {
    		    		fileIsDirectoryInDocumentRoot = true;
    		    	} 
            	} else {
            		
                    if (isRequestForbidden(path))   
    	            {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, MessageFormat.format(nls.getString("File.not.found","File not found: {0}"), new Object[]{httpRequest.getPathInfo()})); //PM88028
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        	logger.exiting(CLASS_NAME,"handleRequest","Forbidden-Extended doc root endsWith");
    		            return;
    	            }
            	
            	    ZipFileResource zipFileResource = docRoot.getMatchedZipFileResource();
            	    if (zipFileResource!=null)
            	    {            		
            		    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            		     logger.logp(Level.FINE, CLASS_NAME,"handleRequest","zip file found: Use ZipFileServletWrapper");
            			
           		         handleZipFileWrapper(req, httpRequest, resp, path, dispatchContext,zipFileResource);
           		         return;
            	     } else {
            	        EntryResource entryResource = docRoot.getMatchedEntryResource();
            	        if(entryResource != null){
            	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            	                logger.logp(Level.FINE, CLASS_NAME,"handleRequest","entry is found: Use EntryServletWrapper");
            	            EntryServletWrapper ewrapper = new EntryServletWrapper(_webapp, this, entryResource.getEntry());
            	            ewrapper.handleRequest(req, resp);
            	            
            	            if(!_webapp.isCachingEnabled()){

            	                //PM84305
            	                if(!_webapp.getConfiguration().isDisableStaticMappingCache()){
            	                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            	                        logger.logp(Level.FINE, CLASS_NAME,"handleRequest","ewrapper addMappingTarget URI -> "+ path);

            	                    _webapp.addMappingTarget(path.toString(), ewrapper);
            	                }
            	                
            	                if (!dispatchContext.isInclude() && !dispatchContext.isForward()) {
            	                    WebContainer.addToCache(httpRequest, ewrapper, this._webapp); //PM88028
            	                }
            	            }
            	            
            	            return;
            	        }  
            	    
            	    
            		           		
            	        File matchedFile = docRoot.getMatchedFile();

            	        if (matchedFile !=null) {


            	            FileServletWrapper fwrapper = getStaticFileWrapper(_webapp, this, matchedFile);
            	            fwrapper.handleRequest(req, resp);
            	            if (!_webapp.isCachingEnabled()){
            	                try
            	                {
            	                    //PM84305
            	                    if(!_webapp.getConfiguration().isDisableStaticMappingCache()){
            	                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            	                            logger.logp(Level.FINE, CLASS_NAME,"handleRequest","fwrapper addMappingTarget URI -> "+ path);

            	                        _webapp.addMappingTarget(path.toString(), fwrapper);
            	                    }

            	                    // begin 309663
            	                    // add to cache now versus wait until Webcontainer.handleRequest finds a IServletWrapper 
            	                    // (allows request.getPathInfo and request.servletPath() to stay valid)
            	                    if (!dispatchContext.isInclude() && !dispatchContext.isForward()) {
            	                        WebContainer.addToCache(httpRequest, fwrapper, this._webapp); //PM88028
            	                    }
            	                    // end 309663
            	                }
            	                catch (Exception e)
            	                {
            	                    // do nothing because its a race condition...won't happen again.
            	                }
            	            } 
            	            return;
            	        }
            	     }
            	}

               // PM17845 Add if statement
                if (!fileIsDirectoryInDocumentRoot) {
                    // Resource was not found...invoke filters                
    				throw new FileNotFoundException(MessageFormat.format(nls.getString("File.not.found","File not found: {0}"), new Object[]{path}));
                }   
            }    
            
            boolean isContainerDirectory = false;
            Container directory = null;
            // Determine if the entry is a directory
            if(webModuleContainer != null){
                if(requestForRoot){
                    directory = webModuleContainer;
                } else if (entry != null){
                    directory = entry.adapt(Container.class);
                }
                isContainerDirectory = requestForRoot || fileIsDirectoryInDocumentRoot || (entry.getSize()==0 && directory != null && directory.isRoot()==false);
            }
            
            // PM17845 Add check for ileIsDirectoryInExtendedDocumnetRoot
            if ((file != null && file.isDirectory()) || fileIsDirectoryInDocumentRoot || isContainerDirectory) 
            {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                    if (file != null) {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "file is a directory --> " + file.getPath());
                    } else if (entry!=null) {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "entry path --> " + entry.getPath());
                    } else {
                        logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "file is null");
                    }
                }
                
                /*
                 * The file is a directory so first see if we should append a trailing slash to the
                 * uri and redirect.  After that try and serve a welcome file or directory browse.
                 */
                String requestString = getURLWithRequestURIEncoded(httpRequest).toString(); //PM88028
                
                //start PI31447
                if(WCCustomProperties.IGNORE_SEMICOLON_ON_REDIRECT_TO_WELCOME_PAGE){
                        String requestURI = req.getRequestURI();
                        int indexAfterContextRoot = requestURI.indexOf(context.getContextPath()) + context.getContextPath().length();
                        if(!context.getContextPath().equals("/"))
                                //if the context root is not / look for the character after next since the next is /
                                indexAfterContextRoot++;
                        if(indexAfterContextRoot < requestURI.length() && requestURI.charAt(indexAfterContextRoot) == ';'){ 
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                        logger.logp(Level.FINE, CLASS_NAME,"handleRequest","Removing semi-colon");
                                requestString = requestString.substring(0, requestString.indexOf(";"));
                        } 
                }
                //end PI31447
                
                String queryString = httpRequest.getQueryString(); //PM88028

                if (!requestString.endsWith("/")) {
                    if (isInclude == false) {
                        String tmpURL = requestString + "/"; // append a slash for redirect purposes.
//                        String qString;
//                        if ((qString = req.getQueryString()) != null) { // if query string exists; add it.
//                            tmpURL += "?" + qString;
//                        }
                        //PM88028
                        if (queryString != null){ // if query string exists; add it.
                            tmpURL += "?" +queryString;
                        }
                        
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        	logger.logp(Level.FINE, CLASS_NAME,"handleRequest","sendRedirect -->" + tmpURL);
                        
                        // encode redirect url to keep session info.
                        resp.sendRedirect(resp.encodeRedirectURL(tmpURL));
                        
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        	logger.exiting(CLASS_NAME,"handleRequest","redirect to context root");
                                              
                        return;
                    }
                }
                //begin PK10057
//                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
//                    logger.logp(Level.FINE, CLASS_NAME,"handleRequest","calling security check for URI --> " + path.toString() );
//                }
                
                //The preInvoke in WebAppFilterManager calls with doAuth/enforceSecurity=true
                //securityPreInvokes.push(secCollab.preInvoke ( req, resp, path.toString(), dispatchContext.isEnforceSecurity()));
             
//                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
//                    logger.logp(Level.FINE, CLASS_NAME,"handleRequest","returned from security check for URI --> " + path.toString() );
//                }
                //end PK10057
                
                String welcomeFileRedirectUri = null;
                String welcomeFileForwardUri = null;
                
                if ((welcomeFileList != null) && (welcomeFileList.size() != 0)) {
                	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    	logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "Check welcome file list");
                	}
                    /*
                     * Check the welcome files to see if we should be serving
                     * one of them up here.
                     */
                    Iterator e = welcomeFileList.iterator();
                    String page = null;

                    while (e.hasNext()) {
                        page = (String) e.next();

                        if ((page.charAt(0) == '/'))
                            page = page.substring(1);

                        // begin 254491    [proxies BOTP] mis-handling of non-existent welcome-file's    WAS.webcontainer -- rewritten
                        RequestProcessor rp = _webapp.getRequestMapper().map(path.toString() + page);
                        if(rp == null){
                        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                            	logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "No request processor found for welcome file:" + path.toString() + page);
                        	}
                        	continue;
                        }
                        else if (rp instanceof WebExtensionProcessor){
                        	boolean available = ((WebExtensionProcessor)rp).isAvailable(path.toString() + page);
                        	if(available == false){
                        		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                	logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "No web extension processor found for welcome file:" + path.toString() + page);
                            	}
                        		continue;
                        	}
                        }
                        else if (rp instanceof IServletWrapper){
                        	boolean available = ((IServletWrapper)rp).isAvailable();
                        	if(available == false){
                        		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                	logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "No servlet wrapper found for welcome file:" + path.toString() + page);
                            	}  
                        		continue;
                        	}
                        }

                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                        	logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "Process welcome file: path: " + path.toString() + ", page: "+page);
                    	} 
                        // end 254491    [proxies BOTP] mis-handling of non-existent welcome-file's    WAS.webcontainer -- rewritten
                        if (redirectToWelcomeFile) {
                            String qString = "";
//                            if ((req.getQueryString()) != null) { // if query string exists; add it.
//                                qString = "?" + req.getQueryString();
//                            }
                            if(queryString != null) { // if query string exists; add it.
                                qString = "?" + queryString; //PM88028
                            }
                            String rPath = removeLeadingSlashes(path.toString());
                            if (rPath == null)
                                rPath = "";
                            //	BEGIN:PK15276
                            String redirectURI;
                            if(com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31 || WCCustomProperties.REDIRECT_WITH_PATH_INFO){
                                redirectURI= page + qString;
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                    logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "Process welcome file: redirecting with pathInfo: " + redirectURI);
                                }
                            }
                            else{
                                redirectURI = rPath + page + qString;
                            }
                            
                            
                            
                            req.setAttribute("com.ibm.ws.webcontainer.welcomefile.redirecturl", redirectURI);
                            welcomeFileRedirectUri = redirectURI;

                        }
                        else {
                            //begin 267395    SVT: can not get Admin Console on Win2000 -- m0514.18 build    WAS.webcontainer    
                            String uri = path.toString() + page;
                            //PK78371 - start
                            if (WCCustomProperties.PROVIDE_QSTRING_TO_WELCOME_FILE){
//                                if ((req.getQueryString()) != null)
//                                    uri = uri + "?" + req.getQueryString();
                                if (queryString != null)
                                    uri = uri + "?" + queryString; //PM88028
                            }
                            //PK78371 - end 
                            req.setAttribute("com.ibm.ws.webcontainer.welcomefile.url", uri);
                            welcomeFileForwardUri = uri;
                        }
                        //end 267395    SVT: can not get Admin Console on Win2000 -- m0514.18 build    WAS.webcontainer    
                    	break; // Break out of the loop with the first match
                    }
                }
                
                if (welcomeFileRedirectUri!=null){
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE,CLASS_NAME,"handleRequest", "sendRedirect to Welcome File:" + welcomeFileRedirectUri);

                    resp.sendRedirect(resp.encodeRedirectURL(welcomeFileRedirectUri));

                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.exiting(CLASS_NAME,"handleRequest");
                    return;

                } else if (welcomeFileForwardUri!=null){
                    RequestDispatcher rd = context.getRequestDispatcher(welcomeFileForwardUri);

                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE,CLASS_NAME,"handleRequest", "forward :" + welcomeFileForwardUri);

                    rd.forward(req, resp);

                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.exiting(CLASS_NAME,"handleRequest");
                    return;
                }
                //End PK64097

                if (_webapp.getConfiguration().isDirectoryBrowsingEnabled()){
					RequestDispatcher dirBrowse = context.getRequestDispatcher(WebApp.DIR_BROWSING_MAPPING);

					if (dirBrowse != null) {
						
						/*
						 * invoke the directory browsing servlet
						 */
						req.setAttribute("com.ibm.servlet.engine.webapp.dir.browsing.path", fileSystemPath);
						req.setAttribute("com.ibm.servlet.engine.webapp.dir.browsing.uri", req.getRequestURI());

						  
						if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
							logger.logp(Level.FINE, CLASS_NAME,"handleRequest","Directory Browse" + fileSystemPath);           		
						dirBrowse.forward(req, resp);
						if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
							logger.exiting(CLASS_NAME,"handleRequest");
						return;
					}
                }

                /*
				 * could not find a welcome file and dir browsing not on
				 */    
                // PK31377 - added if/else
                if (!resp.isCommitted()){  
                    resp.sendError(404, MessageFormat.format(nls.getString("File.not.found", "File not found: {0}"), new Object[] {pathInfo}));
       	   	  		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                	logger.exiting(CLASS_NAME,"handleRequest","handleRequest ---> File not found");
                }else{		    	   
                    logger.exiting("handleRequest", "handleRequest");               	
            	}
                //PK31377 - end
                return;
            }

		// PK23475 code to check for invalid charcters in the request moved to new private method isRequestForbidden

            // String matchString = path.toString();

            // defect 220552: removed checks for URI resources ending with "/" for JSP resources: handled above.

            /**
            * Do not allow ".." to appear in a path as it allows one to serve files from anywhere within
            * the file system.
            * Also check to see if the path or URI ends with '/', '\', or '.' .
            * PQ44346 - Allow files on DFS to be served.
            */
            //if ((matchString.lastIndexOf("..") != -1 && (!matchString.startsWith("/...")))
            //    || matchString.endsWith("\\")
            //    || req.getRequestURI().endsWith(".")
            //    // PK22928
            //    || req.getRequestURI().endsWith("/")){
            //    //PK22928
		
		    // PK23475 call isRequestForbidden to check if request includes ".." etc.
            if (isRequestForbidden(path))   
            {
                resp.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    MessageFormat.format(nls.getString("File.not.found", "File not found: {0}"), new Object[] { pathInfo}));
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                	logger.exiting(CLASS_NAME,"handleRequest","handleRequest --> Forbidden-endsWith");
                return;
            }

            // 94578, "Case Sensitive Security Matching bug":  On Windows and Netware only, filename of
            //         requested file must exactly match case or we will throw FNF exception.
            if (webModuleContainer == null && com.ibm.ws.util.FileSystem.isCaseInsensitive) {
                File caseFile = new Java2SecurityFile(fileSystemPath);

                if (!com.ibm.ws.util.FileSystem.uriCaseCheck(caseFile, path.toString())) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    	logger.logp(Level.FINE, CLASS_NAME,"handleRequest","handleRequest --> Case sensitivity check - throw FileNotFoundException");               	
                    throw new FileNotFoundException(path.toString());
                }
            }
            // end 94578

            IServletWrapper fwrapper = null;
            
            if(file != null){
            // Create the FileServletWrapper to serve the file up.
              fwrapper = getStaticFileWrapper(_webapp, this, file);
			// 224858, must set pathElements for files mapped to staticFileServlet so getServletPath
			// is the same on the first request as on subsequent requests
        	//  begin 309663: should not be resetting path elements here (sitemesh issue since corrected).
			//dispatchContext.setPathElements(path.toString(),null);
        	//          end 309663
			// end 224858
            } else {
              fwrapper = getEntryWrapper(_webapp, this, entry);
            }
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            	logger.logp(Level.FINE, CLASS_NAME,"handleRequest","Use FileServletWrapper");
            
			fwrapper.handleRequest(req, resp);

			if (!_webapp.isCachingEnabled()){
			    try
			    {
			        //PM84305
			        if(!_webapp.getConfiguration().isDisableStaticMappingCache()){
			            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			                logger.logp(Level.FINE, CLASS_NAME,"handleRequest","FileServletWrapper addMappingTarget URI -> "+ path);

			            _webapp.addMappingTarget(path.toString(), fwrapper);
			        }

			        // begin 309663
			        // add to cache now versus wait until Webcontainer.handleRequest finds a IServletWrapper 
			        // (allows request.getPathInfo and request.servletPath() to stay valid)
			        if (!dispatchContext.isInclude() && !dispatchContext.isForward()) {
			            WebContainer.addToCache(httpRequest, fwrapper, this._webapp); //PM88028
			        }
			        // end 309663			
			    }
			    catch (Exception e)
			    {
			        // do nothing because its a race condition...won't happen again.
			    }
			}
			
        }
        catch (FileNotFoundException e) 
		{
            // Give any registered FileNotFoundProcessors a chance to process the request
            if (!RegisterRequestInterceptor.notifyRequestInterceptors(RequestInterceptor.INTERCEPT_POINT_FNF,req, resp)) {
                 // PK64302 Start
                 if (!WCCustomProperties.THROW_404_IN_PREFERENCE_TO_403 && isDirectoryTraverse(path))   
                 {
                     resp.sendError(
                         HttpServletResponse.SC_FORBIDDEN,
                             MessageFormat.format(nls.getString("File.not.found", "File not found: {0}"), new Object[] { pathInfo}));
                     if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))
                         logger.exiting(CLASS_NAME, "handleRequest", "Forbidden-file does not exist");
                     return;
                 }
                 // PK64302 End
        	 // Begin PK27620
        	
        	WebAppErrorReport errorReport = new WebAppErrorReport(MessageFormat.format(nls.getString("File.not.found", "File not found: {0}"), new Object[] { e.getMessage()}), e);
    		logger.logp(Level.WARNING,CLASS_NAME,"handleRequest", e.getMessage());
            
        	errorReport.setErrorCode(HttpServletResponse.SC_NOT_FOUND);
        	errorReport.setTargetServletName ("DefaultExtensionProcessor");
        	
       		// Note: In V8 by default WCCustomProperties.MODIFIED_FNF_BEHAVIOR is true.
                if(!WCCustomProperties.MODIFIED_FNF_BEHAVIOR) { //PK65408
							
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor.handleRequest", "573", this);
							try
							{
								_webapp.sendError(req, resp, errorReport);
							}
							catch (Exception ex)
							{
								// ignore
							
							}
							//PK65408 Start
					 }
					else {
						if (!WCCustomProperties.SERVLET_30_FNF_BEHAVIOR||request.getDispatcherType()!=DispatcherType.INCLUDE){
							if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                				logger.logp(Level.FINE, CLASS_NAME,"handleRequest","dispatch type is not include, throw normal FNF");
                    
							fnf = e;
							resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
						}
						else {
//							Max: Must throw our own type of FNF to differentiate so we can support the following spec requirement
//							and not break backwards compatibility when the servlet itself throws FileNotFound or we're not in an include
//							Spec: If the default servlet is the target of a RequestDispatch.include() and the requested
//							resource does not exist, then the default servlet MUST throw
//							FileNotFoundException. If the exception isn't caught and handled, and the
//							response has not been committed, the status code MUST be set to 500.

							if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                				logger.logp(Level.FINE, CLASS_NAME,"handleRequest","throwing IncludeFileNotFoundException");
                    
							fnf = new IncludeFileNotFoundException(e.getMessage());
						}
						
					}
				
				
				// End PK27620
            }    
        }
	    catch (SecurityViolationException e){
	    	String strPath=null;
	    	if (path!=null){
	    		strPath=path.toString();
	    		collabHelper.processSecurityPreInvokeException (e, this, httpRequest, resp, dispatchContext, _webapp, strPath); //PM88028
	    	}
	    	return;
	    }
        finally {
            //begin PK10057
        	ThreadIdentityManager.reset(token);
        	
        	//The preInvoke in WebAppFilterManager calls with doAuth/enforceSecurity=true which will setup
//        	while (securityPreInvokes.size() != 0){ // may have null objects in list.
//        		secCollab.postInvoke(securityPreInvokes.pop());
//        	}
        	
        	//webAppNameSpaceCollab.postInvoke();
           //end PK10057
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            	logger.logp(Level.FINE, CLASS_NAME,"handleRequest","handleRequest");
          //PK65408 Start
    	    if(fnf!=null){
    	    	throw fnf;
    	    }
    	    //PK65408 End
        }
        return;
    }

	private void handleZipFileWrapper(HttpServletRequest req, HttpServletRequest httpRequest, //PM88028
			HttpServletResponse resp, StringBuffer path,
			WebAppDispatcherContext dispatchContext,
			ZipFileResource metaInfResourceFile) throws Exception {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
        	logger.entering(CLASS_NAME, "handleZipFileWrapper");
		
		ZipFileServletWrapper zfwrapper = getZipFileWrapper(_webapp, this, metaInfResourceFile);
		zfwrapper.handleRequest(req, resp);
		if (!_webapp.isCachingEnabled()){
			try
			{
			    //PM84305
			    if(!_webapp.getConfiguration().isDisableStaticMappingCache()){
			        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			            logger.logp(Level.FINE, CLASS_NAME,"handleZipFileWrapper","addMappingTarget URI -> "+ path);

			        _webapp.addMappingTarget(path.toString(), zfwrapper);
			    }
			    if (!dispatchContext.isInclude() && !dispatchContext.isForward()) {
			        WebContainer.addToCache(httpRequest, zfwrapper, this._webapp); //PM88028
			    }
			}
			catch (Exception e)
			{
				logger.logp(Level.WARNING, CLASS_NAME, "handleZipFileWrapper", "default.extension.exception.adding.mapping.target");
			}
		}
		
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
        	logger.exiting(CLASS_NAME, "handleZipFileWrapper");
	}
	
	/**
	 * @param _webapp
	 * @param processor
	 * @param file
	 * @return
	 */
	protected abstract FileServletWrapper getStaticFileWrapper(IServletContext _webapp, DefaultExtensionProcessor processor, File file);
	/**
	 * @param _webapp
	 * @param processor
	 * @param file
	 * @param entry
	 * @return
	 */
	protected abstract ZipFileServletWrapper getZipFileWrapper(IServletContext _webapp, DefaultExtensionProcessor processor, ZipFileResource zipFileResource);

	/**
         * @param _webapp
         * @param processor
         * @param file
         * @return
         */
        protected abstract FileServletWrapper getEntryWrapper(IServletContext _webapp, DefaultExtensionProcessor processor, Entry entry);
	
        // PQ79698 part2
	protected boolean isValidFilePath(String filePath) {
		   if(filePath == null) return false;
		   int len = filePath.length();
		   for (int i = 0; i < len; i++) {
			   if(filePath.charAt(i) < ' ') return false;
		   }
		   return true;
	}
	// PQ79698 part2 ends

	/*
	 * removeLeadingSlashes -- Removes all slashes from the head of the input String.
	 */
	public String removeLeadingSlashes(String src)
	{
		String result = null;
		int i = 0;
		boolean done = false;

		if (src == null)
			return null;

		int len = src.length();
		while ((!done) && (i < len))
		{
			if (src.charAt(i) == '/')
			{
				i++;
			}
			else
			{
				done = true;
			}
		}

		// If all slashes were stripped off and there was no remainder, then
		// return null.
		if (done)
		{
			result = src.substring(i);
		}

		return result;
	}

	protected StringBuffer getURLWithRequestURIEncoded(HttpServletRequest req)
	{
		StringBuffer url = new StringBuffer();
		String scheme = req.getScheme();
		int port = req.getServerPort();
		String urlPath = null;
		urlPath = new String(req.getRequestURI().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
		url.append(scheme);
		url.append("://");
		url.append(req.getServerName());
		if (scheme.equals("http") && port != 80 || scheme.equals("https") && port != 443)
		{
			url.append(':');
			url.append(req.getServerPort());
		}
		url.append(urlPath);
		return url;
	}

	/**
	 * @return
	 */
	public String getEsiControl()
	{
		return esiControl;
	}

	/**
	 * @return
	 */
	public int getDefaultBufferSize()
	{
		return defaultBufferSize;
	}
	
	//	 defect 220552: begin add ability to handle patternLists served or denied by DefaultExtensionProcessor
	public List getPatternList() {
		return patternList;
	}

	private List parseFileServingExtensions(String exts) {
		List list = new ArrayList();
		StringTokenizer st = new StringTokenizer(exts, ": ;");
		while (st.hasMoreTokens()) {
			String ext = st.nextToken();
			if (ext.equals("/")) {
				ext = "/*";
			}
			if (patternList.contains(ext) == false) {
				list.add(ext);
			}
		}
		return list;
	}
	
	public URIMatcher createURIMatcher (List list){
                // Servlet 4.0 : Use URIMatcherFactory
                URIMatcher uriMatcher = WebContainer.getWebContainer().getURIMatcherFactory().createURIMatcher(true);

		Iterator i = list.iterator();
		while (i.hasNext()){
			String currPattern = (String) i.next();
			if(currPattern.startsWith("*.")){
				try{
					uriMatcher.put( currPattern, currPattern + " _base pattern");
					uriMatcher.put( currPattern + "/", currPattern + " _base pattern 2");	//security check.
				}catch (Exception e){
					logger.logp(Level.SEVERE, CLASS_NAME,"createURIMatcher", "mapping.clash.occurred",new Object[]{currPattern});
					logger.throwing(CLASS_NAME, "createURIMatcher", e);
				}
			}
			else {
				try{
					uriMatcher.put( currPattern, currPattern + " _base pattern");
				}catch (Exception e){
					logger.logp(Level.SEVERE, CLASS_NAME,"createURIMatcher", "mapping.clash.occurred",new Object[]{currPattern});
					logger.throwing(CLASS_NAME, "createURIMatcher", e);
				}
			}
		}
		return uriMatcher;
	}
	//	 defect 220552: end add ability to handle patternLists served or denied by DefaultExtensionProcessor
	
	   // begin 254491    [proxies BOTP] mis-handling of non-existent welcome-file's    WAS.webcontainer
	public boolean isAvailable (String resource){
	    
	    Container c = _webapp.getModuleContainer();
	    if(c != null){
	       Entry entry = c.getEntry(resource); 
	       if(entry != null){
	           return true;
	       }
	       return isAvailableInDocumentRoot(resource,WCCustomProperties.SERVE_WELCOME_FILE_FROM_EDR);
	    }
	    
	    String tempFileString = _webapp.getRealPath(resource);
	    boolean available = false;
	    if (tempFileString != null) { 
	        File caseFile = new Java2SecurityFile(tempFileString);
		available = caseFile.exists();
		if (available && com.ibm.ws.util.FileSystem.isCaseInsensitive) {
			try{
				available = com.ibm.ws.util.FileSystem.uriCaseCheck(caseFile, resource);
			}catch (IOException io){
				available = false;
			}
		}
        }
		
	    if (!available)
	    	available = isAvailableInDocumentRoot(resource,WCCustomProperties.SERVE_WELCOME_FILE_FROM_EDR);			
		
        return available;
	}
    // end 254491    [proxies BOTP] mis-handling of non-existent welcome-file's    WAS.webcontainer
	
	public boolean isAvailableInDocumentRoot(String resource,boolean searchEDR) {
		boolean available = false;
		try { 
	    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))
	    		logger.logp(Level.FINE, CLASS_NAME,"isAvailable()","File not found in WAR directorr so check DocumetRoots");
            DocumentRootUtils docRoot = new DocumentRootUtils( this._webapp, extendedDocumentRoot,preFragmentExtendedDocumentRoot);
            docRoot.handleDocumentRoots(resource,preFragmentExtendedDocumentRoot!=null,!WCCustomProperties.SKIP_META_INF_RESOURCES_PROCESSING,searchEDR);
            
            //Check first to see if we found an Entry match inside of a DocumentRoot
            EntryResource entryr = docRoot.getMatchedEntryResource();
            if(entryr != null){
                if(entryr.getEntry() != null){
                    return true;
                }
            }
            
            String filePath = docRoot.getFilePath();
            if (filePath!=null) {
		    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))
		    		logger.logp(Level.FINE, CLASS_NAME,"isAvailable()","Match found in DocumetRootd");
            	File caseFile = new Java2SecurityFile(filePath);
                available = caseFile.exists();
            }  
		} catch (FileNotFoundException fne) {
	    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))
	    		logger.logp(Level.FINE, CLASS_NAME,"isAvailable()","FileNotFoundException caught");
        	/* ignore */
        } catch (IOException ioe) {
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE))
        		logger.logp(Level.FINE, CLASS_NAME,"isAvailable()","IOException caught");
        	/* ignore */
        }                
        return available;
	}

	//271581    DefaultExtensionProcessor update    WASCC.web.webcontainer    
	/* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.extension.WebExtensionProcessor#createServletWrapper(com.ibm.ws.webcontainer.servlet.ServletConfig)
	 */
	public IServletWrapper createServletWrapper(IServletConfig config) throws Exception
	{
		String filename = _webapp.getRealPath(config.getFileName());
		if(filename != null){
			File wrapperedFile = new File (filename);
			if(wrapperedFile.exists()){
				return this.getStaticFileWrapper(_webapp, this, wrapperedFile);
			}
		}
		return null;
		
	}
	//271581    DefaultExtensionProcessor update    WASCC.web.webcontainer
	
    
    
	private class Java2SecurityFile extends File{
		
		private static final long serialVersionUID = 1L;
		public Java2SecurityFile(File parent, String child) {
			super(parent, child);
		}
		public Java2SecurityFile(String pathname) {
			super(pathname);
		}
		public Java2SecurityFile(String parent, String child) {
			super(parent, child);
		}
		public Java2SecurityFile(URI uri) {
			super(uri);
		}
		
		public boolean canRead() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				Boolean returnType = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	return new Boolean(_canRead());
	                }
	            });
				return returnType.booleanValue();
			}
			else{
				return super.canRead();
			}
		}
		public boolean _canRead() {
			return super.canRead();
		}
		public boolean canWrite() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				Boolean returnType = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	return new Boolean(_canWrite());
	                }
	            });
				return returnType.booleanValue();
			}
			else{
				return super.canWrite();
			}
		}
		public boolean _canWrite() {
			return super.canWrite();
		}

		public boolean delete() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				Boolean returnType = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	return new Boolean(_delete());
	                }
	            });
				return returnType.booleanValue();
			}
			else{
				return super.delete();
			}
		}
		public boolean _delete() {
			return super.delete();
		}

		public void deleteOnExit() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	_deleteOnExit();
	                	return null;
	                }
	            });
			}
			else{
				super.deleteOnExit();
			}
		}
		public void _deleteOnExit() {
			super.deleteOnExit();
		}
		
		public boolean exists() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				Boolean returnType = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	return new Boolean(_exists());
	                }
	            });
				return returnType.booleanValue();
			}
			else{
				return super.exists();
			}
		}
		public boolean _exists() {
			return super.exists();
		}

		public boolean isDirectory() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				Boolean returnType = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	return new Boolean(_isDirectory());
	                }
	            });
				return returnType.booleanValue();
			}
			else{
				return super.isDirectory();
			}
		}
		public boolean _isDirectory() {
			return super.isDirectory();
		}

		public boolean isFile() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				Boolean returnType = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	return new Boolean(_isFile());
	                }
	            });
				return returnType.booleanValue();
			}
			else{
				return super.isFile();
			}
		}
		public boolean _isFile() {
			return super.isFile();
		}

		public boolean isHidden() {
			if ( System.getSecurityManager() != null){
				final File internalFile = this;
				Boolean returnType = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {
	                public Object run(){
	                	return new Boolean(_isHidden());
	                }
	            });
				return returnType.booleanValue();
			}
			else{
				return super.isHidden();
			}
		}
		public boolean _isHidden() {
			return super.isHidden();
		}
	}
	public WebComponentMetaData getMetaData() {
		return cmd;
	}

               //	PK23475 - Method added using code form doGet() so that it can be called from 2 places
	//  returns true if request is forbidden because it contains ".." etc.
	private boolean isRequestForbidden(StringBuffer path)
	{
			
		boolean requestIsForbidden = false;
			
		String matchString = path.toString();
		//PM82876 Start
		// The fileName or dirName can have .. , check and fail for ".." in path only which can allow to serve from different location.
		if(WCCustomProperties.ALLOW_DOTS_IN_NAME){

		    if (matchString.indexOf("..") > -1) {
		        if( (matchString.indexOf("/../") > -1) || (matchString.indexOf("\\..\\") > -1) || 
		                        matchString.startsWith("../") || matchString.endsWith("/..") || 
		                        matchString.startsWith("..\\")|| matchString.endsWith("\\..")) 
		        {

		            requestIsForbidden = true;
		            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
		                logger.logp(Level.FINE, CLASS_NAME, "isRequestForbidden", "bad path :" + matchString);                        
		        }
		    }

		    if ((!requestIsForbidden && (matchString.endsWith("\\") || matchString.endsWith(".") || matchString.endsWith("/")))) {
		        requestIsForbidden = true;                        
		    }

		}
		else{  //PM82876 End
		    /**
		     * Do not allow ".." to appear in a path as it allows one to serve files from anywhere within
		     * the file system.
		     * Also check to see if the path or URI ends with '/', '\', or '.' .
		     * PQ44346 - Allow files on DFS to be served.
		     */

		    if ((matchString.lastIndexOf("..") != -1 && (!matchString.startsWith("/...")))
		                    || matchString.endsWith("\\")
		                    // PK23475 use matchString instead of RequestURI because RequestURI is not decoded
		                    //      || req.getRequestURI().endsWith(".")
		                    || matchString.endsWith(".")
		                    // PK22928
		                    //      || req.getRequestURI().endsWith("/")){
		                    || matchString.endsWith("/"))
		        //PK22928
		    {
		        requestIsForbidden = true;
		    }

		}
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"isRequestForbidden","returning :" + requestIsForbidden + ", matchstring :" + matchString);
			
		return requestIsForbidden;
	}
	
	// 542155 Add isDirectoryTraverse method - reduced version of isRequestForbidden
	private boolean isDirectoryTraverse(StringBuffer path)
	{
			
		boolean directoryTraverse = false;
			
		String matchString = path.toString();
			
		//PM82876 Start
		if (WCCustomProperties.ALLOW_DOTS_IN_NAME) {
		    // The fileName can have .. , check for the failing conditions only.
		    if (matchString.indexOf("..") > -1) {
		        if( (matchString.indexOf("/../") > -1) || (matchString.indexOf("\\..\\") > -1) || 
		                        matchString.startsWith("../") || matchString.endsWith("/..") || 
		                        matchString.startsWith("..\\")|| matchString.endsWith("\\..")) 
		        {
		            directoryTraverse = true;
		            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
		                logger.logp(Level.FINE, CLASS_NAME, "isDirectoryTraverse", "bad path :" + matchString);
		        }
		    }    
		}
                else{//PM82876 End

                    /**
                     * Do not allow ".." to appear in a path as it allows one to serve files from anywhere within
                     * the file system.
                     * Also check to see if the path or URI ends with '/', '\', or '.' .
                     * PQ44346 - Allow files on DFS to be served.
                     */
                    if ( (matchString.lastIndexOf("..") != -1) && (!matchString.startsWith("/...") )) 
                    {
                        directoryTraverse = true;
                    }

                }
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
			logger.logp(Level.FINE, CLASS_NAME,"isDirectoryTraverse", "returning" + directoryTraverse + " , matchstring :" + matchString);
			
		return directoryTraverse;
	}

	public void nameSpacePostInvoke () {
		this.webAppNameSpaceCollab.postInvoke();
	}
	
	public void nameSpacePreInvoke () {
		this.webAppNameSpaceCollab.preInvoke (getMetaData());
	}
	
	public int getOptimizeFileServingSize() {
		return optimizeFileServingSize;
	}


	public void destroy() {
		// TODO Auto-generated method stub
		
	}


	public ServletConfig getServletConfig() {
		// TODO Auto-generated method stub
		return null;
	}


	public String getServletInfo() {
		// TODO Auto-generated method stub
		return null;
	}


	public void init(ServletConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}


	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		try {
			this.handleRequest(request, response);
		}
		catch (Exception e){
			throw new ServletException(e);
		}
	}

    public IServletWrapper getServletWrapper(ServletRequest request, ServletResponse response){
       return null;
    }
    
}
