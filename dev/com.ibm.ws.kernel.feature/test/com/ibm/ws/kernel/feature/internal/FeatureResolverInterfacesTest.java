/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

/**
 * In contrast to {@link FeatureResolverTest}, this class tests the feature resolver using its interfaces directly, without invoking any manifest reading code.
 */
public class FeatureResolverInterfacesTest {

    @Test
    public void testSingleFeatures() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA").build());
        repo.add(TestFeature.create("com.example.featureB").shortName("featureB").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("featureB"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(false));
        assertThat(result.getResolvedFeatures(), containsInAnyOrder("featureB"));

        Result result2 = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureA"), Collections.<String> emptySet(), false);
        assertThat(result2.hasErrors(), is(false));
        assertThat(result2.getResolvedFeatures(), containsInAnyOrder("com.example.featureA"));

        Result result3 = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureA", "com.example.featureB"), Collections.<String> emptySet(), false);
        assertThat(result3.hasErrors(), is(false));
        assertThat(result3.getResolvedFeatures(), containsInAnyOrder("com.example.featureA", "featureB"));
    }

    @Test
    public void testDependency() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA").build());
        repo.add(TestFeature.create("com.example.featureB").shortName("featureB").dependency("com.example.featureA").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("featureB"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(false));
        assertThat(result.getResolvedFeatures(), containsInAnyOrder("featureB", "com.example.featureA"));
    }

    @Test
    public void testToleratesChoosesPreferred() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA-1.0").build());
        repo.add(TestFeature.create("com.example.featureA-1.1").build());
        repo.add(TestFeature.create("com.example.featureB-1.1").shortName("featureB-1.1").dependency("com.example.featureA-1.0", "1.1").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("featureB-1.1"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(false));
        assertThat(result.getResolvedFeatures(), containsInAnyOrder("featureB-1.1", "com.example.featureA-1.0"));
    }

    @Test
    public void testToleratesUsedWhenPreferredMissing() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA-1.1").build());
        repo.add(TestFeature.create("com.example.featureB-1.1").shortName("featureB-1.1").dependency("com.example.featureA-1.0", "1.1").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("featureB-1.1"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(false));
        assertThat(result.getResolvedFeatures(), containsInAnyOrder("featureB-1.1", "com.example.featureA-1.1"));
    }

    @Test
    public void testToleratesUsedWhenConflictingVersionsRequired() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA-1.0").build());
        repo.add(TestFeature.create("com.example.featureA-1.1").build());
        repo.add(TestFeature.create("com.example.featureB-1.1").dependency("com.example.featureA-1.0", "1.1").build());
        repo.add(TestFeature.create("com.example.featureC-1.0").dependency("com.example.featureA-1.1").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureB-1.1", "com.example.featureC-1.0"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(false));
        assertThat(result.getResolvedFeatures(), containsInAnyOrder("com.example.featureC-1.0", "com.example.featureB-1.1", "com.example.featureA-1.1"));
    }

    @Test
    public void testAutoFeaturesResolved() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA-1.0").build());
        repo.add(TestFeature.create("com.example.featureB-1.0").build());

        TestFeature.Builder autoFeature = TestFeature.create("com.example.autoFeature-1.0");
        autoFeature.autofeatureDependency("com.example.featureA-1.0");
        autoFeature.autofeatureDependency("com.example.featureB-1.0");
        repo.add(autoFeature.build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureA-1.0", "com.example.featureB-1.0"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(false));
        assertThat(result.getResolvedFeatures(), containsInAnyOrder("com.example.featureA-1.0", "com.example.featureB-1.0", "com.example.autoFeature-1.0"));
    }

    @Test
    public void testResolveMissingFeature() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.missingFeature"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getResolvedFeatures(), is(empty()));
        assertThat(result.getMissing(), contains("com.example.missingFeature"));
    }

    @Test
    public void testResolveMissingDep() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA-1.0").dependency("com.example.missingFeature").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureA-1.0"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getResolvedFeatures(), contains("com.example.featureA-1.0"));
        assertThat(result.getMissing(), contains("com.example.missingFeature"));
    }

    @Test
    public void testResolveMissingDepWithTolerates() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("com.example.featureA-1.0").dependency("com.example.missingFeature-1.0", "1.1", "1.2").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureA-1.0"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getResolvedFeatures(), contains("com.example.featureA-1.0"));
        assertThat(result.getMissing(), contains("com.example.missingFeature-1.0"));

    }

    @Test
    public void testRootConflictFalsePositives01() {
        FeatureResolver resolver = new FeatureResolverImpl();

        TestRepository repo = new TestRepository();
        repo.add(TestFeature.create("rootConflict-1.0").singleton(true).build());
        repo.add(TestFeature.create("rootConflict-2.0").singleton(true).build());
        repo.add(TestFeature.create("tolerateConflict-1.0").singleton(true).build());
        repo.add(TestFeature.create("tolerateConflict-2.0").singleton(true).build());
        repo.add(TestFeature.create("middleMan-1.0").singleton(true).dependency("tolerateConflict-1.0").build());
        repo.add(TestFeature.create("middleMan-2.0").singleton(true).dependency("tolerateConflict-2.0").build());
        repo.add(TestFeature.create("root1-1.0").dependency("rootConflict-1.0").build());
        repo.add(TestFeature.create("root2-1.0").dependency("rootConflict-2.0").dependency("middleMan-1.0", "2.0").build());
        repo.add(TestFeature.create("root3-1.0").dependency("tolerateConflict-2.0").build());

        Result result = resolver.resolveFeatures(repo, Arrays.asList("root1-1.0", "root2-1.0", "root3-1.0"), Collections.<String> emptySet(), false);
        assertThat(result.hasErrors(), is(true));
        assertThat(result.getResolvedFeatures(), containsInAnyOrder("root1-1.0", "root2-1.0", "root3-1.0", "tolerateConflict-2.0", "middleMan-2.0"));

        assertThat(result.getConflicts().keySet(), Matchers.equalTo(Collections.singleton("rootConflict")));

    }

    /**
     * Test implementation of {@link FeatureResolver.Repository} which holds the features in a map.
     * <p>
     * Features can be added manually with the {@link #add(ProvisioningFeatureDefinition)} method.
     */
    public static class TestRepository implements FeatureResolver.Repository {

        private final HashMap<String, ProvisioningFeatureDefinition> features = new HashMap<>();
        private final HashMap<String, ProvisioningFeatureDefinition> autoFeatures = new HashMap<>();

        @Override
        public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
            return Collections.unmodifiableCollection(autoFeatures.values());
        }

        @Override
        public ProvisioningFeatureDefinition getFeature(String featureName) {
            return features.get(featureName);
        }

        @Override
        public List<String> getConfiguredTolerates(String baseSymbolicName) {
            return Collections.emptyList();
        }

        public void add(ProvisioningFeatureDefinition feature) {
            features.put(feature.getSymbolicName(), feature);
            features.put(feature.getIbmShortName(), feature);

            if (feature.isAutoFeature()) {
                autoFeatures.put(feature.getSymbolicName(), feature);
                autoFeatures.put(feature.getIbmShortName(), feature);
            }
        }

    }

    /**
     * Test implementation of {@link ProvisioningFeatureDefinition}
     * <p>
     * This class only implements the methods required to test the resolver.
     * <p>
     * New instances should be created using {@link #create(String)}.
     */
    public static class TestFeature implements ProvisioningFeatureDefinition {

        private String symbolicName;
        private Visibility visibility;
        private final Collection<FeatureResource> dependencies = new ArrayList<>();
        private String shortName;
        private boolean isSingleton;
        private Collection<Filter> autofeatureFilters;

        public static class Builder {
            private final TestFeature instance = new TestFeature();

            public Builder visibility(Visibility visibility) {
                instance.visibility = visibility;
                return this;
            }

            public Builder shortName(String shortName) {
                instance.shortName = shortName;
                return this;
            }

            public Builder dependency(String symbolicName, String... tolerates) {
                TestDependency dep = new TestDependency();
                dep.symbolicName = symbolicName;
                dep.tolerates.addAll(Arrays.asList(tolerates));
                instance.dependencies.add(dep);
                return this;
            }

            public Builder singleton(boolean isSingleton) {
                instance.isSingleton = isSingleton;
                return this;
            }

            public Builder provisionCapabilities(String provisionCapability) {
                if (instance.autofeatureFilters == null) {
                    instance.autofeatureFilters = new ArrayList<>();
                }

                List<GenericMetadata> metadatas = ManifestHeaderProcessor.parseCapabilityString(provisionCapability);
                try {
                    for (GenericMetadata metadata : metadatas) {
                        String filterString = metadata.getDirectives().get("filter");
                        if (metadata.getNamespace().equals("osgi.identity") && filterString != null) {
                            instance.autofeatureFilters.add(FrameworkUtil.createFilter(filterString));
                        }
                    }
                } catch (InvalidSyntaxException e) {
                    throw new RuntimeException(e);
                }

                return this;
            }

            public Builder autofeatureDependency(String symbolicName) {
                return provisionCapabilities("osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=" + symbolicName + "))\"");
            }

            public TestFeature build() {
                return instance;
            }
        }

        public static Builder create(String symbolicName) {
            Builder builder = new Builder();
            builder.instance.symbolicName = symbolicName;
            builder.instance.visibility = Visibility.PUBLIC;
            builder.instance.isSingleton = true;
            return builder;
        }

        @Override
        public String getSymbolicName() {
            return symbolicName;
        }

        @Override
        public String getFeatureName() {
            if (getIbmShortName() != null) {
                return getIbmShortName();
            } else {
                return getSymbolicName();
            }
        }

        @Override
        public Visibility getVisibility() {
            return visibility;
        }

        @Override
        public EnumSet<ProcessType> getProcessTypes() {
            return EnumSet.of(ProcessType.SERVER);
        }

        @Override
        public Collection<FeatureResource> getConstituents(SubsystemContentType type) {
            return Collections.unmodifiableCollection(dependencies);
        }

        @Override
        public String getBundleRepositoryType() {
            return "";
        }

        @Override
        public String getIbmShortName() {
            return shortName;
        }

        @Override
        public boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck) {
            Collection<Map<String, String>> featureDatas = new ArrayList<>();
            for (ProvisioningFeatureDefinition feature : featureDefinitionsToCheck) {
                Map<String, String> featureData = new HashMap<>();
                featureData.put("type", "osgi.subsystem.feature");
                featureData.put("osgi.identity", feature.getSymbolicName());
                featureDatas.add(featureData);
            }

            for (Filter filter : autofeatureFilters) {
                boolean foundMatch = false;
                for (Map<String, String> featureData : featureDatas) {
                    if (filter.matches(featureData)) {
                        foundMatch = true;
                        continue;
                    }
                }

                if (!foundMatch) {
                    return false;
                }
            }

            // If we get to here, all filters were matched against a feature
            return true;
        }

        @Override
        public boolean isSingleton() {
            return isSingleton;
        }

        @Override
        public boolean isAutoFeature() {
            return autofeatureFilters != null;
        }

        @Override
        public Version getVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AppForceRestart getAppForceRestart() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isKernel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getApiServices() {
            throw new UnsupportedOperationException();
        }

        @Override
        public File getFeatureDefinitionFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public File getFeatureChecksumFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(String string) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(String string, Locale locale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<HeaderElementDefinition> getHeaderElements(String string) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<File> getLocalizationFiles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSupersededBy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSuperseded() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSupportedFeatureVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIbmFeatureVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<String> getIcons() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Test implementation of FeatureResource
     * <p>
     * This implementation only implements {@link #getSymbolicName()} and {@link #getTolerates()}.
     * <p>
     * Instances of this class should be created via {@link TestFeature.Builder#dependency(String, String...)}.
     */
    public static class TestDependency implements FeatureResource {

        private String symbolicName;
        private final List<String> tolerates = new ArrayList<>();

        @Override
        public String getSymbolicName() {
            return symbolicName;
        }

        @Override
        public List<String> getTolerates() {
            return tolerates;
        }

        @Override
        public Map<String, String> getAttributes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String> getDirectives() {
            throw new UnsupportedOperationException();
        }

        @Override
        public VersionRange getVersionRange() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getLocation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SubsystemContentType getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRawType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getOsList() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStartLevel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getMatchString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getBundleRepositoryType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isType(SubsystemContentType type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getExtendedAttributes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String setExecutablePermission() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFileEncoding() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer getRequireJava() {
            throw new UnsupportedOperationException();
        }

    }

}
