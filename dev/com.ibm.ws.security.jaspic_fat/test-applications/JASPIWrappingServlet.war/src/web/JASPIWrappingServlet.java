/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

public class JASPIWrappingServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JASPIWrappingServlet() {
        super("JASPIWrappingServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WriteWrapperStep());
    }
}
