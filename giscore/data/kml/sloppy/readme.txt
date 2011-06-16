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
 police.kml
	ERROR: cvc-datatype-valid.1.2.1: invalid value for 'NCName' <Style id="***default+icon=http://maps.google.com/mapfiles/kml/shapes/poi.png***">
	<altitudeMode>***clampToGround***</altitudeMode>
 udop-bigpull.kmz
	This demonstrates duplicate ids for styles in given KML document.
	ERROR: cvc-attribute.3: The value 'normalState' of attribute 'id' on element 'Style' is not valid with respect to its type, 'ID'.
	 <Style id="***normalState***">
	Also reuses same the ids in separate NetworkLinked KML documents which is legal but needs to be handled by software applying
	styles to features.
	Note the duplicates ids are in the networklinked KML not the root KML file (doc.kml).

Well-formed XML documents (passes schema validation)
 badTrack.kml
    Well-formed KML and valid with respect to KML XML schema but with gx:Track
    coord-when mismatch in addition to gx:SimpleArrayData with an incorrect length 
    with respect to number of coord/when elements.
 badPolygon.kml
    Well-formed KML and valid with respect to KML XML schema but has following errors:
    * Inner ring not contained within outer ring
    * Inner rings in Polygon must not overlap with each other
    * Polygon inner ring points self-intersect.

