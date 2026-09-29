/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json;

/**
 * An immutable JSON string value.
 */
public interface JsonString extends JsonValue {

    /**
     * Returns the JSON string value.
     *
     * @return a JSON string value
     */
    String getString();


    /**
     * Returns the char sequence for the JSON String value
     *
     * @return a char sequence for the JSON String value
     */
    CharSequence getChars();

    /**
     * Compares the specified object with this {@code JsonString} for equality.
     * Returns {@code true} if and only if the specified object is also a
     * {@code JsonString}, and their {@link #getString()} objects are
     * <i>equal</i>.
     *
     * @param obj the object to be compared for equality with this 
     *      {@code JsonString}
     * @return {@code true} if the specified object is equal to this 
     *      {@code JsonString}
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code JsonString} object.  
     * The hash code of a {@code JsonString} object is defined to be its 
     * {@link #getString()} object's hash code.
     *
     * @return the hash code value for this {@code JsonString} object
     */
    @Override
    int hashCode();

}
