/*******************************************************************************
 * Copyright (c) 2010, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.servlet.SessionCookieConfig;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.utils.LoggingUtil;

/*-
 * Moved from SERV1/ws/code/web.webcontainer/src/com/ibm/ws/webcontainer/session/SessionCookieConfigImpl.java 
 * since it doesn't make sense for the session bundle to export a com.ibm.ws.webcontainer package.
 * 
 * Servlet 6.0 - Update to support new APIs; use one Map for all specific attributes.
 */
public class SessionCookieConfigImpl implements SessionCookieConfig, Cloneable {
    private static final String methodClassName = "SessionCookieConfigImpl";


    protected String comment = null;
    private boolean maxAgeSet = false;
    protected String name = null;
    private boolean httpOnlySet = false;
    private boolean secureSet = false;
    protected boolean contextInitialized = false;
    protected boolean programmaticChange = false;
    
    protected static final boolean EXTERNALCALL = true;
    private static final String DOMAIN = "Domain";
    private static final String MAX_AGE = "Max-Age";
    private static final String PATH = "Path";
    private static final String SECURE = "Secure";
    private static final String HTTPONLY = "HttpOnly";

    protected Map<String, String> attributes = null;


    private static TraceNLS nls = TraceNLS.getTraceNLS(SessionCookieConfigImpl.class, "com.ibm.ws.webcontainer.resources.Messages");

    public SessionCookieConfigImpl() {
        this(null, null, null, null, -1, true, false); //set default value in the Map
    }

    public SessionCookieConfigImpl(String name, String domain, String path, String comment, int maxAge, boolean httpOnly, boolean secure) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " Constructor , cookie name [" + name + "]");
        }

        this.name = name;
        this.comment = comment;
        putAttribute(DOMAIN, domain);
        putAttribute(PATH, path);
        putAttribute(MAX_AGE, String.valueOf(maxAge));
        putAttribute(HTTPONLY, String.valueOf(httpOnly));
        putAttribute(SECURE, String.valueOf(secure));
    }

    protected String getAttribute(String name) {
        return (this.attributes == null) ? null : this.attributes.get(name);
    }
    
    protected Map<String, String> getAttributes() {
        return (this.attributes == null) ? Collections.<String, String> emptyMap() : Collections.<String, String> unmodifiableMap(this.attributes);
    }

    @Override
    public String getComment() {
        return comment;
    } 

    @Override
    public String getDomain() {
        return getAttribute(DOMAIN);
    }

    @Override
    public int getMaxAge() {
        String maxAge = getAttribute(MAX_AGE);
        return (maxAge == null) ? -1 : Integer.parseInt(maxAge);
    }

    @Override
    public String getName() {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " getName returns [" + name + "] , this [{0}]", this);
        }
        return name;
    }

    @Override
    public String getPath() {
        return getAttribute(PATH);
    }

    @Override
    public boolean isHttpOnly() {
        return Boolean.parseBoolean(getAttribute(HTTPONLY));
    }

    @Override
    public boolean isSecure() {
        return Boolean.parseBoolean(getAttribute(SECURE));
    }

    protected void putAttribute(String name, String value) {
        if (this.attributes == null)
            this.attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        
        if (value == null) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " putAttribute value is null, remove name [" + name + "] , this [{0}]", this);
            }
            this.attributes.remove(name);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " putAttribute name [" + name + "] , value [" +value +"] , this [{0}]", this);
            }
            this.attributes.put(name, value);
        }
    }
   
    @Override
    public void setComment(String c) {
        setComment(c, EXTERNALCALL);
    }
    
    @Override
    public void setDomain(String d) {
        setDomain(d, EXTERNALCALL);
    }

    @Override
    public void setHttpOnly(boolean b) {
        setHttpOnly(b, EXTERNALCALL);
    }

    @Override
    public void setMaxAge(int m) {
        setMaxAge(m, EXTERNALCALL);
    }

    @Override
    public void setName(String n) {
        setName(n, EXTERNALCALL);
    }

    @Override
    public void setPath(String p) {
        setPath(p, EXTERNALCALL);
    }

    @Override
    public void setSecure(boolean b) {
        setSecure(b, EXTERNALCALL);
    }
   
    public boolean isMaxAgeSet() {
        return maxAgeSet;
    }

    public boolean isHttpOnlySet() {
        return httpOnlySet;
    }

    public boolean isSecureSet() {
        return secureSet;
    }

    public void setContextInitialized() {
        contextInitialized = true;
    }

    public void setComment(String c, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        comment = c;
    }

    public void setDomain(String d, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        putAttribute(DOMAIN, (d != null) ? d : null);
    }

    public void setHttpOnly(boolean b, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        putAttribute(HTTPONLY, String.valueOf(b));
        httpOnlySet = true;
    }

    public void setMaxAge(int m, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        putAttribute(MAX_AGE, String.valueOf(m));
        maxAgeSet = true;
    }

    public void setName(String n, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " setName [" + n + "] , this [{0}]", this);
        }
        
        name = n;
    }

    public void setPath(String p, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        putAttribute(PATH, p);
    }

    public void setSecure(boolean b, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        putAttribute(SECURE, String.valueOf(b));
        secureSet = true;
    }

    @Override
    public SessionCookieConfig clone() throws CloneNotSupportedException {
        SessionCookieConfigImpl temp = new SessionCookieConfigImpl(getName(), getDomain(), getPath(), comment, getMaxAge(), isHttpOnly(), isSecure());
        temp.attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        temp.attributes.putAll(this.attributes);

        return temp;
    }

    protected void throwWarning() {
        String msg = nls.getString("programmatic.sessions.already.been.initialized");
        throw new IllegalStateException(msg);
    }

    public boolean isProgrammaticChange() {
        return programmaticChange;
    }
}

