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
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.joda.time.Duration;

import java.io.IOException;
import java.util.Iterator;

public class EthereumStreamingAnalyticsMain {
    public static void main(String[] args) {
        PipelineOptions options = PipelineOptionsFactory.create();
        Pipeline pipeline = Pipeline.create(options);
        pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription("projects/crypto-etl-ethereum-prod/subscriptions/allenday-streaming-test")
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
                        ParDo.of(new DoFn<Transaction, KV<Long, Long>>() {
                            @ProcessElement
                            public void processElement(ProcessContext c) {
                                Transaction tx = c.element();
                                c.output(KV.of(tx.getBlockNumber(),tx.getReceiptCumulativeGasUsed()));
                            }
                        }))
                .apply(Window.<KV<Long, Long>>into(FixedWindows.of(Duration.standardSeconds(30)))
                        .triggering(
                                AfterProcessingTime.pastFirstElementInPane()
                                        .plusDelayOf(Duration.standardSeconds(30)))
                        .withAllowedLateness(Duration.standardSeconds(30))
                        .discardingFiredPanes())
                .apply(GroupByKey.<Long, Long>create())
                .apply("Calculate Mean Gas per Block",
                        ParDo.of(new DoFn<KV<Long, Iterable<Long>>, KV<Long, Double>>() {
                            @ProcessElement
                            public void processElement(ProcessContext c) {
                                KV<Long, Iterable<Long>> kv = c.element();
                                Long blockNumber = kv.getKey();
                                Iterator<Long> values = kv.getValue().iterator();
                                Long sum = 0L;
                                Integer n = 0;
                                while (values.hasNext()) {
                                    sum += values.next();
                                    n++;
                                }
                                Double mean = (sum+0D) / n;
                                System.err.println(blockNumber + " " + mean);
                                c.output(KV.of(blockNumber, mean));
                            }
                        }))
                        ;
        pipeline.run();
    }
}
