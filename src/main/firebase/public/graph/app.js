require(['db'], function (db) {
 
  var width = 1000;
  var height = 1000;
  var svg = d3.select("svg")
    .style("width", width)
    .style("height", height),
  defs = svg.selectAll('defs').data([1]).enter().append('defs');
  
  defs
    .append('marker')
      .attr('id', 'end-arrow')
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 6)
      .attr('markerWidth', 3)
      .attr('markerHeight', 3)
      .attr('orient', 'auto')
      .append('svg:path')
        .style("opacity", 0.9)
        .attr('d', 'M0,-5L10,0L0,5')
        .attr('fill', '#B64A2A');

  defs
    .append('marker')
      .attr('id', 'start-arrow')
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 4)
      .attr('markerWidth', 3)
      .attr('markerHeight', 3)
      .attr('orient', 'auto')
      .append('svg:path')
        .style("opacity", 0.9)
        .attr('d', 'M10,-5L0,0L10,5')
        .attr('fill', '#B64A2A');


  var link, node, 
    links = svg.append("g")
      .attr("class", "links"),
    nodes = svg.append("g")
      .attr("class", "nodes")
    

  function ticked() {

    link.attr('d', function(d) {
      var rB1 = d.source.r,
        rB2 = d.target.r,
        rA1 = rB1,
        rA2 = rB2,
      
        deltaX = d.target.x - d.source.x,
        deltaY = d.target.y - d.source.y,
        alfa = Math.atan( deltaX / deltaY ),          
        dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) ,
        normX = deltaX / dist,
        normY = deltaY / dist,
          
        r1 = 1 / Math.sqrt( Math.pow( Math.sin( alfa) / rB1, 2) + Math.pow( Math.cos( alfa) / rA1, 2) ),        
        r2 = 1 / Math.sqrt( Math.pow( Math.sin( alfa) / rB2, 2) + Math.pow( Math.cos( alfa) / rA2, 2) ),                
            
        sourcePadding = d.left ? r1 +1 : r1 +1,
        targetPadding = d.right ? r2 +4 : r2 +4,
          
        sourceX = d.source.x + (sourcePadding * normX),
        sourceY = d.source.y + (sourcePadding * normY),
        targetX = d.target.x - (targetPadding * normX),
        targetY = d.target.y - (targetPadding * normY);
          
      return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
    });

      node.attr("transform", d => "translate(" + d.x + "," + d.y + ")");    
  }

  var simulation = d3.forceSimulation()
    .force("link",
      d3.forceLink()
        .id(function (d) { return d.id; })
        //.strength(0.8)
        //.distance(function (d) {
        //  return 300/(1+d.value);
        //})
        .strength(function (d) { return d.intra == 1 ?  1 : 0.1 })
        .distance(function (d) { return d.intra == 1 ? 0.01 : 300/1+d.value })
    )
    .force("charge", d3.forceManyBody())
   // .force("center", d3.forceCenter(width / 2, height / 2))
    .force("collide",d3.forceCollide( d => d.r +10).strength(0.01).iterations(5))
    .force("y", d3.forceY().y( height/2 ).strength(0.04))
    .force("x", d3.forceX().x( width/2 ).strength(0.04))
   

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
    var uNodes = new Set();

    db.collection("demo3").doc('latest').collection('volume').limit(30)
      .onSnapshot(querySnapshot => {
        querySnapshot.docChanges().forEach(change => {
          var link;
          var data = change.doc.data();
       //   console.log(data)
          var amount = data["amount"] / 10e18;

          var source = data.from;
          var target = data.to;
          var source_exchange = data.from.replace("_hw", "").replace("_uw", "");
          var target_exchange = data.to.replace("_hw", "").replace("_uw", "");

          var value = 1 + amount;
          var intra = 0;
          if (source_exchange == target_exchange) {
            value = 1;
            intra = 1;
          }

          if (source != target) { //&& source != 'unknown' && target != 'unknown') {
            // create or update links
            link = findLink(graph.links, source, target);
            if (!link) {
              link = { source: source, target: target, intra: intra, value: 1/value };
              graph.links.push(link);
            } else {
              link.value = value;
            }
 
            // create missing nodes
            if (!uNodes.has(source)) {
              uNodes.add(source);
              graph.nodes.push({ id: source, group: 1, value : -amount })
            }else{
              uNodes.has(source).value -= amount;
            }
            if (!uNodes.has(target)) {
              uNodes.add(target);
              graph.nodes.push({ id: target, group: 1, value : +amount })
            }else{
              uNodes.has(source).value += amount;
            }
          }
        });

       // console.log(graph);
        // Update simulation
        // Apply the general update pattern to the nodes.
        node = nodes.selectAll('.node').data(graph.nodes, d => d.id);
        node.exit().remove();
        node = node.enter()
          .append('g')
          .classed('node', true)
      
          .each(function(d) {
              var el = d3.select(this);

                d.x = width/2, d.y = height/2;
                
                el.append('title')
                  

                el.append("circle")
                  .attr("r", d.r)

                el.append('text')
                  .attr('y', 3)
                  .text(d => d.id);

          })
          .call(d3.drag()
              .on("start", dragstarted)
              .on("drag", dragged)
              .on("end", dragended)
            )
          .merge(node)
          .each(function(d) {
            var el = d3.select(this);
            d.r =  3*d.value + 8;
            el.select('circle').attr("r",d.r);
            el.select('text').attr("x",d.r  + 2);
            el.select('title').text(d.id + ': ' + d.value);

          }) 

            
            
        // Apply the general update pattern to the links.
        link = links.selectAll('.link').data(graph.links, d => d.source.id + "-" + d.target.id);
        link.exit().remove();
        link = link.enter()
          .append("path")
            .classed('link',true)
            .style('marker-end', function(d) { return 'url(#end-arrow)' })
            .attr("stroke-width", d => 2 * d.value)
            .attr('d',"M0,0")
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
