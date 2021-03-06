/*
Copyright 2013 P6Spy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.p6spy.engine.spy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class P6TestPreparedStatement extends P6TestFramework {

    public P6TestPreparedStatement(String db) throws SQLException, IOException {
      super(db);
    }

    @Before
    public void setUpPreparedStatement() {
        try {
            Statement statement = connection.createStatement();
            dropPrepared(statement);
            statement.execute("create table prepstmt_test (col1 varchar(255), col2 integer)");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testPreparedQueryUpdate() {
        try {
            // test a basic insert
            String update = "insert into prepstmt_test values (?, ?)";
            PreparedStatement prep = getPreparedStatement(update);
            prep.setString(1, "miller");
            prep.setInt(2,1);
            prep.executeUpdate();
            assertTrue(super.getLastLogEntry().contains(update));
            assertTrue(super.getLastLogEntry().contains("miller"));
            assertTrue(super.getLastLogEntry().contains("1"));

            // test a basic select
            String query = "select count(*) from prepstmt_test where col2 = ?";
            prep = getPreparedStatement(query);
            prep.setInt(1,1);
            ResultSet rs = prep.executeQuery();
            assertTrue(super.getLastLogEntry().contains(query));
            rs.next();
            assertEquals(1, rs.getInt(1));

            // test dynamic allocation of P6_MAX_FIELDS
            int MaxFields = 10;
            StringBuffer bigSelect = new StringBuffer(MaxFields);
            bigSelect.append("select count(*) from prepstmt_test where");
            for (int i = 0; i < MaxFields; i++) {
                if (i > 0) {
                  bigSelect.append(" or ");
                }
                bigSelect.append(" col2=?");
            }
            prep = getPreparedStatement(bigSelect.toString());
            for (int i = 1; i <= MaxFields; i++) {
                 prep.setInt(i, i);
            }
            
            // test batch inserts
            update = "insert into prepstmt_test values (?,?)";
            prep = getPreparedStatement(update);
            prep.setString(1,"danny");
            prep.setInt(2,2);
            prep.addBatch();
            prep.setString(1,"denver");
            prep.setInt(2,3);
            prep.addBatch();
            prep.setString(1,"aspen");
            prep.setInt(2,4);
            prep.addBatch();
            prep.executeBatch();
            assertTrue(super.getLastLogEntry().contains(update));
            assertTrue(super.getLastLogEntry().contains("aspen"));
            assertTrue(super.getLastLogEntry().contains("4"));

            query = "select count(*) from prepstmt_test";
            prep = getPreparedStatement(query);
            rs = prep.executeQuery();
            rs.next();
            assertEquals(4, rs.getInt(1));
        } catch (Exception e) {
            fail(e.getMessage()+" due to error: "+getStackTrace(e));
        }
    }

    @After
    public void tearDownPreparedStatement() {
        try {
            Statement statement = connection.createStatement();
            dropPrepared(statement);
        }  catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void dropPrepared(Statement statement) {
        dropPreparedStatement("drop table prepstmt_test", statement);
    }

    protected void dropPreparedStatement(String sql, Statement statement) {
        try {
            statement.execute(sql);
        } catch (Exception e) {
            // we don't really care about cleanup failing
        }
    }

    protected PreparedStatement getPreparedStatement(String query) throws SQLException {
        return (connection.prepareStatement(query));
    }

}
