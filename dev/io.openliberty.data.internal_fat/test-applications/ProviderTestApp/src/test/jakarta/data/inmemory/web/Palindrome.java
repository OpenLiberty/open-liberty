/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

package test.jakarta.data.inmemory.web;

import test.jakarta.data.inmemory.provider.Palindromic;

/**
 * Entity class for tests
 */
@Palindromic
public class Palindrome {
    public long id;
    public String letters;
    public int length;

    public Palindrome() {
    }

    public Palindrome(long id, String letters) {
        this.id = id;
        this.letters = letters;
        this.length = letters.length();
    }
}
