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
package test.jakarta.data.experimental.web;

import java.io.Serializable;

/**
 * Id class for Town entity.
 */
public class TownId implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;

    public String stateName;

    public TownId(String name, String state) {
        this.name = name;
        this.stateName = state;
    }

    public static TownId of(String name, String state) {
        return new TownId(name, state);
    }

    @Override
    public String toString() {
        return name + ", " + stateName;
    }
}