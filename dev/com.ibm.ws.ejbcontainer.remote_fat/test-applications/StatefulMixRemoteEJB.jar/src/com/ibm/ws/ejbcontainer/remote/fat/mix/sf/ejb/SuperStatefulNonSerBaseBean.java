/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import static junit.framework.Assert.assertEquals;

import javax.annotation.PostConstruct;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Stateful;

@Stateful
public class SuperStatefulNonSerBaseBean extends StatefulNonSerBase implements SuperStatefulLocal, SuperStatefulRemote {
    private int passCount = 0;
    private int actCount = 0;

    private String superString = "Empty";

    @PostConstruct
    public void create() {
        superString = "SuperDefault";
        setBaseString("BaseDefault");
    }

    @PrePassivate
    public void passivate() {
        passCount++;
    }

    @PostActivate
    public void activate() {
        actCount++;
    }

    @Override
    public String getSuperString() {
        return superString;
    }

    public void setSuperString(String str) {
        superString = str;
    }

    @Override
    public void checkSuperStart() {

        // Make sure the bean was activated and passivated once at this point
        assertEquals("Checking passivate count: " + passCount, 1, passCount);
        assertEquals("Checking activate count: " + actCount, 1, actCount);

        // Check the local and super values
        String lSuperString = getSuperString();
        String lBaseString = getBaseString();
        assertEquals("Checking value of superString: " + lSuperString, lSuperString, "SuperDefault");
        assertEquals("Checking value of baseString: " + lBaseString, lBaseString, "BaseDefault");
        setSuperString("ModifiedSuperString");
        setBaseString("ModifiedBaseString");
    }

    @Override
    public void checkSuperEnd() {
        // Make sure the bean was activated and passivated twice at this point
        assertEquals("Checking passivate count: " + passCount, 2, passCount);
        assertEquals("Checking activate count: " + actCount, 2, actCount);

        // Check the local and super values
        String lSuperString = getSuperString();
        String lBaseString = getBaseString();
        assertEquals("Checking value of superString: " + lSuperString, lSuperString, "ModifiedSuperString");
        assertEquals("Checking value of baseString: " + lBaseString, lBaseString, "ModifiedBaseString");
    }
}
