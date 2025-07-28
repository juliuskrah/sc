package org.sc.ai.cli.github;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.AbstractResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link org.springframework.core.io.Resource} implementation for GitHub files.
 *
 * <p>Resources of the form (GET) {@code github://{owner}/{repo}/contents/{path}} 
 * which fetches a file from the default branch.
 * 
 * <p>For GitHub enterprise, it will look for environment variable {@code GITHUB_HOST}. 
 * For Authentication, the following environment variable will be used: {@code GITHUB_PERSONAL_ACCESS_TOKEN}
 * 
 * <p>Query parameters supported:
 * <ul>
 * <li>{@code ref}: string - The name of the commit/branch/tag. Default: the repository's default branch</li>
 * </ul>
 *
 * @author Julius Krah
 * @since 1.0
 */
public class GitHubResource extends AbstractResource {

    private static final String DEFAULT_GITHUB_HOST = "api.github.com";
    private static final String GITHUB_HOST_ENV = "GITHUB_HOST";
    private static final String GITHUB_TOKEN_ENV = "GITHUB_PERSONAL_ACCESS_TOKEN";
    
    protected final GitHubLocation location;
    protected final RestTemplate restTemplate;
    
    @Nullable
    private GitHubContentMetadata contentMetadata;

    @Nullable
    public static GitHubResource create(String location, RestTemplate restTemplate) {
        if (GitHubLocation.isGitHubResource(location)) {
            return new GitHubResource(location, restTemplate);
        }
        return null;
    }

    public GitHubResource(String location, RestTemplate restTemplate) {
        this(GitHubLocation.of(location), restTemplate);
    }

    public GitHubResource(String owner, String repo, String path, RestTemplate restTemplate) {
        this(GitHubLocation.of(owner, repo, path), restTemplate);
    }

    public GitHubResource(GitHubLocation location, RestTemplate restTemplate) {
        Assert.notNull(location, "location is required");
        Assert.notNull(restTemplate, "restTemplate is required");

        this.location = location;
        this.restTemplate = restTemplate;
    }

    @Override
    @NonNull
    public URL getURL() throws IOException {
        String githubHost = System.getenv(GITHUB_HOST_ENV);
        if (githubHost == null) {
            githubHost = DEFAULT_GITHUB_HOST;
        }
        
        String urlStr = String.format("https://%s/repos/%s/%s/contents/%s", 
            githubHost, location.getOwner(), location.getRepo(), location.getPath());
            
        if (StringUtils.hasText(location.getRef())) {
            urlStr = UriComponentsBuilder.fromUriString(urlStr)
                    .queryParam("ref", location.getRef())
                    .toUriString();
        }
        
        return URI.create(urlStr).toURL();
    }

    @Override
    @NonNull
    public String getDescription() {
        return location.toString();
    }

    @Override
    @NonNull
    public GitHubResource createRelative(@NonNull String relativePath) {
        return new GitHubResource(location.relative(relativePath), this.restTemplate);
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws IOException {
        fetchMetadataIfNeeded();
        
        GitHubContentMetadata metadata = contentMetadata;
        if (metadata == null || metadata.content == null) {
            throw new IOException("Failed to fetch content for: " + location);
        }
        
        // Clean and decode base64 content (GitHub API includes newlines in base64)
        String cleanedContent = metadata.content.replaceAll("\\s", "");
        byte[] decodedContent = Base64.getDecoder().decode(cleanedContent);
        return new ByteArrayInputStream(decodedContent);
    }

    @Override
    public boolean exists() {
        try {
            fetchMetadataIfNeeded();
            return contentMetadata != null;
        } catch (Exception _) {
            return false;
        }
    }

    @Override
    public long contentLength() throws IOException {
        fetchMetadataIfNeeded();
        GitHubContentMetadata metadata = contentMetadata;
        return metadata != null ? metadata.size : 0;
    }

    @Override
    public long lastModified() throws IOException {
        // GitHub API doesn't provide last modified in the contents endpoint
        // This would require additional API calls to get commit information
        return 0;
    }

    @Override
    @NonNull
    public File getFile() {
        throw new UnsupportedOperationException("GitHub resource cannot be resolved to java.io.File objects. "
                + "Use getInputStream() to retrieve the contents of the file!");
    }

    @Override
    @Nullable
    public String getFilename() {
        return this.location.getFileName();
    }

    public GitHubLocation getLocation() {
        return location;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GitHubResource that = (GitHubResource) obj;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @SuppressWarnings("unchecked")
    private void fetchMetadataIfNeeded() throws IOException {
        if (contentMetadata != null) {
            return;
        }
        
        try {
            String url = buildApiUrl();
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                this.contentMetadata = new GitHubContentMetadata(body);
            }
        } catch (Exception e) {
            throw new IOException("Failed to fetch GitHub resource: " + location, e);
        }
    }
    
    private String buildApiUrl() {
        String githubHost = System.getenv(GITHUB_HOST_ENV);
        if (githubHost == null) {
            githubHost = DEFAULT_GITHUB_HOST;
        }
        
        String baseUrl = String.format("https://%s/repos/%s/%s/contents/%s", 
            githubHost, location.getOwner(), location.getRepo(), location.getPath());
            
        if (StringUtils.hasText(location.getRef())) {
            return UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("ref", location.getRef())
                    .toUriString();
        }
        
        return baseUrl;
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "sc-cli/1.0");
        
        String token = System.getenv(GITHUB_TOKEN_ENV);
        if (StringUtils.hasText(token)) {
            headers.set("Authorization", "Bearer " + token);
        }
        
        return headers;
    }

    private static class GitHubContentMetadata {
        private final long size;
        private final String content;

        GitHubContentMetadata(Map<String, Object> response) {
            this.size = response.get("size") != null ? ((Number) response.get("size")).longValue() : 0;
            this.content = (String) response.get("content");
        }
    }
}
