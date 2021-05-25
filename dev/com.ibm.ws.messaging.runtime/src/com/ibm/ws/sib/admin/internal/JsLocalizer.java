/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIBDestinationReliabilityType;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.BaseLocalizationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.admin.AliasDestination;
import com.ibm.ws.sib.admin.BaseDestination;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.ExtendedBoolean;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsShadowMessagingEngine;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.SIBDestination;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBLocalizationPoint;
import com.ibm.ws.sib.processor.Administrator;
import com.ibm.ws.sib.processor.SIMPAdmin;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

public class JsLocalizer {

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsLocalizer";

    private static final TraceComponent tc = SibTr.register(JsLocalizer.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

 
    // Our factory class for creating Admin objects.

    private JsAdminFactory jsaf = null;

    // Our Messaging Engine.

    private BaseMessagingEngineImpl _me = null;

    // MP Administration interface.

    private Administrator _mpAdmin = null;

    private final HashSet missingDestinations = new HashSet();

    // Map of all destination/mediation localization pairs, keyed by the UUID of
    // the target SIBDestination.

    private final HashMap<String, MainEntry> mainMap = new HashMap();

    // Map of all SIBDestination LocalizationEntry objects keyed by UUID, for which a
    // localization exists. (Note: destinations only, not MQ links)

    private HashMap<String, Object> lpMap = new HashMap();

    private final HashSet newDestinations = new HashSet();

    private final HashSet alterDestinations = new HashSet();
    public ArrayList<DestinationDefinition> updatedDestDefList = new ArrayList<DestinationDefinition>();
    public ArrayList<LocalizationDefinition> updatedLocDefList = new ArrayList<LocalizationDefinition>();

    private class MainEntry {

        private LocalizationDefinition _dld = null;

        private void setDestinationLocalization(LocalizationDefinition dld) {
            _dld = dld;
        }

        private LocalizationDefinition getDestinationLocalization() {
            return _dld;
        }
    }

    private class LocalizationEntry {

        private BaseLocalizationDefinition _ld = null;

        private LocalizationEntry(BaseLocalizationDefinition ld) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.entry(tc, "LocalizationEntry", ld);
            }
            _ld = ld;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.exit(tc, "LocalizationEntry");
            }
        }
        private BaseLocalizationDefinition getLocalizationDefinition(){
			return _ld;
        	
        }
    }

    public JsLocalizer(BaseMessagingEngineImpl me) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "JsLocalizer", me);
        }
        _me = me;
        // Get the admin factory class.
        try {
            jsaf = JsAdminFactory.getInstance();
        } catch (Exception e) {
            // No FFDC code needed
        }

        // Create a map of LocalizationDefinitions for the localization points on
        // our ME.

        HashMap<String, SIBLocalizationPoint> lps = _me._me.getMessagingEngine().getSibLocalizationPointList();
        Iterator<Map.Entry<String, SIBLocalizationPoint>> entries = lps.entrySet().iterator();

        while (entries.hasNext()) {

            Map.Entry<String, SIBLocalizationPoint> entry = entries.next();
            addLpToMaps(entry.getValue());
        }
        String busName = _me.getBusName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "Looking for bus called " + busName + ".");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "JsLocalizer");
        }
    }

    public BaseMessagingEngineImpl getEngine() {
        return _me;
    }

    public void loadLocalizations() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "loadLocalizations", this);
        }

        DestinationDefinition dd = null;

        if (!isInZOSServentRegion()) {
            _mpAdmin = ((SIMPAdmin) _me.getMessageProcessor()).getAdministrator();
        }

        Set s = mainMap.entrySet();
        Iterator _i = s.iterator();
        boolean valid;

        while (_i.hasNext()) {

            valid = true;
            Map.Entry mapEntry = (Map.Entry) _i.next();
            String destinationName = (String) mapEntry.getKey();
            MainEntry m = mainMap.get(destinationName);

            try {
                dd = (DestinationDefinition) _me.getSIBDestination(_me.getBusName(), destinationName);
            } catch (Exception e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".loadLocalizations", "1", this);
                String reason = m.getDestinationLocalization().getName();
                SibTr.error(tc, "destination config inconsistent", null);
                valid = false;
            }

            if (valid == true && !isInZOSServentRegion() && _mpAdmin != null) {

                try {
                    LocalizationDefinition dld = m.getDestinationLocalization();
                    try {
                        _mpAdmin.createDestinationLocalization(dd, dld);
                    } catch (Exception e) {
                        SibTr.error(tc, "LOCALIZATION_EXCEPTION_SIAS0113", new Object[]{dd.getName()});
                    }
                    // newly added code for updated the dd and dld returned by runtime code
                    // in the lpMap and mainMap
                    updatedDestDefList.add(dd);
                    updatedLocDefList.add(dld);
                    _i.remove();
                } catch (Exception e) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".loadLocalizations", "2", this);
                    SibTr.error(tc, "CREATE_DESTINATION_FAILED_SIAS0009", dd.getName());
                }
            }
        }
        updateDefinitionsAfterLoadLocalizations();
        // Now create list of updated destination definition and localization definition classes

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "loadLocalizations");
    }

    /**
     * Add the given SIBLocalizationPoint to the internal data structures lpMap
     * with localization point identifier as the key in the hashmap
     * 
     * @param lp Localization point to add.
     */
    private void addLpToMaps(LWMConfig lp) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "addLpToMaps", lp);
        }

        // Create an appropriate LocalizationDefinition and add it to the lpMap
        SIBLocalizationPoint lpConfig = (SIBLocalizationPoint) lp;
        LocalizationDefinition ld = ((JsAdminFactoryImpl) jsaf).createLocalizationDefinition(lpConfig);
        String lpIdentifier = lpConfig.getIdentifier();
        lpMap.put(lpIdentifier, new LocalizationEntry(ld));
        String destName = lpIdentifier.substring(0, lpIdentifier.indexOf("@"));

        // Get the MainEntry object for this LPP. If it doesn't exist create one.

        MainEntry mainEntry = mainMap.get(destName);

        if (mainEntry == null) {
            mainEntry = new MainEntry();
            mainEntry.setDestinationLocalization(ld);
            mainMap.put(destName, mainEntry);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "addLpToMaps");
        }

    }

    /**
     * Update the given SIBLocalizationPoint in the internal data structures lpMap
     * 
     * lpMap contains all types of SIBLocalizationPoints.
     * 
     * @param lp
     *            Localization point to add.
     * @return mainEntry the mainMap entry
     */
    private MainEntry updateLpMaps(LWMConfig lp) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "updateLpMaps", lp);
        }
        SIBLocalizationPoint lpConfig = ((SIBLocalizationPoint) lp);
        // Create a new LocalizationDefinition and update the lpMap with it
        String lpName = lpConfig.getIdentifier();
        if (lpMap.containsKey(lpName)) {
            lpMap.remove(lpName);
        }
        LocalizationDefinition ld = ((JsAdminFactoryImpl) jsaf).createLocalizationDefinition(lpConfig);
        LocalizationEntry lEntry = new LocalizationEntry(ld);
        lpMap.put(lpName, lEntry);
        String destName = lpName.substring(0, lpName.indexOf("@"));
        MainEntry mainEntry = mainMap.get(destName);

        if (mainEntry == null) {
            mainEntry = new MainEntry();
        }
            mainEntry.setDestinationLocalization(ld);
            mainMap.put(destName, mainEntry);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "updateLpMaps", lpMap);
        }
        return mainEntry;
    }

    /**
     * Add a single localization point to this JsLocalizer object and tell MP
     * about it. This method is used by dynamic config in tWAS.
     * 
     * @param lp
     * @param dd
     * @return boolean success Whether the LP was successfully added
     */
    public boolean addLocalizationPoint(LWMConfig lp, DestinationDefinition dd) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "addLocalizationPoint", lp);
        }

        boolean valid = false;
        LocalizationDefinition ld = ((JsAdminFactoryImpl) jsaf).createLocalizationDefinition(lp);
        if (!isInZOSServentRegion()) {
            _mpAdmin = ((SIMPAdmin) _me.getMessageProcessor()).getAdministrator();
        }
        try {
            _mpAdmin.createDestinationLocalization(dd, ld);
            updatedDestDefList.add(dd);
            updatedLocDefList.add(ld);
            LocalizationEntry lEntry = new LocalizationEntry(ld);
            lpMap.put(ld.getName(), lEntry);

            MainEntry newMainEntry = new MainEntry();
            newMainEntry.setDestinationLocalization(ld);
            mainMap.put(dd.getName(), newMainEntry);
            valid = true;
        } catch (Exception e) {
            SibTr.error(tc, "LOCALIZATION_EXCEPTION_SIAS0113", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "addLocalizationPoint", Boolean.valueOf(valid));
        }
        return valid;
    }

    /**
     * Check the old destination cache, if the destination is not found then it
     * has just been created. This method assumes that the destination is in the
     * new cache - this is not checked.
     * 
     * @param key
     * @return true if getSIBDestinationByUuid fails to return anything
     */
    private boolean isNewDestination(String key) {

        Object dd = null;

        try {
            dd = _me.getSIBDestinationByUuid(_me.getBusName(), key, false);
        } catch (Exception e) {
            // No FFDC code needed
        }

        return (dd == null);
    }

    /**
     * Modify the given localization point and tell MP. The parameter is a new
     * SIBLocalizationPoint which will replace the existing object inside of this.
     * This method is used by dynamic config.
     * 
     * @param lp
     *            New localizationPoint
     */
    public void alterLocalizationPoint(BaseDestination destination,LWMConfig lp) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "alterLocalizationPoint", lp);
        }

        boolean valid = true;
        DestinationDefinition dd = null;
        DestinationAliasDefinition dAliasDef=null;
        String key = getMainMapKey(lp);

        // Update localization point in main map entry and lpMap
        MainEntry m = updateLpMaps(lp);

        if (m == null) {
            String reason = CLASS_NAME + ".alterLocalizationPoint(): Entry for name " + key + " not found in cache";
            SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", reason);
            valid = false;
        } else {
            try {
                // We obtain the DD from the new cache in case it has changed. This is
                // lieu of a possible
                // design change to allow the independent signalling of a change to the
                // DD.

                BaseDestinationDefinition bdd =  _me.getSIBDestination(_me.getBusName(), key);
               
                if(destination.isAlias())
                {
                	AliasDestination aliasDest=(AliasDestination)destination;
                	dAliasDef=modifyAliasDestDefinition(aliasDest, (DestinationAliasDefinition) bdd);
                }
                else
                {
                	dd=(DestinationDefinition)modifyDestDefinition(destination,bdd);
                	                	
                }
            } catch (Exception e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".alterLocalizationPoint", "1", this);
                SibTr.exception(tc, e);
                String reason = m.getDestinationLocalization().getName();
                valid = false;
            }
        }

        if (valid == true && !isInZOSServentRegion() && _mpAdmin != null) {
        	if(destination.isAlias())
        	{
        		_mpAdmin.alterDestinationAlias(dAliasDef);
        	}else
        	{
        		 LocalizationDefinition ld=m.getDestinationLocalization();
        		 ld.setAlterationTime(dd.getAlterationTime());
        		 ld.setSendAllowed(dd.isSendAllowed());        		
        	     _mpAdmin.alterDestinationLocalization(dd, ld);
        	}
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "alterLocalizationPoint: LP altered on existing destination deferring alter until end, UUID=" + key + " Name=" + dd.getName());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "alterLocalizationPoint");
        }
    }

    /**
     * Delete a given localization point, this will normally involve altering the
     * destination localization held by mp, unless the associated
     * DestinationDefinition has gone, in which case the DestinationDefinition
     * will be flagged for deletion. The deletetion will occure in the endUpdate()
     * method which will be called after all create/alter/delete operations have
     * occured. This method is used by dynamic config.
     * 
     * @param lp
     * @throws SIException
     * @throws SIBExceptionBase
     */
    public void deleteLocalizationPoint(JsBus bus, LWMConfig dest) throws SIBExceptionBase, SIException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "deleteLocalizationPoint", ((BaseDestination)dest).getName());
        }
        DestinationDefinition destDef = (DestinationDefinition) _me.getSIBDestination(bus.getName(), ((SIBDestination) dest).getName());
        if (destDef == null) {
            missingDestinations.add(destDef.getName());
        }
        if (!isInZOSServentRegion() && _mpAdmin != null) {

            alterDestinations.add(destDef.getName());

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "unlocalize: Dest unlocalized on existing destination deferring alter until end, Destination Name=" + destDef.getName());
        }
        // Now add code to call administrator to delete destination localization
        deleteDestLocalizations(bus);
        alterDestinations.remove(destDef.getName());
        unlocalize(destDef);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteLocalizationPoint");
    }

    /**
     * This method currently just flags the localization point for later deletion
     * as it is not possible to unlocalize a destination. If this method is called
     * the DestinationDefinition will also have been deleted. This method is
     * provided for later expansion and is based on the unmediate method that
     * follows. More functionality than we really need at the moment but it
     * results in a better structure for the delete code.
     * 
     * @param lp
     * @throws SIBExceptionBase
     * @throws SIException
     */
//    private void unlocalize(ConfigObject lp) throws SIBExceptionBase, SIException {
//
//        String thisMethodName = "unlocalize(ConfigObject)";
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.entry(tc, thisMethodName, lp);
//        }
//
//        String mainmapKey = getMainMapKey(lp);
//        String lpUuid = null;
//        if (JsWccmRCSUtils.instanceOfSIBMQLocalizationPointProxy(lp)) {
//            lpUuid = lp.getString(CT_SIBMQLocalizationPointProxy.UUID_NAME, CT_SIBMQLocalizationPointProxy.UUID_DEFAULT);
//        } else {
//            lpUuid = lp.getString(CT_SIBLocalizationPoint.UUID_NAME, CT_SIBLocalizationPoint.UUID_DEFAULT);
//        }
//
//        unlocalize(mainmapKey, lpUuid);
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//            SibTr.exit(tc, thisMethodName);
//    }

    /**
     * Returns the name of the destination associated with the supplied
     * localization point.
     * 
     * @param lp a localization point proxy.
     * @return the identifier of the destination associated with the supplied
     *         localization point proxy.
     */
    private String getMainMapKey(LWMConfig lp) {

        String key = null;
        String lpIdentifier = ((SIBLocalizationPoint) lp).getIdentifier();
        key = lpIdentifier.substring(0, lpIdentifier.indexOf("@"));
        return key;
    }

    /**
     * Clear temporary sets.
     */
    void startUpdate() {
        missingDestinations.clear();
        newDestinations.clear();
        alterDestinations.clear();
    }

//    /**
//     * Complete any add/delete localization point operations that were flagged by
//     * calls to addLocalizationPoint() or deleteLocalizationPoint().
//     */
//    void endUpdate() {
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.entry(tc, "endUpdate", this);
//        }
//        deleteDestLocalizations();
//        createNewDestLocalizations();
//        alterDestLocalizations();
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.exit(tc, "endUpdate");
//        }
//    }

    /**
     * Tell MP about altered LPs identified by calls to
     * alterLocalizationPoint() method. This method is called at the
     * end of a dynamic update cycle to process all LPs for altered
     * locations.
     */
    private void alterDestLocalizations() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "alterDestLocalizations", this);
        }

        if (!isInZOSServentRegion() && _mpAdmin != null) {

            Iterator i = alterDestinations.iterator();

            while (i.hasNext()) {

                String key = (String) i.next();
                MainEntry m = mainMap.get(key);

                try {

                    DestinationDefinition dd = (DestinationDefinition) _me.getSIBDestinationByUuid(_me.getBusName(), key, true);

//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                        SibTr.debug(tc, "alterDestLocalizations: altering DestinationLocalization, UUID=" + key + " Name=" + dd.getName());
//                    }
//
//                    LocalizationDefinition dld = m.getDestinationLocalization();
//                    MediationLocalizationDefinition mld = null;//m.getMediationLocalization();
//                    MediationExecutionPointDefinition mepd = m.getMediationExecutionPoint();
//                    MQLocalizationDefinition mqdld = m.getMQDestinationLocalization();
//                    MQMediationLocalizationDefinition mqmld = m.getMQMediationLocalization();

//                    _mpAdmin.alterDestinationLocalization(dd, dld, mld, mepd, mqdld, mqmld);
                } catch (Exception e) {

                    com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".alterDestLocalizations", "1179", this);
                    SibTr.exception(tc, e);
                    String reason = null;

                    if (m.getDestinationLocalization() != null) {
                        reason = "uuid=" + m.getDestinationLocalization().getUuid() + " targetUuid=" + key;
                    }

                    SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", CLASS_NAME + ": Unable to modify Message Point: " + reason);
                }
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "alterDestLocalizations");
        }
    }

    /**
     * Tell MP about new LPs identified by calls to addLocalizationPoint() method.
     * This method is called at the end of a dynamic update cycle to process all
     * LPs for newly created locations.
     */
    private void createNewDestLocalizations() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "createNewDestLocalizations", this);
        }

        if (!isInZOSServentRegion() && _mpAdmin != null) {

            Iterator i = newDestinations.iterator();

            while (i.hasNext()) {

                String key = (String) i.next();
                MainEntry m = mainMap.get(key);

                try {

                    DestinationDefinition dd = (DestinationDefinition) _me.getSIBDestinationByUuid(_me.getBusName(), key, true);

//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                        SibTr.debug(tc, "createNewDestLocalizations: adding DestinationLocalization, UUID=" + key + " Name=" + dd.getName());
//                    }
//
//                    LocalizationDefinition dld = m.getDestinationLocalization();
//                    MediationLocalizationDefinition mld = m.getMediationLocalization();
//                    MediationExecutionPointDefinition mepd = m.getMediationExecutionPoint();
//                    MQLocalizationDefinition mqdld = m.getMQDestinationLocalization();
//                    MQMediationLocalizationDefinition mqmld = m.getMQMediationLocalization();
//
//                    _mpAdmin.createDestinationLocalization(dd, dld, mld, mepd, mqdld, mqmld);

                } catch (Exception e) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".createNewDestLocalizations", "1", this);
                    SibTr.exception(tc, e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "createNewDestLocalizations");
        }
    }

    /**
     * Tell MP about all deleted LPs that previously existed on locations which
     * have also been deleted.
     */
    private void deleteDestLocalizations(JsBus bus) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "deleteDestLocalizations", this);
        }

        Iterator i = alterDestinations.iterator();

        while (i.hasNext()) {

            String key = (String) i.next();

            try {
                // Get it from the old cache as it is no longer in the new cache.
                DestinationDefinition dd = (DestinationDefinition) _me.getSIBDestination(bus.getName(), key);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "deleteDestLocalizations: deleting DestinationLocalization, name =" + key + " Name=");
                }
                if (!isInZOSServentRegion() && _mpAdmin != null) {
                	LocalizationEntry dldEntry = (LocalizationEntry) lpMap.get(dd.getName()+"@"+_me.getName());
                	LocalizationDefinition dld=(LocalizationDefinition) dldEntry.getLocalizationDefinition();
                	
                    //Venu Liberty change: passing Destination UUID as String.
                    //Destination Definition is passed as NULL as entire destination has to be deleted
                    _mpAdmin.deleteDestinationLocalization(dd.getUUID().toString(), null);  
                }
               
            } catch (Exception e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".deleteDestLocalizations", "1", this);
                SibTr.exception(tc, e);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "deleteDestLocalizations");
        }
    }

    /**
     * Returns true if we are running in a ZOS servent region, in this case the
     * _mpAdmin object will be null as there will no MP.
     * 
     * @return true if parent ME is a JsShadowMessagingEngineImpl
     */
    private boolean isInZOSServentRegion() {

        if (_me instanceof JsShadowMessagingEngine) {
            return true;
        }

        return false;
    }

    /**
     * @param mainMapKey
     * @param lpUuid
     * @throws SIBExceptionBase
     * @throws SIException
     */
    private void unlocalize(DestinationDefinition destDef) throws SIBExceptionBase, SIException {

        String thisMethodName = "unlocalize";
        String destName = destDef.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { destName });
        }

        lpMap.remove(destName + "@" + _me.getName());
        MainEntry mainEntry = mainMap.get(destName);
        
        // Remove destination localization
        mainEntry.setDestinationLocalization(null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    // Reset MP Admin instance as MP has been stopped
    public void clearMPAdmin() {
        _mpAdmin = null;
    }

    void updateDefinitionsAfterLoadLocalizations() {
    	SibTr.entry(tc, "updateDefinitionsAfterLoadLocalizations");
        Iterator iter = updatedLocDefList.iterator();
        while (iter.hasNext()) {
            LocalizationDefinition dld = (LocalizationDefinition) iter.next();
            String destLPName = dld.getName();
            String destinationName = destLPName.substring(0, destLPName.indexOf("@"));
            MainEntry mainEntry = new MainEntry();
            mainEntry.setDestinationLocalization(dld);
            mainMap.put(destinationName, mainEntry);
            String key = destinationName + "@" + this._me.getName();
            if (lpMap.containsKey(key)) {
                lpMap.remove(key);
                lpMap.put(key,  new LocalizationEntry(dld));
            }
        }
        SibTr.exit(tc, "updateDefinitionsAfterLoadLocalizations", updatedLocDefList);
    }
    private DestinationDefinition modifyDestDefinition(BaseDestination config,BaseDestinationDefinition dest)
    {
    	DestinationDefinition ddf=null;
        Reliability _reliability = null;
        Reliability _maxReliability=null;
        boolean maintainStrictOrder=false;
        boolean receiveExclusive=false;
        Reliability _exceptionDiscardReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
        SIBDestination newDest=(SIBDestination)config;
        ddf=(DestinationDefinition)dest;
        ddf.setBlockedRetryTimeout(newDest.getBlockedRetryTimeout());
        ddf.setDefaultPriority(newDest.getDefaultPriority());
        String _rs = newDest.getDefaultReliability();
            if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
                _reliability = Reliability.BEST_EFFORT_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
                _reliability = Reliability.ASSURED_PERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
                _reliability = Reliability.EXPRESS_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
                _reliability = Reliability.RELIABLE_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
                _reliability = Reliability.RELIABLE_PERSISTENT;
            }
            ddf.setDefaultReliability(_reliability);            
            ddf.setExceptionDestination(newDest.getExceptionDestination());            
             _rs = newDest.getExceptionDiscardReliability();

            if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
                _exceptionDiscardReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
                _exceptionDiscardReliability = Reliability.ASSURED_PERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
                _exceptionDiscardReliability = Reliability.EXPRESS_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
                _exceptionDiscardReliability = Reliability.RELIABLE_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
                _exceptionDiscardReliability = Reliability.RELIABLE_PERSISTENT;
            }
            ddf.setExceptionDiscardReliability(_exceptionDiscardReliability);            
            _rs = newDest.getMaximumReliability();

            if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
                _maxReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
                _maxReliability = Reliability.ASSURED_PERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
                _maxReliability = Reliability.EXPRESS_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
                _maxReliability = Reliability.RELIABLE_NONPERSISTENT;
            } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
                _maxReliability = Reliability.RELIABLE_PERSISTENT;
            }
        	ddf.setMaxReliability(_maxReliability);
        	ddf.setMaxFailedDeliveries(newDest.getMaxFailedDeliveries());
        	ddf.setRedeliveryCountPersisted(newDest.isPersistRedeliveryCount());
        	ddf.setOverrideOfQOSByProducerAllowed(newDest.isOverrideOfQOSByProducerAllowed());        	
        	ddf.setReceiveAllowed(newDest.isReceiveAllowed());
        	maintainStrictOrder=newDest.isMaintainStrictOrder();
        	ddf.maintainMsgOrder(maintainStrictOrder);
        	receiveExclusive=newDest.isReceiveExclusive();
        	ddf.setReceiveExclusive(receiveExclusive);
            if(maintainStrictOrder)
            {
                if(!receiveExclusive)
                {
       		    ddf.setReceiveExclusive(true);
       		    // Override and warn if ordered destination.
                SibTr.debug(tc, "RECEIVE_EXCLUSIVE_OVERRIDE_WARNING_SIAS0048", new Object[] { newDest.getName()});
                }
       	    }
        	ddf.setSendAllowed(newDest.isSendAllowed());
        	if (ddf.getDestinationType() == DestinationType.TOPICSPACE) {
        	ddf.setTopicAccessCheckRequired(newDest.isTopicAccessCheckRequired());
        	
        	
        }
        return ddf;
    }
    private DestinationAliasDefinition modifyAliasDestDefinition(BaseDestination destination,DestinationAliasDefinition daf){
    	AliasDestination adest=(AliasDestination)destination;
    	Reliability _reliability = null;
        Reliability _maxReliability=null;
//        Reliability _exceptionDiscardReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
    	String _rs = adest.getDefaultReliability();
        if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
            _reliability = Reliability.BEST_EFFORT_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
            _reliability = Reliability.ASSURED_PERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
            _reliability = Reliability.EXPRESS_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
            _reliability = Reliability.RELIABLE_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
            _reliability = Reliability.RELIABLE_PERSISTENT;
        }
        if(_reliability!=null){
        	daf.setDefaultReliability(_reliability);
        }        
        daf.setDelegateAuthorizationCheckToTarget(adest.getDelegateAuthCheckToTargetDestination());        
        _rs = adest.getMaximumReliability();

        if (_rs.equals(SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT)) {
            _maxReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.ASSURED_PERSISTENT)) {
            _maxReliability = Reliability.ASSURED_PERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT)) {
            _maxReliability = Reliability.EXPRESS_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT)) {
            _maxReliability = Reliability.RELIABLE_NONPERSISTENT;
        } else if (_rs.equals(SIBDestinationReliabilityType.RELIABLE_PERSISTENT)) {
            _maxReliability = Reliability.RELIABLE_PERSISTENT;
        }
        if(_maxReliability!=null){
        daf.setMaxReliability(_maxReliability);
        }
        
        String b= adest.isOverrideOfQOSByProducerAllowed();
        ExtendedBoolean _producerQOSOverrideEnabled;
        ExtendedBoolean receiveAllowed;
        ExtendedBoolean sendAllowed;
        if (b.equalsIgnoreCase("TRUE")) {
            _producerQOSOverrideEnabled = ExtendedBoolean.TRUE;
        } else if(b.equalsIgnoreCase("FALSE")) {

            _producerQOSOverrideEnabled = ExtendedBoolean.FALSE;
        }else{
        	_producerQOSOverrideEnabled= ExtendedBoolean.NONE;
        }
        daf.setOverrideOfQOSByProducerAllowed(_producerQOSOverrideEnabled);
        
        b=adest.isReceiveAllowed();
        if (b.equalsIgnoreCase("TRUE")) {
        	receiveAllowed = ExtendedBoolean.TRUE;
        } else if(b.equalsIgnoreCase("FALSE")) {

        	receiveAllowed = ExtendedBoolean.FALSE;
        }else{
        	receiveAllowed= ExtendedBoolean.NONE;
        }
        daf.setReceiveAllowed(receiveAllowed);        
        b=adest.isSendAllowed();
        if (b.equalsIgnoreCase("TRUE")) {
        	sendAllowed = ExtendedBoolean.TRUE;
        } else if(b.equalsIgnoreCase("FALSE")) {

        	sendAllowed = ExtendedBoolean.FALSE;
        }else{
        	sendAllowed= ExtendedBoolean.NONE;
        }
        daf.setSendAllowed(sendAllowed);
        daf.setTargetName(adest.getTargetDestination());
        return daf;
    	
    }
}
