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
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb;

import javax.ejb.CreateException;

/**
 * Bean implementation class for Enterprise Bean: InitEJBLocal/Remote /
 * InitLocal/Remote
 **/
public class InitCompViewCMTBean {

    private String ivString = "InitCompViewCMTBean";
    private int ivInt = 1;

    /**
     * Default initialize method with no parameters.
     * <p>
     */
    public void initialize() {
        ivString += ":initialize";
        ivInt += 10;
    }

    /**
     * Default initialize method with one parameter.
     * <p>
     */
    public void initialize(String string) {
        ivString += ":initialize:" + string;
        ivInt += 100;
    }

    /**
     * Custom initialize method with no parameters.
     * <p>
     */

    public void initializeDefault() {
        ivString += ":initializeDefault";
        ivInt += 1000;
    }

    /**
     * Custom initialize method with one parameter.
     * <p>
     */
    public void initializeBasic(String string) {
        ivString += ":initializeBasic:" + string;
        ivInt += 10000;
    }

    /**
     * Custom initialize method with one parameter, different type.
     * <p>
     */
    public void initializeBasic(int integer) {
        ivString += ":initializeBasic";
        ivInt += (100000 + integer);
    }

    /**
     * Custom initialize method with two parameters.
     * <p>
     */
    public void initializeAdv(String string, int integer) {
        ivString += ":initializeAdv:" + string;
        ivInt += (1000000 + integer);
    }

    /** Return the String value state of Stateful bean. **/
    public String getString() {
        return ivString;
    }

    /** Return the int value state of Stateful bean. **/
    public int getInt() {
        return ivInt;
    }

    /** Designated 'Remove' method. **/

    public String finish(String string, int integer) {
        ivString += ":finish:" + string;
        ivInt += (3 + integer);

        return (ivString + ":" + ivInt);
    }

    /**
     * Custom create method with two parameters.
     * <p>
     */
    public void ejbCreateAdv(String string, int integer) throws CreateException {
        ivString += ":ejbCreateAdv:" + string;
        ivInt += (1000000 + integer);
    }

    public InitCompViewCMTBean() {
    }
}
