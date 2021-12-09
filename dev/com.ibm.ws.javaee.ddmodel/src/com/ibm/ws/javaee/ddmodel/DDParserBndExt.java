package com.ibm.ws.javaee.ddmodel;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public abstract class DDParserBndExt extends DDParser {
    public static final String NAMESPACE_OMG_XMI =
        "http://www.omg.org/XMI";
    public static final String NAMESPACE_IBM_JAVAEE =
        "http://websphere.ibm.com/xml/ns/javaee";
    
    public static final String NAMESPACE_APP_BND_XMI =
        "applicationbnd.xmi";
    public static final String NAMESPACE_APP_EXT_XMI =
        "applicationext.xmi";
    public static final String NAMESPACE_CLIENT_BND_XMI =
        "clientbnd.xmi";    
    public static final String NAMESPACE_EJB_BND_XMI =
        "ejbbnd.xmi";
    public static final String NAMESPACE_EJB_EXT_XMI =
        "ejbext.xmi";
    public static final String NAMESPACE_WEB_BND_XMI =
        "webappbnd.xmi";
    public static final String NAMESPACE_WEB_EXT_XMI =
        "webappext.xmi";

    protected static final XMLVersionMapping XML_VERSION_MAPPING_10 =
            new XMLVersionMapping("1.0", 10);
    protected static final XMLVersionMapping XML_VERSION_MAPPING_11 =
            new XMLVersionMapping("1.1", 11);
    protected static final XMLVersionMapping XML_VERSION_MAPPING_12 =
            new XMLVersionMapping("1.2", 12);    

    protected static final XMLVersionMapping[] XML_VERSION_MAPPINGS_10_10 = {
            XML_VERSION_MAPPING_10
    };

    protected static final XMLVersionMapping[] XML_VERSION_MAPPINGS_10_11 = {
            XML_VERSION_MAPPING_10,
            XML_VERSION_MAPPING_11,
    };

    protected static final XMLVersionMapping[] XML_VERSION_MAPPINGS_10_12 = {
            XML_VERSION_MAPPING_10,
            XML_VERSION_MAPPING_11,
            XML_VERSION_MAPPING_12
    };
    
    public static final Class<?> UNUSED_CROSS_COMPONENT_TYPE = null;
    public static final boolean IS_XMI = true;
    public static final String UNUSED_XMI_NAMESPACE = null;

    public DDParserBndExt(
        Container ddRootContainer, Entry ddEntry,
        Class<?> crossComponentType,
        boolean xmi, String expectedRootElementName,
        String xmiNamespace,
        XMLVersionMapping[] xmlVersionMappings,
        int xmlDefaultVersion) throws DDParser.ParseException {

        super(ddRootContainer, ddEntry, expectedRootElementName);

        this.crossComponentType = crossComponentType;

        this.xmi = xmi;
        this.xmiNamespace = xmiNamespace;

        this.xmlVersionMappings = xmlVersionMappings;
        this.xmlDefaultVersion = xmlDefaultVersion;
    }    
    
    private final Class<?> crossComponentType;

    /**
     * Answer the type of the linked descriptor.
     * 
     * The presence of this API on {@link DDParser} is
     * a historical artifact.  Cross component types are
     * only used for BND and EXT documents, and the API
     * should have only been exposed to {@link DDParserBndExt}.
     * Rewiring the many parse types is too big of a change,
     * so the API has been left there.
     *
     * @return The type of the linked descriptor.
     */
    @Override
    public Class<?> getCrossComponentType() {
        return crossComponentType;
    }
    
    private final boolean xmi;

    public boolean isXMI() {
        return xmi;
    }

    private final String xmiNamespace;
    
    public String getXMINamespace() {
        return xmiNamespace;
    }

    private final XMLVersionMapping[] xmlVersionMappings;
    
    public XMLVersionMapping[] getXMLVersionMappings() {
        return xmlVersionMappings;
    }
    
    private final int xmlDefaultVersion;
    
    public int getXMLDefaultVersion() {
        return xmlDefaultVersion;
    }
    
    //

    protected ParsableElement createRootParsable() throws ParseException {
        validateRootElementName();

        if ( isXMI() ) {
            return createXMIRootParsable();
        } else {
            return createXMLRootParsable();
        }
    }

    //

    protected static class XMLVersionMapping {
        public final String versionText;
        public final int version;
        
        public XMLVersionMapping(String versionText, int version) {
            this.versionText = versionText;
            this.version = version;
        }
    }

    //
    
    protected ParsableElement createXMLRootParsable() throws ParseException {
        int ddVersion;

        String versionAttr = getAttributeValue("", "version");
        if ( versionAttr != null ) {
            XMLVersionMapping selectedMapping = null;
            for ( XMLVersionMapping versionMapping : getXMLVersionMappings() ) {
                if ( versionMapping.versionText.contentEquals(versionAttr) ) {
                    selectedMapping = versionMapping;
                }
            }
            if ( selectedMapping == null ) {
                throw new ParseException( unsupportedDescriptorVersion(versionAttr) );                
            }
            ddVersion = selectedMapping.version;
        } else {
            ddVersion = getXMLDefaultVersion();
        }

        if ( namespace == null ) {
            patchNamespace(NAMESPACE_IBM_JAVAEE);
        } else if ( !namespace.equals(NAMESPACE_IBM_JAVAEE) ) {
            warning( incorrectDescriptorNamespace(namespace, NAMESPACE_IBM_JAVAEE) );
            patchNamespace(NAMESPACE_IBM_JAVAEE);            
        }
        idNamespace = null;
        version = ddVersion;

        return createRoot();
    }

    protected ParsableElement createXMIRootParsable() {
        String expectedNamespace = getXMINamespace();
        if ( (namespace != null) && !namespace.equals(expectedNamespace) ) {
            warning( incorrectDescriptorNamespace(namespace, expectedNamespace) );
        }

        // This is correct: XMI parsing expects the namespace to be null.        
        namespace = null;
        namespaceOriginal = null;
        idNamespace = NAMESPACE_OMG_XMI;
        version = 9;

        return createRoot();
    }

    protected abstract ParsableElement createRoot();
}
