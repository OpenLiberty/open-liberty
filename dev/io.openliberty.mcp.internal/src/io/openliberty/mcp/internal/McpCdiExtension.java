/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.encoders.EncoderRegistries;
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.exceptions.GenericArgumentException;
import io.openliberty.mcp.internal.exceptions.UnsupportedTypeException;
import io.openliberty.mcp.internal.moduleScope.ModuleContext;
import io.openliberty.mcp.internal.requests.BuiltinDefaultValueConverters;
import io.openliberty.mcp.internal.requests.McpRequestIdDeserializer;
import io.openliberty.mcp.internal.requests.McpRequestIdSerializer;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.internal.tools.BeanMethodHandler.MethodMetadata;
import io.openliberty.mcp.messaging.Encoder;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/**
 * Finds tools
 */

public class McpCdiExtension implements Extension {

    private static final TraceComponent tc = Tr.register(McpCdiExtension.class);
    private static final ServiceCaller<CDIService> CDI_SERVICE = new ServiceCaller<>(McpCdiExtension.class, CDIService.class);

    private final List<Bean<?>> encoderBeans = new ArrayList<>();
    private final Map<Bean<?>, Type> converterBeans = new HashMap<>();
    private ConcurrentHashMap<J2EEName, Map<String, ArrayList<String>>> duplicateToolsMap = new ConcurrentHashMap<>();

    private SchemaRegistry schemas = new SchemaRegistry();
    private ConverterRegistry converterRegistry;
    private Jsonb jsonb = createJsonb();
    private ToolRegistries toolRegistries = new ToolRegistries(schemas, jsonb);
    private ModuleContext moduleContext;

    private static Jsonb createJsonb() {
        JsonbConfig jsonbConfig = new JsonbConfig().withSerializers(new McpRequestIdSerializer())
                                                   .withDeserializers(new McpRequestIdDeserializer());

        return JsonbBuilder.create(jsonbConfig);
    }

    void addModuleContext(@Observes AfterBeanDiscovery abd) {
        moduleContext = new ModuleContext();
        abd.addContext(moduleContext);
    }

    void endModuleContext(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        if (moduleContext != null) {
            moduleContext.shutdown();
        }
    }

    void registerTools(@Observes ProcessManagedBean<?> pmb, BeanManager beanManager) {
        AnnotatedType<?> type = pmb.getAnnotatedBeanClass();
        for (AnnotatedMethod<?> m : type.getMethods()) {
            Tool toolAnnotation = m.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                registerTool(toolAnnotation, pmb.getBean(), m, beanManager);
            }
        }
    }

    void discoverEncoderBeans(@Observes ProcessManagedBean<?> processManagedBean) {
        AnnotatedType<?> type = processManagedBean.getAnnotatedBeanClass();
        Class<?> javaClass = type.getJavaClass();
        if (Encoder.class.isAssignableFrom(javaClass)) {
            encoderBeans.add(processManagedBean.getBean());
        }
    }

    void discoverConverterBeans(@Observes ProcessManagedBean<?> processManagedBean) {
        Class<?> javaClass = processManagedBean.getAnnotatedBeanClass().getJavaClass();
        if (DefaultValueConverter.class.isAssignableFrom(javaClass)) {
            TypeUtility.getDefaultValueConverterType(javaClass).ifPresent(type -> converterBeans.put(processManagedBean.getBean(), type));
        }
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager manager) {
        registerEncoders(manager); // Called once with all encoder beans available
        registerCustomConverters(manager); // Called once with all Converter beans available

        boolean error = false;

        for (ToolRegistry toolRegistry : toolRegistries.getAll()) {
            error |= reportOnInvalidToolNames(afterDeploymentValidation, toolRegistry) |
                     reportOnDuplicateTools(afterDeploymentValidation, toolRegistry) |
                     reportOnToolArgEdgeCases(afterDeploymentValidation, toolRegistry) |
                     reportOnDuplicateSpecialArguments(afterDeploymentValidation, toolRegistry) |
                     reportOnInvalidSpecialArguments(afterDeploymentValidation, toolRegistry);
        }

        if (error) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0005E.validation.error")));
        }
    }

    /**
     * Registers all discovered encoder beans to their appropriate encoder registries.
     *
     * <p>This method handles encoder registration.
     * Encoders must be routed to the correct registry based on their module of origin.
     *
     * <p><b>Encoder Types and Scopes:</b>
     * <ul>
     * <li><b>Global encoders</b> - Beans from EAR/lib or runtime (no module association).
     * Registered to the Global registry and accessible to all modules.</li>
     * <li><b>Module encoders</b> - Beans from specific WAR or EJB modules.
     * Registered to their module's registry and isolated from other modules.</li>
     * </ul>
     *
     *
     * @param beanManager The CDI BeanManager for obtaining encoder bean references
     */
    void registerEncoders(BeanManager beanManager) {
        // we cannot inject into an extension so retrieve encoderRegistries via the beanManager
        EncoderRegistries encoderRegistries = beanManager.createInstance().select(EncoderRegistries.class).get();
        CreationalContext<?> context = beanManager.createCreationalContext(null);

        // Group encoders by module (null = global)
        Map<J2EEName, List<ToolResponseEncoder<?>>> toolEncoders = new HashMap<>();
        Map<J2EEName, List<ContentEncoder<?>>> contentEncoders = new HashMap<>();

        // Collect and classify all encoder beans
        for (Bean<?> bean : encoderBeans) {
            J2EEName module = getModuleForBeanOrNull(bean);
            Object encoder = beanManager.getReference(bean, bean.getBeanClass(), context);

            if (encoder instanceof ToolResponseEncoder<?> tre) {
                toolEncoders.computeIfAbsent(module, k -> new ArrayList<>()).add(tre);
            } else if (encoder instanceof ContentEncoder<?> ce) {
                contentEncoders.computeIfAbsent(module, k -> new ArrayList<>()).add(ce);
            }
            logEncoderRegistration(bean, module);
        }

        // Register global encoders (module = null)
        EncoderRegistry globalInstance = encoderRegistries.getGlobal();
        globalInstance.registerEncoders(toolEncoders.getOrDefault(null, new ArrayList<>()),
                                        contentEncoders.getOrDefault(null, new ArrayList<>()));

        // Register module-specific encoders
        Set<J2EEName> modules = new HashSet<>();
        modules.addAll(toolEncoders.keySet());
        modules.addAll(contentEncoders.keySet());
        modules.remove(null); // Already handled global

        for (J2EEName module : modules) {
            EncoderRegistry moduleInstance = encoderRegistries.getForModule(module);
            moduleInstance.registerEncoders(toolEncoders.getOrDefault(module, new ArrayList<>()),
                                            contentEncoders.getOrDefault(module, new ArrayList<>()));
        }

        context.release();
    }

    private static void logEncoderRegistration(Bean<?> encoderBean, J2EEName moduleName) {
        if (TraceComponent.isAnyTracingEnabled()) {
            String scope = (moduleName == null) ? "GLOBAL (EAR/lib)" : "MODULE (" + moduleName + ")";
            String beanName = encoderBean.getName() != null ? encoderBean.getName() : encoderBean.getBeanClass().getSimpleName();
            if (tc.isDebugEnabled()) {
                Tr.debug(McpCdiExtension.class, tc, "Registered encoder [" + scope + "]: " + beanName, encoderBean);
            } else if (tc.isEventEnabled()) {
                Tr.event(McpCdiExtension.class, tc, "Registered encoder [" + scope + "]: " + beanName);
            }
        }
    }

    void registerCustomConverters(BeanManager beanManager) {
        converterRegistry = beanManager.createInstance().select(ConverterRegistry.class).get();
        CreationalContext<?> context = beanManager.createCreationalContext(null);

        Map<Type, List<DefaultValueConverter<?>>> converterMap = new HashMap<>();

        // Populate converters registry with converters for wrapper types and strings
        for (Map.Entry<Type, DefaultValueConverter<?>> entry : BuiltinDefaultValueConverters.CONVERTERS.entrySet()) {
            converterMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        }
        // Populate converters registry with provided customer converters
        for (Map.Entry<Bean<?>, Type> entry : converterBeans.entrySet()) {
            Bean<?> bean = entry.getKey();
            Type converterType = entry.getValue();
            DefaultValueConverter<?> converter = (DefaultValueConverter<?>) beanManager.getReference(bean, bean.getBeanClass(), context);
            converterMap.computeIfAbsent(converterType, k -> new ArrayList<>()).add(converter);
            logCustomConverterRegistration(bean);
        }

        converterRegistry.registerConverters(converterMap, context);
        // Set on ALL module registries until ConverterRegistries are created
        // see EncoderRegistries.java for the pattern to follow
        for (ToolRegistry toolRegistry : toolRegistries.getAll()) {
            toolRegistry.setConverterRegistry(converterRegistry);
        }
    }

    private static void logCustomConverterRegistration(Bean<?> converterBean) {
        if (TraceComponent.isAnyTracingEnabled()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(McpCdiExtension.class, tc, "Registered converter: " + converterBean.getName(), converterBean);
            } else if (tc.isEventEnabled()) {
                Tr.event(McpCdiExtension.class, tc, "Registered converter: " + converterBean.getName());
            }
        }
    }

    /**
     * @param afterDeploymentValidation
     */
    private boolean reportOnToolArgEdgeCases(AfterDeploymentValidation afterDeploymentValidation, ToolRegistry tools) {
        boolean foundErrors = false;

        for (ToolMetadata tool : tools.getAllTools()) {
            Set<String> names = new HashSet<>();

            for (ToolArgument argMetadata : tool.arguments()) {
                for (var error : ToolValidation.validateToolArgument(argMetadata, converterRegistry)) {
                    switch (error.type()) {
                        case NAME_BLANK -> Tr.error(tc, "CWMCM0001E.blank.arguments", tool.getToolQualifiedName());
                        case NAME_MISSING -> Tr.error(tc, "CWMCM0003E.missing.tool.argument.name", tool.getToolQualifiedName());
                        case NO_CONVERTER -> Tr.error(tc, "CWMCM0017E.missing.toolarg.defaultvalue.converter", tool.getToolQualifiedName(), argMetadata.name(), argMetadata.type());
                        case CONVERSION_ERROR -> Tr.error(tc, "CWMCM0020E.defaultvalue.conversion.error", tool.getToolQualifiedName(), argMetadata.name(), argMetadata.type(),
                                                          argMetadata.defaultValue(), error.exception());
                    }
                    foundErrors = true;
                }

                if (!names.add(argMetadata.name())) {
                    Tr.error(tc, "CWMCM0002E.duplicate.arguments", tool.getToolQualifiedName(), argMetadata.name());
                    foundErrors = true;
                }
            }
        }

        return foundErrors;
    }

    private boolean reportOnDuplicateTools(AfterDeploymentValidation afterDeploymentValidation, ToolRegistry tools) {
        boolean error = false;
        for (var moduleDuplicateToolsMap : duplicateToolsMap.values()) {
            // prune items that are not duplicates
            moduleDuplicateToolsMap.entrySet().removeIf(e -> e.getValue().size() == 1);
            for (String toolName : moduleDuplicateToolsMap.keySet()) {
                error = true;
                List<String> qualifiedNames = moduleDuplicateToolsMap.get(toolName);
                Tr.error(tc, "CWMCM0004E.duplicate.tools", toolName, String.join(",", qualifiedNames));
            }
        }
        return error;

    }

    private boolean reportOnInvalidToolNames(AfterDeploymentValidation afterDeploymentValidation, ToolRegistry tools) {
        boolean hasErrors = false;
        for (ToolMetadata tool : tools.getAllTools()) {
            for (var error : ToolValidation.validateToolName(tool.name())) {
                hasErrors = true;
                switch (error) {
                    case INVALID_CHARACTERS -> Tr.error(tc, "CWMCM0024E.invalid.character.tool.name", tool.name(), tool.getToolQualifiedName());
                    case INVALID_LENGTH -> Tr.error(tc, "CWMCM0023E.invalid.length.tool.name", tool.name(), tool.getToolQualifiedName());
                }
            }
        }
        return hasErrors;
    }

    private boolean reportOnDuplicateSpecialArguments(AfterDeploymentValidation afterDeploymentValidation, ToolRegistry tools) {
        AtomicBoolean error = new AtomicBoolean(false);
        for (ToolMetadata tool : tools.getAllTools()) {
            if (tool.methodMetadata().isEmpty()) {
                continue;
            }
            MethodMetadata methodMetadata = tool.methodMetadata().get();
            Map<SpecialArgumentType.Resolution, Integer> resultCountMap = new HashMap<>();
            for (SpecialArgumentMetadata specialArgument : methodMetadata.specialArguments()) {
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

    private boolean reportOnInvalidSpecialArguments(AfterDeploymentValidation afterDeploymentValidation, ToolRegistry tools) {
        boolean error = false;
        for (ToolMetadata tool : tools.getAllTools()) {
            if (tool.methodMetadata().isEmpty()) {
                continue;
            }
            for (SpecialArgumentMetadata specialArgument : tool.methodMetadata().get().specialArguments()) {
                if (specialArgument.typeResolution().specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    error = true;
                    Tr.error(tc, "CWMCM0007E.invalid.arguments", tool.getToolQualifiedName(),
                             specialArgument.typeResolution());
                }
            }
        }
        return error;
    }

    private void registerTool(Tool tool, Bean<?> bean, AnnotatedMethod<?> method, BeanManager beanManager) {
        try {
            ToolMetadata toolmd = ToolMetadata.createFrom(tool, bean, method, beanManager, jsonb);
            J2EEName module = getModuleForBean(bean);
            List<String> duplicatesList = duplicateToolsMap.computeIfAbsent(module, key -> new HashMap<>())
                                                           .computeIfAbsent(toolmd.name(), key -> new ArrayList<>());
            duplicatesList.add(toolmd.getToolQualifiedName());
            if (duplicatesList.size() <= 1) {
                toolRegistries.getForModule(module).addTool(toolmd);
                if (TraceComponent.isAnyTracingEnabled()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Registered tool: " + toolmd.name(), toolmd);
                    } else if (tc.isEventEnabled()) {
                        Tr.event(this, tc, "Registered tool: " + toolmd.name(), method);
                    }
                }
            }
        } catch (GenericArgumentException e) {
            for (String argument : e.getArguments()) {
                Tr.error(tc, "CWMCM0018E.generic.arguments", ToolMetadata.getToolQualifiedName(bean, method), argument);
            }
        } catch (UnsupportedTypeException e) {
            Tr.error(tc, "CWMCM0025E.unsupported.output", e.getType(), ToolMetadata.getToolQualifiedName(bean, method));
        }
    }

    public ToolRegistry getCurrentToolRegistry() {
        return toolRegistries.getCurrent();
    }

    public SchemaRegistry getSchemaRegistry() {
        return schemas;
    }

    public Jsonb getJsonb() {
        return jsonb;
    }

    private J2EEName getModuleForBean(Bean<?> bean) {
        J2EEName moduleName = CDI_SERVICE.run(cdiService -> cdiService.getModuleNameForClass(bean.getBeanClass()))
                                         .orElseThrow(() -> new RuntimeException("No current CDIService"))
                                         .orElseThrow(() -> new RuntimeException("No module for bean " + bean));
        return moduleName;
    }

    /**
     * Determines the J2EE module (J2EEName) that owns a CDI bean, or null if the bean has no module association.
     *
     *
     * @param bean The CDI bean to check
     * @return The J2EEName of the bean's module, or null if the bean has no module
     * @throws RuntimeException if CDI service is unavailable (system error)
     */
    private J2EEName getModuleForBeanOrNull(Bean<?> bean) {
        // Get the Optional<Optional<J2EEName>> from CDI service
        // Outer Optional: CDI service availability
        // Inner Optional: Module name for bean class
        return CDI_SERVICE.run(cdiService -> cdiService.getModuleNameForClass(bean.getBeanClass()))
                          .orElseThrow(() -> new RuntimeException("No current CDIService"))
                          .orElse(null); // No module = global bean (EAR/lib or runtime)
    }
}
