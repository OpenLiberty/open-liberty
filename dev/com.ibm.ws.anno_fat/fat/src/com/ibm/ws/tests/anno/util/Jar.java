/*******************************************************************************
 * Copyright (c) 2012,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tests.anno.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Logical representation of a jar.  Has a name and a list of
 * package names of contained classes.
 */
public class Jar {

	public Jar(String name) {
		this.name = name;
		this.packageNames = new ArrayList<String>();
	}

	//

	private final String name;

	public String getName() {
		return name;
	}

	//

	private final List<String> packageNames;

	public List<String> getPackageNames() {
		return packageNames;
	}

	public void addPackageName(String packageName) {
		packageNames.add(packageName);
	}
}
