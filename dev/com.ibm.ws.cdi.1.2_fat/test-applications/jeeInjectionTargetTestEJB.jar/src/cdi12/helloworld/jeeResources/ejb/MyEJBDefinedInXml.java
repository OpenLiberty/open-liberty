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
package cdi12.helloworld.jeeResources.ejb;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;

/**
 * This EJB will be defined as a stateful ejb via ejb-jar.xml
 */
@Stateful(name = "MyEJBDefinedInXml")
@LocalBean
public class MyEJBDefinedInXml implements SessionBeanInterface {

    public String hello() {
        return "hello from xml";
    }
}
