/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.spacesuit;

import com.geophile.z.Space;
import com.geophile.spacesuit.apiimpl.Transformer;

/**
 * Utility for running fast spatial searches on top of a database system that does not support spatial
 * indexes.
 */

public class SpaceSuit
{
    /**
     * Returns the value to be assigned to the spatial index column of a table.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @return Spatial index key corresponding to (x, y).
     */
    public long spatialIndexKey(double x, double y)
    {
        xy[0] = x;
        xy[1] = y;
        return space.spatialIndexKey(xy);
    }

    /**
     * Given a query containing an invocation of the inbox function, returns a set of standard SQL queries
     * implementing the spatial search.
     * @param query SQL query containing an invocation of the inbox function.
     * @param maxZValues Maximum number of queries to be returned.
     * @return Standard SQL queries that can be executed. The combined query results provide the complete
     * result of the spatial query.
     */
    public String[] transformQuery(String query, int maxZValues)
    {
        String[] rewrites;
        int leftDelimiterPosition = query.indexOf(leftDelimiter);
        int invocationStart = leftDelimiterPosition + leftDelimiter.length();
        if (leftDelimiterPosition < 0) {
            throw new IllegalArgumentException(String.format("Missing left delimiter %s", leftDelimiter));
        }
        if (query.indexOf(leftDelimiter, invocationStart) >= 0) {
            throw new IllegalArgumentException(String.format("Multiple occurrences of left delimiter %s",
                                                             leftDelimiter));
        }
        int rightDelimiterPosition = query.indexOf(rightDelimiter);
        int invocationEnd = rightDelimiterPosition;
        if (rightDelimiterPosition < 0) {
            throw new IllegalArgumentException(String.format("Missing right delimiter %s", rightDelimiter));
        }
        if (query.indexOf(rightDelimiter, rightDelimiterPosition + rightDelimiter.length()) >= 0) {
            throw new IllegalArgumentException(String.format("Multiple occurrences of right delimiter %s",
                                                             rightDelimiter));
        }
        String invocation = query.substring(invocationStart, invocationEnd);
        Transformer transformer = new Transformer(space);
        String[] replacements = transformer.transform(invocation, maxZValues);
        rewrites = new String[replacements.length];
        String prefix = query.substring(0, leftDelimiterPosition);
        String suffix = query.substring(rightDelimiterPosition + rightDelimiter.length());
        StringBuilder buffer = new StringBuilder();
        buffer.append(prefix);
        for (int r = 0; r < replacements.length; r++) {
            buffer.setLength(prefix.length());
            buffer.append(replacements[r]);
            buffer.append(suffix);
            rewrites[r] = buffer.toString();
        }
        return rewrites;
    }

    /**
     * Creates a new SpaceSuit object.
     * @param space Space describing the space in which the data points exist.
     * @param leftDelimiter Left delimiter for the inbox invocation.
     * @param rightDelimiter Right delimiter for the inbox invocation.
     * @return A new SpaceSuit object.
     */
    public static SpaceSuit create(Space space, String leftDelimiter, String rightDelimiter)
    {
        return new SpaceSuit(space, leftDelimiter, rightDelimiter);
    }

    // For use by this class
    
    private SpaceSuit(Space space, String leftDelimiter, String rightDelimiter)
    {
        if (leftDelimiter.length() == 0 ||
            rightDelimiter.length() == 0 ||
            leftDelimiter.equals(rightDelimiter)) {
            throw new IllegalArgumentException(String.format("left delimiter: \"%s\", right delimiter: \"%s\"",
                                                             leftDelimiter, rightDelimiter));
        }
        this.space = space;
        this.leftDelimiter = leftDelimiter;
        this.rightDelimiter = rightDelimiter;
    }
    
    // Object state

    private final Space space;
    private final String leftDelimiter;
    private final String rightDelimiter;
    private final double[] xy = new double[2];
}
