package com.ibm.ws.infra.depchain;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;

public class Feature {

    private final Attributes rawAttrs;
    private final String shortName;
    private final String symbolicName;

    private final ProvisionCapability provisionCapability;
    private final Set<String> enablesFeatures = new HashSet<>();
    private final Set<String> bundles = new HashSet<>(8);
    private final Set<String> files = new HashSet<>(8);

    public Feature(String manifestFile) throws IOException {
        Manifest mf = ManifestProcessor.parseManifest(new FileInputStream(manifestFile));

        // Parse basic feature attributes
        rawAttrs = mf.getMainAttributes();
        shortName = rawAttrs.getValue("IBM-ShortName");
        NameValuePair bsn = ManifestHeaderProcessor.parseBundleSymbolicName(rawAttrs.getValue("Subsystem-SymbolicName"));
        symbolicName = bsn.getName();
        if (symbolicName == null || symbolicName.isEmpty())
            throw new IllegalArgumentException("Empty Subsystem-SymbolicName for manifest file: " + manifestFile);

        // Parse auto-feature data (if this is an auto feature)
        String rawProvisionCapability = rawAttrs.getValue("IBM-Provision-Capability");
        provisionCapability = rawProvisionCapability == null ? null : new ProvisionCapability(rawProvisionCapability);

        // Parse included bundles and features
        Map<String, Map<String, String>> content = ManifestHeaderProcessor.parseImportString(rawAttrs.getValue("Subsystem-Content"));
        for (Entry<String, Map<String, String>> e : content.entrySet()) {
            String key = e.getKey();
            String type = e.getValue().get("type");
            if (type == null || "jar".equals(type) || "boot.jar".equals(type))
                bundles.add(key);
            else if ("osgi.subsystem.feature".equals(type))
                enablesFeatures.add(key);
            else if ("file".equals(type))
                files.add(key);
            else
                throw new IllegalStateException("Found unknown content: type=" + type + "  " + e.getKey() + "   " + e.getValue());
        }
    }

    public boolean isPublic() {
        // Don't consider install bundles like 'com.ibm.websphere.appserver.webProfile8Bundle' to be public features
        return shortName != null && !shortName.endsWith("Bundle");
    }

    public boolean isAutoFeature() {
        return provisionCapability != null;
    }

    public boolean isCapabilitySatisfied(Set<String> enabledFeatures) {
        return isAutoFeature() && provisionCapability.isSatisfied(enabledFeatures);
    }

    public String getShortName() {
        return shortName;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public Set<String> getEnabledFeatures() {
        return Collections.unmodifiableSet(enablesFeatures);
    }

    public Set<String> getBundles() {
        return Collections.unmodifiableSet(bundles);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("name=");
        sb.append(symbolicName);
        if (isPublic())
            sb.append("   (").append(shortName).append(')');
        sb.append("\n  features=").append(enablesFeatures);
        sb.append("\n  bundles=").append(bundles);
        if (isAutoFeature())
            sb.append("\n  capability=").append(provisionCapability.toString());
        return sb.toString();
    }

    private class ProvisionCapability {
        private final Set<Set<String>> filters = new HashSet<>(2);

        /**
         * @param ibmProvisionCapability The raw value of the 'IBM-Provision-Capability' header in a feature manifest
         */
        public ProvisionCapability(String ibmProvisionCapability) {
            Objects.requireNonNull(ibmProvisionCapability,
                                   "Feature did not contain a 'IBM-Provision-Capability' header which means this is not an auto feature.");
            List<GenericMetadata> provisionCapability = ManifestHeaderProcessor.parseCapabilityString(ibmProvisionCapability);

            // Example: com.ibm.websphere.appserver.beanValidationCDI-2.0  (cdi-2.0 AND beanValidation-2.0)
            //  filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
            //  filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.beanValidation-2.0))"

            // Example: com.ibm.websphere.appserver.batchSecurity-1.0   ((appSecurity-2.0 OR 3.0) AND batch-1.0)
            //  filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))", \
            //  filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.batch-1.0))"
//            System.out.println("================");
            for (GenericMetadata metadata : provisionCapability) {
                Set<String> filterSet = new HashSet<>(3);
                String filter = metadata.getDirectives().get("filter");
                String[] conditions = filter.split("osgi\\.identity=");
                for (int i = 1; i < conditions.length; i++)
                    filterSet.add(conditions[i].substring(0, conditions[i].indexOf(')')));
                filters.add(filterSet);
//                System.out.println("  " + filterSet);
            }
        }

        /**
         * An auto feature is satisfied if ALL filters are satisfied.
         * An individual filter is satisfied if ANY of the features in the filter set are satisfied
         *
         * @param enabledFeatures The set of enabled features to test the provision capability against
         */
        public boolean isSatisfied(Set<String> enabledFeatures) {
            for (Set<String> filter : filters) {
                boolean filterSatisfied = false;
                for (String filterCondition : filter) {
                    if (enabledFeatures.contains(filterCondition)) {
                        filterSatisfied = true;
                        break;
                    }
                }
                if (!filterSatisfied)
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            Iterator<Set<String>> i2 = filters.iterator();
            while (i2.hasNext()) {
//                sb.append(i2.next());
                Set<String> filter = i2.next();
                sb.append('(');
                Iterator<String> i = filter.iterator();
                while (i.hasNext()) {
                    String filterCondition = i.next();
                    sb.append(filterCondition);
                    if (i.hasNext())
                        sb.append(" OR ");
                }
                sb.append(')');
                if (i2.hasNext())
                    sb.append(" AND ");
            }
            sb.append('}');
            return sb.toString();
        }

    }
}