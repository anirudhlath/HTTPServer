"use strict";

let socket = new WebSocket("ws://" + location.host);
let sendButton = document.querySelector("#sendMessageButton");
let joinButton = document.querySelector("#joinRoomButton");

socket.onopen = (event) => {
    console.log("WebSocket connection is open!");
};

socket.onclose = (event) => {
    console.log("WebSocket connection is closed!");
};

socket.onerror = (event) => {
    console.log("WebSocket connection has an error: ", event);
};

socket.onmessage = (event) => {
    console.log("WebSocket connection has received a message!");
    let chat = document.querySelector("#chatbox");
    console.log(event.data);
    let parsedData = JSON.parse(event.data);
    let innerData = "<p><span id='user'>" + parsedData.user + ": " + "</span><span id='userMessage'>" + parsedData.message + "</span></p>";
    chat.innerHTML += innerData;
};

joinButton.addEventListener("click", () => {
    let chatRoom = document.querySelector("#chatroom");
    let chatRoomString = chatRoom.value;
    let innerData = "join " + chatRoomString;

    socket.send(innerData);

    console.log("Joined room " + chatRoomString);
});

sendButton.addEventListener("click", () => {
    let userName = document.querySelector("#username");
    let message = document.querySelector("#message");
    let innerData = userName.value + " " + message.value;

    socket.send(innerData);

    message.value = "";

    console.log("Message Sent");
});

