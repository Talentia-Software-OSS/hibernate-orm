package com.lswe.irisportail;

/**
 * @author ebouladier
 * 
 */
public final class SpecDbHolder {

	/**
	 * Type de base de données sql.
	 */
	private static final String IRISSQL = "MSSQL";

	/**
	 * Type de base de données oracle.
	 */
	private static final String IRISORA = "ORACLE";

	/**
	 * Type de base de données db2400.
	 */
	private static final String IRISDB2 = "DB2/400";

	/**
	 * Url de type db2400.
	 */
	private static final String urlDb2400 = "jdbc:as400";

	/**
	 * Url de type oracle.
	 */
	private static final String urlOracle = "jdbc:oracle";

	/**
	 * Url de type sqlserver.
	 */
	private static final String urlSqlserver = "jdbc:sqlserver";

	/**
	 * Radical SP
	 */
	private static final String libSp = "SP";

	/**
	 * Radical F
	 */
	private static final String libF = "F";

	/**
	 * Radical FX
	 */
	private static final String libFx = "FX";

	/**
	 * Cette classe n'est pas destinée à être instanciée.
	 */
	private SpecDbHolder() {
		super();
	}

	public static String getSqlPrepStat(String unibol_mapping_schema_cfg) {
		if (null != unibol_mapping_schema_cfg && !unibol_mapping_schema_cfg.equals("")) {
			return "select table_name, library_name, file_name from " + unibol_mapping_schema_cfg + ".UNIBOL_MAPPING where library_name IN (?,?,?,?) and file_name IN (?,?) order by file_name DESC";
		} else {
			return "select table_name, library_name, file_name from UNIBOL_MAPPING where library_name IN (?,?,?,?) and file_name IN (?,?) order by file_name DESC";
		}
	}

	/**
	 * Renvoie la partie de la chaine de connexion identifiant le driver jdbc iris pour Microsoft SQL Server.
	 * 
	 * @return la partie de la chaine de connexion identifiant le driver jdbc iris pour Microsoft SQL Server.
	 */
	public static String getIRISSQL() {
		return IRISSQL;
	}

	/**
	 * Renvoie la partie de la chaine de connexion identifiant le driver jdbc iris pour Oracle.
	 * 
	 * @return la partie de la chaine de connexion identifiant le driver jdbc iris pour Oracle.
	 */
	public static String getIRISORA() {
		return IRISORA;
	}

	/**
	 * @return the irisdb2
	 */
	public static String getIrisdb2() {
		return IRISDB2;
	}

	/**
	 * @return the urldb2400
	 */
	public static String getUrlDb2400() {
		return urlDb2400;
	}

	/**
	 * @return the urloracle
	 */
	public static String getUrlOracle() {
		return urlOracle;
	}

	/**
	 * @return the urlsqlserver
	 */
	public static String getUrlSqlserver() {
		return urlSqlserver;
	}

	/**
	 * @return the libsp
	 */
	public static String getLibsp() {
		return libSp;
	}

	/**
	 * @return the libf
	 */
	public static String getLibf() {
		return libF;
	}

	/**
	 * @return the libfx
	 */
	public static String getLibfx() {
		return libFx;
	}

}
