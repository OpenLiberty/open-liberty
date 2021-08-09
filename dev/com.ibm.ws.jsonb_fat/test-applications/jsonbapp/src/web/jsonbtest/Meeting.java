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
package web.jsonbtest;

import java.util.Date;

import javax.json.bind.annotation.JsonbTypeAdapter;

/**
 * Application class that can be marshalled/unmarshalled to/from JSON.
 */
public class Meeting {
    public Date start, end;
    @JsonbTypeAdapter(LocationAdapter.class) // JsonbAdapter disambiguates which Location subclass to use
    public Location location;
    public String title;

    @Override
    public String toString() {
        return title + ": " + start + " - " + end + " @" + location;
    }
}
