package io.smallrye.graphql.cdi.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.smallrye.graphql.bootstrap.Config;

/**
 * Configuration for GraphQL
 * 
 * Liberty change - removed all references to CDI injection and MP Config
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class GraphQLConfig implements Config {

    private Optional<List<String>> hideList = Optional.empty();

    private Optional<List<String>> showList = Optional.empty();

    private String defaultErrorMessage;

    private boolean printDataFetcherException;

    private boolean allowGet;

    private boolean metricsEnabled;

    private boolean tracingEnabled;

    private boolean validationEnabled;

    private boolean includeScalarsInSchema = true;

    private boolean includeDirectivesInSchema;

    private boolean includeSchemaDefinitionInSchema;

    private boolean includeIntrospectionTypesInSchema;

    private boolean logPayload;

    private String fieldVisibility = Config.FIELD_VISIBILITY_DEFAULT;

    @Override
    public String getDefaultErrorMessage() {
        return defaultErrorMessage;
    }

    @Override
    public boolean isPrintDataFetcherException() {
        return printDataFetcherException;
    }

    @Override
    public Optional<List<String>> getHideErrorMessageList() {
        return hideList;
    }

    @Override
    public Optional<List<String>> getShowErrorMessageList() {
        return showList;
    }

    @Override
    public boolean isAllowGet() {
        return allowGet;
    }

    @Override
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    @Override
    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    @Override
    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    @Override
    public boolean isIncludeDirectivesInSchema() {
        return includeDirectivesInSchema;
    }

    @Override
    public boolean isIncludeSchemaDefinitionInSchema() {
        return includeSchemaDefinitionInSchema;
    }

    @Override
    public boolean isIncludeIntrospectionTypesInSchema() {
        return includeIntrospectionTypesInSchema;
    }

    @Override
    public boolean isIncludeScalarsInSchema() {
        return includeScalarsInSchema;
    }

    @Override
    public boolean logPayload() {
        return logPayload;
    }

    @Override
    public String getFieldVisibility() {
        return fieldVisibility;
    }

    public void setHideErrorMessageList(Optional<List<String>> hideList) {
        this.hideList = hideList;
    }

    public void setShowErrorMessageList(Optional<List<String>> showList) {
        this.showList = showList;
    }

    public void setDefaultErrorMessage(String defaultErrorMessage) {
        this.defaultErrorMessage = defaultErrorMessage;
    }

    public void setPrintDataFetcherException(boolean printDataFetcherException) {
        this.printDataFetcherException = printDataFetcherException;
    }

    public void setAllowGet(boolean allowGet) {
        this.allowGet = allowGet;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public void setTracingEnabled(final boolean tracingEnabled) {
        this.tracingEnabled = tracingEnabled;
    }

    public void setIncludeScalarsInSchema(boolean includeScalarsInSchema) {
        this.includeScalarsInSchema = includeScalarsInSchema;
    }

    public void setIncludeDirectivesInSchema(boolean includeDirectivesInSchema) {
        this.includeDirectivesInSchema = includeDirectivesInSchema;
    }

    public void setIncludeSchemaDefinitionInSchema(boolean includeSchemaDefinitionInSchema) {
        this.includeSchemaDefinitionInSchema = includeSchemaDefinitionInSchema;
    }

    public void setIncludeIntrospectionTypesInSchema(boolean includeIntrospectionTypesInSchema) {
        this.includeIntrospectionTypesInSchema = includeIntrospectionTypesInSchema;
    }

    public void setLogPayload(boolean logPayload) {
        this.logPayload = logPayload;
    }

    public void setFieldVisibility(String fieldVisibility) {
        this.fieldVisibility = fieldVisibility;
    }

    private Optional<List<String>> mergeList(Optional<List<String>> currentList, Optional<List<String>> deprecatedList) {

        List<String> combined = new ArrayList<>();
        if (deprecatedList.isPresent()) {
            combined.addAll(deprecatedList.get());
        }
        if (currentList.isPresent()) {
            combined.addAll(currentList.get());
        }

        if (!combined.isEmpty()) {
            return Optional.of(combined);
        } else {
            return Optional.empty();
        }
    }
}

