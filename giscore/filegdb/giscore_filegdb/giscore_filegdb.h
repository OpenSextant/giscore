// giscore_filegdb.h : main header file for the giscore_filegdb DLL
//

#pragma once

#ifndef __AFXWIN_H__
	#error "include 'stdafx.h' before including this file for PCH"
#endif

#include "resource.h"		// main symbols


// Cgiscore_filegdbApp
// See giscore_filegdb.cpp for the implementation of this class
//

class Cgiscore_filegdbApp : public CWinApp
{
public:
	Cgiscore_filegdbApp();

// Overrides
public:
	virtual BOOL InitInstance();

	DECLARE_MESSAGE_MAP()
};
