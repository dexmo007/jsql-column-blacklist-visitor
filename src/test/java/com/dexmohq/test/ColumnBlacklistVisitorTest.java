package com.dexmohq.test;

import com.dexmohq.ColumnBlacklistVisitor;
import com.dexmohq.IllegalColumnAccessException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.Test;

import java.util.Collections;

public class ColumnBlacklistVisitorTest {

    @Test(expected = IllegalColumnAccessException.class)
    public void testAllColumns() throws JSQLParserException {
        test("SELECT * FROM SCAN_EVENT");
    }

    @Test(expected = IllegalColumnAccessException.class)
    public void testAllTableColumns() throws JSQLParserException {
        test("SELECT SCAN_EVENT.* FROM SCAN_EVENT");
    }

    @Test(expected = IllegalColumnAccessException.class)
    public void testAllTableColumnsByAlias() throws JSQLParserException {
        test("SELECT se.* FROM SCAN_EVENT se");
    }

    @Test(expected = IllegalColumnAccessException.class)
    public void testSelectExpressionWithNoTable() throws JSQLParserException {
        test("SELECT USER_ID FROM SCAN_EVENT se");
    }

    @Test(expected = IllegalColumnAccessException.class)
    public void testSelectExpressionWithTable() throws JSQLParserException {
        test("SELECT SCAN_EVENT.USER_ID FROM SCAN_EVENT se");
    }

    @Test(expected = IllegalColumnAccessException.class)
    public void testSelectExpressionWithTableAlias() throws JSQLParserException {
        test("SELECT se.USER_ID FROM SCAN_EVENT se");
    }

    @Test(expected = IllegalColumnAccessException.class)
    public void testUnionSelect() throws JSQLParserException {
        test("SELECT se.TIMESTAMP FROM SCAN_EVENT se UNION SELECT * FROM SCAN_EVENT");
    }

    @Test(expected = IllegalColumnAccessException.class)
    public void testSubSelect() throws JSQLParserException {
        test("SELECT se.USER_ID FROM (SELECT * FROM SCAN_EVENT)");
    }

    @Test
    public void testSubSelectButNotExposed() throws JSQLParserException {
        test("SELECT se.TIMESTAMP FROM (SELECT * FROM SCAN_EVENT) se");
    }

    @Test
    public void testJoin() throws JSQLParserException {
        test("SELECT b.* FROM BENIGN b LEFT JOIN SCAN_EVENT se ON se.ID = b.SE_ID");
    }

    private void test(String s) throws JSQLParserException {
        ((Select) CCJSqlParserUtil.parse(s))
                .getSelectBody()
                .accept(new ColumnBlacklistVisitor(
                        Collections.singletonMap("SCAN_EVENT", Collections.singleton("USER_ID"))
                ));
    }


}
