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
/* Temporary file pending public availability of api jar */
package javax.servlet;

import java.util.EventListener;

public interface ServletContextAttributeListener extends EventListener {

    default public void attributeAdded(ServletContextAttributeEvent event) {}

    default public void attributeRemoved(ServletContextAttributeEvent event) {}

    default public void attributeReplaced(ServletContextAttributeEvent event) {}

}
