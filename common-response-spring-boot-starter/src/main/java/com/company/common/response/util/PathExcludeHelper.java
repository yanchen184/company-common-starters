package com.company.common.response.util;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 路徑排除判斷工具
 */
public final class PathExcludeHelper {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private PathExcludeHelper() {
    }

    /**
     * 判斷 URI 是否符合任一排除 pattern
     */
    public static boolean isExcluded(String uri, List<String> patterns) {
        for (String pattern : patterns) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
