/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

public class TestCaseResult {
    private boolean failure = false;
    private boolean error = false;

    private String packageName;
    private String name;

    public TestCaseResult(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
    }

    public void setError() {
        error = true;
    }

    public void setFailure() {
        failure = true;
    }

    public String toString() {
        String pass;
        if (! failure && ! error) {
            pass = "Passed!";
        } else {
            pass = "Failed!";
        }

        return packageName + "." + name + " " + pass;
    }
}
