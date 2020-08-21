/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jaxrs30.fat.jsonb;

import java.time.LocalDate;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/app")
public class App extends Application {
    final static Widget[] WIDGETS = new Widget[] {
        new Widget("foo", LocalDate.of(2020, 1, 25), 33, 2.304, 
                   new Person("Rob", "Roberts", LocalDate.of(1989, 10, 15), "3311 Elm Street")),
        new Widget("bar", LocalDate.of(2020, 6, 7), 85, 19.216, 
                   new Person("Jim", "Jameson", LocalDate.of(1992, 8, 13), "777 77th Street")),
    };
}
