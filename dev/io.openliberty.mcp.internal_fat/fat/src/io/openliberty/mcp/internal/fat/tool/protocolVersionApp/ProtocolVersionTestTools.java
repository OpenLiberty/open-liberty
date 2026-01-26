/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.protocolVersionApp;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.tools.ToolResponse;
import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProtocolVersionTestTools {

    public record City(String name, String country, int population, boolean isCapital) {};

    @Tool(name = "testListObjectResponse", title = "City List",
          description = "A tool to return a list of cities", structuredContent = true)
    public City testListObjectResponse() {
        City city1 = new City("Paris", "France", 8000, true);
        return city1;

    }

    public record MyResponseObject(String content) {};

    /********************************************************************************************
     * Encode MyResponseObject as a response with just structured content and no regular content
     *
     ********************************************************************************************/

    public static class MyResponseObjectEncoder implements ToolResponseEncoder<MyResponseObject> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return MyResponseObject.class.isAssignableFrom(runtimeType);
        }

        @Override
        public ToolResponse encode(MyResponseObject value) {
            return new ToolResponse(false, null, value, null);
        }

    }

    @Tool(name = "testToolResponseNoContent")
    public MyResponseObject testToolResponseNoContent() {
        return new MyResponseObject("Hello World");
    }

}
