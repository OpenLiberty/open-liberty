/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.delta.internal;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.annocache.util.Util_IdentitySetDelta;
import com.ibm.wsspi.annocache.util.Util_InternMap;
import com.ibm.wsspi.annocache.util.Util_PrintLogger;

public class UtilImpl_IdentitySetDelta implements Util_IdentitySetDelta {

    public static final String CLASS_NAME = UtilImpl_IdentitySetDelta.class.getSimpleName();

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public UtilImpl_IdentitySetDelta(boolean recordAdded, boolean recordRemoved, boolean recordStill) {
        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.added_f = (recordAdded ? new HashSet<String>() : null );
        this.removed_i = (recordRemoved ? new HashSet<String>() : null );
        this.still_f = (recordStill ? new HashSet<String>() : null );
    }

    // All elements of 'added' are from the domain of the final set.
    //
    // All elements of 'removed' are from the domain of the initial set.
    //
    // Currently, all elements of 'still' are from the final set, but,
    // there *could* be two still sets, one for each of the sets.
    // That would be potentially useful for keeping values from
    // the initial and final domains apart.

    protected final Set<String> added_f;
    protected final Set<String> removed_i;
    protected final Set<String> still_f;
    // protected final Set<String> still_i;

    @Override
    public Set<String> getAdded() {
        return ( (added_f == null) ? null : added_f );
    }

    @Override
    public boolean isNullAdded() {
        return ((added_f == null) || added_f.isEmpty());
    }

    @Override
    public Set<String> getRemoved() {
        return ( (removed_i == null) ? null : removed_i );
    }

    @Override
    public boolean isNullRemoved() {
        return ((removed_i == null) || removed_i.isEmpty());
    }

    @Override
    public Set<String> getStill() {
        return ( (still_f == null) ? null : still_f );
    }

    @Override
    public boolean isNullStill() {
        return ((still_f == null) || still_f.isEmpty());
    }

    @Override
    public boolean isNull() {
        return ( isNullAdded() && isNullRemoved() );
    }

    @Override
    public boolean isNull(boolean ignoreRemoved) {
        return ( isNullAdded() && (ignoreRemoved || isNullRemoved()) );
    }

    //

    @Override
    public void describe(String prefix, List<String> nonNull) {
        if ( !isNullAdded() ) {
            nonNull.add(prefix + " Added [ " + getAdded().size() + " ]");
        }
        if ( !isNullRemoved() ) {
            nonNull.add(prefix + " Removed [ " + getAdded().size() + " ]");
        }
    }

    //

    @Override
    public void subtract(Map<String, String> finalSet, Map<String, String> initialSet) {
        subtract(finalSet, null, initialSet, null);
    }

    @Override
    public void subtract(
        Map<String, String> finalSet, Util_InternMap finalDomain,
        Map<String, String> initialSet, Util_InternMap initialDomain) {

        if ( ((finalSet == null) || finalSet.isEmpty()) &&
             ((initialSet == null) || initialSet.isEmpty()) ) {
             // Nothing to do: Both sets are empty.

        } else if ( (finalSet == null) || finalSet.isEmpty() ) {
            // Everything in the initial set was removed.

            if ( removed_i != null ) {
                removed_i.addAll( initialSet.keySet() );
            }

        } else if ( (initialSet == null) || initialSet.isEmpty() ) {
            // Everything in the final set was added.

            if ( added_f != null ) {
                added_f.addAll( finalSet.keySet() );
            }

        } else {
            if ( initialDomain == finalDomain ) {
                // When the domains are the same, which includes the case
                // when both domains are null, the step of interning the
                // element values across domains is skipped.

                for ( String element_f : finalSet.keySet() ) {
                    if ( !initialSet.containsKey(element_f) ) {
                        if ( added_f != null ) {
                            added_f.add(element_f);
                        }
                    } else {
                        if ( still_f != null ) {
                            still_f.add(element_f);
                        }
                    }
                }

                for ( String element_i : initialSet.keySet() ) {
                    if ( !finalSet.containsKey(element_i) ) {
                        if ( removed_i != null ) {
                            removed_i.add(element_i);
                        }
                    } else {
                        // if ( still_i != null ) {
                        //     still_i.add(element_i);
                        // }
                    }
                }

            } else {
                // When the domains are different, element values must be
                // interned across domains before doing membership tests.

                for ( String element_f : finalSet.keySet() ) {
                    String element_i = initialDomain.intern(element_f, Util_InternMap.DO_NOT_FORCE);
                    // A final element that did not cross into the initial domain cannot
                    // be in the initial set.
                    if ( (element_i == null) || !initialSet.containsKey(element_i) ) {
                        if ( added_f != null ) {
                            added_f.add(element_f);
                        }
                    } else {
                        if ( still_f != null ) {
                            still_f.add(element_f);
                        }
                    }
                }

                for ( String element_i : initialSet.keySet() ) {
                    String element_f = finalDomain.intern(element_i, Util_InternMap.DO_NOT_FORCE);
                    // A initial element that did not cross into the final domain cannot
                    // be in the final set.
                    if ( (element_f == null) || !finalSet.containsKey(element_f) ) {
                        if ( removed_i != null ) {
                            removed_i.add(element_i);
                        }
                    } else {
                        // if ( still_i != null ) {
                        //     still_i.add(element_i);
                        // }
                    }
                }
            }
        }
    }

    //

    @Override
    public void log(Logger useLogger) {
        if ( !useLogger.isLoggable(Level.FINER)) {
            return;
        }

        log( new UtilImpl_PrintLogger(useLogger) );
    }

    @Override
    public void log(PrintWriter writer) {
        log( new UtilImpl_PrintLogger(writer) );
    }

    @Override
    public void log(Util_PrintLogger useLogger) {
        String methodName = "log";

        if ( isNull() ) {
            if ( still_f == null ) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "** UNCHANGED **");
            } else {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "** UNCHANGED [ " + still_f.size() + " ] **");
            }
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Added:");
        if ( added_f == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( added_f.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int addedNo = 0;
            for ( String addedElement : added_f ){
                if ( addedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ ... " + added_f.size() + " ]");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + addedNo + " ]  "  + addedElement);
                }
                addedNo++;
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Removed:");
        if ( removed_i == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( removed_i.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            int removedNo = 0;
            for ( String removedElement : removed_i ){
                if ( removedNo > 3 ) {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ ... " + removed_i.size() + " ]");
                    break;
                } else {
                    useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + removedNo + " ]  "  + removedElement);
                }
                removedNo++;
            }
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Still:");
        if ( still_f == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NOT RECORDED **");
        } else if ( still_f.isEmpty() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  ** NONE **");
        } else {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  [ " + still_f.size() + " ]");
            // for ( String stillElement : still ){
            //     useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  "  + stillElement);
            // }
        }
    }
}
