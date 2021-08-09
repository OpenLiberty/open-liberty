/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.util.Collections;
import java.util.List;

import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Implementation of {@link ResourceInfo} for resource-env-ref elements.
 *
 * Only the name and type attributes are provided; all others are defaulted.
 * Note that 'auth' and 'sharingScope' are opposite of the @Resource defaults,
 * but are more correct for resource-env-ref types.
 */
final class ResourceEnvRefInfo implements ResourceInfo {

    private final String name;
    private final String type;

    ResourceEnvRefInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getAuth() {
        return AUTH_APPLICATION;
    }

    @Override
    public int getSharingScope() {
        return SHARING_SCOPE_UNSHAREABLE;
    }

    @Override
    public String getLoginConfigurationName() {
        return null;
    }

    @Override
    public List<? extends Property> getLoginPropertyList() {
        return Collections.emptyList();
    }

    @Override
    public int getIsolationLevel() {
        return java.sql.Connection.TRANSACTION_NONE; // TRANSACTION_NONE
    }

    @Override
    public int getCommitPriority() {
        return 0; // unspecified
    }

    @Override
    public int getBranchCoupling() {
        return BRANCH_COUPLING_UNSET;
    }
}
