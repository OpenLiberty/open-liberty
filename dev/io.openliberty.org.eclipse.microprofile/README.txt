This project exists so as not to have so many MicroProfile API projects that
relatively only have a BND file in them and nothing else of consequence except
the eclipse config files for loading the projects into eclipse.

When adding new org.eclipse.microprofile API bundles to OpenLiberty, the name
of the bnd file should be the suffix to the bundle name that is starting with
io.openliberty.org.eclipse.microprofile.  If the binaries are not available in 
maven yet and you want to get started and need to include the source in the 
OpenLiberty repository you should NOT use this component.  You should make a new
io.openliberty.org.eclipse.microprofile.<spec name>.<version> project with the
source in it.  When the binary is available in maven that project should be
deleted and this project be used to include the new API bundle.