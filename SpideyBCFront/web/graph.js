
$(function() {

  var clicks = 0;
  var timer = null;

  _this = this;
  _this.clicks = clicks;
  _this.timer = timer;


  /* Code Viewer */  
  $(".codeViewer").fancybox({
    fitToView : false,
    width   : '95%',
    height    : '95%',
    autoSize  : false,
    closeClick  : false,
    openEffect  : 'fade',
    closeEffect : 'elastic'
  });

  var timeout_callback = function(d) {
    if(_this.clicks == 1) {
      toggle(d); 
      update(d); 
    } else {
      var selector = '#anchor-' + d.guid;
      console.log(selector)
      $(selector).click();
    }
    _this.clicks = 0;
    _this.timer = null;
  }

  var m = [20, 120, 20, 120],
    w = 2500 - m[1] - m[3],
    h = 900 - m[0] - m[2],
    i = 0,
    root;

  var tree = d3.layout.tree()
      .size([h, w]);

  var diagonal = d3.svg.diagonal()
      .projection(function(d) { return [d.y, d.x]; });

  var vis = d3.select("#graph").append("svg:svg")
    .attr("width", w + m[1] + m[3])
    .attr("height", h + m[0] + m[2])
    .append("svg:g")
    .attr("transform", "translate(" + m[3] + "," + m[0] + ")");

  var div = d3.select("body").append("div")
    .attr("class", "tooltip")         
    .style("opacity", 0);

  json = JSON.parse(dataset)
  root = json;
  root.x0 = h / 2;
  root.y0 = 0;

  function toggleAll(d) {
    if(d.children) {
      d.children.forEach(toggleAll);
      toggle(d);
    }
  }

  // Initialize the display to show a few nodes.
  root.children.forEach(toggleAll);
  toggle(root);

  update(root);

  function update(source) {
    var duration = d3.event && d3.event.altKey ? 5000 : 500;

    // Compute the new tree layout.
    var nodes = tree.nodes(root).reverse();

    // Normalize for fixed-depth.
    nodes.forEach(function(d) { d.y = d.depth * 180; });

    // Update the nodes…
    var node = vis.selectAll("g.node")
        .data(nodes, function(d) { return d.id || (d.id = ++i); });

    // Enter any new nodes at the parent's previous position.
    var nodeEnter = node.enter().append("svg:g")
        .attr("class", "node")
        .attr("transform", function(d) { return "translate(" + source.y0 + "," + source.x0 + ")"; })
        .on("click", function(d) {
          if(_this.timer == null) {
            _this.timer = setTimeout(timeout_callback, 200, d);
          }
          _this.clicks++;
        })
        .on("mouseover", function(d) {
          if(typeof(d.signature) !== "undefined" && d.signature !== null){
            div.transition()        
              .duration(200)      
              .style("opacity", .7);      
            div.html(d.signature + "<br/>" + "cost: " + d.cost)  
              .style("left", (d3.event.pageX + 10) + "px")     
              .style("top", (d3.event.pageY - 40) + "px");
            }
          })
        .on("mouseout", function(d) {       
          div.transition()        
          .duration(500)      
          .style("opacity", 0)
        });

    nodeEnter.append("svg:circle")
      .attr("r", 10)
      .style('stroke', function(d) { return d.color})
      .style("fill", function(d) { return d._children ? d.color : "#FFFFFF"; });

    nodeEnter.append("svg:text")
      .attr("x", function(d) { return d.children || d._children ? -10 : 10; })
      .attr("dy", "-1.3em")
      .attr("text-anchor", function(d) { return d.children || d._children ? "end" : "start"; })
      .text(function(d) { return d.name; })
      .style("fill-opacity", 1e-6);

    nodeEnter.append("svg:text")
      .attr("dx", "-4px")
      .attr("dy", "5px")
      .text(function(d) { return d.cost > 0 ? "C" : ""})

    // Transition nodes to their new position.
    var nodeUpdate = node.transition()
        .duration(duration)
        .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; });

    nodeUpdate.select("circle")
        .attr("r", 10)
        .style('stroke', function(d) { return d.color})
        .style("fill", function(d) { return d._children ? d.color : "#FFFFFF"; });

    nodeUpdate.select("text")
        .style("fill-opacity", 1)

    // Transition exiting nodes to the parent's new position.
    var nodeExit = node.exit().transition()
        .duration(duration)
        .attr("transform", function(d) { return "translate(" + source.y + "," + source.x + ")"; })
        .remove();

    nodeExit.select("circle")
        .attr("r", 10);

    nodeExit.select("text")
        .style("fill-opacity", 1e-6)

    // Update the links…
    var link = vis.selectAll("path.link")
        .data(tree.links(nodes), function(d) { return d.target.id; });

    // Enter any new links at the parent's previous position.
    link.enter().insert("svg:path", "g")
        .attr("class", "link")
        .attr("d", function(d) {
          var o = {x: source.x0, y: source.y0};
          return diagonal({source: o, target: o});
        })
      .transition()
        .duration(duration)
        .attr("d", diagonal);

    // Transition links to their new position.
    link.transition()
        .duration(duration)
        .attr("d", diagonal);

    // Transition exiting nodes to the parent's new position.
    link.exit().transition()
        .duration(duration)
        .attr("d", function(d) {
          var o = {x: source.x, y: source.y};
          return diagonal({source: o, target: o});
        })
        .remove();

    // Stash the old positions for transition.
    nodes.forEach(function(d) {
      d.x0 = d.x;
      d.y0 = d.y;
    });
  }

  // Toggle children.
  function toggle(d) {
    if (d.children) {
      d._children = d.children;
      d.children = null;
    } else {
      d.children = d._children;
      d._children = null;
    }
  }  
})
