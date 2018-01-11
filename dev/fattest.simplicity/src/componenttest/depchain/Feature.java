package componenttest.depchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Feature {
    private static final Class<?> c = Feature.class;
    private String shortName;
    private String symbolicName;
    private final List<String> enables = new ArrayList<String>();
    private final List<String> includes = new ArrayList<String>();
    private final List<String> autoProvision = new ArrayList<String>();

    private final Type type;
    private Boolean isProvisioned = null;

    public static enum Type {
        FEATURE,
        AUTO_FEATURE,
        KERNEL_FEATURE,
        PROTECTED_FEATURE
    }

    public Feature(Element e) {
        if (e.getNodeName().equals("feature")) {
            shortName = e.getAttribute("name");
            type = Type.FEATURE;
        } else if (e.getNodeName().equals("kernelFeature")) {
            type = Type.KERNEL_FEATURE;
        } else if (e.getNodeName().equals("autoFeature")) {
            type = Type.AUTO_FEATURE;
        } else if (e.getNodeName().equals("protectedFeature")) {
            type = Type.PROTECTED_FEATURE;
        } else { // in case a new feature type get created:
            type = Type.FEATURE;
        }
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element) {
                Element child = (Element) n;
                String nodeName = child.getNodeName();

                String content = child.getTextContent().trim();
                if ("symbolicName".equals(nodeName)) {
                    symbolicName = content;
                } else if ("enables".equals(nodeName)) {
                    enables.add(content);
                } else if ("include".equals(nodeName)) {
                    includes.add(child.getAttribute("symbolicName"));
                } else if ("autoProvision".equals(nodeName)) {
                    autoProvision.add(child.getAttribute("autoProvision"));
                }
            }
        }
    }

    public String getShortName() {
        return shortName;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public List<String> getEnables() {
        return enables;
    }

    public List<String> getInclude() {
        return includes;
    }

//    (&amp;(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.transaction-1.2))</autoProvision>
//    (&amp;(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.iioptransport-1.0))</autoProvision>
    public boolean isProvisioned(Map<String, Feature> featureMap, Set<String> installedFeatures) {
        if (isProvisioned != null)
            return isProvisioned;
        if (type != Type.AUTO_FEATURE) {
            isProvisioned = Boolean.TRUE;
            return isProvisioned;
        }

        // Must be an auto feature, check if required features are enabled
        for (String conditionFeature : autoProvision) {
            boolean anyOf = conditionFeature.contains("|");
            boolean isProvisioned = !anyOf;
            for (String identity : conditionFeature.split("osgi.identity=")) {
                int trimAt = identity.indexOf(')');
                if (trimAt < 1)
                    continue;
                identity = identity.substring(0, trimAt);
                if (Pattern.matches("[\\-\\w\\.]+", identity)) {
                    boolean installed = installedFeatures.contains(identity.toLowerCase());
                    if (anyOf)
                        isProvisioned |= installed;
                    else
                        isProvisioned &= installed;
                }
            }
            if (!isProvisioned)
                return false;
        }
        return true;
    }

    public Type getFeatureType() {
        return type;
    }

    @Override
    public String toString() {
        return "{ " + (shortName == null ? "" : ("shortName=" + shortName + ", ")) +
               "symbolicName=" + symbolicName +
               ", enables=" + enables +
               ", includes=" + includes + '}';
    }
}