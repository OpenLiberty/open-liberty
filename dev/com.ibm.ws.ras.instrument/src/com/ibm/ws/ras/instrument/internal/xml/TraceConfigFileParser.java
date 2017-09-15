/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.FieldInfo;
import com.ibm.ws.ras.instrument.internal.model.InstrumentationOptions;
import com.ibm.ws.ras.instrument.internal.model.MethodInfo;
import com.ibm.ws.ras.instrument.internal.model.PackageInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;

public class TraceConfigFileParser extends DefaultHandler {

    private String elementPath = "";
    private File file;

    private InstrumentationOptions instrumentationOptions = null;
    private Map<String, PackageInfo> packages = new HashMap<String, PackageInfo>();
    private Map<String, ClassInfo> classes = new HashMap<String, ClassInfo>();

    private TraceOptionsData currentTraceOptions = null;
    private PackageInfo currentPackage = null;
    private ClassInfo currentClass = null;
    private FieldInfo currentField = null;
    private MethodInfo currentMethod = null;
    private StringBuffer currentData = null;
    private int currentArg = 0;

    public TraceConfigFileParser() {
        super();
    }

    public TraceConfigFileParser(File file) {
        setFile(file);
    }

    public void parse() throws IOException {
        if (file == null) {
            return;
        }

        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(this);
            xr.setErrorHandler(this);

            FileReader fr = new FileReader(file);
            xr.parse(new InputSource(fr));
        } catch (SAXException saxe) {
            IOException ioe = new IOException("Parse failure");
            ioe.initCause(saxe);
            throw ioe;
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        currentData = new StringBuffer();
        elementPath = elementPath + "/" + localName;
        if (elementPath.equals("/trace-instrumentation/instrumentation-options")) {
            instrumentationOptions = new InstrumentationOptions();
        } else if (elementPath.equals("/trace-instrumentation/annotations/package")) {
            String packageName = attributes.getValue("", "name");
            currentPackage = new PackageInfo();
            currentPackage.setPackageName(packageName.trim());
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/trivial")) {
            currentPackage.setTrivial(true);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/trace-options")) {
            currentTraceOptions = new TraceOptionsData();
            currentPackage.setTraceOptionsData(currentTraceOptions);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class")) {
            String className = currentPackage.getPackageName() + "." + attributes.getValue("", "name");
            currentClass = new ClassInfo(className);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/sensitive")) {
            currentClass.setSensitive(true);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/trivial")) {
            currentClass.setTrivial(true);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/trace-options")) {
            currentTraceOptions = new TraceOptionsData();
            currentClass.setTraceOptionsData(currentTraceOptions);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/field")) {
            String fieldName = attributes.getValue("", "name");
            String fieldDescriptor = attributes.getValue("", "descriptor");
            currentField = new FieldInfo();
            currentField.setFieldName(fieldName);
            currentField.setFieldDescriptor(fieldDescriptor);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/field/logger")) {
            currentField.setLoggerField(true);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/field/sensitive")) {
            currentField.setSensitive(true);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/method")) {
            String methodName = attributes.getValue("", "name");
            String methodDescriptor = attributes.getValue("", "descriptor");
            currentMethod = new MethodInfo(methodName, methodDescriptor);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/method/sensitive")) {
            currentMethod.setResultSensitive(true);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/method/trivial")) {
            currentMethod.setTrivial(true);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/method/parameters/arg")) {
            String argNumber = attributes.getValue("", "index");
            currentArg = Integer.parseInt(argNumber);
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/method/parameters/arg/sensitive")) {
            currentMethod.setArgIsSensitive(currentArg, true);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String data = (currentData != null) ? currentData.toString() : null;
        if (elementPath.equals("/trace-instrumentation/annotations/package")) {
            packages.put(currentPackage.getInternalPackageName(), currentPackage);
            currentPackage = null;
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class")) {
            classes.put(currentClass.getInternalClassName(), currentClass);
            currentClass = null;
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/field")) {
            currentClass.getFieldInfoSet().add(currentField);
            currentField = null;
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/method")) {
            currentClass.getMethodInfoSet().add(currentMethod);
            currentMethod = null;
        } else if (elementPath.endsWith("/trace-options")) {
            currentTraceOptions = null;
        } else if (elementPath.equals("/trace-instrumentation/instrumentation-options/packages/include")) {
            instrumentationOptions.addPackagesInclude(data);
        } else if (elementPath.equals("/trace-instrumentation/instrumentation-options/packages/exclude")) {
            instrumentationOptions.addPackagesExclude(data);
        } else if (elementPath.equals("/trace-instrumentation/instrumentation-options/ffdcEnabled")) {
            instrumentationOptions.setAddFFDC(Boolean.valueOf(data).booleanValue());
        } else if (elementPath.equals("/trace-instrumentation/instrumentation-options/trace-type")) {
            instrumentationOptions.setTraceType(data);
        } else if (elementPath.endsWith("/trace-options/traceGroup")) {
            currentTraceOptions.addTraceGroup(data);
        } else if (elementPath.endsWith("/trace-options/traceExceptionThrow")) {
            currentTraceOptions.setTraceExceptionThrow(Boolean.valueOf(data).booleanValue());
        } else if (elementPath.endsWith("/trace-options/traceExceptionHandling")) {
            currentTraceOptions.setTraceExceptionHandling(Boolean.valueOf(data).booleanValue());
        } else if (elementPath.equals("/trace-instrumentation/annotations/package/class/method/ffdcIgnore/exception")) {
            data = data.replaceAll("\\.", "/");
            currentMethod.addFFDCIgnoreException(Type.getObjectType(data));
        }
        elementPath = elementPath.replaceAll("\\/[^\\/]+$", "");
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String data = new String(ch, start, length).trim();
        currentData.append(data);
    }

    @Override
    public void endDocument() throws SAXException {
        for (ClassInfo ci : classes.values()) {
            PackageInfo pi = getPackageInfo(ci.getInternalPackageName());
            if (pi != null) {
                ci.updateDefaultValuesFromPackageInfo(pi);
            }
        }
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Collection<ClassInfo> getClassInfo() {
        return classes.values();
    }

    public ClassInfo getClassInfo(String internalClassName) {
        return classes.get(internalClassName);
    }

    public InstrumentationOptions getInstrumentationOptions() {
        return instrumentationOptions;
    }

    public Collection<PackageInfo> getPackageInfo() {
        return packages.values();
    }

    public PackageInfo getPackageInfo(String internalPackageName) {
        return packages.get(internalPackageName);
    }

    public static void main(String[] args) throws Exception {
        TraceConfigFileParser parser = new TraceConfigFileParser();
        if (args.length != 1) {
            parser.setFile(new File("trace.xml"));
        } else {
            parser.setFile(new File(args[0]));
        }
        parser.parse();

        System.out.println(parser.instrumentationOptions);
        System.out.println(parser.packages);
        System.out.println(parser.classes);
    }
}
