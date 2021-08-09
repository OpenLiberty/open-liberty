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
package com.ibm.ws.wlp.mavenFeatures.utils;

import com.ibm.ws.wlp.mavenFeatures.model.MavenCoordinates;

public class Utils {

	/**
	 * Gets the expected path within a Maven repository for the given Maven
	 * artifact, based on its Maven coordinates.
	 * 
	 * @param artifact
	 *            the MavenArtifact whose path you want
	 * @return the path within a Maven repository where the artifact should be
	 *         stored
	 */
	public static String getRepositorySubpath(MavenCoordinates artifact) {
		StringBuffer buf = new StringBuffer();
		String[] groupDirs = artifact.getGroupId().split("\\.");
		for (String dir : groupDirs) {
			buf.append(dir).append("/");
		}
		buf.append(artifact.getArtifactId()).append("/").append(artifact.getVersion());
		return buf.toString();
	}

	/**
	 * Gets the expected file name for a Maven artifact, based on its Maven
	 * coordinates.
	 * 
	 * @param artifact
	 *            the MavenArtifact whose file name you want
	 * @param type
	 *            the type of artifact
	 * @return the file name including extension
	 */
	public static String getFileName(MavenCoordinates artifact, Constants.ArtifactType type) {
		return artifact.getArtifactId() + "-" + artifact.getVersion() + type.getMavenFileExtension();
	}
	

}
