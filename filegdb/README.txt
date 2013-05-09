To build the dll's or so's in this directory you will need to download
the filegdbapi c++ package from ESRI. This does not cost, but because it
is not redistributable it cannot be checked in with the gdb library.

Note that (at least) the Windows 64bit FileGDB debug library from ESRI is currently suspect
as we've found two serious heap corruption bugs that we've reported to ESRI as of 5/9/13.
