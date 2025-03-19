/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.fat.versionwarning.app;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.DependentRequired;
import org.eclipse.microprofile.openapi.annotations.media.DependentSchema;
import org.eclipse.microprofile.openapi.annotations.media.PatternProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningDataObject.SubschemaA;
import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningDataObject.SubschemaB;
import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningDataObject.SubschemaC;

@Schema(comment = "test comment",
        constValue = "3",
        ifSchema = SubschemaA.class,
        thenSchema = SubschemaB.class,
        elseSchema = SubschemaC.class,
        dependentSchemas = @DependentSchema(name = "test", schema = SubschemaC.class),
        patternProperties = @PatternProperty(regex = "testPattern", schema = SubschemaC.class),
        dependentRequired = @DependentRequired(name = "testA", requires = { "testB", "testC" }),
        propertyNames = SubschemaB.class)
public class VersionWarningDataObject {

    public static class SubschemaA {}

    public static class SubschemaB {}

    public static class SubschemaC {}

    @Schema(contains = SubschemaC.class, minContains = 1, maxContains = 6, prefixItems = { SubschemaA.class, SubschemaA.class })
    private List<String> testList;

    @Schema(contentEncoding = "base64", contentMediaType = "application/json", contentSchema = SubschemaC.class)
    private String encodedTest;
}
