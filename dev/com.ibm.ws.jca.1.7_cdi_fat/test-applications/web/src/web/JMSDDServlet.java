/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Topic;
import jakarta.servlet.annotation.WebServlet;

@JMSDestinationDefinition(name = "java:comp/env/jms/dummydd",
                          description = "Test JMS Destination",
                          resourceAdapter = "testJMSDDResource.jmsdestination",
                          interfaceName = "jakarta.jms.Topic",
                          destinationName = "DUMMY_DESTINATION")
@WebServlet("/*")
public class JMSDDServlet extends FATServlet {

    @Resource(name = "jms/dummyddRef", lookup = "java:comp/env/jms/dummydd")
    Topic dummy;

    public void testJMSDDResource() throws Exception {
        assertNotNull(dummy);
        assertEquals("DUMMY_TOPIC", dummy.getTopicName());
    }

}
