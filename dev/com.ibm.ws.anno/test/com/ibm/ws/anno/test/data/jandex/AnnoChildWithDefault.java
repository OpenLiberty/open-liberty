package com.ibm.ws.anno.test.data.jandex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD,
           ElementType.PARAMETER, ElementType.METHOD,
           ElementType.TYPE } )
//         ElementType.TYPE_PARAMETER } ) // requires java8
public @interface AnnoChildWithDefault {
    int intValue() default 0;
    String strValue() default "1";
}
