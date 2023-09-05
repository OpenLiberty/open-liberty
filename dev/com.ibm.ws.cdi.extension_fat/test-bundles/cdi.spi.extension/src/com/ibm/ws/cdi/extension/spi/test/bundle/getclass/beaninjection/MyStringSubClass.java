/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.spi.test.bundle.getclass.beaninjection;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.beaninjection.AbstractString;

//No BDA
public class MyStringSubClass extends AbstractString {

    @Override
    public String getAbstractMethodString() {
        return "This message comes from a class that extends an abstract class in another bundle";
    }

    @Override
    public String getOverriddenMsgFromSubclass() {
        return "And its BDA is on the abstract class, but it is registered via the SPI";
    }

}
