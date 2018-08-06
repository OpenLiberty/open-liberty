package test.common.junit.rules;

public class JavaInfo {
    public static final int JAVA_VERSION = new JavaInfo().majorVersion();

    /**
     * The java.vendor of the JDK. Note that Sun and Oracle JDKs are considered to be the same.
     */
    public static enum Vendor {
        IBM,
        OPENJ9,
        ORACLE,
        UNKNOWN
    }

    final String JAVA_HOME;
    final int MAJOR;
    final int MINOR;
    final Vendor VENDOR;
    final int SERVICE_RELEASE;
    final int FIXPACK;

    private JavaInfo() {
        JAVA_HOME = System.getProperty("java.home");

        // Parse MAJOR and MINOR versions
        String specVersion = System.getProperty("java.specification.version");
        String[] versions = specVersion.split("[^0-9]"); // split on non-numeric chars
        // Offset for 1.MAJOR.MINOR vs. MAJOR.MINOR version syntax
        int offset = "1".equals(versions[0]) ? 1 : 0;
        if (versions.length <= offset)
            throw new IllegalStateException("Bad Java runtime version string: " + specVersion);
        MAJOR = Integer.parseInt(versions[offset]);
        MINOR = versions.length < (2 + offset) ? 0 : Integer.parseInt(versions[(1 + offset)]);

        // Parse vendor
        String vendor = System.getProperty("java.vendor").toLowerCase();
        if (vendor.contains("ibm"))
            VENDOR = Vendor.IBM;
        else if (vendor.contains("openj9"))
            VENDOR = Vendor.OPENJ9;
        else if (vendor.contains("oracle"))
            VENDOR = Vendor.ORACLE;
        else
            VENDOR = Vendor.UNKNOWN;

        // Parse service release
        String buildInfo = System.getProperty("java.runtime.version");
        int sr = 0;
        int srloc = buildInfo.toLowerCase().indexOf("sr");
        if (srloc > (-1)) {
            srloc += 2;
            if (srloc < buildInfo.length()) {
                int len = 0;
                while ((srloc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(srloc + len))) {
                    len++;
                }
                sr = Integer.parseInt(buildInfo.substring(srloc, srloc + len));
            }
        }
        SERVICE_RELEASE = sr;

        // Parse fixpack
        int fp = 0;
        int fploc = buildInfo.toLowerCase().indexOf("fp");
        if (fploc > (-1)) {
            fploc += 2;
            if (fploc < buildInfo.length()) {
                int len = 0;
                while ((fploc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(fploc + len))) {
                    len++;
                }
                fp = Integer.parseInt(buildInfo.substring(fploc, fploc + len));
            }
        }
        FIXPACK = fp;
    }

    public int majorVersion() {
        return MAJOR;
    }

    public int minorVersion() {
        return MINOR;
    }

    public Vendor vendor() {
        return VENDOR;
    }

    public String javaHome() {
        return JAVA_HOME;
    }

    public int serviceRelease() {
        return SERVICE_RELEASE;
    }

    public int fixpack() {
        return FIXPACK;
    }

    @Override
    public String toString() {
        return "major=" + MAJOR + "  minor=" + MINOR + " service release=" + SERVICE_RELEASE + " fixpack=" + FIXPACK + "  vendor=" + VENDOR + "  javaHome=" + JAVA_HOME;
    }
}