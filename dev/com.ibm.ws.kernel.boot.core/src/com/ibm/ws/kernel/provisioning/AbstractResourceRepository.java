/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.kernel.boot.cmdline.Utils;

public abstract class AbstractResourceRepository {

    /**
     * The root directory is the directory from which all relative locations
     * are addressed.
     */
    protected abstract File getRootDirectory();

    /**
     * The default base location provides a relative location to be used by
     * the repository to search for resources, in absence of explicit
     * specification by the user.
     */
    protected abstract String getDefaultBaseLocation();

    /**
     * This method must return all resources known to the repository with
     * the provided symbolic name (Note: The ordering, but not the contents,
     * of the list may be modified by the selectResource() method).
     */
    protected abstract List<Resource> getResourcesBySymbolicName(String symbolicName);

    /**
     * This method is called by selectResource() to instruct the
     * repository to include the provided baseLocation for resources.
     */
    protected abstract void includeBaseLocation(String baseLocation);

    /**
     * These methods are used to allow a directory to be marked as included,
     * and to test this condition.
     */
    protected abstract boolean isBaseLocationIncluded(String baseLocation);

    /**
     * This method is called if selectResource() ignores a IFix (in the
     * case where the IFix doesn't have a corresponding base version).
     */
    protected abstract void warnThatAnIFixWasIgnored(String fileName, String symbolicName, int majorVersion, int minorVersion, int microVersion);

    protected abstract class Resource implements Comparable<Resource> {

        protected abstract File getFile();

        protected abstract String getBaseLocation();

        protected abstract Version getVersion();

        protected abstract boolean isFix();

        protected abstract String getSymbolicName();

        /**
         * Resources are ordered by their version, where are higher version
         * is lower in the ordering.
         */
        @Override
        public int compareTo(final Resource other) {
            Version otherVersion = other.getVersion();
            Version thisVersion = this.getVersion();
            return otherVersion.compareTo(thisVersion);
        }

        /**
         * Due to restrictions imposed by Java's Collections API, two objects
         * must be equal if they are 0 under comparison.
         */
        @Override
        public boolean equals(final Object other) {
            if (other == null) {
                return false;
            } else if (other == this) {
                return true;
            } else if (other instanceof Resource && other.getClass() == this.getClass()) {
                return this.compareTo((Resource) other) == 0;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return getVersion().hashCode();
        }

    }

    public File selectResource(final String baseLocation, final String symbolicName, final VersionRange versionRange) {
        return selectResource(baseLocation, symbolicName, versionRange, true, false);
    }

    /**
     * Select the resource with the most appropriate version, based
     * on the standard service rules.
     */
    protected File selectResource(String baseLocation, final String symbolicName, final VersionRange versionRange, final boolean performURICheck, final boolean selectBaseBundle) {
        // If the baseLocation is null or the empty string use the default location.
        if (baseLocation == null || "".equals(baseLocation)) {
            baseLocation = getDefaultBaseLocation();
        }

        Set<String> baseLocationParts;

        // If we have an exact location ...
        if (!!!baseLocation.contains(",")) {
            File candidateFile;
            if (performURICheck) {
                // ... try to see if we have a URL (actually not sure this is used ever). If it exists return it.
                try {
                    candidateFile = new File(URI.create(baseLocation));
                    if (candidateFile.exists() && candidateFile.isFile() && candidateFile.isAbsolute()) {
                        return validateR4Bundle(candidateFile);
                    }
                } catch (IllegalArgumentException iae) {
                    // Just ignore, not a URI, and move on.
                }
            }
            // If it wasn't a URL then see if we can find it. If we do return it. If we don't we assume
            // the location is a directory and fall through to that processing.
            candidateFile = new File(getRootDirectory(), baseLocation);
            if (candidateFile.exists() && candidateFile.isFile()) {
                return validateR4Bundle(candidateFile);
            }
            // Prime baseLocationParts with this single baseLocation.
            baseLocationParts = new HashSet<String>();
            baseLocationParts.add(baseLocation);
        } else {
            // If we found a comma we assume we have a comma separated list of directories so split them up.
            baseLocationParts = new HashSet<String>(Arrays.asList(baseLocation.split(",")));
        }

        // Loop around all the locations (well DUH!).
        for (String baseLocationPart : baseLocationParts) {
            // Need to trim whitespace in case someone padded the locations in the source.
            baseLocationPart = baseLocationPart.trim();
            // If we haven't seen this location before we need to include the directory looking at the resources in the directory.
            if (!!!isBaseLocationIncluded(baseLocationPart)) {
                // Make sure we don't include again and then include the directory loading all missed resources into this object.
                includeBaseLocation(baseLocationPart);
            }
        }

        // Now we have loaded everything into memory we grab the candidates with the right symbolic name. If we
        // get nothing back then we will ultimately return a null file.
        List<Resource> candidateResources = getResourcesBySymbolicName(symbolicName);
        if (candidateResources != null) {
            // Sort the candidates (using a sorted set might be more efficient, although 
            // we aren't likely to often select the same resource multiple times).
            // As a result of calling the resource in our example are sorted in this following order:
            // (First entry) a.b/1.0.2.v2, a.b/1.0.1.v1, a.b/1.0.1 (Last entry)
            Collections.sort(candidateResources);

            Resource bestMatch = null;
            // Loop around all the candidates.
            for (Resource candidateResource : candidateResources) {
                // Make sure the candidate is in the baseLocations provided in the selection rules. If it isn't we don't
                // want it. We also don't want it if it doesn't match the version range passed in. If either of these
                // conditions are met we just skip.
                if (!!!baseLocationParts.contains(candidateResource.getBaseLocation()) || !!!versionRange.includes(candidateResource.getVersion())) {
                    continue;
                }

                // If the best match is null this is the first possible match.
                if (bestMatch == null) {
                    // If we've got a standard resource here and isn't an IFix then we need to just take the 
                    // top entry, which will give us the highest versioned resource.
                    if (!!!candidateResource.isFix()) {
                        return candidateResource.getFile(); //resource is a basebundle, no action needed for selectBaseBundle
                    } else {
                        // Save the best match, but we need to keep going to make sure the base resource can be found.
                        bestMatch = candidateResource;
                        // Kernel.boot jar when ifixed has no base bundle as the jar keeps the same name due to tooling
                        // having the jar by name on manifest classpaths.  Therefore for this jar there will only be a
                        // single match so return as soon as we have it.
                        if (("com.ibm.ws.kernel.boot".equals(symbolicName)) || ("com.ibm.ws.kernel.boot.archive".equals(symbolicName))) {
                            return bestMatch.getFile();
                        }
                    }
                } else {
                    // Fetching here because in the error case we will need to re-fetch.
                    int bestMatchMajor = bestMatch.getVersion().getMajor();
                    int bestMatchMinor = bestMatch.getVersion().getMinor();
                    int bestMatchMicro = bestMatch.getVersion().getMicro();
                    // If the major, minor and micro exist then this is a resource on the same base level (could be another
                    // but older IFix, or the base resource, but it has to be one of them).
                    if (bestMatchMajor == candidateResource.getVersion().getMajor() &&
                        bestMatchMinor == candidateResource.getVersion().getMinor() &&
                        bestMatchMicro == candidateResource.getVersion().getMicro()) {
                        // If we hit something that is not an IFix then we have a valid best IFix so we return it.
                        if (!!!candidateResource.isFix()) {
                            //match was for baseBundle for an ifix, return appropriately.
                            return selectBaseBundle ? candidateResource.getFile() : bestMatch.getFile();
                        }
                    } else {
                        // If we got here then the previous best match did not have a base resource installed. So we
                        // need to output a warning and move on. This message makes an assumption about the way the
                        // resources are named that might not hold, but given no base resource exists we have to make an assumption.
                        warnThatAnIFixWasIgnored(bestMatch.getFile().getName(), bestMatch.getSymbolicName(), bestMatchMajor, bestMatchMinor, bestMatchMicro);
                        // If we can't find an existing match resource for the IFix, then we still need to try and load
                        // a non-IFix version of the resource, if one exists.
                        if (!!!candidateResource.isFix()) {
                            return candidateResource.getFile(); //not ifix, no action needed for selectBaseBundle.
                        } else {
                            bestMatch = candidateResource;
                        }
                    }
                }
            }
            // If we get here and we have a best match that is not an ifix, we need to return the file for it.
            // This should not happen, as we only store iFixes into bestMatch.. 
            if (bestMatch != null && !!!bestMatch.isFix()) {
                //not an ifix, so no need to handle selectBaseBundle
                return bestMatch.getFile();
            }
        }

        // If we get here we didn't find a match so just return null.
        return null;
    }

    /**
     * @param candidateFile
     * @return
     */
    private File validateR4Bundle(File candidateFile) {
        JarFile jar = null;
        try {
            jar = new JarFile(candidateFile);
            Manifest man = jar.getManifest();
            if (man != null) {
                if (man.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) != null) {
                    return candidateFile;
                }
            }
        } catch (IOException e) {
            // if this happens then the jar is probably invalid at which point we can't run it anyway.
            // errors will be reported later when we fail to install the bundle.
        } finally {
            Utils.tryToClose(jar);
        }
        return null;
    }
}
