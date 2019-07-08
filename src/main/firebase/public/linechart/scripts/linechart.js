define(function(){
    return function() {
      var margin = {top: 20, right: 20, bottom: 20, left: 50},
          width = 600,
          height = 400,
          duration = 3000,
          delay = 0,
          timeStamp = +(new Date()),
          xMaxPrev = 0,
          xMaxMin,        
          xShift,
          data = [],
          yDom = [],
          legendPosXinit = 15,
          legendPosYinit = 20,
          lines,
        //  xMin,xMax,

          maxDate = false,

        //  t = d3.transition().duration(duration).ease(d3.easeLinear),
          x = d3.scaleTime().rangeRound([0, width-margin.left-margin.right]),
          color = d3.scaleOrdinal(d3.schemeCategory10).domain(['max','mean','min']);



      function chart(selection) {

        var y = d3.scaleLog().rangeRound([height-margin.top-margin.bottom, 1]);
         
        selection.each(function(xdata) {

          init(this);

    
          xdata.forEach( d => !data.some( dd => dd.time == d.time ) && data.push(d) )
          data = data.sort( (a,b) =>  a.time - b.time );
          
          xMin = +data[0].time;
          xMax = data.length > 1 ? +data[data.length-2].time : +data[data.length-1].time;
          if(!xMaxPrev) xMaxPrev = xMax;
          xShift = xMax - xMaxPrev; 
          xMin += xShift;

          xMaxPrev = xMax;
 
      
          x.domain([xMin,xMax]);

    
          if(xShift > 0) {
            while(+data[0].time < xMin) { data.shift() ; }
            console.log('shift=' + xShift)
            lines.transition().duration(400).attr("transform", "translate(" + x(xMin - xShift) + ",0)")
          }      

          yDom = [
            d3.min(data, d => +d.min),
            d3.max(data, d => +d.max)
          ];
          y.domain(yDom)

       
          var lineData = [ "max", "mean", "min"].map(function(c,i) {
            return {
              label: c,
              labelPosY : legendPosYinit + i * 20,
              color : color(c),
              line : d3.line()
                .curve(d3.curveBasis)
                .x(d => x(+d.time))
                .y(d => y(+d[c]))
                .defined(d => d[c])
                (data) 
          } })

           
            d3.select('.axis--y').call(d3.axisLeft(y).ticks(10));
            
            lines = d3.select('#graph-lines').selectAll('path')
              .data(lineData)

            lines.enter()
            .append('path')
              .style("stroke", d => d.color)
              .style("stroke-width", 1)
              .style("fill", "none")
              .attr('d', d => d.line)
              .each(function(d) {
                  d3.select(this.parentNode)
                    .append('text')
                    .text(d.label)
                    .style('stroke',d.color)
                    .attr('y',d.labelPosY)
                    .attr('x',legendPosXinit)
              })

             
              .merge(lines)
              .attr("d", d => d.line)
              .call ( () => d3.select('.axis--x').call(d3.axisBottom(x).ticks(5)))
      
        });
      }


      function init(container) {

        var svg = d3.select(container).selectAll("svg").data([1]);
        var g = svg.enter().append("svg")
            .attr('width', width).attr('height', height)
            .append("g");

        g.append("defs").append("clipPath")
            .attr("id", "clip")
            .append("rect")
              .attr("width", width-margin.left-margin.right)
              .style("fill-opacity", 0.7)
              .attr("height", height-margin.top-margin.bottom);
    
        g.append("g")
            .attr("class", "axis axis--x")
            .attr("transform", "translate(" + [margin.left,height - margin.top -margin.bottom] + ")")
           
        g.append("g")
            .attr("class", "axis axis--y")
            .attr("transform", "translate(" + [margin.left,0] + ")")
    
        g.append("g")
          .attr("transform", "translate(" + [margin.left,0] + ")")
            .attr("clip-path", "url(#clip)")
            .attr('id', 'graph-lines')

      }

      chart.margin = function(_) {
        if (!arguments.length) return margin;
        margin = _;
        return chart;
      };

      chart.width = function(_) {
        if (!arguments.length) return width;
        width = _;
        return chart;
      };

      chart.height = function(_) {
        if (!arguments.length) return height;
        height = _;
        return chart;
      };

      chart.color = function(_) {
        if (!arguments.length) return color;
        color = _;
        return chart;
      };

      chart.duration = function(_) {
        if (!arguments.length) return duration;
        duration = _;
        return chart;
      };

      return chart;
    }
});
