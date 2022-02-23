class io {
	#socket;
	#dataHandlers;
	
	constructor(address) {
		this.#socket = new WebSocket(address);
		this.#dataHandlers = new Map();
		
		this.#socket.onmessage = (data) => {
			let dataobj = JSON.parse(data.data);
			if (this.#dataHandlers.has(dataobj.event)) {
				this.#dataHandlers.get(dataobj.event)(dataobj.data);
			}
		}
	}
	on(event, dataHandler) {
		switch (event) {
			case "connect":
				this.#socket.onopen = dataHandler;
				break;
			case "disconnect":
				this.#socket.onclose = dataHandler;
				break;
			default:
				this.#dataHandlers.set(event, dataHandler);
		}
	}
	emit(event, data) {
		let strData = "{'event': '" + new String(event) + "', 'data': '" + JSON.stringify(data) + "'}";
		this.#socket.send(strData);
	}
}