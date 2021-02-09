/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.eclipse.yasson;

import test.common.OSSMessageTest;

/**
 * The purpose of this test is to ensure that we always pick up changes in Hibernate Validator NLS files so we can translate
 * the changes into languages that OpenLiberty ships but Hibernate Validator does not.
 * Currently OpenLiberty provides: cs, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, ru, zh_TW, zh
 * Currently Yasson provides English only
 *
 * This means that OpenLiberty must provide the other languages
 */
public class YassonMessageTest extends OSSMessageTest {

    public YassonMessageTest() {
        super("yasson-2", "com.ibm.ws.org.eclipse.yasson.2", "yasson-messages.properties");
    }

}
