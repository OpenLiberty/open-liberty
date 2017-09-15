/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.VersionUtility;

/**
 * Package-private utility class helping with management of variables used
 * by {@link SubsystemFeatureDefinitionImpl}.
 */
public class FeatureDefinitionUtils {
    private static final TraceComponent tc = Tr.register(FeatureDefinitionUtils.class);

    static final String EMPTY = "";

    static final String SYMBOLIC_NAME = "Subsystem-SymbolicName";
    static final String TYPE = "Subsystem-Type";
    static final String VERSION = "Subsystem-Version";
    static final String CONTENT = "Subsystem-Content";

    static final String SHORT_NAME = "IBM-ShortName";
    static final String IBM_FEATURE_VERSION = "IBM-Feature-Version";
    static final String IBM_APP_FORCE_RESTART = "IBM-App-ForceRestart";
    public static final String IBM_API_SERVICE = "IBM-API-Service";
    public static final String IBM_API_PACKAGE = "IBM-API-Package";
    public static final String IBM_SPI_PACKAGE = "IBM-SPI-Package";
    public static final String IBM_PROVISION_CAPABILITY = "IBM-Provision-Capability";
    public static final String IBM_PROCESS_TYPES = "IBM-Process-Types";

    static final String FILTER_ATTR_NAME = "filter";
    static final String FILTER_FEATURE_KEY = "osgi.identity";
    static final String FILTER_TYPE_KEY = "type";

    static final List<String> LOCALIZABLE_HEADERS = Collections.unmodifiableList(Arrays.asList(new String[] { "Subsystem-Name", "Subsystem-Description" }));

    public final static Collection<String> ALLOWED_ON_CLIENT_ONLY_FEATURES = Arrays.asList("com.ibm.websphere.appserver.javaeeClient-7.0",
                                                                                           "com.ibm.websphere.appserver.appSecurityClient-1.0");

    public static final String NL = "\r\n";
    static final String SPLIT_CHAR = ";";
    static final Pattern splitPattern = Pattern.compile(SPLIT_CHAR);
    static final String FEATURE_SPLIT_CHAR = ",";
    static final Pattern installedFeatureSplitPattern = Pattern.compile(FEATURE_SPLIT_CHAR);

    /**
     * All attributes of this class are final/immutable.
     * This class is an internal implementation detail
     * of {@link SubsystemFeatureDefinitionImpl}, and as
     * such, is package protected. We won't be creating a host
     * of getters for these variables, which is fine, provided this
     * class remains package-private!
     * 
     */
    static class ImmutableAttributes {
        final String bundleRepositoryType;
        final String featureName;
        final String symbolicName;
        final String shortName;
        final int featureVersion;
        final Visibility visibility;
        final AppForceRestart appRestart;
        final EnumSet<ProcessType> processTypes;
        final Version version;
        final boolean isAutoFeature;
        final boolean hasApiPackages;
        final boolean hasSpiPackages;
        final boolean hasApiServices;
        final boolean isSingleton;
        final File featureFile;
        final long lastModified;
        final long length;
        public File checksumFile;

        ImmutableAttributes(String repoType,
                            String symbolicName,
                            String shortName,
                            int featureVersion,
                            Visibility visibility,
                            AppForceRestart appRestart,
                            Version version,
                            File featureFile,
                            long lastModified,
                            long fileSize,
                            boolean isAutoFeature,
                            boolean hasApiServices,
                            boolean hasApiPackages,
                            boolean hasSpiPackages,
                            boolean isSingleton,
                            EnumSet<ProcessType> processType) {

            this.bundleRepositoryType = repoType;
            this.symbolicName = symbolicName;
            this.shortName = shortName;
            this.featureName = buildFeatureName(repoType, symbolicName, shortName);
            this.featureVersion = featureVersion;
            this.visibility = visibility;
            this.appRestart = appRestart;
            this.processTypes = processType;
            this.version = version;

            this.isAutoFeature = isAutoFeature;
            this.hasApiServices = hasApiServices;
            this.hasApiPackages = hasApiPackages;
            this.hasSpiPackages = hasSpiPackages;
            this.isSingleton = isSingleton;

            this.featureFile = featureFile;
            if (featureFile != null) {
                this.checksumFile = new File(featureFile.getParentFile(), "checksums/" + symbolicName + ".cs");
            }
            this.lastModified = lastModified;
            this.length = fileSize;
        }

        /**
         * Build the feature name, which is used for provisioning operations and lookups.
         * 
         * @param repoType
         * @param symbolicName2
         * @param shortName2
         * @return
         */
        private String buildFeatureName(String repoType, String symbolicName, String shortName) {
            if (repoType == null || repoType.isEmpty()) {
                if (shortName != null)
                    return shortName;
                else
                    return symbolicName;
            } else {
                StringBuilder s = new StringBuilder();
                s.append(repoType).append(":");

                // Use the shortname if there is one, otherwise fall back to the symbolic name
                if (shortName != null)
                    s.append(shortName);
                else
                    s.append(symbolicName);

                return s.toString();
            }
        }

        File getLocalizationDirectory() {
            if (featureFile == null)
                return null;

            return new File(featureFile.getParentFile(), "l10n");
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ImmutableAttributes other = (ImmutableAttributes) obj;
            if (symbolicName == null) {
                if (other.symbolicName != null)
                    return false;
            } else if (!symbolicName.equals(other.symbolicName))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            if (shortName == null) {
                if (other.shortName != null)
                    return false;
            } else if (!shortName.equals(other.shortName))
                return false;
            if (bundleRepositoryType == null) {
                if (other.bundleRepositoryType != null)
                    return false;
            } else if (!bundleRepositoryType.equals(other.bundleRepositoryType))
                return false;
            return true;
        }

        /**
         * @return true if this is a supported feature version: either the IBM-Feature-Version
         *         was unspecified (0), or is 2.
         */
        public boolean isSupportedFeatureVersion() {
            return featureVersion == 0 || featureVersion == 2;
        }

        @Override
        public String toString() {
            return (featureName == symbolicName ? "" : featureName + '/') + symbolicName + '/' + version;
        }
    }

    /**
     * Create the ImmutableAttributes based on the contents read from a subsystem
     * manifest.
     * 
     * @param details ManifestDetails containing manifest parser and accessor methods
     *            for retrieving information from the manifest.
     * @return new ImmutableAttributes
     */
    static ImmutableAttributes loadAttributes(String repoType, File featureFile, ProvisioningDetails details) throws IOException {

        // This will throw exceptions if required attributes mismatch or are missing
        details.ensureValid();

        // retrieve the symbolic name and feature manifest version
        String symbolicName = details.getNameAttribute(null);
        int featureVersion = details.getIBMFeatureVersion();

        // Directive names are name attributes, but end with a colon
        Visibility visibility = Visibility.fromString(details.getNameAttribute("visibility:"));
        boolean isSingleton = Boolean.parseBoolean(details.getNameAttribute("singleton:"));

        // ignore short name for features that are not public
        String shortName = (visibility != Visibility.PUBLIC ? null : details.getMainAttributeValue(SHORT_NAME));

        // retrieve the feature/subsystem version 
        Version version = VersionUtility.stringToVersion(details.getMainAttributeValue(VERSION));

        // retrieve the app restart header
        AppForceRestart appRestart = AppForceRestart.fromString(details.getMainAttributeValue(IBM_APP_FORCE_RESTART));

        String subsystemType = details.getMainAttributeValue(TYPE);

        String value = details.getCachedRawHeader(IBM_PROVISION_CAPABILITY);
        boolean isAutoFeature = value != null && SubsystemContentType.FEATURE_TYPE.getValue().equals(subsystemType);

        value = details.getCachedRawHeader(IBM_API_SERVICE);
        boolean hasApiServices = value != null;

        value = details.getCachedRawHeader(IBM_API_PACKAGE);
        boolean hasApiPackages = value != null;

        value = details.getCachedRawHeader(IBM_SPI_PACKAGE);
        boolean hasSpiPackages = value != null;

        EnumSet<ProcessType> processTypes = ProcessType.fromString(details.getCachedRawHeader(IBM_PROCESS_TYPES));

        ImmutableAttributes iAttr = new ImmutableAttributes(emptyIfNull(repoType),
                                                            symbolicName,
                                                            nullIfEmpty(shortName),
                                                            featureVersion,
                                                            visibility,
                                                            appRestart,
                                                            version,
                                                            featureFile,
                                                            featureFile == null ? -1 : featureFile.lastModified(),
                                                            featureFile == null ? -1 : featureFile.length(),
                                                            isAutoFeature, hasApiServices,
                                                            hasApiPackages, hasSpiPackages, isSingleton,
                                                            processTypes);

        // Link the details object and immutable attributes (used for diagnostic purposes: 
        // the immutable attribute values are necessary for meaningful error messages)
        details.setImmutableAttributes(iAttr);

        return iAttr;
    }

    /**
     * Create the ImmutableAttributes based on an line in a cache file.
     * There is no validation or warnings in this load path, as it is assumed the
     * definition would not have been added to the cache if it were invalid.
     * 
     * @param line Line containing feature information from the cache file
     * @param cachedEntry the previous attributes from the cache. This will be null for initial provisioning.
     * 
     * @return If the backing file has not changed, either the existing ImmutableAttributes or a
     *         new ImmutableAttributes object will be returned. If the backing file has changed,
     *         null will be returned.
     */
    static ImmutableAttributes loadAttributes(String line,
                                              ImmutableAttributes cachedAttributes) {
        // Builder pattern for Immutable attributes
        // This parses a cache line that looks like this: 
        // repoType|symbolicName=Lots;of;attribtues

        int index = line.indexOf('=');
        String key = line.substring(0, index);

        String repoType = FeatureDefinitionUtils.EMPTY;
        String symbolicName = key;

        // Do we have a prefix repoType? If so, split the key.
        int pfxIndex = key.indexOf(':');
        if (pfxIndex > -1) {
            repoType = key.substring(0, pfxIndex);
            symbolicName = key.substring(pfxIndex + 1);
        }

        // Now let's work on the value half... 
        String[] parts = splitPattern.split(line.substring(index + 1));

        // Old or mismatched cache
        if (parts.length < 9)
            return null;

        String path = parts[0];

        // Make sure file information from cache line is still accurate
        // i.e. the file exists, and the modification time and size have not changed.
        File featureFile = new File(path);
        if (featureFile.exists()) {
            // Did we have a previous entry in the cache already?
            // This will happen on a subsequent feature update operation (post-initial provisioning)
            if (cachedAttributes != null) {
                return cachedAttributes;
            }

            // Assuming we're still good, but didn't have a cache entry, we should read the rest
            // of the attributes.. 
            long lastModified = getLongValue(parts[1], -1);
            long fileSize = getLongValue(parts[2], -1);
            String shortName = parts[3];
            int featureVersion = getIntegerValue(parts[4], 2);
            Visibility visibility = Visibility.fromString(parts[5]);
            AppForceRestart appRestart = AppForceRestart.fromString(parts[6]);
            Version version = VersionUtility.stringToVersion(parts[7]);

            String flags = parts[8];
            boolean isAutoFeature = toBoolean(flags.charAt(0));
            boolean hasApiServices = toBoolean(flags.charAt(1));
            boolean hasApiPackages = toBoolean(flags.charAt(2));
            boolean hasSpiPackages = toBoolean(flags.charAt(3));
            boolean isSingleton = flags.length() > 4 ? toBoolean(flags.charAt(4)) : false;

            EnumSet<ProcessType> processTypes = ProcessType.fromString(parts.length > 9 ? parts[9] : null);

            // Everything is ok with the cache contents, 
            // make a new subsystem definition from the cached information
            return new ImmutableAttributes(emptyIfNull(repoType),
                                           symbolicName,
                                           nullIfEmpty(shortName),
                                           featureVersion,
                                           visibility,
                                           appRestart,
                                           version,
                                           featureFile,
                                           lastModified,
                                           fileSize,
                                           isAutoFeature, hasApiServices,
                                           hasApiPackages, hasSpiPackages, isSingleton,
                                           processTypes);
        }

        // The definition in the filesystem has been deleted: return null to remove it from the cache
        return null;
    }

    static void writeAttributes(ImmutableAttributes iAttr, ProvisioningDetails details, PrintWriter writer) throws IOException {
        if (iAttr == null || details == null) // programmer error
            throw new NullPointerException("Both attributes and details are required for caching: attr=" + iAttr + ", details=" + details);

        // If we have a bundle repository specified for this resource, put it in there first.. 
        if (iAttr.bundleRepositoryType != null && !iAttr.bundleRepositoryType.isEmpty()) {
            writer.write(iAttr.bundleRepositoryType);
            writer.write(':');
        }

        writer.write(iAttr.symbolicName);
        writer.write('=');
        writer.write(iAttr.featureFile == null ? EMPTY : iAttr.featureFile.getAbsolutePath()); // 0
        writer.write(SPLIT_CHAR);
        writer.write(String.valueOf(iAttr.lastModified)); // 1
        writer.write(SPLIT_CHAR);
        writer.write(String.valueOf(iAttr.length)); // 2
        writer.write(SPLIT_CHAR);
        writer.write(iAttr.shortName == null ? EMPTY : iAttr.shortName); // 3
        writer.write(SPLIT_CHAR);
        writer.write(String.valueOf(iAttr.featureVersion)); // 4
        writer.write(SPLIT_CHAR);
        writer.write(iAttr.visibility.toString()); // 5
        writer.write(SPLIT_CHAR);
        writer.write(iAttr.appRestart.toString()); // 6 
        writer.write(SPLIT_CHAR);
        writer.write(iAttr.version.toString()); // 7
        writer.write(SPLIT_CHAR);
        writeFlags(writer, iAttr.isAutoFeature, iAttr.hasApiServices, iAttr.hasApiPackages, iAttr.hasSpiPackages, iAttr.isSingleton); // 8
        writer.write(SPLIT_CHAR);
        writer.write(ProcessType.toString(iAttr.processTypes)); // 9
        writer.write(NL);

        // @see ProvisioningDetails ctor
        if (iAttr.isAutoFeature) {
            writer.write("-C:");
            writer.write(details.getCachedRawHeader(IBM_PROVISION_CAPABILITY));
            writer.write(NL);
        }
        if (iAttr.hasApiServices) {
            writer.write("-V:");
            writer.write(details.getCachedRawHeader(IBM_API_SERVICE));
            writer.write(NL);
        }
        if (iAttr.hasApiPackages) {
            writer.write("-A:");
            writer.write(details.getCachedRawHeader(IBM_API_PACKAGE));
            writer.write(NL);
        }
        if (iAttr.hasSpiPackages) {
            writer.write("-S:");
            writer.write(details.getCachedRawHeader(IBM_SPI_PACKAGE));
            writer.write(NL);
        }
    }

    private static void writeFlags(PrintWriter writer, boolean... flags) {
        for (boolean flag : flags) {
            writer.write(flag ? '1' : '0');
        }
    }

    private static boolean toBoolean(char flag) {
        if (flag == '1')
            return true;
        return false;
    }

    @FFDCIgnore(NumberFormatException.class)
    static long getLongValue(String value, long defaultValue) {
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException nex) {
            }
        }
        return defaultValue;
    }

    @FFDCIgnore(NumberFormatException.class)
    static int getIntegerValue(String value, int defaultValue) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nex) {
            }
        }
        return defaultValue;
    }

    /**
     * For some provisioning operations, we need the details of the subsystem:
     * we need access to the manifest and the data that it contains.
     * We store this information in a nested class to allow it to be removed
     * after provisioning operations complete.
     * <p>
     * Mutable attributes are private, to allow coordinated set/retrieval.
     * 
     */
    final static class ProvisioningDetails {
        private Manifest manifest = null;
        private ImmutableAttributes iAttr = null;

        // Pick something random that reading the header is sure to replace. 
        // MIN_VALUE is very unlikely to be in someone's manifest.
        private static final int featureVersion = Integer.MIN_VALUE;

        private String autoFeatureCapability = null;
        private String apiPackages = null;
        private String spiPackages = null;
        private String apiServices = null;

        private boolean supersededChecked = false;
        private String supersededBy = null;

        private Collection<FeatureResource> subsystemContent = null;
        private Collection<Filter> featureCapabilityFilters = null;
        private Map<String, Collection<HeaderElementDefinition>> headerElements = null;

        private String symbolicName = null;
        private Map<String, String> symNameAttributes = null;

        /**
         * The Manifest is required so we can build ImmutableAttributes from
         * the file contents.
         * 
         * @param inStream The InputStream that should be read to create the manifest
         * @param object
         */
        ProvisioningDetails(File mfFile, InputStream inStream) throws IOException {
            manifest = loadManifest(mfFile, inStream);
        }

        /**
         * The ImmutableAttributes were built first from the cache. We'll
         * get the manifest later.
         * 
         * @param iAttr
         */
        ProvisioningDetails(BufferedReader reader, ImmutableAttributes iAttr) throws IOException {
            if (iAttr == null || reader == null) // Programmer error 
                throw new NullPointerException("Reader and Attributes must not be null: reader=" + reader + ", attr=" + iAttr);

            this.iAttr = iAttr;

            // These are primitive and cryptic: the internal details of a cache file.
            // It doesn't matter what this character is, it just helps make sure the cache
            // file was written the way we intend to read it.
            if (iAttr.isAutoFeature) {
                autoFeatureCapability = getExtraLine(iAttr.featureName, IBM_PROVISION_CAPABILITY, 'C', reader);
            }
            if (iAttr.hasApiServices) {
                apiServices = getExtraLine(iAttr.featureName, IBM_API_SERVICE, 'V', reader);
            }
            if (iAttr.hasApiPackages) {
                apiPackages = getExtraLine(iAttr.featureName, IBM_API_PACKAGE, 'A', reader);
            }
            if (iAttr.hasSpiPackages) {
                spiPackages = getExtraLine(iAttr.featureName, IBM_SPI_PACKAGE, 'S', reader);
            }
        }

        /**
         * Per ctor: will either have read the manifest OR will have manifest file.
         * <p>
         * Assumption: this is called during a provisioning operation, which is
         * active on a single thread (guards in the FeatureManager ensure only
         * one feature provisioning operation on one thread is active at a time).
         * 
         * @return
         */
        private Manifest getManifest() throws IOException {
            Manifest mf = manifest;
            if (mf == null) {
                mf = manifest = loadManifest(iAttr.featureFile, null);
            }
            return mf;
        }

        private Map<String, Collection<HeaderElementDefinition>> getHeaderElementMap() {
            if (headerElements == null) {
                headerElements = new HashMap<String, Collection<HeaderElementDefinition>>();
            }
            return headerElements;
        }

        String getMainAttributeValue(String key) throws IOException {
            return getManifest().getMainAttributes().getValue(key);
        }

        /**
         * Validation of the symbolic name is done in this header (as it is required
         * very early in construction of immutable attributes).
         * <p>
         * The symbolic name header contains both the symbolic name and the
         * 
         * @return
         * @throws FeatureManifestException if the attribute is missing from the manifest
         * 
         */
        String getNameAttribute(String key) throws IOException {
            Map<String, String> attr = symNameAttributes;
            if (attr == null) {
                // Get Subsystem-SymbolicName, which is both the name AND attributes/directives
                String nameHeader = getMainAttributeValue(SYMBOLIC_NAME);
                if (nameHeader == null) {
                    // TODO: GET RID OF THIS WHEN WE HAVE PROPER NLS MESSAGE STRING
                    final String fakeNLS = "The required {0} header was missing or empty";

                    // TODO: Replace with proper NLS message!
                    String message = Tr.formatMessage(tc, fakeNLS, SYMBOLIC_NAME);

                    // TODO: Replace with proper NLS message!
                    if (tc.isEventEnabled())
                        Tr.event(tc, message);

                    throw new FeatureManifestException("Unable to read " + SYMBOLIC_NAME + " header from manifest",
                                                       message);
                }

                NameValuePair nvp = ManifestHeaderProcessor.parseBundleSymbolicName(nameHeader);
                symbolicName = nvp.getName();
                attr = symNameAttributes = nvp.getAttributes();
            }

            if (key == null)
                return symbolicName;
            else if (attr != null)
                return attr.get(key);
            else
                return null;
        }

        void setImmutableAttributes(ImmutableAttributes iAttr) {
            this.iAttr = iAttr;
        }

        /**
         * Verify required attributes for valid feature definitions are present.
         * 
         * @throws FeatureManifestException for invalid manifest information
         */
        void ensureValid() throws IOException {
            // TODO: GET RID OF THIS WHEN WE HAVE PROPER NLS MESSAGE STRING
            final String fakeNLS = "The {0} header in feature {1} was missing or specified an invalid value. value={2}";

            // Make sure the symbolic name attribute is present and readable
            String symbolicName = getNameAttribute(null);

            // Make sure the feature version is valid
            int fVersion = getIBMFeatureVersion();

            // If the version is 0 (unspecified) or > 2, it's invalid and we yell. 
            // If it's < 2 (i.e. version 1) we just ignore it.
            if (fVersion > 2) {
                Tr.error(tc, "UNSUPPORTED_FEATURE_VERSION", symbolicName, fVersion);

                String message = Tr.formatMessage(tc, "UNSUPPORTED_FEATURE_VERSION", symbolicName, fVersion);
                throw new FeatureManifestException("Unsupported feature version",
                                                   message);
            }

            // If the Subsystem-Type header is null, or doesn't match the feature type... 
            String type = getMainAttributeValue(TYPE);
            if (type == null || !SubsystemContentType.FEATURE_TYPE.getValue().equals(type)) {
                // TODO: Replace with proper NLS message!
                String message = Tr.formatMessage(tc, fakeNLS,
                                                  TYPE,
                                                  symbolicName,
                                                  type);

                // TODO: replace with Tr.error
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, message);

                throw new FeatureManifestException("Invalid value for " + TYPE + " manifest header: " + type,
                                                   message);
            }

            // If the Subsystem-Type header is null, or doesn't match the feature type... 
            String version = getMainAttributeValue(VERSION);
            if (version == null) {
                // TODO: Replace with proper NLS message!
                String message = Tr.formatMessage(tc, fakeNLS,
                                                  VERSION,
                                                  symbolicName,
                                                  null);

                // TODO: replace with Tr.error
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, message);

                throw new FeatureManifestException("Null value for " + VERSION + " manifest header",
                                                   message);
            }
        }

        /**
         * Return the feature version
         * 
         * @return feature version
         */
        int getIBMFeatureVersion() throws IOException {
            int version = featureVersion;
            if (version < 0) {
                version = getIntegerValue(getMainAttributeValue(IBM_FEATURE_VERSION), 0);
            }

            return version;
        }

        String getCachedRawHeader(String header) {
            String result = null;
            try {
                if (IBM_PROVISION_CAPABILITY.equals(header)) {
                    result = autoFeatureCapability;
                    if (result == null) {
                        result = autoFeatureCapability = getMainAttributeValue(IBM_PROVISION_CAPABILITY);
                    }
                } else if (IBM_API_SERVICE.equals(header)) {
                    result = apiServices;
                    if (result == null) {
                        result = apiServices = getMainAttributeValue(IBM_API_SERVICE);
                    }
                } else if (IBM_API_PACKAGE.equals(header)) {
                    result = apiPackages;
                    if (result == null) {
                        result = apiPackages = getMainAttributeValue(IBM_API_PACKAGE);
                    }
                } else if (IBM_SPI_PACKAGE.equals(header)) {
                    result = spiPackages;
                    if (result == null) {
                        result = spiPackages = getMainAttributeValue(IBM_SPI_PACKAGE);
                    }
                } else {
                    result = getMainAttributeValue(header);
                }
            } catch (IOException e) {
                // Manifest file should have been verified for existence at this point.. 
            }
            return result;
        }

        Collection<Filter> getCapabilityFilters() {
            // Check to see if we've already setup the filters from the capability header
            Collection<Filter> filters = featureCapabilityFilters;

            // If we don't then we need to process the manifest header if there is one.
            if (filters == null) {
                String capabilityHeader = getCachedRawHeader(IBM_PROVISION_CAPABILITY);

                // If we do have a capability header then parse it to get the filters.
                if (capabilityHeader != null) {
                    filters = new ArrayList<Filter>();

                    // For each namespace in the header get the header data.
                    for (GenericMetadata metadata : ManifestHeaderProcessor.parseCapabilityString(capabilityHeader)) {
                        String filterString = metadata.getDirectives().get(FILTER_ATTR_NAME);
                        if (filterString != null) {
                            try {
                                // If we have a filter string, then create an OSGi filter from it and
                                // store it in the list.
                                Filter filter = FrameworkUtil.createFilter(filterString);
                                filters.add(filter);
                            } catch (InvalidSyntaxException ise) {
                                Tr.warning(tc, "INVALID_PROVISION_CAPABILITY_FILTER", new Object[] { filterString,
                                                                                                    iAttr.symbolicName,
                                                                                                    ise.getMessage() });
                            }
                        }
                    }

                    // Store the list regardless of whether we have filters or not.
                    featureCapabilityFilters = filters;
                } else {
                    filters = featureCapabilityFilters = Collections.emptyList();
                }
            }
            return filters;
        }

        Collection<FeatureResource> getConstituents(SubsystemContentType type) {
            // Check to see if we've already figured out our content... 
            Collection<FeatureResource> result = subsystemContent;

            if (result == null) {
                String contents = null;
                try {
                    contents = getMainAttributeValue(CONTENT);
                } catch (IOException e) {
                    // We should be beyond any issue reading the manifest at this point.. 
                    return Collections.emptyList();
                }

                Map<String, Map<String, String>> data = ManifestHeaderProcessor.parseImportString(contents);

                result = new ArrayList<FeatureResource>(data.size());
                for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
                    result.add(new FeatureResourceImpl(entry.getKey(), entry.getValue(), iAttr.bundleRepositoryType, iAttr.featureName));
                }

                subsystemContent = result;
            }

            if (type != null) {
                Collection<FeatureResource> unfiltered = result;

                result = new ArrayList<FeatureResource>();
                for (FeatureResource resource : unfiltered) {
                    if (resource.isType(type)) {
                        result.add(resource);
                    }
                }
            }

            return Collections.unmodifiableCollection(result);
        }

        Collection<HeaderElementDefinition> getRawHeaderElements(String header) {

            Collection<HeaderElementDefinition> elements = getHeaderElementMap().get(header);
            if (elements == null) {
                String contents = getCachedRawHeader(header);

                if (contents == null || contents.isEmpty())
                    return Collections.emptyList();

                List<NameValuePair> data = ManifestHeaderProcessor.parseExportString(contents);
                Iterator<NameValuePair> listIterator = data.listIterator();

                elements = new ArrayList<HeaderElementDefinition>(data.size());
                while (listIterator.hasNext()) {
                    NameValuePair element = listIterator.next();
                    elements.add(new FeatureResourceImpl(element.getName(), element.getAttributes(), iAttr.bundleRepositoryType, iAttr.featureName));
                }

                elements = Collections.unmodifiableCollection(elements);
                getHeaderElementMap().put(header, elements);
            }

            return elements;
        }

        private void checkSuperseded() {
            if (supersededChecked)
                return;

            supersededChecked = true;
            try {
                String value = getNameAttribute("superseded-by");
                if (Boolean.parseBoolean(getNameAttribute("superseded")) == false && value != null) {
                    // superseded attribute is false but the value of superseded-by is not null, display an error
                    Tr.error(tc, "SUPERSEDED_CONFIGURATION_ERROR", new Object[] { iAttr.featureName, value });
                } else {
                    supersededBy = value;
                }
            } catch (IOException e) {
                // We should be beyond any issue reading the manifest at this point.. 
            }
        }

        boolean isSuperseded() {
            checkSuperseded();
            return supersededBy != null;
        }

        String getSupersededBy() {
            checkSuperseded();
            return supersededBy;
        }

        public void setHeaderValue(String header, String value) {
            try {
                getManifest().getMainAttributes().putValue(header, value);
            } catch (IOException e) {
                // We should be beyond any issue reading the manifest at this point.. (FFDC)
            }
        }

        @Override
        public String toString() {
            return (iAttr == null ? "noAttr" : iAttr.toString())
                   + "(loaded"
                   + (manifest == null ? "" : "+")
                   + (headerElements == null ? "" : "%")
                   + ")";
        }
    }

    @Trivial
    public static String emptyIfNull(String str) {
        if (str == null)
            return EMPTY;
        return str;
    }

    @Trivial
    public static String nullIfEmpty(String str) {
        if (str != null && str.isEmpty())
            return null;
        return str;
    }

    @FFDCIgnore(IOException.class)
    private static Manifest loadManifest(File mfFile, InputStream in) throws IOException {
        try {
            if (in == null)
                in = new FileInputStream(mfFile);

            try {
                return ManifestProcessor.parseManifest(in);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                // the manifest parser can throw any number of runtime exceptions if the 
                // format of the file is incorrect.
                // instead of handling invalid input in the parser we just wrap the exception and move on
                throw new IOException(e.getMessage(), e);
            }
        } finally {
            tryToClose(in);
        }
    }

    private static String getExtraLine(String name, String descr, char verify, BufferedReader reader) throws IOException {
        String line = reader.readLine();

        if (line == null || line.charAt(0) != '-' || line.charAt(1) != verify || line.charAt(2) != ':')
            throw new IOException("Missing or invalid cache entry for " + descr + " for " + name + ", line=" + line);

        return line.substring(3);
    }

    /**
     * Close the closeable object: handle null, swallow exceptions.
     * This is called by finally blocks.
     * 
     * @param closeable Stream to close
     */
    @Trivial
    @FFDCIgnore(IOException.class)
    public static boolean tryToClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }
}