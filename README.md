## Info on Giscore ##

GIScore provides the capability to perform streaming input and output of data from different 
file formats with an emphasis on GIS file formats such as ESRI Shapefiles or 
geo-databases (GDB) and Google Earth KML/KMZ. As time went on it was extended to include other 
record oriented formats that included GIS information such as WKT, GeoRSS and 
GeoAtom. Additionally it has proven useful to support some non-GIS formats such as Dbf and CSV.

More information on GIScore can be found in the project Wiki pages.

## Building ##

The build uses gradle, which can be downloaded from www.gradle.org. Use properties 
in your gradle.properties file to control the gradle build. gradle.properties is found at
$HOME/.gradle/gradle.properties.

Note FileGDB support is implemented using a thin JNI layer on top of an ESRI C++ File Geodatabase API,
which if FileGDB is desired then the ESRI library must be downloaded separately.
Details can be found here [FileGDB-Dependencies].

### Configuring a proxy server ###

`
systemProp.http.proxyHost=proxy.example.org
systemProp.http.proxyPort=80
systemProp.http.proxyUser=
systemProp.http.proxyPassword=
systemProp.http.nonProxyHosts=*.example.org|localhost
systemProp.https.proxyHost=proxy.example.org
systemProp.https.proxyPort=80
systemProp.https.proxyUser=
systemProp.https.proxyPassword=
systemProp.https.nonProxyHosts=*.example.org|localhost
`

### Versions and Packages ###

Note that for the public release the version was bumped to 2.0.0 and the java package
is now org.opensextant.giscore

The current public release is 2.0.2

## Other Information of note ##

LICENSE contains our use license

NOTICE contains a list of other works that we use and their copyrights and license references.

glpl-v3.html contains the Lesser GNU Public License version 3

cpl-v10.html contains the Common Public License v1.0

### NB ###

For those who are unaware, this file utilizes a format called markdown. Details at http://daringfireball.net/projects/markdown/syntax
