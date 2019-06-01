package com.google.allenday;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.blockchainetl.ethereum.domain.Transaction;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        PipelineOptions options = PipelineOptionsFactory.fromArgs(args).create();
        Pipeline pipeline = Pipeline.create(options);
        pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromTopic("projects/ethereum-streaming-dev/topics/crypto_ethereum.transactions")
                        )
                .apply("Deserialize JSON",
                        ParDo.of(new DoFn<PubsubMessage, Transaction>() {
                            @ProcessElement
                            public void processElement(ProcessContext c) {
                                PubsubMessage msg = c.element();
                                String jsonString = new String(msg.getPayload());
                                ObjectMapper mapper = new ObjectMapper();
                                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
                                try {
                                    Transaction tx = mapper.readValue(jsonString, Transaction.class);
                                    c.output(tx);
                                } catch (JsonParseException e1) {
                                    e1.printStackTrace();
                                } catch (JsonMappingException e1) {
                                    e1.printStackTrace();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }))
                .apply("Get Block ID and Gas",
                        ParDo.of(new DoFn<Transaction, Long>() {
                            @ProcessElement
                            public void processElement(ProcessContext c) {
                                Transaction tx = c.element();
                                c.output(tx.getReceiptCumulativeGasUsed());
                            }
                        }))
                .apply(Window.<Long>into(
                        SlidingWindows.of(Duration.standardSeconds(120))
                                .every(Duration.standardSeconds(120)))
                        .triggering(
                                AfterProcessingTime.pastFirstElementInPane()
                                        .plusDelayOf(Duration.standardSeconds(30)))
                        .withAllowedLateness(Duration.standardSeconds(30))
                        .discardingFiredPanes())
                .apply(Mean.<Long>globally().withoutDefaults())
                .apply(ParDo.of(new DoFn<Double, Double>() {
                            @ProcessElement
                            public void processElement(ProcessContext c) {
                                System.err.println(c.element());
                                c.output(c.element());
                            }
                }))
//                .apply(GroupByKey.<Long, Long>create())
//                .apply("Calculate Mean Gas per Block",
//                        ParDo.of(new DoFn<KV<Long, Iterable<Long>>, KV<Long, Double>>() {
//                            @ProcessElement
//                            public void processElement(ProcessContext c) {
//                                KV<Long, Iterable<Long>> kv = c.element();
//                                Long blockNumber = kv.getKey();
//                                Iterator<Long> values = kv.getValue().iterator();
//                                Long sum = 0L;
//                                Integer n = 0;
//                                while (values.hasNext()) {
//                                    sum += values.next();
//                                    n++;
//                                }
//                                Double mean = (sum+0D) / n;
//                                System.err.println(blockNumber + " " + mean);
//                                c.output(KV.of(blockNumber, mean));
//                            }
//                        }))
                        ;
        pipeline.run();
    }
}
