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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.osgi.framework.Bundle;

import com.ibm.aries.buildtasks.classpath.VersionMatch;

class FrameworkBundleRepository {
    HashMap<String, Collection<Bundle>> symbNamesToBundles = new HashMap<String, Collection<Bundle>>();

    public FrameworkBundleRepository(Bundle[] installedBundles) {
        for (Bundle b : installedBundles) {
            if (!symbNamesToBundles.containsKey(b.getSymbolicName())) {
                symbNamesToBundles.put(b.getSymbolicName(), new ArrayList<Bundle>());
            }
            symbNamesToBundles.get(b.getSymbolicName()).add(b);
        }
    }

    public Collection<Bundle> matchBundles(String symbName, String versionRange) {
        if (symbNamesToBundles.containsKey(symbName)) {
            VersionMatch locate = versionRange == null ? null : new VersionMatch(versionRange);
            Collection<Bundle> matched = new ArrayList<Bundle>();
            for (Bundle b : symbNamesToBundles.get(symbName)) {
                com.ibm.aries.buildtasks.classpath.Version v = new com.ibm.aries.buildtasks.classpath.Version(b.getVersion().toString());
                if (versionRange == null || locate.matches(v)) {
                    matched.add(b);
                }
            }
            return matched;
        } else {
            return Collections.emptyList();
        }
    }
}