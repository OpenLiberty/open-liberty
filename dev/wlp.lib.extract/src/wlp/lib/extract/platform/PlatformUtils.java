/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wlp.lib.extract.ReturnCode;
import wlp.lib.extract.SelfExtract;

/**
 *
 */
public class PlatformUtils {
    public static final int UMASK_NOT_APPLICABLE = 0x0400;
    public static final Logger platformLogger = Logger.getLogger("wlp.lib.extract.platform");

    private static final String[] EXTENDED_ATTRIBUTES = new String[] { "a", "l", "p", "s" };
    private static final String EXTENDED_ATTRIBUTES_ADD = "+";
    private static final String EXTENDED_ATTRIBUTES_REMOVE = "-";

    public static int executeCommand(String[] cmd, String[] env, File workingDir, Writer out, Writer err) throws IOException {
        Process p = Runtime.getRuntime().exec(cmd, env, workingDir);

        Thread outThread = copyOutputThread(p.getInputStream(), out);
        Thread errThread = copyOutputThread(p.getErrorStream(), err);

        int returnValue = -1;

        try {
            returnValue = p.waitFor();
            outThread.join();
            errThread.join();
        } catch (InterruptedException e) {
            LogRecord lg = new LogRecord(Level.FINE, e.getMessage());
            lg.setThrown(e);
            platformLogger.log(lg);
        }

        return returnValue;
    }

    private static Thread copyOutputThread(final InputStream in, Writer out) {
        final Writer writer = (null == out) ? NULL_WRITER : out;

        Thread thread = new Thread() {
            public void run() {
                char[] buf = new char[4096];
                BufferedReader bReader = null;

                try {
                    bReader = new BufferedReader(new InputStreamReader(in));
                    for (int read; (read = bReader.read(buf)) != -1;) {
                        writer.write(buf, 0, read);
                    }
                } catch (IOException ioe) {
                    LogRecord lg = new LogRecord(Level.FINE, ioe.getMessage());
                    lg.setThrown(ioe);
                    platformLogger.log(lg);
                } finally {
                    if (null != bReader) {
                        try {
                            bReader.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }

        };

        thread.start();
        return thread;
    }

    /**
     * A writer that does nothing
     */
    private static final Writer NULL_WRITER = new Writer() {

        public void write(char[] cbuf, int off, int len) throws IOException {}

        public void flush() throws IOException {}

        public void close() throws IOException {}

    };

    public static ReturnCode setExecutePermissionAccordingToUmask(String[] files) throws IOException {
        if (null == files || files.length == 0) {
            return ReturnCode.OK;
        }

        int umask = getUmask();

        if (umask == UMASK_NOT_APPLICABLE) {
            return ReturnCode.OK;
        }

        String perm = "";

        if ((umask & 0100) == 0) {
            perm = "u";
        }
        if ((umask & 0010) == 0) {
            perm += "g";
        }
        if ((umask & 0001) == 0) {
            perm += "o";
        }

        if (!perm.isEmpty()) {
            perm += "+x";
        }

        return chmod(files, perm);
    }

    public static ReturnCode chmod(String[] fileList, String perm) throws IOException {
        return PPPlatformUtils.getPolicy().chmod(fileList, perm);
    }

    public static ReturnCode setExtendedAttributes(Map extattrFilesMap) throws IOException {
        Map extAttrFilesMap = new HashMap();

        if (extattrFilesMap.size() > EXTENDED_ATTRIBUTES.length) {
            for (Iterator entryIt = extattrFilesMap.entrySet().iterator(); entryIt.hasNext();) {
                Map.Entry entry = (Map.Entry) entryIt.next();
                String attrs = (String) entry.getKey();
                Set files = (Set) entry.getValue();

                for (int i = 0; i < EXTENDED_ATTRIBUTES.length; i++) {
                    if (attrs.contains(EXTENDED_ATTRIBUTES[i])) {
                        String key = EXTENDED_ATTRIBUTES_ADD + EXTENDED_ATTRIBUTES[i];
                        Set attrfiles = ((extAttrFilesMap.containsKey(key)) ? (Set) extAttrFilesMap.get(key) : new HashSet());

                        attrfiles.addAll(files);
                        extAttrFilesMap.put(key, attrfiles);
                    } else {
                        String key = EXTENDED_ATTRIBUTES_REMOVE + EXTENDED_ATTRIBUTES[i];
                        Set attrfiles = (extAttrFilesMap.containsKey(key)) ? (Set) extAttrFilesMap.get(key) : new HashSet();

                        attrfiles.addAll(files);
                        extAttrFilesMap.put(key, attrfiles);
                    }
                }
            }
        } else {
            for (Iterator entryIt = extattrFilesMap.entrySet().iterator(); entryIt.hasNext();) {
                Map.Entry entry = (Map.Entry) entryIt.next();
                String attrs = (String) entry.getKey();
                Set files = (Set) entry.getValue();
                StringBuffer antiAttrsBuffer = new StringBuffer();

                for (int i = 0; i < EXTENDED_ATTRIBUTES.length; i++) {
                    if (!attrs.contains(EXTENDED_ATTRIBUTES[i])) {
                        antiAttrsBuffer.append(EXTENDED_ATTRIBUTES[i]);
                    }
                }
                String antiAttrs = antiAttrsBuffer.toString();
                if (!"".equals(antiAttrs) && !"".equals(attrs)) {
                    attrs = EXTENDED_ATTRIBUTES_ADD + attrs + " " + EXTENDED_ATTRIBUTES_REMOVE + antiAttrs;
                } else if ("".equals(antiAttrs)) {
                    attrs = EXTENDED_ATTRIBUTES_ADD + attrs;
                } else {
                    attrs = EXTENDED_ATTRIBUTES_REMOVE + antiAttrs;
                }
                extAttrFilesMap.put(attrs, files);
            }
        }

        ReturnCode rc = ReturnCode.OK;

        for (Iterator entryIt = extAttrFilesMap.entrySet().iterator(); entryIt.hasNext();) {
            Map.Entry entry = (Map.Entry) entryIt.next();
            String attr = (String) entry.getKey();
            Set files = (Set) entry.getValue();
            rc = extattr((String[]) files.toArray(new String[files.size()]), attr);

            if (!rc.equals(ReturnCode.OK)) {
                break;
            }
        }

        return rc;
    }

    public static ReturnCode extattr(String[] fileList, String attr) throws IOException {
        return PPPlatformUtils.getPolicy().extattr(fileList, attr);
    }

    public static int getUmask() throws IOException {
        return PPPlatformUtils.getPolicy().getUmask();
    }

    public static String getASCIISystemCharSet() {
        return PPPlatformUtils.getPolicy().getASCIISystemCharSet();
    }

    public static String getEBCIDICSystemCharSet() {
        return PPPlatformUtils.getPolicy().getEBCIDICSystemCharSet();
    }

} // =============================================================================

class PPPlatformUtils extends AbstractPlatformPolicyFactory {
    static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(SelfExtract.class.getName() + "Messages");
    private static final PPPlatformUtils INSTANCE = new PPPlatformUtils();

    protected Object createLinuxPolicy() {
        return new PPLinux();
    }

    protected Object createSolarisPolicy() {
        return new PPSolaris();
    }

    protected Object createWindowsPolicy() {
        return new PPWindows();
    }

    protected Object createZOSPolicy() {
        return new PPZOS();
    }

    protected Object createOS400Policy() {
        return new PPOS400();
    }

    protected Object createMACOSPolicy() {
        return new PPMACOS();
    }

    static PPCommon getPolicy() {
        return (PPCommon) INSTANCE.getPlatformPolicy();
    }

    abstract class PPCommon {
        static final String DEFAULT_ASCII_CODESET = "ASCII";

        abstract ReturnCode chmod(String[] fileList, String perm) throws IOException;

        abstract ReturnCode extattr(String[] fileList, String attr) throws IOException;

        abstract int getUmask() throws IOException;

        ReturnCode runCommand(String cmExe, String[] fileList, String[] cmdArgs) throws IOException {
            List filePaths = new ArrayList();
            for (int i = 0; i < fileList.length; i++) {
                File file = new File(fileList[i]);

                if (file.exists()) {
                    filePaths.add(new File(fileList[i]).getAbsolutePath());
                }
            }

            if (filePaths.isEmpty()) {
                return ReturnCode.OK;
            }

            List cmdList = new ArrayList();
            cmdList.add(cmExe);
            if (cmdArgs.length > 0) {
                cmdList.addAll(Arrays.asList(cmdArgs));
            }
            cmdList.addAll(filePaths);

            int returnStatus = 0;
            Writer out = new StringWriter();
            Writer err = new StringWriter();

            returnStatus = PlatformUtils.executeCommand((String[]) cmdList.toArray(
                                                                                   new String[cmdList.size()]),
                                                        null, null, out, err);

            if (returnStatus != 0) {
                return new ReturnCode(ReturnCode.BAD_OUTPUT, "ERROR_EXECUTING_COMMAND", new String[] { convertListToString(cmdList, " "),
                                                                                                       Integer.toString(returnStatus), err.toString() });
            }

            return ReturnCode.OK;
        }

        abstract String getASCIISystemCharSet();

        abstract String getEBCIDICSystemCharSet();

        protected String convertListToString(List list, String delimiter) {
            StringBuilder sb = new StringBuilder();

            if (null == delimiter) {
                delimiter = ",";
            }

            if (null != list && !list.isEmpty()) {
                Iterator it = list.iterator();
                sb.append((String) it.next());

                while (it.hasNext()) {
                    sb.append(delimiter).append((String) it.next());
                }
            }

            return sb.toString();
        }

    } // =============================================================================

    class PPWindows extends PPCommon {

        ReturnCode chmod(String[] fileList, String perm) {
            // There is no chmod on Windows
            return ReturnCode.OK;
        }

        int getUmask() {
            // There is no umask on Windows
            return PlatformUtils.UMASK_NOT_APPLICABLE;
        }

        ReturnCode extattr(String[] fileList, String attr) {
            // There is no extattr on Windows
            return ReturnCode.OK;
        }

        String getASCIISystemCharSet() {
            return DEFAULT_ASCII_CODESET;
        }

        String getEBCIDICSystemCharSet() {
            return null;
        }

    } // =============================================================================

    class PPLinux extends PPCommon {
        private final String[] chmod_locations = new String[] { "/bin/chmod", "/usr/bin/chmod" };

        int getUmask() throws IOException {
            String shellCommand = getCmdLocation(new String[] { "/bin/bash", "/bin/sh" });

            if ("".equals(shellCommand)) {
                throw new IOException(MessageFormat.format(RESOURCE_BUNDLE.getString(
                                                                                     "ERROR_UNABLE_TO_LOCATE_COMMAND_EXE"),
                                                           new String[] { "sh", "/bin/sh" }));
            }

            List umaskCmdList = new ArrayList();
            umaskCmdList.add(shellCommand);
            umaskCmdList.add("-c");
            umaskCmdList.add("umask");

            int returnStatus = 0;
            Writer err = new StringWriter();
            Writer out = new StringWriter();

            returnStatus = PlatformUtils.executeCommand((String[]) umaskCmdList.toArray(
                                                                                        new String[umaskCmdList.size()]),
                                                        null, null, out, err);

            if (returnStatus != 0) {
                throw new IOException(MessageFormat.format(RESOURCE_BUNDLE.getString("ERROR_EXECUTING_COMMAND"),
                                                           new String[] { convertListToString(umaskCmdList, " "),
                                                                          Integer.toString(returnStatus), err.toString() }));
            } else if (out.toString().length() == 0) {
                throw new IOException(MessageFormat.format(RESOURCE_BUNDLE.getString("ERROR_UNABLE_TO_GET_UMASK"),
                                                           new String[] { convertListToString(umaskCmdList, " ") }));
            }

            String umask = out.toString().trim();

            try {
                // If octal fails, then the umask is in symbolic format
                return Integer.parseInt(umask, 8);
            } catch (NumberFormatException e) {
                // The umask is not in octal format
                return parseSymbolicUmask(umask);
            }
        }

        private int parseSymbolicUmask(String umask) {
            int octalUmask = 0777;
            umask.toLowerCase(Locale.ENGLISH);
            String[] perms = umask.split(",");

            for (int i = 0; i < perms.length; i++) {
                switch (perms[i].charAt(0)) {
                    case 'u': // User permissions
                        if (perms[i].contains("r")) {
                            octalUmask -= 0400;
                        }
                        if (perms[i].contains("w")) {
                            octalUmask -= 0200;
                        }
                        if (perms[i].contains("x")) {
                            octalUmask -= 0100;
                        }
                        break;
                    case 'g': // Group permissions
                        if (perms[i].contains("r")) {
                            octalUmask -= 0040;
                        }
                        if (perms[i].contains("w")) {
                            octalUmask -= 0020;
                        }
                        if (perms[i].contains("x")) {
                            octalUmask -= 0010;
                        }
                        break;
                    case 'o': // Other (world) permissions
                        if (perms[i].contains("r")) {
                            octalUmask -= 0004;
                        }
                        if (perms[i].contains("w")) {
                            octalUmask -= 0002;
                        }
                        if (perms[i].contains("x")) {
                            octalUmask -= 0001;
                        }
                        break;
                    default:
                        break;

                }
            }

            return octalUmask;
        }

        ReturnCode chmod(String[] fileList, String perm) throws IOException {
            String chmodCmd = getCmdLocation(chmod_locations); // TODO: Check if location is found
            if (null == perm || "".equals(perm)) {
                return ReturnCode.OK;
            } else if ("".equals(chmodCmd)) {
                return new ReturnCode(ReturnCode.NOT_FOUND, "ERROR_UNABLE_TO_LOCATE_COMMAND_EXE", new String[] { "chmod",
                                                                                                                 convertListToString(Arrays.asList(chmod_locations), ", ") });
            }

            return runCommand(chmodCmd, fileList, new String[] { perm });
        }

        String getCmdLocation(String[] cmdLocations) {
            String cmdLocation = null;

            for (int i = 0; i < cmdLocations.length; i++) {
                if (new File(cmdLocations[i]).exists()) {
                    cmdLocation = cmdLocations[i];
                    break;
                }
            }

            return cmdLocation;
        }

        ReturnCode extattr(String[] fileList, String attr) throws IOException {
            // There is no extattr on Linux
            return ReturnCode.OK;
        }

        String getASCIISystemCharSet() {
            return DEFAULT_ASCII_CODESET;
        }

        String getEBCIDICSystemCharSet() {
            return null;
        }

    } // =============================================================================

    class PPSolaris extends PPLinux {

    } // =============================================================================

    class PPAix extends PPLinux {

    } // =============================================================================

    class PPHpux extends PPLinux {

    } // =============================================================================

    class PPZOS extends PPLinux {
        static final String DEFAULT_EBCDIC_CODESET = "IBM-1047";
        private final String[] extattr_locations = new String[] { "/bin/extattr", "/usr/bin/extattr" };
        private final String[] chcp_locations = new String[] { "/bin/chcp", "/usr/bin/chcp" };

        private String systemASCII = null;
        private String systemEBCDIC = null;

        ReturnCode extattr(String[] fileList, String attr) throws IOException {
            if (null == attr || "".equals(attr)) {
                return ReturnCode.OK;
            } else if (!validateExtAttrArgs(attr)) {
                return new ReturnCode(ReturnCode.BAD_INPUT, "ERROR_INVALID_EXTATTR_PARMS", new String[] { attr });
            }

            String extattrCmd = getCmdLocation(extattr_locations);

            if ("".equals(extattrCmd)) {
                return new ReturnCode(ReturnCode.NOT_FOUND, "ERROR_UNABLE_TO_LOCATE_COMMAND_EXE", new String[] { "extattr",
                                                                                                                 convertListToString(Arrays.asList(extattr_locations), ", ") });

            }

            String[] attrArgs = attr.split(" ");

            return runCommand(extattrCmd, fileList, attrArgs);

        }

        boolean validateExtAttrArgs(String attr) {
            boolean valid = !(attr.length() > 7 || attr.length() < 2
                              || attr.indexOf("a") != attr.lastIndexOf("a")
                              || attr.indexOf("p") != attr.lastIndexOf("p")
                              || attr.indexOf("s") != attr.lastIndexOf("s")
                              || attr.indexOf("l") != attr.lastIndexOf("l"));

            return valid && attr.matches("^[+-][alps]{1,4}([\\s][+-][alps]{1,4})?$");
        }

        private ReturnCode getSystemASCIIandEBCDICvalues() throws IOException {
            String chcpCmd = getCmdLocation(chcp_locations);

            if ("".equals(chcpCmd)) {
                return new ReturnCode(ReturnCode.NOT_FOUND, "ERROR_UNABLE_TO_LOCATE_COMMAND_EXE", new String[] { "chcp",
                                                                                                                 convertListToString(Arrays.asList(chcp_locations), ", ") });
            }

            List chcpCmdList = new ArrayList();
            chcpCmdList.add(chcpCmd);
            chcpCmdList.add("-q");

            int returnStatus = 0;
            Writer err = new StringWriter();
            Writer out = new StringWriter();

            returnStatus = PlatformUtils.executeCommand((String[]) chcpCmdList.toArray(
                                                                                       new String[chcpCmdList.size()]),
                                                        null, null, out, err);

            if (returnStatus != 0) {
                return new ReturnCode(ReturnCode.BAD_OUTPUT, "ERROR_EXECUTING_COMMAND", new String[] { convertListToString(chcpCmdList, " "),
                                                                                                       Integer.toString(returnStatus), err.toString() });
            } else if (out.toString().length() > 0) {
                String ascii_regex = "^.*ASCII.*:\\s*(.*)$";
                String ebcdic_regex = "^.*EBCDIC.*:\\s*(.*)$";

                Pattern pa = Pattern.compile(ascii_regex);
                Pattern pe = Pattern.compile(ebcdic_regex);

                Scanner chcpOutput = null;

                try {
                    chcpOutput = new Scanner(out.toString());

                    while (chcpOutput.hasNext()) {
                        String line = chcpOutput.nextLine();

                        Matcher ma = pa.matcher(line);
                        Matcher me = pe.matcher(line);

                        if (ma.matches()) {
                            systemASCII = ma.group(1);
                        } else if (me.matches()) {
                            systemEBCDIC = me.group(1);
                        }
                    }
                } finally {
                    if (null != chcpOutput)
                        chcpOutput.close();
                }
            }

            return ReturnCode.OK;
        }

        private void setASCIIandEBCDICvalues() {
            String termPath = System.getenv("_BPX_TERMPATH");
            if (!"OMVS".equalsIgnoreCase(termPath)) {
                try {
                    // Try and get the system code pages for ASCII and EBCDIC.
                    getSystemASCIIandEBCDICvalues();
                } catch (Exception e) {
                    // Ignore exceptions and use default values
                }
            }

            // If unable to determine system ASCII code page then use default code page
            if (null == systemASCII) {
                systemASCII = DEFAULT_ASCII_CODESET;
            }

            // If unable to determine system EBCDIC code page then use default code page
            if (null == systemEBCDIC) {
                systemEBCDIC = DEFAULT_EBCDIC_CODESET;
            }
        }

        String getASCIISystemCharSet() {
            if (null == systemASCII) {
                setASCIIandEBCDICvalues();
            }

            return systemASCII;
        }

        String getEBCIDICSystemCharSet() {
            if (null == systemEBCDIC) {
                setASCIIandEBCDICvalues();
            }

            return systemEBCDIC;
        }

    } // =============================================================================

    class PPOS400 extends PPLinux {

    } // =============================================================================

    class PPMACOS extends PPLinux {

    } // =============================================================================

}
