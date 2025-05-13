package com.stysz.config;

import com.stysz.model.JobStatusUpdate;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Configuration
public class WatcherConfig {

    private final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
            .withGroup("sparkoperator.k8s.io")
            .withVersion("v1beta2")
            .withScope("Namespaced")
            .withPlural("sparkapplications")
            .build();

    @Bean
    public MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> resources(KubernetesClient client) {
        return client.genericKubernetesResources(context);
    }

    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    public Sinks.Many<JobStatusUpdate> jobStatusSink(MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> resources) {
        Sinks.Many<JobStatusUpdate> sink = Sinks.many().replay().latest();

        resources.inNamespace("default").watch(new Watcher<>() {
            @Override
            public void eventReceived(Action action, GenericKubernetesResource resource) {
                try {
                    Optional.ofNullable(resource.getAdditionalProperties().get("status"))
                            .filter(Map.class::isInstance)
                            .map(Map.class::cast)
                            .ifPresent(statusMap -> {
                                String name = resource.getMetadata().getName();
                                String state = Optional.ofNullable(statusMap.get("applicationState"))
                                        .filter(Map.class::isInstance)
                                        .map(m -> ((Map<?, ?>) m).get("state"))
                                        .map(Object::toString)
                                        .orElse(null);

                                String appId = Optional.ofNullable(statusMap.get("sparkApplicationId"))
                                        .map(Object::toString)
                                        .orElse(null);

                                JobStatusUpdate update = new JobStatusUpdate(appId, name, state, Instant.now().toString());
                                System.out.println(update);
                                sink.tryEmitNext(update);
                            });
                } catch (Exception e) {
                    System.err.println("Failed to parse SparkApplication status: " + e.getMessage());
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                System.err.println("Watcher closed: " + cause);
            }
        });

        return sink;
    }
}
