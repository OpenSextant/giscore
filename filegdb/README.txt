To build the dll's or so's in this directory you will need to download
the filegdbapi c++ package from ESRI. This does not cost, but because it
is not redistributable it cannot be checked in with the gdb library.

When debugging the dll/so libraries from Java there are a couple of helpers
in place. One is that when you are running in debug mode the FileGDBLibraryLoader
is smart enough to notice and will load the dll/so from the directory under filegdb.

A second mechanism is defining the environment property FILEGDB to have the value 
DEBUG or RELEASE. This will cause the respective library to get loaded for the appropriate
OS that you're running under and avoid the need to build and place the giscore jar on 
the classpath for each test run.

Please note that on the Windows platform the ESRI library is dependent on the 
platform redistributable library. We statically link, but ESRI does not. You therefore
must grab the redistributable library to use the built dynamic libraries.
