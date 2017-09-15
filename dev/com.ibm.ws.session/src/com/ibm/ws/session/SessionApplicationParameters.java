/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.util.EnumSet;

import javax.servlet.ServletContext;
import javax.servlet.SessionTrackingMode;

public class SessionApplicationParameters {

    private String sapAppName = null;
    private long sapSessionTimeout = 0;
    private SessionCookieConfigImpl sapSessionCookieConfig = null;
    private EnumSet<SessionTrackingMode> sapSessionTrackingMode = null;
    private final boolean sapDistributableWebApp;
    private final boolean sapAllowDispatchRemoteInclude;
    private final ServletContext sapServletContext;
    private String _J2EEName = null;
    private final ClassLoader _appClassLoader;
    private boolean hasApplicationSession = false;
    private String applicationSessionName = null;
    private boolean sessionConfigOverridden = false;

    public SessionApplicationParameters(String appName,
                                        boolean session_timeout_set,
                                        long session_timeout,
                                        boolean distributableWebApp,
                                        boolean allowDispatchRemoteInclude,
                                        ServletContext sc,
                                        ClassLoader appClassLoader,
                                        String j2eeName,
                                        SessionCookieConfigImpl cookieConfig,
                                        boolean moduleSessionTrackingModeSet,
                                        EnumSet<SessionTrackingMode> sessionTrackingMode) {
        sapAppName = appName;
        _J2EEName = j2eeName;

        //need to allow the session timeout to function as it did in v7.0 as that was previously supported by the specification
        if (!(session_timeout_set)) {
            sapSessionTimeout = 0;
        } else if (session_timeout > 0) {
            sapSessionTimeout = session_timeout * 60;
        } else {
            sapSessionTimeout = -1;
        }

        //this happens before the creation of the new SessionContext, so the cookieConfig received is only what is within the web.xml
        //this is stored in the sapSessionCookieConfig so as to update the SessionManagerConfig's values after we get the SMC 
        if ((sapSessionCookieConfig = cookieConfig) != null) {
            sessionConfigOverridden = true;
        }
        if (moduleSessionTrackingModeSet) {
            sessionConfigOverridden = true;
            sapSessionTrackingMode = sessionTrackingMode;
        }
        sapDistributableWebApp = distributableWebApp;
        sapAllowDispatchRemoteInclude = allowDispatchRemoteInclude;
        sapServletContext = sc;
        _appClassLoader = appClassLoader;
    }

    //Liberty - Used by extensions to set the sessionTimeout
    public void setSapSessionTimeout(long sapSessionTimeout) {
        this.sapSessionTimeout = sapSessionTimeout;
    }

    public String getAppName() {
        return sapAppName;
    }

    public long getSessionTimeout() {
        return sapSessionTimeout;
    }

    SessionCookieConfigImpl getSessionCookieConfig() {
        return sapSessionCookieConfig;
    }

    EnumSet<SessionTrackingMode> getSessionTrackingModes() {
        return sapSessionTrackingMode;
    }

    boolean getDistributableWebApp() {
        return sapDistributableWebApp;
    }

    public boolean getAllowDispatchRemoteInclude() {
        return sapAllowDispatchRemoteInclude;
    }

    public ServletContext getServletContext() {
        return sapServletContext;
    }

    public String getJ2EEName() {
        return _J2EEName;
    }

    public ClassLoader getAppClassLoader() {
        return _appClassLoader;
    }

    // for application Sessions
    public void setHasApplicationSession(boolean b) {
        hasApplicationSession = b;
    }

    public boolean getHasApplicationSession() {
        return hasApplicationSession;
    }

    public void setApplicationSessionName(String s) {
        applicationSessionName = s;
    }

    public String getApplicationSessionName() {
        return applicationSessionName;
    }

    public boolean isSessionConfigOverridden() {
        return sessionConfigOverridden;
    }
}
