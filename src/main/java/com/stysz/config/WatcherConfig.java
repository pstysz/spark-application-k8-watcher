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

@Configuration
public class WatcherConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    public Sinks.Many<JobStatusUpdate> jobStatusSink(KubernetesClient client) {
        Sinks.Many<JobStatusUpdate> sink = Sinks.many().multicast().onBackpressureBuffer();

        CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                .withGroup("sparkoperator.k8s.io")
                .withVersion("v1beta2")
                .withScope("Namespaced")
                .withPlural("sparkapplications")
                .build();

        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> resources =
                client.genericKubernetesResources(context);

        resources.inNamespace("default").watch(new Watcher<>() {
            @Override
            public void eventReceived(Action action, GenericKubernetesResource resource) {
                try {
                    String name = resource.getMetadata().getName();
                    Map<String, Object> status = resource.getAdditionalProperties();
                    if (status != null && status.containsKey("status")) {
                        Map<String, Object> statusMap = (Map<String, Object>) status.get("status");
                        if (statusMap.containsKey("applicationState")) {
                            Map<String, String> appState = (Map<String, String>) statusMap.get("applicationState");
                            String state = appState.get("state");
                            sink.tryEmitNext(new JobStatusUpdate(name, state, Instant.now().toString()));
                        }
                    }
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
