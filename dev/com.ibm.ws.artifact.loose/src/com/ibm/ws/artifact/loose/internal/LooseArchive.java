/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.loose.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

public class LooseArchive implements ArtifactContainer {
    private final File cacheDir;
    private final File xmlFile;
    private final LooseArchive parent;
    private final LooseContainerFactoryHelper cfh;
    private String pathInParent;
    private final List<EntryInfo> _entries = new ArrayList<EntryInfo>();
    private static final TraceComponent tc = Tr.register(LooseArchive.class);
    private LooseArtifactNotifier artifactNotifier;

    public enum EntryType {
        DIR {
            /**
             * Puts an entry of type dir into the entries list
             */
            @Override
            public void put(LooseArchive la, String archiveDir, Object onDiskDir, LooseContainerFactoryHelper cfh) {
                la._entries.add(new DirEntryInfo(archiveDir, (String) onDiskDir, cfh));
            }

            /**
             * Put method which handles the excludes tag if one is present
             */
            @Override
            public void put(LooseArchive la, String archiveDir, Object onDiskDir, String excludes, LooseContainerFactoryHelper cfh) {
                la._entries.add(new DirEntryInfo(archiveDir, (String) onDiskDir, excludes, cfh));
            }
        },
        FILE {
            /**
             * Puts an entry of type file into the entries list
             */
            @Override
            public void put(LooseArchive la, String archiveFile, Object onDiskFile, LooseContainerFactoryHelper cfh) {
                la._entries.add(new FileEntryInfo(archiveFile, (String) onDiskFile, cfh));
            }

            /**
             * Is an override method which can accept the excludes attribute, but discards it.
             */
            @Override
            public void put(LooseArchive la, String archiveFile, Object onDiskFile, String ignoreValue, LooseContainerFactoryHelper cfh) {
                la._entries.add(new FileEntryInfo(archiveFile, (String) onDiskFile, cfh));
                Tr.warning(tc, "EXCLUDES_ON_FILE", onDiskFile);
            }

        },
        ARCHIVE {
            /**
             * Puts an entry of type archive into the entries list
             */
            @Override
            public void put(LooseArchive la, String archiveFile, Object looseArchive, LooseContainerFactoryHelper cfh) {
                la._entries.add(new ArchiveEntryInfo(archiveFile, (LooseArchive) looseArchive, cfh));
            }

            /**
             * Is an override method which can accept the excludes attribute, but discards it.
             */
            @Override
            public void put(LooseArchive la, String archiveFile, Object looseArchive, String ignoreValue, LooseContainerFactoryHelper cfh) {
                la._entries.add(new ArchiveEntryInfo(archiveFile, (LooseArchive) looseArchive, cfh));
                Tr.warning(tc, "EXCLUDES_ON_ARCHIVE", archiveFile);
            }
        };

        public abstract void put(LooseArchive la, String location, Object obj, LooseContainerFactoryHelper cfh);

        public abstract void put(LooseArchive la, String target, Object source, String excludes, LooseContainerFactoryHelper cfh);
    }

    public static abstract class EntryInfo {
        public abstract boolean matches(String fileName);

        public abstract boolean matches(String fileName, boolean checkIfExists);

        public abstract InputStream getInputStream(String filename);

        public abstract ArtifactContainer createContainer(boolean localOnly, LooseArchive parent, String path);

        public abstract String getVirtualLocation();

        public abstract boolean isBeneath(String fileName);

        public abstract long getSize(String fileName);

        public abstract long getLastModified(String fileName);

        public abstract void addMonitoringPaths(Set<String> files, Set<String> directories, String path);

        /**
         * Gets the URI for the given path within this entry. No check is done to ensure that the path matches this entry so this should only be called after a call to
         * {@link #matches(String)}.
         * 
         * @param path The path to get the URI for
         * @return A collection of URIs for the entry
         */
        abstract Collection<URL> getURLs(String path);

        /**
         * method to obtain the physical location for the resource, or null if there wasnt/isnt one.
         * 
         * @deprecated added only to support the getRealPath method on servlet context for the alpha
         */
        @Deprecated
        abstract String getFirstPhysicalPath(String virtualPath);
    }

    private static class DirEntryInfo extends EntryInfo {

        private final String dirPrefixInArchive;
        private final String dirOnDisk;
        private String formattedDirOnDisk = null;
        private Pattern excludesPattern = null;
        private final LooseContainerFactoryHelper containerFactoryHelper;

        public DirEntryInfo(String location, String onDiskDir, LooseContainerFactoryHelper cfh) {
            dirPrefixInArchive = stripTrailingSeparator(location);
            dirOnDisk = stripTrailingSeparator(onDiskDir);
            containerFactoryHelper = cfh;
        }

        public DirEntryInfo(String location, String onDiskDir, String excludesString, LooseContainerFactoryHelper cfh) {
            this(location, onDiskDir, cfh);
            convertToRegex(excludesString);
        }

        @Override
        public void addMonitoringPaths(Set<String> files, Set<String> dirs, String path) {
            boolean recurse = true;
            if (path.startsWith("!")) {
                recurse = false;
                path = path.substring(1);
            }

            //put the paths back into the set with their ! prefix for 'non recursive'
            String pathForAdd = recurse ? "" : "!";
            pathForAdd += dirOnDisk;
            if (path.length() > dirPrefixInArchive.length() && path.startsWith(dirPrefixInArchive)) {
                String frag = path.substring(dirPrefixInArchive.length());
                frag = frag.replace('/', File.separatorChar);
                //use file to assemble the path to avoid issues with trailing preceding slashes.
                pathForAdd = new File(dirOnDisk, frag).getAbsolutePath();
                pathForAdd = recurse ? pathForAdd : "!" + pathForAdd;
            }
            dirs.add(pathForAdd);
            files.add(pathForAdd);
        }

        @Override
        public String getVirtualLocation() {
            return dirPrefixInArchive;
        }

        @Override
        public boolean matches(String fileName) {
            return matches(fileName, true);
        }

        @Override
        public boolean matches(String fileName, boolean checkIfExists) {
            fileName = stripTrailingSeparator(fileName);

            String onDiskPath = null;

            // The cut file name is the part of the file after the prefix in archive has been removed
            String cutFileName = null;
            if (fileName.length() > dirPrefixInArchive.length() && fileName.startsWith(dirPrefixInArchive)) {
                cutFileName = fileName.substring(dirPrefixInArchive.length());
                if (isSeparatorChar(cutFileName.charAt(0))) {
                    onDiskPath = dirOnDisk + cutFileName;
                } else {
                    onDiskPath = dirOnDisk + '/' + cutFileName;
                }
            } else if (dirPrefixInArchive.equals(fileName)) {
                onDiskPath = dirOnDisk;
                cutFileName = "";
            }
            if (onDiskPath != null) {
                File f = new File(onDiskPath);
                //if the file exists and is not excluded (if there is an exclude string set), return true, else false.
                if (!checkIfExists || FileUtils.fileExists(f)) {
                    if (excludesPattern == null || fileNotExcluded(dirOnDisk + "/", f)) {
                        // One last test is to make sure the case is the same (on windows f.exists() will return true even if the case is different)
                        return PathUtils.checkCase(f, cutFileName);
                    }
                }
            }
            return false;
        }

        @Override
        public boolean isBeneath(String fileName) {
            String s = stripTrailingSeparator(fileName);
            if (s.length() < dirPrefixInArchive.length()) {
                if (s.length() > 1 && s.length() == fileName.length()) {
                    fileName = fileName + '/';
                }
                return dirPrefixInArchive.startsWith(fileName);
            }
            return false;
        }

        @Override
        public ArtifactContainer createContainer(boolean localOnly, LooseArchive parent, String path) {
            String onDiskPath = createOnDiskPath(path);

            File f = new File(onDiskPath);
            if (FileUtils.fileIsDirectory(f)) {
                return new LooseContainer(parent, this, path);
            }
            //Let other people have a crack at the conversion..
            ArtifactContainer rv = null;
            if (!localOnly) {
                LooseArchive owning = parent;
                ArtifactEntry e = owning.getEntry(path);
                ArtifactContainer c = e.getEnclosingContainer();

                // See if the container factory can create a container from us
                File newCacheDir = null;
                String relativeLocation = c.getPath();
                if (relativeLocation.equals("/")) {
                    newCacheDir = parent.getCacheDir();
                } else {
                    //use of substring 1 is ok here, because this entry MUST be within a container, and the smallest path
                    //as container can have is "/", which is dealt with above, therefore, in this branch the relativeLocation MUST 
                    //be longer than "/"
                    newCacheDir = new File(parent.getCacheDir(), relativeLocation.substring(1));
                }
                //newCacheDir = new File(newCacheDir, e.getName());

                rv = containerFactoryHelper.getContainerFactory().getContainer(newCacheDir, c, e, f);
            }

            return rv;
        }

        public List<String> getChildren(String path) {
            String onDiskPath = createOnDiskPath(path);

            File fdir = new File(onDiskPath);
            File[] flist = FileUtils.listFiles(fdir);
            if (flist == null) {
                return new ArrayList<String>(0);
            }
            List<String> retval = new ArrayList<String>(flist.length);
            if (excludesPattern != null) {
                if (formattedDirOnDisk == null) {
                    formattedDirOnDisk = dirOnDisk.replace('\\', '/') + '/';
                }
            }
            boolean pathIsRoot = path.length() == 1 && isSeparatorChar(path.charAt(0));
            //if path is root, we just need to add the "/", else add path a "/" and then the name.
            String prefix = pathIsRoot ? "/" : stripTrailingSeparator(path) + '/';
            for (File f : flist) {
                //if we have an exludes string set, only process file if it doesn't match excludes string
                if (excludesPattern == null || fileNotExcluded(formattedDirOnDisk, f)) {
                    retval.add(prefix + f.getName());
                }
            }
            return retval;
        }

        /**
         * Converts **, /* and "." to be regex expressions so we can convert *.java to be regex compatible as well as /** etc
         * (can't give all examples as ** with a slash will close the javadoc comments).
         * <br/><br/>
         * It sets the excludesPattern object in the looseArchive class to allow efficient regex matching
         * 
         * @param excludeString
         */
        public void convertToRegex(String excludeString) {
            String originalString = excludeString;
            // make all "." safe decimles then convert ** to .* and /* to /.* to make it regex
            if (excludeString.contains(".")) {
                // regex for "." is \. - but we are converting the string to a regex string so need to escape the escape slash...
                excludeString = excludeString.replace(".", "\\.");
            }
            //if we exclude a dir (eg /**/) we need to remove the closing slash so our regex is /.*
            if (excludeString.endsWith("/")) {
                excludeString = excludeString.substring(0, excludeString.length() - 1);
            }
            if (excludeString.contains("**")) {
                excludeString = excludeString.replace("**", ".*");
            }
            if (excludeString.contains("/*")) {
                excludeString = excludeString.replace("/*", "/.*");
            }
            //need to escape the file seperators correctly, as / is a regex keychar
            if (excludeString.contains("/")) {
                excludeString = excludeString.replace("/", "\\/");
            }
            //at this point we should not have any **, if we do replace with * as all * should be prefixed with a .
            if (excludeString.contains("**")) {
                excludeString = excludeString.replace("**", "*");
            }
            if (excludeString.startsWith("*")) {
                excludeString = "." + excludeString;
            }
            if (excludeString.contains("[")) {
                excludeString = excludeString.replace("[", "\\[");
            }
            if (excludeString.contains("]")) {
                excludeString = excludeString.replace("]", "\\]");
            }
            if (excludeString.contains("-")) {
                excludeString = excludeString.replace("-", "\\-");
            }

            try {
                excludesPattern = Pattern.compile(excludeString);
            } catch (PatternSyntaxException pse) {
                FFDCFilter.processException(pse, getClass().getName(), "looseconfigxmlpse");
                Tr.error(tc, "INVALID_EXCLUDE_PATTERN", originalString);
                //set pattern to null so code continues to run, but will not exclude anything
                excludesPattern = null;
            }
        }

        /**
         * checks the current file and the path of the root on the actual file system to see if they match
         * the excludes regex - returns false if it matches the excludes regex, returns true if it does not
         * match the excludes regex.
         * 
         * @param rootPath
         * @param currentFile
         * @return
         */
        public boolean fileNotExcluded(String rootPath, File currentFile) {
            //in case we have some bad formatting/concatonating resulting in double-slashes in path
            rootPath = rootPath.replace("//", "/");

            //if we need to, convert the \'s into /'s
            if ('\\' == File.separatorChar) {
                rootPath = rootPath.replace(File.separatorChar, '/');
            }

            //first we see if we get an exact location match for a file

            //if (currentPath-rootPath) matches excludePath string is in the start of the path then ignore file types asked to ignore
            if (excludesPattern.matcher((currentFile.getAbsolutePath().replace("\\", "/")).replace(rootPath, "/").replace("//", "/")).matches()) {
                return false;
            }

            //if no match is found we need to check if the current file/directory is beneath an excluded directory

            //while current file's path contains the root location's path
            while (currentFile.getAbsolutePath().replace("\\", "/").startsWith(rootPath)) {
                //if current file path minus the rootPath matches the regex
                if (excludesPattern.matcher((currentFile.getAbsolutePath().replace("\\", "/")).replace(rootPath, "/")).matches()) {
                    return false;
                }
                //else current file is now set to the parent and we continue recursive checking
                currentFile = currentFile.getParentFile();
            }
            return true;
        }

        @Override
        public InputStream getInputStream(String filename) {
            String onDiskPath = dirOnDisk + '/' + filename.substring(dirPrefixInArchive.length());
            File f = new File(onDiskPath);
            if (FileUtils.fileIsFile(f) && FileUtils.fileExists(f)) {
                try {
                    return FileUtils.getInputStream(f);
                } catch (FileNotFoundException e) {
                    FFDCFilter.processException(e, getClass().getName(), "looseconfigxmlstreamerrorindir");
                    Tr.error(tc, "DECLARED_FILE_NOT_FOUND", f.getAbsolutePath());
                    return null;
                }
            }
            return null;
        }

        @Override
        public long getSize(String filename) {
            String onDiskPath = dirOnDisk + '/' + filename.substring(dirPrefixInArchive.length());
            File f = new File(onDiskPath);
            if (FileUtils.fileIsFile(f) && FileUtils.fileExists(f)) {
                return FileUtils.fileLength(f);
            }
            return 0L;
        }

        /** {@inheritDoc} */
        @Override
        @FFDCIgnore(PrivilegedActionException.class)
        Collection<URL> getURLs(String path) {
            String pathToDirectory = getFirstPhysicalPath(path);
            if (System.getSecurityManager() == null) {
                try {
                    return Collections.singleton(new File(pathToDirectory).toURI().toURL());
                } catch (MalformedURLException e) {
                    return Collections.emptySet();
                }
            } else {
                final String f_pathToDirectory = pathToDirectory;
                try {
                    return AccessController.doPrivileged(
                                    new PrivilegedExceptionAction<Collection<URL>>() {
                                        @Override
                                        public Collection<URL> run() throws MalformedURLException {
                                            return Collections.singleton(new File(f_pathToDirectory).toURI().toURL());
                                        }
                                    }
                                    );
                } catch (PrivilegedActionException e) {
                    return Collections.emptySet();
                }
            }
        }

        @Override
        String getFirstPhysicalPath(String path) {
            // Need to append the part of the path that is extra to the target in archive onto the dirOnDisk
            String pathToAppend = path.substring(dirPrefixInArchive.length());
            String pathToDirectory = dirOnDisk;

            // Sort out /s
            if (!pathToDirectory.endsWith("/")) {
                if (!pathToAppend.startsWith("/")) {
                    pathToDirectory = pathToDirectory + "/";
                }
            } else if (pathToAppend.startsWith("/")) {
                pathToAppend = pathToAppend.substring(1);
            }
            pathToDirectory = pathToDirectory + pathToAppend;
            return new File(pathToDirectory).getAbsolutePath();
        }

        @Override
        public long getLastModified(String filename) {
            String onDiskPath = dirOnDisk + '/' + filename.substring(dirPrefixInArchive.length());
            File f = new File(onDiskPath);
            if (FileUtils.fileIsFile(f) && FileUtils.fileExists(f)) {
                return FileUtils.fileLastModified(f);
            }
            return 0L;
        }

        private static String stripTrailingSeparator(String s) {
            if (s.length() < 2) {
                return s;
            }
            if (isSeparatorChar(s.charAt(s.length() - 1))) {
                return s.substring(0, s.length() - 1);
            }
            return s;
        }

        private String createOnDiskPath(String path) {
            String dir = stripTrailingSeparator(path).substring(dirPrefixInArchive.length());
            if (dir.length() > 1 && isSeparatorChar(dir.charAt(0))) {
                return dirOnDisk + dir;
            }
            return dirOnDisk + '/' + dir;
        }

        private static boolean isSeparatorChar(char c) {
            // compile-time constant test
            if (File.separatorChar != '/') {
                if (c == File.separatorChar) {
                    return true;
                }
            }
            return c == '/';
        }

    }

    private static class FileEntryInfo extends EntryInfo {
        private final String filenameInArchive;
        private final String filenameOnDisk;
        private final LooseContainerFactoryHelper containerFactoryHelper;
        private final File fileOnDisk;

        public FileEntryInfo(String archiveFile, String onDiskFile, LooseContainerFactoryHelper cfh) {
            filenameInArchive = archiveFile;
            filenameOnDisk = onDiskFile;
            //archive uses a null file on disk.
            if (filenameOnDisk != null)
                fileOnDisk = new File(filenameOnDisk);
            else
                fileOnDisk = null;
            containerFactoryHelper = cfh;
        }

        @Override
        public void addMonitoringPaths(Set<String> files, Set<String> dirs, String path) {
            //add the filename on disk, preserving any ! prefix on path
            String pathToAdd = path.startsWith("!") ? "!" + filenameOnDisk : filenameOnDisk;
            files.add(pathToAdd);
        }

        @Override
        public boolean matches(String fileName) {
            return matches(fileName, true);
        }

        @Override
        public boolean matches(String fileName, boolean checkIfExists) {
            //if the file exists, return true, else false.
            if (filenameInArchive.equals(fileName)) {
                //archive uses a null fileOnDisk, and will always 'exist'.
                if (fileOnDisk != null)
                    return checkIfExists ? FileUtils.fileExists(fileOnDisk) : true;
                else
                    return true;
            }
            return false;
        }

        @Override
        public boolean isBeneath(String fileName) {
            if (fileName.length() < filenameInArchive.length()) {
                //if file name doesn't end in a "/"
                if (!fileName.endsWith("/")) {
                    fileName += "/";
                }
                return filenameInArchive.startsWith(fileName);
            }
            return false;
        }

        @Override
        public InputStream getInputStream(String fileName) {
            try {
                return FileUtils.getInputStream(new File(filenameOnDisk));
            } catch (FileNotFoundException e) {
                FFDCFilter.processException(e, getClass().getName(), "looseconfigxmlstreamerror");
                Tr.error(tc, "DECLARED_FILE_NOT_FOUND", fileName);
                return null;
            }
        }

        @Override
        public ArtifactContainer createContainer(boolean localOnly, LooseArchive parent, String path) {
            ArtifactContainer rv = null;
            if (!localOnly) {
                //Let other people have a crack at the conversion..
                LooseArchive owning = parent;
                ArtifactEntry e = owning.getEntry(path);
                ArtifactContainer c = e.getEnclosingContainer();

                // See if the container factory can create a container from us
                File newCacheDir = null;
                String relativeLocation = c.getPath();
                if (relativeLocation.equals("/")) {
                    newCacheDir = parent.getCacheDir();
                } else {
                    //use of substring 1 is ok here, because this entry MUST be within a container, and the smallest path
                    //as container can have is "/", which is dealt with above, therefore, in this branch the relativeLocation MUST 
                    //be longer than "/"
                    newCacheDir = new File(parent.getCacheDir(), relativeLocation.substring(1));
                }
                //newCacheDir = new File(newCacheDir, e.getName());
                rv = containerFactoryHelper.getContainerFactory().getContainer(newCacheDir, c, e, new File(filenameOnDisk));
            }
            return rv;
        }

        @Override
        public String getVirtualLocation() {
            return filenameInArchive;
        }

        @Override
        public long getSize(String fileName) {
            File f = new File(filenameOnDisk);
            if (FileUtils.fileIsFile(f) && FileUtils.fileExists(f)) {
                return FileUtils.fileLength(f);
            }
            return 0L;
        }

        /** {@inheritDoc} */
        @Override
        Collection<URL> getURLs(String path) {
            // According to the JavaDoc for this method matches must be run so we can assume the path points to the file and return its URI
            try {
                return Collections.singleton(new File(filenameOnDisk).toURI().toURL());
            } catch (MalformedURLException e) {
                return Collections.emptySet();
            }
        }

        @Override
        String getFirstPhysicalPath(String path) {
            return new File(filenameOnDisk).getAbsolutePath();
        }

        @Override
        public long getLastModified(String filename) {
            File f = new File(filenameOnDisk);
            if (FileUtils.fileIsFile(f) && FileUtils.fileExists(f)) {
                return FileUtils.fileLastModified(f);
            }
            return 0L;
        }
    }

    private static class ArchiveEntryInfo extends FileEntryInfo {
        private final LooseArchive la;

        public ArchiveEntryInfo(String archiveFile, LooseArchive archive, LooseContainerFactoryHelper cfh) {
            super(archiveFile, null, cfh);
            la = archive;
        }

        @Override
        public void addMonitoringPaths(Set<String> files, Set<String> dirs, String path) {
            //no-op.
        }

        @Override
        public InputStream getInputStream(String filename) {
            return null;
        }

        @Override
        public ArtifactContainer createContainer(boolean localOnly, LooseArchive parent, String path) {
            if (!localOnly)
                return la;
            else
                return null;
        }

        @Override
        public long getSize(String fileName) {
            return 0L;
        }

        /** {@inheritDoc} */
        @Override
        public Collection<URL> getURLs(String path) {
            // Return the URI of the loose archive
            return la.getURLs();
        }

        @Override
        public String getFirstPhysicalPath(String path) {
            return la.getPhysicalPath();
        }
    }

    public LooseArchive(File cacheDir, LooseContainerFactoryHelper cfh, File xmlPath) {
        this.cacheDir = cacheDir;
        parent = null;
        this.cfh = cfh;
        this.xmlFile = xmlPath;
    }

    public LooseArchive(File cacheDir, LooseContainerFactoryHelper cfh, LooseArchive prior, String pathInPrior, File xmlPath) {
        this.cacheDir = cacheDir;
        parent = prior;
        pathInParent = pathInPrior;
        this.cfh = cfh;
        this.xmlFile = xmlPath;
    }

    //package protected method for the notifier to query the xml path..
    File getXMLFile() {
        return this.xmlFile;
    }

    @Override
    public Iterator<ArtifactEntry> iterator() {
        return iterator("/");
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        if (parent != null) {
            return parent.getEntry(pathInParent).getEnclosingContainer();
        } else
            return null;
    }

    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        if (parent != null) {
            return parent.getEntry(pathInParent);
        } else
            return null;
    }

    @Override
    public String getPath() {
        return "/";
    }

    @Override
    public String getName() {
        return "/";
    }

    @Override
    public void useFastMode() {}

    @Override
    public void stopUsingFastMode() {}

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public ArtifactEntry getEntry(String pathAndName) {
        pathAndName = PathUtils.normalizeUnixStylePath(pathAndName);

        //check the path is not trying to go upwards.
        if (!PathUtils.isNormalizedPathAbsolute(pathAndName)) {
            return null;
        }

        if (pathAndName.equals("/") || pathAndName.equals("")) {
            return null;
        }

        if (!pathAndName.startsWith("/")) {
            pathAndName = '/' + pathAndName;
        }

        //remove trailing /'s 
        if (pathAndName.endsWith("/")) {
            pathAndName = pathAndName.substring(0, pathAndName.length() - 1);
        }

        //go through already processed entries to see if we've already processed the one we have now
        for (EntryInfo ei : _entries) {
            if (ei.matches(pathAndName)) {
                return new LooseEntry(this, ei, pathAndName);
            }
        }
        //go through already processed entries to see if the current one we have (pathAndName) falls beneath them
        for (EntryInfo ei : _entries) {
            if (ei.isBeneath(pathAndName)) {
                return new LooseEntry(this, null, pathAndName);
            }
        }
        //pathAndName entry is not already added, nor have any of its parents (in the vfs) been added already.
        return null;
    }

    public Iterator<ArtifactEntry> iterator(String path) {

        Map<String, EntryInfo> names = new LinkedHashMap<String, EntryInfo>();
        for (EntryInfo ei : _entries) {
            if (ei.matches(path)) {
                if (ei instanceof DirEntryInfo) {
                    DirEntryInfo dei = (DirEntryInfo) ei;
                    for (String s : dei.getChildren(path)) {
                        //if we have not already found a version of this file (we only return the first we find)
                        if (!names.containsKey(s)) {
                            names.put(s, ei);
                        }
                    }
                }
            }
            // if ei instanceof fileEntryInfo and ei matches parentof path also add to names
            if ((ei instanceof FileEntryInfo || ei instanceof DirEntryInfo) && path.equals(PathUtil.getParent(ei.getVirtualLocation()))) {
                //if we have not already found a version of this file (we only return the first we find)

                if (!names.containsKey(ei.getVirtualLocation())) {

                    names.put(ei.getVirtualLocation(), ei);

                }
            }
        }
        for (EntryInfo ei : _entries) {
            if (ei.isBeneath(path)) {
                String startOfPath = "";
                String pathSeparator = "/";
                if (path.equals("/")) {
                    startOfPath = path;
                    pathSeparator = "";
                }
                String dir = PathUtil.getFirstPathComponent(startOfPath + ei.getVirtualLocation().substring(path.length()));

                dir = path + pathSeparator + dir;
                if (!names.containsKey(dir)) {
                    names.put(dir, null);
                }
            }
        }
        return new EntryIterator(names, this);
    }

    private static class EntryIterator implements Iterator<ArtifactEntry> {
        final private Iterator<Map.Entry<String, EntryInfo>> fileListIterator;
        final private LooseArchive looseArch;

        public EntryIterator(Map<String, EntryInfo> names, LooseArchive looseA) {
            fileListIterator = names.entrySet().iterator();
            looseArch = looseA;
        }

        @Override
        public boolean hasNext() {
            return fileListIterator.hasNext();
        }

        @Override
        public ArtifactEntry next() {
            Map.Entry<String, EntryInfo> t = fileListIterator.next();
            LooseEntry le = new LooseEntry(looseArch, t.getValue(), t.getKey());
            return le;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ArtifactContainer getRoot() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<URL> getURLs() {
        // Return all the root URIs
        return getURLs("/");
    }

    @Override
    public String getPhysicalPath() {
        return getPhysicalPath("/");
    }

    String getPhysicalPath(String path) {
        for (EntryInfo ei : _entries) {
            if (ei.matches(path)) {
                return ei.getFirstPhysicalPath(path);
            }
        }
        return null;
    }

    /**
     * <p>
     * This returns all of the URIs that contribute to the supplied path. This only supports nested entities that are exact matches to the path, it will not pick up files that are
     * beneath the path. This is because if you have an entry such as:
     * </p>
     * <code>
     * &lt;archive&gt;<br/>
     * &lt;file locationOnDisk="C:/MyClass.class" targetInArchive="WEB-INF/classes/my/package"/&gt;<br/>
     * &lt;/archive&gt;<br/>
     * </code>
     * <p>
     * then there is no URL for the path <code>"WEB-INF/classes"</code> path that makes sense.
     * </p>
     * 
     * @param path The path to get the URIs for
     * @return A collection of URIs for this path
     */
    Collection<URL> getURLs(String path) {
        // Go through all of the items that match this path and get the URIs for them
        Collection<URL> urls = new HashSet<URL>();
        for (EntryInfo ei : _entries) {
            if (ei.matches(path)) {
                urls.addAll(ei.getURLs(path));
            }
        }
        return urls;
    }

    //package protected method to let the entry info's obtain the cacheDir for this LooseArchive
    File getCacheDir() {
        return this.cacheDir;
    }

    /** {@inheritDoc} */
    @Override
    public final synchronized ArtifactNotifier getArtifactNotifier() {
        if (this.artifactNotifier == null) {
            cascadeCreateArtifactNotifier();
        }
        return this.artifactNotifier;
    }

    private void cascadeCreateArtifactNotifier() {
        LooseArchive root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        root.createArtifactNotifier(null);
    }

    private LooseArtifactNotifier createArtifactNotifier(LooseArtifactNotifier parent) {
        artifactNotifier = new LooseArtifactNotifier(this, _entries, cfh.getBundleContext(), parent, pathInParent);
        for (EntryInfo entry : _entries) {
            if (entry instanceof ArchiveEntryInfo) {
                LooseArchive childArchive = ((ArchiveEntryInfo) entry).la;
                artifactNotifier.addChild(entry, childArchive.createArtifactNotifier(artifactNotifier));
            }
        }
        return artifactNotifier;
    }

}