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
package com.ibm.aries.buildtasks.utils;

import java.util.HashMap;
import java.util.Map;

import com.ibm.aries.buildtasks.classpath.Version;
import com.ibm.aries.buildtasks.classpath.VersionMatch;

/**
 * A package declaration from an Import-Package or Export-Package.
 */
public final class PackageDeclaration
{
    /** The package to import */
    private String packageName;
    /** The attributes for the package */
    private final Map<String, String> attributes = new HashMap<String, String>();
    /** Indicates if it is a package import, rather than an export */
    private boolean isImport;

    @Override
    public boolean equals(Object other)
    {
        if (other == this)
            return true;
        if (other == null)
            return false;

        if (other instanceof PackageDeclaration) {
            PackageDeclaration otherPackage = (PackageDeclaration) other;

            if (packageName.equals(otherPackage.packageName) && isImport != otherPackage.isImport) {
                String version = attributes.get("version");

                VersionMatch match = null;
                Version exportedVersion = null;

                if (isImport && version != null) {
                    match = new VersionMatch(version);
                } else if (version != null) {
                    exportedVersion = new Version(version);
                }

                version = otherPackage.attributes.get("version");

                if (match == null && otherPackage.isImport && version != null) {
                    match = new VersionMatch(version);
                } else if (version != null && exportedVersion == null) {
                    exportedVersion = new Version(version);
                }

                if (match == null)
                    return true;

                if (exportedVersion == null)
                    exportedVersion = new Version(0, 0, 0);

                return match.matches(exportedVersion);
            }
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return packageName.hashCode();
    }

    public String getPackageName()
    {
        return packageName;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public boolean isImport()
    {
        return isImport;
    }

    public void setImport(boolean b)
    {
        isImport = b;
    }

    public void setPackageName(String string)
    {
        packageName = string;
    }

    @Override
    public String toString() {
        return packageName;
    }
}