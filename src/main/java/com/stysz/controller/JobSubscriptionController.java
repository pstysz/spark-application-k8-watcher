package com.stysz.controller;

import com.stysz.model.JobStatusUpdate;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Sinks;

@Controller
class JobSubscriptionController {
    private final Sinks.Many<JobStatusUpdate> sink;

    JobSubscriptionController(Sinks.Many<JobStatusUpdate> sink) {
        this.sink = sink;
    }

    @SubscriptionMapping("jobStatusUpdates")
    public Publisher<JobStatusUpdate> subscribe() {
        return sink.asFlux();
    }
}