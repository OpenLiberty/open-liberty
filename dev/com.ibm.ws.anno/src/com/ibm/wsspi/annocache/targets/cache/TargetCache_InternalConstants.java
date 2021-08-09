/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.targets.cache;

// Save Format:
//
// Common rules:
//
// All strings are written as UTF-8 strings.
//
// Blank lines are ignored.
//
// Any leading or trailing whitespace is ignored.
//
// Any line which begins with "#" is ignored.
//
// All enclosed elements apply to the preceding parent.
//
// Save files use a common header:
//
// CommonHeader:
//
// # ============================================================
// # Header
// Encoding: <encoding>
// Table: <tableName>
// Version: <versionTag>
// TimeStamp: <TBD>
//
// While data for packages and classes are grouped by the writer,
// the reader does not require this grouping. The only necessary
// orderings / grouping is that data for a package must immediately
// follow the package, and data for a class, field, or method, must
// immediately follow the class, field, or method.
//
// This implementation always uses UTF-8 as the encoding.
//
// Targets data uses three primary formats; the targets class table,
// the targets target table, and the targets detail table each is written
// to a different file.
//
//
// TargetsContainerTable:
//
// Table: Container
//
// # ============================================================
// # Containers
//
// Path: <path>
// Policy: <policy>
//
//
// TargetsTimeStampTable:
//
// Table: Container Stamp
//
// Container Path: <path>
// Container Stamp: <timeStamp>
//
//
// Resolved refs:
//
// # ============================================================
// # Resolved Class References:
//
// Unresolved refs:
//
// # ============================================================
// # Unresolved Class References:
//
// Class: <className>
//
//
// TargetsClassTable:
//
// Table: Classes
//
// # ============================================================
// # Packages:
//
// Package: <packageName>
//
// # ============================================================
// # Classes:
//
// Class: <subClassName>
//   Class Source: <classSourceName>
//   Superclass: <superClassName>
//   Interface: <interfaceName>
//
//
// Table: Annotation Targets
//
// # ============================================================
// # Package Annotation Targets:
//
// Package: <packageName>
// PackageAnnotation: <annotationClassName>
//
// # ============================================================
// # Class Annotation Targets:
//
// Class: <className>
//   ClassAnnotation: <annotationClassName>
//   FieldAnnotation: <annotationAnnotation>
//   MethodAnnotation: <annotationClassName>
//
//
// Table: Annotation Details
//
// # ============================================================
// # Package Annotation Details:
//
// Package: <packageName>
//   Annotation: <annotationClassName>
//   Detail: <annotationDetail>
//
// # ============================================================
// # Class Annotation Details:
//
// Class: <className>
//   Annotation: <annotationClassName>
//   Detail: <annotationDetail>
// Field: <fieldName>
//   Annotation: <annotationAnnotation>
//   Detail: <annotationDetail>
// Method: <methodSignature>
//   Annotation: <annotationAnnotation>
//   Detail: <annotationDetail>

public interface TargetCache_InternalConstants {

    // Maximum widths for header values.
	// This is used to ensure the header has a fixed size,
	// which is necessary for header rewrites.

	// The value must be large enough to encompass the entire
	// length of header value lines.  These have an initial
	// tag, plus ": ", plus a value.

	int HEADER_WIDTH = 60;

    // Common encoding:

    String SERIALIZATION_ENCODING = "UTF-8";

    // Common formatting tags:

    String COMMENT_TAG = "#";
    String SPACE_TAG   = " ";
    String COLON_TAG   = ":";

    // Common delimiters:

    String DELIMITER_TAG     = "============================================================";

    // Needed for collapsed container files, which place a stamp,
    // classes data, and annotations targets data in the same file.
    String END_DELIMITER_TAG = "X==========================================================X";

    // A tag to test for for locating end delimiters.
    // See 'TargetCacheImpl_Writer.writeComment' and 'END_DELIMITER_TAG'
    String END_DELIMITER_TEST_TAG = COMMENT_TAG + SPACE_TAG + "X===";

    // Header fields:

    String ENCODING_TAG  = "Encoding";
    String TABLE_TAG     = "Table";
    String VERSION_TAG   = "Version";
    String TIMESTAMP_TAG = "Written";

    // Common constants:

    String NAME_TAG  = "Name";

    // Containers table constants:

    String CONTAINERS_SECTION = "Containers:";

    String CONTAINER_TABLE_TAG     = "Container";
    String CONTAINER_TABLE_VERSION = "1.0";

    String POLICY_TAG    = "Policy";

    // Refs constants:

    String RESOLVED_REFS_TAG = "Resolved References";
    String RESOLVED_REFS_VERSION = "1.0";

    String RESOLVED_REFS_SECTION = "Resolved class references:";

    String UNRESOLVED_REFS_TAG = "Unresolved References";
    String UNRESOLVED_REFS_VERSION = "1.0";

    String UNRESOLVED_REFS_SECTION = "Unresolved class references:";

    // Stamp table constants:

    String STAMP_TABLE_TAG     = "Stamp";
    String STAMP_TABLE_VERSION = "1.0";

    String STAMP_TAG = "Stamp";

    // Common constants:

    String CLASS_SOURCE_TAG = "Class Source";

    String PACKAGE_TAG = "Package";
    String CLASS_TAG   = "Class";
    String FIELD_TAG   = "Field";
    String METHOD_TAG  = "Method";

    // Classes table constants:

    String CLASSES_TABLE_TAG     = "Classes";
    String CLASSES_TABLE_VERSION = "1.0";

    String PACKAGES_SECTION      = "Packages:";

    String CLASSES_SECTION       = "Classes:";
    String SUPERCLASS_TAG        = "Superclass";
    String INTERFACE_TAG         = "Interface";
    String MODIFIERS_TAG         = "Modifiers";

    // Targets table constants:

    String TARGETS_TABLE_TAG       = "Annotation Targets";
    String TARGETS_TABLE_VERSION   = "1.0";

    String PACKAGE_TARGETS_SECTION = "Package Annotation Targets:";
    String PACKAGE_ANNOTATION_TAG  = "Package Annotation";

    String CLASS_TARGETS_SECTION   = "Class Annotation Targets:";
    String CLASS_ANNOTATION_TAG    = "Class Annotation";
    String FIELD_ANNOTATION_TAG    = "Field Annotation";
    String METHOD_ANNOTATION_TAG   = "Method Annotation";

    // Details table constants:

    String DETAILS_TABLE_TAG       = "Annotation Details";
    String DETAILS_TABLE_VERSION   = "1.0";

    String PACKAGE_DETAILS_SECTION = "Package Annotation Details:";
    String CLASS_DETAILS_SECTION   = "Class Annotation Details:";

    String ANNOTATION_TAG          = "Annotation";
    String DETAIL_TAG              = "Detail";

    //

    // Query
    //
    // Table: Annotation Queries
    //
    // # ============================================================
    // Query: <title>
    // Timestamp: <Date-Time>
    // Policies: (SEED, PARTIAL, EXCLUDED, EXTERNAL)
    // Type: (PACKAGE, CLASS, CLASS INHERITED, FIELD, METHOD)
    // Annotation: <annotation class>
    // Result: <result class>

    // # ============================================================
    // Query: <title>
    // Timestamp: <Date-Time>
    // Source: <source name>
    // Type: (PACKAGE, CLASS, CLASS INHERITED, FIELD, METHOD)
    // Annotation: <annotation class>
    // Result: <result class>

    String QUERIES_TABLE_TAG = "Annotation Queries";
    String QUERIES_TABLE_VERSION = "1.0";

    String QUERY_TAG = "Query";
    String QUERY_CLASS_NAME_TAG     = "Query Class";
    String QUERY_METHOD_NAME_TAG    = "Query Method";
    String QUERY_TIMESTAMP_TAG      = "Timestamp";
    String QUERY_POLICIES_TAG       = "Policies";
    String QUERY_SOURCE_TAG         = "Source";
    String QUERY_TYPE_TAG           = "Type";
    String QUERY_SPECIFIC_TAG       = "Specific";
    String QUERY_ANNOTATION_TAG     = "Annotation";
    String QUERY_RESULT_TAG         = "Result";

    String QUERY_TYPE_TAG_PACKAGE   = "Package";
    String QUERY_TYPE_TAG_CLASS     = "Class";
    String QUERY_TYPE_TAG_INHERITED = "Inherited";
    String QUERY_TYPE_TAG_FIELD     = "Field";
    String QUERY_TYPE_TAG_METHOD    = "Method";

    public enum QueryType {
        PACKAGE  (QUERY_TYPE_TAG_PACKAGE),
        CLASS    (QUERY_TYPE_TAG_CLASS),
        INHERITED(QUERY_TYPE_TAG_INHERITED),
        FIELD    (QUERY_TYPE_TAG_FIELD),
        METHOD   (QUERY_TYPE_TAG_METHOD);

        private QueryType(String tag) {
            this.tag = tag;
        }

        private final String tag;

        public String getTag() {
            return tag;
        }
    }
}
