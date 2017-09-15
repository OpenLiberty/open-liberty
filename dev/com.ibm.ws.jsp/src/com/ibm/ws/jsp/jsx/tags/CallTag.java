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
package com.ibm.ws.jsp.jsx.tags;

import java.io.FileReader;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import com.ibm.bsf.BSFEngine;
import com.ibm.bsf.BSFManager;
import com.ibm.bsf.util.IOUtils;
import com.ibm.ws.ffdc.FFDCFilter;


public class CallTag extends TagSupport implements Tag
{
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3256446884846449968L;
	String source;
    String function;
    Vector arguments;

    public void setSource(String s)
    {
        source = s;
    }

    public String getSource()
    {
        return source;
    }

    public void setFunction(String f)
    {
        function = f;
    }

    public String getFunction()
    {
        return function;
    }

    public Vector getArguments()
    {
        return arguments;
    }

    /**
     * @see TagSupport#doStartTag()
     */
    public int doStartTag() throws JspException 
    {
        arguments = new Vector();       
        return EVAL_BODY_INCLUDE;
    }

    /**
     * @see TagSupport#doEndTag()
     */
    public int doEndTag() throws JspException 
    {
        try
        {
            BSFManager mgr = new BSFManager();  
            mgr.declareBean("context", pageContext.getServletContext(), ServletContext.class);
            mgr.declareBean("config", pageContext.getServletConfig(), ServletConfig.class);
            mgr.declareBean("request", pageContext.getRequest(), HttpServletRequest.class);
            mgr.declareBean("response", pageContext.getResponse(), HttpServletResponse.class);
            mgr.declareBean("session", pageContext.getSession(), HttpSession.class);                        
            mgr.declareBean("pageContext", pageContext, PageContext.class);

            String fileName = pageContext.getServletContext().getRealPath(source);
            //PM21451 Start
            BSFEngine engine = null;
            if(fileName != null) {
                engine = mgr.loadScriptingEngine(mgr.getLangFromFilename(fileName));
                engine.exec(fileName, 0, 0, IOUtils.getStringFromReader(new FileReader(fileName)));
            } else {
                throw new Exception("source not found -->"+ source); 
            }
            //PM21451 End

            Object rc = null;
            if (arguments.isEmpty())
            {
                rc = engine.call(null, function, new Object[]{});
            }
            else
            {
                Object[] args = new Object[arguments.size()];
                arguments.copyInto(args);
                rc = engine.call(null, function, args);
            }
            if (getId() != null)
                pageContext.setAttribute(getId(), rc);

//			System.out.println(source + "::" + function + "--> " + rc);				
        }
        catch (Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.jsp.jsx.tags.CallTags.doEndTag", "106", new Object[] {pageContext, arguments});
        }
        
        return EVAL_PAGE;
    }

    /**
     * @see TagSupport#release()
     */
    public void release() 
    {
        arguments.clear();
        arguments = null;
        source = null;
        function = null;
    }
}