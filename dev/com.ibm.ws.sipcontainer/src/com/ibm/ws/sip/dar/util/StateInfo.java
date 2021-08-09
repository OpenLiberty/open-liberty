/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.dar.util;

import java.io.Serializable;

/**
 * The class represents state of the application router
 * 
 * @author Roman Mandeleil
 */
public class StateInfo implements Serializable{


	public static long getSerialVersionUID() {
		return serialVersionUID;
	}



	public int getIndex() {
		return lastIndex;
	}



	public void setIndex(int lastIndex) {
		this.lastIndex = lastIndex;
	}
	
	public void increaseLastIndex(){
		++this.lastIndex;
	}


	
	private static final long serialVersionUID = -4353010448828770451L;
	private int lastIndex = 0;
}
