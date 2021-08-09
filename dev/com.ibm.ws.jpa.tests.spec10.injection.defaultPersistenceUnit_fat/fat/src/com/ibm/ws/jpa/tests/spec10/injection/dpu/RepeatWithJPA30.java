/**
 *
 */
package com.ibm.ws.jpa.tests.spec10.injection.dpu;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTestAction;

public class RepeatWithJPA30 extends JakartaEE9Action implements RepeatTestAction {
    public static final String ID = "JPA30";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "JPA 3.0";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "jpa30.xml";
    }

//    @Override
//    public String getID() {
//        return ID;
//    }

}
