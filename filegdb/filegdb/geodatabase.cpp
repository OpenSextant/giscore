// filegdb_test.cpp : main project file.
#include "Stdafx.h"
#include <vector>
#include <iostream>
#include <string>
#include <jni.h>

using namespace std;
using namespace FileGDBAPI;

extern "C" {

class jni_check : public exception {
public:
    jni_check() {
        
    };
};

jclass findClass(JNIEnv *env, const char* classname);

/**
 * Throw an exception specific to this library code. You still need to return 
 * since this doesn't create a transfer of control.
 * @param env
 * @param message
 */
void throwException(JNIEnv *env, const char* message) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear(); // Clear any existing exception
    }
    jclass exClass = findClass(env, "java/lang/RuntimeException");
    env->ThrowNew(exClass, message);
}

/**
 * Handle exceptions from JNI operations
 * @param env
 * @return 
 */
inline void checkAndThrow(JNIEnv *env) throw(jni_check) {
    jthrowable t = env->ExceptionOccurred();
    if (t != NULL) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        throwException(env, "thrown from native code");
        throw jni_check();
    } 
}

inline jclass findClass(JNIEnv *env, const char* classname) {
    jclass rval = env->FindClass(classname);
    checkAndThrow(env);
    return rval;
}

inline jfieldID getField(JNIEnv* env, jclass clazz, const char* fieldname, const char* sig) {
    jfieldID rval = env->GetFieldID(clazz, fieldname, sig);
    checkAndThrow(env);
    return rval;
}

inline void esriCheckedCall(JNIEnv *env, fgdbError err, const char* message) throw(jni_check) {
	if (err != S_OK) {
		char buf[100];
		cerr << "message is " << message;
		wstring errorText;
		ErrorInfo::GetErrorDescription(err, errorText);
		if (errorText.length() > 0) {
			cerr << " errorText = ";
			wcerr << errorText.c_str();
		} 
		cerr << " [" << itoa(err, buf, 10) << "]\n";
		throwException(env, message);
        throw jni_check();
	}
}

/**
 * Get the geodatabase object from the self passed in.
 */
Geodatabase* getGeodatabase(JNIEnv *env, jobject self) {
	jclass clz = findClass(env, "org/mitre/giscore/filegdb/Geodatabase");
	jfieldID field = getField(env, clz, "ptr", "J");
	jlong ptr = env->GetLongField(self, field);
	checkAndThrow(env);
	return (Geodatabase*) ptr;
}

/*
 * Class:     org_mitre_giscore_filegdb_Geodatabase
 * Method:    open
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_open(JNIEnv * env, jobject self, jstring path) {
	try {
		Geodatabase *geodatabase = new Geodatabase();
		convstr wpath(env, path);
		esriCheckedCall(env, OpenGeodatabase(wpath.getWstr(), *geodatabase), "Creating database failed");
		return (long) geodatabase;
	} catch (jni_check) {
        return 0;
    }
};

/*
 * Class:     org_mitre_giscore_filegdb_Geodatabase
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_close1(JNIEnv * env, jobject self) {
	try {
		Geodatabase* db = getGeodatabase(env, self);
		esriCheckedCall(env, CloseGeodatabase(*db), "Closing database failed");
	} catch (jni_check) {
		//
    }
};


/*
 * Class:     org_mitre_giscore_filegdb_Geodatabase
 * Method:    create
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_create(JNIEnv *env, jobject self, jstring path) {
	try {
		Geodatabase* db = getGeodatabase(env, self);
		convstr wpath(env, path);
		wcerr << L"Path: " << wpath.getWstr() << L"\n";
		esriCheckedCall(env, CreateGeodatabase(wpath.getWstr(), *db), "Creating database failed");
		return (long) db;
	} catch (jni_check) {
		return 0;
    }
}

/*
 * Class:     org_mitre_giscore_filegdb_Geodatabase
 * Method:    delete
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_delete(JNIEnv *env, jclass clz, jstring path) {
	try {
		convstr wpath(env, path);
		esriCheckedCall(env, DeleteGeodatabase(wpath.getWstr()), "Deleting database failed");
	} catch (jni_check) {
        //
    }
}


jobjectArray processJStringArray(JNIEnv *env, vector<wstring> strvector) {
	jclass strclz = findClass(env, "java/lang/String");
	jobjectArray rval = env->NewObjectArray((jsize) strvector.size(), strclz, env->NewStringUTF(""));
	checkAndThrow(env);
	for(int i = 0; i < strvector.size(); i++) {
		convstr wtype(strvector.at(i).c_str());
		jstring type = env->NewStringUTF(wtype.getStr().c_str());
		env->SetObjectArrayElement(rval, i, type);
		checkAndThrow(env);
	}
	return rval;
}

jobjectArray processJStringArray2(JNIEnv *env, vector<string> strvector) {
	jclass strclz = findClass(env, "java/lang/String");
	jobjectArray rval = env->NewObjectArray((jsize) strvector.size(), strclz, env->NewStringUTF(""));
	checkAndThrow(env);
	for(int i = 0; i < strvector.size(); i++) {
		convstr stype(strvector.at(i).c_str());
		jstring type = env->NewStringUTF(stype.getStr().c_str());
		env->SetObjectArrayElement(rval, i, type);
		checkAndThrow(env);
	}
	return rval;
}

/*
 * Class:     Java_org_mitre_giscore_filegdb_Geodatabase
 * Method:    getDatasetTypes
 * Signature: ()
 */
JNIEXPORT jobjectArray JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_getDatasetTypes(JNIEnv *env, jobject self) {
	Geodatabase* db = getGeodatabase(env, self);
	vector<wstring> datasettypes(3);
	try {
		esriCheckedCall(env, db->GetDatasetTypes(datasettypes), "Problem getting data types");
		return processJStringArray(env, datasettypes);
	} catch (jni_check) {
        return 0;
    }
}

/*
 * Class:     org_mitre_giscore_filegdb_Geodatabase
 * Method:    getChildDatasets
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_getChildDatasets(JNIEnv *env, jobject self, jstring parent, jstring dataset) {
	Geodatabase* db = getGeodatabase(env, self);
	convstr parentPath(env, parent);
	convstr datasetstr(env, dataset);
	vector<wstring> datasetTypes;
	try {
		esriCheckedCall(env, db->GetChildDatasets(parentPath.getWstr().c_str(), datasetstr.getWstr().c_str(), datasetTypes),
			"Failed to find dataset types");
		return processJStringArray(env, datasetTypes);
	} catch (jni_check) {
        return 0;
    }
}

/*
 * Class:     org_mitre_giscore_filegdb_Geodatabase
 * Method:    getChildDatasets
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_mitre_giscore_filegdb_Geodatabase_getChildDatasetDefinitions(JNIEnv *env, jobject self, jstring parent, jstring dataset) {
	Geodatabase* db = getGeodatabase(env, self);
	convstr parentPath(env, parent);
	convstr datasetstr(env, dataset);
	vector<string> datasetTypes;
	try {
		esriCheckedCall(env, db->GetChildDatasetDefinitions(parentPath.getWstr().c_str(), datasetstr.getWstr().c_str(), datasetTypes),
			"Failed to find dataset types");
		return processJStringArray2(env, datasetTypes);
	} catch (jni_check) {
        return 0;
    }
}

}
