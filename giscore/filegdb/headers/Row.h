//
// Row.h
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
#include <vector>

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

struct IFields;
struct FieldValue;
class  FieldValues;

namespace FileGDBAPI
{

class ShapeBuffer;
class ByteArray;
class Raster;
class EnumRows;
class FieldInfo;
class FieldDef;
class Guid;

/// Provides methods to get and set data on table rows.
class EXT_FILEGDB_API Row
{
public:

  /// @name Getters and setters
  //@{
  /// Indicates whether the specified field contains a null value.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field
  /// was not found or could not be retrieved properly).
  /// @param[in]    field The field to check for a null value.
  /// @param[out]   isNull Indicates whether the specified field contains a null value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError IsNull(const std::wstring& field, bool& isNull) const;

  /// Assigns a null value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not
  /// found or could not be retrieved properly).
  /// If the field is not nullable, this will fail with an error code of -2147219879 (The field is not nullable).
  /// @param[in]    field The field to set as null.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetNull(const std::wstring& field);

  /// Gets the row's Object ID.
  /// If the row's table does not have an Object ID column, this will fail with an error code of -2147219885
  /// (An expected Field was not found or could not be retrieved properly).
  /// @param[out]   objectID The row's Object ID.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetOID(int32& objectID) const;

  /// Gets the row's Global ID.
  /// If the row's table does not have an Global ID column, this will fail with an error code of -2147219885
  /// (An expected Field was not found or could not be retrieved properly).
  /// @param[out]   globalID The row's Global ID.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetGlobalID(Guid& globalID) const;

  /// Gets the row's geometry. The buffer is a shape record as defined in the ESRI Shapefile Technical
  /// Description, which is included in the documentation. The Editing and Querying samples write and
  /// read a point shape buffer. Other shape types can be read based on the shapefile specification.
  /// If the row's table does not have a geometry column, this will fail with an error code of -2147219885
  /// (An expected Field was not found or could not be retrieved properly).
  /// @param[out]   shapeBuffer The row's geometry.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetGeometry(ShapeBuffer& shapeBuffer) const;

  /// Sets the row's geometry. The buffer is a shape record as defined in the ESRI Shapefile Technical
  /// Description, which is included in the documentation. The Editing and Querying samples write and
  /// read a point shape buffer. If the row's table does not have a geometry column, this will fail with
  /// an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the geometry has an invalid geometry type, this will fail with an error code of E_FAIL.
  /// @param[in]    shapeBuffer The geometry to assign to the row.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetGeometry(const ShapeBuffer& shapeBuffer);

  /// Gets a short value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field is has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetShort(const std::wstring& field, short& value) const;

  /// Assigns a short value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[out]   value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetShort(const std::wstring& field, short value);

  /// Gets an integer value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetInteger(const std::wstring& field, int32& value) const;

  /// Assigns an integer value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]    value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetInteger(const std::wstring& field, int32 value);

  /// Gets a float value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetFloat(const std::wstring& field, float& value) const;

  /// Assigns a float value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]    value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetFloat(const std::wstring& field, float value);

  /// Gets a double value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDouble(const std::wstring& field, double& value) const;

  /// Assigns a double value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]    value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetDouble(const std::wstring& field, double value);

  /// Gets a date/time value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDate(const std::wstring& field, struct tm& value) const;

  /// Assigns a date/time value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to get the value from.
  /// @param[in]    value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetDate(const std::wstring& field, const struct tm& value);

  /// Gets a string value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetString(const std::wstring& field, std::wstring& value) const;

  /// Assigns a string value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]   value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetString(const std::wstring& field, const std::wstring& value);

  /// Gets a GUID value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetGUID(const std::wstring& field, Guid& value) const;

  /// Assigns a GUID value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]    value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetGUID(const std::wstring& field, const Guid& value);

  /// Gets an XML value from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   value The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetXML(const std::wstring& field, std::string& value) const;

  /// Assigns an XML value to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]    value The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetXML(const std::wstring& field, const std::string& value);

  /// Gets a raster from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   raster The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetRaster(const std::wstring& field, Raster& raster) const;

  /// Assigns a raster to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]    raster The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetRaster(const std::wstring& field, const Raster& raster);

  /// Gets a byte array from the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// If the field contains a null value, this will fail with an error code of E_FAIL.
  /// @param[in]    field The name of the field to get the value from.
  /// @param[out]   binaryBuf The field's value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetBinary(const std::wstring& field, ByteArray& binaryBuf) const;

  /// Assigns a byte array to the specified field.
  /// If the field does not exist, this will fail with an error code of -2147219885 (An expected Field was not found or could not be retrieved properly).
  /// If the field has an incompatible data type, this will fail with an error code of -2147217395 (The value type is incompatible with the field type).
  /// @param[in]    field The name of the field to assign the value to.
  /// @param[in]    binaryBuf The value to assign to the field.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetBinary(const std::wstring& field, const ByteArray& binaryBuf);
  //@}

  /// @name Field properties
  //@{
  /// Return information about the fields in the row.
  /// @param[out]   fieldInfo The field information.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetFieldInformation(FieldInfo& fieldInfo) const;

  /// Returns an array of FieldDef objects of the table's field collection.
  /// @param[out]   fieldDefs An array of FieldDef objects containing a collection of field definitions.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetFields(std::vector<FieldDef>& fieldDefs) const;
  //@}

  /// @name Constructors and Destructors
  //@{
  /// The class constructor.
  Row();

  /// The class destructor.
  ~Row();
  //@}

private:

  /// @cond PRIVATE
  fgdbError SetupRow(IFields* pFields, FieldValues* pFieldValues, bool takeOwnership);
  bool IsSetup() const;

  fgdbError FindField(const std::wstring& field, int& fieldNumber, FieldType& fieldType) const;
  fgdbError GetFieldIsNullable(int fieldNumber, bool& isNullable) const;

  fgdbError SetOID(int32 objectID);
  fgdbError SetGlobalID(const Guid& globalID);

  int                 m_numFields;
  long*               m_pFieldMap;
  FieldValue*         m_pValues;
  int                 m_oidFieldNumber;
  int                 m_globalIDFieldNumber;
  int                 m_shpFieldNumber;
  IFields*            m_pFields;

  FieldValues*        m_pFieldValues;
  bool                m_ownsFieldValues;

  friend class EnumRows;
  friend class Table;

  Row(const Row&)             { }
  Row& operator=(const Row&)  { return *this; }
  /// @endcond
};

};  // namespace FileGDBAPI
