package org.iro.aiqo.collector.logparser.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.iro.aiqo.collector.logparser.SQLNormalizer;
import org.junit.jupiter.api.Test;

class SQLNormalizerTest {

    private final SQLNormalizer normalizer = new SQLNormalizer();

    @Test
    void normalizeShouldReturnCanonicalSqlWithoutLiterals() {
        String sql = "select  *  from  foo where id = 42 and name = 'John'";
        String normalized = normalizer.normalize(sql);

        assertEquals("SELECT * FROM foo WHERE id = 0 AND name = '?'", normalized);
    }

    @Test
    void normalizeShouldProduceSameOutputForEquivalentQueries() {
        String first = "SELECT *\nFROM foo\nWHERE id = 1 AND name = 'Alice'";
        String second = " select * from foo where id= 999 and name='Bob' ";

        String normalizedFirst = normalizer.normalize(first);
        String normalizedSecond = normalizer.normalize(second);

        assertEquals(normalizedFirst, normalizedSecond);
    }

    @Test
    void normalizedHashShouldBeStableForEquivalentQueries() {
        String first = "SELECT * FROM foo WHERE cost > 10.5 AND created_at = '2024-01-01'";
        String second = "select * from foo where cost>1000 and created_at='2023-10-10'";

        String firstHash = normalizer.normalizedHash(first);
        String secondHash = normalizer.normalizedHash(second);

        assertEquals(firstHash, secondHash);
        assertTrue(firstHash.matches("[0-9a-f]{64}"));
    }

    @Test
    void normalizeShouldHandleNonParsableSqlGracefully() {
        String invalid = "NOT A SQL STATEMENT";

        String normalized = normalizer.normalize(invalid);

        assertEquals("NOT A SQL STATEMENT", normalized);
        assertEquals(normalized, normalizer.normalize(invalid + "   "));
    }

    @Test
    void normalizeShouldHandleInsertStatements() {
        String sql = "INSERT INTO foo(id, name) VALUES (123, 'abc')";

        String normalized = normalizer.normalize(sql);

        assertEquals("INSERT INTO foo (id, name) VALUES (0, '?')", normalized);
    }
}
