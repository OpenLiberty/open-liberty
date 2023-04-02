/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package web.jsonbtest;

import java.util.Date;

import javax.json.bind.annotation.JsonbTypeDeserializer;

/**
 * Application class that can be marshalled/unmarshalled to/from JSON.
 */
public class Scrum {
    public String squadName;
    public Date start;
    @JsonbTypeDeserializer(LocationDeserializer.class) // JsonbDeserializer disambiguates which Location subclass to use
    public Location location;

    @Override
    public String toString() {
        return "Scrum for " + squadName + ' ' + start + " @" + location;
    }
}
