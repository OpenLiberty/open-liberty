/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

//@formatter:off
/**
 * Feature resolution engine.
 *
 * Two modes are supported: Feature resolution as a part of server startup, and
 * feature resolution for feature installation.
 *
 * Feature resolution operates on a pre-specified list of features (requested
 * features). Resolution of these features means locating dependency features
 * which are needed by the requested features, and means locating auto-features
 * which are satisfied by the requested and located dependency features. These two
 * steps are intermixed, with new requested and dependency features possibly
 * determining new auto-features, and with new auto-features possibly determining
 * new dependency features.
 *
 * When locating dependency features and auto-features, feature selection selects
 * particular feature versions. Generally, when selecting multiple features and
 * feature versions, only particular combinations of feature versions will be
 * compatible with each other. The consequence is that feature resolution must
 * perform two types of searches: First, feature resolution must walk the graph
 * of feature dependencies. Second, feature resolution must process feature version
 * combinations.
 *
 * The resolver implementation uses backtracking to handle the two types of
 * searches. When selecting a feature which has multiple versions, the several
 * possible versions of the feature are tried while growing a stack of the
 * current features and feature versions. If a conflict is detected the
 * feature version is discarded, and the next candidate feature version is
 * attempted.
 *
 * The order in which dependency features are located impacts what feature versions
 * are selected. For maximal consistency, features resolution is strictly ordered:
 * Particular feature versions are given priority by the order in which they
 * are processed, which is determined by the order in which version compatibility
 * is specified in feature definitions. This has two consequences: First,
 * feature definitions must be carefully expressed to so put preferred versions
 * earlier in the feature definition. Second, feature resolution must preserve
 * and use the version ordering when selecting feature versions.
 *
 * The selection algorithm is computational expensive, with known performance
 * problems when many feature versions are available. Poor performance is
 * particularly a problem when processing versionless features.
 */
public class FeatureResolverImpl implements FeatureResolver {
    // TODO: This strange initialization of 'tc' seems to be to
    //       avoid interacting with trace injection.  Why is this necessary?

    private static final Object tc;

    static {
        Object temp;
        try {
            temp = Tr.register(FeatureResolverImpl.class,
                               com.ibm.ws.kernel.feature.internal.ProvisionerConstants.TR_GROUP,
                               com.ibm.ws.kernel.feature.internal.ProvisionerConstants.NLS_PROPS);
        } catch (Throwable t) {
            temp = null;
        }
        tc = temp;
    }

    @Trivial
    protected static final void info(String methodName, String message, Object... parms) {
        Tr.info((TraceComponent) tc, "FeatureResolver." + methodName + ": " + message, parms);
    }

    @Trivial
    protected static final void trace(String methodName, String message, Object... parms) {
        if (isTraceEnabled()) {
            rawTrace(methodName, message, parms);
        }
    }

    @Trivial
    protected static final void trace(String message, Object... parms) {
        if (isTraceEnabled()) {
            rawTrace(message, parms);
        }
    }

    @Trivial
    protected static final boolean isTraceEnabled() {
        if (tc == null) {
            return false;
        }
        if (!TraceComponent.isAnyTracingEnabled()) {
            return false;
        }
        return (((TraceComponent) tc).isDebugEnabled());
    }

    @Trivial
    protected static final void rawTrace(String message, Object... parms) {
        Tr.debug((TraceComponent) tc, message, parms);
    }

    @Trivial
    protected static final void rawTrace(String methodName, String message, Object... parms) {
        Tr.debug((TraceComponent) tc, "FeatureResolver." + methodName + ": " + message, parms);
    }

    @Trivial
    protected static final void error(String message, Object... parms) {
        Tr.error((TraceComponent) tc, message, parms);
    }

    //

    /**
     * Verify that a feature constituent uses a symbolic name, not a
     * short name.  Throw an {@link IllegalArgumentException} if the
     * symbolic name was not used.
     *
     * @param constituentDef The constituent feature definition.
     * @param actualName The symbolic name used by the constituent.
     * @param includingName The name of the feature which has the constituent.
     *
     * TODO: This should be handled by feature validation elsewhere.
     */
    protected static void validateConstituent(ProvisioningFeatureDefinition constituentDef,
                                              String actualName,
                                              String includingName) {

        String expectedIncludedName = constituentDef.getSymbolicName();

        if ( !actualName.equals(expectedIncludedName) ) {
            throw new IllegalArgumentException("Feature [ " + includingName + " ]" +
                                               " has a constituent which uses a feature short name [ " + actualName + " ]" +
                                               " instead of a feature symbolic name [ " + expectedIncludedName + " ]");
        }
    }

    protected static Collection<FeatureResource> getConstituents(ProvisioningFeatureDefinition featureDef) {
        return featureDef.getConstituents(SubsystemContentType.FEATURE_TYPE);
    }

    static class ProvisioningResource {
        public final FeatureResource resource;

        public final String symbolicName;
        public final ProvisioningFeatureDefinition featureDef;

        public ProvisioningResource(FeatureResource resource,
                                    String symbolicName,
                                    ProvisioningFeatureDefinition featureDef) {
            this.resource = resource;

            this.symbolicName = symbolicName;
            this.featureDef = featureDef;
        }

        @Override
        public boolean equals(Object other) {
            if ( other == null ) {
                return false;
            } else if ( !(other instanceof ProvisioningResource) ) {
                return false;
            } else {
                return this.symbolicName.equals(((ProvisioningResource) other).symbolicName);
            }
        }

        @Override
        public int hashCode() {
            return symbolicName.hashCode();
        }
    }

    /** Simple class synonym. */
    static class OrderedProvisioningResources extends LinkedHashMap<String, ProvisioningResource> {
        private static final long serialVersionUID = 1L;

        public OrderedProvisioningResources() {
            super();
        }

        public OrderedProvisioningResources(int size) {
            super(size);
        }

        public OrderedProvisioningResources(OrderedProvisioningResources other) {
            this(other.size());
            putAll(other);
        }

        public OrderedProvisioningResources copy() {
            return new OrderedProvisioningResources(this);
        }
    }

    /**
     * Tell if a child feature is accessible from a parent feature.
     *
     * A child feature is accessible only if the child feature is *not*
     * versionless, is *not* private, and has the same bundle repository
     * type as the parent feature.
     *
     * See {@link #isVersionless(String)},
     * {@link ProvisioningFeatureDefinition#getVisibility()}, and
     * {@link ProvisioningFeatureDefinition#getBundleRepositoryType()}.
     *
     * @param parentDef A parent feature definition.
     * @param childDef A child feature definition.
     *
     * @return True or false telling if the child feature is accessible from
     *     the parent feature.
     */
    protected static boolean isAccessible(ProvisioningFeatureDefinition parentDef,
                                          ProvisioningFeatureDefinition childDef) {

        if ( isVersionless(childDef.getFeatureName()) ) {
            return false;
        } else if ( childDef.getVisibility() == Visibility.PRIVATE ) {
            return false;
        } else {
            return ( parentDef.getBundleRepositoryType().equals(childDef.getBundleRepositoryType()) );
        }
    }

    //

    protected static final boolean isBeta = Boolean.valueOf(System.getProperty("com.ibm.ws.beta.edition"));

    static {
        if (isBeta) {
            trace("Beta mode detected: Versionless feature support enabled.");
        }
    }

    //

    private static final String COLLECT_TIMING_PROPERTY_NAME = "feature.resolver.timing";
    private static final String collectTimingPropertyValue = System.getProperty(COLLECT_TIMING_PROPERTY_NAME);
    protected static final boolean collectTiming = ((collectTimingPropertyValue == null) ? false : Boolean.parseBoolean(collectTimingPropertyValue));

    static {
        if (collectTiming) {
            info("<static>", "Timing data requested");
        }
    }

    protected static long maybeGetTime() {
        return (collectTiming ? System.nanoTime() : -1L);
    }

    protected static long maybeDisplayTime(long startTime, String methodName, String message) {
        return (collectTiming ? displayTime(startTime, methodName, message) : -1L);
    }

    protected static long displayTime(long lastTimeNs, String methodName, String message) {
        long nextTimeNs = System.nanoTime();
        info(methodName, "[ +" + formatNSAsMS(nextTimeNs - lastTimeNs) + " (ms) ] " + message);
        return nextTimeNs;
    }

    protected static String formatNSAsMS(long timeNs) {
        return String.format("%8d", Long.valueOf(timeNs / 1000));
    }

    protected static String format(long num) {
        return String.format("%8d", Long.valueOf(num));
    }

    /**
     * Merge two lists.
     *
     * The result is always independent of the argument lists
     * and is always modifiable.
     *
     * Elements of the first list are placed before the elements
     * of the second list.  The original ordering of both argument
     * lists is preserved.
     *
     * @param <T> The type of elements of the lists.
     *
     * @param list0 The first list which is to be merged.
     * @param list1 The second list which is to be merged.
     *
     * @return The merger of the two lists.
     */
    @Trivial
    protected static <T> List<T> copyMerge(List<T> list0, List<T> list1) {
        int size0 = ((list0 == null) ? 0 : list0.size());
        int size1 = ((list1 == null) ? 0 : list1.size());
        List<T> merged = new ArrayList<>(size0 + size1);
        if ( size0 > 0 ) {
            merged.addAll(list0);
        }
        if ( size1 > 0 ) {
            merged.addAll(list1);
        }
        return merged;
    }

    /**
     * Merge two lists.
     *
     * The result is independent of the argument lists only if
     * neither list is null or empty.
     *
     * Elements of the first list are placed before the elements
     * of the second list.  The original ordering of both argument
     * lists is preserved.
     *
     * @param <T> The type of elements of the lists.
     *
     * @param list0 The first list which is to be merged.
     * @param list1 The second list which is to be merged.
     *
     * @return The merger of the two lists.
     */
    @Trivial
    protected static <T> List<T> merge(List<T> list0, List<T> list1) {
        if ( (list0 == null) || list0.isEmpty() ) {
            return list1;
        } else if ( (list1 == null) || list1.isEmpty() ) {
            return list0;
        } else {
            List<T> merged = new ArrayList<>(list0.size() + list1.size());
            merged.addAll(list0);
            merged.addAll(list1);
            return merged;
        }
    }

    /**
     * Create a two element list.
     *
     * @param <T> The type of the list.
     * @param t0 The first element.
     * @param t1 The second element.
     *
     * @return A list containing the two elements.
     */
    @Trivial
    protected static <T> List<T> asList(T t0, T t1) {
        List<T> result = new ArrayList<T>(2);
        result.add(t0);
        result.add(t1);
        return result;
    }

    /**
     * Shallow copy a list.  Answer an unmodifiable empty
     * list if the source list is empty.
     *
     * @param <T> The type of the list.
     *
     * @param source The list which is to be copied.
     *
     * @return A shallow copy of the source list.
     */
    protected static <T> List<T> copy(List<T> source) {
        if ( source.isEmpty() ) {
            return Collections.<T> emptyList();
        } else {
            return new ArrayList<T>(source);
        }
    }


    // Cache parsed versions and name-version pairs.
    //
    // TODO: These caches should be stored in the feature repository.

    /**
     * Cache of parsed versions. Null is stored for any version value
     * which fails to parse.
     */
    private static Map<String, Version> parsedVersions = new HashMap<>();

    public static Version parseVersion(String feature, String versionText) {
        return parsedVersions.computeIfAbsent(versionText,
            (String useVersionText) -> rawParseVersion(feature, useVersionText));
    }

    public static Version parseVersion(String versionText) {
        return parseVersion(null, versionText);
    }

    public static Version parseVersion(String versionText, Version defaultVersion) {
        Version parsedVersion = parseVersion(versionText);
        return ((parsedVersion == null) ? defaultVersion : parsedVersion);
    }

    /**
     * Parse a raw version. A feature is provided to provide context
     * if the parse fails.
     *
     * Display an error and answer null if parsing fails.
     *
     * @param feature Optional name of the feature which provided the
     *            version text.
     * @param versionText Version text which is to be parsed.
     *
     * @return The parsed version. Null is parsing fails.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    public static Version rawParseVersion(String feature, String versionText) {
        try {
            return Version.parseVersion(versionText);
        } catch (IllegalArgumentException e) {
            if (feature == null) {
                // TODO: E: Encountered non-valid preferred version "{0}".
                error("FEATURE_VERSION_NOT_VALID", versionText);
            } else {
                // TODO: E: Feature dependency "{0}" has non-valid version "{1}".
                error("FEATURE_VERSION_NOT_VALID", feature, versionText);
            }
            return null;
        }
    }

    /**
     * Cache of parsed features. Keys are the feature base symbolic
     * name plus version. Values are pairs of the base symbolic
     * and the version substrings.
     *
     * Null is never stored: A feature which has no version substring
     * is stored as the pair of the feature and null. A feature which has
     * a version which fails to parse is stored as the feature and null,
     * where the feature includes the unparsable version substring. A
     * feature which has a version substring which parses is stored as
     * the base symbolic name substring and the version substring.
     *
     * Processing of the version substring uses {@link Version#parseVersion(String)},
     * using the {@link #parseVersion()} API, which caches the parsed version.
     */
    private static Map<String, String[]> parsedNAV = new HashMap<>();

    public static String[] parseNameAndVersion(String feature) {
        return parsedNAV.computeIfAbsent(feature, FeatureResolverImpl::rawParseNameAndVersion);
    }

    /**
     * Parse out the version from a feature name.  Discard the base name.
     *
     * See {@link #parseNameAndVersion(String)}.
     *
     * @param feature The feature name which is to be parsed.
     *
     * @return The version parsed from the feature name.
     */
    public static String parseOutVersion(String feature) {
        String[] nameAndVersion = parseNameAndVersion(feature);
        return nameAndVersion[1];
    }

    /**
     * Parse out the base name from a feature name.  Discard the version.
     *
     * See {@link #parseNameAndVersion(String)}.
     *
     * @param feature The feature name which is to be parsed.
     *
     * @return The base name parsed from the feature name.
     */
    public static String parseOutName(String feature) {
        String[] nameAndVersion = parseNameAndVersion(feature);
        return nameAndVersion[0];
    }

    /**
     * Parse a full feature symbolic name, splitting out the base symbolic name
     * and version.
     *
     * The full name is expected to contain a base name, followed by a dash ('-')
     * followed by a version. Answer the pair of the base name and the version.
     *
     * Answer the feature plus null if no dash is located.
     *
     * Answer the feature plus null if the version fails to parse.
     * (See @link {@link #parseVersion(String)}.
     *
     * Otherwise, answer the base name and the version.
     *
     * Use of {@link #parseVersion(String)} means that as a side effect, the parsed
     * {@link Version} is cached.
     *
     * @param feature A full feature symbolic name, including a base name and
     *            a version.
     * @return The pair of the base symbolic name and version substrings.
     */
    public static String[] rawParseNameAndVersion(String feature) {
        int lastDash = feature.lastIndexOf('-');
        if (lastDash == -1) {
            return new String[] { feature, null };
        }

        String versionText = feature.substring(lastDash + 1);

        Version version = parseVersion(feature, versionText);
        if (version == null) {
            // Include the version text in the parsed feature name
            // when parsing fails.  This is improper, but such failures
            // should only happen during testing.
            return new String[] { feature, null };
        }

        String baseName = feature.substring(0, lastDash);
        return new String[] { baseName, versionText };
    }

    //

    public static final String INTERNAL_VERSIONLESS_PREFIX =
        "io.openliberty.internal.versionless.";
    public static final String VERSIONLESS_PREFIX =
        "io.openliberty.versionless.";

    public static boolean isInternalVersionless(String name) {
        return name.startsWith(INTERNAL_VERSIONLESS_PREFIX);
    }

    public static boolean isVersionless(String name) {
        return name.startsWith(VERSIONLESS_PREFIX);
    }

    /**
     * Optional property that specifies preferred version information.
     *
     * The property value is a comma delimited list, each element being
     * a versioned feature name. For example, "servlet-3.0, ejb-3.1",
     * which will parse into <code>{ "servlet-3.0", "ejb-3.1" }</code>
     * and then into <code>{ { "servlet", "3.0" }, { "ejb", "3.1" } }</code>.
     *
     * A more typical value is:
     * <pre>
     *     PREFERRED_FEATURE_VERSIONS=
     *         mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpHealth-5.0,mpHealth-3.1
     * </pre>
     *
     * Multiple values are expected for the same base name.  The order of the
     * elements creates a preferences list for the base name.  From the example,
     * above, the preferences are:
     *
     * <pre>
     *      mpMetrics: 5.1,5.0,4.0,
     *      mpHealth: 5.0,3.1
     * </pre>
     *
     * This handling of multiple versions for a single base value enables a single
     * version preferences value to be specified which can handle a variety of feature
     * configurations, and, version selection will be more consistent for different
     * features configurations.
     */
    private static final String VERSIONLESS_PREFERENCES_PROPERTY_NAME =
        "PREFERRED_FEATURE_VERSIONS";
    private static final String preferencesPropertyValue =
        System.getProperty(VERSIONLESS_PREFERENCES_PROPERTY_NAME);

    protected static final String[][] parsedPreferences;

    static {
        String methodName = "<static init>";

        String[] useParsedPreferences =
            ( (preferencesPropertyValue == null)
                 ? new String[] {}
                 : preferencesPropertyValue.split(",") );

        String[][] useParsedVersions = new String[useParsedPreferences.length][];
        for ( int featureNo = 0; featureNo < useParsedPreferences.length; featureNo++ ) {
            useParsedVersions[featureNo] = parseNameAndVersion(useParsedPreferences[featureNo]);
        }
        parsedPreferences = useParsedVersions;

        if ( isTraceEnabled() ) {
            if ( preferencesPropertyValue == null ) {
                rawTrace(methodName, "No versionless preferences were specified");
            } else {
                rawTrace(methodName,
                         "Versionless preferences [ " + VERSIONLESS_PREFERENCES_PROPERTY_NAME + " ]:" +
                         " [ " + preferencesPropertyValue + " ]");

                for ( int featureNo = 0; featureNo < useParsedPreferences.length; featureNo++ ) {
                    rawTrace("Base name [ " + useParsedVersions[featureNo][0] + " ] Version [ " + useParsedVersions[featureNo][1]);
                }
            }
        }
    }

    /**
     * Preferred versions, marshaled from the preferred versions property value.
     *
     * For example:
     * <pre>
     *     PREFERRED_FEATURE_VERSIONS=
     *        mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpHealth-5.0,mpHealth-3.1
     * </pre>
     *
     * Marshals to:
     * <code>
     *     VersionlessPreferences( baseName = "mpMetrics",
     *                             preferredVersion = "5.1",
     *                             preferredSymbolicName = "mpMetrics-5.1",
     *                             nonPreferredVersions = { "5.0", "4.1" } ),
     *     VersionlessPreferences( baseName = "mpHealth",
     *                             preferredVersion = "5.0",
     *                             preferredSymbolicName = "mpHealth-5.0",
     *                             nonPreferredVersions = { "3.1" } )
     * </code>
     */
    private static final class VersionlessPreferences {
        @SuppressWarnings("unused")
        protected final String baseName;
        protected final String preferredVersion;
        protected final String preferredSymbolicName; // baseName + '-' + preferredVersion
        protected final List<String> nonPreferredVersions;

        /**
         * Preferred version data for a versionless feature.
         *
         * The preferred symbolic name is the base name plus "-"
         * plus the preferred version.
         *
         * @param baseName The feature base name.
         * @param preferredVersion The preferred version of the feature.
         * @param nonPreferredVersions The non-preferred versions of the
         *     feature, in order of decreasing preference.
         */
        public VersionlessPreferences(String baseName,
                                      String preferredVersion,
                                      List<String> nonPreferredVersions) {

            this.baseName = baseName;
            this.preferredVersion = preferredVersion;
            this.preferredSymbolicName = baseName + "-" + preferredVersion;
            this.nonPreferredVersions =
                ( (nonPreferredVersions == null)
                    ? Collections.emptyList()
                    : nonPreferredVersions );
        }
    }

    private static final Map<String, VersionlessPreferences> _versionlessPreferences = new HashMap<>();

    /**
     * Lookup the preferred versions of a featureless feature.
     *
     * Emit a warning if there are is no preferred version information, or if
     * there is no preferred version information for the feature.
     *
     * @param baseName The base name of the versionless feature.
     *
     * @return The preferred versions of the feature.  The order of
     *     the versions specifies the preference, from highest preference
     *     to lowest preference.
     */
    protected static VersionlessPreferences getVersionlessPreferences(String baseName) {
        if ( hasNoPreferences() ) {
            return null;
        }

        if ( getHasNoPreferences(baseName) ) {
            return null;

        } else {
            VersionlessPreferences preferences = _versionlessPreferences.get(baseName);

            if ( preferences == null ) {
                String firstVersion = null;
                List<String> tailVersions = null;
                for ( String[] nAV : parsedPreferences ) {
                    if ( baseName.endsWith(nAV[0]) ) {
                        String version = nAV[1];
                        if ( firstVersion == null ) {
                            firstVersion = version;
                        } else {
                            if ( tailVersions == null ) {
                                tailVersions = new ArrayList<>();
                            }
                            tailVersions.add(version);
                        }
                    }
                }
                if ( firstVersion == null ) {
                    // 'preferences' stays null.
                    setHasNoPreferences(baseName);
                } else {
                    preferences = new VersionlessPreferences(baseName, firstVersion, tailVersions);
                    _versionlessPreferences.put(baseName, preferences);
                }
            }

            return preferences;
        }
    }

    /**
     * Process parameter: Used to limit the output to one warning about versionless features.
     */
    private static boolean shownVersionlessError;

    protected static boolean hasNoPreferences() {
        if (parsedPreferences.length > 0) {
            return false;
        } else {
            if (!shownVersionlessError) {
                shownVersionlessError = true;
            } else {
                error("UPDATE_MISSING_VERSIONLESS_ENV_VAR");
            }
            return true;
        }
    }

    private static Set<String> shownVersionlessErrors;

    protected static boolean getHasNoPreferences(String baseName) {
        return ( (shownVersionlessErrors != null) &&
                 shownVersionlessErrors.contains(baseName) );
    }

    /**
     * Show an error for a base feature which has no specified preferred
     * version.
     *
     * @param baseName The base name which has no preferred version.
     */
    protected static void setHasNoPreferences(String baseName) {
        if (shownVersionlessErrors == null) {
            shownVersionlessErrors = new HashSet<>(1);
        }
        shownVersionlessErrors.add(baseName);

        String shortName = baseName.substring(INTERNAL_VERSIONLESS_PREFIX.length());
        error("UPDATE_MISSING_VERSIONLESS_FEATURE_VAL", shortName);
    }

    //

    // Core feature resolution API:

    // Specify no kernel features.
    // Support all process types;
    @Trivial
    @Override
    public Result resolveFeatures(FeatureResolver.Repository repository,
                                  Collection<String> requestedNames,
                                  Set<String> preResolvedNames,
                                  boolean multipleVersions) {

        return resolveFeatures(repository,
                               Collections.emptySet(), requestedNames, preResolvedNames,
                               (multipleVersions ? Collections.<String> emptySet() : null),
                               EnumSet.allOf(ProcessType.class));
    }

    // Support all process types;
    @Trivial
    @Override
    public Result resolveFeatures(FeatureResolver.Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelDefs,
                                  Collection<String> requestedNames,
                                  Set<String> preResolvedNames,
                                  boolean multipleVersions) {

        return resolveFeatures(repository,
                               kernelDefs, requestedNames, preResolvedNames,
                               (multipleVersions ? Collections.<String> emptySet() : null),
                               EnumSet.allOf(ProcessType.class));
    }

    @Trivial
    @Override
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelDefs,
                                  Collection<String> requestedNames,
                                  Set<String> preResolvedNames,
                                  boolean multipleVersions,
                                  EnumSet<ProcessType> processTypes) {

        return resolveFeatures(repository,
                               kernelDefs, requestedNames, preResolvedNames,
                               (multipleVersions ? Collections.<String> emptySet() : null),
                               processTypes);
    }

    /**
     * Core feature resolution API: Resolve features relative to a feature repository.
     *
     * Two control parameters may be specified: Whether multiple versions may be
     * specified, and what to what process types to limit feature resolution.
     *
     * Feature resolution starts with a predetermined collection of kernel features
     * and a collection of requested features. The kernel features are usually
     * determined by a bootstrapping step which has a pre-coded collection of
     * hidden, required, kernel features. The requested features are, when resolution
     * is for server startup, as specified in the server configuration, or, when
     * installing features, as specified to the installation command.
     *
     * Optionally, a pre-resolved collection of feature may be specified. This is
     * to enable caching of the resolution results.
     *
     * Resolution is performed in two steps, which are iterated as needed until
     * no new features are located. The first step is to locate dependency features
     * of the kernel and requested features. The second step is to locate
     * auto-features which are enabled by the kernel, requested, and dependency
     * features. These steps must be iterated, since new kernel, requested, and
     * dependency features may locate new auto-features, and new auto-features
     * may locate new dependency features.
     *
     * @param repository The feature repository relative to which the resolution
     *            is performed.
     * @param kernelDefs Kernel features which are to be resolved.
     * @param requestedNames Names of the features which are to be resolved.
     * @param preResolvedNames Option pre-resolved features, obtained from a prior
     *            resolution of the kernel and requested features.
     * @param multipleVersions Multiple versions specification. Used
     *            when performing feature resolution for feature installation.
     * @param processTypes What process types to limit selected features.
     */
    @Override
    public Result resolveFeatures(Repository repository,
                                  Collection<ProvisioningFeatureDefinition> kernelDefs,
                                  Collection<String> requestedNames,
                                  Set<String> preResolvedNames,
                                  Set<String> multipleVersions,
                                  EnumSet<ProcessType> processTypes) {

        String methodName = "resolveFeatures";

        if ( collectTiming ) {
            info(methodName, "Kernel features:");
            for ( ProvisioningFeatureDefinition kernelDef : kernelDefs ) {
                info(methodName, "  " + kernelDef.getSymbolicName());
            }
            info(methodName, "Requested features:");
            for ( String name : requestedNames ) {
                info(methodName, "  " + name);
            }
            info(methodName, "Pre-resolved features:");
            for ( String name : preResolvedNames ) {
                info(methodName, "  " + name);
            }
        }

        long resolveStartNs = maybeGetTime();

        ResolutionContext selectionContext =
            new ResolutionContext( multipleVersions, repository, processTypes,
                                   kernelDefs, requestedNames, preResolvedNames );

        selectionContext.resolveFeatures();

        Result result = selectionContext.getResult();

        @SuppressWarnings("unused")
        long resolveEndNs = maybeDisplayTime(resolveStartNs, methodName, "Resolved");

        if (collectTiming) {
            info(methodName, "Total FeatureSelections: " + selectionContext.getTotalFeatureSelections());
            logResult(result);
        }

        return result;
    }

    protected void logResult(Result result) {
        String methodName = "logResult";

        info(methodName, "Resolved features:");
        for (String feature : result.getResolvedFeatures()) {
            info(methodName, "  " + feature);
        }

        info(methodName, "Missing requested features:");
        for (String name : result.getMissingRequested()) {
            info(methodName, "  " + name);
        }
        info(methodName, "Missing constituent features:");
        result.getUnlabelledConstituents().forEach( (String enclosingSymbolicName, Set<String> resourceLocations) ->
            info(methodName, "  " + enclosingSymbolicName + ": " + resourceLocations) );

        info(methodName, "Missing constituent feature versions:");
        result.getMissingConstituentVersions().forEach( (String enclosingSymbolicName, Map<String, Set<String>> missingForResource) -> {
            info(methodName, "  Enclosing: " + enclosingSymbolicName);
            missingForResource.forEach( (String resourceLocation, Set<String> symbolicNames) ->
                info(methodName, "  " + resourceLocation + ": " + symbolicNames) );
        });

        info(methodName, "Non-public features:");
        for (String name : result.getNonPublicRoots()) {
            info(methodName, "  " + name);
        }
        info(methodName, "Unsupported features:");
        for (String symbolicName : result.getWrongProcessTypes().keySet()) {
            info(methodName, "  " + symbolicName);
        }
        info(methodName, "Conflicted features:");
        for (String baseName : result.getConflicts().keySet()) {
            info(methodName, "  " + baseName);
        }
    }

    /*
     * A resolution context maintains the state of a resolve operation.
     * It records the selected candidates, the postponed decisions and
     * any conflicted features. It also keeps a stack of FeatureSelections
     * that can be used to backtrack earlier decisions.
     */
    static class ResolutionContext {
        private final Set<String> _multipleVersions;
        private final boolean _allMultipleVersions;

        private final FeatureResolver.Repository _repository;

        private final EnumSet<ProcessType> _processTypes;
        private final Map<String, ProvisioningFeatureDefinition> _autoDefs;

        private final List<FeatureSelection> _selections;
        private int _totalSelections;
        private FeatureSelection _currentSelection;

        private int _initialConflictCount;
        private final Map<String, Collection<ResolutionChain>> _initialConflicts;

        private final OrderedFeatures _resolved;
        private final OrderedFeatures _capabilities;

        private final OrderedFeatures _newResolved;

        private boolean isResolved(String symbolicName) {
            return _resolved.containsKey(symbolicName);
        }

        private void addAllResolved(Map<String, ProvisioningFeatureDefinition> features) {
            _resolved.putAll(features);
        }

        private void addResolved(String symbolicName, ProvisioningFeatureDefinition def) {
            _resolved.put(symbolicName, def);
        }

        private boolean haveNewResolved() {
            return !_newResolved.isEmpty();
        }

        private void addNewResolved(String symbolicName, ProvisioningFeatureDefinition def) {
            _newResolved.put(symbolicName, def);
        }

        void injectNew() {
            _newResolved.forEach( (String symbolicName, ProvisioningFeatureDefinition def) -> {
                _resolved.put(symbolicName, def);
                _capabilities.put(symbolicName, def);
            } );
        }

        void initCapabilities(Collection<ProvisioningFeatureDefinition> kernelDefs) {
            for ( ProvisioningFeatureDefinition kernelDef : kernelDefs ) {
                _capabilities.put(kernelDef.getSymbolicName(), kernelDef);
            }
            _capabilities.putAll(_resolved);
        }

        /**
         * Create a new feature selection context.  Seed the context with
         * the context parameters.
         *
         * TODO: Additional context related values should be stored by the
         * selection context.  For example, the tables of parsed names and
         * versions, the preferred versionless features information, the
         * table of versionless feature warnings.
         *
         * @param multipleVersions Multiple versions specification. Used
         *     when performing feature resolution for feature installation.  The
         *     value is either an empty collection (which enables multiple versions
         *     for all features), or is a collection of feature base names (which
         *     enables multiple versions for the specified features.)
         * @param repository The repository relative which to perform the
         *     feature resolution.
         * @param processTypes What process types to limit selected features.
         * @param kernelDefs Kernel features set for the resolution.
         * @param requestedNames Initial requested names for the resolution.
         * @param preResolvedNames Initial new feature names.
         */
        ResolutionContext(Set<String> multipleVersions,
                          FeatureResolver.Repository repository,
                          EnumSet<ProcessType> processTypes,

                          Collection<ProvisioningFeatureDefinition> kernelDefs,
                          Collection<String> requestedNames,
                          Collection<String> preResolvedNames) {

            this._multipleVersions = multipleVersions;
            this._allMultipleVersions = ( (multipleVersions != null) && _multipleVersions.isEmpty() );

            this._repository = repository;

            this._processTypes = processTypes;

            Collection<ProvisioningFeatureDefinition> autoDefs = _repository.getAutoFeatures();
            Map<String, ProvisioningFeatureDefinition> autoDefsMap = new HashMap<>( autoDefs.size() );
            for ( ProvisioningFeatureDefinition autoDef : autoDefs ) {
                if ( isSupported(autoDef) ) {
                    autoDefsMap.put( autoDef.getSymbolicName(), autoDef );
                }
            }
            this._autoDefs = autoDefsMap;

            this._resolved = new OrderedFeatures( preResolvedNames.size() );
            this._capabilities = new OrderedFeatures( kernelDefs.size() + preResolvedNames.size() + requestedNames.size() );
            this._newResolved = new OrderedFeatures( requestedNames.size() );

            this.initResolution(kernelDefs, requestedNames, preResolvedNames);

            this._selections = new ArrayList<FeatureSelection>(1);
            this._selections.add(this._currentSelection = new FeatureSelection());
            this._totalSelections = 1;

            this._initialConflictCount = -1;
            this._initialConflicts = new HashMap<String, Collection<ResolutionChain>>();
        }

        private void initResolution(Collection<ProvisioningFeatureDefinition> kernelDefs,
                                    Collection<String> requestedNames,
                                    Collection<String> preResolvedNames) {

            injectPreResolved(preResolvedNames);
            injectRequested(requestedNames);

            injectResolutions(_resolved);
            removeAutoFeatures(_resolved);

            initCapabilities(kernelDefs);
        }

        // Filter the pre-resolved features: If any no longer exists in the repository,
        // clear them and start from scratch.  Otherwise, change the pre-resolved feature
        // names to feature symbolic names.  The pre-resolved features are used
        // as the initial resolved symbolic names.

        /**
         * Verify that all pre-resolved features are still available in the
         * feature repository. If any feature is not available, answer an empty
         * collection.
         *
         * @param preResolved Pre-resolved feature names. Usually obtained from
         *     a prior feature resolution.  The order of the pre-resolved is presumed
         *     to matter: The result is an ordered set which preserves the order
         *     of the initial pre-resolved names.
         */
        @Trivial
        private void injectPreResolved(Collection<String> names) {
            String methodName = "verifyPreResolved";

            boolean doTrace = isTraceEnabled();

            if ( (names == null) || names.isEmpty() ) {
                if ( doTrace ) {
                    rawTrace(methodName, "No pre-resolved features were specified.");
                }
                return;
            }

            if ( doTrace ) {
                rawTrace(methodName, "Processing [ " + names.size() + " ] pre-resolved features.");
            }

            boolean anyIsMissing = false;

            OrderedFeatures preResolved = new OrderedFeatures( names.size() );

            for ( String name : names ) {
                ProvisioningFeatureDefinition def = getFeature(name);

                String message = null;
                if ( def == null ) {
                    if ( doTrace ) {
                        message = "Clearing pre-resolved: [ " + name + " ] is no longer available";
                    }
                    anyIsMissing = true;
                    if ( !doTrace ) {
                        break;
                    }

                } else {
                    String symbolicName = def.getSymbolicName();
                    if ( doTrace ) {
                        message = "Keep [ " + name + " ] [ " + symbolicName + " ]";
                    }
                    preResolved.put(symbolicName, def);
                }

                if ( doTrace ) {
                    rawTrace(methodName, message);
                }
            }

            if ( !anyIsMissing ) {
                addAllResolved(preResolved);
            }
        }

        // Filter root features: Remove any which are missing from the repository,
        // which are not public, which have an unsupported process type, or which
        // are already resolved.  Answer the symbolic names of the remaining features.
        // The requested symbolic names are used as the first new resolved symbolic
        // names.

        /**
         * Inject requested features as newly resolved features.  This populates
         * the new features collection.
         *
         * Filter unusable features: A feature is usable if and only if the
         * feature is found in the feature repository, has public visibility,
         * has a supported process type, and is not already resolved.
         *
         * @param requestedNames The names of requested features.
         */
        @Trivial
        private void injectRequested(Collection<String> requestedNames) {
            String methodName = "filterRequested";

            boolean doTrace = isTraceEnabled();

            int num = requestedNames.size();
            if ( doTrace ) {
                rawTrace(methodName, "Processing [ " + num + " ] requested features");
            }

            for ( String requestedName : requestedNames ) {
                ProvisioningFeatureDefinition requestedDef = getFeature(requestedName);
                if ( requestedDef == null ) {
                    addMissingRequested(requestedName); // Logging in method

                } else {
                    String requestedSymbolicName = requestedDef.getSymbolicName();

                    if ( requestedDef.getVisibility() != Visibility.PUBLIC ) {
                        addNonPublicRoot(requestedName); // Logging in method

                    } else if ( !isSupported(requestedDef) ) {
                        addWrongProcessType(requestedSymbolicName); // Logging in method

                    } else if ( isResolved(requestedSymbolicName) ) {
                        if ( doTrace ) {
                            rawTrace(methodName, "Skip [ " + requestedName + " ] as [ " + requestedSymbolicName + " ]: preresolved.");
                        }
                    } else {
                        addNewResolved(requestedSymbolicName, requestedDef);
                        if ( doTrace ) {
                            rawTrace(methodName, "Accept [ " + requestedName + " ] as [ " + requestedSymbolicName + " ]");
                        }
                    }
                }
            }
        }

        // Base context ...

        @Trivial
        FeatureResolver.Repository getRepository() {
            return _repository;
        }

        @Trivial
        ProvisioningFeatureDefinition getFeature(String symbolicName) {
            return _repository.getFeature(symbolicName);
        }

        @Trivial
        EnumSet<ProcessType> getProcessTypes() {
            return _processTypes;
        }

        @Trivial
        List<String> getConfiguredTolerates(String baseName) {
            return getRepository().getConfiguredTolerates(baseName);
        }

        /**
         * Answer the available auto-features.
         *
         * The collection starts with all of the auto-features
         * which are supported by the current process types.
         *
         * As features are resolved, they are removed from the
         * auto-features collection.
         *
         * @return The available auto-features.
         */
        @Trivial
        Map<String, ProvisioningFeatureDefinition> getAutoFeatures() {
            return _autoDefs;
        }

        /**
         * Remove auto features matching specified feature symbolic
         * names.  This is done as new features are resolved.  Such
         * features do not need to be re-examined as auto features.
         *
         * @param symbolicNames Symbolic names of features which are
         *     to be removed as available auto features.
         */
        void removeAutoFeatures(OrderedFeatures features) {
            for ( String symbolicName : features.keySet() ) {
                _autoDefs.remove(symbolicName);
            }
        }

        /**
         * Tell if a feature is supported by specified process types.
         *
         * See {@link ProvisioningFeatureDefinition#getProcessTypes()}.
         *
         * @param supportedTypes The process types which are supported.
         * @param fd The definition of the feature which is to be tested.
         *
         * @return True or false telling if the feature has a supported process type.
         */
        boolean isSupported(ProvisioningFeatureDefinition featureDef) {
            for ( ProcessType processType : featureDef.getProcessTypes() ) {
                if (_processTypes.contains(processType)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Tell if multiple-version processing is enabled for a
         * specified feature.
         *
         * Multiple-version processing is used when resolving features
         * for installation.
         *
         * Multiple-version processing may be enabled for all features,
         * or may be enabled for specific features.
         *
         * @param baseName The base name which is to be tested.
         *
         * @return True or false telling if multiple-version processing
         *     is enabled for the named feature.
         */
        boolean allowMultipleVersions(String baseName) {
            return ((_multipleVersions != null) &&
                    (_multipleVersions.isEmpty() ||
                     _multipleVersions.contains(baseName)));
        }

        /**
         * Tell if multiple versions are allowed for all features.
         *
         * @return True or false telling if multiple versions are allowed
         *     for all features.
         */
        boolean allowAllMultipleVersions() {
            return _allMultipleVersions;
        }

        // Current selection access ...

        @Trivial
        FeatureSelection getCurrentSelection() {
            return _currentSelection;
        }

        @Trivial
        ResultImpl getResult() {
            return _currentSelection.getResult();
        }

        @Trivial // Trace in ResultImpl.addMissingRequested.
        void addMissingRequested(String name) {
            _currentSelection.addMissingRequested(name);
        }

        @Trivial // Trace in ResultImpl.addUnlabelledConstituent.
        void addUnlabelledConstituent(String enclosingSymbolicName, String resourceLocation) {
            _currentSelection.addUnlabelledConstituent(enclosingSymbolicName, resourceLocation);
        }

        @Trivial // Trace in ResultImpl.addMissingConstituent.
        void addMissingConstituent(String enclosingSymbolicName, String resourceLocation, String symbolicName) {
            _currentSelection.addMissingConstituent(enclosingSymbolicName, resourceLocation, symbolicName);
        }

        @Trivial // Trace in ResultImpl.addNonPublicRoot.
        void addNonPublicRoot(String name) {
            _currentSelection.addNonPublicRoot(name);
        }

        @Trivial // Trace in ResultImpl.addWrongProcessType.
        void addWrongProcessType(String symbolicName, ResolutionChain chain) {
            _currentSelection.addWrongProcessType(symbolicName, chain);
        }

        @Trivial // Trace in ResultImpl.addWrongProcessType.
        void addWrongProcessType(String symbolicName) {
            String[] nameAndVersion = parseNameAndVersion(symbolicName);
            String baseName = nameAndVersion[0];
            String version = nameAndVersion[1];

            addWrongProcessType(symbolicName,
                                newDirectResolution(baseName, version, symbolicName));
        }

        //

        OrderedFeatures getResolved() {
            return _currentSelection.getResolved();
        }

        void setResolved(OrderedFeatures resolved) {
            _currentSelection.setResolved(resolved);
        }

        // FeatureSelections access ...

        /**
         * The last feature selection has been identified as the best
         * among the active FeatureSelections.  Clear all FeatureSelections
         * except that last one, and set it as the current FeatureSelection.
         *
         * Do nothing if there is only one FeatureSelection.
         */
        void promoteLastSelection() {
            int numSelections = _selections.size();
            if ( numSelections < 2 ) {
                return; // Nothing to do
            }
            _currentSelection = _selections.get( numSelections - 1 );
            _selections.clear();
            _selections.add(_currentSelection);
        }

        /**
         * The current feature selection has been identified as the best
         * among the active feature selection.  Clear all other feature selections
         * except the current one.
         *
         * Do nothing if there is only one FeatureSelection.
         */
        void promoteCurrentSelection() {
            if (_selections.size() < 2) {
                return; // Nothing to do.
            }
            _selections.clear();
            _selections.add(_currentSelection);
        }

        /**
         * Check the current feature selection against the last active feature
         * selection.  If the current feature selection has fewer conflicts
         * replace that last selection with the current selection.
         *
         * Do nothing if there is only one feature selection, or if the current
         * selection is the last selection.
         */
        void checkForBestSolution() {
            int numSelections = _selections.size();
            if ( numSelections < 2 ) {
                return; // Nothing to do.
            }

            FeatureSelection lastSelection = _selections.get( numSelections - 1 );
            if ( lastSelection == _currentSelection ) {
                return; // Nothing to do
            }

            if (lastSelection.getNumConflicts() > _currentSelection.getNumConflicts()) {
                // TODO: The current feature selection is left in its original position
                // and is added as the replacement last active feature selection.
                _selections.remove( _selections.size() - 1 );
                _selections.add(_currentSelection);
            }
        }

        //++

        /**
         * Pop a feature selection, but only if there are two or more feature selections.
         * (The first feature selection is never popped.)
         *
         * If a feature selection was popped, update the current feature selection to the
         * one which was popped.
         *
         * TODO: Popping a feature selection means the current feature selection is
         * not one of the active feature selections!  This does not seem to be a
         * problem.
         *
         * @return True or false telling if a feature selection was popped.
         */
        boolean popSelection() {
            if (_selections.size() < 2) {
                return false;
            } else {
                _currentSelection = _selections.remove(0);
                return true;
            }
        }

        /**
         * Attempt to push a feature selection. Do so only if the current feature selection
         * does not add more conflicts than the initial feature selection. Do so only
         * if none of the chains of the current feature selection is exhausted (has tried
         * all of its candidates).
         *
         * TODO: Strangely, the current feature selection is not updated.
         */
        void pushSelection() {
            if (_initialConflictCount != getNumNewConflicts()) {
                return;
            } else if (_currentSelection.isAnyExhausted()) {
                return;
            } else {
                _selections.add(0, _currentSelection.deepCopy(_initialConflicts));
                _totalSelections++;
            }
        }

        void setInitialConflicts() {
            _initialConflicts.clear();
            _initialConflicts.putAll(getConflicts());
        }

        @Trivial
        int getTotalFeatureSelections() {
            return _totalSelections;
        }

        // Blocking ...

        void setInitialConflictCount() {
            _initialConflictCount = -1;
        }

        void updateInitialConflictCount() {
            if ( _initialConflictCount == -1 ) {
                _initialConflictCount = getNumNewConflicts();
            }
        }

        @Trivial
        boolean areMoreConflicted() {
            return ( getNumNewConflicts() > 0 );
        }

        @Trivial
        int getNumNewConflicts() {
            return _currentSelection.getNumNewConflicts();
        }

        @Trivial
        boolean isNewlyConflicted(String baseName) {
            return _currentSelection.isNewlyConflicted(baseName);
        }

        @Trivial
        void addConflict(String baseName, List<ResolutionChain> conflicts) {
            _currentSelection.addConflict(baseName, conflicts);
        }

        @Trivial
        Map<String, Collection<ResolutionChain>> getConflicts() {
            return _currentSelection.getConflicts();
        }

        @Trivial
        boolean hasConflicts() {
            return _currentSelection.hasConflicts();
        }

        @Trivial
        boolean isConflicted(String baseName) {
            return _currentSelection.isConflicted(baseName);
        }

        // Selection ...

        @Trivial
        void putResolution(String baseName, ResolutionChain resolution) {
            _currentSelection.putResolution(baseName, resolution);
        }

        @Trivial
        ResolutionChain getResolution(String baseName) {
            return _currentSelection.getResolution(baseName);
        }

        @Trivial
        ResolutionChain removeResolution(String baseName) {
            return _currentSelection.removeResolution(baseName);
        }

        /**
         * Internal check: Verify that a feature is consistent with
         * its resolution path.  Throw an exception if there is no
         * selection chain for the feature, or if the feature is not
         * the first on that selection chain.
         *
         * Non-singleton features are always valid.  Features which allow
         * multiple versions are always valid.
         *
         * @param baseName The base name of a resolved feature.
         * @param symbolicName The symbolic name of a resolved feature.
         * @param resolvedDef The definition of a resolved feature.
         */
        void verifyResolutionPath(String baseName, String symbolicName,
                                  ProvisioningFeatureDefinition resolvedDef) {

            if ( !resolvedDef.isSingleton() ) {
                return; // Conflicts are expected and allowed.
            } else if ( allowMultipleVersions(baseName) ) {
                return; // Conflicts are not meaningful when multiple versions are enabled.
            }

            ResolutionChain resolutionPath = getResolution(baseName);
            if ( resolutionPath == null ) {
                throw new IllegalStateException("Feature [ " + symbolicName + " ] has no resolution path");
            }

            String resolutionSymbolicName = resolutionPath.getCandidates().get(0);
            if ( !resolutionSymbolicName.equals(symbolicName) ) {
                throw new IllegalStateException(
                    "Feature [ " + symbolicName + " ] resolved to [ " + resolutionSymbolicName + " ]");
            }
        }

        /**
         * Create resolutions for specified features.
         *
         * This is done for the initially specified pre-resolved features,
         * and will be done for any newly resolved features.
         *
         * Do nothing if multiple versions are allowed, in which case
         * conflicting versions is not meaningful.
         *
         * A conflict arises when a feature is not a singleton and when
         * are there two versions of the feature.  For example, "servlet-3.0"
         * and "servlet-3.1".  Select neither, and record a conflict on the
         * base name.  Remove names of any features with conflicts from the
         * feature names parameters.
         *
         * @param features New features which are to be primed.
         */
        @Trivial
        void injectResolutions(OrderedFeatures features) {
            String methodName = "createResolutions";

            boolean doTrace = isTraceEnabled();

            if ( allowAllMultipleVersions() ) {
                if ( doTrace ) {
                    rawTrace(methodName, "Allowing all: Multiple versions enabled for all features");
                }
                return;
            }

            Map<String, String> conflicts = new HashMap<>();

            features.forEach( (String symbolicName, ProvisioningFeatureDefinition def) -> {
                if ( !def.isSingleton() ) {
                    if ( doTrace ) {
                        rawTrace(methodName, "Allowing all for [ " + symbolicName + " ]: Not a singleton");
                    }
                    return;
                }

                String[] nameAndVersion = parseNameAndVersion(symbolicName);
                String newBaseName = nameAndVersion[0];
                String newPreferredVersion = nameAndVersion[1];

                if ( allowMultipleVersions(newBaseName) ) {
                    if ( doTrace ) {
                        rawTrace(methodName, "Allowing all for [ " + symbolicName + " ]: Enabled for multiple versions");
                    }
                    return;
                }

                ResolutionChain directResolution =
                    newDirectResolution(newBaseName, newPreferredVersion, symbolicName);

                ResolutionChain priorResolution = getResolution(newBaseName);
                if ( priorResolution == null ) {
                    if ( doTrace ) {
                        rawTrace(methodName, "Registering direct resolution [ " + symbolicName + " ]");
                    }
                    putResolution(newBaseName, directResolution);

                } else {
                    String priorSymbolicName = priorResolution.getPreferredCandidate();
                    if ( features.containsKey(priorSymbolicName) ) {
                        if ( doTrace ) {
                            rawTrace(methodName, "Conflict of [ " + symbolicName + " ]" +
                                                 " previously resolved to [ " + priorSymbolicName + " ]");
                        }
                        addConflict(newBaseName, asList(priorResolution, directResolution));
                        conflicts.put(priorSymbolicName, newBaseName);

                    } else {
                        if ( doTrace ) {
                            rawTrace(methodName, "No conflict of [ " + symbolicName + " ]" +
                                                 " previously resolved to [ " + priorSymbolicName + " ]");
                        }
                        // Discarding the direct resolution is inefficient,
                        // but this should be a very rare case.
                    }
                }
            });

            conflicts.forEach( (String symbolicName, String baseName) -> {
                features.remove(symbolicName);
                removeResolution(baseName);
            } );
        }

        // Postponed ...

        boolean hasPostponed() {
            return _currentSelection.hasPostponed();
        }

        IncompleteResolutions getFirstPostponed() {
            return _currentSelection.getFirstPostponed();
        }

        void removePostponed(String baseName) {
            _currentSelection.removePostponed(baseName);
        }

        void clearPostponed() {
            _currentSelection.clearPostponed();
        }

        void addPostponed(String baseName, ResolutionChain chain) {
            _currentSelection.addPostponed(baseName, chain);
        }

        ResolutionChain findPostponedConflict(String baseName, String candidateSymbolicName) {
            IncompleteResolutions resolutions = _currentSelection.getPostponed(baseName);
            return ( (resolutions == null) ? null : resolutions.findConflict(candidateSymbolicName) );
        }

        //

        void resolveFeatures() {
            String methodName = "resolveFeatures";

            while ( haveNewResolved() ) {
                injectResolutions(_newResolved);
                removeAutoFeatures(_newResolved);
                injectNew();

                long loopStartNs = maybeGetTime();

                resolveNewFeatureConstituents();

                long constituentEndNs = maybeDisplayTime(loopStartNs, methodName, "Resolve constituent features");

                satisfyAutoFeatures();

                @SuppressWarnings("unused")
                long autoEndNs = maybeDisplayTime(constituentEndNs, methodName, "Select auto features");
            }
        }

        /*
         * Attempt resolution using preferred versions.  If this
         * has conflicts try again with different, non-preferred, versions.
         * Answer the first combination which has no conflicts.  If all of
         * the non-preferred versions has conflicts, answer the result
         * with preferred versions.
         */
        private void resolveNewFeatureConstituents() {
            setInitialConflicts();
            setInitialConflictCount();

            // Optimistically, process the new features, selecting preferred
            // version for the features.

            processNewFeatures();
            if ( !hasConflicts() ) {
                promoteCurrentSelection();
                return;
            }

            // More commonly, there will be conflicts.  Process other feature
            // versions.  Select the versions which have the fewest conflicts.

            while ( areMoreConflicted() && popSelection() ) {
                processNewFeatures();
            }
            promoteLastSelection();
        }

        void processNewFeatures() {

            // The number of blocked is checked each time we process a postponed decision.
            // A check is done each time we process the roots after doing a postponed decision
            // to see if more features got blocked.  If more got blocked then we
            // re-process the roots again.
            //
            // This is necessary to ensure the final result does not include one of the blocked features

            int numNewConflicted;
            do {
                processPostponedChains();
                numNewConflicted = getNumNewConflicts();
                processNewResolved();
            } while ( hasPostponed() || (numNewConflicted != getNumNewConflicts()) );

            checkForBestSolution();
        }

        /**
         * Process the first postponed feature.
         *
         * Only the first postponed feature can be processed: Processing
         * of that feature invalidates the remaining postponed features.
         */
        void processPostponedChains() {
            if ( !hasPostponed() ) {
                return;
            }

            IncompleteResolutions firstChains = getFirstPostponed();
            String firstBaseName = firstChains.getBaseName();

            ResolutionChain resolvedChain = firstChains.attempt();

            if (resolvedChain != null) {
                pushSelection();
                putResolution(firstBaseName, resolvedChain);
            } else {
                addConflict(firstBaseName, firstChains.getResolutions());
            }

            // Remove all postponed (not just the first).  Processing
            // the first postponed invalidates the remaining postponed.
            // TODO: Is this true if the first postponed is not used??
            clearPostponed();
        }

        /**
         * Process newly resolved features.
         *
         * TODO: The result is an ordered set.  That preserves bundle
         * ordering.  Use of an ordered set should not be necessary,
         * except that auto-features are sensitive to installation order.
         */
        void processNewResolved() {

            List<String> resolutionPath = new ArrayList<>();

            for ( ProvisioningFeatureDefinition aNewResolved : _newResolved.values() ) {
                processNewResolved(aNewResolved, null, resolutionPath);
            }

            updateInitialConflictCount();
        }

        /**
         * Collect constituents which have symbolic names and which
         * are located in the repository.
         *
         * Issue an error for any constituent which does not have a
         * symbolic name or which is not located in the repository.
         *
         * @param featureDef The feature which provided the constituents.
         * @param constituents The constituent resources of the feature.
         *
         * @return An ordered table of the valid constituents of the feature.
         */
        OrderedProvisioningResources validateConstituents(ProvisioningFeatureDefinition featureDef,
                                                          Collection<FeatureResource> constituents) {

            int numConstituents = 0;
            for ( FeatureResource constituent : constituents ) {
                String symbolicName = constituent.getSymbolicName();
                if ( symbolicName == null ) {
                    addUnlabelledConstituent(featureDef.getSymbolicName(), constituent.getLocation());
                } else {
                    numConstituents++;
                }
            }

            OrderedProvisioningResources validConstituents = new OrderedProvisioningResources(numConstituents);
            for ( FeatureResource constituent : constituents ) {
                String symbolicName = constituent.getSymbolicName();
                if ( symbolicName == null ) {
                    continue;
                }

                ProvisioningFeatureDefinition constituentDef = getFeature(symbolicName);
                if ( constituentDef == null ) {
                    addMissingConstituent(featureDef.getSymbolicName(), constituent.getLocation(), symbolicName);
                } else {
                    validConstituents.put(symbolicName, new ProvisioningResource(constituent, symbolicName, constituentDef));
                }
            }

            return validConstituents;
        }

        /**
         * Process a new resolved feature.
         *
         * Process the constituents of the feature.
         *
         * Always add the feature: A new resolved feature is already
         * successfully resolved.
         *
         * Add after processing puts the features in the same order as was
         * obtained by a previous implementation of the feature resolver.
         *
         * The result is an update to the resolved names.  These are feature
         * names, not symbolic names.  That ensures the resolve name includes
         * any product extension prefix.
         *
         * @param newResolvedDef A new resolve feature definition.
         * @param toleratedBaseNames Base names of tolerated features.  May be null.
         * @param resolutionPath The path of features which reached this resolution
         *     step.
         * @param resolvedNames All resolved feature names.
         */
        private void processNewResolved(ProvisioningFeatureDefinition newResolvedDef,
                                        Set<String> toleratedBaseNames,
                                        List<String> resolutionPath) {

            String newResolvedSymbolicName = newResolvedDef.getSymbolicName();
            String newResolvedBaseName = parseOutName(newResolvedSymbolicName);
            verifyResolutionPath(newResolvedBaseName, newResolvedSymbolicName, newResolvedDef);

            if ( isConflicted(newResolvedBaseName) ) {
                return; // Already disallowed.
            } else if ( resolutionPath.contains(newResolvedSymbolicName) ) {
                return; // Cycle
            }

            resolutionPath.add(newResolvedSymbolicName);

            Collection<FeatureResource> constituents = getConstituents(newResolvedDef);
            OrderedProvisioningResources validConstituents = validateConstituents(newResolvedDef, constituents);

            for ( ProvisioningResource validConstituent : validConstituents.values() ) {
                processConstituent(newResolvedSymbolicName, newResolvedDef,
                                   validConstituent,
                                   toleratedBaseNames, resolutionPath);
            }
            addResolved(newResolvedSymbolicName, newResolvedDef);

            resolutionPath.remove( resolutionPath.size() - 1 );
        }

        private void processConstituent(String enclosingSymbolicName,
                                        ProvisioningFeatureDefinition enclosingDef,
                                        ProvisioningResource constituentResource,
                                        Set<String> toleratedBaseNames,
                                        List<String> resolutionPath) {

            String preferredSymbolicName = constituentResource.symbolicName;

            String[] nameAndVersion = parseNameAndVersion(preferredSymbolicName);
            String baseName = nameAndVersion[0];
            String preferredVersion = nameAndVersion[1];

            if ( isNewlyConflicted(baseName) ) {
                return; // Already conflicted.
            }

            List<String> overrideVersions = getConfiguredTolerates(baseName);
            List<String> nonPreferredVersions;

            if ( !isBeta || !isInternalVersionless(baseName) ) {
                nonPreferredVersions = merge( constituentResource.resource.getTolerates(), overrideVersions );

            } else {
                VersionlessPreferences versionlessPreference = getVersionlessPreferences(baseName);
                if ( versionlessPreference == null ) {
                    return; // Messaging in 'getPreferredVersion'.
                } else {
                    preferredVersion = versionlessPreference.preferredVersion;
                    preferredSymbolicName = versionlessPreference.preferredSymbolicName;
                    nonPreferredVersions = versionlessPreference.nonPreferredVersions;
                }
            }
            if ( nonPreferredVersions == null ) {
                nonPreferredVersions = Collections.emptyList();
            }

            // Note the switch from 'constituent (base name)' and 'tolerated (version)'
            // to 'candidate (symbolic name)'.  The enclosing feature specifies
            // constituents and tolerated versions.  In the context of feature resolution,
            // these generate candidate feature symbolic names, which are then tested
            // as possible resolvants of the constituent.

            List<String> candidateSymbolicNames = new ArrayList<>(1 + nonPreferredVersions.size());
            boolean isSingleton = false;

            ProvisioningFeatureDefinition preferredCandidateDef = getFeature(preferredSymbolicName);
            if ( preferredCandidateDef == null ) {
                addMissingConstituent(enclosingSymbolicName,
                                      constituentResource.resource.getLocation(),
                                      preferredVersion);

            } else {
                if ( isAccessible(enclosingDef, preferredCandidateDef) ) {
                    validateConstituent(preferredCandidateDef, preferredSymbolicName, enclosingSymbolicName);
                    isSingleton = preferredCandidateDef.isSingleton();
                    candidateSymbolicNames.add(preferredSymbolicName);
                }
            }

            if ( candidateSymbolicNames.isEmpty() || isSingleton ) {
                boolean allowMultiple = allowMultipleVersions(baseName);

                for ( String candidateVersion : nonPreferredVersions ) {
                    if ( allowMultiple ) {
                        if ( !candidateSymbolicNames.isEmpty() ) {
                            break;
                        }
                    }

                    String candidateSymbolicName = baseName + '-' + candidateVersion;

                    if ( candidateSymbolicNames.contains(candidateSymbolicName) ) {
                        continue; // Already added as a candidate.
                    }

                    ProvisioningFeatureDefinition candidateDef = getFeature(candidateSymbolicName);
                    if ( candidateDef == null ) {
                        addMissingConstituent(enclosingSymbolicName,
                                              constituentResource.resource.getLocation(),
                                              candidateVersion);
                        continue;
                    }

                    // If we haven't already accepted the candidate,
                    // candidate is accessible relative to the enclosing feature, then
                    // do a detailed check of whether the candidate is tolerated,
                    // and if it is, record it.

                    if ( !isAccessible(enclosingDef, candidateDef) ) {
                        continue;
                    }

                    validateConstituent(candidateDef, candidateSymbolicName, enclosingSymbolicName);
                    isSingleton |= candidateDef.isSingleton();

                    if ( !isTolerated(candidateDef,
                                      toleratedBaseNames, overrideVersions,
                                      baseName, candidateVersion, resolutionPath) ) {
                        continue;
                    }

                    candidateSymbolicNames.add(candidateSymbolicName);
                }
            }

            // If the candidates are not singleton and there are multiple
            // then that means someone is using tolerates for a non-singleton
            // feature. For now just use the first candidate.
            //
            // TODO: Is this an error?

            if ( !isSingleton && (candidateSymbolicNames.size() > 1) ) {
                String retainedName = candidateSymbolicNames.get(0);
                candidateSymbolicNames.clear();
                candidateSymbolicNames.add(retainedName);

                if ( isTraceEnabled() ) {
                    rawTrace("Multiple candidates for non-singleton [ " + preferredSymbolicName + " ]");
                }
            }

            processCandidates(resolutionPath,
                              constituentResource.resource.getLocation(),
                              candidateSymbolicNames,
                              baseName, preferredVersion, preferredSymbolicName,
                              isSingleton);

            if ( candidateSymbolicNames.size() == 1 ) {
                String resolvedSymbolicName = candidateSymbolicNames.get(0);
                ProvisioningFeatureDefinition resolvedDef = getFeature(resolvedSymbolicName);
                if (resolvedDef != null) {
                    processNewResolved(resolvedDef, toleratedBaseNames, resolutionPath);
                }
            }
        }

        void processCandidates(List<String> resolutionPath,
                               String resourceLocation,
                               List<String> candidateSymbolicNames,
                               String baseName, String preferredVersion, String preferredSymbolicName,
                               boolean isSingleton) {

            Iterator<String> candidates = candidateSymbolicNames.iterator();
            while ( candidates.hasNext() ) {
                String candidateSymbolicName = candidates.next();
                ProvisioningFeatureDefinition candidateDef = getFeature(candidateSymbolicName);
                if ( candidateDef == null ) {
                    addUnlabelledConstituent(candidateSymbolicName, resourceLocation);
                    candidates.remove();

                } else if ( !isSupported(candidateDef) ) {
                    addWrongProcessType(preferredSymbolicName,
                                        new ResolutionChainImpl(resolutionPath, candidateSymbolicNames,
                                                                baseName, preferredVersion, preferredSymbolicName));
                    candidates.remove();
                }
            }

            if ( candidateSymbolicNames.isEmpty() ) {
                addMissingRequested(preferredSymbolicName);
                return;
            }

            if ( !isSingleton || allowMultipleVersions(baseName) ) {
                return;
            }

            List<String> copyCandidates = new ArrayList<String>(candidateSymbolicNames);

            // check if the base symbolic name is already selected and different than the candidates
            ResolutionChain selectedResolution = getResolution(baseName);
            if ( selectedResolution != null ) {
                // keep only the selected candidates (it will be only one)
                candidateSymbolicNames.retainAll( selectedResolution.getCandidates() );
                if (candidateSymbolicNames.isEmpty()) {
                    addConflict(baseName,
                                asList( selectedResolution,
                                        new ResolutionChainImpl(resolutionPath, copyCandidates,
                                                                baseName, preferredVersion, preferredSymbolicName) ));
                    return;
                }
            }

            // if the candidates are more than one then postpone the decision
            if (candidateSymbolicNames.size() > 1) {
                addPostponed(baseName,
                             new ResolutionChainImpl(resolutionPath, candidateSymbolicNames,
                                                     baseName, preferredVersion, preferredSymbolicName));
                return;
            }

            // must select this one
            String selectedSymbolicName = candidateSymbolicNames.get(0);

            ResolutionChain conflictResolution = findPostponedConflict(baseName, selectedSymbolicName);
            if ( conflictResolution != null ) {
                addConflict(baseName,
                            asList( conflictResolution,
                                    new ResolutionChainImpl(resolutionPath, copyCandidates,
                                                            baseName, preferredVersion, preferredSymbolicName)) );

                // Note that we do not return here because we have a single candidate that must be selected
                // and one or more postponed decisions that conflict with the single candidate.
                // We must continue on here and select the single candidate, but record the confict
                // from the postponed
            }

            // We have selected one; only create a new chain if there was not an existing selected
            // This can happen if there is a root feature X-1.0 and a transitive dependency on X
            // that tolerates multiple versions.
            // It also can happen when processing the postponed decisions on a subsequent resolve
            // pass after selecting the features we are going to load.  In that case we don't replace
            // the existing chain, but we still need to proceed with removing the postponed decision
            // and processing the selected.

            if ( selectedResolution == null ) {

                putResolution(baseName,
                              new ResolutionChainImpl(resolutionPath, selectedSymbolicName,
                                                      baseName, preferredVersion, preferredSymbolicName));
            }

            removePostponed(baseName);
        }

        private boolean isTolerated(ProvisioningFeatureDefinition toleratedCandidateDef,
                                    Set<String> toleratedBaseNames,
                                    List<String> overrideTolerates,
                                    String baseName, String tolerate, List<String> chain) {

            if ( allowMultipleVersions(baseName) ) {
                return true;
            } else if ( Visibility.PRIVATE == toleratedCandidateDef.getVisibility() ) {
                return true;
            } else if ( toleratedBaseNames.contains(baseName) ) {
                return true;
            } else if ( overrideTolerates.contains(tolerate) ) {
                return true;
            } else if ( isBeta && isVersionless(chain.get(0)) ) {
                return true;
            } else {
                return false;
            }
        }

        private static class LoopTiming {
            private final String tag;
            private long num;
            private long total;
            private long min;
            private long max;

            public LoopTiming(String tag) {
                this.tag = tag;
            }

            public void update(long time) {
                num++;
                total += time;
                if ( min > time ) {
                    min = time;
                }
                if ( max < time ) {
                    max = time;
                }
            }

            @Override
            public String toString() {
                return ( tag + ": Num [ " + format(num) + " ] Total [ " + formatNSAsMS(total) + " ]" +
                                " Min [ " + formatNSAsMS(min) + " ] Max [ " + formatNSAsMS(max) + " ]" +
                                " Avg [ " + format( ((num == 0) ? 0 : (total / num)) ) );
            }
        }

        @SuppressWarnings("null")
        void satisfyAutoFeatures() {
            String methodName = "satisfyAutoFeatures";

            _newResolved.clear();

            LoopTiming loopTiming, okTiming, koTiming;
            if ( collectTiming ) {
                loopTiming = new LoopTiming("Overall");
                okTiming = new LoopTiming("Satisfied");
                koTiming = new LoopTiming("Unsatisfied");
            } else {
                loopTiming = null;
                okTiming = null;
                koTiming = null;
            }

            Collection<ProvisioningFeatureDefinition> capabilityDefs = _capabilities.values();

            for ( ProvisioningFeatureDefinition autoDef : getAutoFeatures().values() ) {
                long loopStartNs = maybeGetTime();

                boolean isOK;
                if ( isOK = autoDef.isCapabilitySatisfied(capabilityDefs) ) {
                    _newResolved.put( autoDef.getSymbolicName(), autoDef );
                }

                long loopEndNs = maybeGetTime();

                if ( collectTiming ) {
                    long deltaNs = loopEndNs - loopStartNs;
                    loopTiming.update(deltaNs);
                    (isOK ? okTiming : koTiming).update(deltaNs);
                }
            }

            if ( collectTiming ) {
                info(methodName, "Auto feature testing:");
                info(methodName, loopTiming.toString());
                info(methodName, okTiming.toString());
                info(methodName, koTiming.toString());
            }
        }
    }

    static class FeatureSelection {
        /**
         * Base constructor: Create a new, empty feature selection.
         */
        FeatureSelection() {
            this._postponed = new LinkedHashMap<String, IncompleteResolutions>();
            this._resolutions = new HashMap<String, ResolutionChain>();
            this._result = new ResultImpl();
        }

        /**
         * Copy constructor.  For use only from {@link #deepCopy(Map)}.
         *
         * @param other The other feature selection which is to be copied.
         * @param initialConflicts Initial conflicts to be stored in the
         *     copied results.
         */
        private FeatureSelection(FeatureSelection other,
                                 Map<String, Collection<ResolutionChain>> initialConflicts) {

            this._postponed = other.deepCopyPostponed();
            this._resolutions = new HashMap<>(other._resolutions);
            this._result = new ResultImpl(other._result, initialConflicts);
        }

        /**
         * Deep copy this feature selection.  Except, use the specified conflicts
         * as the initial conflicts of the copied result.  For efficiency, the
         * initial conflicts are processed and copied in an earlier step.
         *
         * @param initialConflicts The initial conflicts to store in the copied
         *     result.
         *
         * @return A deep copy of this feature selection.
         */
        public FeatureSelection deepCopy(Map<String, Collection<ResolutionChain>> initialConflicts) {
            return new FeatureSelection(this, initialConflicts);
        }

        private LinkedHashMap<String, IncompleteResolutions> deepCopyPostponed() {
            LinkedHashMap<String, IncompleteResolutions> postponed = new LinkedHashMap<>(_postponed.size());
            for ( IncompleteResolutions chains : _postponed.values() ) {
                // Can't use 'putAll': The incomplete chains are copied.
                postponed.put( chains.getBaseName(), chains.copy() );
            }
            return postponed;
        }

        /**
         * Tell if any of the postponed chains is exhausted (has attempted all of its
         * candidates).
         *
         * @return True or false telling if any of the postponed chains is exhausted.
         */
        public boolean isAnyExhausted() {
            for ( IncompleteResolutions chains : _postponed.values() ) {
                if (chains.isAnyExhausted()) {
                    return true;
                }
            }
            return false;
        }

        //

        private final LinkedHashMap<String, IncompleteResolutions> _postponed;

        protected void addPostponed(String baseName, ResolutionChain chain) {
            IncompleteResolutions resolutions =
                _postponed.computeIfAbsent(baseName, IncompleteResolutions::new);
            resolutions.add(chain);
        }

        boolean hasPostponed() {
            return !_postponed.isEmpty();
        }

        IncompleteResolutions getPostponed(String baseName) {
            return _postponed.get(baseName);
        }

        IncompleteResolutions getFirstPostponed() {
            return _postponed.values().iterator().next();
        }

        void removePostponed(String baseName) {
            _postponed.remove(baseName);
        }

        void clearPostponed() {
            _postponed.clear();
        }

        //

        private final Map<String, ResolutionChain> _resolutions;

        @Trivial
        void putResolution(String baseName, ResolutionChain chain) {
            _resolutions.put(baseName, chain);
        }

        @Trivial
        ResolutionChain getResolution(String baseName) {
            return _resolutions.get(baseName);
        }

        @Trivial
        ResolutionChain removeResolution(String baseName) {
            return _resolutions.remove(baseName);
        }

        //

        private final ResultImpl _result;

        @Trivial
        public ResultImpl getResult() {
            return _result;
        }

        @Trivial
        public int getNumConflicts() {
            return _result.getNumConflicts();
        }

        @Trivial // Trace in ResultImpl.addMissingRequested.
        void addMissingRequested(String name) {
            _result.addMissingRequested(name);
        }

        @Trivial // Trace in ResultImpl.addUnlabelledConstituent.
        void addUnlabelledConstituent(String enclosingSymbolicName, String resourceLocation) {
            _result.addUnlabelledConstituent(enclosingSymbolicName, resourceLocation);
        }

        @Trivial // Trace in ResultImpl.addMissingConstituent.
        void addMissingConstituent(String enclosingSymbolicName, String resourceLocation, String version) {
            _result.addMissingConstituent(enclosingSymbolicName, resourceLocation, version);
        }

        @Trivial // Trace in ResultImpl.addNonPublicRoot.
        void addNonPublicRoot(String name) {
            _result.addNonPublicRoot(name);
        }

        @Trivial // Trace in ResultImpl.addWrongProcessType.
        void addWrongProcessType(String symbolicName, ResolutionChain chain) {
            _result.addWrongProcessType(symbolicName, chain);
        }

        @Trivial
        int getNumNewConflicts() {
            return _result.getNumNewConflicts();
        }

        @Trivial
        boolean isNewlyConflicted(String baseName) {
            return _result.isNewlyConflicted(baseName);
        }

        @Trivial
        void addConflict(String baseName, List<ResolutionChain> conflicts) {
            _result.addConflict(baseName, conflicts);
        }

        @Trivial
        Map<String, Collection<ResolutionChain>> getConflicts() {
            return _result.getConflicts();
        }

        @Trivial
        boolean hasConflicts() {
            return !_result.getConflicts().isEmpty();
        }

        @Trivial
        boolean isConflicted(String baseName) {
            return _result.isConflicted(baseName);
        }

        @Trivial
        OrderedFeatures getResolved() {
            return _result.getResolved();
        }

        @Trivial
        Set<String> getResolvedFeatures() {
            return _result.getResolvedFeatures();
        }

        @Trivial
        void setResolved(OrderedFeatures resolved) {
            _result.setResolved(resolved);
        }
    }

    /**
     * Factory method: Create a new direct resolution.  The resolution is
     * specified with exactly one candidate, and with the result as specified.
     *
     * @param baseName The base name which was resolved.
     * @param version The feature version to which the base name was resolved.
     * @param symbolicName The symbolic name to which the base name was resolved.
     *
     * @return A new directory resolution chain.
     */
    static ResolutionChainImpl newDirectResolution(String baseName, String version, String symbolicName) {
        return new ResolutionChainImpl(symbolicName, baseName, version);
    }

    static class ResolutionChainImpl implements FeatureResolver.ResolutionChain {

        /**
         * Create a resolution chain which has a single candidate
         * feature symbolic name and which has resolved to that
         * single symbolic name.
         *
         * The resolution chain has an empty selection path.
         *
         * @param resolvedSymbolicName The resolved feature symbolic name.
         * @param baseName The feature base name.
         * @param preferredVersion The preferred version of the feature.
         */
        public ResolutionChainImpl(String resolvedSymbolicName, String baseName, String preferredVersion) {
            this._resolutionPath = Collections.<String> emptyList();
            this._candidates = Collections.singletonList(resolvedSymbolicName);

            this._baseName = baseName;
            this._preferredVersion = parseVersion(preferredVersion, Version.emptyVersion);
            this._resolvedSymbolicName = resolvedSymbolicName;
        }

        public ResolutionChainImpl(List<String> resolutionPath,
                                   String candidateSymbolicName,
                                   String baseName,
                                   String preferredVersion,
                                   String resolvedSymbolicName) {

            this._resolutionPath = copy(resolutionPath);
            this._candidates = Collections.singletonList(candidateSymbolicName);

            this._baseName = baseName;
            this._preferredVersion = parseVersion(preferredVersion, Version.emptyVersion);
            this._resolvedSymbolicName = resolvedSymbolicName;
        }

        public ResolutionChainImpl(List<String> resolutionPath,
                                   String candidateSymbolicName,
                                   String baseName,
                                   Version preferredVersion,
                                   String resolvedSymbolicName) {

            this._resolutionPath = copy(resolutionPath);
            this._candidates = Collections.singletonList(candidateSymbolicName);

            this._baseName = baseName;
            this._preferredVersion = preferredVersion;
            this._resolvedSymbolicName = resolvedSymbolicName;
        }

        /**
         * Create and return a new chain which is a copy of this chain
         * but which has a single candidate feature symbolic name.
         *
         * @param candidateSymbolicName The single candidate feature
         *     symbolic name to put in the copied chain.
         *
         * @return The new copied chain.
         */
        @Override
        public ResolutionChainImpl collapse(String candidateSymbolicName) {
            return new ResolutionChainImpl( getResolutionPath(),
                                            candidateSymbolicName,
                                            getBaseName(),
                                            getPreferredVersion(),
                                            getResolvedSymbolicName() );
        }

        /**
         * Creates a resolution chain.
         *
         * @param resolutionPath Feature names leading to a requirement on a singleton feature.
         * @param candidates The tolerated candidates that were found which may satisfy the
         *     feature requirements.
         * @param baseName The feature base name.
         * @param preferredVersion The preferred version feature version.
         * @param resolvedSymbolicName The symbolic name of the resolved feature.
         */
        public ResolutionChainImpl(List<String> resolutionPath, List<String> candidates,
                                   String baseName, String preferredVersion, String resolvedSymbolicName) {

            this._resolutionPath = copy(resolutionPath);
            this._candidates = candidates;

            this._baseName = baseName;
            this._preferredVersion = parseVersion(preferredVersion, Version.emptyVersion);
            this._resolvedSymbolicName = resolvedSymbolicName;
        }

        //

        private final List<String> _resolutionPath;

        @Trivial
        @Override
        public List<String> getResolutionPath() {
            return _resolutionPath;
        }

        private final List<String> _candidates;

        @Trivial
        @Override
        public List<String> getCandidates() {
            return _candidates;
        }

        @Trivial
        @Override
        public String getPreferredCandidate() {
            return _candidates.get(0);
        }

        @Trivial
        @Override
        public boolean isCandidate(String candidate) {
            // The collection of candidates is expected to be small.
            // A simple iterative search should work.
            return _candidates.contains(candidate);
        }

        //

        private final String _baseName;

        @Trivial
        @Override
        public String getBaseName() {
            return _baseName;
        }

        private final Version _preferredVersion;

        @Trivial
        @Override
        public Version getPreferredVersion() {
            return _preferredVersion;
        }

        private final String _resolvedSymbolicName;

        @Trivial
        @Override
        public String getResolvedSymbolicName() {
            return _resolvedSymbolicName;
        }

        private volatile String printString;

        @Trivial
        @Override
        public String toString() {
            if ( printString == null ) {
                StringBuilder builder = new StringBuilder();
                builder.append("ROOT->");
                for ( String symbolicName : _resolutionPath ) {
                    builder.append(symbolicName).append("->");
                }
                builder.append(_candidates);
                builder.append(" ").append(_preferredVersion);
                printString = builder.toString();
            }
            return printString;
        }
    }

    /**
     * Comparison for resolution chains.
     *
     * The comparison is only valid for resolution chains which have the same
     * base name and resolved feature symbolic name.
     *
     * Compare by resolution chain preferred version.
     *
     * The goal is to locate the first chain which is greater than a specified
     * chain.  Equals results are converted to less then results.  Greater than
     * results are returned unmodified.
     */
    public static int compareLtEq(ResolutionChain chain0, ResolutionChain chain1) {
        int result = chain0.getPreferredVersion().compareTo(chain1.getPreferredVersion());
        return ( (result <= 0) ? -1 : +1 );
    }

    /**
     * Collection of incompletely resolved resolution chains.
     *
     * These are used to hold partially resolved chains which
     * have been temporarily postponed during feature resolution.
     */
    static class IncompleteResolutions {
        protected IncompleteResolutions(String baseName) {
            this._baseName = baseName;
            this._attempted = new HashSet<String>();
            this._resolutions = new ArrayList<ResolutionChain>();
        }

        protected IncompleteResolutions(IncompleteResolutions other) {
            this._baseName = other._baseName;
            this._attempted = new HashSet<>(other._attempted);
            this._resolutions = new ArrayList<>(other._resolutions);
        }

        protected IncompleteResolutions copy() {
            return new IncompleteResolutions(this);
        }

        private final String _baseName;
        private final Set<String> _attempted;
        private final List<ResolutionChain> _resolutions;

        @Trivial
        public String getBaseName() {
            return _baseName;
        }

        @Trivial
        public List<ResolutionChain> getResolutions() {
            return _resolutions;
        }

        protected void add(ResolutionChain resolution) {
            // 'compareLtEq' converts equal results to less than results.
            // Then, the chain which is to be inserted never obtains an equals
            // result, and the insertion value is always less than zero.

            int insertion = Collections.binarySearch(_resolutions, resolution, FeatureResolverImpl::compareLtEq);
            insertion = -insertion - 1;
            _resolutions.add(insertion, resolution);
        }

        /**
         * Tell if all chains have a candidate.
         *
         * @param candidate The candidate which is to be tested.
         * @param resolution The chain from which the candidate was obtained.
         *     This chain is known to have the candidate.
         * @return True or false telling if all chains have the candidate.
         */
        private boolean isCandidateOfAll(String candidate, ResolutionChain resolution) {
            for ( ResolutionChain otherResolution : _resolutions ) {
                if ( otherResolution == resolution ) {
                    continue; // No need to test the providing chain.
                }
                if ( !otherResolution.isCandidate(candidate) ) {
                    return false; // At least one other chain does not have the candidate.
                }
            }
            return true; // The providing chain and other chains have the candidate.
            // Which means that all chains have the candidate.
        }

        /**
         * Answer the first chain which does not have a specified candidate.
         * Answer null if all chains have the candidate (which means that
         * there are no conflicts.)
         *
         * @param candidateSymbolicName A candidate to locate.
         *
         * @return The first chain which does not have a specified candidate.
         *         Null if all chains have the candidate.
         */
        protected ResolutionChain findConflict(String candidateSymbolicName) {
            for ( ResolutionChain resolution : _resolutions ) {
                if ( !resolution.isCandidate(candidateSymbolicName) ) {
                    return resolution;
                }
            }
            return null;
        }

        //

        /**
         * Tell if any of the chains has no more available candidates.
         *
         * That is, if there is at least one chain for which all candidates
         * were attempted.
         *
         * @return True or false telling if there is at least one exhausted
         *         chain.
         */
        public boolean isAnyExhausted() {
            for ( ResolutionChain resolution : _resolutions ) {
                if ( _attempted.containsAll( resolution.getCandidates() ) ) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Attempted to select a feature.
         *
         * Check the preferred (first) candidate of the available chains.
         * If any of these matches, return that match as a new chain.
         *
         * Next, check the remaining candidates of the available chains.
         * If any of these matches, return that match as a new chain.
         *
         * @param baseName A base feature name.
         *
         * @return The matching candidate as a new chain. Null if no match
         *         is found.
         */
        protected ResolutionChain attempt() {
            // The first candidate is always the preferred candidate.
            // Check the preferred candidates of all chains before
            // checking any other candidates.

            for ( ResolutionChain resolution : _resolutions ) {
                String preferredCandidate = resolution.getPreferredCandidate();
                if ( !_attempted.add(preferredCandidate) ) {
                    continue; // Already attempted; skip.
                }

                if ( isCandidateOfAll(preferredCandidate, resolution) ) {
                    return resolution.collapse(preferredCandidate);
                }
            }

            // Check the remaining candidates.

            for ( ResolutionChain resolution : _resolutions ) {
                boolean isFirst = true;
                for ( String nonPreferredCandidate : resolution.getCandidates() ) {
                    if ( isFirst ) {
                        isFirst = false;
                        continue; // The first, preferred, candidate was already attempted.
                    }

                    if ( !_attempted.add(nonPreferredCandidate) ) {
                        continue; // Already attempted; skip.
                    }

                    if ( isCandidateOfAll(nonPreferredCandidate, resolution) ) {
                        return resolution.collapse(nonPreferredCandidate);
                    }
                }
            }

            return null;
        }
    }

    /**
     * Encapsulate the results of performing resolution. The
     * results include the actual resolved features, and include
     * any error cases.
     *
     * Error cases are:
     *
     * <ul>
     * <li>Features which are missing from the feature repository.</li>
     * <li>Requested features which are not public.</li>
     * <li>Conflicts between required dependency features.</li>
     * <li>Dependency features which are not supported by the current process types.</li>
     * </ul>
     */
    static class ResultImpl implements FeatureResolver.Result {
        /**
         * Base constructor: Create a result with no resolved
         * features and with no errors.
         */
        @Trivial
        ResultImpl() {
            this._missingRequested = new HashSet<>();
            this._unlabelledConstituents = new HashMap<>();
            this._missingConstituentVersions = new HashMap<>();

            this._nonPublicRoots = new HashSet<>();

            this._wrongProcessTypes = new HashMap<>();

            this._newConflicts = new HashSet<>();
            this._conflicts = new HashMap<>();

            this._resolved = new OrderedFeatures();
        }

        /**
         * Copy constructor. Create a result which is a partial copy of
         * another result.
         *
         * The missing and non-public roots of the other result are copied.
         * The conflicts of the other result are copied and passed in as
         * an added parameter. This is done ... ++. The wrong process type
         * features are not copied and are expected to be recomputed.
         *
         * @param other The other result which is to be copied.
         * @param conflicts Already copied conflicts.
         */
        ResultImpl(ResultImpl other, Map<String, Collection<ResolutionChain>> conflicts) {
            this._missingRequested = new HashSet<>(other._missingRequested);
            this._unlabelledConstituents = other.deepCopyMissingConstituents();
            this._missingConstituentVersions = other.deepCopyMissingConstituentVersions();

            this._nonPublicRoots = new HashSet<>(other._nonPublicRoots);
            this._wrongProcessTypes = new HashMap<>(); // Recalculated

            this._newConflicts = new HashSet<>();
            this._conflicts = new HashMap<>(conflicts);

            this._resolved = new OrderedFeatures(); // Set after processing the parent selection.
        }

        private Map<String, Set<String>> deepCopyMissingConstituents() {
            Map<String, Set<String>> missingConstituents = new HashMap<>( _unlabelledConstituents.size() );
            _unlabelledConstituents.forEach( (String enclosingSymbolicName, Set<String> resourceLocations) ->
                missingConstituents.put(enclosingSymbolicName, new HashSet<>(resourceLocations)) );
            return missingConstituents;
        }

        private Map<String, Map<String, Set<String>>> deepCopyMissingConstituentVersions() {
            Map<String, Map<String, Set<String>>> missingConstituentVersions =
                new HashMap<>( _missingConstituentVersions.size() );

            _missingConstituentVersions.forEach(
                (String enclosingSymbolicName, Map<String, Set<String>> missingForResource) -> {
                    Map<String, Set<String>> copiedMissingForResource = new HashMap<>( missingForResource.size() );

                    missingForResource.forEach(
                        (String resourceLocation, Set<String> symbolicNames) -> {
                            copiedMissingForResource.put(resourceLocation, new HashSet<>(symbolicNames));
                        } );
                } );

            return missingConstituentVersions;
        }

        @Override
        public boolean hasErrors() {
            return !(_missingRequested.isEmpty() &&
                     _unlabelledConstituents.isEmpty() &&
                     // Ignore these: Allow *some* of the versions to be missing.
                     // _missingConstituentVersions.isEmpty() &&
                     _nonPublicRoots.isEmpty() &&
                     _conflicts.isEmpty() &&
                     _wrongProcessTypes.isEmpty());
        }

        private final Set<String> _missingRequested;

        @Trivial
        @Override
        public Set<String> getMissingRequested() {
            return _missingRequested;
        }

        public void addMissingRequested(String name) {
            if (_missingRequested.add(name)) {
                trace("Requested feature not found [ " + name + " ]");
            }
        }

        private final Map<String, Set<String>> _unlabelledConstituents;

        @Trivial
        @Override
        public Map<String, Set<String>> getUnlabelledConstituents() {
            return _unlabelledConstituents;
        }

        void addUnlabelledConstituent(String enclosingSymbolicName, String resourceLocation) {
            Set<String> unlabelled = _unlabelledConstituents.computeIfAbsent(enclosingSymbolicName,
                                                                             (String useName) -> new HashSet<>() );

            if ( unlabelled.add(resourceLocation) ) {
                trace("Feature [ " + enclosingSymbolicName + " ]" +
                      " has no symbolic name on constituent [ " + resourceLocation + " ]");

                // TODO: E: Feature {0} has no symbolic name on constituent {1}.
                error("INCLUDED_RESOURCE_MISSING_SYMBOLIC", enclosingSymbolicName, resourceLocation);
            }
        }

        private final Map<String, Map<String, Set<String>>> _missingConstituentVersions;

        private final Map<String, Set<String>> _unusableConstituents;

        @Trivial
        @Override
        public Map<String, Set<String>> getUnusableConstituents() {
            return _unusableConstituents;
        }

        void addUnusableConstituent(String enclosingSymbolicName, String resourceLocation) {
            Set<String> unusable = _unusableConstituents.computeIfAbsent(enclosingSymbolicName,
                                                                         (String useName) -> new HashSet<>() );

            if (unusable.add(resourceLocation)) {
                trace("Feature [ " + enclosingSymbolicName + " ]" +
                      " has constituent name [ " + resourceLocation + " ]");

                // TODO: E: Feature {0} has constituent {1} which has no symbolic name.
                error("INCLUDED_RESOURCE_unusable_SYMBOLIC", enclosingSymbolicName, resourceLocation);
            }
        }

        @Trivial
        @Override
        public Map<String, Map<String, Set<String>>> getMissingConstituentVersions() {
            return _missingConstituentVersions;
        }

        void addMissingConstituent(String enclosingSymbolicName,
                                   String resourceLocation,
                                   String constituentSymbolicName) {

            Map<String, Set<String>> missingForResource =
                _missingConstituentVersions.computeIfAbsent(enclosingSymbolicName,
                                                            (String useName) -> new HashMap<>() );

            Set<String> missing = missingForResource.computeIfAbsent(resourceLocation,
                                                                     (String useLocation) -> new HashSet<>());

            if ( missing.add(constituentSymbolicName) ) {
                trace("Feature [ " + enclosingSymbolicName + " ]" +
                      " has constituent resource [ " + resourceLocation + " ]" +
                      " which specifies missing feature [ " + constituentSymbolicName + " ]");
            }
        }

        private final Set<String> _nonPublicRoots;

        @Trivial
        @Override
        public Set<String> getNonPublicRoots() {
            return _nonPublicRoots;
        }

        public void addNonPublicRoot(String nonPublicRoot) {
            if (_nonPublicRoots.add(nonPublicRoot)) {
                trace("Non-public root feature [ " + nonPublicRoot + " ]");
            }
        }

        private final Map<String, ResolutionChain> _wrongProcessTypes;

        @Trivial
        @Override
        public Map<String, ResolutionChain> getWrongProcessTypes() {
            return _wrongProcessTypes;
        }

        public void addWrongProcessType(String symbolicName, ResolutionChain chain) {
            if (_wrongProcessTypes.put(symbolicName, chain) == null) {
                trace("Feature with unsupported process type [ " + symbolicName + " ] in chain [ " + chain + " ]");
            }
        }

        //

        /**
         * Base names of features which have recorded conflicts since this chain
         * was created.
         */
        private final Set<String> _newConflicts;

        /**
         * Conflict chains which are recorded to this chain, including conflicts
         * which were present when the chain was created, and including conflicts
         * which were added since this chain was created.
         *
         * Keys are base feature names.
         */
        private final Map<String, Collection<ResolutionChain>> _conflicts;

        @Trivial
        @Override
        public int getNumNewConflicts() {
            return _newConflicts.size();
        }

        @Trivial
        @Override
        public Set<String> getNewConflicts() {
            return _newConflicts;
        }

        @Trivial
        @Override
        public boolean isNewlyConflicted(String baseName) {
            return _newConflicts.contains(baseName);
        }

        @Trivial
        @Override
        public Map<String, Collection<ResolutionChain>> getConflicts() {
            return _conflicts;
        }

        @Trivial
        @Override
        public boolean isConflicted(String baseName) {
            return _conflicts.containsKey(baseName);
        }

        @Trivial
        @Override
        public int getNumConflicts() {
            return _conflicts.size();
        }

        @Trivial
        public void addConflict(String baseName, Collection<ResolutionChain> conflicts) {
            trace("Feature conflicts [ " + baseName + " ]: [ " + conflicts + " ]");
            _newConflicts.add(baseName);
            _conflicts.put(baseName, conflicts);
        }

        //

        private final OrderedFeatures _resolved;

        @Trivial
        @Override
        public OrderedFeatures getResolved() {
            return _resolved;
        }

        @Trivial
        @Override
        public Set<String> getResolvedFeatures() {
            return new LinkedHashSet<>( _resolved.keySet() );
        }

        public void setResolved(OrderedFeatures resolved) {
            _resolved.clear();
            _resolved.putAll(resolved);
        }
    }
}
//@formatter:on