package com.dexmohq;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class Main {

    public static void main(String[] args) throws JSQLParserException {

        final Statement statement = CCJSqlParserUtil.parse("SELECT USER_ID FROM SCAN_EVENT se");
//        ((Select) statement).getSelectBody().accept(new ColumnBlacklistVisitor(blacklist));

    }

}
