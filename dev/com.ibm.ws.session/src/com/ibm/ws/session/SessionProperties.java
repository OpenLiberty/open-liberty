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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.ibm.ws.session.utils.LoggingUtil;

final public class SessionProperties {

    private static final String methodClassName = "SessionProperties";

    private static final HashMap<String, String> FullyQualifiedPropertiesMap = new HashMap<String, String>();
    static {
        FullyQualifiedPropertiesMap.put("HttpSessionIdReuse", "idReuse");
        FullyQualifiedPropertiesMap.put("HttpSessionTrackGC", "trackGarbageCollection"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("HttpSessionReaperPollInterval", "reaperPollInterval");
        FullyQualifiedPropertiesMap.put("UseInvalidatedId", "useInvalidatedId");
        FullyQualifiedPropertiesMap.put("DebugSessionCrossover", "debugCrossover");
        FullyQualifiedPropertiesMap.put("SessionRewriteIdentifier", "rewriteId");
        FullyQualifiedPropertiesMap.put("HttpSessionCloneId", "cloneId");
        FullyQualifiedPropertiesMap.put("NoAdditionalSessionInfo", "noAdditionalInfo");
        FullyQualifiedPropertiesMap.put("SessionIdentifierMaxLength", "idMaxLength");
        FullyQualifiedPropertiesMap.put("CloneSeparatorChange", "cloneSeparatorChange");
        FullyQualifiedPropertiesMap.put("CloneSeparator", "cloneSeparator");
        FullyQualifiedPropertiesMap.put("NoAffinitySwitchBack", "noAffinitySwitchBack");
        FullyQualifiedPropertiesMap.put("UseOracleBLOB", "useOracleBlob");
        FullyQualifiedPropertiesMap.put("SessionTableSkipIndexCreation", "skipIndexCreation");
        FullyQualifiedPropertiesMap.put("CheckSessionNewOnIsValidRequest", "checkSessionNewOnIsValidRequest"); // not in metatype.xml yet (may want to add next fix pack)
        FullyQualifiedPropertiesMap.put("UsingCustomSchemaName", "usingCustomSchemaName");
        FullyQualifiedPropertiesMap.put("HideSessionValues", "hideSessionValues"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("DoRemoteInvalidations", "doRemoteInvalidations"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("SessionTableName", "tableName");
        FullyQualifiedPropertiesMap.put("HttpSessionEnableUnmanagedServerReplication", "unmanagedServerReplication"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("Servlet21SessionCompatibility", "servlet21SessionCompatibility"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("AlwaysEncodeURL", "alwaysEncodeUrl");
        FullyQualifiedPropertiesMap.put("CheckSessionCookieNameOnEncodeURL", "checkSessionCookieNameOnEncodeURL");
        FullyQualifiedPropertiesMap.put("OptimizeCacheIdIncrements", "optimizeCacheIdIncrements");
        FullyQualifiedPropertiesMap.put("OnlyCheckInCacheDuringPreInvoke", "onlyCheckInCacheDuringPreInvoke");
        FullyQualifiedPropertiesMap.put("EnableHotFailover", "hotFailover"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("ForceSessionInvalidationMultiple", "forceInvalidationMultiple");
        FullyQualifiedPropertiesMap.put("PersistSessionAfterPassivation", "persistSessionAfterPassivation"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("UsingApplicationSessionsAndInvalidateAll", "usingApplicationSessionsAndInvalidateAll"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("UsingApplicationSessionsAlwaysRetrieve", "usingApplicationSessionsAlwaysRetrieve"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("SIPCookieName", "sipCookieName"); // not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("DelayInvalidationAlarmDuringServerStartup", "delayInvalidationAlarmDuringServerStartup"); //PM74718 not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("UseSeparateSessionInvalidatorThreadPool", "useSeparateSessionInvalidatorThreadPool"); //not in metatype.xml yet
        FullyQualifiedPropertiesMap.put("ForceSessionIdLengthCheck", "forceIdLengthCheck");
        FullyQualifiedPropertiesMap.put("SecurityUserIgnoreCase", "securityUserIgnoreCase");
        FullyQualifiedPropertiesMap.put("ThrowSecurityExceptionOnGetSessionFalse", "throwSecurityExceptionOnGetSessionFalse"); // not in metatype.xml yet (may want to add next fix pack)
        FullyQualifiedPropertiesMap.put("InvalidateOnUnauthorizedSessionRequestException", "invalidateOnUnauthorizedSessionRequestException");
        FullyQualifiedPropertiesMap.put("ExpectedCloneIds", "expectedCloneIds"); //PI18177
        FullyQualifiedPropertiesMap.put("ConnectionRetryCount ","connectionRetryCount"); //Feature 68570
    }

    private SessionProperties() {}

    // static public void setPropertiesInSMC(SessionManagerConfig smc, Properties
    // wcProps, Properties smProps) {
    static public void setPropertiesInSMC(SessionManagerConfig smc, Map<String, Object> xtpProperties) {
        final String methodName = "setPropertiesInSMC";
        final String invalidPropFoundMessage = "SessionContext.invalidPropertyFound";
        final String invalidCloneSeparatorMessage = "SessionContext.invalidCloneSeparator";
        String strProp = null;
        Boolean booleanProp;
        Integer integerProp = null;

        boolean baseServerLevelConfig = smc.isUsingWebContainerSM();

        setDefaultConfiguration(smc, xtpProperties);

        // id length - has upper case D for system property...maintain for
        // compatibility
        final String propIdLengthForSys = "HttpSessionIDLength";
        final String propIdLength = "idLength";
        int idLength = -1;

        Object propIdLengthValue = xtpProperties.get(propIdLength);
        if (propIdLengthValue instanceof Integer) {
            integerProp = (Integer) propIdLengthValue;
        } else {
            strProp = (String) propIdLengthValue;
            if (strProp == null) {
                strProp = getSystemProperty(propIdLength);
                // look for different case on system property
                if (strProp == null) {
                    strProp = getSystemProperty(propIdLengthForSys);
                }
            }
        }

        if (integerProp != null || strProp != null) {
            try {
                if (integerProp != null) {
                    idLength = integerProp.intValue();
                } else {
                    idLength = Integer.parseInt(strProp);
                }
                if ((idLength < 8) || (idLength > 128)) {
                    if (idLength < 8)
                        idLength = 8;
                    if (idLength > 128)
                        idLength = 128;
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, "SessionContext.valueOutOfRange", new Object[] { propIdLength, strProp,
                                                                                                                                                     new Integer(idLength) });
                }
                if (shouldSetAndDoLogging(propIdLength, true, baseServerLevelConfig, xtpProperties, new Integer(idLength), new Integer(SessionManagerConfig.getSessionIDLength()),
                                          false)) {
                    SessionManagerConfig.setSessionIDLength(idLength);
                }
            } catch (NumberFormatException nfe) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, invalidPropFoundMessage, new Object[] { propIdLength, strProp });
            }
        }

        // id reuse - reuse incoming id even if its not in use by another webapp
        final String propIdReuse = "HttpSessionIdReuse";
        strProp = getStringProperty(propIdReuse, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propIdReuse, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.isIdReuse()), false)) {
                SessionManagerConfig.setIdReuse(booleanProp.booleanValue());
            }
        }

        // track gc
        final String propTrackGC = "HttpSessionTrackGC";
        strProp = getStringProperty(propTrackGC, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propTrackGC, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setTrackGCCount(booleanProp.booleanValue());
            }
        }

        // reaper poll interval - override how often inval thread runs
        final String propReaperPollInt = "HttpSessionReaperPollInterval";
        strProp = getStringProperty(propReaperPollInt, xtpProperties);
        long longProp = -1;
        try {
            if (strProp != null) {
                if (shouldSetAndDoLogging(propReaperPollInt, false, baseServerLevelConfig, xtpProperties, strProp, null, false)) {
                    longProp = new Long(strProp).longValue();
                    smc.setReaperPollInterval(longProp);
                }
            }
        } catch (NumberFormatException nfe) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodName, invalidPropFoundMessage, new Object[] { propReaperPollInt, strProp });
        }

        // use invalidated id - default to true for better performance
        final String propUseRecentInvalId = "UseInvalidatedId";
        strProp = getStringProperty(propUseRecentInvalId, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propUseRecentInvalId, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setCheckRecentlyInvalidList(booleanProp.booleanValue());
            }
        }

        // debug session crossover
        final String propDebugSessCross = "DebugSessionCrossover";
        strProp = getStringProperty(propDebugSessCross, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propDebugSessCross, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setDebugSessionCrossover(booleanProp.booleanValue());
                if (baseServerLevelConfig || xtpProperties.get(propDebugSessCross) != null) {
                    if (booleanProp.booleanValue() == true) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, methodName, "SessionContext.DebugCrossoverEnabled");
                    }
                }
            }
        }

        // url rewrite identifier
        final String propSessRewrite = "SessionRewriteIdentifier";
        strProp = getStringProperty(propSessRewrite, xtpProperties);
        if (strProp != null) {
            if (shouldSetAndDoLogging(propSessRewrite, false, baseServerLevelConfig, xtpProperties, strProp, null, false)) {
                smc.setSessUrlRewritePrefix(";" + strProp + "=");
            }
        }

        // clone id
        final String propHttpSessionCloneId = "HttpSessionCloneId";
        strProp = getStringProperty(propHttpSessionCloneId, xtpProperties);
        if (strProp != null) {
            if (shouldSetAndDoLogging(propHttpSessionCloneId, true, baseServerLevelConfig, xtpProperties, strProp, SessionManagerConfig.getCloneId(),
                                      strProp.equals(SessionManagerConfig.getCloneId()))) {
                SessionManagerConfig.setCloneId(strProp);
                SessionManagerConfig.setCloneIdPropertySet(true);
            }
        }

        // no additional session info - eliminates cache and clone on base server
        final String propNoAddSessInfo = "NoAdditionalSessionInfo";
        strProp = getStringProperty(propNoAddSessInfo, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propNoAddSessInfo, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.isTurnOffCacheId()), false)) {
                SessionManagerConfig.setTurnOffCacheId(booleanProp.booleanValue());
                SessionManagerConfig.setTurnOffCloneId(booleanProp.booleanValue());
                if (smc.isUsingMemory()) {
                    SessionManagerConfig.setCacheIdLength(0);
                }
            }
        }

        // session id max length (including all clones)
        final String propSessIdMax = "SessionIdentifierMaxLength";
        strProp = getStringProperty(propSessIdMax, xtpProperties);
        if (strProp != null) {
            try {
                int maxLength = Integer.parseInt(strProp);
                if (shouldSetAndDoLogging(propSessIdMax, true, baseServerLevelConfig, xtpProperties, new Integer(maxLength),
                                          new Integer(SessionManagerConfig.getMaxSessionIdentifierLength()), false)) {
                    SessionManagerConfig.setMaxSessionIdentifierLength(maxLength);
                }
            } catch (NumberFormatException nfe) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, invalidPropFoundMessage, new Object[] { propSessIdMax, strProp });
            }
        }

        // clone separator change
        final String propCloneSepChange = "CloneSeparatorChange";
        strProp = getStringProperty(propCloneSepChange, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propCloneSepChange, true, baseServerLevelConfig, xtpProperties, booleanProp,
                                      Boolean.valueOf('+' == SessionManagerConfig.getCloneSeparator()),
                                      false)) {
                if (booleanProp.booleanValue()) {
                    SessionManagerConfig.setCloneSeparator('+');
                }
            }
        }

        // PM02068 Property for changing CloneSeparator.  
        final String propCloneSeparator = "CloneSeparator";
        strProp = getStringProperty(propCloneSeparator, xtpProperties);
        if (strProp != null) {
            // Must be exactly one char, and cannot be space
            if ((strProp.length() == 1) && (strProp.trim().length() == 1)) {
                if (shouldSetAndDoLogging(propCloneSeparator, true, baseServerLevelConfig, xtpProperties, strProp, null, false)) {
                    char charProp = strProp.charAt(0);
                    SessionManagerConfig.setCloneSeparator(charProp);
                }
            } else {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, invalidCloneSeparatorMessage,
                                                     new Object[] { strProp });
            }
        }

        // no affinity switch back - stick to new server after failover
        final String propNoAffSwitch = "NoAffinitySwitchBack";
        strProp = getStringProperty(propNoAffSwitch, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propNoAffSwitch, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.isNoAffinitySwitchBack()),
                                      false)) {
                SessionManagerConfig.setNoAffinitySwitchBack(booleanProp.booleanValue());
            }
        }

        // use oracle BLOB, rather than long raw, for medium db column
        final String propUseOracleBlob = "UseOracleBLOB";
        strProp = getStringProperty(propUseOracleBlob, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propUseOracleBlob, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setUseOracleBlob(booleanProp.booleanValue());
            }
        }

        // START: PM37139 Users who set this property to true should have already created their own index, so we skip index creation
        final String propSessionTableSkipIndexCreation = "SessionTableSkipIndexCreation";
        strProp = getStringProperty(propSessionTableSkipIndexCreation, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propSessionTableSkipIndexCreation, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setSessionTableSkipIndexCreation(booleanProp.booleanValue());
            }
        }
        // END: PM37139

        // START: PM57590
        final String propCheckSessionNewOnIsValidRequest = "CheckSessionNewOnIsValidRequest";
        strProp = getStringProperty(propCheckSessionNewOnIsValidRequest, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propCheckSessionNewOnIsValidRequest, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setCheckSessionNewOnIsValidRequest(booleanProp.booleanValue());
            }
        }
        // END: PM57590     

        // if the data source has a non default schema name, then we need to mark it as so
        final String propUsingCurrentSchemaCustomProperty = "UsingCustomSchemaName";
        strProp = getStringProperty(propUsingCurrentSchemaCustomProperty, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propUsingCurrentSchemaCustomProperty, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setUsingCustomSchemaName(booleanProp.booleanValue());
            }
        }

        // hide session values (in trace output)
        final String propHideSessVals = "HideSessionValues";
        strProp = getStringProperty(propHideSessVals, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propHideSessVals, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.isHideSessionValues()), false)) {
                SessionManagerConfig.setHideSessionValues(booleanProp.booleanValue());
                SessionManagerConfig.setHideSessionValuesPropertySet(true);
            }
        }

        // do remote invalidations - forces inval even if scheduled session inval
        // (twice a day) is enabled
        final String propDoRemoteInval = "DoRemoteInvalidations";
        strProp = getStringProperty(propDoRemoteInval, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propDoRemoteInval, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.isDoRemoteInvalidations()),
                                      false)) {
                SessionManagerConfig.setDoRemoteInvalidations(booleanProp.booleanValue());
            }
        }
        // session table name
        final String propSessTable = "SessionTableName";
        strProp = getStringProperty(propSessTable, xtpProperties);
        if (strProp != null) {
            if (shouldSetAndDoLogging(propSessTable, false, baseServerLevelConfig, xtpProperties, strProp, null, false)) {
                smc.setTableNameValue(strProp);
            }
        }

        // zOS Base Server Replication
        final String propZosBaseServerReplication = "HttpSessionEnableUnmanagedServerReplication";
        strProp = getStringProperty(propZosBaseServerReplication, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propZosBaseServerReplication, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setZosBaseServerReplicationEnabled(booleanProp.booleanValue());
            }
        }

        // servlet 21 compatibility -- aka global sessions
        final String propServlet21SessionCompat = "Servlet21SessionCompatibility";
        strProp = getStringProperty(propServlet21SessionCompat, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propServlet21SessionCompat, true, baseServerLevelConfig, xtpProperties, booleanProp,
                                      Boolean.valueOf(SessionManagerConfig.getServlet21SessionCompatibility()), false)) {
                SessionManagerConfig.setServlet21SessionCompatibility(booleanProp.booleanValue());
            }
        }

        // always encode the url even if cookies enabled is set
        final String propAlwaysEncode = "AlwaysEncodeURL";
        strProp = getStringProperty(propAlwaysEncode, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propAlwaysEncode, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.isAlwaysEncodeURL()), false)) {
                SessionManagerConfig.setAlwaysEncodeURL(booleanProp.booleanValue());
            }
        }
        
        // START: PM81933 Check session cookie name on EncodeURL method
        final String propCheckSessionCookieNameOnEncodeURL = "CheckSessionCookieNameOnEncodeURL";
        strProp = getStringProperty(propCheckSessionCookieNameOnEncodeURL, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propCheckSessionCookieNameOnEncodeURL, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.checkSessionCookieNameOnEncodeURL()), false)) {
                SessionManagerConfig.setCheckSessionCookieNameOnEncodeURL(booleanProp.booleanValue());
            }
        }
        // END: PM81933        

        // Optimizes the Version Increments if the in-memory session is the same as
        // the one stored on the backend. This property only applies to non-EOS
        // writes since we will always do this for EOS.
        final String propOptimizeCacheIdIncrements = "OptimizeCacheIdIncrements";
        strProp = getStringProperty(propOptimizeCacheIdIncrements, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propOptimizeCacheIdIncrements, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setOptimizeCacheIdIncrements(booleanProp.booleanValue());
            }
        }

        // only checks the cache vs the backend for updating the lastAccessedTime
        // during the preInvoke metho
        final String propOnlyCheckInCacheDuringPreInvoke = "OnlyCheckInCacheDuringPreInvoke";
        strProp = getStringProperty(propOnlyCheckInCacheDuringPreInvoke, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propOnlyCheckInCacheDuringPreInvoke, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setOnlyCheckInCacheDuringPreInvoke(booleanProp.booleanValue());
            }
        }

        // can be used to disable hot failover by setting to false - default is true
        final String propEnableHotFailover = "EnableHotFailover";
        strProp = getStringProperty(propEnableHotFailover, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propEnableHotFailover, true, baseServerLevelConfig, xtpProperties, booleanProp, Boolean.valueOf(SessionManagerConfig.isEnableHotFailover()),
                                      false)) {
                SessionManagerConfig.setEnableHotFailover(booleanProp.booleanValue());
            }
        }

        // PK38538 Begin
        final String propForceSessionInvalidationMultiple = "ForceSessionInvalidationMultiple";
        strProp = getStringProperty(propForceSessionInvalidationMultiple, xtpProperties);
        if (strProp != null) {
            try {
                int invalMult = new Integer(strProp).intValue();
                if (shouldSetAndDoLogging(propForceSessionInvalidationMultiple, false, baseServerLevelConfig, xtpProperties, new Integer(invalMult), null, false)) {
                    smc.setForceSessionInvalidationMultiple(invalMult);
                }
            } catch (NumberFormatException nfe) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, invalidPropFoundMessage, new Object[] { propForceSessionInvalidationMultiple,
                                                                                                                                        strProp });
            }
        } // PK38538 End
        
        //PI73188 Start
        final String propModifyActiveCountOnInvalidatedSession = "ModifyActiveCountOnInvalidatedSession";
        strProp = getStringProperty(propModifyActiveCountOnInvalidatedSession, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propModifyActiveCountOnInvalidatedSession, true, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setModifyActiveCountOnInvalidatedSession(booleanProp.booleanValue());
            }
        }
        //PI73188 End
        
        
        final String propPersistSessionAfterPassivation = "PersistSessionAfterPassivation";
        strProp = getStringProperty(propPersistSessionAfterPassivation, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propPersistSessionAfterPassivation, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setPersistSessionAfterPassivation(booleanProp.booleanValue());
            }
        }

        final String propUsingApplicationSessionsAndInvalidateAll = "UsingApplicationSessionsAndInvalidateAll";
        strProp = getStringProperty(propUsingApplicationSessionsAndInvalidateAll, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propUsingApplicationSessionsAndInvalidateAll, true, baseServerLevelConfig, xtpProperties, booleanProp,
                                      Boolean.valueOf(SessionManagerConfig.getUsingApplicationSessionsAndInvalidateAll()), false)) {
                SessionManagerConfig.setUsingApplicationSessionsAndInvalidateAll(booleanProp.booleanValue());
            }
        }

        final String propUsingApplicationSessionsAlwaysRetrieve = "UsingApplicationSessionsAlwaysRetrieve";
        strProp = getStringProperty(propUsingApplicationSessionsAlwaysRetrieve, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propUsingApplicationSessionsAlwaysRetrieve, true, baseServerLevelConfig, xtpProperties, booleanProp,
                                      Boolean.valueOf(SessionManagerConfig.getUsingApplicationSessionsAlwaysRetrieve()), false)) {
                SessionManagerConfig.setUsingApplicationSessionsAlwaysRetrieve(booleanProp.booleanValue());
            }
        }

        // clone id
        final String propSIPCookieName = "SIPCookieName";
        strProp = getStringProperty(propSIPCookieName, xtpProperties);
        if (strProp != null) {
            if (shouldSetAndDoLogging(propSIPCookieName, true, baseServerLevelConfig, xtpProperties, strProp, SessionManagerConfig.getSipSessionCookieName(),
                                      strProp.equals(SessionManagerConfig.getSipSessionCookieName()))) {
                SessionManagerConfig.setSipSessionCookieName(strProp);
            }
        }
        
        //Property that allows the thread scheduler to be changed
        final String newScheduler= "UseSeparateSessionInvalidatorThreadPool";
        strProp = getStringProperty(newScheduler, xtpProperties);
        if(strProp != null){
            booleanProp=Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(newScheduler, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setUseSeparateSessionInvalidatorThreadPool(booleanProp.booleanValue());
            }
        }        

        //PM74718 Property to control how long to delay the start of the invalidation alarm.
        final String invalidatorAlarmDelay = "DelayInvalidationAlarmDuringServerStartup";
        strProp = getStringProperty(invalidatorAlarmDelay, xtpProperties);
        if (strProp != null) {
            try {
                Integer delayProp = Integer.valueOf(strProp);
                if (shouldSetAndDoLogging(invalidatorAlarmDelay, false, baseServerLevelConfig, xtpProperties, delayProp, null, false)) {
                    smc.setDelayForInvalidationAlarmDuringServerStartup(delayProp.intValue());
                }
            } catch (NumberFormatException nfe) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, invalidPropFoundMessage,new Object[] {invalidatorAlarmDelay, strProp});
            }
        }

        // PK80439 Property for preventing session ids longer than the permissible length getting created
        final String propForceSessionIdLengthCheck = "ForceSessionIdLengthCheck";
        strProp = getStringProperty(propForceSessionIdLengthCheck, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(propForceSessionIdLengthCheck, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setForceSessionIdLengthCheck(booleanProp.booleanValue());
            }
        }
        // PK80439 End

        // PM04304 Property for ignoring case while comparing session owner and authorized user
        final String securityUserIgnoreCase = "SecurityUserIgnoreCase";
        strProp = getStringProperty(securityUserIgnoreCase, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(securityUserIgnoreCase, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setSecurityUserIgnoreCase(booleanProp.booleanValue());
            }
        }

        //712446
        final String throwSecurityExceptionOnGetSessionFalse = "ThrowSecurityExceptionOnGetSessionFalse";
        strProp = getStringProperty(throwSecurityExceptionOnGetSessionFalse, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(throwSecurityExceptionOnGetSessionFalse, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false)) {
                smc.setThrowSecurityExceptionOnGetSessionFalse(booleanProp.booleanValue());
            }
        }

        final String invalidateOnUnauthorizedSessionRequestException = "InvalidateOnUnauthorizedSessionRequestException";
        strProp = getStringProperty(invalidateOnUnauthorizedSessionRequestException, xtpProperties);
        if (strProp != null) {
            booleanProp = Boolean.valueOf(strProp);
            if (shouldSetAndDoLogging(invalidateOnUnauthorizedSessionRequestException, false, baseServerLevelConfig, xtpProperties, booleanProp, null, false))
                smc.setInvalidateOnUnauthorizedSessionRequestException(booleanProp.booleanValue());
        }
        
        
        // PI18177 We will check incoming cloneIds against this string, needs HttpSessionCloneId to be set to take effect
        final String expectedCloneIds = "ExpectedCloneIds";
        strProp = getStringProperty(expectedCloneIds, xtpProperties);
        if (strProp != null) {
            if (shouldSetAndDoLogging(expectedCloneIds, true, baseServerLevelConfig, xtpProperties, strProp, null, false)) {
                SessionManagerConfig.setExpectedCloneIds(strProp);
                SessionManagerConfig.setExpectedCloneIdsPropertySet(true);
            }
        }
        
        // Feature 68570 Begin
        final String propConnectionRetryCount = "ConnectionRetryCount";
        strProp = getStringProperty(propConnectionRetryCount, xtpProperties);
        if (strProp != null) {
            try {
                int retryCount = new Integer(strProp).intValue();
                if (shouldSetAndDoLogging(propConnectionRetryCount, false, baseServerLevelConfig, xtpProperties, new Integer(retryCount), null, false)) {
                    smc.setConnectionRetryCount(retryCount);
                }
            } catch (NumberFormatException nfe) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, invalidPropFoundMessage, new Object[] { propConnectionRetryCount,
                                                                                                                                        strProp });
            }
        } // Feature 68570 End

    }

    /*
     * This method logs appropriate messages and returns whether we should set the
     * variable in our SMC.
     * If the variable is static (meaning all SMCs should contain the same value),
     * then we do not allow this to be set from SMCs other than the base.
     * Log messages will be displayed to document appropriate actions for the
     * customer.
     * If the variable is not static, we check if it is defined for this specific
     * SMC and will log an info message. Otherwise, we will just set it upon
     * return.
     */
    // private static boolean shouldSetAndDoLogging(String propName, boolean
    // isStatic, boolean isBase, Properties smProps, Object myValue, Object
    // baseValue, boolean stringEquals) {
    private static boolean shouldSetAndDoLogging(String propName, boolean isStatic, boolean isBase, Map<String, Object> xtpProperties, Object myValue, Object baseValue,
                                                 boolean stringEquals) {
        final String methodName = "shouldSetAndDoLogging";
        final String propFoundMessage = "SessionContext.propertyFound";
        final String serverLevelConfigOnlyMessage = "SessionProperties.serverLevelConfigOnly";
        final String propFoundButAlreadySetMessage = "SessionProperties.propertyFoundButAlreadySet";

        // variable that tells us whether to set the variable in our SMC
        boolean rc = false;
        if (isStatic) {
            if (isBase) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodName, propFoundMessage, new Object[] { propName, myValue });
                rc = true;
            } else if (xtpProperties.get(propName) != null) { // otherwise it's a webcontainer or system prop and we don't need to log
                                                              // it
                if (myValue != baseValue && !stringEquals) {
                    // only log error if it is not equal to the server level config
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodName, serverLevelConfigOnlyMessage, new Object[] { propName, myValue, baseValue });
                } else {
                    // Log message that it is set but equal!
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodName, propFoundButAlreadySetMessage, new Object[] { propName, myValue });
                }
            }
        } else {
            if (isBase || xtpProperties.get(propName) != null) {
                // only log the message for the serverLevelConfig or if this app has
                // overridden the property
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodName, propFoundMessage, new Object[] { propName, myValue });
            }
            rc = true;
        }
        return rc;
    }

    private static String getStringProperty(String propName, Map<String, Object> xtpProperties) {
        String propKey = propName;
        if (!xtpProperties.containsKey(propKey) && FullyQualifiedPropertiesMap.containsKey(propKey)) {
            /*-
             * If the fully qualified property name is not defined in server.xml,
             * try looking for the short property name instead
             */
            propKey = FullyQualifiedPropertiesMap.get(propKey);
        }
        Object propValue = xtpProperties.get(propKey);
        String strProp = null;
        if (propValue != null) {
            /*-
             * To reuse code from tWAS, we need to translate
             * the real object value to a String,
             * even though we later translate it back.
             */
            strProp = propValue.toString();
        }
        if (strProp == null) { // no SessionManager prop, check system for tWAS propName
            strProp = getSystemProperty(propName);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getStringProperty", propName + "=" + strProp);
        }
        return strProp;
    }

    private static String getSystemProperty(String propName) {
        String sysProp = null;
        try {
            sysProp = System.getProperty(propName);
        } catch (Throwable th) {
            com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.SessionProperties", "285", null);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, propName, "CommonMessage.exception", th);
        }
        return sysProp;
    }

    private static boolean setDefaultConfiguration(SessionManagerConfig smc, Map<String, Object> xtpProperties) {

        String sValue;
        Boolean bValue;
        Integer iValue;
        Long lValue;

        String s = "sslTrackingEnabled";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setUseSLLId(bValue.booleanValue());
        }
        s = "urlRewritingEnabled";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setEnableUrlRewriting(bValue.booleanValue());
        }
        s = "protocolSwitchRewritingEnabled";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setEnableUrlProtocolSwitchRewriting(bValue.booleanValue());
        }
        s = "cookiesEnabled";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setEnableCookies(bValue.booleanValue());
        }
        s = "cookieName";
        sValue = propertyToString(xtpProperties.get(s));
        if (sValue != null) {
            smc.setSessionCookieName(sValue);
        }
        s = "cookieDomain";
        sValue = propertyToString(xtpProperties.get(s));
        if (sValue != null) {
            smc.setSessionCookieDomain(sValue);
        }
        s = "cookieMaxAge";
        lValue = propertyToLong(xtpProperties.get(s));
        if (lValue != null) {
            smc.setSessionCookieMaxAge(lValue.intValue());
        }
        s = "cookiePath";
        sValue = propertyToString(xtpProperties.get(s));
        if (sValue != null) {
            smc.setSessionCookiePath(sValue);
        }
        s = "cookieSecure";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setSessionCookieSecure(bValue.booleanValue());
        }
        s = "cookieHttpOnly";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setSessionCookieHttpOnly(bValue.booleanValue());
        }
        s = "cookieSameSite";
        sValue = propertyToString(xtpProperties.get(s));

        if (sValue != null) {
            smc.setSessionCookieSameSite(SameSiteCookie.get(sValue));  
        }

        s = "maxInMemorySessionCount";
        iValue = propertyToInteger(xtpProperties.get(s));
        if (iValue != null) {
            smc.setInMemorySize(iValue.intValue());
        }
        s = "allowOverflow";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setEnableOverflow(bValue.booleanValue());
        }
        s = "invalidationTimeout";
        lValue = propertyToLong(xtpProperties.get(s));
        if (lValue != null) {
            smc.setSessionInvalidationTime(lValue.longValue());
        }
        s = "securityIntegrationEnabled";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setIntegrateSecurity(bValue.booleanValue());
        }
        s = "sessionPersistenceMode";
        sValue = propertyToString(xtpProperties.get(s));
        if (sValue != null) {
            smc.setPersistenceMode(sValue);
        }
        s = "useContextRootAsCookiePath";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setUseContextRootForSessionCookiePath(bValue.booleanValue());
        }
        s = "allowSerializedAccess"; // default=false
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setAllowSerializedSessionAccess(bValue.booleanValue());
        }
        s = "accessOnTimeout"; // default=true?
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setAccessSessionOnTimeout(bValue.booleanValue());
        }
        s = "maxWaitTime"; // default=0
        iValue = propertyToInteger(xtpProperties.get(s));
        if (iValue != null) {
            smc.setSerializedSessionAccessMaxWaitTime(iValue.intValue());
        }

        // configuration properties from DatabaseStoreService
        s = "db2RowSize";
        sValue = propertyToString(xtpProperties.get(s));
        if (sValue != null) {
            int rowSize = 4; // default is 4
            if (sValue.equals("4KB")) {
                rowSize = 4;
            } else if (sValue.equals("8KB")) {
                rowSize = 8;
            } else if (sValue.equals("16KB")) {
                rowSize = 16;
            } else if (sValue.equals("32KB")) {
                rowSize = 32;
            }
            smc.setRowSize(rowSize);
        }
        s = "tableSpaceName";
        sValue = propertyToString(xtpProperties.get(s));
        if (sValue != null) {
            smc.setTableSpaceName(sValue);
        }
        s = "useMultiRowSchema";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setUsingMultirow(bValue.booleanValue());
        }

        // tuning parameters; currently from DatabaseStoreService
        s = "scheduleInvalidation";
        bValue = propertyToBoolean(xtpProperties.get(s));
        if (bValue != null) {
            smc.setScheduledInvalidation(bValue.booleanValue());
        }
        s = "scheduleInvalidationFirstHour";
        iValue = propertyToInteger(xtpProperties.get(s));
        if (iValue != null) {
            int value = iValue.intValue();
            if (value > 23) {
                value = 23;
            } else if (value < 0) {
                value = 0;
            }
            smc.setFirstHour(value);
        }
        s = "scheduleInvalidationSecondHour";
        iValue = propertyToInteger(xtpProperties.get(s));
        if (iValue != null) {
            int value = iValue.intValue();
            if (value > 23) {
                value = 23;
            } else if (value < 0) {
                value = 0;
            }
            smc.setSecondHour(value);
        }
        s = "writeFrequency"; // note that we MUST always set writeFrequency; default to END_OF_SERVLET_SERVICE when not explicitly set
        sValue = propertyToString(xtpProperties.get(s));
        if ("TIME_BASED_WRITE".equals(sValue)) {
            smc.setEnableTimeBasedWrite(true);
        } else if ("MANUAL_UPDATE".equals(sValue)) {
            smc.setEnableManualWrite(true);
        } else { // assume END_OF_SERVLET_SERVICE
            smc.setEnableEOSWrite(true);
        }
        s = "writeInterval";
        lValue = propertyToLong(xtpProperties.get(s));
        if (lValue != null) {
            smc.setPropertyWriterInterval(lValue.intValue());
        }
        s = "writeContents";
        sValue = propertyToString(xtpProperties.get(s));
        if (sValue != null) {
            if (sValue.equals("ALL_SESSION_ATTRIBUTES"))
                smc.setwriteAllProperties();
            else if (sValue.equals("GET_AND_SET_ATTRIBUTES"))
                smc.setWriteGetAndSetAttributes();
            // otherwise, default to ONLY_UPDATED_ATTRIBUTES
        }

        return true;
    }

    private static String propertyToString(Object o) {
        if (o instanceof String) {
            String s = (String) o;
            if (s.length() > 0) {
                return s;
            }
        }
        return null;
    }

    private static Boolean propertyToBoolean(Object o) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        String s = propertyToString(o);
        if (s != null) {
            return Boolean.valueOf(s);
        }
        return null;
    }

    private static Integer propertyToInteger(Object o) {
        if (o instanceof Integer) {
            return (Integer) o;
        }
        String s = propertyToString(o);
        if (s != null) {
            return Integer.valueOf(s);
        }
        return null;
    }

    private static Long propertyToLong(Object o) {
        if (o instanceof Long) {
            return (Long) o;
        }
        String s = propertyToString(o);
        if (s != null) {
            return Long.valueOf(s);
        }
        return null;
    }

}