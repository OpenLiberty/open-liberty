/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

import java.time.LocalDateTime;
import java.util.List;

/**
 * An entity that is a record
 * that has an embeddable and element collection
 */
public record RecordComplex(long id, Name name, List<String> aliases, LocalDateTime version) {

    public class Name {
        public String firstName;
        public String lastName;
    }
}
