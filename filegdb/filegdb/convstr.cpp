#include "Stdafx.h"
#include "convstr.h"
#if defined __APPLE__ || defined __unix__
#include <stdlib.h>
#else
// Because Microsoft defines this badly, not sure why it is screwed up
#define MB_CUR_MAX ___mb_cur_max_func()
#include <mbstring.h>
#endif
#include <jni.h>

#pragma warning( disable : 4996 ) // We know what we're doing, disables warning on mbstowcs, wcstombs functions

std::wstring convstr::getWstr() {
	   if (wstr.length() == 0) {
		   size_t slen = str.length();
		   size_t max = MB_CUR_MAX;
		   wchar_t wc[5];
		   const char* istr = str.data();
		   mbtowc(NULL, NULL, 0); // reset
		   for(size_t i = 0; i < slen; ) {
			   const char* seg = &istr[i];
			   size_t clen = mblen(seg, max);
			   int r = mbtowc(wc, seg, max);
			   i += clen;
			   if ( r > 0 ) {
				   wstr.append(wc,r);
			   } else if ( r == 0 ) {
				   break;
			   }
		   }
	   }
	   return wstr;
}

std::string convstr::getStr() {
	   if (str.length() == 0) {
		   char buf[5];
		   wctomb(NULL, 0L);
		   size_t len = wstr.length();
		   for(size_t i = 0; i < len; i++) {
			   int r = wctomb(buf, wstr.at(i));
			   if (r > 0) str.append(buf,r);
		   }
	   }
	   return str;
}

convstr& convstr::operator=(const char* &rhs) {
	cleanup();
	str.append(rhs);
	return *this;
}

convstr& convstr::operator=(const wchar_t* &rhs) {
	cleanup();
	wstr.append(rhs);
	return *this;
}

convstr& convstr::operator=(const std::string &rhs) {
	cleanup();
	str.append(rhs);
	return *this;
}

convstr& convstr::operator=(const std::wstring &rhs) {
	cleanup();
	wstr.append(rhs);
	return *this;
}

void convstr::assign(JNIEnv *env, const jstring &javastr) {
	cleanup();
	const char* cstr = env->GetStringUTFChars(javastr, 0);
	str.append(cstr);
	env->ReleaseStringUTFChars(javastr, cstr);
}