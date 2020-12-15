/*
 * $Header: /cvshome/wascvs/M8_jsp/ws/code/jsp/src/org/apache/jasper/runtime/JspFactoryImpl.java,v 1.1.1.1 2004/03/18 17:16:14 backhous Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2004/03/18 17:16:14 $
 *
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
//PK25630 PageContext Pool initialization increased from 4 to 8(Portal requirement)
//feature LIDB4147-9 "Integrate Unified Expression Language"  2006/08/14  Scott Johnson
//defect  388930 "Incorrect ELContext may be used"  2006/09/06  Scott Johnson
//PI24001 Non-reusable objects of type BodyContentImpl cause a memory leak when using custom tags in a JSP  11/11/2014  hmpadill


package org.apache.jasper.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import com.ibm.ws.jsp.runtime.PageContextPool;
import com.ibm.ws.util.WSThreadLocal;

/**
 * Implementation of JspFactory.
 *
 * @author Anil K. Vijendran
 */
public class JspFactoryImpl extends JspFactory {
	private static Logger logger;
	private static final String CLASS_NAME="org.apache.jasper.runtime.JspFactoryImpl";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}       


    private static final String SPEC_VERSION = "2.1";

    private static WSThreadLocal _threadLocal = new WSThreadLocal();
    
    private int bodyContentBufferSize = BodyContentImpl.DEFAULT_TAG_BUFFER_SIZE;

    public JspFactoryImpl() {
    	this(BodyContentImpl.DEFAULT_TAG_BUFFER_SIZE);
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
			logger.logp(Level.FINE, CLASS_NAME, "JspFactoryImpl", "JspFactoryImpl ctor 1 buffsize=["+BodyContentImpl.DEFAULT_TAG_BUFFER_SIZE+"]  this=["+this+"]");
		}
    }    
    public JspFactoryImpl(int bodyContentBufferSize) {
        super();
        this.bodyContentBufferSize = bodyContentBufferSize;
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
			logger.logp(Level.FINE, CLASS_NAME, "JspFactoryImpl", "JspFactoryImpl ctor 2 buffsize=["+this.bodyContentBufferSize+"]  this=["+this+"]");
		}
    }
    
    public PageContext getPageContext(
        Servlet servlet,
        ServletRequest request,
        ServletResponse response,
        String errorPageURL,
        boolean needsSession,
        int bufferSize,
        boolean autoflush) {

        if (System.getSecurityManager() != null) {
            PrivilegedGetPageContext dp = new PrivilegedGetPageContext((JspFactoryImpl) this, servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
            return (PageContext) AccessController.doPrivileged(dp);
        }
        else {
            return internalGetPageContext(servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
        }
    }

    public void releasePageContext(PageContext pc) {
        if (pc == null)
            return;
        if (System.getSecurityManager() != null) {
            PrivilegedReleasePageContext dp = new PrivilegedReleasePageContext((JspFactoryImpl) this, pc);
            AccessController.doPrivileged(dp);
        }
        else {
            internalReleasePageContext(pc);
        }
    }

    public JspEngineInfo getEngineInfo() {
        return new JspEngineInfo() {
            public String getSpecificationVersion() {
                return SPEC_VERSION;
            }
        };
    }

    private PageContext internalGetPageContext(
        Servlet servlet,
        ServletRequest request,
        ServletResponse response,
        String errorPageURL,
        boolean needsSession,
        int bufferSize,
        boolean autoflush) {
        try {
            PageContext pc = getPool().remove();
            pc.initialize(servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
            return pc;
        }
        catch (Throwable ex) {
            /* FIXME: need to do something reasonable here!! */
  			ex.printStackTrace(System.out);
            return null;
        }
    }

    private void internalReleasePageContext(PageContext pc) {
        pc.release();
    }

    void returnFreePageContext(PageContextImpl pc) {
        getPool().add(pc);
    }
    //PI24001 start
    /**
     * Add a PageContextImpl object to the pool.
     * @param pc - the PageContextImpl object to put in the pool.
     * @return true if <i>pc</i> was added to the pool, false otherwise.
     */
    boolean poolFreePageContextIfNotFull(PageContextImpl pc) {
        return getPool().add(pc);
    }
    //PI24001 end
    protected PageContextPool getPool() {
        PageContextPool pool = null;
        if ((pool = (PageContextPool) _threadLocal.get()) == null) {
            //pool = new PageContextPool(4) {
            pool = new PageContextPool(8) {		//PK25630
                protected PageContext createPageContext() {
                    return new PageContextImpl(JspFactoryImpl.this, bodyContentBufferSize);
                }
            };
            
            _threadLocal.set(pool);
        }

        return pool;
    }
    
    private class PrivilegedGetPageContext implements PrivilegedAction {

        private JspFactoryImpl factory;
        private Servlet servlet;
        private ServletRequest request;
        private ServletResponse response;
        private String errorPageURL;
        private boolean needsSession;
        private int bufferSize;
        private boolean autoflush;

        PrivilegedGetPageContext(
            JspFactoryImpl factory,
            Servlet servlet,
            ServletRequest request,
            ServletResponse response,
            String errorPageURL,
            boolean needsSession,
            int bufferSize,
            boolean autoflush) {
            this.factory = factory;
            this.servlet = servlet;
            this.request = request;
            this.response = response;
            this.errorPageURL = errorPageURL;
            this.needsSession = needsSession;
            this.bufferSize = bufferSize;
            this.autoflush = autoflush;
        }

        public Object run() {
            return factory.internalGetPageContext(servlet, request, response, errorPageURL, needsSession, bufferSize, autoflush);
        }
    }

    private class PrivilegedReleasePageContext implements PrivilegedAction {

        private JspFactoryImpl factory;
        private PageContext pageContext;

        PrivilegedReleasePageContext(JspFactoryImpl factory, PageContext pageContext) {
            this.factory = factory;
            this.pageContext = pageContext;
        }

        public Object run() {
            factory.internalReleasePageContext(pageContext);
            return null;
        }
    }
    
    //LIDB4147-9 Begin new method for JSP 2.1.

    // defect 388930 begin
	public JspApplicationContext getJspApplicationContext(ServletContext context) {
        return JspApplicationContextImpl.getInstance(context);	
	}
    // defect 388930 end

    //LIDB4147-9 End
}
