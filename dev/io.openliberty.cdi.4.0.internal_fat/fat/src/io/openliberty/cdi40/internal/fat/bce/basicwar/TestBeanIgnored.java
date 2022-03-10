/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.bce.basicwar;

/**
 * Test bean that is ignored because it doesn't get discovered or registered as a bean
 */
public class TestBeanIgnored implements TestBean {

    @Override
    public String getName() {
        return "ignored";
    }

}
