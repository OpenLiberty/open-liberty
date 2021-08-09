/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.jsp.tagext.Tag;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.taglib.annotation.AnnotationHandler;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

public abstract class HttpJspBase extends HttpServlet implements HttpJspPage {
    static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.runtime.HttpJspBase";
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    private final static int MAX_POOLSIZE = 10;
    private final static int SHARE_POLICY = 1;
/*    
    static {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactoryImpl factory = new JspFactoryImpl();
            if (System.getSecurityManager() != null) {
                String basePackage = "org.apache.jasper.";
                try {
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.JspFactoryImpl$PrivilegedGetPageContext");
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.JspFactoryImpl$PrivilegedReleasePageContext");
                    factory.getClass().getClassLoader().loadClass(basePackage + "runtime.JspRuntimeLibrary");
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.JspRuntimeLibrary$PrivilegedIntrospectHelper");
                    factory.getClass().getClassLoader().loadClass(
                        basePackage + "runtime.ServletResponseWrapperInclude");
                    //factory.getClass().getClassLoader().loadClass(basePackage + "servlet.JspServletWrapper");
                }
                catch (ClassNotFoundException ex) {
                    System.out.println("Jasper JspRuntimeContext preload of class failed: " + ex.getMessage());
                }
            }
            JspFactory.setDefaultFactory(factory);
        }
    }
*/
    private static WSThreadLocal _threadLocal = new WSThreadLocal();
    
    private AnnotationHandler _tagAnnotationHandler;   // LIDB4147-24
    
    protected HttpJspBase() {}

    public final void init(ServletConfig config) throws ServletException {
        
        super.init(config);
        jspInit();
        _jspInit();
            
        // LIDB4147-24
            
        this._tagAnnotationHandler = AnnotationHandler.getInstance
            (getServletContext());
        
    }

    public String getServletInfo() {
        return JspCoreException.getMsg("jsp.engine.info");
    }

    public final void destroy() {
        jspDestroy();
        _jspDestroy();
    }

    /**
     * Entry point into service.
     */
    public final void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        _jspService(request, response);
    }

    public void jspInit() {}

    public void _jspInit() {}

    public void jspDestroy() {}

    protected void _jspDestroy() {}

    public abstract void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException;

    public String getId(HttpServletRequest request) {
        return null;
    }

    public int getSharingPolicy(HttpServletRequest request) {
        return SHARE_POLICY;
    }

    protected Tag getTagHandler(HttpServletRequest request, String tagKey, String tagClassName) {
        Integer threadId = Thread.currentThread().hashCode();
        String webAppKey = request.getLocalName() + "_" + request.getLocalPort() + "_" + request.getContextPath() + "_" + getServletContext().hashCode(); //Added hashcode to reduce the chance of a CLASSCASTEXCEPTION:PK46880;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
         	logger.entering(CLASS_NAME,"getTagHandler", "threadId: [" + threadId + "] webAppKey: [" + webAppKey + "]");
	}
        
        Tag tag = null;
        Map pool = getPool();
        
        Map webAppPool = (HashMap)pool.get(webAppKey);
        if (webAppPool == null) {
            webAppPool = new HashMap();
            pool.put(webAppKey, webAppPool);
        }
        
        Map webAppPoolMap = (Map)getServletContext().getAttribute(Constants.TAG_THREADPOOL_MAP);
        
        synchronized (webAppPoolMap) {
            if (webAppPoolMap.containsKey(threadId) == false) {
                webAppPoolMap.put(threadId, webAppPool);                           
            }
        }
        
        TagArray tagArray = (TagArray)webAppPool.get(tagKey);
        if (tagArray == null) {
            tagArray = new TagArray(MAX_POOLSIZE, getServletContext());            
            
            webAppPool.put(tagKey, tagArray);
            try {
                tag = (Tag)Class.forName(tagClassName, true, ThreadContextHelper.getContextClassLoader()).newInstance();
                
                this._tagAnnotationHandler.doPostConstructAction (tag);   // LIDB4147-24
            }
            catch (Exception e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "getTagHandler", "Failed to create new instance of tag class " + tagClassName, e);
                }
            }
        }
        else {
            tag = tagArray.get();
            if (tag == null) {
                try {
                    tag = (Tag)Class.forName(tagClassName, true, ThreadContextHelper.getContextClassLoader()).newInstance();
                    
                    this._tagAnnotationHandler.doPostConstructAction (tag);   // LIDB4147-24
                }
                catch (Exception e) {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                        logger.logp(Level.WARNING, CLASS_NAME, "getTagHandler", "Failed to create new instance of tag class " + tagClassName, e);
                    }
                }
            }
        }
	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
        	logger.exiting(CLASS_NAME,"getTagHandler");
        }
        return (tag);
    }

    protected void putTagHandler(HttpServletRequest request, String tagKey, Tag tag) {
        Map pool = getPool();
        Map webAppPool = (HashMap)pool.get(request.getLocalName() + "_" + 
                                           request.getLocalPort() + "_" + 
                                           request.getContextPath() + "_" +
                                           getServletContext().hashCode()); //Added hashcode to reduce the chance of a CLASSCASTEXCEPTION:PK46880;
        
        if (webAppPool != null) {                                           
            TagArray tagArray = (TagArray)webAppPool.get(tagKey);
            if (tagArray != null) {
                tagArray.put(tag);        
            }
            else {
                this._tagAnnotationHandler.doPreDestroyAction (tag);   // LIDB4147-24
                
                tag.release();
                tag = null;
            }
        }
        else {
            this._tagAnnotationHandler.doPreDestroyAction (tag);   // LIDB4147-24
             
            tag.release();
            tag = null;
        }
    }
    
    protected Map getPool() {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.entering(CLASS_NAME,"getPool");
        }
        Map m = null;
        if ((m = (Map) _threadLocal.get()) == null) {
            m = new HashMap();
            _threadLocal.set(m);
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "getPool", "Created new HashMap and set on _threadLocal: ["+_threadLocal+"]");
            }
            Integer threadId = Thread.currentThread().hashCode();
            JspThreadPoolListener.addThreadLocal(_threadLocal, threadId );
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "getPool", "added _threadLocal to JspThreadPoolListener  threadId: ["+threadId+"] _threadLocal: ["+_threadLocal+"]");
            }
        }
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
           logger.exiting(CLASS_NAME,"getPool", "returning map: ["+m+"] from _threadLocal: ["+_threadLocal+"]");
        }
        return m;
    }
}
