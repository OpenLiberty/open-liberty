/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.osgi.framework.Bundle;

import com.ibm.websphere.config.ConfigParserException;
import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.internal.DefaultConfiguration.DefaultConfigFile;
import com.ibm.ws.config.xml.internal.variables.ConfigVariable;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.DesignatedXMLInputFactory;
import com.ibm.wsspi.kernel.service.location.MalformedLocationException;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

public class XMLConfigParser {

    /**  */
    private static final String VARIABLE = "variable";
    /**  */
    private static final String INCLUDE = "include";
    private static final String VARIABLE_VALUE = "value";
    private static final String VARIABLE_DEFAULT_VALUE = "defaultValue";
    private static final String VARIABLE_NAME = "name";

    private static final TraceComponent tc = Tr.register(XMLConfigParser.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private static final String IS_SUPPORTING_LOCATION_COORDINATES_PROPERTY = "javax.xml.stream.isSupportingLocationCoordinates";

    protected static final String BEHAVIOR_ATTRIBUTE = "onConflict";

    public static final String REQUIRE_EXISTING = "requireExisting";
    public static final String REQUIRE_DOES_NOT_EXIST = "addIfMissing";

    private int sequenceCounter;
    private final WsLocationAdmin locationService;
    private final LinkedList<String> docLocationStack = new LinkedList<String>();
    private final LinkedList<MergeBehavior> behaviorStack = new LinkedList<MergeBehavior>();

    private final ConfigVariableRegistry variableRegistry;

    public XMLConfigParser(WsLocationAdmin locationService, ConfigVariableRegistry variableRegistry) {
        this.locationService = locationService;
        this.variableRegistry = variableRegistry;
    }

    private static final class XifHolder {
        static final XMLInputFactory INSTANCE;

        static {
            XMLInputFactory xif = DesignatedXMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            // On an IBM JDK this property needs to be enabled in order to support the
            // reporting of the location of an error when an XMLStreamException is thrown.
            // Note that the XMLStreamReader in the Sun JDK reports error coordinates by default.
            //If the location coordinates property is supported, then set it
            if (xif.isPropertySupported(IS_SUPPORTING_LOCATION_COORDINATES_PROPERTY)
                //xlxp supports location coordinates in all versions, but reports incorrectly in
                //versions prior to Java 6 SR2 and Java 7 SR1, so we need this impl check until our
                //minimum supported version is increased beyond those levels.
                || "com.ibm.xml.xlxp.api.stax.XMLInputFactoryImpl".equals(xif.getClass().getName())) {
                xif.setProperty(IS_SUPPORTING_LOCATION_COORDINATES_PROPERTY, Boolean.TRUE);
            }
            INSTANCE = xif;
        }

        private XifHolder() {
        }
    }

    private static XMLInputFactory getXMLInputFactory() {
        return XifHolder.INSTANCE;
    }

    @Trivial
    private int getNextSequenceId() {
        return sequenceCounter++;
    }

    // test entry point only
    public ServerConfiguration parseServerConfiguration(WsResource resource) throws ConfigParserException, ConfigValidationException {
        return parseServerConfiguration(resource, new ServerConfiguration());
    }

    @FFDCIgnore(IOException.class)
    public ServerConfiguration parseServerConfiguration(WsResource resource, ServerConfiguration configuration) throws ConfigParserException, ConfigValidationException {
        String location = resource.toExternalURI().toString();
        tempVariables.variables.clear();
        InputStream in = null;
        try {
            in = resource.get();
            if (parseServerConfiguration(in, location, configuration, MergeBehavior.MERGE)) {
                configuration.updateLastModified(resource.getLastModified());
            } else {
                configuration = null;
            }
            return configuration;
        } catch (IOException e) {
            throw new ConfigParserException("Error loading configuration file " + location, e);
        } finally {
            ConfigUtil.closeIO(in);
        }
    }

    @FFDCIgnore(IOException.class)
    private void parseIncludeConfiguration(WsResource resource, BaseConfiguration configuration,
                                           MergeBehavior mergeBehavior) throws ConfigParserException, ConfigValidationException {
        String location = resource.toExternalURI().toString();
        InputStream in = null;
        try {
            in = resource.get();

            if (parseServerConfiguration(in, location, configuration, mergeBehavior)) {
                configuration.updateLastModified(resource.getLastModified());
            } else {
                configuration = null;
            }

        } catch (ConfigParserTolerableException e) {
            configuration.updateLastModified(resource.getLastModified());
            throw e;
        } catch (IOException e) {
            throw new ConfigParserException("Error loading configuration file " + location, e);
        } finally {
            ConfigUtil.closeIO(in);
        }
    }

    // test entry point only
    public ServerConfiguration parseServerConfiguration(Reader reader) throws ConfigParserException, ConfigValidationException {
        return parseServerConfiguration(reader, new ServerConfiguration());
    }

    // test entry point only
    @FFDCIgnore({ XMLStreamException.class })
    public ServerConfiguration parseServerConfiguration(Reader reader, ServerConfiguration config) throws ConfigParserException, ConfigValidationException {
        try {
            XMLStreamReader parser = getXMLInputFactory().createXMLStreamReader(reader);
            if (!parseServerConfiguration(new DepthAwareXMLStreamReader(parser), "test", config, MergeBehavior.MERGE)) {
                config = null;
            }
            parser.close();
            return config;
        } catch (XMLStreamException e) {
            throw new ConfigParserException(e);
        }
    }

    // private if not for tests
    @FFDCIgnore(XMLStreamException.class)
    public boolean parseServerConfiguration(InputStream in, String docLocation, BaseConfiguration config,
                                            MergeBehavior mergeBehavior) throws ConfigParserException, ConfigValidationException {
        XMLStreamReader parser = null;
        try {
            parser = getXMLInputFactory().createXMLStreamReader(docLocation, in);
            return parseServerConfiguration(new DepthAwareXMLStreamReader(parser), docLocation, config, mergeBehavior);
        } catch (XMLStreamException e) {
            throw new ConfigParserException(e);
        } finally {
            if (parser != null) {
                try {
                    parser.close();
                } catch (XMLStreamException e) {
                    throw new ConfigParserException(e);
                }
            }
        }
    }

    @FFDCIgnore(XMLStreamException.class)
    private boolean parseServerConfiguration(DepthAwareXMLStreamReader parser, String docLocation, BaseConfiguration config,
                                             MergeBehavior mergeBehavior) throws ConfigParserException, ConfigValidationException {
        if (docLocation != null) {
            if (docLocationStack.contains(docLocation)) {
                if (tc.isWarningEnabled()) {
                    List<String> list = docLocationStack.subList(docLocationStack.indexOf(docLocation), docLocationStack.size());
                    list.add(docLocation);
                    Tr.warning(tc, "warn.parse.circular.include", list);
                }
                docLocationStack.removeLast();
                return true;
            }
            docLocationStack.add(docLocation);
            behaviorStack.add(mergeBehavior);
        }

        try {
            int depth = parser.getDepth();
            String processType = locationService.resolveString("${wlp.process.type}");
            while (parser.hasNext(depth)) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = parser.getLocalName();
                    // TODO : Improve the following line that is hard coded with "server"
                    // See Task 154493.
                    if (processType.equals(name) || "server".equals(name)) {
                        parseServer(parser, docLocation, config, processType);
                        return true;
                    } else if ("client".equals(name)) {
                        // Silently ignore client configuration when the processType is not client
                        return false;
                    }
                }
            }
            // If we get here, there is a single element in the file and it is not <server> or <client>
            logError("error.root.must.be.server", docLocation, processType);
            throw new ConfigParserTolerableException();

        } catch (XMLStreamException e) {
            throw new ConfigParserException(e);
        } finally {
            if (docLocation != null) {
                docLocationStack.removeLast();
                behaviorStack.removeLast();
            }
        }
    }

    // test entry point only
    @FFDCIgnore(XMLStreamException.class)
    public ConfigElement parseConfigElement(Reader reader) throws ConfigParserException {
        behaviorStack.add(MergeBehavior.MERGE);
        docLocationStack.add("Test Server");
        DepthAwareXMLStreamReader parser = null;
        try {
            parser = new DepthAwareXMLStreamReader(getXMLInputFactory().createXMLStreamReader(reader));
            ConfigElement configElement = null;
            int depth = parser.getDepth();
            while (parser.hasNext(depth)) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    configElement = parseConfigElement(parser, parser.getLocalName(), null, null, null, false);
                    break;
                }
            }
            parser.close();
            return configElement;
        } catch (IllegalArgumentException ex) {
            throw new ConfigParserException(ex);
        } catch (XMLStreamException e) {
            throw new ConfigParserException(e);
        }
    }

    private final BaseConfiguration tempVariables = new BaseConfiguration();

    @FFDCIgnore({ XMLStreamException.class, ConfigParserTolerableException.class })
    private void parseServer(DepthAwareXMLStreamReader parser, String docLocation, BaseConfiguration config,
                             String processType) throws ConfigParserException, ConfigValidationException {
        String descriptionAttributeValue = getAttributeValue(parser, "description");
        if (descriptionAttributeValue != null) {
            config.setDescription(descriptionAttributeValue);
        }

        List<WsResource> includes = config.getIncludes();

        try {
            ConfigParserTolerableException savedConfigParserException = null;
            int depth = parser.getDepth();
            while (parser.hasNext(depth)) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = parser.getLocalName();
                    if (INCLUDE.equals(name)) {
                        // Pass the importedConfig variable in as a reference so that if an
                        // exception is thrown we still know what had been successfully parsed.
                        BaseConfiguration importedConfig = new BaseConfiguration();

                        try {
                            parseInclude(parser, docLocation, includes, importedConfig);
                        } catch (ConfigParserTolerableException e) {
                            // Catch this tolerable exception so that we can continue parsing this
                            // configuration while saving the first exception to report.
                            if (savedConfigParserException == null) {
                                savedConfigParserException = e;
                            }
                        }

                        // If the importedConfig exists after the normal execution path or after a
                        // tolerable exception was thrown, make sure we append it to it's parent config.
                        if (importedConfig != null) {
                            config.append(importedConfig);
                            config.updateLastModified(importedConfig.getLastModified());
                            includes.addAll(importedConfig.getIncludes());
                        }
                    } else if (VARIABLE.equals(name)) {
                        try {
                            ConfigVariable variable = parseVariable(parser, docLocation);
                            config.addVariable(variable);
                            tempVariables.addVariable(variable);
                        } catch (ConfigParserTolerableException e) {
                            if (savedConfigParserException == null) {
                                savedConfigParserException = e;
                            }
                        }
                    } else if (processType.equals(name)) {
                        // Assume this is a copy/paste error where the user has done something like
                        // <server> <server> ...</server> </server>
                        if (tc.isWarningEnabled()) {
                            Tr.warning(tc, "warning.unexpected.server.element");
                        }
                    } else {
                        SimpleElement configElement = parseConfigElement(parser, name, config, docLocation, null, false);
                        configElement.setDocumentLocation(docLocation);
                        config.addConfigElement(configElement);
                    }
                }
            }

            if (savedConfigParserException != null) {
                throw new ConfigParserTolerableException(savedConfigParserException);
            }

        } catch (XMLStreamException ex) {
            throw new ConfigParserException(ex);
        }
    }

    public enum MergeBehavior {
        MERGE,
        REPLACE,
        IGNORE,
        MERGE_WHEN_EXISTS, MERGE_WHEN_MISSING;
    };

    private void parseInclude(DepthAwareXMLStreamReader parser, String docLocation,
                              List<WsResource> includes, BaseConfiguration configuration) throws ConfigParserException, ConfigParserTolerableException, ConfigValidationException {
        String behaviorAttribute = getAttributeValue(parser, BEHAVIOR_ATTRIBUTE);
        MergeBehavior mergeBehavior = behaviorAttribute == null ? behaviorStack.getLast() : getMergeBehavior(behaviorAttribute);

        String optionalAttributeValue = getAttributeValue(parser, "optional");
        boolean optionalImport = (optionalAttributeValue != null && "true".equalsIgnoreCase(optionalAttributeValue));

        String includeAttributeValue = getAttributeValue(parser, "location");
        if (includeAttributeValue == null) {
            Location l = parser.getLocation();
            logError("error.include.location.not.specified", l.getLineNumber(), l.getSystemId());
            throw new ConfigParserTolerableException();
        }

        if (locationService != null) {
            String location = includeAttributeValue;
            WsResource includeResource = null;
            try {
                includeResource = resolveInclude(location, docLocation, locationService);
            } catch (MalformedLocationException mle) {
                // We're going to handle this after the null check below, so we don't need to handle it immediately.
            }
            if (includeResource != null) {
                includes.add(includeResource);
                if (includeResource.exists() &&
                    ((includeResource.isType(WsResource.Type.FILE) || (includeResource.isType(WsResource.Type.REMOTE))))) {

                    if (includeResource.isType(WsResource.Type.FILE)) {
                        Tr.audit(tc, "audit.include.being.processed", includeResource.asFile());
                    } else {
                        Tr.audit(tc, "audit.include.being.processed", includeResource.toExternalURI());
                    }

                    parseIncludeConfiguration(includeResource, configuration, mergeBehavior);
                    return;

                } else {
                    if (!optionalImport) {
                        logError("error.cannot.read.location", resolvePath(location));
                        throw new ConfigParserTolerableException();
                    }
                }
            } else {
                if (optionalImport) {
                    Tr.warning(tc, "warn.cannot.resolve.optional.include", resolvePath(location));
                    configuration = null;
                } else {
                    logError("error.cannot.read.location", resolvePath(location));
                    throw new ConfigParserTolerableException();
                }
            }
        } else {
            throw new ConfigParserException("LocationService is not available");
        }
    }

    /**
     * @param behaviorAttribute
     * @return
     */
    protected static MergeBehavior getMergeBehavior(String behaviorAttribute) {
        if (behaviorAttribute == null || behaviorAttribute.equalsIgnoreCase(MergeBehavior.MERGE.name()))
            return MergeBehavior.MERGE;
        else if (behaviorAttribute.equalsIgnoreCase(MergeBehavior.IGNORE.name()))
            return MergeBehavior.IGNORE;
        else if (behaviorAttribute.equalsIgnoreCase(MergeBehavior.REPLACE.name()))
            return MergeBehavior.REPLACE;

        // Note that we don't need MERGE_WHEN_EXISTS or MERGE_WHEN_MISSING here -- they are only specified
        // in default configuration manifests

        // Be forgiving with this.. just issue a warning and use default behavior
        if (tc.isWarningEnabled()) {
            Tr.warning(tc, "warning.unrecognized.merge.behavior", behaviorAttribute);
        }

        return MergeBehavior.MERGE;
    }

    @Sensitive
    private ConfigVariable parseVariable(DepthAwareXMLStreamReader parser, String docLocation) throws ConfigParserTolerableException {
        String variableName = null;
        String variableValue = null;
        String variableDefault = null;

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; ++i) {
            String name = parser.getAttributeLocalName(i);
            String value = parser.getAttributeValue(i);
            if (VARIABLE_NAME.equals(name)) {
                variableName = value;
            } else if (VARIABLE_VALUE.equals(name)) {
                variableValue = value;
            } else if (VARIABLE_DEFAULT_VALUE.equals(name)) {
                variableDefault = value;
            }
        }

        if (variableName == null) {
            Location l = parser.getLocation();
            logError("error.variable.name.missing", l.getLineNumber(), l.getSystemId());
            throw new ConfigParserTolerableException();
        }

        if (variableValue == null && variableDefault == null) {
            Location l = parser.getLocation();
            logError("error.variable.value.missing", l.getLineNumber(), l.getSystemId());
            throw new ConfigParserTolerableException();
        }

        return new ConfigVariable(variableName, variableValue, variableDefault, behaviorStack.getLast(), docLocation, false);
    }

    @FFDCIgnore(XMLStreamException.class)
    private SimpleElement parseConfigElement(DepthAwareXMLStreamReader parser,
                                             String entryElementLocalName,
                                             BaseConfiguration config,
                                             String docLocation,
                                             String parentId,
                                             boolean isChild) throws ConfigParserException {
        SimpleElement element = new SimpleElement(entryElementLocalName);
        element.setDocumentLocation(docLocation);
        element.setMergeBehavior(behaviorStack.getLast());
        element.setDocLocationStack(new LinkedList<String>(docLocationStack));
        element.setBehaviorStack(new LinkedList<MergeBehavior>(behaviorStack));

        int nextSeqId = getNextSequenceId();
        element.setSequenceId(nextSeqId);

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; ++i) {
            String name = parser.getAttributeLocalName(i);
            String value = parser.getAttributeValue(i);
            if (XMLConfigConstants.CFG_INSTANCE_ID.equals(name)) {
                element.setId(value);
            }
            element.addAttribute(name, value);
        }

        try {
            int depth = parser.getDepth();
            while (parser.hasNext(depth)) {
                int event = parser.next();
                if (event == XMLStreamConstants.CHARACTERS) {
                    element.setElementValue(element.getElementValue() + parser.getText());
                } else if (event == XMLStreamConstants.START_ELEMENT) {
                    if (isChild)
                        element.setTextOnly(false);
                    String name = parser.getLocalName();
                    String operationValue = getAttributeValue(parser, "merge-op");
                    if (operationValue != null) {
                        if ("append".equals(operationValue)) {
                            element.setMergeOperation(name, ConfigElement.MERGE_OP.APPEND);
                        } else if ("set".equals(operationValue)) {
                            element.setMergeOperation(name, ConfigElement.MERGE_OP.SET);
                        }
                    }
                    //now we always recurse into the child element but based
                    //on what we parsed, we can determine whether to add the
                    //element or just the collectionAttribute
                    SimpleElement childElement = parseConfigElement(parser,
                                                                    name,
                                                                    config,
                                                                    docLocation,
                                                                    element.getFullId(),
                                                                    true);
                    if (childElement.isChildElement() && operationValue == null) {
                        if (childElement.getRefAttr() == null) {
                            childElement.setDocumentLocation(docLocation);
                            element.addChildConfigElement(name, childElement);
                        } else {
                            element.addReference(childElement.getNodeName(), childElement.getRefAttr());
                        }
                    } else {
                        element.addCollectionAttribute(name, childElement.getElementValue());
                    }
                }
            }
        } catch (XMLStreamException ex) {
            throw new ConfigParserException(ex);
        }
        return element;
    }

    private String getAttributeValue(XMLStreamReader reader, String localName) {
        final int attrCount = reader.getAttributeCount();
        for (int i = 0; i < attrCount; ++i) {
            if (localName.equals(reader.getAttributeLocalName(i)) &&
                ("".equals(reader.getAttributeNamespace(i)) ? null : reader.getAttributeNamespace(i)) == null) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }

    /**
     * @param defaultConfigFile
     * @return
     * @throws ConfigParserException
     * @throws ConfigValidationException
     */
    @FFDCIgnore(IOException.class)
    public BaseConfiguration parseDefaultConfiguration(DefaultConfigFile defaultConfigFile) throws ConfigParserException, ConfigValidationException {
        InputStream in = null;
        try {
            in = defaultConfigFile.fileURL.openStream();
            BaseConfiguration config = new BaseConfiguration();
            if (!parseServerConfiguration(in, defaultConfigFile.fileURL.toExternalForm(), config, defaultConfigFile.behavior)) {
                config = null;
            }
            return config;
        } catch (IOException e) {
            throw new ConfigParserException(e);
        } finally {
            ConfigUtil.closeIO(in);
        }
    }

    // test entry point only
    @FFDCIgnore({ XMLStreamException.class })
    public BaseConfiguration parseDefaultConfiguration(Reader reader, String docLocation) throws ConfigParserException, ConfigValidationException {
        try {
            XMLStreamReader parser = getXMLInputFactory().createXMLStreamReader(reader);
            BaseConfiguration config = new BaseConfiguration();
            if (!parseServerConfiguration(new DepthAwareXMLStreamReader(parser), docLocation, config, MergeBehavior.MERGE)) {
                config = null;
            }
            parser.close();
            return config;
        } catch (XMLStreamException e) {
            throw new ConfigParserException(e);
        }
    }

    /**
     * Resolves resource path specified by &lt;include file? location?&gt; using the <code>WsLocationAdmin</code> service.
     * <P>
     * The includePath is resolved against the resourcePath if specified. If resourcePath or includePath cannot
     * be resolved a null is returned.
     *
     * @param includePath
     * @param basePath
     * @param wsLocationAdmin
     * @param vars
     * @return <code>WsResource</code> if resolved. Null otherwise.
     */
    WsResource resolveInclude(String includePath, String basePath, WsLocationAdmin wsLocationAdmin) {
        if (includePath == null) {
            return null;
        }

        includePath = includePath.trim();

        if (includePath.length() == 0) {
            return null;
        }

        if (basePath == null) {
            // no basePath - resolve includePath as is
            String normalIncludePath = resolvePath(includePath);
            return wsLocationAdmin.resolveResource(normalIncludePath);
        } else {
            String normalIncludePath = PathUtils.normalize(resolvePath(includePath));
            if (PathUtils.pathIsAbsolute(normalIncludePath)) {
                // includePath is absolute - resolve includePath as is
                return wsLocationAdmin.resolveResource(normalIncludePath);
            } else {
                // includePath is relative - resolve against basePath
                String normalBasePath = wsLocationAdmin.resolveString(basePath);
                String normalParentPath = PathUtils.getParent(normalBasePath);
                if (normalParentPath == null) {
                    return wsLocationAdmin.resolveResource(normalIncludePath);
                } else if (normalParentPath.endsWith("/")) {
                    return wsLocationAdmin.resolveResource(normalParentPath + normalIncludePath);
                } else {
                    return wsLocationAdmin.resolveResource(normalParentPath + "/" + normalIncludePath);
                }
            }
        }
    }

    private String resolvePath(String path) {

        if (PathUtils.isSymbol(path)) {

            Map<String, LibertyVariable> currentVariables = variableRegistry.getConfigVariables();
            try {
                variableRegistry.updateSystemVariables(tempVariables.getVariables());

                // Look for normal variables of the form $(variableName)
                Matcher matcher = XMLConfigConstants.VAR_PATTERN.matcher(path);

                while (matcher.find()) {
                    String var = matcher.group(1);

                    // Try to resolve the variable normally ( for ${var-Name} resolve var-Name }
                    String rep = variableRegistry.lookupVariable(var);

                    if (rep == null) {
                        rep = variableRegistry.lookupVariableFromAdditionalSources(var);
                    }

                    if (rep == null) {
                        rep = variableRegistry.lookupVariableDefaultValue(var);
                    }

                    if (rep != null) {
                        path = path.replace(matcher.group(0), rep);
                        matcher.reset(path);
                    }
                }
            } finally {
                variableRegistry.updateSystemVariables(currentVariables);
            }
        } else {
            return locationService.resolveString(path);
        }

        return PathUtils.normalize(path);
    }

    public void handleParseError(ConfigParserException e, Bundle bundle) {
        Throwable t = e.getCause();
        if (t instanceof XMLStreamException) {
            XMLStreamException xse = (XMLStreamException) t;
            Location l = xse.getLocation();
            if (l != null) {
                String loc = l.getSystemId();
                if (bundle != null) {
                    loc = loc + "(" + bundle.getLocation() + ")";
                }
                logError("error.syntax.parse.server", getMessage(xse), loc, l.getLineNumber(), l.getColumnNumber());
            } else {
                // XMLStreamException is allowed to return a null Location
                // but in practice this will never occur with the JDK built-in
                // StAX implementations.
                logError("error.syntax.parse.server", getMessage(xse), "[null]", -1, -1);
            }
        } else if (e.getMessage() != null) {
            // If the message is null, assume we have already logged it.
            logError("error.parse.server", e.getMessage());
        }
    }

    private String getMessage(XMLStreamException xse) {
        String message = xse.getMessage();
        // On a Sun JDK XMLStreamException injects hard-coded English text around
        // the actual message. Remove this text from the message before returning
        // to the user.
        if (message != null && message.startsWith("ParseError at [row,col]:[")) {
            final String MESSAGE_HEADER = "Message: ";
            int index = message.indexOf(MESSAGE_HEADER);
            if (index >= 0) {
                return message.substring(index + MESSAGE_HEADER.length());
            }
        }
        return message;
    }

    private void logError(String msgKey, Object... args) {

        switch (ErrorHandler.INSTANCE.getOnError()) {
            case FAIL:
            case WARN:
                Tr.error(tc, msgKey, args);
                break;
            case IGNORE:
                break;
        }
    }
}
