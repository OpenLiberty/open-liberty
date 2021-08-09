/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

/**
 * A set of files on the file system local to the server configuration. Note
 * that these files may be remote to the local JVM.
 *
 */
public class Fileset extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_CASE_SENSITIVE = "caseSensitive";
    private String caseSensitive;

    public final static String XML_ATTRIBUTE_NAME_DIR = "dir";
    private String dir;

    public final static String XML_ATTRIBUTE_NAME_EXCLUDES = "excludes";
    private String excludes;

    public final static String XML_ATTRIBUTE_NAME_INCLUDES = "includes";
    private String includes;

    public final static String XML_ATTRIBUTE_NAME_SCAN_INTERVAL = "scanInterval";
    private String scanInterval;

    /**
     * @return value of the caseSensitive attribute
     */
    public String getCaseSensitive() {
        return this.caseSensitive;
    }

    /**
     * @param caseSensitive value to use for the caseSensitive attribute
     */
    public void setCaseSensitive(String caseSensitive) {
        this.caseSensitive = ConfigElement.getValue(caseSensitive);
    }

    /**
     * @return the parent directory represented by this instance
     */
    public String getDir() {
        return this.dir;
    }

    /**
     * @param dir the parent directory represented by this instance
     */
    public void setDir(String dir) {
        this.dir = ConfigElement.getValue(dir);
    }

    /**
     * @return the pattern to exclude from the target directory
     */
    public String getExcludes() {
        return this.excludes;
    }

    /**
     * @param excludes the pattern to exclude from the target directory
     */
    public void setExcludes(String excludes) {
        this.excludes = ConfigElement.getValue(excludes);
    }

    /**
     * @return the pattern to include from the target directory
     */
    public String getIncludes() {
        return this.includes;
    }

    /**
     * @param includes the pattern to include from the target directory
     */
    public void setIncludes(String includes) {
        this.includes = ConfigElement.getValue(includes);
    }

    /**
     * @return the scan interval
     */
    public String getScanInterval() {
        return this.scanInterval;
    }

    /**
     * @param scanInterval the scan interval
     */
    public void setScanInterval(String scanInterval) {
        this.scanInterval = ConfigElement.getValue(scanInterval);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Fileset{");
        if (caseSensitive != null)
            buf.append("caseSensitive=\"" + caseSensitive + "\" ");
        if (dir != null)
            buf.append("dir=\"" + dir + "\" ");
        if (excludes != null)
            buf.append("excludes=\"" + excludes + "\" ");
        if (includes != null)
            buf.append("includes=\"" + includes + "\" ");
        if (scanInterval != null)
            buf.append("scanInterval=\"" + scanInterval + "\" ");
        buf.append("}");
        return buf.toString();
    }
}
