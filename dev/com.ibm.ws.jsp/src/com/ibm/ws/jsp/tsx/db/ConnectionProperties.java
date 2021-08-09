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
package com.ibm.ws.jsp.tsx.db;

import com.ibm.ws.jsp.JspCoreException;

/**
 * This type was created in VisualAge.
 */
public class ConnectionProperties {

    private String dbDriver = null;
    private String loginUser = null;
    private String loginPasswd = null;
    private String url = null;
    private String jndiName = null;
/*
 * This method was created in VisualAge.
 * @param qs java.lang.String
 * @param qr com.ibm.websphere.jsp.QueryResults
 */
    protected ConnectionProperties()
    {
// do nothing   
    }
/**
 * This method was created in VisualAge.
 * @param jndiName java.lang.String
 */
    public ConnectionProperties(String jndiName) throws JspCoreException
    {
        setJndiName(jndiName);
    }
/**
 * This method was created in VisualAge.
 * @param dbDriver java.lang.String
 * @param url java.lang.String
 * @param loginUser java.lang.String
 * @param loginPasswd java.lang.String
 */

    public ConnectionProperties(String dbDriver,
                                String url, 
                                String loginUser, 
                                String loginPasswd)
    throws JspCoreException
    {
        setDbDriver(dbDriver);
        setUrl(url);
        setLoginUser(loginUser);
        setLoginPasswd(loginPasswd);
    }
/**
 * This method was created in VisualAge.
 * @param dbDriver java.lang.String
 * @param url java.lang.String
 * @param loginUser java.lang.String
 * @param loginPasswd java.lang.String
 * @param queryString java.lang.String
 */
    public ConnectionProperties(String dbDriver,
                                String url, 
                                String loginUser, 
                                String loginPasswd, 
                                String queryString)
    throws JspCoreException
    {
        setDbDriver(dbDriver);
        setUrl(url);
        setLoginUser(loginUser);
        setLoginPasswd(loginPasswd);
    }
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 */
    public String getDbDriver() {
        return dbDriver;
    }
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 */
    public String getJndiName() {
        return jndiName;
    }
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 */
    public String getLoginPasswd() {
        return loginPasswd;
    }
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 */
    public String getLoginUser() {
        return loginUser;
    }
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 */
    public String getUrl() {
        return url;
    }
/**
 * This method was created in VisualAge.
 * @param newValue java.lang.String
 */
    public void setDbDriver(String newValue)
    throws JspCoreException
    {
        if (newValue == null) {
            throw new JspCoreException(JspConstants.NullDbDriver);
        }
        this.dbDriver = newValue.trim();
    }
/**
 * This method was created in VisualAge.
 * @param newValue java.lang.String
 */
    public void setJndiName(String newValue) {
        this.jndiName = newValue;
    }
/**
 * This method was created in VisualAge.
 * @param newValue java.lang.String
 */
    public void setLoginPasswd(String newValue)
    {
        
        if (newValue == null) {
            this.loginPasswd = null;
        }
        else {
            this.loginPasswd = newValue.trim();
        }
    }
/**
 * This method was created in VisualAge.
 * @param newValue java.lang.String
 */
    public void setLoginUser(String newValue)
    {
        if (newValue == null) {
            this.loginUser = null;
        }
        else {
            this.loginUser = newValue.trim();
        }
    }
/**
 * This method was created in VisualAge.
 * @param newValue java.lang.String
 */
    public void setUrl(String newValue)
    throws JspCoreException
    {
        if (newValue == null) {
            throw new JspCoreException(JspConstants.NullUrl);
        }
        this.url = newValue.trim();
    }
/**
 * This method was created in VisualAge.
 */
    protected void verify()
    throws JspCoreException
    {
// do any more verification that may be necessary
    }
}
