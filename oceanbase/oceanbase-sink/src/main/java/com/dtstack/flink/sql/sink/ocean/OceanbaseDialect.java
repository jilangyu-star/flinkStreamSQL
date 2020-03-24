package com.dtstack.flink.sql.sink.ocean;

import com.dtstack.flink.sql.sink.rdb.dialect.JDBCDialect;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author : tiezhu
 * @date : 2020/3/24
 */
public class OceanbaseDialect implements JDBCDialect {
    @Override
    public boolean canHandle(String url) {
        return url.startsWith("jdbc:mysql:");
    }

    @Override
    public Optional<String> defaultDriverName() {
        return Optional.of("com.mysql.jdbc.Driver");
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public Optional<String> getUpsertStatement(String schema,
                                               String tableName,
                                               String[] fieldNames,
                                               String[] uniqueKeyFields,
                                               boolean allReplace) {
        return allReplace ?
                buildReplaceIntoStatement(tableName, fieldNames) :
                buildDuplicateUpsertStatement(tableName, fieldNames);
    }

    private Optional<String> buildDuplicateUpsertStatement(String tableName, String[] fieldsName) {
        String updateClause = Arrays.stream(fieldsName).map(f -> quoteIdentifier(f)
                + "IFNULL(VALUES(" + quoteIdentifier(f) + ")," + quoteIdentifier(f) + ")")
                .collect(Collectors.joining(","));
        return Optional.of(getInsertIntoStatement("", tableName, fieldsName, null) +
                " ON DUPLICATE KEY UPDATE " + updateClause
        );
    }

    private Optional<String> buildReplaceIntoStatement(String tableName, String[] fieldsNames) {
        String columns = Arrays.stream(fieldsNames)
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(","));
        String placeholders = Arrays.stream(fieldsNames)
                .map(f -> "?")
                .collect(Collectors.joining(","));
        return Optional.of("REPLACE INTO " + quoteIdentifier(tableName) +
                "(" + columns + ") VALUES (" + placeholders + ")");
    }
}
