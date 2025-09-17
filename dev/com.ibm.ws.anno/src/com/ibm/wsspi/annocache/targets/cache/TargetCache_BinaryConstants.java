/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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

    String VERSION_10 = "1.0";
    String VERSION_20 = "2.0";
    
    String[] VERSIONS_10 = { VERSION_10 };
    String[] VERSIONS_10_20 = { VERSION_10, VERSION_20 };
    
    int VERSION_VALUE_10 = 10;
    int VERSION_VALUE_20 = 20;
    
    int[] VERSION_VALUES_10 = { VERSION_VALUE_10 };
    int[] VERSION_VALUES_10_20 = { VERSION_VALUE_10, VERSION_VALUE_20 };
    
    String STAMP_TABLE_NAME = "Stamp";
    String STAMP_TABLE_VERSION = VERSION_10;
    int STAMP_TABLE_VERSION_VALUE = VERSION_VALUE_10;

    String CLASSES_TABLE_NAME = "Classes";
    String CLASSES_TABLE_VERSION = VERSION_10;
    int CLASSES_TABLE_VERSION_VALUE = VERSION_VALUE_10;
    
    String TARGETS_TABLE_NAME = "Annotation Targets";
    String TARGETS_TABLE_VERSION = VERSION_10;
    int TARGETS_TABLE_VERSION_VALUE = VERSION_VALUE_10;
    
    String CONTAINER_TABLE_NAME = "Container";
    String CONTAINER_TABLE_VERSION = VERSION_20; // Issue 30315
    int CONTAINER_TABLE_VERSION_VALUE = VERSION_VALUE_20; // Issue 30315

    String[] CONTAINER_TABLE_VERSIONS = VERSIONS_10_20; // Issue 30315
    int[] CONTAINER_TABLE_VERSION_VALUES = VERSION_VALUES_10_20; // Issue 30315
        
    String RESOLVED_REFS_NAME = "Resolved References";
    String RESOLVED_REFS_VERSION = VERSION_10;
    int RESOLVED_REFS_VERSION_VALUE = VERSION_VALUE_10;
    
    String UNRESOLVED_REFS_NAME = "Unresolved References";
    String UNRESOLVED_REFS_VERSION = VERSION_10;
    int UNRESOLVED_REFS_VERSION_VALUE = VERSION_VALUE_10;
    
    // Containers table: ...
    byte SIGNATURE_BYTE           = 0x22; // "Signature" // Issue 30315; the byte value '22' is correct.
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
