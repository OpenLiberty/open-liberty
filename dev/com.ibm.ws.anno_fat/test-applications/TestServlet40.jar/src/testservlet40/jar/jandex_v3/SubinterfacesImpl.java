/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package testservlet40.jar.jandex_v3;

//This class tests both subinterfaces and ensures we have an example of a class without a no-args constructor
public class SubinterfacesImpl implements SubInterface {
	
	String msg;
	
	public SubinterfacesImpl(String m) {
		msg = m;
	}
	
	public String getMsg() {
		return msg;
	}
	
	public String getNote( ) {
		return "note";
	}	
}

