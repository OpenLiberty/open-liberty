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
package com.ibm.ws.featureverifier.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;

import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;

public class AutoFeatureUtils {

    public static Set<String> getFiltersForAutoFeatureHeader(
                                                             FeatureInfo autofeature) {
        String autoFeatureHeaderContent = autofeature.getAutoFeatureHeaderContent();
        if (autoFeatureHeaderContent == null) {
            throw new IllegalStateException("Can only process autofeature info for live features, not ones loaded from framework xml");
        }
        Set<String> filters = new HashSet<String>();
        List<NameValuePair> info = ManifestHeaderProcessor.parseExportString(autoFeatureHeaderContent);
        for (NameValuePair nvp : info) {
            if (nvp.getName().equals("osgi.identity")) {
                Map<String, String> attribs = nvp.getAttributes();
                if (attribs != null) {
                    for (Map.Entry<String, String> attrib : attribs.entrySet()) {
                        if (attrib.getKey().equals("filter:")) {
                            filters.add(attrib.getValue());
                        }
                    }
                }
            }
        }
        return filters;
    }

}
