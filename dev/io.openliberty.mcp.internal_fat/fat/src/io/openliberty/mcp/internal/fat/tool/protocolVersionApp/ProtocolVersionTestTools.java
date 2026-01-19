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

import java.util.List;

import io.openliberty.mcp.annotations.Tool;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProtocolVersionTestTools {

    public record City(String name, String country, int population, boolean isCapital) {};

    @Tool(name = "testListObjectResponse", title = "City List",
          description = "A tool to return a list of cities", structuredContent = true)
    public List<City> testListObjectResponse() {
        City city1 = new City("Paris", "France", 8000, true);
        City city2 = new City("Manchester", "England", 15000, false);
        return List.of(city1, city2);
    }

}
