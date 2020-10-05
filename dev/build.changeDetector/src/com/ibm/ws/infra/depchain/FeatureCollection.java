package com.ibm.ws.infra.depchain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

public class FeatureCollection {

    private final Map<String, Feature> knownFeatures;
    private final Map<String, Feature> kernelFeatures;

    private final FeatureResolver resolver = new FeatureResolverImpl();
    private final FeatureRepository repoImpl = new FeatureRepository();

    private final File repoRoot;

    public FeatureCollection(String wlpDir, String repoRoot) {
        Map<String, Feature> knownFeaturesWritable = new HashMap<>();
        Map<String, Feature> kernelFeaturesWritable = discoverFeatureFiles(wlpDir + "/lib/platform");
        // Remove OS-specific platform features
        for (String feature : new HashSet<>(kernelFeaturesWritable.keySet()))
            if (feature.contains("zos") || feature.contains("os400"))
                kernelFeaturesWritable.remove(feature);
        knownFeaturesWritable.putAll(discoverFeatureFiles(wlpDir + "/lib/features"));
        knownFeaturesWritable.putAll(kernelFeaturesWritable);
        kernelFeatures = Collections.unmodifiableMap(kernelFeaturesWritable);
        knownFeatures = Collections.unmodifiableMap(knownFeaturesWritable);

        if (repoRoot != null && Files.exists(Paths.get(repoRoot)))
            this.repoRoot = new File(repoRoot);
        else
            this.repoRoot = null;

        try {
            File root = new File(wlpDir).getCanonicalFile();
            File lib = new File(root, "lib");
            setUtilsInstallDir(root);
            setKernelUtilsBootstrapLibDir(lib);
            BundleRepositoryRegistry.initializeDefaults("fatServer", true);
            repoImpl.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Feature> view() {
        return knownFeatures;
    }

    public Feature get(String featureSymbolicName) {
        return knownFeatures.get(featureSymbolicName);
    }

    public Set<String> getEnabledFeatures(Set<String> rootFeatures) {
        Result resolverResult = resolver.resolveFeatures(repoImpl, rootFeatures, Collections.emptySet(), true);
        if (resolverResult.getConflicts().size() != 0) {
            throw new RuntimeException("Unable to resolve: " + resolverResult.getConflicts() + "  from " + rootFeatures);
        }
        Set<String> resolvedFeatures = resolverResult.getResolvedFeatures();
        if (resolvedFeatures.size() < rootFeatures.size())
            throw new RuntimeException("Only resolved features " + resolvedFeatures + " for root set " + rootFeatures);
        Set<String> resolvedCanonicalFeatures = new HashSet<>(resolvedFeatures.size());
        for (String f : resolvedFeatures)
            resolvedCanonicalFeatures.add(f.startsWith("com.ibm.") ? f : "com.ibm.websphere.appserver." + f);
        return resolvedCanonicalFeatures;
    }

    /** Replace the location of the kernel lib directory (usually calculated) */
    private static void setKernelUtilsBootstrapLibDir(File bootLibDir) throws Exception {
        KernelUtils.setBootStrapLibDir(bootLibDir);
    }

    /** Replace the location of the utils install directory (usually calculated) */
    private static void setUtilsInstallDir(File installDir) throws Exception {
        Utils.setInstallDir(installDir);
    }

    public Feature getPublic(String featureShortName) {
        // Most of the time this optimization will work getting feature symbolic name
        Feature f = knownFeatures.get("com.ibm.websphere.appserver." + featureShortName);
        if (f != null)
            return f;
        for (Feature knownFeature : knownFeatures.values())
            if (knownFeature.isPublic() && knownFeature.getShortName().equalsIgnoreCase(featureShortName))
                return knownFeature;
        return null;
    }

    public Set<String> filterPublicOnly(Set<String> fList) {
        fList = fList.stream().filter((f) -> knownFeatures.get(f).isPublic()).collect(Collectors.toSet());
        Set<String> result = new HashSet<>();
        for (String f : fList)
            result.add(knownFeatures.get(f).getShortName());
        return result;
    }

    public Set<String> filterAutoOnly(Set<String> fList) {
        Set<String> autos = new HashSet<String>();
        for (String f : fList) {
            if (knownFeatures.get(f).isAutoFeature())
                autos.add(f);
        }
        return autos;
    }

    public void addFeaturesUsingBundle(String bundle, Set<String> featureSet) {
        searchFeaturesUsingBundle(bundle, featureSet);
        for (String f : new HashSet<>(featureSet))
            searchFeaturesUsingFeature(f, featureSet);
        if (featureSet.isEmpty()) {
            // Sometimes project names do not match the bundle symbolic name they produce
            // Check for a bnd.overrides or bnd.bnd file to see if it defines a different 'Bundle-SymbolicName'
            String customBSN = readBundleSymbolicName(bundle, "bnd.overrides");
            if (customBSN == null)
                customBSN = readBundleSymbolicName(bundle, "bnd.bnd");
            if (customBSN != null) {
                addFeaturesUsingBundle(customBSN, featureSet);
                return;
            }
            // Heuristic: Several components have an xyz.core bundle that is included in the 'xyz' bundle...
            // try to drop the '.core' and see if we find a bundle using that
            if (bundle.endsWith(".core")) {
                addFeaturesUsingBundle(bundle.substring(0, bundle.length() - 5), featureSet);
                return;
            }
            throw new IllegalStateException("No features are using bundle " + bundle);
        }
    }

    private String readBundleSymbolicName(String projectName, String fileInProject) {
        if (repoRoot == null)
            return null;

        File bndFile = new File(repoRoot, "dev/" + projectName + "/" + fileInProject);
        if (!bndFile.exists())
            return null;

        Properties bndProps = new Properties();
        try {
            bndProps.load(new FileInputStream(bndFile.getAbsolutePath()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String rawBndProp = bndProps.getProperty("Bundle-SymbolicName");
        if (rawBndProp == null)
            return null;
        return rawBndProp.split(";")[0]; // may have "; singleton=true/false" on the end
    }

    public void addFeaturesUsingFeature(String feature, Set<String> featureSet) {
        // Special case: Every feature uses the kernel features
        if (kernelFeatures.keySet().contains(feature)) {
            featureSet.addAll(knownFeatures.keySet());
            return;
        }
        searchFeaturesUsingFeature(feature, featureSet);
        featureSet.add(feature);
    }

    public void addEnabledAutoFeatures(Set<String> featureSet) {
        for (Feature f : knownFeatures.values()) {
            if (f.isAutoFeature() && f.isCapabilitySatisfied(featureSet)) {
                featureSet.add(f.getSymbolicName());
            }
        }
    }

    private Set<String> searchFeaturesUsingBundle(String bundle, Set<String> featureSet) {
        for (Feature f : knownFeatures.values())
            if (f.getBundles().contains(bundle))
                featureSet.add(f.getSymbolicName());
        return featureSet;
    }

    private Set<String> searchFeaturesUsingFeature(String feature, Set<String> featureSet) {
        for (Feature f : knownFeatures.values()) {
            String curFeature = f.getSymbolicName();
            if (!featureSet.contains(curFeature) && f.getEnabledFeatures().contains(feature)) {
                featureSet.add(curFeature);
                searchFeaturesUsingFeature(curFeature, featureSet);
            }
        }
        return featureSet;
    }

    private Map<String, Feature> discoverFeatureFiles(String dir) {
        File featureDir = new File(dir);
        if (!featureDir.exists() || !featureDir.isDirectory())
            throw new IllegalArgumentException("Directory did not exist: " + dir);
        Map<String, Feature> knownFeaturesWritable = new HashMap<>();
        for (File f : featureDir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".mf"))
                try {
                    Feature feature = new Feature(f.getAbsolutePath());
                    knownFeaturesWritable.put(feature.getSymbolicName(), feature);
                } catch (IOException ex) {
                    // "Should Never Happen"(TM)
                    ex.printStackTrace();
                }
        }
        return knownFeaturesWritable;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Feature f : knownFeatures.values())
            sb.append("\n").append(f.toString());
        return sb.toString();
    }

}
