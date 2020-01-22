package com.google.allenday.input;


import io.blockchainetl.ethereum.domain.Transaction;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collections;

class DeserializeTransactionTests {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    void deserialize() {
        PubsubMessage message = new PubsubMessage(getTransactionJson().getBytes(), Collections.emptyMap());

        PCollection<Transaction> transactions = testPipeline
                .apply(Create.of(message))
                .apply("Deserialize JSON", ParDo.of(new DeserializeTransaction()));

        PAssert
                .that(transactions)
                .containsInAnyOrder(getTransaction());

        testPipeline.run();
    }

    String getTransactionJson() {
        return "{" +
                "  \"hash\": \"0x4ff37b5d95c8e5aac2a290c6e72fb8bdc2978d5e2121cbc576855bf940136ee7\"," +
                "  \"nonce\": \"1046\"," +
                "  \"transaction_index\": \"65\"," +
                "  \"from_address\": \"0x8232372ab342e519eaa2ae9b8b08e6ed81dab2f3\"," +
                "  \"to_address\": \"0x0000000000c90bc353314b6911180ed7e06019a9\"," +
                "  \"value\": 0," +
                "  \"gas\": \"300000\"," +
                "  \"gas_price\": \"1500000000\"," +
                "  \"input\": \"0x000033efaf0000009f04563900000000000000000000030814aa\"," +
                "  \"receipt_cumulative_gas_used\": \"3299756\"," +
                "  \"receipt_gas_used\": \"25310\"," +
                "  \"receipt_status\": \"1\"," +
                "  \"block_timestamp\": \"2019-10-23 00:09:08 UTC\"," +
                "  \"block_number\": \"8793389\"," +
                "  \"block_hash\": \"0x7f741769857d3fd38858183df0d092756cfda6f63f71b7e449305df47333a71d\"," +
                "  \"_offset\": 548.0," +
                "  \"_replay_timestamp\": \"2020-01-21 21:44:37.800000 UTC\"," +
                "  \"_publish_timestamp\": \"2020-01-21 21:44:37.813793 UTC\"" +
                "}";
    }

    Transaction getTransaction() {
        Transaction tx = new Transaction();
        tx.setHash("0x4ff37b5d95c8e5aac2a290c6e72fb8bdc2978d5e2121cbc576855bf940136ee7");
        tx.setNonce("1046");
        tx.setTransactionIndex(65L);
        tx.setFromAddress("0x8232372ab342e519eaa2ae9b8b08e6ed81dab2f3");
        tx.setToAddress("0x0000000000c90bc353314b6911180ed7e06019a9");
        tx.setValue(BigInteger.ZERO);
        tx.setGas(300000L);
        tx.setGasPrice(1500000000L);
        tx.setInput("0x000033efaf0000009f04563900000000000000000000030814aa");
        tx.setReceiptCumulativeGasUsed(3299756L);
        tx.setReceiptGasUsed(25310L);
        tx.setReceiptStatus(1L);
        tx.setBlockTimestamp(1571789348000L);
        tx.setBlockNumber(8793389L);
        tx.setBlockHash("0x7f741769857d3fd38858183df0d092756cfda6f63f71b7e449305df47333a71d");

        return tx;
    }
}
