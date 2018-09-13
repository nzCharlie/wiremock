package com.github.tomakehurst.wiremock;

import com.github.tomakehurst.wiremock.common.Timing;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LogTimingAcceptanceTest extends AcceptanceTestBase {

    @BeforeClass
    public static void setupServer() {
        setupServer(options().asynchronousResponseEnabled(true).asynchronousResponseThreads(5));
    }

    @Test
    public void serveEventIncludesTotalAndServeDuration() {
        stubFor(get("/time-me").willReturn(ok()));

        // Create some work
        for (int i = 0; i < 50; i++) {
            stubFor(get("/time-me/" + i)
                .willReturn(ok()));
        }

        testClient.get("/time-me");

        ServeEvent serveEvent = getAllServeEvents().get(0);

        assertThat(serveEvent.getTiming().getServeTime(), greaterThan(0));
        assertThat(serveEvent.getTiming().getTotalTime(), greaterThan(0));
    }

    @Test
    public void includesAddedDelayInTotalWhenAsync() {
        final int DELAY = 500;

        stubFor(post("/time-me/async")
            .withRequestBody(equalToXml("<value>1111</value>"))
            .willReturn(ok().withFixedDelay(DELAY)));

        // Create some work
        for (int i = 0; i < 50; i++) {
            stubFor(post("/time-me/async")
                .withRequestBody(equalToXml("<value>123456" + i + " </value>"))
                .willReturn(ok()));
        }

        testClient.postXml("/time-me/async", "<value>1111</value>");
        ServeEvent serveEvent = getAllServeEvents().get(0);

        Timing timing = serveEvent.getTiming();
        assertThat(timing.getAddedDelay(), is(DELAY));
        assertThat(timing.getProcessTime(), greaterThan(0));
        assertThat(timing.getResponseSendTime(), greaterThan(0));
        assertThat(timing.getServeTime(), is(timing.getProcessTime() + timing.getResponseSendTime()));
        assertThat(timing.getTotalTime(), is(timing.getProcessTime() + timing.getResponseSendTime() + DELAY));
    }
}
