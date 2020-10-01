package com.atask.util;

public final class Utils {

    private Utils() {

    }

    public static boolean isEmpty(String src) {
        return src == null || src.isEmpty();
    }

    public static boolean isNotEmpty(String src) {
        return !isEmpty(src);
    }

}
