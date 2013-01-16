#include "Stdafx.h"
#include <vector>
#include <iostream>
#include <string>
#include <jni.h>

using namespace std;
using namespace FileGDBAPI;
extern "C" {

long makeEnumRows() {
	return (long) new EnumRows();
}

/**
 * Get the EnumRows object from the self passed in.
 */
EnumRows* getEnumRows(JNIEnv *env, jobject self) {
	menv me(env);
	return (EnumRows*) me.getPtr(self, makeEnumRows);
}

/*
 * Class:     org_mitre_giscore_filegdb_EnumRows
 * Method:    next1
 * Signature: ()Lorg/mitre/giscore/filegdb/Row;
 */
JNIEXPORT jobject JNICALL Java_org_mitre_giscore_filegdb_EnumRows_next1(JNIEnv *env, jobject self) {
	Row *row = new Row();
	menv me(env);
	EnumRows *e = (EnumRows*) getEnumRows(env, self);
	if (S_OK == e->Next(*row)) {
		return me.newObject("org.mitre.giscore.filegdb.Row", row);
	} else {
		delete row;
		return 0L;
	}
}

/*
 * Class:     org_mitre_giscore_filegdb_EnumRows
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_mitre_giscore_filegdb_EnumRows_close(JNIEnv *env, jobject self) {
	try {
		EnumRows *e = (EnumRows*) getEnumRows(env, self);
		delete e;
	} catch(jni_check) {

	}
}


}