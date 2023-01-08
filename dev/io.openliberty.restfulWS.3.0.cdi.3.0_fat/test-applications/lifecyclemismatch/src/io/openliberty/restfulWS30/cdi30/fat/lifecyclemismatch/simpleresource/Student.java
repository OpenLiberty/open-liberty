/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch.simpleresource;

import java.io.Serializable;

import jakarta.enterprise.inject.Default;

@Default
public class Student implements Person, Serializable{

	private static final long serialVersionUID = -423420029320020776L;

	@Override
	public String talk() {
		return "I'm a Student.";
	}
}
