package com.google.allenday;

import com.google.allenday.firestore.WriteDataToFirestoreDbFn;
import io.blockchainetl.ethereum.domain.Transaction;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;

public class Main {

    // TODO: extract to arguments
    private static final int SLIDING_WINDOW_SIZE = 120;
    private static final int SLIDING_WINDOW_PERIOD = 30;

    public static void main(String[] args) {
        PipelineOptions options = PipelineOptionsFactory.fromArgs(args).as(DataflowPipelineOptions.class);
        Pipeline pipeline = Pipeline.create(options);
        pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromTopic("projects/crypto-etl-ethereum-prod/topics/crypto_ethereum.transactions")
                        )
                .apply("Deserialize JSON",  ParDo.of(new DeserializeTransaction()))
                .apply("Get Gas Value",
                        ParDo.of(new DoFn<Transaction, Long>() {
                            @ProcessElement
                            public void processElement(ProcessContext c) {
                                Transaction tx = c.element();
                                c.output(tx.getReceiptCumulativeGasUsed());
                            }
                        }))
                .apply(Window.<Long>into(
                        SlidingWindows.of(Duration.standardSeconds(SLIDING_WINDOW_SIZE))
                                .every(Duration.standardSeconds(SLIDING_WINDOW_PERIOD)))
                        .triggering(
                                AfterProcessingTime.pastFirstElementInPane()
                                        .plusDelayOf(Duration.standardSeconds(30)))
                        .withAllowedLateness(Duration.standardSeconds(30))
                        .discardingFiredPanes())
                .apply(Combine.globally(new MinMaxMeanFn()).withoutDefaults())
                .apply(ParDo.of(new DoFn<Stats, DataPoint>() {
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
        .apply(ParDo.of(new WriteDataToFirestoreDbFn("ethereum-streaming-dev")))
                        ;
        pipeline.run();
    }
}
