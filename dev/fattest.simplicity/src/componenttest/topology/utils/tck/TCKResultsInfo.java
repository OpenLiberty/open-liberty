/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package componenttest.topology.utils.tck;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * Metadata about a set of TCK Results
 */
public class TCKResultsInfo {

    private static final Class<TCKResultsInfo> c = TCKResultsInfo.class;

    public static enum Type {
        MICROPROFILE, //A MicroProfile TCK
        JAKARTA //A Jakarta EE TCK
    }

    public static class TCKJarInfo {
        public String group; //the maven group ID
        public String artifact; //the maven artifact ID
        public String version; //the maven version
        public String jarPath; //the local path on disk to the TCK jar
        public String sha256; //a sha256 hash of the TCK jar
        public String sha1; //a sha256 hash of the TCK jar
    }

    // Calculated fields
    private final String javaMajorVersion;// = resultInfo.get("java_major_version");
    private final String javaVersion;// = resultInfo.get("java_info");
    private final String repeat; // = RepeatTestFilter.getRepeatActionsAsString();

    // Required fields
    private final Type type;// = resultInfo.get("results_type");
    private final String specName;// = resultInfo.get("feature_name");
    private final TCKJarInfo tckJarInfo;
    private final LibertyServer server;

    // Optional fields
    private String platformVersion = "";
    private String[] qualifiers = new String[] {};

    public TCKResultsInfo(Type type, String specName, LibertyServer server, TCKJarInfo tckJarInfo) {
        this.type = type;
        this.specName = specName;
        this.server = server;
        this.tckJarInfo = tckJarInfo;

        this.javaVersion = System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ')';
        JavaInfo javaInfo = JavaInfo.forCurrentVM();
        this.javaMajorVersion = String.valueOf(javaInfo.majorVersion());
        this.repeat = RepeatTestFilter.getRepeatActionsAsString();
    }

    public void withQualifiers(String[] qualifiers) {
        this.qualifiers = qualifiers;
    }

    public void withPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    /**
     * @return the javaMajorVersion
     */
    public String getJavaMajorVersion() {
        return javaMajorVersion;
    }

    /**
     * @return the javaVersion
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * @return the openLibertyVersion
     */
    public String getOpenLibertyVersion() {
        return this.server.getOpenLibertyVersion();
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the osName
     */
    public String getOsName() {
        try {
            return server.getMachine().getRawOSName();
        } catch (Exception e) {
            Log.error(c, "getOsName", e);
            return "UNKNOWN";
        }
    }

    /**
     * @return the osVersion
     */
    public String getOsVersion() {
        try {
            return server.getMachine().getOSVersion();
        } catch (Exception e) {
            Log.error(c, "getOsVersion", e);
            return "UNKNOWN";
        }
    }

    /**
     * @return the specName
     */
    public String getSpecName() {
        return specName;
    }

    /**
     * @return the repeatID
     */
    public String getRepeat() {
        return repeat;
    }

    /**
     * @return the specVersion
     */
    public String getSpecVersion() {
        if (this.tckJarInfo != null) {
            return this.tckJarInfo.version;
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * @return
     */
    public String getSHA256() {
        if (this.tckJarInfo != null) {
            return this.tckJarInfo.sha256;
        } else {
            return "UNKNOWN";
        }

    }

    /**
     * @return
     */
    public String getSHA1() {
        if (this.tckJarInfo != null) {
            return this.tckJarInfo.sha1;
        } else {
            return "UNKNOWN";
        }

    }

    /**
     * @return
     */
    public String getPlatformVersion() {
        return this.platformVersion;
    }

    /**
     * @return
     */
    public String[] getQualifiers() {
        return this.qualifiers;
    }

    /**
     * Returns a human readable full specification name such as "Jakarta Data 1.0"
     *
     * @return
     */
    public String getFullSpecName() {
        switch (type) {
            case JAKARTA:
                return "Jakarta " + specName + " " + getSpecVersion();
            case MICROPROFILE:
                return "Microprofile " + specName + " " + getSpecVersion();
            default:
                return "UKNOWN";

        }
    }

    String getSpecNameForURL() {
        return specName.toLowerCase().replace(" ", "-");
    }

    /**
     * Returns url where the specification can be found
     *
     * @return
     */
    public String getSpecURL() {
        switch (type) {
            case JAKARTA:
                return "https://jakarta.ee/specifications/" + getSpecNameForURL() + "/" + getSpecVersion();
            case MICROPROFILE:
                return "https://github.com/eclipse/microprofile-" + getSpecNameForURL() + "/tree/" + getSpecVersion();
            default:
                return "UNKNOWN";

        }
    }

    public String getTCKURL() {
        switch (type) {
            case JAKARTA:
                return "https://download.eclipse.org/ee4j/" + getSpecNameForURL() + "/jakartaee"
                       + platformVersion + "/promoted/eftl/" + getSpecNameForURL() + "-tck-"
                       + getSpecVersion() + ".zip";
            case MICROPROFILE:
                return "https://repo1.maven.org/maven2/org/eclipse/microprofile/" + getSpecNameForURL()
                       + "/microprofile-" + getSpecNameForURL() + "-tck/" + getSpecVersion()
                       + "/microprofile-" + getSpecNameForURL() + "-tck-" + getSpecVersion() + ".jar";
            default:
                return "UNKNOWN";
        }
    }

    public String getFilename() {
        String filename = getOpenLibertyVersion()
                          + "-" + getFullSpecName()
                          + (getQualifiers().length > 0 ? "-" + String.join("-", getQualifiers()) : "")
                          + "-Java" + getJavaMajorVersion()
                          + "-TCKResults.adoc";

        //Sanitize file name to ensure it works on all systems
        filename = filename.replace(" ", "-");
        filename = filename.replace(",", "-");
        filename = filename.replace("_", "-");
        filename = filename.replace(":", "-");
        filename = filename.replace(";", "-");

        return filename;
    }

    public String getReadableRepeatName() {
        String redeableRepeatName = getRepeat();

        if (redeableRepeatName.contains("FeatureReplacementAction")) {
            redeableRepeatName = redeableRepeatName.replaceAll("FeatureReplacementAction.*REMOVE", "remove")
                            .replaceAll("\\[", "")
                            .replaceAll("\\]", "")
                            .replaceAll("ADD", "add")
                            .replaceAll("  ", " ")
                            .replaceAll(",", "-")
                            .replaceAll(" ", "_");
        }

        return redeableRepeatName;
    }

}
