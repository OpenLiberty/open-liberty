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
package com.ibm.ws.wlp.mavenFeatures.model;

public class MavenCoordinates {
    
    private final String groupId;
    private final String artifactId;
    private final String version;
    
    public MavenCoordinates(String groupId, String artifactId, String version) {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
    
    /**
     * Define Maven coordinates from String format
     * @param mavenCoordinates Maven coordinates in the format of groupId:artifactId:version
     * @throws IllegalArgumentException If the coordinates format is invalid
     */
    public MavenCoordinates(String mavenCoordinates) throws IllegalArgumentException {
        super();
        String[] tokens = mavenCoordinates.split(":");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("The string " + mavenCoordinates
                    + " is not a valid Maven coordinates string. Expected format is groupId:artifactId:version");
        }
        groupId = tokens[0];
        artifactId = tokens[1];
        version = tokens[2];
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }
    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

}
