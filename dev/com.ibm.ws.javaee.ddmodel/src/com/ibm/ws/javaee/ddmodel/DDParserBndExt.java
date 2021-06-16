package com.ibm.ws.javaee.ddmodel;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public abstract class DDParserBndExt extends DDParser {
    public static final String NAMESPACE_IBM_JAVAEE =
        "http://websphere.ibm.com/xml/ns/javaee";
    public static final String NAMESPACE_APP_BND_XMI =
        "applicationbnd.xmi";
    
    
    public DDParserBndExt(
        Container ddRootContainer, Entry ddEntry,
        Class<?> crossComponentType,
        boolean xmi) throws DDParser.ParseException {

        super(ddRootContainer, ddEntry, Application.class);

        this.xmi = xmi;
    }    

    private final boolean xmi;

    public boolean isXMI() {
        return xmi;
    }

    //

    @Override
    protected VersionData[] getVersionData() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void validateRootElementName() throws ParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ParsableElement createRootElement() {
        throw new UnsupportedOperationException();
    }
}
