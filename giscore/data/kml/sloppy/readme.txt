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
	cvc-datatype-valid.1.2.1: invalid value for 'NCName' <Style id="***default+icon=http://maps.google.com/mapfiles/kml/shapes/poi.png***">
	<altitudeMode>***clampToGround***</altitudeMode>

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

