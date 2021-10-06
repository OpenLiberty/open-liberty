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

package com.ibm.ws.testtooling.jpaprovider;

/**
 * Simple Utility class for indicating the JPA Persistence Provider
 */
public enum JPAPersistenceProvider {
    ECLIPSELINK("ECLIPSELINK"),
    HIBERNATE("HIBERNATE"),
    OPENJPA("OPENJPA"),
    DEFAULT("DEFAULT");

    private String name;

    private JPAPersistenceProvider(String name) {
        this.name = name;
    }

    /**
     * Given a persistence provider name, returns the matching JPAPersistenceProvider enumeration value
     */
    public static JPAPersistenceProvider resolveJPAPersistenceProvider(String providerName) {
        if (providerName == null || "".equals(providerName.trim())) {
            System.err.println("Cannot resolve persistence provider " + providerName);
            return DEFAULT;
        }

        final String toUpper = providerName.toUpperCase();
        if (toUpper.contains(ECLIPSELINK.name)) {
            return ECLIPSELINK;
        }
        if (toUpper.contains(HIBERNATE.name)) {
            return HIBERNATE;
        }
        if (toUpper.contains(OPENJPA.name)) {
            return OPENJPA;
        }

        return DEFAULT;
    }

    /**
     * Checks if the given database product name matches on the given DatabaseVendors
     */
    public static boolean checkJPAPersistenceProviderName(String providerName, JPAPersistenceProvider provider) {
        if (providerName == null || "".equals(providerName.trim())) {
            return false;
        }

        final String toUpper = providerName.toUpperCase();
        switch (provider) {
            // Basing determination off product version using
            // info from https://www.ibm.com/support/knowledgecenter/en/SSEPEK_11.0.0/java/src/tpc/imjcc_c0053013.html
            case ECLIPSELINK:
                return toUpper.contains(ECLIPSELINK.name);
            case HIBERNATE:
                return toUpper.contains(HIBERNATE.name);
            case OPENJPA:
                return toUpper.contains(OPENJPA.name);
            case DEFAULT:
                return false;
        }

        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
