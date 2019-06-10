package io.leangen.graphql.metadata;

import graphql.introspection.Introspection;

import java.util.List;

public class Directive {

    private final String name;
    private final String description;
    private final Introspection.DirectiveLocation[] locations;
    private final List<DirectiveArgument> arguments;

    public Directive(String name, String description, Introspection.DirectiveLocation[] locations, List<DirectiveArgument> arguments) {
        this.name = name;
        this.description = description;
        this.locations = locations;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Introspection.DirectiveLocation[] getLocations() {
        return locations;
    }

    public List<DirectiveArgument> getArguments() {
        return arguments;
    }
}
