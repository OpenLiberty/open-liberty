/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.generator;

import java.util.ArrayList;

import org.w3c.dom.Element;

import com.ibm.ws.jsp.JspCoreException;

public class FragmentHelperClassWriter extends MethodWriter {

    class FragmentWriter extends MethodWriter {
        private int id;

        public FragmentWriter(int id) {
            super();
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    private boolean used = false;
    private ArrayList fragments = new ArrayList();
    private String className;

    public FragmentHelperClassWriter(String className) {
        this.className = className + "Helper";
    }

    public String getClassName() {
        return this.className;
    }

    public boolean isUsed() {
        return this.used;
    }

    public void generatePreamble(boolean reuseTags) {
        println();
        println("private class " + className + " extends org.apache.jasper.runtime.JspFragmentHelper {");
        println("private javax.servlet.jsp.tagext.JspTag parentTag;");
        println("private int[] _jspx_push_body_count;");
        println("private java.util.HashMap _jspx_TagLookup;");
        println();
        print("public " + className);
        if (reuseTags)
            print("( java.util.HashMap _jspx_TagLookup, int discriminator, JspContext jspContext, ");
        else
            print("( int discriminator, JspContext jspContext, ");
        println("javax.servlet.jsp.tagext.JspTag parentTag, int[] _jspx_push_body_count ) {");
        println("super( discriminator, jspContext, parentTag );");
        println("this.parentTag = parentTag;");
        println("this._jspx_push_body_count = _jspx_push_body_count;");
        if (reuseTags)
            println("this._jspx_TagLookup = _jspx_TagLookup;");
        println("}");
        println();
    }

    public FragmentWriter openFragment(Element element, String tagHandlerVar, int methodNesting, String pageContextVar) throws JspCoreException {
        FragmentWriter fragment = new FragmentWriter(fragments.size());
        fragments.add(fragment);
        this.used = true;

        // XXX - Returns boolean because if a tag is invoked from
        // within this fragment, the Generator sometimes might
        // generate code like "return true".  This is ignored for now,
        // meaning only the fragment is skipped.  The JSR-152
        // expert group is currently discussing what to do in this case.
        // See comment in closeFragment()
        if (methodNesting > 0) {
            fragment.print("public boolean invoke");
        }
        else {
            fragment.print("public void invoke");
        }
        fragment.print(fragment.getId() + "(java.io.Writer out) throws Throwable {");
        fragment.println();
        // Note: Throwable required because methods like _jspx_meth_*
        // throw Throwable.
        //PK65013 - already know if this isTagFile or not and pageContextVar is set accordingly
        GeneratorUtils.generateLocalVariables(fragment, element, pageContextVar);
        fragment.print("javax.servlet.jsp.tagext.JspTag ");
        fragment.print(tagHandlerVar);
        fragment.print(" = parentTag;");
        fragment.println();

        return fragment;
    }

    public void closeFragment(FragmentWriter fragment, int methodNesting) {
        // XXX - See comment in openFragment()
        if (methodNesting > 0) {
            fragment.println("return false;");
        }
        else {
            fragment.println("return;");
        }
        fragment.println("}");
    }

    public void generatePostamble() {
        // Generate all fragment methods:
        for (int i = 0; i < fragments.size(); i++) {
            FragmentWriter fragment = (FragmentWriter) fragments.get(i);
            printMultiLn(fragment.toString());
            println();
        }

        // Generate postamble:
        
        println("public void invoke(java.io.Writer writer) throws javax.servlet.jsp.JspException {");
        println("java.io.Writer out = null;");
        println("if( writer != null ) {");
        println("out = this.jspContext.pushBody(writer);");
        println("} else {");
        println("out = this.jspContext.getOut();");
        println("}");
        println("try {");
        println("this.jspContext.getELContext().putContext(JspContext.class,this.jspContext);");  // defect 393110
        println("switch( this.discriminator ) {");
        for (int i = 0; i < fragments.size(); i++) {
            println("case " + i + ": {");
            println("invoke" + i + "(out);");
            println("break;");
            println("}"); // switch
        }
        println("}"); // switch
        println("}"); // try
        println("catch( Throwable e ) {");
        println("if (e instanceof javax.servlet.jsp.SkipPageException)");
        println("    throw (javax.servlet.jsp.SkipPageException) e;");
        println("throw new javax.servlet.jsp.JspException( e );");
        println("}"); // catch
        println("finally {");
        println("if( writer != null ) {");
        println("this.jspContext.popBody();");
        println("}");

        println("}"); // finally
        println("}"); // invoke method
        println("}"); // helper class
    }
}
