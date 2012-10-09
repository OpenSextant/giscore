//
// Table.h
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

class Datafile;
class AutoLock;

namespace FileGDBAPI
{

class EnumRows;
class Envelope;
class Row;
class Geodatabase;
class FieldInfo;
class IndexDef;
class FieldDef;

/// Provides methods to work with tables, such as querying and modifying both schema and data.
class EXT_FILEGDB_API Table
{
public:

  /// @name Schema
  //@{
  /// Gets the table's definition as an XML document.
  /// @param[out]   tableDef An XML document than defines the table's schema.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDefinition(std::string& tableDef) const;

  /// Gets the table's metadata as XML.
  /// @param[out]   documentation The table's metadata as XML.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDocumentation(std::string& documentation) const;

  /// Assigns metadata to the table.
  /// @param[in]    documentation An XML document that will be the table's metadata.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetDocumentation(const std::string& documentation);

  /// Return information about the fields in the table.
  /// @param[out]   fieldInfo The field information.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetFieldInformation(FieldInfo& fieldInfo) const;

  /// Returns an array of FieldDef objects of the table's field collection.
  /// @param[out]   fieldDefs An array of FieldDef objects containing a collection of field definitions.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetFields(std::vector<FieldDef>& fieldDefs) const;

  /// Adds a field to the table.
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// If you are adding an OBJECTID field and one already exists, a -2147219707 (The Fields collection contained multiple OID fields)
  /// error will be returned. Only one OBJECTID field is allowed.<br/>
  /// If you are adding an GLOBALID field and one already exists, a -2147219703 (The Fields collection contained multiple Global ID fields)
  /// error will be returned. Only one GLOBALID field is allowed.<br/>
  /// If you attempt to add a NOT NULLABLE field to a table already contains rows,
  /// a -2147219879 (The field is not nullable) will be returned. <br/>
  /// If the field already exists, a -2147219884 (The Field already exists) will be returned.<br/>
  /// <a href="esriFieldTypes.txt">FieldTypes</a>   <a href="Field.xml">XML</a>
  /// @param[in]    fieldDef An XML document defining the field's properties.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError AddField(const std::string& fieldDef);

  /// Adds a field to the table.
  /// @param[in]    fieldDef A FieldDef object defining the field's properties.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError AddField(const FieldDef& fieldDef);

  /// Modifies a field in the table.
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// @param[in]    fieldDef An XML document defining the field's properties.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError AlterField(const std::string& fieldDef);

  /// Deletes a field from the table.
  /// If the field does not exist, an -2147219885 (An expected Field was not found or could not be retrieved properly) error will be returned.<br/>
  /// @param[in]    fieldName The name of the field to delete.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError DeleteField(const std::wstring& fieldName);

  /// Returns an XML definition of the table's index collection.
  /// @param[out]   indexDefs An XML document containing a collection of index definitions.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetIndexes(std::vector<std::string>& indexDefs) const;

  /// Returns an array of IndexDef objects of the table's index collection.
  /// @param[out]   indexDefs An array of IndexDef objects containing a collection of index definitions.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetIndexes(std::vector<IndexDef>& indexDefs) const;

  /// Adds an index to the table.
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// <a href="Index.xml">XML</a>
  /// @param[in]    indexDef An XML document defining the index's properties.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError AddIndex(const std::string& indexDef);

  /// Adds an index to the table.
  /// @param[in]    indexDef An IndexDef object defining the index's properties.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError AddIndex(const IndexDef& indexDef);

  /// Deletes an index from the table.
  /// If the index is not found, an -2147219629 (The index was not found) error will be returned.<br/>
  /// @param[in]    indexName The name of the index to delete.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError DeleteIndex(const std::wstring& indexName);

  /// Creates a new subtype to the table.
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// <a href="SubType.xml">XML</a>
  /// @param[in]    subtypeDef An XML document defining the subtype's properties.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError CreateSubtype(const std::string& subtypeDef);

  /// Modifies an existing subtype of the table.
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// @param[in]    subtypeDef An XML document defining the subtype's properties.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError AlterSubtype(const std::string& subtypeDef);

  /// Deletes a subtype from the table.
  /// @param[in]    subtypeName The name of the subtype to delete.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError DeleteSubtype(const std::wstring& subtypeName);

  /// Enables subtypes on a table.
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// <a href="esriFieldTypes.txt">FieldTypes</a>   <a href="Field.xml">XML</a>
  /// @param[in]    subtypeFieldName The field to use as the subtype field.
  /// @param[in]    subtypeDef The field to use as the subtype Def.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError EnableSubtypes(const std::wstring& subtypeFieldName, const std::string& subtypeDef);

  /// Returns the default subtype code.
  /// @param[out]   defaultCode The table's default subtype code.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDefaultSubtypeCode(int& defaultCode) const;

  /// Sets the default subtype code.
  /// @param[out]   defaultCode The code to assign as the default subtype code.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError SetDefaultSubtypeCode(int defaultCode);

  /// Drops the table's subtypes.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError DisableSubtypes();
  //@}

  /// @name Data
  //@{
  /// Performs a spatial query (envelope intersects) on the table.
  /// @param[in]    subfields (Optional) The fields that should be fetched by the query's returned rows. Must
  /// include a comma delimited list of fields or a "*". Passing in blank will return a -2147220985 (An invalid SQL statement was used) error.
  /// @param[in]    whereClause (Optional) Attribute constraints to apply to the query.
  /// @param[in]    envelope The spatial extent of the query.
  /// @param[in]    recycling Indicates whether row memory should be recycled.
  /// @param[out]   rows The results of the query.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Search(const std::wstring& subfields, const std::wstring& whereClause, const Envelope& envelope, bool recycling, EnumRows& rows);

  /// Performs an attribute query on the table.
  /// @param[in]    subfields (Optional) The fields that should be fetched by the query's returned rows. Must
  /// include a comma delimited list of fields or a "*". A blank will return a -2147220985 (An invalid SQL statement was used) error.
  /// @param[in]    whereClause (Optional) Attribute constraints to apply to the query.
  /// @param[in]    recycling Indicates whether row memory should be recycled.
  /// @param[out]   rows The results of the query.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Search(const std::wstring& subfields, const std::wstring& whereClause, bool recycling, EnumRows& rows);

  /// Creates a new row in memory for the table.
  /// @param[out]   row The newly-created row.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError CreateRowObject(Row& row);

  /// Inserts a newly-created and populated row into the table. When bulk inserting rows use LoadOnlyMode and SetWriteLock\FreeWriteLock to improve performance.
  /// @param[in]    row The row to insert.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Insert(Row& row);

  /// Updates an existing row in the table.
  /// @param[in]    row The row to update.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Update(Row& row);

  /// Deletes a row from the table.
  /// @param[in]    row The row to delete.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Delete(Row& row);

  /// Indicates whether the table should be edited.
  /// @param[out]   isEditable True if the table can safely be edited.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError IsEditable(bool& isEditable);

  /// Returns the number of rows in the table.
  /// @param[out]   rowCount The number of rows in the table.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetRowCount(int& rowCount) const;

  /// Returns the extent of the feature class.
  /// If the table is not a feature class an error of 1 will be returned.
  /// @param[out]   extent The extent of the feature class.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetExtent(Envelope& extent) const;

  /// Sets a write lock on a table.
  /// This should be used when performing bulk updates and inserts. Otherwise a
  /// lock will be created for each update or insert. Should be followed by a call
  /// to FreeWriteLock.
  fgdbError SetWriteLock(void);

  /// Frees a write lock on a table.
  /// This should be used when performing bulk updates and inserts. Otherwise a
  /// lock will be created for each update or insert. Should be preceded by a call
  /// to SetWriteLock.
  fgdbError FreeWriteLock(void);

  /// Begin or End load only mode.
  /// @param[in]   loadOnlyMode true to begin LoadOnlyMode, false to end.
  fgdbError LoadOnlyMode(bool loadOnlyMode);
  //@}

  /// @name Constructors and destructors
  //@{
  /// The class constructor.
  Table();

  /// The class destructor.
  ~Table();
  //@}

private:

  /// @cond PRIVATE
  fgdbError SetupTable(const std::wstring& path, Geodatabase* pGeodatabase, Datafile* pDatafile);
  bool      IsSetup() const;

#pragma warning(push)
#pragma warning(disable : 4251)

  Geodatabase*    m_pGeodatabase;
  Datafile*       m_pDatafile;
  std::wstring    m_Path;
  AutoLock*       m_pWriteLock;
  int             m_isEditable;

#pragma warning(pop)

  friend class    Geodatabase;

  Table(const Table&)             { }
  Table& operator=(const Table&)  { return *this; }
  /// @endcond
};

};  // namespace FileGDBAPI
