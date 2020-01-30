package com.google.allenday;


import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

public interface TransactionMetricsPipelineOptions extends DataflowPipelineOptions {

    @Description("GCP PubSub topic name to subscribe and read messages from")
    @Validation.Required
    String getInputDataTopic();

    void setInputDataTopic(String value);

    @Description("GCP Firestore collection name to write results to")
    @Validation.Required
    String getFirestoreCollection();

    void setFirestoreCollection(String value);

    @Description("Size of calculation sliding window in seconds")
    @Default.Integer(120)
    Integer getSlidingWindowSize();

    void setSlidingWindowSize(Integer value);

    @Description("Period of calculation sliding window in seconds")
    @Default.Integer(15)
    Integer getSlidingWindowPeriod();

    void setSlidingWindowPeriod(Integer value);
}
