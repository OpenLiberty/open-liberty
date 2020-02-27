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
package javax.servlet.sip;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public interface SipServletUtil {

	ServletConfig wrapConfig(ServletConfig cfg);

	void initSiplet(ServletContext servletContext, SipServlet sipServlet);

}
