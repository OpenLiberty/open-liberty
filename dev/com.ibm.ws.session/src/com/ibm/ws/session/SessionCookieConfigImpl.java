/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import javax.servlet.SessionCookieConfig;

import com.ibm.ejs.ras.TraceNLS;

/*-
 * Moved from SERV1/ws/code/web.webcontainer/src/com/ibm/ws/webcontainer/session/SessionCookieConfigImpl.java 
 * since it doesn't make sense for the session bundle to export a com.ibm.ws.webcontainer package.
 */
public class SessionCookieConfigImpl implements SessionCookieConfig, Cloneable {

    private String comment = null;
    private String domain = null;
    private int maxAge = -1;
    private boolean maxAgeSet = false;
    private String name = null;
    private String path = null;
    private boolean httpOnly = true;
    private boolean httpOnlySet = false;
    private boolean secure = false;
    private boolean secureSet = false;
    private boolean contextInitialized = false;
    private static final boolean externalCall = true;
    private boolean programmaticChange = false;

    private static TraceNLS nls = TraceNLS.getTraceNLS(SessionCookieConfigImpl.class, "com.ibm.ws.webcontainer.resources.Messages");

    public SessionCookieConfigImpl() {}

    public SessionCookieConfigImpl(String name, String domain, String path, String comment, int maxAge, boolean httpOnly, boolean secure) {
        this.name = name;
        this.domain = domain;
        this.path = path;
        this.comment = comment;
        this.maxAge = maxAge;
        this.httpOnly = httpOnly;
        this.secure = secure;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public int getMaxAge() {
        return maxAge;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isHttpOnly() {
        return httpOnly;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public void setComment(String c) {
        setComment(c, SessionCookieConfigImpl.externalCall);
    }

    @Override
    public void setDomain(String d) {
        setDomain(d, SessionCookieConfigImpl.externalCall);
    }

    @Override
    public void setHttpOnly(boolean b) {
        setHttpOnly(b, SessionCookieConfigImpl.externalCall);
    }

    @Override
    public void setMaxAge(int m) {
        setMaxAge(m, SessionCookieConfigImpl.externalCall);
    }

    @Override
    public void setName(String n) {
        setName(n, SessionCookieConfigImpl.externalCall);
    }

    @Override
    public void setPath(String p) {
        setPath(p, SessionCookieConfigImpl.externalCall);
    }

    @Override
    public void setSecure(boolean b) {
        setSecure(b, SessionCookieConfigImpl.externalCall);
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
        domain = d;
    }

    public void setHttpOnly(boolean b, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        httpOnly = b;
        httpOnlySet = true;
    }

    public void setMaxAge(int m, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        maxAge = m;
        maxAgeSet = true;
    }

    public void setName(String n, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
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
        path = p;
    }

    public void setSecure(boolean b, boolean externalCall) {
        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }
        secure = b;
        secureSet = true;
    }

    @Override
    public SessionCookieConfigImpl clone() throws CloneNotSupportedException {
        SessionCookieConfigImpl temp = new SessionCookieConfigImpl(name, domain, path, comment, maxAge, httpOnly, secure);
        return temp;
    }

    private void throwWarning() {
        String msg = nls.getString("programmatic.sessions.already.been.initialized");
        throw new IllegalStateException(msg);
    }

    public boolean isProgrammaticChange() {
        return programmaticChange;
    }

}
