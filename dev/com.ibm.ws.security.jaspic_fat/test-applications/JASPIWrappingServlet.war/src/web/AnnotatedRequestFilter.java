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

import javax.servlet.annotation.WebFilter;

@WebFilter(urlPatterns = "/*")
public class AnnotatedRequestFilter extends BaseRequestFilter {
    {
        super.filterName = "AnnotatedRequestFilter";
    }
}
