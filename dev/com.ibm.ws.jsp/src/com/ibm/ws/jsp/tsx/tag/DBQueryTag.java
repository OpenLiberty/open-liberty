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

import java.sql.SQLException;
import java.util.Hashtable;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.tsx.db.ConnectionProperties;
import com.ibm.ws.jsp.tsx.db.Query;
import com.ibm.ws.jsp.tsx.db.QueryResults;

public class DBQueryTag extends BodyTagSupport {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 4121409614048015410L;
	private String id = "";
    private String connection = "";
    private int limit = 0;

    public DBQueryTag() {}

    public String getId() {
        return (id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConnection() {
        return (connection);
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public int getLimit() {
        return (limit);
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int doEndTag() throws JspException {

        Hashtable connectionLookup = (Hashtable) pageContext.getAttribute("TSXConnectionLookup", PageContext.PAGE_SCOPE);
        if (connectionLookup == null) {
            throw new JspException("No dbconnect tag found in jsp : ");
        }
        DefinedIndexManager indexMgr = (DefinedIndexManager) pageContext.getAttribute("TSXDefinedIndexManager", PageContext.PAGE_SCOPE);
        if (indexMgr == null) {
            throw new JspException("No dbconnect tag found in jsp : ");
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

        StringBuffer outputString = new StringBuffer();
        ConnectionProperties conn = (ConnectionProperties) connectionLookup.get(connection);

        QueryResults qr = new QueryResults();
        try {
            Query query = new Query(conn, getBodyContent().getString());
            if (limit != 0) {
                query.setMaxRows(limit);
            }
            qr = query.execute();
            pageContext.setAttribute(id, qr, PageContext.PAGE_SCOPE);
        }
        catch (SQLException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.DBQueryTag.doEndTag", "83", this);
            throw new JspException("SQLException: " + e.toString());
        }
        catch (JspCoreException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.DBQueryTag.doEndTag", "87", this);
            throw new JspException("JasperException: " + e.toString());
        }

        return (EVAL_PAGE);
    }

    public void release() {
        super.release();
        id = "";
        connection = "";
        limit = 0;
    }
}
