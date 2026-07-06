package com.syswiki.util;

import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * SQL参数校验工具类，防止SQL注入攻击。
 * 用于校验用户在SQL模板渲染时传入的参数值。
 */
public class SqlParamValidator {

    private SqlParamValidator() {}

    /** 参数值最大长度 */
    private static final int MAX_PARAM_LENGTH = 100;

    /** 白名单正则：只允许字母、数字、下划线、横杠、点号、斜杠、空格 */
    private static final Pattern SAFE_VALUE_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-./\\s]*$");

    /** 危险关键字（大小写不敏感匹配） */
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
            "(?i)\\b(UNION|SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|EXEC|EXECUTE|TRUNCATE|GRANT|REVOKE|DECLARE|SET|CALL|MERGE|XP_|SP_)\\b"
    );

    /** 危险字符：分号、注释符、反引号、null字节、反斜杠 */
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[;`\\x00\\\\]|--|/\\*|\\*/");

    /**
     * 校验所有参数值。
     *
     * @param params 用户传入的参数键值对
     * @throws BizException 参数不合法时抛出
     */
    public static void validateAll(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            validateParam(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 校验单个参数值。
     *
     * @param paramName  参数名称（用于错误提示）
     * @param paramValue 参数值
     * @throws BizException 参数不合法时抛出
     */
    public static void validateParam(String paramName, String paramValue) {
        if (paramValue == null) {
            return;
        }

        // 1. 长度校验
        if (paramValue.length() > MAX_PARAM_LENGTH) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    String.format("参数「%s」长度超过限制，最大允许%d个字符", paramName, MAX_PARAM_LENGTH));
        }

        // 2. 危险字符校验
        if (DANGEROUS_CHARS.matcher(paramValue).find()) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    String.format("参数「%s」包含非法字符，不允许包含分号、注释符等特殊字符", paramName));
        }

        // 3. 危险关键字校验
        if (DANGEROUS_KEYWORDS.matcher(paramValue).find()) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    String.format("参数「%s」包含SQL关键字，输入不合法", paramName));
        }

        // 4. 白名单校验
        if (!SAFE_VALUE_PATTERN.matcher(paramValue).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    String.format("参数「%s」包含不允许的字符，仅支持字母、数字、下划线、横杠、点号、斜杠", paramName));
        }
    }
}
