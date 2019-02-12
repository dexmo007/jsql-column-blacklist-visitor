package com.dexmohq;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ColumnBlacklistVisitor extends SelectVisitorAdapter {

    private final Map<String, Set<String>> blacklist;

    public ColumnBlacklistVisitor(Map<String, Set<String>> blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    public void visit(SetOperationList setOpList) {
        for (final SelectBody select : setOpList.getSelects()) {
            select.accept(this);
        }
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        final BiMap<String, String> tablesByAlias = HashBiMap.create();
        final HashSet<String> tablesWithoutAlias = new HashSet<>();
        final FromItemVisitorAdapter tableExtractor = new FromItemVisitorAdapter() {

            @Override
            public void visit(Table table) {
                if (table.getAlias() != null) {
                    tablesByAlias.put(table.getAlias().getName(), table.getName());
                } else {
                    tablesWithoutAlias.add(table.getName());
                }
            }

            @Override
            public void visit(SubSelect subSelect) {
                // todo this is not correct, just because a blacklisted column is exposed by a sub select does not mean it is exposed by actual select
                subSelect.getSelectBody().accept(ColumnBlacklistVisitor.this);
            }
        };
        plainSelect.getFromItem().accept(tableExtractor);
        if (plainSelect.getJoins() != null) {
            for (final Join join : plainSelect.getJoins()) {
                join.getRightItem().accept(tableExtractor);
            }
        }
        if (blacklist.keySet().stream().noneMatch(table -> tablesWithoutAlias.contains(table) || tablesByAlias.values().contains(table))) {
            return;
        }
        for (final SelectItem selectItem : plainSelect.getSelectItems()) {
            if (selectItem instanceof AllColumns) {
                throw new IllegalColumnAccessException();
            } else if (selectItem instanceof AllTableColumns) {
                final String tableName = ((AllTableColumns) selectItem).getTable().getName();
                if (blacklist.containsKey(tableName) || blacklist.containsKey(tablesByAlias.get(tableName))) {
                    throw new IllegalColumnAccessException();
                }
            } else {
                ((SelectExpressionItem) selectItem).getExpression().accept(new ExpressionVisitorAdapter() {
                    @Override
                    public void visit(Column column) {
                        if (column.getTable() == null) {
                            if (Stream.concat(tablesByAlias.values().stream(), tablesWithoutAlias.stream())
                                    .anyMatch(table -> {
                                        final Set<String> blacklistedColumns = blacklist.get(table);
                                        return blacklistedColumns != null && blacklistedColumns.contains(column.getColumnName());
                                    })) {
                                throw new IllegalColumnAccessException();
                            }
                        } else {
                            final Set<String> blacklistedColumns = blacklist.getOrDefault(column.getTable().getName(),
                                    blacklist.get(tablesByAlias.get(column.getTable().getName())));
                            if (blacklistedColumns != null && blacklistedColumns.contains(column.getColumnName())) {
                                throw new IllegalColumnAccessException();
                            }
                        }
                    }
                });
            }
        }
    }

}
