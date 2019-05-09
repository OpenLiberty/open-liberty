/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import com.ibm.ejs.ras.TrLevelConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex;

/**
 * TraceSpecification parses and holds the results of a trace specification string:
 * *=info=enabled:Channel=all=enabled
 * 
 */
public class TraceSpecification {

    /**
     * Lazy TraceComponent: don't initialize this unless/until we need to
     */
    private final static class TraceComponentHolder {
        static final TraceComponent instance = Tr.register(TraceSpecification.class);
    }

    private static Comparator<TraceElement> SPEC_COMPARATOR = new Comparator<TraceElement>() {

        @Override
        public int compare(TraceElement spec1, TraceElement spec2) {
            return spec1.groupName.compareTo(spec2.groupName);
        }
    };

    /**
     * TraceElement: holder of various bits of the trace spec to make them easier
     * to work with.
     */
    public final static class TraceElement {
        public final String groupName;
        public final int fineLevel;
        public final int specTraceLevel;
        public final String fullString;

        /** When this element is matched (groupName), trace at the specified level is enabled if this is true, or disabled if this is false. */
        public final boolean action;

        protected boolean matched = false;

        TraceElement(String clazz, int fineLevel, int traceLevel, boolean enableValue, String fullString) {
            this.groupName = clazz;
            this.fineLevel = fineLevel;
            this.specTraceLevel = traceLevel;
            this.action = enableValue;
            this.fullString = fullString;
        }

        public boolean getMatched() {
            return matched;
        }

        public void setMatched(boolean matched) {
            this.matched = matched;
        }

        public String toDisplayString() {
            return fullString.replace("=enabled", "");
        }

        @Override
        public String toString() {
            return fullString;
        }
    }

    /**
     * TraceSpecificationException: holds information about a bad trace spec
     * (bad enabled/disabled flag or unknown trace level), and allows exceptions
     * to be chained (rather than nested).
     */
    public static class TraceSpecificationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        final String msgKey;
        final String badElement;
        final String fullSpecification;

        /**
         * A link to the previous exception: this is not the same as a caused by.
         * Allows chaining of several errors
         */
        TraceSpecificationException previousException;

        public TraceSpecificationException(String message, String msgKey, String badElement, String fullSpecification) {
            super(message);
            this.msgKey = msgKey;
            this.badElement = badElement;
            this.fullSpecification = fullSpecification;
        }

        @Override
        public String getLocalizedMessage() {
            return Tr.formatMessage(TraceComponentHolder.instance, msgKey, badElement, fullSpecification);
        }

        public final TraceSpecificationException getPreviousException() {
            return previousException;
        }

        protected final void setPreviousException(TraceSpecificationException prevException) {
            this.previousException = prevException;
        }

        public void warning(boolean delegateInitialized) {
            if (delegateInitialized)
                Tr.warning(TraceComponentHolder.instance, msgKey, badElement, fullSpecification);
            else
                System.err.println(getLocalizedMessage());
        }
    }

    final List<TraceElement> specs;
    final boolean fineTraceEnabled;

    TraceSpecificationException ex = null;
    private PackageIndex<Integer> safeLevelsIndex = null;
    private boolean suppressSensitiveTrace = false;

    /**
     * Create new TraceSpecification from the provided string. The string is
     * parsed into it's internal parts, consumers can then work with the
     * parsed/validated elements instead of tokenizing every time
     * 
     * @param s
     * @param safeLevelIndex
     * @param suppressSensitiveTrace
     */
    public TraceSpecification(String s, PackageIndex<Integer> safeLevelIndex, boolean suppressSensitiveTrace) {
        StringTokenizer specString = new StringTokenizer(s, ":");
        this.safeLevelsIndex = safeLevelIndex;
        this.suppressSensitiveTrace = suppressSensitiveTrace;

        List<TraceElement> newSpec = new ArrayList<TraceElement>();
        boolean finerTrace = false;

        /*
         * As per defect 55541 we need to make sure that there is a minimum default level of trace set
         * to *=info=enabled but we don't want to override a user setting for the "*"
         * clazz so check if the user has set this
         */
        boolean hasUserSetDefaultLevel = false;
        while (specString.hasMoreTokens()) {
            String clazz = "*"; /* defaults */
            String level = "info";
            String setting = TrLevelConstants.TRACE_ENABLED;

            StringTokenizer specElement = new StringTokenizer(specString.nextToken(), "=");

            if (specElement.hasMoreTokens()) {
                // Get the class/package specification to trace
                clazz = specElement.nextToken().trim();

                // If clazz is * then this is the default base level the user wishes to use so we won't override it
                if ("*".equals(clazz)) {
                    hasUserSetDefaultLevel = true;
                }

                if (specElement.hasMoreTokens()) {
                    // Get the trace level required value
                    level = specElement.nextToken().trim();

                    if (specElement.hasMoreTokens()) {
                        // Get the trace setting value
                        setting = specElement.nextToken().trim();
                    }
                }
            } else {
                // No clazz setting means that the user has supplied a default because we use * as the default when it is missing
                hasUserSetDefaultLevel = true;
            }

            finerTrace |= addSpecToList(newSpec, s, clazz, level, setting);
        }

        if (!hasUserSetDefaultLevel || newSpec.isEmpty()) {
            // The user has not supplied a default level so we will...
            finerTrace |= addSpecToList(newSpec, s, "*", "info", TrLevelConstants.TRACE_ENABLED);
        }

        Collections.sort(newSpec, SPEC_COMPARATOR);
        specs = Collections.unmodifiableList(newSpec);

        fineTraceEnabled = finerTrace;
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        for (TraceElement spec : specs) {
            sb.append(spec.toDisplayString()).append(':');
        }
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * This method will create a new String array for the supplied spec and add it to the <code>specsToAddTo</code> list. It will
     * only do this if it can find the right trace level for the supplied <code>level</code>.
     * 
     * @param updatedSpecList The list to add this spec setting to, may be <code>null</code> which will cause the list to be created
     * @param specString The original spec string that will be used for logging messaged
     * @param clazz The class that this spec setting applies to
     * @param level The level to set the logging to
     * @param enableString The setting for this spec, must be either {@link TrLevelConstants#TRACE_ENABLED} or {@link TrLevelConstants#TRACE_DISABLED}
     * @return true if a trace level lower than INFO was enabled (e.g. FINE, FINER, debug, entry )
     */
    private boolean addSpecToList(List<TraceElement> updatedSpecList, final String specString, final String clazz, final String level, final String enableString) {
        TraceSpecificationException tex = null;

        boolean enable = true;
        final boolean enableSetting;

        final String fullString = clazz + "=" + level + "=" + enableString;

        String setLower = enableString.trim().toLowerCase();
        if (setLower.equals(TrLevelConstants.TRACE_ENABLED) || setLower.equals(TrLevelConstants.TRACE_ON)) {
            enableSetting = true;
        } else if (setLower.equals(TrLevelConstants.TRACE_DISABLED) || setLower.equals(TrLevelConstants.TRACE_OFF)) {
            enableSetting = false;
            enable = false;
        } else {
            tex = new TraceSpecificationException("Unknown trace setting, must be either 'enabled' or 'disabled'",
                            "TRACE_STRING_BAD_ACTION",
                            enableString,
                            fullString);
            enableSetting = true;
            tex.setPreviousException(ex);
            ex = tex;
        }

        int found = -1;
        int traceLevel = 0;
        int traceLevelCount = 0;

        for (int i = 0; i < TrLevelConstants.traceLevels.length; i++) {
            final String[] traceLevelsRow = TrLevelConstants.traceLevels[i];
            for (int j = 0; j < traceLevelsRow.length; j++) {
                if (level.equalsIgnoreCase(traceLevelsRow[j])) {
                    found = i;
                    traceLevel = traceLevelCount;
                    break;
                }
                traceLevelCount++;
            }
            if (found >= 0) {
                break;
            }
        }

        if (found < 0) {
            tex = new TraceSpecificationException("Unknown trace level", "TRACE_STRING_BAD_LEVEL",
                            level,
                            fullString);
            tex.setPreviousException(ex);
            ex = tex;
        } else if (tex == null) {
            // Only add the specification string to the list if there was not an exception
            TraceElement spec = new TraceElement(clazz, found, traceLevel, enableSetting, fullString);
            updatedSpecList.add(spec);

            // If found (as a Logger level) is < INFO, then we have some kind of 
            // detailed trace enabled for this trace spec.
            return enable && found >= 0 && TrLevelConstants.levels[found].intValue() < Level.INFO.intValue();
        }

        return false;
    }

    public TraceSpecificationException getExceptions() {
        return ex;
    }

    /**
     * @return list of TraceElements that describe the enabled or
     *         disabled trace groups and packages.
     */
    public final List<TraceElement> getSpecs() {
        return specs;
    }

    /**
     * @return
     */
    public boolean isAnyTraceEnabled() {
        return fineTraceEnabled;
    }

    /**
     * @return
     */
    public static TraceComponent getTc() {
        return TraceComponentHolder.instance;
    }

    public void warnUnmatchedSpecs() {
        StringBuilder sb = new StringBuilder();
        for (TraceElement spec : specs) {
            if (!spec.getMatched()) {
                sb.append(spec.toDisplayString()).append(':');
            }
        }

        if (sb.length() > 0) {
            Tr.info(getTc(), "UNKNOWN_TRACE_SPEC", sb.substring(0, sb.length() - 1));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (fineTraceEnabled ? 1231 : 1237);
        result = prime * result + ((specs == null) ? 0 : specs.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TraceSpecification other = (TraceSpecification) obj;
        if (fineTraceEnabled != other.fineTraceEnabled)
            return false;
        if (specs == null) {
            if (other.specs != null)
                return false;
        } else {
            if (other.specs == null)
                return false;
            if (specs.size() != other.specs.size())
                return false;
            String mySpecs = specs.toString();
            String oSpecs = other.specs.toString();
            if (!mySpecs.equals(oSpecs))
                return false;
            if (suppressSensitiveTrace != other.suppressSensitiveTrace)
                return false;
        }

        return true;
    }

    /**
     * @return suppressSensitiveTrace
     */
    public boolean isSensitiveTraceSuppressed() {
        return suppressSensitiveTrace;
    }

    /**
     * @return the package index.
     */
    public PackageIndex<Integer> getSafeLevelsIndex() {
        return safeLevelsIndex;
    }
}
