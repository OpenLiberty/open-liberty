package io.leangen.graphql.generator;

public class JavaDeprecationMappingConfig {

    public final boolean enabled;
    public final String deprecationReason;

    public JavaDeprecationMappingConfig(boolean enabled, String deprecationReason) {
        this.enabled = enabled;
        this.deprecationReason = deprecationReason;
    }
}
