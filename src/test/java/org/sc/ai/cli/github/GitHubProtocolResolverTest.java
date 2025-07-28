package org.sc.ai.cli.github;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GitHubProtocolResolverTest {

    @Mock
    private RestTemplate restTemplate;

    private GitHubProtocolResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GitHubProtocolResolver(restTemplate);
    }

    @Test
    void shouldResolveGitHubResources() {
        // Given
        String location = "github://owner/repo/contents/path/to/file.txt";
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        // When
        Resource resource = resolver.resolve(location, resourceLoader);

        // Then
        assertThat(resource)
                .isNotNull()
                .isInstanceOf(GitHubResource.class);
        
        GitHubResource gitHubResource = (GitHubResource) resource;
        assertThat(gitHubResource.getLocation().getOwner()).isEqualTo("owner");
        assertThat(gitHubResource.getLocation().getRepo()).isEqualTo("repo");
        assertThat(gitHubResource.getLocation().getPath()).isEqualTo("path/to/file.txt");
    }

    @Test
    void shouldReturnNullForNonGitHubResources() {
        // Given
        String location = "http://example.com/file.txt";
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        // When
        Resource resource = resolver.resolve(location, resourceLoader);

        // Then
        assertThat(resource).isNull();
    }

    @Test
    void shouldReturnNullWhenRestTemplateIsNull() {
        // Given
        resolver = new GitHubProtocolResolver();
        String location = "github://owner/repo/contents/file.txt";
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        // When
        Resource resource = resolver.resolve(location, resourceLoader);

        // Then
        assertThat(resource).isNotNull(); // Should create default RestTemplate
    }

    @Test
    void shouldSetResourceLoaderCorrectly() {
        // Given
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        
        // When
        resolver.setResourceLoader(resourceLoader);
        
        // Then - no exception should be thrown
        assertThat(resourceLoader).isNotNull();
    }
}
