package org.iro.aiqo.submitter.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.iro.aiqo.submitter.service.ai.openai.OpenAiAICaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = OpenAiAICallerIntegrationTest.TestApplication.class)
class OpenAiAICallerIntegrationTest {

    @Autowired
    private OpenAiAICaller aiCaller;

    @EnabledIfEnvironmentVariable(named = "SPRING_AI_OPENAI_API_KEY", matches = ".+")
    @Test
    void callUsesRealOpenAiApi() {
        String apiKey = System.getenv("SPRING_AI_OPENAI_API_KEY");
        var result = aiCaller.call(
                apiKey,
                "gpt-4o-mini",
                "Analizza il seguente SQL e suggerisci ottimizzazioni. Fornisci un riassunto breve.",
                """
                        CREATE TABLE customers(id BIGINT PRIMARY KEY, region TEXT);
                        CREATE TABLE orders(id BIGINT PRIMARY KEY, customer_id BIGINT);
                        """,
                "Preferisci utilizzare indici sugli id e filtra per regione tramite index scan.",
                """
                        SELECT * FROM orders o
                        JOIN customers c ON c.id = o.customer_id
                        WHERE c.region = 'EU'
                        """,
                "Seq Scan on orders -> Hash Join -> Filter region = EU"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isNotBlank();
        System.out.println(result.getSummary());
    }

    @SpringBootApplication(scanBasePackages = "org.iro.aiqo.submitter")
    static class TestApplication {
    }
}
