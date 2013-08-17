/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.spacesuit;

import com.geophile.z.Space;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SpaceSuitTest
{
    @Test
    public void emptyLeftDelimiter()
    {
        try {
            SpaceSuit.create(SPACE, "", ">>");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void emptyRightDelimiter()
    {
        try {
            SpaceSuit.create(SPACE, "<<", "");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void matchingDelimiters()
    {
        try {
            SpaceSuit.create(SPACE, "<<", "<<");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void noLeftDelimiters()
    {
        SpaceSuit spaceSuit = SpaceSuit.create(SPACE, "<<", ">>");
        try {
            spaceSuit.transformQuery("select ... >>", 4);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void noRightDelimiters()
    {
        SpaceSuit spaceSuit = SpaceSuit.create(SPACE, "<<", ">>");
        try {
            spaceSuit.transformQuery("select ... <<", 4);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void tooManyLeftDelimiters()
    {
        SpaceSuit spaceSuit = SpaceSuit.create(SPACE, "<<", ">>");
        try {
            spaceSuit.transformQuery("select << ... << ... >>", 4);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void tooManyRightDelimiters()
    {
        SpaceSuit spaceSuit = SpaceSuit.create(SPACE, "<<", ">>");
        try {
            spaceSuit.transformQuery("select << ... >> ... >>", 4);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void noFunction()
    {
        SpaceSuit spaceSuit = SpaceSuit.create(SPACE, "<<", ">>");
        try {
            spaceSuit.transformQuery("select << ... >> ...", 4);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void transformQuery()
    {
        SpaceSuit spaceSuit = SpaceSuit.create(SPACE, "<<", ">>");
        String[] transformed = spaceSuit.transformQuery("select ... <<inbox(spatialIndexKey, x, 524287, 524288, y, 524287, 524288)>> ...", 4);
        assertEquals(expectedQuery(524287, 524288, 0x1fffffffff800028L, 0x1fffffffffffffe8L), transformed[0]);
        assertEquals(expectedQuery(524287, 524288, 0x3555555555000028L, 0x35555555557fffe8L), transformed[1]);
        assertEquals(expectedQuery(524287, 524288, 0x4aaaaaaaaa800028L, 0x4aaaaaaaaaffffe8L), transformed[2]);
        assertEquals(expectedQuery(524287, 524288, 0x6000000000000028L, 0x60000000007fffe8L), transformed[3]);
    }

    @Test
    public void transformInsert()
    {
        SpaceSuit spaceSuit = SpaceSuit.create(SPACE, "<<", ">>");
        long z = spaceSuit.spatialIndexKey(1048575, 1048575);
        assertEquals(expectedZ(1048575, 1048575), z);
    }

    private String expectedQuery(long xyLo, long xyHi, long zLo, long zHi)
    {
        return String.format("select ... (spatialIndexKey between %s and %s and x between %s.0 and %s.0 and y between %s.0 and %s.0) ...",
                             zLo, zHi, xyLo, xyHi, xyLo, xyHi);
    }

    private long expectedZ(double x, double y)
    {
        return SPACE.spatialIndexKey(new double[]{x, y});
    }

    private static Space SPACE = Space.newSpace(new double[]{0, 0},
                                                new double[]{(1 << 20), (1 << 20)},
                                                new int[]{20, 20});
}
