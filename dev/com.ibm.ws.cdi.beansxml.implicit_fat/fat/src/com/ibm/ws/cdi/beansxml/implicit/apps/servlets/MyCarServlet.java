/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.apps.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.beansxml.implicit.apps.beans.MyBike;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.MyCar;
import com.ibm.ws.cdi.beansxml.implicit.apps.beans.MyPlane;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/")
public class MyCarServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    MyCar myCar; //in the war, with a beans.xml
    @Inject
    MyBike myBike; //in a jar, with a beans.xml
    @Inject
    BeanManager beanManager;

    @Test
    public void testDisabledArchive() throws IOException {

        assertNotNull(myCar);
        assertEquals(MyCar.CAR, myCar.getMyCar());

        /** just in case the bean manager is mad */
        Set<Bean<?>> myCar = beanManager.getBeans(MyCar.class);
        if (myCar.isEmpty()) {
            fail("No Car Found by Bean Manager");
        }

        assertNotNull(myBike);
        assertEquals(MyBike.BIKE, myBike.getMyBike());

        /** just in case the bean manager is mad */
        Set<Bean<?>> myBike = beanManager.getBeans(MyBike.class);
        if (myBike.isEmpty()) {
            fail("No Bike Found by Bean Manager");
        }
        /** We know the bean manager is honest */
        Set<Bean<?>> myPlane = beanManager.getBeans(MyPlane.class); //MyPlane is in a jar with no beans.xml so should not be found
        if (!myPlane.isEmpty()) {
            fail("A Plane was found by Bean Manager when it should not have been");
        }
    }

}
