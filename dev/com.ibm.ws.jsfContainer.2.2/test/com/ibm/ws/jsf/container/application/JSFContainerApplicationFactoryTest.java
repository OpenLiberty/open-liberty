/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.application;

import org.junit.Test;

public class JSFContainerApplicationFactoryTest {

    @Test
    public void verifyMojarraAppFactory() throws ClassNotFoundException {
        // Verify the name of the Mojarra ApplicationFactoryImpl is unchanged,
        // since we can't have a proper compile-time dependency on the class
        Class.forName(JSFContainerApplicationFactory.MOJARRA_APP_FACTORY);
    }

    @Test
    public void verifyMyFacesAppFactory() throws ClassNotFoundException {
        // Verify the name of the MyFaces ApplicationFactoryImpl is unchanged,
        // since we can't have a proper compile-time dependency on the class
        Class.forName(JSFContainerApplicationFactory.MYFACES_APP_FACTORY);
    }

}