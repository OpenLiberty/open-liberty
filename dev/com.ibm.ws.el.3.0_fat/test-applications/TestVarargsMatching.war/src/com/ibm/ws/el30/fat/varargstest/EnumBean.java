/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el30.fat.varargstest;

public enum EnumBean implements IEnum {

    ENUM1("ENUM1");

	private String message;

	EnumBean(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}
