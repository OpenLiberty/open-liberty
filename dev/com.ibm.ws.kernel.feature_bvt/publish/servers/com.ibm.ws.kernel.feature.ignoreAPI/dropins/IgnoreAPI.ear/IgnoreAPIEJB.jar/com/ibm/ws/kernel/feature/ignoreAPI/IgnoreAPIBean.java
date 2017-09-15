/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.feature.ignoreAPI;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;



@Startup
@Singleton
@LocalBean
public class IgnoreAPIBean {
    private final static String EYE_CATCHER = "*** IgnoreAPITest- able to load blocked package: ";
    
    @PostConstruct
    public void init() {
        // Using our super-secret bootstrap propery, we have blocked access to the 
        // org.codehaus.jackson.xc package.  So the following class load should fail.
        try {
            Class.forName("org.codehaus.jackson.xc.XmlAdapterJsonSerializer");
            System.out.println(EYE_CATCHER + "true");
        } catch (ClassNotFoundException ex) {
            System.out.println(EYE_CATCHER + "false");
        }
    }

}
