/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.repository.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.repository.resolver.internal.kernel.CapabilityMatching;
import com.ibm.ws.repository.resolver.internal.kernel.KernelResolverRepository;

/**
 * Performs a walk over a tree of features by following their dependencies
 * <p>
 * Features are retrieved from a source (either a {@link KernelResolverRepository} or a map of resolved features)
 * <p>
 * If a dependency tolerates multiple versions of a feature, all tolerated versions available in the source are walked.
 */
public class FeatureTreeWalker {

    private Consumer<? super ProvisioningFeatureDefinition> forEach;
    private BiConsumer<? super ProvisioningFeatureDefinition, ? super FeatureResource> onMissingDependency;
    private boolean useAutofeatureProvisionAsDependency = true;
    private final Supplier<Collection<? extends ProvisioningFeatureDefinition>> allFeaturesSupplier;
    private final Function<String, ProvisioningFeatureDefinition> getFeatureByNameFunction;

    /**
     * Create a FeatureTreeWalker for walking feature dependencies for features in the repository
     *
     * @param repository the repository
     */
    public static FeatureTreeWalker walkOver(KernelResolverRepository repositry) {
        return new FeatureTreeWalker(repositry::getAllFeatures,
                                     repositry::getFeature);
    }

    /**
     * Create a FeatureTreeWalker for walking feature dependencies for features from a Map of features
     * <p>
     * The map must map from feature symbolic name to the feature
     *
     * @param resolvedFeatureMap the map of features
     */
    public static FeatureTreeWalker walkOver(Map<String, ProvisioningFeatureDefinition> resolvedFeatureMap) {
        return new FeatureTreeWalker(resolvedFeatureMap::values,
                                     resolvedFeatureMap::get);
    }

    private FeatureTreeWalker(Supplier<Collection<? extends ProvisioningFeatureDefinition>> allFeaturesSupplier,
                              Function<String, ProvisioningFeatureDefinition> getFeatureByNameFunction) {
        this.allFeaturesSupplier = allFeaturesSupplier;
        this.getFeatureByNameFunction = getFeatureByNameFunction;
    }

    /**
     * Perform a breadth first walk
     *
     * @param roots the starting points of the walk
     */
    public void walkBreadthFirst(Collection<ProvisioningFeatureDefinition> roots) {
        Walker.walkCollectionBreadthFirst(roots, this::processForEach, this::getChildren);
    }

    /**
     * Perform a breadth first walk
     *
     * @param root the starting point of the walk
     */
    public void walkBreadthFirst(ProvisioningFeatureDefinition root) {
        Walker.walkBreadthFirst(root, this::processForEach, this::getChildren);
    }

    /**
     * Perform a depth first walk
     * <p>
     * Features are visited before their children
     *
     * @param root the start point of the walk
     */
    public void walkDepthFirst(ProvisioningFeatureDefinition root) {
        Walker.walkDepthFirst(root, this::processForEach, this::getChildren);
    }

    /**
     * Supply a function to be called when visiting a feature
     *
     * @param forEach the function to call
     * @return {@code this} to allow chaining
     */
    public FeatureTreeWalker forEach(Consumer<? super ProvisioningFeatureDefinition> forEach) {
        this.forEach = forEach;
        return this;
    }

    /**
     * Supply a function to be called when an unsatisfied dependency is found
     * <p>
     * For a tolerated dependency, this method is only called if none of the tolerated versions can be found
     * <p>
     * The function is passed two arguments: the feature with the unsatisfied dependency and the unsatisfied dependency itself
     *
     * @param onMissingDependency the function to call
     * @return {@code this} to allow chaining
     */
    public FeatureTreeWalker onMissingDependency(BiConsumer<? super ProvisioningFeatureDefinition, ? super FeatureResource> onMissingDependency) {
        this.onMissingDependency = onMissingDependency;
        return this;
    }

    /**
     * Whether or not to treat features which satisfy one of the capabilities of an autofeature as children of that autofeature when walking the tree
     * <p>
     * Regular dependencies of an autofeature are always walked.
     * <p>
     * The default is {@code true}.
     *
     * @param useAutofeatureProvisionAsDependency whether to walk features which enable an autofeature
     * @return {@code this} for chaining
     */
    public FeatureTreeWalker useAutofeatureProvisionAsDependency(boolean useAutofeatureProvisionAsDependency) {
        this.useAutofeatureProvisionAsDependency = useAutofeatureProvisionAsDependency;
        return this;
    }

    /**
     * This method is called by the walker to get the children of a given feature
     * <p>
     * We look up and return the features dependencies as it's children
     *
     * @param feature the feature
     * @return the feature's children
     */
    private List<ProvisioningFeatureDefinition> getChildren(ProvisioningFeatureDefinition feature) {
        List<ProvisioningFeatureDefinition> result = new ArrayList<>();
        for (FeatureResource dependency : feature.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
            List<ProvisioningFeatureDefinition> children = findDependencies(dependency);

            if (children.isEmpty()) {
                processMissingDependency(feature, dependency);
            }

            result.addAll(children);
        }

        if (useAutofeatureProvisionAsDependency) {
            result.addAll(CapabilityMatching.findFeaturesSatisfyingCapability(feature, allFeaturesSupplier.get()));
        }

        return result;
    }

    private List<ProvisioningFeatureDefinition> findDependencies(FeatureResource featureResource) {
        List<ProvisioningFeatureDefinition> result = new ArrayList<>();
        ProvisioningFeatureDefinition feature = getFeatureByNameFunction.apply(featureResource.getSymbolicName());
        if (feature != null) {
            result.add(feature);
        }

        String baseName = getFeatureBaseName(featureResource.getSymbolicName());
        List<String> tolerates = featureResource.getTolerates();
        if (tolerates != null) {
            for (String toleratedVersion : tolerates) {
                String featureName = baseName + toleratedVersion;

                feature = getFeatureByNameFunction.apply(featureName);
                if (feature != null) {
                    result.add(feature);
                }
            }
        }

        return result;
    }

    private void processMissingDependency(ProvisioningFeatureDefinition feature, FeatureResource dependency) {
        if (onMissingDependency != null) {
            onMissingDependency.accept(feature, dependency);
        }
    }

    private void processForEach(ProvisioningFeatureDefinition feature) {
        if (forEach != null) {
            forEach.accept(feature);
        }
    }

    /**
     * Removes the version from the end of a feature symbolic name
     * <p>
     * The version is presumed to start after the last dash character in the name.
     * <p>
     * E.g. {@code getFeatureBaseName("com.example.featureA-1.0")} returns {@code "com.example.featureA-"}
     *
     * @param nameAndVersion the feature symbolic name
     * @return the feature symbolic name with any version stripped
     */
    private String getFeatureBaseName(String nameAndVersion) {
        int dashPosition = nameAndVersion.lastIndexOf('-');
        if (dashPosition != -1) {
            return nameAndVersion.substring(0, dashPosition + 1);
        } else {
            return nameAndVersion;
        }
    }

}
