/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.embeddable;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.tests.embeddable.tests.AbstractFATSuite;
import com.ibm.ws.jpa.tests.embeddable.tests.JPA10EmbeddableBasic_EJB;
import com.ibm.ws.jpa.tests.embeddable.tests.JPA10EmbeddableBasic_WEB;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                JPA10EmbeddableBasic_EJB.class,
                JPA10EmbeddableBasic_WEB.class,
//                JPA10EmbeddableNested_EJB.class,
//                JPA10EmbeddableNested_WEB.class,
//                JPA10EmbeddableRelationship_EJB.class,
//                JPA10EmbeddableRelationship_WEB.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA32());

}
