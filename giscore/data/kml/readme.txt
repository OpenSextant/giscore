Sample KML files originally harvested at kml-samples.googlecode.com
URL: http://kml-samples.googlecode.com/svn/trunk/interactive/index.html
These KML files have been modified to validate against appropriate KML schema.

Other examples have been added to this collection to cover nearly all possible
features of KML 2.2 as a validation test suite including those from older versions
of KML (KML 2.0, KML 2.1) with deleted and deprecated features.

e.g.
     http://kml-samples.googlecode.com/svn/trunk/kml/Placemark/placemark.kml
     http://kml-samples.googlecode.com/svn/trunk/kml/kmz/simple/big.kmz
     http://kml-samples.googlecode.com/svn/trunk/kml/kmz/simple/mimetype.kmz
     http://kml-samples.googlecode.com/svn/trunk/kml/NetworkLink/visibility.kml

See also http://kml-samples.googlecode.com/svn/trunk/morekml/

----------------------------------------------------------------------------------------------------

Summary of tags and properties used in KML collection

	Angle radians is too big
	Bad poly found, no outer ring
	Camera altitudeMode cannot be clampToGround [ATC 54.2]
	Container end date is later than that of its ancestors
	Document must explicitly reference a shared style
	Feature inherits container time
	Feature uses inline Style
	Feature uses inline StyleMap
	Feature uses merged shared/inline Style
	Feature uses shared Style
	Geometry spans -180/+180 longitude line
	GroundOverlay spans -180/+180 longitude line
	Ignoring invalid altitudeMode value: clampedToGround
	Inner ring not contained within outer ring
	Inner rings in Polygon must not overlap with each other
	Invalid LookAt values
	Invalid time range: start > end
	Latitude value exceeds pole value
	Line clipped at DateLine
	Line has duplicate consecutive points
	LinearRing cannot self-intersect
	LinearRing must start and end with the same point
	Nested MultiGeometries
	NetworkLink missing Link
	NetworkLink uses inline Style
	Out of order elements
	Outer ring has duplicate consecutive points
	Overlay missing icon
	Region has invalid LatLonAltBox [ATC 8]
	Shared styles in Folder not allowed [ATC 7]
	Suspicious Schema name characters
	Suspicious Style id characters
	Suspicious StyleMap highlight URL characters
	Suspicious StyleMap id characters
	Suspicious StyleMap normal URL characters
	Suspicious styleUrl characters
	comma found instead of whitespace between tuples
	gx:SimpleArrayData has incorrect length
	gx:Track coord-when mismatch
	ignore invalid character in coordinate string
	ignore invalid string in coordinate

	BalloonStyle		MultiPoint
	Camera			NetworkLink
	Document		NetworkLinkControl
	ExtendedData		PhotoOverlay
	Folder			Placemark
	GroundOverlay		Point
	IconStyle		PolyStyle
	LabelStyle		Polygon
	LatLonBox		Region
	Line			Schema
	LineStyle		ScreenOverlay
        LinearRing		Style
        ListStyle		StyleMap
        LookAt			TimeSpan
        Model			TimeStamp
        MultiGeometry

        atom:author		gx:SimpleArrayData
        atom:link		gx:TimeSpan
        gx:altitudeMode		gx:TimeStamp
        gx:balloonVisibility	gx:Tour
        gx:LatLonQuad		gx:Track
        gx:MultiTrack		xal:AddressDetails

----------------------------------------------------------------------------------------------------

data\kml\atom\sevenWonders-atom-link.kml

	Feature uses shared Style
	Shared styles in Folder not allowed [ATC 7]
	--
	IconStyle            1
	Placemark            7
	Point                7
	Style                1
	atom:link            1
	# features=7

data\kml\Baghdad.kml

	LinearRing           1
	Placemark            1
	# features=1

data\kml\balloon\balloonstyle-color-inline.kml

	Feature uses inline Style
	BalloonStyle         1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\balloon\balloonstyle-color-shared.kml

	Feature uses shared Style
	--
	BalloonStyle         1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\balloon\default.kml

	Placemark            1
	Point                1
	# features=1

data\kml\balloon\iframe.kml

	Placemark            1
	Point                1
	# features=1

data\kml\balloon\noballoon.kml

	Placemark            1
	Point                1
	# features=1

data\kml\BalloonStyle\displayMode.kml

	Feature uses shared Style
	--
	BalloonStyle         3
	Document             1
	Folder               2
	Placemark            6
	Point                6
	Style                3
	# features=6

data\kml\BalloonStyle\simpleBalloonStyles.kml

	Feature uses shared Style
	--
	BalloonStyle         5
	Placemark            5
	Point                5
	Style                5
	# features=5

data\kml\Camera\golden-gate.kml

	Camera altitudeMode cannot be clampToGround [ATC 54.2]
	--
	Camera               11
	Placemark            10
	# features=10

data\kml\ExtendedData\data-ext-ns.kml
	DEBUG [main] (KmlInputStream.java:548) - skip {http://campsites.com}number
	DEBUG [main] (KmlInputStream.java:548) - skip {http://campsites.com}parkingSpaces
	DEBUG [main] (KmlInputStream.java:548) - skip {http://campsites.com}tentSites

	Placemark            1
	Point                1
	# features=1

data\kml\ExtendedData\data-golf.kml

	Feature uses inline Style
	--
	BalloonStyle         2
	ExtendedData         2
	Placemark            2
	Point                2
	Style                2
	# features=2

data\kml\ExtendedData\mti-data.kmz

	Feature uses shared Style
	Shared styles in Folder not allowed [ATC 7]
	--
	Document             1
	ExtendedData         33
	Folder               3
	IconStyle            2
	LineStyle            1
	LinearRing           2
	Placemark            33
	Point                31
	Style                3
	TimeStamp            33
	# features=33

data\kml\ExtendedData\mti-schema-data.kmz

	Feature uses shared Style
	Shared styles in Folder not allowed [ATC 7]
	--
	BalloonStyle         1
	Document             1
	ExtendedData         33
	Folder               3
	IconStyle            2
	LineStyle            1
	LinearRing           2
	Placemark            33
	Point                31
	Schema               1
	Style                3
	TimeStamp            33
	# features=33

data\kml\feature-anchor\eat-at-google.kml

	LookAt               2
	Placemark            2
	Point                2
	# features=2

data\kml\feature-anchor\feature-anchor-amp.kml

	Placemark            7
	Point                7
	# features=7

data\kml\feature-anchor\feature-anchor-pipe.kml

	Placemark            7
	Point                7
	# features=7

data\kml\feature-anchor\feature-anchor-rel-070531.kml

	Placemark            6
	Point                6
	# features=6

data\kml\feature-anchor\feature-anchor-semi.kmz

	Placemark            7
	Point                7
	# features=7

data\kml\FeatureType\life-of-a-feature-view-data.kml
	DEBUG [main] (KmlReader.java:166) - NetworkLink href is empty or missing

	NetworkLink missing Link
	Overlay missing icon
	--
	BalloonStyle         1
	Document             2
	ExtendedData         7
	Folder               1
	GroundOverlay        1
	ListStyle            1
	LookAt               7
	NetworkLink          1
	PhotoOverlay         1
	Placemark            1
	ScreenOverlay        1
	Style                1
	# features=5

data\kml\GroundOverlay\crossIDL.kml
	DEBUG [main] (KmlInputStream.java:1862) - Normalized GroundOverlay west value

	GroundOverlay spans -180/+180 longitude line
	Overlay missing icon
	--
	GroundOverlay        2
	LatLonBox            2
	# features=2

data\kml\GroundOverlay\empty.kml

	GroundOverlay        1
	LookAt               1
	# features=1

data\kml\GroundOverlay\etna.kml

	GroundOverlay        1
	LatLonBox            1
	LookAt               1
	# features=1

data\kml\GroundOverlay\etna.kmz

	GroundOverlay        1
	LatLonBox            1
	LookAt               1
	# features=1

data\kml\gx\all-gx.kml
	DEBUG [main] (KmlInputStream.java:1758) - Handle tag data gx:altitudeMode
	DEBUG [main] (KmlInputStream.java:1758) - Handle tag data gx:altitudeMode

	Camera altitudeMode cannot be clampToGround [ATC 54.2]
	Feature uses inline Style
	Invalid LookAt values
	Overlay missing icon
	--
	Camera               1
	Document             3
	GroundOverlay        1
	IconStyle            2
	Line                 1
	LookAt               3
	Placemark            7
	Point                1
	Schema               1
	Style                2
	gx:LatLonQuad        1
	gx:MultiTrack        1
	gx:TimeSpan          2
	gx:TimeStamp         1
	gx:Tour              11
	gx:Track             1
	gx:altitudeMode      1
	gx:balloonVisibility 1
	# features=8

data\kml\gx\altitudemode_reference.kml
	DEBUG [main] (KmlInputStream.java:1698) - Handle gx:altitudeMode

        Line                 1
        LookAt               1
        Placemark            1
	gx:altitudeMode      1
        # features=1

data\kml\gx\sample_tour.kml

        IconStyle            1
        LabelStyle           1
        Placemark            1
        Point                1
        Style                1
        gx:Tour              1
        # features=1

data\kml\gx\trackData.kml

	Feature uses shared Style
	--
	IconStyle            6
	LabelStyle           1
	LineStyle            3
	LookAt               1
	Placemark            1
	Schema               1
	Style                7
	StyleMap             3
	gx:SimpleArrayData   1
	gx:TimeSpan          2
	gx:Track             1
	# features=1

data\kml\ItemIcon\kitchensink.kmz

	Feature uses shared Style
	--
	Placemark            12
	Point                12
	Style                14
	# features=12

data\kml\javascript\basic_javascript.kml

	Placemark            1
	Point                1
	# features=1

data\kml\javascript\resize_balloon.kml

	Placemark            1
	Point                1
	# features=1
data\kml\kmz\balloon\balloon-image-abs.kml

	Placemark            1
	Point                1
	# features=1

data\kml\kmz\balloon\balloon-image-abs.kmz

	Placemark            1
	Point                1
	# features=1

data\kml\kmz\balloon\balloon-image-rel.kml

	Placemark            1
	Point                1
	# features=1

data\kml\kmz\balloon\balloon-image-rel.kmz

	Placemark            1
	Point                1
	# features=1

data\kml\kmz\dir\balloon-image-abs.kml

	Placemark            1
	# features=1

data\kml\kmz\dir\balloon-image-abs.kmz

	Placemark            1
	# features=1

data\kml\kmz\dir\balloon-image-rel.kml

	Placemark            1
	# features=1

data\kml\kmz\dir\balloon-image-rel.kmz

	Placemark            1
	# features=1

data\kml\kmz\dir\content.kmz

	url=kml/hi.kml
	NetworkLink          1
	Placemark            2
	Point                1
	# features=3

data\kml\kmz\dir\UrlLink.kmz

	url=kml/hi.kml
	NetworkLink          1
	Placemark            1
	Point                1
	# features=2

data\kml\kmz\iconStyle\styled_placemark.kmz

	Feature uses inline Style
	BalloonStyle         1
	IconStyle            1
	LabelStyle           1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\kmz\networklink\hier.kmz
	DEBUG [main] (KmlReader.java:262) - Parse networkLink: file:/C:/projects/giscore/data/kml/kmz/networklink/hier.kmz/within.kml
	DEBUG [main] (KmlReader.java:262) - Parse networkLink: file:/C:/projects/giscore/data/kml/kmz/networklink/hier.kmz/outside.kml

        url=outside.kml
        url=within.kml
        NetworkLink          2
        Placemark            2
	# features=4

data\kml\kmz\networklink\outside.kml

        Placemark            1
        # features=1

data\kml\kmz\overlay\ground-overlay-abs.kml

	GroundOverlay        1
	LatLonBox            1
	# features=1

data\kml\kmz\overlay\ground-overlay-abs.kmz

	GroundOverlay        1
	LatLonBox            1
	# features=1

data\kml\kmz\overlay\ground-overlay.kml

	GroundOverlay        1
	LatLonBox            1
	# features=1

data\kml\kmz\overlay\ground-overlay.kmz

	GroundOverlay        1
	LatLonBox            1
	# features=1

data\kml\kmz\simple\big.kmz

	Placemark            1
	Point                1
	# features=1

data\kml\kmz\simple\spaceInLink1.kmz

	url=my%20office.kml
	NetworkLink          1
	Placemark            1
	Point                1
	# features=2

data\kml\kmz\simple\spaceInLink2.kmz

	url=my%20office.kml
	NetworkLink          1
	Placemark            1
	Point                1
	# features=2

data\kml\LinkType\life-of-a-link-basic.kml

	url=file:/C:/projects/giscore/data/kml/kmz/simple/big.kmz
	GroundOverlay        1
	LatLonBox            1
	Model                1
	NetworkLink          1
	Placemark            2
	Point                1
	# features=4

data\kml\ListStyle\check-hide-children.kml

	Document             1
	Folder               2
	ListStyle            1
	Placemark            4
	Style                1
	# features=4

data\kml\ListStyle\check-off-only.kml

	ListStyle            1
	Placemark            4
	Point                4
	Style                1
	# features=4

data\kml\ListStyle\check.kml

	Document             1
	Folder               2
	ListStyle            1
	Placemark            4
	Point                4
	Style                1
	# features=4

data\kml\ListStyle\item-icon-hotspot.kml

	IconStyle            1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\ListStyle\more-stuff.kml

	Placemark            2
	Point                2
	# features=2

data\kml\ListStyle\other-stuff.kml

	Placemark            2
	Point                2
	# features=2

data\kml\ListStyle\radio-folder-vis.kml

	Document             1
	Folder               2
	ListStyle            1
	Placemark            4
	Point                4
	Style                1
	# features=4

data\kml\ListStyle\radio-folder.kml

	Document             1
	Folder               2
	ListStyle            1
	Placemark            4
	Point                4
	Style                1
	# features=4

data\kml\ListStyle\radio-hide-children.kml

	url=file:/C:/projects/giscore/data/kml/ListStyle/more-stuff.kml
	url=file:/C:/projects/giscore/data/kml/ListStyle/other-stuff.kml
	Document             1
	Folder               2
	ListStyle            2
	NetworkLink          2
	Placemark            5
	Point                4
	Style                2
	# features=7

data\kml\listview\nosnippet.kml

	Placemark            2
	Point                2
	# features=2

data\kml\Metadata\metadata-data.kml

	BalloonStyle         1
	Placemark            3
	Point                3
	Style                1
	# features=3

data\kml\Metadata\metadata-schemadata.kml

	BalloonStyle         1
	Placemark            3
	Point                3
	Schema               1
	Style                1
	# features=3

data\kml\Metadata\metadata-yourstuff.kml

	Placemark            1
	Point                1
	# features=1

data\kml\Model\House.kmz

	Feature uses inline Style
	Folder               2
	LookAt               2
	Model                1
	Placemark            2
	Style                1
	# features=2

data\kml\Model\MackyBldg.kmz

	LookAt               1
	Model                1
	Placemark            1
	# features=1

data\kml\Model\SharedTextures.kmz

	LookAt               3
	Model                2
	Placemark            2
	# features=2

data\kml\MultiGeometry\emptyGeom.kml
	DEBUG [main] (KmlInputStream.java:1823) - No valid geometries in MultiGeometry
	DEBUG [main] (KmlInputStream.java:1828) - Convert MultiGeometry to single geometry

	Placemark            2
	Point                1
	# features=2

data\kml\MultiGeometry\multi-linestrings.kml

	Line                 10
	LineStyle            1
	MultiGeometry        1
	Placemark            1
	Style                1
	# features=1

data\kml\MultiGeometry\multi-rollover.kml

	LabelStyle           1
	LineStyle            2
	MultiGeometry        1
	Placemark            1
	Point                1
	PolyStyle            2
	Polygon              1
	Style                2
	StyleMap             1
	# features=1

data\kml\MultiGeometry\MultiGeomLinearRings-Pentagon.kml

	LinearRing           2
	MultiGeometry        1
	Placemark            1
	# features=1

data\kml\MultiGeometry\MultiGeomMixed-Pentagon.kml

	LinearRing           1
	MultiGeometry        1
	Placemark            1
	Polygon              1
	# features=1

data\kml\MultiGeometry\NestedMultiGeoms.kml

	Nested MultiGeometries
	--
	Line                 2
	MultiGeometry        1
	Placemark            1
	Point                2
	# features=1

data\kml\MultiGeometry\polygon-point.kml
	DEBUG [main] (LinearRing.java:201) - LinearRing self-intersects at i=0 j=59

	Feature uses inline Style
	LinearRing can not self-intersect
	Outer ring has duplicate consecutive points
	--
	LineStyle            1
	MultiGeometry        1
	Placemark            1
	Point                1
	PolyStyle            1
	Polygon              1
	Style                1
	# features=1

data\kml\MultiGeometry\testLayers.kml

	Document             1
	Folder               8
	Line                 3
	LinearRing           3
	MultiGeometry        6
	MultiPoint           1
	Placemark            8
	Point                7
	Polygon              3
	# features=8

data\kml\NetworkLink\aliasing\2xnl.kml

	url=file:/C:/projects/giscore/data/kml/NetworkLink/aliasing/a.kml
	NetworkLink          2
	Placemark            1
	Point                1
	# features=3

data\kml\NetworkLink\aliasing\a.kml

	Placemark            1
	Point                1
	# features=1

data\kml\NetworkLink\aliasing\nl+desc.kml

	url=file:/C:/projects/giscore/data/kml/NetworkLink/aliasing/a.kml
	NetworkLink          1
	Placemark            3
	Point                1
	# features=4

data\kml\NetworkLink\flyToView\d-lookat-p-lookat.kml

	LookAt               2
	Placemark            1
	# features=1

data\kml\NetworkLink\flyToView\d-p-lookat.kml

	LookAt               1
	Placemark            2
	Point                1
	# features=2

data\kml\NetworkLink\flyToView\flyToView.kml

	url=file:/C:/projects/giscore/data/kml/NetworkLink/flyToView/d-lookat-p-lookat.kml
	url=file:/C:/projects/giscore/data/kml/NetworkLink/flyToView/d-p-lookat.kml
	url=file:/C:/projects/giscore/data/kml/NetworkLink/flyToView/nlc-lookat-p-lookat.kml
	url=file:/C:/projects/giscore/data/kml/NetworkLink/flyToView/nlc-lookat.kml
	Document             3
	LookAt               5
	NetworkLink          4
	NetworkLinkControl   2
	Placemark            4
	Point                1
	# features=8

data\kml\NetworkLink\flyToView\nlc-lookat-p-lookat.kml

	LookAt               1
	NetworkLinkControl   1
	Placemark            1
	# features=1

data\kml\NetworkLink\flyToView\nlc-lookat.kml

	NetworkLinkControl   1

data\kml\NetworkLink\multiLevelNetworkLinks.kmz

	url=networkLink2.kml
	url=placemark.kml
	NetworkLink          2
	Placemark            1
	Point                1
	# features=3

data\kml\NetworkLink\multiLevelNetworkLinks2.kmz

	Feature uses inline Style
	url=placemark.kml
	url=sub1.kml
	Document             2
	IconStyle            3
	NetworkLink          2
	Placemark            3
	Point                3
	Style                3
	# features=5

data\kml\NetworkLink\nlc.kml

	NetworkLinkControl   1

data\kml\NetworkLink\placemark.kml

	Placemark            1
	Point                1
	# features=1

data\kml\NetworkLink\visibility.kml

	url=file:/C:/projects/giscore/data/kml/NetworkLink/placemark.kml
	NetworkLink          2
	Placemark            1
	Point                1
	# features=3

data\kml\Placemark\AllElements.kml

	Region has invalid LatLonAltBox [ATC 8]
	--
	LookAt               1
	Placemark            1
	Point                1
	Region               1
	TimeSpan             1
	atom:author          1
	atom:link            1
	xal:AddressDetails   1
	# features=1

data\kml\Placemark\clippedAtDateLine.kml

	Feature uses inline Style
	Geometry spans -180/+180 longitude line
	Line clipped at DateLine
	--
	Line                 2
	LineStyle            2
	LookAt               1
	Placemark            3
	Point                1
	Style                2
	# features=3

data\kml\Placemark\LinearRing\polygon-lr-all-modes.kml

	Placemark            1
	Polygon              1
	# features=1

data\kml\Placemark\LinearRing\polygon-lr-no-tessellate.kml

	Placemark            1
	Polygon              1
	# features=1

data\kml\Placemark\LinearRing\polygon-lr-tessellate.kml

	Placemark            1
	Polygon              1
	# features=1

data\kml\Placemark\LinearRing\polygon-tessellate-lr.kml

	Placemark            1
	Polygon              1
	# features=2

data\kml\Placemark\LineString\absolute-extruded.kml

	Line                 1
	LineStyle            1
	LookAt               1
	Placemark            1
	PolyStyle            1
	Style                1
	# features=1

data\kml\Placemark\LineString\extruded.kml

	Line                 1
	LineStyle            1
	LookAt               1
	Placemark            1
	PolyStyle            1
	Style                1
	# features=1

data\kml\Placemark\LineString\straight.kml

	Line                 1
	LookAt               1
	Placemark            1
	# features=1

data\kml\Placemark\LineString\styled.kml

	Line                 1
	LineStyle            1
	LookAt               1
	Placemark            1
	PolyStyle            1
	Style                1
	# features=1

data\kml\Placemark\LineString\tessellate.kml

	Line                 1
	LookAt               1
	Placemark            1
	# features=1

data\kml\Placemark\longName.kml

	Placemark            1
	Point                1
	# features=1

data\kml\Placemark\mixedDimsLines.kml
 INFO [main] (Line.java:109) - Line points have mixed dimensionality: downgrading line to 2d

	Line                 1
	Placemark            4
	Point                3
	# features=4

data\kml\Placemark\placemark.kml

	Placemark            1
	Point                1
	# features=1

data\kml\Placemark\placemarks.kml

	IconStyle            3
	Placemark            4
	Point                4
	Style                3
	# features=4

data\kml\Placemark\Point\altitude.kml

	IconStyle            1
	LookAt               1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\Placemark\Point\extruded.kml

	IconStyle            1
	LineStyle            1
	LookAt               1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\Placemark\simple_placemark.kml

	Placemark            1
	Point                1
	# features=1

data\kml\Placemark\styled_placemark.kml

	Feature uses inline Style
	--
	BalloonStyle         1
	IconStyle            1
	LabelStyle           1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\Polygon\InnerBoundaryPoly-Pentagon.kml

	Placemark            1
	Polygon              1
	# features=1

data\kml\Polygon\polyInnerBoundaries.kml

	Placemark            1
	Polygon              1
	# features=1

data\kml\Polygon\treasureIsland.kml

	Feature uses inline Style
	--
	LineStyle            1
	Placemark            1
	PolyStyle            1
	Polygon              1
	Style                1
	# features=1

data\kml\Region\GroundOverlay\usa-ca-sf.kmz

	GroundOverlay        3
	LatLonBox            3
	Region               3
	# features=3

data\kml\Region\minlodpixels.kml

	Line                 1
	Placemark            1
	Region               8
	ScreenOverlay        8
	# features=9

data\kml\Region\minlodpixels.kmz

	Line                 1
	Placemark            1
	Region               8
	ScreenOverlay        8
	# features=9

data\kml\Region\polygon-fade.kml

	Feature uses inline Style
	LinearRing can not self-intersect
	Outer ring has duplicate consecutive points
	--
	Placemark            1
	PolyStyle            1
	Polygon              1
	Region               1
	Style                1
	# features=1

data\kml\Region\polygon-min-max.kml

	Feature uses inline Style
	LinearRing cannot self-intersect
	Outer ring has duplicate consecutive points
	--
	Placemark            1
	PolyStyle            1
	Polygon              1
	Region               1
	Style                1
	# features=1

data\kml\Region\polygon-simple.kml
	DEBUG [main] (LinearRing.java:201) - LinearRing self-intersects at i=0 j=53

	Feature uses inline Style
	LinearRing cannot self-intersect
	Outer ring has duplicate consecutive points
	--
	Placemark            1
	PolyStyle            1
	Polygon              1
	Region               1
	Style                1
	# features=1

data\kml\Region\polygon-swap-fade.kml

	Feature uses inline Style
	LinearRing cannot self-intersect
	Outer ring has duplicate consecutive points
	--
	LineStyle            2
	Placemark            2
	PolyStyle            2
	Polygon              2
	Region               2
	Style                2
	# features=2

data\kml\Region\polygon-swap-pop.kml

	Feature uses inline Style
	LinearRing cannot self-intersect
	Outer ring has duplicate consecutive points
	--
	LineStyle            2
	Placemark            2
	PolyStyle            2
	Polygon              2
	Region               2
	Style                2
	# features=2

data\kml\Region\screen-rulers.kml

	Overlay missing icon
	--
	Document             1
	Folder               2
	ListStyle            1
	Placemark            2
	ScreenOverlay        8
	Style                1
	# features=10

data\kml\Region\ScreenOverlay\continents.kmz

	Document             1
	Folder               2
	Line                 4
	Placemark            4
	Region               8
	ScreenOverlay        4
	# features=8

data\kml\Region\simple-lod-demo.kml

	Line                 1
	Placemark            5
	Point                4
	Region               4
	# features=5

data\kml\Schema\AllTypes.kml

	ExtendedData         1
	Placemark            1
	Point                1
	Schema               1
	# features=1

data\kml\Schema\MixedTypes.kml

	Suspicious Schema name characters
	--
	ExtendedData         1
	Placemark            1
	Point                1
	Schema               2
	# features=1

data\kml\Schema\schemadata-trailhead.kml

	BalloonStyle         1
	ExtendedData         2
	LookAt               1
	Placemark            2
	Point                2
	Schema               1
	Style                1
	# features=2

data\kml\Schema\SchemaOldStyle.kml

	Placemark            3
	Point                3
	Schema               1
	# features=3

data\kml\Schema\SchemaOldStyle2.kml

	Placemark            3
	Point                3
	Schema               1
	# features=3

data\kml\Schema\sigint.kmz

	BalloonStyle         1
	Document             1
	ExtendedData         42
	Folder               4
	IconStyle            8
	LineStyle            8
	Placemark            42
	Point                42
	Schema               1
	Style                9
	TimeSpan             42
	# features=42

data\kml\ScreenOverlay\centered-icon.kml

	ScreenOverlay        1
	# features=1

data\kml\sky\leo.kml

	LookAt               1
	Placemark            1
	Point                1
	# features=1

data\kml\sloppy\badAtom.kml
 WARN [main] (KmlInputStream.java:1315) - Skip element: Document
DEBUG [main] (KmlInputStream.java:1316) - 
javax.xml.stream.XMLStreamException: ParseError at [row,col]:[8,115]
Message: http://www.w3.org/TR/1999/REC-xml-names-19990114#ElementPrefixUnbound?atom&atom:link
	at com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl.next(Unknown Source)
	at com.sun.xml.internal.stream.XMLEventReaderImpl.peek(Unknown Source)
	at org.mitre.giscore.input.kml.KmlInputStream.handleContainer(KmlInputStream.java:367)
	at org.mitre.giscore.input.kml.KmlInputStream.handleStartElement(KmlInputStream.java:1284)
	at org.mitre.giscore.input.kml.KmlInputStream.read(KmlInputStream.java:307)
	at org.mitre.giscore.input.kml.KmlReader.read(KmlReader.java:209)
	at org.mitre.giscore.input.kml.KmlReader.read(KmlReader.java:205)
	at org.mitre.giscore.utils.KmlMetaDump.processKmlSource(KmlMetaDump.java:294)
	at org.mitre.giscore.utils.KmlMetaDump.checkSource(KmlMetaDump.java:191)
	at org.mitre.giscore.utils.KmlMetaDump.main(KmlMetaDump.java:1312)
	*** java.io.IOException: javax.xml.stream.XMLStreamException: ParseError at [row,col]:[9,5]
Message: Element type "null" must be followed by either attribute specifications, ">" or "/>".

	Skip element: Document

data\kml\sloppy\badCoord.kml
 WARN [main] (KmlInputStream.java:2160) - comma found instead of whitespace between tuples before 4.0
 WARN [main] (KmlInputStream.java:2151) - ignore invalid string in coordinate: "ddd"
 WARN [main] (KmlInputStream.java:2257) - ignore invalid character in coordinate string: (+)
 WARN [main] (KmlInputStream.java:2257) - ignore invalid character in coordinate string: (+)
 WARN [main] (KmlInputStream.java:2257) - ignore invalid character in coordinate string: (+)
 WARN [main] (KmlInputStream.java:2151) - ignore invalid string in coordinate: "xxx"
 WARN [main] (KmlInputStream.java:2151) - ignore invalid string in coordinate: "xxx"
 WARN [main] (KmlInputStream.java:2151) - ignore invalid string in coordinate: "xxx"
 WARN [main] (KmlInputStream.java:2151) - ignore invalid string in coordinate: "yyy"
ERROR [main] (KmlInputStream.java:2224) - Invalid coordinate: 200.0
java.lang.IllegalArgumentException: Latitude value exceeds pole value
ERROR [main] (KmlInputStream.java:2039) - Invalid coordinate: 3000.0
java.lang.IllegalArgumentException: Angle 52.35987755982989 radians is too big
 WARN [main] (KmlInputStream.java:1966) - ignore invalid string in coordinate: "xx10"
 WARN [main] (KmlInputStream.java:1966) - ignore invalid string in coordinate: "xx"
 WARN [main] (KmlInputStream.java:1709) - line with single coordinate converted to point: Point at (4° 0' 0" E, 2° 0' 0" N)
 WARN [main] (KmlInputStream.java:1975) - comma found instead of whitespace between tuples before 4.0
 INFO [main] (Line.java:107) - Line points have mixed dimensionality: downgrading line to 2d
 WARN [main] (KmlInputStream.java:1975) - comma found instead of whitespace between tuples before 4.0
 WARN [main] (KmlInputStream.java:1717) - ring with single coordinate converted to point: Point at (5° 0' 0" E, 2° 30' 0" N)
 WARN [main] (KmlInputStream.java:1720) - ring with 2 coordinates converted to line: [Point at (5° 0' 0" E, 2° 30' 0" N), Point at (5° 30' 0" E, 2° 30' 0" N)]
 WARN [main] (KmlInputStream.java:1720) - ring with 3 coordinates converted to line: [Point at (77° 3' 24" W, 38° 52' 18" N), Point at (77° 3' 17" W, 38° 52' 15" N), Point at (77° 3' 21" W, 38° 52' 12" N)]
 WARN [main] (LinearRing.java:145) - LinearRing should start and end with the same point
 WARN [main] (KmlInputStream.java:1740) - polygon with single coordinate converted to point: Point at (5° 12' 0" E, 2° 0' 36" N)
 WARN [main] (KmlInputStream.java:1743) - polygon with 2 coordinates converted to line: [Point at (5° 0' 0" E, 2° 0' 0" N), Point at (5° 30' 0" E, 2° 0' 0" N)]
 WARN [main] (KmlInputStream.java:1743) - polygon with 3 coordinates converted to line: [Point at (77° 3' 24" W, 38° 52' 18" N), Point at (77° 3' 17" W, 38° 52' 15" N), Point at (77° 3' 21" W, 38° 52' 12" N)]
 WARN [main] (KmlInputStream.java:1508) - Failed geometry: Feature data=[name = polygon4, description = polygon with no outer boundary]
 java.lang.IllegalStateException: Bad poly found, no outer ring

	Bad poly found, no outer ring
	Feature uses inline Style
	Feature uses shared Style
	Latitude value exceeds pole value
	LinearRing must start and end with the same point
	comma found instead of whitespace between tuples
	ignore invalid character in coordinate string
	ignore invalid string in coordinate
	--
	Document             1
	Folder               4
	IconStyle            1
	LabelStyle           1
	Line                 9
	LineStyle            4
	LinearRing           1
	Placemark            32
	Point                19
	PolyStyle            3
	Style                5
	# features=32

data\kml\sloppy\badPolygon.kml

	Inner ring not contained within outer ring
	Inner rings in Polygon must not overlap with each other
	LinearRing cannot self-intersect
	--
	Placemark            3
	Polygon              3
	# features=3

data\kml\sloppy\badTrack.kml

	gx:SimpleArrayData has incorrect length
	gx:Track coord-when mismatch
	--
	Placemark            1
	gx:SimpleArrayData   1
	gx:Track             1
	# features=1

data\kml\sloppy\bluedevil20080812-short2.kml
	DEBUG [main] (KmlInputStream.java:1235) - Out of order element: Style
	DEBUG [main] (KmlInputStream.java:1235) - Out of order element: Style
	DEBUG [main] (KmlInputStream.java:1235) - Out of order element: Style
	WARN [main] (KmlInputStream.java:1255) - Skip unexpected element: name
	INFO [main] (AltitudeModeEnumType.java:40) - Ignoring invalid altitudeMode value: clampedToGround
	--
	Feature uses inline Style
	Ignoring invalid altitudeMode value: clampedToGround
	Line has duplicate consecutive points
	Out of order elements
	Overlay missing icon
	Shared styles in Folder not allowed [ATC 7]
	Skip unexpected element: name
	--
	Document             1
	Folder               2
	IconStyle            3
	LabelStyle           3
	Line                 1
	LineStyle            1
	Placemark            2
	Point                1
	ScreenOverlay        1
	Style                4
	TimeSpan             1
	TimeStamp            1
	# features=14

data\kml\sloppy\n.kml
	 WARN [main] (KmlInputStream.java:446) - Unable to parse description as text element: javax.xml.stream.XMLStreamException: ParseError at [row,col]:[24,49]
	Message: elementGetText() function expects text only elment but START_ELEMENT was encountered.
	 WARN [main] (KmlInputStream.java:446) - Unable to parse description as text element: javax.xml.stream.XMLStreamException: ParseError at [row,col]:[29,47]
	Message: elementGetText() function expects text only elment but START_ELEMENT was encountered.
	 WARN [main] (KmlInputStream.java:446) - Unable to parse description as text element: javax.xml.stream.XMLStreamException: ParseError at [row,col]:[34,47]
	Message: elementGetText() function expects text only elment but START_ELEMENT was encountered.
	 WARN [main] (KmlInputStream.java:446) - Unable to parse description as text element: javax.xml.stream.XMLStreamException: ParseError at [row,col]:[39,47]
	Message: elementGetText() function expects text only elment but START_ELEMENT was encountered.

	Placemark            4
	Point                4
	Region               1
	# features=4

data\kml\sloppy\outOrder.kml
DEBUG [main] (KmlInputStream.java:1292) - Out of order element: Style

	Feature uses shared Style
	--
	IconStyle            1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\sloppy\police.kml
 WARN [main] (KmlInputStream.java:1289) - Skip unexpected element: altitudeMode

	Feature uses shared Style
	Skip unexpected element: altitudeMode
	Suspicious Style id characters
	Suspicious StyleMap highlight URL characters
	Suspicious StyleMap id characters
	Suspicious StyleMap normal URL characters
	Suspicious styleUrl characters
	--
	IconStyle            2
	LabelStyle           2
	Placemark            1
	Point                1
	Style                2
	StyleMap             1
	# features=1

data\kml\sloppy\pred.kml
 WARN [main] (KmlInputStream.java:2160) - comma found instead of whitespace between tuples before -81.9980316162109

	Feature uses inline Style
	comma found instead of whitespace between tuples
	--
	BalloonStyle         1
	ExtendedData         1
	IconStyle            1
	Line                 1
	LineStyle            1
	Model                1
	Placemark            3
	Point                1
	Style                2
	# features=3

data\kml\Style\iconStyle.kmz

	Feature uses inline Style
	--
	IconStyle            3
	Placemark            3
	Point                3
	Style                3
	# features=3

data\kml\Style\inline-stylemap.kml

	Feature uses inline StyleMap
	--
	Placemark            1
	Point                1
	StyleMap             1
	# features=1

data\kml\Style\noicon.kml

	Feature uses shared Style
	--
	IconStyle            1
	LabelStyle           1
	LookAt               1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\Style\overrideStyles.kml

	Feature uses merged shared/inline Style
	Feature uses shared Style
	--
	IconStyle            2
	LabelStyle           1
	Placemark            2
	Point                2
	Style                2
	# features=2

data\kml\Style\SharedStyle.kml

	Feature uses shared Style
	--
	BalloonStyle         1
	IconStyle            1
	LabelStyle           1
	LineStyle            1
	Placemark            2
	Point                2
	Style                1
	# features=2

data\kml\Style\style-merging.kml

	Feature uses merged shared/inline Style
	--
	BalloonStyle         2
	IconStyle            1
	LabelStyle           1
	Line                 1
	LineStyle            3
	ListStyle            1
	Placemark            3
	Point                1
	PolyStyle            1
	Polygon              1
	Style                4
	# features=3

data\kml\Style\styledLineString.kml

	Feature uses shared Style
	--
	Line                 1
	LineStyle            1
	LookAt               1
	Placemark            1
	PolyStyle            1
	Style                1
	# features=1

data\kml\Style\styled_placemark.kml

	Feature uses inline Style
	--
	BalloonStyle         1
	IconStyle            1
	LabelStyle           1
	Placemark            1
	Point                1
	Style                1
	# features=1

data\kml\Style\styles.kml

	IconStyle            1
	ListStyle            1
	Style                2

data\kml\time\080708_dirtdevil_test1.kml

	IconStyle            1
	Placemark            12
	Point                12
	Style                1
	TimeStamp            12
	# features=12

data\kml\time\dates.kml

	Placemark            4
	Point                4
	TimeSpan             1
	TimeStamp            3
	# features=4

data\kml\time\multiNestedInherits.kml

	Container end date is later than that of its ancestors
	Feature uses inline Style
	--
	IconStyle            2
	Placemark            2
	Point                2
	Style                2
	TimeSpan             4
	# features=2

data\kml\time\multiNestedInheritsUnboundedSpans.kml

	Feature uses inline Style
	--
	IconStyle            2
	Placemark            2
	Point                2
	Style                2
	TimeSpan             4
	# features=2

data\kml\time\nestedInherits.kml

	Feature inherits container time
	Feature uses inline Style
	--
	Document             1
	Folder               2
	IconStyle            6
	Placemark            6
	Point                6
	Style                6
	TimeSpan             4
	# features=6

data\kml\time\time-inherit2.kml

	Feature inherits container time
	Feature uses shared Style
	--
	Document             1
	Folder               5
	IconStyle            3
	ListStyle            1
	Placemark            13
	Point                13
	Style                4
	TimeSpan             4
	TimeStamp            5
	# features=13

data\kml\time\time-inherits.kml

	Feature inherits container time
	GroundOverlay spans -180/+180 longitude line
	--
	Folder               4
	GroundOverlay        6
	LatLonBox            6
	TimeSpan             3
	# features=6

data\kml\time\time-span-overlay.kml

	GroundOverlay        12
	LatLonBox            12
	TimeSpan             12
	# features=12

data\kml\time\time-stamp-point.kmz

	Feature uses shared Style
	--
	IconStyle            3
	ListStyle            1
	Placemark            361
	Point                361
	Style                4
	TimeStamp            361
	# features=361

data\kml\time\timestamps.kml

	Placemark            11
	Point                11
	TimeSpan             1
	TimeStamp            10
	# features=11

data\kml\time\TimeTest.kml

	Feature uses inline Style
	--
	IconStyle            6
	Placemark            6
	Point                6
	Style                6
	TimeSpan             3
	TimeStamp            2
	# features=6

data\kml\time\YearDates.kml

	Placemark            3
	Point                3
	TimeSpan             1
	TimeStamp            2
	# features=3

data\kml\time\YearMonthDates.kml

	Placemark            4
	Point                4
	TimeSpan             1
	TimeStamp            3
	# features=4

data\kml\xal\gaddr.kml

	Placemark            1
	Point                1
	xal:AddressDetails   1
	# features=1

data\kml\xmlns\earth-google-com-kml-21.kml

	Placemark            1
	Point                1
	# features=1

data\kml\xmlns\earth-google-com-kml-22.kml

	Placemark            1
	Point                1
	# features=1

data\kml\xmlns\earth-google-com-kml-23.kml

	Placemark            1
	Point                1
	# features=1

data\kml\xmlns\earth-google-com-kml-30.kml

	Placemark            1
	Point                1
	# features=1

data\kml\xmlns\no-namespace.kml

	Placemark            1
	Point                1
	# features=1

data\kml\xmlns\opengis-net-kml-22.kml

	Placemark            1
	Point                1
	# features=1

data\kml\xmlns\opengis-net-kml-23.kml

	Placemark            1
	Point                1
	# features=1

data\kml\xmlns\opengis-net-kml-30.kml

	Placemark            1
	Point                1
	# features=1

