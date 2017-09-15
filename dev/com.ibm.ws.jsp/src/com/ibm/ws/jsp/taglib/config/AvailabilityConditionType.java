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

public class AvailabilityConditionType {
    private final static int WEBINF_FILE = 0;
    private final static int SERVLET_CLASSNAME = 1;

    public final static AvailabilityConditionType webinfFileType=new AvailabilityConditionType(WEBINF_FILE);
    public final static AvailabilityConditionType servletClassNameType=new AvailabilityConditionType(SERVLET_CLASSNAME);
    
    private int key=0;
    
    private AvailabilityConditionType(int key) {
        this.key = key;
    }
}
