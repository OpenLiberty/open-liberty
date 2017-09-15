Rules of this project:

#1 As this bundle is included in security-1.0, it must ONLY depend on packages
   that are defined in the kernel, logging and security bundles. This is
   REQUIRED to ensure that security-1.0 can start without any other dependencies.  

#2 This project currently handles MBean authorization and management roles.
   CAREFULLY consider the impact of adding new responsibilities to this project.
