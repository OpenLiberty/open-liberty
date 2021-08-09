/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.componentrenderer.jsf943;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(name="jsf943")
@SessionScoped

public class TestBean implements Serializable{
	
	private Boolean exists= false;
	
	public Boolean getClassExists() {
		try {
			Class<?> act = Class.forName("javax.faces.view.ViewDeclarationLanguageWrapper");
			this.exists = true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			this.exists = false;
			e.printStackTrace();
		} 
		return exists;
	}
	
}