/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ImmutableAttributes;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ProvisioningDetails;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.wsspi.kernel.feature.LibertyFeature;

/**
 *
 */
public class SubsystemFeatureDefinitionImpl implements ProvisioningFeatureDefinition, LibertyFeature {

    private static final TraceComponent tc = Tr.register(SubsystemFeatureDefinitionImpl.class);

    /** Immutable attributes of the subsystem feature definition */
    private final ImmutableAttributes iAttr;

    /** Object containing temporary attributes fetched during provisioning operations */
    private volatile ProvisioningDetails mfDetails;

    /** FIXME: this is temporary.. */
    private final String apiServiceElements;

    // The bundles known to be in the feature.
    private final AtomicReference<Collection<Bundle>> featureBundles = new AtomicReference<Collection<Bundle>>();

    private final AtomicInteger detailUsers = new AtomicInteger(0);

    /**
     * Create a new subsystem definition with the specified immutable attributes.
     * Called when rebuilding from a cache.
     *
     * @param attr    Immutable attributes
     * @param details Provisioning details (will be cleared when provisioning operation is complete)
     * @see #load(String, SubsystemFeatureDefinitionImpl)
     * @see FeatureDefinitionUtils#loadAttributes(String, ImmutableAttributes)
     */
    SubsystemFeatureDefinitionImpl(ImmutableAttributes attr, ProvisioningDetails details) {
        setProvisioningDetails(details);
        this.iAttr = attr;

        if (iAttr.hasApiServices)
            apiServiceElements = mfDetails.getCachedRawHeader(FeatureDefinitionUtils.IBM_API_SERVICE);
        else
            apiServiceElements = null;
    }

    /**
     * Create a new subsystem definition by reading information from the
     * specified input stream. Notice in this case we do not know where the
     * backing manifest file is (e.g. we're reading an entry from a zip).
     * <p>
     * Some operations, like finding resource bundles, may not work.
     *
     * @param repoType    emtpy/null for core, "usr" for user extension, or the product
     *                        extension name
     * @param inputStream The input stream to read from
     * @see ExtensionConstants#CORE_EXTENSION
     * @see ExtensionConstants#USER_EXTENSION
     * @see #load(String, File, InputStream)
     */
    public SubsystemFeatureDefinitionImpl(String repoType, InputStream inputStream) throws IOException {
        setProvisioningDetails(new ProvisioningDetails(null, inputStream));
        iAttr = FeatureDefinitionUtils.loadAttributes(repoType, null, mfDetails);

        if (iAttr.hasApiServices)
            apiServiceElements = mfDetails.getCachedRawHeader(FeatureDefinitionUtils.IBM_API_SERVICE);
        else
            apiServiceElements = null;
    }

    /**
     * Create a new subsystem definition by reading information from the
     * specified input stream.
     *
     * @param repoType emtpy/null for core, "usr" for user extension, or the product
     *                     extension name
     * @param file     Subsystem feature definition manifest file
     *
     * @see ExtensionConstants#CORE_EXTENSION
     * @see ExtensionConstants#USER_EXTENSION
     * @see #load(String, File, InputStream)
     */
    public SubsystemFeatureDefinitionImpl(String repoType, File file) throws IOException {
        mfDetails = new ProvisioningDetails(file, null);
        iAttr = FeatureDefinitionUtils.loadAttributes(repoType, file, mfDetails);

        if (iAttr.hasApiServices)
            apiServiceElements = mfDetails.getCachedRawHeader(FeatureDefinitionUtils.IBM_API_SERVICE);
        else
            apiServiceElements = null;
    }

    ImmutableAttributes getImmutableAttributes() {
        return iAttr;
    }

    ProvisioningDetails getProvisioningDetails() {
        return mfDetails;
    }

    /**
     * Set the provisioning details: used when preparing existing subsystem definition
     * for provisioning operation
     *
     * @param details reconstituted provisioning details
     */
    @Trivial
    synchronized void setProvisioningDetails(ProvisioningDetails details) {
        // In general, we want to allow the provisioning details to be completely cleaned up
        // when the provisioning operation is completed.
        // If LibertyFeature service has requested provisioning details, then we need to keep
        // them around until that reference is cleaned up
        if (details == null) {
            if (detailUsers.decrementAndGet() <= 0) {
                // we are free to clear the details so they can be garbage collected.
                featureBundles.set(null);
                mfDetails = null;
            }
        } else {
            // Refresh the reference with whatever is the latest (previous would be garbage collected)
            mfDetails = details;
            detailUsers.incrementAndGet();
        }
    }

    @Override
    public File getFeatureDefinitionFile() {
        return iAttr.featureFile;
    }

    @Override
    public File getFeatureChecksumFile() {
        return iAttr.getChecksumFile();
    }

    @Override
    public String getFeatureName() {
        return iAttr.featureName;
    }

    @Override
    public String getSymbolicName() {
        return iAttr.symbolicName;
    }

    @Override
    public String getIbmShortName() {
        return iAttr.shortName;
    }

    @Override
    public int getIbmFeatureVersion() {
        return iAttr.featureVersion;
    }

    @Override
    public Version getVersion() {
        return iAttr.version;
    }

    @Override
    public AppForceRestart getAppForceRestart() {
        return iAttr.appRestart;
    }

    @Override
    public Visibility getVisibility() {
        return iAttr.visibility;
    }

    @Override
    public EnumSet<ProcessType> getProcessTypes() {
        return iAttr.processTypes;
    }

    @Override
    public boolean isSingleton() {
        return iAttr.isSingleton;
    };

    @Override
    public String getBundleRepositoryType() {
        return iAttr.bundleRepositoryType;
    }

    @Override
    public boolean isSupportedFeatureVersion() {
        return iAttr.isSupportedFeatureVersion();
    }

    @Override
    public boolean isAutoFeature() {
        return iAttr.isAutoFeature;
    }

    @Override
    public String getApiServices() {
        return apiServiceElements;
    }

    @Override
    public Collection<File> getLocalizationFiles() {
        File dir = iAttr.getLocalizationDirectory();
        File[] files = null;
        if (dir != null && dir.isDirectory()) {
            files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    // KEEP IN SYNC WITH getResourceBundle !!
                    return name.equals(iAttr.symbolicName + ".properties") || (name.startsWith(iAttr.symbolicName + "_") && name.endsWith(".properties"));
                }
            });
        }
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(files);
    }

    protected ResourceBundle getResourceBundle(Locale locale) {
        File dir = iAttr.getLocalizationDirectory();

        //KEEP IN SYNC WITH getLocalizationFiles !!!
        File[] files = new File[] { new File(dir, iAttr.symbolicName + "_" + locale.toString() + ".properties"),
                                    new File(dir, iAttr.symbolicName + "_" + locale.getLanguage() + ".properties"),
                                    new File(dir, iAttr.symbolicName + ".properties") };

        for (File file : files) {
            if (file.exists()) {
                try {
                    return new PropertyResourceBundle(new FileReader(file));
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<FeatureResource> getConstituents(SubsystemContentType type) {
        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        return mfDetails.getConstituents(type);
    }

    @Override
    public String getHeader(String header) {
        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        try {
            return mfDetails.getMainAttributeValue(header);
        } catch (IOException e) {
            // We should be well beyond any IOException issues obtaining the manifest..
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "IOException reading manifest attribute from {0}: {1}", iAttr.featureFile, e);
            }
        }
        return null;
    }

    @Override
    public String getHeader(String header, Locale locale) {
        // Get the value for the header....
        String value = getHeader(header);

        // if null, empty, or no target locale, just return it
        if (value == null || value.isEmpty() || locale == null)
            return value;

        // If this is a localizable header that indicates it wants to be localized...
        if (value.charAt(0) == '%' && FeatureDefinitionUtils.LOCALIZABLE_HEADERS.contains(header)) {
            // Find the resource bundle...
            ResourceBundle rb = getResourceBundle(locale);
            if (rb != null) {
                // Find the new value in the resource bundle
                value = rb.getString(value.substring(1));
            }
        }

        return value;
    }

    @Override
    public Collection<HeaderElementDefinition> getHeaderElements(String header) {
        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        return mfDetails.getRawHeaderElements(header);
    }

    void setHeader(String header, String value) {
        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        mfDetails.setHeaderValue(header, value);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return iAttr.hashCode();
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

        // now defer to immutable attributes
        SubsystemFeatureDefinitionImpl other = (SubsystemFeatureDefinitionImpl) obj;
        return this.iAttr.equals(other.iAttr);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSuperseded() {
        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        return mfDetails.isSuperseded();
    }

    /** {@inheritDoc} */
    @Override
    public String getSupersededBy() {
        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        return mfDetails.getSupersededBy();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.feature.FeatureDefinition#isCapabilitySatified(java.util.Collection)
     */
    @Override
    public boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck) {
        // If it isn't an autofeature, it's satisfied.
        if (!iAttr.isAutoFeature)
            return true;

        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        boolean isCapabilitySatisfied = true;

        Iterator<Filter> iter = mfDetails.getCapabilityFilters().iterator();
        Set<ProvisioningFeatureDefinition> satisfiedFeatureDefs = new HashSet<ProvisioningFeatureDefinition>();

        // Now we need to iterate over each of the filters, until we find we don't have a match.
        while (iter.hasNext() && isCapabilitySatisfied) {
            Filter checkFilter = iter.next();
            Iterator<ProvisioningFeatureDefinition> featureDefIter = featureDefinitionsToCheck.iterator();

            // Now for each filter, iterate over each of the FeatureDefinition headers, checking to see if we have a match.
            boolean featureMatch = false;
            while (featureDefIter.hasNext() && !featureMatch) {
                ProvisioningFeatureDefinition featureDef = featureDefIter.next();

                // If we've already satisfied a capability with this FeatureDefinition, we don't need to use it again
                if (!satisfiedFeatureDefs.contains(featureDef)) {

                    // We have a mismatch between the key the filter is using to look up the feature name and the property containing the name in the
                    // headers. So we need to add a new property for osgi.identity (filter key) that contains the value of the
                    // Subsystem-SymbolicName (manifest header).
                    // We also have to do this for the Subsystem-Type(manifest header) and the type (filter key).
                    Map<String, String> filterProps = new HashMap<String, String>();

                    filterProps.put(FeatureDefinitionUtils.FILTER_FEATURE_KEY, featureDef.getSymbolicName());
                    try {
                        filterProps.put(FeatureDefinitionUtils.FILTER_TYPE_KEY,
                                        mfDetails.getMainAttributeValue(FeatureDefinitionUtils.TYPE));
                    } catch (IOException e) {
                        // We should be well beyond any IOException issues..
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "IOException reading manifest attribute from {0}: {1}", iAttr.featureFile, e);
                        }
                        continue;
                    }

                    if (checkFilter.matches(filterProps)) {
                        satisfiedFeatureDefs.add(featureDef);
                        featureMatch = true;
                    }
                }
            }
            // Once we've checked all the FeatureDefinitions, apply the result to the isCapabilitySatisfied boolean,
            // so we stop processing as soon as we know we don't have a match.
            isCapabilitySatisfied = featureMatch;
        }

        return isCapabilitySatisfied;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isKernel() {
        return false;
    }

    @Override
    public String toString() {
        if (mfDetails == null)
            return iAttr.toString();
        else
            return mfDetails.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition#getIcons()
     */
    @Override
    public Collection<String> getIcons() {
        Collection<String> result = new ArrayList<String>();
        String iconHeader = getHeader("Subsystem-Icon");
        if (iconHeader != null) {
            String[] icons = iconHeader.split(",");
            for (String icon : icons) {
                String[] iconAttrs = icon.split(";");
                // icon has form "<iconUrl>[; size=n]"
                result.add(iconAttrs[0].trim());
            }
        }
        String epIconsHeader = getHeader("Subsystem-Endpoint-Icons");
        if (epIconsHeader != null) {
            String epNamePath = "";
            String[] epIcons = epIconsHeader.split(",");
            for (String epIcon : epIcons) {
                String[] epIconAttrs = epIcon.split(";");
                if (epIconAttrs[0].indexOf("=") >= 0) {
                    // epIcon has form "<epName>=<epIconUrl>[; size=N]
                    String[] epNameAndIconUrl = epIconAttrs[0].split("=");
                    epNamePath = epNameAndIconUrl[0].trim() + "/";
                    result.add(epNamePath + epNameAndIconUrl[1].trim());
                } else {
                    // epIcon has form "<epIconUrl>[; size=n]"
                    result.add(epNamePath + epIconAttrs[0].trim());
                }
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.kernel.feature.LibertyFeature#getBundles()
     */
    @Override
    public Collection<Bundle> getBundles() {
        if (mfDetails == null)
            throw new IllegalStateException("Method called outside of provisioining operation or without a registered service");

        Collection<Bundle> bundles = featureBundles.get();

        if (bundles == null) {
            bundles = new ArrayList<Bundle>();

            Collection<FeatureResource> bundlesInFeature = mfDetails.getConstituents(SubsystemContentType.BUNDLE_TYPE);

            BundleContext ctx = FrameworkUtil.getBundle(FrameworkUtil.class).getBundleContext();

            // symbolic name to bundle map.
            for (FeatureResource bundle : bundlesInFeature) {
                String location = bundle.getLocation();
                Bundle b = ctx.getBundle(location);
                if (b != null) {
                    bundles.add(b);
                }
            }

            // make an unmodifiable version.
            bundles = Collections.unmodifiableCollection(bundles);

            // store it away if it hasn't already been read. If this is read by another
            // thread at once then this thread will get this collection, and others will
            // the winner's.
            featureBundles.compareAndSet(null, bundles);
        }

        return bundles;
    }
}
