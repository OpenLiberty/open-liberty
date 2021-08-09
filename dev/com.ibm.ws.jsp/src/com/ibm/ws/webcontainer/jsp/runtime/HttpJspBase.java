/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.jsp.runtime;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.apache.jasper.runtime.BodyContentImpl;
import org.apache.jasper.runtime.JspFactoryImpl;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.runtime.UnsynchronizedStack;
//import org.apache.jasper.Constants;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

//import com.ibm.ejs.ras.Tr;
//import com.ibm.ejs.ras.TraceComponent;

//Begin IBM Jasper Change
//BKM TEMP import com.ibm.websphere.servlet.cache.CacheableServlet;
//End IBM Jasper Change

/**
 * This is the subclass of all JSP-generated servlets.
 *
 * @author Anil K. Vijendran
 */
public abstract class HttpJspBase
    extends HttpServlet
    implements HttpJspPage
//Begin IBM Jasper Change
//BKM TEMP               CacheableServlet
//End IBM Jasper Change
{
    static {
        if (JspFactory.getDefaultFactory() == null) {
            JspFactoryImpl factory = new JspFactoryImpl(BodyContentImpl.DEFAULT_TAG_BUFFER_SIZE);
            JspFactory.setDefaultFactory(factory);
        }
    }
    protected PageContext pageContext;
    private final static int MAX_POOLSIZE = 10;
    //private static TraceComponent tc = Tr.register(HttpJspBase.class.getName(), "JSP_Engine");

//Begin IBM Jasper Change
    private static TagHandlerPool tagPool = new TagHandlerPool();
    String servletName=null;
//End IBM Jasper Change

    protected HttpJspBase() {
    }

    public final void init(ServletConfig config)
        throws ServletException
    {
    //    if (tc.isEntryEnabled())
    //        Tr.entry(tc, "init");
        super.init(config);
        jspInit();
    //    if (tc.isEntryEnabled())
    //        Tr.exit(tc, "init");
    }

    public String getServletInfo() {
        return JspCoreException.getMsg("jsp.engine.info");
    }

	/* begin 137808 - remove setServletName() and getServletName()
	//Begin IBM Jasper Change defect 112324
    public void setServletName(String servletName) {
    	this.servletName=servletName;
    }
    public String getServletName() {
		return this.servletName;
    }
	//End IBM Jasper Change defect 112324
	 */

    public final void destroy() {
        jspDestroy();
    }

    /**
     * Entry point into service.
     */
    public final void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        _jspService(request, response);
    }

    public void jspInit() {
    }

    public void jspDestroy() {
    }

    public abstract void _jspService(HttpServletRequest request,
                                     HttpServletResponse response)
        throws ServletException, IOException;

//Begin IBM Jasper Change
    public String getId(HttpServletRequest request) {
        return null;
    }

    public int getSharingPolicy(HttpServletRequest request) {
        return 1;
    }

    protected Tag getTagHandler(String tagKey, String tagClassName) {
        //if (tc.isEntryEnabled())
        //    Tr.entry(tc, "getTagHandler");
        //if (tc.isDebugEnabled())
        //    Tr.debug(tc, "tagKey: " + tagKey +", tagClassName: "+tagClassName);
        Tag tag = null;
        HashMap pool = tagPool.getPool();
        UnsynchronizedStack stack = (UnsynchronizedStack)pool.get(tagKey);
        if (stack == null) {
            stack = new UnsynchronizedStack(MAX_POOLSIZE);
            pool.put(tagKey, stack);
	   //     if (tc.isDebugEnabled())
	   //         Tr.debug(tc, "created new stack for tagKey: " + tagKey);
            try {
                ClassLoader contextClassLoader = ThreadContextHelper.getContextClassLoader();
                Class tagClass = Class.forName(tagClassName, true, contextClassLoader);
                tag = (Tag)tagClass.newInstance();
		        //if (tc.isDebugEnabled())
		        //    Tr.debug(tc, "Created first new instance of tagKey: " + tagKey);
            }
            catch (Exception e) {
			//	com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.runtime.HttpJspBase.getTagHandler", "108", this);
			//	if (tc.isDebugEnabled())
			//	    Tr.debug(tc, "Error loading Tag Class " + tagClassName + " : " + e);
            }
        }
        else {
            tag = (Tag)stack.pop();
            if (tag == null) {
                try {
                    ClassLoader contextClassLoader = ThreadContextHelper.getContextClassLoader();
                    Class tagClass = Class.forName(tagClassName, true, contextClassLoader);
                    tag = (Tag)tagClass.newInstance();
			        //if (tc.isDebugEnabled())
			        //    Tr.debug(tc, "Created new instance of tagKey: " + tagKey + " after pop() returned null");
                }
                catch (Exception e) {
				//	com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.runtime.HttpJspBase.getTagHandler", "121", this);
				//	if (tc.isDebugEnabled())
				//	    Tr.debug(tc, "Error loading Tag Class " + tagClassName + " : " + e);
                }
            }
            else {
		    //    if (tc.isDebugEnabled())
		    //        Tr.debug(tc, "successfully popped instance of tagKey: "  + tagKey);
            }

        }

        //if (tc.isEntryEnabled())
        //    Tr.exit(tc, "getTagHandler");
        return (tag);
    }

    protected void putTagHandler(String tagKey, Tag tag) {
        //if (tc.isEntryEnabled())
        //    Tr.entry(tc, "putTagHandler");
        HashMap pool = tagPool.getPool();
        UnsynchronizedStack stack = (UnsynchronizedStack)pool.get(tagKey);
        //if (tc.isDebugEnabled())
        //    Tr.debug(tc, "stacksize for " + tagKey + " is " + stack.size());
        if (stack.size() < MAX_POOLSIZE) {
            stack.push(tag);
	        //if (tc.isDebugEnabled())
	        //    Tr.debug(tc, "pushed instance of " + tagKey);
        }
        else {
	        //if (tc.isDebugEnabled())
	        //    Tr.debug(tc, "destroyed instance of " + tagKey);
            tag.release();
            tag = null;
        }
        //if (tc.isEntryEnabled())
        //    Tr.exit(tc, "putTagHandler");
    }
}
//End IBM Jasper Change
