/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webcontainerext.ws;

/**
 * @author beena
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.servlet.response.DummyResponse;
import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor;
import com.ibm.ws.jsp.webcontainerext.JSPExtensionServletWrapper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.servlet.DummyRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

public class PrepareJspHelper implements Runnable {

    static final protected Logger logger;
    private static final String CLASS_NAME = "com.ibm.ws.jsp.webcontainerext.PrepareJspHelper";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    protected AbstractJSPExtensionProcessor _jspExtProcessor = null;
    protected String appName = null;
    protected int _threads = Constants.PREPARE_JSPS_DEFAULT_THREADS;
    protected int _counter = 0;
    int _notify = 25;
    protected int _minLength = Constants.PREPARE_JSPS_DEFAULT_MINLENGTH;
    protected int _startAt = Constants.PREPARE_JSPS_DEFAULT_STARTAT;

    protected boolean shouldClassload = false;
    protected boolean onlyCLChanged = false;
    private Stack _files = new Stack();
    private Stack _parents = new Stack();
    protected boolean hasContainer = false;
    ArrayList<String> listOfEntries = null;
    Iterator<String> listOfEntriesIterator = null;
    List<String> extList;

    protected IServletContext webapp = null;
    protected JspOptions options = null;

    public PrepareJspHelper(AbstractJSPExtensionProcessor s, IServletContext webapp, JspOptions options) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "PrepareJspHelper", "enter");
        }

        this._jspExtProcessor = s;
        extList = PrepareJspHelper.buildJspFileExtensionList(Constants.STANDARD_JSP_EXTENSIONS, _jspExtProcessor.getPatternList());
        this.webapp = (IServletContext) webapp;
        this.options = options;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "PrepareJspHelper", "exit");
        }
    }

    private void addToList(ArrayList<String> entriesList, Container container, String fullPath) {
        for (Entry entry : container) {
            String entryName = entry.getName();
            try {
                Container dirContainer = entry.adapt(Container.class);
                if (dirContainer != null && entry.getSize() == 0) {
                    addToList(entriesList, dirContainer, fullPath + "/" + entryName);
                } else {
                    //it's a file ... add the jsp
                    if (isJspFile(entryName, extList) && entry.getSize() >= this._minLength) {
                        entriesList.add(fullPath + "/" + entryName);
                    }
                }
            } catch (UnableToAdaptException e) {
                //if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                //    logger.logp(Level.FINER, CLASS_NAME, "run", entry.getName() + " is not a directory");
                //}
            }
        }
    }

    /*
     * Find all the Jsps in the doc root, and spin of threads to compile and/or Classload/JIT
     */
    public void run() {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "run", "enter");
        }

        String docRoot = null, threadParam = null;

        Container container = webapp.getModuleContainer();
        if (container != null) {
            hasContainer = true;
        } else {
            docRoot = webapp.getRealPath("/");
        }
        /*
         * First we gather information about the webapp and the parameters that
         * were specified.
         */
        //appName = webapp.getWebAppName();
        appName = webapp.getWebAppConfig().getApplicationName();

        //Get the maximum length of the files that we will only compile.... the rest are also classloaded
        _minLength = options.getPrepareJSPs() * 1024;

        if (options.getPrepareJSPsClassloadChanged() != null) {
            onlyCLChanged = true;
            _startAt = Constants.PREPARE_JSPS_DEFAULT_STARTAT;
        } else {
            _startAt = options.getPrepareJSPsClassload();
        }

        _threads = options.getPrepareJSPThreadCount();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()) {
            logger.logp(Level.INFO, CLASS_NAME, "run", "PrepareJspHelper executing on application [" + this.appName + "]");
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "run", "PrepareJspHelper: Document Root: " + docRoot);
            logger.logp(Level.FINE, CLASS_NAME, "run", "PrepareJspHelper: File size minimum (in bytes): " + _minLength);
            logger.logp(Level.FINE, CLASS_NAME, "run", "PrepareJspHelper: Number of threads: " + _threads);

            if (onlyCLChanged) {
                logger.logp(Level.FINE, CLASS_NAME, "run", "PrepareJspHelper: Only classloading out-of-date (changed) JSPs.");
            } else {
                logger.logp(Level.FINE, CLASS_NAME, "run", "PrepareJspHelper: Classloading JSPs starting at JSP number " + _startAt);
            }

        }

        if (container != null) {
            createListOfEntries(container);
        }

        //Create worker threads and let them start compiling.
        try {
            Thread[] threads = new Thread[_threads];
            int i;
            for (i = 0; i < _threads; i++) {
                PrepareJspHelperThread helper = new PrepareJspHelperThread(this, docRoot, container);
                threads[i] = new Thread(helper, "PrepareJspHelperThread " + i);
                threads[i].setDaemon(true);
                threads[i].start();
            }
            //now lets wait for all the threads to die.
            for (i = 0; i < _threads; i++) {
                try {
                    threads[i].join();
                } catch (Throwable th) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.WARNING)) {
                        logger.logp(Level.WARNING, CLASS_NAME, "run", "Pretouch Thread died during execution.", th);
                    }
                }
            }
        } catch (Exception ex) {
            logger.logp(Level.WARNING, CLASS_NAME, "run", "Unexpected exception while running pretouch.", ex);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.INFO)) {
            logger.logp(Level.INFO, CLASS_NAME, "run", "PrepareJspHelper in group [" + appName + "]: All " + _counter + " jsp files have been processed.");
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "run", "< run");
        }
    }

    //only called by single thread before others are spawned, but going to synchronize to fix findbugs 
    protected synchronized void createListOfEntries(Container container) {
        listOfEntries = new ArrayList<String>();
        for (Entry entry : container) {
            String entryName = entry.getName();
            try {
                Container dirContainer = entry.adapt(Container.class);
                if (dirContainer != null && entry.getSize() == 0) {
                    if (!"META-INF".equalsIgnoreCase(entry.getName())) {
                        addToList(listOfEntries, dirContainer, "/" + entryName);
                    }
                } else {
                    //only get here if the object wasn't able to be wrapped as a container - not a dir
                    //I'm not sure why there isn't a WEB-INF check either...
                    if (isJspFile(entryName, extList) && entry.getSize() >= this._minLength) {
                        listOfEntries.add("/" + entryName);
                    }
                }
            } catch (UnableToAdaptException e) {
                //if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                //    logger.logp(Level.FINER, CLASS_NAME, "run", entryName + " is not a directory");
                //}
            }
        }
        if (listOfEntries != null) {
            listOfEntriesIterator = listOfEntries.iterator();
        }
    }

    protected synchronized String getContainerEntry() {
        String returnNext = null;
        if (listOfEntriesIterator != null && listOfEntriesIterator.hasNext()) {
            returnNext = listOfEntriesIterator.next();
            increaseCounter();
        }
        return returnNext;
    }

    private void increaseCounter() {
        if (_counter % _notify == 0) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "getJsp", "PrepareJspHelper in group [" + appName + "]: " + _counter + " jsp files have been processed.");
            }
        }
        _counter++;
        if (_counter >= _startAt) {
            shouldClassload = true;
        }

    }

    //This is a big synchronized method, but most of the time we will be exiting
    //near the beginning
    protected synchronized File getJsp() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "getJsp", "enter");
        }

        File[] children;
        try {
            if (_counter % _notify == 0) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.INFO)) {
                    logger.logp(Level.INFO, CLASS_NAME, "getJsp", "PrepareJspHelper in group [" + appName + "]: " + _counter + " jsp files have been processed.");
                }
            }

            if (!_files.isEmpty()) {
                _counter++;
                if (_counter >= _startAt)
                    shouldClassload = true;
                return (File) _files.pop();
            }

            if (_parents.isEmpty()) {
                return null;
            }

            List extList = PrepareJspHelper.buildJspFileExtensionList(Constants.STANDARD_JSP_EXTENSIONS, _jspExtProcessor.getPatternList());
            String resourcePath = null;
            //do a loop in case we run into an empty directory.			
            while (_files.isEmpty() && !_parents.isEmpty()) {
                children = ((File) _parents.pop()).listFiles();

                if (children != null) {
                    for (int i = 0; i < children.length; i++) {
                        resourcePath = children[i].getName().replace('\\', '/');
                        if (resourcePath.startsWith("/META-INF") == false) {
                            if (children[i].isDirectory()) {
                                _parents.push(children[i]);
                            } else if (children[i].isFile() && isJspFile(children[i].getName(), extList)) {
                                //if none have been processed, get up to the starting point
                                _files.push(children[i]);
                            }
                        }
                    }
                }
            }

            if (!_files.isEmpty()) {
                _counter++;
                if (_counter >= _startAt)
                    shouldClassload = true;

                return (File) _files.pop();
            } else {
                return null;
            }
        } catch (Exception ex) {
            logger.logp(
                        Level.WARNING,
                        CLASS_NAME,
                        "getJsp",
                        "Pretouch ERROR: Unexpected exception retrieving jsp names", ex);
            return null;
        } finally {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, "getJsp", "exit");
            }
        }
    }

    private static List<String> buildJspFileExtensionList(String[] standardExtensions, List<String> additionalExtensions) {
        List<String> extFilter = new ArrayList<String>();
        for (int i = 0; i < standardExtensions.length; i++) {
            extFilter.add(standardExtensions[i].substring(standardExtensions[i].lastIndexOf(".") + 1));
        }
        for (String extMap : additionalExtensions) {
            extMap = extMap.substring(extMap.lastIndexOf(".") + 1);
            if (!extFilter.contains(extMap)) {
                extFilter.add(extMap);
            }
        }
        return extFilter;
    }

    private boolean isJspFile(String resourcePath, List extensionFilter) {
        String ext = resourcePath.substring(resourcePath.lastIndexOf(".") + 1);
        return extensionFilter.contains(ext);
    }

    protected boolean compileJsp(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws Exception {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "compileJsp", "enter");
        }
        boolean rv = false;

        try {
            _jspExtProcessor.handleRequest(httpservletrequest, httpservletresponse);
            rv = true;
        } catch (Exception _ex) {
            logger.logp(Level.WARNING, CLASS_NAME, "compileJsp", "PrepareJspHelper: Exception while compiling JSP with pretouch. JSP: [" + httpservletrequest.getServletPath()
                                                                 + "]", _ex);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "compileJsp", "exit: " + rv);
        }
        return rv;
    }
    
    protected PrepareJspServletRequest newPrepareJspServletRequest() {
        DummyRequest dummyRequest = new DummyRequest();
        return new PrepareJspServletRequestImpl(dummyRequest);
    }
    
    protected PrepareJspServletResponse newPrepareJspServletResponse() {
        DummyResponse dummyResponse = new DummyResponse();
        return new PrepareJspServletResponseImpl(dummyResponse);
    }

    protected class PrepareJspHelperThread implements Runnable {

        private static final String CLASS_NAME2 = "com.ibm.ws.jsp.webcontainerext.PrepareJspHelperThread";
        PrepareJspHelper _helper;
        protected int _rootLength;
        String _id;

        public PrepareJspHelperThread(PrepareJspHelper helper, String docRoot, Container container) {

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME2, "PrepareJspHelperThread", "enter");
            }

            int rootLength = -1;
            if (docRoot != null) {
                rootLength = docRoot.length();
                _parents.push(new File(docRoot));
            }

            String fullPath = "";
            _helper = helper;

            _rootLength = rootLength;

            try {
                _id = Integer.toHexString(Thread.currentThread().hashCode());
            } catch (Exception ex) {
                _id = "        ";
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME2, "PrepareJspHelperThread", "exit");
            }
        }

        public void prepareJspReloadClass(ClassLoader targetClassloader, String config) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME2, "prepareJspReloadClass", "enter, Config: " + config);
            }

            if (targetClassloader != null) {
                try {
                    targetClassloader.loadClass(config);
                } catch (Throwable th) {
                    logger.logp(Level.WARNING, CLASS_NAME2, "prepareJspReloadClass", "Error loading jsp class " + config + " during pretouch.", th);
                }
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME2, "prepareJspReloadClass", "exit");
            }
        }

        public void run() {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME2, "run", "enter");
            }

            File _jsp = null;
            if (!hasContainer) {
                _jsp = _helper.getJsp();
            }

            PrepareJspServletRequest preq = newPrepareJspServletRequest();
            PrepareJspServletResponse pres = newPrepareJspServletResponse();

            if (hasContainer) {
                String relativeContainerPath = getContainerEntry(); //gets the next jsp entry from the container
                while (relativeContainerPath != null) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, CLASS_NAME2, "run", "Processing jsp: " + relativeContainerPath);
                    }
                    relativeContainerPath = relativeContainerPath.replace('\\', '/');
                    preq.setServletPath(relativeContainerPath);
                    preq.setRequestURI(relativeContainerPath);
                    preq.setQueryString(Constants.PRECOMPILE);
                    handleCompile(_helper, preq.getHttpServletRequest(), pres.getHttpServletResponse(), relativeContainerPath);
                    relativeContainerPath = getContainerEntry();
                }

            }
            String relativePath = null;

            while (!hasContainer && _jsp != null) {

                if (_jsp.length() >= this._helper._minLength) {
                    relativePath = _jsp.getAbsolutePath().substring(_rootLength, _jsp.getAbsolutePath().length());

                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, CLASS_NAME2, "run", "Processing jsp: " + relativePath);
                    }
                    relativePath = relativePath.replace('\\', '/');
                    preq.setServletPath(relativePath);
                    preq.setRequestURI(relativePath);
                    preq.setQueryString(Constants.PRECOMPILE);
                    handleCompile(_helper, preq.getHttpServletRequest(), pres.getHttpServletResponse(), relativePath);
                }

                _jsp = _helper.getJsp();
            }

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME2, "run", "exit");
            }
        }

        public void handleCompile(PrepareJspHelper _helper, HttpServletRequest preq, HttpServletResponse pres, String relativeP) {
            try {
                if (compileJsp(preq, pres)) {
                    IServletWrapper iServletWrapper = (IServletWrapper) (_helper._jspExtProcessor.findWrapper(preq, pres));
                    if (iServletWrapper instanceof JSPExtensionServletWrapper) {
                        JSPExtensionServletWrapper jspServletWrapper = (JSPExtensionServletWrapper) iServletWrapper;
                        String config = jspServletWrapper.getJspResources().getPackageName() + '.' + jspServletWrapper.getJspResources().getClassName();

                        if (_helper.onlyCLChanged) {
                            if (jspServletWrapper.getJspResources().isOutdated()) {
                                prepareJspReloadClass(jspServletWrapper.getTargetClassLoader(), config);
                            }
                        } else if (shouldClassload) {
                            prepareJspReloadClass(jspServletWrapper.getTargetClassLoader(), config);
                        }
                    }
                }

            } catch (Exception e) {
                logger.logp(Level.WARNING, CLASS_NAME2, "run", "Unexpected exception while processing jsp " + relativeP, e);
            }

        }

    }

}

class PrepareJspServletResponseImpl extends HttpServletResponseWrapper implements PrepareJspServletResponse {

    public PrepareJspServletResponseImpl(HttpServletResponse response) {
        super(response);
    }

    private PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());

    public void addCookie(Cookie cookie) {}

    public void addDateHeader(String name, long date) {}

    public void addHeader(String name, String value) {}

    public void addIntHeader(String name, int value) {}

    public boolean containsHeader(String name) {
        return false;
    }

    public String encodeUrl(String url) {
        return url;
    }

    public String encodeURL(String url) {
        return encodeUrl(url);
    }

    public String encodeRedirectUrl(String url) {
        return url;
    }

    public String encodeRedirectURL(String url) {
        return encodeRedirectUrl(url);
    }

    public void sendError(int code) {}

    public void sendError(int code, String message) {}

    public void sendRedirect(String location) {}

    public void setDateHeader(String name, long date) {}

    public void setHeader(String name, String value) {}

    public void setIntHeader(String name, int value) {}

    public void setStatus(int sc) {}

    public void setStatus(int sc, String sm) {}

    public void flushBuffer() {}

    public int getBufferSize() {
        return 1024;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public String getContentType() {
        return null;
    }

    public Locale getLocale() {
        return null;
    }

    public ServletOutputStream getOutputStream() {
        return null;
    }

    public PrintWriter getWriter() {
        return this.writer;
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {}

    public void resetBuffer() {}

    public void setBufferSize(int size) {}

    public void setCharacterEncoding(String encoding) {}

    public void setContentLength(int length) {}

    public void setContentType(String type) {}

    public void setLocale(Locale loc) {}
    
    public HttpServletResponse getHttpServletResponse() {
        return this;
    }
 }

class PrepareJspServletRequestImpl extends HttpServletRequestWrapper implements PrepareJspServletRequest {

    public PrepareJspServletRequestImpl(HttpServletRequest request) {
        super(request);
        // TODO Auto-generated constructor stub
    }

    private Cookie[] cookies;
    private String method;
    private String requestURI;
    private String servletPath;
    private String pathInfo;
    private String queryString;
    private String attribute;

    public Cookie[] getCookies() {
        return null;
    }

    public String getMethod() {
        return method;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getServletPath() {
        return servletPath;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public String getPathTranslated() {
        return null;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getRemoteUser() {
        return null;
    }

    public String getAuthType() {
        return null;
    }

    public String getHeader(String name) {
        return null;
    }

    public int getIntHeader(String name, int def) {
        return -1;
    }

    public long getLongHeader(String name, long def) {
        return -1;
    }

    public long getDateHeader(String name, long def) {
        return -1;
    }

    public Enumeration getHeaderNames() {
        return null;
    }

    public HttpSession getSession(boolean create) {
        return null;
    }

    public HttpSession getSession() {
        return null;
    }

    public String getRequestedSessionId() {
        return null;
    }

    public boolean isRequestedSessionIdValid() {
        return false;
    }

    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    public long getDateHeader(String arg0) {
        return 0;
    }

    public Enumeration getHeaders(String arg0) {
        return null;
    }

    public int getIntHeader(String arg0) {
        return 0;
    }

    public String getContextPath() {
        return null;
    }

    public boolean isUserInRole(String arg0) {
        return false;
    }

    public Principal getUserPrincipal() {
        return null;
    }

    public StringBuffer getRequestURL() {
        return null;
    }

    public Object getAttribute(String arg0) {
        return attribute;
    }

    public Enumeration getAttributeNames() {
        return null;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {}

    public int getContentLength() {
        return 0;
    }

    public String getContentType() {
        return null;
    }

    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    public String getParameter(String arg0) {
        return null;
    }

    public Enumeration getParameterNames() {
        return null;
    }

    public String[] getParameterValues(String arg0) {
        return null;
    }

    public Map getParameterMap() {
        return null;
    }

    public String getProtocol() {
        return null;
    }

    public String getScheme() {
        return null;
    }

    public String getServerName() {
        return null;
    }

    public int getServerPort() {
        return 0;
    }

    public BufferedReader getReader() throws IOException {
        return null;
    }

    public String getRemoteAddr() {
        return null;
    }

    public String getRemoteHost() {
        return null;
    }

    public void setAttribute(String arg0, Object arg1) {}

    public void removeAttribute(String arg0) {}

    public Locale getLocale() {
        return null;
    }

    public Enumeration getLocales() {
        return null;
    }

    public boolean isSecure() {
        return false;
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        return null;
    }

    public String getRealPath(String arg0) {
        return null;
    }

    public int getRemotePort() {
        return 0;
    }

    public String getLocalName() {
        return null;
    }

    public String getLocalAddr() {
        return null;
    }

    public int getLocalPort() {
        return 0;
    }

    public void setAttribute(String string) {
        attribute = string;
    }

    public void setCookies(Cookie[] cookies) {
        this.cookies = cookies;
    }

    public void setMethod(String string) {
        method = string;
    }

    public void setPathInfo(String string) {
        pathInfo = string;
    }

    public void setQueryString(String string) {
        queryString = string;
    }

    public void setRequestURI(String string) {
        requestURI = string;
    }

    public void setServletPath(String string) {
        servletPath = string;
    }
    
    public HttpServletRequest getHttpServletRequest() {
        return this;
    }
}