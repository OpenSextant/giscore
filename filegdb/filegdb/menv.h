#pragma once

#include "Stdafx.h"
#include <Windows.h>
#include <WinBase.h>
#include <vector>
#include <iostream>


using namespace std;
using namespace FileGDBAPI;

class jni_check : public exception {
public:
    jni_check() {
        
    };
};

class menv {
private:
	bool unlock;
	JNIEnv *env;

protected:
#if defined(_MSC_VER)
	static HANDLE mutex; 
#else
#endif

public:
	/**
	 * Ctor
	 */
	menv(JNIEnv *e) {
#if defined(_MSC_VER)
		// DWORD tid = GetCurrentThreadId();
		// cerr << "Taking lock on tid: " << tid << "\n"; 
		DWORD result = WaitForSingleObject(mutex, INFINITE);
		if (result == WAIT_OBJECT_0) {
			unlock = true;
			// cerr << "Locked tid: " << tid  << "\n";
		} else {
			unlock = false;
			if (result == WAIT_FAILED) {
				// result = GetLastError();	
			}
			// cerr << "Lock failed on tid: " << tid << " result: " << result << "\n";
			throw jni_check();
		}
#else
		// pthreads code here
#endif
		env = e;
	}

	~menv() {
#if defined(_MSC_VER)
		if (unlock) {
			// DWORD tid = GetCurrentThreadId();
			// cerr << "Unlocking tid: " << tid << "\n";
			if (ReleaseMutex(mutex) == 0) {
				// cerr << "Unlock failed on tid: " << tid << "\n";
				throw jni_check(); 
			}
			// cerr << "Unlocked tid: " << tid << "\n";
		}
#else
		// pthreads code here
#endif		
	}

	static void initialize() {
#if defined(_MSC_VER)
		mutex = CreateMutex(NULL, FALSE, NULL);
#else
		// pthreads code here
#endif
	}

	Row* getRow(jobject self);

	Table* getTable(jobject self);

	Geodatabase* getGeodatabase(jobject self);

	jclass findClass(const char* classname);

	/**
	 * Throw an exception specific to this library code. You still need to return 
	 * since this doesn't create a transfer of control. 
	 * @param env
	 * @param message
	 */
	void throwException(const char* message);

	/**
	 * Handle exceptions from JNI operations
	 */
	void checkAndThrow() throw(jni_check);


	jfieldID getField(jclass clazz, const char* fieldname, const char* sig);

	jmethodID getMethod(jclass clazz, const char* methodname, const char* sig);

	jmethodID getCtor(jclass clazz, const char* sig);

	void esriCheckedCall(fgdbError err, const char* message) throw(jni_check);

	/**
	 * Get the ptr from the self passed in.
	 */
	void* getPtr(jobject self, long ctor());

	/**
	 * Set the ptr for the passed instance
	 */
	void setPtr(jobject self, void* ptr);

	void* getLongFieldValue(jobject self, const char* classname, const char* fieldname);

	/**
	 * Allocate a new java object using the default ctor and assign the given
	 * void ptr to the ptr instance variable. Assumes the class is a subclass
	 * of GDB.
	 *
	 * @param env The JNI environment
	 * @param clazz The Java package and class, may use either . or / as separators
	 * @param ptr a void pointer to an allocated object to hold a pointer to in the long in the java object
	 */
	jobject newObject(const char* clazz, void* ptr);

	jobject newBoolean(bool val);

	jobject newShort(short val);

	jobject newInteger(int val);

	jobject newLong(long val);

	jobject newFloat(float val);

	jobject newDouble(double val);

	bool getBoolean(jobject val);

	short getShort(jobject val);

	int getInteger(jobject val);

	long getLong(jobject val);

	float getFloat(jobject val);

	double getDouble(jobject val);

	string getString(jstring val);

	wstring getWString(jstring val);

	jobjectArray processJStringArray(vector<wstring> strvector);

	jobjectArray processJStringArray2(vector<string> strvector);

	jclass getClass(jobject val);

	bool equals(jobject a, jobject b);
};