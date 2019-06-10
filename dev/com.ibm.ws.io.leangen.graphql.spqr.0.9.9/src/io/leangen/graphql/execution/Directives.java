package io.leangen.graphql.execution;

import graphql.analysis.QueryTraversal;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ValuesResolver;
import graphql.introspection.Introspection;
import graphql.language.Directive;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import io.leangen.graphql.util.GraphQLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Directives {

    private Map<Introspection.DirectiveLocation, Map<String, List<Map<String, Object>>>> directives = new HashMap<>();

    private static final ValuesResolver valuesResolver = new ValuesResolver();

    Directives(DataFetchingEnvironment env, ExecutionStepInfo step) {
        List<Field> fields = env.getFields();
        if (step != null) {
            fields = step.getField() != null ? Collections.singletonList(step.getField()) : Collections.emptyList();
        }
        step = step != null ? step : env.getExecutionStepInfo();
        // Field directives
        fields.forEach(field ->
                directives.merge(Introspection.DirectiveLocation.FIELD, parseDirectives(field.getDirectives(), env), (directiveMap1, directiveMap2) -> {
                    directiveMap2.forEach((directiveName, directiveValues) -> directiveMap1.merge(directiveName, directiveValues,
                            (valueList1, valueList2) -> Stream.concat(valueList1.stream(), valueList2.stream()).collect(Collectors.toList()))
                    );
                    return directiveMap1;
                }));

        // Operation directives
        Map<String, List<Map<String, Object>>> operationDirectives = parseDirectives(env.getExecutionContext().getOperationDefinition().getDirectives(), env);
        if (OperationDefinition.Operation.MUTATION.equals(env.getExecutionContext().getOperationDefinition().getOperation())) {
            directives.put(Introspection.DirectiveLocation.MUTATION, operationDirectives);
            directives.put(Introspection.DirectiveLocation.QUERY, Collections.emptyMap());
        } else {
            directives.put(Introspection.DirectiveLocation.QUERY, operationDirectives);
            directives.put(Introspection.DirectiveLocation.MUTATION, Collections.emptyMap());
        }

        // Fragment directives
        if (step.hasParent() && step.getParent().getField() != null) {
            FragmentDirectiveCollector fragmentDirectiveCollector = FragmentDirectiveCollector.collect(env, step);
            directives.put(Introspection.DirectiveLocation.INLINE_FRAGMENT, parseDirectives(fragmentDirectiveCollector.getInlineFragmentDirs(), env));
            directives.put(Introspection.DirectiveLocation.FRAGMENT_SPREAD, parseDirectives(fragmentDirectiveCollector.getFragmentDirs(), env));
            directives.put(Introspection.DirectiveLocation.FRAGMENT_DEFINITION, parseDirectives(fragmentDirectiveCollector.getFragmentDefDirs(), env));
        } else {
            directives.put(Introspection.DirectiveLocation.INLINE_FRAGMENT, Collections.emptyMap());
            directives.put(Introspection.DirectiveLocation.FRAGMENT_SPREAD, Collections.emptyMap());
            directives.put(Introspection.DirectiveLocation.FRAGMENT_DEFINITION, Collections.emptyMap());
        }
    }

    private Map<String, List<Map<String, Object>>> parseDirectives(List<Directive> directives, DataFetchingEnvironment env) {
        return directives.stream().collect(
                Collectors.groupingBy(Directive::getName, Collectors.mapping(dir -> parseDirective(dir, env), Collectors.toList())));
    }

    private Map<String, Object> parseDirective(Directive dir, DataFetchingEnvironment env) {
        GraphQLDirective directive = env.getExecutionContext().getGraphQLSchema().getDirective(dir.getName());
        if (directive == null) {
            return null;
        }
        return Collections.unmodifiableMap(
                valuesResolver.getArgumentValues(env.getGraphQLSchema().getFieldVisibility(), directive.getArguments(),
                        dir.getArguments(), env.getExecutionContext().getVariables()));
    }

    Map<Introspection.DirectiveLocation, Map<String, List<Map<String, Object>>>>  getDirectives() {
        return directives;
    }

    public List<Map<String, Object>> find(Introspection.DirectiveLocation location, String directiveName) {
        return getDirectives().get(location).get(directiveName);
    }

    private static class FragmentDirectiveCollector extends QueryVisitorStub {

        private final List<Directive> inlineFragmentDirs;
        private final List<Directive> fragmentDirs;
        private final List<Directive> fragmentDefDirs;
        private final List<Field> fieldsToFind;
        private final Set<Node> relevantFragments;

        private FragmentDirectiveCollector(DataFetchingEnvironment env) {
            this.inlineFragmentDirs = new ArrayList<>();
            this.fragmentDirs = new ArrayList<>();
            this.fragmentDefDirs = new ArrayList<>();
            this.fieldsToFind = env.getFields();
            this.relevantFragments = new HashSet<>();
        }

        public static FragmentDirectiveCollector collect(DataFetchingEnvironment env, ExecutionStepInfo step) {
            FragmentDirectiveCollector fragmentDirectiveCollector = new FragmentDirectiveCollector(env);
            // This is safe because top-level fields don't get to here and all deeper fields at least have a parent (source object) and a grand-parent (query root)
            ExecutionStepInfo rootStep = step.getParent().getParent();
            if (rootStep == null) { //Should never be possible, see above
                return fragmentDirectiveCollector;
            }
            GraphQLType rootParentType = GraphQLUtils.unwrapNonNull(rootStep.getType());
            while(!(rootParentType instanceof GraphQLObjectType)) {
                rootStep = rootStep.getParent();
                rootParentType = GraphQLUtils.unwrapNonNull(rootStep.getType());
            }
            QueryTraversal traversal = QueryTraversal.newQueryTraversal()
                    .fragmentsByName(env.getExecutionContext().getFragmentsByName())
                    .schema(env.getGraphQLSchema())
                    .variables(env.getExecutionContext().getVariables())
                    .root(env.getExecutionStepInfo().getParent().getField())
                    .rootParentType((GraphQLObjectType) rootParentType)
                    .build();
            traversal.visitPostOrder(fragmentDirectiveCollector);
            return fragmentDirectiveCollector;
        }

        @Override
        public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment env) {
            InlineFragment fragment = env.getInlineFragment();
            boolean containsField = fieldsToFind.stream().anyMatch(field -> fragment.getSelectionSet().getSelections().contains(field));
            boolean isRelevant = containsField || relevantFragments.stream().anyMatch(frag -> fragment.getSelectionSet().getSelections().contains(frag));
            if (isRelevant) {
                relevantFragments.add(fragment);
                inlineFragmentDirs.addAll(fragment.getDirectives());
            }
        }

        @Override
        public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment env) {
            boolean containsField = fieldsToFind.stream()
                    .anyMatch(field -> env.getFragmentDefinition().getSelectionSet().getSelections().contains(field));
            boolean isRelevant = containsField || relevantFragments.stream().anyMatch(frag -> env.getFragmentDefinition().getSelectionSet().getSelections().contains(frag));
            if (isRelevant) {
                relevantFragments.add(env.getFragmentSpread());
                fragmentDirs.addAll(env.getFragmentSpread().getDirectives());
                fragmentDefDirs.addAll(env.getFragmentDefinition().getDirectives());
            }
        }

        List<Directive> getInlineFragmentDirs() {
            return inlineFragmentDirs;
        }

        List<Directive> getFragmentDirs() {
            return fragmentDirs;
        }

        List<Directive> getFragmentDefDirs() {
            return fragmentDefDirs;
        }
    }
}
