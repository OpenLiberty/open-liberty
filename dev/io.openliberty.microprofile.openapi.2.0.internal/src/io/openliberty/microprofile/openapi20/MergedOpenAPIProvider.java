package io.openliberty.microprofile.openapi20;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * An OpenAPIProvider for a model created by merging models from multiple modules
 */
public class MergedOpenAPIProvider implements OpenAPIProvider {
    
    private OpenAPI model;
    private List<String> mergeProblems;
    
    /**
     * @param model the merged OpenAPI model
     * @param mergeProblems the list of merge problems
     */
    public MergedOpenAPIProvider(OpenAPI model, List<String> mergeProblems) {
        this.model = model;
        this.mergeProblems = Collections.unmodifiableList(mergeProblems);
    }

    @Override
    public String getApplicationPath() {
        return null;
    }

    @Override
    public OpenAPI getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "Merged model";
    }

    @Override
    public List<String> getMergeProblems() {
        return mergeProblems;
    }

}
