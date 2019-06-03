define(function () {
  // Initialize Firebase
  var config = {
    apiKey: "AIzaSyCRuxL33WHgy3E5zeBMke_8rlhCIrZhl4A",
    authDomain: "ethereum-streaming-dev.firebaseapp.com",
    databaseURL: "https://ethereum-streaming-dev.firebaseio.com",
    projectId: "ethereum-streaming-dev",
    storageBucket: "",
    messagingSenderId: "249269731739",
    appId: "1:249269731739:web:97dd7bfaa4991d5f"
  };

  firebase.initializeApp(config);

  const db = firebase.firestore();
  db.settings({
    timestampsInSnapshots: true
  });

  return db;
});