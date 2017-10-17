/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package com.ibm.ws.cdi12.test;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

    
    import javax.inject.Qualifier;

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
    public @interface MetaQualifier {}
