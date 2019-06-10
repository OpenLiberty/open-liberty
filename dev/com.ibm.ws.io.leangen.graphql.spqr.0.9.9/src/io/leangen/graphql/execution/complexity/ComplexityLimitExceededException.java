package io.leangen.graphql.execution.complexity;

import graphql.execution.AbortExecutionException;

public class ComplexityLimitExceededException extends AbortExecutionException {

    private final int complexity;
    private final int maximumComplexity;

    ComplexityLimitExceededException(int complexity, int maximumComplexity) {
        super("Requested operation exceeds the permitted complexity limit: " + complexity + " > " + maximumComplexity);
        this.complexity = complexity;
        this.maximumComplexity = maximumComplexity;
    }

    public int getComplexity() {
        return complexity;
    }

    public int getMaximumComplexity() {
        return maximumComplexity;
    }
}
