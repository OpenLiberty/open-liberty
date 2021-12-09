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
package org.gjwatts.liberty;

public class RecordTest {

    /**
     * Records allow much easier creation of objects for containing immutable data by providing initalization, getter methods, equals(), hashCode() and toString() for free   
     * 
     * For more information -> https://openjdk.java.net/jeps/395
     *   
     * @return
     */
    public static String test() {
        record State(String name, String capital, String bestCity, int yearFounded, int population) {};
        State mn = new State("Minnesota", "St. Paul", "Rochester", 1858, 5640000);
        return mn.name + " has a population of " + mn.population + ", was founded in " + mn.yearFounded + " and the capital is " + mn.capital + ".  The best city is " + mn.bestCity + ".";
    }
}
