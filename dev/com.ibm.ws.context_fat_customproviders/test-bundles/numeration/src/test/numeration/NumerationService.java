/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.numeration;

/**
 * This a fake thread context that we made up for testing purposes.
 * It makes a numeration context available to each thread which
 * shows a textual representation for numbers in different numeration
 * systems (for example, binary)
 */
public interface NumerationService {

    /**
     * Sets the radix for the current thread.
     *
     * @param radix in the range of 2 (binary) through 36
     */
    public void setRadix(int radix);

    /**
     * Returns text representing the number.
     *
     * @param number a number
     * @return text representing the number.
     */
    public String toString(long number);
}
