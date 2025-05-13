package com.stysz.controller;

import com.stysz.model.JobStatusUpdate;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Sinks;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class JobSubscriptionController {

    private final Sinks.Many<JobStatusUpdate> sink;

    @SubscriptionMapping("jobStatusUpdates")
    public Publisher<JobStatusUpdate> jobStatusUpdates() {
        return sink.asFlux();
    }

    @QueryMapping("jobsStatus")
    public List<JobStatusUpdate> jobsStatus() {
        return List.of(); // mock
    }
}