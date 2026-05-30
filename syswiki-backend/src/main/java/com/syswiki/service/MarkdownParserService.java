package com.syswiki.service;

import java.util.Map;

public interface MarkdownParserService {
    Map<String, String> parseByHeadings(String markdown);
    String mergeModules(Map<String, String> modules);
}
