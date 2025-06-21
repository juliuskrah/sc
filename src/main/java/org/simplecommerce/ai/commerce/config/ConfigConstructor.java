package org.simplecommerce.ai.commerce.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A Constructor that can map specific record types via custom logic.
 */
public class ConfigConstructor extends Constructor {
    public ConfigConstructor(Class<?> root, LoaderOptions loaderOptions,
        Map<Class<?>, BiFunction<Function<Node, Object>, MappingNode, Object>> mappingNodeConstructors
    ) {
        super(root, loaderOptions);
        this.yamlClassConstructors.put(NodeId.mapping, new ConstructMapping() {
            @Override
            public Object construct(Node node) {
                for (var entry : mappingNodeConstructors.entrySet()) {
                    if (entry.getKey().isAssignableFrom(node.getType())) {
                        if (node.isTwoStepsConstruction()) {
                            throw new YAMLException("No second-step allowed for " + node.getType());
                        }
                        // Apply the supplied constructor function
                        return entry.getValue().apply(ConfigConstructor.this::constructObject, (MappingNode) node);
                    }
                }
                return super.construct(node);
            }

            @Override
            public void construct2ndStep(Node node, Object obj) {
                throw new YAMLException("Unexpected 2nd step for " + node.getType());
            }
        });
    }
}