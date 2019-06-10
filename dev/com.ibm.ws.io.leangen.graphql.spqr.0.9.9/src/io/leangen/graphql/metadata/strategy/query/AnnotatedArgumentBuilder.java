package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.value.DefaultValueProvider;
import io.leangen.graphql.metadata.strategy.value.JsonDefaultValueProvider;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.ReservedStrings;
import io.leangen.graphql.util.Urls;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class AnnotatedArgumentBuilder implements ResolverArgumentBuilder {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedArgumentBuilder.class);

    @Override
    public List<OperationArgument> buildResolverArguments(ArgumentBuilderParams params) {
        Method resolverMethod = params.getResolverMethod();
        List<OperationArgument> operationArguments = new ArrayList<>(resolverMethod.getParameterCount());
        AnnotatedType[] parameterTypes = ClassUtils.getParameterTypes(resolverMethod, params.getDeclaringType());
        for (int i = 0; i < resolverMethod.getParameterCount(); i++) {
            Parameter parameter = resolverMethod.getParameters()[i];
            if (parameter.isSynthetic() || parameter.isImplicit()) continue;
            AnnotatedType parameterType;
            try {
                parameterType = params.getTypeTransformer().transform(parameterTypes[i]);
            } catch (TypeMappingException e) {
                throw new TypeMappingException(resolverMethod, parameter, e);
            }
            operationArguments.add(buildResolverArgument(parameter, parameterType, params.getInclusionStrategy(), params.getEnvironment()));
        }
        return operationArguments;
    }

    protected OperationArgument buildResolverArgument(Parameter parameter, AnnotatedType parameterType,
                                                      InclusionStrategy inclusionStrategy, GlobalEnvironment environment) {

        DefaultValueProvider defaultValueProvider = new JsonDefaultValueProvider(environment);

        return new OperationArgument(
                parameterType,
                getArgumentName(parameter, parameterType, inclusionStrategy, environment.messageBundle),
                getArgumentDescription(parameter, parameterType, environment.messageBundle),
                defaultValue(parameter, parameterType, defaultValueProvider, environment),
                parameter,
                parameter.isAnnotationPresent(Source.class),
                inclusionStrategy.includeArgument(parameter, parameterType)
        );
    }

    protected String getArgumentName(Parameter parameter, AnnotatedType parameterType, InclusionStrategy inclusionStrategy, MessageBundle messageBundle) {
        if (Optional.ofNullable(parameterType.getAnnotation(GraphQLId.class)).filter(GraphQLId::relayId).isPresent()) {
            return GraphQLId.RELAY_ID_FIELD_NAME;
        }
        Argument meta = parameter.getAnnotation(Argument.class);
        if (meta != null && !meta.value().isEmpty()) {
            return messageBundle.interpolate(meta.value());
        } else {
            if (!parameter.isNamePresent() && inclusionStrategy.includeArgument(parameter, parameterType)) {
                log.warn("No explicit argument name given and the parameter name lost in compilation: "
                        + parameter.getDeclaringExecutable().toGenericString() + "#" + parameter.toString()
                        + ". For details and possible solutions see " + Urls.Errors.MISSING_ARGUMENT_NAME);
            }
            return parameter.getName();
        }
    }

    protected String getArgumentDescription(Parameter parameter, AnnotatedType parameterType, MessageBundle messageBundle) {
        Argument meta = parameter.getAnnotation(Argument.class);
        return meta != null ? messageBundle.interpolate(meta.description()) : null;
    }

    protected Object defaultValue(Parameter parameter, AnnotatedType parameterType, DefaultValueProvider defaultValueProvider, GlobalEnvironment environment) {

        DefaultValue meta = parameter.getAnnotation(DefaultValue.class);
        if (meta == null) return null;
        return defaultValueProvider.getDefaultValue(parameter, parameterType, ReservedStrings.decode(environment.messageBundle.interpolate(meta.value())));
    }
}
