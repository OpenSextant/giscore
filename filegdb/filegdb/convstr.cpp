#include "Stdafx.h"
#include "convstr.h"
#if defined __APPLE__ || defined __unix__
#include <stdlib.h>
#else
#include <mbstring.h>
#endif

std::wstring convstr::getWstr() {
	   if (wstr.length() == 0) {
           // If there are multibyte characters this will report more characters than there are, safe enough
           size_t len = str.length();
		   buf = new wchar_t[len+1];
		   size_t chars = mbstowcs((wchar_t *) buf, str.c_str(), len);
		   wstr.append((const wchar_t*) buf, chars);
	   }
	   return wstr;
}

std::string convstr::getStr() {
	   if (str.length() == 0) {
		   size_t len = wcslen(wstr.c_str());
		   buf = new char[2*(len+1)];
		   size_t chars = wcstombs((char*) buf, wstr.c_str(), len);
		   str.append((const char*) buf, chars);
	   }
	   return str;
}

