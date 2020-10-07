/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.check_dhe;

public class GAVEntry {
    private final String groupName;
    private final String artifactName;
    private final String versionName;

    private final String fileType;

    /**
     * @param groupName
     * @param artifactName
     * @param versionName
     */
    public GAVEntry(String groupName, String artifactName, String versionName) {
        super();
        this.groupName = groupName;
        this.artifactName = artifactName;

        if (versionName.startsWith("war:")) {
            this.fileType = "war";
            this.versionName = versionName.substring(4);
        } else {
            this.fileType = "jar";
            this.versionName = versionName;
        }
    }

    /**
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @return the artifactName
     */
    public String getArtifactName() {
        return artifactName;
    }

    /**
     * @return the versionName
     */
    public String getVersionName() {
        return versionName;
    }

    public String getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return "GAVEntry [groupName=" + groupName + ", artifactName=" + artifactName + ", versionName=" + versionName + ", fileType=" + fileType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactName == null) ? 0 : artifactName.hashCode());
        result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
        result = prime * result + ((versionName == null) ? 0 : versionName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GAVEntry other = (GAVEntry) obj;
        if (artifactName == null) {
            if (other.artifactName != null)
                return false;
        } else if (!artifactName.equals(other.artifactName))
            return false;
        if (groupName == null) {
            if (other.groupName != null)
                return false;
        } else if (!groupName.equals(other.groupName))
            return false;
        if (versionName == null) {
            if (other.versionName != null)
                return false;
        } else if (!versionName.equals(other.versionName))
            return false;
        return true;
    }

    public String getBaseURL(String hostname) {
        StringBuilder sb = new StringBuilder();
        sb.append(hostname).append("/");

        String gPath = getGroupName().replace('.', '/');
        String aPath = getArtifactName();
        String vPath = getVersionName();

        sb.append(gPath).append("/").append(aPath).append("/").append(vPath);

        return sb.toString();
    }

    public String getJarURL(String hostname) {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseURL(hostname));
        sb.append("/");
        sb.append(getArtifactName());
        sb.append("-");
        sb.append(getVersionName());
        sb.append(".");
        sb.append(getFileType());

        return sb.toString();
    }

    public String getPomURL(String hostname) {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseURL(hostname));
        sb.append("/");
        sb.append(getArtifactName());
        sb.append("-");
        sb.append(getVersionName());
        sb.append(".pom");

        return sb.toString();
    }

}
