/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

/**
 * Entity that is a Java record which has an embedded ID that is
 * also a Java record.
 */
public record Animal(ScientificName id, String commonName, long version) {
    public static record ScientificName(String genus, String species) {
    }

    public static Animal of(String commonName, String genus, String species) {
        return new Animal(new ScientificName(genus, species), commonName, 0);
    }

    public Animal withCommonName(String newCommonName) {
        return new Animal(id(), newCommonName, version());
    }
}
