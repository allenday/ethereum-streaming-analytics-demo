define(function () {
    const firebaseConfig = {
        apiKey: "AIzaSyD5oDrXfleV1ht410VIM742N-epcA7BzLE",
        authDomain: "crypto-etl-ethereum-dev.firebaseapp.com",
        databaseURL: "https://crypto-etl-ethereum-dev.firebaseio.com",
        projectId: "crypto-etl-ethereum-dev",
        storageBucket: "crypto-etl-ethereum-dev.appspot.com",
        messagingSenderId: "869804627112",
        appId: "1:869804627112:web:49ab7d0d30c45dee"
    };
    firebase.initializeApp(firebaseConfig);

    return firebase.firestore();
});