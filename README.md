## Info on Giscore ##

GIScore provides the capability to perform streaming input and output of data from different 
file formats with an emphasis on GIS file formats such as ESRI Shapefiles or 
geo-databases (GDB) and Google Earth KML/KMZ. As time went on it was extended to include other 
record oriented formats that included GIS information such as WKT, GeoRSS and 
GeoAtom. Additionally it has proven useful to support some non-GIS formats such as Dbf and CSV.

More information on GIScore can be found in the project Wiki pages.

## Building ##

Copy the file local.example.properties to local.properties and (if required) set the 
proxy properties to appropriate values for your local environment. Setup an ivy.properties
file in your home directory with an appropriate maven repository. We strongly encourage you
to have a local maven proxy server to cache artifacts if you don't already have one. 

The build is currently not finalized as this has just been pushed to GitHub and is still
basically set up for our internal publishing. It will be converted to publish to 
sonatype for central ASAP.  

You *can* use this build to publish to a local repository as is by modifying the ivy.settings
file. 

### Versions and Packages ###

Note that for the public release the version was bumped to 2.0.x and the java package
is now org.opensextant.giscore

## Other Information of note ##

LICENSE contains our use license

NOTICE contains a list of other works that we use and their copyrights and license references.

glpl-v3.html contains the Lesser GNU Public License version 3
cpl-v10.html contains the Common Public License v1.0

### NB ###

For those who are unaware, this file utilizes a format called markdown. Details at http://daringfireball.net/projects/markdown/syntax