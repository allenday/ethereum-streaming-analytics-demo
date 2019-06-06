define(function () {
  // Initialize Firebase
  const config = {
    apiKey: "<Isert your Firebase API key here>",
    authDomain: "crypto-etl-ethereum-dev.firebaseapp.com",
    databaseURL: "https://crypto-etl-ethereum-dev.firebaseio.com",
    projectId: "crypto-etl-ethereum-dev",
    storageBucket: "crypto-etl-ethereum-dev.appspot.com",
    messagingSenderId: "869804627112",
    appId: "1:869804627112:web:49ab7d0d30c45dee"
  };

  firebase.initializeApp(config);

  const db = firebase.firestore();
  db.settings({
    timestampsInSnapshots: true
  });

  return db;
});
