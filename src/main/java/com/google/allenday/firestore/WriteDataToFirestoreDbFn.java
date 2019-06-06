package com.google.allenday.firestore;

import com.google.cloud.firestore.WriteResult;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Writes data to Firestore Database
 */
public class WriteDataToFirestoreDbFn extends DoFn<DataPoint, String> {


    private Logger LOG = LoggerFactory.getLogger(WriteDataToFirestoreDbFn.class);

    private FirestoreService firebaseDatastoreService;
    private String projectId;
    private String collection;

    public WriteDataToFirestoreDbFn(String projectId, String collection) {
        this.projectId = projectId;
        this.collection = collection;
    }

    @Setup
    public void setup() {
        try {
            firebaseDatastoreService = FirestoreService.initialize(projectId);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        if (firebaseDatastoreService == null) {
            throw new RuntimeException("Firebase service is not initialized");
        }

        DataPoint data = c.element();
        Future<WriteResult> result = firebaseDatastoreService.writeObjectToFirestoreCollection(
                collection,
                String.format("t_%d", data.getTimestamp()),
                data
        );
        c.output(result.toString());
    }
}
