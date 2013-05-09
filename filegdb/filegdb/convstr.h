#pragma once

#include "Stdafx.h"
#include <string>
#include <jni.h>
#define _CRT_SECURE_NO_WARNINGS 1
#include "jstring.h"

// Hold a reference to a const char* or a const wchar* and 
// allow conversion to a const wchar* or a const char*. 
// Intended for stack allocation and destruction
class convstr {
private:
	std::wstring wstr;
	std::string str;

	void cleanup() {
		wstr = L"";
		str = "";
	}
public:
	convstr(const char* cstr) {
		*this = cstr;
	}

	convstr(const wchar_t* wc) {
		*this = wc;
	}

	convstr(JNIEnv *env, jstring jstr) {
		assign(env, jstr);
	}

	~convstr() {
		cleanup();
	}

	convstr& operator=(const char* &rhs);
	
	convstr& operator=(const wchar_t* &rhs);
	
	convstr& operator=(const std::string &rhs);

	convstr& operator=(const std::wstring &rhs);

	void assign(JNIEnv *env, const jstring &javastr);

    std::wstring getWstr();

    std::string getStr();
};