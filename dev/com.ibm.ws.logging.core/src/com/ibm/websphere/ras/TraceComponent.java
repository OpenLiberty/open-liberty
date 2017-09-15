/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ras;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ejs.ras.TrLevelConstants;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.ws.logging.internal.TraceSpecification.TraceElement;
import com.ibm.ws.logging.internal.WsLogger;

/**
 * A <code>TraceComponent</code> represents a single component registered for logging.
 * The scoping of the component is arbitrary in that it may represent a single class or
 * it could be shared by several classes.
 * <p>
 * A <code>TraceComponent</code> should be registered with Tr using a class to assist in finding
 * the associated ResourceBundle for messages. It may have an additional name and one
 * or more groups. The log level for any given component is controlled by a match in the
 * trace specification for the class (including package), name, or trace group.
 */

public class TraceComponent implements FFDCSelfIntrospectable {
    static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The non-null name either specified or defaulted to the class name.
     */
    private final String name;

    /**
     * The class that registered this TraceComponent, or null if unknown.
     */
    private final Class<?> aClass;

    /**
     * The non-null array of non-null group names. The array does not contain
     * duplicates but might be empty. The monitor for "this" must be held while
     * reading or writing this field.
     *
     * @see #uniquify(String[], boolean)
     */
    private String[] groups;

    /**
     * The resource bundle name, or null.
     */
    private String bundle;

    private volatile Logger logger = null;
    // When WebSphere installs its own LogManager & Logger the Logger needs to
    // be made aware of changes in the traceSpec so that the Logger can filter
    // LogRecords close to the originator.
    private TraceStateChangeListener trStateChangeListener = null;

    /**
     * The cumulative index into the arrays of {@link TrLevelConstants#traceLevels}.
     */
    protected int specTraceLevel;

    /**
     * The index into {@link TrLevelConstants#levels}.
     */
    private int fineLevel = TrLevelConstants.TRACE_LEVEL_OFF;

    /**
     * Bitset of enabled levels. Each bit corresponds to an index
     * into {@link TrLevelConstants#levels}, and a set bit indicates that the
     * level is enabled.
     */
    private int fineLevelsEnabled;

    private static TraceEnabledToken fineTraceEnabled = null;

    /**
     * Package-protected: not SPI.
     *
     * @param activeTraceSpec
     */
    static void setAnyTracingEnabled(boolean useFineTrace) {
        if (useFineTrace == true) {
            fineTraceEnabled = new TraceEnabledToken();
        } else {
            fineTraceEnabled = null;
        }
    }

    public static boolean isAnyTracingEnabled() {
        return (fineTraceEnabled != null);
    }

    protected TraceComponent(Class<?> aClass) {
        this(null, aClass, (String) null, null);
    }

    protected TraceComponent(String name) {
        this(name, null, (String) null, null);
    }

    protected TraceComponent(String name, Class<?> aClass) {
        this(name, aClass, (String) null, null);
    }

    protected TraceComponent(String name, Class<?> aClass, String group, String bundle) {
        this(name, aClass, group == null ? EMPTY_STRING_ARRAY : new String[] { group }, false, bundle);
    }

    protected TraceComponent(String name, Class<?> aClass, String[] groups, String bundle) {
        this(name, aClass, groups == null ? EMPTY_STRING_ARRAY : groups, true, bundle);
    }

    /**
     * @param groups non-null array of groups
     * @param cloneGroups true if groups must be cloned because it might be modified
     */
    TraceComponent(String name, Class<?> aClass, String[] groups, boolean cloneGroups, String bundle) {
        if (name == null) {
            if (aClass == null)
                throw new NullPointerException("Must declare a trace component with either a name, a class, or both");
            else
                name = aClass.getName();
        }

        this.name = name;
        this.aClass = aClass;
        this.groups = uniquify(groups, cloneGroups);
        this.bundle = bundle;
    }

    /**
     * Remove null and duplicate elements from an array.
     *
     * @param arr the input array
     * @param alwaysClone true if the input array should be copied even if it
     *            doesn't contain nulls or duplicates
     * @return the output array
     */
    static String[] uniquify(String[] arr, boolean alwaysClone) {
        int numUnique = uniquifyCountAndCopy(arr, null);
        if (numUnique == 0)
            return EMPTY_STRING_ARRAY;
        if (numUnique == arr.length)
            return alwaysClone ? arr.clone() : arr;

        String[] out = new String[numUnique];
        uniquifyCountAndCopy(arr, out);
        return out;
    }

    /**
     * Count the number of non-null non-duplicate elements in an array, and copy
     * those elements to an output array if provided.
     *
     * @param in the input array
     * @param out an output array, or null to count rather than copying
     * @return the number of non-null non-duplicate elements
     */
    private static int uniquifyCountAndCopy(String[] in, String[] out) {
        int count = 0;
        outer: for (int i = 0; i < in.length; i++) {
            if (in[i] != null) {
                for (int j = 0; j < i; j++)
                    if (in[i].equals(in[j]))
                        continue outer;
                if (out != null)
                    out[count] = in[i];
                count++;
            }
        }
        return count;
    }

    public Class<?> getTraceClass() {
        return this.aClass;
    }

    public synchronized Logger getLogger() {
        if (logger == null) {
            // set to the class associated w/ this trace component
            WsLogger.loggerRegistrationComponent.set(this);

            try {
                logger = Logger.getLogger(name, bundle);
                logger.setLevel(getLoggerLevel());
            } finally {
                WsLogger.loggerRegistrationComponent.set(null); // clear when done.
            }
        }

        return logger;
    }

    synchronized void addGroup(String group) {
        if (!contains(groups, group)) {
            String[] newGroups = Arrays.copyOf(groups, groups.length + 1);
            newGroups[groups.length] = group;
            groups = newGroups;

            // Reprocess the trace spec in case trace the new group causes a
            // level to become enabled/disabled.
            setTraceSpec(TrConfigurator.getTraceSpec());
        }
    }

    private static boolean contains(String[] haystack, String needle) {
        for (String value : haystack) {
            if (needle.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the active trace settings for this component based on the provided string.
     * Protected: Not an SPI method.
     *
     * @param ts TraceSpecification
     */
    @Deprecated
    protected void setTraceSpec(String s) {
        if (s != null) {
            TraceSpecification ts = new TraceSpecification(s, null, false);
            setTraceSpec(ts);
        }
    }

    /**
     * Update the active trace settings for this component based on the provided specification.
     * Package-protected: Not an SPI method.
     *
     * @param ts TraceSpecification
     */
    final synchronized void setTraceSpec(TraceSpecification ts) {
        // update active trace based on incoming trace specification
        if (updateTraceSpec(ts)) {
            // If a listener has been registered with this TraceComponent call
            // the logger to inform it of the change in traceSpec
            if (trStateChangeListener != null)
                trStateChangeListener.traceStateChanged();

            if (logger != null)
                logger.setLevel(getLoggerLevel());

            TrConfigurator.traceComponentUpdated(this);
        }
    }

    /**
     * Package-protected, not an SPI method
     *
     * @param name
     */
    final void setResourceBundleName(String name) {
        this.bundle = name;
    }

    public final String getResourceBundleName() {
        String rb = this.bundle;
        if (rb == null && logger != null)
            rb = logger.getResourceBundleName();
        return rb;
    }

    public synchronized final void setLoggerForCallback(TraceStateChangeListener listener) {
        this.trStateChangeListener = listener;
    }

    public final String getName() {
        return this.name;
    }

    public final boolean isDumpEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_DUMP)) != 0;
    }

    public final boolean isDebugEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_DEBUG)) != 0;
    }

    public final boolean isEntryEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_ENTRY_EXIT)) != 0;
    }

    public final boolean isEventEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_EVENT)) != 0;
    }

    public final boolean isDetailEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_DETAIL)) != 0;
    }

    public final boolean isConfigEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_CONFIG)) != 0;
    }

    public final boolean isInfoEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_INFO)) != 0;
    }

    public final boolean isAuditEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_AUDIT)) != 0;
    }

    public final boolean isWarningEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_WARNING)) != 0;
    }

    public final boolean isErrorEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_ERROR)) != 0;
    }

    public final boolean isFatalEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_FATAL)) != 0;
    }

    public final boolean isServiceEnabled() {
        return isAuditEnabled();
    }

    /*
     * Process a full trace specification of the form *=info:loggerX=fine and
     * set the corresponding trace flags for this trace component.
     */
    private boolean updateTraceSpec(TraceSpecification ts) {
        List<TraceElement> traceSpecs = ts.getSpecs();
        Integer minimumLevel = null;
        if (ts.isSensitiveTraceSuppressed()) {
            minimumLevel = findMinimumSafeLevel(ts.getSafeLevelsIndex());
        }

        int newFineLevel = fineLevel;
        int newFineLevelsEnabled = 0;
        int newSpecTraceLevel = TrLevelConstants.SPEC_TRACE_LEVEL_OFF;

        for (TraceElement spec : traceSpecs) {
            String clazz = spec.groupName;
            int traceElementFineLevel = spec.fineLevel;
            int specTraceLevel = spec.specTraceLevel;
            boolean setValue = spec.action;

            // Do we have a match with our package name?
            boolean process = false;

            if (clazz.endsWith("*")) // packages can end with wildcard
            {
                if (1 == clazz.length()) {
                    process = true;
                } else {
                    clazz = clazz.substring(0, clazz.length() - 1);
                    for (String group : groups) {
                        if (group.startsWith(clazz)) {
                            process = true;
                            break;
                        }
                    }
                    process = process || name.startsWith(clazz);
                }
            } else {
                // or can be a complete package or parent package
                // - look for a full or partial match with the TC package name
                int lastDot = name.lastIndexOf('.');
                if (lastDot > 0) {
                    String packageName = name.substring(0, lastDot);
                    if (packageName.startsWith(clazz))
                        process = true;
                }
                // groups may be class names (eg WsLogger impls)
                for (String group : groups) {
                    lastDot = group.lastIndexOf('.');
                    if (lastDot > 0) {
                        String packageName = group.substring(0, lastDot);
                        if (packageName.startsWith(clazz))
                            process = true;
                        break;
                    }
                }

                // could be a straight group name match
                for (String group : groups) {
                    if (group.equalsIgnoreCase(clazz)) {
                        process = true;
                        break;
                    }
                }
                process = process || name.equalsIgnoreCase(clazz);
            }

            if (process) {
                newFineLevel = traceElementFineLevel;
                newSpecTraceLevel = specTraceLevel;
                if (minimumLevel != null && newFineLevel < minimumLevel) {
                    newFineLevel = minimumLevel;
                }

                for (int level = newFineLevel; level < TrLevelConstants.TRACE_LEVEL_OFF; level++) {
                    if (setValue) {
                        newFineLevelsEnabled |= 1 << level;
                    } else {
                        newFineLevelsEnabled &= ~(1 << level);
                    }
                }

                // Indicate that the trace spec matched something
                spec.setMatched(true);
            }
        } // end for each spec

        boolean updated = false;
        if (newFineLevel != fineLevel) {
            fineLevel = newFineLevel;
            updated = true;
        }
        if (newFineLevelsEnabled != fineLevelsEnabled) {
            fineLevelsEnabled = newFineLevelsEnabled;
            updated = true;
        }
        if (newSpecTraceLevel != specTraceLevel) {
            specTraceLevel = newSpecTraceLevel;
            updated = true;
        }

        return updated;
    }

    /**
     * Search by name first and if not found, then search by groups.
     *
     * @param index
     * @return the minimum safe level to enable trace for, or null if not found.
     */
    private Integer findMinimumSafeLevel(PackageIndex<Integer> index) {
        if (index == null) {
            return null;
        }
        Integer minimumLevel = index.find(name);
        if (minimumLevel == null) {
            // Find by groups
            for (String group : groups) {
                minimumLevel = index.find(group);
                if (minimumLevel != null) {
                    break;
                }
            }
        }
        return minimumLevel;
    }

    public final Level getLoggerLevel() {
        if (this.fineLevel < TrLevelConstants.levels.length) {
            return TrLevelConstants.levels[this.fineLevel];
        } else {
            return Level.OFF;
        }
    }

    @Override
    public String[] introspectSelf() {
        return new String[] { toString() };
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[" + this.name
               + "," + this.aClass
               + "," + Arrays.toString(this.groups)
               + "," + this.bundle
               + "," + this.logger
               + "]";
    }
}

/**
 * A Dummy Object which will be used instead of a boolean one
 */
class TraceEnabledToken {
    TraceEnabledToken() {}
}