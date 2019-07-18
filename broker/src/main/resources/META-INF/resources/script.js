class Source {

	constructor() {
		this.source = new EventSource("http://localhost:8080/events/eventA/subscriptions");
		this.source.onopen    = (e) => console.log(e);
		this.source.onmessage = ({data})  => this.displayLastEvent(data);
	}

	displayLastEvent(data) {
		document.getElementById("events-container").innerHTML = data;
	}
}

new Source();