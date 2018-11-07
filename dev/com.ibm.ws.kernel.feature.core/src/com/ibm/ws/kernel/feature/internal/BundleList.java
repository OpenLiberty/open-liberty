/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.VersionRange;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.VersionUtility;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.kernel.service.location.WsResource;

public class BundleList {
    private static final String CACHE_WRITE_TIME = "Cache-WriteTime";
    private static final TraceComponent tc = Tr.register(BundleList.class);
    private final WsResource cacheFile;
    private final AtomicBoolean stale = new AtomicBoolean(false);
    private final List<RuntimeFeatureResource> resources = new ArrayList<RuntimeFeatureResource>();
    private long writeTime;
    private Integer javaSpecVersion;
    private final FeatureManager featureManager;

    public static interface FeatureResourceHandler {
        public boolean handle(FeatureResource fr);
    }

    static final class RuntimeFeatureResource implements FeatureResource {
        private final FeatureResource fr;
        private int startLevel = -1;
        private long bundleId;
        private WsResource resource;
        private Bundle bundle;

        public RuntimeFeatureResource(FeatureResource resource) {
            fr = resource;
        }

        public RuntimeFeatureResource(String key, String value) {
            fr = new CachedFeatureResource(key, value);
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public String getSymbolicName() {
            return fr.getSymbolicName();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public VersionRange getVersionRange() {
            return fr.getVersionRange();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public Map<String, String> getAttributes() {
            return fr.getAttributes();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public Map<String, String> getDirectives() {
            return fr.getDirectives();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public String getLocation() {
            return fr.getLocation();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public List<String> getOsList() {
            return fr.getOsList();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public SubsystemContentType getType() {
            return fr.getType();
        }

        @Override
        @Trivial
        public String getRawType() {
            return fr.getRawType();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public int getStartLevel() {
            return (startLevel == -1) ? fr.getStartLevel() : startLevel;
        }

        @Override
        @Trivial
        public String toString() {
            String location = getLocation();
            return fr.toString() + ((location != null) ? '@' + location : "");
        }

        public void setBundle(Bundle bundle) {
            this.bundle = bundle;
            bundleId = bundle.getBundleId();
        }

        public void setResource(WsResource resource) {
            this.resource = resource;
        }

        public void setStartLevel(int level) {
            startLevel = level;
        }

        @Trivial
        public Bundle getBundle() {
            return bundle;
        }

        @Trivial
        public long getBundleId() {
            return bundleId;
        }

        public String getResolvedLocation(FeatureManager featureManager) throws MalformedURLException {
            if (resource != null) {
                String productName = "";
                String bundleRepositoryType = fr.getBundleRepositoryType();
                if (bundleRepositoryType != null) {
                    BundleRepositoryHolder bundleRepositoryHolder = featureManager.getBundleRepositoryHolder(bundleRepositoryType);
                    if (bundleRepositoryHolder != null) {
                        productName = bundleRepositoryHolder.getFeatureType();
                    }
                }
                // The bundle location format must match the format in provisioner and SchemaBundle.
                String urlString = Provisioner.BUNDLE_LOC_REFERENCE_TAG + resource.toExternalURI().toURL().toExternalForm();
                return Provisioner.getBundleLocation(urlString, productName);
            }
            return null;
        }

        public String getURLString() throws MalformedURLException {
            if (resource != null) {
                return resource.toExternalURI().toURL().toExternalForm();
            }

            return null;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
            result = prime * result + ((fr == null) ? 0 : fr.hashCode());
            String repoType = fr == null ? null : fr.getBundleRepositoryType();
            if (repoType != null) {
                result = prime * result + repoType.hashCode();
            }
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            // IMPLEMENTATION NOTE: this equals is really only correct for managing
            // the BundleList.resources collection when trying to avoid adding duplicates
            // Note that this does not include checking for all attributes of the
            // FeatureResource by design.  For example start level and requiredOSGiEE
            // TODO this does imply we don't have a good strategy for when two features
            // try to set different start levels for the same bundle.  Which start-level
            // gets used will look to be random.
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (obj instanceof RuntimeFeatureResource) {
                RuntimeFeatureResource other = (RuntimeFeatureResource) obj;
                if (bundle == other.bundle && bundle != null)
                    return true;
                if (!Objects.equals(getBundleRepositoryType(), other.getBundleRepositoryType()))
                    return false;

                if (bundle != other.bundle && (bundle == null || other.bundle == null)) {
                    Bundle b = (bundle == null) ? other.bundle : bundle;
                    RuntimeFeatureResource rfr = (bundle == null) ? this : other;
                    if (b.getSymbolicName().equals(rfr.getSymbolicName()) &&
                        rfr.getVersionRange().includes(b.getVersion())) {
                        return true;
                    }
                }
                if (!fr.getMatchString().equals(other.fr.getMatchString()))
                    return false;
            } else {
                return false;
            }
            return true;
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public String getMatchString() {
            return fr.getMatchString();
        }

        /** {@inheritDoc} */
        @Override
        @Trivial
        public String getBundleRepositoryType() {
            return fr.getBundleRepositoryType();
        }

        @Override
        public boolean isType(SubsystemContentType type) {
            return fr.isType(type);
        }

        @Override
        public String getExtendedAttributes() {
            return fr.getExtendedAttributes();
        }

        @Override
        public String setExecutablePermission() {
            return fr.setExecutablePermission();
        }

        @Override
        public String getFileEncoding() {
            return null;
        }

        @Override
        public List<String> getTolerates() {
            return fr.getTolerates();
        }

        @Override
        public Integer getRequireJava() {
            return fr.getRequireJava();
        }
    }

    private static final class CachedFeatureResource implements FeatureResource {

        private final String stringRepresentation;
        private final String symbolicName;
        private final VersionRange range;
        private final String location;
        private final String bundleRepositoryType;
        private final int startLevel;

        public CachedFeatureResource(String key, String value) {

            String repoType = null;
            String vRange = null;

            // This must match what the store() method writes
            // repoName|symbolicName/version=location;startlevel
            // matchString: symbolicName/version
            int ix1 = key.indexOf('|');
            if (ix1 > -1) {
                repoType = key.substring(0, ix1++);
            } else {
                ix1 = 0;
            }
            int ix2 = key.indexOf('/');
            if (ix2 != -1) {
                symbolicName = key.substring(ix1, ix2);
                vRange = key.substring(ix2 + 1);
            } else {
                symbolicName = key;
            }

            stringRepresentation = key.substring(ix1); // after repo prefix
            bundleRepositoryType = FeatureDefinitionUtils.emptyIfNull(repoType);
            range = VersionUtility.stringToVersionRange(vRange);

            int ix3 = value.lastIndexOf(';');
            // Check index value for backwards compatibility - bundle start level was not always cached
            if (ix3 != -1) {
                location = value.substring(0, ix3);
                this.startLevel = Integer.valueOf(value.substring(ix3 + 1));
            } else {
                location = value;
                this.startLevel = 0;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getSymbolicName() {
            return symbolicName;
        }

        /** {@inheritDoc} */
        @Override
        public VersionRange getVersionRange() {
            return range;
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, String> getAttributes() {
            return Collections.emptyMap();
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, String> getDirectives() {
            return Collections.emptyMap();
        }

        /** {@inheritDoc} */
        @Override
        public String getLocation() {
            return location;
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getOsList() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public SubsystemContentType getType() {
            return SubsystemContentType.BUNDLE_TYPE;
        }

        @Override
        public String getRawType() {
            return SubsystemContentType.BUNDLE_TYPE.getValue();
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        /** {@inheritDoc} */
        @Override
        public int getStartLevel() {
            return this.startLevel;
        }

        /** {@inheritDoc} */
        @Override
        public String getMatchString() {
            return stringRepresentation;
        }

        /** {@inheritDoc} */
        @Override
        public String getBundleRepositoryType() {
            return bundleRepositoryType;
        }

        @Override
        public boolean isType(SubsystemContentType type) {
            return type == SubsystemContentType.BUNDLE_TYPE;
        }

        @Override
        public String getExtendedAttributes() {
            return null;
        }

        @Override
        public String setExecutablePermission() {
            return Boolean.FALSE.toString();
        }

        @Override
        public String getFileEncoding() {
            return null;
        }

        @Override
        public List<String> getTolerates() {
            return Collections.emptyList();
        }

        @Override
        public Integer getRequireJava() {
            return null;
        }

    }

    public BundleList(WsResource bundleCacheFile, FeatureManager featureManager) {
        cacheFile = bundleCacheFile;
        this.featureManager = featureManager;
    }

    public BundleList(FeatureManager featureManager) {
        cacheFile = null;
        this.featureManager = featureManager;
    }

    public void init() {
        resources.clear();
        try {
            load(cacheFile, featureManager);
        } catch (IOException e) {
            Tr.warning(tc, "UPDATE_BUNDLE_CACHE_WARNING", new Object[] { cacheFile.toExternalURI(), e.getMessage() });
        }
    }

    public void dispose() {
        if (stale.get())
            store();
        resources.clear();
    }

    public void addAllNoReplace(BundleList newBundleList) {
        for (RuntimeFeatureResource r : newBundleList.resources) {
            if (!!!resources.contains(r))
                resources.add(r);
        }
        stale.set(true);
    }

    /**
     * This is like retain all except it returns a list of what was removed.
     *
     * TODO remove the bundles.
     *
     * @param newBundleList
     * @return
     */
    public BundleList findExtraBundles(BundleList newBundleList, FeatureManager featureManager) {
        List<RuntimeFeatureResource> bundles = new ArrayList<RuntimeFeatureResource>(resources);
        bundles.removeAll(newBundleList.resources);
        resources.removeAll(bundles);
        BundleList result = new BundleList(featureManager);
        result.resources.addAll(bundles);
        stale.set(true);
        return result;
    }

    public boolean isEmpty() {
        return resources.isEmpty();
    }

    private void load(WsResource res, FeatureManager featureManager) throws IOException {
        if (res == null || !res.exists())
            return;

        InputStream in = res.get();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(CACHE_WRITE_TIME)) {
                    readWriteTimeAndJavaSpecVersion(res, line);
                } else {
                    int index = line.indexOf('=');
                    if (index != -1) {
                        resources.add(new RuntimeFeatureResource(line.substring(0, index), line.substring(index + 1)));
                    }
                }
            }
        } finally {
            FeatureDefinitionUtils.tryToClose(reader);
            FeatureDefinitionUtils.tryToClose(in);
        }
    }

    // ignore the NumberFormatException as we deal with it.
    @FFDCIgnore(NumberFormatException.class)
    private void readWriteTimeAndJavaSpecVersion(WsResource res, String line) {
        int timeIndex = line.indexOf('=');
        int javaSpecVersionIndex = timeIndex >= 0 ? line.indexOf(';', timeIndex) : -1;
        if (timeIndex != -1) {
            try {
                String sTime = javaSpecVersionIndex > timeIndex ? line.substring(timeIndex + 1, javaSpecVersionIndex) : line.substring(timeIndex + 1);
                writeTime = Long.parseLong(sTime);
                if (javaSpecVersionIndex != -1) {
                    javaSpecVersion = Integer.valueOf(line.substring(javaSpecVersionIndex + 1));
                }
            } catch (NumberFormatException nfe) {
            }
        }

        if (writeTime <= 0) {
            writeTime = res.getLastModified();
        }
    }

    public synchronized void store() {
        if (cacheFile != null) {
            OutputStream out = null;
            try {
                out = cacheFile.putStream();
                PrintWriter writer = new PrintWriter(out);
                writer.write(CACHE_WRITE_TIME);
                writer.write('=');
                writeTime = System.currentTimeMillis();
                writer.write(String.valueOf(writeTime));
                writer.write(';');
                writer.write(Integer.toString(JavaInfo.majorVersion()));
                writer.write(FeatureDefinitionUtils.NL);
                for (RuntimeFeatureResource entry : resources) {
                    if (entry.getURLString() != null) {
                        // This must match what the CachedFeatureResource() ctor method reads
                        // repoName|symbolicName/version=location;startLevel
                        // matchString: symbolicName/version
                        String bundleRepositoryType = entry.getBundleRepositoryType();
                        if (bundleRepositoryType.length() > 0) {
                            writer.write(bundleRepositoryType);
                            writer.write('|');
                        }
                        writer.write(entry.getMatchString());
                        writer.write('=');
                        writer.write(entry.getURLString());
                        writer.write(';');
                        writer.write(Integer.toString(entry.getStartLevel()));
                        writer.write(FeatureDefinitionUtils.NL);
                    }
                }
                writer.flush();
                writer.close();
                out.close();
                stale.set(false); // mark cache as current after it has been saved
            } catch (IOException e) {
                Tr.warning(tc, "UPDATE_BUNDLE_CACHE_WARNING", new Object[] { cacheFile.toExternalURI(), e.getMessage() });
            } finally {
                FeatureDefinitionUtils.tryToClose(out);
            }
        }
    }

    public void addAll(ProvisioningFeatureDefinition fdefinition, FeatureManager featureManager) {
        for (FeatureResource fr : fdefinition.getConstituents(SubsystemContentType.BUNDLE_TYPE)) {
            RuntimeFeatureResource rfr = (RuntimeFeatureResource) ((fr instanceof RuntimeFeatureResource) ? fr : new RuntimeFeatureResource(fr));
            // only add bundles that match the current osgi.ee capability
            if (!featureManager.missingRequiredJava(rfr)) {
                resources.add(rfr);
            }
        }
        stale.set(true);
    }

    public void foreach(FeatureResourceHandler featureResourceHandler) {
        for (FeatureResource fr : resources) {
            if (!!!featureResourceHandler.handle(fr)) {
                break; // exit early.
            }
        }
    }

    public void createAssociation(FeatureResource fr, Bundle bundle, WsResource resource, int level) {
        if (fr instanceof RuntimeFeatureResource) {
            RuntimeFeatureResource rfr = (RuntimeFeatureResource) fr;
            rfr.setBundle(bundle);
            rfr.setResource(resource);
            rfr.setStartLevel(level);
        }
    }

    public Bundle getBundle(BundleContext ctx, FeatureResource fr) throws MalformedURLException {
        Bundle b = null;
        if (fr instanceof RuntimeFeatureResource) {
            RuntimeFeatureResource rfr = (RuntimeFeatureResource) fr;
            b = rfr.getBundle();

            if (b == null) {
                long id = rfr.getBundleId();
                b = ctx.getBundle(id);
                if (b == null) {
                    String location = rfr.getResolvedLocation(featureManager);
                    b = location == null ? null : ctx.getBundle(location);
                }
            }
        } else {
            for (RuntimeFeatureResource res : resources) {
                if (res.getSymbolicName().equals(fr.getSymbolicName())) {
                    Bundle checkBundle = res.getBundle();
                    if (fr.getVersionRange().includes(checkBundle.getVersion())) {
                        b = checkBundle;
                        break;
                    }
                }
            }
        }

        return b;
    }

    public Integer getJavaSpecVersion() {
        return javaSpecVersion;
    }
}