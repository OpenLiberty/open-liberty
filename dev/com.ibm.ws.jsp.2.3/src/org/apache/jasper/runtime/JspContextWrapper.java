/*
 * $Header: /cvshome/wascvs/jsp/src/org/apache/jasper/runtime/JspContextWrapper.java,v 1.1.1.1 2003/10/17 13:47:57 backhous Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2003/10/17 13:47:57 $
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
// * feature LIDB4147-9 "Integrate Unified Expression Language"  2006/08/14  Scott Johnson

package org.apache.jasper.runtime;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.VariableInfo;

import com.ibm.ws.jsp.JspCoreException;


/**
 * Implementation of a JSP Context Wrapper.
 *
 * The JSP Context Wrapper is a JspContext created and maintained by a tag
 * handler implementation. It wraps the Invoking JSP Context, that is, the
 * JspContext instance passed to the tag handler by the invoking page via
 * setJspContext().
 *
 * @author Kin-man Chung
 * @author Jan Luehe
 */
public class JspContextWrapper extends PageContext implements VariableResolver {

    // Invoking JSP context
    private PageContext invokingJspCtxt;

    private transient Hashtable	pageAttributes;

    // ArrayList of NESTED scripting variables
    private ArrayList nestedVars;

    // ArrayList of AT_BEGIN scripting variables
    private ArrayList atBeginVars;

    // ArrayList of AT_END scripting variables
    private ArrayList atEndVars;

    private Map aliases;

    private Hashtable originalNestedVars;

    public JspContextWrapper(JspContext jspContext, ArrayList nestedVars,
			ArrayList atBeginVars, ArrayList atEndVars, Map aliases) {
        this.invokingJspCtxt = (PageContext) jspContext;
		this.nestedVars = nestedVars;
		this.atBeginVars = atBeginVars;
		this.atEndVars = atEndVars;
		this.pageAttributes = new Hashtable(16);
		this.aliases = aliases;
	
		if (nestedVars != null) {
		    this.originalNestedVars = new Hashtable(nestedVars.size());
		}
		syncBeginTagFile();
    }

    public void initialize(Servlet servlet, ServletRequest request,
                           ServletResponse response, String errorPageURL,
                           boolean needsSession, int bufferSize,
                           boolean autoFlush)
        throws IOException, IllegalStateException, IllegalArgumentException
    {
    }
    
    public Object getAttribute(String name) {

	if (name == null) {
	    throw new NullPointerException(
	            JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

	return pageAttributes.get(name);
    }

    public Object getAttribute(String name, int scope) {

	if (name == null) {
	    throw new NullPointerException(
        JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

	if (scope == PAGE_SCOPE) {
	    return pageAttributes.get(name);
	}

	return invokingJspCtxt.getAttribute(name, scope);
    }

    public void setAttribute(String name, Object value) {

	if (name == null) {
	    throw new NullPointerException(
        JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

	if (value != null) {
	    pageAttributes.put(name, value);
	} else {
	    removeAttribute(name, PAGE_SCOPE);
	}
    }

    public void setAttribute(String name, Object value, int scope) {

	if (name == null) {
	    throw new NullPointerException(
        JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

	if (scope == PAGE_SCOPE) {
	    if (value != null) {
		pageAttributes.put(name, value);
	    } else {
		removeAttribute(name, PAGE_SCOPE);
	    }
	} else {
	    invokingJspCtxt.setAttribute(name, value, scope);
	}
    }

    public Object findAttribute(String name) {

	if (name == null) {
	    throw new NullPointerException(
        JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

        Object o = pageAttributes.get(name);
        if (o == null) {
	    o = invokingJspCtxt.getAttribute(name, REQUEST_SCOPE);
	    if (o == null) {
		if (getSession() != null) {
		    o = invokingJspCtxt.getAttribute(name, SESSION_SCOPE);
		}
		if (o == null) {
		    o = invokingJspCtxt.getAttribute(name, APPLICATION_SCOPE);
		} 
	    }
	}

	return o;
    }

    public void removeAttribute(String name) {

	if (name == null) {
	    throw new NullPointerException(
        JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

	pageAttributes.remove(name);
	invokingJspCtxt.removeAttribute(name, REQUEST_SCOPE);
	if (getSession() != null) {
	    invokingJspCtxt.removeAttribute(name, SESSION_SCOPE);
	}
	invokingJspCtxt.removeAttribute(name, APPLICATION_SCOPE);
    }

    public void removeAttribute(String name, int scope) {

	if (name == null) {
	    throw new NullPointerException(
        JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

	if (scope == PAGE_SCOPE){
	    pageAttributes.remove(name);
	} else {
	    invokingJspCtxt.removeAttribute(name, scope);
	}
    }

    public int getAttributesScope(String name) {

	if (name == null) {
	    throw new NullPointerException(
        JspCoreException.getMsg("jsp.error.attribute.null_name"));
	}

	if (pageAttributes.get(name) != null) {
	    return PAGE_SCOPE;
	} else {
	    return invokingJspCtxt.getAttributesScope(name);
	}
    }

    public Enumeration getAttributeNamesInScope(int scope) {
        if (scope == PAGE_SCOPE) {
            return pageAttributes.keys();
	}

	return invokingJspCtxt.getAttributeNamesInScope(scope);
    }

    public void release() {
	invokingJspCtxt.release();
    }

    public JspWriter getOut() {
	return invokingJspCtxt.getOut();
    }

    public HttpSession getSession() {
	return invokingJspCtxt.getSession();
    }

    public Object getPage() {
	return invokingJspCtxt.getPage();
    }

    public ServletRequest getRequest() {
	return invokingJspCtxt.getRequest();
    }

    public ServletResponse getResponse() {
	return invokingJspCtxt.getResponse();
    }

    public Exception getException() {
	return invokingJspCtxt.getException();
    }

    public ServletConfig getServletConfig() {
	return invokingJspCtxt.getServletConfig();
    }

    public ServletContext getServletContext() {
	return invokingJspCtxt.getServletContext();
    }

    public void forward(String relativeUrlPath)
        throws ServletException, IOException
    {
	invokingJspCtxt.forward(relativeUrlPath);
    }

    public void include(String relativeUrlPath)
	throws ServletException, IOException
    {
	invokingJspCtxt.include(relativeUrlPath);
    }

    public void include(String relativeUrlPath, boolean flush) 
	    throws ServletException, IOException {
	include(relativeUrlPath, false); // XXX
    }

    public VariableResolver getVariableResolver() {
	return this;
    }

    public BodyContent pushBody() {
	return invokingJspCtxt.pushBody();
    }

    public JspWriter pushBody(Writer writer) {
	return invokingJspCtxt.pushBody(writer);
    }

    public JspWriter popBody() {
        return invokingJspCtxt.popBody();
    }

    public ExpressionEvaluator getExpressionEvaluator() {
	return invokingJspCtxt.getExpressionEvaluator();
    }

    public void handlePageException(Exception ex)
        throws IOException, ServletException 
    {
	// Should never be called since handleException() called with a
	// Throwable in the generated servlet.
	handlePageException((Throwable) ex);
    }

    public void handlePageException(Throwable t)
        throws IOException, ServletException 
    {
	invokingJspCtxt.handlePageException(t);
    }

    /**
     * VariableResolver interface
     */

    //LIDB4147-9 Begin - modified for JSP 2.1
    public Object resolveVariable(String pName) throws ELException {
		ELContext ctx = this.getELContext();
		return ctx.getELResolver().getValue(ctx, null, pName);
	}
    //LIDB4147-9 End

    /**
     * Synchronize variables at begin of tag file
     */
    public void syncBeginTagFile() {
	saveNestedVariables();
    }

    /**
     * Synchronize variables before fragment invokation
     */
    public void syncBeforeInvoke() {
	copyTagToPageScope(VariableInfo.NESTED);
	copyTagToPageScope(VariableInfo.AT_BEGIN);
    }

    /**
     * Synchronize variables at end of tag file
     */
    public void syncEndTagFile() {
	copyTagToPageScope(VariableInfo.AT_BEGIN);
	copyTagToPageScope(VariableInfo.AT_END);
	restoreNestedVariables();
    }

    /**
     * Copies the variables of the given scope from the virtual page scope of
     * this JSP context wrapper to the page scope of the invoking JSP context.
     *
     * @param scope variable scope (one of NESTED, AT_BEGIN, or AT_END)
     */
    private void copyTagToPageScope(int scope) {
	Iterator iter = null;

	switch (scope) {
	case VariableInfo.NESTED:
	    if (nestedVars != null) {
		iter = nestedVars.iterator();
	    }
	    break;
	case VariableInfo.AT_BEGIN:
	    if (atBeginVars != null) {
		iter = atBeginVars.iterator();
	    }
	    break;
	case VariableInfo.AT_END:
	    if (atEndVars != null) {
		iter = atEndVars.iterator();
	    }
	    break;
	}

	while ((iter != null) && iter.hasNext()) {
	    String varName = (String) iter.next();
	    Object obj = getAttribute(varName);
	    varName = findAlias(varName);
	    if (obj != null) {
		invokingJspCtxt.setAttribute(varName, obj);
	    } else {
		invokingJspCtxt.removeAttribute(varName, PAGE_SCOPE);
	    }
	}
    }

    /**
     * Saves the values of any NESTED variables that are present in
     * the invoking JSP context, so they can later be restored.
     */
    private void saveNestedVariables() {
	if (nestedVars != null) {
	    Iterator iter = nestedVars.iterator();
	    while (iter.hasNext()) {
		String varName = (String) iter.next();
		varName = findAlias(varName);
		Object obj = invokingJspCtxt.getAttribute(varName);
		if (obj != null) {
		    originalNestedVars.put(varName, obj);
		}
	    }
	}
    }

    /**
     * Restores the values of any NESTED variables in the invoking JSP
     * context.
     */
    private void restoreNestedVariables() {
	if (nestedVars != null) {
	    Iterator iter = nestedVars.iterator();
	    while (iter.hasNext()) {
		String varName = (String) iter.next();
		varName = findAlias(varName);
		Object obj = originalNestedVars.get(varName);
		if (obj != null) {
		    invokingJspCtxt.setAttribute(varName, obj);
		} else {
		    invokingJspCtxt.removeAttribute(varName, PAGE_SCOPE);
		}
	    }
	}
    }

    /**
     * Checks to see if the given variable name is used as an alias, and if so,
     * returns the variable name for which it is used as an alias.
     *
     * @param varName The variable name to check
     * @return The variable name for which varName is used as an alias, or
     * varName if it is not being used as an alias
     */
    private String findAlias(String varName) {

	if (aliases == null)
	    return varName;

	String alias = (String) aliases.get(varName);
	if (alias == null) {
	    return varName;
	}
	return alias;
    }

    //LIDB4147-9 Begin - new method for JSP 2.1
    public ELContext getELContext() {
        return this.invokingJspCtxt.getELContext();
	}
    //LIDB4147-9 End
}

