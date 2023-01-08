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

package com.ibm.ws.jpa.spec10.embeddable;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.spec10.embeddable.issues.TestOLGH10310_EJB;
import com.ibm.ws.jpa.spec10.embeddable.issues.TestOLGH10310_Web;
import com.ibm.ws.jpa.spec10.embeddable.tests.AbstractFATSuite;
import com.ibm.ws.jpa.spec10.embeddable.tests.JPA10EmbeddableBasic_EJB;
import com.ibm.ws.jpa.spec10.embeddable.tests.JPA10EmbeddableBasic_WEB;
import com.ibm.ws.jpa.spec10.embeddable.tests.JPA10EmbeddableNested_EJB;
import com.ibm.ws.jpa.spec10.embeddable.tests.JPA10EmbeddableNested_WEB;
import com.ibm.ws.jpa.spec10.embeddable.tests.JPA10EmbeddableRelationship_EJB;
import com.ibm.ws.jpa.spec10.embeddable.tests.JPA10EmbeddableRelationship_WEB;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                TestOLGH10310_Web.class,
                TestOLGH10310_EJB.class,
                JPA10EmbeddableBasic_EJB.class,
                JPA10EmbeddableBasic_WEB.class,
                JPA10EmbeddableNested_EJB.class,
                JPA10EmbeddableNested_WEB.class,
                JPA10EmbeddableRelationship_EJB.class,
                JPA10EmbeddableRelationship_WEB.class
})
public class JPA20Suite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new RepeatWithJPA20());

}
