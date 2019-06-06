require(['db'], function (db) {
  var width = 1000;
  var height = 1000;
  var svg = d3.select("svg")
    .style("width", width)
    .style("height", height)
    //  .style("background-color","#FFFAFA");
    ;
  var link, node, circles, labels;

  link = svg.append("g")
    .attr("class", "links")
    .selectAll("line")
    .append("line")
    .attr("stroke-width", function (d) {
      return 1 * d.value;
    });

  node = svg.append("g")
    .attr("class", "nodes")
    .selectAll("g")
    .append("g");

  circles = node.append("circle")
    .attr("r", function (d) { return 5; })
    .call(d3.drag()
      .on("start", dragstarted)
      .on("drag", dragged)
      .on("end", dragended)
    );

  labels = node.append("text")
    .attr('x', 6)
    .attr('y', 3)
    .text(function (d) { return d.id; });

  node.append("title")
    .text(function (d) { return d.id; });

  function ticked() {
    link
      .attr("x1", function (d) { return d.source.x; })
      .attr("y1", function (d) { return d.source.y; })
      .attr("x2", function (d) { return d.target.x; })
      .attr("y2", function (d) { return d.target.y; });
    node
      .attr("transform", function (d) {
        return "translate(" + d.x + "," + d.y + ")";
      });
  }

  var simulation = d3.forceSimulation()
    .force("link",
      d3.forceLink()
        .id(function (d) { return d.id; })
        .distance(function (d) {
          return 300;
          return Math.log(d.value);
        })
        .strength(0.08)
    )
    .force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(width / 2, height / 2))
    .on("tick", ticked);

  function findLink(links, source, target) {
    var i;
    for (i = 0; i < links.length; i++) {
      if (links[i].source == source && links[i].target == target) {
        return links[i];
      }
    }
    return null;
  };

  d3.json("https://raw.githubusercontent.com/allenday/force-layout-from-csv/master/empty.json", function (error, graph) {
    if (error) throw error;
    // add nodes collection
    graph.nodes = [];
    uNodes = new Set();

    db.collection("demo3").doc('latest').collection('volume')
      .onSnapshot(querySnapshot => {
        querySnapshot.docChanges().forEach(change => {
          var link;
          var data = change.doc.data();
          var amount = data["amount"] / 10e18;
          var source = data.from.replace("_hw", "").replace("_uw", "");
          var target = data.to.replace("_hw", "").replace("_uw", "");

          if (source != target) {
            // create or update links
            link = findLink(graph.links, source, target);
            if (!link) {
              link = { source: source, target: target, value: 1 + amount };
              graph.links.push(link);
            } else {
              link.value = 1 + amount;
            }

            // create missing nodes
            if (!uNodes.has(source)) {
              uNodes.add(source);
              graph.nodes.push({ id: source, group: 1 })
            }
            if (!uNodes.has(target)) {
              uNodes.add(target);
              graph.nodes.push({ id: target, group: 1 })
            }
          }
        });

        console.log(graph);
        // Update simulation
        // Apply the general update pattern to the nodes.
        node = node.data(graph.nodes, function (d) { return d.id; });
        node.exit().remove();
        nodeEnter = node.enter()
          .append('text')
          .attr('x', 6)
          .attr('y', 3)
          .text(function (d) { return d.id; });

        node
          .append("circle")
          .attr("r", 8);

        node = node.merge(nodeEnter);

        // Apply the general update pattern to the links.
        link = link
          .data(graph.links, function (d) { return d.source.id + "-" + d.target.id; });
        link.exit().remove();
        link = link.enter()
          .append("line")
          .attr("stroke-width", function (d) {
            return 1 * d.value;
          })
          .merge(link);

        simulation
          .nodes(graph.nodes)
        simulation.force("link")
          .links(graph.links);

        simulation.alpha(1).restart();
      });
  });

  function dragstarted(d) {
    if (!d3.event.active) simulation.alphaTarget(0.3).restart();
    d.fx = d.x;
    d.fy = d.y;
  }

  function dragged(d) {
    d.fx = d3.event.x;
    d.fy = d3.event.y;
  }

  function dragended(d) {
    if (!d3.event.active) simulation.alphaTarget(0);
    d.fx = null;
    d.fy = null;
  }
});