/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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

package com.ibm.ws.kernel.feature.internal.util;

import java.util.List;

public class ImageInfo {

    private static String[] parse(String name) {
        String[] parsedName = new String[2];

        int digitOffset = name.length();
        while ( (digitOffset > 0) && Character.isDigit( name.charAt(digitOffset - 1) ) ) {
            digitOffset--;
        }

        parsedName[0] = name.substring(0, digitOffset);
        parsedName[1] = name.substring(digitOffset);

        return parsedName;
    }

    public ImageInfo(String name, List<String> features) {
        this.name = name;

        String[] parsedName = parse(name);
        this.baseName = parsedName[0];
        this.version = parsedName[1];

        this.features = features;

        this.hashCode = name.hashCode();
        this.toString = getClass().getSimpleName() + "(" + name + ")";
    }

    private final int hashCode;

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (!(obj instanceof ImageInfo)) {
            return false;
        } else {
            ImageInfo other = (ImageInfo) obj;
            if (hashCode() != other.hashCode()) {
                return false;
            }

            String thisName = getName();
            String otherName = other.getName();
            if (thisName == null) {
                return (otherName == null);
            } else if (otherName == null) {
                return false;
            } else {
                return thisName.equals(otherName);
            }
        }
    }

    private final String toString;

    @Override
    public String toString() {
        return toString;
    }

    //

    private final String name;

    public String getName() {
        return name;
    }

    private final String baseName;

    public String getBaseName() {
        return baseName;
    }

    private final String version;

    public String getVersion() {
        return version;
    }

    //

    private final List<String> features;

    public List<String> getFeatures() {
        return features;
    }
}
