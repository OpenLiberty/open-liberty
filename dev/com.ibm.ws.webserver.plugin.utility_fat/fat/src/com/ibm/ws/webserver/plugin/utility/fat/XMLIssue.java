/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.webserver.plugin.utility.fat;

public class XMLIssue {
    String path;
    String problem;

    public XMLIssue(String path, String problem) {
        this.path = path;
        this.problem = problem;
    }

    public String getPath() {return path;}
    public String getProblem() {return problem;}
}
