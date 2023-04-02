/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.wlp.mavenFeatures.model;

/**
 * Exception that occurred when generating a Maven repository from Liberty features
 */
public class MavenRepoGeneratorException extends Exception {

	private static final long serialVersionUID = 1288388481240271518L;

	public MavenRepoGeneratorException(String string, Exception e) {
		super(string, e);
	}

	public MavenRepoGeneratorException(String string) {
		super(string);
	}
	
}
