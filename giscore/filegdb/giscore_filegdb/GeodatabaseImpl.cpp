#include <FileGDBAPI.h>
#include "Stdafx.h"
#include "org_mitre_giscore_filegdb_Geodatabase.h"
#include <iostream>
#include <string>

using namespace std;
using namespace FileGDBAPI;

#ifdef __cplusplus
extern "C" {
#endif
/*
	* Class:     org_mitre_giscore_filegdb_Geodatabase
	* Method:    open
	* Signature: (Ljava/lang/String;)J
	*/
JNIEXPORT jlong JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_open(JNIEnv * env, jobject self, jstring path) {
	jboolean isCopy;
	const char* cpath = env->GetStringUTFChars(path, &isCopy);

	Geodatabase *geodatabase = new Geodatabase();
	fgdbError hr;
	size_t len = strlen(cpath);
	wchar_t *buf = new wchar_t[len*2];
	len = mbstowcs(buf, cpath, len);
	wstring wpath(buf, len);
	delete buf;
	
	if ((hr = OpenGeodatabase(wpath, *geodatabase)) != S_OK) {
		if (geodatabase != NULL) {
			delete geodatabase;
		}
		cerr << "Creating database failed, HR code " << hr << "\n";
		return 0;
	}

	return (long) geodatabase;
};

/*
	* Class:     org_mitre_giscore_filegdb_Geodatabase
	* Method:    close_db
	* Signature: (J)V
	*/
JNIEXPORT void JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_close_1db(JNIEnv *env, jobject self, jlong ptr) {
	if (ptr == 0L) {
		return;
	}
	Geodatabase *geodatabase = (Geodatabase*) ptr;
	fgdbError hr;
	jclass clazz = env->FindClass("java/lang/IllegalStateException");

	bool error = false;
	if ((hr = CloseGeodatabase(*geodatabase)) != S_OK)
	{
		error = true;
	}
	delete geodatabase;
	if (error) env->ThrowNew(clazz, "Bad geodatabase state or other error on close");
};


/**
 * Get the geodatabase object from the self passed in.
 */
Geodatabase* getGeodatabase(JNIEnv *env, jobject self) {
	jclass clz = env->FindClass("org/mitre/giscore/filegdb/Geodatabase");
	jfieldID field = env->GetFieldID(clz, "ptr", "J");
	jlong ptr = env->GetLongField(self, field);
	return (Geodatabase*) ptr;
}

/*
 * Class:     org_mitre_giscore_filegdb_Geodatabase
 * Method:    getDatasetTypes
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_getDatasetTypes(JNIEnv *env, jobject self) {
	Geodatabase* db = getGeodatabase(env, self);
	vector<wstring> datasetTypes;
	fgdbError hr;
	if ((hr = db->GetDatasetTypes(datasetTypes)) != S_OK) {
		jclass clazz = env->FindClass("org/mitre/giscore/filegdb/GdbException");
		env->ThrowNew(clazz, "Failed to find dataset types");
		return NULL;
	}
	jclass strclz = env->FindClass("java/lang/String");
	jobjectArray rval = env->NewObjectArray(datasetTypes.size(), strclz, env->NewStringUTF(""));



	return rval;
}

#ifdef __cplusplus
}
#endif
