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

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.PathItem;
import org.eclipse.microprofile.openapi.annotations.PathItemOperation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.media.DependentRequired;
import org.eclipse.microprofile.openapi.annotations.media.DependentSchema;
import org.eclipse.microprofile.openapi.annotations.media.PatternProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningDataObject.SubschemaA;
import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningDataObject.SubschemaB;
import io.openliberty.microprofile.openapi40.fat.versionwarning.app.VersionWarningDataObject.SubschemaC;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
@OpenAPIDefinition(info = @Info(title = "test", version = "0.1", summary = "test",
                                license = @License(name = "Example License 1.0", identifier = "EXAMPLE-1.0")),
                   components = @Components(pathItems = @PathItem(name = "testPathItem",
                                                                  operations = @PathItemOperation(method = "GET",
                                                                                                  description = "example callback operation")),
                                            schemas = @Schema(name = "componentSchema",
                                                              properties = { @SchemaProperty(name = "prop1",
                                                                                             type = SchemaType.OBJECT,
                                                                                             comment = "testComment",
                                                                                             additionalProperties = VersionWarningDataObject.class,
                                                                                             ifSchema = SubschemaA.class,
                                                                                             thenSchema = SubschemaB.class,
                                                                                             elseSchema = SubschemaC.class,
                                                                                             dependentSchemas = @DependentSchema(name = "prop2",
                                                                                                                                 schema = SubschemaA.class)),
                                                                             @SchemaProperty(name = "prop2",
                                                                                             contains = SubschemaA.class,
                                                                                             minContains = 1,
                                                                                             maxContains = 6,
                                                                                             prefixItems = { SubschemaB.class,
                                                                                                             SubschemaB.class }),
                                                                             @SchemaProperty(name = "prop3",
                                                                                             type = SchemaType.OBJECT,
                                                                                             patternProperties = @PatternProperty(regex = "test\\..*",
                                                                                                                                  schema = SubschemaA.class),
                                                                                             dependentRequired = @DependentRequired(name = "prop2",
                                                                                                                                    requires = "prop3"),
                                                                                             propertyNames = SubschemaC.class),
                                                                             @SchemaProperty(name = "prop4",
                                                                                             type = SchemaType.STRING,
                                                                                             contentEncoding = "base64",
                                                                                             contentMediaType = "application/json",
                                                                                             contentSchema = SubschemaC.class)
                                                              })),
                   webhooks = @PathItem(name = "testWebhook"))
public class VersionWarningApplication extends Application {

}
