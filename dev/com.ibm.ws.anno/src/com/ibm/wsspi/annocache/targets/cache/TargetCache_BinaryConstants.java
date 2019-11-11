/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.targets.cache;

public interface TargetCache_BinaryConstants {

	// File delimiters ...
    byte[] MAGIC_BEGIN_BYTES = { (byte) 0xC0, (byte) 0xCA, (byte) 0x01, (byte) 0x01 };
    byte[] MAGIC_END_BYTES   = { (byte) 0xC0, (byte) 0xCA, (byte) 0xA0, (byte) 0xA0 };

    // Region marks ...
    byte HEADER_BYTE     = 0x01;
    byte DATA_BYTE       = 0x02;
    byte STRINGS_BYTE    = 0x03;
    byte TRAILER_BYTE    = 0x04;

    // Header table ...

	int HEADER_WIDTH = 60;

    byte ENCODING_BYTE  = 0x11; // "Encoding"
    byte TABLE_BYTE     = 0x12; // "Table"
    byte VERSION_BYTE   = 0x13; // "Version"
    byte TIMESTAMP_BYTE = 0x14; // "Written"

    // Stamp table ...
    byte NAME_BYTE      = 0x15; // "Name"
    byte STAMP_BYTE     = 0x16; // "Stamp"

    // Encoding ...
    String UTF8_ENCODING = "UTF-8";

    // Table and version ...

    String STAMP_TABLE_NAME = "Stamp";
    String STAMP_TABLE_VERSION = "1.0";

    String CLASSES_TABLE_NAME = "Classes";
    String CLASSES_TABLE_VERSION = "1.0";

    String TARGETS_TABLE_NAME = "Annotation Targets";
    String TARGETS_TABLE_VERSION = "1.0";

    String CONTAINER_TABLE_NAME = "Container";
    String CONTAINER_TABLE_VERSION = "1.0";

    String RESOLVED_REFS_NAME = "Resolved References";
    String RESOLVED_REFS_VERSION = "1.0";

    String UNRESOLVED_REFS_NAME = "Unresolved References";
    String UNRESOLVED_REFS_VERSION = "1.0";

    // Containers table: ...
    byte POLICY_BYTE              = 0x21; // "Policy"

    // Module classes table ...
    byte PACKAGE_BYTE             = 0x31; // "Package"
    byte CLASS_BYTE               = 0x32; // "Class"
    byte CLASS_SOURCE_BYTE        = 0x33; // "Class Source"

    // Component classes table ...
    byte SUPERCLASS_BYTE          = 0x41; // "Superclass"
    byte INTERFACE_BYTE           = 0x42; // "Interface"
    byte MODIFIERS_BYTE           = 0x42; // "Modifiers"

    // Component targets table ...
    byte PACKAGE_ANNOTATION_BYTE  = 0x51; // "Package Annotation"
    byte CLASS_ANNOTATION_BYTE    = 0x52; // "Class Annotation"
    byte FIELD_ANNOTATION_BYTE    = 0x53; // "Field Annotation"
    byte METHOD_ANNOTATION_BYTE   = 0x54; // "Method Annotation"
}
