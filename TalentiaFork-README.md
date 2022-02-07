# hibernate-orm (forked by Talentia)

Forked from [hibernate/hibernate-orm](https://github.com/hibernate/hibernate-orm/tree/5.6.5) : hibernate-core is the only modified project inside this repository. Modifications are done in a dedicated branch suffixed by tswe.

The goal of the fork is to embed inside hibernate, processing that is related to Infinite and DB2/400 specificities and that is ORM concern. Managing this specificities at ORM level, allows performance gain and simplicity in application coding.

## Infinite specificities
Infinite manages a access to data with a complex strategy, mixing two SGBDR: the proprietary Infinite one and a third party one (MS SQL Server or Oracle). The complexity is hidden by the proprietary Infinite ODBC driver. Because of limitations of this ODBC driver (bugs, 32bits only, poor performances), we need to bypass this driver and access directly to MS SQL Server or Oracle using their standard JDBC drivers. Doing that we have to manage the complexity of determining the real table name using the the Infinite Unibol_Mapping table each time we need to access to a table. This part has been embeded in the starting process of Hibernate and is made once.

During the first access to the database, Hibernate will load in memory the relational-object mapping of the database, defined by the developer. During this loading, each persistence unit is also loaded once with all its parameters. We thus obtain several configurations, containing all the information necessary to check in the "UNIBOL_MAPPING" table the correspondence between the table name defined in the application and the real table name for a given type of database and a list of libraries.
To do this we had to add 3 properties in the persistence units setting in the "persistence.xml" file to specify the database type, as well as the prefix and suffix of the corresponding libraries.

`<property name="lswe.backoffice.database.type" value="" />` Where value can have as value: ORACLE, MSSQL, DB2/400.

`<property name="lswe.backoffice.prefix.lib" value=""/>` Where value is the prefix of a library, e.g. IRH61

`<property name="lswe.backoffice.suffix.lib "value=""/>` Where value corresponds to the suffix of a library, e.g. CLI

With these examples, we would obtain the libraries (in hierarchical order): IRH61SPCLI, IRH61FCLI, IRH61FXCLI, IRH61FX

If one of the 3 parameters " lswe.backoffice.database.type ", " lswe.backoffice.suffix.lib ", " lswe.backoffice.suffix.lib " is not defined, Hibernate will not take into account the UNIBOL_MAPPING.

When checking, and modifying if necessary, the names of the tables in memory and their correspondence in the database via UNIBOL_MAPPING, we take into account the hierarchical order of the libraries defined in the Iris products, namely

   1. Prefix + "SP" + Suffix
   2. Prefix + "F" + Suffix
   3. Prefix + "FX" + Suffix
   4. Prefix + "FX



## iSeries specificities : Null, empty strings and blanks management

The Null value (for a field) is not managed by the IP BackOffice. It is possible to parameterize the management of the Null via Hibernate, this will allow to transform a null value of a java object into an empty value managed by the IP BackOffice. By default if this setting is not defined the value will be "false".

`<property name="lswe.backoffice.replace.null.field" value=""/>`

It is also possible to parameterize the management of empty strings. Indeed during an insertion or update, if a string is empty, it will be set to null, or a blank " " will be added. By default if this setting is not defined the value will be "false".

`<property name="lswe.backoffice.replace.empty.field" value=""/>`

When retrieving a string in base we get the string followed by the blanks that complete it. The following parameterization allows you to remove these blanks in order to recover clean data. Empty strings will be set to "Null". By default if this setting is not defined the value will be "false".

`<property name="lswe.backoffice.delete.end.space" value=""/>`

