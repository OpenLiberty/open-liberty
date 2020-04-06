/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinitionImpl;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinitionImpl;

/**
 * Registry for MetaType information.
 */

final class MetaTypeRegistry {

    private static final TraceComponent tc = Tr.register(MetaTypeRegistry.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    public static final String MTP_REMOVED_TOPIC = "internal_mtp_removed";
    public static final String MTP_ADDED_TOPIC = "internal_mtp_added";
    public static final String UPDATED_PIDS = "internal_mtp_updated_pids";
    public static final String BUNDLE = "internal_mtp_bundle";
    public static final String MTP_INFO = "internal_mtp_info";

    /**
     * Valid registry entries keyed by pid
     */
    private final Map<String, RegistryEntry> entryMap = new ConcurrentHashMap<String, RegistryEntry>();

    /**
     * Valid registry entries keyed by alias
     */
    private final Map<String, RegistryEntry> aliasMap = new ConcurrentHashMap<String, RegistryEntry>();

    /**
     * Invalid registry entries keyed by (missing) extends/refines pid. These are entries invalid due to missing information from another
     * metatype, not internally inconsistent entries which are completely ignored.
     */
    private final Map<String, Collection<RegistryEntry>> invalidEntryMap = new ConcurrentHashMap<String, Collection<RegistryEntry>>();

    /**
     * Map from child alias to map of parent pid to child registry entry. Since you can apparently also use the full pid instead of child alias,
     * everything is also under the (child) pid.
     */
    private final Map<String, Map<String, RegistryEntry>> childAliasMap = new ConcurrentHashMap<String, Map<String, RegistryEntry>>();
    private final Map<String, Map<String, RegistryEntry>> childPidMap = new ConcurrentHashMap<String, Map<String, RegistryEntry>>();

    /**
     * map from service name to registry entries mentioning that service in object class.
     */
    private final Map<String, List<RegistryEntry>> serviceToObjectClassMap = new ConcurrentHashMap<String, List<RegistryEntry>>();

    /**
     * map from service name to registry entries mentioning that service in an AD.
     */
    private final Map<String, List<RegistryEntry>> serviceToServicesMap = new ConcurrentHashMap<String, List<RegistryEntry>>();

    private final ConcurrentHashMap<Bundle, MetaTypeInformation> bundleMap = new ConcurrentHashMap<Bundle, MetaTypeInformation>();

    /** Tracker for Metatype service */
    private ServiceTracker<MetaTypeService, MetaTypeService> metaTypeTracker = null;

    private ServiceTracker<MetaTypeProvider, MetaTypeProvider> mtpTracker = null;

    void start(BundleContext bc) {

        metaTypeTracker = new ServiceTracker<MetaTypeService, MetaTypeService>(bc, MetaTypeService.class.getName(), null);
        metaTypeTracker.open();
    }

    void stop(BundleContext context) {
        if (null != metaTypeTracker) {
            metaTypeTracker.close();
            metaTypeTracker = null;
        }

        if (null != this.mtpTracker) {
            this.mtpTracker.close();
            this.mtpTracker = null;
        }
    }

    Set<RegistryEntry> addMetaType(Bundle bundle) {

        MetaTypeService metaTypeService = metaTypeTracker.getService();

        if (metaTypeService != null) {
            MetaTypeInformation metaTypeInformation = metaTypeService.getMetaTypeInformation(bundle);

            if (metaTypeInformation != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding metatype for bundle {0}", bundle);
                }
                return addMetaType(metaTypeInformation);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "No metatype found for bundle {0}", bundle);
                }
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The metatype service was not available");
            }
        }
        return Collections.emptySet();
    }

    /**
     * get the registry entry, if any, for the supplied pid, which may be null
     *
     * @param pid pid of desired registry entry, or null
     * @return entry for the pid, or null.
     */
    RegistryEntry getRegistryEntry(String pid) {
        if (pid == null) {
            return null;
        }
        return entryMap.get(pid);
    }

    RegistryEntry getRegistryEntryByAlias(String pid) {
        return aliasMap.get(pid);
    }

    RegistryEntry getRegistryEntryByPidOrAlias(String id) {
        if (id == null) {
            return null;
        }
        RegistryEntry re = entryMap.get(id);
        if (re == null) {
            re = aliasMap.get(id);
        }
        return re;
    }

    //TODO for debugging
    Collection<String> getRegistryEntryPids() {
        return entryMap.keySet();
    }

    Collection<RegistryEntry> getAllRegistryEntries() {
        return entryMap.values();
    }

    MetaTypeInformation getMetaTypeInformation(Bundle bundle) {
        return bundleMap.get(bundle);
    }

    synchronized Set<RegistryEntry> addMetaType(MetaTypeInformation information) {
        Set<RegistryEntry> updatedRegistryEntries = new HashSet<RegistryEntry>();

        boolean newEntries = addMetaTypeDefinition(information, information.getPids(), false, updatedRegistryEntries);
        newEntries |= addMetaTypeDefinition(information, information.getFactoryPids(), true, updatedRegistryEntries);

        if (newEntries) {
            MetaTypeInformation existingInfo = bundleMap.putIfAbsent(information.getBundle(), information);
            if (existingInfo != null && existingInfo != information) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "addMetaType: bundleMap.putIfAbsent", information, existingInfo);
                }
                throw new IllegalStateException("addMetaType");
            }
        }

        return updatedRegistryEntries;
    }

    private boolean addMetaTypeDefinition(MetaTypeInformation info, String pid, boolean isFactory, Set<RegistryEntry> updatedRegistryEntries) {
        RegistryEntry pidEntry = entryMap.get(pid);
        if (pidEntry == null) {
            ExtendedObjectClassDefinition ocd = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(info.getObjectClassDefinition(pid, null),
                                                                                                                   info.getBundle().getLocation());
//HOPEFULLY TEMPORARY
            ((ExtendedObjectClassDefinitionImpl) ocd).setPid(pid);
            //validate
            boolean invalid = false;
            if (isFactory) {
                if ((ocd.getExtends() == null) && !(ocd.getExtendsAlias() == null)) {
                    Tr.error(tc, "error.ExtendsAliasMustExtend", new Object[] { pid, ocd.getExtendsAlias() });
                    invalid = true;
                }
                //Is this valid?  What about a top level extends not intended to be nested?
//                if (!(ocd.getExtends() == null && ocd.getRefines() == null) && (ocd.getExtendsAlias() == null)) {
//                    Tr.error(tc, "error.ExtendOrRefineRequiresExtendsAlias", new Object[] { pid, ocd.getExtends(), ocd.getRefines() });
//                    invalid = true;
//                }

            } else {
                if (ocd.getExtends() != null) {
                    Tr.error(tc, "error.factoryOnly", new Object[] { pid, ocd.getExtends() });
                    invalid = true;
                }
                if (ocd.getExtendsAlias() != null) {
                    Tr.error(tc, "error.factoryOnly.extendsAlias", new Object[] { pid, ocd.getExtendsAlias() });
                    invalid = true;
                }
            }
            if ((ocd.getParentPID() != null) && (ocd.getChildAlias() == null)) {
                Tr.error(tc, "error.parentpid.and.childalias", new Object[] { pid, "ibm:parentPid", ocd.getParentPID(), "ibm:childAlias" });
            } else if ((ocd.getParentPID() == null) && (ocd.getChildAlias() != null)) {
                Tr.error(tc, "error.parentpid.and.childalias", new Object[] { pid, "ibm:childAlias", ocd.getChildAlias(), "ibm:parentPid" });
            }
            //not both childAlias and extendsAlias?
            if (invalid) {
                return true;
            }
            pidEntry = new RegistryEntry(pid, isFactory, info.getBundle(), ocd, this);
            synchronized (invalidEntryMap) {
                if (pidEntry.isValid()) {
                    updatedRegistryEntries.add(pidEntry);
                    addValidRegistryEntry(pidEntry, updatedRegistryEntries);
                } else {
                    String extendsPid = pidEntry.getExtends();
                    if (extendsPid == null) {
                        throw new IllegalStateException("invalid RegistryEntry that does not extend " + pid);
                    }
                    Collection<RegistryEntry> invalids = invalidEntryMap.get(extendsPid);
                    if (invalids == null) {
                        invalids = new ArrayList<RegistryEntry>();
                        invalidEntryMap.put(extendsPid, invalids);
                    }
                    invalids.add(pidEntry);
                }
            }
            return true;

        } else if (info.getBundle().getBundleId() != pidEntry.getBundleId()) {
            warnCollision(pid, null, pidEntry, info.getBundle().getBundleId(), info.getObjectClassDefinition(pid, null).getID());
        }
        return false;

    }

    private void addValidRegistryEntry(RegistryEntry pidEntry, Set<RegistryEntry> updatedRegistryEntries) {
        updatedRegistryEntries.add(pidEntry);
        entryMap.put(pidEntry.getPid(), pidEntry);

        Collection<RegistryEntry> invalids = invalidEntryMap.remove(pidEntry.getPid());
        if (invalids != null) {
            for (RegistryEntry invalid : invalids) {
                invalid.complete(pidEntry, this);
                if (invalid.isValid()) {
                    addValidRegistryEntry(invalid, updatedRegistryEntries);
                }
            }
        }
        //hierarchy is now as complete as possible
        processReferencedTypes(pidEntry);

        String alias = pidEntry.getAlias();
        if (alias != null) {
            RegistryEntry aliasEntry = aliasMap.get(alias);
            if (aliasEntry == null) {
                aliasMap.put(alias, pidEntry);
            } else if (pidEntry != aliasEntry) {
                warnCollision(pidEntry.getPid(), alias, pidEntry, aliasEntry.getBundleId(), aliasEntry.getObjectClassDefinition().getID());
            }
        }

        String childAlias = pidEntry.getChildAlias();
        if (childAlias != null) {
            Map<String, RegistryEntry> parentMap = childAliasMap.get(childAlias);
            if (parentMap == null) {
                parentMap = new HashMap<String, RegistryEntry>();
                childAliasMap.put(childAlias, parentMap);
            }
            String parentPid = pidEntry.getObjectClassDefinition().getParentPID();
            if (parentPid == null) {
                Tr.error(tc, "error.specify.parentpid", pidEntry.getPid());
            } else {
                RegistryEntry parentEntry = parentMap.get(parentPid);
                if (parentEntry == null) {
                    parentMap.put(parentPid, pidEntry);
                    childPidMap.put(pidEntry.getPid(), parentMap);
                } else {
                    warnCollision(pidEntry.getPid(), childAlias, pidEntry, parentEntry.getBundleId(), parentEntry.getObjectClassDefinition().getID());
                }

            }
        }

        List<String> objectClass = pidEntry.getObjectClassDefinition().getObjectClass();
        if (objectClass != null) {
            for (String service : objectClass) {
                List<RegistryEntry> exposers = serviceToObjectClassMap.get(service);
                if (exposers == null) {
                    exposers = new CopyOnWriteArrayList<RegistryEntry>();
                    serviceToObjectClassMap.put(service, exposers);
                }
                exposers.add(pidEntry);
            }
        }

    }

    private boolean addMetaTypeDefinition(MetaTypeInformation information, String[] pids, boolean isFactory, Set<RegistryEntry> updatedRegistryEntries) {
        boolean newEntries = false;

        if (pids != null) {
            for (String pid : pids) {
                if (pid != null) {
                    newEntries |= addMetaTypeDefinition(information, pid, isFactory, updatedRegistryEntries);
                }
            }
        }
        return newEntries;
    }

    /**
     * Keeps track of which OCDs are referenced by a PID type attribute in another OCD
     *
     * @param pidEntry
     */
    private void processReferencedTypes(RegistryEntry pidEntry) {
        //parent first, AD has a reference pid
        for (ExtendedAttributeDefinition ad : pidEntry.getAttributeMap().values()) {
            if (ad.getType() == MetaTypeFactory.PID_TYPE) {
                if (ad.getReferencePid() != null) {
                    RegistryEntry other = getRegistryEntry(ad.getReferencePid());
                    if (other != null) {
                        PidReference ref = new PidReference(other, pidEntry, ad.getID(), true);
                        other.addReferencingEntry(ref);
                        pidEntry.addReferencedEntry(ref);
                    }
                } else if (ad.getService() != null) {
                    addServiceUse(ad.getService(), pidEntry);
                } else {
                    // Error.. should always be caught by metatype validation.. warn anyway?
                }
            }
        }

        //child first, child has a parentPid
        if (pidEntry.getObjectClassDefinition().getParentPID() != null) {
            RegistryEntry other = getRegistryEntry(pidEntry.getObjectClassDefinition().getParentPID());
            if (other != null) {
                pidEntry.addReferencingEntry(new PidReference(pidEntry, other, pidEntry.getChildAlias(), false));
            }
            //else.... this should go into invalidParentEntryMap
        }

        //see if anyone is referencing us.
        for (RegistryEntry other : entryMap.values()) {
            if (pidEntry.getPid().equals(other.getObjectClassDefinition().getParentPID())) {
                other.addReferencingEntry(new PidReference(other, pidEntry, other.getChildAlias(), false));
            }
            for (ExtendedAttributeDefinition ad : other.getAttributeMap().values()) {
                if (ad.getType() == MetaTypeFactory.PID_TYPE) {
                    if (pidEntry.getPid().equals(ad.getReferencePid())) {
                        pidEntry.addReferencingEntry(new PidReference(pidEntry, other, ad.getID(), true));
                        //TODO deal with service/objectclass here?
                    }
                }
            }
        }
    }

    /**
     * @param serviceUse
     */
    private void addServiceUse(String service, RegistryEntry entry) {
        List<RegistryEntry> serviceUsers = serviceToServicesMap.get(service);
        if (serviceUsers == null) {
            serviceUsers = new CopyOnWriteArrayList<RegistryEntry>();
            serviceToServicesMap.put(service, serviceUsers);
        }
        serviceUsers.add(entry);

    }

    protected static class PidReference {
        //pid of parent, outer entry
        private final RegistryEntry referencingEntry;
        //AD name for parent first, childAlias/alias (?) for child-first
        private final String baseAccessor;
        //pid of child, nested entry
        private final RegistryEntry referencedEntry;

        private final boolean isParentFirst;

        /**
         * @param referencedEntry  RegistryEntry of child (nested)
         * @param referencingEntry RegistryEntry for parent (outer)
         * @param ad               parent AD name containing the ibm:reference or ibm:service or childAlias
         */
        PidReference(RegistryEntry referencedEntry, RegistryEntry referencingEntry, String ad, boolean isParentFirst) {
            this.referencedEntry = referencedEntry;
            this.referencingEntry = referencingEntry;
            this.baseAccessor = ad;
            this.isParentFirst = isParentFirst;
        }

        public String getAccessor() {
            return baseAccessor;
        }

        public RegistryEntry getReferencedEntry() {
            return referencedEntry;
        }

        public RegistryEntry getReferencingEntry() {
            return referencingEntry;
        }

        public boolean isParentFirst() {
            return isParentFirst;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        @Trivial
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((baseAccessor == null) ? 0 : baseAccessor.hashCode());
            result = prime * result + ((referencedEntry == null) ? 0 : referencedEntry.getPid().hashCode());
            result = prime * result + ((referencingEntry == null) ? 0 : referencingEntry.getPid().hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        @Trivial
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PidReference other = (PidReference) obj;
            if (baseAccessor == null) {
                if (other.baseAccessor != null)
                    return false;
            } else if (!baseAccessor.equals(other.baseAccessor))
                return false;
            if (referencedEntry == null) {
                if (other.referencedEntry != null)
                    return false;
            } else if (!referencedEntry.getPid().equals(other.referencedEntry.getPid()))
                return false;
            if (referencingEntry == null) {
                if (other.referencingEntry != null)
                    return false;
            } else if (!referencingEntry.getPid().equals(other.referencingEntry.getPid()))
                return false;
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        @Trivial
        public String toString() {
            return super.toString() + "[parent: " + referencingEntry.getPid() + ", child: " + referencedEntry.getPid() + ", accessed by: " + getAccessor()
                   + (isParentFirst ? ", parent-first" : ", child-first") + "]";
        }

    }

    static class ServiceUse {
        private final String service;
        private final RegistryEntry registryEntry;
        private final String attribute;

        /**
         * @param service
         * @param registryEntry
         * @param attribute
         */
        public ServiceUse(String service, RegistryEntry registryEntry, String attribute) {
            super();
            this.service = service;
            this.registryEntry = registryEntry;
            this.attribute = attribute;
        }

        /**
         * @return the service
         */
        public String getService() {
            return service;
        }

        /**
         * @return the registryEntry
         */
        public RegistryEntry getRegistryEntry() {
            return registryEntry;
        }

        /**
         * @return the attribute
         */
        public String getAttribute() {
            return attribute;
        }

    }

    List<RegistryEntry> getEntriesExposingService(String service) {
        return serviceToObjectClassMap.get(service);
    }

    List<RegistryEntry> getEntriesUsingService(RegistryEntry entry) {
        List<String> objectClass = entry.getObjectClassDefinition().getObjectClass();
        if (objectClass != null) {
            List<RegistryEntry> references = new ArrayList<RegistryEntry>();
            for (String service : objectClass) {
                List<RegistryEntry> users = serviceToServicesMap.get(service);
                if (users != null) {
                    references.addAll(users);
                }
            }
            return references;
        }
        return Collections.emptyList();
    }

    public Collection<RegistryEntry> getReferencedEntries(ExtendedAttributeDefinition attrDef) {
        if (attrDef == null)
            throw new NullPointerException("attrDef");
        if (attrDef.getType() != MetaTypeFactory.PID_TYPE)
            throw new IllegalArgumentException("Must be pid type, not " + attrDef.getType());
        if (attrDef.getReferencePid() != null) {
            RegistryEntry entry = getRegistryEntry(attrDef.getReferencePid());
            if (entry == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(entry);
        }
        return serviceToObjectClassMap.get(attrDef.getService());
    }

    private void warnCollision(String pid, String alias, RegistryEntry entry1, long bundleId2, String ocdId2) {
        Object pidAlias = pid;
        if (alias != null) {
            pidAlias = Arrays.asList(pid, alias);
        }
        List<String> ids = Arrays.asList(entry1.getBundleId() + ":" + entry1.getObjectClassDefinition().getID(), bundleId2 + ":"
                                                                                                                 + ocdId2);
        Tr.error(tc, "error.alias.collision", new Object[] { pidAlias, ids });

    }

    MetaTypeInformation removeMetaType(Bundle bundle) {
        MetaTypeInformation information = bundleMap.remove(bundle);
        if (information != null) {
            Set<RegistryEntry> removed = removeMetaTypeDefinitions(information, information.getPids());
            removed.addAll(removeMetaTypeDefinitions(information, information.getFactoryPids()));

            for (RegistryEntry entry : removed) {
                // Go through and remove all PidReference entries from types that reference this type

                for (PidReference ref : entry.getReferencedEntries()) {
                    RegistryEntry other = ref.getReferencedEntry();
                    if (other != null) {
                        other.removeReferencingEntry(ref);
                    }
                }
                entry.removeAllReferences();

            }
        }
        return information;

    }

    synchronized Set<RegistryEntry> removeMetaTypeDefinitions(MetaTypeInformation information, String[] pids) {
        Set<RegistryEntry> removedRegistryEntries = new HashSet<RegistryEntry>();
        if (pids != null) {
            for (String pid : pids) {

                RegistryEntry pidEntry = entryMap.remove(pid);
                if (pidEntry == null) {
                    for (Map.Entry<String, Collection<RegistryEntry>> entry : invalidEntryMap.entrySet()) {
                        for (RegistryEntry test : entry.getValue()) {
                            if (test.getPid().equals(pid)) {
                                pidEntry = test;
                                entry.getValue().remove(test);
                                if (entry.getValue().isEmpty()) {
                                    invalidEntryMap.remove(entry.getKey());
                                }
                                break;
                            }
                        }
                    }
                }

                if (pidEntry != null) {
                    removedRegistryEntries.add(pidEntry);

                    if (pidEntry.getAlias() != null) {
                        RegistryEntry aliasEntry = aliasMap.get(pidEntry.getAlias());
                        if (aliasEntry == pidEntry) {
                            aliasMap.remove(pidEntry.getAlias());
                        }
                    }

                    if (pidEntry.getChildAlias() != null) {
                        Map<String, RegistryEntry> parentMap = childAliasMap.get(pidEntry.getChildAlias());
                        String parentPid = pidEntry.getObjectClassDefinition().getParentPID();
                        if (parentPid != null) {
                            parentMap.remove(parentPid);
                        }
                        childPidMap.remove(pidEntry.getPid());
                    }

                    if (pidEntry.getObjectClassDefinition().getObjectClass() != null) {
                        for (String service : pidEntry.getObjectClassDefinition().getObjectClass()) {
                            List<RegistryEntry> exposers = serviceToObjectClassMap.get(service);
                            if (exposers != null) {
                                exposers.remove(pidEntry);
                                if (exposers.isEmpty()) {
                                    serviceToObjectClassMap.remove(service);
                                }
                            }
                        }
                    }

                    for (ExtendedAttributeDefinition ad : pidEntry.getObjectClassDefinition().getAttributeMap().values()) {
                        if (ad.getService() != null) {
                            List<RegistryEntry> uses = serviceToServicesMap.get(ad.getService());
                            if (uses != null) {
                                uses.remove(pidEntry);
                                if (uses.isEmpty()) {
                                    serviceToServicesMap.remove(ad.getService());
                                }
                            }
                        }
                    }

                    pidEntry.unextend(this);
                    invalidEntryMap.remove(pid);
                }

            }
        }
        return removedRegistryEntries;
    }

    public interface EntryAction<T> {
        /**
         * Perform action on registry entry.
         *
         * @param registryEntry
         * @return true to continue, false to stop traversal
         */
        boolean entry(RegistryEntry registryEntry);

        T getResult();
    }

    public final static class RegistryEntry {

        private final String pid;
        private String rootPid;
        private final boolean factory;
        private final ExtendedObjectClassDefinition ocd;
        private final long bundleId;
        private final String bundleName;
        private boolean valid;

        /**
         * Map from name the attribute appears in least extended OCD to the attribute def in the _most refined/extended_ OCD it appears in via renames.
         * The key is used to put stuff into the CA config map: the id of the attribute def is found in server xml config elements.
         */
        private final Map<String, ExtendedAttributeDefinition> effectiveAttributes = new LinkedHashMap<String, ExtendedAttributeDefinition>();
        /**
         * set of attribute names as they appear in least extended OCD for required attributes, i.e. renames have been applied
         */
        private final Set<String> requiredAttributeNames = new HashSet<String>();

        /**
         * Directly extending registry entries.
         */
        private List<RegistryEntry> extenders;

        /**
         * list of pids that have an reference attribute pointing here (or to a supertype) or the parentPid of this entry or a supertype.
         */
        private List<PidReference> referencingPids;

        /**
         * PidReferences where this entry is the origin -- used to help clean up PidReferences on metatype removal
         */
        private List<PidReference> referencedPids;

        private RegistryEntry extendedRegistryEntry;

        /**
         *
         * @param pid
         * @param isFactory
         * @param bundle
         * @param ocd              an already verified to be internally consistent ObjectClassDefinition
         * @param metatypeRegistry
         */
        RegistryEntry(String pid, boolean isFactory, Bundle bundle, ExtendedObjectClassDefinition ocd, MetaTypeRegistry metatypeRegistry) {
            if (pid == null)
                throw new NullPointerException("pid");
            if (bundle == null)
                throw new NullPointerException("bundle");
            if (ocd == null)
                throw new NullPointerException("ocd");
            this.bundleId = bundle.getBundleId();
            this.bundleName = bundle.getSymbolicName();
            this.pid = pid;
            this.factory = isFactory;
            this.ocd = ocd;
            if (this.ocd.getExtends() != null) {
                RegistryEntry extendedEntry = metatypeRegistry.getRegistryEntry(this.ocd.getExtends());
                if (extendedEntry != null) {
                    complete(extendedEntry, metatypeRegistry);
                }
            } else {
                valid = true;
                rootPid = pid;
                computeEffectiveAttributes(this.ocd, effectiveAttributes, requiredAttributeNames, metatypeRegistry);
            }
        }

        /**
         * @param metatypeRegistry MetaTypeRegistry for lookups in error messages
         * @param extendedEntry    valid RegistryEntry that this one extends or refines.
         */
        public void complete(RegistryEntry extendedEntry, MetaTypeRegistry metatypeRegistry) {
            if (!extendedEntry.isFactory()) {
                Tr.error(tc, "error.superFactoryOnly", new Object[] { extendedEntry.getPid(), getPid() });
                return;
            }
            //TODO when is this valid?
            rootPid = extendedEntry.getRootPid();
            extendedRegistryEntry = extendedEntry;
            effectiveAttributes.putAll(extendedEntry.effectiveAttributes);
            requiredAttributeNames.addAll(extendedEntry.requiredAttributeNames);
            valid = computeEffectiveAttributes(this.ocd, effectiveAttributes, requiredAttributeNames, metatypeRegistry);
            if (valid) {
                valid = extendedEntry.addExtender(this, metatypeRegistry);
            }
        }

        /**
         * @param extender RegistryEntry that extends or refines this one.
         */
        private boolean addExtender(RegistryEntry extender, MetaTypeRegistry metatypeRegistry) {
            if (top().check(extender)) {
                if (extenders == null) {
                    extenders = new ArrayList<RegistryEntry>();
                }
                extenders.add(extender);
                return true;
            }
            return false;
        }

        private RegistryEntry top() {
            if (extendedRegistryEntry == null) {
                return this;
            }
            return extendedRegistryEntry.top();
        }

        /**
         * checks that the extendsAlas is unique among extenders.
         *
         * @param extender
         * @return
         */
        private boolean check(RegistryEntry extender) {
            if (extender.getExtendsAlias() == null) {
                return true;
            }
            if (extender.getExtendsAlias().equals(this.getExtendsAlias())) {
                Tr.error(tc, "error.extendsAlias.collision", new Object[] { extender.getPid(), getPid(), getExtendsAlias() });
                return false;
            }
            if (extenders != null) {
                for (RegistryEntry existing : extenders) {
                    if (!existing.check(extender)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * @param metaTypeRegistry
         */
        private void unextend(final MetaTypeRegistry metatypeRegistry) {
            String extendsPid = getExtends();
            if (extendsPid != null) {
                RegistryEntry extended = metatypeRegistry.getRegistryEntry(extendsPid);
                if (extended != null) {
                    extended.removeExtender(metatypeRegistry, this);
                }
            }
            traverseHierarchyWithRoot(new EntryAction<Void>() {

                @Override
                public boolean entry(RegistryEntry registryEntry) {
                    registryEntry.effectiveAttributes.clear();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "List of effective attributes cleared for {0}", registryEntry);
                    }

                    if (registryEntry.extenders != null) {
                        if (!metatypeRegistry.invalidEntryMap.containsKey(registryEntry.getPid())) {
                            metatypeRegistry.invalidEntryMap.put(registryEntry.getPid(), registryEntry.extenders);
                        }
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Setting extenders to null for registry entry {0}", registryEntry);
                        }
                        registryEntry.extenders = null;
                    }
                    return true;
                }

                @Override
                public Void getResult() {
                    return null;
                }

            });
        }

        private void removeExtender(MetaTypeRegistry metatypeRegistry, RegistryEntry extender) {
            Collection<RegistryEntry> extendersToCleanup = extenders;
            if (extenders == null) {
                extendersToCleanup = metatypeRegistry.invalidEntryMap.get(getPid());
            }
            extendersToCleanup.remove(extender);
            if (extendersToCleanup.isEmpty()) {
                if (extenders == null) {
                    metatypeRegistry.invalidEntryMap.remove(getPid());
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Setting extenders to null for extender {0}", extender);
                    }
                    extenders = null;
                }
            }
        }

        private synchronized void addReferencingEntry(PidReference ref) {
            if (ref.getReferencingEntry() == this) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Circular RegistryEntry reference detected: ", this);
                return;
            }
            if (referencingPids == null) {
                referencingPids = new CopyOnWriteArrayList<PidReference>();
                referencingPids.add(ref);
            } else if (!referencingPids.contains(ref)) {
                referencingPids.add(ref);
            }
        }

        /**
         * @param ref
         */
        public void addReferencedEntry(PidReference ref) {
            if (referencedPids == null) {
                referencedPids = new CopyOnWriteArrayList<PidReference>();
            }
            if (!referencedPids.contains(ref)) {
                referencedPids.add(ref);
            }

        }

        private synchronized void removeReferencingEntry(PidReference ref) {
            if (referencingPids != null) {
                referencingPids.remove(ref);
            }
        }

        synchronized List<PidReference> getReferencingEntries() {
            if (referencingPids == null) {
                return Collections.emptyList();
            }
            return referencingPids;
        }

        synchronized List<PidReference> getReferencedEntries() {
            if (referencedPids == null) {
                return Collections.emptyList();
            }

            return referencedPids;
        }

        private synchronized void removeAllReferences() {
            if (referencedPids != null)
                referencedPids.clear();

            if (referencingPids != null)
                referencingPids.clear();

        }

        /**
         * traverse the extends hierarchy not including this RegistryEntry
         *
         * @param action to perform on each RegistryEntry.
         */
        public <T> T traverseHierarchyPreOrder(EntryAction<T> action) {
            traverseHierarchy(action, false, true);
            return action.getResult();
        }

        public <T> T traverseHierarchyWithRootPreOrder(EntryAction<T> action) {
            traverseHierarchy(action, true, true);
            return action.getResult();
        }

        public <T> T traverseHierarchy(EntryAction<T> action) {
            traverseHierarchy(action, false, false);
            return action.getResult();
        }

        public <T> T traverseHierarchyWithRoot(EntryAction<T> action) {
            traverseHierarchy(action, true, false);
            return action.getResult();
        }

        private <T> boolean traverseHierarchy(EntryAction<T> action, boolean includeRoot, boolean pre) {
            if (pre && includeRoot) {
                if (!action.entry(this)) {
                    return false;
                }
            }
            if (extenders != null) {
                for (RegistryEntry extender : extenders) {
                    if (!extender.traverseHierarchy(action, true, pre)) {
                        return false;
                    }
                }
            }
            if (!pre && includeRoot) {
                return action.entry(this);
            }
            return true;
        }

        private boolean computeEffectiveAttributes(ExtendedObjectClassDefinition ocd, Map<String, ExtendedAttributeDefinition> map, Set<String> requiredAttributeNames,
                                                   MetaTypeRegistry metatypeRegistry) {
            Set<String> alreadyRenamed = new HashSet<String>();
            boolean valid = processAttributes(ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED), true, map, requiredAttributeNames, alreadyRenamed, metatypeRegistry);
            valid &= processAttributes(ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL), false, map, requiredAttributeNames, alreadyRenamed, metatypeRegistry);
            return valid;
        }

        private boolean processAttributes(AttributeDefinition[] attrDefs, boolean required, Map<String, ExtendedAttributeDefinition> map, Set<String> requiredAttributeNames,
                                          Set<String> alreadyRenamed, MetaTypeRegistry metatypeRegistry) {
            boolean valid = true;
            if (attrDefs != null) {
                for (AttributeDefinition attrDef : attrDefs) {
                    ExtendedAttributeDefinitionImpl extendedAttrDef = new ExtendedAttributeDefinitionImpl(attrDef);
                    String key = extendedAttrDef.getID();
                    String rename = extendedAttrDef.getRename();
                    ExtendedAttributeDefinition existing = null;
                    if (rename != null) {
                        existing = map.remove(rename);
//                        for (Map.Entry<String, ExtendedAttributeDefinition> entry : map.entrySet()) {
//                            if (rename.equals(entry.getValue().getID())) {
//                                existing = entry.getValue();
//                                key = entry.getKey();
//                                break;
//                            }
//                        }
                        if (existing == null) {
                            if (alreadyRenamed.contains(rename)) {
                                //tried to rename something that is already renamed in this ocd, error  CWWKG0068E
                                Tr.error(tc, "error.conflicting.rename.attribute", extendedAttrDef.getID(), rename, ocd.getID());
                            } else {
                                //tried to rename something that doesn't exist, error  CWWKG0067E
                                Tr.error(tc, "error.rename.attribute.missing", ocd.getID(), rename, extendedAttrDef.getID());
                            }
                            valid = false;
                            continue;
                        }
                        alreadyRenamed.add(rename);
                        //remove the existing from required attributes if it is present there, since it is now being overridden
                        requiredAttributeNames.remove(rename);
                    } else {
                        existing = map.get(extendedAttrDef.getID());
                    }
                    if (existing != null) {
                        if (existing.isFinal()) {
                            //issue errorerror.final.override=CWWKG0060E:
                            //It is not valid to override or rename attribute {0} for persisted identity {1} because it is declared final by the persisted identity {2}.
                            //find the RE that has the final attribute.
                            RegistryEntry re = this;
                            boolean found = false;
                            while (!found) {
                                if (re.getExtends() != null) {
                                    re = metatypeRegistry.getRegistryEntry(re.getExtends());
                                } else {
                                    throw new IllegalStateException("should not happen: " + re.toString());
                                }
                                AttributeDefinition[] ads = re.getObjectClassDefinition().getAttributeDefinitions(ObjectClassDefinition.ALL);
                                for (AttributeDefinition ad : ads) {
                                    if (ad.getID().equals(existing.getID())) {
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            Tr.error(tc, "error.final.override", new Object[] { extendedAttrDef.getID(), pid, re.getPid() }); //TODO Check this message comes out OK!
                            valid = false;
                            continue;
                        }
                        //remove the existing from required attributes if it is present there, since it is now being overridden
                        requiredAttributeNames.remove(existing.getID());
                    }
                    map.put(key, extendedAttrDef);
                    if (required) {
                        requiredAttributeNames.add(key);
                    }
                }
            }
            return valid;
        }

        public Map<String, ExtendedAttributeDefinition> getAttributeMap() {
            return effectiveAttributes;
        }

        public Collection<String> getRequiredAttributeNames() {
            return requiredAttributeNames;
        }

        public String getDefaultId() {
            ExtendedAttributeDefinition id = getAttributeMap().get("id");
            if (id != null) {
                String[] values = id.getDefaultValue();
                if (values == null) {
                    return null;
                } else if (values.length > 1) {
                    throw new IllegalStateException("The id attribute can not have multiple values");
                } else {
                    return values[0];
                }
            }

            return null;
        }

        /**
         * Returns the pid defined for the delegate/ocd this represents
         *
         * @return used to determine attributes etc.
         */
        @Trivial
        public String getPid() {
            return pid;
        }

        /**
         * Returns the pid used to create the configuration this represents. Never null; may be the same as getPid();
         *
         * @return the pid used to create configurations.
         */
        public String getRootPid() {
            return rootPid;
        }

        public String getAlias() {
            return ocd.getAlias();
        }

        public String getChildAlias() {
            return ocd.getChildAlias();
        }

        public String getExtendsAlias() {
            return ocd.getExtendsAlias();
        }

        public String getEffectiveAD(String ad) {
            String extendsAlias = getExtendsAlias();
            if (extendsAlias == null) {
                return null;
            }
            if (extendsAlias.length() == 0) {
                return ad;
            }
            if (extendsAlias.startsWith("!")) {
                return extendsAlias.substring(1);
            }
            return MessageFormat.format("{0}.{1}", ad, extendsAlias);
        }

        public ExtendedObjectClassDefinition getObjectClassDefinition() {
            return ocd;
        }

        public boolean isFactory() {
            return factory;
        }

        public boolean isSingleton() {
            return !factory;
        }

        /**
         * @return the bundleId
         */
        long getBundleId() {
            return bundleId;
        }

        String getBundleName() {
            return this.bundleName;
        }

        public String getExtends() {
            return ocd.getExtends();
        }

        public RegistryEntry getExtendedRegistryEntry() {
            return extendedRegistryEntry;
        }

        public boolean supportsExtensions() {
            return ocd.supportsExtensions() || (extendedRegistryEntry != null && extendedRegistryEntry.supportsExtensions());
        }

        //TODO is this transitive?
        boolean supportsHiddenExtensions() {
            return ocd.supportsHiddenExtensions();
        }

        boolean isValid() {
            return valid;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        @Trivial
        public String toString() {
            return super.toString() + "[" + pid + "]";
        }

    }

    /**
     * @param referenceingPid
     * @param childNodeName
     */
    RegistryEntry getRegistryEntry(RegistryEntry parentEntry, String childNodeName) {
        Map<String, RegistryEntry> parentMap = childAliasMap.get(childNodeName);
        if (parentMap == null) {
            parentMap = childPidMap.get(childNodeName);
        }
        if (parentMap == null) {
            return null;
        }

        while (parentEntry != null) {
            RegistryEntry childEntry = parentMap.get(parentEntry.getPid());
            if (childEntry != null) {
                return childEntry;
            }
            parentEntry = getRegistryEntry(parentEntry.getExtends());
        }
        return null;

    }

    // ---old api

    RegistryEntry getRegistryEntry(ConfigElement element) {
        if (element.getParent() == null || element.childAttributeName == null)
            return getRegistryEntryByPidOrAlias(element.getNodeName());

        Map<String, RegistryEntry> parentMap = childAliasMap.get(element.childAttributeName);
        if (parentMap == null)
            return null;

        return parentMap.get(element.getParent().getNodeName());
    }

    Map<String, ExtendedAttributeDefinition> getHierarchyCompleteAttributeMap(String factoryPid) {
        RegistryEntry entry = getRegistryEntry(factoryPid);
        if (entry != null) {
            return entry.getAttributeMap();
        }
        return null;
    }

    Integer getAttributeCardinality(String pid, String attributeID) {
        RegistryEntry ent = getRegistryEntryByPidOrAlias(pid);
        if (ent != null) {
            Map<String, ExtendedAttributeDefinition> attributeMap;
            attributeMap = ent.getAttributeMap();
            if (attributeMap != null) {
                if (attributeMap.containsKey(attributeID))
                    return attributeMap.get(attributeID).getCardinality();
                else {
                    // If it's a child-first nested element with ibm:final id, then it effectively behaves as cardinality 1
                    Map<String, RegistryEntry> map = childAliasMap.get(attributeID);
                    if (map != null) {
                        ent = map.get(pid);
                        if (ent != null) {
                            ExtendedObjectClassDefinition ocd = ent.getObjectClassDefinition();
                            if (ocd != null) {
                                attributeMap = ocd.getAttributeMap();
                                if (attributeMap != null) {
                                    ExtendedAttributeDefinition id = attributeMap.get("id");
                                    if (id.isFinal())
                                        return -1; // cardinality is negative for Vector, positive for array
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    String getAttributeName(String pid, String attributeID) {
        RegistryEntry ent = getRegistryEntryByPidOrAlias(pid);
        if (ent != null) {
            Map<String, ExtendedAttributeDefinition> attributeMap;
            attributeMap = ent.getAttributeMap();
            if (attributeMap != null && attributeMap.containsKey(attributeID))
                return attributeMap.get(attributeID).getName();
        }
        return null;
    }

    String getElementName(String pid) {
        RegistryEntry ent = getRegistryEntryByPidOrAlias(pid);
        if (ent != null) {
            ObjectClassDefinition ocd = ent.getObjectClassDefinition();
            if (ocd != null)
                return ocd.getName();
        }
        return null;
    }

    List<AttributeDefinition> getRequiredAttributesForHierarchy(String factoryPid) {
        RegistryEntry entry = getRegistryEntry(factoryPid);
        if (entry != null) {
            List<AttributeDefinition> ads = new ArrayList<AttributeDefinition>();
            Map<String, ExtendedAttributeDefinition> attributeMap = entry.getAttributeMap();
            for (String name : entry.getRequiredAttributeNames()) {
                ads.add(attributeMap.get(name));
            }
            return ads;
        }
        return null;
    }

    /**
     * @param referenceingPid
     * @param childNodeName
     */
    RegistryEntry getRegistryEntry(String parentPid, String childNodeName) {
        Map<String, RegistryEntry> parentMap = childAliasMap.get(childNodeName);
        if (parentMap == null)
            return null;

        return parentMap.get(parentPid);

    }

    void missingPid(String pid) throws ConfigUpdateException {
        for (Collection<RegistryEntry> entries : invalidEntryMap.values()) {
            for (RegistryEntry entry : entries) {
                if (entry.getPid().equals(pid)) {
                    String superPid = entry.getExtends();
                    if (getRegistryEntry(superPid) == null) {
                        //log the missing super error
                        Tr.error(tc, "error.missingSuper", new Object[] { pid, superPid });
                        //if failing on error we need to throw an exception here
                        if (ErrorHandler.INSTANCE.fail())
                            throw new ConfigUpdateException(Tr.formatMessage(tc, "error.missingSuper", new Object[] { pid, superPid }));
                    }
                    //TODO there are other reasons for invalid, should we have failed on them too?  bad renames....
                    break;
                }
            }
        }
    }

}
