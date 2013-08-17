package com.geophile.spacesuit.example;

import com.geophile.spacesuit.SpaceSuit;
import com.geophile.z.Space;

import java.io.IOException;
import java.sql.*;

public class QueryDB
{
    public static void main(String[] args) throws Exception
    {
        new QueryDB(args).run();
    }

    private QueryDB(String[] args) throws SQLException
    {
        int a = 0;
        database = args[a++];
        user = args[a++];
        password = args[a++];
        minLat = Double.parseDouble(args[a++]);
        maxLat = Double.parseDouble(args[a++]);
        minLon = Double.parseDouble(args[a++]);
        maxLon = Double.parseDouble(args[a++]);

    }

    private void run() throws SQLException, IOException
    {
        // Describe the space
        Space space = Space.newSpace(new double[]{-90, -180},
                                     new double[]{90, 180},
                                     new int[]{24, 24});
        // Create SpaceSuit object
        SpaceSuit spaceSuit = SpaceSuit.create(space, "<<", ">>");
        // Connect to the database
        String url = String.format("jdbc:mysql://localhost/%s?user=%s&password=%s", database, user, password);
        Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement();
        String spaceSuitQuery = String.format("select latitude, longitude, z, description\n" +
                                              "from place\n" +
                                              "where << inbox(z, latitude, %s, %s, longitude, %s, %s) >> ",
                                              minLat, maxLat, minLon, maxLon);
        String[] queries = spaceSuit.transformQuery(spaceSuitQuery, MAX_QUERIES);
        for (String query : queries) {
            ResultSet resultSet = statement.executeQuery(query);
            System.out.println(query);
            while (resultSet.next()) {
                System.out.println(String.format("\t%8.4f\t%8.4f\t(%s):\t%s",
                                                 resultSet.getDouble(1),
                                                 resultSet.getDouble(2),
                                                 resultSet.getLong(3),
                                                 resultSet.getString(4)));
            }
        }
    }

    private static final int MAX_QUERIES = 4;
    private final String database;
    private final String user;
    private final String password;
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
}
