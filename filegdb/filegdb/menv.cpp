#include "Stdafx.h"
#include <vector>
#include <iostream>
#include <string>
#include <jni.h>
#include "menv.h"

using namespace std;
using namespace FileGDBAPI;

#if defined(_MSC_VER)
HANDLE menv::mutex; 
#else
pthread_mutex_t menv::mutex;
#endif

menv::menv(JNIEnv *e) {
#if defined(_MSC_VER)
        DWORD result = WaitForSingleObject(mutex, INFINITE);
        if (result == WAIT_OBJECT_0) {
                unlock = true;
        } else {
                unlock = false;
                if (result == WAIT_FAILED) {
                }
                throw jni_check();
        }
#else
        pthread_mutex_lock(&mutex);
#endif
        env = e;
}


menv::~menv() {
#if defined(_MSC_VER)
        if (unlock) {
                if (ReleaseMutex(mutex) == 0) {
                        throw jni_check(); 
                }
        }
#else
        pthread_mutex_unlock(&mutex);
#endif		
}
        
void menv::initialize() {
#if defined(_MSC_VER)
        mutex = CreateMutex(NULL, FALSE, NULL);
#elif defined(__APPLE__)
        mutex = PTHREAD_RECURSIVE_MUTEX_INITIALIZER;
#else
        mutex = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP;
#endif
}

/**
 * Throw an exception specific to this library code. You still need to return 
 * since this doesn't create a transfer of control.
 * @param env
 * @param message
 */
void menv::throwException(const char* message) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear(); // Clear any existing exception
    }
    jclass exClass = findClass("java/lang/RuntimeException");
    env->ThrowNew(exClass, message);
}

/**
 * Handle exceptions from JNI operations
 * @param env
 * @return 
 */
void menv::checkAndThrow() throw(jni_check) {
    jthrowable t = env->ExceptionOccurred();
    if (t != NULL) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        throwException("thrown from native code");
        throw jni_check();
    } 
}

void cvtClass(string &classname) {
	for(int i = 0; i < classname.length(); i++) {
		if (classname[i] == '.') {
			classname[i] = '/';
		}
	}
}

jclass menv::findClass(const char* c) {			
	string classname(c);
	cvtClass(classname);
    jclass rval = env->FindClass(classname.c_str());
    checkAndThrow();
    return rval;
}

jfieldID menv::getField(jclass clazz, const char* fieldname, const char* sig) {
	string signature(sig);
	cvtClass(signature);
	jfieldID rval = env->GetFieldID(clazz, fieldname, signature.c_str());
    checkAndThrow();
    return rval;
}

jmethodID menv::getMethod(jclass clazz, const char* methodname, const char* sig) {
    jmethodID rval = env->GetMethodID(clazz, methodname, sig);
    checkAndThrow();
    return rval;
}

jmethodID menv::getCtor(jclass clazz, const char* sig) {
	return getMethod(clazz, "<init>", sig);
}

void menv::esriCheckedCall(fgdbError err, const char* message) throw(jni_check) {
	if (err != S_OK) {
		cerr << "message is " << message;
		cerr << " error number is " << err << "\n";
		throwException(message);
        throw jni_check();
	}
}

/**
 * Get the ptr from the self passed in or create
 * a new instance of the passed in class.
 */
void* menv::getPtr(jobject self, long ctor()) {
	jclass clazz = findClass("org/mitre/giscore/filegdb/GDB");
	jfieldID field = getField(clazz, "ptr", "J");
	jlong ptr = env->GetLongField(self, field);
	checkAndThrow();
	if (ptr == 0L && ctor != 0L) {
		ptr = (long) ctor();
		env->SetLongField(self, field, ptr);
	}
	return (void*) ptr;
}

void menv::setPtr(jobject self, void* ptr) {
	jclass clz = env->GetObjectClass(self);
	jfieldID tptr = getField(clz, "ptr", "J");
	env->SetLongField(self, tptr, (jlong) ptr);
}

/**
 * Get the long field value from the self passed in 
 */
void* menv::getLongFieldValue(jobject self, const char* classname, const char* fieldname) {
	jclass clazz = findClass(classname);
	jfieldID field = getField( clazz, fieldname, "J");
	jlong ptr = env->GetLongField(self, field);
	checkAndThrow();
	return (void*) ptr;
}

/**
 * Set the long field value from the self passed in 
 */
void menv::setLongFieldValue(jobject self, const char* classname, const char* fieldname, void* value) {
	jclass clazz = findClass(classname);
	jfieldID field = getField(clazz, fieldname, "J");
	env->SetLongField(self, field, (long) value);
	checkAndThrow();
}

/**
 * Allocate a new java object using the default ctor and assign the given
 * void ptr to the ptr instance variable. Assumes the class is a subclass
 * of GDB.
 *
 * @param env The JNI environment
 * @param clazz The Java package and class, may use either . or / as separators
 * @param ptr a void pointer to an allocated object to hold a pointer to in the long in the java object
 */
jobject menv::newObject(const char* clazz, void* ptr) {
	jclass tc = findClass(clazz);
	jobject object = env->AllocObject(tc);
	setPtr(object, ptr);
	return object;
}

jobject menv::newBoolean(bool val) {
	jclass tc = findClass("java.lang.Boolean");
	jmethodID ctor = getCtor(tc, "(Z)V");
	jobject rval = env->NewObject(tc, ctor, (jboolean) val);
	checkAndThrow();
	return rval;
}

jobject menv::newShort(short val) {
	jclass tc = findClass("java.lang.Short");
	jmethodID ctor = getCtor(tc, "(S)V");
	jobject rval = env->NewObject(tc, ctor, (jshort) val);
	checkAndThrow();
	return rval;
}

jobject menv::newInteger(int val) {
	jclass tc = findClass("java.lang.Integer");
	jmethodID ctor = getCtor(tc, "(I)V");
	jobject rval = env->NewObject(tc, ctor, (jint) val);
	checkAndThrow();
	return rval;
}

jobject menv::newFloat(float val) {
	jclass tc = findClass("java.lang.Float");
	jmethodID ctor = getCtor(tc, "(F)V");
	jobject rval = env->NewObject(tc, ctor, (jfloat) val);
	checkAndThrow();
	return rval;
}

jobject menv::newDouble(double val) {
	jclass tc = findClass("java.lang.Double");
	jmethodID ctor = getCtor(tc, "(D)V");
	jobject rval = env->NewObject(tc, ctor, (jdouble) val);
	checkAndThrow();
	return rval;
}

jobject menv::newLong(long val) {
	jclass tc = findClass("java.lang.Long");
	jmethodID ctor = getCtor(tc, "(J)V");
	jobject rval = env->NewObject(tc, ctor, (jlong) val);
	checkAndThrow();
	return rval;
}

bool menv::getBoolean(jobject val) {
	jclass tc = findClass("java.lang.Boolean");
	jmethodID get = getMethod(tc, "booleanValue", "()Z");
	jboolean rval = env->CallBooleanMethod(val, get);
	checkAndThrow();
	return rval;
}

short menv::getShort(jobject val) {
	jclass tc = findClass("java.lang.Number");
	jmethodID get = getMethod(tc, "shortValue", "()S");
	jshort rval = env->CallShortMethod(val, get);
	checkAndThrow();
	return rval;
}

int menv::getInteger(jobject val) {
	jclass tc = findClass("java.lang.Number");
	jmethodID get = getMethod(tc, "intValue", "()I");
	jint rval = env->CallIntMethod(val, get);
	checkAndThrow();
	return rval;
}

long menv::getLong(jobject val) {
	jclass tc = findClass("java.lang.Number");
	jmethodID get = getMethod(tc, "longValue", "()I");
	jlong rval = env->CallLongMethod(val, get);
	checkAndThrow();
	return rval;
}

float menv::getFloat(jobject val) {
	jclass tc = findClass("java.lang.Number");
	jmethodID get = getMethod(tc, "floatValue", "()F");
	jfloat rval = env->CallFloatMethod(val, get);
	checkAndThrow();
	return rval;
}

double menv::getDouble(jobject val) {
	jclass tc = findClass("java.lang.Number");
	jmethodID get = getMethod(tc, "doubleValue", "()D");
	jdouble rval = env->CallDoubleMethod(val, get);
	checkAndThrow();
	return rval;
}

wstring menv::getWString(jstring val) {
	convstr rval(env, val);
	return rval.getWstr();
}

string menv::getString(jstring val) {
	convstr rval(env, val);
	return rval.getStr();
}


jobjectArray menv::processJStringArray(vector<wstring> strvector) {
	jclass strclz = findClass("java/lang/String");
	jobjectArray rval = env->NewObjectArray((jsize) strvector.size(), strclz, env->NewStringUTF(""));
	checkAndThrow();
	for(int i = 0; i < strvector.size(); i++) {
		convstr wtype(strvector.at(i).c_str());
		jstring type = env->NewStringUTF(wtype.getStr().c_str());
		env->SetObjectArrayElement(rval, i, type);
		checkAndThrow();
	}
	return rval;
}

jobjectArray menv::processJStringArray2(vector<string> strvector) {
	jclass strclz = findClass("java/lang/String");
	jobjectArray rval = env->NewObjectArray((jsize) strvector.size(), strclz, env->NewStringUTF(""));
	checkAndThrow();
	for(int i = 0; i < strvector.size(); i++) {
		convstr stype(strvector.at(i).c_str());
		jstring type = env->NewStringUTF(stype.getStr().c_str());
		env->SetObjectArrayElement(rval, i, type);
		checkAndThrow();
	}
	return rval;
}

long makeGeodatabase() {
	return (long) new Geodatabase();
}

/**
 * Get the geodatabase object from the self passed in.
 */
Geodatabase* menv::getGeodatabase(jobject self) {
	return (Geodatabase*) getPtr(self, makeGeodatabase);
}

long makeRow() {
	return (long) new Row();
}

/**
 * Get the Row object from the self passed in.
 */
Row* menv::getRow(jobject self) {
	return (Row*) getPtr(self, makeRow);
}

/**
 * Get the Table object from the self passed in.
 */
Table* menv::getTable(jobject self) {
	return (Table*) getPtr(self, 0L);
}

jclass menv::getClass(jobject val) {
	return env->GetObjectClass(val);
}

bool menv::equals(jobject a, jobject b) {
	if (a == 0L && b == 0L) return true;
	if (a == 0L || b == 0L) return false;

	jclass ac = getClass(a);
	jmethodID eq = getMethod(ac, "equals", "(Ljava/lang/Object;)Z");
	return env->CallBooleanMethod(a, eq, b);
}