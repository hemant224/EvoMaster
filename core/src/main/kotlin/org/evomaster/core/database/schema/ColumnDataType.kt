package org.evomaster.core.database.schema

/**
 * SQL Data types from databases
 * See http://www.h2database.com/html/datatypes.html
 * and https://www.postgresql.org/docs/14/datatype.html
 * and https://dev.mysql.com/doc/refman/8.0/en/data-types.html
 */
enum class ColumnDataType(dataTypeName: String) {


    /**
     * TODO
     * String - set
     * https://dev.mysql.com/doc/refman/8.0/en/set.html
     */
    SET("SET"),

    /**
     * date time type
     * https://dev.mysql.com/doc/refman/8.0/en/date-and-time-type-syntax.html
     */
    DATETIME("DATETIME"),
    TIME("TIME"),


    /**
     * year (1 or 2) or 4
     * https://dev.mysql.com/doc/refman/8.0/en/year.html
     */
    YEAR("YEAR"),

    /**
     * enum type
     * https://dev.mysql.com/doc/refman/8.0/en/enum.html
     */
    ENUM("ENUM"),

    /**
     * bit type
     * https://dev.mysql.com/doc/refman/8.0/en/bit-type.html
     */
    BIT("BIT"),

    /**
     * https://www.postgresql.org/docs/14/datatype-bit.html
     */
    VARBIT("VARBIT"),

    /**
     * A Boolean value (true/false)
     */
    BOOL("BOOL"),
    BOOLEAN("BOOLEAN"),
    TINYINT("TINYINT"),

    /**
     * A string value.
     * The length of a CHAR column is fixed.
     */
    CHAR("CHAR"),


    /**
     * A normal-size integer.
     * The signed range is -2147483648 to 2147483647.
     * The unsigned range is 0 to 4294967295.
     */
    INTEGER("INTEGER"),
    INT("INT"),
    INT4("INT4"),

    /**
     * A large integer.
     * The signed range is -9223372036854775808 to 9223372036854775807.
     * The unsigned range is 0 to 18446744073709551615.
     */
    BIGINT("BIGINT"),
    INT8("INT8"),

    /**
     * A string value.
     * The length of the column is variable
     */
    VARCHAR("VARCHAR"),


    /**
     * LONG or LONG VARCHAR
     * https://dev.mysql.com/doc/refman/8.0/en/blob.html
     */
    MEDIUMTEXT("MEDIUMTEXT"),
    LONGBLOB("LONGBLOB"),
    TINYBLOB("TINYBLOB"),
    MEDIUMBLOB("MEDIUMBLOB"),
    LONGTEXT("LONGTEXT"),
    TINYTEXT("TINYTEXT"),

    /**
     * The TIMESTAMP data type is used for values that contain both date and time parts.
     * TIMESTAMP has a range of '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
     */
    TIMESTAMP("TIMESTAMP"),

    /**
     * The timestamptz data is the timestamp with time zone. The timestamptz is a time zone-aware date and time data type.
     * The SQL standard requires that writing just timestamp be equivalent to timestamp without time zone, and PostgreSQL
     * honors that behavior. (Releases prior to 7.3 treated it as timestamp with time zone.) timestamptz is accepted as
     * an abbreviation for timestamp with time zone; this is a PostgreSQL extension.
     */
    TIMESTAMPTZ("TIMESTAMPTZ"),


    /**
     * Alias for time with time zone. It is a PostgreSQL extension.
     */
    TIMETZ("TIMETZ"),


    /**
     * VARBINARY is similar to VARCHAR, except that it contains binary strings rather than nonbinary strings.
     * That is, it contains byte sequences rather than character sequences.
     */
    VARBINARY("VARBINARY"),


    /**
     * https://dev.mysql.com/doc/refman/8.0/en/binary-varbinary.html
     */
    BINARY("BINARY"),

    /**
     * The FLOAT type represents approximates numeric data values.
     * https://dev.mysql.com/doc/refman/8.0/en/floating-point-types.html
     * MySQL also supports this optional precision specification, but the precision
     * value in FLOAT(p) is used only to determine storage size.
     * A precision from 0 to 23 results in a 4-byte single-precision FLOAT column.
     * A precision from 24 to 53 results in an 8-byte double-precision DOUBLE column.
     */
    FLOAT("FLOAT"),

    /**
     *  The DOUBLE type represents approximate numeric data values.
     *  MySQL uses eight bytes for double-precision values.
     */
    DOUBLE("DOUBLE"),


    /**
     * A 16-bit (2 bytes) exact integer value
     */
    SMALLINT("SMALLINT"),
    MEDIUMINT("MEDIUMINT"),
    INT2("INT2"),

    /**
     * A CLOB (character large object) value can be up to 2,147,483,647 characters long.
     * A CLOB is used to store unicode character-based data, such as large documents in any character set.
     * The length is given in number characters for both CLOB, unless one of the suffixes K, M, or G is given, relating to the multiples of 1024, 1024*1024, 1024*1024*1024 respectively.
     **/
    CLOB("CLOB"),

    /**
     * Real data can hold a value 4 bytes in size, meaning it has 7 digits of precision
     * (the number of digits to the right of the decimal point).
     * It is also a floating-point numeric that is identical to the floating point statement float(24).
     */
    REAL("REAL"),

    /**
     * Data type with fixed precision and scale. This data type is recommended for storing currency values.
     * Mapped to java.math.BigDecimal.
     * Example: DECIMAL(20, 2)
     **/
    DECIMAL("DECIMAL"),
    DEC("DEC"),

    /**
     * Same as DECIMAL
     */
    NUMERIC("NUMERIC"),

    /**
     * A Binary Large Object, typically images, audio or multimedia.
     */
    BLOB("BLOB"),


    /**
     * Postgres. The data type uuid stores Universally Unique Identifiers (UUID)
     * as defined by RFC 4122, ISO/IEC 9834-8:2005, and related standards.
     */
    UUID("UUID"),

    /**
     * Postgres. In addition, PostgreSQL provides the text type, which stores strings
     * of any length. Although the type text is not in the SQL standard,
     * several other SQL database management systems have it as well.
     * Both TEXT and VARCHAR have the upper limit at 1 GB
     */
    TEXT("TEXT"),

    /**
     * Postgres. The xml data type can be used to store XML data. Its advantage over
     * storing XML data in a text field is that it checks the input values for well-formedness,
     * and there are support functions to perform type-safe operations on it
     */
    XML("XML"),

    /**
     * date (no time of day) minvalue = 4713 BC, maxvalue= 5874897 AD
     */
    DATE("DATE"),

    JSON("JSON"),
    JSONB("JSONB"),

    /**
     * BigSerial used as auto-incremental ID.
     * The data types serial and bigserial are not true types, but merely a notational convenience for creating unique
     * identifier columns (similar to the AUTO_INCREMENT property supported by some other databases).
     */
    BIGSERIAL("BIGSERIAL"),

    SERIAL("SERIAL"),

    /**
     * http://www.h2database.com/html/datatypes.html#timestamp_with_time_zone_type
     */
    TIMESTAMP_WITH_TIME_ZONE("TIMESTAMP_WITH_TIME_ZONE"),

    /**
     * http://www.h2database.com/html/datatypes.html#time_with_time_zone_type
     */
    TIME_WITH_TIME_ZONE("TIME_WITH_TIME_ZONE"),

    /**
     * https://www.h2database.com/html/datatypes.html#binary_varying_type
     */
    BINARY_VARYING("BINARY_VARYING"),

    /**
     * https://www.h2database.com/html/datatypes.html#double_precision_type
     */
    DOUBLE_PRECISION("DOUBLE_PRECISION"),

    /**
     * https://www.h2database.com/html/datatypes.html#binary_large_object_type
     */
    BINARY_LARGE_OBJECT("BINARY_LARGE_OBJECT"),

    /**
     * https://www.h2database.com/html/datatypes.html#character_large_object_type
     */
    CHARACTER_LARGE_OBJECT("CHARACTER_LARGE_OBJECT"),

    /**
     * https://www.h2database.com/html/datatypes.html#character_type
     */
    CHARACTER("CHARACTER"),

    /**
     * https://www.h2database.com/html/datatypes.html#character_varying_type
     */
    CHARACTER_VARYING("CHARACTER_VARYING"),

    /**
     * https://www.h2database.com/html/datatypes.html#varchar_ignorecase_type
     */
    VARCHAR_IGNORECASE("VARCHAR_IGNORECASE"),

    /**
     * https://www.h2database.com/html/datatypes.html#java_object_type
     */
    JAVA_OBJECT("JAVA_OBJECT"),

    /**
     * https://www.h2database.com/html/datatypes.html#geometry_type
     */
    GEOMETRYCOLLECTION("GEOMETRYCOLLECTION"),

    /**
     *  https://www.postgresql.org/docs/14/datatype-numeric.html
     */
    FLOAT4("FLOAT4"),
    FLOAT8("FLOAT8"),
    SMALLSERIAL("SMALLSERIAL"),

    /**
     * https://www.postgresql.org/docs/14/datatype-money.html
     */
    MONEY("MONEY"),

    /**
     * https://www.postgresql.org/docs/current/typeconv-query.html
     * The bpchar column type stands for blank-padded char
     */
    BPCHAR("BPCHAR"),

    /**
     * https://www.postgresql.org/docs/14/datatype-binary.html
     */
    BYTEA("BYTEA"),

    /**
     * https://www.postgresql.org/docs/14/datatype-datetime.html
     */
    INTERVAL("INTERVAL"),

    /**
     * https://www.postgresql.org/docs/14/datatype-geometric.html
     */
    POINT("POINT"),
    LINE("LINE"),
    LSEG("LSEG"),
    BOX("BOX"),
    PATH("PATH"),
    POLYGON("POLYGON"),
    CIRCLE("CIRCLE"),

    /**
     * https://dev.mysql.com/doc/refman/8.0/en/spatial-types.html
     */
    LINESTRING("LINESTRING"),
    MULTIPOINT("MULTIPOINT"),
    MULTILINESTRING("MULTILINESTRING"),
    MULTIPOLYGON("MULTIPOLYGON"),
    GEOMETRY("GEOMETRY"),
    GEOMCOLLECTION("GEOMCOLLECTION"),

    /**
     * https://www.postgresql.org/docs/14/datatype-net-types.html
     */
    CIDR("CIDR"),
    INET("INET"),
    MACADDR("MACADDR"),
    MACADDR8("MACADDR8"),

    /**
     * https://www.postgresql.org/docs/14/datatype-textsearch.html
     */
    TSVECTOR("TSVECTOR"),
    TSQUERY("TSQUERY"),

    /**
     * https://www.postgresql.org/docs/14/datatype-json.html#DATATYPE-JSONPATH
     */
    JSONPATH("JSONPATH"),

    /**
     * https://www.postgresql.org/docs/14/rangetypes.html
     * built-in range types
     */
    INT4RANGE("INT4RANGE"),
    INT8RANGE("INT8RANGE"),
    NUMRANGE("NUMRANGE"),
    TSRANGE("TSRANGE"),
    TSTZRANGE("TSTZRANGE"),
    DATERANGE("DATERANGE"),

    /**
     * https://www.postgresql.org/docs/14/rangetypes.html
     * built-in multirange types
     */
    INT4MULTIRANGE("INT4MULTIRANGE"),
    INT8MULTIRANGE("INT8MULTIRANGE"),
    NUMMULTIRANGE("NUMMULTIRANGE"),
    TSMULTIRANGE("TSMULTIRANGE"),
    TSTZMULTIRANGE("TSTZMULTIRANGE"),
    DATEMULTIRANGE("DATEMULTIRANGE"),

    /**
     * https://www.postgresql.org/docs/current/datatype-pg-lsn.html
     * postgres log sequence number
     */
    PG_LSN("PG_LSN"),

    /**
     * https://www.postgresql.org/docs/current/datatype-oid.html
     * postgres aliases for object identifiers
     */
    OID("OID"),
    REGCLASS("REGCLASS"),
    REGCOLLATION("REGCOLLATION"),
    REGCONFIG("REGCONFIG"),
    REGDICTIONARY("REGDICTIONARY"),
    REGNAMESPACE("REGNAMESPACE"),
    REGOPER("REGOPER"),
    REGOPERATOR("REGOPERATOR"),
    REGPROC("REGPROC"),
    REGPROCEDURE("REGPROCEDURE"),
    REGROLE("REGROLE"),
    REGTYPE("REGTYPE"),

    /**
     * This is not an actual built-in column data type,
     * but a placeholder for user-defined composite types.
     */
    COMPOSITE_TYPE("\$COMPOSITE_TYPE");

    fun shouldBePrintedInQuotes(): Boolean {
        /*
            TODO double check all them... likely this list is currently incompleted... need test for each
            single type
         */
        return equals(VARCHAR) || equals(CHAR) || equals(TIMESTAMP) || equals(TIMESTAMPTZ) || equals(TEXT)
                || equals(UUID) || equals(CHARACTER) || equals(CHARACTER_LARGE_OBJECT) || equals(CHARACTER_VARYING)
                || equals(VARCHAR_IGNORECASE)
    }
}
