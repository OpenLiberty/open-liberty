/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import com.ibm.ws.security.registry.UserRegistryIllegalArgumentTemplate;

/**
 * @see UserRegistryIllegalArgumentTemplate
 */
public class BasicRegistryIllegalArgumentTest extends UserRegistryIllegalArgumentTemplate {

    public BasicRegistryIllegalArgumentTest() throws Exception {
        super(basicRegistry());
    }

    static BasicRegistry basicRegistry() {
        BasicRegistry basicRegistry = new BasicRegistry();
        basicRegistry.activate(new BasicRegistryConfig() {

            @Override
            public String realm() {
                return "testRealm";
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public User[] user() {
                return new User[] {};
            }

            @Override
            public Group[] group() {
                return new Group[] {};
            }

            @Override
            public String config_id() {
                return "test-config-id";
            }
        });
        return basicRegistry;
    }
}
