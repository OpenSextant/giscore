#pragma once
#include <jni.h>

class jstring_holder {
private:
	JNIEnv* env;
	jstring jstr;
	const char* cstring;

public:
	jstring_holder(JNIEnv *e, jstring orig) {
		env = e;
		jstr = orig;
		cstring = env->GetStringUTFChars(jstr, 0);
	}

	~jstring_holder() {
		env->ReleaseStringUTFChars(jstr, cstring);
	}

	const char* getStr() {
		return cstring;
	}
};