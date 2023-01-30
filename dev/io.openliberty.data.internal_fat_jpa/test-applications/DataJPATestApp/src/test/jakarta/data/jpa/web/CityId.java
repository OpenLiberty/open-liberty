/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.io.Serializable;

/**
 * Id class for City entity.
 */
public class CityId implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;

    public String stateName;

    public static CityId of(String name, String state) {
        CityId c = new CityId();
        c.name = name;
        c.stateName = state;
        return c;
    }

    @Override
    public String toString() {
        return name + ", " + stateName;
    }
}