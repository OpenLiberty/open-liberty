/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;

/**
 * Finds tools
 */

public class McpCdiExtension implements Extension {

    private static final TraceComponent tc = Tr.register(McpCdiExtension.class);

    private ToolRegistry tools = new ToolRegistry();
    private ConcurrentHashMap<String, LinkedList<String>> duplicateToolsMap = new ConcurrentHashMap<>();

    private SchemaRegistry schemas = new SchemaRegistry();

    void registerTools(@Observes ProcessManagedBean<?> pmb) {
        AnnotatedType<?> type = pmb.getAnnotatedBeanClass();
        Class<?> javaClass = type.getJavaClass();
        for (AnnotatedMethod<?> m : type.getMethods()) {
            Tool toolAnnotation = m.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                registerTool(toolAnnotation, pmb.getBean(), m);
            }
        }
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager manager) {
        boolean error = reportOnDuplicateTools(afterDeploymentValidation) | reportOnToolArgEdgeCases(afterDeploymentValidation) |
                        reportOnDuplicateSpecialArguments(afterDeploymentValidation) | reportOnInvalidSpecialArguments(afterDeploymentValidation);
        if (error) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0005E.validation.error")));
        }
    }

    /**
     * @param afterDeploymentValidation
     */
    private boolean reportOnToolArgEdgeCases(AfterDeploymentValidation afterDeploymentValidation) {
        boolean blankArgumentsFound = false;
        boolean duplicateArgumentsFound = false;
        boolean missingArgumentName = false;

        for (ToolMetadata tool : tools.getAllTools()) {
            Map<String, ArgumentMetadata> arguments = tool.arguments();

            for (String argName : arguments.keySet()) {
                if (argName.isBlank()) {
                    Tr.error(tc, "CWMCM0001E.blank.arguments", tool.getToolQualifiedName());
                    blankArgumentsFound = true;
                } else if (arguments.get(argName).isDuplicate()) {
                    Tr.error(tc, "CWMCM0002E.duplicate.arguments", tool.getToolQualifiedName(), argName);
                    duplicateArgumentsFound = true;
                } else if (argName.equals(ToolMetadata.MISSING_TOOL_ARG_NAME)) {
                    Tr.error(tc, "CWMCM0003E.missing.tool.argument.name", tool.getToolQualifiedName());
                    missingArgumentName = true;
                }
            }
        }
        return blankArgumentsFound || duplicateArgumentsFound || missingArgumentName;
    }

    private boolean reportOnDuplicateTools(AfterDeploymentValidation afterDeploymentValidation) {
        boolean error = false;
        // prune items that are not duplicates
        duplicateToolsMap.entrySet().removeIf(e -> e.getValue().size() == 1);
        for (String toolName : duplicateToolsMap.keySet()) {
            error = true;
            LinkedList<String> qualifiedNames = duplicateToolsMap.get(toolName);
            Tr.error(tc, "CWMCM0004E.duplicate.tools", toolName, String.join(",", qualifiedNames));
        }
        return error;

    }

    private boolean reportOnDuplicateSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        AtomicBoolean error = new AtomicBoolean(false);
        for (ToolMetadata tool : tools.getAllTools()) {
            Map<SpecialArgumentType.Resolution, Integer> resultCountMap = new HashMap<>();
            for (SpecialArgumentMetadata specialArgument : tool.specialArguments()) {
                SpecialArgumentType.Resolution specialArgumentTypeResolution = specialArgument.typeResolution();
                if (specialArgumentTypeResolution.specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    continue;
                }
                resultCountMap.merge(specialArgumentTypeResolution, 1, Integer::sum);

            }
            resultCountMap.forEach((k, v) -> {
                if (v > 1) {
                    error.set(true);
                    Tr.error(tc, "CWMCM0006E.duplicate.special.arguments", tool.getToolQualifiedName(),
                             k.actualClass().getSimpleName());

                }

            });
        }
        return error.get();

    }

    private boolean reportOnInvalidSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        boolean error = false;
        for (ToolMetadata tool : tools.getAllTools()) {
            for (SpecialArgumentMetadata specialArgument : tool.specialArguments()) {
                if (specialArgument.typeResolution().specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    error = true;
                    Tr.error(tc, "CWMCM0007E.invalid.arguments", tool.getToolQualifiedName(),
                             specialArgument.typeResolution());
                }
            }
        }
        return error;
    }

    private void registerTool(Tool tool, Bean<?> bean, AnnotatedMethod<?> method) {
        ToolMetadata toolmd = ToolMetadata.createFrom(tool, bean, method);
        duplicateToolsMap.computeIfAbsent(toolmd.name(), key -> new LinkedList<>()).add(toolmd.getToolQualifiedName());
        tools.addTool(toolmd);
        if (TraceComponent.isAnyTracingEnabled()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Registered tool: " + toolmd.name(), toolmd);
            } else if (tc.isEventEnabled()) {
                Tr.event(this, tc, "Registered tool: " + toolmd.name(), method);
            }
        }
    }

    public ToolRegistry getToolRegistry() {
        return tools;
    }

    public SchemaRegistry getSchemaRegistry() {
        return schemas;
    }

}
