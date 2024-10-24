/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                CallbacksValidatorTest.class,
                ComponentsValidatorTest.class,
                ContactValidatorTest.class,
                DiscriminatorValidatorTest.class,
                ExampleValidatorTest.class,
                ExtensionValidatorTest.class,
                ExternalDocumentationValidatorTest.class,
                HeaderValidatorTest.class,
                InfoValidatorTest.class,
                LicenseValidatorTest.class,
                LinkValidatorTest.class,
                MediaTypeValidatorTest.class,
                OAuthFlowsValidatorTest.class,
                OAuthFlowValidatorTest.class,
                OpenAPIRuntimeExpressionTest.class,
                OpenAPIValidatorTest.class,
                OperationValidatorTest.class,
                ParameterValidatorTest.class,
                PathItemValidatorTest.class,
                PathsValidatorTest.class,
                ReferenceValidatorTest.class,
                RequestBodyValidatorTest.class,
                ResponsesValidatorTest.class,
                ResponseValidatorTest.class,
                SchemaValidatorTest.class,
                SecurityRequirementValidatorTest.class,
                SecuritySchemeValidatorTest.class,
                ServerValidatorTest.class,
                ServerVariableValidatorTest.class,
                TagValidatorTest.class
})
public class ValidationTests {}
