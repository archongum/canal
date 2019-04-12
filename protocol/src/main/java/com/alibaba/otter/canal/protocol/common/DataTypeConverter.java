package com.alibaba.otter.canal.protocol.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;


/**
 * Data type converter.
 * @author Archon  4/11/19
 * @since
 */
public class DataTypeConverter {

    private static final long MILLIS_PER_DAY = 86400000L;

    private static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * If error then return null to avoid malformed AVRO.
     * @param source
     * @return
     */
    public static Object datetime2millis(String source) {
        try {
            return dateTimeFormatter.parse(source).getTime();
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * If error then return null to avoid malformed AVRO.
     * @param source
     * @return
     */
    public static Object date2days(String source) {
        try {
            return dateFormatter.parse(source).getTime()/MILLIS_PER_DAY;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Type convert
     * @param strVal
     * @param sqlType
     * @param castBaseType
     * @param castDateTime
     * @return
     */
    public static Object typeConverts(String strVal, int sqlType, boolean castBaseType, boolean castDateTime) {
        Object res = strVal;
        switch (sqlType) {
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                if (castBaseType) {
                    res = Integer.parseInt(strVal);
                }
                break;
            case Types.BIGINT:
                if (castBaseType) {
                    res= new BigInteger(strVal);
                }
                break;
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.REAL:
                if (castBaseType) {
                    res = Double.parseDouble(strVal);
                }
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                if (castBaseType) {
                    res = new BigDecimal(strVal);
                }
                break;
            case Types.DATE:
                if (castDateTime) {
                    if (!strVal.startsWith("0000-00-00")) {
                        res = DataTypeConverter.date2days(strVal);
                    } else {
                        res = null;
                    }
                }
                break;
            case Types.TIMESTAMP:
                if (castDateTime) {
                    if (!strVal.startsWith("0000-00-00")) {
                        res = DataTypeConverter.datetime2millis(strVal);
                    } else {
                        res = null;
                    }
                }
                break;
            case Types.BOOLEAN:
                if (castBaseType) {
                    res = !"0".equals(strVal);
                }
                break;
            default:
                return res;
        }
        return res;
    }
}
