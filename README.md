# ethereum-streaming-analytics-demo
Process Ethereum data with Pubsub and Dataflow

## Build
```bash
mvn clean package
```

## Run
```bash
export RUNNER=org.apache.beam.runners.dataflow.DataflowRunner
export PROJECT=ethereum-streaming-dev
export TOPIC=projects/ethereum-streaming-dev/topics/crypto_ethereum.transactions
export FIRESTORE_COLLECTION=demo_gas_price
java -cp target/ethereum-streaming-analytics-1.0-SNAPSHOT.jar com.google.allenday.TransactionMetricsPipeline \
  --runner=$RUNNER \
  --project=$PROJECT \
  --inputDataTopic=$TOPIC \
  --firestoreCollection=$FIRESTORE_COLLECTION \
  --streaming=true
```