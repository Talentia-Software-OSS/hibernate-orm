# hibernate-orm (forked by Talentia)

Forked from [hibernate/hibernate-orm](https://github.com/hibernate/hibernate-orm/tree/5.6.5) : hibernate-core is the only modified project inside this repository. Modifications are done in a dedicated branch suffixed by tswe.

The goal of the fork is to embed inside hibernate, processing that is related to Infinite and DB2/400 specificities and that is ORM concern. Managing this specificities at ORM level, allows performance gain and simplicity in application coding.

## iSeries specificities : Null, empty strings and blanks management

The Null value (for a field) is not managed by the IP BackOffice. It is possible to parameterize the management of the Null via Hibernate, this will allow to transform a null value of a java object into an empty value managed by the IP BackOffice. By default if this setting is not defined the value will be "false".

	<property name="lswe.backoffice.replace.null.field" value=""/>

It is also possible to parameterize the management of empty strings. Indeed during an insertion or update, if a string is empty, it will be set to null, or a blank " " will be added. By default if this setting is not defined the value will be "false".

	<property name="lswe.backoffice.replace.empty.field" value=""/>

When retrieving a string in base we get the string followed by the blanks that complete it. The following parameterization allows you to remove these blanks in order to recover clean data. Empty strings will be set to "Null". By default if this setting is not defined the value will be "false".

	<property name="lswe.backoffice.delete.end.space" value=""/>

