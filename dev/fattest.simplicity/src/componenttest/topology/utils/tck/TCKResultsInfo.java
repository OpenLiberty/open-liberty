/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils.tck;

import componenttest.topology.impl.JavaInfo;

/**
 * Metadata about a set of TCK Results
 */
public class TCKResultsInfo {

    public static enum Type {
        MICROPROFILE, //A MicroProfile TCK
        JAKARTA //A Jakarta EE TCK
    }

    static class TCKJarInfo {
        String group; //the maven group ID
        String artifact; //the maven artifact ID
        String version; //the maven version
        String jarPath; //the local path on disk to the TCK jar
        String sha; //a sha256 hash of the TCK jar
    }

    private final String javaMajorVersion;// = resultInfo.get("java_major_version");
    private final String javaVersion;// = resultInfo.get("java_info");
    private final String openLibertyVersion;// = resultInfo.get("product_version");
    private final Type type;// = resultInfo.get("results_type");
    private final String osVersion;// = resultInfo.get("os_name");
    private final String specName;// = resultInfo.get("feature_name");
    private TCKJarInfo tckJarInfo;

    public TCKResultsInfo(Type type, String specName, String openLibertyVersion, TCKJarInfo tckJarInfo) {
        this.type = type;
        this.specName = specName;
        this.openLibertyVersion = openLibertyVersion;
        this.javaVersion = System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ')';
        JavaInfo javaInfo = JavaInfo.forCurrentVM();
        this.javaMajorVersion = String.valueOf(javaInfo.majorVersion());
        this.osVersion = System.getProperty("os.name");
        this.tckJarInfo = tckJarInfo;
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
        return openLibertyVersion;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the osVersion
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * @return the specName
     */
    public String getSpecName() {
        return specName;
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
    public String getSHA() {
        if (this.tckJarInfo != null) {
            return this.tckJarInfo.sha;
        } else {
            return "UNKNOWN";
        }

    }

}
