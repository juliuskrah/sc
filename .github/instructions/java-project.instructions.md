---
applyTo: '**/*.java'
description: This instruction file provides coding standards and preferences for Java projects
---

# Framework and libraries

- Prefer using Spring libraries e.g. When dealing with local resources, instead of using `java.nio.file.Files` and `java.nio.file.Paths`, use `org.springframework.core.io.ResourceLoader` to load resources.
- When working with Picocli use declarative annotations like `@Command`, `@Option`, `@Parameters` instead of imperative style.

# Testing

- Test cases are written using JUnit 5 and Mockito annotations should be used
  where it makes sense e.g. `@Mock`, `@InjectMocks`. Annotations for Mockito are activated by
  `@ExtendWith(MockitoExtension.class)` on the test class, however when a test class is
  annotated with `@SpringBootTest`, the Mockito annotations are not processed hence should not be used. Use the Spring Boot test annotations such as `@MockitoBean`, `@MockitoSpyBean`, etc. in that case.
