package com.ibm.ws.cdi12.fat.injectparameters;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

@ApplicationScoped
public class TestProducer {

    @Produces
    @Named("resource1")
    private final String resource1 = "test1";

    @Produces
    @Named("resource2")
    private final String resource2 = "test2";

    @Produces
    @Named("resource3")
    private final String resource3 = "test3";

    @Produces
    @Named("resource4")
    private final String resource4 = "test4";

    @Produces
    @Named("resource5")
    private final String resource5 = "test5";

    @Produces
    @Named("resource6")
    private final String resource6 = "test6";

    @Produces
    @Named("resource7")
    private final String resource7 = "test7";

    @Produces
    @Named("resource8")
    private final String resource8 = "test8";

    @Produces
    @Named("resource9")
    private final String resource9 = "test9";

    @Produces
    @Named("resource10")
    private final String resource10 = "test10";

    @Produces
    @Named("resource11")
    private final String resource11 = "test11";

    @Produces
    @Named("resource12")
    private final String resource12 = "test12";

    @Produces
    @Named("resource13")
    private final String resource13 = "test13";

    @Produces
    @Named("resource14")
    private final String resource14 = "test14";

    @Produces
    @Named("resource15")
    private final String resource15 = "test15";

    @Produces
    @Named("resource16")
    private final String resource16 = "test16";
}
