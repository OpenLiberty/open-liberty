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
        MICROPROFILE, JAKARTA
    }

    private String javaMajorVersion;// = resultInfo.get("java_major_version");
    private String javaVersion;// = resultInfo.get("java_info");
    private String openLibertyVersion;// = resultInfo.get("product_version");
    private Type type;// = resultInfo.get("results_type");
    private String osVersion;// = resultInfo.get("os_name");
    private String specName;// = resultInfo.get("feature_name");
    private String specVersion;// = resultInfo.get("feature_version");
    private String rcVersion;

    public TCKResultsInfo(Type type, String specName, String specVersion, String openLibertyVersion) {
        setType(type);
        setSpecName(specName);
        setSpecVersion(specVersion);
        setJavaVersion(System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ')');
        JavaInfo javaInfo = JavaInfo.forCurrentVM();
        setJavaMajorVersion(String.valueOf(javaInfo.majorVersion()));
        setOsVersion(System.getProperty("os.name"));
        setOpenLibertyVersion(openLibertyVersion);
    }

    /**
     * @return the javaMajorVersion
     */
    public String getJavaMajorVersion() {
        return javaMajorVersion;
    }

    /**
     * @param javaMajorVersion the javaMajorVersion to set
     */
    void setJavaMajorVersion(String javaMajorVersion) {
        this.javaMajorVersion = javaMajorVersion;
    }

    /**
     * @return the javaVersion
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * @param javaVersion the javaVersion to set
     */
    void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    /**
     * @return the openLibertyVersion
     */
    public String getOpenLibertyVersion() {
        return openLibertyVersion;
    }

    /**
     * @param openLibertyVersion the openLibertyVersion to set
     */
    void setOpenLibertyVersion(String openLibertyVersion) {
        this.openLibertyVersion = openLibertyVersion;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    void setType(Type type) {
        this.type = type;
    }

    /**
     * @return the osVersion
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * @param osVersion the osVersion to set
     */
    void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * @return the specName
     */
    public String getSpecName() {
        return specName;
    }

    /**
     * @param specName the specName to set
     */
    void setSpecName(String specName) {
        this.specName = specName;
    }

    /**
     * @return the specVersion
     */
    public String getSpecVersion() {
        return specVersion;
    }

    /**
     * @param specVersion the specVersion to set
     */
    void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    /**
     * @return the rcVersion
     */
    public String getRcVersion() {
        return rcVersion;
    }

    /**
     * @param rcVersion the rcVersion to set
     */
    void setRcVersion(String rcVersion) {
        this.rcVersion = rcVersion;
    }

}
