let socket = new io("ws://localhost:3000");

socket.on('chat message', function(data) {
	console.log(data.sender + ": " + data.message)
});

socket.on('connect', function() {
	let username = prompt("Enter Username");
	socket.emit("username", {name: username});
	window.requestAnimationFrame(sendMessage);
});
function sendMessage() {
	let message = prompt("Enter Message");
	if (message == null) return; // If the cancel button is pressed
	socket.emit("chat message", {message: message});
	window.requestAnimationFrame(sendMessage);
}