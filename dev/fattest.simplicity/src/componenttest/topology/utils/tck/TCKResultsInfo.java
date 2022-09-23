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

/**
 *
 */
public class TCKResultsInfo {

    private String javaMajorVersion;// = resultInfo.get("java_major_version");
    private String javaVersion;// = resultInfo.get("java_info");
    private String openLibertyVersion;// = resultInfo.get("product_version");
    private String type;// = resultInfo.get("results_type");
    private String osVersion;// = resultInfo.get("os_name");
    private String specName;// = resultInfo.get("feature_name");
    private String specVersion;// = resultInfo.get("feature_version");

    /**
     * @return the javaMajorVersion
     */
    public String getJavaMajorVersion() {
        return javaMajorVersion;
    }

    /**
     * @param javaMajorVersion the javaMajorVersion to set
     */
    public void setJavaMajorVersion(String javaMajorVersion) {
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
    public void setJavaVersion(String javaVersion) {
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
    public void setOpenLibertyVersion(String openLibertyVersion) {
        this.openLibertyVersion = openLibertyVersion;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
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
    public void setOsVersion(String osVersion) {
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
    public void setSpecName(String specName) {
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
    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

}
