/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.flush.fat.printTests;

public class DummyObject {

    public String small;
    public String string8192;
    public String string8193;

    DummyObject() {
        generateSmall();
        generate8192();
        generate8193();
    }

    private void generateSmall() {
        small = "smallStr";
    }

    private void generate8192() {
        String starter = "";
        String string = "R";
        for (int i = 0; i < 8192; i++) {
            starter = starter + string;
        }
        string8192 = starter;
    }

    private void generate8193() {
        String starter = "";
        String string = "R";
        for (int i = 0; i < 8193; i++) {
            starter = starter + string;
        }
        string8193 = starter;
    }

    public String toStringSmall() {
        return small;
    }

    public String toString8192() {
        return string8192;
    }

    public String toString8193() {
        return string8193;
    }

}
