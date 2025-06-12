package io.ants.modules.sys.dao;

import org.apache.ibatis.annotations.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Mapper
public interface TableDao {

    @Select("select TABLE_NAME from information_schema.TABLES where TABLE_SCHEMA=(select database()) order by  TABLE_NAME asc ")
    List<Map> listTable();


    @Select("select TABLE_NAME from information_schema.TABLES where TABLE_SCHEMA=(select database()) and TABLE_NAME=#{tableName}")
    List<Map> listTableOne(String tableName);

    //COLUMN_NAME,COLUMN_TYPE,COLUMN_DEFAULT
    //TABLE_CATALOG,IS_NULLABLE,EXTRA,COLUMN_NAME,COLUMN_KEY,NUMERIC_PRECISION,NUMERIC_SCALE,COLUMN_TYPE,ORDINAL_POSITION,DATA_TYPE
    @Select("select COLUMN_NAME,COLUMN_TYPE,ORDINAL_POSITION,EXTRA  from information_schema.COLUMNS where TABLE_SCHEMA = (select database()) and TABLE_NAME=#{tableName} order by COLUMN_NAME ASC")
    List<Map> listTableColumn(String tableName);


    @Select("select COLUMN_NAME,COLUMN_TYPE,ORDINAL_POSITION,EXTRA from information_schema.COLUMNS where TABLE_SCHEMA = (select database()) and TABLE_NAME=#{tableName} and COLUMN_NAME=#{columnName} ")
    List<Map> listTableOneColumn(String tableName,String columnName);

    //select * from information_schema.COLUMNS where TABLE_SCHEMA = (select database()) and TABLE_NAME ='dns_ip' and COLUMN_NAME='info'
    @Update("ALTER TABLE #{tableName} MODIFY COLUMN #{column_name} #{column_type} DEFAULT #{column_default}")
    int alert_modify_tab_column(String tableName,String column_name, String column_type,String column_default );



    public  List<LinkedHashMap<String,Object>> select_sql(@Param(value="sqlStr") String sqlStr);



    public int update_sql(@Param(value="sqlStr") String sqlStr);


    public Map find_row_by_id(@Param(value="tableName") String tableName,@Param(value="id") String id);

    @Delete("DELETE FROM sys_menu")
    void clearSysMenuTable();
}
