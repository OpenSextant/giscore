//
// Geodatabase.h
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
#include <map>

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

class Catalog;
class CatalogRef;

namespace FileGDBAPI
{

class EnumRows;
class Row;
class Table;

/// A class representing a File Geodatabase.
class EXT_FILEGDB_API Geodatabase
{
public:

  /// @name Schema browsing
  //@{
  /// Gets a list of the dataset types in the geodatabase.
  /// @param[out]   datasetTypes The dataset types in the geodatabase.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDatasetTypes(std::vector<std::wstring>& datasetTypes) const;

  /// Gets a list of relationship types in the geodatabase.
  /// @param[out]   relationshipTypes The relationship types in the geodatabase.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDatasetRelationshipTypes(std::vector<std::wstring>& relationshipTypes) const;

  /// Gets the child datasets for a particular dataset, if any.
  /// If a non-existent path is provided, a -2147211775 (The item was not found) error will be returned.
  /// @param[in]    parentPath The dataset to find the children of, e.g. "\usa".
  /// @param[in]    datasetType The child dataset type as a wstring, e.g. "Feature Class". Passing in
  /// an empty string will return all child datasets. <a href="ItemTypes.txt">DatasetType</a>
  /// @param[out]   childDatasets The children of the parent dataset, if any.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetChildDatasets(const std::wstring& parentPath, const std::wstring& datasetType, std::vector<std::wstring>& childDatasets) const;

  /// Gets the related datasets for a particular dataset, if any.
  /// If a non-existent path is provided, a -2147211775 (The item was not found) error will be returned.
  /// @param[in]    path The path of the dataset to find related datasets for, e.g. "\usa\streets_topology".
  /// @param[in]    relType The relationship type to filter return values with, e.g. "DatasetInFeatureDataset". Passing in
  /// an empty string will return all related datasets. <a href="RelationshipTypes.txt">RelationshipType</a>
  /// @param[in]    datasetType The type of the dataset to find related datasets for. <a href="ItemTypes.txt">DatasetType</a>
  /// @param[out]   relatedDatasets The origin dataset's related datasets, if any.
  /// @result       Error code indicating whether the method finished successfully.
  fgdbError GetRelatedDatasets(const std::wstring& path, const std::wstring& relType, const std::wstring& datasetType, std::vector<std::wstring>& relatedDatasets) const;
  //@}

  /// @name Schema definition
  //@{
  /// Gets the definition of a dataset as an XML document.
  /// If the dataset does not exist, this will fail with an error code of -2147220655 (The table was not found).
  /// If a non-existent path is provided, a -2147211775 (The item was not found) error will be returned.
  /// @param[in]    path The requested dataset's path. e.g. "\usa\city_anno"
  /// @param[in]    datasetType The requested dataset's type as a string, e.g. "Table".  <a href="ItemTypes.txt">DatasetType</a>
  /// @param[out]   datasetDef The dataset's definition as an XML document.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDatasetDefinition(const std::wstring& path, const std::wstring& datasetType, std::string& datasetDef) const;

  /// Gets the definitions of child datasets as a collection of XML documents.
  /// If a non-existent path is provided, a -2147211775 (The item was not found) error will be returned.
  /// @param[in]    parentPath The parent dataset's path, e.g. "\usa".
  /// @param[in]    datasetType The parent dataset's type as a string, e.g. "Feature Dataset". Passing in
  /// an empty string will return all child datasets. <a href="ItemTypes.txt">DatasetType</a>
  /// @param[out]   childDatasetDefs A collection of child dataset definitions, if any.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetChildDatasetDefinitions(const std::wstring& parentPath, const std::wstring& datasetType, std::vector<std::string>& childDatasetDefs) const;

  /// Gets the definitions of related datasets as a collection of XML documents.
  /// If a non-existent path is provided, a -2147211775 (The item was not found) error will be returned.
  /// @param[in]    path The origin dataset's path, e.g. "\usa\streets_topology"
  /// @param[in]    relType The relationship type to filter return values with, e.g. "DatasetInFeatureDataset". <a href="RelationshipTypes.txt">RelationshipType</a>
  /// @param[in]    datasetType The origin dataset's type as a string, e.g. "Relationship Class". Passing in
  /// an empty string will return all related datasets. <a href="ItemTypes.txt">DatasetType</a>
  /// @param[out]   relatedDatasetDefs A collection of related dataset definitions, if any.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetRelatedDatasetDefinitions(const std::wstring& path, const std::wstring& relType, const std::wstring& datasetType, std::vector<std::string>& relatedDatasetDefs) const;

  /// Gets the metadata of a dataset as XML.
  /// If a non-existent path is provided, a -2147211775 (The item was not found) error will be returned.
  /// @param[in]    path The requested dataset's path. e.g. "\address_list"
  /// @param[in]    datasetType The requested dataset's type as a string, e.g. "Table". <a href="ItemTypes.txt">DatasetType</a>
  /// @param[out]   documentation The dataset's metadata as XML.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDatasetDocumentation(const std::wstring& path, const std::wstring& datasetType, std::string& documentation) const;
  //@}

  /// @name Datasets
  //@{
  /// Creates a new feature dataset.
  /// If the feature dataset already exists, a -2147220733 (The dataset already exists) error will be returned.<br/>
  /// If the feature dataset name is missing from the XML, a -2147220645 (INVALID_NAME) error will be returned.<br/>
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// <a href="FeatureDataset.xml">XML</a>
  /// <br><br>
  /// @param[in]    featureDatasetDef The XML definition of the feature dataset to be created.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError CreateFeatureDataset(const std::string& featureDatasetDef);

  /// Creates a new table. This can either be a table or a feature class. If a geometry is to support Zs or Ms (measures), HasZ
  /// and or HasM must be set to true in the GeometryDef in the XML. The ZOrigin, MOrigin, ZScale and MScale
  /// must also be set in the SpatialReferences in the XML. These do not default.
  /// See the samlples\XMLsamples\FC_GCS_LineMin.xml for an example. Domain definitions in the table XML definition 
  /// will be ignored. Use Table.AlterField to assign a domain. <br/>
  /// If the table already exists, a -2147220653 (The table already exists) error will be returned.<br/>
  /// If the table name is missing from the XML, a -2147220654 (The table name is invalid) error will be returned.<br/>
  /// If the XML is not UTF-8 encoded, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// <br><a href="Table.xml">XML-Table</a><br><a href="FC_GCS_Line.xml">XML-Feature Class</a>
  /// <br><a href="FC_GCS_LineMin.xml">XML-Feature Class with the minimum spatial reference definition</a>
  /// <br><a href="FeatureClassInAFeatureDataset.xml">XML-Feature Class to be created in a feature dataset</a>
  /// @param[in]    tableDef The XML definition of the table to be created.
  /// @param[in]    parent The location where the table will be created. Pass an empty string if you want to
  /// create a table or feature class at the root. If you want to create a feature class in an existing feature
  /// dataset use the path "\USA".
  /// @param[out]   table An Table instance for the newly created table.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError CreateTable(const std::string& tableDef, const std::wstring& parent, Table& table);

  /// Opens a table. This can also be used to open attributed and M:N relationship class tables.
  /// If the table does not exist, a -2147220655 (The table was not found) error will be returned.
  /// Attempting to open a compressed file is not supported and a -2147220109 (FileGDB compression is
  /// not installed.) error will be returned.
  /// @param[in]    path The path of the table to open. Opening a table or feature class at
  /// the root make sure to include "\". If opening a feature class in a feature dataset include
  /// the feature dataset name in the path "\USA\counties".
  /// @param[out]   table An Table instance for the opened table.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError OpenTable(const std::wstring& path, Table& table);

  /// Closes a table that has been previously created or opened.
  /// @param[in]    table The table to close.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError CloseTable(Table& table);

  /// Renames a dataset.
  /// @param[in]    path The path of the dataset, e.g. "\Landbase\Parcels".
  /// @param[in]    datasetType The requested dataset's type as a string, e.g. "Table".  <a href="ItemTypes.txt">DatasetType</a>
  /// @param[in]    newName The name to apply to the dataset, e.g. "Parcels2".
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Rename(const std::wstring& path,  const std::wstring& datasetType, const std::wstring& newName);

  /// Moves a dataset from one container to another.
  /// @param[in]    path The path of the dataset to move, e.g. "\Landbase\Parcels".
  /// @param[in]    newParentPath The path of the container the dataset will be moved to, e.g. "\LandUse".
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Move(const std::wstring& path, const std::wstring& newParentPath);

  /// Deletes a dataset.
  /// If a the dataset does not exist, this will fail with an error code of -2147219118 (A requested row object could not be located).<br/>
  /// If you do not have delete access to the dataset, this will fail with an error code of E_FAIL.<br/>
  /// @param[in]    path The path of the dataset to delete, e.g. "\Owners".
  /// @param[in]    datasetType The requested dataset's type as a string, e.g. "Table". <a href="ItemTypes.txt">DatasetType</a>
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError Delete(const std::wstring& path, const std::wstring& datasetType);
  //@}

  /// @name Domains
  //@{

  /// Gets the names of all domains, if any.
  /// @param[out]   domainNames The domains.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDomains(std::vector<std::wstring>& domainNames) const;

  /// Creates a domain.
  /// If the XML is not UTF-8, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// If the domain name already exists, a -2147209212 (Domain name already in use) error will be returned.<br/>
  /// <a href="CodedValueDomain.xml">XML - Coded Value Domain</a>   <a href="RangeDomain.xml">XML - Range Domain</a>
  /// @param[in]    domainDef The XML definition of the domain to be created.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError CreateDomain(const std::string& domainDef);

  /// Modifies the properties of an existing domain.
  /// If the XML is not UTF-8, create will fail with an error code of -2147024809 (Invalid function arguments).<br/>
  /// @param[in]    domainDef The modified XML definition of the domain.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError AlterDomain(const std::string& domainDef);

  /// Deletes the specified domain.
  ///If the domain does not exist, this will fail with an error code of -2147209215 (The domain was not found).<br/>
  /// @param[in]    domainName The name of the domain to delete.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError DeleteDomain(const std::wstring& domainName);

  /// Gets the definition of the specified domain.
  /// @param[in]    domainName The name of the domain.
  /// @param[out]   domainDef The XML definition of the domain.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetDomainDefinition(const std::wstring& domainName, std::string& domainDef) const;
  //@}

  /// @name SQL
  //@{
  /// Gets the query name (the name to use in SQL statements) of a table based on its path.
  /// @param[in]    path The path of the dataset that will be queried.
  /// @param[out]   queryName The name that should be used for the table in SQL statements.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError GetQueryName(const std::wstring& path, std::wstring& queryName) const;

  /// Executes a SQL statement on the geodatabase. This may or may not return a result set.
  /// If the SQL statement is invalid, an -2147220985 (An invalid SQL statement was used) error will be returned.<br/>
  /// @param[in]    sqlStmt The SQL statement to be executed.
  /// @param[in]    recycling Indicates whether the row enumerator should recycle memory.
  /// @param[out]   rows An enumerator of rows or a null value.
  /// @return       Error code indicating whether the method finished successfully.
  fgdbError ExecuteSQL(const std::wstring& sqlStmt, bool recycling, EnumRows& rows) const;
  //@}

  /// @name Constructors and Destructors
  //@{
  /// The class constructor.
  Geodatabase();

  /// The class destructor.
  ~Geodatabase();
  //@}

private:

  /// @cond PRIVATE
  fgdbError CreateGeodatabase(const std::wstring& path);
  fgdbError OpenGeodatabase(const std::wstring& path);
  fgdbError CloseGeodatabase();
  fgdbError DeleteGeodatabase();

  bool IsSetup() const;

  Catalog* m_pCatalog;

#pragma warning(push)
#pragma warning(disable : 4251)

  std::map<Table*, Table*> m_tableROT;

#pragma warning(pop)

  friend EXT_FILEGDB_API fgdbError CreateGeodatabase(const std::wstring& path, Geodatabase& geodatabase);
  friend EXT_FILEGDB_API fgdbError OpenGeodatabase(const std::wstring& path, Geodatabase& geodatabase);
  friend EXT_FILEGDB_API fgdbError CloseGeodatabase(Geodatabase& geodatabase);
  friend EXT_FILEGDB_API fgdbError DeleteGeodatabase(const std::wstring& path);

  friend class Table;

  Geodatabase(const Geodatabase&)             { }
  Geodatabase& operator=(const Geodatabase&)  { return *this; }
  /// @endcond
};

};  // namespace FileGDBAPI
