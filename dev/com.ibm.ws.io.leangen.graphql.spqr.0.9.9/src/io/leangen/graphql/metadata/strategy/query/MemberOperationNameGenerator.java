package io.leangen.graphql.metadata.strategy.query;

public class MemberOperationNameGenerator extends DefaultOperationNameGenerator {

    public MemberOperationNameGenerator() {
        withDelegate(new AnnotatedOperationNameGenerator());
    }
}
