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
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch.simpleresource;

import java.io.Serializable;

import javax.enterprise.inject.Default;

@Default
public class Student implements Person, Serializable{

	private static final long serialVersionUID = -423420029320020776L;

	@Override
	public String talk() {
		return "I'm a Studnet.";
	}
}
