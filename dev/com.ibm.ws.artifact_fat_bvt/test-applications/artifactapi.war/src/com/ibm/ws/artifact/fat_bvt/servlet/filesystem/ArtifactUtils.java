/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.servlet.filesystem;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.EnclosedEntity;

/**
 * check each of.
 * name, [done]
 * path, [done]
 * size [done]
 * 
 * 
 * test not null [done]
 * test .. navigation
 * .. alone [done]
 * ../sibling [done]
 * .. out of the root [done]
 * 
 * case sensitivity [done]
 * 
 * getEntry for dirs with/without trailing / [done]
 * 
 * path validation
 * - expected location [done]
 * - always / prefixed [done]
 * - never / suffixed [done]
 * 
 * name validation
 * - expected name & case [done]
 * 
 * getURI [done]
 * - check exact uri count response [done], disabled.
 * cache usage means there may now be uri's where there were none before.
 * 
 * getResource
 * - validate not null when expected [done]
 * - validate IS null when expected [done], disabled.
 * cache usage means resource may be non-null, for previously null locations.
 * - validate url string ends with expected physical path location [done], disabled.
 * cache usage means path will not match precached path
 * 
 * getPhysicalPath
 * - not null when appropriate [done]
 * - IS null when appropriate [done], disabled.
 * cache usage means things previously null, can now have a phys path
 * - has expected content [done], disabled.
 * cache usage means path will not match precached path.
 */
public class ArtifactUtils {

    //should we attempt to verify the content of resource, uri, physical path?
    private static final boolean TEST_RESOURCE_URL_CONTENT = false;
    private static final boolean TEST_URI_CONTENT = false;
    private static final boolean TEST_PHYSICAL_PATH_CONTENT = false;

    //should we verify that resource, physical path are not null when expected to be not null?
    private static final boolean TEST_RESOURCE_URL_NOT_NULL = true;
    private static final boolean TEST_PHYSICAL_PATH_NOT_NULL = true;

    //should we attempt to enforce the uri count exactly.. 
    private static final boolean TEST_URI_COUNT_EXACT = false;
    //or is it ok, as long as there are always 'some' uris, when there should be 'some'.
    private static final boolean TEST_URI_COUNT_GREATER_THAN_ZERO = true;

    //should we attempt to verify that there is no path, resource, or uri's, when none are expected?
    private static final boolean TEST_PHYSICAL_PATH_IS_NULL = false;
    private static final boolean TEST_RESOURCE_URL_IS_NULL = false;
    private static final boolean TEST_URI_COUNT_IS_ZERO = false;

    //allow disabling of the Container!=Entry test.
    public static boolean TEST_INTERFACE_IMPLS_ARE_DIFFERENT = true;

    public static boolean compare(ArtifactContainer c, FileSystem f, PrintWriter out) {
        boolean result = true;

        result &= traversalCompare(c, f, out);

        return result;
    }

    private static boolean traversalCompare(ArtifactContainer c, FileSystem f, PrintWriter out) {
        boolean result = true;

        //out.println("Traversal Compare begins at " + f.getDebugPath() + " with Container " + c.getPath());

        result &= compareEntityInfo(true, c, f, out);

        //out.println("Traversal Compare begins children compare.. for node " + f.getDebugPath());

        FileSystem children[] = f.getChildren();
        if (children != null) {
            result &= compareChildren(c, f, children, out);
        }

        return result;
    }

    public static int counter = 0;

    private static void countCheck() {
        counter++;
    }

    private static boolean check(
        boolean success,
        EnclosedEntity e, FileSystem f,
        PrintWriter out, String failureReason) {

        counter++;

        if ( !success ) {
            out.println("FAIL: " + failureReason);
        }

        return success;
    }

    private static boolean pathCheck(String path, FileSystem f, PrintWriter out) {
        boolean result = true;
        if (path == null) {
            result = false;
            out.println("FAIL: path was null for entity " + f.getDebugPath());
        } else {

            //paths must start with /
            if (!path.startsWith("/")) {
                result = false;
                out.println("FAIL: path " + path + " did not start with a leading '/' character.");
            }
            //don't allow a trailing /, unless path is exactly /
            if (!"/".equals(path) && path.endsWith("/")) {
                result = false;
                out.println("FAIL: path " + path + " ended with a trailing '/' character.");
            }
        }
        return result;
    }

    private static boolean compareEntityInfo(boolean eIsContainer, EnclosedEntity e, FileSystem f, PrintWriter out) {
        return compareEntityInfo(eIsContainer, e, f, out, true);
    }

    private static boolean compareEntityInfo(boolean eIsContainer, EnclosedEntity e, FileSystem f, PrintWriter out, boolean processDownward) {
        boolean result = true;

        if (f == null || e == null) {
            result = false;
            out.println("FAIL: ERROR: compare invoked with a null argument! " + e + " " + f);
        } else {

            //enforce root!=null
            result &= check(e.getRoot() != null, e, f, out,
                            "Root null for " + f.getDebugPath() + " , null root disallowed by API");

            //check if root is enclosed
            FileSystem localRoot = f;
            if (!eIsContainer && f.isRoot()) {
                //the current f.isroot refers to e being a container, e is not a container yet
                //so we must locate the root above the current f.isroot.
                localRoot = f.getParent();
            }
            //could be we were at the top root, in which case theres no enclosure to test.
            if (localRoot != null) {
                while (!localRoot.isRoot()) {
                    localRoot = localRoot.getParent();
                }
                boolean eRootIsEnclosed = e.getRoot().getEnclosingContainer() != null;
                boolean fRootIsEnclosed = localRoot.getParent() != null;
                result &= check(eRootIsEnclosed == fRootIsEnclosed, e, f, out,
                                fRootIsEnclosed ? "Root for " + f.getDebugPath() + " was not enclosed by another container as expected. Actual enclosed " + eRootIsEnclosed
                                                  + " expected "
                                                  + fRootIsEnclosed
                                                : "Root for " + f.getDebugPath() + " was enclosed by another container and was not expected to be. Actual enclosed "
                                                  + eRootIsEnclosed
                                                  + " expected " + fRootIsEnclosed);
            }

            //check if can obtain self via root. (do not attempt for /)
            if (!e.getPath().equals("/")) {
                result &= check(e.getRoot().getEntry(e.getPath()) != null, e, f, out,
                                "Unable to obtain self via root for " + f.getDebugPath() + " , null disallowed by semantics.");
            }

            if (TEST_INTERFACE_IMPLS_ARE_DIFFERENT) {
                //ensure we don't gain any dual purpose implementations.. 
                boolean implementsBoth = (e instanceof ArtifactEntry) && (e instanceof ArtifactContainer);
                result &= check(!implementsBoth, e, f, out,
                                "Implementation " + e.getClass() + " implements both Container and Entry at the same time.. this is not allowed.");
            }

            //type specific checks.. 
            if (!eIsContainer) {
                ArtifactEntry entry = (ArtifactEntry) e;

                //compare name and path
                result &= checkNameAndPath(eIsContainer, e, f, out);

                //check physical path
                result &= checkPhysicalPath(entry, f, out);

                //check resource
                result &= checkResource(entry, f, out);

                //check size
                result &= check(entry.getSize() == f.getSize(), e, f, out,
                                "Size mismatch for " + f.getDebugPath() + " expected " + f.getSize() + " got " + entry.getSize());

                //check if we are supposed to be able to convert.. 
                ArtifactContainer test = entry.convertToContainer();
                ArtifactContainer localtest = entry.convertToContainer(true);
                ArtifactContainer inverselocaltest = entry.convertToContainer(false);
                boolean testnull = test == null;
                boolean iltestnull = inverselocaltest == null;

                result &= check(iltestnull == testnull, e, f, out,
                                "Convert to Container returned non-matching results for convertToContainer() vs convertToContainer(false) for " + f.getDebugPath());

                //only check downward if requested, to avoid infinite up/down loops with getEntryInEnclosingContainer.
                if (f.isContainer() && processDownward) {
                    result &= check(test != null, e, f, out,
                                    "Entry expected to convert to container, and cannot for " + f.getDebugPath());
                    if (f.isRoot()) {
                        //if f is a new root, the local convert should have failed.
                        result &= check(localtest == null, e, f, out,
                                        "Entry expected to be a new Root, and yet converted via localonly " + f.getDebugPath());
                    } else {
                        //if f is not a new root, the local convert should have succeeded.
                        result &= check(localtest != null, e, f, out,
                                        "Entry expected not to be a new Root, and yet did not convert via localonly " + f.getDebugPath());
                    }
                    //retest the container..
                    //this will verify the name/path, etc for the converted output.
                    //do this for every return non-null, in case one type of call builds broken containers.
                    if (test != null) {
                        result &= compareEntityInfo(true, test, f, out);
                    }
                    if (localtest != null) {
                        result &= compareEntityInfo(true, localtest, f, out);
                    }
                    if (inverselocaltest != null) {
                        result &= compareEntityInfo(true, inverselocaltest, f, out);
                    }
                }

                if (!f.isContainer() && !f.isRoot()) {
                    //if not supposed to be a container.. check if we were.
                    result &= check(test == null && localtest == null && inverselocaltest == null, e, f, out,
                                    "Entry not expected to convert to container, and yet did, for " + f.getDebugPath());
                }

            } else if (eIsContainer) {
                ArtifactContainer container = (ArtifactContainer) e;
                //are we supposed to be a container??
                result &= check(f.isContainer(), e, f, out,
                                "Unexpected Container for " + f.getDebugPath());

                //are we supposed to be root.
                result &= check(container.isRoot() == f.isRoot(), e, f, out,
                                "IsRoot mismatch for " + f.getDebugPath() + " container:" + container.getPath() + " expected " + f.isRoot() + " got " + container.isRoot());
                if (container.isRoot() != f.isRoot()) {
                    out.println("this container came from.. ");
                    Exception ex = new Exception();
                    ex.printStackTrace(out);
                }

                //are we supposed to have an owning entry ?
                if (f.getParent() != null) {
                    //out.println("f.getParent was non-null, for container at path " + f.getDebugPath());

                    result &= check(container.getEntryInEnclosingContainer() != null, e, f, out,
                                     "Container got null for it's owning entry, expected not null for " + container.getPath() + " " + f.getDebugPath());

                    if (container.getEntryInEnclosingContainer() != null) {
                        result &= compareEntityInfo(false, container.getEntryInEnclosingContainer(), f, out, false);
                    }
                } else {
                    result &= check(container.getEntryInEnclosingContainer() == null, e, f, out,
                                    "Container got non-null for it's owning entry, expected null for " + container.getPath() + " " + f.getDebugPath());
                }

                //compare name
                result &= check(f.getName().equals(container.getName()), container, f, out,
                                "Name mismatch, for container at " + f.getDebugPath() + " expected '" + f.getName() + "' got '" + container.getName() + "'");
                //compare path
                result &= check(f.getPath().equals(container.getPath()), container, f, out,
                                "Path mismatch, for container at " + f.getDebugPath() + " expected '" + f.getDebugPath() + "' got '" + container.getPath() + "'");

                //enforce path rules.
                result &= pathCheck(container.getPath(), f, out);

                //can we find ourselves via ourself?
                if (!container.getPath().equals("/")) {
                    result &= check(container.getEntry(container.getPath()) != null, e, f, out,
                                    "Unable to locate self via self for " + f.getDebugPath());
                    result &= check(container.getEntry(container.getPath() + "/") != null, e, f, out,
                                    "Unable to locate self with trailing / via self for " + f.getDebugPath());

                }

                //.. path test
                result &= checkParentPath(container, f, out);

                //quick children test.. the full test will be driven by traversalCompare after entity compare
                int entryCount = 0;
                for (ArtifactEntry each : container) {
                    if (each != null)//pointless usage of 'each' to placate the editor ;p
                        entryCount++;
                }
                int wantedCount = f.getChildren() == null ? 0 : f.getChildren().length;
                result &= check(wantedCount == entryCount, container, f, out,
                                "Container at " + f.getDebugPath() + " has incorrect child count of " + entryCount + " expected " + wantedCount);

                //test getUri on Container.
                result &= checkURLs(container, f, out);
            }
            //upward navigation check.. 
            result &= upwardNavigationCheck(eIsContainer, e, f, out);
        }
        return result;
    }

    private static boolean checkParentPath(ArtifactContainer c, FileSystem f, PrintWriter out) {
        boolean result = true;

        FileSystem parent = f.getParent();
        //see if can obtain parent entry via ..
        if (!f.isRoot() && parent != null && !parent.isRoot()) {
            result &= check(c.getEntry("..") != null, c, parent, out,
                            "Unable to obtain parent Entry for Container " + f.getDebugPath() + " using path of \"..\"");

            //see if can obtain sibling via ..
            if (parent.getChildren() != null && parent.getChildren().length > 1) {
                FileSystem chosen = null;
                for (FileSystem child : parent.getChildren()) {
                    if (!child.getName().equals(f.getName())) {
                        chosen = child;
                        break;
                    }
                }
                if (chosen != null) {
                    result &= check(c.getEntry("../" + chosen.getName()) != null, c, chosen, out,
                                    "Unable to obtain sibling '" + chosen.getName() + "' using path '../" + chosen.getName() + "' for Fs node " + chosen.getDebugPath());
                }
            }

            FileSystem superparent = parent.getParent();
            if (superparent != null && !superparent.isRoot()) {
                result &= check(c.getEntry("../..") != null, c, parent, out,
                                "Unable to obtain parent of parent Entry for Container " + f.getDebugPath() + " using path of \"../..\"");
            }
        }

        if (!f.isRoot() && parent != null && parent.isRoot()) {
            //this container is not root, but it's parent is, so we can't obtain an entry to represent it.. 
            ArtifactEntry e = null;
            try {
                e = c.getEntry("..");
            } catch (IllegalArgumentException a) {
                e = null;
            }

            result &= check(e == null, c, f, out,
                            "Able to obtain entry representing an fs root using .. path, this should not happen. Node " + f.getDebugPath());
        }

        if (f.isRoot()) {
            //if we are root, we cannot obtain anything via a .. path
            //regardless of if there is suppoosed to be a parent.. 
            ArtifactEntry e = null;
            try {
                e = c.getEntry("..");
            } catch (IllegalArgumentException a) {
                e = null;
            }
            result &= check(e == null, c, f, out,
                            "Able to obtain parent entry for an fs root using .. path, this should not happen. Node " + f.getDebugPath());
        }

        return result;
    }

    @SuppressWarnings("null")
    private static boolean compareChildren(ArtifactContainer c, FileSystem parent, FileSystem[] children, PrintWriter out) {
        boolean result = true;

        Set<String> namesWanted = new HashSet<String>();
        Set<String> namesSeen = new HashSet<String>();

        for (FileSystem f : children) {
            if (f.isRoot()) {
                namesWanted.add(f.getNameAsEntry());
            } else {
                namesWanted.add(f.getName());
            }
        }

        //out.println("Child compare is hunting for names of " + namesWanted + " obtained from node " + parent.getDebugPath());

        for (ArtifactEntry e : c) {
            //trap if there is a really broken impl out there.
            if (e == null) {
                result &= check( (e != null), c, parent, out,
                                "Null encountered while iterating children at path " + parent.getPath() + " expected children " + namesWanted);
            } else {

                //out.println("Evaluating Entry " + e.getPath());

                String entryName = e.getName();
                namesSeen.add(entryName);

                result &= check(namesWanted.contains(entryName), c, parent, out,
                                "Unexpected entry found in Container at " + c.getPath() + " with a path of " + e.getPath() + " and name '" + e.getName()
                                                + "' expected children were '" + namesWanted + "'");

                if (namesWanted.contains(entryName)) {

                    FileSystem matching = parent.getChildByName(entryName);

                    if (matching == null) {
                        result = false;
                        out.println("FAIL:ERROR: unable to obtain wanted entry name " + entryName + " from fs it was supposedly part of " + parent.getPath());
                    } else {

                        //out.println("Matched Entry " + e.getPath() + " to fs decl " + matching.getDebugPath());

                        result &= compareEntityInfo(false, e, matching, out);

                        //check if we can get the child using the wrong case for filename.
                        String badCase = entryName.toUpperCase();
                        //try to find a badCase that doesn't exist.. 
                        if (parent.getChildByName(badCase) != null) {
                            badCase = entryName.toLowerCase();
                            if (parent.getChildByName(badCase) != null) {
                                badCase = null;
                            }
                        }
                        if (badCase != null) {
                            result &= check(c.getEntry(badCase) == null, c, parent, out,
                                            "Able to obtain entry " + entryName + " using case " + badCase + " from path " + parent.getPath());
                        }

                        //recurse if this child is a container.. 
                        if (matching.isContainer()) {

                            //out.println("According to FS, this node is a container.. initiating descent comparison.");

                            ArtifactContainer converted = e.convertToContainer();
                            if (converted != null) {
                                //out.println("Node DID convert, calling traversal compare");
                                // if the container is null, it would have been flagged in compareEntityInfo.
                                result &= traversalCompare(converted, matching, out);
                            }
                        }
                    }
                }
            }
        }

        result &= check(namesWanted.containsAll(namesSeen), c, parent, out,
                        "Unexpected children found at path " + parent.getPath() + " expected '" + namesWanted + "' got '" + namesSeen + "'");

        result &= check(namesSeen.containsAll(namesWanted), c, parent, out,
                        "Missing children expected at path " + parent.getPath() + " wanted '" + namesWanted + "' got '" + namesSeen + "'");

        return result;
    }

    private static boolean checkNameAndPath(boolean eIsContainer, EnclosedEntity e, FileSystem f, PrintWriter out) {
        boolean result = true;

        String name = f.getName();
        String path = f.getPath();
        String nameAsEntry = f.getNameAsEntry();
        String pathAsEntry = f.getPathAsEntry();

        if (!eIsContainer) {
            String entryNameToTest = name;
            String entryPathToTest = path;
            if (f.isRoot()) {
                entryNameToTest = nameAsEntry;
                entryPathToTest = pathAsEntry;
            }

            //compare name
            result &= check(e.getName().equals(entryNameToTest), e, f, out,
                            "Name mismatch, for entry at " + f.getDebugPath() + " expected '" + entryNameToTest + "' got '" + e.getName() + "'");

            //compare path
            result &= check(e.getPath().equals(entryPathToTest), e, f, out,
                            "Path mismatch, for entry at " + f.getDebugPath() + " expected '" + entryPathToTest + "' got '" + e.getPath() + "'");

            //enforce path rules on path.
            result &= pathCheck(e.getPath(), f, out);
        } else {
            //e is Container.
            String entryNameToTest = name;
            String entryPathToTest = path;

            //compare name
            result &= check(e.getName().equals(entryNameToTest), e, f, out,
                            "Name mismatch, for container at " + f.getDebugPath() + " expected '" + entryNameToTest + "' got '" + e.getName() + "'");

            //compare path
            result &= check(e.getPath().equals(entryPathToTest), e, f, out,
                            "Path mismatch, for container at " + f.getDebugPath() + " expected '" + entryPathToTest + "' got '" + e.getPath() + "'");

            //enforce path rules on path.
            result &= pathCheck(e.getPath(), f, out);
        }

        return result;
    }

    private static boolean checkResource(ArtifactEntry e, FileSystem f, PrintWriter out) {
        boolean result = true;

        String resource = f.getResource();
        //added the .equals("null") to tolerate idiotic test data.
        if (resource != null && !resource.equals("null")) {
            URL u = e.getResource();
            if (TEST_RESOURCE_URL_NOT_NULL) {
                result &= check(u != null, e, f, out,
                                "Resource was null for path " + f.getDebugPath() + " when expected value of " + resource);
            }

            if (TEST_RESOURCE_URL_CONTENT) {
                if (u != null) {
                    String resparts[] = resource.split("#");
                    result &= check(u.toString().startsWith(resparts[0]) && u.toString().endsWith(resparts[1]), e, f, out,
                                    "Unable to validate resource " + u.toString() + " for path " + f.getDebugPath() + " expected value was " + resource);
                }
            }
        } else {
            if (TEST_RESOURCE_URL_IS_NULL) {
                result &= check(e.getResource() == null, e, f, out,
                                    "Resource was not null, when expected to be for path " + f.getDebugPath() + " got value of " + e.getResource());
            }
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    private static boolean checkPhysicalPath(ArtifactEntry e, FileSystem f, PrintWriter out) {
        boolean result = true;

        String physpath = f.getPhysicalPath();
        if (physpath != null) {
            if (physpath.indexOf("#") != -1) {
                physpath = physpath.split("#")[1];
            }
            String p = e.getPhysicalPath();

            if (TEST_PHYSICAL_PATH_NOT_NULL) {
                result &= check(p != null, e, f, out,
                                "Physical Path was null for path " + f.getDebugPath() + " when expected value of " + physpath);
            }
            if (p != null) {
                if (TEST_PHYSICAL_PATH_CONTENT) {
                    result &= check(p.endsWith(physpath), e, f, out,
                                    "Unable to validate physpath " + p + " for path " + f.getDebugPath() + " expected value was " + physpath);
                }
            }
        } else {
            if (TEST_PHYSICAL_PATH_IS_NULL) {
                result &= check(e.getPhysicalPath() == null, e, f, out,
                                "Physical path was not null, when expected to be for path " + f.getDebugPath() + " got value " + e.getPhysicalPath());
            }
        }

        return result;
    }

    private static boolean checkURLs(ArtifactContainer container, FileSystem f, PrintWriter out) {
        boolean result = true;
        Collection<URL> u = container.getURLs();
        if (f.getUrlCount() == -1) {
            if (TEST_URI_COUNT_IS_ZERO) {
                result &= check(u == null, container, f, out,
                                "Unexpected URL Collection obtained from container getURLs at path " + f.getDebugPath() + " expected null");
            }

        } else {
            if (TEST_URI_COUNT_IS_ZERO) {
                result &= check(u != null, container, f, out,
                                "Unexpected null URL Collection obtained from container getURLs at path " + f.getDebugPath() + " expected " + f.getUrlCount() + " entries");
            }
            if (u != null) {
                if (TEST_URI_COUNT_EXACT) {
                    result &= check(u.size() == f.getUrlCount(), container, f, out,
                                    "Incorrect uri count obtained from container getURI at path " + f.getDebugPath() + " expected " + f.getUrlCount() + " entries, got " + u.size());
                } else if (TEST_URI_COUNT_GREATER_THAN_ZERO) {
                    if (f.getUrlCount() > 0) {
                        result &= check(u.size() > 0, container, f, out,
                                        "Incorrect uri count obtained from container getURI at path " + f.getDebugPath() + " expected " + f.getUrlCount() + " entries, got none");
                    }

                }
                if (TEST_URI_CONTENT) {
                    //now we validate each of them.. 
                    for (String expectedURL : f.getURLs()) {
                        boolean found = false;
                        for (URL test : u) {
                            String expectedPaths[] = expectedURL.split("#");
                            if (test.toString().endsWith(expectedPaths[1]) && test.toString().startsWith(expectedPaths[0])) {
                                found = true;
                            }
                        }
                        result &= check(found, container, f, out,
                                        "Unable to match url returned from " + f.getDebugPath() + " url searched for was " + expectedURL + " getURLs gave " + u);
                    }
                }
            }
        }
        return result;
    }

    // Make sure for every entity, that the parent chain has entries with 
    // the expected name and path.

    private static boolean upwardNavigationCheck(
        boolean isContainer,
        EnclosedEntity actual, FileSystem expected,
        PrintWriter out) {

        boolean chainMatches = true;

        boolean nextIsContainer = isContainer;
        EnclosedEntity nextActual = actual;
        FileSystem nextExpected = expected;

        // Stop when any entities have different names or paths.
        // Stop when either the actual or the expected chain completes. 

        while ( chainMatches && ((nextActual != null) && (nextExpected != null)) ) {
            countCheck();

            if ( chainMatches = checkNameAndPath(nextIsContainer, nextActual, nextExpected, out) ) {
                nextExpected = nextExpected.getParent();
                nextActual = nextActual.getEnclosingContainer();
                nextIsContainer = true;
            }
        }

        if ( chainMatches ) {
            // Make sure neither of the chains ended before the other.

            String message;
            if ( nextActual != null ) {
                chainMatches = false;
                message = "FAIL: Upwards navigation from " + expected.getDebugPath() + ": " +
                          " Unexpected parent " + nextActual.getName() + " at " + nextActual.getPath();
            } else if ( nextExpected != null ) {
                chainMatches = false;
                message = "FAIL: Upwards navigation from " + expected.getDebugPath() + ": " +
                          " Unepxected null parent at " + nextExpected.getPath();
            } else {
                message = null;
            }

            if ( message != null ) {
                out.println(message);
            }
        }

        return chainMatches;
    }
}
