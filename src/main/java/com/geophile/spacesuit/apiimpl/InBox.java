/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.spacesuit.apiimpl;

import com.geophile.z.Space;
import com.geophile.z.spatialobject.d2.Box;

import java.util.List;

/*
 * Invocation: inbox(z, x, XLO, XHI, y, YLO, YHI)
 * where:
 * - z is the name of the column carrying z-values
 * - x, y: are the names of the columns carrying point coordinates
 * - XLO, XHI, YLO, YHI: are floating point literals describing the query box.
 */

public class InBox extends Function
{
    public InBox(Space space)
    {
        super(space);
    }

    public String[] invoke(List<String> arguments, int maxZValues)
    {
        int a = 0;
        String zColumn = unquote(arguments.get(a++));
        String xColumn = unquote(arguments.get(a++));
        double xLo = Double.parseDouble(arguments.get(a++));
        double xHi = Double.parseDouble(arguments.get(a++));
        String yColumn = unquote(arguments.get(a++));
        double yLo = Double.parseDouble(arguments.get(a++));
        double yHi = Double.parseDouble(arguments.get(a++));
        if (xLo > xHi || yLo > yHi ||
            xLo < xMin || xHi > xMax ||
            yLo < yMin || yHi > yMax) {
            throw new IllegalArgumentException(String.format("(%s : %s, %s : %s)", xLo, xHi, yLo, yHi));
        }
        Box box = new Box(xLo, xHi, yLo, yHi);
        long[] zs = new long[maxZValues];
        space.decompose(box, zs);
        int nOutput = 0;
        for (int i = 0; i < zs.length; i++) {
            if (zs[i] != -1L) {
                nOutput++;
            }
        }
        String[] output = new String[nOutput];
        for (int i = 0; i < nOutput; i++) {
            long z = zs[i];
            output[i] = String.format
                ("(%s between %s and %s and" + // z
                 " %s between %s and %s and" + // x
                 " %s between %s and %s)",     // y
                 zColumn, Space.zLo(z), Space.zHi(z),
                 xColumn, xLo, xHi,
                 yColumn, yLo, yHi);
        }
        return output;
    }
}
