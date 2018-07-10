-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsClientRx-2.1
visibility=public
singleton=true
IBM-API-Package: org.apache.cxf.jaxrs.rx2.client; type="spec", \
io.reactivex; type="spec", \
io.reactivex.annotations; type="spec", \
io.reactivex.disposables; type="spec", \
io.reactivex.exceptions; type="spec", \
io.reactivex.flowables; type="spec", \
io.reactivex.functions; type="spec", \
io.reactivex.internal.disposables; type="spec", \
io.reactivex.internal.functions; type="spec", \
io.reactivex.internal.fuseable; type="spec", \
io.reactivex.internal.observers; type="spec", \
io.reactivex.internal.operators.completable; type="spec", \
io.reactivex.internal.operators.flowable; type="spec", \
io.reactivex.internal.operators.maybe; type="spec", \
io.reactivex.internal.operators.mixed; type="spec", \
io.reactivex.internal.operators.observable; type="spec", \
io.reactivex.internal.operators.parallel; type="spec", \
io.reactivex.internal.operators.single; type="spec", \
io.reactivex.internal.queue; type="spec", \
io.reactivex.internal.schedulers; type="spec", \
io.reactivex.internal.subscribers; type="spec", \
io.reactivex.internal.subscriptions; type="spec", \
io.reactivex.internal.util; type="spec", \
io.reactivex.observables; type="spec", \
io.reactivex.observers; type="spec", \
io.reactivex.parallel; type="spec", \
io.reactivex.plugins; type="spec", \
io.reactivex.processors; type="spec", \
io.reactivex.subjects; type="spec", \
io.reactivex.subscribers; type="spec", \
io.reactivex.schedulers; type="spec", \
org.reactivestreams; type="spec"
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrsClientRx-2.1
Subsystem-Name: Java RESTful Services Client Rx 2.1
-features=com.ibm.websphere.appserver.jaxrsClient-2.1, \
 com.ibm.websphere.appserver.javaeeCompatible-8.0, \
 com.ibm.websphere.appserver.internal.jaxrs-2.1
-bundles=com.ibm.ws.org.apache.cxf.cxf.rt.rs.extension.reactivestreams.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.rs.extension.rx2.3.2, \
 com.ibm.ws.org.reactivestreams.1.0, \
 com.ibm.ws.io.reactivex.2.1
kind=ga
edition=core
