//
// Raster.h
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

class ByteArray;
class Row;

/// Provides read and write access to raster data.
class EXT_FILEGDB_API Raster
{
public:

  /// @name Data access
  //@{
  /// TODO: Write description.
  /// @param[out]   format TODO
  /// @returns      Error code integer indicating whether the method finished successfully.
  //fgdbError GetFormat(std::wstring& format);

  /// TODO: Write description.
  /// @param[in]    format TODO
  /// @returns      Error code integer indicating whether the method finished successfully.
  //fgdbError SetFormat(const std::wstring& format);

  /// TODO: Write description.
  /// @param[out]   bytes TODO
  /// @returns      Error code integer indicating whether the method finished successfully.
  //fgdbError GetBytes(ByteArray& bytes);

  /// TODO: Write description.
  /// @param[in]    bytes TODO
  /// @returns      Error code integer indicating whether the method finished successfully.
  //fgdbError SetBytes(const ByteArray& bytes);

  /// TODO: Write description.
  /// @param[out]   attributes TODO
  /// @returns      Error code integer indicating whether the method finished successfully.
  //fgdbError GetAttributes(Row attributes);

  /// TODO: Write description.
  /// @param[in]    attributes TODO
  /// @returns      Error code integer indicating whether the method finished successfully.
  //fgdbError SetAttributes(Row attributes);
  //@}

  /// @name Constructors and Destructors
  //@{
  /// The class constructor.
  Raster();

  /// The class destructor.
  ~Raster();
  //@}

private:

  Raster(const Raster&)             { }
  Raster& operator=(const Raster&)  { return *this; }
};

};  // namespace FileGDBAPI
