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

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;

@Stateful(name = "MySessionBean2")
@LocalBean
public class MySessionBean2 implements SessionBeanInterface {

    @Resource(name = "greeting")
    String greeting;

    @Resource(name = "MyManagedBean1")
    MyManagedBean1 managedBean1;

    public String hello() {
        return greeting + "\n" + managedBean1.hello();
    }

}
