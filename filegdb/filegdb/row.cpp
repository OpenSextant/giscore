#include "Stdafx.h"
#include <vector>
#include <iostream>
#include <string>
#include <jni.h>
#include <time.h>
#include <map>
#include "org_opensextant_giscore_filegdb_Row.h"

using namespace std;
using namespace FileGDBAPI;
extern "C" {

jobject getRowTable(JNIEnv* env, jobject rowself) {
	menv me(env);
	jclass clazz = me.findClass("org.opensextant.giscore.filegdb.Row");
	jfieldID field = me.getField(clazz, "table", "Lorg.opensextant.giscore.filegdb.Table;");
	jobject rval = env->GetObjectField(rowself, field);
	me.checkAndThrow();
	return rval;
}

/*
 * Class:     org_opensextant_giscore_filegdb_Row
 * Method:    getOID
 * Signature: ()Ljava/lang/Integer;
 */
JNIEXPORT jobject JNICALL Java_org_opensextant_giscore_filegdb_Row_getOID(JNIEnv *env, jobject self) {
	try {
		menv me(env);
		Row *row = me.getRow(self);
		int32 oid;
		me.esriCheckedCall(row->GetOID(oid), "Failed to get OID");
		return me.newInteger(oid);
	} catch(jni_check) {
		return 0l;
	}
}

int getBasicType(ShapeType st) {
	if (st == shapePoint || st == shapePointM || st == shapePointZM || st == shapePointZ )
		return 0;
	else if (st == shapeMultipoint || st == shapeMultipointM || st == shapeMultipointZM 
		     || st == shapeMultipointZ || st == shapeGeneralPoint)
		return 1;
	else if (st == shapePolyline || st == shapePolylineM || st == shapePolylineZM || st == shapePolylineZ)
		return 2;
	else if (st == shapePolygon || st == shapePolygonM || st == shapePolygonZM || st == shapePolygonZ)
		return 3;
	else if (st == shapeGeneralPolyline || st == shapeGeneralPolygon)
		return 4;
	else if (st == shapeMultiPatch || st == shapeMultiPatchM || st == shapeGeneralMultiPatch)
		return 5;
	else 
		return 6;
}

// On I/O of Geo data from the Java layer:
// 
// For all representations there is an intial Short which holds the
// type from Shape Types. The point entries are either two or three
// doubles depending on whether the z-axis has a value.
//
//		  Short: # shapeType
//		  Boolean: hasz (true if 3D points)
//		  Integer: npoints
//		  Integer: nparts
//		  part array (may be empty if nparts == 0)
//		  Double long, lat, zelev 

/**
 * Get an initialized java array at least big enough to hold the 
 * geometry information. It is initialized with the shape type and 
 * the hasz boolean information and the npoints and nparts information.
 */
jobjectArray getArray(JNIEnv *env, ShapeBuffer *buffer, bool hasz) {
	menv me(env);
	int length = 4; // Type + boolean + nparts + npoints
	int npoints;
	int nparts = 0;
	int ccount = hasz ? 3 : 2;
	ShapeType st;
	me.esriCheckedCall(buffer->GetShapeType(st), "Get shape type");
	int basicType = getBasicType(st);
	switch(basicType) {
	case 0: // Point
		length += ccount;
		npoints = 1;
		break;
	case 1: // Multipoint 
		{
			MultiPointShapeBuffer *mp = (MultiPointShapeBuffer*) buffer;
			mp->GetNumPoints(npoints);
			length += npoints * ccount;
			break;
		}
	case 2: // Polyline
	case 3: // Polygon
	case 4: // General Polyline, Polygon
		{ 
			MultiPartShapeBuffer *mpart = (MultiPartShapeBuffer*) buffer;
			mpart->GetNumPoints(npoints);
			mpart->GetNumParts(nparts);
			length += npoints * ccount;
			length += nparts;
			break;
		}
	case 5: // Patches
		// Unsupported
	default:
		return 0;
	}
	jclass objclz = me.findClass("java.lang.Object");
	jobjectArray rval = env->NewObjectArray((jsize) length, objclz, 0L);
	env->SetObjectArrayElement(rval, 0, me.newShort(basicType));
	env->SetObjectArrayElement(rval, 1, me.newBoolean(hasz));
	env->SetObjectArrayElement(rval, 2, me.newInteger(npoints));
	env->SetObjectArrayElement(rval, 3, me.newInteger(nparts));
	return rval;
}

void outputPoint(JNIEnv *env, int &ptr, jobjectArray oarr, Point &p, double z, bool hasz) {
	menv me(env);
	env->SetObjectArrayElement(oarr, ptr++, me.newDouble(p.x));
	env->SetObjectArrayElement(oarr, ptr++, me.newDouble(p.y));
	if (hasz) {
		env->SetObjectArrayElement(oarr, ptr++, me.newDouble(z));
	}
}

void inputPoint(JNIEnv *env, int &ptr, jobjectArray iarr, Point *&p, double *&z, bool hasz) {
	menv me(env);
	p->x = me.getDouble(env->GetObjectArrayElement(iarr, ptr++));
	p->y = me.getDouble(env->GetObjectArrayElement(iarr, ptr++));
	if (hasz) {
		*z = me.getDouble(env->GetObjectArrayElement(iarr, ptr++));
	}
}

/*
 * Class:     org_opensextant_giscore_filegdb_Row
 * Method:    getGeometry
 * Signature: ()Lorg/opensextant/giscore/geometry/Geometry;
 */
JNIEXPORT jobjectArray JNICALL Java_org_opensextant_giscore_filegdb_Row_getGeo(JNIEnv *env, jobject self) {
	try {
		menv me(env);
		Row *row = me.getRow(self);
		PointShapeBuffer psb;
		me.esriCheckedCall(row->GetGeometry(psb), "Get Geometry Failed");
		ShapeType st;
		int npoints;
		int nparts;
		double *z = 0L;
		double *zs = 0L;
		me.esriCheckedCall(psb.GetShapeType(st), "Get shapetype failed");
		int ptr = 4;
		bool hasz = ShapeBuffer::HasZs(st);
		switch(getBasicType(st)) {
		case 0: // Point
			{
				jobjectArray oarr = getArray(env, &psb, hasz);
				Point *point;
				psb.GetPoint(point);
				if (hasz) psb.GetZ(z);
				outputPoint(env, ptr, oarr, *point, hasz ? *z : 0L, hasz);
				return oarr;
			}
		case 1: // Multipoint
			{
				MultiPointShapeBuffer mp;
				row->GetGeometry(mp);
				jobjectArray oarr = getArray(env, &mp, hasz);
				mp.GetNumPoints(npoints);
				Point *points;
				mp.GetPoints(points);
				if (hasz) mp.GetZs(zs);
				for(int i = 0; i < npoints; i++) {
					outputPoint(env, ptr, oarr, points[i], hasz ? zs[i] : 0L, hasz);
				}
				return oarr;
			}
		case 2: // Polyline
		case 3: // Polygon
		case 4: // General Polyline, Polygon
			{
				MultiPartShapeBuffer mpart;
				row->GetGeometry(mpart);
				jobjectArray oarr = getArray(env, &mpart, hasz);
				mpart.GetNumPoints(npoints);
				mpart.GetNumParts(nparts);
				Point *points;
				int *parts;
				mpart.GetPoints(points);
				mpart.GetParts(parts);
				if (hasz) mpart.GetZs(zs);
				for(int j = 0; j < nparts; j++) {
					env->SetObjectArrayElement(oarr, ptr++, me.newInteger(parts[j]));
				}
				for(int i = 0; i < npoints; i++) {
					outputPoint(env, ptr, oarr, points[i], hasz ? zs[i] : 0L, hasz);
				}
				return oarr;
			}
		case 5: // Patches
			// Unsupported
		default:
			return 0L;
		}

	} catch(jni_check) {
		return 0L;
	}
}

/*
 * Class:     org_opensextant_giscore_filegdb_Row
 * Method:    setGeometry
 * Signature: (Lorg/opensextant/giscore/filegdb/ShapeBuffer;)V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Row_setGeometry(JNIEnv *env, jobject self, jobjectArray iarr) {
	try {
		menv me(env);
		Row *row = me.getRow(self);
		int ptr = 0;
		double *zs = 0L;
		double *z = 0L;
		Point *points;
		ShapeType st = (ShapeType) me.getShort(env->GetObjectArrayElement(iarr, ptr++));
		bool hasz = me.getBoolean(env->GetObjectArrayElement(iarr, ptr++));
		int npoints = me.getInteger(env->GetObjectArrayElement(iarr, ptr++));
		int nparts = me.getInteger(env->GetObjectArrayElement(iarr, ptr++));
		switch (getBasicType(st)) {
		case 0: // Point
			{
				PointShapeBuffer psb;
				psb.Setup(st);
				psb.GetPoint(points);
				if (hasz) psb.GetZ(zs);
				inputPoint(env, ptr, iarr, points, zs, hasz);
				me.esriCheckedCall(row->SetGeometry(psb), "bad geometry");
			}
			break;
		case 1: // Multipoint
			{
				MultiPointShapeBuffer mpart;
				mpart.Setup(st, npoints);
				mpart.GetPoints(points);
				if (hasz) mpart.GetZs(zs);
				for(int i = 0; i < npoints; i++) {
					Point *p = &points[i];
					if (hasz) z = &zs[i];
					inputPoint(env, ptr, iarr, p, z, hasz);
				}
				me.esriCheckedCall(mpart.CalculateExtent(), "error");
				me.esriCheckedCall(row->SetGeometry(mpart), "bad geometry");
			}
			break;
		case 2: // Polyline
		case 3: // Polygon
		case 4: // General Polyline, Polygon
			{
				MultiPartShapeBuffer mpart;
				int *parts;
				mpart.Setup(st, nparts, npoints, 0);
				mpart.GetPoints(points);
				if (hasz) mpart.GetZs(zs);
				mpart.GetParts(parts);
				for(int j = 0; j < nparts; j++) {
					parts[j] = me.getInteger(env->GetObjectArrayElement(iarr, ptr++));
				}
				for(int i = 0; i < npoints; i++) {
					Point *p = &points[i];
					if (hasz) z = &zs[i];
					inputPoint(env, ptr, iarr, p, z, hasz);
				}
				me.esriCheckedCall(mpart.CalculateExtent(), "error");
				me.esriCheckedCall(row->SetGeometry(mpart), "bad geometry");
			}
			break;
		case 5: // Patches
		default: // Unsupported
			;
		}
	} catch(jni_check) {
		
	}
}

FieldInfo* getFieldInfo(JNIEnv *env, jobject self, Table *t);

/*
 * Class:     org_opensextant_giscore_filegdb_Row
 * Method:    getAttrArray
 * Signature: ()[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL Java_org_opensextant_giscore_filegdb_Row_getAttrArray(JNIEnv *env, jobject self) {
	try {
		menv me(env);
		Row *row = me.getRow(self);
		jobject table = getRowTable(env, self);
		Table *tobj = me.getTable(table);
		FieldInfo* fieldInfo = getFieldInfo(env, table, tobj);
		int count;
		fieldInfo->GetFieldCount(count);
		jclass oclass = me.findClass("java.lang.Object");
		jclass calclass = me.findClass("java.util.Calendar");
		jmethodID ccgetinstance = env->GetStaticMethodID(calclass, "getInstance", "()Ljava/util/Calendar;");
		jmethodID ccsetTime = me.getMethod(calclass, "setTimeInMillis", "(J)V");
		jobjectArray rval = env->NewObjectArray(count * 2, oclass, 0L);
		int ptr = 0;
		for(int i = 0; i < count; i++) {
			wstring fieldName;
			fieldInfo->GetFieldName(i, fieldName);
			convstr name(fieldName.c_str());
			jstring jname = env->NewStringUTF(name.getStr().c_str());
			jobject val = 0L;
			FieldType fieldType;
			fieldInfo->GetFieldType(i, fieldType);
			bool isnull;
			row->IsNull(fieldName, isnull);
			if (! isnull && name.getStr().length() > 0) { 
				switch(fieldType) {
				case fieldTypeSmallInteger: 
					{
						short sval;
						row->GetShort(fieldName, sval);
						val = me.newShort(sval);
						break;
					}
				case fieldTypeInteger:
					{
						int ival;
						row->GetInteger(fieldName, ival);
						val = me.newInteger(ival);
						break;
					}
				case fieldTypeSingle:
					{
						float fval;
						row->GetFloat(fieldName, fval);
						val = me.newFloat(fval);
						break;
					}
				case fieldTypeDouble:
					{
						double dval;
						row->GetDouble(fieldName, dval);
						val = me.newDouble(dval);
						break;
					}
				case fieldTypeString:
					{
						wstring strval;
						row->GetString(fieldName, strval);
						convstr cstrval(strval.c_str());
						val = env->NewStringUTF(cstrval.getStr().c_str());
						break;
					}
				case fieldTypeDate:
					{
						tm date;
						row->GetDate(fieldName, date);
						time_t dtm = mktime(&date);
						jobject cal = env->CallStaticObjectMethod(calclass, ccgetinstance);
						env->CallVoidMethod(cal, ccsetTime, me.newLong(1000 * dtm));
						val = cal;
						break;
					}
				case fieldTypeGeometry:
					continue;
				case fieldTypeBlob:
					break;
				case fieldTypeRaster:
					break;
				case fieldTypeGUID:
					break;
				case fieldTypeGlobalID:
					break;
				case fieldTypeXML:
					{
						wstring xstrval;
						row->GetString(fieldName, xstrval);
						convstr cxstrval(xstrval.c_str());
						val = env->NewStringUTF(cxstrval.getStr().c_str());
						break;
					}
				default:
					break;
				}
			}
			env->SetObjectArrayElement(rval, ptr++, jname);
			env->SetObjectArrayElement(rval, ptr++, val);
		}
		return rval;
	} catch(jni_check) {
		return 0l;
	}
}

map<string,FieldType>* getFieldMap(JNIEnv *env, jobject tableself, Table *t);

/*
 * Class:     org_opensextant_giscore_filegdb_Row
 * Method:    setAttrArray
 * Signature: ([Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Row_setAttrArray(JNIEnv *env, jobject self, jobjectArray newvals) {
	try {
		menv me(env);
		Row *row = me.getRow(self);
		jobject table = getRowTable(env, self); // Fields are stored with the table
		Table *tobj = me.getTable(table);
		map<string,FieldType> *fieldMap = getFieldMap(env, table, tobj);
		jclass bclass = me.findClass("java.lang.Boolean");
		jclass calclass = me.findClass("java.util.Calendar");
		jmethodID ccgetTime = me.getMethod(calclass, "getTimeInMillis", "()J");
		jclass dateclass = me.findClass("java.util.Date");
		jmethodID dgetTime = me.getMethod(dateclass, "getTime", "()J");
		jsize len = env->GetArrayLength(newvals);
		for(int i = 0; i < len; i += 2) {
			jstring name = (jstring) env->GetObjectArrayElement(newvals, i);
			jobject val = env->GetObjectArrayElement(newvals, i+1);
			convstr fieldname(env, name);
			FieldType &type = (*fieldMap)[fieldname.getStr()];
			if (val == 0L) {
				row->SetNull(fieldname.getWstr());
			} else {
				switch(type != 0L ? type : 9999999) {
				case fieldTypeSmallInteger: {
						// cerr << "type: si\n";
						jclass vclass = me.getClass(val);
						if (me.equals(vclass, bclass)) {
							bool bval = me.getBoolean(val);
							row->SetShort(fieldname.getWstr(), bval ? 1 : 0);
						} else {
							row->SetShort(fieldname.getWstr(), me.getShort(val));
						}
					}
					break;
				case fieldTypeInteger:
					// cerr << "type: i\n";
					row->SetInteger(fieldname.getWstr(), me.getInteger(val));
					break;
				case fieldTypeSingle:
					// cerr << "type: sf\n";
					row->SetFloat(fieldname.getWstr(), me.getFloat(val));
					break;
				case fieldTypeDouble:
					// cerr << "type: df\n";
					row->SetDouble(fieldname.getWstr(), me.getDouble(val));
					break;
				case fieldTypeString:
					// cerr << "type: str\n";
					row->SetString(fieldname.getWstr(), me.getWString((jstring) val));
					break;
				case fieldTypeDate:
					{
						// cerr << "type: dt\n";
						jclass vclass = env->GetObjectClass(val);
						jlong dtm;
						if (env->IsSameObject(vclass, calclass)) {
							dtm = env->CallLongMethod(val, ccgetTime);
						} else {
							dtm = env->CallLongMethod(val, dgetTime);
						}
						time_t dtime = dtm / 1000L;
						tm *date_tm = localtime(&dtime);
						row->SetDate(fieldname.getWstr(), *date_tm);
						break;
					}
				case fieldTypeGeometry:
					break;
				case fieldTypeBlob:
					// row->Set(fieldname.getWstr(), get(val));
					break;
				case fieldTypeRaster:
					// row->Set(fieldname.getWstr(), get(val));
					break;
				case fieldTypeGUID:
					// row->Set(fieldname.getWstr(), get(val));
					break;
				case fieldTypeGlobalID:
					// row->Set(fieldname.getWstr(), get(val));
					break;
				case fieldTypeXML:
					// cerr << "type: xml\n";
					row->SetXML(fieldname.getWstr(), me.getString((jstring) val));
					break;
				default:
					break;
				}
			}
		}
	} catch(jni_check) {
		//
	}
}

// Match extern "C"
}
// 