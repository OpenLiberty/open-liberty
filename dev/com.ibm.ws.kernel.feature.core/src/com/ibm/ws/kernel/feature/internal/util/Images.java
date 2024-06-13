/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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

package com.ibm.ws.kernel.feature.internal.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Images {

    public Images() {
        this.images = new HashMap<>();
        this.cohorts = new HashMap<>();
    }

    public Images(int capacity) {
        this.images = new HashMap<>(capacity);
        this.cohorts = new HashMap<>();
    }

    //

    protected void put(ImageInfo image) {
        String name = image.getName();
        String cohortName = image.getBaseName();
        String version = image.getVersion();

        images.put(name, image);

        Map<String, ImageInfo> cohort = cohorts.get(cohortName);
        if ( cohort == null ) {
            cohorts.put(cohortName, cohort = new HashMap<>());
        }
        cohort.put(version, image);
    }

    //

    private final Map<String, ImageInfo> images;

    public Map<String, ImageInfo> getImages() {
        return images;
    }

    public ImageInfo getImage(String name) {
        return images.get(name);
    }

    //

    public Map<String, Map<String, ImageInfo>> cohorts;

    public Map<String, Map<String, ImageInfo>> getCohorts() {
        return cohorts;
    }

    public Set<String> getCohortNames() {
        return cohorts.keySet();
    }

    public Map<String, ImageInfo> getCohort(String baseName) {
        return cohorts.get(baseName);
    }

    public ImageInfo getImage(String baseName, String version) {
        Map<String, ImageInfo> cohort = getCohort(baseName);
        return ((cohort == null) ? null : cohort.get(version));
    }
}
