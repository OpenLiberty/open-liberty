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
package test.jsonp.bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Java object for an OSGi service component to marshall/unmarshall to/from JSON via JSON-B.
 */
public class DiscGolfCourse {
    public String name;
    public String street;
    public String city;
    public String state;
    public Integer zipCode;
    public List<Basket> baskets = new ArrayList<Basket>();

    @Override
    public String toString() {
        return name + " @ " + street + ' ' + city + ", " + state + ' ' + zipCode + ' ' + baskets;
    }
}
