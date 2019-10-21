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
package com.ibm.aries.buildtasks.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.aries.buildtasks.semantic.versioning.model.decls.ClassDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.GenericDeclaration;

public class SemanticVersioningUtils
{

    public static final String classExt = ".class";
    public static final String javaExt = ".java";
    public static final String schemaExt = ".xsd";
    public static final String jarExt = ".jar";

    public static final String SEVERITY_2 = "2";
    public static final String SEVERITY_3 = "3";
    public static final String SEVERITY_4 = "4";
    public static final String CONSTRUTOR = "<init>";
    public static final String MAJOR_CHANGE = "major";
    public static final String MINOR_CHANGE = "minor";
    public static final String NO_CHANGE = "no";
    public static final String NEW_PACKAGE = "new";
    public static final String REVERT_CHANGE = "revert the changes";
    public static final String oneLineBreak = "\n";
    public static final String htmlOneLineBreak = "&#13;&#10;";
    public static final String htmlTwoLineBreaks = htmlOneLineBreak + htmlOneLineBreak;
    public static final String twoLineBreaks = oneLineBreak + oneLineBreak;
    public static final String PROPERTY_FILE_IDENTIFIER = "java/util/ListResourceBundle";
    public static final String CLINIT = "<clinit>";
    public static final String SERIALIZABLE_CLASS_IDENTIFIER = "java/io/Serializable";
    public static final String SERIAL_VERSION_UTD = "serialVersionUID";
    public static final String ENUM_CLASS = "java/lang/Enum";
    public static final String WAS_PACKAGE_INITIAL_VERSION = "1.0.0";

    public static boolean isLessAccessible(GenericDeclaration before, GenericDeclaration after) {

        if (before.getAccess() == after.getAccess()) {
            return false;
        }
        //When it reaches here, the two access are different. Let's make sure the whether the after field has less access than the before field.
        if (before.isPublic()) {
            if (!!!after.isPublic()) {
                return true;
            }
        } else if (before.isProtected()) {
            if (!!!(after.isPublic() || after.isProtected())) {
                return true;
            }
        } else {
            if (!!!before.isPrivate()) {
                // the field is package level.
                if (after.isPrivate()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * ASM Type descriptor look up table
     * 
     * @author emily
     * 
     */
    private enum TypeDescriptor {
        I("int"), Z("boolean"), C("char"), B("byte"),
        S("short"), F("float"), J("long"), D("double"), V("void");

        String desc;

        TypeDescriptor(String desc) {
            this.desc = desc;
        }

        String getDesc() {
            return desc;
        }

        private static final Map<String, TypeDescriptor> stringToEnum = new HashMap<String, TypeDescriptor>();
        static {
            for (TypeDescriptor td : values()) {
                stringToEnum.put(td.toString(), td);
            }
        }

        public static TypeDescriptor fromString(String symbol) {
            return stringToEnum.get(symbol);
        }

    }

    /**
     * Transform ASM method desc to a human readable form
     * Method declaration in source file Method descriptor
     * void m(int i, float f) <= (IF)V
     * int m(Object o) <= (Ljava/lang/Object;)I
     * int[] m(int i, String s) <= (ILjava/lang/String;)[I
     * Object m(int[] i) <= ([I)Ljava/lang/Object;
     * 
     * @param methodName
     * @param methodDesc
     * @return
     */
    public static String getReadableMethodSignature(String methodName, String methodDesc) {
        if (methodDesc == null) {
            //This is an error case I've met during development, it's not supposed to happen, but at least this way 
            //we gather a little more info as to what it was doing if it happens.    		
            System.out.println("NULL DESC FOR " + methodName);
            Exception e = new Exception();
            e.printStackTrace();
            throw new IllegalStateException("ERROR null description for methodName " + methodName);
        }

        // need to find the return type first, which is outside the ()
        int lastBrace = methodDesc.lastIndexOf(")");

        // parameter
        StringBuilder methodSignature = new StringBuilder();
        if (lastBrace == -1) {
            // this is odd, don't attempt to transform. Just return back. Won't happen unless byte code weaving is not behaving.
            return "method " + methodName + methodDesc;
        }
        String param = methodDesc.substring(1, lastBrace);
        if (CONSTRUTOR.equals(methodName)) {
            //This means the method is a constructor. In the binary form, the constructor carries a name 'init'. Let's use the source
            // code proper name
            methodSignature.append("constructor with parameter list ");
        } else {
            String returnType = methodDesc.substring(lastBrace + 1);
            methodSignature.append("method ");
            methodSignature.append(transform(returnType));
            methodSignature.append(" ");
            methodSignature.append(methodName);
        }
        // add the paramether list
        methodSignature.append("(");
        methodSignature.append(transform(param));
        methodSignature.append(")");
        return methodSignature.toString();
    }

    public static String transform(String asmDesc)
    {
        String separator = ", ";
        int brkCount = 0;
        StringBuilder returnStr = new StringBuilder();
        //remove the '['s

        while (asmDesc.length() > 0) {
            while (asmDesc.startsWith("[")) {
                asmDesc = asmDesc.substring(1);
                brkCount++;
            }
            while (asmDesc.startsWith("L")) {
                //remove the L and ;
                int semiColonIndex = asmDesc.indexOf(";");

                if (semiColonIndex == -1) {
                    //This is odd. The asm binary code is invalid. Do not attempt to transform.
                    return asmDesc;
                }
                returnStr.append(asmDesc.substring(1, semiColonIndex));
                asmDesc = asmDesc.substring(semiColonIndex + 1);
                for (int index = 0; index < brkCount; index++) {
                    returnStr.append("[]");
                }
                brkCount = 0;
                returnStr.append(separator);
            }

            TypeDescriptor td = null;
            while ((asmDesc.length() > 0) && (td = TypeDescriptor.fromString(asmDesc.substring(0, 1))) != null) {

                returnStr.append(td.getDesc());
                for (int index = 0; index < brkCount; index++) {
                    returnStr.append("[]");
                }
                brkCount = 0;
                returnStr.append(separator);
                asmDesc = asmDesc.substring(1);
            }

        }
        String finalStr = returnStr.toString();
        if (finalStr.endsWith(separator)) {
            finalStr = finalStr.substring(0, finalStr.lastIndexOf(separator));
        }
        //replace "/" with "." as bytecode uses / in the package names
        finalStr = finalStr.replaceAll("/", ".");
        return finalStr;
    }

    /**
     * Return whether the binary is property file. If the binary implements the interface of java.util.ListResourceBundle
     * 
     * @param cd
     * @return
     */
    public static boolean isPropertyFile(ClassDeclaration cd) {
        Collection<String> supers = cd.getAllSupers();
        return (supers.contains(PROPERTY_FILE_IDENTIFIER));
    }

}
