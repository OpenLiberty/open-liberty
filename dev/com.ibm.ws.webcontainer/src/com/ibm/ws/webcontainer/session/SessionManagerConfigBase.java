/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session;

import java.util.EnumSet;

import javax.servlet.SessionTrackingMode;

public class SessionManagerConfigBase {

    protected boolean usingWebContainerSM = true;
    protected boolean useContextRootForSessionCookiePath=false;
    
    private static String serverLevelSessionCookieName = "JSESSIONID"; //default - overridden at server startup
    
    //Tells us if we are on zOS
    static boolean is_zOS = false;

    private static int sessionIDLength = 23;
    
    //Tells us whether SSL Tracking/url rewriting and/or cookies are enabled ... config params for each type 
    protected EnumSet<SessionTrackingMode> trackingModes = EnumSet.noneOf(SessionTrackingMode.class);
    {
        trackingModes.add(SessionTrackingMode.COOKIE);
    }
    private String privateSessionCookieName = "JSESSIONID";
    private String privateSessionCookieComment = "";
    private String privateSessionCookieDomain = null;
    private int privateSessionCookieMaxAge = -1;
    private String privateSessionCookiePath = "/";
    private boolean privateSessionCookieSecure = false;
    private boolean privateSessionCookieHttpOnly = true;
    protected SessionCookieConfigImpl cookieConfig = new SessionCookieConfigImpl(
            privateSessionCookieName,
            privateSessionCookieDomain,
            privateSessionCookiePath,
            privateSessionCookieComment,
            privateSessionCookieMaxAge,
            privateSessionCookieHttpOnly,
            privateSessionCookieSecure);
    
    protected String getDefaultSessionCookieName() {
        return privateSessionCookieName;
    }
    
    public void setEffectiveTrackingModes(EnumSet<SessionTrackingMode> effective) {
        if (effective!=null) {
            trackingModes=EnumSet.copyOf(effective);
        } else {
            trackingModes=EnumSet.noneOf(SessionTrackingMode.class);
        }
    }
    
    public EnumSet<SessionTrackingMode> getSessionTrackingMode() {
        return trackingModes;
    }
    
    public final SessionCookieConfigImpl getSessionCookieConfig() {
        return cookieConfig;
    }
    
    //usingWebContainerSM
    public final boolean isUsingWebContainerSMForBaseConfig() {
        return usingWebContainerSM;
    }
    public final void setUsingWebContainerSMForBaseConfig(boolean b) {
        usingWebContainerSM = b;
    }
    
    public final boolean isUseContextRootForSessionCookiePath() {
        return useContextRootForSessionCookiePath;
    }
    
    public final void setUseContextRootForSessionCookiePath(boolean useContextRootForSessionCookiePath) {
        this.useContextRootForSessionCookiePath = useContextRootForSessionCookiePath;
    }
    
    //sessionIDLength
    public static final int getSessionIDLength() {
        return sessionIDLength;
    }
    public static final void setSessionIDLength(int i) {
        sessionIDLength = i;
    }
    
    //is_zOS
    public static final boolean is_zOS() {
        return is_zOS;
    }
    public static final void set_is_zOS(boolean b) {
        is_zOS = b;
    }

    public static void setServerLevelSessionCookieName(
            String serverLevelSessionCookieName) {
        SessionManagerConfigBase.serverLevelSessionCookieName = serverLevelSessionCookieName;
    }

    public static String getServerLevelSessionCookieName() {
        return serverLevelSessionCookieName;
    }

}
