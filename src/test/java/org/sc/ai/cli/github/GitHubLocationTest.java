package org.sc.ai.cli.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GitHubLocationTest {

    @Test
    void shouldParseValidGitHubLocation() {
        // Given
        String location = "github://owner/repo/contents/path/to/file.txt";

        // When
        GitHubLocation gitHubLocation = GitHubLocation.of(location);

        // Then
        assertThat(gitHubLocation.getOwner()).isEqualTo("owner");
        assertThat(gitHubLocation.getRepo()).isEqualTo("repo");
        assertThat(gitHubLocation.getPath()).isEqualTo("path/to/file.txt");
        assertThat(gitHubLocation.getRef()).isNull();
        assertThat(gitHubLocation.getFileName()).isEqualTo("file.txt");
    }

    @Test
    void shouldParseGitHubLocationWithRef() {
        // Given
        String location = "github://owner/repo/contents/path/to/file.txt?ref=main";

        // When
        GitHubLocation gitHubLocation = GitHubLocation.of(location);

        // Then
        assertThat(gitHubLocation.getOwner()).isEqualTo("owner");
        assertThat(gitHubLocation.getRepo()).isEqualTo("repo");
        assertThat(gitHubLocation.getPath()).isEqualTo("path/to/file.txt");
        assertThat(gitHubLocation.getRef()).isEqualTo("main");
    }

    @Test
    void shouldCreateFromComponents() {
        // When
        GitHubLocation location = GitHubLocation.of("owner", "repo", "path/to/file.txt");

        // Then
        assertThat(location.getOwner()).isEqualTo("owner");
        assertThat(location.getRepo()).isEqualTo("repo");
        assertThat(location.getPath()).isEqualTo("path/to/file.txt");
        assertThat(location.getRef()).isNull();
    }

    @Test
    void shouldCreateFromComponentsWithRef() {
        // When
        GitHubLocation location = GitHubLocation.of("owner", "repo", "path/to/file.txt", "dev");

        // Then
        assertThat(location.getOwner()).isEqualTo("owner");
        assertThat(location.getRepo()).isEqualTo("repo");
        assertThat(location.getPath()).isEqualTo("path/to/file.txt");
        assertThat(location.getRef()).isEqualTo("dev");
    }

    @Test
    void shouldCreateRelativeLocation() {
        // Given
        GitHubLocation base = GitHubLocation.of("owner", "repo", "path/to/file.txt");

        // When
        GitHubLocation relative = base.relative("other.txt");

        // Then
        assertThat(relative.getOwner()).isEqualTo("owner");
        assertThat(relative.getRepo()).isEqualTo("repo");
        assertThat(relative.getPath()).isEqualTo("path/to/other.txt");
    }

    @Test
    void shouldThrowExceptionForInvalidLocation() {
        // When / Then
        assertThatThrownBy(() -> GitHubLocation.of("invalid://location"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Location must start with github://");
    }

    @Test
    void shouldThrowExceptionForInvalidFormat() {
        // When / Then
        assertThatThrownBy(() -> GitHubLocation.of("github://owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid GitHub location format");
    }

    @Test
    void shouldCheckIfLocationIsGitHubResource() {
        assertThat(GitHubLocation.isGitHubResource("github://owner/repo/contents/file.txt")).isTrue();
        assertThat(GitHubLocation.isGitHubResource("http://example.com")).isFalse();
        assertThat(GitHubLocation.isGitHubResource(null)).isFalse();
    }

    @Test
    void shouldGenerateCorrectToString() {
        // Given
        GitHubLocation location = GitHubLocation.of("owner", "repo", "path/to/file.txt");

        // When
        String string = location.toString();

        // Then
        assertThat(string).isEqualTo("github://owner/repo/contents/path/to/file.txt");
    }

    @Test
    void shouldGenerateCorrectToStringWithRef() {
        // Given
        GitHubLocation location = GitHubLocation.of("owner", "repo", "path/to/file.txt", "main");

        // When
        String string = location.toString();

        // Then
        assertThat(string).isEqualTo("github://owner/repo/contents/path/to/file.txt?ref=main");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        GitHubLocation location1 = GitHubLocation.of("owner", "repo", "path/to/file.txt");
        GitHubLocation location2 = GitHubLocation.of("owner", "repo", "path/to/file.txt");
        GitHubLocation location3 = GitHubLocation.of("owner", "repo", "path/to/other.txt");

        // Then
        assertThat(location1)
                .isEqualTo(location2)
                .isNotEqualTo(location3)
                .hasSameHashCodeAs(location2);
        
        assertThat(location1.hashCode()).isNotEqualTo(location3.hashCode());
    }
}
