/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

/**
 * Shrinkwrap asset for an MP reactive connector config
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * ConnectorProperties sourceConfig = new ConnectorProperties(INCOMING, "mySource")
 *              .addProperty("serverUrl", "localhost:1234")
 *              .addProperty("userId", "testUser");
 *
 * ConnectorProperties sinkConfig = new ConnectorProperties(OUTGOING, "mySink")
 *              .addProperty("serverUrl", "localhost:1234")
 *              .addProperty("userId", "testUser2");
 *
 * PropertiesAsset config = new PropertiesAsset()
 *              .include(sourceConfig)
 *              .include(sinkConfig);
 *
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage())
 *              .addAsResource(config, "META-INF/microprofile-config.properties");
 * </code>
 * </pre>
 */
public class ConnectorProperties extends PropertiesAsset {

    private final String prefix;

    public ConnectorProperties(Direction direction, String channelName) {
        prefix = "mp.messaging." + direction.getValue() + "." + channelName + ".";
    }

    @Override
    public ConnectorProperties addProperty(String key, String value) {
        super.addProperty(prefix + key, value);
        return this;
    }

    public enum Direction {
        INCOMING("incoming"),
        OUTGOING("outgoing");

        private String value;

        private Direction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
