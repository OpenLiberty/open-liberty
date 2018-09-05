/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.VersionRange;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.feature.internal.ProvisionerConstants;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.VersionUtility;

public class FeatureResourceImpl implements FeatureResource {
    private static final TraceComponent tc = Tr.register(FeatureResourceImpl.class);

    private final String _symbolicName;
    private final Map<String, String> _rawAttributes;
    private final String _featureName;
    private final String _bundleRepositoryType;

    private String matchString = null;

    private final AtomicReference<VersionRange> _range = new AtomicReference<VersionRange>();
    private final AtomicReference<List<String>> _osList = new AtomicReference<List<String>>();
    private final AtomicReference<Map<String, String>> _attributes = new AtomicReference<Map<String, String>>();
    private final AtomicReference<Map<String, String>> _directives = new AtomicReference<Map<String, String>>();
    private final AtomicInteger _startLevel = new AtomicInteger(-1);
    private final AtomicReference<List<String>> _tolerates = new AtomicReference<List<String>>();

    private volatile SubsystemContentType _type = null;

    public FeatureResourceImpl(String key, Map<String, String> value, String bundleRepositoryType, String featureName) {
        _symbolicName = key;
        _rawAttributes = value;
        _bundleRepositoryType = bundleRepositoryType;
        _featureName = featureName;
    }

    /** {@inheritDoc} */
    @Override
    public String getSymbolicName() {
        return _symbolicName;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    // marked trivial since it is called by the toString method and we don't want to recurse through trace by mistake.
    public VersionRange getVersionRange() {
        VersionRange result = _range.get();
        if (result == null) {
            String range = _rawAttributes.get("version");
            result = VersionUtility.stringToVersionRange(range);

            if (!!!_range.compareAndSet(null, result)) {
                result = _range.get();
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> result = _attributes.get();

        if (result == null) {
            result = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : _rawAttributes.entrySet()) {
                String key = entry.getKey();
                if (!!!key.endsWith(":")) {
                    result.put(key, entry.getValue());
                }
            }

            result = Collections.unmodifiableMap(result);

            if (!!!_attributes.compareAndSet(null, result)) {
                result = _attributes.get();
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getDirectives() {
        Map<String, String> result = _directives.get();

        if (result == null) {
            result = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : _rawAttributes.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith(":")) {
                    result.put(key.substring(0, key.length() - 1), entry.getValue());
                }
            }

            result = Collections.unmodifiableMap(result);

            if (!!!_directives.compareAndSet(null, result)) {
                result = _directives.get();
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return _rawAttributes.get("location:");
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getOsList() {
        List<String> result = _osList.get();
        if (result == null) {
            String osData = _rawAttributes.get("os");
            if (osData == null) {
                osData = "";
            }
            String[] parts = osData.split(",");
            result = new ArrayList<String>();
            for (String p : parts) {
                if (!"".equals(p.trim())) {
                    result.add(p.trim());
                }
            }

            if (!!!_osList.compareAndSet(null, result)) {
                result = _osList.get();
            }
        }

        if (result.isEmpty()) {
            result = null;
        }
        return result;
    }

    @Override
    public boolean isType(SubsystemContentType type) {
        SubsystemContentType thisType = getType();
        return thisType == type;
    }

    /** {@inheritDoc} */
    @Override
    public SubsystemContentType getType() {
        SubsystemContentType result = _type;
        if (result == null) {
            result = _type = SubsystemContentType.fromString(getRawType());
        }
        return result;
    }

    @Override
    public String getRawType() {
        return _rawAttributes.get("type");
    }

    @Override
    public String toString() {
        // Best effort at not calculating this all the time.
        if (matchString == null) {
            matchString = _symbolicName + '/' + getVersionRange();
        }
        return matchString;
    }

    /** {@inheritDoc} */
    @Override
    public int getStartLevel() {
        int result = _startLevel.get();

        if (result == -1) {
            result = ProvisionerConstants.LEVEL_FEATURE_CONTAINERS;
            // Directive names are in the attributes map, but end with a colon
            String phase = _rawAttributes.get("start-phase:");

            result = setStartLevel(phase, result);

            if (!!!_startLevel.compareAndSet(-1, result)) {
                result = _startLevel.get();
            }
        }

        return result;
    }

    private int setStartLevel(String phase, int original) {

        if (phase == null) {
            return original;
        }

        if (ProvisionerConstants.PHASE_APPLICATION.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_APPLICATION;
        } else if (ProvisionerConstants.PHASE_APPLICATION_LATE.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_APPLICATION + ProvisionerConstants.PHASE_INCREMENT;
        } else if (ProvisionerConstants.PHASE_APPLICATION_EARLY.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_APPLICATION - ProvisionerConstants.PHASE_INCREMENT;
        } else if (ProvisionerConstants.PHASE_SERVICE.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_SERVICES;
        } else if (ProvisionerConstants.PHASE_SERVICE_LATE.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_SERVICES + ProvisionerConstants.PHASE_INCREMENT;
        } else if (ProvisionerConstants.PHASE_SERVICE_EARLY.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_SERVICES - ProvisionerConstants.PHASE_INCREMENT;
        } else if (ProvisionerConstants.PHASE_CONTAINER.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_CONTAINERS;
        } else if (ProvisionerConstants.PHASE_CONTAINER_LATE.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_CONTAINERS + ProvisionerConstants.PHASE_INCREMENT;
        } else if (ProvisionerConstants.PHASE_CONTAINER_EARLY.equals(phase)) {
            return ProvisionerConstants.LEVEL_FEATURE_CONTAINERS - ProvisionerConstants.PHASE_INCREMENT;
        } else {
            Tr.warning(tc, "INVALID_START_PHASE_WARNING", new Object[] { phase, this._symbolicName, this._featureName });
            return original;
        }

    }

    @Override
    public String getExtendedAttributes() {
        return getDirectives().get("ibm.zos.extended.attributes");
    }

    @Override
    public String setExecutablePermission() {
        return getDirectives().get("ibm.executable");
    }

    @Override
    public String getFileEncoding() {
        return getDirectives().get("ibm.file.encoding");
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getLocation() == null) ? 0 : getLocation().hashCode());
        result = prime * result + ((getVersionRange() == null) ? 0 : getVersionRange().hashCode());
        result = prime * result + ((_symbolicName == null) ? 0 : _symbolicName.hashCode());
        result = prime * result + ((_type == null) ? 0 : _type.hashCode());
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
        FeatureResourceImpl other = (FeatureResourceImpl) obj;
        return Objects.equals(_symbolicName, other._symbolicName)
               && _type == other._type
               && Objects.equals(getVersionRange(), other.getVersionRange())
               && Objects.equals(getLocation(), other.getLocation())
               && Objects.equals(getOsList(), other.getOsList())
               && Objects.equals(getTolerates(), other.getTolerates())
               && Objects.equals(getRequireJava(), other.getRequireJava());
    }

    /** {@inheritDoc} */
    @Override
    public String getMatchString() {
        return toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getBundleRepositoryType() {
        return _bundleRepositoryType;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getTolerates() {
        List<String> result = _tolerates.get();
        if (result == null) {
            String tolerates = _rawAttributes.get("ibm.tolerates:");
            if (tolerates == null) {
                tolerates = "";
            }
            String[] parts = tolerates.split(",");
            result = new ArrayList<String>();
            for (String p : parts) {
                if (!"".equals(p.trim())) {
                    result.add(p.trim());
                }
            }

            if (!!!_tolerates.compareAndSet(null, result)) {
                result = _tolerates.get();
            }
        }

        if (result.isEmpty()) {
            result = null;
        }
        return result;
    }

    @Override
    public Integer getRequireJava() {
        // Directive names are in the attributes map, but end with a colon
        String requireJava = _rawAttributes.get("require-java:");
        return requireJava == null ? null : Integer.valueOf(requireJava);
    }
}