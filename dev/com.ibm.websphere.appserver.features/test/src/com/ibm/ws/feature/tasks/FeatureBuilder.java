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

package com.ibm.ws.feature.tasks;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;

public class FeatureBuilder extends Builder {


    public Parameters getSubsystemContent() {
        Parameters p = getParameters("Subsystem-Content");
        return p;
    }

    public void setSubsystemContent(Parameters content) throws IOException {
        setProperty("Subsystem-Content", printClauses(content.asMapMap()));
    }

    public Set<Map.Entry<String, Attrs>> getFiles() {
        return getContent("-files");
    }

    public Set<Map.Entry<String, Attrs>> getJars() {
        return getContent("-jars");
    }

    public Set<Map.Entry<String, Attrs>> getFeatures() {
        return getContent("-features");
    }

    public Set<Map.Entry<String, Attrs>> getBundles() {
        return getContent("-bundles");
    }

    private Set<Map.Entry<String, Attrs>> getContent(String contentType) {
        String jars = getProperty(contentType, "");
        if (jars == null || jars.length() == 0) {
            return Collections.emptySet();
        }
        Parameters p = new Parameters(jars);
        return p.entrySet();
    }

    public Set<String> getAutoFeatures() {
    	Set<Entry<String, Attrs>> rawString = getContent(FeatureBnd.IBM_PROVISION_CAPABILITY);
    	Set<String> processedAutoFeatures = new HashSet<String>();
    	String OSGI_PREFIX = "osgi.identity=";
    	Iterator<Entry<String, Attrs>> itr = rawString.iterator();
    	String filterString = rawString.toString();
		String[] messyAutoFeatures = filterString.split(OSGI_PREFIX);
		for (String messyAutoFeature : messyAutoFeatures) {
			if (!messyAutoFeature.startsWith("com") && !messyAutoFeature.startsWith("io.openliberty"))
				continue;

			processedAutoFeatures.add(trimAutofeatureString(messyAutoFeature));
		}

    	return processedAutoFeatures;

    }

    private String trimAutofeatureString(String autoFeature) {
    	if (autoFeature.indexOf(")") > 0)
    		return autoFeature.substring(0, autoFeature.indexOf(")"));
    	else
    		return autoFeature;

    }

    public Set<Map.Entry<String, Attrs>> getIBMProvisionCapability() {
        return getContent(FeatureBnd.IBM_PROVISION_CAPABILITY);
    }

    public void setAppliesTo(String name, Attrs attributes) throws IOException {
        Parameters p = new Parameters();
        p.put(name, attributes);

        setProperty("IBM-AppliesTo", printClauses(p.asMapMap()));
    }

    public void setSubsystemSymbolicName(Parameters p) throws IOException {
        setProperty(FeatureBnd.SUBSYSTEM_SYMBOLIC_NAME, printClauses(p.asMapMap()));
    }

}
