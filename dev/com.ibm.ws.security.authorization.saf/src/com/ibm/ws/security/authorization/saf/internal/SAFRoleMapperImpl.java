/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.saf.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.security.authorization.saf.SAFRoleMapper;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.wsspi.logging.IntrospectableService;

/**
 * Provides the default implementation of a SAFRoleMapper.
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = { "service.vendor=IBM" })
public class SAFRoleMapperImpl implements SAFRoleMapper, IntrospectableService {

    /**
     * The attribute used to specify the profilename pattern in the <safRoleMapper>
     * config element.
     */
    protected static final String PROFILE_PATTERN_KEY = "profilePattern";

    /**
     * variable to save the configured profilePattern
     */
    private String profilePattern;

    /**
     * boolean whether to convert the profilename to uppercase
     */
    private boolean toUpperCase;

    /**
     * Map to hold the roles and the saf profile name
     */
    private static HashMap<String, String> profilesFromRole = new HashMap<String, String>();

    /**
     * Reference to SAFCredentialsService, for retrieving the profilePrefix.
     */
    private SAFCredentialsService safCredentialsService = null;

    /**
     * Called when an instance of this class is activated by the OSGi framework.
     */
    @Activate
    protected void activate(ComponentContext cc) {
        updateConfig((Map<String, Object>) cc.getProperties());
    }

    /**
     * Invoked by OSGi when the <safRoleMapper> configuration has changed.
     */
    @Modified
    protected void modify(ComponentContext cc) {
        updateConfig((Map<String, Object>) cc.getProperties());
    }

    /**
     * This method is called whenever the SAF RoleMapper config is updated.
     */
    protected void updateConfig(Map<String, Object> props) {
        profilePattern = (String) props.get(PROFILE_PATTERN_KEY);
        toUpperCase = ((Boolean) props.get("toUpperCase")).booleanValue();

        // The profile pattern may have been updated, so empty the cache of
        // profiles (which used the old pattern).
        profilesFromRole.clear();
    }

    /**
     * Set the SAFCredentialsService ref.
     */
    @Reference
    protected void setSafCredentialsService(SAFCredentialsService safCredentialsService) {
        this.safCredentialsService = safCredentialsService;
    }

    /**
     * Unset the SAFCredentialsService ref.
     */
    protected void unsetSafCredentialsService(SAFCredentialsService safCredentialsService) {
        if (this.safCredentialsService == safCredentialsService) {
            this.safCredentialsService = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProfileFromRole(final String resourceName, final String role) {
        // Replace null parms with "".
        String localResourceName = (resourceName != null) ? resourceName : "";
        String localRole = (role != null) ? role : "";

        // Check if the safProfile is already cached. Include the profilePrefix
        // in the mappedRoleKey, in case the profilePrefix gets changed dynamically
        // via a config update.
        String mappedRoleKey = localResourceName + "." + localRole + "." + safCredentialsService.getProfilePrefix();
        String safProfile = profilesFromRole.get(mappedRoleKey);
        if (safProfile != null) {
            return safProfile;
        }

        // check if the profile Pattern is configured
        safProfile = profilePattern;
        safProfile = safProfile.replace("%resource%", localResourceName);
        safProfile = safProfile.replace("%role%", localRole);
        safProfile = safProfile.replace("%profilePrefix%", safCredentialsService.getProfilePrefix());

        // The EJBROLE class profile names can't have any of the
        // following characters in them: %&*<blank>
        // We'll have to eat them up.
        safProfile = safProfile.replaceAll("\\%", "#");
        safProfile = safProfile.replaceAll("\\&", "#");
        safProfile = safProfile.replaceAll("\\*", "#");
        safProfile = safProfile.replaceAll("\\s", "#");

        if (toUpperCase) {
            safProfile = safProfile.toUpperCase();
        }

        // Cache it so we don't have to re-gen the mapping for the same parms.
        profilesFromRole.put(mappedRoleKey, safProfile);

        return safProfile;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Provides the default implementation of a SAFRoleMapper";
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "SAFRoleMapperImpl";
    }

    /** {@inheritDoc} */
    @Override
    public void introspect(OutputStream out) throws IOException {
        PrintWriter pw = new PrintWriter(out);
        pw.println();
        if (profilesFromRole.entrySet().isEmpty())
            pw.println("The mapped profile cache is empty.");
        else {
            pw.println("Mapped profile cache contents:");
            for (Entry<String, String> entry : profilesFromRole.entrySet()) {
                pw.println("  " + entry.getKey() + " = " + entry.getValue());
            }
        }
        pw.println();
        pw.flush();
    }
}
