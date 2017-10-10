/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.dynamicannotationconflict;

import web.common.BaseServlet;

/**
 * Dynamic Annotation Conflict Servlet
 */
//Conflict in web.xml
public class DynamicAnnotationConflict1 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public DynamicAnnotationConflict1() {
        super("DynamicAnnotationConflict1");
    }

}
