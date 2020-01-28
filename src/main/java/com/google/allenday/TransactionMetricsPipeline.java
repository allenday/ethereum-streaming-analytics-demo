package com.google.allenday;

import com.google.allenday.calculation.MinMaxMeanFn;
import com.google.allenday.calculation.Stats;
import com.google.allenday.firestore.DataPoint;
import com.google.allenday.firestore.WriteDataToFirestoreDbFn;
import com.google.allenday.input.DeserializeTransaction;
import com.google.allenday.transaction.EthereumTransaction;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;

public class TransactionMetricsPipeline {

    public static void main(String[] args) {
        TransactionMetricsPipelineOptions options =
                PipelineOptionsFactory.fromArgs(args).withValidation().as(TransactionMetricsPipelineOptions.class);

        Pipeline pipeline = Pipeline.create(options);
        pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromTopic(options.getInputDataTopic()))
                .apply("Deserialize JSON", ParDo.of(new DeserializeTransaction()))
                .apply("Get Gas Value",
                        ParDo.of(new DoFn<EthereumTransaction, Long>() {
                            @ProcessElement
                            public void processElement(ProcessContext c) {
                                EthereumTransaction tx = c.element();
                                c.output(tx.getGasPrice());
                            }
                        }))
                .apply(Window.<Long>into(
                        SlidingWindows.of(Duration.standardSeconds(options.getSlidingWindowSize()))
                                .every(Duration.standardSeconds(options.getSlidingWindowPeriod())))
                        .triggering(
                                AfterProcessingTime.pastFirstElementInPane()
                                        .plusDelayOf(Duration.standardSeconds(30)))
                        .withAllowedLateness(Duration.standardSeconds(30))
                        .discardingFiredPanes())
                .apply("Calculate statistic", Combine.globally(new MinMaxMeanFn()).withoutDefaults())
                .apply("Prepare data points", ParDo.of(new DoFn<Stats, DataPoint>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Stats stats = c.element();

                        System.err.format("timestamp: %d, min: %d, max: %d, mean: %f\n",
                                c.timestamp().getMillis(), stats.getMin(), stats.getMax(), stats.getMean()
                        );

                        DataPoint dataPoint = new DataPoint();
                        dataPoint.setTimestamp(c.timestamp().getMillis());
                        dataPoint.setStats(stats);

                        c.output(dataPoint);
                    }
                }))
                .apply("Write to FireStore", ParDo.of(
                        new WriteDataToFirestoreDbFn(options.getProject(), options.getFirestoreCollection())
                ))
        ;
        pipeline.run();
    }
}
