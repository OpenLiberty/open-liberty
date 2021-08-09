/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import java.util.Set;
import java.util.Map.Entry;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.AliasDestination;
import com.ibm.ws.sib.admin.BaseDestination;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.SIBDestination;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

public class JsDestinationCache {
  

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.impl.JsDestinationCache";
    private static final TraceComponent tc = SibTr.register(JsDestinationCache.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);


    // The bus.

    private JsBusImpl _bus = null;

    // The topicSpace audits.

    private final HashMap tsAuditMap = null;

    private JsAdminFactory _jsaf = null;

    // Cache of raw destinations read from config file.

    private final ArrayList<BaseDestination> _rawDestinations = new ArrayList<BaseDestination>();

    // Map of all destinations on this bus, and foreign destinations on another
    // bus defined on this bus, keyed by name.

    private final HashMap<String, HashMap<String, DestinationMapEntry>> _cacheByName = new HashMap<String, HashMap<String, DestinationMapEntry>>();

    // Map of all destinations on this bus, and foreign destinations on another
    // bus defined on this bus, keyed by UUID.

    private final HashMap<String, HashMap<String, DestinationMapEntry>> _cacheByUuid = new HashMap<String, HashMap<String, DestinationMapEntry>>();

    private class DestinationMapEntry {
        private BaseDestinationDefinition _dd = null;
        private final Set _set = new HashSet();

        private DestinationMapEntry(BaseDestinationDefinition dd, LWMConfig dest) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.entry(tc, "DestinationMapEntry", new Object[] { dd, dest });
            }

            _dd = dd;
//            buildLocalitySet(dest);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            {
                SibTr.exit(tc, "DestinationMapEntry", this);
            }
        }    

//
//        private void buildLocalitySet(LWMConfig dest) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//                SibTr.entry(tc, "buildLocalitySet", dest);
//            }
//
//            if (JsWccmRCSUtils.instanceOfSIBDestination(dest)) {
//                List el = dest.getObjectList(CT_SIBDestination.LOCALIZATIONPOINTREFS_NAME);
//                Iterator iter = el.iterator();
//
//                while (iter.hasNext()) {
//                    ConfigObject lpref = (ConfigObject) iter.next();
//                    this._set.add(lpref.getString(CT_SIBLocalizationPointRef.ENGINEUUID_NAME, CT_SIBLocalizationPointRef.ENGINEUUID_DEFAULT));
//                }
//            }
//
//            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//                SibTr.exit(tc, "buildLocalitySet");
//            }
//        }

        private BaseDestinationDefinition getDestinationDefinition() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.entry(tc, "getDestinationDefinition", this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.exit(tc, "getDestinationDefinition", _dd);
            }

            return _dd;
        }
    }

    // Map of all Mediation Localization Points on this ME's bus, keyed by uuid.

    public JsDestinationCache(JsBusImpl bus) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "JsDestinationCache", bus);
        }

        _bus = bus;

        // Get the admin factory class
        try {
            _jsaf = JsAdminFactory.getInstance();
        } catch (Exception e) {
            // No FFDC code needed
        }

        populateCache();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "JsDestinationCache", this);
        }
    }

    /**
     * Build the cache of DestinationDefinition objects in the configuration
     */
    private void populateCache() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "populateCache", this);
        }
        try {
            HashMap<String, BaseDestination> destList = _bus.getLWMMEConfig().getMessagingEngine().getDestinationList();
            Iterator<Entry<String, BaseDestination>> entries = destList.entrySet().iterator();
            while (entries != null && entries.hasNext()) 
            {
                Entry<String, BaseDestination> entry = entries.next();
                BaseDestination bd = (BaseDestination) entry.getValue();
                if(bd.isAlias())
                {
                	AliasDestination alias=(AliasDestination)bd;
                	_rawDestinations.add(alias);
                	BaseDestinationDefinition dd = ((JsAdminFactoryImpl) _jsaf).createDestinationAliasDefinition(alias);
                	addEntry(_bus.getName(), dd, alias);
                }
                else
                {
	                SIBDestination oo = (SIBDestination) entry.getValue();
	                _rawDestinations.add(oo);
	                if (oo.getDestinationType() == DestinationType.QUEUE) 
	                {
	                    
	                    LWMConfig queue = oo;
	                    DestinationDefinition dd = ((JsAdminFactoryImpl) _jsaf).createDestinationDefinition(queue);
	                    addEntry(_bus.getName(), dd, queue);
	                } else if (oo.getDestinationType() == DestinationType.TOPICSPACE) 
	                {
	                	
	                    LWMConfig topicspace = oo;
	                    DestinationDefinition dd = ((JsAdminFactoryImpl) _jsaf).createDestinationDefinition(topicspace);

                        // Set auditAllowed.

//                      Boolean auditAllowed = (Boolean) tsAuditMap.get(dd.getName());

//                      if (auditAllowed != null) {
//                        ((DestinationDefinitionImpl) dd).setAuditAllowed(auditAllowed.booleanValue());
//                      }
	                    addEntry(_bus.getName(), dd, topicspace);
	                }
                }
            } // ...end while...
        } catch (Exception e) {

            SibTr.error(tc, "POPULATE_DESTINATION_FAILED_SIAS0114" + e);
            e.printStackTrace();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "populateCache");
        }

    }

    private void addEntry(String busName, BaseDestinationDefinition dd, LWMConfig dest) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "addEntry", new Object[] { busName, dd, dest });
        }
       

        HashMap<String, DestinationMapEntry> destMap = _cacheByName.get(busName);

        if (destMap == null) {
            HashMap<String, DestinationMapEntry> newDestMap = new HashMap<String, DestinationMapEntry>();
            newDestMap.put(dd.getName(), new DestinationMapEntry(dd, dest));
            _cacheByName.put(busName, newDestMap);
        } else {
            destMap.put(dd.getName(), new DestinationMapEntry(dd, dest));
            _cacheByName.put(busName, destMap);
        }
/*
 * destMap = (HashMap) _cacheByUuid.get(busName);
 * 
 * if (destMap == null) {
 * HashMap newDestMap = new HashMap();
 * newDestMap.put(dest.getString(CT_SIBAbstractDestination.UUID_NAME, CT_SIBAbstractDestination.UUID_DEFAULT), new DestinationMapEntry(dd, dest));
 * _cacheByUuid.put(busName, newDestMap);
 * } else {
 * destMap.put(dest.getString(CT_SIBAbstractDestination.UUID_NAME, CT_SIBAbstractDestination.UUID_DEFAULT), new DestinationMapEntry(dd, dest));
 * }
 */

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "addEntry");
        }
    }

    private BaseDestinationDefinition getEntryByName(String busName, String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getEntryByName", new Object[] { busName, name });
        }

        BaseDestinationDefinition returnEntry = null;
        HashMap destMap = _cacheByName.get(busName);

        if (destMap != null) {
            DestinationMapEntry entry = (DestinationMapEntry) destMap.get(name);

            if (entry != null) {
                returnEntry = entry.getDestinationDefinition();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getEntryByName", returnEntry);
        }

        return returnEntry;
    }

    private BaseDestinationDefinition getEntryByUuid(String busName, String uuid) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getEntryByUuid", new Object[] { busName, uuid });
        }

        BaseDestinationDefinition returnEntry = null;
        HashMap destMap = _cacheByUuid.get(busName);

        if (destMap != null) {
            DestinationMapEntry entry = (DestinationMapEntry) destMap.get(uuid);

            if (entry != null) {
                returnEntry = entry.getDestinationDefinition();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getEntryByUuid", returnEntry);
        }

        return returnEntry;
    }

    /**
     * @param s
     */
    private void illegalArguments(String s) throws SIBExceptionBase {
        throw new SIBExceptionBase("Illegal argument value(s) specified; " + s);
    }

    /**
     * @param busName
     * @param name
     * @return
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public BaseDestinationDefinition getSIBDestination(String busName, String name) throws SIBExceptionBase, SIBExceptionDestinationNotFound {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getSIBDestination", new Object[] { busName, name });
        }

        String bus = null;

        if ((name == null)) {
            illegalArguments("name is mandatory");
        }

        if ((busName == null) || busName.equals("")) {
            bus = _bus.getName();
        } else {
            bus = busName;
        }

        BaseDestinationDefinition dd = getEntryByName(bus, name);

        if (dd == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.exit(tc, "getSIBDestination", "SIBExceptionDestinationNotFound");
            }

            throw new SIBExceptionDestinationNotFound(name);
        }
        

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getSIBDestination", dd);
        }

        return dd;
    }

    /**
     * @param busName
     * @param uuid
     * @return
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public BaseDestinationDefinition getSIBDestinationByUuid(String busName, String uuid) throws SIBExceptionBase, SIBExceptionDestinationNotFound {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getSIBDestinationByUuid", new Object[] { busName, uuid });
        }

        String bus = null;

        if ((uuid == null)) {
            illegalArguments("uuid is mandatory");
        }

        if ((busName == null) || busName.equals("")) {
            bus = _bus.getName();
        } else {
            bus = busName;
        }

        BaseDestinationDefinition dd = getEntryByUuid(bus, uuid);

        if (dd == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.exit(tc, "getSIBDestinationByUuid", "SIBExceptionDestinationNotFound");
            }

            throw new SIBExceptionDestinationNotFound(uuid);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getSIBDestinationByUuid", dd);
        }

        return dd;
    }

    /**
     * @param busName
     * @param name
     * @param dd
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public void getSIBDestination(String busName, String name, DestinationDefinition dd) throws SIBExceptionBase, SIBExceptionDestinationNotFound {
        // TODO: HIGH: Believe this method can be removed.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getSIBDestination", new Object[] { busName, name, dd });
        }

        String bus = null;

        if ((name == null) || (dd == null)) {
            illegalArguments("name or dd are mandatory");
        }

        if ((busName == null) || busName.equals("")) {
            bus = _bus.getName();
        } else {
            bus = busName;
        }

        // Get the destination from the configuration.

        LWMConfig sibdd = _getSIBDestination(bus, name);

        if (sibdd == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                SibTr.exit(tc, "getSIBDestination", "SIBExceptionDestinationNotFound");
            }

            throw new SIBExceptionDestinationNotFound(name);
        }

        // Overwrite the DD which was passed in

        ((DestinationDefinitionImpl) dd).reset(sibdd);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getSIBDestination");
        }
    }

    /**
     * @param busName
     * @param uuid
     * @return
     */
    public Set getSIBDestinationLocalitySet(String busName, String uuid) throws SIBExceptionBase {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getSIBDestinationLocalitySet", new Object[] { busName, uuid });
        }

        if ((uuid == null)) {
            illegalArguments("name is mandatory");
        }

        if ((busName != null) && (!(busName.equals(_bus.getName())))) {
            illegalArguments("will only currently support single bus");
        }

        Set set = _getSIBDestinationLocalitySet(busName, uuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getSIBDestinationLocalitySet", set);
        }

        return set;
    }

    /**
     * @param busName
     * @param name
     * @return
     */
    private LWMConfig _getSIBDestination(String busName, String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "_getSIBDestination", new Object[] { busName, name });
        }

        HashMap<String, BaseDestination> destList = null;
        String bus = null;
        String destBus = null;

        if ((busName == null) || (busName.equals(""))) {
            bus = _bus.getName();
        } else {
            bus = busName;
        }
        destList = _bus.getLWMMEConfig().getMessagingEngine().getDestinationList();
        // Find the logical destination on the bus to which this ME belongs.

        BaseDestination dest = null;
        Iterator<Entry<String, BaseDestination>> entries = destList.entrySet().iterator();

        while (entries.hasNext() && (dest == null)) {
            Entry<String, BaseDestination> entry = entries.next();
            BaseDestination d = entry.getValue();

            if (d.getName().equalsIgnoreCase(name)) {
                dest = d;
            }

        } // ...end while...

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "_getSIBDestination", dest);
        }

        return dest;
    }

    /**
     * @param busName
     * @param uuid
     * @return
     */
    private Set _getSIBDestinationLocalitySet(String busName, String uuid) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "_getSIBDestinationLocalitySet", new Object[] { busName, uuid });
        }

        Set set = new HashSet();
        Iterator<BaseDestination> _i = _rawDestinations.iterator();

//        while (_i.hasNext()) {
//            ConfigObject o = (ConfigObject) _i.next();
//
//            if (JsWccmRCSUtils.instanceOfSIBDestination(o)) {
//                ConfigObject d = o;
//
//                if (d.getString(CT_SIBDestination.UUID_NAME, CT_SIBDestination.UUID_DEFAULT).equals(uuid)) {
//                    Iterator iter = d.getObjectList(CT_SIBDestination.LOCALIZATIONPOINTREFS_NAME).iterator();
//
//                    while (iter.hasNext()) {
//                        ConfigObject lpref = (ConfigObject) iter.next();
//                        set.add(lpref.getString(CT_SIBLocalizationPointRef.ENGINEUUID_NAME, CT_SIBLocalizationPointRef.ENGINEUUID_DEFAULT));
//                    }
//                }
//            }
//        } // ...end while...

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "_getSIBDestinationLocalitySet", set);
        }

        return set;
    }

    private void populateAuditMap() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "populateAuditMap", this);
        } //   How to implement auditMap in libertyTBD
//
//        tsAuditMap = new HashMap();
//        ConfigObject sibAudit = null;
//
//        try {
//            // Get config service.
//
//            ConfigService service = (ConfigService) WsServiceRegistry.getService(this, ConfigService.class);
//
//            // Create server scope.
//
//            ConfigScope scope = service.createScope(ConfigScope.BUS);
//            scope.set(ConfigScope.BUS, _bus.getName());
//
//            // Get iterator over contents of audit document, could by from dynamic
//            // config so don't use cache.
//
//            List auditList = service.getDocumentObjects(scope, JsConstants.WCCM_DOC_SECURITY_AUDIT, false);
//
//            if (auditList != null && !auditList.isEmpty()) {
//                // There will only be one entry.
//
//                sibAudit = (ConfigObject) auditList.get(0);
//            }
//        } catch (FileNotFoundException e) {
//            // No FFDC code needed.
//            // Document doesn't exist yet.
//            sibAudit = null;
//        } catch (Exception e) {
//            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.admin.impl.JsDestinationCache.populateAuditMap", "1050", this);
//            SibTr.debug(tc, "CONFIG_LOAD_FAILED_SIAS0008", "buses\\" + _bus.getName() + "\\" + JsConstants.WCCM_DOC_SECURITY_AUDIT);
//            sibAudit = null;
//        }
//
//        // Read the configuration for the specified bus, and build a cache of
//        // configured audits.
//
//        if (sibAudit != null) {
//            // Get topicSpace audits.
//
//            List audits = sibAudit.getObjectList(CT_SIBAudit.TOPICSPACEAUDIT_NAME);
//            Iterator i = audits.iterator();
//
//            while (i.hasNext()) {
//                ConfigObject tsAudit = (ConfigObject) i.next();
//
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    SibTr.debug(tc, "Adding topic space to audit map", new Object[] {
//                                                                                     tsAudit.getString(CT_SIBTopicSpaceAudit.IDENTIFIER_NAME,
//                                                                                                       CT_SIBTopicSpaceAudit.IDENTIFIER_DEFAULT),
//                                                                                     Boolean.valueOf(tsAudit.getBoolean(CT_SIBTopicSpaceAudit.ALLOWAUDIT_NAME,
//                                                                                                                        CT_SIBTopicSpaceAudit.ALLOWAUDIT_DEFAULT)) });
//                }
//
//                tsAuditMap.put(tsAudit.getString(CT_SIBTopicSpaceAudit.IDENTIFIER_NAME, CT_SIBTopicSpaceAudit.IDENTIFIER_DEFAULT),
//                               Boolean.valueOf(tsAudit.getBoolean(
//                                                                  CT_SIBTopicSpaceAudit.ALLOWAUDIT_NAME, CT_SIBTopicSpaceAudit.ALLOWAUDIT_DEFAULT)));
//            }
//        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "populateAuditMap");
        }
    }

    public void populateUuidCache(ArrayList list) throws Exception {
    	String busName=_bus.getName();
        Iterator iter = list.iterator();
        HashMap<String, DestinationMapEntry> storedUUIDMap=_cacheByUuid.get(busName);
        if(storedUUIDMap==null){
        	storedUUIDMap=new HashMap<String, DestinationMapEntry>();
        }
        HashMap<String, DestinationMapEntry> storedNameMap=_cacheByName.get(busName);
        
        while (iter.hasNext()) {
        	BaseDestinationDefinition bdef = (BaseDestinationDefinition) iter.next();
        	BaseDestinationDefinition destDef;
        	if(bdef.isAlias()){
        		destDef=(DestinationAliasDefinition)bdef;
        	}else{
            destDef = (DestinationDefinition)bdef;
        	}
            DestinationMapEntry destMapEntry=setLocalizedDestinationInUUIDCache(destDef);
            String dname=destDef.getName();
            if(storedUUIDMap.containsKey(dname)){
            	storedUUIDMap.remove(dname);
            }
            storedUUIDMap.put(destDef.getUUID().toString(),destMapEntry);
            
            if(storedNameMap.containsKey(dname)){
            	storedNameMap.remove(dname);
            }
            storedNameMap.put(dname,destMapEntry);
        }
        _cacheByName.put(busName, storedNameMap);
        _cacheByUuid.put(busName, storedUUIDMap);
    }

    private DestinationMapEntry setLocalizedDestinationInUUIDCache(BaseDestinationDefinition destDef) {
    	SibTr.entry(tc, "setLocalizedDestinationInUUIDCache"+destDef);
    	
    	BaseDestination destConfig = getDestinationConfigByName(destDef);

        SibTr.exit(tc, "setLocalizedDestinationInUUIDCache");
        return new DestinationMapEntry(destDef, destConfig);
    }

    private BaseDestination getDestinationConfigByName(BaseDestinationDefinition destDef) {
        HashMap<String, BaseDestination> destMap = _bus.getLWMMEConfig().getMessagingEngine().getDestinationList();
        BaseDestination dConfig = destMap.get(destDef.getName());
        return dConfig;
    }

    public BaseDestinationDefinition addNewDestinationToCache(BaseDestination config) {
    	BaseDestinationDefinition dd = null;

        if(!config.isAlias())
        {
        	SIBDestination dest=(SIBDestination)config;

        	 _rawDestinations.add(dest);
        	if (dest.getDestinationType() == DestinationType.QUEUE) 
        	{           
            LWMConfig queue = config;
            dd = ((JsAdminFactoryImpl) _jsaf).createDestinationDefinition(queue);
            addEntry(_bus.getName(), dd, queue);
        	}
        	else if (dest.getDestinationType() == DestinationType.TOPICSPACE) 
        	{
            LWMConfig topicspace = config;
            dd = ((JsAdminFactoryImpl) _jsaf).createDestinationDefinition(topicspace);
            addEntry(_bus.getName(), dd, topicspace);
        	}
            // Set auditAllowed.

//            Boolean auditAllowed = (Boolean) tsAuditMap.get(dd.getName());

//            if (auditAllowed != null) {
//                ((DestinationDefinitionImpl) dd).setAuditAllowed(auditAllowed.booleanValue());
//            }
            
        }else
        {
        AliasDestination d=(AliasDestination)config;
        _rawDestinations.add(d);
        dd = ((JsAdminFactoryImpl) _jsaf).createDestinationAliasDefinition(config);
        addEntry(_bus.getName(),dd,d);
        }

        return dd;
    }

    public void deleteDestination(String busName, String destName) throws Exception {
    	SibTr.entry(tc, "deleteDestination");
        Iterator<BaseDestination> iter = _rawDestinations.iterator();
        while (iter.hasNext()) {
            BaseDestination dest = iter.next();
            if (dest.getName().equals(destName)) {
                iter.remove();
            }

        }
        HashMap mp = _cacheByName.get(busName);
        mp.remove(destName);
        SibTr.exit(tc, "deleteDestination"+destName);
    }

    

}
