/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package configuratorApp.web.producer;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import configuratorApp.web.ConfiguratorTestBase;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/producerConfiguratorTest")
public class ProducerConfiguratorTest extends ConfiguratorTestBase {

    @Inject
    @ThreeDimensional
    Instance<Dodecahedron> dodecahedronInstance;

    @Test
    @Mode(TestMode.FULL)
    public void sniffProducerConfigurator() {
        assertTrue(dodecahedronInstance.isResolvable());
        Dodecahedron dodecahedron = dodecahedronInstance.get();
        assertTrue(ProcessProducerObserver.applied);
        assertNull(dodecahedron.getParamInjectedBean().getAnnotation());
        dodecahedronInstance.destroy(dodecahedron);
        assertTrue(ProcessProducerObserver.accepted);
    }
}