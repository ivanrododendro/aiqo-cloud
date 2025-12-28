package org.iro.aiqo.api;

import org.iro.aiqo.model.Application;
import org.iro.aiqo.model.DBType;
import org.iro.aiqo.model.LLM;
import org.iro.aiqo.model.LLMFamily;
import org.iro.aiqo.model.LLMVendor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.liquibase.enabled=false")
@Transactional
class ApplicationRestRepositoryMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRestRepository applicationRestRepository;

    @Autowired
    private LLMRestRepository llmRestRepository;

    @Autowired
    private RepositoryRestConfiguration repositoryRestConfiguration;

    @Test
    void byNameAndTenantReturnsApplicationWhenPresent() throws Exception {
        persistApplication("Reporting", 7);

        mockMvc.perform(get(restPath("/applications/search/byNameAndTenant"))
                        .param("name", "Reporting")
                        .param("tenantId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reporting"))
                .andExpect(jsonPath("$.tenantId").value(7));
    }

    @Test
    void byNameAndTenantReturnsNotFoundWhenAbsent() throws Exception {
        persistApplication("Reporting", 8);

        mockMvc.perform(get(restPath("/applications/search/byNameAndTenant"))
                        .param("name", "Unknown")
                        .param("tenantId", "7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchEndpointExposesByNameAndTenantLink() throws Exception {
        mockMvc.perform(get(restPath("/applications/search")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.byNameAndTenant.href").exists());
    }

    private Application persistApplication(String name, int tenantId) {
        LLM llm = new LLM();
        llm.setTenantId(tenantId);
        llm.setName("Default LLM");
        llm.setFamily(LLMFamily.CHAT);
        llm.setProvider("OpenAI");
        llm.setModelName("gpt-4");
        llm.setInputTokenCost(1L);
        llm.setOutputTokenCost(2L);
        llm.setCurrency(Currency.getInstance("USD"));
        llm.setMainPrompt("Provide insights based on input data.");
        llm.setStructurePrompt("Return structured JSON results.");
        llm.setLlmVendor(LLMVendor.OPENAI);
        LLM savedLlm = llmRestRepository.save(llm);

        Application application = new Application();
        application.setTenantId(tenantId);
        application.setName(name);
        application.setDefaultDBType(DBType.PGSQL);
        application.setDefaultLLM(savedLlm);
        return applicationRestRepository.save(application);
    }

    private String restPath(String suffix) {
        String basePath = repositoryRestConfiguration.getBasePath().toString();
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (!basePath.isEmpty() && !basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        return basePath + suffix;
    }
}
