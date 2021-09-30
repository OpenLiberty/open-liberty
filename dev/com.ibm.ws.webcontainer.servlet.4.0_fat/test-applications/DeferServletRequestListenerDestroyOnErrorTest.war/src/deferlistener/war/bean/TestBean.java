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
package deferlistener.war.bean;

import javax.inject.Named;

import javax.enterprise.context.RequestScoped;
/**
*  Used to test default values for deferServletRequestListenerDestroyOnError on servlet-4.0 and servlet-5.0 
*  See test description for more details. 
*/
@Named("testBean")
@RequestScoped
public class TestBean {

    String prop = "test";

    public String getProp(){
        return prop;
    }

}
