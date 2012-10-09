//
// GeodatabaseManagement.h
//

/*
  COPYRIGHT © 2011 ESRI
  TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
  Unpublished material - all rights reserved under the
  Copyright Laws of the United States and applicable international
  laws, treaties, and conventions.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts and Legal Services Department
  380 New York Street
  Redlands, California, 92373
  USA

  email: contracts@esri.com
*/

/// A set of functions for accessing, creating and deleting file geodatabases.
/// @file GeodatabaseManagement.h

#pragma once

#include <string>

#ifndef EXPORT_FILEGDB_API
# if defined linux || defined __APPLE__
#  define EXT_FILEGDB_API
# else
#  define EXT_FILEGDB_API _declspec(dllimport)
# endif
#else
# if defined linux || defined __APPLE__
#  define EXT_FILEGDB_API __attribute__((visibility("default")))
# else
#  define EXT_FILEGDB_API _declspec(dllexport)
# endif
#endif

#include "FileGDBCore.h"

namespace FileGDBAPI
{

class Geodatabase;

/// Creates a new 10.x file geodatabase in the specified location.
/// If the file geodatabase already exists a -2147220653 (The table already exists) error will be returned.
/// If the path is seriously in error, say pointing to the wrong drive, a -2147467259 (E_FAIL) error is returned.
/// @param[in]    path The location where the geodatabase should be created.
/// @param[out]   geodatabase A reference to the newly-created geodatabase.
/// @return       Error code indicating whether the method finished successfully.
EXT_FILEGDB_API fgdbError CreateGeodatabase(const std::wstring& path, Geodatabase& geodatabase);

/// Opens an existing 10.x file geodatabase.
/// If the path is incorrect a -2147024894 (The system cannot find the file specified) error will be returned. If the
/// release is pre-10.x a -2147220965 (This release of the GeoDatabase is either invalid or out of date) error will be returned.
/// @param[in]    path The path of the geodatabase.
/// @param[out]   geodatabase A reference to the opened geodatabase.
/// @return       Error code indicating whether the method finished successfully.
EXT_FILEGDB_API fgdbError OpenGeodatabase(const std::wstring& path, Geodatabase& geodatabase);

/// Closes an open file geodatabase.
/// @param[in]    geodatabase A reference to the geodatabase.
/// @return       Error code indicating whether the method finished successfully.
EXT_FILEGDB_API fgdbError CloseGeodatabase(Geodatabase& geodatabase);

/// Deletes a file geodatabase.
/// If the path is incorrect a -2147024894 (The system cannot find the file specified) error will be returned.
/// If another process has a lock on the geodatabase, a -2147220947 (Cannot acquire a lock) error will be returned.
/// If access is denied an E_FAIL is returned.
/// @param[in]    path The path of the geodatabase.
/// @return       Error code indicating whether the method finished successfully.
EXT_FILEGDB_API fgdbError DeleteGeodatabase(const std::wstring& path);

};  // namespace FileGDBAPI
