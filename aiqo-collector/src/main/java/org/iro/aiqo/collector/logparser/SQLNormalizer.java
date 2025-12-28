package org.iro.aiqo.collector.logparser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SQLNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern STRING_LITERAL = Pattern.compile("(?i)(?:E|N)?'([^']|'')*'");
    private static final Pattern NUMERIC_LITERAL =
            Pattern.compile("(?<![A-Za-z0-9_])[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?(?![A-Za-z0-9_])");

    public String normalize(String sql) {
        if (sql == null) {
            return "";
        }
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String canonical = trimmed;
        try {
            Statement statement = CCJSqlParserUtil.parse(trimmed);
            canonical = statement.toString();
        } catch (JSQLParserException ex) {
            log.debug("Unable to parse SQL for normalization, applying fallback normalization: {}", ex.getMessage());
        }

        String withoutStrings = STRING_LITERAL.matcher(canonical).replaceAll("'?'");
        String withoutNumbers = NUMERIC_LITERAL.matcher(withoutStrings).replaceAll("0");
        return collapseWhitespace(withoutNumbers);
    }

    public String normalizedHash(String sql) {
        String normalized = normalize(sql);
        if (normalized.isEmpty()) {
            return "";
        }
        return sha256(normalized);
    }

    private String collapseWhitespace(String value) {
        return WHITESPACE.matcher(value).replaceAll(" ").trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}

