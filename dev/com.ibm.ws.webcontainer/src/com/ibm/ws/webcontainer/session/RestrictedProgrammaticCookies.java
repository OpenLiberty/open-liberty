/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.session;

public class RestrictedProgrammaticCookies {

    String name=null;
    String domain=null;
    String path=null;
    
    public RestrictedProgrammaticCookies(String n, String d, String p) {
        name=n;
        domain=d;
        path=p;        
    }
    
    public String getName() {
        return name;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public String getPath() {
        return path;
    }
    
}
