let socket = new io("ws://localhost:80");

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
	socket.emit("chat message", {message: message});
	window.requestAnimationFrame(sendMessage);
}