package com.ibm.ws.anno.test.data.jandex;

@AnnoParentWithDefault

@AnnoParentWithoutDefault(
    intValue = 1,
    strValue = "2",
    child = @AnnoChildWithoutDefault( intValue = 3, strValue = "3" ),
    children = { @AnnoChildWithoutDefault( intValue = 41, strValue = "41" ),
                 @AnnoChildWithoutDefault( intValue = 42, strValue = "42" ) } )

// public class AnnoTarget<@AnnoChildWithDefault TypeParm> { // Requires java8
public class AnnoTarget<TypeParm> {
    @AnnoChildWithDefault()
    public int intField;
    @AnnoChildWithoutDefault( intValue = 20, strValue = "20" )
    public int strField;

    @AnnoChildWithDefault()
    public int intMethod_1() {
        return 10;
    }
    @AnnoChildWithoutDefault( intValue = 20, strValue = "20" )
    public int intMethod_2() {
        return 20;
    }

    @AnnoChildWithDefault()
    public int intMethod_3( @AnnoChildWithDefault int intParm ) {
        return intParm;
    }

    @AnnoChildWithoutDefault( intValue = 40, strValue = "40" )
    public int strMethod_3( @AnnoChildWithoutDefault( intValue = 41, strValue = "41") int intParm ) {
        return intParm;
    }
}
