/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.SessionTrackingMode;

public class SessionManagerConfig implements Cloneable {

    // Tells us if we are using the Server Level Session Manager
    private boolean usingWebContainerSM = true;
    private boolean useContextRootForSessionCookiePath = false;

    // One of the next 4 boolean values will tell us what type of configuration we
    // are using
    private boolean usingMemory = false;
    private boolean usingDatabase = false;
    private boolean usingJCache = false;
    private boolean usingMemToMem = false;

    // Tells us whether SSL Tracking/url rewriting and/or cookies are enabled ...
    // config params for each type
    private boolean enableUrlProtocolSwitchRewriting = false;
    public static final String dcookieName = "SSLJSESSION";
    public static String sipSessionCookieName = "ibmappid";

    // Tells us how many sessions are stored in Memory and for in-memory
    // configuration whether overflow is enabled
    private int inMemorySize = 1000;
    private boolean enableOverflow = true;

    // Specifies the invalidation timeout interval ... -1 if turned off
    private long sessionInvalidationTime = 30;

    // Tells us whether to check security
    private boolean integrateSec = false;
    private boolean throwSecurityExceptionOnGetSessionFalse = true;
    private boolean invalidateOnUnauthorizedSessionRequestException = false;

    // Tells us whether to force serialized access
    private boolean allowSerializedSessionAccess = false;
    private int serializedSessionAccessMaxWaitTime = 0;
    private boolean accessSessionOnTimeout = true; // in tWAS, default value is false, but WCCM sets it to true

    // Tells us the configuration parameters when accessing a database
    private String JNDIDataSourceName = "jdbc/sample";
    private String sessionDBID = null;
    private String sessionDBPWD = null;
    private int rowSize = 4;
    private String tableSpaceName = null;
    private boolean usingMultirow = false;

    // Object which contains MTM settings
    private Object drsSettings;

    // Tells us when to persist information ... manually, time-based, or end of
    // service
    private boolean enableEOSWrite = false;
    private boolean enableManualWrite = false;
    private boolean enableTimeBasedWrite = false;
    private long mPropertyWriterInterval = 120;
    private boolean writeAllProperties = false;
    private boolean writeGetAndSetAttributes; 

    // Tells us if scheduledInvalidation is enabled and contains parameters
    private boolean scheduledInvalidation = false;
    private int firstHour = 0;
    private int secondHour = 0;

    // Tells us if we are on zOS
    static boolean is_zOS = false;

    // Unique server information
    static String serverId = null;
    static String cloneId = null;

    // Tells us if security is enabled (cannot be cached on lWAS)
    //    private boolean serverSecurityEnabled = false;

    // sacKey - we may have multiple SessionAffinityContext's on a request when
    // request
    // is dispatched across apps with different cookie names/paths/domains.
    private String sacKey = "DEFAULT_SAC_KEY";

    // The following properties are set via Custom Properties
    private static char cloneSeparator = ':';
    private boolean debugSessionCrossover = false;
    private static boolean cloneIdPropertySet = false;
    private static boolean expectedCloneIdsPropertySet=false; //PI18177
    private static String expectedCloneIds = null; //PI18177
    private static boolean idReuse = false; // not documented, but think people
                                            // are using this
    private long reaperPollInterval = -1;
    private static boolean turnOffCacheId = false;
    private static boolean turnOffCloneId = false;
    private static boolean noAffinitySwitchBack = false;
    private static int maxSessionIdentifierLength = 1024;
    private String sessUrlRewritePrefix = ";jsessionid=";
    private String tableNameValue = null; // LIDB2775.25 zOS
    private boolean checkRecentlyInvalidList = true; // not documented, but think
                                                     // people are using this
    private boolean modifyActiveCountOnInvalidatedSession = true; //was initialized to false for PI73188
    // User was getting a negative activeCount. This prevents WAS
    // from decrementing activeCount multiple times on invalidation.
    private boolean useOracleBlob = false;
    private boolean usingCustomSchemaName = false; // PM27191
    // PMI Cntr that isn't selectable via admin console ... but displayed when
    // PMISessionStatistics.toString() is called
    private boolean trackGCCount = false; // not documented, unsure of usage
    private static boolean hideSessionValues = true; // not documented, but think
                                                     // people are using this
                                                     // //change from V6.1
    private static boolean hideSessionValuesPropertySet = false;
    private static boolean doRemoteInvalidations = true; // not documented, but
                                                         // think people are using
                                                         // this
    private boolean zosBaseServerReplication = false; // used to be
                                                      // httpSessionEnableUnmanagedServerReplicationZOS
    private static boolean servlet21SessionCompat = false;
    // cacheIdLength is set to 0 if we are running in-memory and turnOffCacheId is
    // true
    private static int cacheIdLength = 4;
    private static int sessionIDLength = 23;

    // Tells us whether SSL Tracking/url rewriting and/or cookies are enabled
    // ... config params for each type
    protected EnumSet<SessionTrackingMode> trackingModes = EnumSet.noneOf(SessionTrackingMode.class);
    {
        trackingModes.add(SessionTrackingMode.COOKIE);
    }
    private final String privateSessionCookieName = "JSESSIONID";
    private final String privateSessionCookieComment = ""; // see CMVC defect 712310 (PM44643)
    private final String privateSessionCookieDomain = ""; // in tWAS, empty string comes from WCCM
    private final int privateSessionCookieMaxAge = -1;
    private final String privateSessionCookiePath = "/";
    private final boolean privateSessionCookieSecure = false;
    private final boolean privateSessionCookieHttpOnly = true;
    protected SessionCookieConfigImpl cookieConfig = new SessionCookieConfigImpl(privateSessionCookieName, privateSessionCookieDomain, privateSessionCookiePath,
                    privateSessionCookieComment, privateSessionCookieMaxAge, privateSessionCookieHttpOnly, privateSessionCookieSecure);

    //TODO Lierty Anat: Do we need to read those properties from metatype ??
    //JSR289 SIP converged app custom properties
    private int SIPConvergedHttpPort = 80;
    private int SIPConvergedHttpsPort = 443;
    private String SIPConvergedHostName = "localhost";
    //end of JSR289 SIP converged app custom properties
    
    protected String getDefaultSessionCookieName() {
        return privateSessionCookieName;
    }

    public final SessionCookieConfigImpl getSessionCookieConfig() {
        return cookieConfig;
    }

    private boolean optimizeCacheIdIncrements = false; // PK47847 - added in v6.1
    // new for v7
    // not compliant with spec but what < 6.1 did- only tell customers that
    // complain
    private static boolean alwaysEncodeURL = false; // not documented
    private static boolean checkSessionCookieNameOnEncodeURL=false; // PM81933
    private boolean onlyCheckInCacheDuringPreInvoke = false; // not documented
    private static boolean enableHotFailover = true; // undocumented - can set to
                                                     // false to turn off hot
                                                     // failover
    private int forceSessionInvalidationMultiple = 3; // PK38538
    private boolean persistSessionAfterPassivation = false;
    private static boolean usingApplicationSessionsAndInvalidateAll = false;
    private static boolean usingApplicationSessionsAlwaysRetrieve = false;
    private boolean forceSessionIdLengthCheck = false; // PK80439
    private boolean securityUserIgnoreCase = false; // PM04304

    // private boolean invalIfActive = false; // This is handled with the
    // forceSessionInvalidationMultiple now
    // not documented..force inval even if session active
    // This was done for PK03711 and then backed out in v7 for CTS defect 391577
    // If PK03711 issue surfaces in v7, have customer set InvalidateIfActive=true
    private boolean sessionTableSkipIndexCreation = false; // PM37139
    private boolean checkSessionNewOnIsValidRequest = true; //PM57590
    
    private int delayInvalidationAlarmDuringServerStartup = 5; //PM74718
    
    //Please keep variable set to true unless there is a crucial reason as to why the thread 
    //scheduler should be java.util.Timer
    private boolean useSeparateSessionInvalidatorThreadPool = true; //set default to true so that ScheduledExecutorService is used as thread scheduler
    
    private int connectionRetryCount = 2; // Feature 68570
    private SameSiteCookie sessionCookieSameSite;
    
    // finished Custom Properties

    // is_zOS
    public static final boolean is_zOS() {
        return is_zOS;
    }

    public static final void set_is_zOS(boolean b) {
        is_zOS = b;
    }

    public void setEffectiveTrackingModes(EnumSet<SessionTrackingMode> effective) {
        if (effective != null) {
            trackingModes = EnumSet.copyOf(effective);
        } else {
            trackingModes = EnumSet.noneOf(SessionTrackingMode.class);
        }
    }

    public EnumSet<SessionTrackingMode> getSessionTrackingMode() {
        return trackingModes;
    }

    // serverId
    public static final String getServerId() {
        return serverId;
    }

    public static final void setServerId(String s) {
        serverId = s;
    }

    // cloneId
    public static final String getCloneId() {
        return cloneId;
    }

    public static final void setCloneId(String s) {
        cloneId = s;
    }

    public final boolean isUseContextRootForSessionCookiePath() {
        return useContextRootForSessionCookiePath;
    }

    public final void setUseContextRootForSessionCookiePath(boolean useContextRootForSessionCookiePath) {
        this.useContextRootForSessionCookiePath = useContextRootForSessionCookiePath;
    }

    // constructor
    public SessionManagerConfig() {}

    // usingWebContainerSM
    public final boolean isUsingWebContainerSM() {
        return usingWebContainerSM;
    }

    public final void setUsingWebContainerSM(boolean b) {
        usingWebContainerSM = b;
    }

    // usingMemory
    public final boolean isUsingMemory() {
        return usingMemory;
    }

    public final void setUsingMemory(boolean val) {
        usingMemory = val;
    }

    // usingDatabase
    public final boolean isUsingDatabase() {
        return usingDatabase;
    }

    public final void setUsingDatabase(boolean val) {
        usingDatabase = val;
    }
    
    // using JCache
    public final boolean isUsingJCache() {
        return usingJCache;
    }

    public final void setUsingJCache(boolean val) {
        usingJCache = val;
    }

    // usingMemToMem
    public final boolean isUsingMemtoMem() {
        return usingMemToMem;
    }

    public final void setUsingMemtoMem(boolean val) {
        usingMemToMem = val;
    }

    public final void setPersistenceMode(String s) {
        if ("None".equalsIgnoreCase(s)) {
            this.setUsingMemory(true);
        } else if ("DATABASE".equalsIgnoreCase(s)) {
            this.setUsingDatabase(true);
        } else if ("JCACHE".equalsIgnoreCase(s)) {
            this.setUsingJCache(true);
        } else {
            this.setUsingMemory(true);
        }
    }

    // enableSSLTracking
    public final boolean useSSLId() {
        return trackingModes.contains(SessionTrackingMode.SSL);
    }

    public final void setUseSLLId(boolean b) {
        if (b) {
            trackingModes.add(SessionTrackingMode.SSL);
            //cookieConfig.setName(dcookieName);??????
        } else {
            trackingModes.remove(SessionTrackingMode.SSL);
        }
    }

    // enableUrlRewriting
    public final boolean getEnableUrlRewriting() {
        return trackingModes.contains(SessionTrackingMode.URL);
    }

    public final void setEnableUrlRewriting(boolean enable) {
        if (enable) {
            trackingModes.add(SessionTrackingMode.URL);
        } else {
            trackingModes.remove(SessionTrackingMode.URL);
        }
    }

    // enableUrlProtocolSwitchRewriting
    public final boolean getEnableUrlProtocolSwitchRewriting() {
        return enableUrlProtocolSwitchRewriting;
    }

    public final void setEnableUrlProtocolSwitchRewriting(boolean b) {
        enableUrlProtocolSwitchRewriting = b;
    }

    // enableCookies
    public final boolean getEnableCookies() {
        return trackingModes.contains(SessionTrackingMode.COOKIE);
    }

    public final void setEnableCookies(boolean b) {
        if (b) {
            trackingModes.add(SessionTrackingMode.COOKIE);
        } else {
            trackingModes.remove(SessionTrackingMode.COOKIE);
        }
    }

    // sessionCookieName
    public final String getSessionCookieName() {
        return cookieConfig.getName();
    }

    public final void setSessionCookieName(String s) {
        final boolean externalCall = false;
        cookieConfig.setName(s, externalCall);
    }

    // sessionCookieComment
    public final String getSessionCookieComment() {
        return cookieConfig.getComment();
    }

    public final void setSessionCookieComment(String s) {
        final boolean externalCall = false;
        cookieConfig.setComment(s, externalCall);
    }

    // sessionCookieDomain
    public final String getSessionCookieDomain() {
        return cookieConfig.getDomain();
    }

    public final void setSessionCookieDomain(String s) {
        final boolean externalCall = false;
        cookieConfig.setDomain(s, externalCall);
    }

    // sessionCookieMaxAge
    public final int getSessionCookieMaxAge() {
        return cookieConfig.getMaxAge();
    }

    public final void setSessionCookieMaxAge(int i) {
        final boolean externalCall = false;
        cookieConfig.setMaxAge(i, externalCall);
    }

    // sessionCookiePath
    public final String getSessionCookiePath() {
        return cookieConfig.getPath();
    }

    public final void setSessionCookiePath(String s) {
        final boolean externalCall = false;
        cookieConfig.setPath(s, externalCall);
    }

    // sessionCookieSecure
    public final boolean getSessionCookieSecure() {
        return cookieConfig.isSecure();
    }

    public final void setSessionCookieSecure(boolean b) {
        final boolean externalCall = false;
        cookieConfig.setSecure(b, externalCall);
    }

    // sipSessionCookieName
    public static final String getSipSessionCookieName() {
        return sipSessionCookieName;
    }

    public static final void setSipSessionCookieName(String s) {
        sipSessionCookieName = s;
    }

    // inMemorySize
    public final int getInMemorySize() {
        return inMemorySize;
    }

    public final void setInMemorySize(int i) {
        inMemorySize = i;
    }

    // enableOverflow
    public final boolean getEnableOverflow() {
        return enableOverflow;
    }

    public final void setEnableOverflow(boolean b) {
        enableOverflow = b;
    }

    // sessionInvalidationTime
    public final long getSessionInvalidationTime() {
        return sessionInvalidationTime;
    }

    public final void setSessionInvalidationTime(long t) {
        sessionInvalidationTime = t;
    }

    // integrateSec
    public final boolean getIntegrateSecurity() {
        return integrateSec;
    }

    public final void setIntegrateSecurity(boolean sec) {
        integrateSec = sec;
    }

    public final boolean getThrowSecurityExceptionOnGetSessionFalse() {
        return throwSecurityExceptionOnGetSessionFalse;
    }

    public final void setThrowSecurityExceptionOnGetSessionFalse(boolean throwEx) {
        throwSecurityExceptionOnGetSessionFalse = throwEx;
    }

    public final boolean getInvalidateOnUnauthorizedSessionRequestException() {
        return invalidateOnUnauthorizedSessionRequestException;
    }

    public final void setInvalidateOnUnauthorizedSessionRequestException(boolean invalidateOnEx) {
        invalidateOnUnauthorizedSessionRequestException = invalidateOnEx;
    }

    // allowSerializedSessionAccess
    public final boolean getAllowSerializedSessionAccess() {
        return allowSerializedSessionAccess;
    }

    public final void setAllowSerializedSessionAccess(boolean b) {
        allowSerializedSessionAccess = b;
    }

    // serializedSessionAccessMaxWaitTime
    public final int getSerializedSessionAccessMaxWaitTime() {
        return serializedSessionAccessMaxWaitTime;
    }

    public final void setSerializedSessionAccessMaxWaitTime(int i) {
        serializedSessionAccessMaxWaitTime = i;
    }

    // accessSessionOnTimeout
    public final boolean getAccessSessionOnTimeout() {
        return accessSessionOnTimeout;
    }

    public final void setAccessSessionOnTimeout(boolean b) {
        accessSessionOnTimeout = b;
    }

    //PI73188 Start
    // activeCountOnInvalidatedSession
    public final boolean getModifyActiveCountOnInvalidatedSession() {
        return modifyActiveCountOnInvalidatedSession;
    }
    
    public final void setModifyActiveCountOnInvalidatedSession(boolean b) {
        modifyActiveCountOnInvalidatedSession = b;
    }
    //PI73188 End
    
    // JNDIDataSourceName
    public final String getJNDIDataSourceName() {
        return JNDIDataSourceName;
    }

    public final void setJNDIDataSourceName(String name) {
        JNDIDataSourceName = name;
    }

    // sessionDBID
    public final String getSessionDBID() {
        return sessionDBID;
    }

    public final void setSessionDBID(String s) {
        sessionDBID = s;
    }

    // sessionDBPWD
    public final String getSessionDBPWD() {
        return sessionDBPWD;
    }

    public final void setSessionDBPWD(String s) {
        sessionDBPWD = s;
    }

    // rowSize
    public final int getRowSize() {
        return rowSize;
    }

    public final void setRowSize(int size) {
        rowSize = size;
    }

    // tableSpaceName
    public final String getTableSpaceName() {
        return tableSpaceName;
    }

    public final void setTableSpaceName(String name) {
        tableSpaceName = name;
    }

    // usingMultirow
    public final boolean isUsingMultirow() {
        return usingMultirow;
    }

    public final void setUsingMultirow(boolean b) {
        usingMultirow = b;
    }

    // drsSettings
    public final Object getDRSSettings() {
        return drsSettings;
    }

    public final void setDRSSettings(Object drss) {
        drsSettings = drss;
    }

    // enableEOSWrite
    public final boolean getEnableEOSWrite() {
        return enableEOSWrite;
    }

    public final void setEnableEOSWrite(boolean b) {
        enableEOSWrite = b;
    }

    // enableManualWrite
    public final boolean getEnableManualWrite() {
        return enableManualWrite;
    }

    public final void setEnableManualWrite(boolean b) {
        enableManualWrite = b;
    }

    // enableTimeBasedWrite
    public final boolean getEnableTimeBasedWrite() {
        return enableTimeBasedWrite;
    }

    public final void setEnableTimeBasedWrite(boolean b) {
        enableTimeBasedWrite = b;
    }

    // mPropertyWriterInterval
    public final long getPropertyWriterInterval() {
        return mPropertyWriterInterval;
    }

    public final void setPropertyWriterInterval(long time) {
        mPropertyWriterInterval = time;
    }

    // writeAllProperties
    public final boolean writeAllProperties() {
        return writeAllProperties;
    }

    public final void setwriteAllProperties() {
        writeAllProperties = true;
    }

    /**
     * @returns true if writeContents is configured to GET_AND_SET_ATTRIBUTES
     */
    public final boolean writeGetAndSetAttributes() {
        return writeGetAndSetAttributes;
    }

    final void setWriteGetAndSetAttributes() {
        writeGetAndSetAttributes = true;
    }

    // scheduledInvalidation
    public final boolean getScheduledInvalidation() {
        return scheduledInvalidation;
    }

    public final void setScheduledInvalidation(boolean b) {
        scheduledInvalidation = b;
    }

    // firstHour
    public final int getInvalTime1() {
        return firstHour;
    }

    public final void setFirstHour(int time) {
        firstHour = time;
    }

    // secondHour
    public final int getInvalTime2() {
        return secondHour;
    }

    public final void setSecondHour(int time) {
        secondHour = time;
    }

    // serverSecurityEnabled (cannot be cached on lWAS)
    //    public final boolean getServerSecurityEnabled() {
    //        return serverSecurityEnabled;
    //    }
    //
    //    public final void setServerSecurityEnabled(boolean b) {
    //        serverSecurityEnabled = b;
    //    }

    // SessionAffinityContextKey
    public final void setSessionAffinityContextKey(String key) {
        sacKey = key;
    }

    public final String getSessionAffinityContextKey() {
        return sacKey;
    }

    /*
     * 
     * 
     * CUSTOM PROPERTIES
     */

    // cloneSeparator
    public static final char getCloneSeparator() {
        return cloneSeparator;
    }

    public static final void setCloneSeparator(char c) {
        cloneSeparator = c;
    }

    // debugSessionCrossover
    public final boolean isDebugSessionCrossover() {
        return debugSessionCrossover;
    }

    public final void setDebugSessionCrossover(boolean b) {
        debugSessionCrossover = b;
    }

    // cloneIdPropertySet
    public static final boolean isCloneIdPropertySet() {
        return cloneIdPropertySet;
    }

    public static final void setCloneIdPropertySet(boolean b) {
        cloneIdPropertySet = b;
    }

    // idReuse
    public static final boolean isIdReuse() {
        return idReuse;
    }

    public static final void setIdReuse(boolean b) {
        idReuse = b;
    }

    // reaperPollInterval
    public final long getInvalidationCheckInterval() {
        return reaperPollInterval;
    }

    public final long getReaperPollInterval() {
        return reaperPollInterval;
    }

    public final void setReaperPollInterval(long l) {
        reaperPollInterval = l;
    }

    // turnOffCacheId
    public static final boolean isTurnOffCacheId() {
        return turnOffCacheId;
    }

    public static final void setTurnOffCacheId(boolean b) {
        turnOffCacheId = b;
    }

    // turnOffCloneId
    public static final boolean isTurnOffCloneId() {
        return turnOffCloneId;
    }

    public static final void setTurnOffCloneId(boolean b) {
        turnOffCloneId = b;
    }

    // noAffinitySwitchBack
    public static final boolean isNoAffinitySwitchBack() {
        return noAffinitySwitchBack;
    }

    public static final void setNoAffinitySwitchBack(boolean b) {
        noAffinitySwitchBack = b;
    }

    // maxSessionIdentifierLength
    public static final int getMaxSessionIdentifierLength() {
        return maxSessionIdentifierLength;
    }

    public static final void setMaxSessionIdentifierLength(int i) {
        maxSessionIdentifierLength = i;
    }

    // sessUrlRewritePrefix
    public final String getSessUrlRewritePrefix() {
        return sessUrlRewritePrefix;
    }

    public final void setSessUrlRewritePrefix(String s) {
        sessUrlRewritePrefix = s;
    }

    /* Get the DB2 Table name MD15761 */
    public final String getTableNameValue() {
        return tableNameValue;
    }

    public final void setTableNameValue(String s) {
        tableNameValue = s;
    }

    // checkRecentlyInvalidList
    public final boolean getCheckRecentlyInvalidList() {
        return checkRecentlyInvalidList;
    }

    public final void setCheckRecentlyInvalidList(boolean b) {
        checkRecentlyInvalidList = b;
    }

    // useOracleBlob
    public final boolean isUseOracleBlob() {
        return useOracleBlob;
    }

    public final void setUseOracleBlob(boolean b) {
        useOracleBlob = b;
    }

    // START: PM37139
    public final boolean isSessionTableSkipIndexCreation() {
        return sessionTableSkipIndexCreation;
    }

    public final void setSessionTableSkipIndexCreation(boolean b) {
        sessionTableSkipIndexCreation = b;
    }

    // END: PM37139

    // START: PM57590
    public final boolean checkSessionNewOnIsValidRequest() {
        return checkSessionNewOnIsValidRequest;
    }

    public final void setCheckSessionNewOnIsValidRequest(boolean b) {
        checkSessionNewOnIsValidRequest = b;
    }

    // END: PM57590    

    //useCurrentSchemaCustomProperty
    public final boolean isUsingCustomSchemaName() {
        return usingCustomSchemaName;
    }

    public final void setUsingCustomSchemaName(boolean b) {
        usingCustomSchemaName = b;
    }

    // trackGCCount
    public final boolean isTrackGCCount() {
        return trackGCCount;
    }

    public final void setTrackGCCount(boolean b) {
        trackGCCount = b;
    }

    // hideSessionValues
    public static final boolean isHideSessionValues() {
        return hideSessionValues;
    }

    public static final void setHideSessionValues(boolean b) {
        hideSessionValues = b;
    }

    // hideSessionValuesPropertySet
    public static final boolean isHideSessionValuesPropertySet() {
        return hideSessionValuesPropertySet;
    }

    public static final void setHideSessionValuesPropertySet(boolean b) {
        hideSessionValuesPropertySet = b;
    }

    // doRemoteInvalidations
    public static final boolean isDoRemoteInvalidations() {
        return doRemoteInvalidations;
    }

    public static final void setDoRemoteInvalidations(boolean b) {
        doRemoteInvalidations = b;
    }

    // zosBaseServerReplication
    public final boolean isZosBaseServerReplicationEnabled() {
        return zosBaseServerReplication;
    }

    public final void setZosBaseServerReplicationEnabled(boolean b) {
        zosBaseServerReplication = b;
    }

    // servlet21SessionCompat
    public static final boolean getServlet21SessionCompatibility() {
        return servlet21SessionCompat;
    }

    public static final void setServlet21SessionCompatibility(boolean b) {
        servlet21SessionCompat = b;
    }

    // cacheIdLength
    public static final int getCacheIdLength() {
        return cacheIdLength;
    }

    public static final void setCacheIdLength(int i) {
        cacheIdLength = i;
    }

    // sessionIDLength
    public static final int getSessionIDLength() {
        return sessionIDLength;
    }

    public static final void setSessionIDLength(int i) {
        sessionIDLength = i;
    }

    // alwaysEncodeURL
    public static final boolean isAlwaysEncodeURL() {
        return alwaysEncodeURL;
    }

    public static final void setAlwaysEncodeURL(boolean b) {
        alwaysEncodeURL = b;
    }
    
    //checkSessionCookieNameOnEncodeURL
    public static final boolean checkSessionCookieNameOnEncodeURL() {
        return checkSessionCookieNameOnEncodeURL;
    }
    
    public static final void setCheckSessionCookieNameOnEncodeURL(boolean b) {
        checkSessionCookieNameOnEncodeURL = b;
    }    

    public final boolean getOptimizeCacheIdIncrements() {
        return optimizeCacheIdIncrements;
    }

    public final void setOptimizeCacheIdIncrements(boolean b) {
        optimizeCacheIdIncrements = b;
    }

    // onlyCheckInCacheDuringPreInvoke
    public final boolean getOnlyCheckInCacheDuringPreInvoke() {
        return onlyCheckInCacheDuringPreInvoke;
    }

    public final void setOnlyCheckInCacheDuringPreInvoke(boolean b) {
        onlyCheckInCacheDuringPreInvoke = b;
    }

    // enableHotFailover
    public static final boolean isEnableHotFailover() {
        return enableHotFailover;
    }

    public static final void setEnableHotFailover(boolean b) {
        enableHotFailover = b;
    }

    public int getForceSessionInvalidationMultiple() {
        return forceSessionInvalidationMultiple;
    }

    public void setForceSessionInvalidationMultiple(int i) {
        forceSessionInvalidationMultiple = i;
    }

    public boolean getPersistSessionAfterPassivation() {
        return persistSessionAfterPassivation;
    }

    public void setPersistSessionAfterPassivation(boolean b) {
        persistSessionAfterPassivation = b;
    }

    public static boolean getUsingApplicationSessionsAndInvalidateAll() {
        return usingApplicationSessionsAndInvalidateAll;
    }

    public static void setUsingApplicationSessionsAndInvalidateAll(boolean b) {
        usingApplicationSessionsAndInvalidateAll = b;
    }

    public static boolean getUsingApplicationSessionsAlwaysRetrieve() {
        return usingApplicationSessionsAlwaysRetrieve;
    }

    public static void setUsingApplicationSessionsAlwaysRetrieve(boolean b) {
        usingApplicationSessionsAlwaysRetrieve = b;
    }

    public boolean getForceSessionIdLengthCheck() {
        return forceSessionIdLengthCheck;
    }

    public void setForceSessionIdLengthCheck(boolean b) {
        forceSessionIdLengthCheck = b;
    }

    // PM04304: Add method for custom property SecurityUserIgnoreCase
    public boolean getSecurityUserIgnoreCase() {
        return securityUserIgnoreCase;
    }

    public void setSecurityUserIgnoreCase(boolean b) {
        securityUserIgnoreCase = b;
    }
    
    //PM63475
    public int getDelayForInvalidationAlarmDuringServerStartup() {
        return delayInvalidationAlarmDuringServerStartup;
    }
    
    public void setDelayForInvalidationAlarmDuringServerStartup(int delay) { 
        delayInvalidationAlarmDuringServerStartup = delay;
    }
    
    //Get and set methods for scheduler change session property
    public boolean getUseSeparateSessionInvalidatorThreadPool(){
        return useSeparateSessionInvalidatorThreadPool;
    }
    
    public void setUseSeparateSessionInvalidatorThreadPool(boolean switched){
        useSeparateSessionInvalidatorThreadPool = switched;
    }
    
    //PI18177
    public static final void setExpectedCloneIdsPropertySet(boolean b) {
        expectedCloneIdsPropertySet = b;
    }
    public static final boolean isExpectedCloneIdsPropertySet() {
        return expectedCloneIdsPropertySet;
    }
    public static final void setExpectedCloneIds(String cloneIds) {
        expectedCloneIds = cloneIds;
    }
    public static final String getExpectedCloneIds() {
        return expectedCloneIds;
    }
    
    public int getConnectionRetryCount() {
        return connectionRetryCount;
    }

    public void setConnectionRetryCount(int i) {
        connectionRetryCount = i;
    }
    
    public void printSessionManagerConfigForDebug(Logger logger) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            StringBuffer msg = new StringBuffer();
            msg.append("usingWebContainerSM=").append(usingWebContainerSM).append("\n");
            msg.append("usingMemory=").append(usingMemory).append("\n");
            msg.append("usingDatabase=").append(usingDatabase).append("\n");
            msg.append("usingJCache=").append(usingJCache).append("\n");
            msg.append("usingMemToMem=").append(usingMemToMem).append("\n");
            msg.append("enableSSLTracking=").append(this.useSSLId()).append("\n");
            msg.append("enableUrlRewriting=").append(this.getEnableUrlRewriting()).append("\n");
            msg.append("enableUrlProtocolSwitchRewriting=").append(enableUrlProtocolSwitchRewriting).append("\n");
            msg.append("enableCookies=").append(this.getEnableCookies()).append("\n");
            msg.append("sessionCookieName=").append(this.getSessionCookieName()).append("\n");
            msg.append("sessionCookieDomain=").append(this.getSessionCookieDomain()).append("\n");
            msg.append("sessionCookieMaxAge=").append(this.getSessionCookieMaxAge()).append("\n");
            msg.append("sessionCookiePath=").append(this.getSessionCookiePath()).append("\n");
            msg.append("sessionCookieSecure=").append(this.getSessionCookieSecure()).append("\n");
            msg.append("sessionCookieSameSite=").append(this.getSessionCookieSameSite().getSameSiteCookieValue()).append("\n");
            msg.append("sessionCookieHttpOnly=").append(this.getSessionCookieHttpOnly()).append("\n");
            msg.append("inMemorySize=").append(inMemorySize).append("\n");
            msg.append("enableOverflow=").append(enableOverflow).append("\n");
            msg.append("sessionInvalidationTime=").append(sessionInvalidationTime).append("\n");
            msg.append("integrateSec=").append(integrateSec).append("\n");
            msg.append("is_zOS=").append(is_zOS).append("\n");
            msg.append("cloneID=").append(cloneId).append("\n");
            msg.append("cloneSeparator=").append(cloneSeparator).append("\n");
            logger.logp(Level.FINE, "SessionManagerConfig", "printSessionManagerConfigForDebug", msg.toString());
        }
    }

    @Override
    public SessionManagerConfig clone() throws CloneNotSupportedException {
        SessionManagerConfig tempSMC = (SessionManagerConfig) super.clone();
        tempSMC.setClonedCookieConfig(this.cookieConfig.clone());
        tempSMC.setClonedTrackingModes(this.trackingModes);
        //I don't think we need to clone the drsSettings since this can 
        //not be set by the application and if it has been configured differently
        //then you already have a different SMC object
        //if (this.drsSettings!=null) {
        //    tempSMC.setDRSSettings(this.drsSettings.clone());
        //}
        return tempSMC;
    }

    public void setClonedCookieConfig(SessionCookieConfigImpl scci) {
        this.cookieConfig = scci;
    }

    public void setClonedTrackingModes(EnumSet<SessionTrackingMode> stm) {
        if (stm != null) {
            this.trackingModes = EnumSet.copyOf(stm);
        }
    }

    //sessionCookieHttpOnly
    public final boolean getSessionCookieHttpOnly() {
        return cookieConfig.isHttpOnly();
    }

    public final void setSessionCookieHttpOnly(boolean b) {
        final boolean externalCall = false;
        cookieConfig.setHttpOnly(b, externalCall);
    }
    
    //sessionSameSite
    public final SameSiteCookie getSessionCookieSameSite() {
        return this.sessionCookieSameSite;
    }
    
    public final void setSessionCookieSameSite(SameSiteCookie sameSite) {
       this.sessionCookieSameSite = sameSite;
    }
    

    public void updateCookieInfo(SessionCookieConfigImpl scc) {
        if (scc != null) {
            if (scc.getComment() != null) {
                this.setSessionCookieComment(scc.getComment());
            }
            if (scc.getDomain() != null) {
                this.setSessionCookieDomain(scc.getDomain());
            }
            if (scc.isMaxAgeSet()) {
                this.setSessionCookieMaxAge(scc.getMaxAge());
            }
            if (scc.getName() != null) {
                this.setSessionCookieName(scc.getName());
            }
            if (scc.getPath() != null) {
                this.setSessionCookiePath(scc.getPath());
            }
            if (scc.isHttpOnlySet()) {
                this.setSessionCookieHttpOnly(scc.isHttpOnly());
            }
            if (scc.isSecureSet()) {
                this.setSessionCookieSecure(scc.isSecure());
            }
        }
    }

    public void updateTrackingMode(EnumSet<SessionTrackingMode> modes) {
        //no need to do a copy as this is only called from createCoreSessionManager
        this.trackingModes = modes;
    }

  //JSR289 SIP converged app custom properties
    public String getSIPConvergedHostName() {
        return SIPConvergedHostName;
    }
    public void setSIPConvergedHostName(String convergedHostName) {
        SIPConvergedHostName = convergedHostName;
    }
    public int getSIPConvergedHttpPort() {
        return SIPConvergedHttpPort;
    }
    public void setSIPConvergedHttpPort(int convergedHttpPort) {
        SIPConvergedHttpPort = convergedHttpPort;
    }
    public int getSIPConvergedHttpsPort() {
        return SIPConvergedHttpsPort;
    }
    public void setSIPConvergedHttpsPort(int convergedHttpsPort) {
        SIPConvergedHttpsPort = convergedHttpsPort;
    }
    //end of JSR289 SIP converged app custom properties
}
