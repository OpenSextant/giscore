#include "Stdafx.h"
#include <vector>
#include <iostream>
#include <string>
#include <jni.h>

using namespace std;
using namespace FileGDBAPI;

extern "C" {

// Used to reallocate, 
ShapeBuffer makeShapeBuffer(ShapeType &shapeType) {
	switch shapeType {
	case shapePoint, shapePointM, shapePointZM, shapePointZ:
		return 0;
	case shapeMultipoint, shapeMultipointM, shapeMultipointZM, shapeMultipointZ:
		return new MultiPointShapeBuffer();
	case shapePolyline, shapePolylineM, shapePolylineZM, shapePolylineZ:
		return new MultiPointShapeBuffer();
	case shapePolygon, shapePolygonM, shapePolygonZM, shapePolygonZ:
		return new MultiPointShapeBuffer();
	default:
		return 0;
	}
}

/**
 * Get the shapebuffer object from the self passed in.
 */
ShapeBuffer* getShapeBuffer(JNIEnv *env, jobject self) {
	return getPtr(env, self, null);
}

}