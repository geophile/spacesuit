package com.geophile.spacesuit.example;

import com.geophile.spacesuit.SpaceSuit;
import com.geophile.z.Space;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PopulateDB
{
    public static void main(String[] args) throws Exception
    {
        new PopulateDB(args).run();
    }

    private PopulateDB(String[] args) throws SQLException
    {
        int a = 0;
        inputFileName = args[a++];
        database = args[a++];
        user = args[a++];
        password = args[a++];
    }

    private void run() throws SQLException, IOException, ClassNotFoundException
    {
        // Describe the space
        Space space = Space.newSpace(new double[]{-90, -180},
                                     new double[]{90, 180},
                                     new int[]{24, 24});
        // Create SpaceSuit object
        SpaceSuit spaceSuit = SpaceSuit.create(space, "<<", ">>");
        // Prepare to read input file, containing lines containing latitude, longitude, description.
        BufferedReader input = new BufferedReader(new FileReader(inputFileName));
        String line;
        // Connect to the database
        Class.forName("com.mysql.jdbc.Driver");
        String url = String.format("jdbc:mysql://localhost/%s?user=%s&password=%s", database, user, password);
        Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement();
        while ((line = input.readLine()) != null) {
            // Read and parse line
            String[] fields = parseLine(line);
            double latitude = Double.parseDouble(fields[0]);
            double longitude = Double.parseDouble(fields[1]);
            String description = escapeQuote(fields[2]);
            // Compute spatialIndexKey-value
            long z = spaceSuit.spatialIndexKey(latitude, longitude);
            // Insert to database
            String query = String.format("insert into place(latitude, longitude, description, z) values (%s, %s, '%s', %s)",
                                         latitude, longitude, description, z);
            statement.executeUpdate(query);
        }
    }

    private String[] parseLine(String line)
    {
        line = line.trim();
        int latitudeStart = 0;
        int latitudeEnd = line.indexOf(' ');
        int p = latitudeEnd;
        while (line.charAt(p) == ' ') p++;
        int longitudeStart = p;
        int longitudeEnd = line.indexOf(' ', longitudeStart);
        p = longitudeEnd;
        while (line.charAt(p) == ' ') p++;
        int descriptionStart = p;
        String[] fields = new String[3];
        fields[0] = line.substring(latitudeStart, latitudeEnd);
        fields[1] = line.substring(longitudeStart, longitudeEnd);
        fields[2] = line.substring(descriptionStart);
        return fields;
    }

    private String escapeQuote(String s)
    {
        return s.replace("'", "\\'");
    }

    private final String inputFileName;
    private final String database;
    private final String user;
    private final String password;
}
