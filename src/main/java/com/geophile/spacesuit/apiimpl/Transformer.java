/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.spacesuit.apiimpl;

import com.geophile.z.Space;

import java.util.*;

public class Transformer
{
    public String[] transform(String invocation, int maxZValues)
    {
        parse(invocation);
        Function function = functions.get(functionName);
        if (function == null) {
            throw new IllegalArgumentException(functionName);
        }
        return function.invoke(arguments, maxZValues);
    }

    public Transformer(Space space)
    {
        register("inbox", new InBox(space));
    }

    private void parse(String invocation)
    {
        invocation = invocation.trim();
        int openParen = invocation.indexOf('(');
        if (openParen == -1) {
            throw new IllegalArgumentException(invocation);
        }
        int closeParen = invocation.indexOf(')');
        if (closeParen == -1) {
            throw new IllegalArgumentException(invocation);
        }
        functionName = invocation.substring(0, openParen);
        String argumentString = invocation.substring(openParen + 1, closeParen);
        StringTokenizer tokenizer = new StringTokenizer(argumentString, ",");
        while (tokenizer.hasMoreTokens()) {
            arguments.add(tokenizer.nextToken());
        }
    }

    private void register(String functionName, Function function)
    {
        functions.put(functionName, function);
    }

    protected String functionName;
    protected List<String> arguments = new ArrayList<>();
    private Map<String, Function> functions = new HashMap<>();
}
