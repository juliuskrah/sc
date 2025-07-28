package org.sc.ai.cli.github;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a GitHub resource location.
 * 
 * <p>Parses GitHub resource URLs of the form:
 * {@code github://{owner}/{repo}/contents/{path}?ref={ref}}
 * 
 * @author Julius Krah
 * @since 1.0
 */
public class GitHubLocation {

    private static final String GITHUB_PROTOCOL = "github://";
    private static final String CONTENTS_PATH = "/contents/";

    private final String owner;
    private final String repo;
    private final String path;
    private final String ref;

    public static boolean isGitHubResource(String location) {
        return location != null && location.startsWith(GITHUB_PROTOCOL);
    }

    public static GitHubLocation of(String location) {
        Assert.hasText(location, "Location must not be empty");
        Assert.isTrue(isGitHubResource(location), "Location must start with github://");

        // Remove protocol
        String remaining = location.substring(GITHUB_PROTOCOL.length());
        
        // Split query parameters
        String path = remaining;
        String ref = null;
        int queryIndex = remaining.indexOf('?');
        if (queryIndex != -1) {
            path = remaining.substring(0, queryIndex);
            String query = remaining.substring(queryIndex + 1);
            
            // Parse ref parameter
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2 && "ref".equals(keyValue[0])) {
                    ref = keyValue[1];
                    break;
                }
            }
        }
        
        // Parse owner/repo/contents/path
        String[] parts = path.split("/");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid GitHub location format. Expected: github://{owner}/{repo}/contents/{path}");
        }
        
        String owner = parts[0];
        String repo = parts[1];
        
        // Find contents part
        StringBuilder pathBuilder = new StringBuilder();
        boolean foundContents = false;
        for (int i = 2; i < parts.length; i++) {
            if ("contents".equals(parts[i]) && !foundContents) {
                foundContents = true;
                continue;
            }
            if (foundContents) {
                if (!pathBuilder.isEmpty()) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(parts[i]);
            }
        }
        
        if (!foundContents) {
            throw new IllegalArgumentException("Invalid GitHub location format. Expected: github://{owner}/{repo}/contents/{path}");
        }
        
        return new GitHubLocation(owner, repo, pathBuilder.toString(), ref);
    }

    public static GitHubLocation of(String owner, String repo, String path) {
        return of(owner, repo, path, null);
    }

    public static GitHubLocation of(String owner, String repo, String path, @Nullable String ref) {
        return new GitHubLocation(owner, repo, path, ref);
    }

    private GitHubLocation(String owner, String repo, String path, @Nullable String ref) {
        Assert.hasText(owner, "Owner must not be empty");
        Assert.hasText(repo, "Repository must not be empty");
        Assert.hasText(path, "Path must not be empty");
        
        this.owner = owner;
        this.repo = repo;
        this.path = path;
        this.ref = ref;
    }

    @NonNull
    public String getOwner() {
        return owner;
    }

    @NonNull
    public String getRepo() {
        return repo;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    @Nullable
    public String getRef() {
        return ref;
    }

    @NonNull
    public String getFileName() {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    @NonNull
    public GitHubLocation relative(String relativePath) {
        Assert.hasText(relativePath, "Relative path must not be empty");
        
        String newPath;
        if (relativePath.startsWith("/")) {
            // Absolute path
            newPath = relativePath.substring(1);
        } else {
            // Relative path
            int lastSlash = path.lastIndexOf('/');
            String basePath = lastSlash >= 0 ? path.substring(0, lastSlash) : "";
            String pathSeparator = "/";
            newPath = StringUtils.hasText(basePath) ? basePath + pathSeparator + relativePath : relativePath;
        }
        
        return new GitHubLocation(owner, repo, newPath, ref);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GITHUB_PROTOCOL).append(owner).append("/").append(repo).append(CONTENTS_PATH).append(path);
        if (StringUtils.hasText(ref)) {
            sb.append("?ref=").append(ref);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GitHubLocation that = (GitHubLocation) obj;
        return Objects.equals(owner, that.owner) &&
               Objects.equals(repo, that.repo) &&
               Objects.equals(path, that.path) &&
               Objects.equals(ref, that.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, repo, path, ref);
    }
}
