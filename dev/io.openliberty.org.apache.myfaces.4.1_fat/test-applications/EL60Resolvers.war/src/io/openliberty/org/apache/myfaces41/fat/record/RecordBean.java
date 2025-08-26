/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.record;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named("recordBean")
@RequestScoped
public class RecordBean {

    public record TestRecord(String property1, String property2) {}

    public TestRecord getTestRecord() {
        return new TestRecord("abc", "def");
    }
}