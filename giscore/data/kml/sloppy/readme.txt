Examples of KML that are edges cases (most do not conform to the KML XML Schema) but
GIScore KML parser has been adapted to be more lax and accept all/most of the
features in these examples as if they were valid KML. Treats KML as does Google Earth.

Valid KML documents wrt XML Schema
 FolderSharedStyle.kml
 badCoord.kml
 badTime.kml
 badform2.kml

Well-formed XML documents (fail schema validation)
 bluedevil20080812-short2.kml
 n.kml
 pred.kml
