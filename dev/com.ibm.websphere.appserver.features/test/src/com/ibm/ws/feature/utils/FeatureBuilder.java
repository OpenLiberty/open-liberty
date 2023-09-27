/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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

package com.ibm.ws.feature.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;

/**
 * aQute builder extension. Provides APIs for direct access to various feature
 * content.
 */
public class FeatureBuilder extends Builder {

    /**
     * Base property setter.
     *
     * Convert the parameters into the raw property value as a standard map
     * based OSGi header value. See {@link Parameters#asMapMap()} and
     * {@link aQute.bnd.osgi.Processor#printClauses(Map)}.
     *
     * @param name The property name.
     * @param parameters The property parameters.
     *
     * @throws IOException Thrown if printing the header value fails.
     */
    public void setProperty(String name, Parameters parameters) throws IOException {
        setProperty(name, Processor.printClauses(parameters.asMapMap()));
    }

    /**
     * Convert a name and attributes into parameters.
     *
     * Create new parameters and assign a single parameter using
     * the supplied name and attributes.
     *
     * @param name A single parameter name.
     * @param attributes Attribute values used as the single parameter value.
     *
     * @return Parameters with a single assigned parameter.
     */
    public static Parameters asParameters(String name, Attrs attributes) {
        Parameters parameters = new Parameters();
        parameters.put(name, attributes);
        return parameters;
    }

    //

    public void setSubsystemSymbolicName(Parameters parameters) throws IOException {
        setProperty(FeatureConstants.SUBSYSTEM_SYMBOLIC_NAME, parameters);
    }

    public void setSubsystemContent(Parameters parameters) throws IOException {
        setProperty(FeatureConstants.SUBSYSTEM_CONTENT, parameters);
    }

    public void setAppliesTo(String name, Attrs attributes) throws IOException {
        setProperty(FeatureConstants.IBM_APPLIES_TO, asParameters(name, attributes));
    }

    //

    public Parameters getSubsystemContent() {
        return getParameters(FeatureConstants.SUBSYSTEM_CONTENT);
    }

    //

    /**
     * Retrieve content of a specified type as a set of entries.
     *
     * Expected content types are {@link FeatureConstants#CONTENT_FILES},
     * {@link FeatureConstants#CONTENT_JARS}, {@link FeatureConstants#CONTENT_FEATURES},
     * {@link FeatureConstants#CONTENT_BUNDLES}, and
     * {@link FeatureConstants#IBM_PROVISION_CAPABILITY}.
     *
     * @param contentType The type of content which is requested.
     *
     * @return The requested content type, as a set of entries.
     */
    private Set<Map.Entry<String, Attrs>> getContent(String contentType) {
        String content = getProperty(contentType, "");
        if ((content == null) || content.isEmpty()) {
            return Collections.emptySet();
        }
        Parameters p = new Parameters(content);
        return p.entrySet();
    }

    public Set<Map.Entry<String, Attrs>> getFiles() {
        return getContent(FeatureConstants.CONTENT_FILES);
    }

    public Set<Map.Entry<String, Attrs>> getJars() {
        return getContent(FeatureConstants.CONTENT_JARS);
    }

    public Set<Map.Entry<String, Attrs>> getFeatures() {
        return getContent(FeatureConstants.CONTENT_FEATURES);
    }

    public Set<Map.Entry<String, Attrs>> getBundles() {
        return getContent(FeatureConstants.CONTENT_BUNDLES);
    }

    public Set<Map.Entry<String, Attrs>> getIBMProvisionCapability() {
        return getContent(FeatureConstants.IBM_PROVISION_CAPABILITY);
    }

    //

    /**
     * Answer auto feature names. These are the {@link FeatureConstants#OSGI_IDENTITY_PREFIX}
     * values from {@link FeatureConstants#IBM_PROVISION_CAPABILITY} content types.
     *
     * Ignore any names which begin with one of {@link FeatureConstants#IGNORED_AUTOFEATURE_PREFIXES}:
     * These should never be set as auto-features.
     *
     * @return Auto-feature names.
     */
    public Set<String> getAutoFeatures() {
        Set<Entry<String, Attrs>> capabilities = getIBMProvisionCapability();
        String capabilitiesText = capabilities.toString();

        Set<String> autoFeatures = new HashSet<>();

        String[] osgiIdentities = capabilitiesText.split(FeatureConstants.OSGI_IDENTITY_PREFIX);
        for (String osgiIdentity : osgiIdentities) {
            boolean ignore = false;
            for (String ignoredPrefix : FeatureConstants.IGNORED_AUTOFEATURE_PREFIXES) {
                if (osgiIdentity.startsWith(ignoredPrefix)) {
                    ignore = true;
                    break;
                }
            }
            if (ignore) {
                continue;
            }

            autoFeatures.add(trimAutofeature(osgiIdentity));
        }

        return autoFeatures;
    }

    private String trimAutofeature(String autoFeature) {
        int closeOffset = autoFeature.indexOf(')');
        return ((closeOffset < 0) ? autoFeature : autoFeature.substring(0, closeOffset));
    }

}
