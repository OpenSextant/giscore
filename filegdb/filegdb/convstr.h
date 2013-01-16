#pragma once

#include "Stdafx.h"
#include <string>
#include <jni.h>
#define _CRT_SECURE_NO_WARNINGS 1
#include <mbstring.h>
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
			delete buf;
		}
	}

	std::wstring getWstr() {		
		if (wstr.length() == 0) {
			size_t len = _mbslen((const unsigned char*) str.c_str());
			buf = new wchar_t[len+1];
			size_t chars = mbstowcs((wchar_t *) buf, str.c_str(), len);
			wstr.append((const wchar_t*) buf, chars);
		}
		return wstr;
	}

	std::string getStr() {
		if (str.length() == 0) {
			size_t len = wcslen(wstr.c_str());
			buf = new char[2*(len+1)];
			size_t chars = wcstombs((char*) buf, wstr.c_str(), len);
			str.append((const char*) buf, chars);
		}
		return str;
	}
};