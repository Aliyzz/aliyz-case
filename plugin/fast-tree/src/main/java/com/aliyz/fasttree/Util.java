package com.aliyz.fasttree;


import com.sun.istack.internal.Nullable;

import java.util.Collection;

public class Util {

    static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
