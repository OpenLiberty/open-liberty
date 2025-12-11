/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package componenttest.topology.database.container;

import java.util.Arrays;
import java.util.List;

import org.testcontainers.utility.DockerImageName;

/**
 * This is a Derby no-op database test container that is returned
 * when attempting to test against derby embedded.
 *
 * This test container overrides the start and stop methods
 * to prevent the creation of a docker container.
 *
 */
class DerbyJava17PlusContainer extends DerbyNoopContainer {

    public static final List<String> supportLibraries = Arrays.asList("derbytools.jar", "derbyshared.jar");

    // Calling super constructor like this since super("") doesn't compile with
    // Java 25 due to stricter annotation checking rules
    public DerbyJava17PlusContainer(DockerImageName image) {
        super(DockerImageName.parse(""));
    }

    // Calling super constructor like this since super("") doesn't compile with
    // Java 25 due to stricter annotation checking rules
    public DerbyJava17PlusContainer(String image) {
        super(DockerImageName.parse(""));
    }

    // Calling super constructor like this since super("") doesn't compile with
    // Java 25 due to stricter annotation checking rules
    public DerbyJava17PlusContainer() {
        super(DockerImageName.parse(""));
    }

}
