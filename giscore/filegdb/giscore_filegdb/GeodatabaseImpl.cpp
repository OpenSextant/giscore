#include <FileGDBAPI.h>
#include "Stdafx.h"
#include "org_mitre_giscore_filegdb_Geodatabase.h"

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
	wchar_t* wpath = new wchar_t[len+1];
	mbtowc(wpath, cpath, len);

	if ((hr = OpenGeodatabase(wpath, *geodatabase)) != S_OK) {
		delete geodatabase;
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

#ifdef __cplusplus
}
#endif
