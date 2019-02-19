package io.leangen.graphql.generator.mapping.strategy;

import io.github.classgraph.ClassInfo;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.leangen.graphql.util.ClassFinder.ALL;
import static io.leangen.graphql.util.ClassFinder.NON_IGNORED;
import static io.leangen.graphql.util.ClassFinder.PUBLIC;

public class DefaultImplementationDiscoveryStrategy implements ImplementationDiscoveryStrategy {

    private final List<Predicate<ClassInfo>> filters;

    public DefaultImplementationDiscoveryStrategy() {
        this.filters = new ArrayList<>();
        this.filters.add(PUBLIC);
    }

    @Override
    public List<AnnotatedType> findImplementations(AnnotatedType type, String[] scanPackages, BuildContext buildContext) {
        if (Utils.isArrayEmpty(scanPackages) && Utils.isArrayNotEmpty(buildContext.basePackages)) {
            scanPackages = buildContext.basePackages;
        }
        Predicate<ClassInfo> filter = NON_IGNORED.and(filters.stream().reduce(Predicate::and).orElse(ALL));
        return buildContext.classFinder.findImplementations(type, filter, scanPackages).stream()
                .filter(impl -> !ClassUtils.isMissingTypeParameters(impl.getType()))
                .collect(Collectors.toList());
    }

    public DefaultImplementationDiscoveryStrategy withNonPublicClasses() {
        this.filters.remove(PUBLIC);
        return this;
    }

    @SafeVarargs
    public final DefaultImplementationDiscoveryStrategy withFilters(Predicate<ClassInfo>... filters) {
        Collections.addAll(this.filters, filters);
        return this;
    }
}
