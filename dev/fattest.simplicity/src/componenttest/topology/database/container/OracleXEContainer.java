/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.database.container;

import java.util.concurrent.Future;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TODO - delete this class once it is no longer being used by any test buckets
 * This is a shadowed class for org.testcontainers.containers.OracleContainer
 * which has been replaced by org.testcontainers.oracle.OracleContainer
 */
@Deprecated
public class OracleXEContainer extends OracleContainer {

    /**
     * @deprecated use {@link #OracleContainer(DockerImageName)} instead
     */
    @Deprecated
    public OracleXEContainer() {
        super();
    }

    public OracleXEContainer(String dockerImageName) {
        super(dockerImageName);
    }

    public OracleXEContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public OracleXEContainer(Future<String> dockerImageName) {
        super(dockerImageName);
    }

}