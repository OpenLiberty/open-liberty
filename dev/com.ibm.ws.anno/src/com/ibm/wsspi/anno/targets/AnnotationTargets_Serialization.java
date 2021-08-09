/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.anno.targets;

// Save Format (all names are java package, class, field, and method names):
//
// Package: <packageName>
// PackageAnnotation: <className>
// Class: <className>
// Superclass: <className>
// Interface: <interfaceName>
// ClassAnnotation: <className>
// FieldAnnotation: <fieldAnnotation>
// MethodAnnotation: <methodAnnotation>
//
// All enclosed elements apply to the preceeding parent, e.g.:
//
// Class: aClass.name
//   Superclass: aSuperclass.name
//   ClassAnnotation: anAnnotationClass.name
// ...
//
// Any leading or trailing whitespace is ignored.
//
// Any line which begins with "#" is ignored.
//
// All serialization uses UTF-8.

public interface AnnotationTargets_Serialization {
    String SERIALIZATION_ENCODING = "UTF-8";
    String SERIALIZATION_VERSION = "1.0";

    //

    String COMMENT_TAG = "#";
    String SPACE_TAG = " ";
    String COLON_TAG = ":";

    //

    String ENCODING_TAG = "Encoding";
    String VERSION_TAG = "Version";
    String TIMESTAMP_TAG = "TimeStamp";

    //

    String HEADER_TAG = "============================================================";

    //

    String CLASS_SOURCES_SECTION = "Class Source Data:";
    String CLASS_SOURCE_TAG = "Class-Source";

    //

    String PACKAGES_SECTION = "Package Data:";

    String PACKAGE_TAG = "Package";
    String PACKAGE_CLASSSOURCE_TAG = "PackageSource";
    String PACKAGE_ANNOTATION_TAG = "PackageAnnotation";

    //

    String CLASSES_SECTION = "Class Data:";

    String SEED_CLASS_TAG = "Class-Seed";
    String PARTIAL_CLASS_TAG = "Class-Partial";
    String EXCLUDED_CLASS_TAG = "Class-Excluded";

    String CLASS_TAGS = SEED_CLASS_TAG + ", " + PARTIAL_CLASS_TAG + ", or " + EXCLUDED_CLASS_TAG;

    String CLASS_CLASSSOURCE_TAG = "ClassSource";
    String SUPERCLASS_TAG = "Superclass";
    String INTERFACE_TAG = "Interface";

    String CLASS_ANNOTATION_TAG = "ClassAnnotation";
    String FIELD_ANNOTATION_TAG = "FieldAnnotation";
    String METHOD_ANNOTATION_TAG = "MethodAnnotation";

    //

    String UNRESOLVED_PACKAGES_SECTION = "Unresolved Package Data:";
    String UNRESOLVED_PACKAGE_TAG = "Package-Unresolved";

    String UNRESOLVED_CLASSES_SECTION = "Unresolved Class Data:";
    String UNRESOLVED_CLASS_TAG = "Class-Unresolved";
}
