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
package com.ibm.ws.jsp.taglib.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GlobalTagLibConfig {
    private String jarName = null;
    private URL jarURL = null;
    private ClassLoader classloader = null;
    private List tldPathList = null;
    
    public GlobalTagLibConfig() {
        tldPathList = new ArrayList();
    }
    
    public String getJarName() {
        return jarName;
    }

    public List getTldPathList() {
        return tldPathList;
    }
    
    public void setJarName(String string) {
        jarName = string;
    }

	public void setJarURL(URL jarURL) {
		this.jarURL = jarURL;
	}
    
	public URL getJarURL() {
		return jarURL;
	}
    
	public ClassLoader getClassloader() {
		return classloader;
	}
    
	public void setClassloader(ClassLoader classloader) {
		this.classloader = classloader;
	}
}
