/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.spacesuit;

import com.geophile.z.Space;
import com.geophile.z.spatialobject.d2.Box;

import java.sql.*;
import java.util.Random;

import static java.lang.Math.sqrt;

public class Benchmark
{
    public static void main(String[] args) throws SQLException
    {
        new Benchmark(args).run();
    }

    private Benchmark(String[] args)
    {
        int a = 0;
        database = args[a++];
        user = args[a++];
        password = args[a++];
        action = args[a++];
        count = Integer.parseInt(args[a++]);
        queries = new Box[count];
    }

    private void run() throws SQLException
    {
        if (action.equals("load")) {
            load();
        } else if (action.equals("query")) {
            query();
        } else {
            throw new IllegalArgumentException(action);
        }
    }

    private void load() throws SQLException
    {
        connect();
        runDDL();
        populate();
    }

    private void query() throws SQLException
    {
        connect();
        generateQueries();
        int rows = runQueries("no index", false);
        addIndex("x");
        int xRows = runQueries("x", false);
        addIndex("y");
        int xyRows = runQueries("xy", false);
        dropIndex("x");
        dropIndex("y");
        addIndex("z");
        int zRows = runQueries("z", true);
        dropIndex("z");
        if (rows != xRows || rows != xyRows || rows != zRows) {
            throw new AssertionError(String.format("rows: %s, xRows: %s, xyRows: %s, zRows: %s",
                                                   rows, xRows, xyRows, zRows));
        }
    }

    private void connect() throws SQLException
    {
        String url = String.format("jdbc:mysql://localhost/%s?user=%s&password=%s", database, user, password);
        connection = DriverManager.getConnection(url);
        connection.setAutoCommit(false);
    }

    private void generateQueries() throws SQLException
    {
        // Find number of rows
        int rows;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(COUNT);
            resultSet.next();
            rows = resultSet.getInt(1);
            resultSet.close();
        }
        // Size query so that the result size should be EXPECTED_ROWS. Query size is qx * qy.
        // (qx * qy) / (NX * NY) = EXPECTED_ROWS / rows. Assume everything is square (NX = NY, qx = qy)
        assert NX == NY;
        int qx = (int) sqrt(((double) NX * NY * EXPECTED_ROWS / rows));
        int qy = qx;
        for (int i = 0; i < count; i++) {
            int xLo = random.nextInt(NX - qx);
            int xHi = xLo + qx;
            int yLo = random.nextInt(NY - qy);
            int yHi = yLo + qy;
            queries[i] = new Box(xLo, xHi, yLo, yHi);
        }
    }

    private int runQueries(String label, boolean zQuery) throws SQLException
    {
        int rowsFound = 0;
        try (Statement statement = connection.createStatement()) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                if (zQuery) {
                    rowsFound += runZQuery(statement, queries[i]);
                } else {
                    rowsFound += runPlainQuery(statement, queries[i]);
                }
            }
            long stop = System.currentTimeMillis();
            double averageMsec = (double) (stop - start) / count;
            System.out.println(String.format("%s: %s msec", label, averageMsec));
            return rowsFound;
        }
    }

    private void runDDL() throws SQLException
    {
        try (Statement statement = connection.createStatement()) {
            for (String ddl : SCHEMA) {
                if (ddl.contains("%s")) {
                    ddl = String.format(ddl, database);
                }
                statement.execute(ddl);
            }
        }
    }

    private int runPlainQuery(Statement statement, Box box) throws SQLException
    {
        int rowsFound = 0;
        String query = String.format(PLAIN_QUERY, box.xLo(), box.xHi(), box.yLo(), box.yHi());
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            resultSet.getInt(1);
            resultSet.getDouble(2);
            resultSet.getDouble(3);
            rowsFound++;
        }
        resultSet.close();
        return rowsFound;
    }

    private int runZQuery(Statement statement, Box box) throws SQLException
    {
        int rowsFound = 0;
        String zQuery = String.format(Z_QUERY, box.xLo(), box.xHi(), box.yLo(), box.yHi());
        String[] queries = QUERY_TRANSFORMER.transformQuery(zQuery, 6);
        for (String query : queries) {
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                resultSet.getInt(1);
                resultSet.getDouble(2);
                resultSet.getDouble(3);
                rowsFound++;
            }
            resultSet.close();
        }
        return rowsFound;
    }

    private void addIndex(String columnName) throws SQLException
    {
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format(ADD_INDEX, indexName(columnName), columnName));
            statement.execute(ANALYZE);
        }
    }

    private void dropIndex(String columnName) throws SQLException
    {
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format(DROP_INDEX, indexName(columnName)));
            statement.execute(ANALYZE);
        }
    }

    private void populate() throws SQLException
    {
        double[] point = new double[2];
        try (Statement statement = connection.createStatement()) {
            for (int i = 0; i < count; i++) {
                if (i % 1000 == 0) {
                    connection.commit();
                }
                double x = random.nextInt(NX);
                double y = random.nextInt(NY);
                point[0] = x;
                point[1] = y;
                long z = SPACE.spatialIndexKey(point);
                String sql = String.format(INSERT, x, y, z, FILLER);
                statement.executeUpdate(sql);
            }
        }
        connection.commit();
    }

    private String indexName(String columnName)
    {
        return String.format("idx_%s", columnName);
    }

    private static String[] SCHEMA = new String[]
        {
            "drop table if exists %s.t", // database
            "create table t(id int not null auto_increment, " +
            "               x double not null, " +
            "               y double not null, " +
            "               z bigint not null, " +
            "               filler varchar(100), " +
            "               primary key(id))"
        };
    private static final String INSERT =
        "insert into t(x, y, z, filler) values(%s, %s, %s, '%s')";
    private static final String PLAIN_QUERY =
        "select id, x, y " +
        "from t " +
        "where x between %s and %s " +
        "and   y between %s and %s";
    private static final String Z_QUERY =
        "select id, x, y " +
        "from t " +
        "where << inbox(z, x, %s, %s, y, %s, %s) >>";
    private static final String COUNT =
        "select count(*) from t";
    private static final String ADD_INDEX =
        "create index %s on t(%s)";
    private static final String DROP_INDEX =
        "drop index %s on t";
    private static final String ANALYZE =
        "analyze table t";
    private static final int NX = 1_000_000;
    private static final int NY = 1_000_000;
    private static final int X_BITS = 20;
    private static final int Y_BITS = 20;
    private static final Space SPACE = Space.newSpace(new double[]{0, 0},
                                                      new double[]{NX, NY},
                                                      new int[]{X_BITS, Y_BITS});
    private static final SpaceSuit QUERY_TRANSFORMER = SpaceSuit.create(SPACE, "<<", ">>");
    private static final String FILLER =
        "abcdefghijklmnopqrst" +
        "abcdefghijklmnopqrst" +
        "abcdefghijklmnopqrst" +
        "abcdefghijklmnopqrst" +
        "abcdefghijklmnopqrst";
    private static final int EXPECTED_ROWS = 10;

    private final String database;
    private final String user;
    private final String password;
    private final String action;
    private final int count;
    private Connection connection;
    private final Random random = new Random(System.currentTimeMillis());
    private Box[] queries;
}
