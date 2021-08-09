/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

import java.io.*;

public class Range {
	public int low;
	public int high;

	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("range low: "+low+ " high: "+high);
		return sw.toString();
	}

	public String fancyFormat(int level) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (int i = level;i>0;i--) pw.print("\t");
		pw.println("range low: "+low+ " high: "+high);
		return sw.toString();
	}
	
	public Object clone() {
		Range c =  new Range();
		c.low = low;
		c.high = high;
		return c;
	}
}
