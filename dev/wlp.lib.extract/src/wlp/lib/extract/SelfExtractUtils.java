/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import wlp.lib.extract.Content.Entry;
import wlp.lib.extract.SelfExtractor.ExternalDependency;
import wlp.lib.extract.platform.PlatformUtils;

/**
 * Some utils to simplify the contents of SelfExtract.java
 */
public class SelfExtractUtils {

    private static final int LINE_WRAP_COLUMNS = 72;
    //URL object representing the URL in the dependencies XML
    public static final String DOWNLOAD_URL = "download.url";
    //String object equal to the relative path supplied in the dependencies XML
    public static final String DOWNLOAD_TARGET = "download.target";
    //File object representing the absolute path on disk where the file is being downloaded to
    public static final String DOWNLOAD_TARGET_FILE = "download.target.file";
    //Integer holding content length of the download in bytes
    public static final String DOWNLOAD_CONTENT_SIZE = "download.content.size";
    // True if running on Windows (.bat files, no chmod, etc.)
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).indexOf("win") >= 0;
    // Manifest subsystem-content attribute name
    private static final String SUBSYSTEM_CONTENT = "Subsystem-Content";
    // Manifest subsystem-content attribute location directive name
    private static final String LOCATION_DIRECTIVE = "location:=";
    // Manifest subsystem-content attribute ibm.executable directive name
    private static final String EXECUTABLE_DIRECTIVE = "ibm.executable:=";

    public static final File getSelf() {
        File s = null;

        ProtectionDomain pd = SelfExtract.class.getProtectionDomain();
        CodeSource cs = pd.getCodeSource();
        if (cs != null) {
            URL url = cs.getLocation();
            if (url != null) {
                try {
                    s = new File(new URI(url.toString()));
                } catch (URISyntaxException ex) {
                    if (url.getProtocol().equals("file")) {
                        s = new File(url.getPath());
                    }
                }
            }
        }

        return s;
    }

    static final Entry getLicenseFile(Content container, String prefix) {
        if (!!!prefix.endsWith("_")) {
            prefix = prefix + "_";
        }

        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        String country = locale.getCountry();

        Entry entry = null;
        String[] suffixes = new String[] { lang + '_' + country, lang, "en" };
        for (int i = 0; i < suffixes.length; i++) {
            entry = container.getEntry(prefix + suffixes[i]);
            if (entry != null) {
                break;
            }
        }
        return entry;
    }

    /**
     * @param licenseFile License file to display: paginate/word wrap
     */
    public static Exception showLicenseFile(InputStream in) {
        List lines = new ArrayList();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, "UTF-16"));
            for (String line; (line = reader.readLine()) != null;) {
                wordWrap(line, lines);
            }

            for (int i = 0; i < lines.size(); i++) {
                System.out.println(lines.get(i));
            }
            System.out.flush();
            System.out.println();
            return null;
        } catch (Exception e) {
            return e;
        } finally {
            tryToClose(reader);
        }
    }

    static List wordWrap(String line, List lines) {
        if (lines == null)
            lines = new ArrayList();

        // If lines is empty, add empty string and return;
        if (line.length() == 0) {
            lines.add("");
            return lines;
        }

        // Split a more complicated line...
        for (int begin = 0; begin < line.length();) {
            // ??? Java has no wcwidth (Unicode TR#11), so we assume
            // all code points have a console width of 1.
            // ??? This code assumes all characters are BMP.

            // Does the rest of the string fit in a single line?
            if (begin + LINE_WRAP_COLUMNS >= line.length()) {
                lines.add(line.substring(begin));
                break;
            }

            // Choose a split point.
            int tryEnd = Math.min(line.length(), begin + LINE_WRAP_COLUMNS);

            // If we're in the middle of a word, find the beginning.
            int end = tryEnd;
            while (end > begin && !Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }

            // Skip preceding whitespace.
            while (end > begin && Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }

            // If we couldn't find a preceding split point, then this
            // is a really long word (e.g., a URL).  Find the end of
            // the word and add it without splitting.
            if (end == begin) {
                end = tryEnd;
                while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
                    end++;
                }
            }

            lines.add(line.substring(begin, end));

            // Skip whitespace and find the beginning of the next word.
            begin = end;
            while (begin < line.length() && Character.isWhitespace(line.charAt(begin))) {
                begin++;
            }
        }

        // phew.
        return lines;
    }

    static final Exception makeExecutable(List scripts, ExtractProgress ep) {
        List cmd = new ArrayList();
        cmd.add("chmod");
        cmd.add("+x");
        cmd.addAll(scripts);

        ep.commandRun(cmd);

        try {
            Process p = Runtime.getRuntime().exec((String[]) cmd.toArray(new String[cmd.size()]));
            Thread stdout = copyAsync(p.getInputStream(), System.out);
            Thread stderr = copyAsync(p.getErrorStream(), System.err);

            stdout.join();
            stderr.join();
            p.waitFor();
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    private static Thread copyAsync(final InputStream in, final OutputStream out) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                byte[] buf = new byte[4096];
                try {
                    for (int read; (read = in.read(buf)) != -1;) {
                        out.write(buf, 0, read);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
        thread.start();
        return thread;
    }

    public static final void tryToClose(ZipFile f) {
        if (f != null) {
            try {
                f.close();
            } catch (Exception e) {
            }
        }
    }

    /** Close the stream. Sadly, JDK 1.4 does not have Closable */
    public static final void tryToClose(Reader c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
            }
        }
    }

    /** Close the stream. Sadly, JDK 1.4 does not have Closable */
    public static final void tryToClose(InputStream c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
            }
        }
    }

    /** Close the stream. Sadly, JDK 1.4 does not have Closable */
    public static final void tryToClose(OutputStream c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * @param zipself
     * @return
     */
    public static List getEmptyDirectories(Attributes attribs) {

        if (attribs != null) {
            String value = attribs.getValue("Empty-Dirs");
            if (value != null) {
                return Arrays.asList(value.split(","));
            }
        }

        return Arrays.asList(new Object[] { "usr/shared/apps", "usr/shared/config", "usr/shared/resources", "templates/servers/defaultServer/apps",
                                            "templates/servers/defaultServer/dropins" });
    }

    /**
     * @param outputDir
     * @param keepDir
     */
    static void delete(File outputDir, boolean keepDir) {
        if (outputDir.isDirectory()) {
            File[] files = outputDir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    delete(files[i], false);
                }
            }
        }

        if (!!!keepDir) {
            outputDir.delete();
        }
    }

    static int tryGetContentLengthOfURL(URL url) {
        try {
            return url.openConnection().getContentLength();
        } catch (Exception e) {
            return -1;
        }
    }

    static List convertDependenciesListToMapsList(SelfExtractor.ExternalDependencies deps) {
        List depsList = deps.getDependencies();
        List depsMapsList = new ArrayList();
        for (int i = 0; i < depsList.size(); i++) {
            ExternalDependency thisDep = (ExternalDependency) depsList.get(i);
            Map depMap = new HashMap();
            depMap.put(DOWNLOAD_URL, thisDep.getSourceUrl());//URL
            depMap.put(DOWNLOAD_TARGET, thisDep.getTargetPath());//String
            depsMapsList.add(depMap);
        }
        return depsMapsList;
    }

    static boolean trackedMkdirs(File dir, List allDirectories) {
        if (dir.exists()) {
            return false;
        }
        File potentialNewDirRoot = dir.getParentFile();
        File lastPotentialRoot = dir;
        while (!potentialNewDirRoot.exists()) {
            lastPotentialRoot = potentialNewDirRoot;
            potentialNewDirRoot = potentialNewDirRoot.getParentFile();
        }
        //lastPotentialRoot now tells us where directories will be created from
        //Add it to the list, as mkdirs can give partial directory structures, and we want proper
        //cleanup. Just make sure to call exists() before we delete the dirs in rollback.
        allDirectories.add(lastPotentialRoot);
        return dir.mkdirs();
    }

    public static void rollbackExtract(List filesAndDirectories) {
        for (int i = 0; i < filesAndDirectories.size(); i++) {
            File target = (File) filesAndDirectories.get(i);
            if (target.exists()) {
                delete(target, false);
            }
        }
    }

    private static void collectScripts(File[] files, List scripts) {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            File[] childFiles = file.listFiles();
            if (childFiles != null) {
                collectScripts(childFiles, scripts);
            } else {
                // Skip .bat and .jar.
                if (file.getName().indexOf('.') == -1) {
                    scripts.add(file.getPath());
                }
            }
        }
    }

    /**
     * @param ep        - The object that will invoke the chmod commands.
     * @param outputDir - The Liberty runtime install root.
     * @param filter    - A zip file containing files that we want to do the chmod against.
     */
    public static ReturnCode fixScriptPermissions(ExtractProgress ep, File outputDir, ZipFile filter) {
        // We've extracted the files, now apply some permission changes
        if (!isWindows) {
            File[] binFiles = new File(outputDir, "bin").listFiles();

            // If we have a filter, we need to trim down the list of scripts to change the permissions
            // of. For Ifixes it means we only change the permissions of the scripts that are contained in the ifix.
            if (binFiles != null && filter != null) {
                // Filter bin files using passed in filter.
                List filteredBinFiles = new ArrayList();
                for (int i = 0; i < binFiles.length; i++) {
                    File currBinFile = binFiles[i];
                    if (filter.getEntry("bin/" + currBinFile.getName()) != null)
                        filteredBinFiles.add(currBinFile);
                }

                if (filteredBinFiles.size() == 0)
                    binFiles = null;
                else
                    binFiles = (File[]) filteredBinFiles.toArray(new File[filteredBinFiles.size()]);
            }

            if (binFiles != null) {
                List scripts = new ArrayList();
                collectScripts(binFiles, scripts);
                Exception e = SelfExtractUtils.makeExecutable(scripts, ep);
                if (e != null) {
                    return new ReturnCode(ReturnCode.BAD_OUTPUT, "chmodError", e.getMessage());
                }
            }

            File zOutputDir = new File(outputDir, "lib" + File.separator + "native" + File.separator + "zos" + File.separator + "s390x");
            File properties = new File(zOutputDir, "extattr.properties");
            if (properties.exists()) { //then this is z/OS trial jar
                Map attrs = new HashMap();
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(properties), "IBM-1047"));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        String[] props = line.split(" ");
                        attrs.put(props[0], props[1]);
                    }
                    br.close();
                    properties.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //make all executable
                File[] execFiles = zOutputDir.listFiles();
                List scripts = new ArrayList();
                for (int i = 0; i < execFiles.length; i++) {
                    scripts.add(execFiles[i].getPath()); // can't use collectFiles because it excludes libzNativeServices.so for containing '.' in name
                }
                Exception e = SelfExtractUtils.makeExecutable(scripts, ep);
                if (e != null) {
                    return new ReturnCode(ReturnCode.BAD_OUTPUT, "chmodError", e.getMessage());
                }

                //set attrs
                Set attrset = attrs.entrySet();
                Iterator iter = attrset.iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String k = (String) entry.getKey();
                    String v = (String) entry.getValue();
                    List execlist = new ArrayList();
                    execlist.add(getExtAttrsToSet(v));
                    execlist.add(getExtAttrsToUnset(v));
                    execlist.add(zOutputDir.getPath() + File.separator + k);
                    e = setExtAttrs(execlist, ep);
                    if (e != null) {
                        e.printStackTrace();
                    }

                }
            }
        }

        return null;
    }

    /**
     * @param attrs string from properties, e.g. "ap--"
     * @return only the set attrs, in order to run with extattrs, e.g. "+ap"
     */
    private static String getExtAttrsToSet(String attrs) {
        String result = "+";
        if (attrs.contains("a")) {
            result += "a";
        }
        if (attrs.contains("p")) {
            result += "p";
        }
        if (attrs.contains("s")) {
            result += "s";
        }
        if (attrs.contains("l")) {
            result += "l";
        }
        return result;
    }

    /**
     * @param attrs string from properties, e.g. "ap--"
     * @return only the unset attrs, in order to run with extattrs, e.g. "-sl"
     */
    private static String getExtAttrsToUnset(String attrs) {
        String result = "-";
        if (!attrs.contains("a")) {
            result += "a";
        }
        if (!attrs.contains("p")) {
            result += "p";
        }
        if (!attrs.contains("s")) {
            result += "s";
        }
        if (!attrs.contains("l")) {
            result += "l";
        }
        return result;
    }

    static final Exception setExtAttrs(List parameters, ExtractProgress ep) {
        List cmd = new ArrayList();
        cmd.add("extattr");
        cmd.addAll(parameters);

        ep.commandRun(cmd);

        try {
            Process p = Runtime.getRuntime().exec((String[]) cmd.toArray(new String[cmd.size()]));
            Thread stdout = copyAsync(p.getInputStream(), System.out);
            Thread stderr = copyAsync(p.getErrorStream(), System.err);

            stdout.join();
            stderr.join();
            p.waitFor();
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    private static File[] findAllFeatureManifests(File outputDir) {
        final File featureDir = new File(outputDir, "lib" + File.separator + "features");

        if (!featureDir.isDirectory()) {
            return new File[0];
        }

        // Get an Array of all manifest files in the specified directory.
        File[] manifestFiles = (File[]) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                File[] manifestFiles = featureDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        boolean result = false;
                        // Store any files ending in .mf
                        if (pathname.getName().toLowerCase().endsWith(".mf"))
                            result = true;

                        return result;
                    }

                });

                return manifestFiles;
            }
        });

        return manifestFiles;
    }

    private static String[] getSubSystemContentFromManifest(final File manifestFile) throws PrivilegedActionException {
        Manifest manifest = (Manifest) AccessController.doPrivileged(new PrivilegedExceptionAction() {
            @Override
            public Object run() throws FileNotFoundException, IOException {
                FileInputStream fin = null;
                Manifest mf = new Manifest();
                try {
                    fin = new FileInputStream(manifestFile);
                    mf.read(fin);
                } finally {
                    tryToClose(fin);
                }

                return mf;
            }
        });

        Attributes attributes = manifest.getMainAttributes();
        String value = attributes.getValue(SUBSYSTEM_CONTENT);

        return (null != value && !value.isEmpty()) ? value.split(",") : new String[0];
    }

    public static ReturnCode processExecutableDirective(File outputDir) throws Exception {
        File[] manifestfiles = findAllFeatureManifests(outputDir);
        HashSet executableFiles = new HashSet();

        for (int i = 0; i < manifestfiles.length; i++) {
            String[] subSystemContent;
            try {
                subSystemContent = getSubSystemContentFromManifest(manifestfiles[i]);
            } catch (PrivilegedActionException e) {
                // PriviledgedActionException is just a wrapper.  Return the cause of the exception.
                return new ReturnCode(ReturnCode.UNREADABLE, "exception.reading.manifest", new String[] { manifestfiles[i].getAbsolutePath(), e.getCause().getMessage() });

            }

            for (int j = 0; j < subSystemContent.length; j++) {
                String[] values = subSystemContent[j].split(";");
                String location = "";
                boolean executable = false;
                for (int k = 0; k < values.length && (location.isEmpty() || !executable); k++) {
                    String value = values[k].trim();
                    if (value.startsWith(LOCATION_DIRECTIVE)) {
                        // Get the location value.  Add 1 to start and subtract 1 from end to remove quotations.
                        location = value.substring(LOCATION_DIRECTIVE.length() + 1, value.length() - 1);
                    } else if (value.startsWith(EXECUTABLE_DIRECTIVE)) {
                        // Get the executable directive value.  Add 1 to start and subtract 1 from end to remove quotations.
                        executable = "true".equalsIgnoreCase(value.substring(EXECUTABLE_DIRECTIVE.length() + 1,
                                                                             value.length() - 1));
                    }
                }

                if (!location.isEmpty() && executable) {
                    File exeFile = new File(outputDir, location);

                    if (exeFile.exists())
                        executableFiles.add(exeFile.getAbsolutePath());
                }
            }
        }

        return PlatformUtils.setExecutePermissionAccordingToUmask((String[]) executableFiles.toArray(new String[executableFiles.size()]));
    }

    /**
     * Return String array copy of environment. If environment contains
     * WLP_USER_DIR, override by setting to extractDirectory/wlp/usr
     *
     * @param extractDirectory specifies extraction directory for app jar
     * @return string array of environment
     */
    public static String[] runEnv(String extractDirectory) {

        Map<String, String> envmap = System.getenv();
        Iterator<String> iKeys = envmap.keySet().iterator();
        List<String> envList = new ArrayList<String>(envmap.size() + 1);

        boolean javaHomeSet = false;
        while (iKeys.hasNext()) {
            String key = iKeys.next();
            String val = envmap.get(key);
            if (key.equals("WLP_USER_DIR")) {
                envList.add("WLP_USER_DIR=" + extractDirectory + File.separator + "wlp" + File.separator + "usr");
            } else {
                envList.add(key + "=" + val);
            }
            // check if JAVA_HOME is set
            if (key.equals("JAVA_HOME")) {
                javaHomeSet = true;
            }
        }
        if (!javaHomeSet) {
            // JAVA_HOME is not set.  Need to set it to the value of java.home
            String javaHome = System.getProperty("java.home");
            SelfExtract.out("RUN_IN_CHILD_JVM_SET_JAVA_HOME", javaHome);
            envList.add("JAVA_HOME=" + javaHome);
        }
        return envList.toArray(new String[0]);

    }

    /**
     * Check if WLP_JAR_CYGWIN env var set, indicating running under cygwin.
     *
     * @return true if running under cygwin else false
     */
    private static boolean isCygwin() {
        String cygwin = System.getenv("WLP_JAR_CYGWIN");

        if (cygwin != null) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * PlatformType provides constants to denote Unix, Windows, or Cygwin platform/environment.
     */

    public static final int PlatformType_UNIX = 1;
    public static final int PlatformType_WINDOWS = 2;
    public static final int PlatformType_CYGWIN = 3;

    /**
     * Return platform type. Determine this by interrogating properties and environemnt variables
     * as follows:
     * 1) If Java os.name property indicates Windows and the WLP_CYGWIN env var is not set, then it's Windows.
     * 2) If Java os.name property indicates Windows and the WLP_CYGWIN env var *is* set, then it's Cygwin.
     * 3) If Java os.name property indicates other than Windows, then platform type is regarded a Unix.
     *
     * @return int value for unix(1), windows(2), or cygwin(3) according to interrogation
     */
    public static int getPlatform() {
        if (System.getProperty("os.name").startsWith("Win")) {
            if (SelfExtractUtils.isCygwin()) {
                return PlatformType_CYGWIN;
            } else {
                return PlatformType_WINDOWS;
            }
        } else {
            return PlatformType_UNIX;
        }
    }

}
