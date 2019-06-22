package com.ibm.ws.anno.jandex.internal;

public class LimitedAnnotationHolder{

    private final DotName name;
    private final LimitedAnnotation[] annotations;

    public LimitedAnnotationHolder(DotName name, LimitedAnnotation[] annotations){
        this.name = name;
        this.annotations = annotations;
    }

    public DotName getName(){
        return name;
    }

    public LimitedAnnotation[] getAnnotations(){
        return annotations;
    }
}