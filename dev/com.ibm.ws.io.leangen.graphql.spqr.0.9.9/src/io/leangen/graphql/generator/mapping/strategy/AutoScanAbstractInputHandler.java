package io.leangen.graphql.generator.mapping.strategy;

import io.github.classgraph.ClassInfo;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.leangen.graphql.util.ClassFinder.ALL;
import static io.leangen.graphql.util.ClassFinder.CONCRETE;
import static io.leangen.graphql.util.ClassFinder.NON_IGNORED;
import static io.leangen.graphql.util.ClassFinder.PUBLIC;

public class AutoScanAbstractInputHandler implements AbstractInputHandler {

    private final Map<Type, Set<Type>> abstractComponents = new HashMap<>();
    private final List<Predicate<ClassInfo>> filters;

    private static final Logger log = LoggerFactory.getLogger(AutoScanAbstractInputHandler.class);

    public AutoScanAbstractInputHandler() {
        this.filters = new ArrayList<>();
        this.filters.add(PUBLIC);
    }

    @Override
    public Set<Type> findConstituentAbstractTypes(AnnotatedType javaType, BuildContext buildContext) {
        if (Scalars.isScalar(javaType.getType())
                || ClassUtils.isSubPackage(ClassUtils.getRawType(javaType.getType()).getPackage(), "java.")
                || buildContext.scalarStrategy.isDirectlyDeserializable(javaType)) {
            return Collections.emptySet();
        }
        if (javaType instanceof AnnotatedParameterizedType) {
            Set<Type> abstractTypes = Arrays.stream(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments())
                    .flatMap(arg -> findConstituentAbstractTypes(arg, buildContext).stream())
                    .collect(Collectors.toSet());
            abstractTypes.addAll(findAbstract(javaType, buildContext));
            return abstractTypes;
        }
        if (javaType instanceof AnnotatedArrayType) {
            return findConstituentAbstractTypes(((AnnotatedArrayType) javaType).getAnnotatedGenericComponentType(), buildContext);
        }
        if (javaType instanceof AnnotatedWildcardType || javaType instanceof AnnotatedTypeVariable) {
            throw new TypeMappingException(javaType.getType());
        }
        return findAbstract(javaType, buildContext);
    }

    @Override
    public List<Class<?>> findConcreteSubTypes(Class abstractType, BuildContext buildContext) {
        Predicate<ClassInfo> filter = CONCRETE.and(NON_IGNORED).and(filters.stream().reduce(Predicate::and).orElse(ALL));
        List<Class<?>> subTypes = buildContext.classFinder.findImplementations(abstractType, filter, buildContext.basePackages);
        if (subTypes.isEmpty()) {
            log.warn("No concrete subtypes of " + abstractType.getName() + " found");
        }
        return subTypes;
    }

    public AutoScanAbstractInputHandler withNonPublicClasses() {
        this.filters.remove(PUBLIC);
        return this;
    }

    @SafeVarargs
    public final AutoScanAbstractInputHandler withFilters(Predicate<ClassInfo>... filters) {
        Collections.addAll(this.filters, filters);
        return this;
    }

    private Set<Type> findAbstract(AnnotatedType javaType, BuildContext buildContext) {
        if (abstractComponents.get(javaType.getType()) != null) {
            return abstractComponents.get(javaType.getType());
        }
        if (abstractComponents.containsKey(javaType.getType())) {
            return Collections.emptySet();
        }
        abstractComponents.put(javaType.getType(), null);
        Set<Type> abstractTypes = new HashSet<>();
        if (ClassUtils.isAbstract(javaType)) {
            abstractTypes.add(javaType.getType());
        }
        buildContext.inputFieldBuilders.getInputFields(
                InputFieldBuilderParams.builder()
                        .withType(javaType)
                        .withEnvironment(buildContext.globalEnvironment)
                        .build())
                .forEach(childQuery -> abstractTypes.addAll(findConstituentAbstractTypes(childQuery.getDeserializableType(), buildContext)));
        abstractComponents.put(javaType.getType(), abstractTypes);
        return abstractTypes;
    }
}
