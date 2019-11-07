/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FrameworkInfo {
    VersionedEntityMap<PkgInfo, PackageContent> pkgInfo;
    List<FeatureInfo> featureInfos;
    Map<VersionedEntity, Set<PkgInfo>> bundleToPkgs;

    public VersionedEntityMap<PkgInfo, PackageContent> getPkgInfo() {
        return pkgInfo;
    }

    public void setPkgInfo(VersionedEntityMap<PkgInfo, PackageContent> pkgInfo) {
        this.pkgInfo = pkgInfo;
    }

    public List<FeatureInfo> getFeatureInfos() {
        return featureInfos;
    }

    public void setFeatureInfos(List<FeatureInfo> featureInfos) {
        this.featureInfos = featureInfos;
    }

    public Map<VersionedEntity, Set<PkgInfo>> getBundleToPkgs() {
        return bundleToPkgs;
    }

    public void setBundleToPkgs(Map<VersionedEntity, Set<PkgInfo>> bundleToPkgs) {
        this.bundleToPkgs = bundleToPkgs;
    }
}