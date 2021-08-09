/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.base.internal;

import java.util.Iterator;
import java.util.Map;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

/**
 * Generates generic get, set APIs to replace dynamic SDO APIs
 * Generates metadata APIs for retrieving the bean info, superTypes, subTypes and its field datatypes.
 *
 * Update wimdatagraph.xsd with globalbindings to generate isSetXX/unSetXX methods
 * Place the vmmjaxbplugin.jar in the classpath
 * Generate bean from xsd using below command from WAS_HOME\bin OR from the JDK_HOME/bin
 * xjc -classpath <jar_loc>\vmmjaxbplugin.jar -p com.ibm.websphere.wim.model wimdatagraph.xsd -extension -Xvmmcodegen -no-header
 *
 * The model classes will be generated in the current working directory by default.
 * Use -d option to specify the directory where you want to generate the model classes
 *
 *
 */
public class VMMAXBCodeGenPlugin extends Plugin {

    private static final String PROPNAME = "propName";
    private static final String GET_API = "get";
    private static final String SET_API = "set";
    private static final String IS_SET_API = "isSet";
    private static final String UN_SET_API = "unset";
    private static final String GET_SET_PARAM = PROPNAME;
    private static final String SET_PARAM = "value";
    private static final String SUPER_TYPE_NAME = "superTypeName";
    private static final String TYPE_NAME = "entityTypeName";
    JType voidType = null;
    JType objType = null;
    JType strType = null;
    JType mapType = null;
    JType listType = null;
    JType setType = null;
    JType boolType = null;
    JType collectionType = null;
    JType ListType = null;
    Outline outline = null;

    @Override
    public String getOptionName() {
        return "Xvmmcodegen";
    }

    @Override
    public String getUsage() {
        return "  -Xvmmcodegen        :  generate metadata code and getter setter api in generated beans";
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        this.outline = outline;
        voidType = outline.getCodeModel().VOID;
        boolType = outline.getCodeModel().BOOLEAN;
        try {
            objType = outline.getCodeModel().parseType("java.lang.Object");
            strType = outline.getCodeModel().parseType("java.lang.String");
            mapType = outline.getCodeModel().parseType("java.util.HashMap");
            listType = outline.getCodeModel().parseType("java.util.ArrayList");
            setType = outline.getCodeModel().parseType("java.util.HashSet");
            collectionType = outline.getCodeModel().parseType("java.util.Collections");
            ListType = outline.getCodeModel().parseType("java.util.List");
            generateGetter();
            generateIsSet();
            generateSetter();
            generateUnSetter();
            generateGetTypeName();
            //generateDataTypeMapping();
            generateCustomSetters();
            generateCustomIsSetters();
            generateGetTransientProperties();
            generateSetPropertyNames();
            generateGetPropertyNames();
            generateSetDataTypeMap();
            generateGetDataType();
            //generateSuperTypes();
            generateSetSuperTypes();
            generateGetSuperTypes();
            generateIsSubType();
            //generateSubTypes();
            generateSetSubTypes();
            generateEntitySubTypes();
            generateGetSubTypes();
            generatetoString();
            generateIsUnset();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JClassAlreadyExistsException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return true;
    }

    public void generateGetter() {

        JType returnType = null;
        String methodName = GET_API;
        // Process every pojo class generated by jaxb
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            Map fields = implClass.fields();
            /*
             * if (fields == null || fields.size() < 1)
             * continue;
             *///moving this down since we need to have empty dynamic getter setters to support super.get behaviour
            int mods = JMod.PUBLIC;

            JMethod dynamicMethod = implClass.method(mods, implClass, methodName);

            JVar jvarParam = dynamicMethod.param(String.class, GET_SET_PARAM);
            returnType = objType;

            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            /*
             * if (fields == null || fields.size() < 1) {
             * body._return(JExpr._null());
             * //continue;
             * }
             */
            Iterator itr = fields.keySet().iterator();
            JMethod jMethod = null;
            while (itr.hasNext()) {
                String fieldNameOrg = (String) itr.next();
                JFieldVar field = (JFieldVar) fields.get(fieldNameOrg);
                String fieldName = convertCase(fieldNameOrg);
                JType[] ars = {};
                jMethod = implClass.getMethod(methodName + fieldName, ars);
                if (jMethod != null) {
                    JConditional cond = body._if(jvarParam.invoke("equals").arg(fieldNameOrg));
                    cond._then()._return(JExpr.invoke(jMethod));
                }
            }
            if (!implClass._extends().equals(objType))
                body._return(JExpr._super().invoke(methodName).arg(jvarParam));
            else
                body._return(JExpr._null());

        }

    }

    public void generateSetter() {
        generateSetters(SET_API);
    }

    public void generateUnSetter() {
        generateSetters(UN_SET_API);
    }

    public void generateIsSet() {
        generateSetters(IS_SET_API);
    }

    public void generateSetters(String methodName) {

        JType returnType = null;
        JVar jvarParam2 = null;

        //String methodName = SET_API;
        // Process every pojo class generated by jaxb
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;

            Map fields = implClass.fields();
            /*
             * if (fields == null || fields.size() < 1)
             * continue;
             */// see comment similar for generateGetters
            int mods = JMod.PUBLIC;

            JMethod dynamicMethod = implClass.method(mods, implClass, methodName);

            JVar jvarParam = dynamicMethod.param(String.class, GET_SET_PARAM);
            //value param should be set only when its a set and not unset
            if (methodName.equals(SET_API)) {
                jvarParam2 = dynamicMethod.param(Object.class, SET_PARAM);
            }
            returnType = voidType;
            //for isSet methods return type is boolean
            if (methodName.equals(IS_SET_API))
                returnType = boolType;
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            /*
             * if (fields == null || fields.size() < 1) {
             * if (methodName.equals(IS_SET_API)) {
             * body._return(JExpr.FALSE);
             * }
             * //continue;
             * }
             */
            JClass type = null;
            Iterator itr = fields.keySet().iterator();
            JMethod jMethod = null;
            while (itr.hasNext()) {
                String fieldNameOrg = (String) itr.next();
                JFieldVar field = (JFieldVar) fields.get(fieldNameOrg);
                String fieldName = convertCase(fieldNameOrg);
                //for unset API no param
                JType[] ars = {};
                //for set API one param
                if (methodName.equals(SET_API)) {
                    ars = new JType[1];
                    ars[0] = field.type().unboxify();
                }

                jMethod = implClass.getMethod(methodName + fieldName, ars);
                JConditional cond = null;
                //set API
                if (methodName.equals(SET_API)) {
                    cond = body._if(jvarParam.invoke("equals").arg(fieldNameOrg));
                    if (jMethod != null) {
                        cond._then().add(JExpr.invoke(jMethod).arg(JExpr.cast(field.type().boxify(), jvarParam2)));
                    } else {
                        //for set if no match found which is the case for MV props, invoke, getXXX.add() example getDisplayName().add(newItem);
                        JType[] temp = {};
                        jMethod = implClass.getMethod(GET_API + fieldName, temp);
                        if (jMethod != null) {
                            String typeToCast = field.type().binaryName();
                            if (typeToCast.indexOf("<") != -1) {
                                typeToCast = typeToCast.substring(typeToCast.indexOf("<") + 1, typeToCast.indexOf(">"));
                                if (typeToCast.indexOf("$") != -1) {
                                    typeToCast = typeToCast.replace("$", ".");
                                }
                                type = outline.getCodeModel().ref(typeToCast);

                                cond._then().add(JExpr.invoke(jMethod).invoke("add").arg(JExpr.cast(type, jvarParam2)));
                            }
                        }
                    }
                }
                //unset API
                if (methodName.equals(UN_SET_API)) {
                    if (jMethod != null) {
                        //only add the if condition for isSet/unset when method is found else empty if can be seen
                        cond = body._if(jvarParam.invoke("equals").arg(fieldNameOrg));
                        cond._then().add(JExpr.invoke(jMethod));
                    }
                } //isSet API
                else if (methodName.equals(IS_SET_API)) {
                    if (jMethod != null) {
                        //only add the if condition for isSet/unset when method is found else empty if can be seen
                        cond = body._if(jvarParam.invoke("equals").arg(fieldNameOrg));
                        cond._then()._return(JExpr.invoke(jMethod));

                    }

                }

            }
            if (methodName.equals(SET_API)) {
                if (!implClass._extends().equals(objType))

                    body.add(JExpr._super().invoke(methodName).arg(jvarParam).arg(jvarParam2));

            } else if (methodName.equals(UN_SET_API)) {
                if (!implClass._extends().equals(objType))

                    body.add(JExpr._super().invoke(methodName).arg(jvarParam));

                if (implClass.name().equalsIgnoreCase("Entity")) {
                    JFieldVar fieldVar = implClass.field(JMod.PRIVATE, ListType, "deletedProperties", JExpr._null());
                    JConditional cond = null;
                    cond = body._if(fieldVar.eq(JExpr._null()));
                    cond._then().assign(fieldVar, JExpr._new(listType));
                    body.add(fieldVar.invoke("add").arg(jvarParam));
                }
            } else if (methodName.equals(IS_SET_API)) {
                if (!implClass._extends().equals(objType))

                    body._return(JExpr._super().invoke(methodName).arg(jvarParam));
                else
                    body._return(JExpr.FALSE);
            }
        }
    }

    public void generateGetTypeName() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.PUBLIC;
            JType returnType = strType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "getTypeName");
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            body._return(JExpr.lit(implClass.name()));
        }
    }

    public void generateSuperTypes() {
        int mods = JMod.STATIC;

        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            JFieldVar fieldVar = implClass.field(mods, listType, "superTypeList", JExpr._new(listType));

            JBlock staticBlock = implClass.init();
            JClass superclass = implClass._extends();
            while (superclass instanceof JDefinedClass) {
                staticBlock.add(fieldVar.invoke("add").arg(superclass.name()));
                superclass = superclass._extends();

            }

        }

    }

    public void generateSetSuperTypes() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.STATIC | JMod.PRIVATE | JMod.SYNCHRONIZED;
            JType returnType = voidType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "setSuperTypes");
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            JBlock staticBlock = implClass.init();
            staticBlock.invoke(dynamicMethod);
            JFieldVar fieldVar = implClass.field(JMod.PRIVATE | JMod.STATIC, listType, "superTypeList", JExpr._null());
            JConditional cond = null;
            cond = body._if(fieldVar.eq(JExpr._null()));
            cond._then().assign(fieldVar, JExpr._new(listType));
            JClass superclass = implClass._extends();
            while (superclass instanceof JDefinedClass) {
                body.add(fieldVar.invoke("add").arg(superclass.name()));
                superclass = superclass._extends();
            }
        }
    }

    public void generateSetDataTypeMap() {

        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.STATIC | JMod.PRIVATE | JMod.SYNCHRONIZED;
            JType returnType = voidType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "setDataTypeMap");
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            JBlock staticBlock = implClass.init();
            staticBlock.invoke(dynamicMethod);
            JFieldVar fieldVar = implClass.field(JMod.PRIVATE | JMod.STATIC, mapType, "dataTypeMap", JExpr._null());
            JConditional cond = null;
            cond = body._if(fieldVar.eq(JExpr._null()));
            cond._then().assign(fieldVar, JExpr._new(mapType));
            //implClass.fields().remove("dataTypeMap");
            Iterator fieldItr = implClass.fields().keySet().iterator();
            String fieldName = null;
            JFieldVar field = null;
            String typeName = null;
            while (fieldItr.hasNext()) {
                fieldName = (String) fieldItr.next();
                field = implClass.fields().get(fieldName);
                typeName = field.type().name();
                if (typeName.indexOf("<") != -1)
                    typeName = typeName.substring(typeName.indexOf("<") + 1, typeName.indexOf(">"));
                if (!fieldName.equals("dataTypeMap") && !fieldName.equals("propertyNames") &&
                    !fieldName.equals("mandatoryProperties") && !fieldName.equals("transientProperties")
                    && !fieldName.equals("deletedProperties") && !fieldName.equals("properties"))
                    body.add(fieldVar.invoke("put").arg(fieldName).arg(JExpr.lit(typeName)));
                if (fieldName.equals("properties") && implClass.name().equalsIgnoreCase("PropertyControl"))
                    body.add(fieldVar.invoke("put").arg(fieldName).arg(JExpr.lit(typeName)));
            }

        }

    }

    public void generateSubTypes() {
        int mods = JMod.STATIC;

        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            JFieldVar fieldVar = implClass.field(mods, setType, "subTypeList", JExpr._new(setType));
            JBlock staticBlock = implClass.init();
            for (ClassOutline cClass : outline.getClasses()) {
                JDefinedClass cImplClass = cClass.implClass;

                //if Entity=currentclass
                //add Roleplayer to list
                //if supertype assignable from subtype
                if (!implClass.equals(cImplClass) && implClass.isAssignableFrom(cImplClass)) {
                    staticBlock.add(fieldVar.invoke("add").arg(cImplClass.name()));
                }
            }

        }
    }

    public void generateSetSubTypes() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.STATIC | JMod.PRIVATE | JMod.SYNCHRONIZED;
            JType returnType = voidType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "setSubTypes");
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            JBlock staticBlock = implClass.init();
            staticBlock.invoke(dynamicMethod);
            JFieldVar fieldVar = implClass.field(JMod.PRIVATE | JMod.STATIC, setType, "subTypeList", JExpr._null());
            JConditional cond = null;
            cond = body._if(fieldVar.eq(JExpr._null()));
            cond._then().assign(fieldVar, JExpr._new(setType));

            for (ClassOutline cClass : outline.getClasses()) {
                JDefinedClass cImplClass = cClass.implClass;
                //if Entity=currentclass
                //add Roleplayer to list
                //if supertype assignable from subtype
                if (!implClass.equals(cImplClass) && implClass.isAssignableFrom(cImplClass))
                    body.add(fieldVar.invoke("add").arg(cImplClass.name()));
            }
        }
    }

    public void generateEntitySubTypes() {
        int mods = JMod.STATIC | JMod.PUBLIC;

        for (ClassOutline classOutline : outline.getClasses()) {

            JDefinedClass implClass = classOutline.implClass;
            if (!implClass.name().equals("Entity"))
                continue;
            JFieldVar fieldVar = implClass.field(JMod.STATIC, mapType, "subTypeMap", JExpr._null());
            JMethod dynamicMethodForSubTypeMap = implClass.method(JMod.STATIC | JMod.PRIVATE | JMod.SYNCHRONIZED, implClass, "setSubTypeMap");
            dynamicMethodForSubTypeMap.type(voidType);
            JBlock bodyOfSubTypeMapMethod = dynamicMethodForSubTypeMap.body();
            JBlock staticBlock = implClass.init();
            staticBlock.invoke(dynamicMethodForSubTypeMap);
            JConditional cond = null;
            cond = bodyOfSubTypeMapMethod._if(fieldVar.eq(JExpr._null()));
            cond._then().assign(fieldVar, JExpr._new(mapType));
            for (ClassOutline cClass : outline.getClasses()) {
                JDefinedClass cImplClass = cClass.implClass;
                if (cImplClass.outer() == null && implClass.isAssignableFrom(cImplClass))
                    bodyOfSubTypeMapMethod.add(fieldVar.invoke("put").arg(cImplClass.name()).arg(
                                                                                                 cImplClass.staticInvoke("getSubTypes")));
            }

            JMethod dynamicMethodForSubEntityTypes = implClass.method(mods, implClass, "getSubEntityTypes");
            JVar jvarParam = dynamicMethodForSubEntityTypes.param(String.class, TYPE_NAME);
            dynamicMethodForSubEntityTypes.type(setType);
            JBlock bodyOfEntityTypesMethod = dynamicMethodForSubEntityTypes.body();
            JFieldVar jVar = null;
            if (implClass.name().equalsIgnoreCase("Entity")) {
                JVar var = bodyOfEntityTypesMethod.decl(setType, "hs", JExpr.cast(setType, implClass.fields().get("subTypeMap").invoke("get").arg(jvarParam)));
                JConditional jcond = bodyOfEntityTypesMethod._if(var.eq(JExpr._null()));
                JConditional jcond1 = jcond._then()._if(JExpr.lit("LoginAccount").invoke("equals").arg(jvarParam));
                JCodeModel model = new JCodeModel();
                JClass cls = model.directClass("LoginAccount");
                jVar = implClass.fields().get("subTypeMap");
                jcond1._then().add(jVar.invoke("put").arg("LoginAccount").arg(cls.staticInvoke("getSubTypes")));
            }
            bodyOfEntityTypesMethod._return(JExpr.cast(setType, jVar.invoke("get").arg(jvarParam)));
        }

    }

    public void generateGetSubTypes() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.PUBLIC | JMod.STATIC;
            JType returnType = setType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "getSubTypes");
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            JFieldVar fieldVar = implClass.fields().get("subTypeList");
            JConditional cond = null;
            JType[] args = {};
            cond = body._if(fieldVar.eq(JExpr._null()));
            cond._then().invoke(implClass.getMethod("setSubTypes", args));
            body._return(fieldVar);
        }

    }

    public void generateGetSuperTypes() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.PUBLIC;
            JType returnType = listType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "getSuperTypes");
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            JFieldVar fieldVar = implClass.fields().get("superTypeList");
            JConditional cond = null;
            JType[] args = {};
            cond = body._if(fieldVar.eq(JExpr._null()));
            cond._then().invoke(implClass.getMethod("setSuperTypes", args));
            body._return(fieldVar);
        }

    }

    public void generateIsSubType() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;

            int mods = JMod.PUBLIC;
            JType returnType = JType.parse(outline.getCodeModel(), "boolean");

            JMethod dynamicMethod = implClass.method(mods, implClass, "isSubType");

            dynamicMethod.type(returnType);

            JBlock body = dynamicMethod.body();

            JVar jvarParam = dynamicMethod.param(String.class, SUPER_TYPE_NAME);

            body._return(implClass.fields().get("superTypeList").invoke("contains").arg(jvarParam));
        }

    }

    public void generateGetDataType() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;

            int mods = JMod.PUBLIC;
            JType returnType = strType;
            //JType returnType = objType;

            JMethod dynamicMethod = implClass.method(mods, implClass, "getDataType");

            dynamicMethod.type(returnType);

            JBlock body = dynamicMethod.body();
            JVar jvarParam = dynamicMethod.param(String.class, GET_SET_PARAM);

            //TODO use generics instead of casting
            JFieldVar fieldVar = implClass.fields().get("dataTypeMap");
            JConditional cond = null;
            JType[] args = {};
            cond = body._if(fieldVar.invoke("containsKey").arg(jvarParam));
            cond._then()._return(JExpr.cast(strType, implClass.fields().get("dataTypeMap").invoke("get").arg(jvarParam)));
            if (!implClass._extends().equals(objType))
                cond._else()._return(JExpr._super().invoke(dynamicMethod).arg(jvarParam));
            else
                cond._else()._return(JExpr._null());
        }

    }

    public void generateDataTypeMapping() {
        int mods = JMod.STATIC;

        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            JFieldVar fieldVar = implClass.field(mods, mapType, "dataTypeMap", JExpr._new(mapType));
            JBlock staticBlock = implClass.init();
            //implClass.fields().remove("dataTypeMap");
            Iterator fieldItr = implClass.fields().keySet().iterator();
            String fieldName = null;
            JFieldVar field = null;
            String typeName = null;
            while (fieldItr.hasNext()) {
                fieldName = (String) fieldItr.next();
                field = implClass.fields().get(fieldName);
                typeName = field.type().name();
                if (typeName.indexOf("<") != -1)
                    typeName = typeName.substring(typeName.indexOf("<") + 1, typeName.indexOf(">"));

                staticBlock.add(fieldVar.invoke("put").arg(fieldName).arg(JExpr.lit(typeName)));
            }

        }

    }

    public void generateGetPropertyNames() throws JClassAlreadyExistsException {

        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.PUBLIC | JMod.STATIC | JMod.SYNCHRONIZED;
            JType returnType = ListType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "getPropertyNames");
            dynamicMethod.type(returnType);
            JBlock body = dynamicMethod.body();
            JVar jvarParam = dynamicMethod.param(String.class, TYPE_NAME);
            //JFieldVar fieldVar = implClass.fields().get("propertyNames");
            JFieldVar fieldVar = implClass.field(JMod.STATIC | JMod.PRIVATE, ListType, "propertyNames", JExpr._null());
            JConditional cond = null;
            JType[] args = {};
            if (implClass.name().equalsIgnoreCase("Entity")) {
                cond = body._if(jvarParam.eq(JExpr._null()));
                cond._then()._return(JExpr._null());

                JVar var = implClass.fields().get("properties");
                cond = body._if((jvarParam.invoke("equals").arg(JExpr.lit("Entity"))).not());
                cond._then()._return(JExpr.cast(ListType, var.invoke("get").arg(jvarParam)));
            }
            cond = body._if(fieldVar.ne(JExpr._null()));
            cond._then()._return(fieldVar);
            JBlock jblock = cond._else().block();
            JVar jbfieldVar = jblock.decl(0, ListType, "names", JExpr._new(listType));
            Iterator fieldItr = implClass.fields().keySet().iterator();
            String fieldName = null;
            JFieldVar field = null;
            String typeName = null;
            while (fieldItr.hasNext()) {
                fieldName = (String) fieldItr.next();
                field = implClass.fields().get(fieldName);
                if (!fieldName.equals("propertyNames") && !fieldName.equals("mandatoryProperties")
                    && !fieldName.equals("transientProperties") && !fieldName.equals("deletedProperties")
                    && !fieldName.equals("properties"))
                    jblock.add(jbfieldVar.invoke("add").arg(fieldName));
                if (fieldName.equals("properties") && implClass.name().equalsIgnoreCase("PropertyControl"))
                    jblock.add(jbfieldVar.invoke("add").arg(fieldName));
            }
            if (!implClass._extends().equals(objType))
                jblock.add(jbfieldVar.invoke("addAll").arg(implClass._extends().staticInvoke("getPropertyNames").arg(implClass._extends().name())));
            JCodeModel codeModel = new JCodeModel();
            JDefinedClass cc = codeModel._class("java.util.Collections");
            fieldVar.assign(cc.staticInvoke("unmodifiableList").arg(jbfieldVar));
            jblock.assign(fieldVar, cc.staticInvoke("unmodifiableList").arg(jbfieldVar));
            jblock._return(fieldVar);
        }
    }

    private static String convertCase(String inValue) {
        char[] chars = inValue.toCharArray();
        char c = Character.toUpperCase(chars[0]);
        chars[0] = c;
        String outValue = new String(chars);
        return outValue;
    }

    public void generatetoString() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            int mods = JMod.PUBLIC;
            JType returnType = strType;
            JMethod dynamicMethod = implClass.method(mods, implClass, "toString");
            dynamicMethod.type(returnType);
            dynamicMethod.annotate(Override.class);
            JBlock body = dynamicMethod.body();
            JCodeModel model = new JCodeModel();
            JClass traceHelper = model.directClass("com.ibm.websphere.security.wim.ras.WIMTraceHelper");
            body._return(traceHelper.staticInvoke("trace").arg(JExpr._this()));
        }
    }

    public void generateCustomSetters() {
        generateCustomSetter("mandatoryProperties");
        generateCustomSetter("transientProperties");
    }

    public void generateCustomIsSetters() {
        generateCustomIsSetter("mandatoryProperties");
        generateCustomIsSetter("transientProperties");
    }

    public void generateCustomSetter(String propName) {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            if (implClass.name().equalsIgnoreCase("Entity") || implClass.name().equalsIgnoreCase("Group")
                || implClass.name().equalsIgnoreCase("PersonAccount")
                || implClass.name().equalsIgnoreCase("Party")
                || implClass.name().equalsIgnoreCase("RolePlayer")
                || implClass.name().equalsIgnoreCase("LoginAccount")) {
                int mods = JMod.STATIC | JMod.PRIVATE | JMod.SYNCHRONIZED;
                JType returnType = voidType;
                JMethod dynamicMethod = null;
                if (propName.equalsIgnoreCase("mandatoryProperties"))
                    dynamicMethod = implClass.method(mods, implClass, "setMandatoryPropertyNames");
                else if (propName.equalsIgnoreCase("transientProperties"))
                    dynamicMethod = implClass.method(mods, implClass, "setTransientPropertyNames");
                dynamicMethod.type(returnType);
                JBlock body = dynamicMethod.body();
                JBlock staticBlock = implClass.init();
                staticBlock.invoke(dynamicMethod);
                JFieldVar fieldVar = null;
                if (propName.equalsIgnoreCase("mandatoryProperties"))
                    fieldVar = implClass.field(JMod.STATIC | JMod.PRIVATE, ListType, "mandatoryProperties", JExpr._null());
                else if (propName.equalsIgnoreCase("transientProperties"))
                    fieldVar = implClass.field(JMod.STATIC | JMod.PRIVATE, ListType, "transientProperties", JExpr._null());
                JConditional cond = null;
                cond = body._if(fieldVar.ne(JExpr._null()));
                cond._then()._return();
                body.assign(fieldVar, JExpr._new(listType));
                if (fieldVar.name().equalsIgnoreCase("mandatoryProperties")) {
                    if (implClass.name().equalsIgnoreCase("PersonAccount"))
                        body.add(fieldVar.invoke("add").arg("sn"));
                    if (implClass.name().equalsIgnoreCase("PersonAccount") || implClass.name().equalsIgnoreCase("Group"))
                        body.add(fieldVar.invoke("add").arg("cn"));
                    if (implClass.name().equalsIgnoreCase("Entity")) {
                        body.add(fieldVar.invoke("add").arg("identifier"));
                        body.add(fieldVar.invoke("add").arg("createTimestamp"));
                    }

                }
                if (fieldVar.name().equalsIgnoreCase("transientProperties")) {
                    if (implClass.name().equalsIgnoreCase("Group"))
                        body.add(fieldVar.invoke("add").arg("members"));
                    if (implClass.name().equalsIgnoreCase("Entity")) {
                        body.add(fieldVar.invoke("add").arg("identifier"));
                        body.add(fieldVar.invoke("add").arg("viewIdentifier"));
                        body.add(fieldVar.invoke("add").arg("parent"));
                        body.add(fieldVar.invoke("add").arg("children"));
                        body.add(fieldVar.invoke("add").arg("groups"));
                        body.add(fieldVar.invoke("add").arg("entitlementInfo"));
                        body.add(fieldVar.invoke("add").arg("changeType"));
                    }
                    if (implClass.name().equalsIgnoreCase("LoginAccount"))
                        body.add(fieldVar.invoke("add").arg("certificate"));
                    if (!implClass._extends().equals(objType)) {
                        body.add(fieldVar.invoke("addAll").arg(implClass._extends().staticInvoke("getTransientProperties")));
                    }
                }
            }
        }
    }

    public void generateCustomIsSetter(String propName) {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            if (implClass.name().equalsIgnoreCase("Entity") || implClass.name().equalsIgnoreCase("Group")
                || implClass.name().equalsIgnoreCase("PersonAccount")
                || implClass.name().equalsIgnoreCase("Party")
                || implClass.name().equalsIgnoreCase("RolePlayer")
                || implClass.name().equalsIgnoreCase("LoginAccount")) {
                int mods = JMod.PUBLIC;
                JType returnType = JType.parse(outline.getCodeModel(), "boolean");
                JMethod dynamicMethod = null;
                JFieldVar fieldVar = null;
                if (propName.equalsIgnoreCase("mandatoryProperties")) {
                    dynamicMethod = implClass.method(mods, implClass, "isMandatory");
                    fieldVar = implClass.fields().get("mandatoryProperties");
                } else if (propName.equalsIgnoreCase("transientProperties")) {
                    dynamicMethod = implClass.method(mods, implClass, "isPersistentProperty");
                    fieldVar = implClass.fields().get("transientProperties");
                }

                dynamicMethod.type(returnType);
                JBlock body = dynamicMethod.body();
                JVar jvarParam = dynamicMethod.param(String.class, PROPNAME);
                JConditional cond = null;
                JConditional cond1 = null;
                cond = body._if(fieldVar.eq(JExpr._null()));
                if (propName.equalsIgnoreCase("mandatoryProperties")) {
                    cond._then().invoke("setMandatoryPropertyNames");
                    cond1 = body._if(fieldVar.invoke("contains").arg(jvarParam));
                    cond1._then()._return(JExpr.TRUE);
                    cond1._else()._return(JExpr.FALSE);
                } else if (propName.equalsIgnoreCase("transientProperties")) {
                    cond._then().invoke("setTransientPropertyNames");
                    cond1 = body._if(fieldVar.invoke("contains").arg(jvarParam));
                    cond1._then()._return(JExpr.FALSE);
                    cond1._else()._return(JExpr.TRUE);
                }
            }
        }
    }

    public void generateGetTransientProperties() {
        for (ClassOutline classOutline : outline.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            if (implClass.name().equalsIgnoreCase("Entity") || implClass.name().equalsIgnoreCase("Group")
                || implClass.name().equalsIgnoreCase("PersonAccount")
                || implClass.name().equalsIgnoreCase("Party")
                || implClass.name().equalsIgnoreCase("RolePlayer")
                || implClass.name().equalsIgnoreCase("LoginAccount")) {
                int mods = JMod.STATIC | JMod.PROTECTED;
                JType returnType = ListType;
                JMethod dynamicMethod = null;
                dynamicMethod = implClass.method(mods, implClass, "getTransientProperties");
                dynamicMethod.type(returnType);
                JBlock body = dynamicMethod.body();
                JBlock staticBlock = implClass.init();
                staticBlock.invoke(dynamicMethod);
                JFieldVar fieldVar = null;
                fieldVar = implClass.fields().get("transientProperties");
                JConditional cond = null;
                cond = body._if(fieldVar.eq(JExpr._null()));
                cond._then().invoke("setTransientPropertyNames");
                body._return(fieldVar);
            }
        }
    }

    public void generateSetPropertyNames() {
        int mods = JMod.STATIC | JMod.PRIVATE | JMod.SYNCHRONIZED;

        for (ClassOutline classOutline : outline.getClasses()) {

            JDefinedClass implClass = classOutline.implClass;
            if (implClass.name().equals("Entity")) {
                JFieldVar fieldVar = implClass.field(JMod.STATIC | JMod.PRIVATE, mapType, "properties", JExpr._null());
                JMethod dynamicMethodForSubTypeMap = implClass.method(mods, implClass, "setPropertyNames");
                dynamicMethodForSubTypeMap.type(voidType);
                JBlock bodyOfSubTypeMapMethod = dynamicMethodForSubTypeMap.body();
                JBlock staticBlock = implClass.init();
                staticBlock.invoke(dynamicMethodForSubTypeMap);
                JConditional cond = null;
                cond = bodyOfSubTypeMapMethod._if(fieldVar.eq(JExpr._null()));
                cond._then().assign(fieldVar, JExpr._new(mapType));
                for (ClassOutline cClass : outline.getClasses()) {
                    JDefinedClass cImplClass = cClass.implClass;
                    if (cImplClass.outer() == null && implClass.isAssignableFrom(cImplClass))
                        bodyOfSubTypeMapMethod.add(fieldVar.invoke("put").arg(cImplClass.name()).arg(cImplClass.staticInvoke("getPropertyNames").arg(cImplClass.name())));
                }
            }
        }
    }

    public void generateIsUnset() {

        for (ClassOutline classOutline : outline.getClasses()) {

            JDefinedClass implClass = classOutline.implClass;
            if (implClass.name().equals("Entity")) {
                JMethod dynamicMethodForSubTypeMap = implClass.method(JMod.PUBLIC, implClass, "isUnset");
                JType returnType = JType.parse(outline.getCodeModel(), "boolean");
                dynamicMethodForSubTypeMap.type(returnType);
                JVar var = dynamicMethodForSubTypeMap.param(String.class, PROPNAME);
                JBlock bodyOfSubTypeMapMethod = dynamicMethodForSubTypeMap.body();
                JConditional cond = null;
                JFieldVar fieldVar = implClass.fields().get("deletedProperties");
                cond = bodyOfSubTypeMapMethod._if(fieldVar.ne(JExpr._null()));

                JConditional cond1 = cond._then()._if(fieldVar.invoke("contains").arg(var));
                cond1._then()._return(JExpr.TRUE);
                bodyOfSubTypeMapMethod._return(JExpr.FALSE);
            }
        }
    }
}
