# SpaceSuit

SpaceSuit is a utility for implementing fast spatial queries on top of
SQL database systems that don't have such a capability. The main use
case is MySQL, where application developers have to choose between
InnoDB tables, and spatial indexes which are only available with
MyISAM tables.

Currently, there is only a Java version of SpaceSuit, and it only
supports indexes on 2d point data, (e.g. latitude/longitude). Support
for other languages, other shapes, and for more dimensions is
possible, and these capabilities will be added based on demand.

## Overview

Suppose you have a table `T` with point coordinates described in the
columns `latitude` and `longitude`, and that you want a spatial index.
SpaceSuit provides the ability for you to do this as follows:

1) Add to table `T` a column that can store a 64-bit integer. (The name
of this column is `z` in the example below.)

2) Define an ordinary index on `T.z`.

3) When adding rows to `T`, use SpaceSuit to compute the value of the
`z` column.

4) When querying T with a spatial predicate, use SpaceSuit to
transform the query into a set of queries, each having an additional
condition on `z`. Run all of these queries and combine the results.

An example is given below.

## Installation

SpaceSuit can be built from source using [maven](http://maven.apache.org):

        mvn install

This creates `target/spacesuit-1.0.0.jar`.

To create Javadoc files:

        mvn javadoc:javadoc

These files are located in `target/site/apidocs`.


SpaceSuit depends on
[Geophile](https://github.com/geophile/geophile). If you build
SpaceSuit using maven, then the Geophile artifacts should be retrieved
automatically. To run SpaceSuit, you need to include the Geophile jar
file in your CLASSPATH, e.g. 
`~/.m2/repository/com/geophile/geophile/1.1.1/geophile-1.1.1.jar`.

## Example

The source code for this example can be found in
`src/test/java/com/geophile/spacesuit/example`. Scripts running the
examples can be found in `src/test/example`.

This example assumes that you are using the MySQL database. Any recent
version should work. 


### Create the schema

`src/test/example/schema.sql` creates a schema named spacesuit and
creates this table within the schema:

    create table place(
        id int not null,
        latitude double not null,
        longitude double not null,
        description varchar(200) not null,
        z bigint not null,
        primary key(id)
    );

To create the schema, run this command (substituting your own username
and password).
    
    mysql -u USERNAME -pPASSWORD < schema.sql

This creates a table named `place`. Each place has a position,
described by the `latitude` and `longitude` columns.  The `z` column
will be used to support a spatial index.

### Populate the table

`cities.txt` contains about 2000 cities and their
locations. `com.geophile.spacesuit.example.PopulateDB` can be used to
read `cities.txt` and populate the `place` table. The script
`src/test/example/load` runs `PopulateDB`. Run it as follows (assuming
your current directory is `src/test/example`):

    ./load cities.txt spacesuit USERNAME PASSWORD

substituting your own USERNAME and PASSWORD.

You can now inspect the `spacesuit.place` table using SQL.

`PopulateDB` is a typical JDBC application for loading data into a
database. The only unusual aspect is how the value for the `z` column
is obtained:

            // Compute z-value
            long z = spaceSuit.spatialIndexKey(latitude, longitude);

The variable `spaceSuit` is of type
`com.geophile.spacesuit.SpaceSuit`, initialized as follows:

        // Describe the space
        ApplicationSpace applicationSpace = 
            ApplicationSpace.newApplicationSpace(new double[]{-90, -180},
                                                 new double[]{90, 180});
        Space space = Space.newSpace(applicationSpace, 24, 24);
        // Create SpaceSuit object
        SpaceSuit spaceSuit = SpaceSuit.create(space, "<<", ">>");

First the application's space is described. The lower-left coordinate
is (-90, -180), and the upper-right is (90, 180), i.e., we're using
latitude and longitude.

Next, the space used by
[Geophile](https://github.com/geophile/geophile) is created. The
resolution of the grid used by Geophile is 24 bits by 24 bits. (See
the Geophile documentation for more information on `ApplicationSpace`
and `Space`.)

Finally, the `SpaceSuit` object is created, by passing in the `Space`,
and delimiters for the parts of queries that are rewritten by spacesuit.

### Query the table

`com.geophile.spacesuit.example.QueryDB` uses SpaceSuit to query the
database, using the spatial index defined on the column `z`. To run
`QueryDB`, use the script `src/test/example/query` as follows
(assuming your current directory is `src/test/example`):

    ./query spacesuit USERNAME PASSWORD 41 43 -72 -70

This locates all `place` rows with latitude between 41.0 and 43.0, and
longitude between -72.0 and -70.0:

    select latitude, longitude, z, description
    from place
    where (z between 5578834038405201933 and 5579959938312044493 
           and latitude between 41.0 and 43.0 
           and longitude between -72.0 and -70.0) 
    	 41.4170	-71.8330	(5578972815639609392):	Westerly,RI
    	 41.4817	-71.3167	(5579077535176851504):	Newport,RI
    	 41.7900	-71.8800	(5579184057605718064):	Danielson, CT
    	 41.9170	-71.9170	(5579232009708175408):	Putnam,CT
    	 41.7017	-71.4583	(5579276589924909104):	Warwick,RI
    	 41.7083	-71.5250	(5579277157833474096):	W._Warwick,RI
    	 41.7833	-71.4383	(5579281345579417648):	Cranston, RI
    	 41.8255	-71.4114	(5579282478885470256):	Providence,RI
    	 41.8200	-71.4100	(5579282519016865840):	Rumford,RI
    	 41.6830	-71.2330	(5579283601677713456):	Bristol, RI
    	 41.7017	-71.1550	(5579285246832017456):	Fall River, MA
    	 41.6369	-70.9281	(5579297207316250672):	New_Bedford,MA
    	 41.8783	-71.3833	(5579329907761119280):	Pawtucket,RI
    	 42.0000	-71.5000	(5579334659487498288):	Woonsocket,RI
    	 41.9580	-71.2919	(5579340332229263408):	Attleboro, MA
    	 42.0839	-71.0236	(5579357329439916080):	Brockton, MA
    	 42.0000	-70.7500	(5579370347983503408):	Plymouth,MA
    select latitude, longitude, z, description
    from place
    where (z between 5582211738125729805 and 5583337638032572365 
           and latitude between 41.0 and 43.0 
           and longitude between -72.0 and -70.0) 
    	 42.2603	-71.8047	(5582259860157956144):	Worcester,MA
    	 42.5830	-71.8000	(5582329204807925808):	Fitchburg, MA
    	 42.8182	-71.7209	(5582351083585634352):	Milford,NH
    	 42.2733	-71.4150	(5582356734299832368):	Framingham, MA
    	 42.3317	-71.1217	(5582369210459226160):	Brookline, MA
    	 42.3483	-71.1900	(5582369261558792240):	Newton,MA
    	 42.3430	-71.0500	(5582369934186053680):	East Boston, MA
    	 42.3500	-71.0500	(5582369954636628016):	Holliston,MA
    	 42.3567	-71.0569	(5582370004649377840):	Boston, MA
    	 42.3567	-71.0569	(5582370004649377840):	Somerville,MA
    	 42.3733	-71.2367	(5582379482034044976):	Waltham,MA
    	 42.3669	-71.1061	(5582381250570321968):	Cambridge, MA
    	 42.3875	-71.1019	(5582381889045626928):	Somerville,MA
    	 42.4150	-71.1517	(5582382226109235248):	Arlington, MA
    	 42.4183	-71.1067	(5582382413857849392):	Medford,MA
    	 42.4233	-71.0667	(5582382830090354736):	Malden,MA
    	 42.5000	-71.0667	(5582387159397531696):	Wakefield,MA
    	 42.2217	-70.9650	(5582388143565275184):	Weymouth,MA
    	 42.2517	-71.0017	(5582388859111800880):	Quincy,MA
    	 42.4633	-70.9483	(5582409891211149360):	Lynn,MA
    	 42.5170	-70.9000	(5582411427617767472):	Salem,MA
    	 42.5022	-70.5128	(5582420262566002736):	Mashpee,MA
    	 42.6403	-71.3205	(5582436246199369776):	Lowell,MA
    	 42.7000	-71.2500	(5582438017568112688):	Salem,NH
    	 42.7044	-71.1689	(5582439715084435504):	Lawrence,MA
    	 42.7830	-71.3830	(5582444735029215280):	Nashua,NH
    	 42.8817	-71.3233	(5582455120947871792):	Derry, NH
    	 42.8183	-70.8117	(5582482328664309808):	Merrimack,NH
    	 42.9911	-71.4614	(5582640798541840432):	Manchester,NH
    	 42.9333	-71.2800	(5582643296052412464):	Keene,NH
    	 42.9660	-70.9200	(5582671083252809776):	Exeter, NH

`QueryDB` uses SpaceSuit to take a query containing special syntax
describing the spatial query, and generate a set of standard SQL
queries that are then run by the application.

Here, `String.format` plugs in the coordinates of the query box

        String spaceSuitQuery = 
            String.format("select latitude, longitude, z, description\\n" +
                          "from place\\n" +
                          "where << inbox(z, latitude, %s, %s, longitude, %s, %s) >> ",
                          minLat, maxLat, minLon, maxLon);

For the example above, the query would be:

        select latitude, longitude, z, description
        from place
        where << inbox(z, latitude, 41, 43, longitude, -72, -70) >>

`<<` and `>>` are the delimiters passed to the `SpaceSuit`
constructor. Inside those delimiters is a specification of a spatial
query. `inbox` indicates that we want to locate points inside a given
box. `z` is the name of the column carrying the spatial
index. `latitude, 41, 43` says that we want rows in which latitude is
between 41 and 43, and `longitude, -72, -70` similarly restricts the
longitude column.

Next, the `inbox` query is transformed to a set of standard SQL
queries using `SpaceSuit.transform`:

        String[] queries = 
            spaceSuit.transformQuery(spaceSuitQuery, MAX_QUERIES);

MAX_QUERIES specifies the maximum number of queries to be created. This
value controls a tradeoff between the number of queries, and the speed
of each. Values of 4-10 are typically best. 

The rest of the code simply runs each query in the `queries` array,
printing the query and the results obtained from it.

## Performance

`com.geophile.spacesuit.Benchmark` compares four different strategies
for an inbox query on a table specifying point coordinates stored in
columns x and y:

1. No indexes (full table scan)

2. Index on x

3. Indexes on x and y

4. Index on z (only)

Using 1 million uniformly distributed points, and measuring the
average time to run 10 queries sized for an expected output size of 10
rows, I obtained the following results:

<table border="1">
    <tr>
        <td><b>Indexes</b></td>
        <td><b>Time (msec)</b></td>
    </tr>
    <tr>
        <td>None</td>
        <td>772.0</td>
    </tr>
    <tr>
        <td>X</td>
        <td>45.5</td>
    </tr>
    <tr>
        <td>X, Y</td>
        <td>40.2</td>
    </tr>
    <tr>
        <td>Z</td>
        <td>4.9</td>
    </tr>
</table>

These results were obtained with MySQL 5.5.32 on an Ubuntu 12.04 VM
running on a MacBook Pro with a 2.2 GHz Intel Core i7, and 16GB of
1333 MHz DDR3 memory.