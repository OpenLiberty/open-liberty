/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.permissions;

import com.ibm.ws.javaee.dd.permissions.PermissionsConfig;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserSpec;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class PermissionsConfigDDParser extends DDParser {

    // Unused; provided as documentation.

    public static final VersionData[] VERSION_DATA = {
        new VersionData("7", null, NAMESPACE_JCP_JAVAEE, PermissionsConfig.VERSION_7_0, VERSION_7_0_INT),                            
        new VersionData("9", null, NAMESPACE_JAKARTA, PermissionsConfig.VERSION_9_0, VERSION_9_0_INT),
        new VersionData("10", null, NAMESPACE_JAKARTA, PermissionsConfig.VERSION_10_0, VERSION_10_0_INT)
    };

    public static int getMaxTolerated() {
        return PermissionsConfig.VERSION_10_0;
    }

    public static int getMaxImplemented() {
        return PermissionsConfig.VERSION_9_0;
    }    
    
    /**
     * Create a permissions configuration parser.
     * 
     * Note that a maximum schema version is not accepted.
     * permissions parsing currently accepts up-level schema
     * versions.
     *
     * @param ddRootContainer The container containing the
     *     descriptor entry.
     * @param ddEntry The entry containing the descriptor.
     *
     * @throws ParseException Thrown if parsing fails.
     */
    public PermissionsConfigDDParser(Container ddRootContainer, Entry ddEntry)
        throws ParseException {

        super(ddRootContainer, ddEntry, "permissions");
    }

    @Override
    public PermissionsConfigType parse() throws ParseException {
        super.parseRootElement();

        return (PermissionsConfigType) rootParsable;
    }

    /**
     * Override: The rules for parsing permissions descriptors are
     * simpler than the usual rules.  Permissions currently don't
     * care about provisioning.
     */
    @Override
    protected PermissionsConfigType createRootParsable() throws ParseException {
        validateRootElementName();

        int ddVersion;

        String ddVersionAttr = getAttributeValue("", "version");
        if ( ddVersionAttr != null ) {
            // First, try to use the version.  Don't try to use
            // the namespace if the version is available.

            String expectedNamespace;
            if ( PermissionsConfig.VERSION_7_STR.equals(ddVersionAttr) ) {
                ddVersion = PermissionsConfig.VERSION_7_0;
                expectedNamespace = NAMESPACE_JCP_JAVAEE;
            } else if ( PermissionsConfig.VERSION_9_STR.equals(ddVersionAttr) ) {
                ddVersion = PermissionsConfig.VERSION_9_0;
                expectedNamespace = NAMESPACE_JAKARTA;
            } else if ( PermissionsConfig.VERSION_10_STR.equals(ddVersionAttr) ) {
                ddVersion = PermissionsConfig.VERSION_10_0;
                expectedNamespace = NAMESPACE_JAKARTA;                
            } else {
                throw new ParseException( unsupportedDescriptorVersion(ddVersionAttr) );         
            }

            // Fill in or correct the namespace, if necessary.
            boolean assignNamespace;
            if ( namespace == null ) {
                assignNamespace = true;
            } else if ( !namespace.contentEquals(expectedNamespace) ) {
                assignNamespace = true;
                warning( incorrectDescriptorNamespace(ddVersionAttr, namespace, expectedNamespace) );
            } else {
                assignNamespace = false;
            }
            if ( assignNamespace ) {
                patchNamespace(expectedNamespace);
            }

        } else if ( namespace != null ) {
            // Next, try to use the namespace.
            // No maximum version is set.  We could use that to set the
            // descriptor version to 8 or 10.  However, permissions parsing
            // allows reading of higher versioned elements, and ignores
            // 'maxVersion'.  Contrast with 'ApplicationDDParser', and others.
            if ( namespace.equals(DDParserSpec.NAMESPACE_JCP_JAVAEE) ) {
                ddVersion = PermissionsConfig.VERSION_7_0;
            } else if ( namespace.equals(DDParserSpec.NAMESPACE_JAKARTA) ) {
                ddVersion = PermissionsConfig.VERSION_9_0;
            } else {
                throw new ParseException( unsupportedDescriptorNamespace(namespace) );
            }

        } else {
            // Fail if there is no version attribute and no namespace.
            throw new ParseException( missingDescriptorVersion() );
        }

        // Always assign the version, even if later parsing will override it.
        version = ddVersion;

        return new PermissionsConfigType( getDeploymentDescriptorPath() );
    }
}
