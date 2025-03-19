/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
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
package io.openliberty.ejb32.javaglobal.ejb;

import static junit.framework.Assert.assertEquals;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;

@Stateless
@Local(InjectIntoServletLocal.class)
public class InjectIntoServletBean {
    @Resource(lookup = "java:global/env/AppXmlGlobalString")
    String appXmlGlobalString;

    @Resource(name = "java:app/env/AppXmlAppStringBean")
    String appXmlAppStringBean;

    public void testJavaGlobalfromAppXml() {
        assertEquals("Incorrect value for java:global/env/AppXmlGlobalString", "AppXmlString: Hello!", appXmlGlobalString);
    }

    public void testJavaAppfromAppXml() {
        assertEquals("Incorrect value for java:app/env/AppXmlAppStringBean", "AppXmlString: Howdy, Bean!", appXmlAppStringBean);
    }
}
