package org.sc.ai.cli.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Resolves {@link GitHubResource} for resource paths starting with github://. 
 * Registers resolver for GitHub protocol in {@link ResourceLoader}.
 *
 * <p>Resources of the form (GET) {@code github://{owner}/{repo}/contents/{path}} 
 * which fetches a file from the default branch.
 * 
 * <p>For GitHub enterprise, it will look for environment variable {@code GITHUB_HOST}. 
 * For Authentication, the following environment variable will be used: {@code GITHUB_PERSONAL_ACCESS_TOKEN}
 *
 * @author Julius Krah
 * @since 1.0
 */
@Component
public class GitHubProtocolResolver implements ProtocolResolver, ResourceLoaderAware, BeanFactoryPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubProtocolResolver.class);
    private static final String GITHUB_PROTOCOL = "github://";

    @Nullable
    private RestTemplate restTemplate;

    @Nullable
    private BeanFactory beanFactory;

    public GitHubProtocolResolver() {
    }

    // for direct usages outside of Spring context, when BeanFactory is not available
    public GitHubProtocolResolver(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Nullable
    public Resource resolve(@NonNull String location, @NonNull ResourceLoader resourceLoader) {
        if (!location.startsWith(GITHUB_PROTOCOL)) {
            return null;
        }

        RestTemplate httpClient = getRestTemplate();
        if (httpClient == null) {
            LOGGER.warn("Could not resolve RestTemplate. Resource {} could not be resolved", location);
            return null;
        }

        return GitHubResource.create(location, httpClient);
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        if (DefaultResourceLoader.class.isAssignableFrom(resourceLoader.getClass())) {
            ((DefaultResourceLoader) resourceLoader).addProtocolResolver(this);
        } else {
            LOGGER.warn("The provided delegate resource loader is not an implementation "
                    + "of DefaultResourceLoader. Custom Protocol using github:// prefix will not be enabled.");
        }
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Nullable
    private RestTemplate getRestTemplate() {
        if (restTemplate != null) {
            return restTemplate;
        } else if (beanFactory != null) {
            try {
                RestTemplate httpClient = beanFactory.getBean(RestTemplate.class);
                this.restTemplate = httpClient;
                return httpClient;
            } catch (Exception e) {
                LOGGER.debug("RestTemplate bean not found, creating default instance", e);
                // Create a default RestTemplate if none is available
                RestTemplate defaultRestTemplate = new RestTemplate();
                this.restTemplate = defaultRestTemplate;
                return defaultRestTemplate;
            }
        } else {
            // Fallback to creating a default RestTemplate
            RestTemplate defaultRestTemplate = new RestTemplate();
            this.restTemplate = defaultRestTemplate;
            return defaultRestTemplate;
        }
    }
}
