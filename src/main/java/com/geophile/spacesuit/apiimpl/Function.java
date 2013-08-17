/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.spacesuit.apiimpl;

import com.geophile.z.Space;

import java.util.List;

public abstract class Function
{
    public abstract String[] invoke(List<String> arguments, int nZValues);

    protected String unquote(String s)
    {
        s = s.trim();
        boolean startQuote = s.charAt(0) == IDENTIFIER_QUOTE;
        boolean endQuote = s.charAt(s.length() - 1) == IDENTIFIER_QUOTE;
        if (startQuote != endQuote) {
            throw new IllegalArgumentException(s);
        }
        if (startQuote) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    protected Function(Space space)
    {
        this.space = space;
        this.xMin = space.lo(0);
        this.xMax = space.hi(0);
        this.yMin = space.lo(1);
        this.yMax = space.hi(1);
    }

    // Class state

    protected Space space;
    protected double xMin;
    protected double xMax;
    protected double yMin;
    protected double yMax;
    private static final char IDENTIFIER_QUOTE = '`';
}
