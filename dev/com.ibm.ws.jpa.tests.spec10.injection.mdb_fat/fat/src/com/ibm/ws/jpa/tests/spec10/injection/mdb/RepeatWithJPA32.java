/**
 *
 */
package com.ibm.ws.jpa.tests.spec10.injection.mdb;

import componenttest.rules.repeater.JakartaEE11Action;
import componenttest.rules.repeater.RepeatTestAction;

public class RepeatWithJPA32 extends JakartaEE11Action implements RepeatTestAction {
    public static final String ID = "JPA32";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "JPA 3.2";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        RepeaterInfo.repeatPhase = "jpa32.xml";
    }

//    @Override
//    public String getID() {
//        return ID;
//    }

}
