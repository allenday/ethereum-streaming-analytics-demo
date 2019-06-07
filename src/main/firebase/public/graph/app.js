require(['db'], function (db) {
  var width = 1000;
  var height = 1000;
  var svg = d3.select("svg")
    .style("width", width)
    .style("height", height)
    //  .style("background-color","#FFFAFA");
    ;
  var link, node, 
    links = svg.append("g")
      .attr("class", "links"),
    nodes = svg.append("g")
      .attr("class", "nodes")
    

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
        .strength(0.8)
        .distance(function (d) {
          return 300/(1+d.value);
        })
    )
    .force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(width / 2, height / 2))
   

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

          if (source != target && source != 'unknown' && target != 'unknown') {
            // create or update links
            link = findLink(graph.links, source, target);
            if (!link) {
              link = { source: source, target: target, value: 1/(1 + amount) };
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
        node = nodes.selectAll('.node').data(graph.nodes, d => d.id);
        node.exit().remove();
        node = node.enter()
          .append('g')
          .classed('node', true)
      
          .each(function(d) {
              var el = d3.select(this);
                
                el.append('title')
                  .text(d => d.id)

                el.append("circle")
                  .attr("r", 8)

                el.append('text')
                  .attr('x', 8)
                  .attr('y', 3)
                  .text(d => d.id);

          })
          .call(d3.drag()
              .on("start", dragstarted)
              .on("drag", dragged)
              .on("end", dragended)
            )
          .merge(node);
            
            
        // Apply the general update pattern to the links.
        link = links.selectAll('.link').data(graph.links, d => d.source.id + "-" + d.target.id);
        link.exit().remove();
        link = link.enter()
          .append("line")
          .classed('link',true)
          .attr("stroke-width", function (d) {
            return 2 * d.value;
          })
          .merge(link);

        simulation
        .on("tick", ticked)
          .nodes(graph.nodes)
          .force("link")
            .links(graph.links)
          
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
    d.fx = d.x; // null
    d.fy = d.y; // null
  }
});
