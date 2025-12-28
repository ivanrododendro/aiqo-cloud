**Language**
Always respond in English

- **Logging**: components that produce application logs use Lombok `@Slf4j` to obtain the preconfigured SLF4J logger. Example: `org.iro.aiqo.<module>.logparser.parser.PlainTextLogFileParser`.
- **Domain POJOs**: all data structures (e.g., `LogEntry`, `Buffers`, `Wal`, `ParseResult`) use `@Data` to generate getters, setters, `equals`, `hashCode`, and `toString`. Add `@AllArgsConstructor`/`@NoArgsConstructor` where required to preserve constructors.
- **Mapping**: for DTO, entity, and view transformations use MapStruct interfaces annotated with `@Mapper`. Implementations are generated in the build folder; do not add ad-hoc mappers by hand.
- **Adoption rule**: when introducing new POJOs or classes that require logging or mapping, consistently use the corresponding Lombok annotations (`@Data` for data, `@Slf4j` for logs) and define MapStruct mappers for structured conversions to keep style consistent and reduce boilerplate.

- Every new class and every new method must be accompanied by unit tests
- All REST clients must be implemented as standalone classes based on RestTemplate
- Logging: one INFO log entry for every entry method of a use case, for example a REST controller
- Logging: DEBUG logs must allow the full logical execution path to be followed with contextual variable details
- Package structure:
  - org.iro.aiqo.<module>
    - local: all classes/interfaces for local file collection
    - log: high-level classes/interfaces common to any log handling
    - logparser: parsing-related files common to all parsing
    - logparser.model: output model of parsing
    - logparser.parser: different parser implementations
    - api: all Java files related to persistence APIs
    - api.<domain>: all objects related to the domain API (Client, DTO, ...)
    - service: services, i.e. *Service classes with logic and call aggregation
  - In pom.xml always use properties for dependency versions
  - Prefer existing Java implementations over specific custom implementations when possible
  - If Java implementations are not available, check for and propose third-party libraries
  - Do not add third-party libraries without asking
  - Put all module parameters in `aiqo.<module>.properties`
  - Use `application.properties` only for Spring properties
  - Do not create Java packages with fewer than three classes; start a package only from three classes onward
  - Never use class variables as cache, do not implement application caching mechanisms unless explicitly requested

Validation
- Use Bean Validation (JSR 380/303) for method parameter validation
- Use `java.util.Objects.requireNonNull` and `org.springframework.util.StringUtils.hasText` for in-method validation

Tests
- Use Jupiter for test automation
- Use the Given-When-Then BDD form to structure tests, highlighting preconditions, subject under test, and expected result

Exception handling
- Always log every exception at error level with its stacktrace, unless explicitly told otherwise
