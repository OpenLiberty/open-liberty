/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyContainingAssertion;
import org.apache.neethi.PolicyOperator;

public class AssertionInfoMap extends HashMap<QName, Collection<AssertionInfo>> {

    private static final long serialVersionUID = -4059701923851991413L;
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AssertionInfoMap.class, "APIMessages");

    public AssertionInfoMap(Policy p) {
        this(getAssertions(p));
    }

    public AssertionInfoMap(Collection<? extends Assertion> assertions) {
        super(assertions.size() < 6 ? 6 : assertions.size());
        for (Assertion a : assertions) {
            putAssertionInfo(a);
        }
    }

    private void putAssertionInfo(Assertion a) {
        if (a instanceof PolicyContainingAssertion) {
            Policy p = ((PolicyContainingAssertion)a).getPolicy();
            if (p != null) {
                List<Assertion> pcs = new ArrayList<>();
                getAssertions(p, pcs);
                for (Assertion na : pcs) {
                    putAssertionInfo(na);
                }
            }
        }
        AssertionInfo ai = new AssertionInfo(a);
        Collection<AssertionInfo> ail = get(a.getName());
        if (ail == null) {
            ail = new ArrayList<>();
            put(a.getName(), ail);
        }
        for (AssertionInfo ai2 : ail) {
            if (ai2.getAssertion() == a) {
                return;
            }
        }
        ail.add(ai);
    }

    public Collection<AssertionInfo> getAssertionInfo(QName name) {
        Collection<AssertionInfo> ail = get(name);
        return ail != null ? ail
            : CastUtils.cast(Collections.EMPTY_LIST, AssertionInfo.class);

    }

    public boolean supportsAlternative(PolicyComponent assertion,
                                       List<QName> errors) {
        boolean pass = true;
        if (assertion instanceof PolicyAssertion) {
            PolicyAssertion a = (PolicyAssertion)assertion;
            if (!a.isAsserted(this) && !a.isOptional()) {
                errors.add(a.getName());
                pass = false;
            }
        } else if (assertion instanceof Assertion) {
            Assertion ass = (Assertion)assertion;
            Collection<AssertionInfo> ail = getAssertionInfo(ass.getName());
            boolean found = false;
            for (AssertionInfo ai : ail) {
                if (ai.getAssertion().equal(ass) || ai.getAssertion().equals(ass)) {
                    found = true;
                    if (!ai.isAsserted() && !ass.isOptional()) {
                        errors.add(ass.getName());
                        pass = false;
                    }
                }
            }
            if (!found) {
                errors.add(ass.getName());
                return false;
            }
        }
        if (assertion instanceof PolicyContainingAssertion) {
            Policy p = ((PolicyContainingAssertion)assertion).getPolicy();
            if (p != null) {
                Iterator<List<Assertion>> alternatives = p.getAlternatives();
                while (alternatives.hasNext()) {
                    List<Assertion> pc = alternatives.next();
                    for (Assertion p2 : pc) {
                        pass &= supportsAlternative(p2, errors);
                    }
                }
            }
        }
        return pass;
    }
    public boolean supportsAlternative(Collection<? extends PolicyComponent> alternative,
                                       List<QName> errors) {
        boolean pass = true;
        for (PolicyComponent a : alternative) {
            pass &= supportsAlternative(a, errors);
        }
        return pass;
    }

    public List<List<Assertion>> checkEffectivePolicy(Policy policy) {
        List<List<Assertion>> validated = new ArrayList<>(4);
        List<QName> errors = new ArrayList<>();
        Iterator<List<Assertion>> alternatives = policy.getAlternatives();
        while (alternatives.hasNext()) {
            List<Assertion> pc = alternatives.next();
            if (supportsAlternative(pc, errors)) {
                validated.add(pc);
            }
        }
        if (!validated.isEmpty()) {
            return validated;
        }

        Set<String> msgs = new LinkedHashSet<>();

        for (QName name : errors) {
            Collection<AssertionInfo> ais = getAssertionInfo(name);
            boolean found = false;
            for (AssertionInfo ai : ais) {
                if (!ai.isAsserted()) {
                    String s = name.toString();
                    if (ai.getErrorMessage() != null) {
                        s += ": " + ai.getErrorMessage();
                    }
                    msgs.add(s);
                    found = true;
                }
            }
            if (!found) {
                msgs.add(name.toString());
            }
        }
        StringBuilder error = new StringBuilder();
        for (String msg : msgs) {
            error.append('\n').append(msg);
        }

        throw new PolicyException(new Message("NO_ALTERNATIVE_EXC", BUNDLE, error.toString()));
    }


    public void check() {
        for (Collection<AssertionInfo> ais : values()) {
            for (AssertionInfo ai : ais) {
                if (!ai.isAsserted()) {
                    throw new PolicyException(new org.apache.cxf.common.i18n.Message(
                        "NOT_ASSERTED_EXC", BUNDLE, ai.getAssertion().getName()));
                }
            }
        }
    }
    private static Collection<Assertion> getAssertions(PolicyOperator p) {
        Collection<Assertion> assertions = new ArrayList<>();
        getAssertions(p, assertions);
        return assertions;
    }

    private static void getAssertions(PolicyOperator p, Collection<Assertion> assertions) {
        List<PolicyComponent> pcs = p.getPolicyComponents();
        for (PolicyComponent pc : pcs) {
            if (pc instanceof Assertion) {
                assertions.add((Assertion)pc);
            } else {
                getAssertions((PolicyOperator)pc, assertions);
            }
        }
    }
}
