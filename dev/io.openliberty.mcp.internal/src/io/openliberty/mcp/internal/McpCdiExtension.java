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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
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
        reportOnDuplicateTools(afterDeploymentValidation);
        reportOnToolArgEdgeCases(afterDeploymentValidation);
        reportOnDuplicateSpecialArguments(afterDeploymentValidation);
        reportOnInvalidSpecialArguments(afterDeploymentValidation);
    }

    /**
     * @param afterDeploymentValidation
     */
    private void reportOnToolArgEdgeCases(AfterDeploymentValidation afterDeploymentValidation) {
        StringBuilder sbBlankArgs = new StringBuilder("Blank arguments found in MCP Tool:");
        StringBuilder sbDuplicateArgs = new StringBuilder("Duplicate arguments found in MCP Tool:");
        boolean blankArgumentsFound = false;
        boolean duplicateArgumentsFound = false;

        for (ToolMetadata tool : tools.getAllTools()) {
            Map<String, ArgumentMetadata> arguments = tool.arguments();

            for (String argName : arguments.keySet()) {
                if (argName.isBlank()) {
                    sbBlankArgs.append("\n").append("Tool: " + tool.getToolQualifiedName());
                    blankArgumentsFound = true;
                } else if (arguments.get(argName).isDuplicate()) {
                    sbDuplicateArgs.append("\n").append("Tool: " + tool.getToolQualifiedName() + " -  Argument: " + argName);
                    duplicateArgumentsFound = true;
                }
            }
        }
        if (blankArgumentsFound) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(sbBlankArgs.toString()));
        }
        if (duplicateArgumentsFound) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(sbDuplicateArgs.toString()));
        }
    }

    private void reportOnDuplicateTools(AfterDeploymentValidation afterDeploymentValidation) {
        // prune items that are not duplicates
        duplicateToolsMap.entrySet().removeIf(e -> e.getValue().size() == 1);
        StringBuilder sb = new StringBuilder("More than one MCP tool has the same name: \n");
        for (String toolName : duplicateToolsMap.keySet()) {
            LinkedList<String> qualifiedNames = duplicateToolsMap.get(toolName);
            sb.append("Tool: ").append(toolName);
            sb.append(" -- Methods found:\n");
            for (String qualifiedName : qualifiedNames) {
                sb.append("    - ").append(qualifiedName + "\n");
            }
        }

        if (duplicateToolsMap.size() != 0) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(sb.toString()));
        }
    }

    private void reportOnDuplicateSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        StringBuilder sbDuplicateSpecialArgs = new StringBuilder("Only 1 instance is allowed, of type: ");
        for (ToolMetadata tool : tools.getAllTools()) {
            Map<SpecialArgumentType.Resolution, Integer> resultCountMap = new HashMap<>();
            for (SpecialArgumentMetadata specialArgument : tool.specialArguments()) {
                SpecialArgumentType.Resolution specialArgumentTypeResolution = specialArgument.typeResolution();
                if (specialArgumentTypeResolution.specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    continue;
                }
                resultCountMap.merge(specialArgumentTypeResolution, 1, Integer::sum);
                if (resultCountMap.get(specialArgumentTypeResolution) > 1) {
                    sbDuplicateSpecialArgs.append(specialArgumentTypeResolution);
                    sbDuplicateSpecialArgs.append("\n  But more than 1 argument was found. Please remove the extra instance, or check if you meant to include @ToolArg to one of them");
                    sbDuplicateSpecialArgs.append("\n").append("Tool: " + tool.getToolQualifiedName());
                    afterDeploymentValidation.addDeploymentProblem(new Exception(sbDuplicateSpecialArgs.toString()));
                }
            }
        }
    }

    private void reportOnInvalidSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        StringBuilder sbInvalidSpecialArgs = new StringBuilder("Special argument type not supported: ");
        for (ToolMetadata tool : tools.getAllTools()) {
            for (SpecialArgumentMetadata specialArgument : tool.specialArguments()) {
                if (specialArgument.typeResolution().specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    sbInvalidSpecialArgs.append(specialArgument.typeResolution());
                    sbInvalidSpecialArgs.append("\n  Please check if you have the correct class imported for your argument, or you meant to include @ToolArg");
                    sbInvalidSpecialArgs.append("\n").append("Tool: " + tool.getToolQualifiedName());
                    afterDeploymentValidation.addDeploymentProblem(new Exception(sbInvalidSpecialArgs.toString()));
                }
            }
        }
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
}
