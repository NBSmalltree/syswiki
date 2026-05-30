package com.syswiki.service.impl;

import com.syswiki.service.MarkdownParserService;
import com.syswiki.util.MarkdownUtil;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class MarkdownParserServiceImpl implements MarkdownParserService {
    @Override
    public Map<String, String> parseByHeadings(String markdown) {
        return MarkdownUtil.parseByH2(markdown);
    }

    @Override
    public String mergeModules(Map<String, String> modules) {
        return MarkdownUtil.mergeModules(modules);
    }
}
