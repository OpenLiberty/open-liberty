/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.cdi40.internal.fat.bce.basicwar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/basicBceTest")
@SuppressWarnings("serial")
public class BasicBCETestServlet extends FATServlet {

    @Inject
    private Instance<TestBean> testBeans;

    @Test
    @Mode(TestMode.LITE)
    public void testInjectedBeans() {
        List<String> beanNames = testBeans.stream()
                                          .map(TestBean::getName)
                                          .collect(Collectors.toList());

        assertThat(beanNames, containsInAnyOrder("registered", "synthetic", "annotated"));
    }
}
