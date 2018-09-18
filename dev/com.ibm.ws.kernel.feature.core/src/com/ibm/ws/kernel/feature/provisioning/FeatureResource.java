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
package com.ibm.ws.kernel.feature.provisioning;

import java.util.List;

import org.osgi.framework.VersionRange;

public interface FeatureResource extends HeaderElementDefinition {

    public VersionRange getVersionRange();

    public String getLocation();

    public SubsystemContentType getType();

    /**
     * @return the raw type attribute, not as an enum value
     */
    public String getRawType();

    /**
     * obtain a list of operating systems this resource is relevant to.
     *
     * @return null, if for ALL os, or a list of platform names this resource is for.
     */
    public List<String> getOsList();

    public int getStartLevel();

    public String getMatchString();

    public String getBundleRepositoryType();

    public boolean isType(SubsystemContentType type);

    public String getExtendedAttributes();

    public String setExecutablePermission();

    public String getFileEncoding();

    public List<String> getTolerates();

    public Integer getRequireJava();
}