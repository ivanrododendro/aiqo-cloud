package org.iro.aiqo.collector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "collector.local.directory=",
        "collector.local.file-patterns[0]="
})
class AiqoCollectorApplicationTests {

    @Test
    void contextLoads() {
        // Ensures Spring context starts up with the default configuration
    }
}
