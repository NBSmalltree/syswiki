package com.syswiki.service;

import com.syswiki.model.dto.ContentSaveDTO;
import com.syswiki.model.vo.ContentVO;
import com.syswiki.model.vo.ContentVersionVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ContentService {
    List<ContentVO> listContents(String systemId);
    ContentVO getContent(String systemId, String moduleType);
    ContentVO saveContent(String systemId, String moduleType, ContentSaveDTO dto);
    void importMarkdown(String systemId, MultipartFile file, String operator);
    String exportMarkdown(String systemId, List<String> moduleTypes);
    List<ContentVersionVO> getVersionHistory(String systemId, String moduleType);
}
