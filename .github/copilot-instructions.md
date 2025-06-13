# Java instructions

We use gradle groovy DSL to manage dependencies for this project. 
This is a command line application so wherever possible we use the picocli library.
Java 22 is the minimum version preferred for this project.
Test cases are written using JUnit 5 and Mockito annotations should be used where it makes sense e.g. `@Mock`, `@InjectMocks`. Annotations for Mockito are activated by `@ExtendWith(MockitoExtension.class)` on the test class, however when a test class is annotated with `@SpringBootTest`, the Mockito annotations are not processed hence should not be used. Use the Spring Boot test annotations such as `@MockitoBean`, `@MockitoSpyBean`, etc. in that case.
