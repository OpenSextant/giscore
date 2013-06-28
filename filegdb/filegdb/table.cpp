#include "Stdafx.h"
#include <vector>
#include <iostream>
#include <string>
#include <jni.h>
#include <unordered_map>
#include "org_opensextant_giscore_filegdb_Table.h"

using namespace std;
using namespace FileGDBAPI;

extern "C" {

/*
 * Class:     org_opensextant_giscore_filegdb_Table
 * Method:    initRow
 * Signature: (Lorg/opensextant/giscore/filegdb/Row;)V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Table_initRow(JNIEnv *env, jobject self, jobject robj) {
	try {
		menv me(env);
		Table *t = me.getTable(self);
		Row *row = me.getRow(robj);
		me.esriCheckedCall(t->CreateRowObject(*row), "Failed to create row");
	} catch(jni_check) {
		//
	}
}

/*
 * Class:     org_opensextant_giscore_filegdb_Table
 * Method:    add
 * Signature: (Lorg/opensextant/giscore/filegdb/Row;)V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Table_add(JNIEnv *env, jobject self, jobject robj) {
	try {
		menv me(env);
		Table *t = me.getTable(self);
		Row *row = me.getRow(robj);
		me.esriCheckedCall(t->Insert(*row), "Failed to insert row");
	} catch(jni_check) {
		//
	}
}

/*
 * Class:     org_opensextant_giscore_filegdb_Table
 * Method:    enumerate
 * Signature: ()Lorg/opensextant/giscore/filegdb/EnumRows;
 */
JNIEXPORT jobject JNICALL Java_org_opensextant_giscore_filegdb_Table_enumerate1(JNIEnv *env, jobject self) {
	try {
		menv me(env);
		Table *t = me.getTable(self);
		EnumRows *enumRows = new EnumRows();
		Envelope envelope;
		envelope.SetEmpty();
		wstring fields(L"*");
		wstring where(L"");
		me.esriCheckedCall(t->Search(fields, where, envelope, true, *enumRows), "Search failed");
		return me.newObject("org.opensextant.giscore.filegdb.EnumRows", (void*) enumRows);
	} catch(jni_check) {
		return 0l;
	}
}

FieldInfo* getFieldInfo(JNIEnv *env, jobject self, Table *t) {
	menv me(env);
	FieldInfo* fieldInfo = (FieldInfo*) me.getLongFieldValue(self, "org.opensextant.giscore.filegdb.Table", "fieldinfo_holder");
	if (fieldInfo == 0L) {
		fieldInfo = new FieldInfo();
		me.esriCheckedCall(t->GetFieldInformation(*fieldInfo), "Field Info Retrieval Failed");
		me.setLongFieldValue(self, "org.opensextant.giscore.filegdb.Table", "fieldinfo_holder", fieldInfo);
	}
	return fieldInfo;
}

unordered_map<string,FieldType>* getFieldMap(JNIEnv *env, jobject self, Table *t) {
	menv me(env);
	unordered_map<string,FieldType> *fieldtype_map = (unordered_map<string,FieldType> *) me.getLongFieldValue(self, "org.opensextant.giscore.filegdb.Table", "fieldtype_map");	
	wstring fieldName;
	if (fieldtype_map == 0L) {
		FieldInfo *info = getFieldInfo(env, self, t);
		fieldtype_map = new unordered_map<string,FieldType>();
		int count;
		if (info->GetFieldCount(count) == S_OK) {
			for(int i = 0; i < count; i++) {
				FieldType type;
				info->GetFieldName(i, fieldName);
				convstr fn(fieldName.data());
				info->GetFieldType(i, type);
				(*fieldtype_map)[fn.getStr()] = type;
			}
			me.setLongFieldValue(self, "org.opensextant.giscore.filegdb.Table", "fieldtype_map", fieldtype_map);
		}
	}
	return fieldtype_map;
}

/*
 * Class:     org_opensextant_giscore_filegdb_Table
 * Method:    getFieldInfo
 * Signature: ()[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL Java_org_opensextant_giscore_filegdb_Table_getFieldInfo(JNIEnv *env, jobject self) {
	try {
		menv me(env);
		Table *t = me.getTable(self);
		FieldInfo* fieldInfo = getFieldInfo(env, self, t);
		int count;
		me.esriCheckedCall(fieldInfo->GetFieldCount(count), "Field Count");
		jclass objclz = me.findClass("java.lang.Object");
		jobjectArray oarr = env->NewObjectArray((jsize) count * 4, objclz, 0L);
		int ptr = 0;
		for(int i = 0; i < count; i++) {
			wstring fieldName;
			FieldType type;
			int fieldLen;
			bool isNullable;
			fieldInfo->GetFieldName(i, fieldName);
			fieldInfo->GetFieldType(i, type);
			fieldInfo->GetFieldLength(i, fieldLen);
			fieldInfo->GetFieldIsNullable(i, isNullable);
			convstr sstr(fieldName.c_str());
			env->SetObjectArrayElement(oarr, ptr++, env->NewStringUTF(sstr.getStr().c_str()));
			env->SetObjectArrayElement(oarr, ptr++, me.newInteger(type));
			env->SetObjectArrayElement(oarr, ptr++, me.newInteger(fieldLen));
			env->SetObjectArrayElement(oarr, ptr++, me.newBoolean(isNullable));
		}
		return oarr;
	} catch(jni_check) {
		return 0l;
	}
}

/*
 * Class:     org_opensextant_giscore_filegdb_Table
 * Method:    close1
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_opensextant_giscore_filegdb_Table_close1(JNIEnv *env, jobject self) {
	try {
		menv me(env);
		Table *t = me.getTable(self);
		FieldInfo* fieldInfo = (FieldInfo*) me.getLongFieldValue(self, "org.opensextant.giscore.filegdb.Table", "fieldinfo_holder");
		if (fieldInfo != 0L) {
			delete fieldInfo;
		}
		map<string,FieldType> *fieldtype_map = (map<string,FieldType> *) me.getLongFieldValue(self, "org.opensextant.giscore.filegdb.Table", "fieldtype_map");
		if (fieldtype_map != 0L) {
			delete fieldtype_map;
		}
	} catch(jni_check) {
	}
}

}