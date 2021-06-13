/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.permissions;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

final class PermissionsConfigDDParser extends DDParser {

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

        super(ddRootContainer, ddEntry);
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
            if ( JavaEEVersion.VERSION_7_STR.equals(ddVersionAttr) ) {
                ddVersion = JavaEEVersion.VERSION_7_0_INT;
                expectedNamespace = NAMESPACE_JCP_JAVAEE;
            } else if ( JavaEEVersion.VERSION_9_STR.equals(ddVersionAttr) ) {
                ddVersion = JavaEEVersion.VERSION_9_0_INT;
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
                namespace = expectedNamespace;
            }

        } else if ( namespace != null ) {
            // Next, try to use the namespace.
            if ( namespace.equals(NAMESPACE_JCP_JAVAEE) ) {
                ddVersion = 70;
            } else if ( namespace.equals(NAMESPACE_JAKARTA) ) {
                ddVersion = 90;
            } else {
                throw new ParseException( unsupportedDescriptorNamespace(namespace) );
            }

        } else {
            // Fail if there is no version attribute and no namespace.
            throw new ParseException( missingDescriptorVersion() );
        }

        // Always assign the version, even if later parsing will override it.
        version = ddVersion;

        return createRootElement();
    }

    @Override
    protected VersionData[] getVersionData() {
        return null; // Unused 
    }

    @Override
    protected void validateRootElementName() throws ParseException {
        if ( !"permissions".equals(rootElementLocalName) ) {
            throw new ParseException(invalidRootElement());
        }
    }

    @Override
    protected PermissionsConfigType createRootElement() {
        return new PermissionsConfigType( getDeploymentDescriptorPath() );
    }
}
