/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.hibernate.validator;

import test.common.OSSMessageTest;

/**
 * The purpose of this test is to ensure that we always pick up changes in Hibernate Validator NLS files so we can translate
 * the changes into languages that OpenLiberty ships but Hibernate Validator does not.
 * Currently OpenLiberty provides: cs, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, ru, zh_TW, zh
 * Currently Hibernate Validator provides: ar, cs, de, en, es, fa, fr, hu, ko, mn_MN, pt_BR, ru, sk, tr, uk, zh_CN
 *
 * This means that OpenLiberty must provide the following languages: it, ja, pl, ro, zh_TW, zh
 */
public class HibernateValidatorMessageTest extends OSSMessageTest {

    public HibernateValidatorMessageTest() {
        super("hibernate-validator-6", "com.ibm.ws.org.hibernate.validator", "org/hibernate/validator/ValidationMessages.properties");
    }

}
