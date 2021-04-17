/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.schemagen.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.equinox.metatype.impl.ExtendableHelper;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.metatype.OutputVersion;
import com.ibm.websphere.metatype.SchemaVersion;
import com.ibm.ws.config.schemagen.internal.TypeBuilder.OCDType;
import com.ibm.ws.config.schemagen.internal.TypeBuilder.OCDTypeReference;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinitionImpl;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition;
import com.ibm.ws.config.xml.internal.schema.AttributeDefinitionSpecification;

/**
 *
 */
class SchemaWriter {

    public static final String XSD = "http://www.w3.org/2001/XMLSchema";
    public static final String IBM_EXT_NS = "http://www.ibm.com/xmlns/dde/schema/annotation/ext";

    private static final String INCLUDE_TYPE = "includeType";
    private static final String SERVER_TYPE = "serverType";
    private static final String FACTORY_ID_TYPE = "factoryIdType";
    private static final String INTERNAL_PROPERTIES = "internal.properties";
    private static final String INTERNAL_PROPERTIES_TYPE = "internalPropertiesType";
    private static final String VARIABLE_DEFINITION_TYPE = "variableDefinitionType";
    private static final String GENERATE_SCHEMA_ACTION = "generateSchema";
    private static final String EARLY_ACCESS = "EARLY_ACCESS";

    private static final ResourceBundle _msgs = ResourceBundle.getBundle(SchemaGenConstants.NLS_PROPS);

    private final XMLStreamWriter writer;
    private final List<MetaTypeInformation> definitions;
    private String encoding;
    private boolean generateDocumentation;
    private boolean generateWildcards;
    private Locale locale;
    private boolean ignoreErrors;
    private Set<String> ignoredPids;
    private boolean preferShortNames;
    private ResourceBundle resourceBundle;
    private AttributeDefinition onErrorDefinition;

    private final Set<Type> hasType;
    private final Set<String> restrictedTypes;
    private boolean isRuntime;
    private SchemaVersion schemaVersion;
    private OutputVersion outputVersion;
    private boolean beta = false;
    private static File installDir;

    public SchemaWriter(XMLStreamWriter writer) {
        this.writer = writer;
        this.definitions = new ArrayList<MetaTypeInformation>();
        this.generateDocumentation = true;
        this.generateWildcards = true;
        this.ignoreErrors = true;
        this.preferShortNames = true;
        // Tooling wants order (especially attribute order) preserved from metatypes
        // LinkedHashMaps and LinkedHashSets iterate in insertion order.
        // We may not actually need _Linked_ for all of these, but we can afford it since schemagen isn't a runtime operation
        this.hasType = new LinkedHashSet<Type>(); // Preserves (and iterates in) insertion order
        this.hasType.add(Type.VARIABLE);
        this.hasType.add(Type.LOCATION);
        this.restrictedTypes = new LinkedHashSet<String>();
        this.isRuntime = false;
        beta = isEarlyAccess();
    }

    /**
     * Work out whether we should generate the schema for a Beta build or not.
     *
     * @return true if beta schema, false otherwise.
     */
    private static boolean isEarlyAccess() {
        boolean result = false;

        final Properties props = new Properties();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {
                try {
                    Reader r;
                    final File wasFile = new File(getInstallDir(), "lib/versions/WebSphereApplicationServer.properties");
                    final File olFile = new File(getInstallDir(), "lib/versions/openliberty.properties");
                    if (olFile.exists())
                        r = new InputStreamReader(new FileInputStream(olFile), StandardCharsets.UTF_8);
                    else
                        r = new InputStreamReader(new FileInputStream(wasFile), StandardCharsets.UTF_8);
                    props.load(r);
                    r.close();
                } catch (IOException e) {
                    // ignore because we fail safe. Returning true will result in a GA suitable schema
                }
                return null;
            }
        });
        String edition = props.getProperty("com.ibm.websphere.productEdition");

        if (edition == null) {
            result = false;
        } else {
            result = edition.equals(EARLY_ACCESS);
        }
        return result;
    }

    private static File getInstallDir() {
        if (installDir == null) {
            String installDirProp = System.getProperty("wlp.install.dir");
            if (installDirProp == null) {
                URL url = SchemaWriter.class.getProtectionDomain().getCodeSource().getLocation();

                if (url.getProtocol().equals("file")) {
                    // Got the file for the command line launcher, this lives in lib
                    try {
                        if (url.getAuthority() != null) {
                            url = new URL("file://" + url.toString().substring("file:".length()));
                        }

                        File f = new File(url.toURI());
                        // The parent of the jar is lib, so the parent of the parent is the install.
                        installDir = f.getParentFile();
                    } catch (MalformedURLException e) {
                        // Not sure we can get here so ignore.
                    } catch (URISyntaxException e) {
                        // Not sure we can get here so ignore.
                    }
                }
            } else {
                installDir = new File(installDirProp);
            }
        }

        return installDir;
    }

    public void setIsRuntime(boolean value) {
        this.isRuntime = value;
    }

    public void setGenerateDocumentation(boolean generateDocumentation) {
        this.generateDocumentation = generateDocumentation;
    }

    public boolean getGenerateDocumentation() {
        return generateDocumentation;
    }

    public void setGenerateWildcards(boolean generateWildcards) {
        this.generateWildcards = generateWildcards;
    }

    public boolean getGenerateWildcards() {
        return generateWildcards;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public boolean getIgnoreErrors() {
        return ignoreErrors;
    }

    public boolean isPreferShortNames() {
        return preferShortNames;
    }

    public void setPreferShortNames(boolean preferShortNames) {
        this.preferShortNames = preferShortNames;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setIgnoredPids(Set<String> ignoredPids) {
        this.ignoredPids = ignoredPids;
    }

    public Set<String> getIgnoredPids() {
        return ignoredPids;
    }

    public void add(MetaTypeInformation metatype) {
        definitions.add(metatype);
    }

    public void generate(boolean full) throws XMLStreamException {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        resourceBundle = ResourceBundle.getBundle("com.ibm.ws.config.internal.resources.SchemaData", locale);

        writer.writeStartDocument((encoding == null) ? "UTF-8" : encoding, "1.0");
        writer.writeStartElement("xsd", "schema", XSD);
        writer.writeNamespace("xsd", XSD);
        writer.writeNamespace("ext", IBM_EXT_NS);

        TypeBuilder builder = new TypeBuilder(ignoreErrors, ignoredPids, locale, isRuntime);

        builder.buildStart();

        for (MetaTypeInformation metatype : definitions) {
            builder.build(metatype);
        }

        builder.buildComplete();

        Collection<OCDType> types = builder.getTypes();

        for (TypeBuilder.OCDType type : types) {
            if ("com.ibm.ws.config".equals(type.getTypeName())) {
                ExtendedObjectClassDefinition ocd = type.getObjectClassDefinition();
                onErrorDefinition = ocd.getAttributeMap().get("onError");
                break;
            }
        }

        Map<String, TypeBuilder.OCDTypeReference> typeReferences = builder.getPidTypeMap();

        for (TypeBuilder.OCDType type : types) {
            if (type.getParentPids() != null) {
                for (String parentPid : type.getParentPids()) {
                    OCDTypeReference parentTypeRef = typeReferences.get(parentPid);
                    if (parentTypeRef != null) {
                        OCDType parentType = parentTypeRef.getOCDType();
                        if (parentType != null && parentType.getSupportsExtensions() && shouldAddOCD(type)) {
                            parentType.addChild(type);
                        }
                    }
                }
            }
        }

        // write types first for each OCD
        for (TypeBuilder.OCDType type : types) {
            if (shouldAddOCD(type)) {
                writeObjectClassType(builder, type);
            }
        }

        writeTypes();

        if (full) {
            writeIncludeType();
            writeVariableDefinitionType();

            writer.writeStartElement(XSD, "complexType");
            writer.writeAttribute("name", SERVER_TYPE);
            writer.writeStartElement(XSD, "choice");
            writer.writeAttribute("minOccurs", "0");
            writer.writeAttribute("maxOccurs", "unbounded");

            // if we add any more elements like include and variable without
            // metatype the com.ibm.ws.kernel.feature.internal.generator.FeatureList
            // class will need to be updated so they are documented correctly.
            writeElement("include", INCLUDE_TYPE);
            writeElement("variable", VARIABLE_DEFINITION_TYPE);
        }

        // write element for each defined pid or factory pid
        for (Map.Entry<String, TypeBuilder.OCDTypeReference> entry : typeReferences.entrySet()) {
            String pid = entry.getKey();
            TypeBuilder.OCDTypeReference type = entry.getValue();
            OCDType ocdType = type.getOCDType();
            if (shouldAddOCD(ocdType)) {
                if (preferShortNames && (ocdType.getAliasName() != null || ocdType.getChildAliasName() != null)) {
                    // write alias only
                    continue;
                }

                if (ocdType.getAliasName() == null) {
                    // Must have an alias name to be considered a top level element
                    continue;
                }

                if (ocdType.hasParentPids() || (ocdType.isPidReferenced() && ocdType.getAliasName() == null) || ocdType.getExtendsAlias() != null) {
                    // nested so not a child of server
                    continue;
                }
                String typeName = type.getOcdTypeName();
                writeElement(pid, typeName);
            }
        }

        // write out any aliases
        // Note, we don't need to deal with childAlias here because they are always nested
        outer: for (Map.Entry<String, List<TypeBuilder.OCDType>> entry : builder.getAliasMap().entrySet()) {
            List<TypeBuilder.OCDType> typesList = entry.getValue();
            if (typesList.size() == 0) {
                continue;
            }
            TypeBuilder.OCDType type = null;
            if (typesList.size() == 1) {
                // non-shared alias - write with right type
                type = typesList.get(0);
                if (type.hasParentPids()) {
                    continue;
                }
            } else {
                for (TypeBuilder.OCDType t : typesList) {
                    if (t.hasParentPids()) {
                        continue;
                    }
                    if (type != null && !!!type.isInternal()) {
                        // shared alias - write out as xsd:anyType for now
                        writeElement(entry.getKey(), "xsd:anyType");
                        continue outer;
                    }
                    type = t;
                }
                if (type == null) {
                    // all were nested
                    continue;
                }
            }
            if (shouldAddOCD(type)) {
                String typeName = type.getTypeName();
                if (type.getHasFactoryReference()) {
                    writeElement(entry.getKey(), typeName + "-factory");
                } else {
                    writeElement(entry.getKey(), typeName);
                }
            }
        }

        if (full) {
            // cannot have wildcard here as it violates UPA
            //            if (generateWildcards) {
            //                writer.writeStartElement(XSD, "any");
            //                writer.writeAttribute("processContents", "skip");
            //                writer.writeEndElement();
            //            }

            writer.writeEndElement(); // close xsd:choice

            writeAttribute("description", "xsd:string", false);
            if (generateWildcards) {
                writeAttributeWildcard();
            }

            writer.writeEndElement(); // close xsd:complexType

            writeElement("client", SERVER_TYPE);
            writeElement("server", SERVER_TYPE);

        }

        writer.writeEndElement();
        writer.writeEndDocument();

        writer.flush();
    }

    /**
     * @param ocdType
     * @return
     */
    private boolean shouldAddOCD(OCDType ocdType) {
        return !!!ocdType.isInternal() &&
               !!!(!beta &&
                   ocdType.isBeta());
    }

    private void writeAttributeWildcard() throws XMLStreamException {
        writer.writeStartElement(XSD, "anyAttribute");
        writer.writeAttribute("processContents", "skip");
        writer.writeEndElement();
    }

    /*
     * String[0] is always name + "Ref", and String[1] is always name.
     */
    private static String[] getReferenceAttributes(String name) {
        if (name.endsWith(SchemaGenConstants.CFG_REFERENCE_SUFFIX)) {
            return new String[] { name, name.substring(0, name.length() - SchemaGenConstants.CFG_REFERENCE_SUFFIX.length()) };
        }
        return new String[] { name + SchemaGenConstants.CFG_REFERENCE_SUFFIX, name };
    }

    private static boolean isSingleCardinality(int cardinality) {
        return (cardinality == 0 || cardinality == 1 || cardinality == -1);
    }

    private void buildTypeMembers(TypeBuilder builder,
                                  TypeBuilder.OCDType ocdType,
                                  Map<String, TypeMember> xmlAttributes,
                                  List<TypeMember> xmlElements) {

        List<ExtendedObjectClassDefinition> ocdDefs = new ArrayList<ExtendedObjectClassDefinition>();

        ExtendedObjectClassDefinition ocd = ocdType.getObjectClassDefinition();

        // If our definition extends another definition, we need to build a list of the definition hierarchy.
        if (ocd.getExtends() != null) {
            ExtendedObjectClassDefinition currOcd = ocd;
            TypeBuilder.OCDType currOcdType = ocdType;
            boolean readExtensions = true;
            // Loop round each type and keep looping until we've reached the top of the hierarchy or if we've hit a problem.
            while (readExtensions) {
                String extension = currOcd.getExtends();
                // Check that we've got extensions and ensure we're a Factory Pid. If so, add the definition to the list, and get the parent type
                // to check on the next loop.
                if (extension != null && (currOcdType.getHasFactoryReference() || currOcdType.getHasIBMFinalWithDefault())) {
                    ocdDefs.add(0, currOcd);
                    TypeBuilder.OCDTypeReference ocdSuperType = builder.getPidType(extension);
                    if (ocdSuperType != null) {
                        // Set the type and Definition for the next loop.
                        currOcdType = ocdSuperType.getOCDType();
                        currOcd = currOcdType.getObjectClassDefinition();
                    } else {
                        // If we've got here we may need to look up an internal PID which won't be in the normal PID map, as we don't
                        // want to write these out. The internal PIDs are stored in order to work out which variables we need to add
                        // to PIDs that extend these internal PIDs.
                        TypeBuilder.OCDTypeReference internalOcdSuperType = builder.getInternalPidType(extension);
                        if (internalOcdSuperType != null) {
                            currOcdType = internalOcdSuperType.getOCDType();
                            currOcd = currOcdType.getObjectClassDefinition();
                        } else {
                            // If we can't find the parent PID then we need to issue a warning and stop writing out the metatype.
                            error("schemagen.invalid.extension.pid", new Object[] { currOcd.getID(), extension });
                            readExtensions = false;
                        }
                    }
                } else {
                    // We will get here if we have extensions but the type isn't a factoryPid, or if we're at the top of the heirarchy tree, and
                    // we've no further extensions. In both cases we need to check whether we're a factory Pid, and issue a warning if we're not.
                    if (!currOcdType.getHasFactoryReference()) {
                        error("schemagen.non.factorypid.extension", currOcd.getID());
                    } else {
                        // If we don't have need to issue a warning we need to add the top level type to the list.
                        ocdDefs.add(0, currOcd);
                    }
                    readExtensions = false;
                }
            }
        } else {
            // If we don't have any extensions then, we still have to process the current definition.
            ocdDefs.add(ocd);
        }

        // _Linked_HashMap to preserve insertion order, for tools
        Map<String, AttributeDefinition> requiredAttributes = new LinkedHashMap<String, AttributeDefinition>();
        Map<String, AttributeDefinition> optionalAttributes = new LinkedHashMap<String, AttributeDefinition>();
        Map<String, ExtendedAttributeDefinition> attributeMap = new LinkedHashMap<String, ExtendedAttributeDefinition>();
        // Now iterate over the definitions, and calculate the rename/final attributes as we go down the heirarchical stack.
        for (ExtendedObjectClassDefinition currDef : ocdDefs) {

            // Add all of the ExtendedAttributeDefinition attributes from the current Defintions to the Map of all attributes.
            // We need these so we can check for the rename/final values.
            attributeMap.putAll(currDef.getAttributeMap());

            // Because the ExtendedAttributeDefintions don't allow us to check whether the attribute is required or not, we have
            // to also get the list of required/optional attrs separately.
            processOCDAttributes(currDef.getID(), currDef.getAttributeDefinitions(ObjectClassDefinition.REQUIRED), requiredAttributes,
                                 optionalAttributes, attributeMap, true);
            processOCDAttributes(currDef.getID(), currDef.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL), requiredAttributes,
                                 optionalAttributes, attributeMap, false);
        }

        buildTypeMembers(builder, ocdType, requiredAttributes.values().toArray(new AttributeDefinition[] {}), true, xmlAttributes, xmlElements);
        buildTypeMembers(builder, ocdType, optionalAttributes.values().toArray(new AttributeDefinition[] {}), false, xmlAttributes, xmlElements);
    }

    /**
     * This method takes a set of Attribute Definitions and processes them against an existing Map of attributes.
     *
     * @param currDefId - A String containing the id of the ObjectClassDefinition that we're processing.
     * @param attrDefs - The Attribute Definitions of the ObjectClassDefinition that we're processing
     * @param currentAttributes - The existing map of properties from previous ObjectClassDefinition's in the heirarchy.
     * @param requiredAttributes - The current list of required Attributes from the processed definitions.
     * @param optionalAttributes - The current list of optional Attributes from the processed definitions.
     * @param attributeMap - The list of ExtendedDefinitionAttributes for all of the OCD's in the heirarchy.
     * @param required - A boolean indicating whether the list of currentAttributes are required or optional.
     */
    private void processOCDAttributes(String currDefId, AttributeDefinition[] attrDefs, Map<String, AttributeDefinition> requiredAttributes,
                                      Map<String, AttributeDefinition> optionalAttributes, Map<String, ExtendedAttributeDefinition> attributeMap,
                                      boolean required) {

        // We need to set the current Attributes Map to be the optional list if we're processing option attrs, or required otherwise. We set the
        // alternativeAttributes to be the other Map of attrs.
        Map<String, AttributeDefinition> currentAttributes;
        Map<String, AttributeDefinition> alternateAttributes;

        if (required) {
            currentAttributes = requiredAttributes;
            alternateAttributes = optionalAttributes;
        } else {
            currentAttributes = optionalAttributes;
            alternateAttributes = requiredAttributes;
        }

        // Iterate over the list of attributes
        for (AttributeDefinition attrDef : attrDefs) {

            String attrId = attrDef.getID();
            ExtendedAttributeDefinition extendedAttr = attributeMap.get(attrId);
            String rename;
            if (extendedAttr != null) {
                // If we have a final attr, we don't add it to the list of attrs to write out, and we should also
                // remove it from the existing list if it was defined in a super type.
                if (extendedAttr.isFinal()) {
                    currentAttributes.remove(attrId);
                    alternateAttributes.remove(attrId);

                } else if ((rename = extendedAttr.getRename()) != null) {
                    // If  we have a rename, then we should take the existing attr, and replace any values that are defined in
                    // in the new attr.
                    boolean alternativeAttr = false;
                    AttributeDefinition attrToRename = currentAttributes.get(rename);
                    // if the attribute to rename is null, then it might be because it has been defined as an optional and is now a required attr,
                    // or visa versa. Before we complain that the attr doesn't exist, check the other list of attrs to see if that is the case.
                    // If so, remove the attr from whichever Map it has been defined in, and add it to the current Attribute Map.
                    // e.g. if we have an renamed attr that is not optional, it may have been defined as required in the super definition.
                    //      if so remove from required attr Map and add to the optional attr Map.
                    if (attrToRename == null) {
                        attrToRename = alternateAttributes.get(rename);
                        alternativeAttr = true;
                    }
                    // If we still haven't found the attr that we're renaming, issue a warning msg, and stop processing this Definition.
                    // Otherwise build up a new attribute based which is a "merge" of new values, and any old values that haven't been defined
                    // in the rename attribute. We add this new one, and remove the original from whichever Map of attributes it was previous
                    // defined in.
                    if (attrToRename == null) {
                        // If we can't find the attribute that we're trying to rename then issue a warning.
                        error("schemagen.rename.attribute.missing", new Object[] { currDefId, rename, extendedAttr.getID() });
                    } else {
                        // Add the new attr to the current list, and remove the old version from the optional or required depending on where it was
                        // defined.
                        currentAttributes.put(attrId, renameAttribute(currDefId, attributeMap.get(rename), extendedAttr, required));
                        if (alternativeAttr)
                            alternateAttributes.remove(rename);
                        else
                            currentAttributes.remove(rename);
                    }
                    // Otherwise we've got no extensions so just add the attribute to the list.
                } else {
                    currentAttributes.put(attrId, attrDef);
                }
            }
        }
    }

    /**
     * This method processes the rename attribute. It builds up a new attribute based on the original attribute, but override any values
     * that have been defined in the new attribute.
     *
     * @param currDefId - A String containing the id of the ObjectClassDefinition that we're processing.
     * @param oldAttribute - The Attribute that is being renamed.
     * @param newAttribute - The new attribute that has the rename extension.
     * @param required - A boolean indicating whether this attribute is required or optional.
     * @return - An AttributeDefinition object that contains the "merged" values.
     */
    private AttributeDefinition renameAttribute(String currDefId, ExtendedAttributeDefinition oldAttribute, ExtendedAttributeDefinition newAttribute, boolean required) {

        AttributeDefinitionSpecification result = new AttributeDefinitionSpecification();

        // Set some original values
        result.setId(newAttribute.getID());
        result.setType(oldAttribute.getType());
        result.setRequired(required);

        if (result.getType() != newAttribute.getType()) {
            // If the type of the new Attribute doesn't match the type of the original we need to issue a warning.
            error("schemagen.invalid.type.override", new Object[] { newAttribute.getID(), currDefId, result.getType() });
        }

        result.setName((newAttribute.getName() != null) ? newAttribute.getName() : oldAttribute.getName());
        result.setDescription((newAttribute.getDescription() != null) ? newAttribute.getDescription() : oldAttribute.getDescription());
        result.setDefaultValue((newAttribute.getDefaultValue() != null) ? newAttribute.getDefaultValue() : oldAttribute.getDefaultValue());

        result.setCardinality((newAttribute.getCardinality() != 0) ? newAttribute.getCardinality() : oldAttribute.getCardinality());

        // We need to build up the valueOptions. We have to do this by getting the labels and options, because there are no methods that
        // return the valueOptions in a list.
        String[] optionLabels = newAttribute.getOptionLabels();
        String[] optionValues = newAttribute.getOptionValues();

        if (optionLabels == null || optionLabels.length == 0)
            optionLabels = oldAttribute.getOptionLabels();

        if (optionValues == null || optionValues.length == 0)
            optionValues = oldAttribute.getOptionValues();

        // We need to carefully build up the arrays that are stored in the list.
        List<String[]> valueOptions = new ArrayList<String[]>();

        if (optionLabels != null) {
            for (int i = 0; i < optionLabels.length; i++) {
                String[] newArray = new String[] { optionValues[i], optionLabels[i] };
                valueOptions.add(newArray);
            }
        }

        result.setValueOptions(valueOptions);

        // We need to build up the extensions into an ExtendableHelper. Again we can get the individual extensions but have to build
        // the map of maps ourselves. We also make sure any overridden extension uris are also replaced in the final map.
        // _Linked_HashMaps are used to preserve insertion order, for tools
        Map<String, Map<String, String>> extensions = new LinkedHashMap<String, Map<String, String>>();
        // Get a list of all the Extension URIs in both the old and new Attribute.
        Set<String> extensionURIs = new LinkedHashSet<String>();
        Set<String> newExtensionURIs = newAttribute.getExtensionUris();

        extensionURIs.addAll(oldAttribute.getExtensionUris());
        extensionURIs.addAll(newExtensionURIs);

        // Now go and get the extensions for each of the URIs.
        for (String currUri : extensionURIs) {
            Map<String, String> currExtensions = new LinkedHashMap<String, String>();
            if (newExtensionURIs.contains(currUri))
                currExtensions.putAll(newAttribute.getExtensions(currUri));
            else
                currExtensions.putAll(oldAttribute.getExtensions(currUri));

            // If we're dealing with the metatype extension uri, remove the rename extension.
            if (SchemaGenConstants.METATYPE_EXTENSION_URI.equals(currUri)) {
                currExtensions.remove("rename");
            }
            extensions.put(currUri, Collections.unmodifiableMap(currExtensions));
        }

        ExtendableHelper helper = new ExtendableHelper(extensions);
        result.setExtendedAttributes(helper);

        return result;
    }

    private void buildTypeMembers(TypeBuilder builder,
                                  TypeBuilder.OCDType ocdType,
                                  AttributeDefinition[] attributes,
                                  boolean required,
                                  Map<String, TypeMember> xmlAttributes,
                                  List<TypeMember> xmlElements) {
        String bundleId = "";
        Bundle b = ocdType.getMetaTypeInformation().getBundle();
        if (b != null) {
            bundleId = b.getSymbolicName() + '/' + b.getVersion();
        }
        if (attributes == null) {
            return;
        }
        for (AttributeDefinition attribute : attributes) {
            // skip internal attributes
            if (attribute.getID().startsWith(SchemaGenConstants.CFG_CONFIG_PREFIX)) {
                continue;
            }

            // Don't process this attribute if it's included in the list of excluded children
            if (ocdType.getExcludedChildren().contains(attribute.getID())) {
                continue;
            }

            ExtendedAttributeDefinition attributeDef = new ExtendedAttributeDefinitionImpl(attribute);
            boolean add = shouldAddAttribute(attributeDef);
            if (attributeDef.getType() == MetaTypeFactory.PID_TYPE || (attributeDef.getType() == AttributeDefinition.STRING && attributeDef.getUIReference() != null)) {
                List<String> referencePids = new ArrayList<String>();
                if (attributeDef.getUIReference() != null) {
                    referencePids.addAll(attributeDef.getUIReference());
                } else {
                    if (attributeDef.getReferencePid() != null) {
                        referencePids.add(attributeDef.getReferencePid());
                    } else if (attributeDef.getService() != null) {
                        referencePids.addAll(builder.getServiceMatches(attributeDef.getService()));
                    } else {
                        error("schemagen.bad.reference.extension", attributeDef.getID());
                        continue;
                    }
                }
                List<OCDTypeReference> ocdReferences = gatherOCDTypeReferences(builder, referencePids);

                boolean requiredForThisAttribute = required;

                // Do not consider childAlias here since it isn't used with parent first nesting
                String[] referenceAttributes = getReferenceAttributes(attributeDef.getID());
                if (add) {
                    Set<String> aliases = new LinkedHashSet<String>(); // _Linked_HashSet to preserve order, for tools
                    // Gather aliases for each OCDTypeReference
                    for (OCDTypeReference ocdReference : ocdReferences) {
                        gatherAliases(ocdReference.getOCDType(), aliases);
                    }
                    if (!!!aliases.isEmpty()) {
                        // we have an alias, generate Ref attribute
                        TypeMember refAttribute = new TypeMember(attributeDef);
                        refAttribute.setID(referenceAttributes[0]);
                        Type type = isSingleCardinality(attributeDef.getCardinality()) ? Type.PID : Type.PID_LIST;
                        refAttribute.setType(type);
                        hasType.add(type);
                        refAttribute.setCardinality(0);
                        requiredForThisAttribute = false;
                        refAttribute.setRequired(requiredForThisAttribute);
                        refAttribute.setDefaultValue(attribute.getDefaultValue());
                        xmlAttributes.put(refAttribute.getID(), refAttribute);
                        processExtensions(ocdType, refAttribute, attributeDef);

                        for (String alias : aliases) {
                            refAttribute.addAppInfoEntry(AppInfoEntry.createReferenceTag(alias));
                        }
                    }
                }
                // make sure we don't generate the nested if we are referencing an internal type, or
                // if we're using ibm:filter
                if (ocdReferences.size() == 1) {
                    OCDTypeReference ocdReference = ocdReferences.get(0);
                    buildRefElement(ocdType, xmlElements, attributeDef, requiredForThisAttribute, referenceAttributes[1], ocdReference.getOCDType(), true);
                }

            } else {
                if (!add) {
                    // do not generate from internal
                    continue;
                }

                String description = attribute.getDescription();
                String name = attribute.getName();
                // validate the NLS type information
                if (description == null || description.length() == 0) {
                    error("schemagen.no.attrib.desc", attribute.getID(), ocdType.getObjectClassDefinition().getID(), bundleId);
                } else if (description.charAt(0) == '%') {
                    error("schemagen.unresolved.attrib.desc", attribute.getID(), ocdType.getObjectClassDefinition().getID(), bundleId);
                }

                if (name == null || name.length() == 0) {
                    error("schemagen.no.attrib.name", attribute.getID(), ocdType.getObjectClassDefinition().getID(), bundleId);
                } else if (name.charAt(0) == '%') {
                    error("schemagen.unresolved.attrib.name", attribute.getID(), ocdType.getObjectClassDefinition().getID(), bundleId);
                }

                TypeMember attr = new TypeMember(attributeDef);
                attr.setID(attributeDef.getID());
                attr.setCardinality(attribute.getCardinality());
                attr.setRequired(required);
                attr.setDefaultValue(attribute.getDefaultValue());
                processExtensions(ocdType, attr, attributeDef);
                if (attribute.getCardinality() == 0) {
                    xmlAttributes.put(attr.getID(), attr);
                } else {
                    xmlElements.add(attr);
                }
            }
        }
    }

    /**
     * @param attributeDef
     * @return
     */
    private boolean shouldAddAttribute(ExtendedAttributeDefinition attributeDef) {
        return !"internal".equals(attributeDef.getName()) &&
               !!!(!beta &&
                   attributeDef.isBeta());
    }

    private void buildRefElement(TypeBuilder.OCDType ocdType, List<TypeMember> xmlElements, ExtendedAttributeDefinition attributeDef, boolean requiredForThisAttribute,
                                 String baseId, OCDType ocdReference, boolean topLevel) {
        if (generateNested(attributeDef) && !!!ocdReference.isInternal() && (topLevel || (ocdReference.getExtendsAlias() != null))
            && !(topLevel && !shouldAddAttribute(attributeDef))) {
            TypeMember refElement = new TypeMember(attributeDef);
            if (topLevel) {
                refElement.setID(baseId);
            } else {
                String extendsAlias = ocdReference.getExtendsAlias();
                if (extendsAlias.startsWith("!")) {
                    refElement.setID(extendsAlias.substring(1));
                } else {
                    refElement.setID(baseId + "." + extendsAlias);
                }
            }
            refElement.setType((Type) null);
            refElement.setCardinality(attributeDef.getCardinality() == 0 ? 1 : attributeDef.getCardinality());
            String typeName = null;

            if (ocdReference.getHasIBMFinalWithDefault() == false) {
                typeName = refElement.getCardinality() == 1 ? ocdReference.getTypeName() : ocdReference.getTypeName() + "-factory";
            } else {
                typeName = refElement.getCardinality() == 1 ? ocdReference.getTypeName() : ocdReference.getTypeName();
            }

            refElement.setType(typeName);
            refElement.setRequired(requiredForThisAttribute);
            xmlElements.add(refElement);
            processExtensions(ocdType, refElement, attributeDef);
            /*
             * //TODO there are a few errors if this is generated, should it be? it seems fairly useful?
             * if ("internal".equals(attributeDef.getName())) {
             * ExtendedObjectClassDefinition ocd = ocdReference.getObjectClassDefinition();
             * refElement.setDescription(ocd.getDescription());
             * refElement.addAppInfoEntry(AppInfoEntry.createLabelTag(ocd.getName(), null));
             * }
             */
        }
        for (OCDType extender : ocdReference.getExtensions()) {
            if (shouldAddOCD(extender)) {
                buildRefElement(ocdType, xmlElements, attributeDef, requiredForThisAttribute, baseId, extender, false);
            }
        }
    }

    /**
     * @param referencePids
     * @return
     */
    private List<OCDTypeReference> gatherOCDTypeReferences(TypeBuilder builder, List<String> referencePids) {
        List<OCDTypeReference> references = new ArrayList<OCDTypeReference>();

        for (String referencePid : referencePids) {
            OCDTypeReference ocdReference = builder.getPidTypeMap().get(referencePid);
            if (ocdReference == null) {
                // if the reference is null, we may be extending an internal PID, so we need to check that map.
                ocdReference = builder.getInternalPidTypeMap().get(referencePid);
                if (ocdReference == null) {
                    error("schemagen.bad.reference.pid", referencePid);
                    continue;
                }
            }
            references.add(ocdReference);
        }

        return references;
    }

    private void gatherAliases(OCDType ocdType, Set<String> aliases) {
        if (!!!ocdType.isInternal()) {
            String alias = ocdType.getAliasName();
            if (alias != null) {
                aliases.add(alias);
            }
        }

        for (OCDType child : ocdType.getExtensions()) {
            if (shouldAddOCD(child)) {
                gatherAliases(child, aliases);
            }
        }
    }

    private boolean generateNested(ExtendedAttributeDefinition attributeDef) {
        // Temporary work-around, see Defect 70372 for details
        if ("loginModuleRef".equals(attributeDef.getID()) &&
            "com.ibm.ws.security.authentication.internal.jaas.jaasLoginModuleConfig".equals(attributeDef.getReferencePid())) {
            return false;
        } else if ("taskStoreRef".equals(attributeDef.getID()) &&
                   "com.ibm.ws.persistence.databaseStore".equals(attributeDef.getReferencePid())) {
            // Another temporary workaround until ibm:extends/refines are available. See 164774
            return false;
        } else if (attributeDef.getService() != null || attributeDef.getUIReference() != null) {
            return false;
        } else {
            return true;
        }
    }

    private void processExtensions(TypeBuilder.OCDType ocdType, TypeMember attr, ExtendedAttributeDefinition attributeDef) {
        if (shouldAddAttribute(attributeDef)) {
            String description;
            int type = attributeDef.getType();
            if (type == MetaTypeFactory.DURATION_TYPE) {
                description = combineStrings(attributeDef.getDescription(), resourceBundle.getString("config.internal.metatype.duration.desc"));
            } else if (type == MetaTypeFactory.DURATION_H_TYPE) {
                description = combineStrings(attributeDef.getDescription(), resourceBundle.getString("config.internal.metatype.duration-h.desc"));
            } else if (type == MetaTypeFactory.DURATION_M_TYPE) {
                description = combineStrings(attributeDef.getDescription(), resourceBundle.getString("config.internal.metatype.duration-m.desc"));
            } else if (type == MetaTypeFactory.DURATION_S_TYPE) {
                description = combineStrings(attributeDef.getDescription(), resourceBundle.getString("config.internal.metatype.duration-s.desc"));
            } else {
                description = attributeDef.getDescription();
            }
            attr.setDescription(description);
            String name = attributeDef.getName();
            if (name != null) {
                attr.addAppInfoEntry(AppInfoEntry.createLabelTag(name, attributeDef.getAttributeName()));
            }
        }
        String requiresFalse = attributeDef.getRequiresFalse();
        if (requiresFalse != null) {
            attr.addAppInfoEntry(AppInfoEntry.createRequiresTag(requiresFalse, false));
        }
        String requiresTrue = attributeDef.getRequiresTrue();
        if (requiresTrue != null) {
            attr.addAppInfoEntry(AppInfoEntry.createRequiresTag(requiresTrue, true));
        }
        String group = attributeDef.getGroup();
        if (group != null) {
            attr.addAppInfoEntry(AppInfoEntry.createGroupTag(group));
            ocdType.addGroup(group);
        }
        String variable = attributeDef.getVariable();
        if (variable != null) {
            attr.addAppInfoEntry(AppInfoEntry.createVariableTag(variable));
        }

        String unique = attributeDef.getUniqueCategory();
        if (unique != null) {
            attr.addAppInfoEntry(AppInfoEntry.createUniqueTag(unique));
        }
    }

    private void writeObjectClassType(TypeBuilder builder, TypeBuilder.OCDType type) throws XMLStreamException {

        writer.writeStartElement(XSD, "complexType");
        writer.writeAttribute("name", type.getTypeName());

        Map<String, TypeMember> singleValued = new LinkedHashMap<String, TypeMember>(); // _Linked_HashMap to preserve order, for tools
        List<TypeMember> multiValued = new ArrayList<TypeMember>();

        buildTypeMembers(builder, type, singleValued, multiValued);

        writeDocumentation(type, type.getObjectClassDefinition().getDescription(), type.getAppInfoEntries());
        Collection<OCDType> children = type.getChildren();
        int numKnownChildren = multiValued.size() + children.size();
        int numAnyChildren = type.getXsdAny();
        int numChildren = numKnownChildren + numAnyChildren;
        if (numChildren != 0) {
            boolean singleton = numChildren == 1;
            if (singleton) {
                writer.writeStartElement(XSD, "sequence");
            } else {
                writer.writeStartElement(XSD, "choice");
                writer.writeAttribute("minOccurs", "0");
                writer.writeAttribute("maxOccurs", "unbounded");
            }

            boolean writeKnown = numKnownChildren != 0;
            boolean writeAny = numAnyChildren != 0;
            String writeAnyMaxOccurs;

            // XSD 1.1 supports xsd:choice with xsd:any and other elements, but
            // XSD 1.0 does not.
            if (schemaVersion == SchemaVersion.v1_0 && writeKnown && writeAny) {
                if (outputVersion == OutputVersion.v1) {
                    // For output version 1, write explicit children so that
                    // tools have precise type information at the expense of
                    // failing XSD validation for unknown elements.
                    writeAny = false;
                    writeAnyMaxOccurs = null;
                } else {
                    // For output version 2, write xsd:any so that unknown
                    // elements pass XSD validation at the expense of losing
                    // validation for known elements.  Use maxOccurs="unbounded"
                    // to allow all known elements; we could count them, but
                    // we've already given up validation, so it's not worth it.
                    writeKnown = false;
                    writeAnyMaxOccurs = "unbounded";
                }
            } else {
                writeAnyMaxOccurs = writeAny ? Integer.toString(numAnyChildren) : null;
            }

            if (writeKnown) {
                for (TypeMember member : multiValued) {
                    // will only output these ocd attributes as xsd:elements
                    writeTypeMember(member, singleton);
                }

                for (TypeBuilder.OCDType child : children) {
                    writeNestedElement(child, singleton);
                }
            }

            if (writeAny) {
                writer.writeEmptyElement(XSD, "any");
                writer.writeAttribute("processContents", "skip");
                writer.writeAttribute("minOccurs", "0");
                writer.writeAttribute("maxOccurs", writeAnyMaxOccurs);
            }

            writer.writeEndElement();
        } else {
            // XSD idiom recommended by Sandy Gao: A simple empty <xsd:sequence/> would be ignored as meaning "no children".
            // The nested empty-sequence is taken as meaning "no children after whitespace handling", and the default is to discard whitespace.
            writer.writeComment("Idiom for \"empty element context\" (whitespace accepted and ignored)");
            writer.writeStartElement(XSD, "sequence");
            writer.writeEmptyElement(XSD, "sequence");
            writer.writeEndElement();
        }

        // write any remaining single-valued attributes as xsd attributes
        for (TypeMember member : singleValued.values()) {
            writeTypeMember(member, false);
        }

        if (type.getHasExtraProperties()) {
            writeInternalPropertiesType(type);
        }

        if (generateWildcards) {
            writeAttributeWildcard();
        }

        writer.writeEndElement();

        if (type.getHasFactoryReference()) {
            writer.writeStartElement(XSD, "complexType");
            writer.writeAttribute("name", type.getTypeName() + "-factory");
            writer.writeStartElement(XSD, "complexContent");
            writer.writeStartElement(XSD, "extension");
            writer.writeAttribute("base", type.getTypeName());

            TypeMember id = singleValued.get(SchemaGenConstants.CFG_INSTANCE_ID);
            if (id == null) {//not null will be included in non -factory complex type above
                // write definition for "id" attribute
                writer.writeStartElement(XSD, "attribute");
                writer.writeAttribute("name", SchemaGenConstants.CFG_INSTANCE_ID);
                writer.writeAttribute("type", FACTORY_ID_TYPE);
                writer.writeAttribute("use", "optional");
                String doc = resourceBundle.getString("config.internal.metatype.id.documentation");
                String label = resourceBundle.getString("config.internal.metatype.id.label");
                writeDocumentation(doc, label);
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            restrictedTypes.add(FACTORY_ID_TYPE);
        }

    }

    private void writeNestedElement(TypeBuilder.OCDType type, boolean addMinMax) throws XMLStreamException {
        writer.writeStartElement(XSD, "element");
        writer.writeAttribute("name", type.getAliasName() == null ? type.getChildAliasName() : type.getAliasName());//TODO extendsAlias??
        if (addMinMax) {
            writer.writeAttribute("minOccurs", "0");
            writer.writeAttribute("maxOccurs", "unbounded");
        }
        writer.writeAttribute("type", type.getTypeName());
        writer.writeEndElement();
    }

    private void writeTypeMember(final TypeMember member, boolean addCardinalityMinMax) throws XMLStreamException {
        ExtendedAttributeDefinition attribute = member.getAttribute();
        boolean required = member.isRequired();
        boolean alternateLabel = false;
        // if there is a default value, make the attribute optional
        String[] defaultValue = member.getDefaultValue();
        if (defaultValue != null && defaultValue.length > 0) {
            required = false;
        }

        int cardinality = member.getCardinality();
        if (cardinality == 0) {
            // expressed as an attribute
            writer.writeStartElement(XSD, "attribute");
            writer.writeAttribute("name", member.getID());
            if (required) {
                writer.writeAttribute("use", "required");
            } else {
                writer.writeAttribute("use", "optional");
            }
            alternateLabel = true;
        } else {
            // expressed as an element
            writer.writeStartElement(XSD, "element");
            writer.writeAttribute("name", member.getID());
            if (addCardinalityMinMax) {
                writer.writeAttribute("minOccurs", required ? "1" : "0");
                String max = (cardinality == Integer.MAX_VALUE || cardinality == Integer.MIN_VALUE) ? "unbounded" : String.valueOf(Math.abs(cardinality));
                writer.writeAttribute("maxOccurs", max);
            }
        }

        if (defaultValue != null && defaultValue.length > 0) {
            if (cardinality != 0)
                writer.writeAttribute("default", defaultValue[0]);
            else { //Multiple default value case in a simple string.

                //Concatenates individual defaultValues into one string.
                //Example ",spec,ibm-api" notice the leading comma to reduce loop complexity.
                StringBuffer concatDefaultValue = new StringBuffer();

                for (int i = 0; i < defaultValue.length; ++i)
                    concatDefaultValue.append("," + defaultValue[i]);

                //Then remove the leading comma after processing
                String multiDefaultValue = concatDefaultValue.substring(1);

                //Write full default value, sans leading comma.
                writer.writeAttribute("default", multiDefaultValue);
            }
        }

        // If the type is onError then we should generate the onError enum.
        AttributeDefinition optionAD = attribute.getType() == MetaTypeFactory.ON_ERROR_TYPE ? onErrorDefinition : attribute;

        String[] optionValues = optionAD != null ? optionAD.getOptionValues() : null;

        final boolean altLabel = alternateLabel;
        DocumentationWriter docWriter = new DocumentationWriter() {
            @Override
            public void writeDoc() throws XMLStreamException {
                writeDocumentation(altLabel, member.getDescription(), member.getAppInfoEntries());
            }
        };

        if (optionValues != null && optionValues.length > 0) {
            String[] optionLables = optionAD.getOptionLabels();

            writeDocumentation(member.getDescription(), member.getAppInfoEntries());
            writer.writeStartElement(XSD, "simpleType");
            if (outputVersion == OutputVersion.v2) {
                writer.writeStartElement(SchemaWriter.XSD, "union");
                writer.writeAttribute("memberTypes", "variableType");
                writer.writeStartElement(XSD, "simpleType");
            }

            writer.writeStartElement(XSD, "restriction");
            writer.writeAttribute("base", member.getType(true));
            hasType.add(member.getType());

            for (int i = 0; i < optionValues.length; i++) {
                writer.writeStartElement(XSD, "enumeration");
                writer.writeAttribute("value", optionValues[i]);
                if (optionLables != null && i < optionLables.length) {
                    writeDocumentation(optionLables[i]);
                }
                writer.writeEndElement(); // enumeration
            }

            writer.writeEndElement(); // restriction
            if (outputVersion == OutputVersion.v2) {
                writer.writeEndElement(); // simpleType
                writer.writeEndElement(); // union
            }
            writer.writeEndElement(); // simpleType
        } else if (outputVersion == OutputVersion.v2 && member.isMinMaxSet()) {
            member.getType().writeType(writer, member.getMin(), member.getMax(), docWriter);
        } else {
            Type t = member.getType();
            if (t != null) {
                t.writeType(writer, docWriter);
                hasType.add(t);
            } else {
                writer.writeAttribute("type", member.getType(true));
                writeDocumentation(alternateLabel, member.getDescription(), member.getAppInfoEntries());
            }
        }

        writer.writeEndElement();
    }

    private void writeInternalPropertiesType(TypeBuilder.OCDType type) throws XMLStreamException {
        writer.writeStartElement(XSD, "attribute");
        writer.writeAttribute("name", INTERNAL_PROPERTIES);
        writer.writeAttribute("type", INTERNAL_PROPERTIES_TYPE);
        writer.writeAttribute("use", "optional");

        String doc = type.getTranslatedText(type.getDescriptionKeys("extraProperties"));
        String label = type.getTranslatedText(type.getLabelKeys("extraProperties"));
        writeDocumentation(doc, label);

        writer.writeEndElement();

        restrictedTypes.add(INTERNAL_PROPERTIES_TYPE);
    }

    private void writeTypes() throws XMLStreamException {

        for (Type t : hasType) {
            t.writeGlobalType(writer);
        }

        for (String type : restrictedTypes) {
            writeRestrictedType(type, "xsd:string");
        }

        writeRestrictedType("schemaPropertiesType", "xsd:string");
    }

    private void writeRestrictedType(String name, String type) throws XMLStreamException {
        writer.writeStartElement(XSD, "simpleType");
        writer.writeAttribute("name", name);

        writer.writeEmptyElement(XSD, "restriction");
        writer.writeAttribute("base", type);

        writer.writeEndElement();
    }

    private void writeIncludeType() throws XMLStreamException {
        String doc, label;

        writer.writeStartElement(XSD, "complexType");
        writer.writeAttribute("name", INCLUDE_TYPE);
        doc = resourceBundle.getString("config.internal.metatype.includeType.documentation");
        label = resourceBundle.getString("config.internal.metatype.includeType.label");
        writeDocumentation(doc, label);

        writer.writeStartElement(XSD, "attribute");
        writer.writeAttribute("name", "optional");
        writer.writeAttribute("type", "xsd:boolean");
        writer.writeAttribute("use", "optional");
        writer.writeAttribute("default", "false");
        doc = resourceBundle.getString("config.internal.metatype.includeType.attribute.optional.documentation");
        label = resourceBundle.getString("config.internal.metatype.includeType.attribute.optional.label");
        writeDocumentation(doc, label);
        writer.writeEndElement();

        writer.writeStartElement(XSD, "attribute");
        writer.writeAttribute("name", "location");
        writer.writeAttribute("type", "location");
        writer.writeAttribute("use", "required");
        doc = resourceBundle.getString("config.internal.metatype.includeType.attribute.location.documentation");
        label = resourceBundle.getString("config.internal.metatype.includeType.attribute.location.label");
        writeDocumentation(doc, label);
        writer.writeEndElement();

        writer.writeStartElement(XSD, "attribute");
        writer.writeAttribute("name", "onConflict");
        writer.writeAttribute("use", "optional");
        writer.writeAttribute("default", "MERGE");

        doc = resourceBundle.getString("config.internal.metatype.includeType.attribute.onConflict.documentation");
        label = resourceBundle.getString("config.internal.metatype.includeType.attribute.onConflict.label");
        writeDocumentation(doc, label);

        String[] optionValues = { "MERGE", "REPLACE", "IGNORE" };
        String[] optionLabels = { "config.internal.metatype.onConflictType.merge.label",
                                  "config.internal.metatype.onConflictType.replace.label",
                                  "config.internal.metatype.onConflictType.ignore.label" };

        writer.writeStartElement(XSD, "simpleType");
        if (outputVersion == OutputVersion.v2) {
            writer.writeStartElement(SchemaWriter.XSD, "union");
            writer.writeAttribute("memberTypes", "variableType");
            writer.writeStartElement(XSD, "simpleType");
        }

        writer.writeStartElement(XSD, "restriction");
        writer.writeAttribute("base", "xsd:string");

        for (int i = 0; i < optionValues.length; i++) {
            writer.writeStartElement(XSD, "enumeration");
            writer.writeAttribute("value", optionValues[i]);
            writeDocumentation(resourceBundle.getString(optionLabels[i]));
            writer.writeEndElement(); // enumeration
        }

        writer.writeEndElement(); // restriction
        if (outputVersion == OutputVersion.v2) {
            writer.writeEndElement(); // simpleType
            writer.writeEndElement(); // union
        }
        writer.writeEndElement(); // simpleType
        writer.writeEndElement(); // attribute

        writer.writeEndElement(); // complexType
    }

    private void writeVariableDefinitionType() throws XMLStreamException {
        String doc, label;

        writer.writeStartElement(XSD, "complexType");
        writer.writeAttribute("name", VARIABLE_DEFINITION_TYPE);
        doc = resourceBundle.getString("config.internal.metatype.variableDefinitionType.documentation");
        label = resourceBundle.getString("config.internal.metatype.variableDefinitionType.label");
        writeDocumentation(doc, label);

        writer.writeStartElement(XSD, "attribute");
        writer.writeAttribute("name", "name");
        writer.writeAttribute("type", "xsd:string");
        writer.writeAttribute("use", "required");
        doc = resourceBundle.getString("config.internal.metatype.variableDefinitionType.name.documentation");
        label = resourceBundle.getString("config.internal.metatype.variableDefinitionType.name.label");
        writeDocumentation(doc, label);
        writer.writeEndElement();

        writer.writeStartElement(XSD, "attribute");
        writer.writeAttribute("name", "value");
        writer.writeAttribute("type", "xsd:string");

        doc = resourceBundle.getString("config.internal.metatype.variableDefinitionType.value.documentation");
        label = resourceBundle.getString("config.internal.metatype.variableDefinitionType.value.label");
        writeDocumentation(doc, label);
        writer.writeEndElement();

        writer.writeStartElement(XSD, "attribute");
        writer.writeAttribute("name", "defaultValue");
        writer.writeAttribute("type", "xsd:string");
        doc = resourceBundle.getString("config.internal.metatype.variableDefinitionType.defaultValue.documentation");
        label = resourceBundle.getString("config.internal.metatype.variableDefinitionType.defaultValue.label");
        writeDocumentation(doc, label);
        writer.writeEndElement();

        writer.writeEndElement();
    }

    private void writeDocumentation(String documentation, String label) throws XMLStreamException {
        if (!generateDocumentation) {
            return;
        }
        writeDocumentation(documentation, AppInfoEntry.createLabelTag(label, label));
    }

    private void writeDocumentation(TypeBuilder.OCDType type, String description, AppInfoEntry... appInfoEntries) throws XMLStreamException {
        writeDocumentation(type, false, description, appInfoEntries);
    }

    private void writeDocumentation(String description, AppInfoEntry... appInfoEntries) throws XMLStreamException {
        writeDocumentation(null, false, description, appInfoEntries);
    }

    private void writeDocumentation(boolean alternate, String description, AppInfoEntry... appInfoEntries) throws XMLStreamException {
        writeDocumentation(null, alternate, description, appInfoEntries);
    }

    private void writeDocumentation(TypeBuilder.OCDType type, boolean alternate, String description, AppInfoEntry... appInfoEntries) throws XMLStreamException {
        if (!generateDocumentation) {
            return;
        }
        if (description == null && (appInfoEntries == null || appInfoEntries.length == 0)) {
            return;
        }

        writer.writeStartElement(XSD, "annotation");

        if (description != null) {
            writer.writeStartElement(XSD, "documentation");
            writer.writeCharacters(description);
            writer.writeEndElement();
        }

        if (appInfoEntries != null && appInfoEntries.length > 0) {
            writer.writeStartElement(XSD, "appinfo");

            if (type != null && type.getAction() != null && type.getAction().equals(GENERATE_SCHEMA_ACTION)) {
                writeSchemaGenAnnotations(type, alternate);
            }
            for (AppInfoEntry appInfoEntry : appInfoEntries) {
                appInfoEntry.write(writer, alternate);
            }

            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeElement(String name, String type) throws XMLStreamException {
        writer.writeEmptyElement(XSD, "element");
        writer.writeAttribute("name", name);
        writer.writeAttribute("type", type);
    }

    private void writeAttribute(String name, String type, boolean required) throws XMLStreamException {
        writer.writeEmptyElement(XSD, "attribute");
        writer.writeAttribute("name", name);
        writer.writeAttribute("type", type);
        writer.writeAttribute("use", (required) ? "required" : "optional");
    }

    private static String combineStrings(String one, String two) {
        one = trim(one);
        two = trim(two);
        if (isEmpty(one)) {
            return (isEmpty(two)) ? null : two;
        } else if (isEmpty(two)) {
            return one;
        } else {
            if (!one.endsWith(".")) {
                return one + ". " + two;
            } else {
                return one + " " + two;
            }
        }
    }

    private static String trim(String str) {
        return (str == null) ? null : str.trim();
    }

    private static boolean isEmpty(String str) {
        return (str == null || str.length() == 0);
    }

    private void error(String message, Object... args) {
        if (!isRuntime) {
            // Don't generate an error when this is being run by the mbean. Not all
            // bundles may be included by a running server.
            String msg = message;
            try {
                msg = _msgs.getString(message);
            } catch (MissingResourceException mre) {
                // Ignore this, we just output the message key if we get here.
            }
            if (args.length > 0) {
                msg = MessageFormat.format(msg, args);
            }
            // Tr isn't initialized if isRuntime is false.
            System.err.println(msg);
        }
        if (!ignoreErrors) {
            throw new RuntimeException("Error during schema generation");
        }
    }

    /**
     * @param schemaVersion
     */
    public void setSchemaVersion(SchemaVersion schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    /**
     * @param outputVersion
     */
    public void setOutputVersion(OutputVersion outputVersion) {
        this.outputVersion = outputVersion;
    }

    /**
     * This method adds additional annotations to required schema elements so that WDT is aware
     * that it needs to invoke server specific schema generation and use the generated schema.
     *
     * @param action The action that should be invoked by WDT on encountering this element
     * @param currentType The current OCD type
     * @throws XMLStreamException
     */
    private void writeSchemaGenAnnotations(OCDType type, boolean alternate) throws XMLStreamException {
        AppInfoEntry tag = null;
        //Resource adapter and all sub types which have action set as generateSchema
        if (type != null) {
            tag = new AppInfoEntry("action");
            tag.addAttribute("type", "generateSchema");

            //Embedded resource adapter element
            if ("resourceAdapter".equals(type.getChildAliasName())) {
                tag.addAttribute("elemPrefix", "properties.");
                tag.addAttribute("idAttr", "${parent.name}.${id}");
                tag.addAttribute("alternateIdAttr", "${parent.name}.${alias}");
            }
            //ResourceAdapter element
            else if ("resourceAdapter".equals(type.getAliasName())) {
                tag.addAttribute("elemPrefix", "properties.");
                tag.addAttribute("idAttr", "${id}");
                tag.addAttribute("alternateIdAttr", "${location}");
            }
            tag.write(writer, alternate);
        }
    }
}
