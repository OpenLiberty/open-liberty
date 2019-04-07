/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.VersionRange;

import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;

/**
 * Represents a feature requirement in a way that the kernel resolver understands
 * <p>
 * The kernel resolver only uses the symbolic name and tolerates information so all the other methods are unimplemented.
 */
public class KernelResolverRequirement implements FeatureResource {

    private final String symbolicName;
    private final List<String> tolerates;

    public KernelResolverRequirement(String symbolicName, Collection<String> tolerates) {
        this.symbolicName = symbolicName;
        this.tolerates = Collections.unmodifiableList(new ArrayList<>(tolerates));
    }

    @Override
    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public List<String> getTolerates() {
        return tolerates;
    }

    @Override
    public Map<String, String> getAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getDirectives() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBundleRepositoryType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getExtendedAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFileEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMatchString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getOsList() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRawType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStartLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SubsystemContentType getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionRange getVersionRange() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isType(SubsystemContentType arg0) {
        return false;
    }

    @Override
    public String setExecutablePermission() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getRequireJava() {
        throw new UnsupportedOperationException();
    }

}
