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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapper;
import com.ibm.ws.webcontainer.servlet.ServletConfig;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer.webapp.WebGroup;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.ServletUtil;

/**
 * @author asisin
 *
 */
@SuppressWarnings("unchecked")
public class InvokerExtensionProcessor extends WebExtensionProcessor
{

    private static TraceNLS nls = TraceNLS.getTraceNLS(InvokerExtensionProcessor.class, "com.ibm.ws.webcontainer.resources.Messages");
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.extension");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.extension.InvokerExtensionProcessor";
    private static String showCfg = "com.ibm.websphere.examples.ServletEngineConfigDumper";

    //PK16467
    protected static final int numSyncObjects = 41;
    private static final Object syncObjectCreationLock = new Object();
    //PK16467

    private List patternList = new ArrayList();
    private static final String DEFAULT_MAPPING = "/servlet/*";

    private Map params;
    private static final boolean servletCaseSensitive = WCCustomProperties.SERVLET_CASE_SENSITIVE;

    //Added PK16467
    static final private Object getSyncObject(String name) {
        int syncIndex = Math.abs(name.hashCode() % numSyncObjects);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"getSyncObject", "grabbed syncObject in position-->["+syncIndex+"]");
        }
        return SyncHelper.syncObjects[syncIndex];
    }

    //PK52059 START
    static HashSet blockedClassesList;
    static boolean doNotServeByClassName = false;
    static {
        if (WCCustomProperties.DO_NOT_SERVE_BY_CLASSNAME != null){
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,"InvokerExtensionProcessor", "doNotServeByClassName set to true");
            }
            doNotServeByClassName = true;
            String blockedClasses = WCCustomProperties.DO_NOT_SERVE_BY_CLASSNAME;
            StringTokenizer tokenizedBlockedClasses = new StringTokenizer(blockedClasses,"; \t\n\r\f");
            blockedClassesList = new HashSet(tokenizedBlockedClasses.countTokens());
            while(tokenizedBlockedClasses.hasMoreTokens()){
                String classToBlock = tokenizedBlockedClasses.nextToken();
                if(classToBlock.equals("*"))
                    logger.logp(Level.WARNING, CLASS_NAME,"handleRequest",nls.getString("filtering.by.asterisk", "Filtering by asterisk is not allowed."));
                blockedClassesList.add(classToBlock);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,"InvokerExtensionProcessor", "Added " + classToBlock + " to blockedClassesList.");
                }
            }
        }
    };
    //PK52059 END

    /**
     * 
     * @param webApp
     */
    public InvokerExtensionProcessor(WebApp webApp, HashMap params)
    {
        super(webApp);
        this.params = params;
        String fileServingExtensions = getInitParameter("invoker.patterns");
        if (fileServingExtensions != null) {
            patternList = parseInvokerExtensions(fileServingExtensions);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,"InvokerExtensionProcessor", "URI patterns for invoking servlets =[" + patternList +"]");
            }
        } else {
            this.patternList.add(DEFAULT_MAPPING);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,"InvokerExtensionProcessor", "Default URI pattern for invoking servlets =[" + patternList +"]");
            }
        }

    }

    /**
     * This method will only get invoked if 
     * (1) This is the first request to the target servlet to be served by classname/name
     * (2) The request is an include/forward and matched /servlet/*
     * 
     */
    public void handleRequest(ServletRequest req, ServletResponse res)
                    throws Exception
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "should not enter invoker extension processor to handle request");
        }
        //even though we should have just called getServletWrapper, this time we pass true to do the error handling.
        //Before we passed it false in case the filters are invoked and give us a new url-pattern.
        IServletWrapper s = getServletWrapper(req,	res,true);
        if (s!=null){
            s.handleRequest(req,	res);
        } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "unable to find servlet wrapper to handle request");
        }

        return;
    }



    public IServletWrapper getServletWrapper(ServletRequest req,
                                             ServletResponse resp, boolean handleFailure) throws Exception{
        String methodName = "getServletWrapper";
        //PK16467
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.entering(CLASS_NAME, methodName);
        }
        //PK16467

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext)((IExtendedRequest)ServletUtil.unwrapRequest(request)).getWebAppDispatcherContext();
        String errorString = null;
        String invokePath = null;
        String servletName = null;
        IServletWrapper s = null;
        String reqURI = null;
        boolean failedAddMappingTarget=false;//PK16467
        StringBuffer cacheKey = new StringBuffer(req.getServerName());
        String pathInfo;
        cacheKey.append(':');
        cacheKey.append(req.getServerPort());
        try
        {
            boolean isInclude = dispatchContext.isInclude();

            if (isInclude)
            {
                //PK16467
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,methodName, "InvokerExtensionProcessor is called from include so use request attributes");
                }
                //PK16467
                invokePath = (String)request.getAttribute("javax.servlet.include.servlet_path");
                pathInfo   = (String)request.getAttribute("javax.servlet.include.path_info");
                reqURI = (String) request.getAttribute("javax.servlet.include.request_uri");
            }
            else
            {
                //PK16467
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,methodName, "InvokerExtensionProcessor called directly or from forward.");
                }
                //PK16467
                invokePath = request.getServletPath();  
                pathInfo   = request.getPathInfo();
                reqURI = request.getRequestURI();
            }

            //PK16467
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,methodName, "before stripping /servlet/, servletName-->["+servletName+ "], reqURI-->["+reqURI+"]");
            }
            //PK16467
            servletName = getServletName(pathInfo);

            if (servletName == null||servletName.length( ) == 0||servletName.equals(showCfg)) 
            {
                return null;
            }
            //PK16467
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,methodName, "after stripping /servlet/, servletName-->["+servletName+ "]");
            }
            //PK16467

            //PK52059 START
            if (doNotServeByClassName)
            {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "Checking if " + servletName + " is on the blockedClassesList");
                }
                if(blockedClassesList.contains(servletName)){
                    logger.logp(Level.WARNING, CLASS_NAME,methodName,MessageFormat.format(nls.getString("servlet.on.blocked.list.{0}","Servlet on the blocked list: {0}"), new Object[]{servletName}));
                    if (handleFailure)
                        errorString = "/servlet/"+servletName;
                    return null;
                }
            }
            //PK52059 END

            if (pathInfo != null)
            {
                int ind = pathInfo.indexOf(servletName);
                ind += servletName.length();

                invokePath += pathInfo.substring(0, ind);
                pathInfo = pathInfo.substring(ind);
            }
            //PK16467
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,methodName, "after parsing servletName from request elements, invokePath -->["+invokePath+"], pathInfo-->["+pathInfo+"]");
            }
            //PK16467

            dispatchContext.setPathElements(invokePath, pathInfo);


            cacheKey.append(reqURI);

            // check in cache to see if present
            CacheServletWrapper cWrapper = WebContainer.getFromCache(cacheKey);

            if (cWrapper != null)
            {
                //PK16467
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,methodName, "InvokerExtensionProcessor found cacheServletWrapper");
                }
                //PK16467
                return cWrapper.getCacheTarget();
            }


            if (((WebApp)extensionContext).isInternalServlet(servletName))
            {
                return null;
            }

            s = ((WebApp)extensionContext).getServletWrapper(servletName);

            if (s != null)
            {
                //PK16467
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,methodName, "found servletWrapper already in the webApp for -->["+servletName+"]");
                }
                //PK16467
                return s;
            }
            else
            {
                //PK16467
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME,methodName, "Couldn't find servletWrapper, traverse servletInfos or create servletWrapper");
                }
                synchronized (getSyncObject(invokePath)){
                    
                    s = ((WebApp)extensionContext).getServletWrapper(servletName);
                    if (s != null)
                    {
                        //PK16467
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                            logger.logp(Level.FINE, CLASS_NAME,methodName, "found servletWrapper already in the webApp for -->["+servletName+"]");
                        }
                        //PK16467
                        return s;
                    }
                    // check by classname
                    Iterator i = ((WebApp)extensionContext).getConfiguration().getServletInfos();
                    while (i.hasNext())
                    {
                        ServletConfig sc = (ServletConfig) i.next();
                        String className = sc.getClassName();
                        //PK42055
                        if (className != null){				
                            if(servletCaseSensitive ? className.equals(servletName) : className.equalsIgnoreCase(servletName))
                                //if (className != null && className.equalsIgnoreCase(servletName))
                                //end PK42055
                            {
                                //PK16467
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                    logger.logp(Level.FINE, CLASS_NAME,methodName, "className for current ServletConfig is ["+className+"]");
                                }
                                //PK16467
                                String realName = sc.getServletName();
                                s = ((WebApp)extensionContext).getServletWrapper(realName);
                                if (s != null)
                                {
                                    //PK16467
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                        logger.logp(Level.FINE, CLASS_NAME,methodName, "found servletWrapper for real servlet name -->["+realName+"], handle the request");
                                    }
                                    return s;
                                }
                                else
                                {
                                    //PK16467
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                        logger.logp(Level.FINE, CLASS_NAME,methodName, "unable to retrieve a servlet wrapper even though the class names matched");
                                    }
                                    //PK16467
                                    return null;
                                }
                            }
                        } //PK42055
                    }

                    // Came here, hence it is a servlet thats not in the config (web.xml)
                    // PK16467
                    //synchronized (getSyncObject(invokePath)){
                    s = ((WebApp)extensionContext).getServletWrapper(servletName);
                    //PK16467

                    if(s == null)
                    {

                        IServletConfig sconfig = createConfig("Invoked_" + System.currentTimeMillis());
                        sconfig.setServletName(servletName);
                        sconfig.setDisplayName(servletName);
                        sconfig.setClassName(servletName);
                        sconfig.setStartUpWeight(Integer.valueOf(1));
                        sconfig.setServletContext(((WebApp)extensionContext).getFacade());
                        sconfig.setIsJsp(false);
                        sconfig.setAsyncSupported(true);

                        try
                        {
                            //PK16467
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                logger.logp(Level.FINE, CLASS_NAME,methodName, "InvokerExtensionProcessor will create a servlet wrapper");
                            }
                            //PK16467

                            s = createServletWrapper(sconfig);

                            //PK16467
                            WebAppConfiguration wConfig = (WebAppConfiguration)extensionContext.getWebAppConfig();
                            ServletConfig internalSConfig = (ServletConfig)sconfig;
                            wConfig.addServletInfo(servletName, internalSConfig);
                            //remove because addServletMapping does this and it screwed up using
                            //the same api that is now in javax.servlet.Servlet
                            //((ServletConfig)sconfig).addMapping(invokePath);
                            wConfig.addServletMapping(servletName, invokePath);
                            try{
                                extensionContext.addMappingTarget(invokePath+"/*", s);

                            }
                            catch (Exception e2){
                                failedAddMappingTarget=true;
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                    logger.logp(Level.FINE, CLASS_NAME,methodName,"Error adding mapping Target",e2);

                            }
                            //PK16467
                        }
                        catch (Throwable t)
                        {
                            //PK16467
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                                logger.logp(Level.FINE, CLASS_NAME,methodName, "exception caught creating servlet wrapper or adding mapping",t);
                            }
                            //PK16467
                            if (handleFailure)
                                throw new ServletException("Initialization Error :"+t.getMessage());
                        }
                    }
                }
                //begin PK12809
            }
        }
        //end PK12809

        finally
        {
            if (s==null&&handleFailure)
                response.sendError(HttpServletResponse.SC_NOT_FOUND, MessageFormat.format(nls.getString("Servlet.Not.Found.{0}","Servlet Not Found: {0}"), new Object[]{errorString}));
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
                logger.exiting(CLASS_NAME, methodName);
            }
        }
        return s;
    }

    private String getServletName(String pathInfo){
        String servletName = null;
        if ((pathInfo != null)){
            servletName = WebGroup.stripURL(pathInfo);
            if (servletName!=null){
                if (servletName.charAt(0) == '/')
                {
                    servletName = servletName.substring(1);
                }
                int index = servletName.indexOf("/");
                if (index != -1)
                {
                    servletName = servletName.substring(0,index);
                }
            }
        }
        return servletName;
    }

    public List getPatternList() {
        return patternList;
    }

    private String getInitParameter(String param) {
        return (String) params.get(param);
    }

    private List parseInvokerExtensions(String exts) {
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

    public WebComponentMetaData getMetaData() {
        // TODO Auto-generated method stub
        return null;
    }

    public IServletWrapper getServletWrapper(ServletRequest req,
                                             ServletResponse resp) throws Exception {
        return getServletWrapper(req,resp,false);
    }

    private static class SyncHelper {
        public static Object[] syncObjects;
        static {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "Sync objects are being intialized for synchronization during creation of servlet wrappers.");
            }
            syncObjects = new Object[numSyncObjects];
            for (int k = 0; k < numSyncObjects; k++)
                syncObjects[k] = new Object();
        }
    }

}

