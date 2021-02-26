/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.util.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_Factory;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class UtilImpl_BidirectionalMap implements Util_BidirectionalMap {
    private static final TraceComponent tc = Tr.register(UtilImpl_BidirectionalMap.class);
    public static final String CLASS_NAME = UtilImpl_BidirectionalMap.class.getName();

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    protected UtilImpl_BidirectionalMap(Util_Factory factory,
                                        String holderTag, String heldTag,
                                        Util_InternMap holderInternMap,
                                        Util_InternMap heldInternMap) {
        this(factory, holderTag, heldTag, holderInternMap, heldInternMap, Util_BidirectionalMap.IS_ENABLED);
    }

    protected UtilImpl_BidirectionalMap(Util_Factory factory,
                                        String holderTag, String heldTag,
                                        Util_InternMap holderInternMap,
                                        Util_InternMap heldInternMap,
                                        boolean isEnabled) {
        super();

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this) +
                        "(" + holderTag + " : " + heldTag + ", enabled='" + (isEnabled ? "true" : "false") + "')";

        this.factory = factory;

        this.holderTag = holderTag;
        this.heldTag = heldTag;

        this.holderInternMap = holderInternMap;
        this.heldInternMap = heldInternMap;

        this.isEnabled = isEnabled;

        if (this.isEnabled) {
            this.i_holderToHeldMap = new IdentityHashMap<String, Set<String>>();
        } else {
            this.i_holderToHeldMap = null;
        }

        if (this.isEnabled) {
            this.i_heldToHoldersMap = new IdentityHashMap<String, Set<String>>();
        } else {
            this.i_heldToHoldersMap = null;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ]", this.hashText));
        }
    }

    //

    protected final Util_Factory factory;

    @Override
    @Trivial
    public Util_Factory getFactory() {
        return factory;
    }

    //

    protected final String holderTag;

    @Override
    @Trivial
    public String getHolderTag() {
        return holderTag;
    }

    protected String heldTag;

    @Override
    @Trivial
    public String getHeldTag() {
        return heldTag;
    }

    //

    protected final Util_InternMap holderInternMap;

    @Override
    public Util_InternMap getHolderInternMap() {
        return holderInternMap;
    }

    @Override
    public boolean containsHolder(String holderName) {
        return (getIsEnabled() && holderInternMap.contains(holderName));
    }

    protected String internHolder(String name, boolean doForce) {
        return holderInternMap.intern(name, doForce);
    }

    protected Util_InternMap heldInternMap;

    @Override
    public Util_InternMap getHeldInternMap() {
        return heldInternMap;
    }

    @Override
    public boolean containsHeld(String heldName) {
        return (getIsEnabled() && heldInternMap.contains(heldName));
    }

    protected String internHeld(String name, boolean doForce) {
        return heldInternMap.intern(name, doForce);
    }

    //

    protected final boolean isEnabled;

    @Override
    public boolean getIsEnabled() {
        return isEnabled;
    }

    //

    // Implementation intention:
    //
    // className -> Set<annotationName> : tell what annotations a class has
    // annotationName -> Set<className> : tell what classes have an annotation

    protected Map<String, Set<String>> i_holderToHeldMap;

    @Override
    @Trivial
    public Set<String> getHolderSet() {
        return (getIsEnabled() ? i_holderToHeldMap.keySet() : Collections.<String>emptySet());
    }

    //

    protected Map<String, Set<String>> i_heldToHoldersMap;

    @Override
    @Trivial
    public Set<String> getHeldSet() {
        return (getIsEnabled() ? i_heldToHoldersMap.keySet() : Collections.<String>emptySet());
    }

    //

    @Override
    public boolean holds(String holderName, String heldName) {
        if (!getIsEnabled()) {
            return false;
        }

        String i_holderName = internHolder(holderName, Util_InternMap.DO_NOT_FORCE);
        if (i_holderName == null) {
            return false;
        }

        String i_heldName = internHeld(heldName, Util_InternMap.DO_NOT_FORCE);
        if (i_heldName == null) {
            return false;
        }

        // Symmetric: Go through the holder map.  The held map would work just as well.

        Set<String> i_held = i_holderToHeldMap.get(i_holderName);

        return ((i_held == null) ? false : i_held.contains(i_heldName));

        // Note (*):
        //
        // The held map can be null, in case the holder map is shared
        // (which is the case for the annotation targets maps, which
        // use a single class intern map for all of the package, class,
        // class method, and class field maps.
        //
        // Similarly, if the lookup was through the held map, the holders
        // mapping could be null.
    }

    //

    @Override
    public Set<String> selectHeldOf(String holderName) {
        if (!getIsEnabled()) {
            return Collections.emptySet();
        }

        String i_holderName = internHolder(holderName, Util_InternMap.DO_NOT_FORCE);
        if (i_holderName == null) {
            return Collections.emptySet();
        }

        Set<String> i_held = i_holderToHeldMap.get(i_holderName);
        if (i_held == null) { // See the note (*), above.
            return Collections.emptySet();
        }

        return i_held;
    }

    @Override
    public Set<String> selectHoldersOf(String heldName) {
        if (!getIsEnabled()) {
            return Collections.emptySet();
        }

        String i_heldName = internHeld(heldName, Util_InternMap.DO_NOT_FORCE);

        if (i_heldName == null) {
            return Collections.emptySet();
        }

        Set<String> i_holders = i_heldToHoldersMap.get(i_heldName);

        if (i_holders == null) { // See the note (*), above.
            return Collections.emptySet();
        }

        return i_holders;
    }

    //

    // TODO: Check 'isEnabled' and bounce?
    //       Or rely on the caller to know to make no store calls?

    public boolean record(String holderName, String heldName) {
        return i_record(internHolder(holderName, Util_InternMap.DO_FORCE),
                        internHeld(heldName, Util_InternMap.DO_FORCE));
    }

    public boolean i_record(String i_holderName, String i_heldName) {

        boolean addedHeldToHolder = i_recordHolderToHeld(i_holderName, i_heldName);
        boolean addedHolderToHeld = i_recordHeldToHolder(i_holderName, i_heldName);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Holder [ {1} ] Held [ {2} ] [ {3} ]",
                                              getHashText(), i_holderName, i_heldName, Boolean.valueOf(addedHeldToHolder)));
        }

        if (addedHeldToHolder != addedHolderToHeld) {
            //  CWWKC0057W
            Tr.warning(tc, "ANNO_UTIL_MAPPING_INCONSISTENCY", getHashText(),
                       i_holderName, i_heldName,
                       Boolean.valueOf(addedHeldToHolder),
                       Boolean.valueOf(addedHolderToHeld));
        }

        return addedHeldToHolder;
    }

    protected boolean i_recordHolderToHeld(String i_holderName, String i_heldName) {
        Set<String> i_held = i_recordHolder(i_holderName);
        return i_held.add(i_heldName);
    }

    protected Set<String> i_recordHolder(String i_holderName) {

        Set<String> i_held = i_holderToHeldMap.get(i_holderName);
        if (i_held == null) {
            i_held = factory.createIdentityStringSet();
            i_holderToHeldMap.put(i_holderName, i_held);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Holder [ {1} ] Added",
                                                  getHashText(), i_holderName));
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Holder [ {1} ] Already present",
                                                  getHashText(), i_holderName));
            }
        }

        return i_held;
    }

    protected boolean i_recordHeldToHolder(String i_holderName, String i_heldName) {
        Set<String> i_holders = i_recordHeld(i_heldName);
        return i_holders.add(i_holderName);
    }

    protected Set<String> i_recordHeld(String i_heldName) {
        Set<String> i_holders = i_heldToHoldersMap.get(i_heldName);
        if (i_holders == null) {
            i_holders = factory.createIdentityStringSet();
            i_heldToHoldersMap.put(i_heldName, i_holders);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Held [ {1} ] Added",
                                                  getHashText(), i_heldName));
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Held [ {1} ] Already present",
                                                  getHashText(), i_heldName));
            }
        }

        return i_holders;
    }

    //

    @Override
    @Trivial
    public void logState() {
        TraceComponent stateLogger = AnnotationServiceImpl_Logging.stateLogger;

        if (stateLogger.isDebugEnabled()) {
            log(stateLogger);
        }
    }

    @Override
    @Trivial
    public void log(TraceComponent useTc) {

        Tr.debug(useTc, MessageFormat.format("BEGIN STATE [ {0} ]", getHashText()));

        Tr.debug(useTc, MessageFormat.format("  Is Enabled [ {0} ]", Boolean.valueOf(getIsEnabled())));
        Tr.debug(useTc, MessageFormat.format("  Holder Tag [ {0} ]", getHolderTag()));
        Tr.debug(useTc, MessageFormat.format("  Held Tag   [ {0} ]", getHeldTag()));

        logHolderMap(useTc);
        logHeldMap(useTc);

        logInternMaps(useTc);

        Tr.debug(useTc, MessageFormat.format("END STATE [ {0} ]", getHashText()));
    }

    @Trivial
    public void logHolderMap(TraceComponent logger) {

        if (!getIsEnabled()) {
            Tr.debug(logger, "Holder-to-held Map: NULL (disabled)");
            return;
        }

        Tr.debug(logger, "Holder-to-held Map: BEGIN");

        for (Map.Entry<String, Set<String>> i_holderEntry : i_holderToHeldMap.entrySet()) {
            String i_holderName = i_holderEntry.getKey();
            Set<String> i_held = i_holderEntry.getValue();

            Tr.debug(logger, MessageFormat.format("  Holder [ {0} ] Held [ {1} ]", i_holderName, i_held));
        }

        Tr.debug(logger, "Holder-to-held Map: END");
    }

    @Trivial
    public void logHeldMap(TraceComponent useTc) {

        if (!getIsEnabled()) {
            Tr.debug(useTc, "Held-to-holder Map: NULL (disabled)");
            return;
        }

        Tr.debug(useTc, "Held-to-holder Map: BEGIN");

        for (Map.Entry<String, Set<String>> i_heldEntry : i_heldToHoldersMap.entrySet()) {
            String i_heldName = i_heldEntry.getKey();
            Set<String> i_holders = i_heldEntry.getValue();

            Tr.debug(useTc, MessageFormat.format("  Held [ {0} ] Holders [ {1} ]", i_heldName, i_holders));
        }

        Tr.debug(useTc, "Held-to-holder Map: END");
    }

    @Trivial
    public void logInternMaps(TraceComponent logger) {

        Tr.debug(logger, "Intern Maps: BEGIN");

        Util_InternMap useHolderMap = getHolderInternMap();
        if (useHolderMap == null) {
            Tr.debug(logger, "  Null holder intern map");
        } else {
            useHolderMap.log(logger);
        }

        Util_InternMap useHeldMap = getHeldInternMap();
        if (useHeldMap == null) {
            Tr.debug(logger, "  Null held intern map");
        } else {
            useHeldMap.log(logger);
        }

        Tr.debug(logger, "Intern Maps: END");
    }
}
