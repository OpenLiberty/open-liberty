/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package web;

public class JASPIServlet30 extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JASPIServlet30() {
        super("JASPIServlet30");

        mySteps.add(new ProcessServlet30MethodStep());
        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WriteRunAsSubjectStep());
    }
}
