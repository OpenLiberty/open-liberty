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
package io.openliberty.org.eclipse.yasson;

import test.common.OSSMessageTest;

/**
 * The purpose of this test is to ensure that we always pick up changes in Yasson NLS files so we can translate
 * the changes into languages that OpenLiberty ships but Yasson does not.
 * Currently OpenLiberty provides: cs, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, ru, zh_TW, zh
 * Currently Yasson provides English only
 *
 * This means that OpenLiberty must provide the other languages
 */
public class YassonMessageTest extends OSSMessageTest {

    public YassonMessageTest() {
        super("yasson-3", "io.openliberty.org.eclipse.yasson.3", "yasson-messages.properties");
    }

}
