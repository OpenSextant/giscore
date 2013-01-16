//
// FileGDBCore.h
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

typedef unsigned char  byte;
typedef int            int32;
typedef unsigned int   uint32;
typedef short          int16;
typedef unsigned short uint16;

typedef int            fgdbError;

#if !defined (SUCCEEDED)
  #define SUCCEEDED(result)                   ((fgdbError)(result) >= 0)
#endif

#if !defined (FAILED)
  #define FAILED(result)                      ((fgdbError)(result) < 0)
#endif

#if !defined (S_OK)
  #define S_OK                                ((fgdbError)0x00000000)
#endif

#if !defined (S_FALSE)
  #define S_FALSE                             ((fgdbError)0x00000001)
#endif

#if !defined (E_FAIL)
  #define E_FAIL                              ((fgdbError)0x80004005)
#endif

#if !defined (E_INVALIDARG)
  #define E_INVALIDARG                        ((fgdbError)0x80070057)
#endif

#if !defined (E_NOTIMPL)
  #define E_NOTIMPL                           ((fgdbError)0x80004001)
#endif

#if !defined (E_OUTOFMEMORY)
  #define E_OUTOFMEMORY                       ((fgdbError)0x8007000E)
#endif

#if !defined (E_POINTER)
  #define E_POINTER                           ((fgdbError)0x80004003)
#endif

#if !defined (E_NOINTERFACE)
  #define E_NOINTERFACE                       ((fgdbError)0x80004002)
#endif

#if !defined (E_UNEXPECTED)
  #define E_UNEXPECTED                        ((fgdbError)0x8000FFFF)
#endif

#if !defined (E_ACCESSDENIED)
  #define E_ACCESSDENIED                      ((fgdbError)0x80070005)
#endif

#define FGDB_E_FILE_NOT_FOUND                 ((fgdbError)0x80070002)
#define FGDB_E_PATH_NOT_FOUND                 ((fgdbError)0x80070003)
#define FGDB_E_ACCESS_DENIED                  ((fgdbError)0x80070005)
#define FGDB_E_CANNOT_MAKE                    ((fgdbError)0x80070052)
#define FGDB_E_SEEK                           ((fgdbError)0x80070019)
#define FGDB_E_INVALID_HANDLE                 ((fgdbError)0x80070006)
#define FGDB_E_FILE_EXISTS                    ((fgdbError)0x80070050)
#define FGDB_E_HANDLE_DISK_FULL               ((fgdbError)0x80070027)

#define FGDB_E_NO_PERMISSION                  ((fgdbError)-2147220987)
#define FGDB_E_NOT_SUPPORTED                  ((fgdbError)-2147220989)
#define FGDB_E_FILE_IO                        ((fgdbError)-2147220975)
#define FGDB_E_FIELD_NOT_FOUND                ((fgdbError)-2147219885)
#define FGDB_E_FIELD_INVALID_NAME             ((fgdbError)-2147219886)
#define FGDB_E_FIELD_NOT_NULLABLE             ((fgdbError)-2147219879)
#define FGDB_E_FIELD_NOT_EDITABLE             ((fgdbError)-2147219880)
#define FGDB_E_FIELD_INVALID_TYPE             ((fgdbError)-2147219883)
#define FGDB_E_FIELD_ALREADY_EXISTS           ((fgdbError)-2147219884)
#define FGDB_E_FIELDS_MULTIPLE_OIDS           ((fgdbError)-2147219707)
#define FGDB_E_FIELDS_MULTIPLE_GEOMETRIES     ((fgdbError)-2147219706)
#define FGDB_E_FIELDS_MULTIPLE_RASTERS        ((fgdbError)-2147219704)
#define FGDB_E_FIELDS_MULTIPLE_GLOBALIDS      ((fgdbError)-2147219703)
#define FGDB_E_FIELDS_EMPTY                   ((fgdbError)-2147219702)
#define FGDB_E_FIELD_CANNOT_DELETE_REQUIRED_FIELD ((fgdbError)-2147219877)
#define FGDB_E_TABLE_INVALID_NAME             ((fgdbError)-2147220654)
#define FGDB_E_TABLE_NOT_FOUND                ((fgdbError)-2147220655)
#define FGDB_E_TABLE_ALREADY_EXISTS           ((fgdbError)-2147220653)
#define FGDB_E_TABLE_NO_OID_FIELD             ((fgdbError)-2147220652)
#define FGDB_E_DATASET_INVALID_NAME           ((fgdbError)-2147220734)
#define FGDB_E_DATASET_ALREADY_EXISTS         ((fgdbError)-2147220733)
#define FGDB_E_INDEX_NOT_FOUND                ((fgdbError)-2147219629)
#define FGDB_E_GRID_SIZE_TOO_SMALL            ((fgdbError)-2147216881)
#define FGDB_E_INVALID_GRID_SIZE              ((fgdbError)-2147216894)
#define FGDB_E_NO_SPATIALREF                  ((fgdbError)-2147216889)
#define FGDB_E_INVALID_SQL                    ((fgdbError)-2147220985)
#define FGDB_E_XML_PARSE_ERROR                ((fgdbError)-2147215103)
#define FGDB_E_SPATIALFILTER_INVALID_GEOMETRY ((fgdbError)-2147216814)
#define FGDB_E_SPATIALREF_INVALID             ((fgdbError)-2147216892)
#define FGDB_E_WORKSPACE_ALREADY_EXISTS       ((fgdbError)-2147220902)
#define FGDB_E_INVALID_RELEASE                ((fgdbError)-2147220965)
#define FGDB_E_LOCK_CONFLICT                  ((fgdbError)-2147220947)
#define FGDB_E_SCHEMA_LOCK_CONFLICT           ((fgdbError)-2147220970)
#define FGDB_E_OBJECT_NOT_LOCKED              ((fgdbError)-2147220968)
#define FGDB_E_WORKSPACE_READONLY             ((fgdbError)-2147220893)
#define FGDB_E_CANNOT_EDIT_COMPRESSED_DATASET ((fgdbError)-2147220113)
#define FGDB_E_CANNOT_UPDATE_COMPRESSED_DATASET ((fgdbError)-2147220112)
#define FGDB_E_COMPRESSED_DATASET_NOT_INSTALLED ((fgdbError)-2147220109)
#define FGDB_E_NEGATIVE_FID                   ((fgdbError)-2147220945)
#define FGDB_E_FEATURE_VALUE_TYPE_MISMATCH    ((fgdbError)-2147217395)
#define FGDB_E_ROW_BAD_VALUE                  ((fgdbError)-2147219115)
#define FGDB_E_ROW_ALREADY_EXISTS             ((fgdbError)-2147219114)
#define FGDB_E_ROW_NOT_FOUND                  ((fgdbError)-2147219118)
#define FGDB_E_TABLE_SIZE_EXCEEDED            ((fgdbError)-2147220951)
#define FGDB_E_NOT_EDITING                    ((fgdbError)-2147220134)
#define FGDB_E_EDIT_OPERATION_REQUIRED        ((fgdbError)-2147220957)
#define FGDB_E_CANNOT_CHANGE_ITEM_VISIBILITY  ((fgdbError)-2147211770)
#define FGDB_E_ITEM_NOT_FOUND                 ((fgdbError)-2147211775)
#define FGDB_E_ITEM_RELATIONSHIP_NOT_FOUND    ((fgdbError)-2147211762)
#define FGDB_E_DOMAIN_NOT_FOUND               ((fgdbError)-2147209215)
#define FGDB_E_DOMAIN_NAME_ALREADY_EXISTS     ((fgdbError)-2147209212)
#define FGDB_E_DOMAIN_INVALID_NAME            ((fgdbError)-2147209194)

namespace FileGDBAPI
{

enum FieldType
{
  fieldTypeSmallInteger   =  0,
  fieldTypeInteger        =  1,
  fieldTypeSingle         =  2,
  fieldTypeDouble         =  3,
  fieldTypeString         =  4,
  fieldTypeDate           =  5,
  fieldTypeOID            =  6,
  fieldTypeGeometry       =  7,
  fieldTypeBlob           =  8,
  fieldTypeRaster         =  9,
  fieldTypeGUID           = 10,
  fieldTypeGlobalID       = 11,
  fieldTypeXML            = 12,
};

enum ShapeType
{
  shapeNull               =  0,
  shapePoint              =  1,
  shapePointM             = 21,
  shapePointZM            = 11,
  shapePointZ             =  9,
  shapeMultipoint         =  8,
  shapeMultipointM        = 28,
  shapeMultipointZM       = 18,
  shapeMultipointZ        = 20,
  shapePolyline           =  3,
  shapePolylineM          = 23,
  shapePolylineZM         = 13,
  shapePolylineZ          = 10,
  shapePolygon            =  5,
  shapePolygonM           = 25,
  shapePolygonZM          = 15,
  shapePolygonZ           = 19,
  shapeMultiPatchM        = 31,
  shapeMultiPatch         = 32,
  shapeGeneralPolyline    = 50,
  shapeGeneralPolygon     = 51,
  shapeGeneralPoint       = 52,
  shapeGeneralMultipoint  = 53,
  shapeGeneralMultiPatch  = 54,
};

enum ShapeModifiers
{
  shapeHasZs                  = 0x80000000,
  shapeHasMs                  = 1073741824,
  shapeHasCurves              = 536870912,
  shapeHasIDs                 = 268435456,
  shapeHasNormals             = 134217728,
  shapeHasTextures            = 67108864,
  shapeHasPartIDs             = 33554432,
  shapeHasMaterials           = 16777216,
  shapeIsCompressed           = 8388608,
  shapeModifierMask           = -16777216,
  shapeMultiPatchModifierMask = 15728640,
  shapeBasicTypeMask          = 255,
  shapeBasicModifierMask      = -1073741824,
  shapeNonBasicModifierMask   = 1056964608,
  shapeExtendedModifierMask   = -587202560
};

enum GeometryType
{
  geometryNull            = 0,
  geometryPoint           = 1,
  geometryMultipoint      = 2,
  geometryPolyline        = 3,
  geometryPolygon         = 4,
  geometryMultiPatch      = 9,
};

enum CurveType
{
  curveTypeCircularArc    = 1,
  curveTypeBezier         = 4,
  curveTypeEllipticArc    = 5,
};

};  // namespace FileGDBAPI
