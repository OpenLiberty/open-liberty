package com.ibm.ws.anno.jandex.internal;

public class LimitedAnnotation{
    private final DotName name;
    private final DotName targetName;
    public static final LimitedAnnotation[] EMPTY_ARRAY = new LimitedAnnotation[0];

    public LimitedAnnotation(){
        this(null,null);
    }

    public LimitedAnnotation(DotName name){
        this(name,null);
    }

    public LimitedAnnotation(DotName name, DotName targetName){
        this.name = name;
        this.targetName = targetName;
    }

    public DotName getName(){
        return name;
    }

    public DotName getTargetName(){
        return targetName;
    }
}


