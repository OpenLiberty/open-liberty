/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.beans.v2;

import javax.enterprise.context.Dependent;

/**
 * CDI Testing: Type for upgrade handler constructor injection.
 */
// Cannot be session or request scoped: There is no active session or request context in a listener.
// Used by Servlet and Filter, but also by Listener.
@Dependent
public class ConstructorBean extends CDIDataBean {
    // EMPTY
}
