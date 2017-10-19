/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import static org.junit.Assert.assertThat;

import java.util.Comparator;

import org.hamcrest.Matchers;
import org.junit.Test;

public class NaturalComparatorTest {
    @Test
    public void testNaturalComparator() {
        Comparator<String> c = NaturalComparator.instance;
        assertThat(c.compare("", ""), Matchers.equalTo(0));
        assertThat(c.compare("", "a"), Matchers.lessThan(0));
        assertThat(c.compare("a", ""), Matchers.greaterThan(0));

        assertThat(c.compare("0", "0"), Matchers.equalTo(0));
        assertThat(c.compare("0", "1"), Matchers.lessThan(0));
        assertThat(c.compare("1", "0"), Matchers.greaterThan(0));

        assertThat(c.compare("0a", "0a"), Matchers.equalTo(0));
        assertThat(c.compare("0a", "0"), Matchers.greaterThan(0));
        assertThat(c.compare("0", "0a"), Matchers.lessThan(0));
        assertThat(c.compare("0a", "1a"), Matchers.lessThan(0));
        assertThat(c.compare("0a", "1"), Matchers.lessThan(0));
        assertThat(c.compare("0", "1a"), Matchers.lessThan(0));
        assertThat(c.compare("1a", "0a"), Matchers.greaterThan(0));
        assertThat(c.compare("1a", "0"), Matchers.greaterThan(0));
        assertThat(c.compare("1", "0a"), Matchers.greaterThan(0));

        assertThat(c.compare("00", "00"), Matchers.equalTo(0));
        assertThat(c.compare("00a", "00a"), Matchers.equalTo(0));
        assertThat(c.compare("00a", "00"), Matchers.greaterThan(0));
        assertThat(c.compare("00", "00a"), Matchers.lessThan(0));
        assertThat(c.compare("00", "10"), Matchers.lessThan(0));
        assertThat(c.compare("00a", "10a"), Matchers.lessThan(0));
        assertThat(c.compare("00a", "10"), Matchers.lessThan(0));
        assertThat(c.compare("00", "10a"), Matchers.lessThan(0));
        assertThat(c.compare("10", "00"), Matchers.greaterThan(0));
        assertThat(c.compare("10a", "00a"), Matchers.greaterThan(0));
        assertThat(c.compare("10a", "00"), Matchers.greaterThan(0));
        assertThat(c.compare("10", "00a"), Matchers.greaterThan(0));
        assertThat(c.compare("00", "01"), Matchers.lessThan(0));
        assertThat(c.compare("00a", "01a"), Matchers.lessThan(0));
        assertThat(c.compare("00a", "01"), Matchers.lessThan(0));
        assertThat(c.compare("00", "01a"), Matchers.lessThan(0));
        assertThat(c.compare("01", "00"), Matchers.greaterThan(0));
        assertThat(c.compare("01a", "00a"), Matchers.greaterThan(0));
        assertThat(c.compare("01a", "00"), Matchers.greaterThan(0));
        assertThat(c.compare("01", "00a"), Matchers.greaterThan(0));

        assertThat(c.compare("1", "10"), Matchers.lessThan(0));
        assertThat(c.compare("1a", "10a"), Matchers.lessThan(0));
        assertThat(c.compare("1a", "10"), Matchers.lessThan(0));
        assertThat(c.compare("1", "10a"), Matchers.lessThan(0));
        assertThat(c.compare("10", "1"), Matchers.greaterThan(0));
        assertThat(c.compare("10a", "1a"), Matchers.greaterThan(0));
        assertThat(c.compare("10a", "1"), Matchers.greaterThan(0));
        assertThat(c.compare("10", "1a"), Matchers.greaterThan(0));

        assertThat(c.compare("2", "10"), Matchers.lessThan(0));
        assertThat(c.compare("2a", "10a"), Matchers.lessThan(0));
        assertThat(c.compare("2a", "10"), Matchers.lessThan(0));
        assertThat(c.compare("2", "10a"), Matchers.lessThan(0));
        assertThat(c.compare("10", "2"), Matchers.greaterThan(0));
        assertThat(c.compare("10a", "2a"), Matchers.greaterThan(0));
        assertThat(c.compare("10a", "2"), Matchers.greaterThan(0));
        assertThat(c.compare("10", "2a"), Matchers.greaterThan(0));

        assertThat(c.compare("exception_summary_13.09.25_01.52.43.1.log", "exception_summary_13.09.25_01.52.43.3.log"), Matchers.lessThan(0));
        assertThat(c.compare("exception_summary_13.09.25_01.52.43.2.log", "exception_summary_13.09.25_01.52.43.3.log"), Matchers.lessThan(0));
        assertThat(c.compare("exception_summary_13.09.25_01.52.43.3.log", "exception_summary_13.09.25_01.52.43.3.log"), Matchers.equalTo(0));
        assertThat(c.compare("exception_summary_13.09.25_01.52.43.3.log", "exception_summary_13.09.25_01.52.43.1.log"), Matchers.greaterThan(0));
        assertThat(c.compare("exception_summary_13.09.25_01.52.43.3.log", "exception_summary_13.09.25_01.52.43.2.log"), Matchers.greaterThan(0));
        assertThat(c.compare("exception_summary_13.09.25_01.52.43.3.log", "exception_summary_13.09.25_01.52.43.3.log"), Matchers.equalTo(0));

        assertThat(c.compare("exception_summary_13.09.25_01.52.43.9.log", "exception_summary_13.09.25_01.52.43.10.log"), Matchers.lessThan(0));
        assertThat(c.compare("exception_summary_13.09.25_01.52.43.10.log", "exception_summary_13.09.25_01.52.43.9.log"), Matchers.greaterThan(0));
    }

    public void testFileLogSetTest() {}
}
