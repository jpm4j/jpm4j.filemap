var w = 960, h = 500, color = d3.scale.category20c();

var treemap = d3.layout.treemap().size([ w, h ]).sticky(false).value(
		function(d) {
			return d.size;
		});

var svg = d3.select("#chart").append("svg").style("position", "relative")
		.style("width", w + "px").style("height", h + "px");

function doit() {
	d3.json("treemap.json", function(error,root) {
		var all = svg.datum(root)
			.selectAll("g")
			.data(treemap.nodes, function(d) {return d.n;})
			
		all.enter().append("g").call(enter)
		all.transition().duration(1500).call(set)
		all.exit().remove();
		
		setTimeout(doit, 2000);
	});
}

doit();

function enter() {
	this.append("rect").attr("fill", function(d) {
		return color(d.size);
	}).append("title").text(function(d) {
		return d.name + " (" + size(d.size) + ")";
	});
}

function size(n) {
	if ( n < 1024 ) return n;
	n = Math.floor(n/1024)
	if ( n < 1024 ) return n + " Kb";
	n = Math.floor(n/1024)
	if ( n < 1024 ) return n + " Mb";
	n = Math.floor(n/1024)
	
	return n + " Gb";
}
function set() {
	this.select("rect").attr("x", function(d) {
		return d.x;
	}).attr("y", function(d) {
		return d.y;
	}).attr("width", function(d) {
		return Math.max(0, d.dx - 1);
	}).attr("height", function(d) {
		return Math.max(0, d.dy - 1);
	})
}
