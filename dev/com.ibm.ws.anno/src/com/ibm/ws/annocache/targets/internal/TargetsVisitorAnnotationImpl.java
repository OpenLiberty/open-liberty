/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

/**
 * <p>Annotation detail visitor. Build detail text for an annotation.</p>
 *
 * <p>An empty string is used for an annotation which has no non-default values.</p>
 *
 * <p>An annotation which has at least one non-default value is presented as
 * a list of value assignments enclosed by braces and using comma delimiters.</p>
 *
 * <p>Defaulted values are omitted from the value assignments list.</p>
 *
 * <p>Simple value assignments (non-array, non-annotation values) are placed
 * as name=value.</p>
 *
 * <p>A nested array value is represented by a bracket enclosed list using the
 * java default array string representation.</p>
 *
 * <p>A nested annotation value is represented either by a bare name, or by
 * a name=value assignment where the value re-uses the detail visitor format.</p>
 *
 * <p>A nested annotation with all default values must be distinguished from
 * a nested annotation value which is wholly defaulted. A nested annotation value
 * which is wholly defaulted is omitted from the value assignment list. (The actual
 * value is the value provided by the enclosing annotation definition, which will
 * often be different than an occurrence of the nested annotation which uses default
 * values of the nested annotation.) A nested annotation value which has all
 * default values is represented by a bare name in the value assignment list.</p>
 *
 * <p>Examples:</p>
 *
 * <ul>
 * <li>EMPTY_STRING - The annotation has no assigned values. All values are defaulted.</li>
 * <li>{name=value} - The annotation assigns one value. All other values are defaulted.</li>
 * <li>{name1=value1,name2=value2} - The annotation assigns two values.</li>
 * <li>{name} - The annotation has a nested annotation value which uses all default values.</li>
 * <li>{name1={name2=value2}} - The annotation has a nested annotation which has one assigned value.</li>
 * <li>{name=[value1,value2]} - The annotation has a nested array value.</li>
 * </ul>
 *
 * <p>Annotation which are defaulted are not presented to the annotation visitor. A value
 * assignment with a null value is not possible.</p>
 */

public class TargetsVisitorAnnotationImpl extends TargetsVisitorAnnotationBaseImpl {

    /**
     * <p>Standard constructor. Construct a non-delegating root detail visitor.</p>
     *
     * @param parentVisitor The parent class visitor.
     */
    public TargetsVisitorAnnotationImpl(TargetsVisitorClassImpl parentVisitor) {
        super(parentVisitor);

        this.didAdd = false;
    }

    /**
     * <p>Standard constructor. Construct a non-delegating nested detail visitor.</p>
     *
     * @param parentDetailText The detail text builder from the parent detail visitor.
     * @param parentEncoder The encoder from the parent detail visitor.
     */
    public TargetsVisitorAnnotationImpl(StringBuilder parentDetailText, StringBuilder parentEncoder) {
        super(parentDetailText, parentEncoder);

        this.didAdd = false;
    }

    //

    /** <p>Flow parameter: Have any values been added?</p> */
    protected boolean didAdd;

    /**
     * <p>Common flow control case: A new value was received.
     * If this is not the first value, append a comma delimiter and
     * return. If this is the first value, append a starting brace
     * character. In addition, if the annotation is a nested annotation
     * value, place an assignment character before the starting brace.</p>
     */
    protected void newValue() {
        if (!didAdd) {
            if (isNested()) {
                append(ASSIGNMENT);
            }

            append(OPENING_BRACE);

            didAdd = true;

        } else {
            append(COMMA);
        }
    }

    // Simple cases:

    @Override
    public void visit(String name, Object value) {
        appendValue(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        appendValue(name, value);
    }

    protected void appendValue(String name, Object value) {
        newValue();

        appendName(name);

        append(ASSIGNMENT);

        appendValue(value.toString());
    }

    // Complex case #1: The nested value is an annotation value.

    @Override
    public TargetsVisitorAnnotationImpl visitAnnotation(String name, String desc) {
        newValue();

        appendName(name);

        // The nested visitor handles the assignment character,
        // and handles the value.
        //
        // The presence of the assignment character distinguishes a
        // nested annotation which has assigned values from a nested
        // annotation which is entirely defaulted.
        return super.visitAnnotation(name, desc);
    }

    // Complex case #2: The nested value is an array value.

    @Override
    public TargetsVisitorAnnotationArrayImpl visitArray(String name) {
        newValue();

        appendName(name);

        append(ASSIGNMENT);

        // The nested visitor handles the array value.
        return super.visitArray(name);
    }

    //

    @Override
    public void visitEnd() {
        if (didAdd) {
            append(CLOSING_BRACE);
            didAdd = false;
        } else {
            // Do nothing! Leave as an empty string.
        }

        // The annotation class name was set (in the parent visitor)
        // before this annotation visitor was returned for use.  That
        // annotation class name was used immediately to record the
        // annotation to the targets table, and was left for use to
        // record the annotation detail, after the entire annotation
        // had been read.  Recording of the annotation detail is done
        // here, in 'visitEnd'.  The detail text is retrieved (and cleared
        // as a side effect), then provided to the parent visitor to
        // be recorded.

        if (!isNested()) {
            getParentVisitor().recordAnnotation(retrieveDetailText());
        }
    }

    //

    // For safety, do a reset before visit calls,
    // in case the last call failed with an exception.

    @Override
    protected void reset() {
        super.reset();

        this.didAdd = false;
    }
}
