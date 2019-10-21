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
package com.ibm.aries.buildtasks.semantic.versioning.model.decls;

import static com.ibm.aries.buildtasks.utils.SemanticVersioningUtils.htmlOneLineBreak;
import static com.ibm.aries.buildtasks.utils.SemanticVersioningUtils.htmlTwoLineBreaks;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.aries.buildtasks.semantic.versioning.BinaryCompatibilityStatus;
import com.ibm.aries.buildtasks.utils.SemanticVersioningUtils;

public class ClassDeclaration extends GenericDeclaration {

    // Binary Compatibility - deletion of package-level access field/method/constructors of classes and interfaces in the package
    // will not break binary compatibility when an entire package is updated.

    // Assumptions:
    // 1.  This tool assumes that the deletion of package-level fields/methods/constructors is not break binary compatibility 
    // based on the assumption of the entire package is updated.
    // 

    private final String superName;
    private final String[] interfaces;
    private final Map<String, FieldDeclaration> fields;
    private final Map<String, Set<MethodDeclaration>> methods;

    private final Map<String, Set<MethodDeclaration>> methodsInUpperChain = new HashMap<String, Set<MethodDeclaration>>();
    private final Map<String, FieldDeclaration> fieldsInUpperChain = new HashMap<String, FieldDeclaration>();
    private final Collection<String> supers = new ArrayList<String>();

    private long serialVersionUID;

    private final BinaryCompatibilityStatus binaryCompatible = new BinaryCompatibilityStatus(true, null);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClassDecl@" + this.hashCode());
        sb.append(" name:" + getName());
        sb.append(" methods:" + getMethods().size());
        sb.append(" fields: " + getFields().size());
        return sb.toString();
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder();
        sb.append("  <clsdecl>\n");
        sb.append("    <name>" + escapeXML(getName()) + "</name>\n");
        sb.append("    <access>" + getRawAccess() + "</access>\n");
        sb.append("    <superName>" + escapeXML(getSuperName()) + "</superName>\n");
        if (getSignature() != null) {
            sb.append("    <signature>" + escapeXML(getSignature()) + "</signature>\n");
        }
        //do not compute serial uid's for interfaces.. or non-serializable ;p it doesnt work.
        if (!isInterface() && (getAllSupers().contains(SemanticVersioningUtils.SERIALIZABLE_CLASS_IDENTIFIER))) {
            sb.append("    <serialuid>" + getSerialVersionUID() + "</serialuid>\n");
        }
        if (interfaces != null && interfaces.length > 0) {
            sb.append("    <interfaces>\n");
            for (String s : interfaces) {
                sb.append("      <interface>" + escapeXML(s) + "</interface>\n");
            }
            sb.append("    </interfaces>\n");
        }
        if (supers != null && supers.size() > 0) {
            sb.append("    <supers>\n");
            for (String s : supers) {
                sb.append("      <super>" + escapeXML(s) + "</super>\n");
            }
            sb.append("    </supers>\n");
        }
        if (fields != null && fields.size() > 0) {
            sb.append("    <fields>\n");
            for (FieldDeclaration f : getFields().values()) {
                sb.append(f.toXML());
            }
            sb.append("    </fields>\n");
        }
        Collection<FieldDeclaration> superFields = getFieldsInUpperChain().values();
        if (superFields != null && superFields.size() > 0) {
            sb.append("    <superfields>\n");
            for (FieldDeclaration f : superFields) {
                sb.append(f.toXML());
            }
            sb.append("    </superfields>\n");
        }

        Map<String, Set<MethodDeclaration>> methodInfo = getMethods();
        if (methodInfo != null && methodInfo.size() > 0) {
            sb.append("    <methods>\n");
            for (Map.Entry<String, Set<MethodDeclaration>> m : methodInfo.entrySet()) {
                for (MethodDeclaration md : m.getValue()) {
                    sb.append(md.toXML());
                }
            }
            sb.append("    </methods>\n");
        }
        Map<String, Set<MethodDeclaration>> superMethodInfo = getMethods();
        if (superMethodInfo != null && superMethodInfo.size() > 0) {
            sb.append("    <supermethods>\n");
            for (Map.Entry<String, Set<MethodDeclaration>> m : superMethodInfo.entrySet()) {
                for (MethodDeclaration md : m.getValue()) {
                    sb.append(md.toXML());
                }
            }
            sb.append("    </supermethods>\n");
        }
        sb.append("  </clsdecl>\n");
        return sb.toString();
    }

    public Map<String, FieldDeclaration> getFields() {
        return fields;
    }

    public Map<String, FieldDeclaration> getAllFields() {
        Map<String, FieldDeclaration> allFields = new HashMap<String, FieldDeclaration>(getFields());
        Map<String, FieldDeclaration> fieldsFromSupers = getFieldsInUpperChain();
        putIfAbsent(allFields, fieldsFromSupers);
        return allFields;
    }

    private void putIfAbsent(Map<String, FieldDeclaration> allFields,
                             Map<String, FieldDeclaration> fieldsFromSupers) {
        for (Map.Entry<String, FieldDeclaration> superFieldEntry : fieldsFromSupers.entrySet()) {
            String fieldName = superFieldEntry.getKey();
            FieldDeclaration fd = superFieldEntry.getValue();
            if (allFields.get(fieldName) == null) {
                allFields.put(fieldName, fd);
            }
        }
    }

    /**
     * Get the methods in the current class plus the methods in the upper chain
     * 
     * @return
     */
    public Map<String, Set<MethodDeclaration>> getAllMethods() {
        Map<String, Set<MethodDeclaration>> methods = new HashMap<String, Set<MethodDeclaration>>(getMethods());
        Map<String, Set<MethodDeclaration>> methodsFromSupers = getMethodsInUpperChain();
        for (Map.Entry<String, Set<MethodDeclaration>> superMethodsEntry : methodsFromSupers.entrySet()) {
            Set<MethodDeclaration> overloadingMethods = methods.get(superMethodsEntry.getKey());
            if (overloadingMethods != null) {
                overloadingMethods.addAll(superMethodsEntry.getValue());
            } else {
                methods.put(superMethodsEntry.getKey(), superMethodsEntry.getValue());
            }

        }
        return methods;
    }

    public Map<String, Set<MethodDeclaration>> getMethods() {
        return methods;
    }

    public ClassDeclaration(int access, String name, String signature, String superName,
                            String[] interfaces) {
        super(access, name, signature);
        this.superName = superName;
        this.interfaces = interfaces;
        this.fields = new HashMap<String, FieldDeclaration>();
        this.methods = new HashMap<String, Set<MethodDeclaration>>();
    }

    public Map<String, FieldDeclaration> getFieldsInUpperChain() {
        return fieldsInUpperChain;
    }

    public void addFieldInUpperChain(Map<String, FieldDeclaration> fields) {
        putIfAbsent(fieldsInUpperChain, fields);
    }

    public Map<String, Set<MethodDeclaration>> getMethodsInUpperChain() {
        return methodsInUpperChain;
    }

    public void addMethodsInUpperChain(Map<String, Set<MethodDeclaration>> methods) {
        for (Map.Entry<String, Set<MethodDeclaration>> method : methods.entrySet()) {
            String methodName = method.getKey();
            Set<MethodDeclaration> mds = new HashSet<MethodDeclaration>();
            if (methodsInUpperChain.get(methodName) != null) {
                mds.addAll(methodsInUpperChain.get(methodName));
            }
            mds.addAll(method.getValue());

            methodsInUpperChain.put(methodName, mds);
        }
    }

    public Collection<String> getAllSupers() {
        return supers;
    }

    public void addSuper(String sName) {
        supers.add(sName);
    }

    public void addSuper(Collection<String> sNames) {
        supers.addAll(sNames);
    }

    public String getSuperName() {
        return superName;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public void addFields(FieldDeclaration fd) {
        fields.put(fd.getName(), fd);
    }

    public void addMethods(MethodDeclaration md) {
        String key = md.getName();
        Set<MethodDeclaration> overloadingMethods = methods.get(key);
        if (overloadingMethods != null) {
            overloadingMethods.add(md);
            methods.put(key, overloadingMethods);
        } else {
            Set<MethodDeclaration> mds = new HashSet<MethodDeclaration>();
            mds.add(md);
            methods.put(key, mds);
        }
    }

    public BinaryCompatibilityStatus getBinaryCompatibleStatus(ClassDeclaration old) {
        // check class signature, fields, methods
        if (old == null) {
            return binaryCompatible;
        }
        StringBuilder reason = new StringBuilder();
        boolean isCompatible = true;

        Set<BinaryCompatibilityStatus> bcsSet = new HashSet<BinaryCompatibilityStatus>();
        bcsSet.add(getClassSignatureBinaryCompatibleStatus(old));
        bcsSet.add(getAllMethodsBinaryCompatibleStatus(old));
        bcsSet.add(getAllFieldsBinaryCompatibleStatus(old));
        bcsSet.add(getAllSuperPresentStatus(old));
        if (!isInterface()) {
            bcsSet.add(getSerializableBackCompatable(old));
        }
        for (BinaryCompatibilityStatus bcs : bcsSet) {
            if (!bcs.isCompatible()) {
                isCompatible = false;
                reason.append(bcs.getReason());
            }
        }
        if (!isCompatible) {
            return new BinaryCompatibilityStatus(isCompatible, reason.toString());
        } else {
            return binaryCompatible;
        }

    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getAccess());
    }

    @Override
    public boolean isInterface() {
        return Modifier.isInterface(getAccess());
    }

    private BinaryCompatibilityStatus getClassSignatureBinaryCompatibleStatus(ClassDeclaration originalClass) {
        // if a class was not abstract but changed to abstract
        // not final changed to final
        // public changed to non-public
        String prefix = " The class " + getName();
        StringBuilder reason = new StringBuilder();
        boolean compatible = true;
        if (!!!originalClass.isAbstract() && isAbstract()) {
            reason.append(prefix + " was not abstract but is changed to be abstract.");
            compatible = false;
        }
        if (!!!originalClass.isFinal() && isFinal()) {
            reason.append(prefix + " was not final but is changed to be final.");
            compatible = false;
        }
        if (originalClass.isPublic() && !!!isPublic()) {
            reason.append(prefix + " was public but is changed to be non-public.");
            compatible = false;
        }
        return new BinaryCompatibilityStatus(compatible, compatible ? null : reason.toString());
    }

    public BinaryCompatibilityStatus getAllFieldsBinaryCompatibleStatus(ClassDeclaration originalClass) {
        // for each field to see whether the same field has changed
        // not final -> final
        // static <-> nonstatic
        Map<String, FieldDeclaration> oldFields = originalClass.getAllFields();
        Map<String, FieldDeclaration> newFields = getAllFields();
        return areFieldsBinaryCompatible(oldFields, newFields);
    }

    private BinaryCompatibilityStatus areFieldsBinaryCompatible(Map<String, FieldDeclaration> oldFields, Map<String, FieldDeclaration> currentFields) {

        boolean overallCompatible = true;
        StringBuilder reason = new StringBuilder();

        for (Map.Entry<String, FieldDeclaration> entry : oldFields.entrySet()) {
            FieldDeclaration bef_fd = entry.getValue();
            FieldDeclaration cur_fd = currentFields.get(entry.getKey());

            boolean compatible = isFieldBinaryCompatible(reason, bef_fd, cur_fd);
            if (!compatible) {
                overallCompatible = compatible;
            }

        }
        if (!overallCompatible) {
            return new BinaryCompatibilityStatus(overallCompatible, reason.toString());
        } else {
            return binaryCompatible;
        }
    }

    private boolean isFieldBinaryCompatible(StringBuilder reason,
                                            FieldDeclaration bef_fd, FieldDeclaration cur_fd) {
        String fieldName = bef_fd.getName();
        //only interested in the public or protected fields

        boolean compatible = true;

        if (bef_fd.isPublic() || bef_fd.isProtected()) {
            String prefix = htmlOneLineBreak + "The " + (bef_fd.isPublic() ? "public" : "protected") + " field " + fieldName;

            if (cur_fd == null) {
                reason.append(prefix + " has been deleted.");
                compatible = false;
            } else {

                if ((!!!bef_fd.isFinal()) && (cur_fd.isFinal())) {
                    // make sure it has not been changed to final
                    reason.append(prefix + " was not final but has been changed to be final.");
                    compatible = false;

                }
                if (bef_fd.isStatic() != cur_fd.isStatic()) {
                    // make sure it the static signature has not been changed
                    reason.append(prefix + " was static but is changed to be non static or vice versa.");
                    compatible = false;
                }
                // check to see the field type is the same 
                if (!isFieldTypeSame(bef_fd, cur_fd)) {
                    reason.append(prefix + " has changed its type.");
                    compatible = false;

                }
                if (SemanticVersioningUtils.isLessAccessible(bef_fd, cur_fd)) {
                    // check whether the new field is less accessible than the old one
                    reason.append(prefix + " becomes less accessible.");
                    compatible = false;
                }

            }
        }
        return compatible;
    }

    /**
     * Return whether the serializable class is binary compatible. The serial verison uid change breaks binary compatibility.
     * 
     * @param old
     * @return
     */
    private BinaryCompatibilityStatus getSerializableBackCompatable(ClassDeclaration old) {
        // It does not matter one of them is not seralizable.
        boolean serializableBackCompatible = true;
        String reason = null;
        if ((getAllSupers().contains(SemanticVersioningUtils.SERIALIZABLE_CLASS_IDENTIFIER))
            && (old.getAllSupers().contains(SemanticVersioningUtils.SERIALIZABLE_CLASS_IDENTIFIER))) {
            // check to see whether the serializable id is the same
            //ignore if it is enum
            if ((!getAllSupers().contains(SemanticVersioningUtils.ENUM_CLASS) && (!old.getAllSupers().contains(SemanticVersioningUtils.ENUM_CLASS)))) {
                long oldValue = old.getSerialVersionUID();
                long curValue = this.getSerialVersionUID();
                if ((oldValue != curValue)) {
                    serializableBackCompatible = false;
                    reason = htmlOneLineBreak + "The serializable class is no longer back compatible as the value of SerialVersionUID has changed from " + oldValue + " to "
                             + curValue + ".";
                }
            }
        }

        if (!serializableBackCompatible) {
            return new BinaryCompatibilityStatus(serializableBackCompatible, reason);
        }
        return binaryCompatible;
    }

    public void setSerialVersionUID(long value) {
        this.serialVersionUID = value;
    }

    public long getSerialVersionUID() {
        return this.serialVersionUID;
    }

    private boolean isFieldTypeSame(FieldDeclaration bef_fd, FieldDeclaration cur_fd) {
        return bef_fd.getDesc().equals(cur_fd.getDesc());
    }

    private BinaryCompatibilityStatus getAllMethodsBinaryCompatibleStatus(ClassDeclaration originalClass) {
        //  for all methods
        // no methods should have deleted
        // method return type has not changed
        // method changed from not abstract -> abstract
        Map<String, Set<MethodDeclaration>> oldMethods = originalClass.getAllMethods();
        Map<String, Set<MethodDeclaration>> newMethods = getAllMethods();
        return areMethodsBinaryCompatible(oldMethods, newMethods);
    }

    public BinaryCompatibilityStatus areMethodsBinaryCompatible(Map<String, Set<MethodDeclaration>> oldMethods,
                                                                Map<String, Set<MethodDeclaration>> newMethods) {

        StringBuilder reason = new StringBuilder();
        boolean compatible = true;
        Map<String, Collection<MethodDeclaration>> extraMethods = new HashMap<String, Collection<MethodDeclaration>>();

        //assume all new methods are extra..
        for (Map.Entry<String, Set<MethodDeclaration>> me : newMethods.entrySet()) {
            Collection<MethodDeclaration> mds = new ArrayList<MethodDeclaration>(me.getValue());
            extraMethods.put(me.getKey(), mds);
        }

        //now process the old methods, and report access violations
        //and remove matches from the extra methods map.
        for (Map.Entry<String, Set<MethodDeclaration>> methods : oldMethods.entrySet()) {
            // all overloading methods, check against the current class
            String methodName = methods.getKey();
            Collection<MethodDeclaration> oldMDSigs = methods.getValue();
            // If the method cannot be found in the current class, it means that it has been deleted.
            Collection<MethodDeclaration> newMDSigs = newMethods.get(methodName);
            // for each overloading methods
            outer: for (MethodDeclaration md : oldMDSigs) {
                String mdName = md.getName();

                String prefix = htmlOneLineBreak + "The " + SemanticVersioningUtils.getReadableMethodSignature(mdName, md.getDesc());
                if (md.isProtected() || md.isPublic()) {
                    boolean found = false;
                    if (newMDSigs != null) {
                        // try to find it in the current class
                        for (MethodDeclaration new_md : newMDSigs) {
                            // find the method with the same return type, parameter list 
                            if ((md.equals(new_md))) {
                                found = true;
                                // If the old method is final but the new one is not or vice versa
                                // If the old method is static but the new one is non static
                                // If the old method is not abstract but the new is
                                if (md.isProtected() || md.isPublic()) {
                                    if (!!!Modifier.isFinal(md.getAccess()) && !!!Modifier.isStatic(md.getAccess()) && Modifier.isFinal(new_md.getAccess())) {
                                        compatible = false;
                                        reason.append(prefix + " was not final but has been changed to be final.");
                                    }
                                    if (Modifier.isStatic(md.getAccess()) != Modifier.isStatic(new_md.getAccess())) {
                                        compatible = false;
                                        reason.append(prefix + " has changed from static to non-static or vice versa.");
                                    }
                                    if ((Modifier.isAbstract(new_md.getAccess()) == true) && (Modifier.isAbstract(md.getAccess()) == false)) {
                                        compatible = false;
                                        reason.append(prefix + " has changed from non abstract to abstract. ");
                                    }
                                    if (SemanticVersioningUtils.isLessAccessible(md, new_md)) {
                                        compatible = false;
                                        reason.append(prefix + " is less accessible.");
                                    }
                                }
                                //this method was compatible, so we can forget it.
                                if (compatible) {
                                    // remove from the extra map
                                    Collection<MethodDeclaration> mds = extraMethods.get(methodName);
                                    mds.remove(new_md);
                                    continue outer;
                                }
                            }
                        }
                    }

                    // 
                    // if we are here, it means that we have not found the method with the same description and signature
                    // which means that the method has been deleted. Let's make sure it is not moved to its upper chain.
                    if (!found && (md.isProtected() || md.isPublic())) {
                        if (!isMethodInSuperClass(md)) {
                            compatible = false;
                            reason.append(prefix + " has been deleted or its return type or parameter list has changed.");
                        } else {
                            if (newMDSigs != null) {
                                for (MethodDeclaration new_md : newMDSigs) {
                                    // find the method with the same return type, parameter list 
                                    if ((md.equals(new_md))) {
                                        Collection<MethodDeclaration> mds = extraMethods.get(methodName);
                                        mds.remove(new_md);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Check the newly added method has not caused binary incompatibility
        for (Map.Entry<String, Collection<MethodDeclaration>> extraMethodSet : extraMethods.entrySet()) {
            for (MethodDeclaration md : extraMethodSet.getValue()) {
                String head = htmlOneLineBreak + "The " + SemanticVersioningUtils.getReadableMethodSignature(md.getName(), md.getDesc());
                if (isNewMethodSpecialCase(md, head, reason)) {
                    compatible = false;
                }
            }
        }
        if (compatible) {
            return binaryCompatible;
        } else {
            return new BinaryCompatibilityStatus(compatible, reason.toString());
        }
    }

    /**
     * Return the newly added fields
     * 
     * @param old
     * @return
     */
    public Collection<FieldDeclaration> getExtraFields(ClassDeclaration old) {
        Map<String, FieldDeclaration> oldFields = old.getAllFields();
        Map<String, FieldDeclaration> newFields = getAllFields();
        Map<String, FieldDeclaration> extraFields = new HashMap<String, FieldDeclaration>(newFields);
        for (String key : oldFields.keySet()) {
            extraFields.remove(key);
        }
        return extraFields.values();
    }

    /**
     * Return the extra non-private methods
     * 
     * @param old
     * @return
     */
    public Collection<MethodDeclaration> getExtraMethods(ClassDeclaration old) {
        // Need to find whether there are new methods added.
        Collection<MethodDeclaration> extraMethods = new HashSet<MethodDeclaration>();
        Map<String, Set<MethodDeclaration>> currMethodsMap = getAllMethods();
        Map<String, Set<MethodDeclaration>> oldMethodsMap = old.getAllMethods();

        for (Map.Entry<String, Set<MethodDeclaration>> currMethod : currMethodsMap.entrySet()) {
            String methodName = currMethod.getKey();
            Collection<MethodDeclaration> newMethods = currMethod.getValue();

            // for each  method, we look for whether it exists in the old class
            Collection<MethodDeclaration> oldMethods = oldMethodsMap.get(methodName);
            for (MethodDeclaration new_md : newMethods) {
                if (!new_md.isPrivate()) {
                    if (oldMethods == null || !oldMethods.contains(new_md)) {
                        extraMethods.add(new_md);
                    }
                }
            }
        }
        return extraMethods;
    }

    public boolean isMethodInSuperClass(MethodDeclaration md) {
        // scan the super class and interfaces
        String methodName = md.getName();
        Collection<MethodDeclaration> overloaddingMethods = getMethodsInUpperChain().get(methodName);
        if (overloaddingMethods != null) {
            for (MethodDeclaration value : overloaddingMethods) {
                // method signature and name same and also the method should not be less accessible
                if (md.equals(value) && (!!!SemanticVersioningUtils.isLessAccessible(md, value)) && (value.isStatic() == md.isStatic())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The newly added method is less accessible than the old one in the super or is a static (respectively instance) method.
     * 
     * @param md
     * @return
     */
    public boolean isNewMethodSpecialCase(MethodDeclaration md, String prefix, StringBuilder reason) {
        // scan the super class and interfaces
        String methodName = md.getName();
        boolean special = false;
        Collection<MethodDeclaration> overloaddingMethods = getMethodsInUpperChain().get(methodName);
        if (overloaddingMethods != null) {
            for (MethodDeclaration value : overloaddingMethods) {
                // method signature and name same and also the method should not be less accessible
                if (!SemanticVersioningUtils.CONSTRUTOR.equals(md.getName())) {
                    if (md.equals(value)) {
                        if (SemanticVersioningUtils.isLessAccessible(value, md)) {
                            special = true;
                            reason.append(prefix + " is less accessible than the same method in its parent.");
                        }
                        if (value.isStatic()) {
                            if (!md.isStatic()) {
                                special = true;
                                reason.append(prefix + " is non-static but the same method in its parent is static.");
                            }
                        } else {
                            if (md.isStatic()) {
                                special = true;
                                reason.append(prefix + " is static but the same method is its parent is not static.");
                            }
                        }
                    }
                }
            }
        }
        return special;
    }

    public BinaryCompatibilityStatus getAllSuperPresentStatus(ClassDeclaration old) {
        Collection<String> oldSupers = new ArrayList<String>(old.getAllSupers());
        boolean containsAll = getAllSupers().containsAll(oldSupers);
        if (!!!containsAll) {
            oldSupers.removeAll(getAllSupers());
            return new BinaryCompatibilityStatus(false, htmlTwoLineBreaks + "The superclasses or superinterfaces have stopped being super: " + oldSupers.toString() + ".");
        }
        return binaryCompatible;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime
                 * result
                 + ((binaryCompatible == null) ? 0 : binaryCompatible.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime
                 * result
                 + ((fieldsInUpperChain == null) ? 0 : fieldsInUpperChain
                                 .hashCode());
        result = prime * result + Arrays.hashCode(interfaces);
        result = prime * result + ((methods == null) ? 0 : methods.hashCode());
        result = prime
                 * result
                 + ((methodsInUpperChain == null) ? 0 : methodsInUpperChain
                                 .hashCode());
        result = prime * result
                 + (int) (serialVersionUID ^ (serialVersionUID >>> 32));
        result = prime * result
                 + ((superName == null) ? 0 : superName.hashCode());
        result = prime * result + ((supers == null) ? 0 : supers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClassDeclaration other = (ClassDeclaration) obj;
        if (binaryCompatible == null) {
            if (other.binaryCompatible != null)
                return false;
        } else if (!binaryCompatible.equals(other.binaryCompatible))
            return false;
        if (fields == null) {
            if (other.fields != null)
                return false;
        } else if (!fields.equals(other.fields))
            return false;
        if (fieldsInUpperChain == null) {
            if (other.fieldsInUpperChain != null)
                return false;
        } else if (!fieldsInUpperChain.equals(other.fieldsInUpperChain))
            return false;
        if (!Arrays.equals(interfaces, other.interfaces))
            return false;
        if (methods == null) {
            if (other.methods != null)
                return false;
        } else if (!methods.equals(other.methods))
            return false;
        if (methodsInUpperChain == null) {
            if (other.methodsInUpperChain != null)
                return false;
        } else if (!methodsInUpperChain.equals(other.methodsInUpperChain))
            return false;
        if (serialVersionUID != other.serialVersionUID)
            return false;
        if (superName == null) {
            if (other.superName != null)
                return false;
        } else if (!superName.equals(other.superName))
            return false;
        if (supers == null) {
            if (other.supers != null)
                return false;
        } else if (!supers.equals(other.supers))
            return false;
        return true;
    }

}
