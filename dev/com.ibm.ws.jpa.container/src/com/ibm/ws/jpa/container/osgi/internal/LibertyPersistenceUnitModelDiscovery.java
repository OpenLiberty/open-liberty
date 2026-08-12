/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ws.container.service.annocache.ContainerAnnotations;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.InfoStore;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;

/**
 * Persistence-unit-scoped model discovery backed by Liberty deployment metadata.
 *
 */
final class LibertyPersistenceUnitModelDiscovery implements PersistenceUnitModelDiscovery {
    private static final String DISCOVERABLE = "jakarta.persistence.spi.Discoverable";
    private static final String REPOSITORY = "jakarta.data.repository.Repository";
    private static final String DEFAULT_MAPPING_FILE = "META-INF/orm.xml";
    private static final int SEED = AnnotationTargets_Targets.POLICY_SEED;

    @Override
    public Result discover(Context context) {
        LinkedHashSet<String> complete = new LinkedHashSet<String>();
        complete.addAll(context.getExplicitClassNames());

        parseDefaultMapping(context, complete);
        for (String mappingFileName : context.getMappingFileNames()) {
            InputStream stream = context.getMappingResourceAccess().open(mappingFileName);
            if (stream == null) {
                throw failure(context, mappingFileName, "Mapping resource could not be resolved", null);
            }
            parseMapping(context, mappingFileName, stream, complete);
        }

        if (context.isDiscoverRoot()) {
            discoverSource(context, context.getRootSource(), complete);
        }
        for (Source source : context.getReferencedSources()) {
            discoverSource(context, source, complete);
        }

        return new Result(new ArrayList<String>(complete));
    }

    private void parseDefaultMapping(Context context, Set<String> complete) {
        Source root = context.getRootSource();
        Entry entry = root.getContainer().getEntry(resourcePath(root, DEFAULT_MAPPING_FILE));
        if (entry == null) {
            return;
        }

        try {
            parseMapping(context, DEFAULT_MAPPING_FILE, entry.adapt(InputStream.class), complete);
        } catch (UnableToAdaptException e) {
            throw failure(context, root.getDiagnosticName(), "Default mapping resource could not be read", e);
        }
    }

    private void parseMapping(Context context, String resourceName, InputStream stream, Set<String> complete) {
        try (InputStream mappingStream = stream) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            MappingClassHandler handler = new MappingClassHandler();
            factory.newSAXParser().parse(new InputSource(mappingStream), handler);
            complete.addAll(handler.getClassNames());
        } catch (Exception e) {
            throw failure(context, resourceName, "Mapping resource could not be parsed", e);
        }
    }

    private void discoverSource(Context context, Source source, Set<String> complete) {
        ContainerAnnotations annotations;
        try {
            annotations = source.getContainer().adapt(ContainerAnnotations.class);
        } catch (UnableToAdaptException e) {
            throw failure(context, source.getDiagnosticName(), "Annotation metadata could not be obtained", e);
        }
        if (annotations == null) {
            throw failure(context, source.getDiagnosticName(), "Annotation metadata adapter returned null", null);
        }

        annotations.setAppName(context.getApplicationName());
        annotations.setModName(context.getModuleName());
        annotations.setIsUnnamedMod(false);
        annotations.setIsLightweight(true);
        annotations.setEntryPrefix(source.getEntryPrefix());
        annotations.setUseJandex(context.isUseJandex());
        annotations.setClassLoader(context.getClassLoader());

        AnnotationTargets_Targets targets = annotations.getTargets();
        InfoStore infoStore = annotations.getInfoStore();
        if (targets == null || infoStore == null) {
            throw failure(context, source.getDiagnosticName(), "Annotation scan did not produce metadata", null);
        }

        Set<String> candidateAnnotations = new LinkedHashSet<String>();
        candidateAnnotations.addAll(targets.getClassAnnotations(SEED));
        candidateAnnotations.addAll(targets.getPackageAnnotations(SEED));

        Set<String> moduleAnnotations = readModuleAnnotations(context, source);
        candidateAnnotations.addAll(moduleAnnotations);

        List<String> discovered = new ArrayList<String>();
        annotations.openInfoStore();
        try {
            for (String annotationName : candidateAnnotations) {
                if (!isDiscoverable(context, source, annotationName, infoStore)) {
                    continue;
                }
                discovered.addAll(targets.getAnnotatedClasses(annotationName, SEED));
                for (String packageName : targets.getAnnotatedPackages(annotationName, SEED)) {
                    discovered.add(packageName + ".package-info");
                }
                if (moduleAnnotations.contains(annotationName)) {
                    discovered.add("module-info");
                }
            }
        } finally {
            annotations.closeInfoStore();
        }

        Collections.sort(discovered);
        complete.addAll(discovered);
    }

    private boolean isDiscoverable(Context context, Source source, String annotationName, InfoStore infoStore) {
        if (REPOSITORY.equals(annotationName)) {
            return true;
        }

        ClassInfo annotationInfo = infoStore.getDelayableClassInfo(annotationName);
        if (annotationInfo == null || annotationInfo.isArtificial()) {
            throw failure(context,
                          source.getDiagnosticName(),
                          "Annotation type metadata could not be resolved: " + annotationName,
                          null);
        }
        return annotationInfo.isDeclaredAnnotationPresent(DISCOVERABLE);
    }

    private Set<String> readModuleAnnotations(Context context, Source source) {
        Entry entry = source.getContainer().getEntry(resourcePath(source, "module-info.class"));
        if (entry == null) {
            return Collections.emptySet();
        }

        try (InputStream stream = entry.adapt(InputStream.class)) {
            return ClassFileAnnotations.read(stream);
        } catch (Exception e) {
            throw failure(context, source.getDiagnosticName(), "Root module descriptor could not be scanned", e);
        }
    }

    private static String resourcePath(Source source, String resourceName) {
        return source.getEntryPrefix() == null ? resourceName : source.getEntryPrefix() + resourceName;
    }

    private static DiscoveryException failure(Context context, String resource, String message, Throwable cause) {
        String detail = message + " [application=" + context.getApplicationName()
                        + ", module=" + context.getModuleName()
                        + ", persistence-unit=" + context.getPersistenceUnitName()
                        + ", resource=" + resource + "]";
        return new DiscoveryException(detail, cause);
    }

    private static final class MappingClassHandler extends DefaultHandler {
        private final List<String> classNames = new ArrayList<String>();
        private String packageName;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String elementName = localName == null || localName.isEmpty() ? qName : localName;
            int separator = elementName.indexOf(':');
            if (separator != -1) {
                elementName = elementName.substring(separator + 1);
            }

            if ("entity-mappings".equals(elementName)) {
                packageName = trimToNull(attributes.getValue("package"));
            } else if ("entity".equals(elementName)
                       || "mapped-superclass".equals(elementName)
                       || "embeddable".equals(elementName)
                       || "converter".equals(elementName)) {
                String className = trimToNull(attributes.getValue("class"));
                if (className != null) {
                    classNames.add(qualify(className));
                }
            }
        }

        List<String> getClassNames() {
            return classNames;
        }

        private String qualify(String className) {
            return packageName != null && className.indexOf('.') == -1 ? packageName + "." + className : className;
        }

        private static String trimToNull(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }

    /**
     * Minimal class-file reader for annotations on a root module descriptor.
     */
    private static final class ClassFileAnnotations {
        private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;

        static Set<String> read(InputStream stream) throws IOException {
            DataInputStream input = new DataInputStream(stream);
            if (input.readInt() != CLASS_FILE_MAGIC) {
                throw new IOException("Invalid class-file header");
            }
            input.readUnsignedShort();
            input.readUnsignedShort();

            Object[] constantPool = readConstantPool(input);
            input.readUnsignedShort();
            input.readUnsignedShort();
            input.readUnsignedShort();
            skipU2Table(input);
            skipMembers(input);
            skipMembers(input);

            Set<String> annotationNames = new LinkedHashSet<String>();
            int attributeCount = input.readUnsignedShort();
            for (int i = 0; i < attributeCount; i++) {
                String attributeName = utf8(constantPool, input.readUnsignedShort());
                int length = input.readInt();
                if ("RuntimeVisibleAnnotations".equals(attributeName)
                    || "RuntimeInvisibleAnnotations".equals(attributeName)) {
                    readAnnotations(input, constantPool, annotationNames);
                } else {
                    skipFully(input, length);
                }
            }
            return annotationNames;
        }

        private static Object[] readConstantPool(DataInputStream input) throws IOException {
            Object[] constantPool = new Object[input.readUnsignedShort()];
            for (int i = 1; i < constantPool.length; i++) {
                int tag = input.readUnsignedByte();
                switch (tag) {
                    case 1:
                        constantPool[i] = input.readUTF();
                        break;
                    case 3:
                    case 4:
                        skipFully(input, 4);
                        break;
                    case 5:
                    case 6:
                        skipFully(input, 8);
                        i++;
                        break;
                    case 7:
                    case 8:
                    case 16:
                    case 19:
                    case 20:
                        skipFully(input, 2);
                        break;
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 17:
                    case 18:
                        skipFully(input, 4);
                        break;
                    case 15:
                        skipFully(input, 3);
                        break;
                    default:
                        throw new IOException("Unknown constant-pool tag " + tag);
                }
            }
            return constantPool;
        }

        private static void skipMembers(DataInputStream input) throws IOException {
            int count = input.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                skipFully(input, 6);
                int attributes = input.readUnsignedShort();
                for (int j = 0; j < attributes; j++) {
                    skipFully(input, 2);
                    skipFully(input, input.readInt());
                }
            }
        }

        private static void skipU2Table(DataInputStream input) throws IOException {
            skipFully(input, input.readUnsignedShort() * 2);
        }

        private static void readAnnotations(DataInputStream input, Object[] constantPool, Set<String> names) throws IOException {
            int count = input.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                String descriptor = utf8(constantPool, input.readUnsignedShort());
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    names.add(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
                }
                skipElementValuePairs(input);
            }
        }

        private static void skipElementValuePairs(DataInputStream input) throws IOException {
            int pairCount = input.readUnsignedShort();
            for (int i = 0; i < pairCount; i++) {
                input.readUnsignedShort();
                skipElementValue(input);
            }
        }

        private static void skipElementValue(DataInputStream input) throws IOException {
            int tag = input.readUnsignedByte();
            switch (tag) {
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'I':
                case 'J':
                case 'S':
                case 'Z':
                case 's':
                case 'c':
                    input.readUnsignedShort();
                    break;
                case 'e':
                    skipFully(input, 4);
                    break;
                case '@':
                    input.readUnsignedShort();
                    skipElementValuePairs(input);
                    break;
                case '[':
                    int count = input.readUnsignedShort();
                    for (int i = 0; i < count; i++) {
                        skipElementValue(input);
                    }
                    break;
                default:
                    throw new IOException("Unknown annotation element tag " + tag);
            }
        }

        private static String utf8(Object[] constantPool, int index) throws IOException {
            Object value = constantPool[index];
            if (!(value instanceof String)) {
                throw new IOException("Invalid UTF-8 constant-pool index " + index);
            }
            return (String) value;
        }

        private static void skipFully(DataInputStream input, int bytes) throws IOException {
            int remaining = bytes;
            while (remaining > 0) {
                int skipped = input.skipBytes(remaining);
                if (skipped == 0) {
                    input.readByte();
                    skipped = 1;
                }
                remaining -= skipped;
            }
        }
    }
}
