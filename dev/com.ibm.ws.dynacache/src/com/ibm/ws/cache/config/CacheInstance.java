/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

import java.io.PrintWriter;
import java.io.StringWriter;


public class CacheInstance {

	public String name;
	public ConfigEntry configEntries[];

	//Object array used for storing processor specific data
	//typically property data that has been parsed
	//format and size are determined by the processor
	public Object processorData[] = null;

	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("[CacheInstance]");
		pw.println("name          : " + name);

		for (int i = 0; configEntries != null && i < configEntries.length; i++) {
			pw.println("[CacheEntry " + i + "]");
			pw.println(configEntries[i]);
		}
		return sw.toString();
	}

	//produces nice ascii text
	public String fancyFormat() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("[" + name + "]");

		for (int i = 0; configEntries != null && i < configEntries.length; i++) {
			pw.println("[CacheEntry " + i + "]");
			pw.println(configEntries[i].fancyFormat());
		}
		return sw.toString();
	}

	public Object clone() {
		CacheInstance ci = new CacheInstance();

		ci.name = name;

		if (configEntries != null) {
			ci.configEntries = new ConfigEntry[configEntries.length];
			for (int i = 0; i < configEntries.length; i++) {
				ci.configEntries[i] = (ConfigEntry) configEntries[i].clone();
			}
		}

		return ci;
	}
}
