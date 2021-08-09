/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.ejb.EntityBean;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;

import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.NO_PARAMS;

/**
 * Just In Time Deployment utility methods, which are of general purpose use
 * for EJB deployment for dealing with ASM, generating code, or managing
 * class bytes. <p>
 */
public final class EJBUtils
{
    private static final TraceComponent tc = Tr.register(EJBUtils.class,
                                                         JITUtils.JIT_TRACE_GROUP,
                                                         JITUtils.JIT_RSRC_BUNDLE);

    /**
     * Returns the 'methodId' of the specified method, which is basically
     * the index of the method in the array of all methods. <p>
     * 
     * The methodId is 'hard coded' into the generated code, and passed to
     * preInvoke, to allow the runtime to quickly associate the method
     * call with its corresponding method info. And, to insure the methodId
     * is the same between the generated code (formerly EJBDeploy) and
     * the runtime, the methods are sorted in basically alphabetical order. <p>
     * 
     * This method searches through the list of the 'sorted' set of all methods,
     * to find the correct index or methodId for the specified index. And, it
     * takes advantage of the fact they are sorted, by not searching from the
     * beginning of the list every time, but from the last index that was
     * found. <p>
     * 
     * Unfortunately, the methods are not sorted by method name and parameters
     * alone, but also the return type and fully package qualified classname
     * of the defining class. This poses a problem when a method is present
     * on multiple interfaces for an EJB; for example, both the component
     * and a business interface. Duplicate method name / parameter combinations
     * do not exist in the sorted list.. and this method does find matches
     * regardless of the defining class, but the sort order may vary depending
     * on the interface currently being deployed. To accomodate this, this
     * method will always search the entire list of all methods, though it
     * will generally see a performance advantage by starting with the last
     * found index. <p>
     * 
     * @param method method to get the methodId/index for
     * @param allMethods array of all methods for the desired interface type
     * @param startIndex position in the array of all methods to start
     *            looking for the current method.
     * @return methodId for the specified method.
     **/
    static int getMethodId(Method method,
                           Method[] allMethods,
                           int startIndex)
    {
        int numMethods = allMethods.length;
        for (int i = startIndex; i < numMethods; ++i)
        {
            if (methodsMatch(method, allMethods[i]))
                return i;
        }

        // Search through from the beginning back to the starting point,
        // in case the method is on multiple interfaces, and thus may
        // be sorted in a different order in the allMethods list.        d369262.6
        for (int i = 0; i < startIndex; ++i)
        {
            if (methodsMatch(method, allMethods[i]))
                return i;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "getMethodId: start = " + startIndex);
            Tr.debug(tc, "getMethodId: method = " + method);
            for (int i = 0; i < numMethods; ++i)
            {
                Tr.debug(tc, "getMethodId: [" + i + "] = " + allMethods[i]);
            }
        }

        throw new RuntimeException("Internal Error: Method not found: " + method);
    }

    /**
     * Returns true for two methods that have the same name and parameters. <p>
     * 
     * Similar to Method.equals(), except declaring class and return type are
     * NOT considered. <p>
     * 
     * Useful for determining when multiple component or business interfaces
     * implement the same method... as they will both be mapped to the same
     * methodId. <p>
     * 
     * @param m1 first of two methods to compare
     * @param m2 second of two methods to compare
     * 
     * @return true if both methods have the same name and parameters;
     *         otherwise false.
     **/
    static boolean methodsMatch(Method m1, Method m2)
    {
        if (m1 == m2)
        {
            return true;
        }
        else if (m1.getName().equals(m2.getName()))
        {
            Class<?>[] parms1 = m1.getParameterTypes();
            Class<?>[] parms2 = m2.getParameterTypes();
            if (parms1.length == parms2.length)
            {
                int length = parms1.length;
                for (int i = 0; i < length; i++)
                {
                    if (parms1[i] != parms2[i])
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    /*
     * private static int addJaccCopyParameters(MethodVisitor mv,
     * int index,
     * String className,
     * Class[] parameters)
     * {
     * int numParams = parameters.length;
     * 
     * // Object[] aobj = (Object[])null;
     * mv.visitInsn(ACONST_NULL);
     * mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
     * mv.visitVarInsn(ASTORE, index);
     * 
     * // if ( container.doesJaccNeedsEJBArguments(this) )
     * mv.visitVarInsn(ALOAD, 0);
     * mv.visitFieldInsn(GETFIELD, className, "container", "Lcom/ibm/ejs/container/EJSContainer;");
     * mv.visitVarInsn(ALOAD, 0);
     * mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer", "doesJaccNeedsEJBArguments", "(Lcom/ibm/ejs/container/EJSWrapperBase;)Z");
     * Label l1 = new Label();
     * mv.visitJumpInsn(IFEQ, l1);
     * 
     * // aobj = new Object[numParms];
     * switch (numParams)
     * {
     * case 0:
     * mv.visitInsn(ICONST_0);
     * break;
     * case 1:
     * mv.visitInsn(ICONST_1);
     * break;
     * case 2:
     * mv.visitInsn(ICONST_2);
     * break;
     * case 3:
     * mv.visitInsn(ICONST_3);
     * break;
     * case 4:
     * mv.visitInsn(ICONST_4);
     * break;
     * case 5:
     * mv.visitInsn(ICONST_5);
     * break;
     * default:
     * if ( numParams < 128 )
     * mv.visitIntInsn(BIPUSH, numParams);
     * else
     * mv.visitIntInsn(SIPUSH, numParams);
     * }
     * mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
     * mv.visitVarInsn(ASTORE, index);
     * 
     * // set each parameter in to the object array
     * int pIndex = 1;
     * for (int i=0; i<numParams; ++i)
     * {
     * String typeName = parameters[i].getName();
     * 
     * if ( typeName.equals("boolean") )
     * {
     * }
     * else if ( typeName.equals("int") )
     * {
     * }
     * else if ( typeName.equals("long") )
     * {
     * }
     * else if ( typeName.equals("double") )
     * {
     * }
     * else if ( typeName.equals("float") )
     * {
     * }
     * else if ( typeName.equals("char") )
     * {
     * }
     * else if ( typeName.equals("byte") )
     * {
     * }
     * else if ( typeName.equals("short") )
     * {
     * }
     * else
     * {
     * mv.visitVarInsn(ALOAD, index);
     * mv.visitInsn(ICONST_0);
     * mv.visitVarInsn(ALOAD, pIndex);
     * mv.visitInsn(AASTORE);
     * ++pIndex;
     * }
     * }
     * 
     * mv.visitVarInsn(ALOAD, index);
     * mv.visitInsn(ICONST_0);
     * mv.visitVarInsn(ALOAD, 1);
     * mv.visitInsn(AASTORE);
     * 
     * mv.visitVarInsn(ALOAD, index);
     * mv.visitInsn(ICONST_1);
     * mv.visitVarInsn(ALOAD, 2);
     * mv.visitInsn(AASTORE);
     * 
     * // Add the closing } of if (doesJaccNeedsEJBArguments)
     * mv.visitLabel(l1);
     * 
     * return index;
     * }
     */

    /**
     * Perform validation of the EJB Class provided by the cutomer, similar
     * to validation performed by EJBDeploy. <p>
     * 
     * An EJBConfigurationException will be thrown if the following rules
     * are violated:
     * 
     * For all bean types:
     * <ul>
     * <li> The class must be defined as public, must not be final, and must not
     * be abstract. The class must be a top level class.
     * <li> The class must have a public constructor that takes no parameters.
     * <li> The class must not define the finalize() method.
     * </ul>
     * 
     * For Entity beans:
     * <ul>
     * <li> The class must implement, directly or indirectly, the
     * javax.ejb.EntityBean interface.
     * </ul>
     * 
     * For Container Managed Entity beans:
     * <ul>
     * <li> The class must be defined as abstract.
     * </ul>
     * 
     * @param ejbClass the EJB implementation class provided by customer.
     * @param beanName name used to identify the bean if an error is logged.
     * @param beanType Type of EJB, using constants defined in
     *            EJBComponentMetaData. Not all bean types are
     *            supported; only Stateless, Stateful, and BMP.
     **/
    // d457128
    static void validateEjbClass(Class<?> ejbClass,
                                 String beanName,
                                 int beanType)
                    throws EJBConfigurationException
    {
        int modifiers = ejbClass.getModifiers();

        if (!Modifier.isPublic(modifiers))
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NON_PUBLIC_CLASS_CNTR5003E",
                     new Object[] { beanName,
                                   ejbClass.getName() });
            throw new EJBConfigurationException("EJB class " + ejbClass.getName() +
                                                " must be defined as public : " + beanName);
        }

        if (Modifier.isFinal(modifiers))
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INVALID_FINAL_CLASS_CNTR5004E",
                     new Object[] { beanName,
                                   ejbClass.getName() });
            throw new EJBConfigurationException("EJB class " + ejbClass.getName() +
                                                " must not be defined as final : " + beanName);
        }

        // CMP 2.x beans must be abstract, whereas CMP 1.x beans must NOT be
        // abstract, so just skipping CMP beans entirely.
        if (beanType != InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY)
        {
            if (Modifier.isAbstract(modifiers))
            {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_INVALID_ABSTRACT_CLASS_CNTR5005E",
                         new Object[] { beanName,
                                       ejbClass.getName() });
                throw new EJBConfigurationException("EJB class " + ejbClass.getName() +
                                                    " must not be defined as abstract : " + beanName);
            }
        }

        if (ejbClass.getEnclosingClass() != null)
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NOT_TOP_LEVEL_CLASS_CNTR5006E",
                     new Object[] { beanName,
                                   ejbClass.getName() });
            throw new EJBConfigurationException("EJB class " + ejbClass.getName() +
                                                " must be a top level class : " + beanName);
        }

        try
        {
            ejbClass.getConstructor(NO_PARAMS);
        } catch (Throwable ex)
        {
            // FFDC is not needed, as a meaningful exception is being thrown.
            // FFDCFilter.processException(ejbex, CLASS_NAME + ".validateEjbClass", "653");
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_DEFAULT_CTOR_CNTR5007E",
                     new Object[] { beanName,
                                   ejbClass.getName() });
            throw new EJBConfigurationException("EJB class " + ejbClass.getName() +
                                                " must have a public constructor that takes no parameters : " +
                                                beanName, ex);
        }

        try
        {
            ejbClass.getDeclaredMethod("finalize", NO_PARAMS);

            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INVALID_FINALIZE_MTHD_CNTR5008E",
                     new Object[] { beanName,
                                   ejbClass.getName() });
            throw new EJBConfigurationException("EJB class " + ejbClass.getName() +
                                                " must not define the finalize() method : " +
                                                beanName);
        } catch (Throwable ex)
        {
            // FFDC is not needed... finalize method should NOT be found.
            // FFDCFilter.processException(ejbex, CLASS_NAME + ".validateEjbClass", "667");
        }

        if (beanType == InternalConstants.TYPE_BEAN_MANAGED_ENTITY ||
            beanType == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY)
        {
            if (!(EntityBean.class).isAssignableFrom(ejbClass))
            {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_MISSING_ENTITYBEAN_CNTR5009E",
                         new Object[] { beanName,
                                       ejbClass.getName() });
                throw new EJBConfigurationException("EJB Entity class " + ejbClass.getName() +
                                                    " must implement javax.ejb.EntityBean : " + beanName);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "validateEjbClass : successful : " +
                         ejbClass.getName());
    }

}
