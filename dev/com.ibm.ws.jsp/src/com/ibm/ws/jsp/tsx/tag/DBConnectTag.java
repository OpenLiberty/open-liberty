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
package com.ibm.ws.jsp.tsx.tag;

import java.util.Hashtable;
import java.util.Stack;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.tsx.db.ConnectionProperties;

public class DBConnectTag extends BodyTagSupport {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257849891597268275L;
	private String id = "";
    private String userid = "";
    private String passwd = "";
    private String url = "";
    private String driver = "";
    private String jndiname = "";

    public DBConnectTag() {}

    public String getId() {
        return (id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserid() {
        return (userid);
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getPasswd() {
        return (passwd);
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getUrl() {
        return (url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDriver() {
        return (driver);
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getJndiname() {
        return (jndiname);
    }

    public void setJndiname(String jndiname) {
        this.jndiname = jndiname;
    }

    public int doStartTag() throws JspException {
        Stack connectionStack = (Stack) pageContext.getAttribute("TSXConnectionStack", PageContext.PAGE_SCOPE);
        if (connectionStack == null) {
            connectionStack = new Stack();
            pageContext.setAttribute("TSXConnectionStack", connectionStack, PageContext.PAGE_SCOPE);
        }
        DefinedIndexManager indexMgr = (DefinedIndexManager) pageContext.getAttribute("TSXDefinedIndexManager", PageContext.PAGE_SCOPE);
        if (indexMgr == null) {
            indexMgr = new DefinedIndexManager();
            pageContext.setAttribute("TSXDefinedIndexManager", indexMgr, PageContext.PAGE_SCOPE);
        }
        Hashtable connectionLookup = (Hashtable) pageContext.getAttribute("TSXConnectionLookup", PageContext.PAGE_SCOPE);
        if (connectionLookup == null) {
            connectionLookup = new Hashtable();
            pageContext.setAttribute("TSXConnectionLookup", connectionLookup, PageContext.PAGE_SCOPE);
        }

        if (id == null || id.equals("")) {
            id = indexMgr.getNextIndex();
        }
        else {
            if (indexMgr.exists(id) == true) {
                throw new JspException("Index specified in <tsx:dbconnect> tag has already been defined.");
            }
            else {
                indexMgr.addIndex(id);
            }
        }

        connectionStack.push(id);

        ConnectionProperties conn = null;
        try {
            if (jndiname == null || jndiname.equals("")) {
                conn = new ConnectionProperties(driver, url, userid, passwd);
            }
            else {
                conn = new ConnectionProperties(jndiname);
            }
            connectionLookup.put(id, conn);
        }
        catch (JspCoreException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.DBConnectTag.doStartTag", "114", this);
            throw new JspException("JasperException caught : " + e.toString());
        }

        return (EVAL_BODY_INCLUDE);
    }

    public int doEndTag() throws JspException {
        Stack connectionStack = (Stack) pageContext.getAttribute("TSXConnectionStack", PageContext.PAGE_SCOPE);
        connectionStack.pop();

        return (EVAL_PAGE);
    }

    public void release() {
        super.release();
        id = "";
        userid = "";
        passwd = "";
        url = "";
        driver = "";
        jndiname = "";
    }
}
