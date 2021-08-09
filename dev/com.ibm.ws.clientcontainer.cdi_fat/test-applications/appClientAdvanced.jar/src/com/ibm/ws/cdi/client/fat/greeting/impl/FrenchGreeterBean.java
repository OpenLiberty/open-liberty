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
package com.ibm.ws.cdi.client.fat.greeting.impl;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.cdi.client.fat.counting.Counted;
import com.ibm.ws.cdi.client.fat.greeting.French;
import com.ibm.ws.cdi.client.fat.greeting.Greeter;

@ApplicationScoped
@French
public class FrenchGreeterBean implements Greeter {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cdi.client.fat.Hello#getHello()
     */
    @Override
    @Counted
    public String getHello() {
        return "Bonjour";
    }

}
