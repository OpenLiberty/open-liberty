/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
 * <p>Array visitor: Specialized to ignore the name parameters, which provide no
 * information for array visit calls.</p>
 */
public class TargetsVisitorAnnotationArrayImpl extends TargetsVisitorAnnotationBaseImpl {
    /**
     * <p>Standard constructor. Construct a non-delegating visitor.</p>
     * 
     * <p>Array detail visitors are always nested.</p>
     * 
     * @param parentDetailText The detail text builder from the parent detail visitor.
     * @param parentEncoder The encoder from the parent detail visitor.
     */
    public TargetsVisitorAnnotationArrayImpl(StringBuilder parentDetailText, StringBuilder parentEncoder) {
        super(parentDetailText, parentEncoder);
    }

    //

    /** <p>Flow parameter: Have any element values been added?</p> */
    protected boolean didAdd = false;

    protected void newElement() {
        if (!didAdd) {
            append(OPENING_BRACKET);
            didAdd = true;
        } else {
            append(COMMA);
        }
    }

    //

    @Override
    public void visit(String name, Object value) {
        newElement();

        appendValue(value.toString());
    }

    @Override
    public TargetsVisitorAnnotationImpl visitAnnotation(String name, String desc) {
        newElement();

        // Append the name, but don't append "=" unless the
        // annotation has a non-defaulted value.

        appendName(name);

        return super.visitAnnotation(name, desc);
    }

    @Override
    public TargetsVisitorAnnotationArrayImpl visitArray(String name) {
        newElement();

        // Don't append the name!  The name parameter to array visits does
        // not have a meaningful value.

        return super.visitArray(name);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        newElement();

        // Don't append the name!  The name parameter to array visits does
        // not have a meaningful value.

        appendValue(value);
    }

    @Override
    public void visitEnd() {
        if (didAdd) {
            append(CLOSING_BRACKET);
            didAdd = false;
        } else {
            append(OPENING_BRACKET);
            append(CLOSING_BRACKET);
        }
    }

    //

    @Override
    protected void reset() {
        super.reset();

        didAdd = false;
    }
}
