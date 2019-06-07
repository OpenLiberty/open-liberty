package io.leangen.graphql.execution.relay;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.leangen.graphql.execution.SortField;

public class PagingArguments {

    private final String after;
    private final Integer first;
    private final String before;
    private final Integer last;
    private final List<SortField> sortFields = Collections.emptyList();

    public PagingArguments(Map<String, Object> arguments) {
        this.after = (String) arguments.get("after");
        this.first = (Integer) arguments.get("first");
        this.before = (String) arguments.get("before");
        this.last = (Integer) arguments.get("last");
    }

    public String getAfter() {
        return after;
    }

    public Integer getFirst() {
        return first;
    }

    public String getBefore() {
        return before;
    }

    public Integer getLast() {
        return last;
    }

    public List<SortField> getSortFields() {
        return sortFields;
    }
}
