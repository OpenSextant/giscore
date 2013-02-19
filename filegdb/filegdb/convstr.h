#pragma once

#include "Stdafx.h"
#include <string>
#include <jni.h>
#define _CRT_SECURE_NO_WARNINGS 1
#ifdef __unix__
#include <stdlib.h>
size_t _mbslen(const unsigned char* string);
#else
#include <mbstring.h>
#endif
#include "jstring.h"

// Hold a reference to a const char* or a const wchar* and 
// allow conversion to a const wchar* or a const char*. 
// Intended for stack allocation and destruction
class convstr {
private:
	void* buf;
	std::wstring wstr;
	std::string str;

public:
	convstr(const char* cstr) {
		buf = 0L;
		str.append(cstr);
	}

	convstr(const wchar_t* wc) {
		buf = 0L;
		wstr.append(wc);
	}

	convstr(JNIEnv *env, jstring jstr) {
		buf = 0L;
		jstring_holder strchars(env, jstr);
		str.append(strchars.getStr());
	}

	~convstr() {
		if (buf != 0L) {
			delete (char*) buf;
		}
	}

        std::wstring getWstr();

        std::string getStr();
};