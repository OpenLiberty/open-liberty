/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.impl;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.exception.TopologyException;
import componenttest.topology.utils.LibertyServerUtils;

/**
 * Class for managing files relative to a liberty server.
 */
public class LibertyFileManager {
    private static final Class<? extends LibertyFileManager> CLASS = LibertyFileManager.class;

    /**
     * Container object to encapsulate the search results and last
     * position within the searched file.
     */
    public static class LogSearchResult {
        private final Long offset;
        private final List<String> matches;
        private final String firstSearchedLine;
        private final String lastSearchedLine;

        LogSearchResult(Long offset, List<String> matches, String firstSearchedLine, String lastSearchedLine) {
            this.offset = offset;
            this.matches = matches;
            this.firstSearchedLine = firstSearchedLine;
            this.lastSearchedLine = lastSearchedLine;
        }

        @Override
        public String toString() {
            return super.toString() + "[matches=" + matches.size() + ", offset=" + offset + ']';
        }

        Long getOffset() {
            return offset;
        }

        List<String> getMatches() {
            return matches;
        }

        String getFirstLine() {
            return firstSearchedLine;
        }

        String getLastLine() {
            return lastSearchedLine;
        }
    }

    /**
     * Searches the given file for multiple instances of the given regular expression.
     *
     * @param numberOfMatches number of matches required
     * @param regexp a regular expression (or just a text snippet) to search for
     * @param fileToSearch the file to search
     * @return Number of matches found
     * @throws Exception
     */
    static int findMultipleStringsInFile(int numberOfMatches, String regexp,
                                         RemoteFile fileToSearch) throws Exception {
        final String method = "findMultipleStringsInFile";
        Log.entering(CLASS, method);

        List<String> matches = Collections.emptyList();

        LogSearchResult allMatches = findStringsInFileCommon(Collections.singletonList(regexp), numberOfMatches, fileToSearch, 0L);
        if (allMatches != null) {

            // only get obj[1] since we only want the matches found for this method
            matches = allMatches.getMatches();

            //Produce some trace if user was interested in more than one match but didn't find enough
            if (numberOfMatches > 1 && matches.size() < numberOfMatches) {
                Log.finer(CLASS, method, "Only found " + matches.size() + " matches");
            }
        }

        Log.exiting(CLASS, method);
        return matches.size();
    }

    /**
     * Searches the given file for the given regular expression.
     *
     * @param regexp a regular expression (or just a text snippet) to search for
     * @param fileToSearch the file to search
     * @return The first line which includes the pattern, or null if the pattern isn't found or if the file doesn't exist
     * @throws Exception
     */
    static String findStringInFile(String regexp,
                                   RemoteFile fileToSearch) throws Exception {
        final String method = "findStringInFile";
        Log.entering(CLASS, method);

        String foundString = null;

        LogSearchResult allMatches = findStringsInFileCommon(Collections.singletonList(regexp), 1, fileToSearch, 0L);
        if (allMatches != null) {
            // only get obj[1] since we only want the matches found for this method
            List<String> matches = allMatches.getMatches();

            if (matches != null && !matches.isEmpty()) {
                foundString = matches.get(0);
            }
        }

        Log.exiting(CLASS, method);
        return foundString;

    }

    /**
     * Searches the given file starting at the given offset for the given
     * regular expression. Halts on the first match and returns the offset
     * of the first match.
     *
     * @param regexp a regular expression (or just a text snippet) to search for
     * @param fileToSearch the file to search
     * @param offset the position to start the search
     * @return LogSearchResult containing the new offset for the log and a {@code List<String>} holding the matches found.
     *         The list is empty if no matches found.
     * @throws Exception
     */
    static LogSearchResult findStringInFile(String regexp, RemoteFile fileToSearch, Long offset) throws Exception {
        final String method = "findStringInFile";
        Log.entering(CLASS, method, new Object[] { regexp, fileToSearch.getAbsolutePath(), offset });

        LogSearchResult offsetAndMatches = findStringsInFileCommon(Collections.singletonList(regexp), 1, fileToSearch, offset);

        Log.exiting(CLASS, method);
        return offsetAndMatches;
    }

    /**
     * Searches the given file for the given regular expression.
     *
     * @param regexp a regular expression (or just a text snippet) to search for
     * @param fileToSearch the file to search
     * @return List of Strings which match the pattern. No match results in an empty list.
     * @throws Exception
     */
    public static List<String> findStringsInFile(String regexp,
                                                 RemoteFile fileToSearch) throws Exception {
        final String method = "findStringsInFile";
        Log.entering(CLASS, method);

        List<String> matches = null;

        LogSearchResult allMatches = findStringsInFileCommon(Collections.singletonList(regexp), Integer.MAX_VALUE, fileToSearch, 0L);
        if (allMatches != null) {
            // only get obj[1] since we only want the matches found for this method
            matches = allMatches.getMatches();
        }

        Log.exiting(CLASS, method);
        return matches;
    }

    /**
     * Searches the given file starting at the given offset for the given regular expression.
     *
     * @param regexp a regular expression (or just a text snippet) to search for
     * @param fileToSearch the file to search
     * @param offset the position to start the search
     * @return LogSearchResult containing the new offset for the log and a {@code List<String>} holding the matches found.
     *         The list is empty if no matches found.
     * @throws Exception
     */
    static LogSearchResult findStringsInFile(String regexp, RemoteFile fileToSearch, Long offset) throws Exception {
        final String method = "findStringsInFile";
        Log.entering(CLASS, method, new Object[] { regexp, fileToSearch.getAbsolutePath(), offset });

        LogSearchResult offsetAndMatches = findStringsInFileCommon(Collections.singletonList(regexp), Integer.MAX_VALUE, fileToSearch, offset);

        Log.exiting(CLASS, method);
        return offsetAndMatches;
    }

    /**
     * Searches the given file for the given regular expressions, starting at the given file offset.
     *
     * @param regexpList a list of regular expressions to search for
     * @param fileToSearch the file to search
     * @param offset the position in the file to start the search
     * @return LogSearchResult containing the new offset for the log and a {@code List<String>} holding the matches found.
     *         The list is empty if no matches found.
     * @throws Exception
     */
    static LogSearchResult findStringsInFile(List<String> regexpList, RemoteFile fileToSearch, Long offset) throws Exception {
        final String method = "findStringsInFile";
        Log.entering(CLASS, method, new Object[] { regexpList, fileToSearch.getAbsolutePath(), offset });

        LogSearchResult offsetAndMatches = findStringsInFileCommon(regexpList, Integer.MAX_VALUE, fileToSearch, offset);

        Log.exiting(CLASS, method);
        return offsetAndMatches;
    }

    /**
     * Searches the given file for the given regular expression.
     *
     * @param regexpList a list of regular expressions to search for
     * @param searchLimit the maximum number of times the regexps will be searched for.
     * @param fileToSearch the file to search
     * @param offset the position to start the search
     * @return LogSearchResult containing the new offset for the log and a {@code List<String>} holding the matches found.
     *         The list is empty if no matches found.
     *         If the file does not exist, {@code null} is returned.
     * @throws Exception
     */
    static LogSearchResult findStringsInFileCommon(List<String> regexpList,
                                                   int searchLimit,
                                                   RemoteFile fileToSearch,
                                                   Long offset) throws Exception {
        final String method = "findStringsInFileCommon";
        Log.entering(CLASS, method, new Object[] { regexpList, (searchLimit == Integer.MAX_VALUE ? "(all)" : searchLimit), fileToSearch.getAbsolutePath(), offset });

        if (!fileToSearch.exists()) {
            Log.info(CLASS, method, "The file being validated doesn't exist: " + fileToSearch.getAbsolutePath());
            return null;
        }

        InputStream rawInput = new BufferedInputStream(fileToSearch.openForReading());
        CountingInputStream input = new CountingInputStream(rawInput);
        LineReader reader = null;
        List<String> matches = null;
        Long newOffset;
        String firstLine = null;
        String lastLine = null;

        try {

            for (long totalSkipped = 0; totalSkipped < offset;) {
                long skipped = input.skip(offset);
                if (skipped == 0) {
                    Log.info(CLASS, method, "The file might have been rotated: " + fileToSearch.getAbsolutePath());
                    return null;
                }
                totalSkipped += skipped;
            }

            UnbufferedInputStreamReader rawReader = new UnbufferedInputStreamReader(input, fileToSearch.getEncoding());
            reader = new LineReader(rawReader);

            Log.finer(CLASS, method, "Now looking for strings " + regexpList
                                 + " in the file " + fileToSearch.getName());

            Pattern[] patterns = new Pattern[regexpList.size()];
            for (int i = 0; i < regexpList.size(); i++) {
                patterns[i] = Pattern.compile(regexpList.get(i));
            }

            matches = new ArrayList<String>();
            newOffset = offset;

            for (String line; (line = reader.readLine()) != null;) {
                Log.finest(CLASS, method, "offset " + newOffset + ": " + line);

                // We only want to match against complete lines.
                if (!reader.eof()) {
                    if (firstLine == null)
                        firstLine = line;
                    lastLine = line;

                    newOffset = input.count();

                    if (findAny(line, patterns)) {
                        matches.add(line);
                        Log.finer(CLASS, method, "Match number " + matches.size() + " after reading " + input.count() + " bytes");
                        if (matches.size() >= searchLimit) {
                            break;
                        }
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            } else {
                // if the reader is null, we might still have the input stream
                // if there was an early return
                if (input != null) {
                    input.close();
                }

            }
        }

        LogSearchResult result = new LogSearchResult(newOffset, matches, firstLine, lastLine);
        Log.exiting(CLASS, method, result);
        return result;
    }

    private static boolean findAny(String line, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Answer a remote file on a specified machine and having a specified absolute
     * path.
     * 
     * @param machine The machine which has the specified file.
     * @param absPath The absolute path of the file.
     *
     * @return A remote file which encapsulates the machine and path.
     *
     * @throws Exception Thrown if the target file does not exist.
     */
    public static RemoteFile getLibertyFile(Machine machine, String absPath) throws Exception {
        RemoteFile remoteFile = createRemoteFile(machine, absPath);
        if ( !remoteFile.exists() ) {
            String msg = "File \'" + absPath + "\' on machine '" + machine + "' does not exist.";
            throw new FileNotFoundException(msg);
        }
        return remoteFile;
    }

    /**
     * Tell if a remote file exists.
     *
     * @param machine The machine which has the specified file.
     * @param absPath The absolute path of the file.
     *
     * @return True or false telling if the target remote file exists.
     */
    
    public static boolean libertyFileExists(Machine machine, String absPath) throws Exception {
        RemoteFile file = createRemoteFile(machine, absPath);
        return file.exists();
    }

    /**
     * Create a remote file which encapsulates a machine and an absolute path.
     * The target remote file need not exist.
     *
     * @param machine The machine which has the specified file.
     * @param absPath The absolute path of the file.
     *
     * @return A remote file which encapsulates the machine and path.
     */
    public static RemoteFile createRemoteFile(Machine machine, String absPath) {
        return new RemoteFile(machine, LibertyServerUtils.makeJavaCompatible(absPath, machine));
    }

    // Delete operations ...

    /**
     * Attempt to delete a file on a specified machine.
     *
     * Emit a log message describing one of three result cases:
     *
     * 1) The target file already does not exist.
     * 2) The target file was deleted.
     * 3) The target file was not deleted.
     *
     * Retry the deletion operation if the first attempt fails.
     * See {@link RemoteFile#delete()}.
     *
     * @param machine The machine which has the specified file.
     * @param absPath The absolute path of the file.
     *
     * @throws Exception Thrown in case of an error deleting the file.
     */
    public static void deleteLibertyFile(Machine machine, String absPath) throws Exception {
        String method = "deleteLibertyFile";

        // TODO: For now, don't log the machine, since it doesn't seem to
        //       be significant.
        // + " on machine '" + machine + "'";
        String prefix = "File '" + absPath + "'";
        Log.finer(CLASS, method, prefix);

        RemoteFile remoteFile = createRemoteFile(machine, absPath);
        if ( !remoteFile.exists() ) {
            Log.finer(CLASS, method, prefix + " does not exist");
        } else {
            if ( remoteFile.delete() ) { // throws Exception
                Log.finer(CLASS, method, prefix + " was deleted");
            } else {
                Log.finer(CLASS, method, prefix + " could not be deleted");
            }
        }
    }

    public static void deleteLibertyDirectoryAndContents(Machine machine, String absPath) throws Exception {
        deleteLibertyFile(machine, absPath); // throws Exception
    }

    //

    /**
     * This copies the named file into the liberty server and maintains the name of the file. This is equivalent to calling
     * {@link #copyFileIntoLiberty(Machine, String, String, String)} with the file name set to the name part of the <code>relPathTolocalFile</code> parameter.
     *
     * @param machine The machine to copy the file to
     * @param path The path to copy the file to
     * @param relPathTolocalFile The path to the file being copied
     * @return The path of the copied file
     * @throws Exception
     */
    public static String copyFileIntoLiberty(Machine machine, String path, String relPathTolocalFile) throws Exception {
        LocalFile localFileToCopy = new LocalFile(LibertyServerUtils.makeJavaCompatible(relPathTolocalFile));
        return LibertyFileManager.copyFileIntoLiberty(machine, path, localFileToCopy.getName(), relPathTolocalFile);
    }

    //

    public static boolean renameLibertyFile(Machine machine, String oldFilePath, String newFilePath) throws Exception {
        RemoteFile source = createRemoteFile(machine, oldFilePath);
        RemoteFile target = createRemoteFile(machine, newFilePath);
        if ( source.exists() ) {
            return source.rename(target);
        } else {
            Log.info(CLASS, "renameLibertyFile", "Copying: " + source.getAbsolutePath() + " to " + target.getAbsolutePath() + " failed because " + source.getAbsolutePath() + "does not exist");
        }
        return false;
    }

    public static boolean renameLibertyFileNoRetry(Machine machine, String oldFilePath, String newFilePath) throws Exception {
        RemoteFile source = createRemoteFile(machine, oldFilePath);
        RemoteFile target = createRemoteFile(machine, newFilePath);
        if ( source.exists() ) {
            return source.renameNoRetry(target);
        } else {
            Log.info(CLASS, "renameLibertyFileNoRetry", "Copying: " + source.getAbsolutePath() + " to " + target.getAbsolutePath() + " failed because " + source.getAbsolutePath() + "does not exist");
        }
        return false;
    }

    //

    /**
     * This method will copy a file into the Liberty server using the file name provided, it will not copy the contents of the file if it is a directory.
     *
     * @param machine The machine to copy the file to
     * @param path The path to copy the file to
     * @param destinationFileName The name of the file in the destination, this needs to be Java compatible
     * @param relPathTolocalFile The path to the file being copied
     * @return The path of the copied file
     * @see LibertyServerUtils#makeJavaCompatible(String)
     * @throws Exception
     */
    public static String copyFileIntoLiberty(Machine machine, String path, String destinationFileName, String relPathTolocalFile) throws Exception {
        return copyFileIntoLiberty(machine, path, destinationFileName, relPathTolocalFile, false);
    }

    /**
     * This method will copy a file into the Liberty server using the file name provided.
     *
     * @param machine The machine to copy the file to
     * @param path The path to copy the file to
     * @param destinationFileName The name of the file in the destination, this needs to be Java compatible
     * @param relPathTolocalFile The path to the file being copied
     * @param recursivelyCopy <code>true</code> if child files should also be copied
     * @return The path of the copied file
     * @see LibertyServerUtils#makeJavaCompatible(String)
     * @throws Exception
     */
    public static String copyFileIntoLiberty(Machine machine, String path, String destinationFileName, String relPathTolocalFile, boolean recursivelyCopy) throws Exception {
        return copyFileIntoLiberty(machine, path, destinationFileName, relPathTolocalFile, recursivelyCopy, null);
    }

    /**
     * This method will copy a file into the Liberty server using the file name provided.
     * If a single file is being copied to a destination that does not exist and tmpDir
     * is specified, then the file will be copied to a temporary file and then renamed
     * into place, which avoids other processes reading a partially written file.
     *
     * @param machine The machine to copy the file to
     * @param path The path to copy the file to
     * @param destinationFileName The name of the file in the destination, this needs to be Java compatible
     * @param relPathTolocalFile The path to the file being copied
     * @param recursivelyCopy <code>true</code> if child files should also be copied
     * @param tmpDir The temporary directory to use if atomically copying
     * @return The path of the copied file
     * @see LibertyServerUtils#makeJavaCompatible(String)
     * @throws Exception
     */
    public static String copyFileIntoLiberty(Machine machine,
                                             String path,
                                             String destinationFileName,
                                             String relPathTolocalFile,
                                             boolean recursivelyCopy,
                                             String tmpDir) throws Exception {
        LocalFile src = new LocalFile(LibertyServerUtils.makeJavaCompatible(relPathTolocalFile));
        RemoteFile dest = new RemoteFile(machine, path + "/" + destinationFileName);
        RemoteFile destFile = dest.isDirectory() ? new RemoteFile(dest, src.getName()) : dest;
        RemoteFile destDir = dest.isDirectory() ? dest : destFile.getParentFile();

        // If possible, we would like to create the destination file atomically
        // in order to avoid file monitoring issues.  We atomically create by
        // copying to a temporary file and then renaming to the actual
        // destination, which can only succeed if the rename() will succeed:
        // the destination must not exist. We could also allow if the destination
        // exists as a file, but renameTo(...) fails on Windows.
        if (tmpDir != null && src.isFile() && !destFile.exists()) {
            // We choose the temporary filename by appending a timestamp to the
            // destination filename in the temporary directory.  We don't use
            // Machine.getTempDir as that could be /tmp, which could be in a
            // different file system (e.g., tmpfs on Linux), which would cause
            // rename to fail.
            RemoteFile tmpFile = new RemoteFile(machine, tmpDir + "/" + destFile.getName() + '.' + System.currentTimeMillis());
            Log.info(CLASS, "copyFileIntoLiberty", "Copying: " + src.getAbsolutePath() + " to " + dest.getAbsolutePath() + " via " + tmpFile.getAbsolutePath());
            if (!src.copyToDest(tmpFile, recursivelyCopy, true)) {
                throw new TopologyException("Failed to copy " + src.getAbsolutePath() + " to " + tmpFile.getAbsolutePath());
            }

            // Create the destination directory as RemoteFile.copyToDest would.
            Log.finer(CLASS, "copyFileIntoLiberty", "mkdirs " + destDir);
            destDir.mkdirs();

            if (!tmpFile.rename(destFile)) {
                // Make a best effort to clean up the temp file.
                String extraMessage = "";
                Throwable cause = null;
                try {
                    if (!tmpFile.delete()) {
                        extraMessage = ", and failed to delete the temporary file";
                    }
                } catch (Exception e) {
                    extraMessage = ", and failed to delete the temporary file";
                    cause = e;
                }

                throw new TopologyException("Failed to rename " + tmpFile.getAbsolutePath() + " to " + destFile.getAbsolutePath() + extraMessage, cause);
            }
            Log.finer(CLASS, "copyFileIntoLiberty", "Done: copied " + destFile.length() + " / " + destFile.length() + " bytes");
        } else {
            Log.info(CLASS, "copyFileIntoLiberty", "Copying: " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
            if (!src.copyToDest(dest, recursivelyCopy, true)) {
                throw new TopologyException("Failed to copy " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
            }
            Log.finer(CLASS, "copyFileIntoLiberty", "Done: copied " + dest.length() + " / " + src.length() + " bytes");
        }

        Log.finer(CLASS, "copyFileIntoLiberty", "Copy successfull!");
        return dest.getAbsolutePath();
    }

    public static String moveFileIntoLiberty(Machine machine, String path, String destinationFileName, String relPathTolocalFile) throws Exception {
        LocalFile localFile = new LocalFile(LibertyServerUtils.makeJavaCompatible(relPathTolocalFile));
        RemoteFile remoteFileTmp = new RemoteFile(machine, path + "/" + destinationFileName + ".tmp");
        RemoteFile remoteFile = new RemoteFile(machine, path + "/" + destinationFileName);

        Log.info(CLASS, "moveFileIntoLiberty", "Copying " + localFile.getAbsolutePath() + " to " + remoteFileTmp.getAbsolutePath());

        if (!localFile.copyToDest(remoteFileTmp, false, true)) {
            throw new TopologyException("The file \"" + localFile.getName() + "\" failed to copy");
        }

        moveLibertyFile(remoteFileTmp, remoteFile);

        return remoteFile.getAbsolutePath();
    }

    public static void moveLibertyFile(RemoteFile srcFile, RemoteFile dstFile) throws Exception {
        Log.info(CLASS, "moveLibertyFile", "Moving " + srcFile.getAbsolutePath() + " to " + dstFile.getAbsolutePath());
        if (srcFile.rename(dstFile)) {
            Log.info(CLASS, "moveLibertyFile", dstFile.getName() + " was successfully moved.");
        } else {
            // rename failed, let's copy
            if (srcFile.copyToDest(dstFile)) {
                srcFile.delete();
                Log.info(CLASS, "moveLibertyFile", dstFile.getName() + " was successfully copied.");
            } else {
                srcFile.delete();
                throw new TopologyException("Failed to move " + srcFile.getAbsolutePath() + " to " + dstFile.getAbsolutePath());
            }
        }
    }

    public static String getInstallPath(Bootstrap bootstrap) throws Exception {
        String installPath = bootstrap.getValue("libertyInstallPath");
        Machine machine = LibertyServerUtils.createMachine(bootstrap);
        return getLibertyFile(machine, installPath).getAbsolutePath();
    }
}
