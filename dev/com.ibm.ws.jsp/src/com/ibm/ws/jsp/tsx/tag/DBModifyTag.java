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
import java.sql.*;

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.tsx.db.*;

public class DBModifyTag extends BodyTagSupport {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3763099643902310454L;
	private String connection = "";

    public DBModifyTag() {}

    public String getConnection() {
        return (connection);
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public int doEndTag() throws JspException {

        Hashtable connectionLookup = (Hashtable) pageContext.getAttribute("TSXConnectionLookup", PageContext.PAGE_SCOPE);
        if (connectionLookup == null) {
            throw new JspException("No dbconnect tag found in jsp : ");
        }

        StringBuffer outputString = new StringBuffer();
        ConnectionProperties conn = (ConnectionProperties) connectionLookup.get(connection);

        try {
            Query query = new Query(conn, getBodyContent().getString());
            query.executeUpdate();
        }
        catch (SQLException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.DBModifyTag.doEndTag", "43", this);
            outputString.append("Exception: " + e.toString());
        }
        catch (JspCoreException e) {
            //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.DBModifyTag.doEndTag", "47", this);
            outputString.append("Exception: " + e.toString());
        }

        if (outputString.length() > 0) {
            JspWriter writer = pageContext.getOut();
            try {
                writer.print(outputString.toString());
            }
            catch (java.io.IOException e) {
                //com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.jsp.tsx.tag.DBModifyTag.doEndTag", "57", this);
                throw new JspException("IOException writing tag : " + e.toString());
            }
        }
        return (EVAL_PAGE);
    }

    public void release() {
        super.release();
        connection = "";
    }
}
