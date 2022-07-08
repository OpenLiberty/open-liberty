/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.util.List;

import io.openliberty.data.Data;
import io.openliberty.data.Query;

/**
 *
 */
@Data
public interface PersonRepo {
    @Query("SELECT o FROM Person o WHERE o.lastName=?1")
    List<Person> find(String lastName);

    void save(Person p);
}
