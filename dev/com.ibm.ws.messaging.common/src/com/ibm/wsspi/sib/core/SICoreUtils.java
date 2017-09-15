/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.sib.core;

/**
 * <p>Utilities class for small pieces of implementation which implementors of the core SPI
 * can use to do simple common tasks, such as validating parameters.
 * <p>
 * This class has no security implications.
 * 
 */
public final class SICoreUtils
{

    /**
     * Private constructor as nobody should implement instances of this class.
     */
    private SICoreUtils()
    {
        // Nobody should need to instantiate this class. 
    }

    /**
     * A valid destination prefix has length which is up to this many characters.
     */
    public static final int DESTINATION_PREFIX_MAX_LENGTH = 12;

    public static final String VALID = "VALID";
    public static final String MAX_LENGTH_EXCEEDED = "MAX_LENGTH_EXCEEDED";

    /**
     * Determines whether a destination prefix is valid or not.
     * 
     * <p>If the destination prefix has more than {@link #DESTINATION_PREFIX_MAX_LENGTH} characters, then it is invalid.
     * <p>The destination prefix is invalid if it contains any characters not in the following
     * list:
     * <ul>
     * <li>a-z (lower-case alphas)</li>
     * <li>A-Z (upper-case alphas)</li>
     * <li>0-9 (numerics)</li>
     * <li>. (period)</li>
     * <li>/ (slash)</li>
     * <li>% (percent)</li>
     * </ul>
     * <p>null and empty string values for a destination prefix are valid, and
     * simply indicate an empty prefix.
     * 
     * @param destinationPrefix The destination prefix to which the validity
     *            check is applied.
     * @return String VALID if the prefix is valid or
     *         MAX_LENGTH_EXCEEDED if prefix has more than {@link #DESTINATION_PREFIX_MAX_LENGTH} characters or
     *         The Invalid character in the prefix
     */
    public static final String isDestinationPrefixValid(String destinationPrefix)
    {

        String result = VALID; // Assume the prefix is valid until we know otherwise.
        boolean isValid = true;

        // null indicates that no destination prefix is being used.
        if (null != destinationPrefix)
        {
            // Check for the length first.
            int len = destinationPrefix.length();

            if (len > DESTINATION_PREFIX_MAX_LENGTH)
            {
                isValid = false;
                result = MAX_LENGTH_EXCEEDED;
            }
            else
            {
                // Cycle through each character in the prefix until we find an invalid character, 
                // or until we come to the end of the string.
                int along = 0;

                while ((along < len) && isValid)
                {
                    char c = destinationPrefix.charAt(along);
                    if (!(('A' <= c) && ('Z' >= c)))
                    {
                        if (!(('a' <= c) && ('z' >= c)))
                        {
                            if (!(('0' <= c) && ('9' >= c)))
                            {
                                if ('.' != c && '/' != c && '%' != c)
                                {
                                    // This character isn't a valid one...  
                                    isValid = false;
                                    result = String.valueOf(c);
                                }
                            }
                        }
                    }
                    // Move along to the next character in the string.
                    along += 1;
                }
            }
        }
        return result;
    }

}
